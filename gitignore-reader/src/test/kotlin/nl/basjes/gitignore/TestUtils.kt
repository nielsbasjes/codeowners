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
package nl.basjes.gitignore

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue


object TestUtils {
    fun assertIgnore(baseDir: String, gitIgnore: String, vararg filenames: String) {
        assertIgnore(GitIgnore(baseDir, gitIgnore), *filenames)
    }

    fun assertIgnore(gitIgnore: GitIgnore, vararg filenames: String) {
        for (filename in filenames) {
            interalAssertIgnore(gitIgnore, filename)
        }
    }

    private fun interalAssertIgnore(gitIgnore: GitIgnore, filename: String) {
        assertSame(
            true,
            gitIgnore.isIgnoredFile(filename),
            "Filename \"$filename\" should match but did not.\n$gitIgnore"
        )
        assertTrue(
            gitIgnore.ignoreFile(filename),
            "Filename \"$filename\" should match but did not.\n$gitIgnore"
        )
        assertFalse(
            gitIgnore.keepFile(filename),
            "Filename \"$filename\" should match but did not.\n$gitIgnore"
        )

        // Same but now with a windows path separator
        val wFilename = windowsFileName(filename)
        assertSame(
            true,
            gitIgnore.isIgnoredFile(wFilename),
            "Filename \"$wFilename\" should match but did not.\n$gitIgnore"
        )
        assertTrue(
            gitIgnore.ignoreFile(wFilename),
            "Filename \"$wFilename\" should match but did not.\n$gitIgnore"
        )
        assertFalse(
            gitIgnore.keepFile(wFilename),
            "Filename \"$wFilename\" should match but did not.\n$gitIgnore"
        )
    }

    fun assertNotIgnore(baseDir: String, gitIgnore: String, vararg filenames: String) {
        assertNotIgnore(GitIgnore(baseDir, gitIgnore), *filenames)
    }

    fun assertNotIgnore(gitIgnore: GitIgnore, vararg filenames: String) {
        for (filename in filenames) {
            internalAssertNotIgnore(gitIgnore, filename)
        }
    }

    private fun internalAssertNotIgnore(gitIgnore: GitIgnore, filename: String) {
        val isIgnoredFile = gitIgnore.isIgnoredFile(filename)
        assertTrue(
            isIgnoredFile == null || !isIgnoredFile,
            "Filename \"$filename\" should NOT match but did.\n$gitIgnore"
        )
        assertFalse(
            gitIgnore.ignoreFile(filename),
            "Filename \"$filename\" should NOT match but did.\n$gitIgnore"
        )
        assertTrue(
            gitIgnore.keepFile(filename),
            "Filename \"$filename\" should NOT match but did.\n$gitIgnore"
        )

        // Same but now with a windows path separator
        val wFilename = windowsFileName(filename)
        val wIsIgnoredFile = gitIgnore.isIgnoredFile(wFilename)
        assertTrue(
            wIsIgnoredFile == null || !wIsIgnoredFile,
            "Filename \"$wFilename\" should NOT match but did.\n$gitIgnore"
        )
        assertFalse(
            gitIgnore.ignoreFile(wFilename),
            "Filename \"$wFilename\" should NOT match but did.\n$gitIgnore"
        )
        assertTrue(
            gitIgnore.keepFile(wFilename),
            "Filename \"$wFilename\" should NOT match but did.\n$gitIgnore"
        )
    }


    fun assertNullMatch(gitIgnore: GitIgnore, filename: String) {
        assertNull(
            gitIgnore.isIgnoredFile(filename),
            "Filename \"$filename\" should NOT match but did."
        )

        // Same but now with a windows path separator
        val wFilename = windowsFileName(filename)
        assertNull(
            gitIgnore.isIgnoredFile(wFilename),
            "Filename \"$wFilename\" should NOT match but did."
        )
    }

//    fun assertNullMatch(baseDir: String, gitIgnore: String, filename: String) {
//        assertNullMatch(GitIgnore(baseDir, gitIgnore), filename)
//    }

    fun windowsFileName(filename: String): String {
        return filename.replace("/", "\\")
    }

    fun verifyGeneratedRegex(baseDir: String, gitIgnoreContent: String, expectedRegex: String?) {
        val gitIgnore = GitIgnore(baseDir, gitIgnoreContent)
        val ignoreRules: List<GitIgnore.IgnoreRule> = gitIgnore.ignoreRules
        assertEquals(1, ignoreRules.size)
        val ignoreRule = ignoreRules[0]
        assertEquals(expectedRegex, ignoreRule.ignorePattern.pattern, "Incorrect regex generated")
    }
}
