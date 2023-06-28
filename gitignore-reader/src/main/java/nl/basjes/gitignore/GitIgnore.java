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
        this.baseDir =
            (
                (baseDir.startsWith("/")?"":"/") +
                baseDir +
                (baseDir.endsWith("/")?"":"/")
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
        String matchFileName = filename;
        if (!filename.startsWith("/")) {
            matchFileName = "/" + filename;
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
        ignoreRules.add(new IgnoreRule(ctx.not != null, filePattern));
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

    public static class IgnoreRule {
        private final boolean negate;
        private final String fileExpression;
        private final Pattern filePattern;
        private boolean verbose = false;

        public IgnoreRule(boolean negate, String fileExpression) {
            this.negate = negate;
            this.fileExpression = fileExpression;

            String fileRegex = fileExpression
                .trim() // Clear leading and trailing spaces

                .replaceAll("^!", "") // Strip the negation at the start of the line (if any)

                .replace("[!", "[^") // Fix the 'not' range or 'not' set

                .replace("\\ ", " ") // The escaped spaces must become spaces again.
                ;

            if (fileExpression.contains("/") && !fileExpression.endsWith("/")) {
                // Patterns specifying a file in a particular directory are relative to the repository root.
                fileRegex = "/" + fileRegex;
            } else {
                // If a path does not start with a /, the path is treated as if it starts with a globstar. README.md is treated the same way as /**/README.md
                fileRegex = fileRegex.replaceAll("^([^/*.])", "/**/$1");
            }

            fileRegex = fileRegex
                // "/foo" --> End can be a filename (so we pin to the end) or a directory name (so we expect another / )
                .replaceAll("([^/*])$", "$1(/|\\$)")

                .replace(".", "\\.") // Avoid bad wildcards
                .replace("?", ".")   // Single character match

                // The Globstar "foo/**/bar" must also match "foo/bar"
                .replace("/**","(/.*)?")

                // The wildcard "foo/*day/bar" must match "foo/tuesday/bar"
                .replaceAll("/\\*([^*])","/.*$1")

                .replace("**",".*") // Convert to the Regex wildcards

                .replaceAll("/\\*$","/[^/]+\\$") // A trailing '/*' means NO further subdirs should be matched
                .replaceAll("^\\*", ".*") // Match anything at the start

                .replace("/*","/.*") // "/foo/*\.js"  --> "/foo/.*\.js"

                .replaceAll("^/", "^/") // If starts with / then pin to the start.
                .replaceAll("/+","/") // Remove duplication
                ;

//            LOG.info("{}     -->     {}", fileExpression, fileRegex);
            filePattern = Pattern.compile(fileRegex);
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

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }

        @Override
        public String toString() {
            return (negate?"!":"")+ fileExpression + "          # Used Regex: " + filePattern;
        }
    }

}
