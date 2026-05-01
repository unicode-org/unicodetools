package org.unicode.text.UCD;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.CldrUtility;

public class TestAdditionComparisons {
    @Test
    void testAdditionComparisons() throws IOException {
        final var directory = new File(getClass().getResource("AdditionComparisons").getPath());
        if (!directory.exists()) {
            throw new IOException(directory.getAbsolutePath() + " does not exist");
        }
        int rc = 0;
        // Ideally, this would be a @ParameterizedTest, and we would use the Dtest parameter of the
        // mvn command line to filter a specific test.
        // Unfortunately, this does not appear to be possible with JUnit 5 and Surefire, see
        // https://stackoverflow.com/questions/69198795/junit5-running-a-single-instance-of-parameterized-test,
        // so we have one big test and we filter by hand.
        final String rmgIssueFilter = CldrUtility.getProperty("RMG_ISSUE", null);
        boolean matchedFilter = false;
        for (var file : directory.listFiles()) {
            final String filename = file.getName();
            if (!file.getName().endsWith(".txt")) {
                continue;
            }
            final String nameWithoutExtension = filename.substring(0, filename.length() - 4);
            if (rmgIssueFilter != null) {
                if (nameWithoutExtension.equals(rmgIssueFilter)) {
                    matchedFilter = true;
                } else {
                    continue;
                }
            }
            final int errors =
                    TestUnicodeInvariants.testInvariants(
                            "AdditionComparisons/" + filename,
                            "addition-comparisons-" + nameWithoutExtension,
                            true);
            if (errors != 0) {
                System.err.println(errors + " errors in " + filename);
            }
            rc += errors;
        }
        if (rmgIssueFilter != null) {
            assertTrue(matchedFilter, "Could not find test " + rmgIssueFilter + ".txt");
        }
        assertEquals(0, rc, "TestUnicodeInvariants.testInvariants(addition-comparisons) failed");
    }
}
