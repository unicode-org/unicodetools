package org.unicode.unittest;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

public class TestFmwkMinusMinus {
    public List<String> errLines = new LinkedList<>();
    public Logger logger = Logger.getLogger(getClass().getSimpleName());

    @BeforeEach
    public void setUp() {
        errLines.clear();
    }

    @AfterEach
    public void tearDown() {
        Assertions.assertEquals(0, errLines.size(), "Expected no errors");
    }


    /**
     * Collect an error and keep going
     * @param s
     */
    public void errln(String s) {
        errLines.add(s);
        logger.severe(s);
    }

    public void logln(String s) {
        logger.info(s);
    }

    boolean assertEquals(String msg, Object a, Object b) {
        Assertions.assertEquals(a, b, msg);
        return true;
    }
}
