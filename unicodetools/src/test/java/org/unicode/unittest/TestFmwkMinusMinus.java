package org.unicode.unittest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestFmwkMinusMinus {
    public List<String> errLines = new LinkedList<>();
    public Logger logger = Logger.getLogger(getClass().getSimpleName());

    @BeforeEach
    public void setUp() {
        errLines.clear();
    }

    @AfterEach
    public void tearDown() {
        assertEquals(0, errLines.size(), "Expected no errors");
    }


    /**
     * Collect an error and keep going
     * @param s
     */
    void errln(String s) {
        errLines.add(s);
        logger.severe(s);
    }

    void logln(String s) {
        logger.info(s);
    }
}
