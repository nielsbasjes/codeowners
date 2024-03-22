/*
 * CodeOwners Tools
 * Copyright (C) 2023-2024 Niels Basjes
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

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TestRelativeAndAbsoluteFilenames {

    @Test
    void testRat362() {
        // https://issues.apache.org/jira/browse/RAT-362
        // Bugreport summary
        //     In the root of the file system
        //     Project root and working dir is '/foo'
        //     File '/foo/.gitgnore' contains '/foo.md'

        //     File /foo/foo.md is NOT ignored (Is incorrect).
        //     File /foo/foo/foo.md is ignored (Is incorrect).

        // Rootcause: This library expects absolute paths and was provided with a relative path.

        GitIgnoreFileSet gitIgnoreFileSet = new GitIgnoreFileSet(new File("/foo"), false);
        gitIgnoreFileSet.add(new GitIgnore("/", "/foo.md"));

        // Absolute path is the default (for backwards compatibility)
        assertTrue(gitIgnoreFileSet.ignoreFile("/foo/foo.md"));
        assertTrue(gitIgnoreFileSet.keepFile("/foo/foo/foo.md"));

        // Project relative (explicit)
        assertTrue(gitIgnoreFileSet.ignoreFile("foo.md", true));
        assertTrue(gitIgnoreFileSet.keepFile("/foo/foo.md", true));

        // Project relative (assumption)
        gitIgnoreFileSet.assumeQueriesAreProjectRelative();
        assertTrue(gitIgnoreFileSet.ignoreFile("foo.md"));
        assertTrue(gitIgnoreFileSet.keepFile("/foo/foo.md"));

        // Absolute path (explicit)
        assertTrue(gitIgnoreFileSet.ignoreFile("/foo/foo.md", false));
        assertTrue(gitIgnoreFileSet.keepFile("/foo/foo/foo.md", false));

        // Absolute path (assumption)
        gitIgnoreFileSet.assumeQueriesIncludeProjectBaseDir();
        assertTrue(gitIgnoreFileSet.ignoreFile("/foo/foo.md"));
        assertTrue(gitIgnoreFileSet.keepFile("/foo/foo/foo.md"));
    }



}
