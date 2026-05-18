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

import org.junitpioneer.jupiter.SetEnvironmentVariable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestGitlabConfigurationAccessToken {
    @Test
    @SetEnvironmentVariable(key = "MY_SECRET_TOKEN", value = "gltst-mySecretToken") // <<-- Is good
    fun testAccessTokenEnvValidValue() {
        val accessToken = GitlabConfiguration.AccessToken("MY_SECRET_TOKEN")
        assertTrue(accessToken.isValid())
        assertEquals("gltst-mySecretToken", accessToken.value)
        assertEquals("gltst-*****en", accessToken.sanitizedValue)

        // Ensure the actual token value is hidden in a toString()
        log.info("{}", accessToken)
        assertFalse(accessToken.toString().contains("gltst-mySecretToken"))
        assertTrue(accessToken.toString().contains("gltst-*****en"))
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SECRET_TOKEN", value = "short") // <<-- Is good
    fun testAccessTokenEnvValidValueShortToken() {
        val accessToken = GitlabConfiguration.AccessToken("MY_SECRET_TOKEN")
        assertTrue(accessToken.isValid())
        assertEquals("short", accessToken.value)
        assertEquals("***", accessToken.sanitizedValue)

        // Ensure the actual token value is hidden in a toString()
        log.info("{}", accessToken)
        assertFalse(accessToken.toString().contains("short"))
        assertTrue(accessToken.toString().contains("***"))
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SECRET_TOKEN", value = "foo bar") // <<-- Is bad
    fun testAccessTokenEnvBadValue() {
        val accessToken = GitlabConfiguration.AccessToken("MY_SECRET_TOKEN")
        assertFalse(accessToken.isValid())
        assertNull(accessToken.value)
        log.info("{}", accessToken)
        assertEquals("<<<null>>>", accessToken.sanitizedValue)
        assertTrue(
            accessToken.toString()
                .contains("AccessToken could not be loaded: the value from environment variable \"MY_SECRET_TOKEN\" is NOT valid.")
        )
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SECRET_TOKEN", value = "") // <<-- Is bad
    fun testAccessTokenEnvEmptyValue() {
        val accessToken = GitlabConfiguration.AccessToken("MY_SECRET_TOKEN")
        assertFalse(accessToken.isValid())
        assertNull(accessToken.value)
        log.info("{}", accessToken)
        assertEquals("<<<null>>>", accessToken.sanitizedValue)
        assertTrue(
            accessToken.toString()
                .contains("AccessToken could not be loaded: the value from environment variable \"MY_SECRET_TOKEN\" is NOT valid.")
        )
    }

    @Test //    @SetEnvironmentVariable(key = "MY_SECRET_TOKEN", value = "") // <<-- Is bad
    fun testAccessTokenEnvMissing() {
        val accessToken = GitlabConfiguration.AccessToken("MY_SECRET_TOKEN")
        assertFalse(accessToken.isValid())
        assertNull(accessToken.value)
        log.info("{}", accessToken)
        assertEquals("<<<null>>>", accessToken.sanitizedValue)
        assertTrue(
            accessToken.toString()
                .contains("AccessToken could not be loaded: the environment variable \"MY_SECRET_TOKEN\" does not exist.")
        )
    }

    @Test
    fun testAccessTokenEnvNameEmpty() {
        val accessToken = GitlabConfiguration.AccessToken("")
        assertFalse(accessToken.isValid())
        assertNull(accessToken.value)
        log.info("{}", accessToken)
        assertEquals("<<<null>>>", accessToken.sanitizedValue)
        assertTrue(
            accessToken.toString().contains("AccessToken could not be loaded: no environment variable was specified.")
        )
    }

    @Test
    fun testAccessTokenEnvNameBlank() {
        val accessToken = GitlabConfiguration.AccessToken("    ")
        assertFalse(accessToken.isValid())
        assertNull(accessToken.value)
        log.info("{}", accessToken)
        assertEquals("<<<null>>>", accessToken.sanitizedValue)
        assertTrue(
            accessToken.toString()
                .contains("AccessToken could not be loaded: the environment variable name \"    \" is blank.")
        )
    }

    @Test
    fun testAccessTokenEnvNameNull() {
        val accessToken = GitlabConfiguration.AccessToken(null)
        assertFalse(accessToken.isValid())
        assertNull(accessToken.value)
        log.info("{}", accessToken)
        assertEquals("<<<null>>>", accessToken.sanitizedValue)
        assertTrue(
            accessToken.toString().contains("AccessToken could not be loaded: no environment variable was specified.")
        )
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TestGitlabConfigurationAccessToken::class.java)
    }
}
