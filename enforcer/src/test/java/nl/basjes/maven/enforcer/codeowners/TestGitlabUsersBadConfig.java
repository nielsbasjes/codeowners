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
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static nl.basjes.maven.enforcer.codeowners.TestGitlabUsers.makeConfig;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static wiremock.org.hamcrest.MatcherAssert.assertThat;
import static wiremock.org.hamcrest.core.StringContains.containsString;

@WireMockTest
public class TestGitlabUsersBadConfig {

    @Test
//    @SetEnvironmentVariable(key = "CI_SERVER_URL",           value = "foobar") // <<-- Is bad
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testServerUrlInvalid() {
        GitlabConfiguration configuration = makeConfig(null, null, "FETCH_USER_ACCESS_TOKEN");

        EnforcerRuleException enforcerRuleException = assertThrows(EnforcerRuleException.class, () -> {
                try (GitlabProjectMembers ignored = new GitlabProjectMembers(configuration)) {
                    fail("Should never get here:" + ignored);
                }
            }
        );

        assertThat(enforcerRuleException.getMessage(), containsString("CI_SERVER_URL is NOT valid"));
        assertThat(enforcerRuleException.getMessage(), containsString("CI_PROJECT_ID is valid"));
        assertThat(enforcerRuleException.getMessage(), containsString("FETCH_USER_ACCESS_TOKEN is valid"));
    }

    @Test
    @SetEnvironmentVariable(key = "CI_SERVER_URL",           value = "http://localhost:0") // <<-- Is bad
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testServerUrlUnreachable() {
        GitlabConfiguration configuration = makeConfig(null, null, "FETCH_USER_ACCESS_TOKEN");

        EnforcerRuleException enforcerRuleException = assertThrows(EnforcerRuleException.class, () -> {
                try (GitlabProjectMembers ignored = new GitlabProjectMembers(configuration)) {
                    fail("Should never get here:" + ignored);
                }
            }
        );

        assertThat(enforcerRuleException.getMessage(), containsString("CI_SERVER_URL is valid"));
        assertThat(enforcerRuleException.getMessage(), containsString("CI_PROJECT_ID is valid"));
        assertThat(enforcerRuleException.getMessage(), containsString("FETCH_USER_ACCESS_TOKEN is valid"));
    }


    @Test
    @SetEnvironmentVariable(key = "CI_SERVER_URL",           value = "https://git.example.nl")
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels   project") // <<-- Is bad
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testProjectIdInvalid() {
        GitlabConfiguration configuration = makeConfig(null, null, "FETCH_USER_ACCESS_TOKEN");

        EnforcerRuleException enforcerRuleException = assertThrows(EnforcerRuleException.class, () -> {
                try (GitlabProjectMembers ignored = new GitlabProjectMembers(configuration)) {
                    fail("Should never get here:" + ignored);
                }
            }
        );

        assertThat(enforcerRuleException.getMessage(), containsString("CI_SERVER_URL is valid"));
        assertThat(enforcerRuleException.getMessage(), containsString("CI_PROJECT_ID is NOT valid"));
        assertThat(enforcerRuleException.getMessage(), containsString("FETCH_USER_ACCESS_TOKEN is valid"));
    }


    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "doesnotexist")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    public void testProjectIdDoesNotExist(WireMockRuntimeInfo wmRuntimeInfo) {
        GitlabConfiguration configuration = makeConfig(wmRuntimeInfo.getHttpBaseUrl(), null, "FETCH_USER_ACCESS_TOKEN");

        EnforcerRuleException enforcerRuleException = assertThrows(EnforcerRuleException.class, () -> {
                try (GitlabProjectMembers ignored = new GitlabProjectMembers(configuration)) {
                    fail("Should never get here:" + ignored);
                }
            }
        );

        assertThat(enforcerRuleException.getMessage(), containsString("Unable to load projectId from Gitlab"));
    }


    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "")
    public void testAccessTokenInvalid(WireMockRuntimeInfo wmRuntimeInfo) {
        GitlabConfiguration configuration = makeConfig(wmRuntimeInfo.getHttpBaseUrl(), null, "FETCH_USER_ACCESS_TOKEN");

        EnforcerRuleException enforcerRuleException = assertThrows(EnforcerRuleException.class, () -> {
                try (GitlabProjectMembers ignored = new GitlabProjectMembers(configuration)) {
                    fail("Should never get here:" + ignored);
                }
            }
        );

        assertThat(enforcerRuleException.getMessage(), containsString("gitlab.serverUrl.url is valid"));
        assertThat(enforcerRuleException.getMessage(), containsString("CI_PROJECT_ID is valid"));
        assertThat(enforcerRuleException.getMessage(), containsString("FETCH_USER_ACCESS_TOKEN is NOT valid"));
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-badtoken")
    public void testAccessTokenBad(WireMockRuntimeInfo wmRuntimeInfo) {
        GitlabConfiguration configuration = makeConfig(wmRuntimeInfo.getHttpBaseUrl(), null, "FETCH_USER_ACCESS_TOKEN");

        EnforcerRuleException enforcerRuleException = assertThrows(EnforcerRuleException.class, () -> {
                try (GitlabProjectMembers ignored = new GitlabProjectMembers(configuration)) {
                    fail("Should never get here:" + ignored);
                }
            }
        );

        assertThat(enforcerRuleException.getMessage(), containsString("Unable to load projectId from Gitlab"));
    }

}
