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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.regex.PatternSyntaxException

/**
 * A class that holds a single .gitignore file
 */
class GitIgnore @JvmOverloads constructor(
    projectRelativeBaseDir: String,
    gitIgnoreContent: String,
    verbose: Boolean = false
) {
    @JvmField
    val projectRelativeBaseDir: String = standardizeFilename(projectRelativeBaseDir + GITIGNORE_PATH_SEPARATOR)

    internal val ignoreRules: List<IgnoreRule>
    var verbose = false
        set(verbose) {
            field = verbose
            ignoreRules.forEach{ it.setVerbose(verbose) }
        }

    // Load the gitignore from a file
    constructor(file: File) : this("", file)

    constructor(projectRelativeBaseDir: String, file: File) : this(projectRelativeBaseDir, readFileToString(file))

    constructor(gitIgnoreContent: String) : this("", gitIgnoreContent)

    constructor(gitIgnoreContent: String, verbose: Boolean) : this("", gitIgnoreContent, verbose)

    init {

        val allIgnoreRules: MutableList<IgnoreRule> = mutableListOf()

        for (line in gitIgnoreContent.lines()) {
            if (line.isBlank()) {
                continue
            }
            if (line.startsWith("#")) {
                continue
            }
            if (line.startsWith("!")) {
                allIgnoreRules.add(IgnoreRule(this.projectRelativeBaseDir, true, line.trim().substring(1), verbose))
            } else {
                allIgnoreRules.add(IgnoreRule(this.projectRelativeBaseDir, false, line.trim(), verbose))
            }
        }
        ignoreRules = allIgnoreRules.toList()
        this.verbose = verbose // The setter NEEDS the ignoreRules !
    }

    /**
     * Checks if the file matches the stored expressions.
     * @param filename The filename to be checked (which is a project relative filename).
     * @return NULL: not matched, True: must be ignored, False: it must be UNignored
     */
    fun isIgnoredFile(filename: String): Boolean? {
        if (verbose) {
            LOG.info("# vvvvvvvvvvvvvvvvvvvvvvvvvvv")
            LOG.info("Checking: {}", filename)
        }
        val matchFileName: String = standardizeFilename(filename)

        if (verbose) {
            LOG.info("Matching: {}", matchFileName)
        }

        // If a file is NOT matched at all then there is no verdict.
        var mustBeIgnored: Boolean? = null

        if (!matchFileName.startsWith(projectRelativeBaseDir)) {
            if (verbose) {
                LOG.info("# Not in my baseDir: {}", projectRelativeBaseDir)
            }
        } else {
            for (ignoreRule in ignoreRules) {
                val ruleVerdict = ignoreRule.isIgnoredFile(matchFileName) ?: continue
                mustBeIgnored = ruleVerdict

                // Handling the "bug" in git that results in unexpected ignores
                if (ruleVerdict && ignoreRule.isDirectoryMatch) {
                    // From: https://www.atlassian.com/git/tutorials/saving-changes/gitignore
                    // These rules
                    //    logs/
                    //    !logs/important.log
                    // ignore these files
                    //    logs/debug.log
                    //    logs/important.log
                    // Documentation:
                    //    Wait a minute! Shouldn't logs/important.log be negated in the example on the left
                    //    Nope! Due to a performance-related quirk in Git, you can not negate a file that
                    //    is ignored due to a pattern matching a directory

                    // So if there was a directory match rule that ignores this file then that one always wins.

                    break
                }
            }
        }

        if (verbose) {
            when(mustBeIgnored) {
                null  -> LOG.info("Conclusion: Not matched: Not ignored")
                true  -> LOG.info("Conclusion: Must be ignored")
                false -> LOG.info("Conclusion: Must NOT be ignored")
            }
        }
        return mustBeIgnored
    }

    /**
     * Checks if the file matches the stored expressions.
     * This is NOT suitable for combining multiple sets of rules!
     * @param filename The filename to be checked (which is a project relative filename).
     * @return true: must be ignored, false: it must be not be ignored
     */
    fun ignoreFile(filename: String): Boolean {
        return isIgnoredFile(filename) ?: false
    }

    /**
     * Checks if the file matches the stored expressions.
     * This is NOT suitable for combining multiple sets of rules!
     * @param filename The filename to be checked (which is a project relative filename).
     * @return true: must be kept, false: it must be ignored
     */
    fun keepFile(filename: String): Boolean {
        return !ignoreFile(filename)
    }


    override fun toString(): String {
        val result = StringBuilder()
        result.append("# GitIgnore content in ").append(projectRelativeBaseDir).append("\n")

        for (ignoreRule in ignoreRules) {
            result.append(ignoreRule).append('\n')
        }
        return result.toString()
    }

    // Internal, package private for testing purposes
    internal class IgnoreRule(
        projectRelativeBaseDir: String?,
        negate: Boolean,
        fileExpression: String,
        private var verbose: Boolean
    ) {
        /**
         * @return The directory in which this gitIgnore was located
         */
        val ignoreBasedir: String
        private val negate: Boolean
        private val fileExpression: String

        /**
         * There is a strange edgecase in git where a rule that matches an entire directory
         * disables ignore rules in that same directory.
         * @return Is this rule matching an entire directory tree?
         */
        val isDirectoryMatch: Boolean

        /**
         * @return The basedir and ignore expression combined into a regular expression.
         */
        val ignorePattern: Regex

        init {
            val baseDirRegex: String?
            if (projectRelativeBaseDir == null ||
                "/" == projectRelativeBaseDir ||
                projectRelativeBaseDir.trim().isEmpty()
            ) {
                this.ignoreBasedir = "/"
                baseDirRegex = "^/?" // The leading slash is optional
            } else {
                // Enforce the base dir starts and ends with a single '/'
                this.ignoreBasedir =
                    ("/" + projectRelativeBaseDir.trim() + "/").replace("/+".toRegex(), "/")
                baseDirRegex = "^/?\\Q" + this.ignoreBasedir.substring(1) + "\\E"
            }

            this.negate = negate
            this.fileExpression = fileExpression
            this.isDirectoryMatch = !negate && fileExpression.endsWith("/")

            var fileRegex = fileExpression
                .trim()  // Clear leading and trailing spaces

                // Strip the negation at the start of the line (if any)
                .replace("^!".toRegex(), "")

                // Fix the 'not' range or 'not' set
                .replace("[!", "[^")

            val literalStarMarker = "||>s<||"
            val literalQuestionMarker = "||>q<||"

            fileRegex = fileRegex
                // Move the escaped * to something special that does not have a * in it
                .replace("\\*", literalStarMarker)
                // Move the escaped ? to something special that does not have a ? in it
                .replace("\\?", literalQuestionMarker)
                // The character "?" matches any one character except "/".
                .replace("?", "[^/]")

            if (fileExpression.contains("/") && !fileExpression.endsWith("/")) {
                // Patterns specifying a file in a particular directory are relative to the repository root.
                if (fileRegex.startsWith("/")) {
                    fileRegex = fileRegex.substring(1)
                }
            } else {
                // If there is a separator at the beginning or middle (or both) of the pattern,
                // then the pattern is relative to the directory level of the particular .gitignore file itself.
                // Otherwise, the pattern may also match at any level below the .gitignore level.
                if (!Regex("./.").containsMatchIn(fileRegex)) {
                    // If a path does not start with a /, the path is treated as if it starts with a globstar. README.md is treated the same way as /**/README.md
                    fileRegex = fileRegex.replace("^([^/*])".toRegex(), "**/$1")
                }
            }

            fileRegex = fileRegex
                // The escaped spaces must become spaces again.
                .replace("\\ ", " ")

                // Some characters do NOT have a special meaning
                .replace("$", "\\$")
                .replace("(", "\\(")
                .replace(")", "\\)")

            // Convert cases like
            //     coverage*[.json, .xml, .info]
            // into
            //     coverage*(.json|.xml|.info)
            if (fileRegex.contains("[") && fileRegex.contains(",")) {
                var changed = false
                while (true) {
                    val newRegex = fileRegex.replace("\\[([^]]+) *, *([^]]+)]".toRegex(), "[$1|$2]")
                    if (newRegex == fileRegex) {
                        break
                    }
                    fileRegex = newRegex
                    changed = true
                }
                if (changed) {
                    fileRegex = fileRegex.replace("\\[([^]]+\\|[^]]+)]".toRegex(), "($1)")
                }
            }

            fileRegex =
                fileRegex
                    // "/foo" --> End can be a filename (so we pin to the end) or a directory name (so we expect another / )
                    .replace("([^/*])$".toRegex(), "$1(/|\\$)")

                    // Avoid bad wildcards
                    .replace(".", "\\.")

                    //  matching  /.* onto /.foo/bar.xml
                    .replace("\\.*", "\\.[^/]*")

                    // Single character match
                    .replace("?", "[^/]")

                    // The Globstar "/**/bar" must also match "bar"
                    .replace("^\\*\\*/".toRegex(), "(.*/)?")

                    // The Globstar "foo/**/bar" must also match "foo/bar"
                    .replace(
                        "/**",
                        "(/.*)?"
                    )

                    // The wildcard "foo/*/bar" must match exactly 1 subdir "foo/something/bar"
                    // and not "foo/bar", "foo//bar" or "foo/something/something/bar"
                    .replace("/*/", "/[^/]+/")
                    .replace("/*/", "/[^/]+/")

                    // Convert to the Regex wildcards
                    .replace("**", ".*")

                    // Match anything at the start
                    .replace("^\\*".toRegex(), ".*")

                    // If starts with / then pin to the start.
                    .replace("^/".toRegex(), "^/")

                    // A trailing '/*something' means NO further subdirs should be matched
                    .replace("/\\*([^/]*)$".toRegex(), "/[^/]*$1\\$")

                    // "/foo/*\.js"  --> "/foo/.*\.js"
                    .replace("/*", "/[^/]*")

                    // Match anything at the start
                    .replace("([^.\\]])\\*".toRegex(), "$1[^/]*")

                    // Move the 'something special' to the literal *
                    .replace(literalStarMarker, "\\Q*\\E")

                    // Move the 'something special' to the literal ?
                    .replace(literalQuestionMarker, "\\Q?\\E")

                    .replace("/+".toRegex(), "/") // Remove duplication

                    .replace("/\\E/", "/\\E") // Remove '/' duplication around a literal marker

            val finalRegex = baseDirRegex + fileRegex
            try {
                this.ignorePattern = Regex(finalRegex)
                if (verbose) {
                    LOG.info("IgnoreRule for expression {}   -->   Regex {}", this.fileExpression, fileRegex)
                }
            } catch (pse: PatternSyntaxException) {
                val errorMsg =
                    "You either have an invalid gitignore rule (which you should fix) " +
                    "or you have found an edge case that should be fixed. " +
                    "In the latter case please file a bug report to https://github.com/nielsbasjes/codeowners/issues " +
                    "indicating that the expression >>>" + (if (negate) "!" else "") + this.fileExpression + "<<< " +
                    "was converted to regex >>>" + finalRegex + "<<< " +
                    "which triggered the error: " + pse.message
                throw PatternSyntaxException(errorMsg, finalRegex, pse.index)
            }
        }

        /**
         * Checks if the file matches the stored expression.
         * @param filename The filename to be checked
         * @return NULL: not matched, True: must be ignored, False: it must be UNignored
         */
        fun isIgnoredFile(filename: String): Boolean? {
            if (!ignorePattern.containsMatchIn((filename))) {
                if (verbose) {
                    LOG.info(
                        "NO MATCH     |{}| ~ |{}| --> |{}|", fileExpression,
                        this.ignorePattern, filename
                    )
                }
                return null
            }
            if (negate) {
                if (verbose) {
                    LOG.info(
                        "MATCH NEGATE |{}| ~ |{}| --> |{}|", fileExpression,
                        this.ignorePattern, filename
                    )
                }
                return false
            } else {
                if (verbose) {
                    LOG.info(
                        "MATCH IGNORE |{}| ~ |{}| --> |{}|", fileExpression,
                        this.ignorePattern, filename
                    )
                }
                return true
            }
        }

        val ignoreExpression: String
            /**
             * @return The ignore expression as it was present in the .gitignore file
             */
            get() = (if (negate) "!" else "") + fileExpression

        fun setVerbose(verbose: Boolean) {
            this.verbose = verbose
        }

        override fun toString(): String {
            return String.format(
                "%-20s     # Used Regex: %s", (if (negate) "!" else "") + fileExpression,
                this.ignorePattern
            )
        }
    }

    companion object {
        // This is the separator dictated in the gitignore documentation (same as Linux/Unix)
        private const val GITIGNORE_PATH_SEPARATOR = "/"

        private val LOG: Logger = LoggerFactory.getLogger(GitIgnore::class.java)

        @Throws(IOException::class)
        private fun readFileToString(file: File): String {
            return String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
        }

        fun separatorsToUnix(path: String): String {
            return path.replace("\\", "/")
        }

        /**
         * Converts filename to unix convention and ensures it starts with a /
         * @param filename The filename to clean
         * @return A standardized form.
         */
        @JvmStatic
        fun standardizeFilename(filename: String): String {
            var unixifiedName: String = separatorsToUnix(filename)
            if (!unixifiedName.matches("^[a-zA-Z]:/.*".toRegex())) {
                unixifiedName = GITIGNORE_PATH_SEPARATOR + unixifiedName
            }
            return unixifiedName.replace("/+".toRegex(), "/")
        }
    }
}
