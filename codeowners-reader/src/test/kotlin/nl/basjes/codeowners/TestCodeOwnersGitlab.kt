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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class TestCodeOwnersGitlab {
    @Test
    fun gitlabPathsWithSpaces() {
        // Paths containing whitespace must be escaped with backslashes: path\ with\ spaces/*.md.
        val codeOwners = CodeOwners(
            "internalstuff/README.md @user1\n" +
                    "internal\\ stuff/README.md @user2\n"
        )

        TestUtils.assertOwners(codeOwners, "internalstuff/README.md", "@user1")
        TestUtils.assertOwners(codeOwners, "internal stuff/README.md", "@user2")
        TestUtils.assertOwners(codeOwners, "internal  stuff/README.md") // No owners
    }

    @Test
    fun gitlabSections() {
        val codeOwners = CodeOwners(
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
        )

        codeOwners.verbose = true
        // The Code Owners for the README.md in the root directory are @user1, @user2, and @user3 + @user5 because of the extra rule for all *.md files.
        TestUtils.assertOwners(codeOwners, "README.md", "@user1", "@user2", "@user3", "@user5")
        // The Code Owners for internal/README.md are @user4 and @user3 + @user5 because of the extra rule for all *.md files.
        TestUtils.assertOwners(codeOwners, "internal/README.md", "@user3", "@user4", "@user5")
    }

    @Test
    fun gitlabRelativePaths() {
        // If a path does not start with a /, the path is treated as if it starts with a globstar.
        // README.md is treated the same way as /**/README.md:

        // This will match /README.md, /internal/README.md, /app/lib/README.md

        val codeOwnersRoot = CodeOwners("README.md @username")
        TestUtils.assertOwners(codeOwnersRoot, "/README.md", "@username")
        TestUtils.assertOwners(codeOwnersRoot, "/internal/README.md", "@username")
        TestUtils.assertOwners(codeOwnersRoot, "/app/lib/README.md", "@username")

        val codeOwnersSubdir = CodeOwners("internal/README.md @username")
        TestUtils.assertOwners(codeOwnersSubdir, "/internal/README.md", "@username")
        TestUtils.assertOwners(codeOwnersSubdir, "/docs/internal/README.md", "@username")
        TestUtils.assertOwners(codeOwnersSubdir, "/docs/api/internal/README.md", "@username")
    }

    @Test
    fun gitlabWildcardPaths() {
        val codeOwners = CodeOwners(
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
                    "/docs/*/README.md @user4"
        )

        TestUtils.assertOwners(codeOwners, "/docs/test.md", "@user1")

        TestUtils.assertOwners(codeOwners, "/docs/index.md", "@user2")
        TestUtils.assertOwners(codeOwners, "/docs/index.html", "@user2")
        TestUtils.assertOwners(codeOwners, "/docs/index.xml", "@user2")

        TestUtils.assertOwners(codeOwners, "/docs/qa_specs.rb", "@user3")
        TestUtils.assertOwners(codeOwners, "/docs/spec_helpers.rb", "@user3")
        TestUtils.assertOwners(codeOwners, "/docs/runtime.spec", "@user3")

        TestUtils.assertOwners(codeOwners, "/docs/api/README.md", "@user4")
    }

    @Test
    fun gitlabGlobstarPaths() {
        //This will match /docs/index.md, /docs/api/index.md, /docs/api/graphql/index.md
        val codeOwners = CodeOwners("/docs/**/index.md @username")
        TestUtils.assertOwners(codeOwners, "/docs/index.md", "@username")
        TestUtils.assertOwners(codeOwners, "/docs/api/index.md", "@username")
        TestUtils.assertOwners(codeOwners, "/docs/api/graphql/index.md", "@username")
    }

    @Test
    fun gitlabSectionsDefaults() {
        val codeOwners = CodeOwners(
            "[Documentation] @docs-team\n" +
                    "docs/\n" +
                    "README.md\n" +
                    "\n" +
                    "[Database] @database-team\n" +
                    "model/db/\n" +
                    "config/db/database-setup.md @docs-team"
        )

        TestUtils.assertOwners(codeOwners, "docs/api/graphql/index.md", "@docs-team")
        TestUtils.assertOwners(codeOwners, "/something/README.md", "@docs-team")

        TestUtils.assertOwners(codeOwners, "/model/db/README.md", "@docs-team", "@database-team")
    }

    @Test
    fun gitlabMergeSectionsProblems() {
        val codeOwners = CodeOwners(
            "[  Documentation  ] @default-user1\n" +
                    "*\n" +
                    "docs/ @docs-team1\n" +
                    "README.md @docs-team1\n" +
                    "\n" +
                    "[DoCuMeNtAtIoN] @default-user2\n" +  // According to Gitlab this should merge with the previous section.
                    "docs/ @docs-team2\n" +
                    "README.md @docs-team2\n"
        )

        LOG.info("Merged codeowners:\n{}", codeOwners)
        TestUtils.assertOwners(codeOwners, "docs/api/graphql/index.md", "@docs-team2")
        TestUtils.assertOwners(codeOwners, "/something/README.md", "@docs-team2")
        TestUtils.assertOwners(codeOwners, "README.md", "@docs-team2")
        TestUtils.assertOwners(codeOwners, "MatchWildCard.txt", "@default-user1", "@default-user2")
        assertTrue(codeOwners.hasStructuralProblems)
    }

    @Test
    fun gitlabMergeSectionsNoProblems() {
        val codeOwners = CodeOwners(
            "[Documentation] @default-user1\n" +
                    "*\n" +
                    "docs/ @docs-team1\n" +
                    "\n" +
                    "[DoCuMeNtAtIoN] @default-user2\n" +  // According to Gitlab this should merge with the previous section.
                    "README.md @docs-team2\n"
        )

        LOG.info("Merged codeowners:\n{}", codeOwners)
        TestUtils.assertOwners(codeOwners, "docs/api/graphql/index.md", "@docs-team1")
        TestUtils.assertOwners(codeOwners, "/something/README.md", "@docs-team2")
        TestUtils.assertOwners(codeOwners, "README.md", "@docs-team2")
        TestUtils.assertOwners(codeOwners, "MatchWildCard.txt", "@default-user1", "@default-user2")
        assertFalse(codeOwners.hasStructuralProblems)
    }

    @Test
    fun gitlabMergeSectionsOptionalMix() {
        val codeOwners = CodeOwners(
            "[Documentation] @default-user1\n" +
                    "*\n" +
                    "docs/ @docs-team1\n" +
                    "\n" +
                    "^[DoCuMeNtAtIoN] @default-user2\n" +
                    "README.md @docs-team2\n" +
                    "^[Documentation] @default-user1\n" +  // Deliberate duplication!
                    "INSTALL.md @docs-team3\n"
        )

        LOG.info("Merged codeowners:\n------------------\n{}\n------------------", codeOwners)
        TestUtils.assertOwners(codeOwners, "docs/api/graphql/index.md", "@docs-team1")
        TestUtils.assertOwners(codeOwners, "/something/README.md", "@docs-team2")
        TestUtils.assertOwners(codeOwners, "README.md", "@docs-team2")
        TestUtils.assertOwners(codeOwners, "MatchWildCard.txt", "@default-user1", "@default-user2")
        assertTrue(codeOwners.hasStructuralProblems)

        assertEquals(
            "# CODEOWNERS file:\n" +
                    "[Documentation] @default-user1 @default-user2\n" +
                    "* \n" +
                    "docs/ @docs-team1\n" +
                    "README.md @docs-team2\n" +
                    "INSTALL.md @docs-team3\n" +
                    "\n", codeOwners.toString()
        )
    }

    @Test
    fun gitlabCodeOwnersExample() {
        val codeOwners = CodeOwners(GITLAB_CODEOWNERS_DOC_EXAMPLE)
        TestUtils.assertOwners(codeOwners, "Foo.txt", "@code", "@multiple", "@owners", "@dev-team")
        TestUtils.assertOwners(codeOwners, "Foo.rb", "@ruby-owner", "@dev-team")
        TestUtils.assertOwners(codeOwners, "#file_with_pound.rb", "@owner-file-with-pound", "@dev-team")

        TestUtils.assertOwners(codeOwners, "README.md", "@docs", "@docs-team", "@code", "@multiple", "@owners")
        TestUtils.assertOwners(codeOwners, "docs/README.md", "@docs", "@docs-team", "@root-docs")
    }

    @Test
    fun allowElaborateSectionNames() {
        val codeOwners = CodeOwners(
            "[ Some Thing | And & Some $ Thing @ More ]\n" +
                    "README.md @docs-team\n"
        )
        val codeOwnersString = codeOwners.toString()
        LOG.info("\n{}", codeOwnersString)
        TestUtils.assertOwners(codeOwners, "README.md", "@docs-team")
    }

    @Test
    fun gitlabOptional() {
        val codeOwners = CodeOwners(
            "^[One][11] @docs-team\n" +  // Optional + minimal --> Warning message
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
        )

        codeOwners.verbose = false
        TestUtils.assertOwners(codeOwners, "docs/api/graphql/index.md", "@docs-team")
        TestUtils.assertMandatoryOwners(codeOwners, "docs/api/graphql/index.md")

        codeOwners.verbose = true
        TestUtils.assertOwners(codeOwners, "/something/README.md", "@docs-team")
        TestUtils.assertMandatoryOwners(codeOwners, "/something/README.md")

        TestUtils.assertOwners(codeOwners, "/model/db/README.md", "@docs-team", "@database-team")

        codeOwners.verbose = false
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
                    "\n", codeOwners.toString()
        )
    }

    @Test
    fun gitlabRoles() {
        val codeOwners = CodeOwners(
            "^[One][11] @docs-team @@optionalsection some-1@example.nl\n" +  // Optional + minimal --> Warning message
                    "docs/\n" +
                    "*.md\n" +
                    "\n" +
                    "[Two][22] @database-team @@developer user_1_foo@example.nl\n" +
                    "model/db/\n" +
                    "config/db/database-setup.md @docs-team @@maintainer other-2_user@example.nl\n" +
                    "\n"
        )

        codeOwners.verbose = false
        TestUtils.assertOwners(
            codeOwners,
            "docs/api/graphql/index.md",
            "@docs-team",
            "@@optionalsection",
            "some-1@example.nl"
        )
        TestUtils.assertMandatoryOwners(codeOwners, "docs/api/graphql/index.md")

        codeOwners.verbose = true
        TestUtils.assertOwners(
            codeOwners,
            "/something/README.md",
            "@docs-team",
            "@@optionalsection",
            "some-1@example.nl"
        )
        TestUtils.assertMandatoryOwners(codeOwners, "/something/README.md")

        TestUtils.assertOwners(
            codeOwners,
            "/model/db/README.md",
            "@docs-team",
            "@database-team",
            "@@developer",
            "@@optionalsection",
            "some-1@example.nl",
            "user_1_foo@example.nl"
        )
        TestUtils.assertMandatoryOwners(
            codeOwners,
            "/model/db/README.md",
            "@database-team",
            "@@developer",
            "user_1_foo@example.nl"
        )

        TestUtils.assertOwners(
            codeOwners,
            "/config/db/database-setup.md",
            "@docs-team",
            "@@maintainer",
            "@@optionalsection",
            "some-1@example.nl",
            "other-2_user@example.nl"
        )
        TestUtils.assertMandatoryOwners(
            codeOwners,
            "/config/db/database-setup.md",
            "@docs-team",
            "@@maintainer",
            "other-2_user@example.nl"
        )

        codeOwners.verbose = false
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
            codeOwners.toString()
        )
    }


    // https://docs.gitlab.com/user/project/codeowners/reference/#exclusion-patterns
    @Test
    fun gitlabExclusionExample() {
        val codeOwners = CodeOwners(
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
        )

        codeOwners.verbose = true
        TestUtils.assertOwners(codeOwners, "README.md", "@username")
        TestUtils.assertOwners(codeOwners, "pom.xml")
        TestUtils.assertOwners(codeOwners, "something.rb", "@username", "@ruby-team")
        TestUtils.assertOwners(codeOwners, "/somedir/something.rb", "@username", "@ruby-team")
        TestUtils.assertOwners(codeOwners, "/config/something.rb", "@username")

        // Test the toString also
        codeOwners.verbose = false
        LOG.info("CodeOwners were parsed as (non-verbose):\n{}", codeOwners)
        codeOwners.verbose = true
        LOG.info("CodeOwners were parsed as (verbose):\n{}", codeOwners)
    }

    @Test
    fun gitlabExclusionInOrder() {
        val codeOwners = CodeOwners(
            """
            * @default-owner
            # Excludes all Ruby files.
            !*.rb
            # This won't take effect as *.rb is already excluded.
            /special/*.rb @ruby-owner
            """.trimIndent()
        )

        codeOwners.verbose = true
        TestUtils.assertOwners(codeOwners, "README.md", "@default-owner")
        TestUtils.assertOwners(codeOwners, "something.rb")
        TestUtils.assertOwners(codeOwners, "/special/something.rb") // << NOT @ruby-owner
    }

    @Test
    fun gitlabExclusionNoReincludeWithinSection() {
        val codeOwners = CodeOwners(
            "[Ruby]\n" +
                    "*.rb @ruby-team           # All Ruby files need Ruby team approval.\n" +
                    "!/config/**/*.rb          # Ruby files in config don't need Ruby team approval.\n" +
                    "/config/routes.rb @ops    # This won't take effect as config Ruby files are excluded."
        )

        codeOwners.verbose = true
        TestUtils.assertOwners(codeOwners, "something.rb", "@ruby-team")
        TestUtils.assertOwners(codeOwners, "/config/something.rb")
        TestUtils.assertOwners(codeOwners, "/config/subdir/something.rb")
        TestUtils.assertOwners(codeOwners, "/config/routes.rb") // << NOT @ops
    }

    @Test
    fun gitlabExclusionMultipleSections() {
        val codeOwners = CodeOwners(
            "[Ruby]\n" +
                    "*.rb @ruby-team\n" +
                    "!/config/**/*.rb        # Config Ruby files don't need Ruby team approval.\n" +
                    "\n" +
                    "[Config]\n" +
                    "/config/ @ops-team      # Config files still require ops-team approval."
        )

        codeOwners.verbose = true
        TestUtils.assertOwners(codeOwners, "something.rb", "@ruby-team")
        TestUtils.assertOwners(codeOwners, "/config/something.rb", "@ops-team")
        TestUtils.assertOwners(codeOwners, "/config/something.conf", "@ops-team")
    }

    @Test
    fun gitlabExclusionRealUsecase() {
        val codeOwners = CodeOwners(
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
        )

        codeOwners.verbose = true
        TestUtils.assertOwners(codeOwners, "README.md", "@quality") // Not for *.md
        TestUtils.assertOwners(codeOwners, "docs/README.md", "@quality") // Not for *.md
        TestUtils.assertOwners(codeOwners, "subdir/README.md", "@quality") // Not for *.md
        TestUtils.assertOwners(codeOwners, "Something.rb", "@quality", "@changemanagement")
        TestUtils.assertOwners(codeOwners, "docs/Something.rb", "@quality") // Not in /docs/
        TestUtils.assertOwners(codeOwners, "subdir/Something.rb", "@quality", "@changemanagement")
        TestUtils.assertOwners(codeOwners, "Foo.gitignore", "@quality", "@changemanagement")
        TestUtils.assertOwners(codeOwners, ".gitignore", "@quality") // Not .gitignore
        TestUtils.assertOwners(codeOwners, "docs/Foo.gitignore", "@quality") // Not in /docs/
        TestUtils.assertOwners(codeOwners, "docs/.gitignore", "@quality") // Not in /docs/ AND Not .gitignore
        TestUtils.assertOwners(codeOwners, "subdir/Foo.gitignore", "@quality", "@changemanagement")
        TestUtils.assertOwners(codeOwners, "subdir/.gitignore", "@quality") // Not .gitignore
    }


    @Test
    fun gitlabFileMatchingBugReproduction() {
        // The bug: The second line also matches "foo.gitignore"
        val codeOwners = CodeOwners(
            "*.gitignore @one\n" +
                    ".gitignore @two\n"
        )
        codeOwners.verbose = true
        TestUtils.assertOwners(codeOwners, ".gitignore", "@two") // Last match in a section is used
        TestUtils.assertOwners(codeOwners, "foo.gitignore", "@one")
        TestUtils.assertOwners(codeOwners, "/.gitignore", "@two") // Last match in a section is used
        TestUtils.assertOwners(codeOwners, "/foo.gitignore", "@one")
        TestUtils.assertOwners(codeOwners, "/subdir/.gitignore", "@two")
        TestUtils.assertOwners(codeOwners, "/subdir/foo.gitignore", "@one")
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(TestCodeOwnersGitlab::class.java)

        // From https://docs.gitlab.com/ee/user/project/codeowners/reference.html#example-codeowners-file
        private val GITLAB_CODEOWNERS_DOC_EXAMPLE = """
            # This is an example of a CODEOWNERS file.
            # Lines that start with `#` are ignored.

            # app/ @commented-rule

            # Specify a default Code Owner by using a wildcard:
            * @default-codeowner

            # Specify multiple Code Owners by using a tab or space:
            * @multiple @code @owners

            # Rules defined later in the file take precedence over the rules
            # defined before.
            # For example, for all files with a filename ending in `.rb`:
            *.rb @ruby-owner

            # Files with a `#` can still be accessed by escaping the pound sign:
            \#file_with_pound.rb @owner-file-with-pound

            # Specify multiple Code Owners separated by spaces or tabs.
            # In the following case the CODEOWNERS file from the root of the repo
            # has 3 Code Owners (@multiple @code @owners):
            CODEOWNERS @multiple @code @owners

            # You can use both usernames or email addresses to match
            # users. Everything else is ignored. For example, this code
            # specifies the `@legal` and a user with email `janedoe@gitlab.com` as the
            # owner for the LICENSE file:
            LICENSE @legal this_does_not_match janedoe@gitlab.com

            # Use group names to match groups, and nested groups to specify
            # them as owners for a file:
            README @group @group/with-nested/subgroup

            # End a path in a `/` to specify the Code Owners for every file
            # nested in that directory, on any level:
            /docs/ @all-docs

            # End a path in `/*` to specify Code Owners for every file in
            # a directory, but not nested deeper. This code matches
            # `docs/index.md` but not `docs/projects/index.md`:
            /docs/* @root-docs

            # Include `/**` to specify Code Owners for all subdirectories
            # in a directory. This rule matches `docs/projects/index.md` or
            # `docs/development/index.md`
            /docs/**/*.md @root-docs

            # This code makes matches a `lib` directory nested anywhere in the repository:
            lib/ @lib-owner

            # This code match only a `config` directory in the root of the repository:
            /config/ @config-owner

            # If the path contains spaces, escape them like this:
            path\ with\ spaces/ @space-owner

            # Code Owners section:
            [Documentation]
            ee/docs    @docs
            docs       @docs

            # Use of default owners for a section. In this case, all files (*) are owned by
            # the dev team except the README.md and data-models which are owned by other teams.
            [Development] @dev-team
            *
            README.md @docs-team
            data-models/ @data-science-team

            # This section is combined with the previously defined [Documentation] section:
            [DOCUMENTATION]
            README.md  @docs""".trimIndent()
    }
}
