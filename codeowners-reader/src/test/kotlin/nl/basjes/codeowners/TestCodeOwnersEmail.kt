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

internal class TestCodeOwnersEmail {
    @Test
    fun testEmailOwners() {
        val codeOwners = CodeOwners(
            """
            file0.txt @someone
            file1.txt someone@example.nl
            file2.txt some.one@example.nl
            file3.txt some+one@example.nl
            file4.txt s.o{m}e-o_n|e@example.nl
            fileall.txt @someone someone@example.nl some.one@example.nl some+one@example.nl s.o{m}e-o_n|e@example.nl
            """.trimIndent()
        )
        TestUtils.assertOwners(codeOwners, "file1.txt", "someone@example.nl")
        TestUtils.assertOwners(codeOwners, "file2.txt", "some.one@example.nl")
        TestUtils.assertOwners(codeOwners, "file3.txt", "some+one@example.nl")
        TestUtils.assertOwners(codeOwners, "file4.txt", "s.o{m}e-o_n|e@example.nl")
        TestUtils.assertOwners(codeOwners, "fileall.txt", "@someone", "someone@example.nl", "some.one@example.nl", "some+one@example.nl", "s.o{m}e-o_n|e@example.nl")
    }

    @Test
    fun testEmailOwnersCommentsBefore() {
        val codeOwners = CodeOwners(
            "file0.txt @someone\n" +
            "file1.txt (before)someone@example.nl\n" +
            "file2.txt (before)some.one@example.nl\n" +
            "file3.txt (before)some+one@example.nl\n" +
            "file4.txt (before)s.o{m}e-o_n|e@example.nl\n" +
            "fileall.txt @someone (before)someone@example.nl (before)some.one@example.nl (before)some+one@example.nl (before)s.o{m}e-o_n|e@example.nl"
        )

        // The comments will be stripped automatically
        TestUtils.assertOwners(codeOwners, "file1.txt", "someone@example.nl")
        TestUtils.assertOwners(codeOwners, "file2.txt", "some.one@example.nl")
        TestUtils.assertOwners(codeOwners, "file3.txt", "some+one@example.nl")
        TestUtils.assertOwners(codeOwners, "file4.txt", "s.o{m}e-o_n|e@example.nl")
        TestUtils.assertOwners(codeOwners, "fileall.txt", "@someone", "someone@example.nl", "some.one@example.nl", "some+one@example.nl", "s.o{m}e-o_n|e@example.nl")
    }

    @Test
    fun testEmailOwnersCommentsAfter() {
        val codeOwners = CodeOwners(
            """
            file0.txt @someone
            file1.txt someone(after)@example.nl
            file2.txt some.one(after)@example.nl
            file3.txt some+one(after)@example.nl
            file4.txt s.o{m}e-o_n|e(after)@example.nl
            fileall.txt @someone someone(after)@example.nl some.one(after)@example.nl some+one(after)@example.nl s.o{m}e-o_n|e(after)@example.nl
            """.trimIndent()
        )

        // The comments will be stripped automatically
        TestUtils.assertOwners(codeOwners, "file1.txt", "someone@example.nl")
        TestUtils.assertOwners(codeOwners, "file2.txt", "some.one@example.nl")
        TestUtils.assertOwners(codeOwners, "file3.txt", "some+one@example.nl")
        TestUtils.assertOwners(codeOwners, "file4.txt", "s.o{m}e-o_n|e@example.nl")
        TestUtils.assertOwners(codeOwners, "fileall.txt", "@someone", "someone@example.nl", "some.one@example.nl", "some+one@example.nl", "s.o{m}e-o_n|e@example.nl")
    }

    @Test
    fun testEmailOwnersCommentsBeforeAndAfter() {
        val codeOwners = CodeOwners(
            """
            file0.txt @someone
            file1.txt (before)someone(after)@example.nl
            file2.txt (before)some.one(after)@example.nl
            file3.txt (before)some+one(after)@example.nl
            file4.txt (before)s.o{m}e-o_n|e(after)@example.nl
            fileall.txt @someone (before)someone(after)@example.nl (before)some.one(after)@example.nl (before)some+one(after)@example.nl (before)s.o{m}e-o_n|e(after)@example.nl
            """.trimIndent()
        )

        // The comments will be stripped automatically
        TestUtils.assertOwners(codeOwners, "file1.txt", "someone@example.nl")
        TestUtils.assertOwners(codeOwners, "file2.txt", "some.one@example.nl")
        TestUtils.assertOwners(codeOwners, "file3.txt", "some+one@example.nl")
        TestUtils.assertOwners(codeOwners, "file4.txt", "s.o{m}e-o_n|e@example.nl")
        TestUtils.assertOwners(codeOwners, "fileall.txt", "@someone", "someone@example.nl", "some.one@example.nl", "some+one@example.nl", "s.o{m}e-o_n|e@example.nl")
    }
}
