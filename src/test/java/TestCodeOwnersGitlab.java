import nl.basjes.maven.enforcer.codeowners.CodeOwners;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestCodeOwnersGitlab {

    @Test
    void gitlabSections() {
        CodeOwners codeOwners = new CodeOwners(
                "[README Owners][2] @user5 @user6\n" +
                "internal/README.md @user4\n" +
                "\n" +
                "[README other owners] @user6\n" +
                "README.md @user3\n" +
                "^[README Owners]\n" +
                "README.md @user1 @user2\n");

        System.out.println(codeOwners);

        // The Code Owners for the README.md in the root directory are @user1, @user2, and @user3.
        assertEquals(
                Arrays.asList("@user1", "@user2", "@user3"),
                codeOwners.getAllApprovers("README.md"));

        // The Code Owners for internal/README.md are @user4 and @user3.
        assertEquals(
                Arrays.asList("@user3", "@user4"),
                codeOwners.getAllApprovers("internal/README.md"));
    }

//    @Test
//    void gitlabSectionsDefaults() {
//
//        CodeOwners codeOwners = new CodeOwners(
//                "[Documentation] @docs-team\n" +
//                "docs/\n" +
//                "README.md\n" +
//                "\n" +
//                "[Database] @database-team\n" +
//                "model/db/\n" +
//                "config/db/database-setup.md @docs-team");
//
//        // The Code Owners for the README.md in the root directory are @user1, @user2, and @user3.
//        assertEquals(
//                Arrays.asList("@user1", "@user2", "@user3"),
//                codeOwners.getAllApprovers("README.md"));
//
//        // The Code Owners for internal/README.md are @user4 and @user3.
//        assertEquals(
//                Arrays.asList("@user3", "@user4"),
//                codeOwners.getAllApprovers("internal/README.md"));
//
//    }


//    void gitlabSectionsOptional() {
//
//
//    }

    private void assertOwners(CodeOwners codeOwners, String filename, String... expectedOwners) {
        assertEquals(
                Arrays.asList(expectedOwners),
                codeOwners.getAllApprovers(filename));
    }


}
