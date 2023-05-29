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

package nl.basjes.maven.enforcer.codeowners;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import nl.basjes.codeowners.CodeOwners;
import nl.basjes.gitignore.GitIgnore;
import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.rtinfo.RuntimeInformation;

/**
 * Custom Enforcer Rule - example
 */
@Named("codeOwners") // rule name - must start from lowercase character
public class CodeOwnersEnforcerRule extends AbstractEnforcerRule {

    private File codeOwnersFile;

    private boolean allFilesMustHaveCodeOwner = false;

    // Inject needed Maven components

    @Inject
    private MavenProject project;

    @Inject
    private MavenSession session;

    @Inject
    private RuntimeInformation runtimeInformation;

    public void execute() throws EnforcerRuleException {
        List<String> allFilesInProject = null;
        String baseDir = project.getBasedir().getPath();

        // Get a list of all files in the project
        try {
            Path baseDirPath = Paths.get(baseDir);
            try(Stream<Path> projectFiles = Files.find(baseDirPath, 128, (filePath, fileAttr) -> fileAttr.isRegularFile())) {
                allFilesInProject = projectFiles
                    .map(file -> file.toString().replace(baseDir,""))
                    .sorted()
                    .collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new EnforcerRuleException("Unable to list the files", e);
        }

        // Get the files that are ignored by the SCM
        List<GitIgnore> gitIgnores = new ArrayList<>();

        // Start with the internal files that are used by git.
        gitIgnores.add(new GitIgnore(
            "/.git/\n"
        ));

        List<Object> commonCodeOwnersFiles = Arrays.asList(
            "/CODEOWNERS",
            "/.github/CODEOWNERS",
            "/.gitlab/CODEOWNERS",
            "/docs/CODEOWNERS"
            );

        for (String projectFile : allFilesInProject) {
            if (projectFile.endsWith("/.gitignore")){
                try {
                    getLog().info("Using gitignore: " + projectFile);
                    gitIgnores.add(new GitIgnore(new File(baseDir + projectFile)));
                } catch (IOException e) {
                    throw new EnforcerRuleException("Unable to open the gitignore file: " + projectFile);
                }
            }
            if (codeOwnersFile == null) {
                if (commonCodeOwnersFiles.contains(projectFile)) {
                    getLog().info("Using CODEOWNERS: " + projectFile);
                    codeOwnersFile = new File(baseDir + projectFile);
                }
            }
        }

        if (codeOwnersFile == null) {
            throw new EnforcerRuleException("This project does NOT have a CODEOWNERS file");
        }

        CodeOwners codeOwners;
        try {
            codeOwners = new CodeOwners(codeOwnersFile);
        } catch (IOException e) {
            throw new EnforcerRuleException("Unable to read the CODEOWNERS: " + codeOwnersFile, e);
        }
        getLog().debug("CODEOWNER:\n"+codeOwners);

        boolean pass = true;
        for (String projectFile : allFilesInProject) {
            getLog().debug("Checking: " + projectFile);
            if (allFilesMustHaveCodeOwner) {
                if (ignore(gitIgnores, projectFile)) {
                    getLog().debug("- Ignored");
                } else {
                    getLog().debug("- Approvers: " + codeOwners.getAllApprovers(projectFile));
                    if (codeOwners.getAllApprovers(projectFile).isEmpty()) {
                        getLog().error("No approvers for " + projectFile);
                        pass = false;
                    }
                }
            }
            if (!pass) {
                throw new EnforcerRuleException("Not all files had an approver.");
            }
        }
    }

    boolean ignore(List<GitIgnore> gitIgnoreList, String filename) {
        Boolean ignore=null;
        for (GitIgnore gitIgnore : gitIgnoreList) {
            Boolean ignoreResult = gitIgnore.isIgnoredFile(filename);
            if (ignoreResult != null) {
                ignore = ignoreResult;
            }
        }
        return !(ignore == null || !ignore);
    }


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

