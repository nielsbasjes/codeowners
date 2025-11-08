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

package nl.basjes.codeowners.validator;

import nl.basjes.codeowners.validator.CodeOwnersValidator.DirectoryOwners;
import nl.basjes.codeowners.validator.CodeOwnersValidator.FileOwners;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestCodeOwnersValidator {

    private static Logger log = LoggerFactory.getLogger(TestCodeOwnersValidator.class);

    private DirectoryOwners analyze(String directoryName, boolean verbose) throws CodeOwnersValidationException {
        CodeOwnersValidator validator = new CodeOwnersValidator(null, null, verbose);

        DirectoryOwners directoryOwners = validator.analyzeDirectory(new File("src/test/resources/testdirectories/"+directoryName));

        log.info("\n{}", directoryOwners.toTable());
        return directoryOwners;
    }

    private static void assertMandatoryApprovers(DirectoryOwners directoryOwners, String path, String... expected) {
        Path asPath = Path.of(path);
        assertNotNull(asPath, "Unable to convert " +path+ " into a Path.");
        FileOwners fileOwners = directoryOwners.get(asPath);
        assertNotNull(fileOwners, "No FileOwners found for " + asPath);
        assertEquals(List.of(expected), fileOwners.mandatoryApprovers);
    }

    private static void assertIgnored(DirectoryOwners directoryOwners, String path) {
        Path asPath = Path.of(path);
        assertNotNull(asPath, "Unable to convert " +path+ " into a Path.");
        FileOwners fileOwners = directoryOwners.get(asPath);
        assertNull(fileOwners, "No FileOwners should have been found for " + asPath);
    }

    @Test
    void testCovered() throws CodeOwnersValidationException {
        DirectoryOwners directoryOwners = analyze("covered", false);

        assertMandatoryApprovers(directoryOwners, "",                     "@integrationtest");
        assertMandatoryApprovers(directoryOwners, ".gitlab/",             "@integrationtest");
        assertMandatoryApprovers(directoryOwners, ".gitlab/CODEOWNERS",   "@nielsbasjes");
        assertMandatoryApprovers(directoryOwners, "pom.xml",              "@integrationtest");
        assertMandatoryApprovers(directoryOwners, "src/",                 "@integrationtest");
        assertMandatoryApprovers(directoryOwners, "src/main/",            "@projectteam");
        assertMandatoryApprovers(directoryOwners, "src/main/README.java", "@projectteam");
        assertMandatoryApprovers(directoryOwners, "src/main/README.md",   "@username");
        assertMandatoryApprovers(directoryOwners, "src/main/README.txt",  "@projectteam");
    }

    @Test
    void testGitignore() throws CodeOwnersValidationException {
        DirectoryOwners directoryOwners = analyze("gitignore", false);

        assertMandatoryApprovers(directoryOwners, "",                   "@integrationtest"      );
        assertMandatoryApprovers(directoryOwners, ".gitignore",         "@integrationtest"      );
        assertMandatoryApprovers(directoryOwners, ".gitlab/",           "@integrationtest"      );
        assertMandatoryApprovers(directoryOwners, ".gitlab/CODEOWNERS", "@nielsbasjes"          );
        assertMandatoryApprovers(directoryOwners, "pom.xml",            "@integrationtest"      );
        assertMandatoryApprovers(directoryOwners, "src/",               "@integrationtest"      );
        assertMandatoryApprovers(directoryOwners, "src/.gitignore",     "@nielsbasjes"          );
        assertMandatoryApprovers(directoryOwners, "src/main/",          "@projectteam"          );
        assertMandatoryApprovers(directoryOwners, "src/main/README.md", "@username"             );

        assertIgnored(directoryOwners, "build.log");
        assertIgnored(directoryOwners, "src/main/README.java");
        assertIgnored(directoryOwners, "src/main/README.txt");
    }

        @Test
    void testMissing() throws CodeOwnersValidationException {
        DirectoryOwners directoryOwners = analyze("missing", false);

        assertMandatoryApprovers(directoryOwners, "",                     "@integrationtest"   );
        assertMandatoryApprovers(directoryOwners, ".gitlab/",             "@integrationtest"   );
        assertMandatoryApprovers(directoryOwners, ".gitlab/CODEOWNERS",   "@nielsbasjes"       );
        assertMandatoryApprovers(directoryOwners, "pom.xml",              "@integrationtest"   );
        assertMandatoryApprovers(directoryOwners, "src/",                 "@integrationtest"   );
        assertMandatoryApprovers(directoryOwners, "src/main/"             ); // <-- NO APPROVERS!
        assertMandatoryApprovers(directoryOwners, "src/main/README.java"  ); // <-- NO APPROVERS!
        assertMandatoryApprovers(directoryOwners, "src/main/README.md",   "@username"          );
        assertMandatoryApprovers(directoryOwners, "src/main/README.txt"   ); // <-- NO APPROVERS!
    }

        @Test
    void testOutSideGitlab() throws CodeOwnersValidationException {
            DirectoryOwners directoryOwners = analyze("OutsideGitlab", false);

            assertMandatoryApprovers(directoryOwners, "",                     "@integrationtest"  );
            assertMandatoryApprovers(directoryOwners, ".gitlab/",             "@integrationtest"  );
            assertMandatoryApprovers(directoryOwners, ".gitlab/CODEOWNERS",   "@nielsbasjes"      );
            assertMandatoryApprovers(directoryOwners, "pom.xml",              "@integrationtest"  );
            assertMandatoryApprovers(directoryOwners, "src/",                 "@integrationtest"  );
            assertMandatoryApprovers(directoryOwners, "src/main/"             ); // <-- NO APPROVERS!
            assertMandatoryApprovers(directoryOwners, "src/main/README.java"  ); // <-- NO APPROVERS!
            assertMandatoryApprovers(directoryOwners, "src/main/README.md",   "@username"         );
            assertMandatoryApprovers(directoryOwners, "src/main/README.txt"  ); // <-- NO APPROVERS!

//        assert text.contains("[ERROR] Unable to load projectId from Gitlab: GitlabConfiguration: {");
//        assert text.contains("[ERROR]   ServerUrl='http://localhost:0' found via gitlab.serverUrl.url is valid.");
//        assert text.contains("[ERROR]   ProjectId='niels/project' found via gitlab.projectId.id is valid.");
//        assert text.contains("[ERROR]   AccessToken= 'gltst-*****ue' found via environment variable CHECK_USERS_TOKEN is valid.");
        }

    @Test
    void testTestTree() throws CodeOwnersValidationException {
        DirectoryOwners directoryOwners = analyze("testtree", true);
//        assert text.contains("[INFO] Using GitIgnore : \${baseDir}/.gitignore")
//        assert text.contains("[INFO] Using GitIgnore : \${baseDir}/dir1/.gitignore")
//        assert text.contains("[INFO] Using GitIgnore : \${baseDir}/dir3/.gitignore")
//
//// These exist but are ignored so they may not be read.
//        assert !text.contains("[INFO] Using GitIgnore : \${baseDir}/dir4/.gitignore")
//        assert !text.contains("[INFO] Using GitIgnore : \${baseDir}/dir4/subdir/.gitignore")
//        assert !text.contains("[INFO] Using GitIgnore : \${baseDir}/dir4/subdir/subdir/.gitignore")

//        assert text.contains("[INFO] Using CODEOWNERS: \${baseDir}/docs/CODEOWNERS")

        assertMandatoryApprovers(directoryOwners,""                   , "@integrationtest" );
        assertMandatoryApprovers(directoryOwners,".gitignore"         , "@integrationtest" );
        assertMandatoryApprovers(directoryOwners,"docs/CODEOWNERS"    , "@documentation"   );
        assertMandatoryApprovers(directoryOwners,"dir1/"              , "@integrationtest" );
        assertMandatoryApprovers(directoryOwners,"dir1/.gitignore"    , "@dir1project"     );
        assertMandatoryApprovers(directoryOwners,"dir1/dir1.md"       , "@dir1project"     );
        assertMandatoryApprovers(directoryOwners,"dir2/"              , "@integrationtest" );
        assertMandatoryApprovers(directoryOwners,"dir2/dir2.txt"      , "@txt"             );
        assertMandatoryApprovers(directoryOwners,"dir2/file2.log"     , "@log"             );
        assertMandatoryApprovers(directoryOwners,"dir3/"              , "@integrationtest" );
        assertMandatoryApprovers(directoryOwners,"dir3/.gitignore"    , "@dir3project"     );
        assertMandatoryApprovers(directoryOwners,"dir3/dir3.txt"      , "@dir3project"     );
        assertMandatoryApprovers(directoryOwners,"dir3/file3.log"     , "@dir3project"     );
        assertMandatoryApprovers(directoryOwners,"pom.xml"            , "@integrationtest" );

        // Make sure NONE of the ignored files get an approver

        assertIgnored(directoryOwners, "dir1/dir1.log");
        assertIgnored(directoryOwners, "dir1/dir1.txt");
        assertIgnored(directoryOwners, "dir1/file1.log");
        assertIgnored(directoryOwners, "dir2/dir2.log");
        assertIgnored(directoryOwners, "dir2/dir2.md");
        assertIgnored(directoryOwners, "dir3/dir3.log");
        assertIgnored(directoryOwners, "dir3/dir3.md");
        assertIgnored(directoryOwners, "dir4/"); // Which matches everything under dir4
        assertIgnored(directoryOwners, "README.md");
        assertIgnored(directoryOwners, "root.md");
    }
}
