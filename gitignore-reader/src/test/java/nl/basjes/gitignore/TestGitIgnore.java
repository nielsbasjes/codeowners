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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nl.basjes.gitignore.TestUtils.assertMatch;
import static nl.basjes.gitignore.TestUtils.assertNotMatch;

class TestGitIgnore {

    @Test
    void testFullRangeExpression() {
        // This expression contains all features described in the examples below to ensure the parser can handle it
        String input =
            "*.log\n" +
            "!\\#important?/debug[0-9]/debug[!01]/**/*debug[a-z]/*.log";
        GitIgnore gitIgnore = new GitIgnore(input);
        assertMatch(gitIgnore, "logs/debug.log");
        assertNotMatch(gitIgnore, "#important_/debug4/debug4/something/something/local_debugb/Something.log");
    }


    // I used the examples provided by Atlassian in their gitignore tutorial as test cases
    // https://www.atlassian.com/git/tutorials/saving-changes/gitignore
    // This documentation is licensed via  https://creativecommons.org/licenses/by/2.5/au/

    @Test
    void testPrependGlobstar1() {
        // You can prepend a pattern with a double asterisk to match directories anywhere in the repository.
        GitIgnore gitIgnore = new GitIgnore("**/logs");

        assertMatch(gitIgnore, "logs/debug.log");
        assertMatch(gitIgnore, "logs/monday/foo.bar");
        assertMatch(gitIgnore, "build/logs/debug.log");
    }

    // ------------------------------------------

    @Test
    void testPrependGlobstar2() {
        //You can also use a double asterisk to match files based on their name and the name of their parent directory.
        GitIgnore gitIgnore = new GitIgnore("**/logs/debug.log");

        assertMatch(gitIgnore, "logs/debug.log");
        assertMatch(gitIgnore, "build/logs/debug.log");

        assertNotMatch(gitIgnore, "logs/build/debug.log");
    }

    // ------------------------------------------

    @Test
    void testWildcard() {
        //An asterisk is a wildcard that matches zero or more characters.
        GitIgnore gitIgnore = new GitIgnore("*.log");

        assertMatch(gitIgnore, "debug.log");
        assertMatch(gitIgnore, "foo.log");
        assertMatch(gitIgnore, ".log");
        assertMatch(gitIgnore, "logs/debug.log");
    }

    // ------------------------------------------

    @Test
    void testNegate() {
        // Prepending an exclamation mark to a pattern negates it.
        // If a file matches a pattern, but also matches a negating pattern defined later in the file, it will not be ignored.
        GitIgnore gitIgnore = new GitIgnore(
            "*.log\n" +
            "!important.log");

        assertMatch(gitIgnore, "debug.log");
        assertMatch(gitIgnore, "trace.log");

        assertNotMatch(gitIgnore, "important.log");
        assertNotMatch(gitIgnore, "logs/important.log");
    }

    // ------------------------------------------

    @Test
    void testNegateAndReignore() {
        // Patterns defined after a negating pattern will re-ignore any previously negated files.
        GitIgnore gitIgnore = new GitIgnore(
            "*.log\n" +
            "!important/*.log\n" +
            "trace.* ");
        assertMatch(gitIgnore, "debug.log");
        assertMatch(gitIgnore, "important/trace.log");

        assertNotMatch(gitIgnore, "important/debug.log");
    }

    // ------------------------------------------

    @Test
    void testPinRoot() {
        // Prepending a slash matches files only in the repository root.
        GitIgnore gitIgnore = new GitIgnore("/debug.log ");
        assertMatch(gitIgnore, "debug.log");

        assertNotMatch(gitIgnore, "logs/debug.log");
    }

    // ------------------------------------------

    @Test
    void testdefaultAnyDirectory() {
        // By default, patterns match files in any directory
        GitIgnore gitIgnore = new GitIgnore("debug.log");
        assertMatch(gitIgnore, "debug.log");
        assertMatch(gitIgnore, "logs/debug.log");
    }

    // ------------------------------------------

    @Test
    void testSingleCharWildcard() {
        // A question mark matches exactly one character.
        GitIgnore gitIgnore = new GitIgnore("debug?.log ");
        assertMatch(gitIgnore, "debug0.log");
        assertMatch(gitIgnore, "debugg.log");

        assertNotMatch(gitIgnore, "debug10.log");
    }

    // ------------------------------------------

    @Test
    void testCharRange() {
        // Square brackets can also be used to match a single character from a specified range.
        GitIgnore gitIgnore = new GitIgnore("debug[0-9].log ");

        assertMatch(gitIgnore, "debug0.log");
        assertMatch(gitIgnore, "debug1.log");

        assertNotMatch(gitIgnore, "debug10.log");
    }

    // ------------------------------------------

    @Test
    void testCharSet() {
        // Square brackets match a single character form the specified set.
        GitIgnore gitIgnore = new GitIgnore("debug[01].log");

        assertMatch(gitIgnore, "debug0.log");
        assertMatch(gitIgnore, "debug1.log");

        assertNotMatch(gitIgnore, "debug2.log");
        assertNotMatch(gitIgnore, "debug01.log");
    }

    // ------------------------------------------

    @Test
    void testNotCharSet() {
        // An exclamation mark can be used to match any character except one from the specified set.
        GitIgnore gitIgnore = new GitIgnore("debug[!01].log ");

        assertMatch(gitIgnore, "debug2.log");

        assertNotMatch(gitIgnore, "debug0.log");
        assertNotMatch(gitIgnore, "debug1.log");
        assertNotMatch(gitIgnore, "debug01.log");
    }

    // ------------------------------------------

    @Test
    void testCharRangeAlpabetical() {
        // Ranges can be numeric or alphabetic.
        GitIgnore gitIgnore = new GitIgnore("debug[a-z].log ");

        assertMatch(gitIgnore, "debuga.log");
        assertMatch(gitIgnore, "debugb.log");

        assertNotMatch(gitIgnore, "debug1.log");
    }

    // ------------------------------------------

    @Test
    void testNoSlashMatchDirsAndFiles() {
        // If you don't append a slash, the pattern will match both files and the contents of directories with that name.
        // In the example matches on the left, both directories and files named logs are ignored
        GitIgnore gitIgnore = new GitIgnore("logs");
        assertMatch(gitIgnore, "logs");
        assertMatch(gitIgnore, "logs/debug.log");
        assertMatch(gitIgnore, "logs/latest/foo.bar");
        assertMatch(gitIgnore, "build/logs");
        assertMatch(gitIgnore, "build/logs/debug.log");
    }

    // ------------------------------------------

    @Test
    void testAppendSlash() {
        // Appending a slash indicates the pattern is a directory.
        // The entire contents of any directory in the repository matching that name –
        // including all of its files and subdirectories – will be ignored
        GitIgnore gitIgnore = new GitIgnore("logs/");

        assertMatch(gitIgnore, "logs/debug.log");
        assertMatch(gitIgnore, "logs/latest/foo.bar");
        assertMatch(gitIgnore, "build/logs/foo.bar");
        assertMatch(gitIgnore, "build/logs/latest/debug.log");
    }

    // ------------------------------------------

    // FIXME: Cannot handle this edge case yet
    @Disabled("FIXME: Cannot handle this edge case yet. Sorry, not 100% compliant yet.")
    @Test
    void testEdgeCase() {
        // Wait a minute! Shouldn't logs/important.log be negated in the example on the left?
        // Nope! Due to a performance-related quirk in Git, you can not negate a file
        // that is ignored due to a pattern matching a directory
        GitIgnore gitIgnore = new GitIgnore(
            "logs/\n" +
            "!logs/important.log");

        assertMatch(gitIgnore, "logs/debug.log");
        assertMatch(gitIgnore, "logs/important.log");
    }

    // ------------------------------------------

    @Test
    void testGlobStarDirectories() {
        // A double asterisk matches zero or more directories.
        GitIgnore gitIgnore = new GitIgnore("logs/**/debug.log");

        assertMatch(gitIgnore, "logs/debug.log");
        assertMatch(gitIgnore, "logs/monday/debug.log");
        assertMatch(gitIgnore, "logs/monday/pm/debug.log");
    }

    // ------------------------------------------

    @Test
    void testDirNameWildcard() {
        // Wildcards can be used in directory names as well.
        GitIgnore gitIgnore = new GitIgnore("logs/*day/debug.log");

        assertMatch(gitIgnore, "logs/monday/debug.log");
        assertMatch(gitIgnore, "logs/tuesday/debug.log");

        assertNotMatch(gitIgnore, "logs/latest/debug.log");
    }

    // ------------------------------------------

    @Test
    void testRootRelative() {
        // Patterns specifying a file in a particular directory are relative to the repository root.
        // (You can prepend a slash if you like, but it doesn't do anything special.)
        GitIgnore gitIgnore = new GitIgnore("logs/debug.log");

        assertMatch(gitIgnore, "logs/debug.log");

        assertNotMatch(gitIgnore, "debug.log");
        assertNotMatch(gitIgnore, "build/logs/debug.log");
    }

    // ------------------------------------------

    @Test
    void testIgnoreEscapedSpecials() {
        GitIgnore gitIgnore = new GitIgnore("foo\\[01\\].txt ");

        assertMatch(gitIgnore, "foo[01].txt");
    }

}
