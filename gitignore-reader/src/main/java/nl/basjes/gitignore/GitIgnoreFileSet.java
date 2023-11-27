/*
 * CodeOwners Tools
 * Copyright (C) 2023 Niels Basjes
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.io.FilenameUtils.separatorsToUnix;

/**
 * A class that holds a set of .gitignore files and also handles the interactions between them.
 */
public class GitIgnoreFileSet implements FileFilter {

    private static final Logger LOG = LoggerFactory.getLogger(GitIgnoreFileSet.class);

    // The TreeMap is used to sort the GitIgnore files by the name of the basedir they are relevant for.
    // This sorting is very important in the evaluation of the rules!
    private final TreeMap<String, GitIgnore> gitIgnores = new TreeMap<>();

    // The absolute directory which is to be used as the project root for all the gitignore files.
    private final File projectBaseDir;

    /**
     * @param projectBaseDir The base directory of the project.
     *                       The root gitignore rules are relative to this directory.
     *                       This will automatically find and load all gitignore files.
     */
    public GitIgnoreFileSet(final File projectBaseDir) {
        this(projectBaseDir, true);
    }

    /**
     * @param projectBaseDir The base directory of the project. The root gitignore rules are relative to this directory.
     * @param autoload Automatically find and load all gitignore files?
     */
    public GitIgnoreFileSet(final File projectBaseDir, boolean autoload) {
        this.projectBaseDir = projectBaseDir;
        if (autoload) {
            addAllGitIgnoreFiles();
        }
    }

    public void setVerbose(boolean verbose) {
        gitIgnores.values().forEach(gitIgnore -> gitIgnore.setVerbose(verbose));
    }

    public void add(final GitIgnore gitIgnore) {
        gitIgnores.put(gitIgnore.getProjectRelativeBaseDir(), gitIgnore);
    }

    public void addGitIgnoreFile(final File gitIgnoreFile) {
        try {
            add(new GitIgnore(getProjectRelative(gitIgnoreFile.getParent()), gitIgnoreFile));
        } catch (IOException e) {
            LOG.error("Cannot read {} due to {}. Will skip this file.", gitIgnoreFile, e.getMessage());
        }
    }

    public void addAllGitIgnoreFiles() {
        // Find all files in the project
        try(Stream<Path> projectFiles = Files.find(projectBaseDir.toPath(), 128, (filePath, fileAttr) -> fileAttr.isRegularFile())) {
            projectFiles
                // Only the .gitignore files
                .filter(filePath -> filePath.getFileName().toString().equals(".gitignore"))
                // Then parse each of them and add the expressions.
                .forEach(gitIgnoreFile -> addGitIgnoreFile(gitIgnoreFile.toFile()));
        }
        catch (IOException e) {
            LOG.error("Unable to find .gitignore files in {} due to {}", projectBaseDir, e.toString());
        }
    }

    public boolean isEmpty() {
        return gitIgnores.isEmpty();
    }

    /**
     * Checks if the file matches the stored expressions.
     * This is suitable for combining multiple sets of rules!
     * @param filename The filename to be checked
     * @return NULL: not matched, True: must be ignored, False: it must be UNignored
     */
    public Boolean isIgnoredFile(String filename) {
        Boolean result = null;
        String projectBaseFileName = getProjectRelative(filename);

        // Iterate over all available GitIgnore files in the correct order!
        for (GitIgnore gitIgnore : gitIgnores.values()) {
            Boolean isIgnoredFile = gitIgnore.isIgnoredFile(projectBaseFileName);
            if (isIgnoredFile != null) {
                result = isIgnoredFile;
            }
        }
        return result;
    }

    /**
     * Checks if the file matches the stored expressions.
     * This is NOT suitable for combining multiple sets of rules!
     * @param filename The filename to be checked
     * @return true: must be ignored, false: it must be not be ignored
     */
    public boolean ignoreFile(String filename) {
        return TRUE.equals(isIgnoredFile(filename));
    }

    /**
     * Checks if the file matches the stored expressions.
     * This is NOT suitable for combining multiple sets of rules!
     * @param filename The filename to be checked
     * @return true: must be kept, false: it must be ignored
     */
    public boolean keepFile(String filename) {
        return !ignoreFile(filename);
    }

    private String getProjectRelative(String fileName) {
        return separatorsToUnix(fileName)
            .replaceAll("^\\Q" + projectBaseDir + "\\E", "/")
            .replace("//", "/");
    }

    @Override
    public boolean accept(File pathname) {
        return keepFile(pathname.getName());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GitIgnoreFileSet\n");
        for (Map.Entry<String, GitIgnore> gitIgnoreEntry : gitIgnores.entrySet()) {
            sb.append("# Base directory: ").append(gitIgnoreEntry.getKey()).append("\n")
              .append(gitIgnoreEntry.getValue())
              .append("=========================\n");
        }

        return sb.toString();
    }
}
