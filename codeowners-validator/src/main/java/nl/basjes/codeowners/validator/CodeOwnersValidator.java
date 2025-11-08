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

package nl.basjes.codeowners.validator;

import lombok.Setter;
import nl.basjes.codeowners.CodeOwners;
import nl.basjes.codeowners.validator.gitlab.GitlabConfiguration;
import nl.basjes.codeowners.validator.gitlab.GitlabProjectMembers;
import nl.basjes.codeowners.validator.utils.ProblemTable;
import nl.basjes.codeowners.validator.utils.StringTable;
import nl.basjes.gitignore.GitIgnore;
import nl.basjes.gitignore.GitIgnoreFileSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static nl.basjes.gitignore.GitIgnore.standardizeFilename;
import static nl.basjes.gitignore.Utils.findAllNonIgnored;

public class CodeOwnersValidator {
    @Setter
    private boolean verbose = false;
    private GitlabConfiguration gitlab = null;
    private static final String UNLIKELY_FILENAME = "NewlyCreated_NiElSbAsJeSwRoTeThIs";

    private Logger log = LoggerFactory.getLogger("CodeOwnersValidator"); // Default logger

    public enum FileType {
        FILE,
        DIRECTORY,
        NEWLY_CREATED_FILE,
    }

    public static class FileOwners {
        final Path path;
        final FileType fileType;
        final List<String> approvers;
        final List<String> mandatoryApprovers;

        public FileOwners(Path path, FileType fileType) {
            this.path = path;
            this.fileType = fileType;
            this.approvers = new ArrayList<>();
            this.mandatoryApprovers = new ArrayList<>();
        }

        public void addApprovers(List<String> approvers) {
            this.approvers.addAll(approvers);
        }
        public void addMandatoryApprovers(List<String> approvers) {
            this.mandatoryApprovers.addAll(approvers);
        }

        @Override
        public String toString() {
            return "FileOwners{" +
                "path=" + path +
                ", fileType=" + fileType +
                ", approvers=" + approvers +
                ", mandatoryApprovers=" + mandatoryApprovers +
                '}';
        }
    }

    public static class DirectoryOwners extends TreeMap<Path, FileOwners> {
        Path baseDir;
        // Get the ignore rules
        GitIgnoreFileSet gitIgnores;

        // Get the codeowners
        CodeOwners codeOwners;

        public DirectoryOwners(Path baseDir, GitIgnoreFileSet gitIgnores, CodeOwners codeOwners) {
            this.baseDir = baseDir;
            this.gitIgnores = gitIgnores;
            this.codeOwners = codeOwners;
        }

        /**
         * @return TRUE if all existing files and directories have at least 1 mandatory code owner, else FALSE
         */
        public boolean allExistingFilesHaveMandatoryCodeOwner() {
            for (FileOwners fileOwners : values() ) {
                if (fileOwners.fileType == FileType.FILE || fileOwners.fileType == FileType.NEWLY_CREATED_FILE) {
                    if (fileOwners.mandatoryApprovers.isEmpty()) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * @return TRUE if all existing files and directories have at least 1 mandatory or optional code owner, else FALSE
         */
        public boolean allExistingFilesHaveAnyCodeOwner() {
            for (FileOwners fileOwners : values() ) {
                if (fileOwners.fileType == FileType.FILE || fileOwners.fileType == FileType.NEWLY_CREATED_FILE) {
                    if (fileOwners.approvers.isEmpty()) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * @return TRUE if all newly created files and directories have at least 1 mandatory code owner, else FALSE
         */
        public boolean allNewlyCreatedFilesHaveMandatoryCodeOwner() {
            for (FileOwners fileOwners : values() ) {
                if (fileOwners.fileType == FileType.NEWLY_CREATED_FILE) {
                    if (fileOwners.mandatoryApprovers.isEmpty()) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * @return TRUE if all existing files and directories have at least 1 mandatory or optional code owner, else FALSE
         */
        public boolean allNewlyCreatedFilesHaveAnyCodeOwner() {
            for (FileOwners fileOwners : values() ) {
                if (fileOwners.fileType == FileType.NEWLY_CREATED_FILE) {
                    if (fileOwners.approvers.isEmpty()) {
                        return false;
                    }
                }
            }
            return true;
        }

        // ------------------------------------------

        public String toTable() {
            StringTable table = new StringTable();
            table.withHeaders("Path", "Mandatory Approvers");
            for (Path path : navigableKeySet()) {
                List<String> mandatoryApprovers = codeOwners.getMandatoryApprovers(path.toString());
                if (mandatoryApprovers.isEmpty()) {
                    table.addRow(pathToLoggingString(path), mandatoryApprovers.toString(), "<-- NO APPROVERS!");
                } else {
                    table.addRow(pathToLoggingString(path), mandatoryApprovers.toString());
                }
            }
            return table.toString();
        }

        // ------------------------------------------
        public String toJson() {
            StringBuilder result = new StringBuilder("[");
            boolean first = true;
            for (Path path: navigableKeySet()) {
                if (first) {
                    first = false;
                } else {
                    result.append(",");
                }
                List<String> mandatoryApprovers = codeOwners.getMandatoryApprovers(path.toString());
                String filename = standardizeFilename(path.toString()) + (path.toFile().isDirectory() ? "/" : "");
                filename = filename.replaceAll("/+", "/");
                result.append("{\"").append(filename).append("\":[\"").append(String.join("\",\"", mandatoryApprovers)).append("\"]}");
            }
            return result.append("]").toString();
        }
    }

    public CodeOwnersValidator(
        GitlabConfiguration gitlab,
        Logger logger,
        boolean verbose
    ) throws CodeOwnersValidationException {
        this.verbose = verbose;
        this.gitlab = gitlab;

        if (logger != null) {
            this.log = logger;
        }

        if (gitlab != null) {
            if (gitlab.isDefaultCIConfigRunningOutsideCI()) {
                log.warn("Found Gitlab CI config that only works within Gitlab CI.");
                log.warn("Skipping Gitlab Project Members check because this is not GitlabCI.");
                log.info("Found GitLab configuration:\n{}", gitlab);
                this.gitlab = null;
            } else {
                if (gitlab.isValid()) {
                    if (verbose) {
                        log.info("Using GitLab configuration:\n{}", gitlab);
                    }
                } else {
                    throw new CodeOwnersValidationException("The GitLab configuration is not valid:" + gitlab);
                }
            }
        }

    }

    public DirectoryOwners analyzeDirectory(
        File baseDir
    ) throws CodeOwnersValidationException {
        return analyzeDirectory(baseDir, null);
    }

    public DirectoryOwners analyzeDirectory(
        File baseDir,
        File codeOwnersFile
    ) throws CodeOwnersValidationException {
        if (baseDir == null || !baseDir.isDirectory()) {
            throw new CodeOwnersValidationException("The provided baseDir is invalid: " + baseDir);
        }
        Path baseDirPath = baseDir.toPath();

        if (verbose) {
            log.info("BaseDir=|{}|", baseDir);
        } else {
            log.debug("BaseDir=|{}|", baseDir);
        }

        // Get the ignore rules
        GitIgnoreFileSet gitIgnores = loadAllGitIgnoreFiles(baseDir);
        // Because all files will be forced to be project relative we must change the gitIgnores matching.
        gitIgnores.assumeQueriesAreProjectRelative();

        // Get the codeowners
        CodeOwners codeOwners = loadCodeOwners(baseDir, codeOwnersFile);

        if (verbose) {
            log.info("=================================");
            log.info("All configured GitIgnore rules:\n{}", gitIgnores);
            log.info("=================================");
            log.info("All configured CODEOWNER rules:\n{}", codeOwners);
            log.info("=================================");
        }

        DirectoryOwners result = new DirectoryOwners(baseDirPath, gitIgnores, codeOwners);

        // Get a list of all files in the project and sort them
        List<Path> allNonIgnoredFilesAndDirectoriesInProject =
            findAllNonIgnored(gitIgnores)
                .stream()
                .map(baseDirPath::relativize)
                .sorted()
                .distinct()
                .collect(Collectors.toList());


        // Set everything to the requested verbosity
        codeOwners.setVerbose(verbose);
        gitIgnores.setVerbose(verbose);

        for(Path path : allNonIgnoredFilesAndDirectoriesInProject) {
            File fullFile = new File(baseDir.getAbsoluteFile(), path.toString());
            if (fullFile.isFile()) {
                FileOwners fileOwners = new FileOwners(path, FileType.FILE);
                fileOwners.addApprovers(codeOwners.getAllApprovers(path.toString()));
                fileOwners.addMandatoryApprovers(codeOwners.getMandatoryApprovers(path.toString()));
                result.put(path, fileOwners);
                continue;
            }
            if (fullFile.isDirectory()) {
                FileOwners fileOwners = new FileOwners(path, FileType.DIRECTORY);
                fileOwners.addApprovers(codeOwners.getAllApprovers(path.toString()));
                fileOwners.addMandatoryApprovers(codeOwners.getMandatoryApprovers(path.toString()));
                result.put(path, fileOwners);

                Path newFile = Path.of( path.toString(), UNLIKELY_FILENAME);
                if (gitIgnores.keepFile(newFile.toString())) {
                    Path newFileIndex = Path.of( path.toString(), "*");
                    FileOwners newFileOwners = new FileOwners(newFileIndex, FileType.NEWLY_CREATED_FILE);
                    newFileOwners.addApprovers(codeOwners.getAllApprovers(newFile.toString()));
                    newFileOwners.addMandatoryApprovers(codeOwners.getMandatoryApprovers(newFile.toString()));
                    result.put(newFileIndex, newFileOwners);
                }
            }
        }

        if (gitlab != null) {
            try (GitlabProjectMembers gitlabProjectMembers = new GitlabProjectMembers(gitlab)) {
                ProblemTable problemTable = gitlabProjectMembers.verifyAllCodeowners(log, codeOwners);
                if (gitlab.isShowAllApprovers() || problemTable.hasWarnings() || problemTable.hasErrors() || problemTable.hasFatalErrors()) {
                    log.info(problemTable.toString());
                    log.info("============================");
                    log.info("Summary per problem message:");
                    log.info(problemTable.toProblemMessageGroupedString());
                }
                gitlabProjectMembers.failIfExceededFailLevel(problemTable);
            }
        }

        return result;
    }

    // ------------------------------------------

    GitIgnoreFileSet loadAllGitIgnoreFiles(File baseDir) {
        // Get the files that are ignored by the SCM
        GitIgnoreFileSet gitIgnores = new GitIgnoreFileSet(baseDir, false)
            .assumeQueriesIncludeProjectBaseDir();

        gitIgnores.setVerbose(verbose);

        // Start with the internal files that are used by common SCMs.
        gitIgnores.add(new GitIgnore(
            "/.git/\n" +
            "/.hg/\n" +
            ".svn/\n"
        ));

        // Load all available gitignore configs.
        List<Path> loadedFiles = gitIgnores.addAllGitIgnoreFiles();
        for (Path loadedFile : loadedFiles) {
            if (verbose) {
                log.info("Using GitIgnore : " + pathToLoggingString(baseDir.toPath().relativize(loadedFile)));
            }
        }

        return gitIgnores;
    }

    // ------------------------------------------

    CodeOwners loadCodeOwners(File baseDir, File codeOwnersFile) throws CodeOwnersValidationException {
        List<String> commonCodeOwnersFiles = Arrays.asList(
            "/CODEOWNERS",
            "/.github/CODEOWNERS",
            "/.gitlab/CODEOWNERS",
            "/docs/CODEOWNERS"
        );

        if (codeOwnersFile == null) {
            // If no file was specified we try the default locations
            for (String codeOwnersFileName : commonCodeOwnersFiles) {
                File tryingFile = new File(baseDir + codeOwnersFileName);
                if (tryingFile.exists() && tryingFile.isFile()) {
                    codeOwnersFile = tryingFile;
                    break;
                }
            }
        }

        if (codeOwnersFile == null) {
            throw new CodeOwnersValidationException("This project does NOT have a CODEOWNERS file");
        }

        if (verbose) {
            log.info("Using CODEOWNERS: " + pathToLoggingString(baseDir.toPath().relativize(codeOwnersFile.toPath())));
        }
        try {
            CodeOwners codeOwners = new CodeOwners(codeOwnersFile);
            log.debug(codeOwners.toString());
            return codeOwners;
        } catch (IOException e) {
            throw new CodeOwnersValidationException("Unable to read the CODEOWNERS: " + codeOwnersFile, e);
        }
    }

    // ------------------------------------------

    static String pathToLoggingString(Path path) {
        String filename = standardizeFilename(path.toString()) + (path.toFile().isDirectory() ? "/" : "");
        filename = filename.replaceAll("/+", "/");
        if (filename.startsWith("/")) {
            return "${baseDir}" + filename;
        }
        return filename;
    }


    // ------------------------------------------

}

