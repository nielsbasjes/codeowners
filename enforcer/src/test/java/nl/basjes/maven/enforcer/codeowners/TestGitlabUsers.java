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

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import nl.basjes.codeowners.CodeOwners;
import nl.basjes.maven.enforcer.codeowners.GitlabConfiguration.AccessToken;
import nl.basjes.maven.enforcer.codeowners.GitlabConfiguration.ProjectId;
import nl.basjes.maven.enforcer.codeowners.GitlabConfiguration.ServerUrl;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WireMockTest
public class TestGitlabUsers {

    static GitlabConfiguration makeConfig(String httpBaseUrl, String projectId, String tokenEnvVariableName) {
        return new GitlabConfiguration(
            new ServerUrl(httpBaseUrl, null),
            new ProjectId(projectId, null),
            new AccessToken(tokenEnvVariableName),
            true
        );
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testValidCodeOwners(WireMockRuntimeInfo wmRuntimeInfo) throws EnforcerRuleException {
        String httpBaseUrl = wmRuntimeInfo.getHttpBaseUrl();
        GitlabConfiguration configuration = makeConfig(httpBaseUrl, null, "FETCH_USER_ACCESS_TOKEN");

        try(GitlabProjectMembers gitlabProjectMembers = new GitlabProjectMembers(configuration)) {
            CodeOwners codeOwners = new CodeOwners(
                "[README Owners]\n" +
                "README.md @niels @SomeUser @isguest\n" +
                "README_owner.md @@owner\n" +
                "README_owners.md @@owners\n" +
                "README_maintainer.md @@maintainer\n" +
                "README_maintainers.md @@maintainers\n" +
                "README_developer.md @@developer\n" +
                "README_developers.md @@developers\n" +
                "README_group_dev.md @codeowners/developers\n" +
                "README_group_main.md @codeowners/maintainers\n" +
                "README_user_public_email.md public@example.nl\n"
            );

            EnforcerTestLogger logger = new EnforcerTestLogger();
            gitlabProjectMembers.setShowAllApprovers(true);
            gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners);
            gitlabProjectMembers.setShowAllApprovers(false);
            gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners);
        }
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testRoles(WireMockRuntimeInfo wmRuntimeInfo) throws EnforcerRuleException {
        String httpBaseUrl = wmRuntimeInfo.getHttpBaseUrl();
        GitlabConfiguration configuration = makeConfig(httpBaseUrl, null, "FETCH_USER_ACCESS_TOKEN");

        try(GitlabProjectMembers gitlabProjectMembers = new GitlabProjectMembers(configuration)) {
            CodeOwners codeOwners = new CodeOwners(
                "[README Owners]\n" +
                    "README_niels.md @niels\n" +
                    "README_owner.md @@owner\n" +
                    "README_owners.md @@owners\n" +
                    "README_maintainer.md @@maintainer\n" +
                    "README_maintainers.md @@maintainers\n" +
                    "README_developer.md @@developer\n" +
                    "README_developers.md @@developers\n"
            );

            EnforcerTestLogger logger = new EnforcerTestLogger();
            gitlabProjectMembers.setShowAllApprovers(true);
            gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners);
            gitlabProjectMembers.setShowAllApprovers(false);
            gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners);
        }
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testSharedGroups(WireMockRuntimeInfo wmRuntimeInfo) throws EnforcerRuleException {
        String httpBaseUrl = wmRuntimeInfo.getHttpBaseUrl();
        GitlabConfiguration configuration = makeConfig(httpBaseUrl, null, "FETCH_USER_ACCESS_TOKEN");

        try(GitlabProjectMembers gitlabProjectMembers = new GitlabProjectMembers(configuration)) {
            CodeOwners codeOwners = new CodeOwners(
                "[README Owners]\n" +
                "README_shared_group_guests.md @codeowners/guests\n" +
                "README_shared_group_developers.md @codeowners/developers\n" +
                "README_shared_group_maintainers.md @codeowners/maintainers\n"
            );

            EnforcerTestLogger logger = new EnforcerTestLogger();
            gitlabProjectMembers.setShowAllApprovers(true);
            assertThrows(EnforcerRuleException.class, () -> gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners));
            gitlabProjectMembers.setShowAllApprovers(false);
            assertThrows(EnforcerRuleException.class, () -> gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners));
            logger.assertContainsWarn ("| README Owners | README_shared_group_guests.md | @codeowners/guests | Shared group does not have sufficient approver level: GUEST |");
            logger.assertContainsError("| README Owners | README_shared_group_guests.md |                    | NO Valid Approvers for rule                                 |");
        }
    }


    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "nomembers")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testBadRoles(WireMockRuntimeInfo wmRuntimeInfo) throws EnforcerRuleException {
        String httpBaseUrl = wmRuntimeInfo.getHttpBaseUrl();
        GitlabConfiguration configuration = makeConfig(httpBaseUrl, null, "FETCH_USER_ACCESS_TOKEN");

        try(GitlabProjectMembers gitlabProjectMembers = new GitlabProjectMembers(configuration)) {
            CodeOwners codeOwners = new CodeOwners(
                "[README Owners]\n" +
                "README_1.md @niels @dummy\n" +
                "README_2.md @niels @dummy\n" +
                "README_owner.md @@owner\n" +
                "README_owners.md @@owners\n" +
                "README_maintainer.md @@maintainer\n" +
                "README_maintainers.md @@maintainers\n" +
                "README_developer.md @@developer\n" +
                "README_developers.md @@developers\n" +
                "README_nosuchrole.md @@nosuchrole\n" +
                "README_disabled.md @disabledowner @disabledmaintainer @disableddeveloper\n" +
                "README_locked.md @lockedowner @lockedmaintainer @lockeddeveloper\n" +
                "README_disabled.md @disabledowner @disabledmaintainer @disableddeveloper\n" +
                "README_locked.md @lockedowner @lockedmaintainer @lockeddeveloper\n" +
                "README_dummy_1.md @dummy\n" +
                "README_dummy_2.md @dummy\n" +
                "README_dummy_3.md @dummy\n" +
                "README_nonsharedgroup_1.md @opensource \n" +
                "README_nonsharedgroup_2.md @opensource \n" +
                "README_nonsharedgroup_3.md @opensource \n" +
                "README_sharedgroup_guests_1.md @codeowners/guests \n" +
                "README_sharedgroup_guests_2.md @codeowners/guests \n" +
                "README_sharedgroup_guests_3.md @codeowners/guests \n" +
                "README_nosuchgroup_1.md @codeowners/nosuchgroup \n" +
                "README_nosuchgroup_2.md @codeowners/nosuchgroup \n" +
                "README_nosuchgroup_3.md @codeowners/nosuchgroup \n"
            );

            EnforcerTestLogger logger = new EnforcerTestLogger();
            gitlabProjectMembers.setShowAllApprovers(false);
            assertThrows(EnforcerRuleException.class, () -> gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners));

            // We have 10 because of messages during loading the information
            assertEquals(10, logger.countInfo(), "There should be no INFO level messages.");

            logger.assertContainsError("| README Owners | README_1.md                    | @niels                  | User is not a member of with this project: Niels Basjes     |");
            logger.assertContainsError("| README Owners | README_1.md                    | @dummy                  | User is not a member of with this project: Dummy User       |");
            logger.assertContainsError("| README Owners | README_1.md                    |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsError("| README Owners | README_2.md                    | @niels                  | User is not a member of with this project: Niels Basjes     |");
            logger.assertContainsError("| README Owners | README_2.md                    | @dummy                  | User is not a member of with this project: Dummy User       |");
            logger.assertContainsError("| README Owners | README_2.md                    |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsWarn ("| README Owners | README_owner.md                | @@owner                 | No direct project members are owner                         |");
            logger.assertContainsError("| README Owners | README_owner.md                |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsWarn ("| README Owners | README_owners.md               | @@owners                | No direct project members are owner                         |");
            logger.assertContainsError("| README Owners | README_owners.md               |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsWarn ("| README Owners | README_maintainer.md           | @@maintainer            | No direct project members are maintainer                    |");
            logger.assertContainsError("| README Owners | README_maintainer.md           |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsWarn ("| README Owners | README_maintainers.md          | @@maintainers           | No direct project members are maintainer                    |");
            logger.assertContainsError("| README Owners | README_maintainers.md          |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsWarn ("| README Owners | README_developer.md            | @@developer             | No direct project members are developer                     |");
            logger.assertContainsError("| README Owners | README_developer.md            |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsWarn ("| README Owners | README_developers.md           | @@developers            | No direct project members are developer                     |");
            logger.assertContainsError("| README Owners | README_developers.md           |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsError("| README Owners | README_nosuchrole.md           | @@nosuchrole            | Illegal role attempted                                      |");
            logger.assertContainsError("| README Owners | README_nosuchrole.md           |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsWarn ("| README Owners | README_disabled.md             | @disabledowner          | Disabled account: State=disabled; Locked=false              |");
            logger.assertContainsWarn ("| README Owners | README_disabled.md             | @disabledmaintainer     | Disabled account: State=disabled; Locked=false              |");
            logger.assertContainsWarn ("| README Owners | README_disabled.md             | @disableddeveloper      | Disabled account: State=disabled; Locked=false              |");
            logger.assertContainsError("| README Owners | README_disabled.md             |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsWarn ("| README Owners | README_locked.md               | @lockedowner            | Disabled account: State=active; Locked=true                 |");
            logger.assertContainsWarn ("| README Owners | README_locked.md               | @lockedmaintainer       | Disabled account: State=active; Locked=true                 |");
            logger.assertContainsWarn ("| README Owners | README_locked.md               | @lockeddeveloper        | Disabled account: State=active; Locked=true                 |");
            logger.assertContainsError("| README Owners | README_locked.md               |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsWarn ("| README Owners | README_disabled.md             | @disabledowner          | Disabled account: State=disabled; Locked=false              |");
            logger.assertContainsWarn ("| README Owners | README_disabled.md             | @disabledmaintainer     | Disabled account: State=disabled; Locked=false              |");
            logger.assertContainsWarn ("| README Owners | README_disabled.md             | @disableddeveloper      | Disabled account: State=disabled; Locked=false              |");
            logger.assertContainsError("| README Owners | README_disabled.md             |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsWarn ("| README Owners | README_locked.md               | @lockedowner            | Disabled account: State=active; Locked=true                 |");
            logger.assertContainsWarn ("| README Owners | README_locked.md               | @lockedmaintainer       | Disabled account: State=active; Locked=true                 |");
            logger.assertContainsWarn ("| README Owners | README_locked.md               | @lockeddeveloper        | Disabled account: State=active; Locked=true                 |");
            logger.assertContainsError("| README Owners | README_locked.md               |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsError("| README Owners | README_dummy_1.md              | @dummy                  | User is not a member of with this project: Dummy User       |");
            logger.assertContainsError("| README Owners | README_dummy_1.md              |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsError("| README Owners | README_dummy_2.md              | @dummy                  | User is not a member of with this project: Dummy User       |");
            logger.assertContainsError("| README Owners | README_dummy_2.md              |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsError("| README Owners | README_dummy_3.md              | @dummy                  | User is not a member of with this project: Dummy User       |");
            logger.assertContainsError("| README Owners | README_dummy_3.md              |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsError("| README Owners | README_nonsharedgroup_1.md     | @opensource             | Group is not a group shared with this project.              |");
            logger.assertContainsError("| README Owners | README_nonsharedgroup_1.md     |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsError("| README Owners | README_nonsharedgroup_2.md     | @opensource             | Group is not a group shared with this project.              |");
            logger.assertContainsError("| README Owners | README_nonsharedgroup_2.md     |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsError("| README Owners | README_nonsharedgroup_3.md     | @opensource             | Group is not a group shared with this project.              |");
            logger.assertContainsError("| README Owners | README_nonsharedgroup_3.md     |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsWarn ("| README Owners | README_sharedgroup_guests_1.md | @codeowners/guests      | Shared group does not have sufficient approver level: GUEST |");
            logger.assertContainsError("| README Owners | README_sharedgroup_guests_1.md |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsWarn ("| README Owners | README_sharedgroup_guests_2.md | @codeowners/guests      | Shared group does not have sufficient approver level: GUEST |");
            logger.assertContainsError("| README Owners | README_sharedgroup_guests_2.md |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsWarn ("| README Owners | README_sharedgroup_guests_3.md | @codeowners/guests      | Shared group does not have sufficient approver level: GUEST |");
            logger.assertContainsError("| README Owners | README_sharedgroup_guests_3.md |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsWarn ("| README Owners | README_nosuchgroup_1.md        | @codeowners/nosuchgroup | Approver does not exist in Gitlab                           |");
            logger.assertContainsError("| README Owners | README_nosuchgroup_1.md        |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsWarn ("| README Owners | README_nosuchgroup_2.md        | @codeowners/nosuchgroup | Approver does not exist in Gitlab                           |");
            logger.assertContainsError("| README Owners | README_nosuchgroup_2.md        |                         | NO Valid Approvers for rule                                 |");
            logger.assertContainsWarn ("| README Owners | README_nosuchgroup_3.md        | @codeowners/nosuchgroup | Approver does not exist in Gitlab                           |");
            logger.assertContainsError("| README Owners | README_nosuchgroup_3.md        |                         | NO Valid Approvers for rule                                 |");

        }
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testOwnerHandling(WireMockRuntimeInfo wmRuntimeInfo) throws EnforcerRuleException {
        String httpBaseUrl = wmRuntimeInfo.getHttpBaseUrl();
        GitlabConfiguration configuration = makeConfig(httpBaseUrl, null, "FETCH_USER_ACCESS_TOKEN");

        try(GitlabProjectMembers gitlabProjectMembers = new GitlabProjectMembers(configuration)) {
            CodeOwners codeOwners = new CodeOwners(
                "[README Owners]\n" +
                "README_niels.md @niels\n" + // niels --> is member
                "README_dummy.md @dummy\n" + // dummy --> is NOT member --> Error
                "README_niels_public_email.md public@example.nl\n" + // niels --> is member
                "README_niels_private_email.md private@example.nl\n" + // niels --> is member --> Cannot find --> Warning only
                "README_dummy_public_email.md public@dummy.example.nl\n" + // dummy --> is NOT member --> Error
                "README_dummy_private_email.md private@dummy.example.nl\n"  // niels --> is NOT member --> Cannot find --> Warning only
            );

            EnforcerTestLogger logger = new EnforcerTestLogger();
            gitlabProjectMembers.setShowAllApprovers(true);
            assertThrows(EnforcerRuleException.class, () -> gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners));
            gitlabProjectMembers.setShowAllApprovers(false);
            assertThrows(EnforcerRuleException.class, () -> gitlabProjectMembers.verifyAllCodeowners(logger, codeOwners));

            logger.assertContainsInfo ("| README Owners | README_niels.md               | @niels                   | Valid approver. Member with username \"niels\" has access level 50 (=OWNER)");
            logger.assertContainsError("| README Owners | README_dummy.md               | @dummy                   | User is not a member of with this project: Dummy User");
            logger.assertContainsError("| README Owners | README_dummy.md               |                          | NO Valid Approvers for rule");
            logger.assertContainsInfo ("| README Owners | README_niels_public_email.md  | public@example.nl        | Valid approver. Member with username \"niels\" has access level 50 (=OWNER)");
            logger.assertContainsWarn ("| README Owners | README_niels_private_email.md | private@example.nl       | Cannot verify access because this is an email address");
            logger.assertContainsError("| README Owners | README_dummy_public_email.md  | public@dummy.example.nl  | User is not a member of with this project: Dummy User");
            logger.assertContainsError("| README Owners | README_dummy_public_email.md  |                          | NO Valid Approvers for rule");
            logger.assertContainsWarn ("| README Owners | README_dummy_private_email.md | private@dummy.example.nl | Cannot verify access because this is an email address");
        }
    }


}
