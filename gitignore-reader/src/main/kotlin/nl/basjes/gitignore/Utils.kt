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
package nl.basjes.gitignore

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object Utils {
    private val LOG: Logger = LoggerFactory.getLogger(Utils::class.java)

    /**
     * Automatically find all non-ignored directories and files starting in the projects root.
     *
     * @return List all non-ignored directories and files.
     */
    @JvmStatic
    @JvmOverloads
    fun findAllNonIgnored(
        gitIgnoreFileSet: GitIgnoreFileSet,
        baseDir: Path = gitIgnoreFileSet.projectBaseDir.toPath()
    ): MutableList<Path?> {
        val wasProjectRelative = gitIgnoreFileSet.isAssumeQueriesAreProjectRelative
        // Because all files will be forced to be project relative we must change the gitIgnores matching.
        gitIgnoreFileSet.assumeQueriesAreProjectRelative()
        //        gitIgnoreFileSet.assumeQueriesIncludeProjectBaseDir();
        val allNonIgnored = findAllNonIgnored(gitIgnoreFileSet, baseDir, baseDir, 128)

        gitIgnoreFileSet.setAssumeProjectRelativeQueries(wasProjectRelative)
        return allNonIgnored
    }

    /**
     * Recursively find all directories and files that are not ignored.
     * @param current The current directory
     * @param maxRecursionDepth A limiter to avoid going infinitely deep.
     * @return List of the found Paths.
     */
    private fun findAllNonIgnored(
        gitIgnoreFileSet: GitIgnoreFileSet,
        current: Path,
        baseDir: Path,
        maxRecursionDepth: Int
    ): MutableList<Path?> {
        val found: MutableList<Path?> = ArrayList<Path?>()
        val subDirs: MutableList<Path> = ArrayList<Path>()

        if (!Files.isDirectory(current)) {
            LOG.debug("Locate GI: Not DIR  {}", current)
            return mutableListOf<Path?>() // It must be a directory
        }

        if (gitIgnoreFileSet.ignoreFile(baseDir.relativize(current).toString())) {
            LOG.debug("Locate GI: Ignored  {}", current)
            return mutableListOf<Path?>() // Is ignored
        }

        LOG.debug("Locate GI: Scan     {}", current)

        found.add(current)

        try {
            Files.newDirectoryStream(current).use { stream ->
                for (path in stream) {
                    if (Files.isDirectory(path)) {
                        if (gitIgnoreFileSet.keepFile(baseDir.relativize(path).toString() + "/")) {
                            subDirs.add(path)
                        }
                    } else {
                        if (gitIgnoreFileSet.keepFile(baseDir.relativize(path).toString())) {
                            found.add(path)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            LOG.error("Unable to list the content of {} due to {}", current, e.toString())
            return mutableListOf<Path?>()
        }

        val nextMaxRecursionDepth = maxRecursionDepth - 1
        if (nextMaxRecursionDepth > 0) {
            for (subDir in subDirs) {
                found.addAll(findAllNonIgnored(gitIgnoreFileSet, subDir, baseDir, nextMaxRecursionDepth))
            }
        }
        Collections.sort<Path?>(found)
        return found
    }
}
