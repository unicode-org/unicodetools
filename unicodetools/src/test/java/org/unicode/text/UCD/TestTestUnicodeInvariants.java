package org.unicode.text.UCD;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.ParsePosition;
import org.junit.jupiter.api.Test;
import org.unicode.text.UCD.TestUnicodeInvariants.BackwardParseException;
import org.unicode.text.utility.Settings;

public class TestTestUnicodeInvariants {
    @Test
    void testSRC_UCD_DIR() {
        assertAll(
                "assert that no components of Settings.SRC_UCD_DIR are null",
                () -> assertNotNull(Settings.SRC_UCD_DIR, "Settings.SRC_UCD_DIR"),
                () -> assertNotNull(Settings.SRC_DIR, "Settings.SRC_DIR"),
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
        final var directory = new File(Settings.SRC_DIR + "UCD/AdditionComparisons/");
        if (!directory.exists()) {
            throw new IOException(directory.getAbsolutePath() + " does not exist");
        }
        int rc = 0;
        for (var file : directory.listFiles()) {
            final String filename = file.getName();
            if (!file.getName().endsWith(".txt")) {
                continue;
            }
            final String nameWithoutExtension = filename.substring(0, filename.length() - 4);
            rc +=
                    TestUnicodeInvariants.testInvariants(
                            "AdditionComparisons/" + filename,
                            "addition-comparisons-" + nameWithoutExtension,
                            true);
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
        assertEquals("No character matching \\N escape", thrown.getMessage());
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
