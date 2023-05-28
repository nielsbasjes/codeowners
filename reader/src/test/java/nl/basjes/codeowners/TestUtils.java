package nl.basjes.codeowners;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtils {
    public static void assertOwners(CodeOwners codeOwners, String filename, String... expectedOwners) {
        List<String> allApprovers = codeOwners.getAllApprovers(filename);
        assertEquals(
            Arrays.stream(expectedOwners).sorted().collect(Collectors.toList()),
            allApprovers,
            "Filename \""+filename+"\" should have owners " + Arrays.toString(expectedOwners) + " but got " + allApprovers);
    }

    public static void assertOwners(String codeOwners, String filename, String... expectedOwners) {
        assertOwners(new CodeOwners(codeOwners), filename, expectedOwners);
    }


}
