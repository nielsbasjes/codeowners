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

package nl.basjes.codeowners;

import org.junit.jupiter.api.Test;

import static nl.basjes.codeowners.TestUtils.assertOwners;

class TestCodeOwnersEmail {

    @Test
    void testEmailOwners() {
        CodeOwners codeOwners = new CodeOwners(
            "file0.txt @someone\n" +
            "file1.txt someone@example.nl\n" +
            "file2.txt some.one@example.nl\n"+
            "file3.txt some+one@example.nl\n"+
            "file4.txt s.o{m}e-o_n|e@example.nl\n"+
            "fileall.txt @someone someone@example.nl some.one@example.nl some+one@example.nl s.o{m}e-o_n|e@example.nl"
        );
        assertOwners(codeOwners, "file1.txt", "someone@example.nl");
        assertOwners(codeOwners, "file2.txt", "some.one@example.nl");
        assertOwners(codeOwners, "file3.txt", "some+one@example.nl");
        assertOwners(codeOwners, "file4.txt", "s.o{m}e-o_n|e@example.nl");
        assertOwners(codeOwners, "fileall.txt", "@someone", "someone@example.nl", "some.one@example.nl", "some+one@example.nl", "s.o{m}e-o_n|e@example.nl");
    }

    @Test
    void testEmailOwnersCommentsBefore() {
        CodeOwners codeOwners = new CodeOwners(
            "file0.txt @someone\n" +
            "file1.txt (before)someone@example.nl\n" +
            "file2.txt (before)some.one@example.nl\n"+
            "file3.txt (before)some+one@example.nl\n"+
            "file4.txt (before)s.o{m}e-o_n|e@example.nl\n"+
            "fileall.txt @someone (before)someone@example.nl (before)some.one@example.nl (before)some+one@example.nl (before)s.o{m}e-o_n|e@example.nl"
        );

        // The comments will be stripped automatically
        assertOwners(codeOwners, "file1.txt", "someone@example.nl");
        assertOwners(codeOwners, "file2.txt", "some.one@example.nl");
        assertOwners(codeOwners, "file3.txt", "some+one@example.nl");
        assertOwners(codeOwners, "file4.txt", "s.o{m}e-o_n|e@example.nl");
        assertOwners(codeOwners, "fileall.txt", "@someone", "someone@example.nl", "some.one@example.nl", "some+one@example.nl", "s.o{m}e-o_n|e@example.nl");
    }

    @Test
    void testEmailOwnersCommentsAfter() {
        CodeOwners codeOwners = new CodeOwners(
            "file0.txt @someone\n" +
            "file1.txt someone(after)@example.nl\n" +
            "file2.txt some.one(after)@example.nl\n"+
            "file3.txt some+one(after)@example.nl\n"+
            "file4.txt s.o{m}e-o_n|e(after)@example.nl\n"+
            "fileall.txt @someone someone(after)@example.nl some.one(after)@example.nl some+one(after)@example.nl s.o{m}e-o_n|e(after)@example.nl"
        );

        // The comments will be stripped automatically
        assertOwners(codeOwners, "file1.txt", "someone@example.nl");
        assertOwners(codeOwners, "file2.txt", "some.one@example.nl");
        assertOwners(codeOwners, "file3.txt", "some+one@example.nl");
        assertOwners(codeOwners, "file4.txt", "s.o{m}e-o_n|e@example.nl");
        assertOwners(codeOwners, "fileall.txt", "@someone", "someone@example.nl", "some.one@example.nl", "some+one@example.nl", "s.o{m}e-o_n|e@example.nl");
    }

    @Test
    void testEmailOwnersCommentsBeforeAndAfter() {
        CodeOwners codeOwners = new CodeOwners(
            "file0.txt @someone\n" +
            "file1.txt (before)someone(after)@example.nl\n" +
            "file2.txt (before)some.one(after)@example.nl\n"+
            "file3.txt (before)some+one(after)@example.nl\n"+
            "file4.txt (before)s.o{m}e-o_n|e(after)@example.nl\n"+
            "fileall.txt @someone (before)someone(after)@example.nl (before)some.one(after)@example.nl (before)some+one(after)@example.nl (before)s.o{m}e-o_n|e(after)@example.nl"
        );

        // The comments will be stripped automatically
        assertOwners(codeOwners, "file1.txt", "someone@example.nl");
        assertOwners(codeOwners, "file2.txt", "some.one@example.nl");
        assertOwners(codeOwners, "file3.txt", "some+one@example.nl");
        assertOwners(codeOwners, "file4.txt", "s.o{m}e-o_n|e@example.nl");
        assertOwners(codeOwners, "fileall.txt", "@someone", "someone@example.nl", "some.one@example.nl", "some+one@example.nl", "s.o{m}e-o_n|e@example.nl");
    }
}
