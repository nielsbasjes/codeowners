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

package nl.basjes.maven.enforcer.codeowners;

import nl.basjes.codeowners.validator.CodeOwnersValidationException;
import nl.basjes.codeowners.validator.CodeOwnersValidator;
import nl.basjes.codeowners.validator.CodeOwnersValidator.DirectoryOwners;
import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration;
import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;

@SuppressWarnings("unused") // Used by the enforcer-plugin that finds it via the @Named annotation
@Named("codeOwners") // rule name - must start from lowercase character
public class CodeOwnersEnforcerRule extends AbstractEnforcerRule {

    private static final Logger log = LoggerFactory.getLogger(CodeOwnersEnforcerRule.class);
    private File baseDir;

    private File codeOwnersFile;

    private boolean allFilesMustHaveCodeOwner = false;

    private boolean allExisingFilesMustHaveCodeOwner = false;

    private boolean allNewlyCreatedFilesMustHaveCodeOwner = false;

    private boolean verbose = false;

    private boolean showApprovers = false;

    private String unlikelyFilename = "NewlyCreated_NiElSbAsJeSwRoTeThIs";

    private GitlabConfiguration gitlab = null;

    private final MavenProject project;

    @Inject
    public CodeOwnersEnforcerRule(MavenProject project) {
        this.project = project;
    }

    public void execute() throws EnforcerRuleException {
        if (baseDir == null) {
            baseDir = project.getBasedir();
        }

        DirectoryOwners directoryOwners;
        boolean pass = true;
        try {
            CodeOwnersValidator validator =
                new CodeOwnersValidator(gitlab, new EnforcerLoggerSlf4j(getLog()), verbose);

            directoryOwners = validator.analyzeDirectory(baseDir, codeOwnersFile);

            if (showApprovers) {
                getLog().info("Approvers:\n" + directoryOwners.toTable());
            }

        } catch (CodeOwnersValidationException e) {
            throw new EnforcerRuleException(e.getMessage(), e);
        }

        if (allFilesMustHaveCodeOwner || allExisingFilesMustHaveCodeOwner) {
            if (!directoryOwners.allExistingFilesHaveMandatoryCodeOwner()) {
                pass = false;
            }
        }
        if (allFilesMustHaveCodeOwner || allNewlyCreatedFilesMustHaveCodeOwner) {
            if (!directoryOwners.allNewlyCreatedFilesHaveMandatoryCodeOwner()) {
                pass = false;
            }
        }
        if (!pass) {
            throw new EnforcerRuleException("The failed checks:\n" + directoryOwners.toTable());
        }
    }

    // ------------------------------------------

    /**
     * If your rule is cacheable, you must return a unique id when parameters or conditions
     * change that would cause the result to be different. Multiple cached results are stored
     * based on their id.
     * <p>
     * The easiest way to do this is to return a hash computed from the values of your parameters.
     * <p>
     * If your rule is not cacheable, then you don't need to override this method or return null
     */
    @Override
    public String getCacheId() {
        // Because of the Gitlab API calls this cannot be cached.
        return null;
    }

    // ------------------------------------------

    /**
     * A good practice is provided toString method for Enforcer Rule.
     * <p>
     * Output is used in verbose Maven logs, can help during investigate problems.
     *
     * @return rule description
     */
    @Override
    public String toString() {
        return String.format(
            "CodeOwnersEnforcerRule[codeOwnersFile=%s ; allFilesMustHaveCodeOwner=%b]",
            codeOwnersFile, allFilesMustHaveCodeOwner);
    }
}

