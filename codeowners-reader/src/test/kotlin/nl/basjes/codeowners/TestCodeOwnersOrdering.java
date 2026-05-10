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

import static nl.basjes.codeowners.TestUtils.assertMandatoryOwnersCheckOrdering;
import static nl.basjes.codeowners.TestUtils.assertOwnersCheckOrdering;

class TestCodeOwnersOrdering {

    @Test
    void ensureMaintainingOrdering() {
        CodeOwners codeOwners = new CodeOwners(
            "[README Owners]\n" +
            "README.md @user5 @user2 @user5\n" + // extra user5 to test deduplication
            "\n" +
            "^[README other owners]\n" + // Optional section
            "README.md @user3 \n" +
            "\n" +
            "[README default] @user2 @user1 @user2\n" + // extra user2 to test deduplication
            "*.md\n" +
            "SomethingElse.md @user3\n" +
            "\n" +
            "[README Owners]\n" +
            // Because this rule is ADDED to an existing section (i.e. earlier in the sections list)
            // the result will be earlier in the source sorted output.
            "internal/README.md @user4\n"
        );

        assertOwnersCheckOrdering(codeOwners,
            "README.md",           "@user5", "@user2", "@user3", "@user1");
        assertOwnersCheckOrdering(codeOwners,
                                    // Note that the user4 at the front is correct
            "internal/README.md",  "@user4", "@user3", "@user2", "@user1" );

        // Mandatory owners means we skip the optional sections --> No more user3 for these testcases
        assertMandatoryOwnersCheckOrdering(codeOwners,
            "README.md",           "@user5", "@user2", "@user1");
        assertMandatoryOwnersCheckOrdering(codeOwners,
                                   // Note that the user4 at the front is correct
            "internal/README.md",  "@user4", "@user2", "@user1" );
    }

    @Test
    void ensureMaintainingOrderingOptionalDeduplicateSwitch() {
        CodeOwners codeOwners = new CodeOwners(
            "[README Owners]\n" +
            "README.md @user1\n" +
            "\n" +
            "^[README other owners]\n" + // Optional section
            "README.md @user3 \n" +
            "\n" +
            "[README default] @user2 @user3\n" +
            "*.md\n" +
            "\n"
        );

        assertOwnersCheckOrdering(codeOwners,
            "README.md", "@user1", "@user3", "@user2");

        // Mandatory owners means we skip the optional sections
        // Because the user3 is no longer present in the optional section and
        // it is present in a later section it will have changed place in the final result list.
        assertMandatoryOwnersCheckOrdering(codeOwners,
            "README.md", "@user1", "@user2", "@user3");
    }
}
