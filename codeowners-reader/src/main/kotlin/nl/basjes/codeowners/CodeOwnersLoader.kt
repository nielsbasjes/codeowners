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

import nl.basjes.codeowners.parser.CodeOwnersLexer
import nl.basjes.codeowners.parser.CodeOwnersParser
import nl.basjes.codeowners.parser.CodeOwnersParserBaseVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.TerminalNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer
import java.util.stream.Collectors

internal class CodeOwnersLoader(codeownersContent: String) : CodeOwnersParserBaseVisitor<Void?>() {
    // Map name of Section to Sections
    val sections: MutableMap<String, Section> = LinkedHashMap()

    private fun storeCurrentSection() {
        // Only if the previous Section had ANY rules do we keep it.
        if (!currentSection.rules.isEmpty()) {
            val existingSectionsWithSameName =
                sections.values.stream().map { it.name }
                    .filter { name: String? -> name.equals(currentSection.name, ignoreCase = true) }
                    .collect(Collectors.toList())

            if (existingSectionsWithSameName.isEmpty()) {
                sections[currentSection.name] = currentSection
            } else {
                val existingSection: Section? = sections[existingSectionsWithSameName[0]]
                requireNotNull(existingSection) { "This should never happen, we just checked that it must exist"}
                currentSection
                    .defaultApprovers
                    .forEach(Consumer { name -> existingSection.addDefaultApprover(name) })

                currentSection
                    .rules
                    .forEach{ existingSection.addRule(it) }

                if (currentSection.isOptional != existingSection.isOptional) {
                    // You cannot MIX these two, it is bad.
                    LOG.error(
                        "Merging two sections with a different Optional flag is BAD. Section [{}] has optional={} and Section [{}] has optional={}.",
                        existingSection.name,
                        existingSection.isOptional,
                        currentSection.name,
                        currentSection.isOptional
                    )
                    hasStructuralProblems = true
                }
            }
        }
    }

    /**
     * @return true if any kind of (even minor) problem is found.
     */
    fun hasStructuralProblems(): Boolean {
        return hasStructuralProblems
    }

    private var hasStructuralProblems = false

    /**
     * Check if any problems are present in the config
     */
    fun checkForAnyStructuralProblems() {
        for (section in sections.values) {
            // An optional section where you expect a MinimalNumberOfApprovers is a problem
            val minimalNumberOfApprovers = section.minimalNumberOfApprovers
            if (section.isOptional && minimalNumberOfApprovers != 0) {
                LOG.warn(
                    "CODEOWNERS Section \"{}\" is Optional so the specified MinimalNumberOfApprovers {} is IGNORED!",
                    section.name, minimalNumberOfApprovers
                )
                hasStructuralProblems = true
            }

            // Having in the same section the same file pattern multiple times is bad.
            val duplicates: MutableList<String> = section
                .rules.stream()
                .collect(Collectors.groupingBy(Rule::fileExpression, Collectors.counting()))
                .entries.stream()
                .filter { it.value > 1 }
                .map { it.key }
                .sorted()
                .collect(Collectors.toList())
            if (!duplicates.isEmpty()) {
                LOG.warn("In section [{}] these file patterns occur multiple times: {}", section.name, duplicates)
                hasStructuralProblems = true
            }
        }
    }

    private var currentSection: Section

    /**
     * Construct the CodeOwners with the provided rules string
     * The rules are read from the provided codeownersContent.
     */
    init {
        currentSection = Section(IMPLICIT_SECTION_NAME)

        val input = CharStreams.fromString(codeownersContent)
        val lexer = CodeOwnersLexer(input)
        val tokens = CommonTokenStream(lexer)
        val parser = CodeOwnersParser(tokens)
        val codeowners = parser.codeowners()
        visit(codeowners)

        // Make sure we retain the last section also
        storeCurrentSection()
        checkForAnyStructuralProblems()
    }

    /**
     * Internal parser method, do not use
     * @param ctx the parse tree
     * @return Nothing
     */
    override fun visitSection(ctx: CodeOwnersParser.SectionContext): Void? {
        val section = Section(ctx.section.getText().trim { it <= ' ' })
        section.isOptional = ctx.OPTIONAL() != null
        if (ctx.approvers != null) {
            section.minimalNumberOfApprovers = ctx.approvers.getText().trim { it <= ' ' }.toInt()
        }
        for (user in ctx.USERID()) {
            section.addDefaultApprover(user.getText())
        }

        // Only if the previous Section had ANY rules do we keep it.
        storeCurrentSection()
        currentSection = section
        return null
    }

    /**
     * Internal parser method, do not use
     * @param ctx the parse tree
     * @return Nothing
     */
    override fun visitApprovalRule(ctx: CodeOwnersParser.ApprovalRuleContext): Void? {
        val filePattern = ctx.fileExpression.getText()
        val approvers = ctx.USERID().stream()
            .map<String?> { obj: TerminalNode? -> obj!!.getText() }
            .map<String?> { obj: String? -> obj!!.trim { it <= ' ' } }
            .map<String?> { approver: String? ->
                approver!!.replace(
                    "^\\([a-zA-Z0-9!#$%&'*+/=?^_`{|}~.-]+\\)".toRegex(),
                    ""
                )
            }
            .map<String?> { approver: String? ->
                approver!!.replace(
                    "\\([a-zA-Z0-9!#$%&'*+/=?^_`{|}~.-]+\\)@".toRegex(),
                    "@"
                )
            }
            .distinct()
            .collect(Collectors.toList())
        currentSection.addRule(ApprovalRule(filePattern, approvers))
        return null
    }


    /**
     * Internal parser method, do not use
     * @param ctx the parse tree
     * @return Nothing
     */
    override fun visitExcludeRule(ctx: CodeOwnersParser.ExcludeRuleContext): Void? {
        val filePattern = ctx.fileExpression.getText()
        currentSection.addRule(ExcludeRule(filePattern))
        return null
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(CodeOwnersLoader::class.java)

        const val IMPLICIT_SECTION_NAME: String = "Implicit Default Section"
    }
}
