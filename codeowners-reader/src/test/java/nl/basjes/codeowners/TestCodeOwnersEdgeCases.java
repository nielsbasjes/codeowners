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
