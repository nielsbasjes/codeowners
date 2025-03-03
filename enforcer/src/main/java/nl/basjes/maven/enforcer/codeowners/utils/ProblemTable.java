/*
 * Yet Another UserAgent Analyzer
 * Copyright (C) 2013-2025 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.basjes.maven.enforcer.codeowners.utils;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ProblemTable extends StringTable{
    private final List<Problem> problems = new ArrayList<>();

    public ProblemTable() {
        super();
        withHeaders("Section", "Expression", "Approver", "Problem");
    }

    public boolean hasErrors() {
        return getNumberOfErrors() > 0;
    }

    public boolean hasWarnings() {
        return getNumberOfWarnings() > 0;
    }

    public int getNumberOfProblems() {
        return problems.size();
    }

    public long getNumberOfErrors() {
        return problems.stream().filter(problem -> problem instanceof Problem.Error).count();
    }

    public long getNumberOfWarnings() {
        return problems.stream().filter(problem -> problem instanceof Problem.Warning).count();
    }

    public boolean isEmpty() {
        return problems.isEmpty();
    }


    public void toLog(EnforcerLogger logger) {
        logger.info(writeSeparator());
        logger.info(writeHeaders());
        logger.info(writeSeparator());
        for (Problem problem : problems) {
            if (problem instanceof Problem.Info) {
                logger.info(writeLine(problem));
                continue;
            }
            if (problem instanceof Problem.Warning) {
                logger.warn(writeLine(problem));
                continue;
            }
            if (problem instanceof Problem.Error) {
                logger.error(writeLine(problem));
                continue;
            }
            if (problem != null) {
                logger.info(writeLine(problem));
            }
        }
        logger.warn(writeSeparator());
    }

    private String writeLine(Problem problem) {
        return writeLine(problem.getFields());
    }

    public void addProblem(Problem problem) {
        problems.add(problem);
        addRow(problem.getFields());
    }
}
