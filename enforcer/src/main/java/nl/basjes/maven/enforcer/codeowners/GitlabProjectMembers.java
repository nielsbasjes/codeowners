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
import nl.basjes.maven.enforcer.codeowners.GitlabConfiguration.ProjectId;
import nl.basjes.maven.enforcer.codeowners.utils.Problem;
import nl.basjes.maven.enforcer.codeowners.utils.ProblemTable;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.gitlab4j.api.models.AccessLevel.DEVELOPER;
import static org.gitlab4j.api.models.AccessLevel.MAINTAINER;
import static org.gitlab4j.api.models.AccessLevel.OWNER;

public class GitlabProjectMembers implements AutoCloseable {

    private final ProjectId     projectId;
    @Getter @Setter
    private       boolean       showAllApprovers;

    private final GitLabApi gitLabApi;

    // Same pattern as defined in the codeowners parser
    private static final Pattern EMAIL_ADDRESS = Pattern.compile("^[a-zA-Z0-9_-]+@[a-zA-Z0-9._-]+$");

    public GitlabProjectMembers(GitlabConfiguration configuration) throws EnforcerRuleException {
        if (!configuration.isValid()) {
            throw new EnforcerRuleException("Invalid Gitlab configuration: " + configuration);
        }

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

    private void info(ProblemTable table, Section section, ApprovalRule approvalRule, String approver, String message) {
        table.addProblem(new Problem.Info(section.getName(), approvalRule.getFileExpression(), approver, message));
    }
    private void warning(ProblemTable table, Section section, ApprovalRule approvalRule, String approver, String message) {
        table.addProblem(new Problem.Warning(section.getName(), approvalRule.getFileExpression(), approver, message));
    }
    private void error(ProblemTable table, Section section, ApprovalRule approvalRule, String approver, String message) {
        table.addProblem(new Problem.Error(section.getName(), approvalRule.getFileExpression(), approver, message));
    }

    public void verifyAllCodeowners(EnforcerLogger log, CodeOwners codeOwners) throws EnforcerRuleException {
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
                                    warning(results, definedSection, approvalRule, rawApprover, "No direct project members are owner");
                                } else {
                                    if (showAllApprovers) {
                                        info(results, definedSection, approvalRule, rawApprover, "Valid approver (found " + owners.size() + " owners:" + owners.stream().map(Member::getUsername).collect(Collectors.toList()) + ")");
                                    }
                                    ruleHasValidApprovers=true;
                                }
                                continue;
                            case "@@developer":
                            case "@@developers":
                                if (developers.isEmpty()) {
                                    warning(results, definedSection, approvalRule, rawApprover, "No direct project members are developer");
                                } else {
                                    if (showAllApprovers) {
                                        info(results, definedSection, approvalRule, rawApprover, "Valid approver (found " + developers.size() + " developers:" + developers.stream().map(Member::getUsername).collect(Collectors.toList()) + ")");
                                    }
                                    ruleHasValidApprovers=true;
                                }
                                continue;
                            case "@@maintainer":
                            case "@@maintainers":
                                if (maintainers.isEmpty()) {
                                    warning(results, definedSection, approvalRule, rawApprover, "No direct project members are maintainer");
                                } else {
                                    if (showAllApprovers) {
                                        info(results, definedSection, approvalRule, rawApprover, "Valid approver (found " + maintainers.size() + " maintainers: " + maintainers.stream().map(Member::getUsername).collect(Collectors.toList()) + ")");
                                    }
                                    ruleHasValidApprovers=true;
                                }
                                continue;
                            default:
                                error(results, definedSection, approvalRule, rawApprover, "Illegal role attempted");
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
                                warning(results, definedSection, approvalRule, rawApprover, "Cannot verify access because this is an email address");
                                ruleHasValidApprovers=true; // FIXME: Dubious choice; we cannot verify if this approver will actually work
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
                            warning(results, definedSection, approvalRule, rawApprover,
                                "Disabled account: " +
                                    "State=" + member.getState() + "; " +
                                    "Locked=" + member.getLocked());
                            continue;
                        }
                        if (canApprove(member)) {
                            // Success, we have found this valid approver.
                            if (showAllApprovers) {
                                AccessLevel memberAccessLevel = member.getAccessLevel();
                                info(results, definedSection, approvalRule, rawApprover, "Valid approver. Member with username \""+approver+"\" has access level "+ memberAccessLevel.toValue() +" (="+memberAccessLevel.name()+")");
                            }
                            ruleHasValidApprovers=true;
                        } else {
                            warning(results, definedSection, approvalRule, rawApprover,
                                "Insufficient permissions to approve: AccessLevel=" + member.getAccessLevel().name());
                        }
                        continue;
                    }

                    // Is this a Shared Group?
                    if (sharedGroups.containsKey(approver)) {
                        SharedGroup sharedGroup = sharedGroups.get(approver);
                        if (accessLevelCanApprove(sharedGroup.getGroupAccessLevel())) {
                            // Success, we have found this valid approver to be the name of a shared group.
                            if (showAllApprovers) {
                                AccessLevel groupAccessLevel = sharedGroup.getGroupAccessLevel();
                                info(results, definedSection, approvalRule, rawApprover, "Valid approver. Group with groupname \""+approver+"\" has access level "+ groupAccessLevel.toValue() +" (="+groupAccessLevel.name()+")");
                            }
                            ruleHasValidApprovers = true;
                            continue;
                        }
                        warning(results, definedSection, approvalRule, rawApprover, "Shared group does not have sufficient approver level: " + sharedGroup.getGroupAccessLevel().name());
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
                        error(results, definedSection, approvalRule, rawApprover, "User is not a member of with this project: " + user.getName());
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
                        error(results, definedSection, approvalRule, rawApprover, "Group is not a group shared with this project.");
                        continue;
                    }

                    // Ok, it doesn't exist.
                    warning(results, definedSection, approvalRule, rawApprover, "Approver does not exist in Gitlab");
                }

                if (!ruleHasValidApprovers) {
                    error(results, definedSection, approvalRule, "", "NO Valid Approvers for rule");
                }
            }
        }

        if (!results.isEmpty()) {
            results.toLog(log);
            if (results.hasErrors()) {
                throw new EnforcerRuleException("Found " + results.getNumberOfErrors() + " errors of the CODEOWNERS file in relation to the Gitlab project.");
            }
        }
    }
}
