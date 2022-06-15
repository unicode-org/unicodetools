package org.unicode.text.UCD;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import org.junit.jupiter.api.Test;
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
        int rc = TestUnicodeInvariants.testInvariants(null, true);
        assertEquals(0, rc, "TestUnicodeInvariants.testInvariants() failed");
    }
}
