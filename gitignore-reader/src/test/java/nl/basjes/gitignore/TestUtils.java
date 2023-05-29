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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUtils {
    public static void assertMatch(GitIgnore gitIgnore, String filename) {
        assertTrue(
            gitIgnore.isIgnoredFile(filename),
            "Filename \""+filename+"\" should match but did not.");
    }

    public static void assertMatch(String baseDir, String gitIgnore, String filename) {
        assertTrue(
            new GitIgnore(baseDir, gitIgnore).isIgnoredFile(filename),
            "Filename \""+filename+"\" should match but did not.");
    }

    public static void assertNotMatch(GitIgnore gitIgnore, String filename) {
        assertFalse(
            gitIgnore.isIgnoredFile(filename),
            "Filename \""+filename+"\" should match NOT but did.");
    }

    public static void assertNotMatch(String baseDir, String gitIgnore, String filename) {
        assertFalse(
            new GitIgnore(baseDir, gitIgnore).isIgnoredFile(filename),
            "Filename \""+filename+"\" should match NOT but did.");
    }

}
