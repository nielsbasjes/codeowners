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

import nl.basjes.gitignore.TestUtils.assertIgnore
import kotlin.test.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.regex.PatternSyntaxException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

internal class TestGitIgnore {
    @Test
    fun testFullRangeExpression() {
        // This expression contains all features described in the examples below to ensure the parser can handle it
        val input =
            "*.log\n" +
            "!\\#important?/debug[0-9]/debug[!01]/**/*debug[a-z]/*.log"
        val gitIgnore = GitIgnore(input)
        assertIgnore(gitIgnore, "logs/debug.log")
        TestUtils.assertNullMatch(
            gitIgnore,
            "#important_/debug4/debug4/something/something/local_debugb/Something.logxxx"
        )
        LOG.info("Input:\n{}\nParsed:\n{}\n", input, gitIgnore)
    }

    // ------------------------------------------
    // I used the examples provided by Atlassian in their gitignore tutorial as test cases
    // https://www.atlassian.com/git/tutorials/saving-changes/gitignore
    // This documentation is licensed via  https://creativecommons.org/licenses/by/2.5/au/
    @Test
    fun testPrependGlobstar1() {
        // You can prepend a pattern with a double asterisk to match directories anywhere in the repository.
        val gitIgnore = GitIgnore("**/logs")

        assertIgnore(gitIgnore, "logs/debug.log")
        assertIgnore(gitIgnore, "logs/monday/foo.bar")
        assertIgnore(gitIgnore, "build/logs/debug.log")
    }

    // ------------------------------------------
    @Test
    fun testPrependGlobstar2() {
        //You can also use a double asterisk to match files based on their name and the name of their parent directory.
        val gitIgnore = GitIgnore("**/logs/debug.log")

        assertIgnore(gitIgnore, "logs/debug.log")
        assertIgnore(gitIgnore, "build/logs/debug.log")

        TestUtils.assertNotIgnore(gitIgnore, "logs/build/debug.log")
    }

    // ------------------------------------------
    @Test
    fun testWildcard() {
        //An asterisk is a wildcard that matches zero or more characters.
        val gitIgnore = GitIgnore("*.log")

        assertIgnore(gitIgnore, "debug.log")
        assertIgnore(gitIgnore, "foo.log")
        assertIgnore(gitIgnore, ".log")
        assertIgnore(gitIgnore, "logs/debug.log")
    }

    // ------------------------------------------
    @Test
    fun testNegate() {
        // Prepending an exclamation mark to a pattern negates it.
        // If a file matches a pattern, but also matches a negating pattern defined later in the file, it will not be ignored.
        val gitIgnore = GitIgnore(
            "*.log\n" +
            "!important.log"
        )

        assertIgnore(gitIgnore, "debug.log")
        assertIgnore(gitIgnore, "trace.log")

        TestUtils.assertNotIgnore(gitIgnore, "important.log")
        TestUtils.assertNotIgnore(gitIgnore, "logs/important.log")
    }

    // ------------------------------------------
    @Test
    fun testNegateAndReignore() {
        // Patterns defined after a negating pattern will re-ignore any previously negated files.
        val gitIgnore = GitIgnore(
            "*.log\n" +
            "!important/*.log\n" +
            "trace.* "
        )
        assertIgnore(gitIgnore, "debug.log")
        assertIgnore(gitIgnore, "important/trace.log")

        TestUtils.assertNotIgnore(gitIgnore, "important/debug.log")
    }

    // ------------------------------------------
    @Test
    fun testPinRoot() {
        // Prepending a slash matches files only in the repository root.
        val gitIgnore = GitIgnore("/debug.log ")
        assertIgnore(gitIgnore, "debug.log")

        TestUtils.assertNotIgnore(gitIgnore, "logs/debug.log")
    }

    // ------------------------------------------
    @Test
    fun testDefaultAnyDirectory() {
        // By default, patterns match files in any directory
        val gitIgnore = GitIgnore("debug.log")
        assertIgnore(gitIgnore, "debug.log")
        assertIgnore(gitIgnore, "logs/debug.log")
    }

    // ------------------------------------------
    @Test
    fun testSingleCharWildcard() {
        // A question mark matches exactly one character.
        val gitIgnore = GitIgnore("debug?.log ")
        assertIgnore(gitIgnore, "debug0.log")
        assertIgnore(gitIgnore, "debugg.log")

        TestUtils.assertNotIgnore(gitIgnore, "debug10.log")
    }

    // ------------------------------------------
    @Test
    fun testCharRange() {
        // Square brackets can also be used to match a single character from a specified range.
        val gitIgnore = GitIgnore("debug[0-9].log ")

        assertIgnore(gitIgnore, "debug0.log")
        assertIgnore(gitIgnore, "debug1.log")

        TestUtils.assertNotIgnore(gitIgnore, "debug10.log")
    }

    // ------------------------------------------
    @Test
    fun testCharSet() {
        // Square brackets match a single character form the specified set.
        val gitIgnore = GitIgnore("debug[01].log")

        assertIgnore(gitIgnore, "debug0.log")
        assertIgnore(gitIgnore, "debug1.log")

        TestUtils.assertNotIgnore(gitIgnore, "debug2.log")
        TestUtils.assertNotIgnore(gitIgnore, "debug01.log")
    }

    // ------------------------------------------
    @Test
    fun testNotCharSet() {
        // An exclamation mark can be used to match any character except one from the specified set.
        val gitIgnore = GitIgnore("debug[!01].log ")

        assertIgnore(gitIgnore, "debug2.log")

        TestUtils.assertNotIgnore(gitIgnore, "debug0.log")
        TestUtils.assertNotIgnore(gitIgnore, "debug1.log")
        TestUtils.assertNotIgnore(gitIgnore, "debug01.log")
    }

    // ------------------------------------------
    @Test
    fun testCharRangeAlphabetical() {
        // Ranges can be numeric or alphabetic.
        val gitIgnore = GitIgnore("debug[a-z].log ")

        assertIgnore(gitIgnore, "debuga.log")
        assertIgnore(gitIgnore, "debugb.log")

        TestUtils.assertNotIgnore(gitIgnore, "debug1.log")
    }

    // ------------------------------------------
    @Test
    fun testNoSlashMatchDirsAndFiles() {
        // If you don't append a slash, the pattern will match both files and the contents of directories with that name.
        // In the example matches on the left, both directories and files named logs are ignored
        val gitIgnore = GitIgnore("logs")
        assertIgnore(gitIgnore, "logs")
        assertIgnore(gitIgnore, "logs/debug.log")
        assertIgnore(gitIgnore, "logs/latest/foo.bar")
        assertIgnore(gitIgnore, "build/logs")
        assertIgnore(gitIgnore, "build/logs/debug.log")
    }

    // ------------------------------------------
    @Test
    fun testAppendSlash() {
        // Appending a slash indicates the pattern is a directory.
        // The entire contents of any directory in the repository matching that name –
        // including all of its files and subdirectories – will be ignored
        val gitIgnore = GitIgnore("logs/")

        assertIgnore(gitIgnore, "logs/")
        assertIgnore(gitIgnore, "logs/debug.log")
        assertIgnore(gitIgnore, "logs/latest/foo.bar")
        assertIgnore(gitIgnore, "build/logs/foo.bar")
        assertIgnore(gitIgnore, "build/logs/latest/debug.log")
    }

    // ------------------------------------------
    @Test
    fun testEdgeCase() {
        // From https://www.atlassian.com/git/tutorials/saving-changes/gitignore
        //     Wait a minute! Shouldn't logs/important.log be negated in the example on the left?
        //     Nope! Due to a performance-related quirk in Git, you can not negate a file
        //     that is ignored due to a pattern matching a directory
        val gitIgnore = GitIgnore(
            "logs/\n" +
            "!logs/important.log\n"
        )

        assertIgnore(gitIgnore, "logs/debug.log")
        assertIgnore(gitIgnore, "logs/important.log")
    }

    @Test
    fun testEdgeCaseRuleOrdering() {
        // Same as above but now the rules in a different order
        val gitIgnore = GitIgnore(
            "!logs/important.log\n" +
                    "logs/\n"
        )

        assertIgnore(gitIgnore, "logs/debug.log")
        assertIgnore(gitIgnore, "logs/important.log")
    }

    @Test
    fun testEdgeCase2() {
        // As generated by the Initializer on https://jmonkeyengine.org/start/
        // Apparently unaware of this issue in git.
        val gitIgnore = GitIgnore(
            "#Although most of the .idea directory should not be committed there is a legitimate purpose for committing run configurations\n" +
            "/.idea/\n" +
            "!/.idea/runConfigurations/\n"
        )

        assertIgnore(gitIgnore, ".idea/ignore.txt")
        assertIgnore(gitIgnore, ".idea/runConfigurations/important.txt")
    }

    // ------------------------------------------
    @Test
    fun testGlobStarDirectories() {
        // A double asterisk matches zero or more directories.
        val gitIgnore = GitIgnore("logs/**/debug.log")

        assertIgnore(gitIgnore, "logs/debug.log")
        assertIgnore(gitIgnore, "logs/monday/debug.log")
        assertIgnore(gitIgnore, "logs/monday/pm/debug.log")
    }

    // ------------------------------------------
    @Test
    fun testDirNameWildcard() {
        // Wildcards can be used in directory names as well.
        val gitIgnore = GitIgnore("logs/*day/debug.log")

        assertIgnore(gitIgnore, "logs/monday/debug.log")
        assertIgnore(gitIgnore, "logs/tuesday/debug.log")

        TestUtils.assertNotIgnore(gitIgnore, "logs/latest/debug.log")
    }

    @Test
    fun testSubDirectoriesCase() {
        val gitIgnore = GitIgnore(
            "/dir1/*\n" +
                    "/dir2/*/*\n" +
                    "/dir3/*/*/*\n" +
                    "/dir4/**/*\n"
        )
        gitIgnore.verbose = true
        assertTrue(gitIgnore.verbose)
        LOG.info("GitIgnore:\n{}", gitIgnore)
        // NO Subdirs
        assertIgnore(gitIgnore, "/dir1/bar.txt")
        assertIgnore(gitIgnore, "/dir1//bar.txt") // Duplicate / are simplified
        TestUtils.assertNotIgnore(gitIgnore, "/dir1/foo/bar.txt")
        TestUtils.assertNotIgnore(gitIgnore, "/dir1/foo/foo/bar.txt")
        TestUtils.assertNotIgnore(gitIgnore, "/dir1/foo/foo/foo/bar.txt")

        // Exactly 1 Subdir
        TestUtils.assertNotIgnore(gitIgnore, "/dir2/bar.txt")
        TestUtils.assertNotIgnore(gitIgnore, "/dir2//bar.txt")
        assertIgnore(gitIgnore, "/dir2/foo/bar.txt")
        TestUtils.assertNotIgnore(gitIgnore, "/dir2/foo/foo/bar.txt")
        TestUtils.assertNotIgnore(gitIgnore, "/dir2/foo/foo/foo/bar.txt")

        // Exactly 2 Subdirs
        TestUtils.assertNotIgnore(gitIgnore, "/dir3/bar.txt")
        TestUtils.assertNotIgnore(gitIgnore, "/dir3//bar.txt")
        TestUtils.assertNotIgnore(gitIgnore, "/dir3/foo/bar.txt")
        assertIgnore(gitIgnore, "/dir3/foo/foo/bar.txt")
        assertIgnore(gitIgnore, "/dir3///foo///foo////bar.txt") // Duplicate / are simplified
        TestUtils.assertNotIgnore(gitIgnore, "/dir3///bar.txt")
        TestUtils.assertNotIgnore(gitIgnore, "/dir3/a//bar.txt")
        TestUtils.assertNotIgnore(gitIgnore, "/dir3//b/bar.txt")
        TestUtils.assertNotIgnore(gitIgnore, "/dir3/foo/foo/foo/bar.txt")

        // Any Subdirs
        assertIgnore(gitIgnore, "/dir4/bar.txt")
        assertIgnore(gitIgnore, "/dir4//bar.txt")
        assertIgnore(gitIgnore, "/dir4/foo/bar.txt")
        assertIgnore(gitIgnore, "/dir4/foo/foo/bar.txt")
        assertIgnore(gitIgnore, "/dir4/foo/foo/foo/bar.txt")
    }

    // ------------------------------------------
    @Test
    fun testRootRelative() {
        // Patterns specifying a file in a particular directory are relative to the repository root.
        // (You can prepend a slash if you like, but it doesn't do anything special.)
        val gitIgnore = GitIgnore("logs/debug.log")

        assertIgnore(gitIgnore, "logs/debug.log")

        TestUtils.assertNotIgnore(gitIgnore, "debug.log")
        TestUtils.assertNotIgnore(gitIgnore, "build/logs/debug.log")

        // (You can prepend a slash if you like, but it doesn't do anything special.)
        assertIgnore(gitIgnore, "/logs/debug.log")

        TestUtils.assertNotIgnore(gitIgnore, "/debug.log")
        TestUtils.assertNotIgnore(gitIgnore, "/build/logs/debug.log")
    }

    // ------------------------------------------
    @Test
    fun testIgnoreEscapedSpecials() {
        val gitIgnore = GitIgnore("foo\\[01\\].txt ")

        assertIgnore(gitIgnore, "foo[01].txt")
        TestUtils.assertNotIgnore(gitIgnore, "foo01.txt")
        TestUtils.assertNotIgnore(gitIgnore, "foo0.txt")
        TestUtils.assertNotIgnore(gitIgnore, "foo1.txt")
    }

    @Test
    fun testIgnoreBaseDir() {
        // To ensure all variants work as expected.
        testIgnoreBaseDir("src/test")
        testIgnoreBaseDir("/src/test")
        testIgnoreBaseDir("src/test/")
        testIgnoreBaseDir("/src/test/")
    }

    fun testIgnoreBaseDir(baseDir: String) {
        val gitIgnore = GitIgnore(baseDir, "*.properties")
        gitIgnore.verbose = true
        assertIgnore(gitIgnore, "src/test/test.properties")
        assertIgnore(gitIgnore, "/src/test/test.properties")

        // Note: This actually contains the '/src/test/'
        TestUtils.assertNotIgnore(gitIgnore, "/somethingelse/src/test/test.properties")

        // Note: This actually contains the 'src/test'
        TestUtils.assertNotIgnore(gitIgnore, "src/test.properties")

        // Note: This actually contains the 'src/test'
        TestUtils.assertNotIgnore(gitIgnore, "foo/src/test/something.properties")

        TestUtils.assertNotIgnore(gitIgnore, "src/main/test.properties")
        TestUtils.assertNotIgnore(gitIgnore, "test.properties")

        // To ensure more code coverage
        gitIgnore.verbose = false
        TestUtils.assertNotIgnore(gitIgnore, "foo/src/test/something.properties")
    }

    // ------------------------------------------
    @Test
    @Throws(IOException::class)
    fun testIgnoreFromFile() {
        val url = this.javaClass
            .classLoader
            .getResource("gitignore_fileread")

        assertNotNull(url)

        val gitIgnore = GitIgnore(File(url.file))

        gitIgnore.verbose = true

        assertIgnore(gitIgnore, "foo[01].txt")
        TestUtils.assertNotIgnore(gitIgnore, "foo01.txt")
        TestUtils.assertNotIgnore(gitIgnore, "foo0.txt")
        TestUtils.assertNotIgnore(gitIgnore, "foo1.txt")
        assertIgnore(gitIgnore, "src/test/test.properties")
        assertIgnore(gitIgnore, "/src/test/test.properties")
        TestUtils.assertNotIgnore(gitIgnore, "/src/test/bla.properties")
    }

    // ------------------------------------------
    @Test
    fun testGeneratedRegexesBasedir() {
        // Patterns defined after a negating pattern will re-ignore any previously negated files.
        val gitIgnore = GitIgnore(
            "*.log\n" +
                    "!important/*.log\n" +
                    "trace.* "
        )
        assertIgnore(gitIgnore, "debug.log")
        assertIgnore(gitIgnore, "trace.txt")
        assertIgnore(gitIgnore, "important/trace.log")
        TestUtils.assertNotIgnore(gitIgnore, "important/debug.log")

        // Verify the created ignore rules.
        assertEquals(3, gitIgnore.ignoreRules.size)
        for (ignoreRule in gitIgnore.ignoreRules) {
            when (ignoreRule.ignoreExpression) {
                "*.log" ->
                    assertEquals("^/?.*\\.log(/|$)", ignoreRule.ignorePattern.pattern)
                "!important/*.log" ->
                    assertEquals("^/?important/[^/]*\\.log(/|$)", ignoreRule.ignorePattern.pattern)

                "trace.*" -> assertEquals("^/?(.*/)?trace\\.[^/]*", ignoreRule.ignorePattern.pattern)
                else -> fail("Unexpected expression:${ignoreRule.ignoreExpression}")
            }
        }
    }

    @Test
    fun testSubdirs() {
        assertIgnore("", "/*.log", "/test.log")
        assertIgnore("", "/*.log", "/docs/test.log")
        assertIgnore("", "/*.log", "/src/test.log")
        assertIgnore("", "/*.log", "/src/main/test.log")
        assertIgnore("", "/*.log", "/src/test/test.log")

        TestUtils.assertNotIgnore("src", "/*.log", "/test.log")
        TestUtils.assertNotIgnore("src", "/*.log", "/docs/test.log")
        assertIgnore("src", "/*.log", "/src/test.log")
        assertIgnore("src", "/*.log", "/src/main/test.log")
        assertIgnore("src", "/*.log", "/src/test/test.log")

        TestUtils.assertNotIgnore("src/main", "/*.log", "/test.log")
        TestUtils.assertNotIgnore("src/main", "/*.log", "/docs/test.log")
        TestUtils.assertNotIgnore("src/main", "/*.log", "/src/test.log")
        assertIgnore("src/main", "/*.log", "/src/main/test.log")
        TestUtils.assertNotIgnore("src/main", "/*.log", "/src/test/test.log")
    }


    private fun verifyBaseDir(gitIgnore: GitIgnore, baseDir: String?, matchingFilename: String) {
        assertEquals(baseDir, gitIgnore.projectRelativeBaseDir, "Wrong basedir in GitIgnore")
        for (ignoreRule in gitIgnore.ignoreRules) {
            assertEquals(baseDir, ignoreRule.ignoreBasedir, "Wrong basedir in rule")
        }
        assertEquals(
            true,
            gitIgnore.isIgnoredFile(matchingFilename),
            "The filename $matchingFilename should have matched($gitIgnore)."
        )
    }

    @Test
    fun testBaseDir() {
        verifyBaseDir(GitIgnore("", "test.md"), "/", "test.md")
        verifyBaseDir(GitIgnore("/", "test.md"), "/", "test.md")

        verifyBaseDir(GitIgnore("foo", "test.md"), "/foo/", "foo/test.md")
        verifyBaseDir(GitIgnore("/foo", "test.md"), "/foo/", "foo/test.md")
        verifyBaseDir(GitIgnore("foo/", "test.md"), "/foo/", "foo/test.md")
        verifyBaseDir(GitIgnore("/foo/", "test.md"), "/foo/", "foo/test.md")

        verifyBaseDir(GitIgnore("foo/bar", "test.md"), "/foo/bar/", "foo/bar/test.md")
        verifyBaseDir(GitIgnore("/foo/bar", "test.md"), "/foo/bar/", "foo/bar/test.md")
        verifyBaseDir(GitIgnore("foo/bar/", "test.md"), "/foo/bar/", "foo/bar/test.md")
        verifyBaseDir(GitIgnore("/foo/bar/", "test.md"), "/foo/bar/", "foo/bar/test.md")
    }

    @Test
    fun testGeneratedRegexesSubdir() {
        TestUtils.verifyGeneratedRegex("", "*.txt", "^/?.*\\.txt(/|$)")
        TestUtils.verifyGeneratedRegex("src/", "*.log", "^/?\\Qsrc/\\E.*\\.log(/|$)")
        TestUtils.verifyGeneratedRegex("src/main/", "*.md", "^/?\\Qsrc/main/\\E.*\\.md(/|$)")
    }

    // ------------------------------------------
    @Test
    fun testGitDocumentation() {
        // From the git documentation:

        // If there is a separator at the beginning or middle (or both) of the pattern,
        // then the pattern is relative to the directory level of the particular .gitignore file itself.
        // Otherwise, the pattern may also match at any level below the .gitignore level.

        var gitIgnore: GitIgnore?
        // a pattern doc/frotz/ matches doc/frotz directory, but not a/doc/frotz directory;
        gitIgnore = GitIgnore("doc/frotz/")
        assertIgnore(gitIgnore, "doc/frotz/")
        assertIgnore(gitIgnore, "doc/frotz/file.txt")
        TestUtils.assertNotIgnore(gitIgnore, "a/doc/frotz/")
        TestUtils.assertNotIgnore(gitIgnore, "a/doc/frotz/file.txt")

        // however frotz/ matches frotz and a/frotz that is a directory (all paths are relative from the .gitignore file).
        gitIgnore = GitIgnore("frotz/")
        assertIgnore(gitIgnore, "frotz/")
        assertIgnore(gitIgnore, "frotz/file.txt")
        assertIgnore(gitIgnore, "a/frotz/")
        assertIgnore(gitIgnore, "a/frotz/file.txt")

        // Beginning
        gitIgnore = GitIgnore("/frotz")
        assertIgnore(gitIgnore, "frotz/")
        assertIgnore(gitIgnore, "frotz/file.txt")
        TestUtils.assertNotIgnore(gitIgnore, "a/frotz/")
        TestUtils.assertNotIgnore(gitIgnore, "a/frotz/file.txt")

        // Both
        gitIgnore = GitIgnore("/doc/frotz/")
        assertIgnore(gitIgnore, "doc/frotz/")
        assertIgnore(gitIgnore, "doc/frotz/file.txt")
        TestUtils.assertNotIgnore(gitIgnore, "a/doc/frotz/")
        TestUtils.assertNotIgnore(gitIgnore, "a/doc/frotz/file.txt")
    }

    @Test
    fun testGitDocumentationSlashesAndStars() {
        // From the git documentation:
        var gitIgnore: GitIgnore

        // Put a backslash ("\") in front of the first "!" for patterns that
        // begin with a literal "!", for example, "\!important!.txt".
        gitIgnore = GitIgnore("\\!important!.txt")
        assertIgnore(gitIgnore, "!important!.txt")

        // An asterisk "*" matches anything except a slash.
        gitIgnore = GitIgnore("foo*bar")
        assertIgnore(gitIgnore, "foo_bar")
        TestUtils.assertNotIgnore(gitIgnore, "foo/bar")
        assertIgnore(gitIgnore, "foo_____bar")
        TestUtils.assertNotIgnore(gitIgnore, "foo__/__bar")

        // The character "?" matches any one character except "/".
        gitIgnore = GitIgnore("foo?bar")
        assertIgnore(gitIgnore, "foo_bar")
        assertIgnore(gitIgnore, "foo.bar")
        TestUtils.assertNotIgnore(gitIgnore, "foo/bar")
    }

    @Test
    fun testEmptyIgnoreRules() {
        val gitIgnore = GitIgnore("")
        TestUtils.assertNotIgnore(gitIgnore, "debug.log")
        TestUtils.assertNotIgnore(gitIgnore, "logs/debug.log")
    }

    @Test
    fun testBaseDirNull() {
        val rule = GitIgnore.IgnoreRule(null, false, "debug.log", true)
        assertTrueAndNotNull(rule.isIgnoredFile("debug.log"))
        assertTrueAndNotNull(rule.isIgnoredFile("logs/debug.log"))
    }

    @Test
    fun testBaseDirEmpty() {
        val rule = GitIgnore.IgnoreRule("", false, "debug.log", true)
        assertTrueAndNotNull(rule.isIgnoredFile("debug.log"))
        assertTrueAndNotNull(rule.isIgnoredFile("logs/debug.log"))
    }

    @Test
    fun testBaseDirRoot() {
        val rule = GitIgnore.IgnoreRule("/", false, "debug.log", true)
        assertTrueAndNotNull(rule.isIgnoredFile("debug.log"))
        assertTrueAndNotNull(rule.isIgnoredFile("logs/debug.log"))
    }

    @Test
    fun testBadExpression() {
        assertFailsWith<PatternSyntaxException> { GitIgnore("[[[[[***+++") }
    }

    private fun assertTrueAndNotNull(condition: Boolean?) {
        assertNotNull(condition)
        assertTrue(condition)
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(TestGitIgnore::class.java)
    }
}
