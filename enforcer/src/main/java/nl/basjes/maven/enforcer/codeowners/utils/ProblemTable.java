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

public class ProblemTable {
    private final List<String> headers = Arrays.asList("Section", "Expression", "Approver", "Problem");
    private final List<Problem> problems = new ArrayList<>();

    public boolean hasErrors() {
        return problems.stream().anyMatch(problem -> problem instanceof Problem.Error);
    }

    public int getNumberOfProblems() {
        return problems.size();
    }

    public long getNumberOfErrors() {
        return problems.stream().filter(problem -> problem instanceof Problem.Error).count();
    }

    public boolean isEmpty() {
        return problems.isEmpty();
    }

    private List<Integer> calculateColumnWidths() {
        List<Integer> columnWidths = new ArrayList<>();
        for (int column = 0; column < headers.size(); column++) {
            int maxWidth = headers.get(column).length();
            for (Problem problem : problems) {
                String columnValue = problem.getFields().get(column);
                if (columnValue != null) {
                    maxWidth = Math.max(maxWidth, columnValue.length());
                }
            }
            columnWidths.add(maxWidth);
        }
        return columnWidths;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(1024);
        List<Integer> columnWidths = calculateColumnWidths();
        sb.append(writeSeparator(columnWidths)).append("\n");
        sb.append(writeLine(columnWidths, headers)).append("\n");
        sb.append(writeSeparator(columnWidths)).append("\n");
        for (Problem problem : problems) {
            sb.append(writeLine(columnWidths, problem)).append("\n");
        }
        sb.append(writeSeparator(columnWidths)).append("\n");
        return sb.toString();
    }

    public void toLog(EnforcerLogger logger) {
        List<Integer> columnWidths = calculateColumnWidths();
        logger.info(writeSeparator(columnWidths));
        logger.info(writeLine(columnWidths, headers));
        logger.info(writeSeparator(columnWidths));
        for (Problem problem : problems) {
            if (problem instanceof Problem.Info) {
                logger.info(writeLine(columnWidths, problem));
                continue;
            }
            if (problem instanceof Problem.Warning) {
                logger.warn(writeLine(columnWidths, problem));
                continue;
            }
            if (problem instanceof Problem.Error) {
                logger.error(writeLine(columnWidths, problem));
                continue;
            }
            if (problem != null) {
                logger.info(writeLine(columnWidths, problem));
            }
        }
        logger.warn(writeSeparator(columnWidths));
    }

    private String writeSeparator(List<Integer> columnWidths) {
        StringBuilder sb = new StringBuilder(512);
        boolean first = true;
        for (Integer columnWidth : columnWidths) {
            if (first) {
                sb.append('|');
                first = false;
            } else {
                sb.append('+');
            }
            sb.append("-".repeat(columnWidth+2));
        }
        sb.append('|');
        return sb.toString();
    }

    private String writeLine(List<Integer> columnWidths, Problem problem) {
        return writeLine(columnWidths, problem.getFields());
    }

    private String writeLine(List<Integer> columnWidths, List<String> fields) {
        if (fields.isEmpty()) {
            return writeSeparator(columnWidths);
        }

        int columns = Math.max(columnWidths.size(), fields.size());

        StringBuilder sb = new StringBuilder(512);
        for (int columnNr = 0; columnNr < columns; columnNr++) {
            int columnWidth = 1;
            if (columnNr < columnWidths.size()) {
                columnWidth = columnWidths.get(columnNr);
            }
            if (columnNr <= columnWidths.size()) {
                sb.append('|');
            }
            String field = "";
            if (columnNr < fields.size()) {
                field = fields.get(columnNr);
            }
            sb.append(String.format(" %-" + columnWidth + "s ", field)); // NOSONAR java:S3457 This is creative, I know.
        }
        if (columns <= columnWidths.size()) {
            sb.append('|');
        }
        return sb.toString();
    }

    public void addProblem(Problem problem) {
        this.problems.add(problem);
    }
}
