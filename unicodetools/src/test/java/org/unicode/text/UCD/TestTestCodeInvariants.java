package org.unicode.text.UCD;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestTestCodeInvariants {
    @Test
    void testScriptExtensions() {
        int rc = TestCodeInvariants.testScriptExtensions();
        assertEquals(0, rc, "Invariant test for Script_Extensions failed!");
    }

    @Test
    void testGcbInDecompositions() {
        int rc = TestCodeInvariants.testScriptExtensions();
        assertEquals(0, rc, "Invariant test for GCB in canonical decompositions failed!");
    }
}
