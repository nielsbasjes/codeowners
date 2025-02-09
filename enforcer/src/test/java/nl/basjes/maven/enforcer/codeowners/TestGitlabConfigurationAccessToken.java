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

import nl.basjes.maven.enforcer.codeowners.GitlabConfiguration.AccessToken;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestGitlabConfigurationAccessToken {

    @Test
    @SetEnvironmentVariable(key = "MY_SECRET_TOKEN", value = "gltst-mySecretToken") // <<-- Is good
    public void testAccessTokenEnvValidValue() {
        AccessToken accessToken = new AccessToken("MY_SECRET_TOKEN");
        assertTrue(accessToken.isValid());
        assertEquals("gltst-mySecretToken", accessToken.getValue());

        // Ensure the actual token value is hidden in a toString()
        assertFalse(accessToken.toString().contains("gltst-mySecretToken"));
        assertTrue(accessToken.toString().contains("gltst-*****en"));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SECRET_TOKEN", value = "short") // <<-- Is good
    public void testAccessTokenEnvValidValueShortToken() {
        AccessToken accessToken = new AccessToken("MY_SECRET_TOKEN");
        assertTrue(accessToken.isValid());
        assertEquals("short", accessToken.getValue());

        // Ensure the actual token value is hidden in a toString()
        assertFalse(accessToken.toString().contains("short"));
        assertTrue(accessToken.toString().contains("***"));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SECRET_TOKEN", value = "foo bar") // <<-- Is bad
    public void testAccessTokenEnvBadValue() {
        AccessToken accessToken = new AccessToken("MY_SECRET_TOKEN");
        assertFalse(accessToken.isValid());
        assertNull(accessToken.getValue());
        assertTrue(accessToken.toString().contains("AccessToken found via environment variable MY_SECRET_TOKEN is NOT valid."));
    }

    @Test
    @SetEnvironmentVariable(key = "MY_SECRET_TOKEN", value = "") // <<-- Is bad
    public void testAccessTokenEnvEmptyValue() {
        AccessToken accessToken = new AccessToken("MY_SECRET_TOKEN");
        assertFalse(accessToken.isValid());
        assertNull(accessToken.getValue());
        assertTrue(accessToken.toString().contains("AccessToken found via environment variable MY_SECRET_TOKEN is NOT valid."));
    }

    @Test
//    @SetEnvironmentVariable(key = "MY_SECRET_TOKEN", value = "") // <<-- Is bad
    public void testAccessTokenEnvMissing() {
        AccessToken accessToken = new AccessToken("MY_SECRET_TOKEN");
        assertFalse(accessToken.isValid());
        assertNull(accessToken.getValue());
        assertTrue(accessToken.toString().contains("AccessToken found via environment variable MY_SECRET_TOKEN is NOT valid."));
    }

    @Test
    public void testAccessTokenEnvNameEmpty() {
        AccessToken accessToken = new AccessToken("");
        assertFalse(accessToken.isValid());
        assertNull(accessToken.getValue());
        assertTrue(accessToken.toString().contains("AccessToken found via invalid environment variable \"null\" is NOT valid."));
    }

    @Test
    public void testAccessTokenEnvNameNull() {
        AccessToken accessToken = new AccessToken(null);
        assertFalse(accessToken.isValid());
        assertNull(accessToken.getValue());
        assertTrue(accessToken.toString().contains("AccessToken found via invalid environment variable \"null\" is NOT valid."));
    }

}
