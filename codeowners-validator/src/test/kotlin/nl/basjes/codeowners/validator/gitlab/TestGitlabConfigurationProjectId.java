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

import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration.ProjectId;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestGitlabConfigurationProjectId {

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project") // <<-- Is good
    public void testProjectIdDefaultEnvValidValue() {
        ProjectId ProjectId = new ProjectId(null, null);
        assertTrue(ProjectId.isValid());
        assertEquals("niels/project", ProjectId.getValue());
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "foo bar") // <<-- Is bad
    public void testProjectIdDefaultEnvBadValue() {
        ProjectId ProjectId = new ProjectId(null, null);
        assertFalse(ProjectId.isValid());
        assertNull(ProjectId.getValue());
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "") // <<-- Is bad
    public void testProjectIdDefaultEnvEmptyValue() {
        ProjectId ProjectId = new ProjectId(null, null);
        assertFalse(ProjectId.isValid());
        assertNull(ProjectId.getValue());
    }

    @Test
//    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "") // <<-- Is bad
    public void testProjectIdDefaultEnvMissing() {
        ProjectId ProjectId = new ProjectId(null, null);
        assertFalse(ProjectId.isValid());
        assertNull(ProjectId.getValue());
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "niels/project") // <<-- Is good
    public void testProjectIdCustomEnvValidValue() {
        ProjectId ProjectId = new ProjectId(null, "MY_SPECIAL_PROJECT_ID");
        assertTrue(ProjectId.isValid());
        assertEquals("niels/project", ProjectId.getValue());
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "foo bar") // <<-- Is bad
    public void testProjectIdCustomEnvBadValue() {
        ProjectId ProjectId = new ProjectId(null, "MY_SPECIAL_PROJECT_ID");
        assertFalse(ProjectId.isValid());
        assertNull(ProjectId.getValue());
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "") // <<-- Is bad
    public void testProjectIdCustomEnvEmptyValue() {
        ProjectId ProjectId = new ProjectId(null, "MY_SPECIAL_PROJECT_ID");
        assertFalse(ProjectId.isValid());
        assertNull(ProjectId.getValue());
    }

    @Test
//    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "") // <<-- Is bad
    public void testProjectIdCustomEnvMissing() {
        ProjectId ProjectId = new ProjectId(null, "MY_SPECIAL_PROJECT_ID");
        assertFalse(ProjectId.isValid());
        assertNull(ProjectId.getValue());
    }

    @Test
    public void testProjectIdCustomEnvNameEmpty() {
        ProjectId ProjectId = new ProjectId(null, "");
        assertFalse(ProjectId.isValid());
        assertNull(ProjectId.getValue());
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "niels/project") // <<-- Fallback
    public void testProjectIdDirectValidValue() {
        ProjectId ProjectId = new ProjectId("otherproject", "MY_SPECIAL_PROJECT_ID");
        assertTrue(ProjectId.isValid());
        assertEquals("otherproject", ProjectId.getValue());
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "niels/project") // <<-- Fallback
    public void testProjectIdDirectBadValue() {
        ProjectId ProjectId = new ProjectId("foo bar", "MY_SPECIAL_PROJECT_ID");
        // Do NOT use the fallback because that would cause confusion with the person configuring it.
        assertFalse(ProjectId.isValid());
        assertNull(ProjectId.getValue());
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project") // <<-- Fallback
    public void testProjectIdDirectEmptyValueFallBackDefault() {
        ProjectId ProjectId = new ProjectId("", null);
        assertTrue(ProjectId.isValid());
        assertEquals("niels/project", ProjectId.getValue());
    }

    @Test
    @SetEnvironmentVariable(key = "CI_PROJECT_ID", value = "niels/project") // <<-- Fallback
    public void testProjectIdDirectMissingFallBackDefault() {
        ProjectId ProjectId = new ProjectId(null, null);
        assertTrue(ProjectId.isValid());
        assertEquals("niels/project", ProjectId.getValue());
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "niels/project") // <<-- Fallback
    public void testProjectIdDirectEmptyValueFallBackCustom() {
        ProjectId ProjectId = new ProjectId("", "MY_SPECIAL_PROJECT_ID");
        assertTrue(ProjectId.isValid());
        assertEquals("niels/project", ProjectId.getValue());
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_PROJECT_ID", value = "niels/project") // <<-- Fallback
    public void testProjectIdDirectMissingFallBackCustom() {
        ProjectId ProjectId = new ProjectId(null, "MY_SPECIAL_PROJECT_ID");
        assertTrue(ProjectId.isValid());
        assertEquals("niels/project", ProjectId.getValue());
    }

}
