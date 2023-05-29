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

package nl.basjes.gitignore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUtils {
    public static void assertIgnore(GitIgnore gitIgnore, String filename) {
        assertTrue(
            gitIgnore.isIgnoredFile(filename),
            "Filename \""+filename+"\" should match but did not.");
    }

    public static void assertIgnore(String baseDir, String gitIgnore, String filename) {
        assertIgnore(new GitIgnore(baseDir, gitIgnore), filename);
    }

    public static void assertNotIgnore(GitIgnore gitIgnore, String filename) {
        Boolean isIgnoredFile = gitIgnore.isIgnoredFile(filename);
        assertTrue(
            isIgnoredFile == null || isIgnoredFile == Boolean.FALSE,
            "Filename \""+filename+"\" should NOT match but did.");
    }

    public static void assertNotIgnore(String baseDir, String gitIgnore, String filename) {
        assertNotIgnore(new GitIgnore(baseDir, gitIgnore), filename);
    }

    public static void assertNullMatch(GitIgnore gitIgnore, String filename) {
        assertNull(
            gitIgnore.isIgnoredFile(filename),
            "Filename \""+filename+"\" should NOT match but did.");
    }

    public static void assertNullMatch(String baseDir, String gitIgnore, String filename) {
        assertNullMatch(new GitIgnore(baseDir, gitIgnore), filename);
    }

}
