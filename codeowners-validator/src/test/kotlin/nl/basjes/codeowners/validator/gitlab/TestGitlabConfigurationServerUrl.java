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

import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration.ServerUrl;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestGitlabConfigurationServerUrl {

    private static final Logger log = LoggerFactory.getLogger(TestGitlabConfigurationServerUrl.class);

    @Test
    @SetEnvironmentVariable(key = "CI_SERVER_URL", value = "https://git.example.nl") // <<-- Is good
    public void testServerUrlDefaultEnvValidValue() {
        ServerUrl serverUrl = new ServerUrl(null, null);
        assertTrue(serverUrl.isValid());
        assertEquals("https://git.example.nl", serverUrl.getValue());
        log.info("{}", serverUrl);
        assertTrue(serverUrl.toString().contains("(via environment variable \"CI_SERVER_URL\")"));
    }

    @Test
    @SetEnvironmentVariable(key = "CI_SERVER_URL", value = "foobar") // <<-- Is bad
    public void testServerUrlDefaultEnvBadValue() {
        ServerUrl serverUrl = new ServerUrl(null, null);
        assertFalse(serverUrl.isValid());
        assertNull(serverUrl.getValue());
        log.info("{}", serverUrl);
        assertTrue(serverUrl.toString().contains("the value from environment variable \"CI_SERVER_URL\" is NOT valid"));
    }

    @Test
    @SetEnvironmentVariable(key = "CI_SERVER_URL", value = "") // <<-- Is bad
    public void testServerUrlDefaultEnvEmptyValue() {
        ServerUrl serverUrl = new ServerUrl(null, null);
        assertFalse(serverUrl.isValid());
        assertNull(serverUrl.getValue());
        log.info("{}", serverUrl);
        assertTrue(serverUrl.toString().contains("the value from environment variable \"CI_SERVER_URL\" is NOT valid"));
    }

    @Test
//    @SetEnvironmentVariable(key = "CI_SERVER_URL", value = "") // <<-- Is bad
    public void testServerUrlDefaultEnvMissing() {
        ServerUrl serverUrl = new ServerUrl(null, null);
        assertFalse(serverUrl.isValid());
        assertNull(serverUrl.getValue());
        log.info("{}", serverUrl);
        assertTrue(serverUrl.toString().contains("the environment variable \"CI_SERVER_URL\" does not exist"));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_SERVER_URL", value = "https://git.example.nl") // <<-- Is good
    public void testServerUrlCustomEnvValidValue() {
        ServerUrl serverUrl = new ServerUrl(null, "MY_SPECIAL_SERVER_URL");
        assertTrue(serverUrl.isValid());
        assertEquals("https://git.example.nl", serverUrl.getValue());
        log.info("{}", serverUrl);
        assertTrue(serverUrl.toString().contains("(via environment variable \"MY_SPECIAL_SERVER_URL\")"));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_SERVER_URL", value = "foobar") // <<-- Is bad
    public void testServerUrlCustomEnvBadValue() {
        ServerUrl serverUrl = new ServerUrl(null, "MY_SPECIAL_SERVER_URL");
        assertFalse(serverUrl.isValid());
        assertNull(serverUrl.getValue());
        log.info("{}", serverUrl);
        assertTrue(serverUrl.toString().contains("the value from environment variable \"MY_SPECIAL_SERVER_URL\" is NOT valid"));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_SERVER_URL", value = "") // <<-- Is bad
    public void testServerUrlCustomEnvEmptyValue() {
        ServerUrl serverUrl = new ServerUrl(null, "MY_SPECIAL_SERVER_URL");
        assertFalse(serverUrl.isValid());
        assertNull(serverUrl.getValue());
        log.info("{}", serverUrl);
        assertTrue(serverUrl.toString().contains("the value from environment variable \"MY_SPECIAL_SERVER_URL\" is NOT valid"));
    }

    @Test
//    @SetEnvironmentVariable(key = "MY_SPECIAL_SERVER_URL", value = "") // <<-- Is bad
    public void testServerUrlCustomEnvMissing() {
        ServerUrl serverUrl = new ServerUrl(null, "MY_SPECIAL_SERVER_URL");
        assertFalse(serverUrl.isValid());
        assertNull(serverUrl.getValue());
        log.info("{}", serverUrl);
        assertTrue(serverUrl.toString().contains("the environment variable \"MY_SPECIAL_SERVER_URL\" does not exist"));
    }

    @Test
    public void testServerUrlCustomEnvNameEmpty() {
        ServerUrl serverUrl = new ServerUrl(null, "");
        assertFalse(serverUrl.isValid());
        assertNull(serverUrl.getValue());
        log.info("{}", serverUrl);
        assertTrue(serverUrl.toString().contains("the environment variable \"CI_SERVER_URL\" does not exist"));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_SERVER_URL", value = "https://git.example.nl") // <<-- Fallback
    public void testServerUrlDirectValidValue() {
        ServerUrl serverUrl = new ServerUrl("https://git.example.com", "MY_SPECIAL_SERVER_URL");
        assertTrue(serverUrl.isValid());
        assertEquals("https://git.example.com", serverUrl.getValue());
        log.info("{}", serverUrl);
        assertTrue(serverUrl.toString().contains("(via property \"gitlab.serverUrl.url\")"));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_SERVER_URL", value = "https://git.example.nl") // <<-- Fallback
    public void testServerUrlDirectBadValue() {
        ServerUrl serverUrl = new ServerUrl("foobar", "MY_SPECIAL_SERVER_URL");
        // Do NOT use the fallback because that would cause confusion with the person configuring it.
        assertFalse(serverUrl.isValid());
        assertNull(serverUrl.getValue());
        log.info("{}", serverUrl);
        assertTrue(serverUrl.toString().contains("the value found using property \"gitlab.serverUrl.url\" is not valid"));
    }

    @Test
    @SetEnvironmentVariable(key = "CI_SERVER_URL", value = "https://git.example.nl") // <<-- Fallback
    public void testServerUrlDirectEmptyValueFallBackDefault() {
        ServerUrl serverUrl = new ServerUrl("", null);
        assertTrue(serverUrl.isValid());
        assertEquals("https://git.example.nl", serverUrl.getValue());
        log.info("{}", serverUrl);
        assertTrue(serverUrl.toString().contains("(via environment variable \"CI_SERVER_URL\")"));
    }

    @Test
    @SetEnvironmentVariable(key = "CI_SERVER_URL", value = "https://git.example.nl") // <<-- Fallback
    public void testServerUrlDirectMissingFallBackDefault() {
        ServerUrl serverUrl = new ServerUrl(null, null);
        assertTrue(serverUrl.isValid());
        assertEquals("https://git.example.nl", serverUrl.getValue());
        log.info("{}", serverUrl);
        assertTrue(serverUrl.toString().contains("(via environment variable \"CI_SERVER_URL\")"));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_SERVER_URL", value = "https://git.example.nl") // <<-- Fallback
    public void testServerUrlDirectEmptyValueFallBackCustom() {
        ServerUrl serverUrl = new ServerUrl("", "MY_SPECIAL_SERVER_URL");
        assertTrue(serverUrl.isValid());
        assertEquals("https://git.example.nl", serverUrl.getValue());
        log.info("{}", serverUrl);
        assertTrue(serverUrl.toString().contains("(via environment variable \"MY_SPECIAL_SERVER_URL\")"));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SPECIAL_SERVER_URL", value = "https://git.example.nl") // <<-- Fallback
    public void testServerUrlDirectMissingFallBackCustom() {
        ServerUrl serverUrl = new ServerUrl(null, "MY_SPECIAL_SERVER_URL");
        assertTrue(serverUrl.isValid());
        assertEquals("https://git.example.nl", serverUrl.getValue());
        log.info("{}", serverUrl);
        assertTrue(serverUrl.toString().contains("(via environment variable \"MY_SPECIAL_SERVER_URL\")"));
    }

}
