/*
 * CodeOwners Tools
 * Copyright (C) 2023-2024 Niels Basjes
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

import org.opentest4j.AssertionFailedError;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtils {
    public static void assertOwners(CodeOwners codeOwners, String filename, String... expectedOwners) {
        assertOwnersInternal(codeOwners, filename,                  true, expectedOwners);
        assertOwnersInternal(codeOwners, windowsFileName(filename), true, expectedOwners);
    }

    public static void assertOwnersCheckOrdering(CodeOwners codeOwners, String filename, String... expectedOwners) {
        assertOwnersInternal(codeOwners, filename,                  false, expectedOwners);
        assertOwnersInternal(codeOwners, windowsFileName(filename), false, expectedOwners);
    }

    private static void assertOwnersInternal(CodeOwners codeOwners, String filename, boolean anyOrderIsValid, String... expectedApproversParam) {
        List<String> expectedApprovers = Arrays.asList(expectedApproversParam);
        List<String> actualApprovers = codeOwners.getAllApprovers(filename);
        if (anyOrderIsValid) {
            expectedApprovers.sort(String::compareTo);
            actualApprovers.sort(String::compareTo);
        }
        try {
            assertEquals(
                expectedApprovers,
                actualApprovers,
                "Filename \"" + filename + "\" should have approvers " + expectedApprovers + " but got " + actualApprovers);
        } catch (AssertionFailedError afe) {
            codeOwners.setVerbose(true);
            codeOwners.getAllApprovers(filename);
            codeOwners.setVerbose(false);
            throw afe;
        }
    }

    public static void assertMandatoryOwners(CodeOwners codeOwners, String filename, String... expectedOwners) {
        assertMandatoryOwnersInternal(codeOwners, filename,                  true, expectedOwners);
        assertMandatoryOwnersInternal(codeOwners, windowsFileName(filename), true, expectedOwners);
    }
    public static void assertMandatoryOwnersCheckOrdering(CodeOwners codeOwners, String filename, String... expectedOwners) {
        assertMandatoryOwnersInternal(codeOwners, filename,                  false, expectedOwners);
        assertMandatoryOwnersInternal(codeOwners, windowsFileName(filename), false, expectedOwners);
    }


    private static void assertMandatoryOwnersInternal(CodeOwners codeOwners, String filename, boolean anyOrderIsValid, String... expectedApproversParam) {
        List<String> expectedApprovers = Arrays.asList(expectedApproversParam);
        List<String> actualApprovers = codeOwners.getMandatoryApprovers(filename);
        if (anyOrderIsValid) {
            expectedApprovers.sort(String::compareTo);
            actualApprovers.sort(String::compareTo);
        }
        try {
            assertEquals(
                expectedApprovers,
                actualApprovers,
                "Filename \"" + filename + "\" should have mandatory approvers " + expectedApprovers + " but got " + actualApprovers);
        } catch (AssertionFailedError afe) {
            codeOwners.setVerbose(true);
            codeOwners.getMandatoryApprovers(filename);
            codeOwners.setVerbose(false);
            throw afe;
        }
    }

    public static void assertOwners(String codeOwners, String filename, String... expectedOwners) {
        assertOwners(new CodeOwners(codeOwners), filename, expectedOwners);
    }

    public static String windowsFileName(String filename) {
        return filename.replace("/", "\\");
    }

}
