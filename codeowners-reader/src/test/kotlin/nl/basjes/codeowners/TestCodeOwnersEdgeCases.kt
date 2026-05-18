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

class TestCodeOwnersEdgeCases {
    @Test
    fun testDuplicatesSamePattern() {
        val codeOwners = CodeOwners(
            """
            packages/client/docker/ @team-a
            packages/client/docker/ @team-b
            """.trimIndent()
        )
        TestUtils.assertOwners(codeOwners, "/packages/client/docker/something.txt", "@team-b")
        TestUtils.assertOwners(codeOwners, "/somewhere/packages/client/docker/something.txt", "@team-b")
    }

    @Test
    fun testDuplicatesDifferentPattern() {
        val codeOwners = CodeOwners(
            """
            # Global
            *                                 @tech

            # Excludes
            !Data/

            # Teams
            /Engine/                          @tech/core
            /App/                             @tech/core

            # Individuals
            /Engine/Something/                @person

            # Critical
            /ThirdParty/Attributions/         @cto # CTO must approve
            """.trimIndent()
        )
        TestUtils.assertOwners(codeOwners, "/ThirdParty/Attributions/Licenses/SOME_FILE.txt", "@cto")
    }
}
