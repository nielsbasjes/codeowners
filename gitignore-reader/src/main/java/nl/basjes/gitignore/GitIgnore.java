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

import nl.basjes.gitignore.parser.GitIgnoreBaseVisitor;
import nl.basjes.gitignore.parser.GitIgnoreLexer;
import nl.basjes.gitignore.parser.GitIgnoreParser;
import nl.basjes.gitignore.parser.GitIgnoreParser.GitignoreContext;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GitIgnore extends GitIgnoreBaseVisitor<Void> {

    // This is the separator dictated in the gitignore documentation
    private static final String GITIGNORE_PATH_SEPARATOR = "/";

    private static final Logger LOG = LoggerFactory.getLogger(GitIgnore.class);

    private final String baseDir;

    private final List<IgnoreRule> ignoreRules = new ArrayList<>();
    private boolean verbose = false;

    // Load the gitignore from a file
    public GitIgnore(File file) throws IOException {
        this("", file);
    }

    public GitIgnore(String baseDir, File file) throws IOException {
        this(baseDir, FileUtils.readFileToString(file, UTF_8));
    }

    public GitIgnore(String gitIgnoreContent) {
        this("", gitIgnoreContent);
    }

    public GitIgnore(String baseDir, String gitIgnoreContent) {
        String cleanBaseDir = baseDir.replace("\\", GITIGNORE_PATH_SEPARATOR);
        this.baseDir =
            (
                (cleanBaseDir.startsWith("/")?"":"/") +
                    cleanBaseDir +
                (cleanBaseDir.endsWith("/")?"":"/")
            ).replace("//", "/");
        CodePointCharStream input = CharStreams.fromString(gitIgnoreContent);
        GitIgnoreLexer lexer = new GitIgnoreLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        GitIgnoreParser parser = new GitIgnoreParser(tokens);
        GitignoreContext gitignore = parser.gitignore();
        visit(gitignore);
    }

    /**
     * Checks if the file matches the stored expressions.
     * @param filename The filename to be checked
     * @return NULL: not matched, True: must be ignored, False: it must be UNignored
     */
    public Boolean isIgnoredFile(String filename) {
        if (verbose) {
            LOG.info("# vvvvvvvvvvvvvvvvvvvvvvvvvvv");
            LOG.info("Checking: {}", filename);
        }
        String matchFileName = filename.replace("\\", GITIGNORE_PATH_SEPARATOR);
        if (!matchFileName.startsWith(GITIGNORE_PATH_SEPARATOR)) {
            matchFileName = GITIGNORE_PATH_SEPARATOR + matchFileName;
        }

        if (verbose) {
            LOG.info("Matching: {}", matchFileName);
        }

        // If a file is NOT matched at all then there is no verdict.
        Boolean mustBeIgnored = null;

        if (!matchFileName.startsWith(baseDir)) {
            if (verbose) {
                LOG.info("# Not in my baseDir: {}", baseDir);
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
                if (Boolean.TRUE.equals(mustBeIgnored)) {
                    LOG.info("Conclusion: Must be ignored");
                } else {
                    LOG.info("Conclusion: Must NOT be ignored");
                }
            }
        }
        return mustBeIgnored;
    }

    @Override
    public Void visitIgnoreRule(GitIgnoreParser.IgnoreRuleContext ctx) {
        String filePattern = ctx.fileExpression.getText();
        ignoreRules.add(new IgnoreRule(baseDir, ctx.not != null, filePattern));
        return null;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
        ignoreRules.forEach(rule->rule.setVerbose(verbose));
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("# GitIgnore file:\n");

        for (IgnoreRule ignoreRule : ignoreRules) {
            result.append(ignoreRule).append('\n');
        }
        return result.toString();
    }

    public String getBaseDir() {
        return baseDir;
    }

    public List<IgnoreRule> getIgnoreRules() {
        return new ArrayList<>(ignoreRules);
    }

    public static class IgnoreRule {
        private final String baseDir;
        private final boolean negate;
        private final String fileExpression;
        private final boolean directoryMatch;
        private final Pattern filePattern;
        private boolean verbose = false;

        public IgnoreRule(String baseDir, boolean negate, String fileExpression) {

            String baseDirRegex;
            if (baseDir == null || "/".equals(baseDir) || baseDir.trim().isEmpty()) {
                this.baseDir = "/";
                baseDirRegex = "^/?"; // The leading slash is optional
            } else {
                // Enforce the base dir starts and ends with a '/'
                this.baseDir = (baseDir.startsWith("/") ? "" : "/") +
                                baseDir.trim() +
                               (baseDir.endsWith("/")   ? "" : "/");

                baseDirRegex = "^/?\\Q" + this.baseDir.substring(1) + "\\E";
            }

            this.negate = negate;
            this.fileExpression = fileExpression;
            this.directoryMatch = !negate && fileExpression.endsWith("/");

            String fileRegex = fileExpression
                .trim() // Clear leading and trailing spaces

                .replaceAll("^!", "") // Strip the negation at the start of the line (if any)

                .replace("[!", "[^") // Fix the 'not' range or 'not' set
                ;

            if (fileExpression.contains("/") && !fileExpression.endsWith("/")) {
                // Patterns specifying a file in a particular directory are relative to the repository root.
                if (fileRegex.startsWith("/")) {
                    fileRegex = fileRegex.substring(1);
                }
            } else {
                // If a path does not start with a /, the path is treated as if it starts with a globstar. README.md is treated the same way as /**/README.md
                fileRegex = fileRegex.replaceAll("^([^/*.])", "**/$1");
            }

            fileRegex = fileRegex
                .replace("\\ ", " ") // The escaped spaces must become spaces again.

                // "/foo" --> End can be a filename (so we pin to the end) or a directory name (so we expect another / )
                .replaceAll("([^/*])$", "$1(/|\\$)")

                .replace(".", "\\.") // Avoid bad wildcards
                .replace("\\.*", "\\..*")//  matching  /.* onto /.foo/bar.xml
                .replace("?", ".")   // Single character match

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

                .replace("/*","/.*") // "/foo/*\.js"  --> "/foo/.*\.js"

                .replaceAll("([^.\\]])\\*", "$1.*") // Match anything at the start

                .replaceAll("/+","/") // Remove duplication

                .replace("/\\E/", "/\\E")
                ;

//            LOG.info("{}     -->     {}", fileExpression, fileRegex);
            filePattern = Pattern.compile(baseDirRegex + fileRegex);
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
                return Boolean.TRUE;
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
            return baseDir;
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
            return (negate?"!":"")+ fileExpression + "          # Used Regex: " + filePattern;
        }
    }

}
