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

class TestGitlabConfigurationServerUrl {
    @Test
    @SetEnvironmentVariable(key = "CI_SERVER_URL", value = "https://git.example.nl") // <<-- Is good
    fun testServerUrlDefaultEnvValidValue() {
        val serverUrl = GitlabConfiguration.ServerUrl(null, null)
        assertTrue(serverUrl.isValid())
        assertEquals("https://git.example.nl", serverUrl.value)
        log.info("{}", serverUrl)
        assertEquals("https://git.example.nl", serverUrl.sanitizedValue)
        assertTrue(serverUrl.toString().contains("(via environment variable \"CI_SERVER_URL\")"))
    }

    @Test
    @SetEnvironmentVariable(key = "CI_SERVER_URL", value = "foobar") // <<-- Is bad
    fun testServerUrlDefaultEnvBadValue() {
        val serverUrl = GitlabConfiguration.ServerUrl(null, null)
        assertFalse(serverUrl.isValid())
        assertNull(serverUrl.value)
        log.info("{}", serverUrl)
        assertEquals("<<<null>>>", serverUrl.sanitizedValue)
        assertTrue(
            serverUrl.toString().contains("the value from environment variable \"CI_SERVER_URL\" is NOT valid")
        )
    }

    @Test
    @SetEnvironmentVariable(key = "CI_SERVER_URL", value = "") // <<-- Is bad
    fun testServerUrlDefaultEnvEmptyValue() {
        val serverUrl = GitlabConfiguration.ServerUrl(null, null)
        assertFalse(serverUrl.isValid())
        assertNull(serverUrl.value)
        log.info("{}", serverUrl)
        assertEquals("<<<null>>>", serverUrl.sanitizedValue)
        assertTrue(
            serverUrl.toString().contains("the value from environment variable \"CI_SERVER_URL\" is NOT valid")
        )
    }

    @Test //    @SetEnvironmentVariable(key = "CI_SERVER_URL", value = "") // <<-- Is bad
    fun testServerUrlDefaultEnvMissing() {
        val serverUrl = GitlabConfiguration.ServerUrl(null, null)
        assertFalse(serverUrl.isValid())
        assertNull(serverUrl.value)
        log.info("{}", serverUrl)
        assertEquals("<<<null>>>", serverUrl.sanitizedValue)
        assertTrue(
            serverUrl.toString().contains("the environment variable \"CI_SERVER_URL\" does not exist")
        )
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_SERVER_URL", value = "https://git.example.nl") // <<-- Is good
    fun testServerUrlCustomEnvValidValue() {
        val serverUrl = GitlabConfiguration.ServerUrl(null, "MY_SPECIAL_SERVER_URL")
        assertTrue(serverUrl.isValid())
        assertEquals("https://git.example.nl", serverUrl.value)
        log.info("{}", serverUrl)
        assertEquals("https://git.example.nl", serverUrl.sanitizedValue)
        assertTrue(serverUrl.toString().contains("(via environment variable \"MY_SPECIAL_SERVER_URL\")"))
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_SERVER_URL", value = "foobar") // <<-- Is bad
    fun testServerUrlCustomEnvBadValue() {
        val serverUrl = GitlabConfiguration.ServerUrl(null, "MY_SPECIAL_SERVER_URL")
        assertFalse(serverUrl.isValid())
        assertNull(serverUrl.value)
        log.info("{}", serverUrl)
        assertEquals("<<<null>>>", serverUrl.sanitizedValue)
        assertTrue(
            serverUrl.toString().contains("the value from environment variable \"MY_SPECIAL_SERVER_URL\" is NOT valid")
        )
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_SERVER_URL", value = "") // <<-- Is bad
    fun testServerUrlCustomEnvEmptyValue() {
        val serverUrl = GitlabConfiguration.ServerUrl(null, "MY_SPECIAL_SERVER_URL")
        assertFalse(serverUrl.isValid())
        assertNull(serverUrl.value)
        log.info("{}", serverUrl)
        assertEquals("<<<null>>>", serverUrl.sanitizedValue)
        assertTrue(
            serverUrl.toString().contains("the value from environment variable \"MY_SPECIAL_SERVER_URL\" is NOT valid")
        )
    }

    @Test //    @SetEnvironmentVariable(key = "MY_SPECIAL_SERVER_URL", value = "") // <<-- Is bad
    fun testServerUrlCustomEnvMissing() {
        val serverUrl = GitlabConfiguration.ServerUrl(null, "MY_SPECIAL_SERVER_URL")
        assertFalse(serverUrl.isValid())
        assertNull(serverUrl.value)
        log.info("{}", serverUrl)
        assertEquals("<<<null>>>", serverUrl.sanitizedValue)
        assertTrue(
            serverUrl.toString().contains("the environment variable \"MY_SPECIAL_SERVER_URL\" does not exist")
        )
    }

    @Test
    fun testServerUrlCustomEnvNameEmpty() {
        val serverUrl = GitlabConfiguration.ServerUrl(null, "")
        assertFalse(serverUrl.isValid())
        assertNull(serverUrl.value)
        log.info("{}", serverUrl)
        assertEquals("<<<null>>>", serverUrl.sanitizedValue)
        assertTrue(
            serverUrl.toString().contains("the environment variable \"CI_SERVER_URL\" does not exist")
        )
    }

    @Test
    fun testServerUrlCustomEnvNameBlank() {
        val serverUrl = GitlabConfiguration.ServerUrl(null, "    ")
        assertFalse(serverUrl.isValid())
        assertNull(serverUrl.value)
        log.info("{}", serverUrl)
        assertEquals("<<<null>>>", serverUrl.sanitizedValue)
        assertTrue(serverUrl.toString().contains("the environment variable name \"    \" is blank."))
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_SERVER_URL", value = "https://git.example.nl") // <<-- Fallback
    fun testServerUrlDirectValidValue() {
        val serverUrl = GitlabConfiguration.ServerUrl("https://git.example.com", "MY_SPECIAL_SERVER_URL")
        assertTrue(serverUrl.isValid())
        assertEquals("https://git.example.com", serverUrl.value)
        log.info("{}", serverUrl)
        assertEquals("https://git.example.com", serverUrl.sanitizedValue)
        assertTrue(serverUrl.toString().contains("(via property \"gitlab.serverUrl.url\")"))
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_SERVER_URL", value = "https://git.example.nl") // <<-- Fallback
    fun testServerUrlDirectBadValue() {
        val serverUrl = GitlabConfiguration.ServerUrl("foobar", "MY_SPECIAL_SERVER_URL")
        // Do NOT use the fallback because that would cause confusion with the person configuring it.
        assertFalse(serverUrl.isValid())
        assertNull(serverUrl.value)
        log.info("{}", serverUrl)
        assertEquals("<<<null>>>", serverUrl.sanitizedValue)
        assertTrue(
            serverUrl.toString().contains("the value found using property \"gitlab.serverUrl.url\" is not valid")
        )
    }

    @Test
    @SetEnvironmentVariable(key = "CI_SERVER_URL", value = "https://git.example.nl") // <<-- Fallback
    fun testServerUrlDirectEmptyValueFallBackDefault() {
        val serverUrl = GitlabConfiguration.ServerUrl("", null)
        assertTrue(serverUrl.isValid())
        assertEquals("https://git.example.nl", serverUrl.value)
        log.info("{}", serverUrl)
        assertEquals("https://git.example.nl", serverUrl.sanitizedValue)
        assertTrue(serverUrl.toString().contains("(via environment variable \"CI_SERVER_URL\")"))
    }

    @Test
    @SetEnvironmentVariable(key = "CI_SERVER_URL", value = "https://git.example.nl") // <<-- Fallback
    fun testServerUrlDirectMissingFallBackDefault() {
        val serverUrl = GitlabConfiguration.ServerUrl(null, null)
        assertTrue(serverUrl.isValid())
        assertEquals("https://git.example.nl", serverUrl.value)
        log.info("{}", serverUrl)
        assertEquals("https://git.example.nl", serverUrl.sanitizedValue)
        assertTrue(serverUrl.toString().contains("(via environment variable \"CI_SERVER_URL\")"))
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_SERVER_URL", value = "https://git.example.nl") // <<-- Fallback
    fun testServerUrlDirectEmptyValueFallBackCustom() {
        val serverUrl = GitlabConfiguration.ServerUrl("", "MY_SPECIAL_SERVER_URL")
        assertTrue(serverUrl.isValid())
        assertEquals("https://git.example.nl", serverUrl.value)
        log.info("{}", serverUrl)
        assertEquals("https://git.example.nl", serverUrl.sanitizedValue)
        assertTrue(serverUrl.toString().contains("(via environment variable \"MY_SPECIAL_SERVER_URL\")"))
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_SERVER_URL", value = "https://git.example.nl") // <<-- Fallback
    fun testServerUrlDirectMissingFallBackCustom() {
        val serverUrl = GitlabConfiguration.ServerUrl(null, "MY_SPECIAL_SERVER_URL")
        assertTrue(serverUrl.isValid())
        assertEquals("https://git.example.nl", serverUrl.value)
        log.info("{}", serverUrl)
        assertEquals("https://git.example.nl", serverUrl.sanitizedValue)
        assertTrue(serverUrl.toString().contains("(via environment variable \"MY_SPECIAL_SERVER_URL\")"))
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TestGitlabConfigurationServerUrl::class.java)
    }
}
