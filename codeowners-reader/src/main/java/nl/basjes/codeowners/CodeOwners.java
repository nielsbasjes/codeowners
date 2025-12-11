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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CodeOwners {

    private static final String CODEOWNERS_PATH_SEPARATOR = "/";

    static final Logger LOG = LoggerFactory.getLogger(CodeOwners.class);

    private boolean verbose = false;

    // Map name of Section to Sections
    private final Map<String, Section> sections;

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

    private boolean hasStructuralProblems = false;

    /**
     * Construct the CodeOwners with the provided rules string
     * @param codeownersContent The rules must be read. Will NPE if the content is null.
     */
    @SuppressWarnings("this-escape") // Because of generated code
    public CodeOwners(String codeownersContent) {
        CodeOwnersLoader codeOwnersLoader = new CodeOwnersLoader(codeownersContent);
        sections = codeOwnersLoader.getSections();
        hasStructuralProblems = codeOwnersLoader.hasStructuralProblems();
    }

    /**
     * @return true if any kind of (even minor) problem is found.
     */
    public boolean hasStructuralProblems() {
        return hasStructuralProblems;
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
     * @param filename The filename for which the mandatory approvers are requested. This filename MUST be relative to the project base directory.
     * @return The list of mandatory approver usernames for this filename in the order (as good as possible) as they appear in the code owner rules.
     */
    public List<String> getMandatoryApprovers(String filename) {
        return getAllApprovers(filename, true);
    }

    /**
     * Get all approvers for a specific filename.
     * @param filename The filename for which the approvers are requested. This filename MUST be relative to the project base directory.
     * @return The list of approver usernames for this filename in the order (as good as possible) as they appear in the code owner rules.
     */
    public List<String> getAllApprovers(String filename) {
        return getAllApprovers(filename, false);
    }

    private List<String> getAllApprovers(String filename, boolean onlyMandatory) {
        if (verbose) {
            LOG.info("# vvvvvvvvvvvvvvvvvvvvvvvvvvv");
            if (onlyMandatory) {
                LOG.info("Getting mandatory approvers: {}", filename);
            } else {
                LOG.info("Getting all approvers: {}", filename);
            }
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
                for (Rule rule : firstSection.getRules()) {
                    result.append(rule).append('\n');
                }
                return result.toString();
            }
        }
        for (Section section : sections.values()) {
            result.append(section).append('\n');
        }
        return result.toString();
    }

}
