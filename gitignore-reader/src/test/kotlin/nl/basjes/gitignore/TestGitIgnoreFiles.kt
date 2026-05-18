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
import nl.basjes.gitignore.GitIgnoreFileSet.Companion.getGlobalGitIgnore
import nl.basjes.gitignore.Utils.findAllNonIgnored
import kotlin.test.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junitpioneer.jupiter.ClearEnvironmentVariable
import org.junitpioneer.jupiter.EnvironmentVariableUtilsFacade
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.streams.asSequence

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class TestGitIgnoreFiles {
    val expectedKeepFiles = listOf(
        "/",
        "/dir1",
        "/dir1/dir1.md",
        "/dir1/.gitignore",
        "/dir2",
        "/dir2/dir2.txt",
        "/dir2/file2.log",
        "/dir3",
        "/dir3/dir3.txt",
        "/dir3/file3.log",
        "/dir3/.gitignore",
        "/dir5",
        "/dir5/file5.log",
        "/.gitignore"
    ).sorted()

    val expectedIgnoredFiles = listOf(
        "/dir1/dir1.log",
        "/dir1/dir1.txt",
        "/dir1/file1.log",
        "/dir2/dir2.log",
        "/dir2/dir2.md",
        "/dir3/dir3.log",
        "/dir3/dir3.md",
        "/dir4/.gitignore",
        "/dir4/dir4.log",
        "/dir4/dir4.md",
        "/dir4/dir4.txt",
        "/dir4/file4.log",
        "/dir4/subdir/.gitignore",
        "/dir4/subdir/dir1.log",
        "/dir4/subdir/dir1.md",
        "/dir4/subdir/dir1.txt",
        "/dir4/subdir/file1.log",
        "/dir4/subdir/subdir/.gitignore",
        "/dir4/subdir/subdir/dir1.log",
        "/dir4/subdir/subdir/dir1.md",
        "/dir4/subdir/subdir/dir1.txt",
        "/dir4/subdir/subdir/file1.log",
        "/dir5/ignored_from_global_gitignore",
        "/README.md",
        "/root.md"
    ).sorted()

    private fun stripTestTreeBaseDir(input: List<Path>): List<String> {
        return input
            .map { it.toString() }
            .map { standardizeFilename(it) }  // Needed for testing on Windows.
            .map { it.replace(("^\\Q/$TEST_TREE_DIR\\E").toRegex(), "/").replace("//", "/") }
            .sorted()
            .toList()

    }

    private fun stripTestTreeBaseDir(input: Path): String {
        return standardizeFilename(input.toString())
            .replace(("^\\Q/$TEST_TREE_DIR\\E").toRegex(), "/")
            .replace("//", "/")
    }

    @Test
    fun ensureExpectationsDoNotOverlap() {
        for (expectedKeepFile in expectedKeepFiles) {
            assertFalse(
                expectedIgnoredFiles.contains(expectedKeepFile),
                "Expected Ignores has $expectedKeepFile"
            )
        }
        for (expectedIgnoreFile in expectedIgnoredFiles) {
            assertFalse(
                expectedKeepFiles.contains(expectedIgnoreFile),
                "Expected Keeps has $expectedIgnoreFile"
            )
        }
    }

    @Test
    @Throws(IOException::class)
    fun ensureAllFilesInTestTreeAreEitherInKeepOrIgnore() {
        Files
            .find(
                testTree.toPath(),
                128,
                { _: Path, fileAttr: BasicFileAttributes -> fileAttr.isRegularFile }
            )
            .use {
                it
                    .sorted()
                    .map { path -> stripTestTreeBaseDir(path) }
                    .forEach {
                        name ->
                            assertTrue(
                                expectedIgnoredFiles.contains(name) || expectedKeepFiles.contains(name),
                                "Missing entry for $name"
                            )
                    }
            }
    }

    private fun checkIgnoredList(ignore: List<String>) {
        checkIgnoredList(expectedIgnoredFiles, ignore)
    }

    private fun checkIgnoredList(expectedIgnore: List<String>?, ignore: List<String>) {
        val ignored = ignore
            .map { standardizeFilename(File(it).absolutePath) }  // Needed for testing on Windows.
            .map { it.replace( ("^\\Q" + standardizeFilename(testTree.absolutePath) + "\\E").toRegex(), "") }
            .sorted()
            .toList()
        assertEquals(expectedIgnore, ignored)
    }

    @Test
    @Throws(Exception::class)
    fun testGetGlobalGitIgnore() {
        assertNull(getGlobalGitIgnore(null, null))
        assertNull(getGlobalGitIgnore(null, ""))
        assertNull(getGlobalGitIgnore("", null))
        assertNull(getGlobalGitIgnore("", ""))

        val dirWithGitIgnoreURL = this.javaClass
            .classLoader
            .getResource("xdg_config_home")
        assertNotNull(dirWithGitIgnoreURL)
        val dirWithGitIgnore = dirWithGitIgnoreURL.file

        val dirWithConfigGitIgnoreURL = this.javaClass
            .classLoader
            .getResource("home")
        assertNotNull(dirWithConfigGitIgnoreURL)
        val dirWithConfigGitIgnore = dirWithConfigGitIgnoreURL.file

        assertNull(getGlobalGitIgnore(dirWithConfigGitIgnore, ""))
        assertNull(getGlobalGitIgnore("", dirWithGitIgnore))

        assertEquals(
            Paths.get(dirWithGitIgnoreURL.toURI()).resolve("git").resolve("ignore"),
            getGlobalGitIgnore(dirWithGitIgnore, "")
        )
        assertEquals(
            Paths.get(dirWithGitIgnoreURL.toURI()).resolve("git").resolve("ignore"),
            getGlobalGitIgnore(dirWithGitIgnore, dirWithConfigGitIgnore)
        )

        assertEquals(
            Paths.get(dirWithConfigGitIgnoreURL.toURI()).resolve(".config").resolve("git").resolve("ignore"),
            getGlobalGitIgnore(null, dirWithConfigGitIgnore)
        )
        assertNull(getGlobalGitIgnore(dirWithConfigGitIgnore, dirWithConfigGitIgnore))
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    @Throws(Exception::class)
    fun testGetGlobalGitIgnoreUnreadableFiles() {
        val dirWithGitIgnoreURL = this.javaClass
            .classLoader
            .getResource("unreadable/.config")
        assertNotNull(dirWithGitIgnoreURL)
        val dirWithGitIgnore = dirWithGitIgnoreURL.file

        val dirWithConfigGitIgnoreURL = this.javaClass
            .classLoader
            .getResource("unreadable")
        assertNotNull(dirWithConfigGitIgnoreURL)
        val dirWithConfigGitIgnore = dirWithConfigGitIgnoreURL.file

        val ignoreFileToMakeUnreadable = File("$dirWithGitIgnore/git/ignore")

        val expectedIgnorePath = Paths.get(dirWithGitIgnoreURL.toURI()).resolve("git").resolve("ignore")

        assertTrue(
            ignoreFileToMakeUnreadable.isFile,
            "This $ignoreFileToMakeUnreadable should be a File."
        )
        assertTrue(
            ignoreFileToMakeUnreadable.canRead(),
            "This $ignoreFileToMakeUnreadable should be readable."
        )

        // Use Pioneer's underlying utility to inject it programmatically
        EnvironmentVariableUtilsFacade.set("XDG_CONFIG_HOME", dirWithGitIgnore)
        EnvironmentVariableUtilsFacade.set("HOME", dirWithConfigGitIgnore)

        try {
            LOG.info("Making {} unreadable", ignoreFileToMakeUnreadable)
            assertTrue(ignoreFileToMakeUnreadable.setReadable(false, false))
            assertTrue(
                ignoreFileToMakeUnreadable.isFile,
                "This $ignoreFileToMakeUnreadable should be a File."
            )
            assertFalse(
                ignoreFileToMakeUnreadable.canRead(),
                "This $ignoreFileToMakeUnreadable should NO LONGER be readable."
            )

            // Check 1: Even if the file is not readable it should be found
            assertEquals(expectedIgnorePath, getGlobalGitIgnore(null, dirWithConfigGitIgnore))
            assertEquals(expectedIgnorePath, getGlobalGitIgnore(dirWithGitIgnore, null))

            // Check 2: When trying to use it in a GitIgnoreFileSet it should all fail (for code coverage).
            val gitIgnoreFileSet = GitIgnoreFileSet(targetDirTestTree, false)
            gitIgnoreFileSet.addAllGitIgnoreFiles(true)
        } finally {
            assertTrue(ignoreFileToMakeUnreadable.setReadable(true, false))
            assertTrue(
                ignoreFileToMakeUnreadable.canRead(),
                "This $ignoreFileToMakeUnreadable should be readable."
            )
        }
    }


    @Test
    @DisabledOnOs(OS.WINDOWS)
    @Throws(Exception::class)
    fun testUnreadableDirectory() {
        val testTreeURL = this.javaClass
            .classLoader
            .getResource("testtree")
        assertNotNull(testTreeURL)

        val directoryToMakeUnreadable = Paths.get(testTreeURL.toURI()).resolve("dir1")
        val directoryToMakeUnreadableAsFile = directoryToMakeUnreadable.toFile()

        assertTrue(
            directoryToMakeUnreadableAsFile.isDirectory,
            "This $directoryToMakeUnreadableAsFile should be a Directory."
        )
        assertTrue(
            directoryToMakeUnreadableAsFile.canRead(),
            "This $directoryToMakeUnreadableAsFile should be readable."
        )
        assertTrue(
            directoryToMakeUnreadableAsFile.canExecute(),
            "This $directoryToMakeUnreadableAsFile should be executable."
        )

        try {
            LOG.info("Making {} unreadable", directoryToMakeUnreadableAsFile)
            assertTrue(directoryToMakeUnreadableAsFile.setReadable(false, false))
            assertTrue(directoryToMakeUnreadableAsFile.setExecutable(false, false))

            assertTrue(
                directoryToMakeUnreadableAsFile.isDirectory,
                "This $directoryToMakeUnreadableAsFile should be a Directory."
            )
            assertFalse(
                directoryToMakeUnreadableAsFile.canRead(),
                "This $directoryToMakeUnreadableAsFile should NO LONGER be readable."
            )
            assertFalse(
                directoryToMakeUnreadableAsFile.canExecute(),
                "This $directoryToMakeUnreadableAsFile should NO LONGER be executable."
            )

            // Check 2: When trying to use it in a GitIgnoreFileSet it should all fail (for code coverage).
            val ioException = assertFailsWith<IOException> { GitIgnoreFileSet(targetDirTestTree, true) }
            assertTrue(ioException.message!!.contains("Unable to load .gitignore files"))
        } finally {
            LOG.info("Making {} readable", directoryToMakeUnreadableAsFile)
            assertTrue(directoryToMakeUnreadableAsFile.setReadable(true, false))
            assertTrue(directoryToMakeUnreadableAsFile.setExecutable(true, false))

            assertTrue(
                directoryToMakeUnreadableAsFile.isDirectory,
                "This $directoryToMakeUnreadableAsFile should be a Directory."
            )
            assertTrue(
                directoryToMakeUnreadableAsFile.canRead(),
                "This $directoryToMakeUnreadableAsFile should be readable."
            )
            assertTrue(
                directoryToMakeUnreadableAsFile.canExecute(),
                "This $directoryToMakeUnreadableAsFile should be executable."
            )
        }
    }

    @Test
    @Throws(IOException::class)
    fun testIsIgnoredFile() {
        val gitIgnoreFileSet = GitIgnoreFileSet(testTree) //            .setVerbose(true)
            .assumeQueriesIncludeProjectBaseDir()

        assertFalse(gitIgnoreFileSet.isEmpty, "Unable to load any .gitignore files")

        LOG.info("LOADED: {}", gitIgnoreFileSet)

        val ignore = mutableListOf<String>()

        Files.find(
            testTree.toPath(),
            128,
            { _: Path, fileAttr: BasicFileAttributes -> fileAttr.isRegularFile })
            .use { projectFiles ->
                for (path in projectFiles.sorted()) {
                    val ignoredFile = gitIgnoreFileSet.isIgnoredFile(path.toString())
                    if (ignoredFile == null) {
                        LOG.info("Path {} : Keep (No match)", path)
                    } else {
                        if (ignoredFile) {
                            LOG.error("Path {} : Ignore (Match) --------------- ", path)
                            ignore.add(path.toString())
                        } else {
                            LOG.warn("Path {} : Keep (Match) ++++++++++++++ ", path)
                        }
                    }
                }
            }
        checkIgnoredList(ignore)
    }

    @Test
    fun testVerboseFlag() {
        val gitIgnoreFileSet = GitIgnoreFileSet(testTree, false)
        gitIgnoreFileSet.verbose = false
        assertFalse(gitIgnoreFileSet.verbose)
        gitIgnoreFileSet.verbose = true
        assertTrue(gitIgnoreFileSet.verbose)
    }

    @Test
    @ClearEnvironmentVariable(key = "XDG_CONFIG_HOME")
    @ClearEnvironmentVariable(key = "HOME")
    @Throws(IOException::class)
    fun testIgnoreFile() {
        val gitIgnoreFileSet = GitIgnoreFileSet(testTree)
        gitIgnoreFileSet.verbose = true
        gitIgnoreFileSet.assumeQueriesIncludeProjectBaseDir()

        assertFalse(gitIgnoreFileSet.isEmpty, "Unable to load any .gitignore files")

        Files.find(
            testTree.toPath(),
            128,
            { _: Path, fileAttr: BasicFileAttributes -> fileAttr.isRegularFile })
            .use { projectFiles ->
                val ignore = mutableListOf<String>()
                for (path in projectFiles.sorted()) {
                    if (gitIgnoreFileSet.ignoreFile(path.toString())) {
                        LOG.info("Path {} : Ignore --------------- ", path)
                        ignore.add(path.toString())
                    } else {
                        LOG.info("Path {} : Keep", path)
                    }
                }

                // Because we have excluded the global ignore file; the list of expected ignores must be updated.
                val adjustedExpectedIgnoredFiles = expectedIgnoredFiles.toMutableList()
                // This file is no longer ignored!
                adjustedExpectedIgnoredFiles.remove("/dir5/ignored_from_global_gitignore")
                checkIgnoredList(adjustedExpectedIgnoredFiles, ignore)
            }
    }

    @Test
    fun testRecursiveIncrementalLoading() {
        val gitIgnoreFileSet = GitIgnoreFileSet(testTree, false)
        gitIgnoreFileSet.verbose = true

        val files = recursivelyFindFiles(gitIgnoreFileSet, testTree)

        // Check we got it right
        files
            .map { it.path }
            .map { standardizeFilename(it) }  // Needed for testing on Windows.
            .map { it.replace(testTree.path, "") }
            .forEach { assertFalse(expectedIgnoredFiles.contains(it)) }
    }

    private fun recursivelyFindFiles(gitIgnoreFileSet: GitIgnoreFileSet, directory: File): List<File> {
        // Load the new gitignore files (if any).
        val newGitIgnoreFiles =
            directory.listFiles { it.name == ".gitignore" }?.toList()?:listOf()
        newGitIgnoreFiles.forEach { gitIgnoreFileSet.addGitIgnoreFile(it) }

        // Only keep the non ignored files
        val normalFiles =
            directory.listFiles(gitIgnoreFileSet)?.toList() ?: listOf<File>() // << Using it as a FileFilter

        val result = mutableListOf<File>()
        for (file in normalFiles) {
            if (file.isFile) {
                if (!gitIgnoreFileSet.ignoreFile(file.path)) {
                    result.add(file)
                }
            }

            if (file.isDirectory) {
                if (!gitIgnoreFileSet.ignoreFile(file.path)) {
                    result.addAll(recursivelyFindFiles(gitIgnoreFileSet, file))
                }
            }
        }
        return result
    }

    @Test
    @Throws(IOException::class)
    fun testFileFilter() {
        val gitIgnoreFileSet = GitIgnoreFileSet(testTree).assumeQueriesIncludeProjectBaseDir()

        Files
            .find(
                testTree.toPath(),
                128,
                { filePath: Path, fileAttr: BasicFileAttributes ->
                    fileAttr.isRegularFile && gitIgnoreFileSet.ignoreFile(filePath.toString()) }
            )
            .use {
                projectFiles ->
                val ignored = projectFiles
                    .asSequence()
                    .map { it.toString() }
                    .sorted()
                    .toList()
                checkIgnoredList(ignored)
            }
    }

    @Test
    fun testWindowsPaths() {
        val gitIgnoreFileSet =
            GitIgnoreFileSet(File(separatorsToWindows(testTree.path)), false).assumeQueriesAreProjectRelative()

        assertFalse(gitIgnoreFileSet.isAssumeQueriesIncludeProjectBaseDir)
        assertTrue(gitIgnoreFileSet.isAssumeQueriesAreProjectRelative)

        gitIgnoreFileSet.add(GitIgnore(separatorsToWindows("/"), "*.txt"))
        gitIgnoreFileSet.add(GitIgnore(separatorsToWindows("/dir1/"), "*.md"))
        gitIgnoreFileSet.add(GitIgnore(separatorsToWindows("/dir2/"), "!foo.txt"))

        assertFalse(gitIgnoreFileSet.isEmpty)

        assertTrue(gitIgnoreFileSet.ignoreFile("\\foo.txt"))
        assertTrue(gitIgnoreFileSet.ignoreFile("\\dir1\\foo.txt"))
        assertFalse(gitIgnoreFileSet.ignoreFile("\\dir2\\foo.txt"))

        assertFalse(gitIgnoreFileSet.ignoreFile("\\foo.md"))
        assertTrue(gitIgnoreFileSet.ignoreFile("\\dir1\\foo.md"))
        assertFalse(gitIgnoreFileSet.ignoreFile("\\dir2\\foo.md"))
    }

    @Test
    fun testWindowsPathsWithDriveLetter() {
        val gitIgnoreFileSet =
            GitIgnoreFileSet(File("A:\\MyProject\\src\\test\\resources\\"), false).assumeQueriesIncludeProjectBaseDir()

        assertTrue(gitIgnoreFileSet.isAssumeQueriesIncludeProjectBaseDir)
        assertFalse(gitIgnoreFileSet.isAssumeQueriesAreProjectRelative)

        gitIgnoreFileSet.add(GitIgnore("\\", "*.txt"))
        gitIgnoreFileSet.add(GitIgnore("\\dir1\\", "*.md"))
        gitIgnoreFileSet.add(GitIgnore("\\dir2\\", "!foo.txt"))
        gitIgnoreFileSet.verbose = true

        assertFalse(gitIgnoreFileSet.isEmpty)

        // Correctly handle absolute paths (assume)
        gitIgnoreFileSet.assumeQueriesIncludeProjectBaseDir()
        assertTrue(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\foo.txt"))
        assertTrue(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\dir1\\foo.txt"))
        assertFalse(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\dir2\\foo.txt"))

        assertFalse(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\foo.md"))
        assertTrue(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\dir1\\foo.md"))
        assertFalse(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\dir2\\foo.md"))

        // Correctly handle absolute paths (explicit)
        gitIgnoreFileSet.assumeQueriesAreProjectRelative()
        assertTrue(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\foo.txt", false))
        assertTrue(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\dir1\\foo.txt", false))
        assertFalse(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\dir2\\foo.txt", false))

        assertFalse(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\foo.md", false))
        assertTrue(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\dir1\\foo.md", false))
        assertFalse(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\dir2\\foo.md", false))

        // Correctly handle project relative paths (assume)
        gitIgnoreFileSet.assumeQueriesAreProjectRelative()
        assertTrue(gitIgnoreFileSet.ignoreFile("\\foo.txt"))
        assertTrue(gitIgnoreFileSet.ignoreFile("\\dir1\\foo.txt"))
        assertFalse(gitIgnoreFileSet.ignoreFile("\\dir2\\foo.txt"))

        assertFalse(gitIgnoreFileSet.ignoreFile("\\foo.md"))
        assertTrue(gitIgnoreFileSet.ignoreFile("\\dir1\\foo.md"))
        assertFalse(gitIgnoreFileSet.ignoreFile("\\dir2\\foo.md"))

        // Correctly handle project relative paths (explicit)
        gitIgnoreFileSet.assumeQueriesIncludeProjectBaseDir()
        assertTrue(gitIgnoreFileSet.ignoreFile("\\foo.txt", true))
        assertTrue(gitIgnoreFileSet.ignoreFile("\\dir1\\foo.txt", true))
        assertFalse(gitIgnoreFileSet.ignoreFile("\\dir2\\foo.txt", true))

        assertFalse(gitIgnoreFileSet.ignoreFile("\\foo.md", true))
        assertTrue(gitIgnoreFileSet.ignoreFile("\\dir1\\foo.md", true))
        assertFalse(gitIgnoreFileSet.ignoreFile("\\dir2\\foo.md", true))
    }

    @Test
    fun testErrorHandlingNosuchDirectory() {
        val gitIgnoreFileSet = GitIgnoreFileSet(File("/no-such-file-really"))
        assertTrue(gitIgnoreFileSet.isEmpty)
    }

    @Test
    fun testErrorHandlingNosuchFile() {
        val gitIgnoreFileSet = GitIgnoreFileSet(File("src/test/resources/"), false)
        gitIgnoreFileSet.addGitIgnoreFile(File("src/test/resources/no-such-file-really"))
        assertTrue(gitIgnoreFileSet.isEmpty)
    }

    @Test
    fun ignoreDirectoriesWithAndWithoutSlash() {
        val gitIgnoreFileSet = GitIgnoreFileSet(testTree, false)
        val addedGitIgnoreFiles: List<Path?> = gitIgnoreFileSet.addAllGitIgnoreFiles(false)

        LOG.info("Added gitignore files: {}", addedGitIgnoreFiles)

        val expected = mutableListOf(
            "/src/test/resources/testtree/.gitignore",
            "/src/test/resources/testtree/dir1/.gitignore",
            "/src/test/resources/testtree/dir3/.gitignore"
        )

        assertEquals(
            expected, addedGitIgnoreFiles
                .map { it.toString() }
                .map { standardizeFilename(it) }  // Needed for testing on Windows.
                .sorted()
                .toList())
    }

    @Test
    fun listNonIgnoredFilesAndDirectories() {
        val gitIgnoreFileSet = GitIgnoreFileSet(testTree).assumeQueriesIncludeProjectBaseDir()
        val allNonIgnored: List<Path> = findAllNonIgnored(gitIgnoreFileSet)

        LOG.info("All non ignored files: {}", allNonIgnored)

        assertEquals(expectedKeepFiles, stripTestTreeBaseDir(allNonIgnored))

        // Correct use: Relative path
        assertEquals(true, gitIgnoreFileSet.isIgnoredFile("/README.md", true))
        // Correct use: Absolute path
        assertEquals(true, gitIgnoreFileSet.isIgnoredFile(testTree.absolutePath + "/README.md", false))
        // Wrong use: Relative path when absolute is needed.
        assertFailsWith<IllegalArgumentException> { gitIgnoreFileSet.isIgnoredFile("/README.md", false) }
    }

    @Test
    fun triggerNotExist() {
        val gitIgnoreFileSet = GitIgnoreFileSet(testTree).assumeQueriesIncludeProjectBaseDir()
        val allNonIgnored: List<Path?> =
            findAllNonIgnored(gitIgnoreFileSet, File("/no-such-file-really").toPath())
        assertTrue(allNonIgnored.isEmpty())
    }

    @Test
    fun triggerNotADirectory() {
        val gitIgnoreFileSet = GitIgnoreFileSet(testTree).assumeQueriesIncludeProjectBaseDir()
        val allNonIgnored: List<Path?> = findAllNonIgnored(
            gitIgnoreFileSet,
            File(gitIgnoreFileSet.projectBaseDir.toPath().toString() + "/README.md").toPath()
        )
        assertTrue(allNonIgnored.isEmpty())
    }

    @Test
    fun triggerEverythingIsIgnored() {
        val gitIgnoreFileSet = GitIgnoreFileSet(testTree).assumeQueriesIncludeProjectBaseDir()
        val gitIgnore = GitIgnore("*")
        gitIgnoreFileSet.add(gitIgnore)
        val allNonIgnored: List<Path?> = findAllNonIgnored(gitIgnoreFileSet)
        assertTrue(allNonIgnored.isEmpty())
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    fun triggerIOErrorDirectory() {
        val gitIgnoreFileSet = GitIgnoreFileSet(testTree).assumeQueriesIncludeProjectBaseDir()

        val testDir = File(gitIgnoreFileSet.projectBaseDir.toPath().toString() + "/dir5")
        assertTrue(testDir.isDirectory)
        assertTrue(testDir.canExecute())
        assertTrue(testDir.canRead())
        val allNonIgnored: List<Path?> = findAllNonIgnored(gitIgnoreFileSet)
        assertTrue(
            allNonIgnored.contains(testDir.toPath()),
            "The list $allNonIgnored is missing $testDir"
        )

        try {
            assertTrue(testDir.setExecutable(false, false))
            assertTrue(testDir.setReadable(false, false))
            val allNonIgnored2: List<Path?> = findAllNonIgnored(gitIgnoreFileSet)
            assertFalse(
                allNonIgnored2.contains(testDir.toPath()),
                "The list $allNonIgnored SHOULD NOT contain $testDir"
            )
        } finally {
            assertTrue(testDir.setExecutable(true, false))
            assertTrue(testDir.setReadable(true, false))
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(TestGitIgnoreFiles::class.java)

        fun separatorsToWindows(path: String): String {
            return path.replace("/", "\\")
        }

        const val TEST_TREE_DIR: String = "src/test/resources/testtree"
        val testTree: File = File(TEST_TREE_DIR)
        val targetDirTestTree: File = File(TestGitIgnoreFiles::class.java.classLoader.getResource("testtree")?.file ?: throw IllegalStateException("This should not occur"))
    }
}
