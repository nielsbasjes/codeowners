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

package nl.basjes.codeowners;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TestCodeOwnersGetters {

    @Test
    void verifyGetters() {
        CodeOwners codeOwners = new CodeOwners(
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
            "^[Four]\n" + // Optional
            "four/\n" +
            "\n" +
            "[  tHrEe  ]\n" +
            "three2/ @docs-team\n" +
            "\n"
        );

        Set<Section> allDefinedSections = codeOwners.getAllDefinedSections();
        assertEquals(4, allDefinedSections.size());
        Rule rule0;
        Rule rule1;
        for (Section section : allDefinedSections) {
            List<Rule> rules;
            switch (section.getName()) {
                case "One":
                    assertFalse(section.isOptional());
                    assertEquals(11, section.getMinimalNumberOfApprovers());
                    assertEquals(1, section.getDefaultApprovers().size());
                    assertEquals("@docs-team", section.getDefaultApprovers().get(0));

                    rules = section.getRules();
                    assertEquals(2, rules.size());

                    rule0 = rules.get(0);
                    assertEquals("docs/",                                       rule0.getFileExpression());
                    assertEquals("(/.*)?/docs/",                                rule0.getFilePattern().pattern());
                    assertInstanceOf(ApprovalRule.class, rule0);
                    assertEquals(Collections.emptyList(),                       ((ApprovalRule) rule0).getApprovers());

                    rule1 = rules.get(1);
                    assertEquals("*.md",                                        rule1.getFileExpression());
                    assertEquals(".*\\.md(/|$)",                                rule1.getFilePattern().pattern());
                    assertEquals(Collections.emptyList(),                       ((ApprovalRule) rule1).getApprovers());
                    break;

                case "Two":
                    assertFalse(section.isOptional());
                    assertEquals(22, section.getMinimalNumberOfApprovers());
                    assertEquals(1, section.getDefaultApprovers().size());
                    assertEquals("@database-team", section.getDefaultApprovers().get(0));

                    rules = section.getRules();
                    assertEquals(2, rules.size());

                    rule0 = rules.get(0);
                    assertEquals("model/db/",                                   rule0.getFileExpression());
                    assertEquals("(/.*)?/model/db/",                            rule0.getFilePattern().pattern());
                    assertEquals(Collections.emptyList(),                       ((ApprovalRule)rule0).getApprovers());

                    rule1 = rules.get(1);
                    assertEquals("config/db/database-setup.md",                 rule1.getFileExpression());
                    assertEquals("(/.*)?/config/db/database-setup\\.md(/|$)",   rule1.getFilePattern().pattern());
                    assertEquals(Collections.singletonList("@docs-team"),       ((ApprovalRule)rule1).getApprovers());
                    break;

                case "Three":
                    assertFalse(section.isOptional());
                    assertEquals(0, section.getMinimalNumberOfApprovers());
                    assertEquals(0, section.getDefaultApprovers().size());

                    rules = section.getRules();
                    assertEquals(2, rules.size());

                    rule0 = rules.get(0);
                    assertEquals("three1/",                                     rule0.getFileExpression());
                    assertEquals("(/.*)?/three1/",                              rule0.getFilePattern().pattern());
                    assertEquals(Collections.emptyList(),                       ((ApprovalRule)rule0).getApprovers());

                    rule1 = rules.get(1);
                    assertEquals("three2/",                                     rule1.getFileExpression());
                    assertEquals("(/.*)?/three2/",                              rule1.getFilePattern().pattern());
                    assertEquals(Collections.singletonList("@docs-team"),       ((ApprovalRule)rule1).getApprovers());
                    break;

                case "Four":
                    assertTrue(section.isOptional());
                    assertEquals(0, section.getMinimalNumberOfApprovers());
                    assertEquals(0, section.getDefaultApprovers().size());

                    rules = section.getRules();
                    assertEquals(1, rules.size());

                    rule0 = rules.get(0);
                    assertEquals("four/",                                       rule0.getFileExpression());
                    assertEquals("(/.*)?/four/",                                rule0.getFilePattern().pattern());
                    assertEquals(Collections.emptyList(),                       ((ApprovalRule)rule0).getApprovers());
                    break;

                default:
                    fail("The section name " + section.getName() + " is not supported");
            }
        }
    }

}
