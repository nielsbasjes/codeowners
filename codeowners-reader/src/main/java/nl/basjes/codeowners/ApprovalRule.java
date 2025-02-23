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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ApprovalRule {
    private final String fileExpression;
    private final List<String> approvers;
    private final Pattern filePattern;
    private boolean verbose = false;

    public ApprovalRule(String fileExpression, List<String> approvers) {
        this.fileExpression = fileExpression;
        this.approvers = approvers;

        String fileRegex = fileExpression
            .trim() // Clear leading and trailing spaces

            .replace("\\ ", " ") // The escaped spaces must become spaces again.

            // If a path does not start with a /, the path is treated as if it starts with a globstar. README.md is treated the same way as /**/README.md
            .replaceAll("^([^/*.])", "/**/$1")
            // "/foo" --> End can be a filename (so we pin to the end) or a directory name (so we expect another / )
            .replaceAll("([^/*])$", "$1(/|\\$)")

            .replace(".", "\\.") // Avoid bad wildcards
            .replace("\\.*", "\\..*")//  matching  /.* onto /.foo/bar.xml
            .replace("?", ".")   // Single character match

            // The Globstar "foo/**/bar" must also match "foo/bar"
            .replace("/**", "(/.*)?")

            // The wildcard "foo/*/bar" must match exactly 1 subdir "foo/something/bar"
            // and not "foo/bar", "foo//bar" or "foo/something/something/bar"
            .replace("/*/", "/[^/]+/")
            .replace("/*/", "/[^/]+/")

            .replace("**", ".*") // Convert to the Regex wildcards

            .replaceAll("^\\*", ".*") // Match anything at the start

            .replaceAll("^/", "^/") // If starts with / then pin to the start.

            .replaceAll("/\\*([^/]*)$", "/[^/]*$1\\$") // A trailing '/*something' means NO further subdirs should be matched

            .replace("/*", "/.*") // "/foo/*\.js"  --> "/foo/.*\.js"

            .replaceAll("([^.\\]])\\*", "$1.*") // Match anything at the start

            .replaceAll("/+", "/") // Remove duplication
            ;

        filePattern = Pattern.compile(fileRegex);
    }

    /**
     * @return The provided file expression used to build this rule
     */
    public String getFileExpression() {
        return fileExpression;
    }

    /**
     * @return The Pattern which was constructed from the provided fileExpression
     */
    public Pattern getFilePattern() {
        return filePattern;
    }

    /**
     * @return All approvers (in the same order as they are in the file) that will be returned IF
     * the file pattern matches.
     */
    public List<String> getApprovers() {
        return approvers;
    }

    /**
     * @return The approvers (in the same order as they are in the file) if the provided file matches the
     * configured fileExpression. If not it returns null.
     */
    public List<String> getApprovers(String filename) {
        if (!filePattern.matcher(filename).find()) {
            if (verbose) {
                CodeOwners.LOG.info("NO MATCH  |{}| ~ |{}| --> {}", fileExpression, filePattern, filename);
            }
            return null;
        }
        if (verbose) {
            CodeOwners.LOG.info("MATCH     |{}| ~ |{}| --> {}    approvers:{}", fileExpression, filePattern, filename, approvers);
        }
        return new ArrayList<>(approvers);
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (verbose) {
            result.append("# Regex used for the next rule:   ").append(filePattern).append('\n');
        }
        return result.append(fileExpression).append(" ").append(String.join(" ", approvers)).toString();
    }
}
