package org.unicode.unittest;

public class TestTestFmwk extends TestFmwkMinusMinus {
    @org.junit.jupiter.api.Test
    void Test() {
        assertContains("hay hay hay hay needle hay hay hay", "needle");
        logln("Everything is OK");
    }
}
