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
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    /**
     * Simple param. This rule fails if the value is true.
     */
    private File codeOwnersFile;

    /**
     * Simple param. This rule fails if the value is true.
     */
    private boolean allFilesMustHaveCodeOwner = false;

    /**
     * Rule parameter as list of items.
     */
    private List<String> listParameters;

    // Inject needed Maven components

    @Inject
    private MavenProject project;

    @Inject
    private MavenSession session;

    @Inject
    private RuntimeInformation runtimeInformation;

    public void execute() throws EnforcerRuleException {
        if (codeOwnersFile == null) {
            codeOwnersFile = new File("CODEOWNERS");
        }
        GitIgnore codeOwners;
        try {
            codeOwners = new GitIgnore(codeOwnersFile);
        } catch (IOException e) {
            throw new EnforcerRuleException("Unable to read the CODEOWNERS: " + codeOwnersFile, e);
        }

        getLog().info("CODEOWNER:\n"+codeOwners);

        List<String> allFilesInProject = null;

        try {
            String baseDir = project.getBasedir().getPath();
            // FIXME: Not sure how to do this right
            allFilesInProject = Files
                .find(Paths.get(baseDir), 999, (p, bfa) -> bfa.isRegularFile())
                .map(file -> file.toString().replace(baseDir.toString(),""))
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new EnforcerRuleException("Unable to list the files", e);
        }

        // TODO: Remove the .gitignore files

        boolean pass = true;
        if (allFilesMustHaveCodeOwner) {
            for (String path : allFilesInProject) {
                getLog().info("File: " + path + " --> " + codeOwners.getAllApprovers(path));
                if (codeOwners.getAllApprovers(path).isEmpty()) {
                    getLog().error("Missing approvers for: " + path);
                    pass = false;
                }
            }
            if (!pass) {
                throw new EnforcerRuleException("Not all files had an approver.");
            }
        }
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
            allFilesMustHaveCodeOwner,
            listParameters));
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
        return String.format("MyCustomRule[allFilesMustHaveCodeOwner=%b]", allFilesMustHaveCodeOwner);
    }
}

