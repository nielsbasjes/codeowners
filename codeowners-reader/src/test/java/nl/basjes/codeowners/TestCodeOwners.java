/*
 * CodeOwners Tools
 * Copyright (C) 2023 Niels Basjes
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

package nl.basjes.codeowners;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static nl.basjes.codeowners.TestUtils.assertOwners;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestCodeOwners {

    private static final Logger LOG = LoggerFactory.getLogger(TestCodeOwners.class);

    private void runChecks(CodeOwners codeOwners) {
        assertOwners(codeOwners, "Foo.txt","@code","@multiple","@owners", "@dev-team");
        assertOwners(codeOwners, "Foo.rb","@ruby-owner", "@dev-team");
        assertOwners(codeOwners, "#file_with_pound.rb","@owner-file-with-pound", "@dev-team");
    }

    @Test
    void testDotStarCase() {
        CodeOwners codeOwners = new CodeOwners(
            "/foo/.* @user1\n" + // Intended to match '/foo/.bar' NOT '/foo/' and NOT '/foo/foo/.bar'
            "*.xml @user2\n"
        );
//        codeOwners.setVerbose(true);
//        LOG.info("CODEOWNERS:\n{}", codeOwners);
        assertOwners(codeOwners, "/foo/.foo", "@user1");
        assertOwners(codeOwners, "/foo/.foo/bar", "@user1");
        assertOwners(codeOwners, "/foo/foo/.bar"); // No users
        assertOwners(codeOwners, "/foo/xfoo"); // No users
        assertOwners(codeOwners, "/foo/xfoo/bar");
        assertOwners(codeOwners, "/foo/.foo/bar.xml", "@user2");
        assertOwners(codeOwners, "/foo/foo"); // No users
        assertOwners(codeOwners, "/foo/foo/bar.xml", "@user2");
    }

    @Test
    void testMidStarCase() {
        CodeOwners codeOwners = new CodeOwners(
            "/tool-*/ @user1\n" + // Intended to match '/tool-library/bar.txt'
            "*.xml @user2\n"
        );
        codeOwners.setVerbose(true);
        LOG.info("CODEOWNERS:\n{}", codeOwners);
        assertOwners(codeOwners, "/tool-app/bar.txt", "@user1");
        assertOwners(codeOwners, "/tool-app/foo/bar.txt", "@user1");
        assertOwners(codeOwners, "/tool-app/bar.xml", "@user2");
        assertOwners(codeOwners, "/tool-app/foo/bar.xml", "@user2");
        assertOwners(codeOwners, "/bar.txt");
        assertOwners(codeOwners, "/bar.xml", "@user2");
    }

    @Test
    void testCodeOwnersToStringRoundTrip() throws IOException {
        URL url = this.getClass()
            .getClassLoader()
            .getResource("CODEOWNERS_base");

        assertNotNull(url);

        CodeOwners codeOwners = new CodeOwners(new File(url.getFile()));
//        LOG.info("\n{}", codeOwners);
        runChecks(codeOwners);
        // Now reparse the toString output... (NORMAL)
        codeOwners.setVerbose(false);
        CodeOwners codeOwners2 = new CodeOwners(codeOwners.toString());
        runChecks(codeOwners2);

        // Now reparse the toString output... (VERBOSE)
        codeOwners.setVerbose(true);
        CodeOwners codeOwners3 = new CodeOwners(codeOwners.toString());
        runChecks(codeOwners3);

    }

    @Test
    void testToString() {
        CodeOwners codeOwners = new CodeOwners(
            "/tool-*/ @user1\n" +
            "*.xml @user2\n"
        );
        codeOwners.setVerbose(true);
        assertEquals(
            "# CODEOWNERS file:\n" +
            "# Regex used for the next rule:   ^/tool-.*/\n" +
            "/tool-*/ @user1\n" +
            "# Regex used for the next rule:   .*\\.xml(/|$)\n" +
            "*.xml @user2\n",
            codeOwners.toString());

        codeOwners.setVerbose(false);
        assertEquals(
            "# CODEOWNERS file:\n" +
            "/tool-*/ @user1\n" +
            "*.xml @user2\n",
            codeOwners.toString());
    }

    @Test
    void testToStringEmpty() {
        CodeOwners codeOwners = new CodeOwners(
            "# Nothing here, only comments\n"
        );
        codeOwners.setVerbose(true);
        assertEquals(
            "# CODEOWNERS file:\n" +
            "# No CODEOWNER rules were defined.\n",
            codeOwners.toString());

        codeOwners.setVerbose(false);
        assertEquals(
            "# CODEOWNERS file:\n" +
            "# No CODEOWNER rules were defined.\n",
            codeOwners.toString());
    }


}
