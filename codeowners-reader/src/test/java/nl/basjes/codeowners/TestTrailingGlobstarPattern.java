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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.basjes.codeowners;

import org.junit.jupiter.api.Test;

import static nl.basjes.codeowners.TestUtils.assertOwners;

class TestTrailingGlobstarPattern {

    /**
     * Test case for patterns ending with /** (trailing globstar).
     *
     * The pattern "dirname/**" should match in the root and in a subdirectory:
     * - dirname/ (directory itself)
     * - dirname/file (files inside the directory)
     * - dirname/subdir/file (files in subdirectories)
     *
     * But should NOT match in the root and in a subdirectory:
     * - dirname (file at root level with the same name)
     * - dirname.properties (file at root level starting with dirname)
     * - dirname-something (file at root level starting with dirname)
     *
     * This ensures that earlier patterns like "*" can match files that merely
     * start with the directory name, following the "last match wins" rule.
     */

    @Test
    void testTrailingGlobstarMatching() {
        CodeOwners codeOwners = new CodeOwners(
            "*          @mismatch\n" + // Fall back to ensure we always get a response
            "dirname/** @match\n"
        );

        // Match in root
        assertOwners(codeOwners, "dirname/",                            "@match");    // directory itself
        assertOwners(codeOwners, "dirname/file",                        "@match");    // files inside the directory
        assertOwners(codeOwners, "dirname/subdir/file",                 "@match");    // files in subdirectories

        // Match in subdir/
        assertOwners(codeOwners, "subdir/dirname/",                     "@match");    // directory itself
        assertOwners(codeOwners, "subdir/dirname/file",                 "@match");    // files inside the directory
        assertOwners(codeOwners, "subdir/dirname/subdir/file",          "@match");    // files in subdirectories

        // Match in subdir/subdir/
        assertOwners(codeOwners, "subdir/subdir/dirname/",              "@match");    // directory itself
        assertOwners(codeOwners, "subdir/subdir/dirname/file",          "@match");    // files inside the directory
        assertOwners(codeOwners, "subdir/subdir/dirname/subdir/file",   "@match");    // files in subdirectories

        // Do NOT match files with the name in root
        assertOwners(codeOwners, "dirname",                             "@mismatch"); // file with the same name
        assertOwners(codeOwners, "dirname.properties",                  "@mismatch"); // file starting with dirname
        assertOwners(codeOwners, "dirname-something",                   "@mismatch"); // file starting with dirname

        // Do NOT match files with the name in subdir/
        assertOwners(codeOwners, "subdir/dirname",                      "@mismatch"); // file with the same name
        assertOwners(codeOwners, "subdir/dirname.properties",           "@mismatch"); // file starting with dirname
        assertOwners(codeOwners, "subdir/dirname-something",            "@mismatch"); // file starting with dirname

        // Do NOT match files with the name in subdir/subdir/
        assertOwners(codeOwners, "subdir/subdir/dirname",               "@mismatch"); // file with the same name
        assertOwners(codeOwners, "subdir/subdir/dirname.properties",    "@mismatch"); // file starting with dirname
        assertOwners(codeOwners, "subdir/subdir/dirname-something",     "@mismatch"); // file starting with dirname
    }

}
