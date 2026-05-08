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

import static nl.basjes.codeowners.TestUtils.assertOwners;

public class TestCodeOwnersEdgeCases {

    @Test
    // https://gitlab.com/gitlab-org/gitlab/-/work_items/585698
    void testDuplicatesSamePattern() {
        CodeOwners codeOwners = new CodeOwners(
            "packages/client/docker/ @team-a\n"+
            "packages/client/docker/ @team-b\n"
        );
        assertOwners(codeOwners, "/packages/client/docker/something.txt", "@team-b");
        assertOwners(codeOwners, "/somewhere/packages/client/docker/something.txt", "@team-b");
    }

    @Test
    void testDuplicatesDifferentPattern() {
        CodeOwners codeOwners = new CodeOwners(
            "# Global\n" +
            "*                                 @tech\n" +
            "\n" +
            "# Excludes\n" +
            "!Data/\n" +
            "\n" +
            "# Teams\n" +
            "/Engine/                          @tech/core\n" +
            "/App/                             @tech/core\n" +
            "\n" +
            "# Individuals\n" +
            "/Engine/Something/                @person\n" +
            "\n" +
            "# Critical\n" +
            "/ThirdParty/Attributions/         @cto # CTO must approve\n"
        );
        assertOwners(codeOwners, "/ThirdParty/Attributions/Licenses/SOME_FILE.txt", "@cto");
    }
}
