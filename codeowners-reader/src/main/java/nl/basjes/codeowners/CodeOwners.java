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

package nl.basjes.codeowners;

import nl.basjes.codeowners.parser.CodeOwnersBaseVisitor;
import nl.basjes.codeowners.parser.CodeOwnersLexer;
import nl.basjes.codeowners.parser.CodeOwnersParser;
import nl.basjes.codeowners.parser.CodeOwnersParser.ApprovalRuleContext;
import nl.basjes.codeowners.parser.CodeOwnersParser.CodeownersContext;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.STGroupString;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CodeOwners extends CodeOwnersBaseVisitor<Void> {

    private static final Logger LOG = LoggerFactory.getLogger(CodeOwners.class);

    @SuppressWarnings("unused") // Used in StringTemplate
    public Map<String, Section> getSections() {
        return sections;
    }

    @SuppressWarnings("unused") // Used in StringTemplate
    public boolean getHasMultipleSections() {
        return sections.size() > 1;
    }

    // Map name of Section to Sections
    private final Map<String, Section> sections = new TreeMap<>();

    // Load the code owners from a file
    public CodeOwners(File file) throws IOException {
        this(FileUtils.readFileToString(file, UTF_8));
    }

    private static final String IMPLICIT_SECTION_NAME = "Implicit Default Section";

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
            Integer minimalNumberOfApprovers = section.getMinimalNumberOfApprovers();
            if (minimalNumberOfApprovers != null) {
                if (section.isOptional() && minimalNumberOfApprovers != 0) {
                    LOG.warn("CODEOWNERS Section \"{}\" is Optional so the specified MinimalNumberOfApprovers {} is IGNORED!",
                            section.getName(), minimalNumberOfApprovers);
                    hasStructuralProblems = true;
                }
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

    public List<String> getMandatoryApprovers(String filename) {
        String matchFileName = filename;
        if (!filename.startsWith("/")) {
            matchFileName = "/" + filename;
        }

        List<String> approvers = new ArrayList<>();
        for (Section section: sections.values()) {
            if (!section.isOptional()) {
                approvers.addAll(section.getApprovers(matchFileName));
            }
        }
        return approvers.stream().sorted().distinct().collect(Collectors.toList());
    }

    public List<String> getAllApprovers(String filename) {
        String matchFileName = filename;
        if (!filename.startsWith("/")) {
            matchFileName = "/" + filename;
        }

        List<String> approvers = new ArrayList<>();
        for (Section section: sections.values()) {
            approvers.addAll(section.getApprovers(matchFileName));
        }
        return approvers.stream().sorted().distinct().collect(Collectors.toList());
    }

    private Section currentSection;

    @Override
    public Void visitSection(CodeOwnersParser.SectionContext ctx) {
        String sectionName = ctx.section.getText();
        Section section = sections.get(sectionName);
        if (section == null) {
            // New section.
            section = new Section(sectionName);
            section.optional = ctx.OPTIONAL() != null;
            if (ctx.approvers != null) {
                section.setMinimalNumberOfApprovers(Integer.valueOf(ctx.approvers.getText().trim()));
            }
            for (TerminalNode user : ctx.USERID()) {
                section.addDefaultApprover(user.getText());
            }
        }

        // Only if the previous Section had ANY rules do we keep it.
        storeCurrentSection();
        currentSection = section;
        return null;
    }

    @Override
    public Void visitApprovalRule(ApprovalRuleContext ctx) {
        String filePattern = ctx.fileExpression.getText();
        List<String> approvers = ctx.USERID().stream()
                .map(ParseTree::getText)
                .map(String::trim)
                .sorted()
                .distinct()
                .collect(Collectors.toList());
        currentSection.addApprovalRule(new ApprovalRule(filePattern, approvers));
        return null;
    }

    @Override
    public String toString() {
        return ST_GROUP_STRING.getInstanceOf("CodeOwners").add("codeowners", this).render();
    }

    public static class Section {
        private boolean optional = false;
        private final String name;
        private Integer minimalNumberOfApprovers = null;
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

        void setOptional(boolean optional) {
            this.optional = optional;
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

        void setMinimalNumberOfApprovers(Integer minimalNumberOfApprovers) {
            this.minimalNumberOfApprovers = minimalNumberOfApprovers;
        }

        public Integer getMinimalNumberOfApprovers() {
            return minimalNumberOfApprovers;
        }

        public List<String> getApprovers(String filename) {
            List<String> approvers = new ArrayList<>();
            for (ApprovalRule approvalRule : approvalRules) {
                List<String> ruleApprovers = approvalRule.getApprovers(filename);
                if (ruleApprovers != null) {
                    // GitHub: Order is important; the last matching pattern takes the most precedence.
                    // Gitlab: When a file or directory matches multiple entries in the CODEOWNERS file, the users from last pattern matching the file or directory are used.
                    approvers.clear();
                    if (ruleApprovers.isEmpty()) {
                        approvers.addAll(defaultApprovers);
                    } else {
                        approvers.addAll(ruleApprovers);
                    }
                }
            }
            return approvers;
        }

        @Override
        public String toString() {
            return ST_GROUP_STRING.getInstanceOf("Section").add("section", this).render();
        }
    }

    public static class ApprovalRule {
        private final String fileExpression;
        private final List<String> approvers;
        private final Pattern filePattern;

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

                // The Globstar "foo/**/bar" must also match "foo/bar"
                .replace("/**","(/.*)?")

                .replace("**",".*") // Convert to the Regex wildcards

                .replaceAll("/\\*$","/[^/]+\\$") // A trailing '/*' means NO further subdirs should be matched
                .replaceAll("^\\*", ".*") // Match anything at the start

                .replace("/*","/.*") // "/foo/*\.js"  --> "/foo/.*\.js"

                .replaceAll("^/", "^/") // If starts with / then pin to the start.
                ;

//            LOG.info("{}     -->     {}", fileExpression, fileRegex);
            filePattern = Pattern.compile(fileRegex);
        }

        public String getFileExpression() {
            return fileExpression;
        }

        public List<String> getApprovers() {
            return new ArrayList<>(approvers);
        }

        public List<String> getApprovers(String filename) {
            if (!filePattern.matcher(filename).find()) {
//                LOG.warn("FAIL  {} --> {}", fileExpression, filename);
                return null;
            }
//            LOG.warn("MATCH {} --> {}", fileExpression, filename);
            return new ArrayList<>(approvers);
        }

        @Override
        public String toString() {
            return ST_GROUP_STRING.getInstanceOf("ApprovalRule").add("approvalRule", this).render();
        }
    }

    private static final STGroupString ST_GROUP_STRING = new STGroupString(
        "CodeOwners(codeowners) ::= <<\n" +
        "<if(codeowners.hasMultipleSections)>" +
        "<codeowners.sections.values:Section() ;separator=\"\n\n\">\n" +
        "<else>\n" +
        "<codeowners.sections.values:DefaultSection() ;separator=\"\n\n\">\n" +
        "<endif>\n" +
        ">>\n" +

        "Section(section) ::= <<\n" +
        "<if(section.optional)>^<endif>" +
            "[<section.name>]" +
            "<if(section.minimalNumberOfApprovers)>[<section.minimalNumberOfApprovers>]<endif>" +
            "<if(section.defaultApprovers)> <section.defaultApprovers;separator=\" \"><endif>\n" +
        "<section.approvalRules:{ rule | <ApprovalRule(rule)>};separator=\"\n\">\n" +
        ">>\n" +

        "DefaultSection(section) ::= <<\n" +
        "<if(section.defaultSection)>\n" +
        "<section.approvalRules:{ rule | <ApprovalRule(rule)>};separator=\"\n\">\n" +
        "<else>\n" +
        "<Section(section)>\n" +
        "<endif>\n" +
        ">>\n" +

        "ApprovalRule(approvalRule) ::= <<\n" +
        "<approvalRule.fileExpression><if(approvalRule.approvers)> <approvalRule.approvers; separator=\" \"><endif>\n" +
        ">>\n"
    );
}
