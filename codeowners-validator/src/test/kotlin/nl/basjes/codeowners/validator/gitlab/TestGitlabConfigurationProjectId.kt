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

class TestGitlabConfigurationProjectId {
    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project") // <<-- Is good
    fun testProjectIdDefaultEnvValidValue() {
        val projectId = GitlabConfiguration.ProjectId(null, null)
        assertTrue(projectId.isValid())
        assertEquals("niels/project", projectId.value)
        log.info("{}", projectId)
        assertEquals("niels/project", projectId.sanitizedValue)
        assertTrue(projectId.toString().contains("(via environment variable \"CI_PROJECT_ID\")"))
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "foo bar") // <<-- Is bad
    fun testProjectIdDefaultEnvBadValue() {
        val projectId = GitlabConfiguration.ProjectId(null, null)
        assertFalse(projectId.isValid())
        assertNull(projectId.value)
        log.info("{}", projectId)
        assertEquals("<<<null>>>", projectId.sanitizedValue)
        assertTrue(
            projectId.toString().contains("the value from environment variable \"CI_PROJECT_ID\" is NOT valid")
        )
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "") // <<-- Is bad
    fun testProjectIdDefaultEnvEmptyValue() {
        val projectId = GitlabConfiguration.ProjectId(null, null)
        assertFalse(projectId.isValid())
        assertNull(projectId.value)
        log.info("{}", projectId)
        assertEquals("<<<null>>>", projectId.sanitizedValue)
        assertTrue(
            projectId.toString().contains("the value from environment variable \"CI_PROJECT_ID\" is NOT valid")
        )
    }

    @Test //    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "") // <<-- Is bad
    fun testProjectIdDefaultEnvMissing() {
        val projectId = GitlabConfiguration.ProjectId(null, null)
        assertFalse(projectId.isValid())
        assertNull(projectId.value)
        log.info("{}", projectId)
        assertEquals("<<<null>>>", projectId.sanitizedValue)
        assertTrue(
            projectId.toString().contains("the environment variable \"CI_PROJECT_ID\" does not exist")
        )
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "niels/project") // <<-- Is good
    fun testProjectIdCustomEnvValidValue() {
        val projectId = GitlabConfiguration.ProjectId(null, "MY_SPECIAL_PROJECT_ID")
        assertTrue(projectId.isValid())
        assertEquals("niels/project", projectId.value)
        log.info("{}", projectId)
        assertEquals("niels/project", projectId.sanitizedValue)
        assertTrue(projectId.toString().contains("(via environment variable \"MY_SPECIAL_PROJECT_ID\")"))
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "foo bar") // <<-- Is bad
    fun testProjectIdCustomEnvBadValue() {
        val projectId = GitlabConfiguration.ProjectId(null, "MY_SPECIAL_PROJECT_ID")
        assertFalse(projectId.isValid())
        assertNull(projectId.value)
        log.info("{}", projectId)
        assertEquals("<<<null>>>", projectId.sanitizedValue)
        assertTrue(
            projectId.toString().contains("the value from environment variable \"MY_SPECIAL_PROJECT_ID\" is NOT valid")
        )
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "") // <<-- Is bad
    fun testProjectIdCustomEnvEmptyValue() {
        val projectId = GitlabConfiguration.ProjectId(null, "MY_SPECIAL_PROJECT_ID")
        assertFalse(projectId.isValid())
        assertNull(projectId.value)
        log.info("{}", projectId)
        assertEquals("<<<null>>>", projectId.sanitizedValue)
        assertTrue(
            projectId.toString().contains("the value from environment variable \"MY_SPECIAL_PROJECT_ID\" is NOT valid")
        )
    }

    @Test //    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "") // <<-- Is bad
    fun testProjectIdCustomEnvMissing() {
        val projectId = GitlabConfiguration.ProjectId(null, "MY_SPECIAL_PROJECT_ID")
        assertFalse(projectId.isValid())
        assertNull(projectId.value)
        log.info("{}", projectId)
        assertEquals("<<<null>>>", projectId.sanitizedValue)
        assertTrue(
            projectId.toString().contains("the environment variable \"MY_SPECIAL_PROJECT_ID\" does not exist")
        )
    }

    @Test
    fun testProjectIdCustomEnvNameEmpty() {
        val projectId = GitlabConfiguration.ProjectId(null, "")
        assertFalse(projectId.isValid())
        assertNull(projectId.value)
        log.info("{}", projectId)
        assertTrue(
            projectId.toString().contains("the environment variable \"CI_PROJECT_ID\" does not exist")
        )
    }

    @Test
    fun testProjectIdCustomEnvNameBlank() {
        val projectId = GitlabConfiguration.ProjectId(null, "    ")
        assertFalse(projectId.isValid())
        assertNull(projectId.value)
        log.info("{}", projectId)
        assertEquals("<<<null>>>", projectId.sanitizedValue)
        assertTrue(projectId.toString().contains("the environment variable name \"    \" is blank."))
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "niels/project") // <<-- Fallback
    fun testProjectIdDirectValidValue() {
        val projectId = GitlabConfiguration.ProjectId("otherproject", "MY_SPECIAL_PROJECT_ID")
        assertTrue(projectId.isValid())
        assertEquals("otherproject", projectId.value)
        log.info("{}", projectId)
        assertEquals("otherproject", projectId.sanitizedValue)
        assertTrue(projectId.toString().contains("(via property \"gitlab.projectId.id\")"))
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "niels/project") // <<-- Fallback
    fun testProjectIdDirectBadValue() {
        val projectId = GitlabConfiguration.ProjectId("foo bar", "MY_SPECIAL_PROJECT_ID")
        // Do NOT use the fallback because that would cause confusion with the person configuring it.
        assertFalse(projectId.isValid())
        assertNull(projectId.value)
        log.info("{}", projectId)
        assertEquals("<<<null>>>", projectId.sanitizedValue)
        assertTrue(
            projectId.toString().contains("the value found using property \"gitlab.projectId.id\" is not valid")
        )
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project") // <<-- Fallback
    fun testProjectIdDirectEmptyValueFallBackDefault() {
        val projectId = GitlabConfiguration.ProjectId("", null)
        assertTrue(projectId.isValid())
        assertEquals("niels/project", projectId.value)
        log.info("{}", projectId)
        assertEquals("niels/project", projectId.sanitizedValue)
        assertTrue(projectId.toString().contains("(via environment variable \"CI_PROJECT_ID\")"))
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project") // <<-- Fallback
    fun testProjectIdDirectMissingFallBackDefault() {
        val projectId = GitlabConfiguration.ProjectId(null, null)
        assertTrue(projectId.isValid())
        assertEquals("niels/project", projectId.value)
        log.info("{}", projectId)
        assertEquals("niels/project", projectId.sanitizedValue)
        assertTrue(projectId.toString().contains("(via environment variable \"CI_PROJECT_ID\")"))
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "niels/project") // <<-- Fallback
    fun testProjectIdDirectEmptyValueFallBackCustom() {
        val projectId = GitlabConfiguration.ProjectId("", "MY_SPECIAL_PROJECT_ID")
        assertTrue(projectId.isValid())
        assertEquals("niels/project", projectId.value)
        log.info("{}", projectId)
        assertEquals("niels/project", projectId.sanitizedValue)
        assertTrue(projectId.toString().contains("(via environment variable \"MY_SPECIAL_PROJECT_ID\")"))
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "niels/project") // <<-- Fallback
    fun testProjectIdDirectMissingFallBackCustom() {
        val projectId = GitlabConfiguration.ProjectId(null, "MY_SPECIAL_PROJECT_ID")
        assertTrue(projectId.isValid())
        assertEquals("niels/project", projectId.value)
        log.info("{}", projectId)
        assertEquals("niels/project", projectId.sanitizedValue)
        assertTrue(projectId.toString().contains("(via environment variable \"MY_SPECIAL_PROJECT_ID\")"))
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TestGitlabConfigurationProjectId::class.java)
    }
}
