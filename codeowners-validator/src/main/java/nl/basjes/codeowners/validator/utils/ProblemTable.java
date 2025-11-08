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
package nl.basjes.codeowners.validator.utils;

import lombok.Getter;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static nl.basjes.codeowners.validator.utils.LogColor.BLACK_BACKGROUND_BRIGHT;
import static nl.basjes.codeowners.validator.utils.LogColor.BLUE_BOLD_BRIGHT;
import static nl.basjes.codeowners.validator.utils.LogColor.BLUE_BRIGHT;
import static nl.basjes.codeowners.validator.utils.LogColor.RED_BOLD;
import static nl.basjes.codeowners.validator.utils.LogColor.RED_BRIGHT;
import static nl.basjes.codeowners.validator.utils.LogColor.RESET;
import static nl.basjes.codeowners.validator.utils.LogColor.YELLOW_BRIGHT;

@Getter
public final class ProblemTable extends StringTable{
    private final List<Problem> problems = new ArrayList<>();

    public ProblemTable() {
        super();
        withHeaders("Section", "Expression", "Approver", "Problem");
    }

    public boolean hasFatalErrors() {
        return getNumberOfFatalErrors() > 0;
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

    public long getNumberOfFatalErrors() {
        return problems.stream().filter(problem -> problem instanceof Problem.Fatal).count();
    }

    public long getNumberOfErrors() {
        return problems.stream().filter(problem -> problem instanceof Problem.Error).count();
    }

    public long getNumberOfWarnings() {
        return problems.stream().filter(problem -> problem instanceof Problem.Warning).count();
    }

    public boolean contains(Problem problem) {
        return problems.contains(problem);
    }

    public boolean isEmpty() {
        return problems.isEmpty();
    }

    private void addLineInfo(StringBuilder sb, String line)    { addLine(sb, BLUE_BRIGHT,      "INFO ", RESET,            line); }
    private void addLineWarning(StringBuilder sb, String line) { addLine(sb, YELLOW_BRIGHT,    "WARN ", YELLOW_BRIGHT,    line); }
    private void addLineError(StringBuilder sb, String line)   { addLine(sb, RED_BRIGHT,       "ERROR", RED_BRIGHT,       line); }
    private void addLineFatal(StringBuilder sb, String line)   { addLine(sb, BLACK_BACKGROUND_BRIGHT.toString() + RED_BOLD,  "FATAL", BLACK_BACKGROUND_BRIGHT.toString() + RED_BOLD,  line); }
    private void addLineUnknown(StringBuilder sb, String line) { addLine(sb, BLUE_BOLD_BRIGHT, "?????", BLUE_BOLD_BRIGHT, line); }

    private void addLine(StringBuilder sb, Object tagColor, String tag, Object lineColor, String line) {
        sb.append("[").append(tagColor).append(tag).append(RESET).append("] : ") .append(lineColor).append(line).append(RESET).append('\n');
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder("\n");
        addLineInfo(buffer, writeSeparator());
        addLineInfo(buffer, writeHeaders());
        addLineInfo(buffer, writeSeparator());
        for (Problem problem : problems) {
            if (problem instanceof Problem.Info) {
                addLineInfo(buffer, writeLine(problem));
                continue;
            }
            if (problem instanceof Problem.Warning) {
                addLineWarning(buffer, writeLine(problem));
                continue;
            }
            if (problem instanceof Problem.Error) {
                addLineError(buffer, writeLine(problem));
                continue;
            }
            if (problem instanceof Problem.Fatal) {
                addLineFatal(buffer, writeLine(problem));
                continue;
            }
            if (problem != null) {
                addLineUnknown(buffer, writeLine(problem));
            }
        }
        addLineInfo(buffer, writeSeparator());

        return buffer.toString();
    }

    public void toLogAsBlock(Logger logger) {
        if (hasFatalErrors() || hasErrors()) {
            logger.error(this.toString());
            return;
        }
        if (hasWarnings()) {
            logger.warn(this.toString());
            return;
        }
        logger.info(this.toString());
    }


    public void toLog(Logger logger) {
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
            if (problem instanceof Problem.Fatal) {
                logger.error(writeLine(problem));
                continue;
            }
            if (problem != null) {
                logger.info(writeLine(problem));
            }
        }
        logger.info(writeSeparator());
    }

    private String writeLine(Problem problem) {
        return writeLine(Arrays.asList(
            problem.getSection(),
            problem.getExpression(),
            problem.getApprover(),
            problem.getDescription(),
            problem.getMarker()
        ));
    }

    public void addProblem(Problem problem) {
        problems.add(problem);
        addRow(problem.getSection(),
            problem.getExpression(),
            problem.getApprover(),
            problem.getDescription(),
            problem.getMarker());
    }

    public String toProblemMessageGroupedString() {
        Map<String, Set<String>> problemMessageToUsersMap = new TreeMap<>();
        problems.forEach(problem -> problemMessageToUsersMap.computeIfAbsent(problem.getDescription(), s -> new HashSet<>()).add(problem.getApprover()));
        StringTable table = new StringTable();
        table.withHeaders("Message", "Affected users");
        problemMessageToUsersMap.forEach((message, users) -> table.addRow(message, users.stream().sorted().distinct().collect(Collectors.joining(", "))));
        return "\n"+table;
    }
}
