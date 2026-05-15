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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.stream.Collectors

internal val LOG: Logger = LoggerFactory.getLogger(CodeOwners::class.java)

class CodeOwners(codeownersContent: String) {

    private var verbose = false

    // Map name of Section to Sections
    private val sections: MutableMap<String, Section>

    /**
     * @return true if any kind of (even minor) problem is found.
     */
    var hasStructuralProblems = false
        private set

    /**
     * Construct the CodeOwners with the provided rules string
     * The rules must be read.
     */
    init {
        val codeOwnersLoader = CodeOwnersLoader(codeownersContent)
        sections = codeOwnersLoader.sections
        hasStructuralProblems = codeOwnersLoader.hasStructuralProblems()
    }

    /**
     * Construct the CodeOwners from a file
     * @param file The file from which the rules must be read. Will NPE if file is null.
     * @throws IOException In case of problems.
     */
    constructor(file: File) : this(readFileToString(file))


    /**
     * @param verbose True enables logging, False disables logging
     */
    fun setVerbose(verbose: Boolean) {
        this.verbose = verbose
        sections.values.forEach { it.setVerbose(verbose) }
    }

    val allDefinedSections: MutableSet<Section>
        /**
         * If the application needs to inspect the defined rules then this is the
         * way to retrieve all defined sections AFTER they were cleaned and merged !
         * @return The set of all sections in an undefined order !
         */
        get() = sections.values.toMutableSet()

    /**
     * Get all mandatory approvers for a specific filename.
     * @param filename The filename for which the mandatory approvers are requested. This filename MUST be relative to the project base directory.
     * @return The list of mandatory approver usernames for this filename in the order (as good as possible) as they appear in the code owner rules.
     */
    fun getMandatoryApprovers(filename: String) = getAllApprovers(filename, true)

    /**
     * Get all approvers for a specific filename.
     * @param filename The filename for which the approvers are requested. This filename MUST be relative to the project base directory.
     * @return The list of approver usernames for this filename in the order (as good as possible) as they appear in the code owner rules.
     */
    fun getAllApprovers(filename: String) = getAllApprovers(filename, false)

    private fun getAllApprovers(filename: String, onlyMandatory: Boolean): List<String> {
        if (verbose) {
            LOG.info("# vvvvvvvvvvvvvvvvvvvvvvvvvvv")
            if (onlyMandatory) {
                LOG.info("Getting mandatory approvers: {}", filename)
            } else {
                LOG.info("Getting all approvers: {}", filename)
            }
        }

        var matchFileName = filename.replace("\\", CODEOWNERS_PATH_SEPARATOR)
        if (!matchFileName.startsWith(CODEOWNERS_PATH_SEPARATOR)) {
            matchFileName = CODEOWNERS_PATH_SEPARATOR + matchFileName
        }

        if (verbose) {
            LOG.info("Matching: {}", matchFileName)
        }

        val approvers: MutableList<String> = mutableListOf()
        for (section in sections.values) {

            if (!onlyMandatory || !section.isOptional) {
                approvers.addAll(section.getApprovers(matchFileName))
            }
        }
        val endResultApprovers = approvers.stream().distinct().collect(Collectors.toList())
        if (verbose) {
            LOG.info("# ---------------------------")
            if (onlyMandatory) {
                LOG.info("# Mandatory Approvers (all sections combined): {}", endResultApprovers)
            } else {
                LOG.info("# All Approvers (all sections combined): {}", endResultApprovers)
            }
            LOG.info("# ^^^^^^^^^^^^^^^^^^^^^^^^^^^")
            LOG.info("")
        }
        return endResultApprovers
    }

    override fun toString(): String {
        val result = StringBuilder()
        result.append("# CODEOWNERS file:\n")
        if (sections.isEmpty()) {
            return result.append("# No CODEOWNER rules were defined.\n").toString()
        }

        if (sections.size == 1) {
            val firstSection = sections.values.iterator().next()
            if (firstSection.isDefaultSection) {
                // If ONLY the default section then no section header
                for (rule in firstSection.rules) {
                    result.append(rule).append('\n')
                }
                return result.toString()
            }
        }
        for (section in sections.values) {
            result.append(section).append('\n')
        }
        return result.toString()
    }
}

private const val CODEOWNERS_PATH_SEPARATOR = "/"

@Throws(IOException::class)
private fun readFileToString(file: File): String {
    return String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
}
