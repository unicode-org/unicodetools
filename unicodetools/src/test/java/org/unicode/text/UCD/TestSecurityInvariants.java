package org.unicode.text.UCD;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;

public class TestSecurityInvariants {
    @Test
    void testSecurityInvariants() throws IOException {
        int rc =
                TestUnicodeInvariants.testInvariants("SecurityInvariantTest.txt", "security", true);
        assertEquals(0, rc, "TestUnicodeInvariants.testInvariants(security) failed");
    }
}
