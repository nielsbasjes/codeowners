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

import org.opentest4j.AssertionFailedError
import kotlin.test.assertEquals

object TestUtils {
    fun assertOwners(codeOwners: CodeOwners, filename: String, vararg expectedOwners: String) {
        assertOwnersInternal(codeOwners, filename, true, *expectedOwners)
        assertOwnersInternal(codeOwners, windowsFileName(filename), true, *expectedOwners)
    }

    fun assertOwnersCheckOrdering(codeOwners: CodeOwners, filename: String, vararg expectedOwners: String) {
        assertOwnersInternal(codeOwners, filename, false, *expectedOwners)
        assertOwnersInternal(codeOwners, windowsFileName(filename), false, *expectedOwners)
    }

    private fun assertOwnersInternal(
        codeOwners: CodeOwners,
        filename: String,
        anyOrderIsValid: Boolean,
        vararg expectedApproversParam: String
    ) {
        var expectedApprovers = listOf(*expectedApproversParam)
        var actualApprovers = codeOwners.getAllApprovers(filename)
        if (anyOrderIsValid) {
            expectedApprovers = expectedApprovers.sorted()
            actualApprovers   = actualApprovers.sorted()
        }
        try {
            assertEquals(
                expectedApprovers,
                actualApprovers,
                "Filename \"$filename\" should have approvers $expectedApprovers but got $actualApprovers"
            )
        } catch (afe: AssertionFailedError) {
            codeOwners.verbose = true
            codeOwners.getAllApprovers(filename)
            codeOwners.verbose = false
            throw afe
        }
    }

    fun assertMandatoryOwners(codeOwners: CodeOwners, filename: String, vararg expectedOwners: String) {
        assertMandatoryOwnersInternal(codeOwners, filename, true, *expectedOwners)
        assertMandatoryOwnersInternal(codeOwners, windowsFileName(filename), true, *expectedOwners)
    }

    fun assertMandatoryOwnersCheckOrdering(codeOwners: CodeOwners, filename: String, vararg expectedOwners: String) {
        assertMandatoryOwnersInternal(codeOwners, filename, false, *expectedOwners)
        assertMandatoryOwnersInternal(codeOwners, windowsFileName(filename), false, *expectedOwners)
    }


    private fun assertMandatoryOwnersInternal(
        codeOwners: CodeOwners,
        filename: String,
        anyOrderIsValid: Boolean,
        vararg expectedApproversParam: String
    ) {
        var expectedApprovers = mutableListOf(*expectedApproversParam)
        var actualApprovers: MutableList<String> = codeOwners.getMandatoryApprovers(filename).toMutableList()
        if (anyOrderIsValid) {
            expectedApprovers = expectedApprovers.sorted().toMutableList()
            actualApprovers = actualApprovers.sorted().toMutableList()
        }
        try {
            assertEquals(
                expectedApprovers,
                actualApprovers,
                "Filename \"$filename\" should have mandatory approvers $expectedApprovers but got $actualApprovers"
            )
        } catch (afe: AssertionFailedError) {
            codeOwners.verbose = true
            codeOwners.getMandatoryApprovers(filename)
            codeOwners.verbose = false
            throw afe
        }
    }

    fun windowsFileName(filename: String): String {
        return filename.replace("/", "\\")
    }
}
