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
import nl.basjes.codeowners.validator.CodeOwnersValidationException
import org.junitpioneer.jupiter.SetEnvironmentVariable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wiremock.org.hamcrest.MatcherAssert.assertThat
import wiremock.org.hamcrest.core.StringContains.containsString
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@WireMockTest
class TestGitlabConfiguration {
    @Test //    @SetEnvironmentVariable(key = "CI_SERVER_URL",           value = "foobar") // <<-- Is bad
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    fun testServerUrlInvalid() {
        val configuration = TestGitlabUsers.makeConfig(null, null, "FETCH_USER_ACCESS_TOKEN")

        val exception = assertFailsWith<IllegalArgumentException> { GitlabProjectMembers(configuration) }

        assertThat(
            exception.message,
            containsString("the environment variable \"CI_SERVER_URL\" does not exist")
        )
        assertThat(
            exception.message,
            containsString("(via environment variable \"CI_PROJECT_ID\")")
        )
        assertThat(
            exception.message,
            containsString("(via environment variable \"FETCH_USER_ACCESS_TOKEN\")")
        )
        assertFalse(configuration.isDefaultCIConfigRunningOutsideCI)
    }

    @Test
    @SetEnvironmentVariable(key = "CI_SERVER_URL", value = "http://localhost:0") // <<-- Is bad
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    fun testServerUrlUnreachable() {
        val configuration = TestGitlabUsers.makeConfig(null, null, "FETCH_USER_ACCESS_TOKEN")

        val exception = assertFailsWith<CodeOwnersValidationException> { GitlabProjectMembers(configuration) }

        assertThat(
            exception.message,
            containsString("(via environment variable \"CI_SERVER_URL\")")
        )
        assertThat(
            exception.message,
            containsString("(via environment variable \"CI_PROJECT_ID\")")
        )
        assertThat(
            exception.message,
            containsString("(via environment variable \"FETCH_USER_ACCESS_TOKEN\")")
        )
        assertFalse(configuration.isDefaultCIConfigRunningOutsideCI)
    }


    @Test
    @SetEnvironmentVariable(key = "CI_SERVER_URL", value = "https://git.example.nl")
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels   project") // <<-- Is bad
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    fun testProjectIdInvalid() {
        val configuration = TestGitlabUsers.makeConfig(null, null, "FETCH_USER_ACCESS_TOKEN")

        val exception = assertFailsWith<IllegalArgumentException> { GitlabProjectMembers(configuration) }

        assertThat(
            exception.message,
            containsString("(via environment variable \"CI_SERVER_URL\")")
        )
        assertThat(
            exception.message,
            containsString("the value from environment variable \"CI_PROJECT_ID\" is NOT valid")
        )
        assertThat(
            exception.message,
            containsString("(via environment variable \"FETCH_USER_ACCESS_TOKEN\")")
        )
        assertFalse(configuration.isDefaultCIConfigRunningOutsideCI)
    }


    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "doesnotexist")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    fun testProjectIdDoesNotExist(wmRuntimeInfo: WireMockRuntimeInfo?) {
        val configuration = TestGitlabUsers.makeConfig(wmRuntimeInfo, null, "FETCH_USER_ACCESS_TOKEN")
        log.info("Test with config:\n{}", configuration)
        val exception = assertFailsWith<CodeOwnersValidationException> { GitlabProjectMembers(configuration) }

        assertThat(
            exception.message,
            containsString("Unable to load projectId from Gitlab")
        )
        assertFalse(configuration.isDefaultCIConfigRunningOutsideCI)
    }


    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "")
    fun testAccessTokenInvalid(wmRuntimeInfo: WireMockRuntimeInfo?) {
        val configuration = TestGitlabUsers.makeConfig(wmRuntimeInfo, null, "FETCH_USER_ACCESS_TOKEN")

        val exception =
            assertFailsWith<IllegalArgumentException> {
                GitlabProjectMembers(configuration)
            }

        assertThat(
            exception.message,
            containsString("(via property \"gitlab.serverUrl.url\")")
        )
        assertThat(
            exception.message,
            containsString("(via environment variable \"CI_PROJECT_ID\")")
        )
        assertThat(
            exception.message,
            containsString("the value from environment variable \"FETCH_USER_ACCESS_TOKEN\" is NOT valid")
        )
        assertFalse(configuration.isDefaultCIConfigRunningOutsideCI)
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-badtoken")
    fun testAccessTokenBad(wmRuntimeInfo: WireMockRuntimeInfo?) {
        val configuration = TestGitlabUsers.makeConfig(wmRuntimeInfo, null, "FETCH_USER_ACCESS_TOKEN")

        val exception = assertFailsWith<CodeOwnersValidationException> { GitlabProjectMembers(configuration) }

        assertThat(
            exception.message,
            containsString("Unable to load projectId from Gitlab")
        )
        assertFalse(configuration.isDefaultCIConfigRunningOutsideCI)
    }

    // Check if the config IN CI is picked up
    @Test
    @SetEnvironmentVariable(key = "CI_SERVER_URL", value = "https://gitlab.example.nl")
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    fun testCIConfig() {
        val configuration = GitlabConfiguration(
            GitlabConfiguration.ServerUrl(null, null),
            GitlabConfiguration.ProjectId(null, null),
            GitlabConfiguration.AccessToken("FETCH_USER_ACCESS_TOKEN")
        )
        assertFalse(
            configuration.isDefaultCIConfigRunningOutsideCI,
            "This is the default config INSIDE the pipeline"
        )
        assertTrue(configuration.isValid(), "Config should be valid")
    }

    private val baseCIConfig: GitlabConfiguration
        get() = GitlabConfiguration(
            GitlabConfiguration.ServerUrl(null, null),
            GitlabConfiguration.ProjectId(null, null),
            GitlabConfiguration.AccessToken("FETCH_USER_ACCESS_TOKEN")
        )

    @Test //    @SetEnvironmentVariable(key = "CI_SERVER_URL",           value = "https://gitlab.example.nl")
    //    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    //    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    fun testCIConfigOutsideOfCI1() {
        val configuration = this.baseCIConfig
        assertTrue(
            configuration.isDefaultCIConfigRunningOutsideCI,
            "Config should be seen as the default CI config"
        )
        assertFalse(configuration.isValid(), "Config should be invalid")
    }

    @Test //    @SetEnvironmentVariable(key = "CI_SERVER_URL",           value = "https://gitlab.example.nl")
    //    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    //    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    fun testCIConfigOutsideOfCI2() {
        val configuration = this.baseCIConfig
        assertTrue(
            configuration.isDefaultCIConfigRunningOutsideCI,
            "Config should be seen as the default CI config"
        )
        assertFalse(configuration.isValid(), "Config should be invalid")
    }

    @Test
    @SetEnvironmentVariable(
        key = "CI_SERVER_URL",
        value = "https://gitlab.example.nl"
    ) //    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    //    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    fun testCIConfig1() {
        val configuration = this.baseCIConfig
        assertFalse(
            configuration.isDefaultCIConfigRunningOutsideCI,
            "Partial CI parameters should be invalid"
        )
        assertFalse(configuration.isValid(), "Config should be invalid")
    }

    @Test //    @SetEnvironmentVariable(key = "CI_SERVER_URL",           value = "https://gitlab.example.nl")
    @SetEnvironmentVariable(
        key = "CI_PROJECT_ID",
        value = "niels/project"
    ) //    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    fun testCIConfig2() {
        val configuration = this.baseCIConfig
        assertFalse(
            configuration.isDefaultCIConfigRunningOutsideCI,
            "Partial CI parameters should be invalid"
        )
        assertFalse(configuration.isValid(), "Config should be invalid")
    }

    @Test
    @SetEnvironmentVariable(key = "CI_SERVER_URL", value = "https://gitlab.example.nl")
    @SetEnvironmentVariable(
        key = "CI_PROJECT_ID",
        value = "niels/project"
    ) //    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    fun testCIConfig3() {
        val configuration = this.baseCIConfig
        assertFalse(
            configuration.isDefaultCIConfigRunningOutsideCI,
            "Partial CI parameters should be invalid"
        )
        assertFalse(configuration.isValid(), "Config should be invalid")
    }

    @Test //    @SetEnvironmentVariable(key = "CI_SERVER_URL",           value = "https://gitlab.example.nl")
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    fun testCIConfig4() {
        val configuration = this.baseCIConfig
        assertFalse(
            configuration.isDefaultCIConfigRunningOutsideCI,
            "Partial CI parameters should be invalid"
        )
        assertFalse(configuration.isValid(), "Config should be invalid")
    }

    @Test
    @SetEnvironmentVariable(
        key = "CI_SERVER_URL",
        value = "https://gitlab.example.nl"
    ) //    @SetEnvironmentVariable(key = "CI_PROJECT_ID",           value = "niels/project")
    @SetEnvironmentVariable(key = "FETCH_USER_ACCESS_TOKEN", value = "gltst-validtoken")
    fun testCIConfig5() {
        val configuration = this.baseCIConfig
        assertFalse(
            configuration.isDefaultCIConfigRunningOutsideCI,
            "Partial CI parameters should be invalid"
        )
        assertFalse(configuration.isValid(), "Config should be invalid")
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TestGitlabConfiguration::class.java)
    }
}
