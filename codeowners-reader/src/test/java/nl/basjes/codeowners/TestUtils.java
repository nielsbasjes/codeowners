/*
 * CodeOwners Tools
 * Copyright (C) 2023 Niels Basjes
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtils {
    public static void assertOwners(CodeOwners codeOwners, String filename, String... expectedOwners) {
        assertOwnersInternal(codeOwners, filename, expectedOwners);
        assertOwnersInternal(codeOwners, windowsFileName(filename), expectedOwners);
    }

    private static void assertOwnersInternal(CodeOwners codeOwners, String filename, String... expectedOwners) {
        List<String> allApprovers = codeOwners.getAllApprovers(filename);
        try {
            assertEquals(
                Arrays.stream(expectedOwners).sorted().collect(Collectors.toList()),
                allApprovers,
                "Filename \"" + filename + "\" should have owners " + Arrays.toString(expectedOwners) + " but got " + allApprovers);
        } catch (AssertionFailedError afe) {
            codeOwners.setVerbose(true);
            codeOwners.getAllApprovers(filename);
            codeOwners.setVerbose(false);
            throw afe;
        }
    }

    public static void assertMandatoryOwners(CodeOwners codeOwners, String filename, String... expectedOwners) {
        assertMandatoryOwnersInternal(codeOwners, filename, expectedOwners);
        assertMandatoryOwnersInternal(codeOwners, windowsFileName(filename), expectedOwners);
    }

    private static void assertMandatoryOwnersInternal(CodeOwners codeOwners, String filename, String... expectedOwners) {
        List<String> mandatoryApprovers = codeOwners.getMandatoryApprovers(filename);
        try {
            assertEquals(
                Arrays.stream(expectedOwners).sorted().collect(Collectors.toList()),
                mandatoryApprovers,
                "Filename \"" + filename + "\" should have mandatory owners " + Arrays.toString(expectedOwners) + " but got " + mandatoryApprovers);
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
