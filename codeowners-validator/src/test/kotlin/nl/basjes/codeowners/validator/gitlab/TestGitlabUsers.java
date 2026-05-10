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
package nl.basjes.codeowners.validator.gitlab;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.codeowners.CodeOwners;
import nl.basjes.codeowners.validator.CodeOwnersValidationException;
import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration.AccessToken;
import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration.FailLevel;
import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration.ProjectId;
import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration.ServerUrl;
import nl.basjes.codeowners.validator.utils.Problem;
import nl.basjes.codeowners.validator.utils.ProblemTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.stream.Collectors;

import static nl.basjes.codeowners.validator.gitlab.GitlabConfiguration.FailLevel.ERROR;
import static nl.basjes.codeowners.validator.gitlab.GitlabConfiguration.FailLevel.FATAL;
import static nl.basjes.codeowners.validator.gitlab.GitlabConfiguration.FailLevel.NEVER;
import static nl.basjes.codeowners.validator.gitlab.GitlabConfiguration.FailLevel.WARNING;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@WireMockTest
public class TestGitlabUsers {

    static GitlabConfiguration makeConfig(WireMockRuntimeInfo wmRuntimeInfo, String projectId, String tokenEnvVariableName) {
        return new GitlabConfiguration(
            new ServerUrl(wmRuntimeInfo==null ? null : wmRuntimeInfo.getHttpBaseUrl(), null),
            new ProjectId(projectId, null),
            new AccessToken(tokenEnvVariableName)
        );
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testValidCodeOwners(WireMockRuntimeInfo wmRuntimeInfo) throws CodeOwnersValidationException {
        GitlabConfiguration configuration = makeConfig(wmRuntimeInfo, null, "FETCH_USER_ACCESS_TOKEN");

        try(GitlabProjectMembers gitlabProjectMembers = new GitlabProjectMembers(configuration)) {
            CodeOwners codeOwners = new CodeOwners(
                "[README Owners]\n" +
                "README.md                    @niels @SomeUser @isguest\n" +
                "README_owner.md              @@owner\n" +
                "README_owners.md             @@owners\n" +
                "README_maintainer.md         @@maintainer\n" +
                "README_maintainers.md        @@maintainers\n" +
                "README_developer.md          @@developer\n" +
                "README_developers.md         @@developers\n" +
                "README_group_dev.md          @codeowners/developers\n" +
                "README_group_main.md         @codeowners/maintainers\n" +
                "README_user_public_email.md  public@example.nl\n"
            );

            Logger logger =  LoggerFactory.getLogger("testValidCodeOwners");
            gitlabProjectMembers.setShowAllApprovers(true);
            logger.info(gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners).toString());
            logger.info(gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners).toProblemMessageGroupedString());
            gitlabProjectMembers.setShowAllApprovers(false);
            logger.info(gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners).toString());
            logger.info(gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners).toProblemMessageGroupedString());
        }
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testRoles(WireMockRuntimeInfo wmRuntimeInfo) throws CodeOwnersValidationException {
        GitlabConfiguration configuration = makeConfig(wmRuntimeInfo, null, "FETCH_USER_ACCESS_TOKEN");

        try(GitlabProjectMembers gitlabProjectMembers = new GitlabProjectMembers(configuration)) {
            CodeOwners codeOwners = new CodeOwners(
                "[README Owners]\n" +
                "README_niels.md       @niels\n" +
                "README_owner.md       @@owner\n" +
                "README_owners.md      @@owners\n" +
                "README_maintainer.md  @@maintainer\n" +
                "README_maintainers.md @@maintainers\n" +
                "README_developer.md   @@developer\n" +
                "README_developers.md  @@developers\n"
            );

            Logger logger =  LoggerFactory.getLogger("testRoles");
            gitlabProjectMembers.setShowAllApprovers(true);
            gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners);
            gitlabProjectMembers.setShowAllApprovers(false);
            gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners);
        }
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testSharedGroups(WireMockRuntimeInfo wmRuntimeInfo, TestInfo testInfo) throws CodeOwnersValidationException {
        GitlabConfiguration configuration = makeConfig(wmRuntimeInfo, null, "FETCH_USER_ACCESS_TOKEN");
        configuration.setFailLevel(FATAL);

        CodeOwnersValidationException exception = runCodeownersValidation(testInfo, configuration,
            new CodeOwners(
                "[README Owners]\n" +
                "README_shared_group_guests.md      @codeowners/guests\n" +
                "README_shared_group_developers.md  @codeowners/developers\n" +
                "README_shared_group_maintainers.md @codeowners/maintainers\n"
            ),
            new Problem.Warning("README Owners", "README_shared_group_guests.md", "@codeowners/guests", "Shared group does not have sufficient permissions to approve: AccessLevel=10 (=GUEST)"),
            new Problem.Error  ("README Owners", "README_shared_group_guests.md", "",                   "NO Valid Approvers for rule")
        );
        if (exception != null) {
            throw exception;
        }
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testUnknownEmailAssumptions(WireMockRuntimeInfo wmRuntimeInfo, TestInfo testInfo) throws CodeOwnersValidationException {
        GitlabConfiguration configuration = makeConfig(wmRuntimeInfo, null, "FETCH_USER_ACCESS_TOKEN");

        CodeOwners codeOwners = new CodeOwners(
            "[README Owners]\n" +
            "README_niels_private_email.md private@example.nl\n" // WARNING: niels --> is member --> Cannot find --> Warning only
        );

        configuration.setAssumeUncheckableEmailExistsAndCanApprove(true);
        assertNull(runCodeownersValidation(testInfo, configuration, codeOwners,
            new Problem.Warning("README Owners", "README_niels_private_email.md", "private@example.nl", "Unable to verify email address: Assuming the user exists and can approve")
        ));

        configuration.setAssumeUncheckableEmailExistsAndCanApprove(false);
        configuration.getProblemLevels().setUserUnknownEmail(GitlabConfiguration.Level.ERROR);
        configuration.getProblemLevels().setNoValidApprovers(GitlabConfiguration.Level.FATAL);
        assertNotNull(runCodeownersValidation(testInfo, configuration, codeOwners,
            new Problem.Error("README Owners", "README_niels_private_email.md", "private@example.nl", "Unable to verify email address: Assuming the user does not exist and/or cannot approve"),
            new Problem.Fatal("README Owners", "README_niels_private_email.md", "",                   "NO Valid Approvers for rule")
        ));
    }


    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "nomembers")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testBadRoles(WireMockRuntimeInfo wmRuntimeInfo, TestInfo testInfo) throws CodeOwnersValidationException {
        GitlabConfiguration configuration = makeConfig(wmRuntimeInfo, null, "FETCH_USER_ACCESS_TOKEN");

        CodeOwnersValidationException exception = runCodeownersValidation(testInfo, configuration,
            new CodeOwners(
                "[README Owners]\n" +
                "README_1.md                      @niels @dummy\n" +
                "README_2.md                      @niels @dummy\n" +
                "README_owner.md                  @@owner\n" +
                "README_owners.md                 @@owners\n" +
                "README_maintainer.md             @@maintainer\n" +
                "README_maintainers.md            @@maintainers\n" +
                "README_developer.md              @@developer\n" +
                "README_developers.md             @@developers\n" +
                "README_nosuchrole.md             @@nosuchrole\n" +
                "README_disabled_1.md             @disabledowner @disabledmaintainer @disableddeveloper\n" +
                "README_locked_1.md               @lockedowner   @lockedmaintainer   @lockeddeveloper\n" +
                "README_disabled_2.md             @disabledowner @disabledmaintainer @disableddeveloper\n" +
                "README_locked_2.md               @lockedowner   @lockedmaintainer   @lockeddeveloper\n" +
                "README_dummy_1.md                @dummy\n" +
                "README_dummy_2.md                @dummy\n" +
                "README_dummy_3.md                @dummy\n" +
                "README_nonsharedgroup_1.md       @opensource \n" +
                "README_nonsharedgroup_2.md       @opensource \n" +
                "README_nonsharedgroup_3.md       @opensource \n" +
                "README_sharedgroup_guests_1.md   @codeowners/guests \n" +
                "README_sharedgroup_guests_2.md   @codeowners/guests \n" +
                "README_sharedgroup_guests_3.md   @codeowners/guests \n" +
                "README_nosuchgroup_1.md          @codeowners/nosuchgroup \n" +
                "README_nosuchgroup_2.md          @codeowners/nosuchgroup \n" +
                "README_nosuchgroup_3.md          @codeowners/nosuchgroup \n"
            ),

            new Problem.Error   ("README Owners", "README_1.md",                    "@niels",                   "User exists but is not a member of with this project: Niels Basjes"),
            new Problem.Error   ("README Owners", "README_1.md",                    "@dummy",                   "User exists but is not a member of with this project: Dummy User"),
            new Problem.Error   ("README Owners", "README_1.md",                    "",                         "NO Valid Approvers for rule"),
            new Problem.Error   ("README Owners", "README_2.md",                    "@niels",                   "User exists but is not a member of with this project: Niels Basjes"),
            new Problem.Error   ("README Owners", "README_2.md",                    "@dummy",                   "User exists but is not a member of with this project: Dummy User"),
            new Problem.Error   ("README Owners", "README_2.md",                    "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_owner.md",                "@@owner",                  "No direct project members have the \"owner\" role"),
            new Problem.Error   ("README Owners", "README_owner.md",                "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_owners.md",               "@@owners",                 "No direct project members have the \"owner\" role"),
            new Problem.Error   ("README Owners", "README_owners.md",               "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_maintainer.md",           "@@maintainer",             "No direct project members have the \"maintainer\" role"),
            new Problem.Error   ("README Owners", "README_maintainer.md",           "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_maintainers.md",          "@@maintainers",            "No direct project members have the \"maintainer\" role"),
            new Problem.Error   ("README Owners", "README_maintainers.md",          "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_developer.md",            "@@developer",              "No direct project members have the \"developer\" role"),
            new Problem.Error   ("README Owners", "README_developer.md",            "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_developers.md",           "@@developers",             "No direct project members have the \"developer\" role"),
            new Problem.Error   ("README Owners", "README_developers.md",           "",                         "NO Valid Approvers for rule"),
            new Problem.Fatal   ("README Owners", "README_nosuchrole.md",           "@@nosuchrole",             "Illegal role was specified"),
            new Problem.Error   ("README Owners", "README_nosuchrole.md",           "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_disabled_1.md",           "@disabledowner",           "Disabled account: State=disabled; Locked=false"),
            new Problem.Warning ("README Owners", "README_disabled_1.md",           "@disabledmaintainer",      "Disabled account: State=disabled; Locked=false"),
            new Problem.Warning ("README Owners", "README_disabled_1.md",           "@disableddeveloper",       "Disabled account: State=disabled; Locked=false"),
            new Problem.Error   ("README Owners", "README_disabled_1.md",           "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_locked_1.md",             "@lockedowner",             "Disabled account: State=active; Locked=true"),
            new Problem.Warning ("README Owners", "README_locked_1.md",             "@lockedmaintainer",        "Disabled account: State=active; Locked=true"),
            new Problem.Warning ("README Owners", "README_locked_1.md",             "@lockeddeveloper",         "Disabled account: State=active; Locked=true"),
            new Problem.Error   ("README Owners", "README_locked_1.md",             "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_disabled_2.md",           "@disabledowner",           "Disabled account: State=disabled; Locked=false"),
            new Problem.Warning ("README Owners", "README_disabled_2.md",           "@disabledmaintainer",      "Disabled account: State=disabled; Locked=false"),
            new Problem.Warning ("README Owners", "README_disabled_2.md",           "@disableddeveloper",       "Disabled account: State=disabled; Locked=false"),
            new Problem.Error   ("README Owners", "README_disabled_2.md",           "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_locked_2.md",             "@lockedowner",             "Disabled account: State=active; Locked=true"),
            new Problem.Warning ("README Owners", "README_locked_2.md",             "@lockedmaintainer",        "Disabled account: State=active; Locked=true"),
            new Problem.Warning ("README Owners", "README_locked_2.md",             "@lockeddeveloper",         "Disabled account: State=active; Locked=true"),
            new Problem.Error   ("README Owners", "README_locked_2.md",             "",                         "NO Valid Approvers for rule"),
            new Problem.Error   ("README Owners", "README_dummy_1.md",              "@dummy",                   "User exists but is not a member of with this project: Dummy User"),
            new Problem.Error   ("README Owners", "README_dummy_1.md",              "",                         "NO Valid Approvers for rule"),
            new Problem.Error   ("README Owners", "README_dummy_2.md",              "@dummy",                   "User exists but is not a member of with this project: Dummy User"),
            new Problem.Error   ("README Owners", "README_dummy_2.md",              "",                         "NO Valid Approvers for rule"),
            new Problem.Error   ("README Owners", "README_dummy_3.md",              "@dummy",                   "User exists but is not a member of with this project: Dummy User"),
            new Problem.Error   ("README Owners", "README_dummy_3.md",              "",                         "NO Valid Approvers for rule"),
            new Problem.Error   ("README Owners", "README_nonsharedgroup_1.md",     "@opensource",              "Group exists and is not shared with this project"),
            new Problem.Error   ("README Owners", "README_nonsharedgroup_1.md",     "",                         "NO Valid Approvers for rule"),
            new Problem.Error   ("README Owners", "README_nonsharedgroup_2.md",     "@opensource",              "Group exists and is not shared with this project"),
            new Problem.Error   ("README Owners", "README_nonsharedgroup_2.md",     "",                         "NO Valid Approvers for rule"),
            new Problem.Error   ("README Owners", "README_nonsharedgroup_3.md",     "@opensource",              "Group exists and is not shared with this project"),
            new Problem.Error   ("README Owners", "README_nonsharedgroup_3.md",     "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_sharedgroup_guests_1.md", "@codeowners/guests",       "Shared group does not have sufficient permissions to approve: AccessLevel=10 (=GUEST)"),
            new Problem.Error   ("README Owners", "README_sharedgroup_guests_1.md", "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_sharedgroup_guests_2.md", "@codeowners/guests",       "Shared group does not have sufficient permissions to approve: AccessLevel=10 (=GUEST)"),
            new Problem.Error   ("README Owners", "README_sharedgroup_guests_2.md", "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_sharedgroup_guests_3.md", "@codeowners/guests",       "Shared group does not have sufficient permissions to approve: AccessLevel=10 (=GUEST)"),
            new Problem.Error   ("README Owners", "README_sharedgroup_guests_3.md", "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_nosuchgroup_1.md",        "@codeowners/nosuchgroup",  "Approver does not exist in Gitlab (not as user and not as group)"),
            new Problem.Error   ("README Owners", "README_nosuchgroup_1.md",        "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_nosuchgroup_2.md",        "@codeowners/nosuchgroup",  "Approver does not exist in Gitlab (not as user and not as group)"),
            new Problem.Error   ("README Owners", "README_nosuchgroup_2.md",        "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_nosuchgroup_3.md",        "@codeowners/nosuchgroup",  "Approver does not exist in Gitlab (not as user and not as group)"),
            new Problem.Error   ("README Owners", "README_nosuchgroup_3.md",        "",                         "NO Valid Approvers for rule")
        );
        assertNotNull(exception, "There should be an exception because we have a Fatal Error and the FailLevel is ERROR");
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testOwnerHandling(WireMockRuntimeInfo wmRuntimeInfo, TestInfo testInfo) throws CodeOwnersValidationException {
        GitlabConfiguration configuration = makeConfig(wmRuntimeInfo, null, "FETCH_USER_ACCESS_TOKEN").setFailLevel(FATAL);

        CodeOwnersValidationException exception = runCodeownersValidation(testInfo, configuration,
            new CodeOwners(
                "[README Owners]\n" +
                "README_niels.md                @niels\n" + // niels --> is member
                "README_dummy.md                @dummy\n" + // dummy --> is NOT member --> Error
                "README_niels_public_email.md   public@example.nl\n" + // niels --> is member
                "README_niels_private_email.md  private@example.nl\n" + // niels --> is member --> Cannot find --> Warning only
                "README_dummy_public_email.md   public@dummy.example.nl\n" + // dummy --> is NOT member --> Error
                "README_dummy_private_email.md  private@dummy.example.nl\n"  // niels --> is NOT member --> Cannot find --> Warning only
            ),
            new Problem.Info    ("README Owners", "README_niels.md",               "@niels",                   "Valid approver: Member with username \"niels\" can approve (AccessLevel:50=OWNER)"),
            new Problem.Error   ("README Owners", "README_dummy.md",               "@dummy",                   "User exists but is not a member of with this project: Dummy User"),
            new Problem.Error   ("README Owners", "README_dummy.md",               "",                         "NO Valid Approvers for rule"),
            new Problem.Info    ("README Owners", "README_niels_public_email.md",  "public@example.nl",        "Valid approver: Member with username \"niels\" can approve (AccessLevel:50=OWNER)"),
            new Problem.Warning ("README Owners", "README_niels_private_email.md", "private@example.nl",       "Unable to verify email address: Assuming the user exists and can approve"),
            new Problem.Error   ("README Owners", "README_dummy_public_email.md",  "public@dummy.example.nl",  "User exists but is not a member of with this project: Dummy User"),
            new Problem.Error   ("README Owners", "README_dummy_public_email.md",  "",                         "NO Valid Approvers for rule"),
            new Problem.Warning ("README Owners", "README_dummy_private_email.md", "private@dummy.example.nl", "Unable to verify email address: Assuming the user exists and can approve")
        );
        if (exception != null) {
            throw exception;
        }
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testShowMixedErrorReport(WireMockRuntimeInfo wmRuntimeInfo, TestInfo testInfo) throws CodeOwnersValidationException {
        GitlabConfiguration configuration = new GitlabConfiguration(
            new ServerUrl(wmRuntimeInfo.getHttpBaseUrl(), null),
            new ProjectId(null, null),
            new AccessToken("FETCH_USER_ACCESS_TOKEN")
        )
            .setShowAllApprovers(true);
        CodeOwnersValidationException exception = runCodeownersValidation(testInfo, configuration,
            true,
            new CodeOwners(
                "[README Owners]\n" +
                "README_niels.md @niels\n" + // INFO:  niels --> is member
                "README_niels_private_email.md private@example.nl private@dummy.example.nl\n" + // WARNING: niels --> is member --> Cannot find --> Warning only
                "README_dummy.md @dummy\n" + // ERROR: dummy --> is NOT member --> Error
                "README_badrole.md @@badrole\n" // ERROR: dummy --> is NOT member --> Error
            ),
            new Problem.Info    ("README Owners", "README_niels.md",               "@niels",                   "Valid approver: Member with username \"niels\" can approve (AccessLevel:50=OWNER)"),
            new Problem.Warning ("README Owners", "README_niels_private_email.md", "private@example.nl",       "Unable to verify email address: Assuming the user exists and can approve"),
            new Problem.Warning ("README Owners", "README_niels_private_email.md", "private@dummy.example.nl", "Unable to verify email address: Assuming the user exists and can approve"),
            new Problem.Error   ("README Owners", "README_dummy.md",               "@dummy",                   "User exists but is not a member of with this project: Dummy User"),
            new Problem.Error   ("README Owners", "README_dummy.md",               "",                         "NO Valid Approvers for rule"),
            new Problem.Fatal   ("README Owners", "README_badrole.md",             "@@badrole",                "Illegal role was specified")
        );
        assertNotNull(exception);
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testFailLevelNever(WireMockRuntimeInfo wmRuntimeInfo, TestInfo testInfo) throws CodeOwnersValidationException {
        GitlabConfiguration configuration = new GitlabConfiguration(
            new ServerUrl(wmRuntimeInfo.getHttpBaseUrl(), null),
            new ProjectId(null, null),
            new AccessToken("FETCH_USER_ACCESS_TOKEN")
        );
        runFailLevelTests(testInfo, configuration, NEVER);
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testFailLevelFatal(WireMockRuntimeInfo wmRuntimeInfo, TestInfo testInfo) throws CodeOwnersValidationException {
        GitlabConfiguration configuration = new GitlabConfiguration(
            new ServerUrl(wmRuntimeInfo.getHttpBaseUrl(), null),
            new ProjectId(null, null),
            new AccessToken("FETCH_USER_ACCESS_TOKEN")
        );
        runFailLevelTests(testInfo, configuration, FATAL);
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testFailLevelError(WireMockRuntimeInfo wmRuntimeInfo, TestInfo testInfo) throws CodeOwnersValidationException {
        GitlabConfiguration configuration = new GitlabConfiguration(
            new ServerUrl(wmRuntimeInfo.getHttpBaseUrl(), null),
            new ProjectId(null, null),
            new AccessToken("FETCH_USER_ACCESS_TOKEN")
        );
        runFailLevelTests(testInfo, configuration, ERROR);
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testFailLevelWarning(WireMockRuntimeInfo wmRuntimeInfo, TestInfo testInfo) throws CodeOwnersValidationException {
        GitlabConfiguration configuration = new GitlabConfiguration(
            new ServerUrl(wmRuntimeInfo.getHttpBaseUrl(), null),
            new ProjectId(null, null),
            new AccessToken("FETCH_USER_ACCESS_TOKEN")
        )
        .setFailLevel(WARNING);

        CodeOwnersValidationException exception;
        exception = runFailLevelTestInfo(testInfo, configuration);
        assertNull(exception, "No exception should have been thrown");
        exception = runFailLevelTestWarning(testInfo, configuration);
        assertNotNull(exception, "An exception should have been thrown");
        exception = runFailLevelTestError(testInfo, configuration);
        assertNotNull(exception, "An exception should have been thrown");
        exception = runFailLevelTestFatal(testInfo, configuration);
        assertNotNull(exception, "An exception should have been thrown");
    }

    private void runFailLevelTests(TestInfo testInfo,
                                   GitlabConfiguration configuration,
                                   FailLevel failLevel
                                   ) throws CodeOwnersValidationException {

        boolean infoShouldFail = false;
        boolean warningShouldFail = false;
        boolean errorShouldFail = false;
        boolean fatalShouldFail = false;
        switch (failLevel) {
            case NEVER:
                break;
            case FATAL:
                fatalShouldFail = true;
                break;
            case ERROR:
                errorShouldFail = true;
                fatalShouldFail = true;
                break;
            case WARNING:
                warningShouldFail = true;
                errorShouldFail = true;
                fatalShouldFail = true;
                break;
        }
        configuration.setFailLevel(failLevel);
        CodeOwnersValidationException exception;
        exception = runFailLevelTestInfo(testInfo, configuration);
        assertFail(infoShouldFail, exception);
        exception = runFailLevelTestWarning(testInfo, configuration);
        assertFail(warningShouldFail, exception);
        exception = runFailLevelTestError(testInfo, configuration);
        assertFail(errorShouldFail, exception);
        exception = runFailLevelTestFatal(testInfo, configuration);
        assertFail(fatalShouldFail, exception);
    }

    private void assertFail(boolean shouldFail, CodeOwnersValidationException exception) {
        if (shouldFail) {
            assertNotNull(exception, "An exception should have been thrown");
        } else {
            assertNull(exception, "No exception should have been thrown");
        }
    }

    private CodeOwnersValidationException runFailLevelTestFatal(TestInfo testInfo, GitlabConfiguration configuration) {
        return runCodeownersValidation(testInfo, configuration,
            new CodeOwners(
                "[README Owners]\n" +
                "README_niels.md @niels\n" + // INFO:  niels --> is member
                "README_niels_private_email.md private@example.nl\n" + // WARNING: niels --> is member --> Cannot find --> Warning only
                "README_dummy.md @dummy\n" + // ERROR: dummy --> is NOT member --> Error
                "README_badrole.md @@badrole\n" // ERROR: dummy --> is NOT member --> Error
            ),
            new Problem.Info    ("README Owners", "README_niels.md",               "@niels",             "Valid approver: Member with username \"niels\" can approve (AccessLevel:50=OWNER)"),
            new Problem.Warning ("README Owners", "README_niels_private_email.md", "private@example.nl", "Unable to verify email address: Assuming the user exists and can approve"),
            new Problem.Error   ("README Owners", "README_dummy.md",               "@dummy",             "User exists but is not a member of with this project: Dummy User"),
            new Problem.Error   ("README Owners", "README_dummy.md",               "",                   "NO Valid Approvers for rule"),
            new Problem.Fatal   ("README Owners", "README_badrole.md",             "@@badrole",          "Illegal role was specified")
        );
    }

    private CodeOwnersValidationException runFailLevelTestError(TestInfo testInfo, GitlabConfiguration configuration) {
        return runCodeownersValidation(testInfo, configuration,
            new CodeOwners(
                "[README Owners]\n" +
                "README_niels.md @niels\n" + // INFO:  niels --> is member
                "README_niels_private_email.md private@example.nl\n" + // WARNING: niels --> is member --> Cannot find --> Warning only
                "README_dummy.md @dummy\n" // ERROR: dummy --> is NOT member --> Error
            ),
            new Problem.Info    ("README Owners", "README_niels.md",               "@niels",             "Valid approver: Member with username \"niels\" can approve (AccessLevel:50=OWNER)"),
            new Problem.Warning ("README Owners", "README_niels_private_email.md", "private@example.nl", "Unable to verify email address: Assuming the user exists and can approve"),
            new Problem.Error   ("README Owners", "README_dummy.md",               "@dummy",             "User exists but is not a member of with this project: Dummy User"),
            new Problem.Error   ("README Owners", "README_dummy.md",               "",                   "NO Valid Approvers for rule")
        );
    }

    private CodeOwnersValidationException runFailLevelTestWarning(TestInfo testInfo, GitlabConfiguration configuration) {
        return runCodeownersValidation(testInfo, configuration,
            new CodeOwners(
                "[README Owners]\n" +
                "README_niels.md @niels\n" + // INFO:  niels --> is member
                "README_niels_private_email.md private@example.nl\n" // WARNING: niels --> is member --> Cannot find --> Warning only
            ),
            new Problem.Warning("README Owners", "README_niels_private_email.md", "private@example.nl","Unable to verify email address: Assuming the user exists and can approve")
        );
    }

    private CodeOwnersValidationException runFailLevelTestInfo(TestInfo testInfo, GitlabConfiguration configuration) {
        return runCodeownersValidation(testInfo, configuration,
            new CodeOwners(
                "[README Owners]\n" +
                "README_niels.md @niels\n" // INFO:  niels --> is member
            ),
            new Problem.Info("README Owners", "README_niels.md", "@niels", "Valid approver: Member with username \"niels\" can approve (AccessLevel:50=OWNER)")
        );
    }

    private CodeOwnersValidationException runCodeownersValidation(TestInfo testInfo, GitlabConfiguration configuration, CodeOwners codeOwners, Problem...expectedProblems) {
        return runCodeownersValidation(testInfo, configuration, false, codeOwners, expectedProblems);
    }

    private CodeOwnersValidationException runCodeownersValidation(TestInfo testInfo, GitlabConfiguration configuration, boolean logResult, CodeOwners codeOwners, Problem...expectedProblems) {
        try(GitlabProjectMembers gitlabProjectMembers = new GitlabProjectMembers(configuration)) {
            Logger logger =  LoggerFactory.getLogger(testInfo.getTestMethod().map(Method::getName).orElse("runCodeownersValidation"));
            gitlabProjectMembers.setShowAllApprovers(true);
            CodeOwnersValidationException exception = null;
            ProblemTable problemTable = null;
            try {
                problemTable = gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners);
                gitlabProjectMembers.failIfExceededFailLevel(problemTable);
            } catch (CodeOwnersValidationException e) {
                exception = e;
            }

            assertNotNull(problemTable);
            for (Problem expectedProblem : expectedProblems) {
                assertTrue(problemTable.contains(expectedProblem), "The problem table \n" + problemTable.getProblems().stream().map(Problem::toString).collect(Collectors.joining("\n")) + "\n should contain \n" + expectedProblem);
            }
            if (logResult) {
                logger.info(problemTable.toString());
                logger.info(problemTable.toProblemMessageGroupedString());
            }
            return exception;
        }
        catch (CodeOwnersValidationException e) {
            return e;
        }
    }
}
