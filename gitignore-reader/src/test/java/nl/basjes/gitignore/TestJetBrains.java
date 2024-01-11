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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.PatternSyntaxException;

import static nl.basjes.gitignore.TestUtils.assertIgnore;
import static nl.basjes.gitignore.TestUtils.assertNotIgnore;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class TestJetBrains {

    @Test
    void testJetBrainsIgnoreRules() {
        // The JetBrains ignore plugin for IntelliJ has a lot of prebuilt rules to choose from.
        // This simply loads all of them and tests a few edge cases.
        // https://plugins.jetbrains.com/plugin/7495--ignore
        try {
            GitIgnoreFileSet ignoreFileSet = new GitIgnoreFileSet(new File("src/test/resources/JetBrains"));
        } catch (Exception e) {
            fail("Should not throw anything");
        }
    }

    // ------------------------------------------

    @Test
    void testCharacterRange() {
        GitIgnore gitIgnore = new GitIgnore("*.sw[mnop]");
        assertIgnore(gitIgnore, "test.swm");
        assertIgnore(gitIgnore, "logs/test.swp");
        assertNotIgnore(gitIgnore, "test.swa");
        assertNotIgnore(gitIgnore, "test.swma");
    }

    @Test
    void testExtensionsRange() {
        GitIgnore gitIgnore = new GitIgnore("coverage*[.json, .xml, .info]");
        assertIgnore(gitIgnore, "coverage.json");
        assertIgnore(gitIgnore, "coverage-001.json");
        assertIgnore(gitIgnore, "dir1/coverage-001.json");
        assertIgnore(gitIgnore, "dir2/coverage-001.json");
        assertNotIgnore(gitIgnore, "foo_coverage-001.json");

        assertIgnore(gitIgnore, "coverage.xml");
        assertIgnore(gitIgnore, "coverage-001.xml");
        assertIgnore(gitIgnore, "dir1/coverage-001.xml");
        assertIgnore(gitIgnore, "dir2/coverage-001.xml");
        assertNotIgnore(gitIgnore, "foo_coverage-001.xml");

        assertIgnore(gitIgnore, "coverage.info");
        assertIgnore(gitIgnore, "coverage-001.info");
        assertIgnore(gitIgnore, "dir1/coverage-001.info");
        assertIgnore(gitIgnore, "dir2/coverage-001.info");
        assertNotIgnore(gitIgnore, "foo_coverage-001.info");

        assertNotIgnore(gitIgnore, "coverage.j");
        assertNotIgnore(gitIgnore, "coverage.x");
        assertNotIgnore(gitIgnore, "coverage.i");
        assertNotIgnore(gitIgnore, "coverage.,");
        assertNotIgnore(gitIgnore, "coverage.");
    }

    @Test
    void testSpecialCharactersTilde() {
        GitIgnore gitIgnore = new GitIgnore(
            "foo*\n" +
            "foo\n" +
            ".~lock.*\n" +
            ".log.*\n" +
            ".log\n",
            true);
        assertIgnore(gitIgnore, "foo");
        assertIgnore(gitIgnore, "/dir/foo");

        assertIgnore(gitIgnore, "foobar");
        assertIgnore(gitIgnore, "/dir/foobar");

        assertIgnore(gitIgnore, ".log.something1234");
        assertIgnore(gitIgnore, "/dir/.log.something1234");

        assertIgnore(gitIgnore, ".~lock.something1234");
        assertIgnore(gitIgnore, "/dir/.~lock.something1234");

        // Ensure the trailing . is not a wildcard.
        assertNotIgnore(gitIgnore, ".logger");
        assertNotIgnore(gitIgnore, "/dir/.logger");
        assertNotIgnore(gitIgnore, ".~locker");
        assertNotIgnore(gitIgnore, "/dir/.~locker");
    }


    @Test
    void testSpecials() {
        assertIgnore("", "*.project.~u",                    "dummy.project.~u", "/dir/dummy.project.~u");
        assertIgnore("", ".idea/**/workspace.xml",          ".idea/something/workspace.xml");
        assertIgnore("", "[Bb]uild/",                       "build/foo.txt", "Build/foo.txt", "/dir/build/foo.txt",   "/dir/Build/foo.txt");
        assertIgnore("", "[Ww][Ii][Nn]32/",                 "wIn32/", "wIn32/foo.txt", "WiN32/", "WiN32/foo.txt");
        assertIgnore("", "Generated\\ Files/",              "Generated Files/", "Generated Files/foo.txt");
        assertIgnore("", "*~",                              "foo.txt~",  "/dir/foo.txt~");
        assertIgnore("", "*~.nib",                          "foo.txt~.nib", "/dir/foo.txt~.nib");
        assertNotIgnore("", "*~.nib",                       "foo.txt~xnib", "/dir/foo.txt~xnib");
        assertIgnore("", "._*",                             "._foo.txt", "/dir/._bar.txt");
        assertIgnore("", "@eaDir",                          "@eaDir", "/dir/@eaDir");
        assertIgnore("", "\\#recycle",                      "#recycle", "/dir/#recycle");
        assertIgnore("", "*.py[cod]",                       "foo.pyc", "/dir/bar.pyo");
        assertIgnore("", "*$py.class",                      "foo.$py.class", "/dir/bar.$py.class");
        assertIgnore("", "*- [Bb]ackup ([0-9][0-9]).rdl",   "/dir/foo - Backup (42).rdl");
        assertIgnore("", "*.lyx~",                          "foo.lyx~", "/dir/bar.lyx~");
        assertIgnore("", "~$*.doc*",                        "~$foo.docxx", "/dir/~$bar.doc");
        assertIgnore("", "*.l$?",                           "foo.l$1", "/dir/bar.l$b");
        assertIgnore("", "*.$$$",                           "foo.$$$", "/dir/bar.$$$");
    }


    @Test
    void testSpecialCharactersHash() {
        assertIgnore("", ".#*",         ".#foo",         "/dir/.#foo");
        assertIgnore("", "\\#*\\#",     "#foo#",         "/dir/#foo#");
        assertIgnore("", ".\\#*",       ".#foo",         "/dir/.#foo");
        assertIgnore("", ".~lock.*#",   ".~lock.foo#",   "/dir/.~lock.foo#");
        assertIgnore("", "*.s#?",       "foo.s#b",       "/dir/foo.s#b");
        assertIgnore("", "*.b#?",       "foo.b#b",       "/dir/foo.b#b");
        assertIgnore("", "*.l#?",       "foo.l#b",       "/dir/foo.l#b");
        assertIgnore("", "*.b$?",       "foo.b$b",       "/dir/foo.b$b");
        assertIgnore("", "*.s$?",       "foo.s$b",       "/dir/foo.s$b");
        assertIgnore("", "*.l$?",       "foo.l$b",       "/dir/foo.l$b");
        assertIgnore("", "*.s#*",       "foo.s#bar",     "/dir/foo.s#bar");
        assertIgnore("", "*.b#*",       "foo.b#bar",     "/dir/foo.b#bar");
        assertIgnore("", "*#",          "foo.#",         "/dir/foo.#");
        assertIgnore("", "*#*#",        "foo.#bar#",     "/dir/foo.#bar#");
        assertIgnore("", "*.#*",        "foo.#bar",      "/dir/foo.#bar");
        assertIgnore("", "*.ss#*",      "foo.ss#bar",    "/dir/foo.ss#bar");
        assertIgnore("", ".#*.ss",      ".#foo.ss",      "/dir/.#foo.ss");

        assertIgnore("", "*.lyx#",      "foo.lyx#",      "/dir/bar.lyx#");
        assertIgnore("", "*.l#?",       "foo.l#2",       "/dir/bar.l#a");
        assertIgnore("", "\\#*.rkt#",   "#foo.rkt#",     "/dir/#foo.rkt#");
        assertIgnore("", "\\#*.rkt#*#", "#foo.rkt#bar#", "/dir/#foo.rkt#bar#");
    }

    @Test
    void testMultiStarsAndBraces() {
        GitIgnore gitIgnore = new GitIgnore("*- Copy (*).*");
        assertIgnore(gitIgnore, "Something - Copy (1).docx");
        assertIgnore(gitIgnore, "/dir/Something - Copy (1).docx");
    }

    @Test
    void testSpecialCharactersUnderscore() {
        GitIgnore gitIgnore = new GitIgnore("_");
        assertIgnore(gitIgnore, "_");
        assertIgnore(gitIgnore, "/dir/_");
    }

    @Test
    void testBadExpression() {
        assertThrows(PatternSyntaxException.class, () -> new GitIgnore("["));
    }

}
