/*
 * CodeOwners Tools
 * Copyright (C) 2023-2026 Niels Basjes
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
package nl.basjes.codeowners.validator.gitlab

import nl.basjes.codeowners.ApprovalRule
import nl.basjes.codeowners.CodeOwners
import nl.basjes.codeowners.Section
import nl.basjes.codeowners.validator.CodeOwnersValidationException
import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration.*
import nl.basjes.codeowners.validator.utils.Problem
import nl.basjes.codeowners.validator.utils.Problem.Fatal
import nl.basjes.codeowners.validator.utils.ProblemTable
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.GitLabApiException
import org.gitlab4j.api.models.*
import org.slf4j.Logger

class GitlabProjectMembers(configuration: GitlabConfiguration) : AutoCloseable {
    private val projectId: ProjectId

    var showAllApprovers: Boolean
    private val failLevel: FailLevel
    private val assumeUncheckableEmailExistsAndCanApprove: Boolean

    private val gitLabApi: GitLabApi

    private val roleNoUsers: Level
    private val userUnknownEmail: Level
    private val userDisabled: Level
    private val approverDoesNotExist: Level
    private val userTooLowPermissions: Level
    private val groupTooLowPermissions: Level
    private val userNotProjectMember: Level
    private val groupNotProjectMember: Level
    private val noValidApprovers: Level

    init {
        require(configuration.valid) { "Invalid Gitlab configuration: $configuration" }

        val problemLevels: ProblemLevels = configuration.problemLevels
        roleNoUsers            = problemLevels.roleNoUsers
        userUnknownEmail       = problemLevels.userUnknownEmail
        userDisabled           = problemLevels.userDisabled
        approverDoesNotExist   = problemLevels.approverDoesNotExist
        userTooLowPermissions  = problemLevels.userTooLowPermissions
        groupTooLowPermissions = problemLevels.groupTooLowPermissions
        userNotProjectMember   = problemLevels.userNotProjectMember
        groupNotProjectMember  = problemLevels.groupNotProjectMember
        noValidApprovers       = problemLevels.noValidApprovers

        gitLabApi = GitLabApi(
            configuration.serverUrl.value,
            configuration.accessToken.value
        )

        projectId = configuration.projectId

        try {
            gitLabApi.getProjectApi().getProject(projectId.value)
        } catch (e: GitLabApiException) {
            throw CodeOwnersValidationException("Unable to load projectId from Gitlab: $configuration", e)
        }
        showAllApprovers = configuration.showAllApprovers
        assumeUncheckableEmailExistsAndCanApprove = configuration.isAssumeUncheckableEmailExistsAndCanApprove()
        failLevel = configuration.failLevel
    }

    override fun close() {
        gitLabApi.close()
    }

    private fun accessLevelCanApprove(accessLevel: AccessLevel): Boolean {
        // This ignores the possibility of assigning extra approvers in
        // merge request approval rules because that works completely outside the CODEOWNERS file.
        return accessLevel.toValue() >= AccessLevel.DEVELOPER.toValue()
    }

    private fun usableAccount(member: Member): Boolean {
        val locked = member.locked ?: false
        val active = "active" == member.state
        return !locked && active
    }

    private fun canApprove(member: Member): Boolean {
        return (usableAccount(member) && accessLevelCanApprove(member.accessLevel))
    }

    @get:Throws(CodeOwnersValidationException::class)
    val allProjectMembers: List<Member>
        get() {
            try {
                return gitLabApi.getProjectApi().getAllMembers(projectId.value)
            } catch (e: GitLabApiException) {
                throw CodeOwnersValidationException("Error while retrieving all project members.", e)
            }
        }

    @get:Throws(CodeOwnersValidationException::class)
    val directProjectMembers: List<Member>
        get() {
            try {
                return gitLabApi.getProjectApi().getMembers(projectId.value)
            } catch (e: GitLabApiException) {
                throw CodeOwnersValidationException("Error while retrieving direct project members.", e)
            }
        }

    @get:Throws(CodeOwnersValidationException::class)
    val sharedGroups: List<SharedGroup>
        get() {
            try {
                // Get the direct shared groups of this project that have the right name.
                val project =
                    gitLabApi.getProjectApi().getProject(projectId.value)
                return project.sharedWithGroups
            } catch (e: GitLabApiException) {
                throw CodeOwnersValidationException("Error while retrieving project shared groups.", e)
            }
        }

    @Throws(GitLabApiException::class)
    fun getGroup(groupId: String?): Group? {
        // Get the direct shared groups of this project that have the right name.
        return gitLabApi.getGroupApi().getGroup(groupId)
    }

    private fun report(
        level: Level,
        table: ProblemTable,
        section: Section,
        approvalRule: ApprovalRule,
        approver: String,
        message: String
    ) {
        when (level) {
            Level.FATAL -> table.addProblem(
                Fatal(
                    section.getName(),
                    approvalRule.getFileExpression(),
                    approver,
                    message
                )
            )

            Level.ERROR -> table.addProblem(
                Problem.Error(
                    section.getName(),
                    approvalRule.getFileExpression(),
                    approver,
                    message
                )
            )

            Level.WARNING -> table.addProblem(
                Problem.Warning(
                    section.getName(),
                    approvalRule.getFileExpression(),
                    approver,
                    message
                )
            )

            Level.INFO -> table.addProblem(
                Problem.Info(
                    section.getName(),
                    approvalRule.getFileExpression(),
                    approver,
                    message
                )
            )
        }
    }

    @Throws(CodeOwnersValidationException::class)
    fun verifyAllCodeowners(log: Logger, codeOwners: CodeOwners): ProblemTable {
        log.info("Fetching all project members from Gitlab (can easily take >15 seconds on a many user project)")
        val allProjectMembers = this.allProjectMembers
            .distinctBy { it.username }
            .associateBy { it.username }
        log.info("-- Received project members: {}", allProjectMembers.size)

        log.info("Fetching all direct project members from Gitlab (can easily take >15 seconds on a many user project)")
        val directProjectMembers = this.directProjectMembers
            .distinctBy { it.username }
            .associateBy { it.username }
        log.info("-- Received direct project members: {}", directProjectMembers.size)

        log.info("Fetching shared groups from Gitlab")
        val sharedGroups = this.sharedGroups
            .distinctBy { it.groupFullPath }
            .associateBy { it.groupFullPath }
        log.info("-- Received shared groups: {}", sharedGroups.size)

        log.info("Verifying CODEOWNERS with actual user permissions...")

        // Also cache a null to indicate it does not exist
        val userCache: MutableMap<String, User?> = mutableMapOf()
        val groupCache: MutableMap<String, Group?> = mutableMapOf()

        val owners = directProjectMembers.values
            .filter { canApprove(it) }
            .filter { it.accessLevel == AccessLevel.OWNER }
            .toList()

        val developers = directProjectMembers.values
            .filter { canApprove(it) }
            .filter { it.accessLevel == AccessLevel.DEVELOPER }
            .toList()

        val maintainers = directProjectMembers.values
            .filter { canApprove(it) }
            .filter { it.accessLevel == AccessLevel.MAINTAINER }
            .toList()

        val results = ProblemTable()

        for (definedSection in codeOwners.allDefinedSections) {
            for (rule in definedSection.rules) {
                // Only check the rules that provide approvers.
                if (rule !is ApprovalRule) {
                    continue
                }
                var ruleHasValidApprovers = false
                for (rawApprover in rule.approvers) {
                    var approver: String = rawApprover
                    // Check if this is a role
                    if (approver.startsWith("@@")) {
                        // https://docs.gitlab.com/user/project/codeowners/reference/#add-a-role-as-a-code-owner
                        // Only direct project members
                        // Only Developer, Maintainer, and Owner roles are available.

                        // Here we only check if there is at least 1 in this collection
                        when (approver) {
                            "@@owner", "@@owners" -> {
                                if (owners.isEmpty()) {
                                    report(
                                        roleNoUsers, results, definedSection, rule, rawApprover,
                                        "No direct project members have the \"owner\" role"
                                    )
                                } else {
                                    if (showAllApprovers) {
                                        report(
                                            Level.INFO,
                                            results,
                                            definedSection,
                                            rule,
                                            rawApprover,
                                            "Valid approver: Found " + owners.size + " owners"
                                        )
                                    }
                                    ruleHasValidApprovers = true
                                }
                                continue
                            }

                            "@@developer", "@@developers" -> {
                                if (developers.isEmpty()) {
                                    report(
                                        roleNoUsers, results, definedSection, rule, rawApprover,
                                        "No direct project members have the \"developer\" role"
                                    )
                                } else {
                                    if (showAllApprovers) {
                                        report(
                                            Level.INFO,
                                            results,
                                            definedSection,
                                            rule,
                                            rawApprover,
                                            "Valid approver: Found " + developers.size + " developers"
                                        )
                                    }
                                    ruleHasValidApprovers = true
                                }
                                continue
                            }

                            "@@maintainer", "@@maintainers" -> {
                                if (maintainers.isEmpty()) {
                                    report(
                                        roleNoUsers, results, definedSection, rule, rawApprover,
                                        "No direct project members have the \"maintainer\" role"
                                    )
                                } else {
                                    if (showAllApprovers) {
                                        report(
                                            Level.INFO,
                                            results,
                                            definedSection,
                                            rule,
                                            rawApprover,
                                            "Valid approver: Found " + maintainers.size + " maintainers"
                                        )
                                    }
                                    ruleHasValidApprovers = true
                                }
                                continue
                            }

                            else -> {
                                report(
                                    Level.FATAL,
                                    results,
                                    definedSection,
                                    rule,
                                    rawApprover,
                                    "Illegal role was specified"
                                )
                                continue
                            }
                        }
                    }

                    if (EMAIL_ADDRESS.matches(approver)) {
                        try {
                            val userByEmail = gitLabApi.getUserApi().getUserByEmail(approver)
                            if (userByEmail == null) {
                                // Unable to find the user by email.
                                // This is VERY common to happen because Gitlab does not allow a normal user
                                // (who commonly runs this code) to query other users by their private email.
                                // Gitlab itself IS allowed to check this while verifying the CODEOWNERS file.
                                // https://docs.gitlab.com/api/users/#as-a-regular-user
                                report(
                                    userUnknownEmail,
                                    results,
                                    definedSection,
                                    rule,
                                    rawApprover,
                                    "Unable to verify email address: " +
                                            "Assuming the user " + (if (assumeUncheckableEmailExistsAndCanApprove) "exists and can approve" else "does not exist and/or cannot approve")
                                )
                                ruleHasValidApprovers = assumeUncheckableEmailExistsAndCanApprove
                                continue
                            } else {
                                approver = userByEmail.username
                            }
                        } catch (e: GitLabApiException) {
                            throw CodeOwnersValidationException(
                                "Error while searching for user by email:$approver",
                                e
                            )
                        }
                    } else {
                        // Strip the leading '@'
                        approver = approver.substring(1)
                    }

                    // Is this a Member?
                    if (allProjectMembers.containsKey(approver)) {
                        val member: Member = allProjectMembers[approver]!!
                        if (!usableAccount(member)) {
                            report(
                                userDisabled, results, definedSection, rule, rawApprover,
                                "Disabled account: " +
                                        "State=" + member.state + "; " +
                                        "Locked=" + member.locked
                            )
                            continue
                        }
                        if (canApprove(member)) {
                            // Success, we have found this valid approver.
                            if (showAllApprovers) {
                                val memberAccessLevel = member.accessLevel
                                report(
                                    Level.INFO, results, definedSection,
                                    rule, rawApprover,
                                    "Valid approver: Member with username \"" + approver + "\" can approve (AccessLevel:" + memberAccessLevel.toValue() + "=" + memberAccessLevel.name + ")"
                                )
                            }
                            ruleHasValidApprovers = true
                        } else {
                            report(
                                userTooLowPermissions, results, definedSection,
                                rule, rawApprover,
                                "User does not have sufficient permissions to approve: AccessLevel=" + member.accessLevel.name
                            )
                        }
                        continue
                    }

                    // Is this a Shared Group?
                    if (sharedGroups.containsKey(approver)) {
                        val sharedGroup: SharedGroup = sharedGroups[approver]!!
                        val groupAccessLevel = sharedGroup.groupAccessLevel
                        if (accessLevelCanApprove(groupAccessLevel)) {
                            // Success, we have found this valid approver to be the name of a shared group.
                            if (showAllApprovers) {
                                report(
                                    Level.INFO, results, definedSection,
                                    rule, rawApprover,
                                    "Valid approver: Members of group \"" + approver + "\" can approve (AccessLevel:" + groupAccessLevel.toValue() + "=" + groupAccessLevel.name + ")"
                                )
                            }
                            ruleHasValidApprovers = true
                            continue
                        }
                        report(
                            groupTooLowPermissions, results, definedSection,
                            rule, rawApprover,
                            "Shared group does not have sufficient permissions to approve: AccessLevel=" + groupAccessLevel.toValue() + " (=" + groupAccessLevel.name + ")"
                        )
                        continue
                    }

                    // Is this a user that exists but has not been made part of this project?
                    var user = userCache[approver]
                    if (user == null && !userCache.containsKey(approver)) {
                        try {
                            user = gitLabApi.getUserApi().getUser(approver)
                        } catch (_: GitLabApiException) {
                            // If error then group == null
                        }
                        userCache[approver] = user
                    }
                    if (user != null) {
                        report(
                            userNotProjectMember,
                            results,
                            definedSection,
                            rule,
                            rawApprover,
                            "User exists but is not a member of with this project: ${user.name}"
                        )
                        continue
                    }

                    // Is this a group that exists but has not been made part of this project?
                    var group = groupCache[approver]
                    if (group == null && !groupCache.containsKey(approver)) {
                        try {
                            group = getGroup(approver)
                        } catch (_: GitLabApiException) {
                            // If error then group == null
                        }
                        groupCache[approver] = group
                    }
                    if (group != null) {
                        report(
                            groupNotProjectMember,
                            results,
                            definedSection,
                            rule,
                            rawApprover,
                            "Group exists and is not shared with this project"
                        )
                        continue
                    }

                    // Ok, it doesn't exist.
                    report(
                        approverDoesNotExist,
                        results,
                        definedSection,
                        rule,
                        rawApprover,
                        "Approver does not exist in Gitlab (not as user and not as group)"
                    )
                }

                if (!ruleHasValidApprovers) {
                    report(noValidApprovers, results, definedSection, rule, "", "NO Valid Approvers for rule")
                }
            }
        }
        return results
    }

    @Throws(CodeOwnersValidationException::class)
    fun failIfExceededFailLevel(problemTable: ProblemTable) {
        if (problemTable.isEmpty) {
            return
        }
        if (
            when (failLevel) {
                FailLevel.NEVER   -> false
                FailLevel.FATAL   -> problemTable.hasFatalErrors()
                FailLevel.ERROR   -> problemTable.hasFatalErrors() || problemTable.hasErrors()
                FailLevel.WARNING -> problemTable.hasFatalErrors() || problemTable.hasErrors() || problemTable.hasWarnings()
            }
        ) {
            throw CodeOwnersValidationException(
                "Found " +
                "${problemTable.numberOfWarnings} warnings, " +
                "${problemTable.numberOfErrors} errors and " +
                "${problemTable.numberOfFatalErrors} fatal problems " +
                "of the CODEOWNERS file in relation to the Gitlab project.\n$problemTable"
            )
        }
    }

    companion object {
        // Same pattern as defined in the codeowners parser
        private val EMAIL_ADDRESS = Regex("^[a-zA-Z0-9_-]+@[a-zA-Z0-9._-]+$")
    }
}
