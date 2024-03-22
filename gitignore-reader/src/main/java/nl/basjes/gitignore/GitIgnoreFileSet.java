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
import static nl.basjes.gitignore.GitIgnore.standardizeFilename;

/**
 * A class that holds a set of .gitignore files and also handles the interactions between them.
 */
public class GitIgnoreFileSet implements FileFilter {

    private static final Logger LOG = LoggerFactory.getLogger(GitIgnoreFileSet.class);

    // The TreeMap is used to sort the GitIgnore files by the name of the basedir they are relevant for.
    // This sorting is very important in the evaluation of the rules!
    private final TreeMap<String, GitIgnore> gitIgnores = new TreeMap<>();

    // The "absolute" directory which is to be used as the project root for all the gitignore files.
    private final File projectBaseDir;

    // Do we assume a query to be project relative if not explicitly stated?
    private boolean assumeProjectRelativeQueries = false;

    /**
     * This will automatically find and load all gitignore files.
     * @param projectBaseDir The base directory of the project.
     *                       The root gitignore rules are relative to this directory.
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

    /**
     * Make all currently loaded gitignore files become verbose when used.
     * @param verbose True: be verbose, False: be silent
     */
    public GitIgnoreFileSet setVerbose(boolean verbose) {
        gitIgnores.values().forEach(gitIgnore -> gitIgnore.setVerbose(verbose));
        return this;
    }

    /**
     * Sets the default assumption on how to interpret the filenames that must be checked.
     * @param assumeProjectRelativeQueries The new assumption: True = all queries are assumed to be project relative;  False = all queries are assumed to start with the path to the project directory in the SAME form as used during construction of this class.
     */
    public GitIgnoreFileSet setAssumeProjectRelativeQueries(boolean assumeProjectRelativeQueries) {
        this.assumeProjectRelativeQueries = assumeProjectRelativeQueries;
        return this;
    }

    /**
     * Sets the default assumption on how to interpret the filenames that must be checked to "all queries are assumed to be project relative"
     */
    public GitIgnoreFileSet assumeQueriesAreProjectRelative() {
        return setAssumeProjectRelativeQueries(true);
    }
    public boolean isAssumeQueriesAreProjectRelative() {
        return assumeProjectRelativeQueries;
    }

    /**
     * Sets the default assumption on how to interpret the filenames that must be checked to "all queries are assumed to start with the path to the project directory"
     */
    public GitIgnoreFileSet assumeQueriesIncludeProjectBaseDir() {
        return setAssumeProjectRelativeQueries(false);
    }
    public boolean isAssumeQueriesIncludeProjectBaseDir() {
        return !assumeProjectRelativeQueries;
    }

    public void add(final GitIgnore gitIgnore) {
        gitIgnores.put(gitIgnore.getProjectRelativeBaseDir(), gitIgnore);
    }

    /**
     * Add a .gitignore file to the set.
     * @param gitIgnoreFile The handle of the file of a gitignore file to be added. This MUST be able to get the ABSOLUTE path within the project.
     */
    public void addGitIgnoreFile(final File gitIgnoreFile) {
        try {
            add(new GitIgnore(gitIgnoreFile.getParent(), gitIgnoreFile));
        } catch (IOException e) {
            LOG.error("Cannot read {} due to {}. Will skip this file.", gitIgnoreFile, e.getMessage());
        }
    }

    /**
     * Automatically find all .gitignore files starting in the projects root and add them all to the set.
     */
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
     * @param filename The filename to be checked which follows the assumeProjectRelativeQueries flag.
     * @return NULL: not matched, True: must be ignored, False: it must be UNignored
     */
    public Boolean isIgnoredFile(String filename) {
        return isIgnoredFile(filename, assumeProjectRelativeQueries);
    }

    /**
     * Checks if the file matches the stored expressions.
     * This is suitable for combining multiple sets of rules!
     * @param filename The filename to be checked.
     * @param isRelative True: The provided filename is a RELATIVE path (i.e. the project base directory is assumed to be the root).
     *                   False:  The provided filename is an ABSOLUTE path (i.e. it still includes the project base directory).
     * @return NULL: not matched, True: must be ignored, False: it must be UNignored
     */
    public Boolean isIgnoredFile(String filename, boolean isRelative) {
        Boolean result = null;
        String projectBaseFileName = isRelative ? filename : getProjectRelative(filename);

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
     * @param filename The filename to be checked which follows the assumeProjectRelativeQueries flag.
     * @return true: must be ignored, false: it must be not be ignored
     */
    public boolean ignoreFile(String filename) {
        return ignoreFile(filename, assumeProjectRelativeQueries);
    }

    /**
     * Checks if the file matches the stored expressions.
     * This is NOT suitable for combining multiple sets of rules!
     * @param filename The filename to be checked.
     * @param isProjectRelative True: The provided filename is a project RELATIVE path (i.e. "/directory in project/filename").
     *                          False: The provided filename is a path that starts with the path used to initialize this class (i.e. "project base directory/directory in project/filename").
     * @return true: must be ignored, false: it must be not be ignored
     */
    public boolean ignoreFile(String filename, boolean isProjectRelative) {
        return TRUE.equals(isIgnoredFile(filename, isProjectRelative));
    }

    /**
     * Checks if the file matches the stored expressions.
     * This is NOT suitable for combining multiple sets of rules!
     * @param filename The filename to be checked which follows the assumeProjectRelativeQueries flag.
     * @return true: must be kept, false: it must be ignored
     */
    public boolean keepFile(String filename) {
        return keepFile(filename, assumeProjectRelativeQueries);
    }

    /**
     * Checks if the file matches the stored expressions.
     * This is NOT suitable for combining multiple sets of rules!
     * @param filename The filename to be checked.
     * @param isProjectRelative True: The provided filename is a project RELATIVE path (i.e. "/directory in project/filename").
     *                   False:  The provided filename is a path that starts with the path used to initialize this class (i.e. "project base directory/directory in project/filename").
     * @return true: must be kept, false: it must be ignored
     */
    public boolean keepFile(String filename, boolean isProjectRelative) {
        return !ignoreFile(filename, isProjectRelative);
    }

    private String getProjectRelative(String fileName) {
        String standardizedFilename       = standardizeFilename(fileName);
        String projectBaseDirPath         = standardizeFilename(projectBaseDir.getPath());
        if (standardizedFilename.startsWith(projectBaseDirPath)) {
            return standardizeFilename(fileName)
                .replaceAll("^\\Q" + projectBaseDirPath + "\\E", "/")
                .replace("//", "/");
        }

        String projectBaseDirAbsolutePath = standardizeFilename(projectBaseDir.getAbsolutePath());
        if (standardizedFilename.startsWith(projectBaseDirAbsolutePath)) {
            return standardizeFilename(fileName)
                .replaceAll("^\\Q" + projectBaseDirAbsolutePath + "\\E", "/")
                .replace("//", "/");
        }

        throw new IllegalArgumentException("The requested file \""+standardizedFilename+"\" is not relative to project root and is NOT in the projectBaseDir \""+projectBaseDirPath+"\"");
    }

    @Override
    public boolean accept(File pathname) {
        return keepFile(pathname.getName(), true);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GitIgnoreFileSet\n");
        sb.append("# Project base directory: ").append(projectBaseDir)
            .append(" ( Absolute: ").append(projectBaseDir.getAbsoluteFile()).append(" )\n")
            .append("=========================\n");

        for (Map.Entry<String, GitIgnore> gitIgnoreEntry : gitIgnores.entrySet()) {
            sb.append(gitIgnoreEntry.getValue())
              .append("=========================\n");
        }

        return sb.toString();
    }

}
