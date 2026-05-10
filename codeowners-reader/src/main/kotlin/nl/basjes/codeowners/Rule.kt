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
package nl.basjes.codeowners

import java.util.regex.Pattern

abstract class Rule(
    /**
     * @return The provided file expression used to build this rule
     */
    @JvmField val fileExpression: String
) {
    fun getFileExpression() = fileExpression
    /**
     * The Pattern which was constructed from the provided fileExpression
     */
    val filePattern: Pattern

    var verbose: Boolean = false

    init {

        val fileRegex = fileExpression
            .trim { it <= ' ' }  // Clear leading and trailing spaces

            .replace("\\ ", " ") // The escaped spaces must become spaces again.
            // If a path does not start with a /, the path is treated as if it starts with a globstar. README.md is treated the same way as /**/README.md

            .replace(
                "^([^/*])".toRegex(),
                "/**/$1"
            ) // "/foo" --> End can be a filename (so we pin to the end) or a directory name (so we expect another / )
            .replace("([^/*])$".toRegex(), "$1(/|\\$)")

            .replace(".", "\\.") // Avoid bad wildcards
            .replace("\\.*", "\\..*") //  matching  /.* onto /.foo/bar.xml
            .replace("?", ".") // Single character match
            // The Globstar "foo/**/bar" must also match "foo/bar"
            // Process trailing /** before middle /** to handle them correctly:
            // 1. Trailing: "foo/**" matches "foo/" and its contents, but NOT "foo" or "foobar"

            .replace(
                "/\\*\\*$".toRegex(),
                "/.*"
            ) // 2. Middle: "foo/**/bar" matches both "foo/bar" and "foo/anything/bar"
            .replace("/**", "(/.*)?") // The wildcard "foo/*/bar" must match exactly 1 subdir "foo/something/bar"
            // and not "foo/bar", "foo//bar" or "foo/something/something/bar"

            .replace("/*/", "/[^/]+/")
            .replace("/*/", "/[^/]+/")

            .replace("**", ".*") // Convert to the Regex wildcards

            .replace("^\\*".toRegex(), ".*") // Match anything at the start

            .replace("^/".toRegex(), "^/") // If starts with / then pin to the start.

            .replace(
                "/\\*([^/]*)$".toRegex(),
                "/[^/]*$1\\$"
            ) // A trailing '/*something' means NO further subdirs should be matched

            .replace("/*", "/.*") // "/foo/*\.js"  --> "/foo/.*\.js"

            .replace("([^.\\]])\\*".toRegex(), "$1.*") // Match anything at the start

            .replace("/+".toRegex(), "/")
        // Remove duplication


        filePattern = Pattern.compile(fileRegex)
    }

    /**
     * @return True if the provided file matches the configured fileExpression. If not it returns false.
     */
    fun matches(filename: String): Boolean {
        val matches = filePattern.matcher(filename).find()
        if (verbose) {
            if (matches) {
                LOG.info("MATCH     |{}| ~ |{}| --> {}", fileExpression, filePattern, filename)
            } else {
                LOG.info("NO MATCH  |{}| ~ |{}| --> {}", fileExpression, filePattern, filename)
            }
        }
        return matches
    }

}
