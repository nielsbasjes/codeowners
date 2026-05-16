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
import nl.basjes.codeowners.validator.utils.Problem.ProblemColor.COLOR_ERROR_TAG
import nl.basjes.codeowners.validator.utils.Problem.ProblemColor.COLOR_ERROR_TEXT
import nl.basjes.codeowners.validator.utils.Problem.ProblemColor.COLOR_FATAL_TAG
import nl.basjes.codeowners.validator.utils.Problem.ProblemColor.COLOR_FATAL_TEXT
import nl.basjes.codeowners.validator.utils.Problem.ProblemColor.COLOR_INFO_TAG
import nl.basjes.codeowners.validator.utils.Problem.ProblemColor.COLOR_INFO_TEXT
import nl.basjes.codeowners.validator.utils.Problem.ProblemColor.COLOR_WARNING_TAG
import nl.basjes.codeowners.validator.utils.Problem.ProblemColor.COLOR_WARNING_TEXT
import java.util.*

sealed class Problem private constructor(
    val name: String,
    val tag: Tags,
    val section: String,
    val expression: String,
    val approver: String,
    val description: String,
    val marker: String = "\uD83E\uDD26",
    val messageColor: ProblemColor,
) {

    enum class Tags(val text: String, val color: ProblemColor) {
        INFO_TAG    ("INFO ", COLOR_INFO_TAG),
        WARNING_TAG ("WARN ", COLOR_WARNING_TAG),
        ERROR_TAG   ("ERROR", COLOR_ERROR_TAG),
        FATAL_TAG   ("FATAL", COLOR_FATAL_TAG);
        override fun toString(): String {
            return "[${color}${text}${RESET}]"
        }
    }

    enum class ProblemColor(val code: String) {
        // Specific colors for problems
        COLOR_INFO_TAG    (LogColor.BLUE_BRIGHT.toString()),
        COLOR_INFO_TEXT     (LogColor.RESET.toString()),
        COLOR_WARNING_TAG    (LogColor.YELLOW_BRIGHT.toString()),
        COLOR_WARNING_TEXT  (LogColor.YELLOW_BRIGHT.toString()),
        COLOR_ERROR_TAG   (LogColor.RED_BRIGHT.toString()),
        COLOR_ERROR_TEXT    (LogColor.RED_BRIGHT.toString()),
        COLOR_FATAL_TAG   (LogColor.BLACK_BACKGROUND_BRIGHT.toString() + LogColor.RED_BOLD.toString()),
        COLOR_FATAL_TEXT    (LogColor.BLACK_BACKGROUND_BRIGHT.toString() + LogColor.RED_BOLD.toString());

        override fun toString(): String {
            return code
        }

    }

    class Info(section: String, expression: String, approver: String, description: String) :
        Problem("Info", Tags.INFO_TAG, section, expression, approver, description, "", COLOR_INFO_TEXT)

    class Warning(section: String, expression: String, approver: String, description: String) :
        Problem("Warning", Tags.WARNING_TAG, section, expression, approver, description, "⚠️",  COLOR_WARNING_TEXT)

    class Error(section: String, expression: String, approver: String, description: String) :
        Problem("Error", Tags.ERROR_TAG, section, expression, approver, description, "❌",  COLOR_ERROR_TEXT)

    class Fatal(section: String, expression: String, approver: String, description: String) :
        Problem("Fatal", Tags.FATAL_TAG, section, expression, approver, description, "⛔\uFE0F", COLOR_FATAL_TEXT)

    override fun toString(): String {
        return "Problem $name(" +
                "Section='$section' " +
                "Expression='$expression' " +
                "Approver='$approver' " +
                "Description='$description')"
    }

    override fun equals(o: Any?): Boolean {
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val problem = o as Problem
        return section     == problem.section &&
               expression  == problem.expression &&
               approver    == problem.approver &&
               description == problem.description
    }

    override fun hashCode(): Int {
        return Objects.hash(section, expression, approver, description)
    }
}
