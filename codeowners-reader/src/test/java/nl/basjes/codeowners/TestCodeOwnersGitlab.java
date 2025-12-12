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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.basjes.codeowners.TestUtils.assertMandatoryOwners;
import static nl.basjes.codeowners.TestUtils.assertOwners;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestCodeOwnersGitlab {

    private static final Logger LOG = LoggerFactory.getLogger(TestCodeOwnersGitlab.class);

    @Test
    void gitlabPathsWithSpaces() {
        // Paths containing whitespace must be escaped with backslashes: path\ with\ spaces/*.md.
        CodeOwners codeOwners = new CodeOwners(
            "internalstuff/README.md @user1\n" +
            "internal\\ stuff/README.md @user2\n"
        );

        assertOwners(codeOwners, "internalstuff/README.md",  "@user1");
        assertOwners(codeOwners, "internal stuff/README.md", "@user2");
        assertOwners(codeOwners, "internal  stuff/README.md"); // No owners
    }

    @Test
    void gitlabSections() {
        CodeOwners codeOwners = new CodeOwners(
                "[README Owners]\n" +
                "README.md @user1 @user2\n" +
                "internal/README.md @user4\n" +
                "\n" +
                "[README other owners]\n" +
                "README.md @user3 \n" +
                "\n" +
                "[README default] @user5\n" +
                "*.md\n" +
                "SomethingElse.md @user3"
        );

        codeOwners.setVerbose(true);
        // The Code Owners for the README.md in the root directory are @user1, @user2, and @user3 + @user5 because of the extra rule for all *.md files.
        assertOwners(codeOwners, "README.md",           "@user1", "@user2", "@user3", "@user5");
        // The Code Owners for internal/README.md are @user4 and @user3 + @user5 because of the extra rule for all *.md files.
        assertOwners(codeOwners, "internal/README.md",  "@user3", "@user4", "@user5");
    }

    @Test
    void gitlabRelativePaths() {
        // If a path does not start with a /, the path is treated as if it starts with a globstar.
        // README.md is treated the same way as /**/README.md:

        // This will match /README.md, /internal/README.md, /app/lib/README.md
        CodeOwners codeOwnersRoot = new CodeOwners("README.md @username");
        assertOwners(codeOwnersRoot, "/README.md",                      "@username");
        assertOwners(codeOwnersRoot, "/internal/README.md",             "@username");
        assertOwners(codeOwnersRoot, "/app/lib/README.md",              "@username");

        CodeOwners codeOwnersSubdir = new CodeOwners("internal/README.md @username");
        assertOwners(codeOwnersSubdir, "/internal/README.md",           "@username");
        assertOwners(codeOwnersSubdir, "/docs/internal/README.md",      "@username");
        assertOwners(codeOwnersSubdir, "/docs/api/internal/README.md",  "@username");
    }

    @Test
    void gitlabWildcardPaths() {
        CodeOwners codeOwners = new CodeOwners(
            "# Any markdown files in the docs directory\n" +
            "/docs/*.md @user1\n" +
            "\n" +
            "# /docs/index file of any filetype\n" +
            "# For example: /docs/index.md, /docs/index.html, /docs/index.xml\n" +
            "/docs/index.* @user2\n" +
            "\n" +
            "# Any file in the docs directory with 'spec' in the name.\n" +
            "# For example: /docs/qa_specs.rb, /docs/spec_helpers.rb, /docs/runtime.spec\n" +
            "/docs/*spec* @user3\n" +
            "\n" +
            "# README.md files one level deep within the docs directory\n" +
            "# For example: /docs/api/README.md\n" +
            "/docs/*/README.md @user4");

        assertOwners(codeOwners, "/docs/test.md",         "@user1");

        assertOwners(codeOwners, "/docs/index.md",        "@user2");
        assertOwners(codeOwners, "/docs/index.html",      "@user2");
        assertOwners(codeOwners, "/docs/index.xml",       "@user2");

        assertOwners(codeOwners, "/docs/qa_specs.rb",     "@user3");
        assertOwners(codeOwners, "/docs/spec_helpers.rb", "@user3");
        assertOwners(codeOwners, "/docs/runtime.spec",    "@user3");

        assertOwners(codeOwners, "/docs/api/README.md",   "@user4");
    }

    @Test
    void gitlabGlobstarPaths() {
        //This will match /docs/index.md, /docs/api/index.md, /docs/api/graphql/index.md
        CodeOwners codeOwners = new CodeOwners("/docs/**/index.md @username");
        assertOwners(codeOwners, "/docs/index.md",             "@username");
        assertOwners(codeOwners, "/docs/api/index.md",         "@username");
        assertOwners(codeOwners, "/docs/api/graphql/index.md", "@username");
    }

    @Test
    void gitlabSectionsDefaults() {
        CodeOwners codeOwners = new CodeOwners(
            "[Documentation] @docs-team\n" +
            "docs/\n" +
            "README.md\n" +
            "\n" +
            "[Database] @database-team\n" +
            "model/db/\n" +
            "config/db/database-setup.md @docs-team");

        assertOwners(codeOwners, "docs/api/graphql/index.md", "@docs-team");
        assertOwners(codeOwners, "/something/README.md", "@docs-team");

        assertOwners(codeOwners, "/model/db/README.md", "@docs-team", "@database-team");
    }

    @Test
    void gitlabMergeSectionsProblems() {
        CodeOwners codeOwners = new CodeOwners(
            "[  Documentation  ] @default-user1\n" +
            "*\n" +
            "docs/ @docs-team1\n" +
            "README.md @docs-team1\n" +
            "\n" +
            "[DoCuMeNtAtIoN] @default-user2\n" + // According to Gitlab this should merge with the previous section.
            "docs/ @docs-team2\n" +
            "README.md @docs-team2\n");

        LOG.info("Merged codeowners:\n{}", codeOwners);
        assertOwners(codeOwners, "docs/api/graphql/index.md",   "@docs-team2");
        assertOwners(codeOwners, "/something/README.md",        "@docs-team2");
        assertOwners(codeOwners, "README.md",                   "@docs-team2");
        assertOwners(codeOwners, "MatchWildCard.txt",           "@default-user1", "@default-user2");
        assertTrue(codeOwners.hasStructuralProblems());
    }

    @Test
    void gitlabMergeSectionsNoProblems() {
        CodeOwners codeOwners = new CodeOwners(
            "[Documentation] @default-user1\n" +
            "*\n" +
            "docs/ @docs-team1\n" +
            "\n" +
            "[DoCuMeNtAtIoN] @default-user2\n" + // According to Gitlab this should merge with the previous section.
            "README.md @docs-team2\n");

        LOG.info("Merged codeowners:\n{}", codeOwners);
        assertOwners(codeOwners, "docs/api/graphql/index.md",   "@docs-team1");
        assertOwners(codeOwners, "/something/README.md",        "@docs-team2");
        assertOwners(codeOwners, "README.md",                   "@docs-team2");
        assertOwners(codeOwners, "MatchWildCard.txt",           "@default-user1", "@default-user2");
        assertFalse(codeOwners.hasStructuralProblems());
    }

    @Test
    void gitlabMergeSectionsOptionalMix() {
        CodeOwners codeOwners = new CodeOwners(
            "[Documentation] @default-user1\n" +
            "*\n" +
            "docs/ @docs-team1\n" +
            "\n" +
            "^[DoCuMeNtAtIoN] @default-user2\n" +
            "README.md @docs-team2\n" +
            "^[Documentation] @default-user1\n" + // Deliberate duplication!
            "INSTALL.md @docs-team3\n"
            );

        LOG.info("Merged codeowners:\n------------------\n{}\n------------------", codeOwners);
        assertOwners(codeOwners, "docs/api/graphql/index.md",   "@docs-team1");
        assertOwners(codeOwners, "/something/README.md",        "@docs-team2");
        assertOwners(codeOwners, "README.md",                   "@docs-team2");
        assertOwners(codeOwners, "MatchWildCard.txt",           "@default-user1", "@default-user2");
        assertTrue(codeOwners.hasStructuralProblems());

        assertEquals(
            "# CODEOWNERS file:\n" +
            "[Documentation] @default-user1 @default-user2\n" +
            "* \n" +
            "docs/ @docs-team1\n" +
            "README.md @docs-team2\n" +
            "INSTALL.md @docs-team3\n" +
            "\n", codeOwners.toString());
    }

    // From https://docs.gitlab.com/ee/user/project/codeowners/reference.html#example-codeowners-file
    private static final String GITLAB_CODEOWNERS_DOC_EXAMPLE =
        "# This is an example of a CODEOWNERS file.\n" +
        "# Lines that start with `#` are ignored.\n" +
        "\n" +
        "# app/ @commented-rule\n" +
        "\n" +
        "# Specify a default Code Owner by using a wildcard:\n" +
        "* @default-codeowner\n" +
        "\n" +
        "# Specify multiple Code Owners by using a tab or space:\n" +
        "* @multiple @code @owners\n" +
        "\n" +
        "# Rules defined later in the file take precedence over the rules\n" +
        "# defined before.\n" +
        "# For example, for all files with a filename ending in `.rb`:\n" +
        "*.rb @ruby-owner\n" +
        "\n" +
        "# Files with a `#` can still be accessed by escaping the pound sign:\n" +
        "\\#file_with_pound.rb @owner-file-with-pound\n" +
        "\n" +
        "# Specify multiple Code Owners separated by spaces or tabs.\n" +
        "# In the following case the CODEOWNERS file from the root of the repo\n" +
        "# has 3 Code Owners (@multiple @code @owners):\n" +
        "CODEOWNERS @multiple @code @owners\n" +
        "\n" +
        "# You can use both usernames or email addresses to match\n" +
        "# users. Everything else is ignored. For example, this code\n" +
        "# specifies the `@legal` and a user with email `janedoe@gitlab.com` as the\n" +
        "# owner for the LICENSE file:\n" +
        "LICENSE @legal this_does_not_match janedoe@gitlab.com\n" +
        "\n" +
        "# Use group names to match groups, and nested groups to specify\n" +
        "# them as owners for a file:\n" +
        "README @group @group/with-nested/subgroup\n" +
        "\n" +
        "# End a path in a `/` to specify the Code Owners for every file\n" +
        "# nested in that directory, on any level:\n" +
        "/docs/ @all-docs\n" +
        "\n" +
        "# End a path in `/*` to specify Code Owners for every file in\n" +
        "# a directory, but not nested deeper. This code matches\n" +
        "# `docs/index.md` but not `docs/projects/index.md`:\n" +
        "/docs/* @root-docs\n" +
        "\n" +
        "# Include `/**` to specify Code Owners for all subdirectories\n" +
        "# in a directory. This rule matches `docs/projects/index.md` or\n" +
        "# `docs/development/index.md`\n" +
        "/docs/**/*.md @root-docs\n" +
        "\n" +
        "# This code makes matches a `lib` directory nested anywhere in the repository:\n" +
        "lib/ @lib-owner\n" +
        "\n" +
        "# This code match only a `config` directory in the root of the repository:\n" +
        "/config/ @config-owner\n" +
        "\n" +
        "# If the path contains spaces, escape them like this:\n" +
        "path\\ with\\ spaces/ @space-owner\n" +
        "\n" +
        "# Code Owners section:\n" +
        "[Documentation]\n" +
        "ee/docs    @docs\n" +
        "docs       @docs\n" +
        "\n" +
        "# Use of default owners for a section. In this case, all files (*) are owned by\n" +
        "# the dev team except the README.md and data-models which are owned by other teams.\n" +
        "[Development] @dev-team\n" +
        "*\n" +
        "README.md @docs-team\n" +
        "data-models/ @data-science-team\n" +
        "\n" +
        "# This section is combined with the previously defined [Documentation] section:\n" +
        "[DOCUMENTATION]\n" +
        "README.md  @docs";

    @Test
    void gitlabCodeOwnersExample() {
        CodeOwners codeOwners = new CodeOwners(GITLAB_CODEOWNERS_DOC_EXAMPLE);
        assertOwners(codeOwners, "Foo.txt","@code","@multiple","@owners", "@dev-team");
        assertOwners(codeOwners, "Foo.rb","@ruby-owner", "@dev-team");
        assertOwners(codeOwners, "#file_with_pound.rb","@owner-file-with-pound", "@dev-team");

        assertOwners(codeOwners, "README.md", "@docs", "@docs-team", "@code","@multiple","@owners");
        assertOwners(codeOwners, "docs/README.md", "@docs","@docs-team","@root-docs");
    }

    @Test
    void allowElaborateSectionNames() {
        CodeOwners codeOwners = new CodeOwners(
        "[ Some Thing | And & Some $ Thing @ More ]\n" +
        "README.md @docs-team\n");
        String codeOwnersString = codeOwners.toString();
        LOG.info("\n{}", codeOwnersString);
        assertOwners(codeOwners, "README.md", "@docs-team");
    }

    @Test
    void gitlabOptional() {
        CodeOwners codeOwners = new CodeOwners(
            "^[One][11] @docs-team\n" + // Optional + minimal --> Warning message
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
            "^[Four]\n" +
            "four/\n" +
            "\n" +
            "[Three]\n" +
            "three2/\n" +
            "\n"
        );

        codeOwners.setVerbose(false);
        assertOwners(codeOwners, "docs/api/graphql/index.md", "@docs-team");
        assertMandatoryOwners(codeOwners, "docs/api/graphql/index.md");

        codeOwners.setVerbose(true);
        assertOwners(codeOwners, "/something/README.md", "@docs-team");
        assertMandatoryOwners(codeOwners, "/something/README.md");

        assertOwners(codeOwners, "/model/db/README.md", "@docs-team", "@database-team");

        codeOwners.setVerbose(false);
        assertEquals(
            "# CODEOWNERS file:\n" +
            "^[One][11] @docs-team\n" +
            "docs/ \n" +
            "*.md \n" +
            "\n" +
            "[Two][22] @database-team\n" +
            "model/db/ \n" +
            "config/db/database-setup.md @docs-team\n" +
            "\n" +
            "[Three]\n" +
            "three1/ \n" +
            "three2/ \n" +
            "\n" +
            "^[Four]\n" +
            "four/ \n" +
            "\n", codeOwners.toString());
    }

    @Test
    void gitlabRoles() {
        CodeOwners codeOwners = new CodeOwners(
            "^[One][11] @docs-team @@optionalsection some-1@example.nl\n" + // Optional + minimal --> Warning message
            "docs/\n" +
            "*.md\n" +
            "\n" +
            "[Two][22] @database-team @@developer user_1_foo@example.nl\n" +
            "model/db/\n" +
            "config/db/database-setup.md @docs-team @@maintainer other-2_user@example.nl\n" +
            "\n"
        );

        codeOwners.setVerbose(false);
        assertOwners(codeOwners, "docs/api/graphql/index.md", "@docs-team", "@@optionalsection", "some-1@example.nl");
        assertMandatoryOwners(codeOwners, "docs/api/graphql/index.md");

        codeOwners.setVerbose(true);
        assertOwners(codeOwners, "/something/README.md", "@docs-team", "@@optionalsection", "some-1@example.nl");
        assertMandatoryOwners(codeOwners, "/something/README.md");

        assertOwners(codeOwners, "/model/db/README.md", "@docs-team", "@database-team", "@@developer", "@@optionalsection", "some-1@example.nl", "user_1_foo@example.nl");
        assertMandatoryOwners(codeOwners, "/model/db/README.md", "@database-team", "@@developer", "user_1_foo@example.nl");

        assertOwners(codeOwners, "/config/db/database-setup.md", "@docs-team", "@@maintainer", "@@optionalsection", "some-1@example.nl", "other-2_user@example.nl");
        assertMandatoryOwners(codeOwners, "/config/db/database-setup.md", "@docs-team", "@@maintainer", "other-2_user@example.nl");

        codeOwners.setVerbose(false);
        assertEquals(
            "# CODEOWNERS file:\n" +
            "^[One][11] @docs-team @@optionalsection some-1@example.nl\n" +
            "docs/ \n" +
            "*.md \n" +
            "\n" +
            "[Two][22] @database-team @@developer user_1_foo@example.nl\n" +
            "model/db/ \n" +
            "config/db/database-setup.md @docs-team @@maintainer other-2_user@example.nl\n" +
            "\n",
            codeOwners.toString());
    }


    // https://docs.gitlab.com/user/project/codeowners/reference/#exclusion-patterns
    @Test
    void gitlabExclusionExample() {
        CodeOwners codeOwners = new CodeOwners(
            "# All files require approval from @username\n" +
            "* @username\n" +
            "\n" +
            "# Except pom.xml which needs no approval\n" +
            "!pom.xml\n" +
            "\n" +
            "[Ruby]\n" +
            "# All ruby files require approval from @ruby-team\n" +
            "*.rb @ruby-team\n" +
            "\n" +
            "# Except Ruby files in the config directory\n" +
            "!/config/**/*.rb"
        );

        codeOwners.setVerbose(true);
        assertOwners(codeOwners, "README.md", "@username");
        assertOwners(codeOwners, "pom.xml");
        assertOwners(codeOwners, "something.rb", "@username", "@ruby-team");
        assertOwners(codeOwners, "/somedir/something.rb", "@username", "@ruby-team");
        assertOwners(codeOwners, "/config/something.rb", "@username");
    }

    @Test
    void gitlabExclusionInOrder() {
        CodeOwners codeOwners = new CodeOwners(
            "* @default-owner\n" +
            "!*.rb                      # Excludes all Ruby files.\n" +
            "/special/*.rb @ruby-owner  # This won't take effect as *.rb is already excluded."
        );

        codeOwners.setVerbose(true);
        assertOwners(codeOwners, "README.md", "@default-owner");
        assertOwners(codeOwners, "something.rb");
        assertOwners(codeOwners, "/special/something.rb"); // << NOT @ruby-owner
    }

    @Test
    void gitlabExclusionNoReincludeWithinSection() {
        CodeOwners codeOwners = new CodeOwners(
            "[Ruby]\n" +
            "*.rb @ruby-team           # All Ruby files need Ruby team approval.\n" +
            "!/config/**/*.rb          # Ruby files in config don't need Ruby team approval.\n" +
            "/config/routes.rb @ops    # This won't take effect as config Ruby files are excluded."
        );

        codeOwners.setVerbose(true);
        assertOwners(codeOwners, "something.rb",  "@ruby-team");
        assertOwners(codeOwners, "/config/something.rb");
        assertOwners(codeOwners, "/config/subdir/something.rb");
        assertOwners(codeOwners, "/config/routes.rb"); // << NOT @ops
    }

    @Test
    void gitlabExclusionMultipleSections() {
        CodeOwners codeOwners = new CodeOwners(
            "[Ruby]\n" +
            "*.rb @ruby-team\n" +
            "!/config/**/*.rb        # Config Ruby files don't need Ruby team approval.\n" +
            "\n" +
            "[Config]\n" +
            "/config/ @ops-team      # Config files still require ops-team approval."
        );

        codeOwners.setVerbose(true);
        assertOwners(codeOwners, "something.rb",  "@ruby-team");
        assertOwners(codeOwners, "/config/something.rb", "@ops-team");
        assertOwners(codeOwners, "/config/something.conf", "@ops-team");
    }

    @Test
    void gitlabExclusionRealUsecase() {
        CodeOwners codeOwners = new CodeOwners(
            "[Code Quality][3] @quality\n" +
            "*\n" +
            "\n" +
            "[Change Management Process][1] @changemanagement\n" +
            "!/docs/\n" +
            "!*.md\n" +
            "!*.example\n" +
            "!.gitignore\n" +
            "!.prettierignore\n" +
            "*"
        );

        codeOwners.setVerbose(true);
        assertOwners(codeOwners, "README.md",             "@quality"); // Not for *.md
        assertOwners(codeOwners, "docs/README.md",        "@quality"); // Not for *.md
        assertOwners(codeOwners, "subdir/README.md",      "@quality"); // Not for *.md
        assertOwners(codeOwners, "Something.rb",          "@quality", "@changemanagement");
        assertOwners(codeOwners, "docs/Something.rb",     "@quality"); // Not in /docs/
        assertOwners(codeOwners, "subdir/Something.rb",   "@quality", "@changemanagement");
        assertOwners(codeOwners, "Foo.gitignore",         "@quality", "@changemanagement");
        assertOwners(codeOwners, ".gitignore",            "@quality"); // Not .gitignore
        assertOwners(codeOwners, "docs/Foo.gitignore",    "@quality"); // Not in /docs/
        assertOwners(codeOwners, "docs/.gitignore",       "@quality"); // Not in /docs/ AND Not .gitignore
        assertOwners(codeOwners, "subdir/Foo.gitignore",  "@quality", "@changemanagement");
        assertOwners(codeOwners, "subdir/.gitignore",     "@quality"); // Not .gitignore
    }


    @Test
    void gitlabFileMatchingBugReproduction() {
        // The bug: The second line also matches "foo.gitignore"
        CodeOwners codeOwners = new CodeOwners(
            "*.gitignore @one\n" +
            ".gitignore @two\n"
        );
        codeOwners.setVerbose(true);
        assertOwners(codeOwners, ".gitignore", "@two"); // Last match in a section is used
        assertOwners(codeOwners, "foo.gitignore", "@one");
        assertOwners(codeOwners, "/.gitignore", "@two"); // Last match in a section is used
        assertOwners(codeOwners, "/foo.gitignore", "@one");
        assertOwners(codeOwners, "/subdir/.gitignore", "@two");
        assertOwners(codeOwners, "/subdir/foo.gitignore", "@one");
    }

}
