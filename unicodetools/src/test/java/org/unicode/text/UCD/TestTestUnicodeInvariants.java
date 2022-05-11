package org.unicode.text.UCD;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class TestTestUnicodeInvariants {
    @Test
    void testUnicodeInvariants() throws IOException {
        int rc = TestUnicodeInvariants.testInvariants(null, true);
        assertEquals(0, rc, "TestUnicodeInvariants.testInvariants() failed");
    }
}
