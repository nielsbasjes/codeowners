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
package nl.basjes.codeowners.validator

import nl.basjes.codeowners.CodeOwners
import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration
import nl.basjes.codeowners.validator.gitlab.GitlabProjectMembers
import nl.basjes.codeowners.validator.utils.StringTable
import nl.basjes.gitignore.GitIgnore
import nl.basjes.gitignore.GitIgnoreFileSet
import nl.basjes.gitignore.Utils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors

class CodeOwnersValidator(
    private val gitlab: GitlabConfiguration?,
    var logger: Logger?,
    var verbose: Boolean = false,
) {
    private var log = LoggerFactory.getLogger("CodeOwnersValidator") // Default logger

    private var gitlabConfigurationIsValid = false

    enum class FileType {
        FILE,
        DIRECTORY,
        NEWLY_CREATED_FILE,
    }

    class FileOwners(val path: Path?, val fileType: FileType?) {
        val approvers: MutableList<String> = mutableListOf()
        @JvmField
        val mandatoryApprovers: MutableList<String> = mutableListOf()

        fun addApprovers(approvers: List<String>) {
            this.approvers.addAll(approvers)
        }

        fun addMandatoryApprovers(approvers: List<String>) {
            this.mandatoryApprovers.addAll(approvers)
        }

        override fun toString(): String {
            return "FileOwners{" +
                    "path=" + path +
                    ", fileType=" + fileType +
                    ", approvers=" + approvers +
                    ", mandatoryApprovers=" + mandatoryApprovers +
                    '}'
        }
    }

    class DirectoryOwners(
        var baseDir: Path?, // Get the ignore rules
        var gitIgnores: GitIgnoreFileSet?, // Get the codeowners
        var codeOwners: CodeOwners
    ) : TreeMap<Path, FileOwners>() {
        /**
         * @return TRUE if all existing files and directories have at least 1 mandatory code owner, else FALSE
         */
        fun allExistingFilesHaveMandatoryCodeOwner(): Boolean {
            for (fileOwners in values) {
                if (fileOwners.fileType == FileType.FILE || fileOwners.fileType == FileType.NEWLY_CREATED_FILE) {
                    if (fileOwners.mandatoryApprovers.isEmpty()) {
                        return false
                    }
                }
            }
            return true
        }

        /**
         * @return TRUE if all existing files and directories have at least 1 mandatory or optional code owner, else FALSE
         */
        fun allExistingFilesHaveAnyCodeOwner(): Boolean {
            for (fileOwners in values) {
                if (fileOwners.fileType == FileType.FILE || fileOwners.fileType == FileType.NEWLY_CREATED_FILE) {
                    if (fileOwners.approvers.isEmpty()) {
                        return false
                    }
                }
            }
            return true
        }

        /**
         * @return TRUE if all newly created files and directories have at least 1 mandatory code owner, else FALSE
         */
        fun allNewlyCreatedFilesHaveMandatoryCodeOwner(): Boolean {
            for (fileOwners in values) {
                if (fileOwners.fileType == FileType.NEWLY_CREATED_FILE) {
                    if (fileOwners.mandatoryApprovers.isEmpty()) {
                        return false
                    }
                }
            }
            return true
        }

        /**
         * @return TRUE if all existing files and directories have at least 1 mandatory or optional code owner, else FALSE
         */
        fun allNewlyCreatedFilesHaveAnyCodeOwner(): Boolean {
            for (fileOwners in values) {
                if (fileOwners.fileType == FileType.NEWLY_CREATED_FILE) {
                    if (fileOwners.approvers.isEmpty()) {
                        return false
                    }
                }
            }
            return true
        }

        // ------------------------------------------
        fun toTable(): String {
            val table = StringTable()
            table.withHeaders("Path", "Mandatory Approvers")
            for (path in navigableKeySet()) {
                val mandatoryApprovers = codeOwners.getMandatoryApprovers(path.toString())
                if (mandatoryApprovers.isEmpty()) {
                    table.addRow(pathToLoggingString(path), mandatoryApprovers.toString(), "<-- NO APPROVERS!")
                } else {
                    table.addRow(pathToLoggingString(path), mandatoryApprovers.toString())
                }
            }
            return table.toString()
        }

        // ------------------------------------------
        fun toJson(): String {
            val result = StringBuilder("[")
            var first = true
            for (path in navigableKeySet()) {
                if (first) {
                    first = false
                } else {
                    result.append(",")
                }
                val mandatoryApprovers = codeOwners.getMandatoryApprovers(path.toString())
                var filename =
                    GitIgnore.standardizeFilename(path.toString()) + (if (path.toFile().isDirectory) "/" else "")
                filename = filename.replace("/+".toRegex(), "/")
                result.append("{\"${filename}\":[\"${mandatoryApprovers.joinToString("\",\"")}\"]\"]}")
            }
            return result.append("]").toString()
        }
    }

    init {
        if (logger != null) {
            this.log = logger
        }

        if (gitlab != null) {
            if (gitlab.isDefaultCIConfigRunningOutsideCI) {
                log.warn("Found Gitlab CI config that only works within Gitlab CI.")
                log.warn("Skipping Gitlab Project Members check because this is not GitlabCI.")
                log.info("Found GitLab configuration:\n{}", gitlab)
                gitlabConfigurationIsValid = false
            } else {
                gitlabConfigurationIsValid = gitlab.isValid
                if (gitlab.isValid) {
                    if (verbose) {
                        log.info("Using GitLab configuration:\n{}", gitlab)
                    }
                } else {
                    throw CodeOwnersValidationException("The GitLab configuration is not valid:\n$gitlab")
                }
            }
        }
    }

    @JvmOverloads
    @Throws(CodeOwnersValidationException::class)
    fun analyzeDirectory(
        baseDir: File,
        codeOwnersFile: File? = null
    ): DirectoryOwners {
        if (!baseDir.isDirectory) {
            throw CodeOwnersValidationException("The provided baseDir is invalid: $baseDir")
        }
        val baseDirPath = baseDir.toPath()

        if (verbose) {
            log.info("BaseDir=|{}|", baseDir)
        } else {
            log.debug("BaseDir=|{}|", baseDir)
        }

        // Get the ignore rules
        val gitIgnores = loadAllGitIgnoreFiles(baseDir)
        // Because all files will be forced to be project relative we must change the gitIgnores matching.
        gitIgnores.assumeQueriesAreProjectRelative()

        // Get the codeowners
        val codeOwners = loadCodeOwners(baseDir, codeOwnersFile)

        if (verbose) {
            log.info("=================================")
            log.info("All configured GitIgnore rules:\n{}", gitIgnores)
            log.info("=================================")
            log.info("All configured CODEOWNER rules:\n{}", codeOwners)
            log.info("=================================")
        }

        val result = DirectoryOwners(baseDirPath, gitIgnores, codeOwners)

        // Get a list of all files in the project and sort them
        val allNonIgnoredFilesAndDirectoriesInProject =
            Utils.findAllNonIgnored(gitIgnores)
                .stream()
                .map { baseDirPath.relativize(it) }
                .sorted()
                .distinct()
                .collect(Collectors.toList())


        // Set everything to the requested verbosity
        codeOwners.setVerbose(verbose)
        gitIgnores.setVerbose(verbose)

        for (path in allNonIgnoredFilesAndDirectoriesInProject) {
            val fullFile = File(baseDir.absoluteFile, path.toString())
            if (fullFile.isFile) {
                val fileOwners = FileOwners(path, FileType.FILE)
                fileOwners.addApprovers(codeOwners.getAllApprovers(path.toString()))
                fileOwners.addMandatoryApprovers(codeOwners.getMandatoryApprovers(path.toString()))
                result[path] = fileOwners
                continue
            }
            if (fullFile.isDirectory) {
                val fileOwners = FileOwners(path, FileType.DIRECTORY)
                fileOwners.addApprovers(codeOwners.getAllApprovers(path.toString()))
                fileOwners.addMandatoryApprovers(codeOwners.getMandatoryApprovers(path.toString()))
                result[path] = fileOwners

                val newFile = Path.of(path.toString(), UNLIKELY_FILENAME)
                if (gitIgnores.keepFile(newFile.toString())) {
                    val newFileIndex = Path.of(path.toString(), "x")
                    val newFileOwners = FileOwners(newFileIndex, FileType.NEWLY_CREATED_FILE)
                    newFileOwners.addApprovers(codeOwners.getAllApprovers(newFile.toString()))
                    newFileOwners.addMandatoryApprovers(codeOwners.getMandatoryApprovers(newFile.toString()))
                    result[newFileIndex] = newFileOwners
                }
            }
        }


        if (gitlab != null && gitlabConfigurationIsValid) {
            gitlab.let {
                GitlabProjectMembers(it)
                    .use { gitlabProjectMembers ->
                        val problemTable = gitlabProjectMembers.verifyAllCodeowners(log, codeOwners)
                        if (it.showAllApprovers || problemTable.hasWarnings() || problemTable.hasErrors() || problemTable.hasFatalErrors()) {
                            log.info(problemTable.toString())
                            log.info("============================")
                            log.info("Summary per problem message:")
                            log.info(problemTable.toProblemMessageGroupedString())
                        }
                        gitlabProjectMembers.failIfExceededFailLevel(problemTable)
                    }
            }
        }

        return result
    }

    // ------------------------------------------
    fun loadAllGitIgnoreFiles(baseDir: File): GitIgnoreFileSet {
        // Get the files that are ignored by the SCM
        val gitIgnores = GitIgnoreFileSet(baseDir, false)
            .assumeQueriesIncludeProjectBaseDir()

        gitIgnores.setVerbose(verbose)

        // Start with the internal files that are used by common SCMs.
        gitIgnores.add(
            GitIgnore(
                "/.git/\n" +
                        "/.hg/\n" +
                        ".svn/\n"
            )
        )

        // Load all available gitignore configs.
        val loadedFiles = gitIgnores.addAllGitIgnoreFiles()
        for (loadedFile in loadedFiles) {
            if (verbose) {
                log.info("Using GitIgnore : " + pathToLoggingString(baseDir.toPath().relativize(loadedFile)))
            }
        }

        return gitIgnores
    }

    // ------------------------------------------
    @Throws(CodeOwnersValidationException::class)
    fun loadCodeOwners(baseDir: File, codeOwnersFile: File?): CodeOwners {
        var codeOwnersFile = codeOwnersFile
        val commonCodeOwnersFiles = mutableListOf<kotlin.String>(
            "/CODEOWNERS",
            "/.github/CODEOWNERS",
            "/.gitlab/CODEOWNERS",
            "/docs/CODEOWNERS"
        )

        if (codeOwnersFile == null) {
            // If no file was specified we try the default locations
            for (codeOwnersFileName in commonCodeOwnersFiles) {
                val tryingFile = File(baseDir.toString() + codeOwnersFileName)
                if (tryingFile.exists() && tryingFile.isFile()) {
                    codeOwnersFile = tryingFile
                    break
                }
            }
        }

        if (codeOwnersFile == null) {
            throw CodeOwnersValidationException("This project does NOT have a CODEOWNERS file")
        }

        if (verbose) {
            log.info("Using CODEOWNERS: " + pathToLoggingString(baseDir.toPath().relativize(codeOwnersFile.toPath())))
        }
        val codeOwners = CodeOwners(codeOwnersFile)
        log.debug(codeOwners.toString())
        return codeOwners
    }

    companion object {
        private const val UNLIKELY_FILENAME = "NewlyCreated_NiElSbAsJeSwRoTeThIs"

        // ------------------------------------------
        fun pathToLoggingString(path: Path): kotlin.String {
            var filename =
                GitIgnore.standardizeFilename(path.toString()) + (if (path.toFile().isDirectory()) "/" else "")
            filename = filename.replace("/+".toRegex(), "/")
            if (filename.startsWith("/")) {
                return "\${baseDir}" + filename
            }
            return filename
        } // ------------------------------------------
    }
}

