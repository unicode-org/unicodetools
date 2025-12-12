package org.unicode.text.UCD;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.ParsePosition;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.text.UCD.TestUnicodeInvariants.BackwardParseException;
import org.unicode.text.utility.Settings;

public class TestTestUnicodeInvariants {
    @Test
    void testSRC_UCD_DIR() {
        assertAll(
                "assert that no components of Settings.SRC_UCD_DIR are null",
                () ->
                        assertNotNull(
                                Settings.UnicodeTools.UNICODETOOLS_RSRC_DIR,
                                "Settings.UnicodeTools.UNICODETOOLS_RSRC_DIR"),
                () ->
                        assertNotNull(
                                Settings.UnicodeTools.UNICODETOOLS_DIR,
                                "Settings.UnicodeTools.UNICODETOOLS_DIR"),
                () ->
                        assertNotNull(
                                Settings.UnicodeTools.UNICODETOOLS_REPO_DIR,
                                "Settings.UnicodeTools.UNICODETOOLS_REPO_DIR"));
    }

    @Test
    void testUnicodeInvariants() throws IOException {
        int rc = TestUnicodeInvariants.testInvariants(null, null, true);
        assertEquals(0, rc, "TestUnicodeInvariants.testInvariants(default) failed");
    }

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

    @Test
    void testSecurityInvariants() throws IOException {
        int rc =
                TestUnicodeInvariants.testInvariants("SecurityInvariantTest.txt", "security", true);
        assertEquals(0, rc, "TestUnicodeInvariants.testInvariants(security) failed");
    }

    @Test
    void testUnicodeSetParsing() throws ParseException {
        assertEquals(
                26,
                TestUnicodeInvariants.parseUnicodeSet(
                                "TEST [\\N{LATIN SMALL LETTER A}-\\N{LATIN SMALL LETTER Z}]",
                                new ParsePosition(5))
                        .size());
        ParseException thrown =
                assertThrows(
                        ParseException.class,
                        () ->
                                TestUnicodeInvariants.parseUnicodeSet(
                                        "TEST [\\N{MEOW}]", new ParsePosition(5)));
        assertEquals("No character name nor name alias matches MEOW", thrown.getMessage());
        assertEquals("TEST [".length(), thrown.getErrorOffset());
        thrown =
                assertThrows(
                        BackwardParseException.class,
                        () ->
                                TestUnicodeInvariants.parseUnicodeSet(
                                        "TEST [[a-z]-\\N{LATIN SMALL LETTER Z}]",
                                        new ParsePosition(5)));
        assertEquals("Error: Set expected after operator", thrown.getMessage());
        assertEquals("TEST [[a-z]-.N{LATIN SMALL LETTER Z}".length(), thrown.getErrorOffset());
    }
}
