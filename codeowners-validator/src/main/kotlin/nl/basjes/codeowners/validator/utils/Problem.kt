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

import java.util.*

open class Problem @JvmOverloads constructor(
    private val section: String,
    private val expression: String,
    private val approver: String,
    private val description: String,
    private val marker: String = "\uD83E\uDD26"
) {
    fun getSection() = section
    fun getExpression() = expression
    fun getApprover() = approver
    fun getDescription() = description
    fun getMarker() = marker

    class Info(section: String, expression: String, approver: String, description: String) :
        Problem(section, expression, approver, description, "")

    class Warning(section: String, expression: String, approver: String, description: String) :
        Problem(section, expression, approver, description, "⚠️")

    class Error(section: String, expression: String, approver: String, description: String) :
        Problem(section, expression, approver, description, "❌")

    class Fatal(section: String, expression: String, approver: String, description: String) :
        Problem(section, expression, approver, description, "⛔\uFE0F")

    override fun toString(): String {
        return "Problem " + this.javaClass.simpleName +
                "(Section='" + section + '\'' +
                " Expression='" + expression + '\'' +
                " Approver='" + approver + '\'' +
                " Description='" + description + '\'' +
                ')'
    }

    override fun equals(o: Any?): Boolean {
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val problem = o as Problem
        return section == problem.section &&
                expression == problem.expression &&
                approver == problem.approver &&
                description == problem.description
    }

    override fun hashCode(): Int {
        return Objects.hash(section, expression, approver, description)
    }
}
