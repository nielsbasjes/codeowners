/*
 * CodeOwners Tools
 * Copyright (C) 2023-2026 Niels Basjes
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
package nl.basjes.maven.enforcer.codeowners

import nl.basjes.codeowners.validator.CodeOwnersValidationException
import nl.basjes.codeowners.validator.CodeOwnersValidator
import nl.basjes.codeowners.validator.CodeOwnersValidator.DirectoryOwners
import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration
import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule
import org.apache.maven.enforcer.rule.api.EnforcerRuleException
import org.apache.maven.project.MavenProject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Named

@Suppress("unused") // Used by the enforcer-plugin that finds it via the @Named annotation
@Named("codeOwners") // rule name - must start from lowercase character
class CodeOwnersEnforcerRule @Inject constructor(private val project: MavenProject) : AbstractEnforcerRule() {
    private var baseDir: File? = null

    private val codeOwnersFile: File? = null

    private val allFilesMustHaveCodeOwner = false

    private val allExisingFilesMustHaveCodeOwner = false

    private val allNewlyCreatedFilesMustHaveCodeOwner = false

    private val verbose = false

    private val showApprovers = false

    private val unlikelyFilename = "NewlyCreated_NiElSbAsJeSwRoTeThIs"

    private val gitlab: GitlabConfiguration? = null

    @Throws(EnforcerRuleException::class)
    override fun execute() {
        if (baseDir == null) {
            baseDir = project.basedir
        }

        val directoryOwners: DirectoryOwners?
        var pass = true
        try {
            val validator =
                CodeOwnersValidator(gitlab, EnforcerLoggerSlf4j(log), verbose)

            directoryOwners = validator.analyzeDirectory(baseDir!!, codeOwnersFile)

            if (showApprovers) {
                log.info("Approvers:\n" + directoryOwners.toTable())
            }
        } catch (e: CodeOwnersValidationException) {
            throw EnforcerRuleException(e.message, e)
        }

        if (allFilesMustHaveCodeOwner || allExisingFilesMustHaveCodeOwner) {
            if (!directoryOwners.allExistingFilesHaveMandatoryCodeOwner()) {
                pass = false
            }
        }
        if (allFilesMustHaveCodeOwner || allNewlyCreatedFilesMustHaveCodeOwner) {
            if (!directoryOwners.allNewlyCreatedFilesHaveMandatoryCodeOwner()) {
                pass = false
            }
        }
        if (!pass) {
            throw EnforcerRuleException("The failed checks:\n" + directoryOwners.toTable())
        }
    }

    // ------------------------------------------
    /**
     * If your rule is cacheable, you must return a unique id when parameters or conditions
     * change that would cause the result to be different. Multiple cached results are stored
     * based on their id.
     *
     *
     * The easiest way to do this is to return a hash computed from the values of your parameters.
     *
     *
     * If your rule is not cacheable, then you don't need to override this method or return null
     */
    override fun getCacheId(): String? {
        // Because of the Gitlab API calls this cannot be cached.
        return null
    }

    // ------------------------------------------
    /**
     * A good practice is provided toString method for Enforcer Rule.
     *
     *
     * Output is used in verbose Maven logs, can help during investigate problems.
     *
     * @return rule description
     */
    override fun toString(): String {
        return String.format(
            "CodeOwnersEnforcerRule[codeOwnersFile=%s ; allFilesMustHaveCodeOwner=%b]",
            codeOwnersFile, allFilesMustHaveCodeOwner
        )
    }

    companion object {
        private val log: Logger? = LoggerFactory.getLogger(CodeOwnersEnforcerRule::class.java)
    }
}

