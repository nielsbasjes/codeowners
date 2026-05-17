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
package nl.basjes.codeowners.validator.gitlab;

import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration.ProjectId;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestGitlabConfigurationProjectId {

    private static final Logger log = LoggerFactory.getLogger(TestGitlabConfigurationProjectId.class);

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project") // <<-- Is good
    public void testProjectIdDefaultEnvValidValue() {
        ProjectId projectId = new ProjectId(null, null);
        assertTrue(projectId.isValid());
        assertEquals("niels/project", projectId.getValue());
        log.info("{}", projectId);
        assertTrue(projectId.toString().contains("(via environment variable \"CI_PROJECT_ID\")"));
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "foo bar") // <<-- Is bad
    public void testProjectIdDefaultEnvBadValue() {
        ProjectId projectId = new ProjectId(null, null);
        assertFalse(projectId.isValid());
        assertNull(projectId.getValue());
        log.info("{}", projectId);
        assertTrue(projectId.toString().contains("the value from environment variable \"CI_PROJECT_ID\" is NOT valid"));
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "") // <<-- Is bad
    public void testProjectIdDefaultEnvEmptyValue() {
        ProjectId projectId = new ProjectId(null, null);
        assertFalse(projectId.isValid());
        assertNull(projectId.getValue());
        log.info("{}", projectId);
        assertTrue(projectId.toString().contains("the value from environment variable \"CI_PROJECT_ID\" is NOT valid"));
    }

    @Test
//    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "") // <<-- Is bad
    public void testProjectIdDefaultEnvMissing() {
        ProjectId projectId = new ProjectId(null, null);
        assertFalse(projectId.isValid());
        assertNull(projectId.getValue());
        log.info("{}", projectId);
        assertTrue(projectId.toString().contains("the environment variable \"CI_PROJECT_ID\" does not exist"));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "niels/project") // <<-- Is good
    public void testProjectIdCustomEnvValidValue() {
        ProjectId projectId = new ProjectId(null, "MY_SPECIAL_PROJECT_ID");
        assertTrue(projectId.isValid());
        assertEquals("niels/project", projectId.getValue());
        log.info("{}", projectId);
        assertTrue(projectId.toString().contains("(via environment variable \"MY_SPECIAL_PROJECT_ID\")"));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "foo bar") // <<-- Is bad
    public void testProjectIdCustomEnvBadValue() {
        ProjectId projectId = new ProjectId(null, "MY_SPECIAL_PROJECT_ID");
        assertFalse(projectId.isValid());
        assertNull(projectId.getValue());
        log.info("{}", projectId);
        assertTrue(projectId.toString().contains("the value from environment variable \"MY_SPECIAL_PROJECT_ID\" is NOT valid"));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "") // <<-- Is bad
    public void testProjectIdCustomEnvEmptyValue() {
        ProjectId projectId = new ProjectId(null, "MY_SPECIAL_PROJECT_ID");
        assertFalse(projectId.isValid());
        assertNull(projectId.getValue());
        log.info("{}", projectId);
        assertTrue(projectId.toString().contains("the value from environment variable \"MY_SPECIAL_PROJECT_ID\" is NOT valid"));
    }

    @Test
//    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "") // <<-- Is bad
    public void testProjectIdCustomEnvMissing() {
        ProjectId projectId = new ProjectId(null, "MY_SPECIAL_PROJECT_ID");
        assertFalse(projectId.isValid());
        assertNull(projectId.getValue());
        log.info("{}", projectId);
        assertTrue(projectId.toString().contains("the environment variable \"MY_SPECIAL_PROJECT_ID\" does not exist"));
    }

    @Test
    public void testProjectIdCustomEnvNameEmpty() {
        ProjectId projectId = new ProjectId(null, "");
        assertFalse(projectId.isValid());
        assertNull(projectId.getValue());
        log.info("{}", projectId);
        assertTrue(projectId.toString().contains("the environment variable \"CI_PROJECT_ID\" does not exist"));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "niels/project") // <<-- Fallback
    public void testProjectIdDirectValidValue() {
        ProjectId projectId = new ProjectId("otherproject", "MY_SPECIAL_PROJECT_ID");
        assertTrue(projectId.isValid());
        assertEquals("otherproject", projectId.getValue());
        log.info("{}", projectId);
        assertTrue(projectId.toString().contains("(via property \"gitlab.projectId.id\")"));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "niels/project") // <<-- Fallback
    public void testProjectIdDirectBadValue() {
        ProjectId projectId = new ProjectId("foo bar", "MY_SPECIAL_PROJECT_ID");
        // Do NOT use the fallback because that would cause confusion with the person configuring it.
        assertFalse(projectId.isValid());
        assertNull(projectId.getValue());
        log.info("{}", projectId);
        assertTrue(projectId.toString().contains("the value found using property \"gitlab.projectId.id\" is not valid"));
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project") // <<-- Fallback
    public void testProjectIdDirectEmptyValueFallBackDefault() {
        ProjectId projectId = new ProjectId("", null);
        assertTrue(projectId.isValid());
        assertEquals("niels/project", projectId.getValue());
        log.info("{}", projectId);
        assertTrue(projectId.toString().contains("(via environment variable \"CI_PROJECT_ID\")"));
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project") // <<-- Fallback
    public void testProjectIdDirectMissingFallBackDefault() {
        ProjectId projectId = new ProjectId(null, null);
        assertTrue(projectId.isValid());
        assertEquals("niels/project", projectId.getValue());
        log.info("{}", projectId);
        assertTrue(projectId.toString().contains("(via environment variable \"CI_PROJECT_ID\")"));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "niels/project") // <<-- Fallback
    public void testProjectIdDirectEmptyValueFallBackCustom() {
        ProjectId projectId = new ProjectId("", "MY_SPECIAL_PROJECT_ID");
        assertTrue(projectId.isValid());
        assertEquals("niels/project", projectId.getValue());
        log.info("{}", projectId);
        assertTrue(projectId.toString().contains("(via environment variable \"MY_SPECIAL_PROJECT_ID\")"));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "niels/project") // <<-- Fallback
    public void testProjectIdDirectMissingFallBackCustom() {
        ProjectId projectId = new ProjectId(null, "MY_SPECIAL_PROJECT_ID");
        assertTrue(projectId.isValid());
        assertEquals("niels/project", projectId.getValue());
        log.info("{}", projectId);
        assertTrue(projectId.toString().contains("(via environment variable \"MY_SPECIAL_PROJECT_ID\")"));
    }

}
