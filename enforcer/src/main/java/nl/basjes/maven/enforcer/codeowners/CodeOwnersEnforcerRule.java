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

import nl.basjes.codeowners.CodeOwners;
import nl.basjes.gitignore.GitIgnore;
import nl.basjes.gitignore.GitIgnoreFileSet;
import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static nl.basjes.gitignore.GitIgnore.standardizeFilename;
import static nl.basjes.gitignore.Utils.findAllNonIgnored;

@SuppressWarnings("unused") // Used by the enforcer-plugin that finds it via the @Named annotation
@Named("codeOwners") // rule name - must start from lowercase character
public class CodeOwnersEnforcerRule extends AbstractEnforcerRule {

    private File baseDir;

    private File codeOwnersFile;

    private boolean allFilesMustHaveCodeOwner = false;

    private boolean allExisingFilesMustHaveCodeOwner = false;

    private boolean allNewlyCreatedFilesMustHaveCodeOwner = false;

    private boolean verbose = false;

    private boolean showApprovers = false;

    private String unlikelyFilename = "NewlyCreated_NiElSbAsJeSwRoTeThIs";

    @Inject
    private MavenProject project;

    public void execute() throws EnforcerRuleException {
        if (baseDir == null) {
            baseDir = project.getBasedir();
        }
        getLog().debug("BaseDir=|"+baseDir+"|");

        // Get the ignore rules
        GitIgnoreFileSet gitIgnores = loadAllGitIgnoreFiles(baseDir);

        // Get the codeowners
        CodeOwners codeOwners = loadCodeOwners(baseDir, codeOwnersFile);

        if (verbose) {
            getLog().info("=================================\n");
            getLog().info("All configured GitIgnore rules:\n" + gitIgnores);
            getLog().info("=================================\n");
            getLog().info("All configured CODEOWNER rules:\n" + codeOwners);
            getLog().info("=================================\n");
        }

        // Get a list of all files in the project and sort them
        Path baseDirPath = baseDir.toPath();
        List<Path> allNonIgnoredFilesAndDirectoriesInProject =
            findAllNonIgnored(gitIgnores)
                .stream()
                .map(baseDirPath::relativize)
                .sorted()
                .collect(Collectors.toList());

        // Because all files have been forced to be project relative we must change the gitIgnores matching.
        gitIgnores.assumeQueriesAreProjectRelative();

        if (showApprovers) {
            // Run this listing without the verbose set to keep the output usable
            codeOwners.setVerbose(false);
            gitIgnores.setVerbose(false);
            printApprovers(allNonIgnoredFilesAndDirectoriesInProject, codeOwners);
        }

        // Set everything to the requested verbosity
        codeOwners.setVerbose(verbose);
        gitIgnores.setVerbose(verbose);

        if (allFilesMustHaveCodeOwner || allExisingFilesMustHaveCodeOwner) {
            List<String> allNonIgnoredFilesInProject = allNonIgnoredFilesAndDirectoriesInProject
                .stream()
                .filter(path -> path.toFile().isFile())
                .map(Path::toString)
                .collect(Collectors.toList());

            allNonIgnoredFilesHaveApprovers(allNonIgnoredFilesInProject, codeOwners);
        }

        if (allFilesMustHaveCodeOwner || allNewlyCreatedFilesMustHaveCodeOwner) {
            List<String> newFileForEveryDirectory = allNonIgnoredFilesAndDirectoriesInProject
                .stream()
                .filter(path -> path.toFile().isDirectory())
                .map(directoryName -> (directoryName + "/" + unlikelyFilename).replace("//", "/"))
                .filter(gitIgnores::keepFile)
                .collect(Collectors.toList());

            allNonIgnoredFilesHaveApprovers(newFileForEveryDirectory, codeOwners);
        }
    }

    // ------------------------------------------

    GitIgnoreFileSet loadAllGitIgnoreFiles(File baseDir) {
        // Get the files that are ignored by the SCM
        GitIgnoreFileSet gitIgnores = new GitIgnoreFileSet(this.baseDir, false)
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
            getLog().info("Using GitIgnore : " + pathToLoggingString(baseDir.toPath().relativize(loadedFile)));
        }

        return gitIgnores;
    }

    // ------------------------------------------

    CodeOwners loadCodeOwners(File baseDir, File codeOwnersFile) throws EnforcerRuleException {
        List<String> commonCodeOwnersFiles = Arrays.asList(
            "/CODEOWNERS",
            "/.github/CODEOWNERS",
            "/.gitlab/CODEOWNERS",
            "/docs/CODEOWNERS"
        );

        if (this.codeOwnersFile == null) {
            // If no file was specified we try the default locations
            for (String codeOwnersFileName : commonCodeOwnersFiles) {
                File tryingFile = new File(baseDir + codeOwnersFileName);
                if (tryingFile.exists() && tryingFile.isFile()) {
                    this.codeOwnersFile = tryingFile;
                    break;
                }
            }
        }

        if (this.codeOwnersFile == null) {
            throw new EnforcerRuleException("This project does NOT have a CODEOWNERS file");
        }

        getLog().info("Using CODEOWNERS: " + pathToLoggingString(baseDir.toPath().relativize(this.codeOwnersFile.toPath())));
        try {
            CodeOwners codeOwners = new CodeOwners(this.codeOwnersFile);
            getLog().debug(codeOwners.toString());
            return codeOwners;
        } catch (IOException e) {
            throw new EnforcerRuleException("Unable to read the CODEOWNERS: " + this.codeOwnersFile, e);
        }
    }

    // ------------------------------------------

    void allNonIgnoredFilesHaveApprovers(List<String> filenames, CodeOwners codeOwners) throws EnforcerRuleException {
        boolean pass = true;
        List<String> filesWithoutApprover = new ArrayList<>();

        for (String filename : filenames) {
            getLog().debug("Checking: ${baseDir}/" + filename);
            List<String> approvers = codeOwners.getMandatoryApprovers(filename);
            getLog().debug("- Approvers: " + approvers);
            if (approvers.isEmpty()) {
                getLog().error("No approvers for " + pathToLoggingString(filename));
                filesWithoutApprover.add(filename);
                pass = false;
            }
        }
        if (!pass) {
            throw new EnforcerRuleException("Not all files had an approver: \n--> " +
                String.join("\n--> ", filesWithoutApprover));
        }
    }

    // ------------------------------------------

    void printApprovers(List<Path> paths, CodeOwners codeOwners) {
        getLog().info("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
        getLog().info("Listing all Mandatory Approvers:");
        getLog().info("--------------------------------");
        for (Path path : paths) {
            List<String> mandatoryApprovers = codeOwners.getMandatoryApprovers(path.toString());
            getLog().info("Approvers for: " + pathToLoggingString(path) + " : " + mandatoryApprovers);
        }
        getLog().info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
    }

    // ------------------------------------------

    private String pathToLoggingString(String path) {
        String filename = standardizeFilename(path);
        if (filename.startsWith("/")) {
            return "${baseDir}" + filename;
        }
        return filename;
    }

    // ------------------------------------------

    private String pathToLoggingString(Path path) {
        String filename = standardizeFilename(path.toString()) + (path.toFile().isDirectory()?"/":"");
        if (filename.startsWith("/")) {
            return "${baseDir}" + filename;
        }
        return filename;
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
        return String.valueOf(Objects.hash(
            codeOwnersFile,
            allFilesMustHaveCodeOwner));
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

