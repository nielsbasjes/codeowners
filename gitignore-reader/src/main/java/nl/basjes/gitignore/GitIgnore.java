/*
 * CodeOwners Tools
 * Copyright (C) 2023-2024 Niels Basjes
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.lang.Boolean.TRUE;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A class that holds a single .gitignore file
 */
public class GitIgnore {

    // This is the separator dictated in the gitignore documentation (same as Linux/Unix)
    private static final String GITIGNORE_PATH_SEPARATOR = "/";

    private static final Logger LOG = LoggerFactory.getLogger(GitIgnore.class);

    private final String projectRelativeBaseDir;

    private final List<IgnoreRule> ignoreRules = new ArrayList<>();
    private boolean verbose;

    // Load the gitignore from a file
    public GitIgnore(File file) throws IOException {
        this("", file);
    }

    public GitIgnore(String projectRelativeBaseDir, File file) throws IOException {
        this(projectRelativeBaseDir, readFileToString(file));
    }

    private static String readFileToString(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), UTF_8);
    }

    public GitIgnore(String gitIgnoreContent) {
        this("", gitIgnoreContent);
    }

    public GitIgnore(String gitIgnoreContent, boolean verbose) {
        this("", gitIgnoreContent, verbose);
    }

    public GitIgnore(String projectRelativeBaseDir, String gitIgnoreContent) {
        this(projectRelativeBaseDir, gitIgnoreContent, false);
    }

    public GitIgnore(String projectRelativeBaseDir, String gitIgnoreContent, boolean verbose) {
        this.verbose = verbose;
        this.projectRelativeBaseDir = standardizeFilename(projectRelativeBaseDir + GITIGNORE_PATH_SEPARATOR);

        if (gitIgnoreContent == null) {
            return; // Nothing to read
        }

        BufferedReader reader = new BufferedReader(new StringReader(gitIgnoreContent));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("!")) {
                    ignoreRules.add(new IgnoreRule(this.projectRelativeBaseDir, true, line.substring(1), verbose));
                } else {
                    ignoreRules.add(new IgnoreRule(this.projectRelativeBaseDir, false, line, verbose));
                }
            }
        } catch (IOException io) {
            LOG.error("Got an IOException while reading the gitignore file content: {}", io.toString());
        }

    }

    /**
     * Checks if the file matches the stored expressions.
     * @param filename The filename to be checked (which is a project relative filename).
     * @return NULL: not matched, True: must be ignored, False: it must be UNignored
     */
    public Boolean isIgnoredFile(String filename) {
        if (verbose) {
            LOG.info("# vvvvvvvvvvvvvvvvvvvvvvvvvvv");
            LOG.info("Checking: {}", filename);
        }
        String matchFileName = standardizeFilename(filename);

        if (verbose) {
            LOG.info("Matching: {}", matchFileName);
        }

        // If a file is NOT matched at all then there is no verdict.
        Boolean mustBeIgnored = null;

        if (!matchFileName.startsWith(projectRelativeBaseDir)) {
            if (verbose) {
                LOG.info("# Not in my baseDir: {}", projectRelativeBaseDir);
            }
        } else {
            for (IgnoreRule ignoreRule : ignoreRules) {
                Boolean ruleVerdict = ignoreRule.isIgnoredFile(matchFileName);
                if (ruleVerdict == null) {
                    continue;
                }
                mustBeIgnored = ruleVerdict;

                // Handling the "bug" in git that results in unexpected ignores
                if (ruleVerdict && ignoreRule.isDirectoryMatch()) {
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
                    break;
                }
            }
        }

        if (verbose) {
            if (mustBeIgnored == null) {
                LOG.info("Conclusion: Not matched: Not ignored");
            } else {
                if (TRUE.equals(mustBeIgnored)) {
                    LOG.info("Conclusion: Must be ignored");
                } else {
                    LOG.info("Conclusion: Must NOT be ignored");
                }
            }
        }
        return mustBeIgnored;
    }

    public static String separatorsToUnix(String path) {
        return path.replace("\\", "/");
    }

    /**
     * Converts filename to unix convention and ensures it starts with a /
     * @param filename The filename to clean
     * @return A standardized form.
     */
    static String standardizeFilename(String filename) {
        String unixifiedName = separatorsToUnix(filename);
        if (!unixifiedName.matches("^[a-zA-Z]:/.*")) {
            unixifiedName = GITIGNORE_PATH_SEPARATOR + unixifiedName;
        }
        return unixifiedName.replaceAll("/+", "/");
    }

    /**
     * Checks if the file matches the stored expressions.
     * This is NOT suitable for combining multiple sets of rules!
     * @param filename The filename to be checked (which is a project relative filename).
     * @return true: must be ignored, false: it must be not be ignored
     */
    public boolean ignoreFile(String filename) {
        return TRUE.equals(isIgnoredFile(filename));
    }

    /**
     * Checks if the file matches the stored expressions.
     * This is NOT suitable for combining multiple sets of rules!
     * @param filename The filename to be checked (which is a project relative filename).
     * @return true: must be kept, false: it must be ignored
     */
    public boolean keepFile(String filename) {
        return !ignoreFile(filename);
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
        ignoreRules.forEach(rule->rule.setVerbose(verbose));
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("# GitIgnore content in ").append(projectRelativeBaseDir).append("\n");

        for (IgnoreRule ignoreRule : ignoreRules) {
            result.append(ignoreRule).append('\n');
        }
        return result.toString();
    }

    public String getProjectRelativeBaseDir() {
        return projectRelativeBaseDir;
    }

    // Only for testing purposes
    List<IgnoreRule> getIgnoreRules() {
        return new ArrayList<>(ignoreRules);
    }

    // Internal, package private for testing purposes
    static class IgnoreRule {
        private final String projectRelativeBaseDir;
        private final boolean negate;
        private final String fileExpression;
        private final boolean directoryMatch;
        private final Pattern filePattern;
        private boolean verbose;

        public IgnoreRule(String projectRelativeBaseDir, boolean negate, final String fileExpression, boolean verbose) {
            this.verbose = verbose;
            String baseDirRegex;
            if (projectRelativeBaseDir == null || "/".equals(projectRelativeBaseDir) || projectRelativeBaseDir.trim().isEmpty()) {
                this.projectRelativeBaseDir = "/";
                baseDirRegex = "^/?"; // The leading slash is optional
            } else {
                // Enforce the base dir starts and ends with a single '/'
                this.projectRelativeBaseDir = ("/" + projectRelativeBaseDir.trim() + "/").replaceAll("/+", "/");
                baseDirRegex = "^/?\\Q" + this.projectRelativeBaseDir.substring(1) + "\\E";
            }

            this.negate = negate;
            this.fileExpression = fileExpression;
            this.directoryMatch = !negate && fileExpression.endsWith("/");

            String fileRegex = fileExpression
                .trim() // Clear leading and trailing spaces

                .replaceAll("^!", "") // Strip the negation at the start of the line (if any)

                .replace("[!", "[^") // Fix the 'not' range or 'not' set
                ;

            final String LITERAL_STAR_MARKER = "||>s<||";
            final String LITERAL_QUESTION_MARK = "||>q<||";

            fileRegex = fileRegex
                .replace("\\*", LITERAL_STAR_MARKER) // Move the escaped * to something special that does not have a * in it
                .replace("\\?", LITERAL_QUESTION_MARK) // Move the escaped ? to something special that does not have a ? in it
                .replace("?", "[^/]"); // The character "?" matches any one character except "/".

            if (fileExpression.contains("/") && !fileExpression.endsWith("/")) {
                // Patterns specifying a file in a particular directory are relative to the repository root.
                if (fileRegex.startsWith("/")) {
                    fileRegex = fileRegex.substring(1);
                }
            } else {
                // If there is a separator at the beginning or middle (or both) of the pattern,
                // then the pattern is relative to the directory level of the particular .gitignore file itself.
                // Otherwise, the pattern may also match at any level below the .gitignore level.
                if (!Pattern.compile("./.").matcher(fileRegex).find()) {
                    // If a path does not start with a /, the path is treated as if it starts with a globstar. README.md is treated the same way as /**/README.md
                    fileRegex = fileRegex.replaceAll("^([^/*])", "**/$1");
                }
            }

            fileRegex = fileRegex
                .replace("\\ ", " ") // The escaped spaces must become spaces again.

                // Some characters do NOT have a special meaning
                .replace("$", "\\$")
                .replace("(", "\\(")
                .replace(")", "\\)");

            // Convert cases like
            //     coverage*[.json, .xml, .info]
            // into
            //     coverage*(.json|.xml|.info)
            if (fileRegex.contains("[") && fileRegex.contains(",")) {
                boolean changed = false;
                while (true) {
                    String newRegex = fileRegex.replaceAll("\\[([^]]+) *, *([^]]+)]", "[$1|$2]");
                    if (newRegex.equals(fileRegex)) {
                        break;
                    }
                    fileRegex = newRegex;
                    changed = true;
                }
                if (changed) {
                    fileRegex = fileRegex.replaceAll("\\[([^]]+\\|[^]]+)]", "($1)");
                }
            }

            fileRegex = fileRegex
                // "/foo" --> End can be a filename (so we pin to the end) or a directory name (so we expect another / )
                .replaceAll("([^/*])$", "$1(/|\\$)")

                .replace(".", "\\.") // Avoid bad wildcards
                .replace("\\.*", "\\.[^/]*")//  matching  /.* onto /.foo/bar.xml
                .replace("?", "[^/]")   // Single character match

                // The Globstar "/**/bar" must also match "bar"
                .replaceAll("^\\*\\*/","(.*/)?")
                // The Globstar "foo/**/bar" must also match "foo/bar"
                .replace("/**","(/.*)?")

                // The wildcard "foo/*/bar" must match exactly 1 subdir "foo/something/bar"
                // and not "foo/bar", "foo//bar" or "foo/something/something/bar"
                .replace("/*/","/[^/]+/")
                .replace("/*/","/[^/]+/")

                .replace("**",".*") // Convert to the Regex wildcards

                .replaceAll("^\\*", ".*") // Match anything at the start

                .replaceAll("^/", "^/") // If starts with / then pin to the start.

                .replaceAll("/\\*([^/]*)$","/[^/]*$1\\$") // A trailing '/*something' means NO further subdirs should be matched

                .replace("/*","/[^/]*") // "/foo/*\.js"  --> "/foo/.*\.js"

                .replaceAll("([^.\\]])\\*", "$1[^/]*") // Match anything at the start

                .replace(LITERAL_STAR_MARKER, "\\Q*\\E") // Move the 'something special' to the literal *
                .replace(LITERAL_QUESTION_MARK, "\\Q?\\E") // Move the 'something special' to the literal ?

                .replaceAll("/+","/") // Remove duplication

                .replace("/\\E/", "/\\E") // Remove '/' duplication around a literal marker
                ;

            String finalRegex = baseDirRegex + fileRegex;
            try {
                filePattern = Pattern.compile(finalRegex);
                if (verbose) {
                    LOG.info("IgnoreRule for expression {}   -->   Regex {}", this.fileExpression, fileRegex);
                }
            } catch (PatternSyntaxException pse) {
                String errorMsg = "You either have an invalid gitignore rule (which you should fix) " +
                    "or you have found an edge case that should be fixed. " +
                    "In the latter case please file a bug report to https://github.com/nielsbasjes/codeowners/issues " +
                    "indicating that the expression >>>" + (negate?"!":"") + this.fileExpression + "<<< " +
                    "was converted to regex >>>" + finalRegex + "<<< " +
                    "which triggered the error: " + pse.getMessage();
                throw new PatternSyntaxException(errorMsg, finalRegex, pse.getIndex());
            }
        }

        /**
         * Checks if the file matches the stored expression.
         * @param filename The filename to be checked
         * @return NULL: not matched, True: must be ignored, False: it must be UNignored
         */
        public Boolean isIgnoredFile(String filename) {
            if (!filePattern.matcher(filename).find()) {
                if (verbose) {
                    LOG.info("NO MATCH     |{}| ~ |{}| --> |{}|", fileExpression, filePattern, filename);
                }
                return null;
            }
            if (negate) {
                if (verbose) {
                    LOG.info("MATCH NEGATE |{}| ~ |{}| --> |{}|", fileExpression, filePattern, filename);
                }
                return Boolean.FALSE;
            } else {
                if (verbose) {
                    LOG.info("MATCH IGNORE |{}| ~ |{}| --> |{}|", fileExpression, filePattern, filename);
                }
                return TRUE;
            }
        }

        /**
         * There is a strange edgecase in git where a rule that matches an entire directory
         * disables ignore rules in that same directory.
         * @return Is this rule matching an entire directory tree?
         */
        public boolean isDirectoryMatch() {
            return directoryMatch;
        }

        /**
         * @return The directory in which this gitIgnore was located
         */
        public String getIgnoreBasedir() {
            return projectRelativeBaseDir;
        }

        /**
         * @return The ignore expression as it was present in the .gitignore file
         */
        public String getIgnoreExpression() {
            return (negate ? "!" : "") + fileExpression;
        }

        /**
         * @return The basedir and ignore expression combined into a regular expression.
         */
        public Pattern getIgnorePattern() {
            return filePattern;
        }

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }

        @Override
        public String toString() {
            return String.format("%-20s     # Used Regex: %s", (negate?"!":"")+ fileExpression, filePattern);
        }
    }

}
