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

package nl.basjes.gitignore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUtils {

    public static void assertIgnore(String baseDir, String gitIgnore, String... filenames) {
        assertIgnore(new GitIgnore(baseDir, gitIgnore), filenames);
    }

    public static void assertIgnore(GitIgnore gitIgnore, String ... filenames) {
        for (String filename : filenames) {
            __assertIgnore(gitIgnore, filename);
        }
    }

    private static void __assertIgnore(GitIgnore gitIgnore, String filename) {
        assertSame(Boolean.TRUE,
            gitIgnore.isIgnoredFile(filename),
            "Filename \"" + filename + "\" should match but did not.\n" + gitIgnore);
        assertTrue(gitIgnore.ignoreFile(filename),
            "Filename \"" + filename + "\" should match but did not.\n" + gitIgnore);
        assertFalse(gitIgnore.keepFile(filename),
            "Filename \"" + filename + "\" should match but did not.\n" + gitIgnore);

        // Same but now with a windows path separator
        String wFilename = windowsFileName(filename);
        assertSame(Boolean.TRUE,
            gitIgnore.isIgnoredFile(wFilename),
            "Filename \"" + wFilename + "\" should match but did not.\n" + gitIgnore);
        assertTrue(gitIgnore.ignoreFile(wFilename),
            "Filename \"" + wFilename + "\" should match but did not.\n" + gitIgnore);
        assertFalse(gitIgnore.keepFile(wFilename),
            "Filename \"" + wFilename + "\" should match but did not.\n" + gitIgnore);
    }

    public static void assertNotIgnore(String baseDir, String gitIgnore, String... filenames) {
        assertNotIgnore(new GitIgnore(baseDir, gitIgnore), filenames);
    }

    public static void assertNotIgnore(GitIgnore gitIgnore, String ... filenames) {
        for (String filename : filenames) {
            __assertNotIgnore(gitIgnore, filename);
        }
    }

    private static void __assertNotIgnore(GitIgnore gitIgnore, String filename) {
        Boolean isIgnoredFile = gitIgnore.isIgnoredFile(filename);
        assertTrue(
            isIgnoredFile == null || isIgnoredFile == Boolean.FALSE,
            "Filename \""+filename+"\" should NOT match but did.\n" + gitIgnore);
        assertFalse(gitIgnore.ignoreFile(filename),
            "Filename \""+filename+"\" should NOT match but did.\n" + gitIgnore);
        assertTrue(gitIgnore.keepFile(filename),
            "Filename \""+filename+"\" should NOT match but did.\n" + gitIgnore);

        // Same but now with a windows path separator
        String wFilename = windowsFileName(filename);
        Boolean wIsIgnoredFile = gitIgnore.isIgnoredFile(wFilename);
        assertTrue(
            wIsIgnoredFile == null || wIsIgnoredFile == Boolean.FALSE,
            "Filename \""+wFilename+"\" should NOT match but did.\n" + gitIgnore);
        assertFalse(gitIgnore.ignoreFile(wFilename),
            "Filename \""+wFilename+"\" should NOT match but did.\n" + gitIgnore);
        assertTrue(gitIgnore.keepFile(wFilename),
            "Filename \""+wFilename+"\" should NOT match but did.\n" + gitIgnore);
    }


    public static void assertNullMatch(GitIgnore gitIgnore, String filename) {
        assertNull(
            gitIgnore.isIgnoredFile(filename),
            "Filename \""+filename+"\" should NOT match but did.");

        // Same but now with a windows path separator
        String wFilename = windowsFileName(filename);
        assertNull(
            gitIgnore.isIgnoredFile(wFilename),
            "Filename \""+wFilename+"\" should NOT match but did.");
    }

    public static void assertNullMatch(String baseDir, String gitIgnore, String filename) {
        assertNullMatch(new GitIgnore(baseDir, gitIgnore), filename);
    }

    public static String windowsFileName(String filename) {
        return filename.replace("/", "\\");
    }

    public static void verifyGeneratedRegex(String baseDir, String gitIgnoreContent, String expectedRegex) {
        GitIgnore gitIgnore = new GitIgnore(baseDir, gitIgnoreContent);
        List<GitIgnore.IgnoreRule> ignoreRules = gitIgnore.getIgnoreRules();
        assertEquals(1, ignoreRules.size());
        GitIgnore.IgnoreRule ignoreRule = ignoreRules.get(0);
        assertEquals(expectedRegex, ignoreRule.getIgnorePattern().pattern(), "Incorrect regex generated");
    }

}
