/*
 * CodeOwners Tools
 * Copyright (C) 2023-2026 Niels Basjes
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

import org.junit.jupiter.api.Assertions.assertInstanceOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

internal class TestCodeOwnersGetters {
    @Test
    fun verifyVerbose() {
        val codeOwners = CodeOwners("docs/")
        codeOwners.verbose = true
        assertTrue(codeOwners.verbose)
        codeOwners.verbose = false
        assertFalse(codeOwners.verbose)

        val section = Section("foo")
        section.verbose = true
        assertTrue(section.verbose)
        section.verbose = false
        assertFalse(section.verbose)
    }

    @Test
    fun verifyGetters() {
        val codeOwners = CodeOwners(
            "[One][11] @docs-team\n" +
            "docs/\n" +
            "*.md\n" +
            "\n" +
            "[Two][22] @database-team\n" +
            "model/db/\n" +
            "config/db/database-setup.md @docs-team\n" +
            "\n" +
            "[Three]\n" +
            "three1/ \n" +
            "\n" +
            "^[Four]\n" +  // Optional
            "four/\n" +
            "\n" +
            "[  tHrEe  ]\n" +
            "three2/ @docs-team\n" +
            "\n"
        )

        val allDefinedSections: Set<Section> = codeOwners.allDefinedSections
        assertEquals(4, allDefinedSections.size)
        var rule0: Rule
        var rule1: Rule
        for (section in allDefinedSections) {
            val rules: MutableList<Rule>
            when (section.name) {
                "One" -> {
                    assertFalse(section.isOptional)
                    assertEquals(11, section.minimalNumberOfApprovers)
                    assertEquals(1, section.defaultApprovers.size)
                    assertEquals("@docs-team", section.defaultApprovers[0])

                    rules = section.rules
                    assertEquals(2, rules.size)

                    rule0 = rules[0]
                    assertEquals("docs/", rule0.fileExpression)
                    assertEquals("(/.*)?/docs/", rule0.filePattern.pattern)
                    assertInstanceOf(ApprovalRule::class.java, rule0)
                    assertEquals(mutableListOf(), (rule0 as ApprovalRule).approvers)

                    rule1 = rules[1]
                    assertEquals("*.md", rule1.fileExpression)
                    assertEquals(".*\\.md(/|$)", rule1.filePattern.pattern)
                    assertEquals(mutableListOf(), (rule1 as ApprovalRule).approvers)
                }

                "Two" -> {
                    assertFalse(section.isOptional)
                    assertEquals(22, section.minimalNumberOfApprovers)
                    assertEquals(1, section.defaultApprovers.size)
                    assertEquals("@database-team", section.defaultApprovers[0])

                    rules = section.rules
                    assertEquals(2, rules.size)

                    rule0 = rules[0]
                    assertEquals("model/db/", rule0.fileExpression)
                    assertEquals("(/.*)?/model/db/", rule0.filePattern.pattern)
                    assertEquals(mutableListOf(), (rule0 as ApprovalRule).approvers)

                    rule1 = rules[1]
                    assertEquals("config/db/database-setup.md", rule1.fileExpression)
                    assertEquals("(/.*)?/config/db/database-setup\\.md(/|$)", rule1.filePattern.pattern)
                    assertEquals(mutableListOf("@docs-team"), (rule1 as ApprovalRule).approvers)
                }

                "Three" -> {
                    assertFalse(section.isOptional)
                    assertEquals(0, section.minimalNumberOfApprovers)
                    assertEquals(0, section.defaultApprovers.size)

                    rules = section.rules
                    assertEquals(2, rules.size)

                    rule0 = rules[0]
                    assertEquals("three1/", rule0.fileExpression)
                    assertEquals("(/.*)?/three1/", rule0.filePattern.pattern)
                    assertEquals(mutableListOf(), (rule0 as ApprovalRule).approvers)

                    rule1 = rules[1]
                    assertEquals("three2/", rule1.fileExpression)
                    assertEquals("(/.*)?/three2/", rule1.filePattern.pattern)
                    assertEquals(mutableListOf("@docs-team"), (rule1 as ApprovalRule).approvers)
                }

                "Four" -> {
                    assertTrue(section.isOptional)
                    assertEquals(0, section.minimalNumberOfApprovers)
                    assertEquals(0, section.defaultApprovers.size)

                    rules = section.rules
                    assertEquals(1, rules.size)

                    rule0 = rules[0]
                    assertEquals("four/", rule0.fileExpression)
                    assertEquals("(/.*)?/four/", rule0.filePattern.pattern)
                    assertEquals(mutableListOf(), (rule0 as ApprovalRule).approvers)
                }

                else -> fail("The section name ${section.name} is not supported")
            }
        }
    }
}
