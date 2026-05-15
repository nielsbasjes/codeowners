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
package nl.basjes.codeowners.validator.utils

import nl.basjes.codeowners.validator.utils.Problem.Fatal
import org.slf4j.Logger
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors

class ProblemTable : StringTable() {

    init {
        withHeaders("Section", "Expression", "Approver", "Problem")
    }
    private val theProblems: MutableList<Problem> = mutableListOf()
    val problems: List<Problem>
        get() = theProblems

    fun hasFatalErrors(): Boolean {
        return this.numberOfFatalErrors > 0
    }

    fun hasErrors(): Boolean {
        return this.numberOfErrors > 0
    }

    fun hasWarnings(): Boolean {
        return this.numberOfWarnings > 0
    }

    val numberOfProblems: Int
        get() = theProblems.size

    val numberOfFatalErrors: Long
        get() = theProblems.stream().filter { problem: Problem? -> problem is Fatal }
            .count()

    val numberOfErrors: Long
        get() = theProblems.stream()
            .filter { problem: Problem? -> problem is Problem.Error }
            .count()

    val numberOfWarnings: Long
        get() = theProblems.stream()
            .filter { problem: Problem? -> problem is Problem.Warning }
            .count()

    fun contains(problem: Problem?): Boolean {
        return theProblems.contains(problem)
    }

    val isEmpty: Boolean
        get() = theProblems.isEmpty()

    private fun addLineInfo(sb: StringBuilder, line: String) {
        addLine(sb, LogColor.BLUE_BRIGHT, "INFO ", LogColor.RESET, line)
    }

    private fun addLineWarning(sb: StringBuilder, line: String) {
        addLine(sb, LogColor.YELLOW_BRIGHT, "WARN ", LogColor.YELLOW_BRIGHT, line)
    }

    private fun addLineError(sb: StringBuilder, line: String) {
        addLine(sb, LogColor.RED_BRIGHT, "ERROR", LogColor.RED_BRIGHT, line)
    }

    private fun addLineFatal(sb: StringBuilder, line: String) {
        addLine(
            sb,
            LogColor.BLACK_BACKGROUND_BRIGHT.toString() + LogColor.RED_BOLD,
            "FATAL",
            LogColor.BLACK_BACKGROUND_BRIGHT.toString() + LogColor.RED_BOLD,
            line
        )
    }

    private fun addLineUnknown(sb: StringBuilder, line: String) {
        addLine(sb, LogColor.BLUE_BOLD_BRIGHT, "?????", LogColor.BLUE_BOLD_BRIGHT, line)
    }

    private fun addLine(sb: StringBuilder, tagColor: Any?, tag: String, lineColor: Any?, line: String) {
        sb.append("[${tagColor}${tag}${LogColor.RESET}] : ${lineColor}${line}")
            .append(LogColor.RESET).append('\n')
    }

    override fun toString(): String {
        val buffer = StringBuilder("\n")
        addLineInfo(buffer, writeSeparator())
        addLineInfo(buffer, writeHeaders())
        addLineInfo(buffer, writeSeparator())
        for (problem in theProblems) {
            if (problem is Problem.Info) {
                addLineInfo(buffer, writeLine(problem))
                continue
            }
            if (problem is Problem.Warning) {
                addLineWarning(buffer, writeLine(problem))
                continue
            }
            if (problem is Problem.Error) {
                addLineError(buffer, writeLine(problem))
                continue
            }
            if (problem is Fatal) {
                addLineFatal(buffer, writeLine(problem))
                continue
            }
            if (problem != null) {
                addLineUnknown(buffer, writeLine(problem))
            }
        }
        addLineInfo(buffer, writeSeparator())

        return buffer.toString()
    }

    fun toLogAsBlock(logger: Logger) {
        if (hasFatalErrors() || hasErrors()) {
            logger.error(this.toString())
            return
        }
        if (hasWarnings()) {
            logger.warn(this.toString())
            return
        }
        logger.info(this.toString())
    }


    fun toLog(logger: Logger) {
        logger.info(writeSeparator())
        logger.info(writeHeaders())
        logger.info(writeSeparator())
        for (problem in theProblems) {
            when(problem) {
                is Problem.Info     -> logger.info(writeLine(problem))
                is Problem.Warning  -> logger.warn(writeLine(problem))
                is Problem.Error    -> logger.error(writeLine(problem))
                is Problem.Fatal    -> logger.error(writeLine(problem))
                else                -> logger.info(writeLine(problem))
            }

        }
        logger.info(writeSeparator())
    }

    private fun writeLine(problem: Problem): String {
        return writeLine(
            listOf(
                problem.getSection(),
                problem.getExpression(),
                problem.getApprover(),
                problem.getDescription(),
                problem.getMarker()
            )
        )
    }

    fun addProblem(problem: Problem) {
        theProblems.add(problem)
        addRow(
            problem.getSection(),
            problem.getExpression(),
            problem.getApprover(),
            problem.getDescription(),
            problem.getMarker()
        )
    }

    fun toProblemMessageGroupedString(): String {
        val problemMessageToUsersMap: MutableMap<String, MutableSet<String>?> =
            TreeMap<String, MutableSet<String>?>()
        theProblems.forEach(Consumer { problem ->
            problemMessageToUsersMap
                .computeIfAbsent(problem.getDescription()) { mutableSetOf() }!!
                .add(problem.getApprover())
        })
        val table = StringTable()
        table.withHeaders("Message", "Affected users")
        problemMessageToUsersMap.forEach { (message: String, users: MutableSet<String>?) ->
            table.addRow(
                message,
                users!!.stream().sorted().distinct().collect(Collectors.joining(", "))
            )
        }
        return "\n" + table
    }
}
