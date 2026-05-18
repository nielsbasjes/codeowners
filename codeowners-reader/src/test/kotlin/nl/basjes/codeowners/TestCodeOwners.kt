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
package nl.basjes.codeowners

import kotlin.test.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class TestCodeOwners {
    private fun runChecks(codeOwners: CodeOwners) {
        TestUtils.assertOwners(codeOwners, "Foo.txt", "@code", "@multiple", "@owners", "@dev-team")
        TestUtils.assertOwners(codeOwners, "Foo.rb", "@ruby-owner", "@dev-team")
        TestUtils.assertOwners(codeOwners, "#file_with_pound.rb", "@owner-file-with-pound", "@dev-team")
    }

    @Test
    fun testDotStarCase() {
        val codeOwners = CodeOwners(
            // Intended to match '/foo/.bar' NOT '/foo/' and NOT '/foo/foo/.bar'
            """
            /foo/.* @user1
            *.xml @user2
            """.trimIndent()
        )
        //        codeOwners.setVerbose(true);
//        LOG.info("CODEOWNERS:\n{}", codeOwners);
        TestUtils.assertOwners(codeOwners, "/foo/.foo", "@user1")
        TestUtils.assertOwners(codeOwners, "/foo/.foo/bar", "@user1")
        TestUtils.assertOwners(codeOwners, "/foo/foo/.bar") // No users
        TestUtils.assertOwners(codeOwners, "/foo/xfoo") // No users
        TestUtils.assertOwners(codeOwners, "/foo/xfoo/bar")
        TestUtils.assertOwners(codeOwners, "/foo/.foo/bar.xml", "@user2")
        TestUtils.assertOwners(codeOwners, "/foo/foo") // No users
        TestUtils.assertOwners(codeOwners, "/foo/foo/bar.xml", "@user2")
    }

    @Test
    fun testMidStarCase() {
        val codeOwners = CodeOwners(
            // Intended to match '/tool-library/bar.txt'
            """
            /tool-*/ @user1
            *.xml @user2
            """.trimIndent()
        )
        codeOwners.verbose = true
        LOG.info("CODEOWNERS:\n{}", codeOwners)
        TestUtils.assertOwners(codeOwners, "/tool-app/bar.txt", "@user1")
        TestUtils.assertOwners(codeOwners, "/tool-app/foo/bar.txt", "@user1")
        TestUtils.assertOwners(codeOwners, "/tool-app/bar.xml", "@user2")
        TestUtils.assertOwners(codeOwners, "/tool-app/foo/bar.xml", "@user2")
        TestUtils.assertOwners(codeOwners, "/bar.txt")
        TestUtils.assertOwners(codeOwners, "/bar.xml", "@user2")
    }

    @Test
    fun testSubDirectoriesCase() {
        // # The `docs/*` pattern will match files like
        // # `docs/getting-started.md` but not further nested files like
        // # `docs/build-app/troubleshooting.md`.
        val codeOwners = CodeOwners(
            "/dir1/* @user1\n" +
                    "/dir2/*/* @user2\n" +
                    "/dir3/*/*/* @user3\n" +
                    "/dir4/**/* @user4\n"
        )
        codeOwners.verbose = true
        LOG.info("CODEOWNERS:\n{}", codeOwners)
        // NO Subdirs
        TestUtils.assertOwners(codeOwners, "/dir1/bar.txt", "@user1")
        TestUtils.assertOwners(codeOwners, "/dir1//bar.txt")
        TestUtils.assertOwners(codeOwners, "/dir1/foo/bar.txt")
        TestUtils.assertOwners(codeOwners, "/dir1/foo/foo/bar.txt")
        TestUtils.assertOwners(codeOwners, "/dir1/foo/foo/foo/bar.txt")

        // Exactly 1 Subdir
        TestUtils.assertOwners(codeOwners, "/dir2/bar.txt")
        TestUtils.assertOwners(codeOwners, "/dir2//bar.txt")
        TestUtils.assertOwners(codeOwners, "/dir2/foo/bar.txt", "@user2")
        TestUtils.assertOwners(codeOwners, "/dir2/foo/foo/bar.txt")
        TestUtils.assertOwners(codeOwners, "/dir2/foo/foo/foo/bar.txt")

        // Exactly 2 Subdirs
        TestUtils.assertOwners(codeOwners, "/dir3/bar.txt")
        TestUtils.assertOwners(codeOwners, "/dir3//bar.txt")
        TestUtils.assertOwners(codeOwners, "/dir3/foo/bar.txt")
        TestUtils.assertOwners(codeOwners, "/dir3/foo/foo/bar.txt", "@user3")
        TestUtils.assertOwners(codeOwners, "/dir3///bar.txt")
        TestUtils.assertOwners(codeOwners, "/dir3/a//bar.txt")
        TestUtils.assertOwners(codeOwners, "/dir3//b/bar.txt")
        TestUtils.assertOwners(codeOwners, "/dir3/foo/foo/foo/bar.txt")

        // Any Subdirs
        TestUtils.assertOwners(codeOwners, "/dir4/bar.txt", "@user4")
        TestUtils.assertOwners(codeOwners, "/dir4//bar.txt", "@user4")
        TestUtils.assertOwners(codeOwners, "/dir4/foo/bar.txt", "@user4")
        TestUtils.assertOwners(codeOwners, "/dir4/foo/foo/bar.txt", "@user4")
        TestUtils.assertOwners(codeOwners, "/dir4/foo/foo/foo/bar.txt", "@user4")
    }


    @Test
    @Throws(IOException::class)
    fun testCodeOwnersToStringRoundTrip() {
        val url = this.javaClass
            .classLoader
            .getResource("CODEOWNERS_base")

        assertNotNull(url)

        val codeOwners = CodeOwners(File(url.file))
        //        LOG.info("\n{}", codeOwners);
        runChecks(codeOwners)
        // Now reparse the toString output... (NORMAL)
        codeOwners.verbose = false
        val codeOwners2 = CodeOwners(codeOwners.toString())
        runChecks(codeOwners2)

        // Now reparse the toString output... (VERBOSE)
        codeOwners.verbose = true
        val codeOwners3 = CodeOwners(codeOwners.toString())
        runChecks(codeOwners3)
    }

    @Test
    fun testToString() {
        val codeOwners = CodeOwners(
            """
            /tool-*/ @user1
            *.xml @user2
            """.trimIndent()
        )
        codeOwners.verbose = true
        assertEquals(
            """
            # CODEOWNERS file:
            # Regex used for the next rule:   ^/tool-.*/
            /tool-*/ @user1
            # Regex used for the next rule:   .*\.xml(/|$)
            *.xml @user2

            """.trimIndent(),
            codeOwners.toString()
        )

        codeOwners.verbose = false
        assertEquals(
            """
            # CODEOWNERS file:
            /tool-*/ @user1
            *.xml @user2

            """.trimIndent(),
            codeOwners.toString()
        )
    }

    @Test
    fun testToStringEmpty() {
        val codeOwners = CodeOwners(
            """
            # Nothing here, only comments
            """.trimIndent()
        )
        codeOwners.verbose = true
        assertEquals(
            """
            # CODEOWNERS file:
            # No CODEOWNER rules were defined.

            """.trimIndent(),
            codeOwners.toString()
        )

        codeOwners.verbose = false
        assertEquals(
            """
            # CODEOWNERS file:
            # No CODEOWNER rules were defined.

            """.trimIndent(),
            codeOwners.toString()
        )
    }


    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(TestCodeOwners::class.java)
    }
}
