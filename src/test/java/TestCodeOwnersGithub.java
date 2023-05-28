import nl.basjes.maven.enforcer.codeowners.CodeOwners;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestCodeOwnersGithub {

    // https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners#codeowners-syntax
    private static final String GITHUB_CODEOWNERS_DOC_EXAMPLE =
        "# This is a comment.\n" +
        "# Each line is a file pattern followed by one or more owners.\n" +
        "\n" +
        "# These owners will be the default owners for everything in\n" +
        "# the repo. Unless a later match takes precedence,\n" +
        "# @global-owner1 and @global-owner2 will be requested for\n" +
        "# review when someone opens a pull request.\n" +
        "*       @global-owner1 @global-owner2\n" +
        "\n" +
        "# Order is important; the last matching pattern takes the most\n" +
        "# precedence. When someone opens a pull request that only\n" +
        "# modifies JS files, only @js-owner and not the global\n" +
        "# owner(s) will be requested for a review.\n" +
        "*.js    @js-owner #This is an inline comment.\n" +
        "\n" +
        "# You can also use email addresses if you prefer. They'll be\n" +
        "# used to look up users just like we do for commit author\n" +
        "# emails.\n" +
        "*.go docs@example.com\n" +
        "\n" +
        "# Teams can be specified as code owners as well. Teams should\n" +
        "# be identified in the format @org/team-name. Teams must have\n" +
        "# explicit write access to the repository. In this example,\n" +
        "# the octocats team in the octo-org organization owns all .txt files.\n" +
        "*.txt @octo-org/octocats\n" +
        "\n" +
        "# In this example, @doctocat owns any files in the build/output\n" +
        "# directory at the root of the repository and any of its\n" +
        "# subdirectories.\n" +
        // Changed /logs to /output for better testing
        "/build/output/ @doctocat\n" +
        "\n" +
        "# The `docs/*` pattern will match files like\n" +
        "# `docs/getting-started.md` but not further nested files like\n" +
        "# `docs/build-app/troubleshooting.md`.\n" +
        "docs/*  docs@example.com\n" +
        "\n" +
        "# In this example, @octocat owns any file in an apps directory\n" +
        "# anywhere in your repository.\n" +
        "apps/ @octocat\n" +
        "\n" +
        "# In this example, @doctocat owns any file in the `/docs`\n" +
        "# directory in the root of your repository and any of its\n" +
        "# subdirectories.\n" +
        "/docs/ @doctocat\n" +
        "\n" +
        "# In this example, any change inside the `/scripts` directory\n" +
        "# will require approval from @doctocat or @octocat.\n" +
        "/scripts/ @doctocat @octocat\n" +
        "\n" +
        "# In this example, @octocat owns any file in a `/logs` directory such as\n" +
        "# `/build/logs`, `/scripts/logs`, and `/deeply/nested/logs`. Any changes\n" +
        "# in a `/logs` directory will require approval from @octocat.\n" +
        "**/logs @octocat\n" +
        "\n" +
        "# In this example, @octocat owns any file in the `/apps`\n" +
        "# directory in the root of your repository except for the `/apps/github`\n" +
        "# subdirectory, as its owners are left empty.\n" +
        "/apps/ @octocat\n" +
        "/apps/github\n";

    @Test
    void testCodeOwnersGithubDocumentation() {
        CodeOwners codeOwners = new CodeOwners(GITHUB_CODEOWNERS_DOC_EXAMPLE);

        // # These owners will be the default owners for everything in
        // # the repo. Unless a later match takes precedence,
        // # @global-owner1 and @global-owner2 will be requested for
        // # review when someone opens a pull request.
        // *       @global-owner1 @global-owner2
        assertOwners(codeOwners, "RandomName.docx", "@global-owner1", "@global-owner2");

        // # Order is important; the last matching pattern takes the most
        // # precedence. When someone opens a pull request that only
        // # modifies JS files, only @js-owner and not the global
        // # owner(s) will be requested for a review.
        // *.js    @js-owner #This is an inline comment.
        assertOwners(codeOwners, "File.js", "@js-owner");
        assertOwners(codeOwners, "/File.js", "@js-owner");
        assertOwners(codeOwners, "/foo/File.js", "@js-owner");
        assertOwners(codeOwners, "/one/two/three/File.js", "@js-owner");

        // # You can also use email addresses if you prefer. They'll be
        // # used to look up users just like we do for commit author
        // # emails.
        // *.go docs@example.com
        assertOwners(codeOwners, "File.go", "docs@example.com");
        assertOwners(codeOwners, "/File.go", "docs@example.com");
        assertOwners(codeOwners, "/foo/File.go", "docs@example.com");
        assertOwners(codeOwners, "/one/two/three/File.go", "docs@example.com");

        // # Teams can be specified as code owners as well. Teams should
        // # be identified in the format @org/team-name. Teams must have
        // # explicit write access to the repository. In this example,
        // # the octocats team in the octo-org organization owns all .txt files.
        // *.txt @octo-org/octocats
        assertOwners(codeOwners, "File.txt", "@octo-org/octocats");
        assertOwners(codeOwners, "/File.txt", "@octo-org/octocats");
        assertOwners(codeOwners, "/foo/File.txt", "@octo-org/octocats");
        assertOwners(codeOwners, "/one/two/three/File.txt", "@octo-org/octocats");

        // Changed /logs to /output for testing !!!!!!!!!!!!!
        // # In this example, @doctocat owns any files in the build/output
        // # directory at the root of the repository and any of its
        // # subdirectories.
        // /build/output/ @doctocat
        assertOwners(codeOwners, "/build/RandomName.docx", "@global-owner1", "@global-owner2");
        assertOwners(codeOwners, "/build/outputx/RandomName.docx", "@global-owner1", "@global-owner2");
        assertOwners(codeOwners, "/build/outputx/RandomName.js", "@js-owner");
        assertOwners(codeOwners, "/build/output/RandomName.docx", "@doctocat");
        assertOwners(codeOwners, "/build/output/File.js", "@doctocat");
        assertOwners(codeOwners, "/build/output/foo/File.js", "@doctocat");

        // # The `docs/*` pattern will match files like
        // # `docs/getting-started.md` but not further nested files like
        // # `docs/build-app/troubleshooting.md`.
        // docs/*  docs@example.com
        assertOwners(codeOwners, "/foo/docs/getting-started.md", "docs@example.com");
        assertOwners(codeOwners, "/foo/docs/build-app/troubleshooting.md", "@global-owner1", "@global-owner2");

        // # In this example, @octocat owns any file in an apps directory
        // # anywhere in your repository.
        // apps/ @octocat
        assertOwners(codeOwners, "/apps/RandomName.docx", "@octocat");
        assertOwners(codeOwners, "/foo/apps/RandomName.docx", "@octocat");

        // # In this example, @doctocat owns any file in the `/docs`
        // # directory in the root of your repository and any of its
        // # subdirectories.
        // /docs/ @doctocat
        assertOwners(codeOwners, "/apps/RandomName.docx", "@octocat");
        assertOwners(codeOwners, "/foo/apps/RandomName.docx", "@octocat");

        // # In this example, any change inside the `/scripts` directory
        // # will require approval from @doctocat or @octocat.
        // /scripts/ @doctocat @octocat
        assertOwners(codeOwners, "/scriptsx/RandomName.docx", "@global-owner1", "@global-owner2");
        assertOwners(codeOwners, "/scriptsx/RandomName.js", "@js-owner");
        assertOwners(codeOwners, "/scripts/RandomName.docx", "@doctocat", "@octocat");
        assertOwners(codeOwners, "/scripts/File.js", "@doctocat", "@octocat");
        assertOwners(codeOwners, "/scripts/foo/File.js", "@doctocat", "@octocat");

        // # In this example, @octocat owns any file in a `/logs` directory such as
        // # `/build/logs`, `/scripts/logs`, and `/deeply/nested/logs`. Any changes
        // # in a `/logs` directory will require approval from @octocat.
        // **/logs @octocat
        assertOwners(codeOwners, "/logssss/access.log", "@global-owner1", "@global-owner2");
        assertOwners(codeOwners, "/build/logssss/access.log", "@global-owner1", "@global-owner2");
        assertOwners(codeOwners, "/scripts/logssss/access.log", "@doctocat", "@octocat");
        assertOwners(codeOwners, "/deeply/nested/logssss/access.log", "@global-owner1", "@global-owner2");

        assertOwners(codeOwners, "/logs/access.log", "@octocat");

        // !!!!!!!!!!! NOTE THE OVERLAP WITH THE ABOVE '/build/logs/' rule !!!!!!!!!!
        assertOwners(codeOwners, "/build/logs/access.log", "@octocat");
        assertOwners(codeOwners, "/scripts/logs/access.log", "@octocat");
        assertOwners(codeOwners, "/deeply/nested/logs/access.log", "@octocat");

        // # In this example, @octocat owns any file in the `/apps`
        // # directory in the root of your repository except for the `/apps/github`
        // # subdirectory, as its owners are left empty.
        // /apps/ @octocat
        // /apps/github

        assertOwners(codeOwners, "/apps/RandomName.txt", "@octocat");
        // TODO: What should be the permissions for a file somewhere under /apps/github ???????
        assertOwners(codeOwners, "/apps/github/RandomName.txt");
    }

    private void assertOwners(CodeOwners codeOwners, String filename, String... expectedOwners) {
        List<String> allApprovers = codeOwners.getAllApprovers(filename);
        assertEquals(
                Arrays.asList(expectedOwners),
                allApprovers,
                "Filename \""+filename+"\" should have owners " + Arrays.toString(expectedOwners) + " but got " + allApprovers);
    }


}
