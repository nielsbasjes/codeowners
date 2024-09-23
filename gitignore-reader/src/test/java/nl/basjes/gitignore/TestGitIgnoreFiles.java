/*
 * CodeOwners Tools
 * Copyright (C) 2023-2024 Niels Basjes
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

package nl.basjes.gitignore;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nl.basjes.gitignore.GitIgnore.standardizeFilename;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestGitIgnoreFiles {

    private static final Logger LOG = LoggerFactory.getLogger(TestGitIgnoreFiles.class);

    public static String separatorsToWindows(String path) {
        return path.replace("/", "\\");
    }

    final List<String> expectedIgnoredFiles = Stream.of(
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
        "/README.md",
        "/root.md").sorted().collect(Collectors.toList());

    static final File testTree = new File("src/test/resources/testtree");

    private void checkIgnoredList(List<String> ignore) {
        List<String> ignored = ignore
            .stream()
            .map(filename -> standardizeFilename(new File(filename).getAbsolutePath())) // Needed for testing on Windows.
            .map(filename -> filename.replaceAll("^\\Q"+ standardizeFilename(testTree.getAbsolutePath()) + "\\E", ""))
            .sorted()
            .collect(Collectors.toList());
        assertEquals(expectedIgnoredFiles, ignored);
    }

    @Test
    void testIsIgnoredFile() throws IOException {
        GitIgnoreFileSet gitIgnoreFileSet = new GitIgnoreFileSet(testTree)
//            .setVerbose(true)
            .assumeQueriesIncludeProjectBaseDir();

        assertFalse(gitIgnoreFileSet.isEmpty(), "Unable to load any .gitignore files");

        LOG.info("LOADED: {}", gitIgnoreFileSet);

        List<String> ignore = new ArrayList<>();

        try (Stream<Path> projectFiles = Files.find(testTree.toPath(), 128, (filePath, fileAttr) -> fileAttr.isRegularFile())) {
            for (Path path : projectFiles.sorted().collect(Collectors.toList())) {
                Boolean ignoredFile = gitIgnoreFileSet.isIgnoredFile(path.toString());
                if (ignoredFile == null) {
                    LOG.info("Path {} : Keep (No match)", path);
                } else {
                    if (ignoredFile) {
                        LOG.error("Path {} : Ignore (Match) --------------- ", path);
                        ignore.add(path.toString());
                    } else {
                        LOG.warn("Path {} : Keep (Match) ++++++++++++++ ", path);
                    }
                }
            }
        }

        checkIgnoredList(ignore);
    }

    @Test
    void testIgnoreFile() throws IOException {
        GitIgnoreFileSet gitIgnoreFileSet = new GitIgnoreFileSet(testTree)
            .setVerbose(true)
            .assumeQueriesIncludeProjectBaseDir();

        assertFalse(gitIgnoreFileSet.isEmpty(), "Unable to load any .gitignore files");

        try (Stream<Path> projectFiles = Files.find(testTree.toPath(), 128, (filePath, fileAttr) -> fileAttr.isRegularFile())) {
            List<String> ignore = new ArrayList<>();

            for (Path path : projectFiles.sorted().collect(Collectors.toList())) {
                if (gitIgnoreFileSet.ignoreFile(path.toString())) {
                    LOG.info("Path {} : Ignore --------------- ", path);
                    ignore.add(path.toString());
                } else {
                    LOG.info("Path {} : Keep", path);
                }
            }

            checkIgnoredList(ignore);
        }
    }

    @Test
    void testRecursiveIncrementalLoading() {
        GitIgnoreFileSet gitIgnoreFileSet = new GitIgnoreFileSet(testTree, false);
        gitIgnoreFileSet.setVerbose(true);

        List<File> files = recursivelyFindFiles(gitIgnoreFileSet, testTree);

        // Check we got it right
        files
            .stream()
            .map(File::getPath)
            .map(GitIgnore::standardizeFilename) // Needed for testing on Windows.
            .map(f -> f.replace(testTree.getPath(), ""))
            .forEach(file -> assertFalse(expectedIgnoredFiles.contains(file)));
    }

    private List<File> recursivelyFindFiles(GitIgnoreFileSet gitIgnoreFileSet, File directory) {
        if (directory == null) {
            return Collections.emptyList();
        }

        // Load the new gitignore files (if any).
        File[] newGitIgnoreFiles = directory.listFiles(pathname -> ".gitignore".equals(pathname.getName()));
        if (newGitIgnoreFiles != null) {
            Arrays
                .asList(newGitIgnoreFiles)
                .forEach(gitIgnoreFileSet::addGitIgnoreFile);
        }

        // Only keep the non ignored files
        File[] normalFiles = directory.listFiles(gitIgnoreFileSet); // << Using it as a FileFilter

        if (normalFiles == null) {
            return Collections.emptyList();
        }

        List<File> result = new ArrayList<>();
        for (File file : normalFiles) {
            if (file.isFile()) {
                if (!gitIgnoreFileSet.ignoreFile(file.getPath())) {
                    result.add(file);
                }
            }

            if (file.isDirectory()) {
                if (!gitIgnoreFileSet.ignoreFile(file.getPath())) {
                    result.addAll(recursivelyFindFiles(gitIgnoreFileSet, file));
                }
            }
        }
        return result;
    }

    @Test
    void testFileFilter() throws IOException {
        GitIgnoreFileSet gitIgnoreFileSet = new GitIgnoreFileSet(testTree).assumeQueriesIncludeProjectBaseDir();

        try (Stream<Path> projectFiles = Files.find(testTree.toPath(), 128, (filePath, fileAttr) -> fileAttr.isRegularFile() && gitIgnoreFileSet.ignoreFile(filePath.toString()))) {
            List<String> ignored = projectFiles
                .map(Path::toString)
                .sorted()
                .collect(Collectors.toList());
            checkIgnoredList(ignored);
        }
    }

    @Test
    void testWindowsPaths() {
        GitIgnoreFileSet gitIgnoreFileSet = new GitIgnoreFileSet(new File(separatorsToWindows(testTree.getPath())), false).assumeQueriesAreProjectRelative();

        assertFalse(gitIgnoreFileSet.isAssumeQueriesIncludeProjectBaseDir());
        assertTrue(gitIgnoreFileSet.isAssumeQueriesAreProjectRelative());

        gitIgnoreFileSet.add(new GitIgnore(separatorsToWindows("/"),      "*.txt"));
        gitIgnoreFileSet.add(new GitIgnore(separatorsToWindows("/dir1/"), "*.md"));
        gitIgnoreFileSet.add(new GitIgnore(separatorsToWindows("/dir2/"), "!foo.txt"));

        assertFalse(gitIgnoreFileSet.isEmpty());

        assertTrue(gitIgnoreFileSet.ignoreFile("\\foo.txt"));
        assertTrue(gitIgnoreFileSet.ignoreFile("\\dir1\\foo.txt"));
        assertFalse(gitIgnoreFileSet.ignoreFile("\\dir2\\foo.txt"));

        assertFalse(gitIgnoreFileSet.ignoreFile("\\foo.md"));
        assertTrue(gitIgnoreFileSet.ignoreFile("\\dir1\\foo.md"));
        assertFalse(gitIgnoreFileSet.ignoreFile("\\dir2\\foo.md"));
    }

    @Test
    void testWindowsPathsWithDriveLetter() {
        GitIgnoreFileSet gitIgnoreFileSet = new GitIgnoreFileSet(new File("A:\\MyProject\\src\\test\\resources\\"), false).assumeQueriesIncludeProjectBaseDir();

        assertTrue(gitIgnoreFileSet.isAssumeQueriesIncludeProjectBaseDir());
        assertFalse(gitIgnoreFileSet.isAssumeQueriesAreProjectRelative());

        gitIgnoreFileSet.add(new GitIgnore("\\",       "*.txt"));
        gitIgnoreFileSet.add(new GitIgnore("\\dir1\\", "*.md"));
        gitIgnoreFileSet.add(new GitIgnore("\\dir2\\", "!foo.txt"));
        gitIgnoreFileSet.setVerbose(true);

        assertFalse(gitIgnoreFileSet.isEmpty());

        // Correctly handle absolute paths (assume)
        gitIgnoreFileSet.assumeQueriesIncludeProjectBaseDir();
        assertTrue(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\foo.txt"));
        assertTrue(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\dir1\\foo.txt"));
        assertFalse(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\dir2\\foo.txt"));

        assertFalse(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\foo.md"));
        assertTrue(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\dir1\\foo.md"));
        assertFalse(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\dir2\\foo.md"));

        // Correctly handle absolute paths (explicit)
        gitIgnoreFileSet.assumeQueriesAreProjectRelative();
        assertTrue(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\foo.txt", false));
        assertTrue(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\dir1\\foo.txt", false));
        assertFalse(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\dir2\\foo.txt", false));

        assertFalse(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\foo.md", false));
        assertTrue(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\dir1\\foo.md", false));
        assertFalse(gitIgnoreFileSet.ignoreFile("A:\\MyProject\\src\\test\\resources\\dir2\\foo.md", false));

        // Correctly handle project relative paths (assume)
        gitIgnoreFileSet.assumeQueriesAreProjectRelative();
        assertTrue(gitIgnoreFileSet.ignoreFile("\\foo.txt"));
        assertTrue(gitIgnoreFileSet.ignoreFile("\\dir1\\foo.txt"));
        assertFalse(gitIgnoreFileSet.ignoreFile("\\dir2\\foo.txt"));

        assertFalse(gitIgnoreFileSet.ignoreFile("\\foo.md"));
        assertTrue(gitIgnoreFileSet.ignoreFile("\\dir1\\foo.md"));
        assertFalse(gitIgnoreFileSet.ignoreFile("\\dir2\\foo.md"));

        // Correctly handle project relative paths (explicit)
        gitIgnoreFileSet.assumeQueriesIncludeProjectBaseDir();
        assertTrue(gitIgnoreFileSet.ignoreFile("\\foo.txt", true));
        assertTrue(gitIgnoreFileSet.ignoreFile("\\dir1\\foo.txt", true));
        assertFalse(gitIgnoreFileSet.ignoreFile("\\dir2\\foo.txt", true));

        assertFalse(gitIgnoreFileSet.ignoreFile("\\foo.md", true));
        assertTrue(gitIgnoreFileSet.ignoreFile("\\dir1\\foo.md", true));
        assertFalse(gitIgnoreFileSet.ignoreFile("\\dir2\\foo.md", true));
    }

    @Test
    void testErrorHandlingNosuchDirectory() {
        GitIgnoreFileSet gitIgnoreFileSet = new GitIgnoreFileSet(new File("/no-such-file-really"));
        assertTrue(gitIgnoreFileSet.isEmpty());
    }

    @Test
    void testErrorHandlingNosuchFile() {
        GitIgnoreFileSet gitIgnoreFileSet = new GitIgnoreFileSet(new File("src/test/resources/"), false);
        gitIgnoreFileSet.addGitIgnoreFile(new File("src/test/resources/no-such-file-really"));
        assertTrue(gitIgnoreFileSet.isEmpty());
    }

}
