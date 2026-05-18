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

import kotlin.test.Test

internal class TestCodeOwnersOrdering {
    @Test
    fun ensureMaintainingOrdering() {
        val codeOwners = CodeOwners(
            """
            [README Owners]
            README.md @user5 @user2 @user5

            ^[README other owners]
            README.md @user3

            [README default] @user2 @user1 @user2
            *.md
            SomethingElse.md @user3

            [README Owners]
            internal/README.md @user4
            """.trimIndent()
        )

        TestUtils.assertOwnersCheckOrdering(
            codeOwners,
            "README.md", "@user5", "@user2", "@user3", "@user1"
        )
        TestUtils.assertOwnersCheckOrdering(
            codeOwners,  // Note that the user4 at the front is correct
            "internal/README.md", "@user4", "@user3", "@user2", "@user1"
        )

        // Mandatory owners means we skip the optional sections --> No more user3 for these testcases
        TestUtils.assertMandatoryOwnersCheckOrdering(
            codeOwners,
            "README.md", "@user5", "@user2", "@user1"
        )
        TestUtils.assertMandatoryOwnersCheckOrdering(
            codeOwners,  // Note that the user4 at the front is correct
            "internal/README.md", "@user4", "@user2", "@user1"
        )
    }

    @Test
    fun ensureMaintainingOrderingOptionalDeduplicateSwitch() {
        val codeOwners = CodeOwners(
            """
            [README Owners]
            README.md @user1

            ^[README other owners]
            README.md @user3

            [README default] @user2 @user3
            *.md
            """.trimIndent()
        )

        TestUtils.assertOwnersCheckOrdering(
            codeOwners,
            "README.md", "@user1", "@user3", "@user2"
        )

        // Mandatory owners means we skip the optional sections
        // Because the user3 is no longer present in the optional section and
        // it is present in a later section it will have changed place in the final result list.
        TestUtils.assertMandatoryOwnersCheckOrdering(
            codeOwners,
            "README.md", "@user1", "@user2", "@user3"
        )
    }
}
