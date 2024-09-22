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

package nl.basjes.codeowners;

import nl.basjes.codeowners.parser.CodeOwnersLexer;
import nl.basjes.codeowners.parser.CodeOwnersParser;
import nl.basjes.codeowners.parser.CodeOwnersParser.ApprovalRuleContext;
import nl.basjes.codeowners.parser.CodeOwnersParser.CodeownersContext;
import nl.basjes.codeowners.parser.CodeOwnersParserBaseVisitor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CodeOwners extends CodeOwnersParserBaseVisitor<Void> {

    private static final String CODEOWNERS_PATH_SEPARATOR = "/";

    private static final Logger LOG = LoggerFactory.getLogger(CodeOwners.class);

    private boolean verbose = false;

    // Map name of Section to Sections
    private final Map<String, Section> sections = new LinkedHashMap<>();

    // Load the code owners from a file

    /**
     * Construct the CodeOwners from a file
     * @param file The file from which the rules must be read. Will NPE if file is null.
     * @throws IOException In case of problems.
     */
    public CodeOwners(File file) throws IOException {
        this(readFileToString(file));
    }

    private static String readFileToString(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), UTF_8);
    }

    private static final String IMPLICIT_SECTION_NAME = "Implicit Default Section";

    /**
     * Construct the CodeOwners with the provided rules string
     * @param codeownersContent The rules must be read. Will NPE if the content is null.
     */
    @SuppressWarnings("this-escape") // Because of generated code
    public CodeOwners(String codeownersContent) {
        currentSection = new Section(IMPLICIT_SECTION_NAME);

        CodePointCharStream input = CharStreams.fromString(codeownersContent);
        CodeOwnersLexer lexer = new CodeOwnersLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CodeOwnersParser parser = new CodeOwnersParser(tokens);
        CodeownersContext codeowners = parser.codeowners();
        visit(codeowners);

        // Make sure we retain the last section also
        storeCurrentSection();
        checkForAnyStructuralProblems();
    }

    private void storeCurrentSection() {
        // Only if the previous Section had ANY rules do we keep it.
        if (!currentSection.approvalRules.isEmpty()) {
            List<String> existingSectionsWithSameName = sections.values().stream().map(Section::getName).filter(name -> name.equalsIgnoreCase(currentSection.name)).collect(Collectors.toList());
            if (existingSectionsWithSameName.isEmpty()) {
                sections.put(currentSection.name, currentSection);
            } else {
                Section existingSection = sections.get(existingSectionsWithSameName.get(0));
                currentSection.getDefaultApprovers().forEach(existingSection::addDefaultApprover);
                currentSection.getApprovalRules().forEach(existingSection::addApprovalRule);
                if (currentSection.isOptional() != existingSection.isOptional()) {
                    // You cannot MIX these two, it is bad.
                    LOG.error("Merging two sections with a different Optional flag is BAD. Section [{}] has optional={} and Section [{}] has optional={}.",
                        existingSection.getName(), existingSection.isOptional(), currentSection.getName(), currentSection.isOptional());
                    hasStructuralProblems = true;
                }
            }
        }
    }

    /**
     * @return true if any kind of (even minor) problem is found.
     */
    public boolean hasStructuralProblems() {
        return hasStructuralProblems;
    }

    private boolean hasStructuralProblems = false;
    /**
     * Check if any problems are present in the config
     */
    public void checkForAnyStructuralProblems() {
        for (Section section : sections.values()) {
            // An optional section where you expect a MinimalNumberOfApprovers is a problem
            int minimalNumberOfApprovers = section.getMinimalNumberOfApprovers();
            if (section.isOptional() && minimalNumberOfApprovers != 0) {
                LOG.warn("CODEOWNERS Section \"{}\" is Optional so the specified MinimalNumberOfApprovers {} is IGNORED!",
                        section.getName(), minimalNumberOfApprovers);
                hasStructuralProblems = true;
            }

            // Having in the same section the same file pattern multiple times is bad.
            List<String> duplicates = section
                .getApprovalRules().stream()
                .collect(Collectors.groupingBy(ApprovalRule::getFileExpression, Collectors.counting()))
                .entrySet().stream()
                .filter(m -> m.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
            if (!duplicates.isEmpty()) {
                LOG.warn("In section [{}] these file patterns occur multiple times: {}", section.getName(), duplicates);
                hasStructuralProblems = true;
            }
        }
    }

    /**
     * @param verbose True enables logging, False disables logging
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
        sections.values().forEach(section->section.setVerbose(verbose));
    }

    /**
     * If the application needs to inspect the defined rules then this is the
     * way to retrieve all defined sections AFTER they were cleaned and merged !
     * @return The set of all sections in an undefined order !
     */
    public Set<Section> getAllDefinedSections() {
        return new HashSet<>(sections.values());
    }

    /**
     * Get all mandatory approvers for a specific filename.
     * @param filename The filename for which the mandatory approvers are requested.
     * @return The list of mandatory approver usernames for this filename in the order (as good as possible) as they appear in the code owner rules.
     */
    public List<String> getMandatoryApprovers(String filename) {
        return getAllApprovers(filename, true);
    }

    /**
     * Get all approvers for a specific filename.
     * @param filename The filename for which the approvers are requested.
     * @return The list of approver usernames for this filename in the order (as good as possible) as they appear in the code owner rules.
     */
    public List<String> getAllApprovers(String filename) {
        return getAllApprovers(filename, false);
    }

    private List<String> getAllApprovers(String filename, boolean onlyMandatory) {
        if (verbose) {
            LOG.info("# vvvvvvvvvvvvvvvvvvvvvvvvvvv");
            LOG.info("Checking: {}", filename);
        }

        String matchFileName = filename.replace("\\", CODEOWNERS_PATH_SEPARATOR);
        if (!matchFileName.startsWith(CODEOWNERS_PATH_SEPARATOR)) {
            matchFileName = CODEOWNERS_PATH_SEPARATOR + matchFileName;
        }

        if (verbose) {
            LOG.info("Matching: {}", matchFileName);
        }

        List<String> approvers = new ArrayList<>();
        for (Section section: sections.values()) {
            if (!onlyMandatory || !section.isOptional()) {
                approvers.addAll(section.getApprovers(matchFileName));
            }
        }
        List<String> endResultApprovers = approvers.stream().distinct().collect(Collectors.toList());
        if (verbose) {
            LOG.info("# ---------------------------");
            if (onlyMandatory) {
                LOG.info("# Mandatory Approvers (all sections combined): {}", endResultApprovers);
            } else {
                LOG.info("# All Approvers (all sections combined): {}", endResultApprovers);
            }
            LOG.info("# ^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            LOG.info("");
        }
        return endResultApprovers;
    }

    private Section currentSection;

    /**
     * Internal parser method, do not use
     * @param ctx the parse tree
     * @return Nothing
     */
    @Override
    public Void visitSection(CodeOwnersParser.SectionContext ctx) {
        Section section = new Section(ctx.section.getText().trim());
        section.optional = ctx.OPTIONAL() != null;
        if (ctx.approvers != null) {
            section.setMinimalNumberOfApprovers(Integer.parseInt(ctx.approvers.getText().trim()));
        }
        for (TerminalNode user : ctx.USERID()) {
            section.addDefaultApprover(user.getText());
        }

        // Only if the previous Section had ANY rules do we keep it.
        storeCurrentSection();
        currentSection = section;
        return null;
    }

    /**
     * Internal parser method, do not use
     * @param ctx the parse tree
     * @return Nothing
     */
    @Override
    public Void visitApprovalRule(ApprovalRuleContext ctx) {
        String filePattern = ctx.fileExpression.getText();
        List<String> approvers = ctx.USERID().stream()
                .map(ParseTree::getText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        currentSection.addApprovalRule(new ApprovalRule(filePattern, approvers));
        return null;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("# CODEOWNERS file:\n");
        if (sections.isEmpty()) {
            return result.append("# No CODEOWNER rules were defined.\n").toString();
        }

        if (sections.size() == 1) {
            Section firstSection = sections.values().iterator().next();
            if (firstSection.isDefaultSection()) {
                // If ONLY the default section then no section header
                for (ApprovalRule approvalRule : firstSection.getApprovalRules()) {
                    result.append(approvalRule).append('\n');
                }
                return result.toString();
            }
        }
        for (Section section : sections.values()) {
            result.append(section).append('\n');
        }
        return result.toString();
    }

    public static class Section {
        private boolean verbose = false;
        private boolean optional = false;
        private final String name;
        private int minimalNumberOfApprovers = 0;
        private final List<String> defaultApprovers = new ArrayList<>();
        private final List<ApprovalRule> approvalRules = new ArrayList<>();

        public Section(String name) {
            this.name = name;
        }

        void addDefaultApprover(String name) {
            String cleanedName = name.trim();
            if (!defaultApprovers.contains(cleanedName)) {
                defaultApprovers.add(cleanedName);
            }
        }

        void addApprovalRule(ApprovalRule rule) {
            approvalRules.add(rule);
        }

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
            approvalRules.forEach(rule->rule.setVerbose(verbose));
        }

        public String getName() {
            return name;
        }

        public List<String> getDefaultApprovers() {
            return defaultApprovers;
        }

        public List<ApprovalRule> getApprovalRules() {
            return approvalRules;
        }

        public boolean isOptional() {
            return optional;
        }

        public boolean isDefaultSection() { return IMPLICIT_SECTION_NAME.equals(name); }

        void setMinimalNumberOfApprovers(int minimalNumberOfApprovers) {
            this.minimalNumberOfApprovers = minimalNumberOfApprovers;
        }

        public int getMinimalNumberOfApprovers() {
            return minimalNumberOfApprovers;
        }

        /**
         * @param filename The filename for which the approvers are requested.
         * @return The list of approver usernames for this filename in the order (as good as possible) as they appear in the code owner rules in this section.
         */
        public List<String> getApprovers(String filename) {
            if (verbose) {
                LOG.info("# ---------------------------");
                LOG.info("# Section [{}]", getName());
            }
            List<String> approvers = new ArrayList<>();
            for (ApprovalRule approvalRule : approvalRules) {
                List<String> ruleApprovers = approvalRule.getApprovers(filename);
                if (ruleApprovers != null) {
                    // GitHub: Order is important; the last matching pattern takes the most precedence.
                    // Gitlab: When a file or directory matches multiple entries in the CODEOWNERS file, the users from last pattern matching the file or directory are used.
                    approvers.clear();
                    if (ruleApprovers.isEmpty()) {
                        if (verbose) {
                            LOG.info("-- MATCH WITHOUT APPROVERS --> Using Default approvers {}", defaultApprovers);
                        }
                        approvers.addAll(defaultApprovers);
                    } else {
                        approvers.addAll(ruleApprovers);
                    }
                }
            }
            if (verbose) {
                LOG.info("# Section [{}] approvers: {}", getName(), approvers);
                LOG.info("# ---------------------------");
            }
            return approvers;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            if (optional) {
                result.append('^');
            }
            result.append('[').append(name).append(']');
            if (minimalNumberOfApprovers > 0) {
                result.append('[').append(minimalNumberOfApprovers).append(']');
            }
            if (!defaultApprovers.isEmpty()) {
                result.append(' ').append(String.join(" ", defaultApprovers));
            }
            result.append('\n');
            for (ApprovalRule approvalRule : approvalRules) {
                result.append(approvalRule).append('\n');
            }
            return result.toString();
        }
    }

    public static class ApprovalRule {
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
                    LOG.info("NO MATCH  |{}| ~ |{}| --> {}", fileExpression, filePattern, filename);
                }
                return null;
            }
            if (verbose) {
                LOG.info("MATCH     |{}| ~ |{}| --> {}    approvers:{}", fileExpression, filePattern, filename, approvers);
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
}
