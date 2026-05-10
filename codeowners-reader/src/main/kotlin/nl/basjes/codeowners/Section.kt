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
package nl.basjes.codeowners

class Section(@JvmField val name: String) {
    fun getName() = name

    private var verbose = false
    var isOptional: Boolean = false

    var minimalNumberOfApprovers: Int = 0

    val defaultApprovers: MutableList<String> = mutableListOf()

    @JvmField val rules: MutableList<Rule> = mutableListOf()
    fun getRules() = rules

    fun addDefaultApprover(name: String) {
        val cleanedName = name.trim { it <= ' ' }
        if (!defaultApprovers.contains(cleanedName)) {
            defaultApprovers.add(cleanedName)
        }
    }

    fun addRule(rule: Rule) {
        rules.add(rule)
    }

    fun setVerbose(verbose: Boolean) {
        this.verbose = verbose
        rules.forEach { it.verbose = verbose }
    }

    val isDefaultSection: Boolean
        get() = CodeOwnersLoader.IMPLICIT_SECTION_NAME == name

    /**
     * @param filename The filename for which the approvers are requested.
     * @return The list of approver usernames for this filename in the order (as good as possible) as they appear in the code owner rules in this section.
     */
    fun getApprovers(filename: String): MutableList<String> {
        if (verbose) {
            LOG.info("# ---------------------------")
            LOG.info("# Section [{}]", this.name)
        }
        val approvers: MutableList<String> = mutableListOf()
        for (rule in rules) {
            if (!rule.matches(filename)) {
                continue
            }

            if (rule is ExcludeRule) {
                // Gitlab: After a pattern is excluded, it cannot be included again in the same section.
                approvers.clear()
                break
            }

            // Here: rule instanceof ApprovalRule
            val approvalRule = rule as ApprovalRule
            val ruleApprovers: MutableList<String> = approvalRule.approvers
            // GitHub: Order is important; the last matching pattern takes the most precedence.
            // Gitlab: When a file or directory matches multiple entries in the CODEOWNERS file, the users from last pattern matching the file or directory are used.
            approvers.clear()
            if (ruleApprovers.isEmpty()) {
                if (verbose) {
                    LOG.info("-- MATCH WITHOUT APPROVERS --> Using Default approvers {}", defaultApprovers)
                }
                approvers.addAll(defaultApprovers)
            } else {
                approvers.addAll(ruleApprovers)
            }
        }
        if (verbose) {
            LOG.info("# Section [{}] approvers: {}", this.name, approvers)
            LOG.info("# ---------------------------")
        }
        return approvers
    }

    override fun toString(): String {
        val result = StringBuilder()
        if (this.isOptional) {
            result.append('^')
        }
        result.append('[').append(name).append(']')
        if (minimalNumberOfApprovers > 0) {
            result.append('[').append(minimalNumberOfApprovers).append(']')
        }
        if (!defaultApprovers.isEmpty()) {
            result.append(' ').append(defaultApprovers.joinToString(separator = " "))
        }
        result.append('\n')
        for (rule in rules) {
            result.append(rule).append('\n')
        }
        return result.toString()
    }
}
