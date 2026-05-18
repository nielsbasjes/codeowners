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

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import nl.basjes.codeowners.CodeOwners
import nl.basjes.codeowners.validator.CodeOwnersValidationException
import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration.AccessToken
import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration.ProjectId
import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration.ServerUrl
import nl.basjes.codeowners.validator.utils.Problem
import nl.basjes.codeowners.validator.utils.ProblemTable
import org.junit.jupiter.api.TestInfo
import org.junitpioneer.jupiter.SetEnvironmentVariable
import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import java.util.function.Function
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@WireMockTest
class TestGitlabUsers {
    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    @Throws(CodeOwnersValidationException::class)
    fun testValidCodeOwners(wmRuntimeInfo: WireMockRuntimeInfo?) {
        val configuration: GitlabConfiguration = makeConfig(wmRuntimeInfo, null, "FETCH_USER_ACCESS_TOKEN")

        GitlabProjectMembers(configuration).use { gitlabProjectMembers ->
            val codeOwners = CodeOwners(
                """
                [README Owners]
                README.md                    @niels @SomeUser @isguest
                README_owner.md              @@owner
                README_owners.md             @@owners
                README_maintainer.md         @@maintainer
                README_maintainers.md        @@maintainers
                README_developer.md          @@developer
                README_developers.md         @@developers
                README_group_dev.md          @codeowners/developers
                README_group_main.md         @codeowners/maintainers
                README_user_public_email.md  public@example.nl
                """.trimIndent()
            )
            val logger = LoggerFactory.getLogger("testValidCodeOwners")
            gitlabProjectMembers.showAllApprovers = true
            logger.info(gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners).toString())
            logger.info(gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners).toProblemMessageGroupedString())
            gitlabProjectMembers.showAllApprovers = false
            logger.info(gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners).toString())
            logger.info(gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners).toProblemMessageGroupedString())
        }
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    @Throws(CodeOwnersValidationException::class)
    fun testRoles(wmRuntimeInfo: WireMockRuntimeInfo?) {
        val configuration: GitlabConfiguration = makeConfig(wmRuntimeInfo, null, "FETCH_USER_ACCESS_TOKEN")

        GitlabProjectMembers(configuration).use { gitlabProjectMembers ->
            val codeOwners = CodeOwners(
                """
                [README Owners]
                README_niels.md       @niels
                README_owner.md       @@owner
                README_owners.md      @@owners
                README_maintainer.md  @@maintainer
                README_maintainers.md @@maintainers
                README_developer.md   @@developer
                README_developers.md  @@developers
                """.trimIndent()
            )
            val logger = LoggerFactory.getLogger("testRoles")
            gitlabProjectMembers.showAllApprovers = true
            gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners)
            gitlabProjectMembers.showAllApprovers = false
            gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners)
        }
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    @Throws(CodeOwnersValidationException::class)
    fun testEmpty(wmRuntimeInfo: WireMockRuntimeInfo?, testInfo: TestInfo) {
        val configuration: GitlabConfiguration = makeConfig(wmRuntimeInfo, null, "FETCH_USER_ACCESS_TOKEN")
        configuration.failLevel = GitlabConfiguration.FailLevel.WARNING

        val exception = runCodeownersValidation(
            testInfo, configuration,
            CodeOwners(
                """
                [README Owners]
                """.trimIndent()
            )
        )
        if (exception != null) {
            throw exception
        }
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    @Throws(CodeOwnersValidationException::class)
    fun testSharedGroups(wmRuntimeInfo: WireMockRuntimeInfo?, testInfo: TestInfo) {
        val configuration: GitlabConfiguration = makeConfig(wmRuntimeInfo, null, "FETCH_USER_ACCESS_TOKEN")
        configuration.failLevel = GitlabConfiguration.FailLevel.FATAL

        val exception = runCodeownersValidation(
            testInfo, configuration,
            CodeOwners(
                """
                [README Owners]
                README_shared_group_guests.md      @codeowners/guests
                README_shared_group_developers.md  @codeowners/developers
                README_shared_group_maintainers.md @codeowners/maintainers
                """.trimIndent()
            ),
            Problem.Warning(
                "README Owners",
                "README_shared_group_guests.md",
                "@codeowners/guests",
                "Shared group does not have sufficient permissions to approve: AccessLevel=10 (=GUEST)"
            ),
            Problem.Error("README Owners", "README_shared_group_guests.md", "",                                                                     "NO Valid Approvers for rule")
        )
        if (exception != null) {
            throw exception
        }
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    @Throws(CodeOwnersValidationException::class)
    fun testUnknownEmailAssumptions(wmRuntimeInfo: WireMockRuntimeInfo?, testInfo: TestInfo) {
        val configuration: GitlabConfiguration = makeConfig(wmRuntimeInfo, null, "FETCH_USER_ACCESS_TOKEN")

        val codeOwners = CodeOwners(
            """
            [README Owners]
            README_niels_private_email.md private@example.nl
            """.trimIndent()
        )

        configuration.assumeUncheckableEmailExistsAndCanApprove = true
        assertNull(
            runCodeownersValidation(
                testInfo, configuration, codeOwners,
                Problem.Warning(
                    "README Owners",
                    "README_niels_private_email.md",
                    "private@example.nl",
                    "Unable to verify email address: Assuming the user exists and can approve"
                )
            )
        )

        configuration.assumeUncheckableEmailExistsAndCanApprove = false
        configuration.problemLevels.userUnknownEmail = GitlabConfiguration.Level.ERROR
        configuration.problemLevels.noValidApprovers = GitlabConfiguration.Level.FATAL
        assertNotNull(
            runCodeownersValidation(
                testInfo, configuration, codeOwners,
                Problem.Error(
                    "README Owners",
                    "README_niels_private_email.md",
                    "private@example.nl",
                    "Unable to verify email address: Assuming the user does not exist and/or cannot approve"
                ),
                Problem.Fatal("README Owners", "README_niels_private_email.md", "",                                                                     "NO Valid Approvers for rule")
            )
        )
    }


    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "nomembers")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    @Throws(CodeOwnersValidationException::class)
    fun testBadRoles(wmRuntimeInfo: WireMockRuntimeInfo?, testInfo: TestInfo) {
        val configuration: GitlabConfiguration = makeConfig(wmRuntimeInfo, null, "FETCH_USER_ACCESS_TOKEN")

        val exception = runCodeownersValidation(
            testInfo, configuration,
            CodeOwners(
                """
                [README Owners]
                README_1.md                      @niels @dummy
                README_2.md                      @niels @dummy
                README_owner.md                  @@owner
                README_owners.md                 @@owners
                README_maintainer.md             @@maintainer
                README_maintainers.md            @@maintainers
                README_developer.md              @@developer
                README_developers.md             @@developers
                README_nosuchrole.md             @@nosuchrole
                README_disabled_1.md             @disabledowner @disabledmaintainer @disableddeveloper
                README_locked_1.md               @lockedowner   @lockedmaintainer   @lockeddeveloper
                README_disabled_2.md             @disabledowner @disabledmaintainer @disableddeveloper
                README_locked_2.md               @lockedowner   @lockedmaintainer   @lockeddeveloper
                README_dummy_1.md                @dummy
                README_dummy_2.md                @dummy
                README_dummy_3.md                @dummy
                README_nonsharedgroup_1.md       @opensource
                README_nonsharedgroup_2.md       @opensource
                README_nonsharedgroup_3.md       @opensource
                README_sharedgroup_guests_1.md   @codeowners/guests
                README_sharedgroup_guests_2.md   @codeowners/guests
                README_sharedgroup_guests_3.md   @codeowners/guests
                README_nosuchgroup_1.md          @codeowners/nosuchgroup
                README_nosuchgroup_2.md          @codeowners/nosuchgroup
                README_nosuchgroup_3.md          @codeowners/nosuchgroup
                """.trimIndent()
            ),

            Problem.Error(   "README Owners",   "README_1.md",                     "@niels",                    "User exists but is not a member of with this project: Niels Basjes"                        ),
            Problem.Error(   "README Owners",   "README_1.md",                     "@dummy",                    "User exists but is not a member of with this project: Dummy User"                          ),
            Problem.Error(   "README Owners",   "README_1.md",                     "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Error(   "README Owners",   "README_2.md",                     "@niels",                    "User exists but is not a member of with this project: Niels Basjes"                        ),
            Problem.Error(   "README Owners",   "README_2.md",                     "@dummy",                    "User exists but is not a member of with this project: Dummy User"                          ),
            Problem.Error(   "README Owners",   "README_2.md",                     "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Warning( "README Owners",   "README_owner.md",                 "@@owner",                   "No direct project members have the \"owner\" role"                                         ),
            Problem.Error(   "README Owners",   "README_owner.md",                 "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Warning( "README Owners",   "README_owners.md",                "@@owners",                  "No direct project members have the \"owner\" role"                                         ),
            Problem.Error(   "README Owners",   "README_owners.md",                "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Warning( "README Owners",   "README_maintainer.md",            "@@maintainer",              "No direct project members have the \"maintainer\" role"                                    ),
            Problem.Error(   "README Owners",   "README_maintainer.md",            "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Warning( "README Owners",   "README_maintainers.md",           "@@maintainers",             "No direct project members have the \"maintainer\" role"                                    ),
            Problem.Error(   "README Owners",   "README_maintainers.md",           "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Warning( "README Owners",   "README_developer.md",             "@@developer",               "No direct project members have the \"developer\" role"                                     ),
            Problem.Error(   "README Owners",   "README_developer.md",             "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Warning( "README Owners",   "README_developers.md",            "@@developers",              "No direct project members have the \"developer\" role"                                     ),
            Problem.Error(   "README Owners",   "README_developers.md",            "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Fatal(   "README Owners",   "README_nosuchrole.md",            "@@nosuchrole",              "Illegal role was specified"                                                                ),
            Problem.Error(   "README Owners",   "README_nosuchrole.md",            "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Warning( "README Owners",   "README_disabled_1.md",            "@disabledowner",            "Disabled account: State=disabled; Locked=false"                                            ),
            Problem.Warning( "README Owners",   "README_disabled_1.md",            "@disabledmaintainer",       "Disabled account: State=disabled; Locked=false"                                            ),
            Problem.Warning( "README Owners",   "README_disabled_1.md",            "@disableddeveloper",        "Disabled account: State=disabled; Locked=false"                                            ),
            Problem.Error(   "README Owners",   "README_disabled_1.md",            "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Warning( "README Owners",   "README_locked_1.md",              "@lockedowner",              "Disabled account: State=active; Locked=true"                                               ),
            Problem.Warning( "README Owners",   "README_locked_1.md",              "@lockedmaintainer",         "Disabled account: State=active; Locked=true"                                               ),
            Problem.Warning( "README Owners",   "README_locked_1.md",              "@lockeddeveloper",          "Disabled account: State=active; Locked=true"                                               ),
            Problem.Error(   "README Owners",   "README_locked_1.md",              "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Warning( "README Owners",   "README_disabled_2.md",            "@disabledowner",            "Disabled account: State=disabled; Locked=false"                                            ),
            Problem.Warning( "README Owners",   "README_disabled_2.md",            "@disabledmaintainer",       "Disabled account: State=disabled; Locked=false"                                            ),
            Problem.Warning( "README Owners",   "README_disabled_2.md",            "@disableddeveloper",        "Disabled account: State=disabled; Locked=false"                                            ),
            Problem.Error(   "README Owners",   "README_disabled_2.md",            "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Warning( "README Owners",   "README_locked_2.md",              "@lockedowner",              "Disabled account: State=active; Locked=true"                                               ),
            Problem.Warning( "README Owners",   "README_locked_2.md",              "@lockedmaintainer",         "Disabled account: State=active; Locked=true"                                               ),
            Problem.Warning( "README Owners",   "README_locked_2.md",              "@lockeddeveloper",          "Disabled account: State=active; Locked=true"                                               ),
            Problem.Error(   "README Owners",   "README_locked_2.md",              "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Error(   "README Owners",   "README_dummy_1.md",               "@dummy",                    "User exists but is not a member of with this project: Dummy User"                          ),
            Problem.Error(   "README Owners",   "README_dummy_1.md",               "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Error(   "README Owners",   "README_dummy_2.md",               "@dummy",                    "User exists but is not a member of with this project: Dummy User"                          ),
            Problem.Error(   "README Owners",   "README_dummy_2.md",               "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Error(   "README Owners",   "README_dummy_3.md",               "@dummy",                    "User exists but is not a member of with this project: Dummy User"                          ),
            Problem.Error(   "README Owners",   "README_dummy_3.md",               "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Error(   "README Owners",   "README_nonsharedgroup_1.md",      "@opensource",               "Group exists and is not shared with this project"                                          ),
            Problem.Error(   "README Owners",   "README_nonsharedgroup_1.md",      "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Error(   "README Owners",   "README_nonsharedgroup_2.md",      "@opensource",               "Group exists and is not shared with this project"                                          ),
            Problem.Error(   "README Owners",   "README_nonsharedgroup_2.md",      "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Error(   "README Owners",   "README_nonsharedgroup_3.md",      "@opensource",               "Group exists and is not shared with this project"                                          ),
            Problem.Error(   "README Owners",   "README_nonsharedgroup_3.md",      "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Warning( "README Owners",   "README_sharedgroup_guests_1.md",  "@codeowners/guests",        "Shared group does not have sufficient permissions to approve: AccessLevel=10 (=GUEST)"     ),
            Problem.Error(   "README Owners",   "README_sharedgroup_guests_1.md",  "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Warning( "README Owners",   "README_sharedgroup_guests_2.md",  "@codeowners/guests",        "Shared group does not have sufficient permissions to approve: AccessLevel=10 (=GUEST)"     ),
            Problem.Error(   "README Owners",   "README_sharedgroup_guests_2.md",  "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Warning( "README Owners",   "README_sharedgroup_guests_3.md",  "@codeowners/guests",        "Shared group does not have sufficient permissions to approve: AccessLevel=10 (=GUEST)"     ),
            Problem.Error(   "README Owners",   "README_sharedgroup_guests_3.md",  "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Warning( "README Owners",   "README_nosuchgroup_1.md",         "@codeowners/nosuchgroup",   "Approver does not exist in Gitlab (not as user and not as group)"                          ),
            Problem.Error(   "README Owners",   "README_nosuchgroup_1.md",         "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Warning( "README Owners",   "README_nosuchgroup_2.md",         "@codeowners/nosuchgroup",   "Approver does not exist in Gitlab (not as user and not as group)"                          ),
            Problem.Error(   "README Owners",   "README_nosuchgroup_2.md",         "",                          "NO Valid Approvers for rule"                                                               ),
            Problem.Warning( "README Owners",   "README_nosuchgroup_3.md",         "@codeowners/nosuchgroup",   "Approver does not exist in Gitlab (not as user and not as group)"                          ),
            Problem.Error(   "README Owners",   "README_nosuchgroup_3.md",          "",                         "NO Valid Approvers for rule"                                                               ),
        )

        assertNotNull(
            exception,
            "There should be an exception because we have a Fatal Error and the FailLevel is ERROR"
        )
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    @Throws(CodeOwnersValidationException::class)
    fun testOwnerHandling(wmRuntimeInfo: WireMockRuntimeInfo?, testInfo: TestInfo) {
        val configuration: GitlabConfiguration = makeConfig(wmRuntimeInfo, null, "FETCH_USER_ACCESS_TOKEN")
        configuration.failLevel = GitlabConfiguration.FailLevel.FATAL

        val exception = runCodeownersValidation(
            testInfo, configuration,
            CodeOwners(
                """
                [README Owners]
                README_niels.md                @niels
                README_dummy.md                @dummy
                README_niels_public_email.md   public@example.nl
                README_niels_private_email.md  private@example.nl
                README_dummy_public_email.md   public@dummy.example.nl
                README_dummy_private_email.md  private@dummy.example.nl
                """.trimIndent()
            ),

            Problem.Info(    "README Owners",   "README_niels.md",                  "@niels",                       "Valid approver: Member with username \"niels\" can approve (AccessLevel:50=OWNER)" ),
            Problem.Error(   "README Owners",   "README_dummy.md",                  "@dummy",                       "User exists but is not a member of with this project: Dummy User"                  ),
            Problem.Error(   "README Owners",   "README_dummy.md",                  "",                             "NO Valid Approvers for rule"                                                       ),
            Problem.Info(    "README Owners",   "README_niels_public_email.md",     "public@example.nl",            "Valid approver: Member with username \"niels\" can approve (AccessLevel:50=OWNER)" ),
            Problem.Warning( "README Owners",   "README_niels_private_email.md",    "private@example.nl",           "Unable to verify email address: Assuming the user exists and can approve"          ),
            Problem.Error(   "README Owners",   "README_dummy_public_email.md",     "public@dummy.example.nl",      "User exists but is not a member of with this project: Dummy User"                  ),
            Problem.Error(   "README Owners",   "README_dummy_public_email.md",     "",                             "NO Valid Approvers for rule"                                                       ),
            Problem.Warning( "README Owners",   "README_dummy_private_email.md",    "private@dummy.example.nl",     "Unable to verify email address: Assuming the user exists and can approve"          ),
        )
        if (exception != null) {
            throw exception
        }
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    @Throws(CodeOwnersValidationException::class)
    fun testShowMixedErrorReport(wmRuntimeInfo: WireMockRuntimeInfo, testInfo: TestInfo) {
        val configuration = GitlabConfiguration(
            ServerUrl(wmRuntimeInfo.httpBaseUrl, null),
            ProjectId(null, null),
            AccessToken("FETCH_USER_ACCESS_TOKEN")
        )
        configuration.showAllApprovers = true
        val exception = runCodeownersValidation(
            testInfo, configuration,
            true,
            CodeOwners(
                """
                [README Owners]
                # INFO:  niels --> is member
                README_niels.md @niels
                # WARNING: niels --> is member --> Cannot find --> Warning only
                README_niels_private_email.md private@example.nl private@dummy.example.nl
                # ERROR: dummy --> is NOT member --> Error
                README_dummy.md @dummy
                # ERROR: dummy --> is NOT member --> Error
                README_badrole.md @@badrole
                """.trimIndent()
            ),
            Problem.Info(       "README Owners",    "README_niels.md",               "@niels",                      "Valid approver: Member with username \"niels\" can approve (AccessLevel:50=OWNER)"     ),
            Problem.Warning(    "README Owners",    "README_niels_private_email.md", "private@example.nl",          "Unable to verify email address: Assuming the user exists and can approve"              ),
            Problem.Warning(    "README Owners",    "README_niels_private_email.md", "private@dummy.example.nl",    "Unable to verify email address: Assuming the user exists and can approve"              ),
            Problem.Error(      "README Owners",    "README_dummy.md",               "@dummy",                      "User exists but is not a member of with this project: Dummy User"                      ),
            Problem.Error(      "README Owners",    "README_dummy.md",               "",                            "NO Valid Approvers for rule"                                                           ),
            Problem.Fatal(      "README Owners",    "README_badrole.md",             "@@badrole",                   "Illegal role was specified"                                                            ),
        )
        assertNotNull(exception)
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    @Throws(CodeOwnersValidationException::class)
    fun testFailLevelNever(wmRuntimeInfo: WireMockRuntimeInfo, testInfo: TestInfo) {
        val configuration = GitlabConfiguration(
            ServerUrl(wmRuntimeInfo.httpBaseUrl, null),
            ProjectId(null, null),
            AccessToken("FETCH_USER_ACCESS_TOKEN")
        )
        runFailLevelTests(testInfo, configuration, GitlabConfiguration.FailLevel.NEVER)
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    @Throws(CodeOwnersValidationException::class)
    fun testFailLevelFatal(wmRuntimeInfo: WireMockRuntimeInfo, testInfo: TestInfo) {
        val configuration = GitlabConfiguration(
            ServerUrl(wmRuntimeInfo.httpBaseUrl, null),
            ProjectId(null, null),
            AccessToken("FETCH_USER_ACCESS_TOKEN")
        )
        runFailLevelTests(testInfo, configuration, GitlabConfiguration.FailLevel.FATAL)
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    @Throws(CodeOwnersValidationException::class)
    fun testFailLevelError(wmRuntimeInfo: WireMockRuntimeInfo, testInfo: TestInfo) {
        val configuration = GitlabConfiguration(
            ServerUrl(wmRuntimeInfo.httpBaseUrl, null),
            ProjectId(null, null),
            AccessToken("FETCH_USER_ACCESS_TOKEN")
        )
        runFailLevelTests(testInfo, configuration, GitlabConfiguration.FailLevel.ERROR)
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    @Throws(CodeOwnersValidationException::class)
    fun testFailLevelWarning(wmRuntimeInfo: WireMockRuntimeInfo, testInfo: TestInfo) {
        val configuration = GitlabConfiguration(
            ServerUrl(wmRuntimeInfo.httpBaseUrl, null),
            ProjectId(null, null),
            AccessToken("FETCH_USER_ACCESS_TOKEN")
        )
        configuration.failLevel = GitlabConfiguration.FailLevel.WARNING

        assertNull(    runFailLevelTestInfo(testInfo, configuration),     "No exception should have been thrown")
        assertNotNull( runFailLevelTestWarning(testInfo, configuration),  "An exception should have been thrown")
        assertNotNull( runFailLevelTestError(testInfo, configuration),    "An exception should have been thrown")
        assertNotNull( runFailLevelTestFatal(testInfo, configuration),    "An exception should have been thrown")
    }

    @Throws(CodeOwnersValidationException::class)
    private fun runFailLevelTests(
        testInfo: TestInfo,
        configuration: GitlabConfiguration,
        failLevel: GitlabConfiguration.FailLevel
    ) {
        val infoShouldFail = false
        var warningShouldFail = false
        var errorShouldFail = false
        var fatalShouldFail = false
        when (failLevel) {
            GitlabConfiguration.FailLevel.NEVER -> {}
            GitlabConfiguration.FailLevel.FATAL -> fatalShouldFail = true
            GitlabConfiguration.FailLevel.ERROR -> {
                errorShouldFail = true
                fatalShouldFail = true
            }

            GitlabConfiguration.FailLevel.WARNING -> {
                warningShouldFail = true
                errorShouldFail = true
                fatalShouldFail = true
            }
        }
        configuration.failLevel = failLevel
        var exception: CodeOwnersValidationException?
        exception = runFailLevelTestInfo(testInfo, configuration)
        assertFail(infoShouldFail, exception)
        exception = runFailLevelTestWarning(testInfo, configuration)
        assertFail(warningShouldFail, exception)
        exception = runFailLevelTestError(testInfo, configuration)
        assertFail(errorShouldFail, exception)
        exception = runFailLevelTestFatal(testInfo, configuration)
        assertFail(fatalShouldFail, exception)
    }

    private fun assertFail(shouldFail: Boolean, exception: CodeOwnersValidationException?) {
        if (shouldFail) {
            assertNotNull(exception, "An exception should have been thrown")
        } else {
            assertNull(exception, "No exception should have been thrown")
        }
    }

    private fun runFailLevelTestFatal(
        testInfo: TestInfo,
        configuration: GitlabConfiguration
    ): CodeOwnersValidationException? {
        return runCodeownersValidation(
            testInfo, configuration,
            CodeOwners(
                """
                [README Owners]
                README_niels.md @niels
                README_niels_private_email.md private@example.nl
                README_dummy.md @dummy
                README_badrole.md @@badrole
                """.trimIndent()
            ),
            Problem.Info(       "README Owners",    "README_niels.md",                  "@niels",               "Valid approver: Member with username \"niels\" can approve (AccessLevel:50=OWNER)" ),
            Problem.Warning(    "README Owners",    "README_niels_private_email.md",    "private@example.nl",   "Unable to verify email address: Assuming the user exists and can approve"          ),
            Problem.Error(      "README Owners",    "README_dummy.md",                  "@dummy",               "User exists but is not a member of with this project: Dummy User"                  ),
            Problem.Error(      "README Owners",    "README_dummy.md",                  "",                     "NO Valid Approvers for rule"                                                       ),
            Problem.Fatal(      "README Owners",    "README_badrole.md",                "@@badrole",            "Illegal role was specified"                                                        ),
        )
    }

    private fun runFailLevelTestError(
        testInfo: TestInfo,
        configuration: GitlabConfiguration
    ): CodeOwnersValidationException? {
        return runCodeownersValidation(
            testInfo, configuration,
            CodeOwners(
                """
                [README Owners]
                README_niels.md @niels
                README_niels_private_email.md private@example.nl
                README_dummy.md @dummy
                """.trimIndent()
            ),
            Problem.Info(       "README Owners",    "README_niels.md",                  "@niels",               "Valid approver: Member with username \"niels\" can approve (AccessLevel:50=OWNER)" ),
            Problem.Warning(    "README Owners",    "README_niels_private_email.md",    "private@example.nl",   "Unable to verify email address: Assuming the user exists and can approve"          ),
            Problem.Error(      "README Owners",    "README_dummy.md",                  "@dummy",               "User exists but is not a member of with this project: Dummy User"                  ),
            Problem.Error(      "README Owners",    "README_dummy.md",                  "",                     "NO Valid Approvers for rule"                                                       ),
        )
    }

    private fun runFailLevelTestWarning(
        testInfo: TestInfo,
        configuration: GitlabConfiguration
    ): CodeOwnersValidationException? {
        return runCodeownersValidation(
            testInfo, configuration,
            CodeOwners(
                """
                [README Owners]
                README_niels.md @niels
                README_niels_private_email.md private@example.nl
                """.trimIndent()
            ),
            Problem.Warning("README Owners", "README_niels_private_email.md", "private@example.nl", "Unable to verify email address: Assuming the user exists and can approve"),
        )
    }

    private fun runFailLevelTestInfo(
        testInfo: TestInfo,
        configuration: GitlabConfiguration
    ): CodeOwnersValidationException? {
        return runCodeownersValidation(
            testInfo, configuration,
            CodeOwners(
                """
                [README Owners]
                README_niels.md @niels
                """.trimIndent()
            ),
            Problem.Info("README Owners", "README_niels.md", "@niels", "Valid approver: Member with username \"niels\" can approve (AccessLevel:50=OWNER)")
        )
    }

    private fun runCodeownersValidation(
        testInfo: TestInfo,
        configuration: GitlabConfiguration,
        codeOwners: CodeOwners,
        vararg expectedProblems: Problem?
    ): CodeOwnersValidationException? {
        return runCodeownersValidation(testInfo, configuration, false, codeOwners, *expectedProblems)
    }

    private fun runCodeownersValidation(
        testInfo: TestInfo,
        configuration: GitlabConfiguration,
        logResult: Boolean,
        codeOwners: CodeOwners,
        vararg expectedProblems: Problem?
    ): CodeOwnersValidationException? {
        GitlabProjectMembers(configuration).use { gitlabProjectMembers ->
            val logger = LoggerFactory.getLogger(
                testInfo.testMethod.map(Function { obj: Method? -> obj!!.name })
                    .orElse("runCodeownersValidation")
            )
            gitlabProjectMembers.showAllApprovers = true
            var exception: CodeOwnersValidationException? = null
            var problemTable: ProblemTable? = null
            try {
                problemTable = gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners)
                gitlabProjectMembers.failIfExceededFailLevel(problemTable)
            } catch (e: CodeOwnersValidationException) {
                exception = e
            }

            assertNotNull(problemTable)
            for (expectedProblem in expectedProblems) {
                assertTrue(
                    problemTable.contains(expectedProblem),
                    "The problem table \n" +
                    problemTable.problems.joinToString(separator = "\n") { it.toString() } +
                    "\n should contain \n" + expectedProblem)
            }
            if (logResult) {
                logger.info(problemTable.toString())
                logger.info(problemTable.toProblemMessageGroupedString())
            }
            return exception
        }
    }

    companion object {
        fun makeConfig(
            wmRuntimeInfo: WireMockRuntimeInfo?,
            projectId: String?,
            tokenEnvVariableName: String?
        ): GitlabConfiguration {
            return GitlabConfiguration(
                ServerUrl(wmRuntimeInfo?.httpBaseUrl),
                ProjectId(projectId),
                AccessToken(tokenEnvVariableName)
            )
        }
    }
}
