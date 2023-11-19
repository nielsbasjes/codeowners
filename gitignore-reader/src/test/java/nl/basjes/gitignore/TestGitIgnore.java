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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static nl.basjes.gitignore.TestUtils.assertIgnore;
import static nl.basjes.gitignore.TestUtils.assertNotIgnore;
import static nl.basjes.gitignore.TestUtils.assertNullMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestGitIgnore {

    private static final Logger LOG = LoggerFactory.getLogger(TestGitIgnore.class);

    @Test
    void testFullRangeExpression() {
        // This expression contains all features described in the examples below to ensure the parser can handle it
        String input =
            "*.log\n" +
            "!\\#important?/debug[0-9]/debug[!01]/**/*debug[a-z]/*.log";
        GitIgnore gitIgnore = new GitIgnore(input);
        assertIgnore(gitIgnore, "logs/debug.log");
        assertNullMatch(gitIgnore, "#important_/debug4/debug4/something/something/local_debugb/Something.logxxx");
        LOG.info("Input:\n{}\nParsed:\n{}\n", input, gitIgnore);
    }

    // ------------------------------------------

    // I used the examples provided by Atlassian in their gitignore tutorial as test cases
    // https://www.atlassian.com/git/tutorials/saving-changes/gitignore
    // This documentation is licensed via  https://creativecommons.org/licenses/by/2.5/au/

    @Test
    void testPrependGlobstar1() {
        // You can prepend a pattern with a double asterisk to match directories anywhere in the repository.
        GitIgnore gitIgnore = new GitIgnore("**/logs");

        assertIgnore(gitIgnore, "logs/debug.log");
        assertIgnore(gitIgnore, "logs/monday/foo.bar");
        assertIgnore(gitIgnore, "build/logs/debug.log");
    }

    // ------------------------------------------

    @Test
    void testPrependGlobstar2() {
        //You can also use a double asterisk to match files based on their name and the name of their parent directory.
        GitIgnore gitIgnore = new GitIgnore("**/logs/debug.log");

        assertIgnore(gitIgnore, "logs/debug.log");
        assertIgnore(gitIgnore, "build/logs/debug.log");

        assertNotIgnore(gitIgnore, "logs/build/debug.log");
    }

    // ------------------------------------------

    @Test
    void testWildcard() {
        //An asterisk is a wildcard that matches zero or more characters.
        GitIgnore gitIgnore = new GitIgnore("*.log");

        assertIgnore(gitIgnore, "debug.log");
        assertIgnore(gitIgnore, "foo.log");
        assertIgnore(gitIgnore, ".log");
        assertIgnore(gitIgnore, "logs/debug.log");
    }

    // ------------------------------------------

    @Test
    void testNegate() {
        // Prepending an exclamation mark to a pattern negates it.
        // If a file matches a pattern, but also matches a negating pattern defined later in the file, it will not be ignored.
        GitIgnore gitIgnore = new GitIgnore(
            "*.log\n" +
            "!important.log");

        assertIgnore(gitIgnore, "debug.log");
        assertIgnore(gitIgnore, "trace.log");

        assertNotIgnore(gitIgnore, "important.log");
        assertNotIgnore(gitIgnore, "logs/important.log");
    }

    // ------------------------------------------

    @Test
    void testNegateAndReignore() {
        // Patterns defined after a negating pattern will re-ignore any previously negated files.
        GitIgnore gitIgnore = new GitIgnore(
            "*.log\n" +
            "!important/*.log\n" +
            "trace.* ");
        assertIgnore(gitIgnore, "debug.log");
        assertIgnore(gitIgnore, "important/trace.log");

        assertNotIgnore(gitIgnore, "important/debug.log");
    }

    // ------------------------------------------

    @Test
    void testPinRoot() {
        // Prepending a slash matches files only in the repository root.
        GitIgnore gitIgnore = new GitIgnore("/debug.log ");
        assertIgnore(gitIgnore, "debug.log");

        assertNotIgnore(gitIgnore, "logs/debug.log");
    }

    // ------------------------------------------

    @Test
    void testdefaultAnyDirectory() {
        // By default, patterns match files in any directory
        GitIgnore gitIgnore = new GitIgnore("debug.log");
        assertIgnore(gitIgnore, "debug.log");
        assertIgnore(gitIgnore, "logs/debug.log");
    }

    // ------------------------------------------

    @Test
    void testSingleCharWildcard() {
        // A question mark matches exactly one character.
        GitIgnore gitIgnore = new GitIgnore("debug?.log ");
        assertIgnore(gitIgnore, "debug0.log");
        assertIgnore(gitIgnore, "debugg.log");

        assertNotIgnore(gitIgnore, "debug10.log");
    }

    // ------------------------------------------

    @Test
    void testCharRange() {
        // Square brackets can also be used to match a single character from a specified range.
        GitIgnore gitIgnore = new GitIgnore("debug[0-9].log ");

        assertIgnore(gitIgnore, "debug0.log");
        assertIgnore(gitIgnore, "debug1.log");

        assertNotIgnore(gitIgnore, "debug10.log");
    }

    // ------------------------------------------

    @Test
    void testCharSet() {
        // Square brackets match a single character form the specified set.
        GitIgnore gitIgnore = new GitIgnore("debug[01].log");

        assertIgnore(gitIgnore, "debug0.log");
        assertIgnore(gitIgnore, "debug1.log");

        assertNotIgnore(gitIgnore, "debug2.log");
        assertNotIgnore(gitIgnore, "debug01.log");
    }

    // ------------------------------------------

    @Test
    void testNotCharSet() {
        // An exclamation mark can be used to match any character except one from the specified set.
        GitIgnore gitIgnore = new GitIgnore("debug[!01].log ");

        assertIgnore(gitIgnore, "debug2.log");

        assertNotIgnore(gitIgnore, "debug0.log");
        assertNotIgnore(gitIgnore, "debug1.log");
        assertNotIgnore(gitIgnore, "debug01.log");
    }

    // ------------------------------------------

    @Test
    void testCharRangeAlpabetical() {
        // Ranges can be numeric or alphabetic.
        GitIgnore gitIgnore = new GitIgnore("debug[a-z].log ");

        assertIgnore(gitIgnore, "debuga.log");
        assertIgnore(gitIgnore, "debugb.log");

        assertNotIgnore(gitIgnore, "debug1.log");
    }

    // ------------------------------------------

    @Test
    void testNoSlashMatchDirsAndFiles() {
        // If you don't append a slash, the pattern will match both files and the contents of directories with that name.
        // In the example matches on the left, both directories and files named logs are ignored
        GitIgnore gitIgnore = new GitIgnore("logs");
        assertIgnore(gitIgnore, "logs");
        assertIgnore(gitIgnore, "logs/debug.log");
        assertIgnore(gitIgnore, "logs/latest/foo.bar");
        assertIgnore(gitIgnore, "build/logs");
        assertIgnore(gitIgnore, "build/logs/debug.log");
    }

    // ------------------------------------------

    @Test
    void testAppendSlash() {
        // Appending a slash indicates the pattern is a directory.
        // The entire contents of any directory in the repository matching that name –
        // including all of its files and subdirectories – will be ignored
        GitIgnore gitIgnore = new GitIgnore("logs/");

        assertIgnore(gitIgnore, "logs/debug.log");
        assertIgnore(gitIgnore, "logs/latest/foo.bar");
        assertIgnore(gitIgnore, "build/logs/foo.bar");
        assertIgnore(gitIgnore, "build/logs/latest/debug.log");
    }

    // ------------------------------------------

    // FIXME: Cannot handle this edge case yet
    @Disabled("FIXME: Cannot handle this edge case yet. Sorry, not 100% compliant yet.")
    @Test
    void testEdgeCase() {
        // From https://www.atlassian.com/git/tutorials/saving-changes/gitignore
        //     Wait a minute! Shouldn't logs/important.log be negated in the example on the left?
        //     Nope! Due to a performance-related quirk in Git, you can not negate a file
        //     that is ignored due to a pattern matching a directory
        GitIgnore gitIgnore = new GitIgnore(
            "logs/\n" +
            "!logs/important.log");

        assertIgnore(gitIgnore, "logs/debug.log");
        assertIgnore(gitIgnore, "logs/important.log");
    }

    // ------------------------------------------

    @Test
    void testGlobStarDirectories() {
        // A double asterisk matches zero or more directories.
        GitIgnore gitIgnore = new GitIgnore("logs/**/debug.log");

        assertIgnore(gitIgnore, "logs/debug.log");
        assertIgnore(gitIgnore, "logs/monday/debug.log");
        assertIgnore(gitIgnore, "logs/monday/pm/debug.log");
    }

    // ------------------------------------------

    @Test
    void testDirNameWildcard() {
        // Wildcards can be used in directory names as well.
        GitIgnore gitIgnore = new GitIgnore("logs/*day/debug.log");

        assertIgnore(gitIgnore, "logs/monday/debug.log");
        assertIgnore(gitIgnore, "logs/tuesday/debug.log");

        assertNotIgnore(gitIgnore, "logs/latest/debug.log");
    }

    @Test
    void testSubDirectoriesCase() {
        GitIgnore gitIgnore = new GitIgnore(
            "/dir1/*\n" +
            "/dir2/*/*\n" +
            "/dir3/*/*/*\n" +
            "/dir4/**/*\n"
        );
        gitIgnore.setVerbose(true);
        LOG.info("GitIgnore:\n{}", gitIgnore);
        // NO Subdirs
        assertIgnore(gitIgnore, "/dir1/bar.txt");
        assertNotIgnore(gitIgnore, "/dir1//bar.txt");
        assertNotIgnore(gitIgnore, "/dir1/foo/bar.txt");
        assertNotIgnore(gitIgnore, "/dir1/foo/foo/bar.txt");
        assertNotIgnore(gitIgnore, "/dir1/foo/foo/foo/bar.txt");

        // Exactly 1 Subdir
        assertNotIgnore(gitIgnore, "/dir2/bar.txt");
        assertNotIgnore(gitIgnore, "/dir2//bar.txt");
        assertIgnore(gitIgnore, "/dir2/foo/bar.txt");
        assertNotIgnore(gitIgnore, "/dir2/foo/foo/bar.txt");
        assertNotIgnore(gitIgnore, "/dir2/foo/foo/foo/bar.txt");

        // Exactly 2 Subdirs
        assertNotIgnore(gitIgnore, "/dir3/bar.txt");
        assertNotIgnore(gitIgnore, "/dir3//bar.txt");
        assertNotIgnore(gitIgnore, "/dir3/foo/bar.txt");
        assertIgnore(gitIgnore,  "/dir3/foo/foo/bar.txt");
        assertNotIgnore(gitIgnore, "/dir3///bar.txt");
        assertNotIgnore(gitIgnore, "/dir3/a//bar.txt");
        assertNotIgnore(gitIgnore, "/dir3//b/bar.txt");
        assertNotIgnore(gitIgnore, "/dir3/foo/foo/foo/bar.txt");

        // Any Subdirs
        assertIgnore(gitIgnore, "/dir4/bar.txt");
        assertIgnore(gitIgnore, "/dir4//bar.txt");
        assertIgnore(gitIgnore, "/dir4/foo/bar.txt");
        assertIgnore(gitIgnore, "/dir4/foo/foo/bar.txt");
        assertIgnore(gitIgnore, "/dir4/foo/foo/foo/bar.txt");
    }

    // ------------------------------------------

    @Test
    void testRootRelative() {
        // Patterns specifying a file in a particular directory are relative to the repository root.
        // (You can prepend a slash if you like, but it doesn't do anything special.)
        GitIgnore gitIgnore = new GitIgnore("logs/debug.log");

        assertIgnore(gitIgnore, "logs/debug.log");

        assertNotIgnore(gitIgnore, "debug.log");
        assertNotIgnore(gitIgnore, "build/logs/debug.log");

        // (You can prepend a slash if you like, but it doesn't do anything special.)
        assertIgnore(gitIgnore, "/logs/debug.log");

        assertNotIgnore(gitIgnore, "/debug.log");
        assertNotIgnore(gitIgnore, "/build/logs/debug.log");

    }

    // ------------------------------------------

    @Test
    void testIgnoreEscapedSpecials() {
        GitIgnore gitIgnore = new GitIgnore("foo\\[01\\].txt ");

        assertIgnore(gitIgnore, "foo[01].txt");
        assertNotIgnore(gitIgnore, "foo01.txt");
        assertNotIgnore(gitIgnore, "foo0.txt");
        assertNotIgnore(gitIgnore, "foo1.txt");
    }

    @Test
    void testIgnoreBaseDir() {
        // To ensure all variants work as expected.
        testIgnoreBaseDir("src/test");
        testIgnoreBaseDir("/src/test");
        testIgnoreBaseDir("src/test/");
        testIgnoreBaseDir("/src/test/");
    }

    void testIgnoreBaseDir(String baseDir) {
        GitIgnore gitIgnore = new GitIgnore(baseDir, "*.properties");
        gitIgnore.setVerbose(true);
        assertIgnore(gitIgnore, "src/test/test.properties");
        assertIgnore(gitIgnore, "/src/test/test.properties");

        // Note: This actually contains the '/src/test/' !!
        assertNotIgnore(gitIgnore, "/somethingelse/src/test/test.properties");

        // Note: This actually contains the 'src/test' !!
        assertNotIgnore(gitIgnore, "src/test.properties");

        // Note: This actually contains the 'src/test' !!
        assertNotIgnore(gitIgnore, "foo/src/test/something.properties");

        assertNotIgnore(gitIgnore, "src/main/test.properties");
        assertNotIgnore(gitIgnore, "test.properties");

        // To ensure more code coverage
        gitIgnore.setVerbose(false);
        assertNotIgnore(gitIgnore, "foo/src/test/something.properties");
    }

    // ------------------------------------------

    @Test
    void testIgnoreFromFile() throws IOException {
        URL url = this.getClass()
            .getClassLoader()
            .getResource("gitignore_fileread");

        assertNotNull(url);

        GitIgnore gitIgnore = new GitIgnore(new File(url.getFile()));

        gitIgnore.setVerbose(true);

        assertIgnore(gitIgnore, "foo[01].txt");
        assertNotIgnore(gitIgnore, "foo01.txt");
        assertNotIgnore(gitIgnore, "foo0.txt");
        assertNotIgnore(gitIgnore, "foo1.txt");
        assertIgnore(gitIgnore, "src/test/test.properties");
        assertIgnore(gitIgnore, "/src/test/test.properties");
        assertNotIgnore(gitIgnore, "/src/test/bla.properties");
    }
}
