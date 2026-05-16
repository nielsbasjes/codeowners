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
package nl.basjes.gitignore

import nl.basjes.gitignore.GitIgnore.Companion.standardizeFilename
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/**
 * A class that holds a set of .gitignore files and also handles the interactions between them.
 */
/**
 * @param projectBaseDir The "absolute" directory which is to be used as the project root for all the gitignore files.
 * @param autoload       Automatically find and load all gitignore files (default = true).
 */
class GitIgnoreFileSet @JvmOverloads constructor(
    @JvmField val projectBaseDir: File,
    autoload: Boolean = true
) : FileFilter {
    // The TreeMap is used to sort the GitIgnore files by the name of the basedir they are relevant for.
    // This sorting is very important in the evaluation of the rules!
    private val gitIgnores = TreeMap<String, MutableList<GitIgnore>>()

    private var verbose = false

    // Do we assume a query to be project relative if not explicitly stated?
    var isAssumeQueriesAreProjectRelative: Boolean = false
        private set

    init {
        if (autoload) {
            addAllGitIgnoreFiles()
        }
    }

    /**
     * Make all currently loaded gitignore files become verbose when used.
     *
     * @param verbose True: be verbose, False: be silent
     */
    fun setVerbose(verbose: Boolean): GitIgnoreFileSet {
        this.verbose = verbose
        gitIgnores
            .values
            .flatten()
            .forEach { it.setVerbose(verbose) }
        return this
    }

    /**
     * Sets the default assumption on how to interpret the filenames that must be checked.
     *
     * @param assumeProjectRelativeQueries The new assumption: True = all queries are assumed to be project relative;  False = all queries are assumed to start with the path to the project directory in the SAME form as used during construction of this class.
     */
    fun setAssumeProjectRelativeQueries(assumeProjectRelativeQueries: Boolean): GitIgnoreFileSet {
        this.isAssumeQueriesAreProjectRelative = assumeProjectRelativeQueries
        return this
    }

    /**
     * Sets the default assumption on how to interpret the filenames that must be checked to "all queries are assumed to be project relative"
     */
    fun assumeQueriesAreProjectRelative(): GitIgnoreFileSet {
        return setAssumeProjectRelativeQueries(true)
    }

    /**
     * Sets the default assumption on how to interpret the filenames that must be checked to "all queries are assumed to start with the path to the project directory"
     */
    fun assumeQueriesIncludeProjectBaseDir(): GitIgnoreFileSet {
        return setAssumeProjectRelativeQueries(false)
    }

    val isAssumeQueriesIncludeProjectBaseDir: Boolean
        get() = !this.isAssumeQueriesAreProjectRelative


    /**
     * Add a new gitIgnore file to the set to be checked.
     * The rules in the files depend on the ordering over files!
     * If a second 'GitIgnore' is added for an existing directory then they will be sorted
     * in the order they have been added to this GitIgnoreFileSet.
     *
     * @param gitIgnore The instance of the gitIgnore file.
     */
    fun add(gitIgnore: GitIgnore) {
        gitIgnores
            .computeIfAbsent(gitIgnore.projectRelativeBaseDir) { mutableListOf() }
            .add(gitIgnore)
        gitIgnore.setVerbose(verbose)
    }

    /**
     * Add a .gitignore file to the set.
     *
     * @param gitIgnoreFile The handle of the file of a gitignore file to be added. This MUST be able to get the ABSOLUTE path within the project.
     */
    fun addGitIgnoreFile(gitIgnoreFile: File) {
        try {
            add(GitIgnore(getProjectRelative(gitIgnoreFile.parent), gitIgnoreFile))
        } catch (e: IOException) {
            LOG.error("Cannot read {} due to {}. Will skip this file.", gitIgnoreFile, e.message)
        }
    }

    /**
     * Automatically find all .gitignore files starting in the projects root and add them all to the set.
     *
     * @param includeGlobalGitignore Whether to also include the global gitignore
     * @return List of the loaded gitIgnore files.
     */
    @JvmOverloads
    fun addAllGitIgnoreFiles(includeGlobalGitignore: Boolean = true): MutableList<Path> {
        return addAllGitIgnoreFiles(projectBaseDir.toPath(), 128, includeGlobalGitignore)
    }

    /**
     * add the global gitignore file (from `$XDG_CONFIG_HOME/git/ignore`, or, if `$XDG_CONFIG_HOME` is either not set or empty, `$HOME/.config/git/ignore`)
     *
     * @return the loaded global gitignore file, or null if none was found.
     */
    fun addGlobalGitIgnore(): Path? {
        val ignorePath: Path? = getGlobalGitIgnore(System.getenv("XDG_CONFIG_HOME"), System.getenv("HOME"))
        if (ignorePath != null) {
            try {
                add(GitIgnore("/", ignorePath.toFile()))
                return ignorePath
            } catch (e: IOException) {
                LOG.error("Cannot read global gitignore file {} due to {}. Will skip this file.", ignorePath, e.message)
                return null
            }
        } else {
            return null
        }
    }

    /**
     * Recursively add the gitignore files found in directories.
     * This first scans the directory, adds the provided gitignore (if any),
     * and then only traverses into subdirectories that have not been ignored.
     *
     * @param current                The current directory
     * @param maxRecursionDepth      A limiter to avoid going infinitely deep.
     * @param includeGlobalGitignore Whether to also include the global gitignore
     * @return List of the loaded gitIgnore files.
     */
    private fun addAllGitIgnoreFiles(
        current: Path,
        maxRecursionDepth: Int,
        includeGlobalGitignore: Boolean
    ): MutableList<Path> {
        val loadedGitIgnoreFiles: MutableList<Path> = mutableListOf()
        val subDirs: MutableList<Path> = ArrayList<Path>()

        if (!Files.isDirectory(current)) {
            LOG.debug("Locate GI: Not DIR  {}", current)
            return mutableListOf() // It must be a directory
        }

        if (includeGlobalGitignore) {
            val globalGitIgnore = addGlobalGitIgnore()
            if (globalGitIgnore != null) {
                loadedGitIgnoreFiles.add(globalGitIgnore)
            }
        }

        var dirPath = current.toFile().path
        if (!dirPath.endsWith(File.separator)) {
            dirPath += File.separatorChar
        }

        if (ignoreFile(dirPath)) {
            LOG.debug("Locate GI: Ignored  {}", current)
            return mutableListOf() // Is ignored
        }

        LOG.debug("Locate GI: Scan     {}", current)

        try {
            Files.newDirectoryStream(current).use { stream ->
                for (path in stream) {
                    if (Files.isRegularFile(path)) {
                        if (".gitignore" == path.fileName.toString()) {
                            LOG.debug("Locate GI: ADDING   {}", path)
                            addGitIgnoreFile(path.toFile())
                            loadedGitIgnoreFiles.add(path)
                        }
                        continue
                    }
                    if (Files.isDirectory(path)) {
                        subDirs.add(path)
                    }
                }
            }
        } catch (e: IOException) {
            LOG.error("Unable to find .gitignore files in {} due to {}", projectBaseDir, e.toString())
            return mutableListOf()
        }

        val nextMaxRecursionDepth = maxRecursionDepth - 1
        if (nextMaxRecursionDepth > 0) {
            for (subDir in subDirs) {
                loadedGitIgnoreFiles.addAll(addAllGitIgnoreFiles(subDir, nextMaxRecursionDepth, false))
            }
        }
        return loadedGitIgnoreFiles
    }

    val isEmpty: Boolean
        get() = gitIgnores.isEmpty()

    /**
     * Checks if the file matches the stored expressions.
     * This is suitable for combining multiple sets of rules!
     *
     * @param filename The filename to be checked which follows the assumeProjectRelativeQueries flag.
     * @return NULL: not matched, True: must be ignored, False: it must be UNignored
     */
    fun isIgnoredFile(filename: String): Boolean? {
        return isIgnoredFile(filename, this.isAssumeQueriesAreProjectRelative)
    }

    /**
     * Checks if the file matches the stored expressions.
     * This is suitable for combining multiple sets of rules!
     *
     * @param filename   The filename to be checked.
     * @param isRelative True: The provided filename is a RELATIVE path (i.e. the project base directory is assumed to be the root).
     * False:  The provided filename is an ABSOLUTE path (i.e. it still includes the project base directory).
     * @return NULL: not matched, True: must be ignored, False: it must be UNignored
     */
    fun isIgnoredFile(filename: String, isRelative: Boolean): Boolean? {
        var result: Boolean? = null
        val projectBaseFileName = if (isRelative) filename else getProjectRelative(filename)

        // Iterate over all available GitIgnore files in the correct order!
        for (gitIgnoreLists in gitIgnores.values) {
            for (gitIgnore in gitIgnoreLists) {
                val isIgnoredFile: Boolean? = gitIgnore.isIgnoredFile(projectBaseFileName)
                if (isIgnoredFile != null) {
                    result = isIgnoredFile
                }
            }
        }
        return result
    }

    /**
     * Checks if the file matches the stored expressions.
     * This is NOT suitable for combining multiple sets of rules!
     *
     * @param filename          The filename to be checked.
     * @param isProjectRelative True: The provided filename is a project RELATIVE path (i.e. "/directory in project/filename").
     * False: The provided filename is a path that starts with the path used to initialize this class (i.e. "project base directory/directory in project/filename").
     * @return true: must be ignored, false: it must be not be ignored
     */
    @JvmOverloads
    fun ignoreFile(filename: String, isProjectRelative: Boolean = this.isAssumeQueriesAreProjectRelative): Boolean {
        return java.lang.Boolean.TRUE == isIgnoredFile(filename, isProjectRelative)
    }

    /**
     * Checks if the file matches the stored expressions.
     * This is NOT suitable for combining multiple sets of rules!
     *
     * @param filename          The filename to be checked.
     * @param isProjectRelative True: The provided filename is a project RELATIVE path (i.e. "/directory in project/filename").
     * False:  The provided filename is a path that starts with the path used to initialize this class (i.e. "project base directory/directory in project/filename").
     * @return true: must be kept, false: it must be ignored
     */
    @JvmOverloads
    fun keepFile(filename: String, isProjectRelative: Boolean = this.isAssumeQueriesAreProjectRelative): Boolean {
        return !ignoreFile(filename, isProjectRelative)
    }

    private fun getProjectRelative(fileName: String): String {
        val standardizedFilename = standardizeFilename(fileName)
        val projectBaseDirPath = standardizeFilename(projectBaseDir.path)
        if (standardizedFilename.startsWith(projectBaseDirPath)) {
            return standardizeFilename(fileName)
                .replace(("^\\Q$projectBaseDirPath\\E").toRegex(), "/")
                .replace("//", "/")
        }

        val projectBaseDirAbsolutePath = standardizeFilename(projectBaseDir.absolutePath)
        if (standardizedFilename.startsWith(projectBaseDirAbsolutePath)) {
            return standardizeFilename(fileName)
                .replace(("^\\Q$projectBaseDirAbsolutePath\\E").toRegex(), "/")
                .replace("//", "/")
        }

        throw IllegalArgumentException("The requested file \"$standardizedFilename\" is not relative to project root and is NOT in the projectBaseDir \"$projectBaseDirPath\"")
    }

    override fun accept(pathname: File): Boolean {
        return keepFile(pathname.name, true)
    }

    override fun toString(): String {
        val sb = StringBuilder("GitIgnoreFileSet\n")
        sb.append("# Project base directory: ").append(projectBaseDir)
            .append(" ( Absolute: ").append(projectBaseDir.absoluteFile).append(" )\n")
            .append("=========================\n")

        for (gitIgnoreListEntry in gitIgnores.entries) {
            for (gitIgnore in gitIgnoreListEntry.value) {
                sb
                    .append(gitIgnore)
                    .append("=========================\n")
            }
        }

        return sb.toString()
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(GitIgnoreFileSet::class.java)

        /**
         * add the global gitignore file (from `$XDG_CONFIG_HOME/git/ignore`, or, if `$XDG_CONFIG_HOME` is either not set or empty, `$HOME/.config/git/ignore`)
         *
         * @return the loaded global gitignore file, or null if none was found.
         */
        @JvmStatic
        fun getGlobalGitIgnore(xdgConfigHome: String?, home: String?): Path? {
            val ignorePath: Path?
            ignorePath = if (!xdgConfigHome.isNullOrEmpty()) {
                File(xdgConfigHome).toPath().resolve("git").resolve("ignore")
            } else {
                if (!home.isNullOrEmpty()) {
                    File(home).toPath().resolve(".config").resolve("git").resolve("ignore")
                } else {
                    return null
                }
            }
            return if (Files.isRegularFile(ignorePath)) {
                ignorePath
            } else {
                null
            }
        }
    }
}
