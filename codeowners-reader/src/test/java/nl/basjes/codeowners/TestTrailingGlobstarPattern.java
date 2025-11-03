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
     * The pattern "dirname/**" should only match:
     * - dirname/ (directory itself)
     * - dirname/file (files inside the directory)
     * - dirname/subdir/file (files in subdirectories)
     *
     * But should NOT match:
     * - dirname (file at root level with the same name)
     * - dirname.properties (file at root level starting with dirname)
     * - dirname-something (file at root level starting with dirname)
     *
     * This ensures that earlier patterns like "*" can match files that merely
     * start with the directory name, following the "last match wins" rule.
     */
    @Test
    void shouldNotMatchFileWithSimilarNameAtRoot() {
        CodeOwners codeOwners = new CodeOwners(
            "* @org/team\n" +
            "gradle/**\n"
        );

        assertOwners(codeOwners, "gradle.properties", "@org/team");
    }

    @Test
    void shouldNotMatchFileNamedExactlyLikeDirectory() {
        CodeOwners codeOwners = new CodeOwners(
            "* @org/team\n" +
            "gradle/**\n"
        );

        assertOwners(codeOwners, "gradle", "@org/team");
    }

    @Test
    void shouldNotMatchFileWithDirectoryPrefix() {
        CodeOwners codeOwners = new CodeOwners(
            "* @org/team\n" +
            "gradle/**\n"
        );

        assertOwners(codeOwners, "gradle.something", "@org/team");
    }

    @Test
    void shouldNotMatchFileStartingWithDirectoryName() {
        CodeOwners codeOwners = new CodeOwners(
            "* @org/team\n" +
            "gradle/**\n"
        );

        assertOwners(codeOwners, "gradlew", "@org/team");
    }

    @Test
    void shouldNotMatchHyphenatedFileWithDirectoryPrefix() {
        CodeOwners codeOwners = new CodeOwners(
            "* @org/team\n" +
            "gradle/**\n"
        );

        assertOwners(codeOwners, "gradle-wrapper.jar", "@org/team");
    }

    @Test
    void shouldMatchFilesInsideDirectory() {
        CodeOwners codeOwners = new CodeOwners(
            "* @org/team\n" +
            "gradle/**\n"
        );

        assertOwners(codeOwners, "gradle/wrapper/gradle-wrapper.properties");
        assertOwners(codeOwners, "gradle/libs.versions.toml");
    }

    @Test
    void shouldMatchWildcardWhenNoTrailingGlobstar() {
        CodeOwners codeOwners = new CodeOwners(
            "* @org/team\n"
        );

        assertOwners(codeOwners, "gradle.properties", "@org/team");
        assertOwners(codeOwners, "gradle/file", "@org/team");
    }
}
