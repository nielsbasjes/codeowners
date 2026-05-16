/*
 * CodeOwners Tools
 * Copyright (C) 2023-2026 Niels Basjes
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

import nl.basjes.codeowners.validator.utils.LogColor.RESET
import nl.basjes.codeowners.validator.utils.Problem.Error
import nl.basjes.codeowners.validator.utils.Problem.Fatal
import nl.basjes.codeowners.validator.utils.Problem.Info
import nl.basjes.codeowners.validator.utils.Problem.ProblemColor.COLOR_INFO_TAG
import nl.basjes.codeowners.validator.utils.Problem.ProblemColor.COLOR_INFO_TEXT
import nl.basjes.codeowners.validator.utils.Problem.Tags.INFO_TAG
import nl.basjes.codeowners.validator.utils.Problem.Warning
import org.slf4j.Logger
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
        get() = theProblems.stream()
            .filter { it is Fatal }
            .count()

    val numberOfErrors: Long
        get() = theProblems.stream()
            .filter { it is Error }
            .count()

    val numberOfWarnings: Long
        get() = theProblems.stream()
            .filter { it is Warning }
            .count()

    fun contains(problem: Problem?): Boolean {
        return theProblems.contains(problem)
    }

    val isEmpty: Boolean
        get() = theProblems.isEmpty()

    override fun toString(): String {
        clearCaches();
        val buffer = StringBuilder("\n")
        fun addLineInfo(line: String) {
            buffer.append("$INFO_TAG : ${COLOR_INFO_TEXT}${line}${RESET}\n")
        }

        addLineInfo(writeSeparator())
        addLineInfo(writeHeaders())
        addLineInfo(writeSeparator())
        for (problem in theProblems) {
            buffer.append("${problem.tag} : ${writeLine(problem)}\n")
        }
        addLineInfo( writeSeparator())

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
        clearCaches();
        logger.info(writeSeparator())
        logger.info(writeHeaders())
        logger.info(writeSeparator())
        for (problem in theProblems) {
            when(problem) {
                is Info     -> logger.info(writeLine(problem))
                is Warning  -> logger.warn(writeLine(problem))
                is Error    -> logger.error(writeLine(problem))
                is Fatal    -> logger.error(writeLine(problem))
            }
        }
        logger.info(writeSeparator())
    }

    private fun writeLine(problem: Problem) =
        "${problem.messageColor}${writeLine(
            listOf(
                problem.section,
                problem.expression,
                problem.approver,
                problem.description,
                problem.marker,
            )
            )}${RESET}"

    fun addProblem(problem: Problem) {
        theProblems.add(problem)
        addRow(
            problem.section,
            problem.expression,
            problem.approver,
            problem.description,
            problem.marker
        )
    }

    fun toProblemMessageGroupedString(): String {
        val problemMessageToUsersMap: MutableMap<String, MutableSet<String>> = mutableMapOf()
        theProblems.forEach {
            problemMessageToUsersMap
                .computeIfAbsent(it.description) { mutableSetOf() }
                .add(it.approver)
        }
        val table = StringTable()
        table.withHeaders("Message", "Affected users")
        problemMessageToUsersMap
            .toSortedMap()
            .forEach { (message, users) ->
            table.addRow(
                message,
                users.stream().sorted().distinct().collect(Collectors.joining(", "))
            )
        }
        return "\n" + table
    }
}
