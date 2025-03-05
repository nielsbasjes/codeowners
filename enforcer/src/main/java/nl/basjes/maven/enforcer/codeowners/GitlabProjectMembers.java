/*
 * CodeOwners Tools
 * Copyright (C) 2023-2025 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.basjes.maven.enforcer.codeowners;

import lombok.Getter;
import lombok.Setter;
import nl.basjes.codeowners.ApprovalRule;
import nl.basjes.codeowners.CodeOwners;
import nl.basjes.codeowners.Section;
import nl.basjes.maven.enforcer.codeowners.GitlabConfiguration.FailLevel;
import nl.basjes.maven.enforcer.codeowners.GitlabConfiguration.Level;
import nl.basjes.maven.enforcer.codeowners.GitlabConfiguration.ProjectId;
import nl.basjes.maven.enforcer.codeowners.utils.Problem;
import nl.basjes.maven.enforcer.codeowners.utils.ProblemTable;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.SharedGroup;
import org.gitlab4j.api.models.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static nl.basjes.maven.enforcer.codeowners.GitlabConfiguration.Level.INFO;
import static org.gitlab4j.api.models.AccessLevel.DEVELOPER;
import static org.gitlab4j.api.models.AccessLevel.MAINTAINER;
import static org.gitlab4j.api.models.AccessLevel.OWNER;

public class GitlabProjectMembers implements AutoCloseable {

    private final ProjectId     projectId;
    @Getter @Setter
    private       boolean       showAllApprovers;
    private final FailLevel     failLevel;
    private final boolean       assumeUncheckableEmailExistsAndCanApprove;

    private final GitLabApi gitLabApi;

    // Same pattern as defined in the codeowners parser
    private static final Pattern EMAIL_ADDRESS = Pattern.compile("^[a-zA-Z0-9_-]+@[a-zA-Z0-9._-]+$");

    private final Level roleNoUsers;
    private final Level userUnknownEmail;
    private final Level userDisabled;
    private final Level approverDoesNotExist;
    private final Level userTooLowPermissions;
    private final Level groupTooLowPermissions;
    private final Level userNotProjectMember;
    private final Level groupNotProjectMember;
    private final Level noValidApprovers;

    public GitlabProjectMembers(GitlabConfiguration configuration) throws EnforcerRuleException {
        if (!configuration.isValid()) {
            throw new EnforcerRuleException("Invalid Gitlab configuration: " + configuration);
        }

        GitlabConfiguration.ProblemLevels problemLevels = configuration.getProblemLevels();
        roleNoUsers              = problemLevels.roleNoUsers;
        userUnknownEmail         = problemLevels.userUnknownEmail;
        userDisabled             = problemLevels.userDisabled;
        approverDoesNotExist     = problemLevels.approverDoesNotExist;
        userTooLowPermissions    = problemLevels.userTooLowPermissions;
        groupTooLowPermissions   = problemLevels.groupTooLowPermissions;
        userNotProjectMember     = problemLevels.userNotProjectMember;
        groupNotProjectMember    = problemLevels.groupNotProjectMember;
        noValidApprovers         = problemLevels.noValidApprovers;

        gitLabApi = new GitLabApi(
            configuration.getServerUrl().getValue(),
            configuration.getAccessToken().getValue()
        );

        projectId = configuration.getProjectId();

        try {
            gitLabApi.getProjectApi().getProject(projectId.getValue());
        } catch (GitLabApiException e) {
            throw new EnforcerRuleException("Unable to load projectId from Gitlab: " + configuration, e);
        }
        showAllApprovers = configuration.isShowAllApprovers();
        assumeUncheckableEmailExistsAndCanApprove = configuration.isAssumeUncheckableEmailExistsAndCanApprove();
        failLevel = configuration.getFailLevel();
    }

    @Override
    public void close() {
        if (gitLabApi != null) {
            gitLabApi.close();
        }
    }

    private boolean accessLevelCanApprove(AccessLevel accessLevel) {
        // This ignores the possibility of assigning extra approvers in
        // merge request approval rules because that works completely outside the CODEOWNERS file.
        return accessLevel.toValue() >= DEVELOPER.toValue();
    }

    private boolean usableAccount(Member member) {
        boolean locked = member.getLocked() == Boolean.TRUE;
        boolean active = "active".equals(member.getState());
        return !locked && active;
    }

    private boolean canApprove(Member member) {
        return (usableAccount(member) && accessLevelCanApprove(member.getAccessLevel()));
    }

    public List<Member> getAllProjectMembers() throws EnforcerRuleException {
        try {
            return gitLabApi.getProjectApi().getAllMembers(projectId.getValue());
        } catch (GitLabApiException e) {
            throw new EnforcerRuleException("Error while retrieving all project members.", e);
        }
    }

    public List<Member> getDirectProjectMembers() throws EnforcerRuleException {
        try {
            return gitLabApi.getProjectApi().getMembers(projectId.getValue());
        } catch (GitLabApiException e) {
            throw new EnforcerRuleException("Error while retrieving direct project members.", e);
        }
    }

    public List<SharedGroup> getSharedGroups() throws EnforcerRuleException {
        try {
            // Get the direct shared groups of this project that have the right name.
            Project project = gitLabApi.getProjectApi().getProject(projectId.getValue());
            return project.getSharedWithGroups();
        } catch (GitLabApiException e) {
            throw new EnforcerRuleException("Error while retrieving project shared groups.", e);
        }
    }

    public Group getGroup(String groupId) throws GitLabApiException {
        // Get the direct shared groups of this project that have the right name.
        return gitLabApi.getGroupApi().getGroup(groupId);
    }

    private void report(Level level, ProblemTable table, Section section, ApprovalRule approvalRule, String approver, String message) {
        switch (level) {
            case FATAL:
                table.addProblem(new Problem.Fatal(section.getName(), approvalRule.getFileExpression(), approver, message));
                break;
            case ERROR:
                table.addProblem(new Problem.Error(section.getName(), approvalRule.getFileExpression(), approver, message));
                break;
            case WARNING:
                table.addProblem(new Problem.Warning(section.getName(), approvalRule.getFileExpression(), approver, message));
                break;
            case INFO:
                table.addProblem(new Problem.Info(section.getName(), approvalRule.getFileExpression(), approver, message));
                break;
        }
    }

    public ProblemTable verifyAllCodeowners(EnforcerLogger log, CodeOwners codeOwners) throws EnforcerRuleException {
        log.info("Fetching all project members from Gitlab (can easily take >15 seconds on a many user project)");
        Map<String, Member>      allProjectMembers    = getAllProjectMembers().stream().collect(Collectors.toMap(Member::getUsername, Function.identity()));
        log.info("-- Received project members: " + allProjectMembers.size());

        log.info("Fetching all direct project members from Gitlab (can easily take >15 seconds on a many user project)");
        Map<String, Member>      directProjectMembers = getDirectProjectMembers().stream().collect(Collectors.toMap(Member::getUsername, Function.identity()));
        log.info("-- Received direct project members: " + directProjectMembers.size());

        log.info("Fetching shared groups from Gitlab");
        Map<String, SharedGroup> sharedGroups         = getSharedGroups().stream().collect(Collectors.toMap(SharedGroup::getGroupFullPath, Function.identity()));
        log.info("-- Received shared groups: " + sharedGroups.size());

        log.info("Verifying CODEOWNERS with actual user permissions...");

        Map<String, User>  userCache = new HashMap<>();
        Map<String, Group> groupCache = new HashMap<>();

        List<Member> owners       = directProjectMembers.values().stream().filter(this::canApprove).filter(member -> member.getAccessLevel().equals(OWNER)).collect(Collectors.toList());
        List<Member> developers   = directProjectMembers.values().stream().filter(this::canApprove).filter(member -> member.getAccessLevel().equals(DEVELOPER)).collect(Collectors.toList());
        List<Member> maintainers  = directProjectMembers.values().stream().filter(this::canApprove).filter(member -> member.getAccessLevel().equals(MAINTAINER)).collect(Collectors.toList());

        ProblemTable results = new ProblemTable();

        for (Section definedSection : codeOwners.getAllDefinedSections()) {
            for (ApprovalRule approvalRule : definedSection.getApprovalRules()) {
                boolean ruleHasValidApprovers = false;
                for (String rawApprover : approvalRule.getApprovers()) {
                    String approver = rawApprover;
                    // Check if this is a role
                    if (approver.startsWith("@@")) {
                        // https://docs.gitlab.com/user/project/codeowners/reference/#add-a-role-as-a-code-owner
                        // Only direct project members
                        // Only Developer, Maintainer, and Owner roles are available.

                        // Here we only check if there is at least 1 in this collection
                        switch (approver) {
                            case "@@owner":
                            case "@@owners":
                                if (owners.isEmpty()) {
                                    report(roleNoUsers, results, definedSection, approvalRule, rawApprover,
                                        "No direct project members have the \"owner\" role");
                                } else {
                                    if (showAllApprovers) {
                                        report(INFO, results, definedSection, approvalRule, rawApprover,
                                            "Valid approver: Found " + owners.size() + " owners");
                                    }
                                    ruleHasValidApprovers=true;
                                }
                                continue;
                            case "@@developer":
                            case "@@developers":
                                if (developers.isEmpty()) {
                                    report(roleNoUsers, results, definedSection, approvalRule, rawApprover,
                                        "No direct project members have the \"developer\" role");
                                } else {
                                    if (showAllApprovers) {
                                        report(INFO, results, definedSection, approvalRule, rawApprover,
                                            "Valid approver: Found " + developers.size() + " developers");
                                    }
                                    ruleHasValidApprovers=true;
                                }
                                continue;
                            case "@@maintainer":
                            case "@@maintainers":
                                if (maintainers.isEmpty()) {
                                    report(roleNoUsers, results, definedSection, approvalRule, rawApprover,
                                        "No direct project members have the \"maintainer\" role");
                                } else {
                                    if (showAllApprovers) {
                                        report(INFO, results, definedSection, approvalRule, rawApprover,
                                            "Valid approver: Found " + maintainers.size() + " maintainers");
                                    }
                                    ruleHasValidApprovers=true;
                                }
                                continue;
                            default:
                                report(Level.FATAL, results, definedSection, approvalRule, rawApprover, "Illegal role was specified");
                                continue;
                        }
                    }

                    if (EMAIL_ADDRESS.matcher(approver).matches()) {
                        try {
                            User userByEmail = gitLabApi.getUserApi().getUserByEmail(approver);
                            if (userByEmail == null) {
                                // Unable to find the user by email.
                                // This is VERY common to happen because Gitlab does not allow a normal user
                                // (who commonly runs this code) to query other users by their private email.
                                // Gitlab itself IS allowed to check this while verifying the CODEOWNERS file.
                                // https://docs.gitlab.com/api/users/#as-a-regular-user
                                report(userUnknownEmail, results, definedSection, approvalRule, rawApprover, "Unable to verify email address: " +
                                    "Assuming the user " + (assumeUncheckableEmailExistsAndCanApprove ? "exists and can approve" : "does not exist and/or cannot approve"));
                                ruleHasValidApprovers=assumeUncheckableEmailExistsAndCanApprove;
                                continue;
                            } else {
                                approver = userByEmail.getUsername();
                            }
                        } catch (GitLabApiException e) {
                            throw new EnforcerRuleException("Error while searching for user by email:" + approver, e);
                        }
                    } else {
                        // Strip the leading '@'
                        approver = approver.substring(1);
                    }

                    // Is this a Member?
                    if (allProjectMembers.containsKey(approver)) {
                        Member member = allProjectMembers.get(approver);
                        if (!usableAccount(member)) {
                            report(userDisabled, results, definedSection, approvalRule, rawApprover,
                                "Disabled account: " +
                                    "State=" + member.getState() + "; " +
                                    "Locked=" + member.getLocked());
                            continue;
                        }
                        if (canApprove(member)) {
                            // Success, we have found this valid approver.
                            if (showAllApprovers) {
                                AccessLevel memberAccessLevel = member.getAccessLevel();
                                report(INFO, results, definedSection, approvalRule, rawApprover,
                                    "Valid approver: Member with username \"" + approver + "\" can approve (AccessLevel:" + memberAccessLevel.toValue() + "=" + memberAccessLevel.name() + ")");
                            }
                            ruleHasValidApprovers=true;
                        } else {
                            report(userTooLowPermissions, results, definedSection, approvalRule, rawApprover,
                                "User does not have sufficient permissions to approve: AccessLevel=" + member.getAccessLevel().name());
                        }
                        continue;
                    }

                    // Is this a Shared Group?
                    if (sharedGroups.containsKey(approver)) {
                        SharedGroup sharedGroup = sharedGroups.get(approver);
                        AccessLevel groupAccessLevel = sharedGroup.getGroupAccessLevel();
                        if (accessLevelCanApprove(groupAccessLevel)) {
                            // Success, we have found this valid approver to be the name of a shared group.
                            if (showAllApprovers) {
                                report(INFO, results, definedSection, approvalRule, rawApprover,
                                    "Valid approver: Members of group \"" + approver + "\" can approve (AccessLevel:" + groupAccessLevel.toValue() + "="+groupAccessLevel.name() + ")");
                            }
                            ruleHasValidApprovers = true;
                            continue;
                        }
                        report(groupTooLowPermissions, results, definedSection, approvalRule, rawApprover,
                            "Shared group does not have sufficient permissions to approve: AccessLevel=" + groupAccessLevel.toValue() + " (="+groupAccessLevel.name() + ")");
                        continue;
                    }

                    // Is this a user that exists but has not been made part of this project?
                    User user = userCache.get(approver);
                    if (user == null && !userCache.containsKey(approver)) {
                        try {
                            user = gitLabApi.getUserApi().getUser(approver);
                        } catch (GitLabApiException ignored) {
                            // If error then group == null
                        }
                        userCache.put(approver, user);
                    }
                    if (user != null) {
                        report(userNotProjectMember, results, definedSection, approvalRule, rawApprover, "User exists but is not a member of with this project: " + user.getName());
                        continue;
                    }

                    // Is this a group that exists but has not been made part of this project?
                    Group group = groupCache.get(approver);
                    if (group == null && !groupCache.containsKey(approver)) {
                        try {
                            group = getGroup(approver);
                        } catch (GitLabApiException ignored) {
                            // If error then group == null
                        }
                        groupCache.put(approver, group);
                    }
                    if (group != null) {
                        report(groupNotProjectMember, results, definedSection, approvalRule, rawApprover, "Group exists and is not shared with this project");
                        continue;
                    }

                    // Ok, it doesn't exist.
                    report(approverDoesNotExist, results, definedSection, approvalRule, rawApprover, "Approver does not exist in Gitlab (not as user and not as group)");
                }

                if (!ruleHasValidApprovers) {
                    report(noValidApprovers, results, definedSection, approvalRule, "", "NO Valid Approvers for rule");
                }
            }
        }
        return results;
    }

    public void failIfExceededFailLevel(ProblemTable problemTable) throws EnforcerRuleException {
        if (problemTable.isEmpty()) {
            return;
        }
        switch (failLevel) {
            case NEVER: // Never fail
                return;
            case FATAL:
                if (!problemTable.hasFatalErrors()) {
                    return;
                }
                break;
            case ERROR:
                if (!problemTable.hasFatalErrors() && !problemTable.hasErrors()) {
                    return;
                }
                break;
            case WARNING:
                if (!problemTable.hasFatalErrors() && !problemTable.hasErrors() && !problemTable.hasWarnings()) {
                    return;
                }
                break;
        }
        throw new EnforcerRuleException("Found " + problemTable.getNumberOfWarnings() + " warnings, " + problemTable.getNumberOfErrors() + " errors and " + problemTable.getNumberOfFatalErrors() + " fatal problems of the CODEOWNERS file in relation to the Gitlab project.\n" + problemTable);
    }

}
