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

import nl.basjes.codeowners.parser.CodeOwnersLexer;
import nl.basjes.codeowners.parser.CodeOwnersParser;
import nl.basjes.codeowners.parser.CodeOwnersParser.ApprovalRuleContext;
import nl.basjes.codeowners.parser.CodeOwnersParser.CodeownersContext;
import nl.basjes.codeowners.parser.CodeOwnersParser.SectionContext;
import nl.basjes.codeowners.parser.CodeOwnersParserBaseVisitor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class CodeOwnersLoader extends CodeOwnersParserBaseVisitor<Void> {

    private static final Logger LOG = LoggerFactory.getLogger(CodeOwnersLoader.class);

    // Map name of Section to Sections
    private final Map<String, Section> sections = new LinkedHashMap<>();

    public Map<String, Section> getSections() {
        return sections;
    }

    static final String IMPLICIT_SECTION_NAME = "Implicit Default Section";

    /**
     * Construct the CodeOwners with the provided rules string
     * @param codeownersContent The rules must be read. Will NPE if the content is null.
     */
    @SuppressWarnings("this-escape") // Because of generated code
    public CodeOwnersLoader(String codeownersContent) {
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
        if (!currentSection.getApprovalRules().isEmpty()) {
            List<String> existingSectionsWithSameName = sections.values().stream().map(Section::getName).filter(name -> name.equalsIgnoreCase(currentSection.getName())).collect(Collectors.toList());
            if (existingSectionsWithSameName.isEmpty()) {
                sections.put(currentSection.getName(), currentSection);
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

    private Section currentSection;

    /**
     * Internal parser method, do not use
     * @param ctx the parse tree
     * @return Nothing
     */
    @Override
    public Void visitSection(SectionContext ctx) {
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
                .map(approver  -> approver.replaceAll("^\\([a-zA-Z0-9!#$%&'*+/=?^_`{|}~.-]+\\)", ""))
                .map(approver  -> approver.replaceAll("\\([a-zA-Z0-9!#$%&'*+/=?^_`{|}~.-]+\\)@", "@"))
                .distinct()
                .collect(Collectors.toList());
        currentSection.addApprovalRule(new ApprovalRule(filePattern, approvers));
        return null;
    }

}
