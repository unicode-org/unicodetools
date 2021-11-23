package org.unicode.unittest;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
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
        Assertions.assertEquals(0, errLines.size(), "errln()\n" + String.join("\n", errLines));
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
        logger.fine(s);
    }

    public void warnln(String s) {
        logger.warning(s);
    }

    public boolean assertEquals(String msg, Object a, Object b) {
        Assertions.assertEquals(a, b, msg);
        return true;
    }

    public boolean assertEquals(String msg, int a, int b) {
        Assertions.assertEquals(a, b, msg);
        return true;
    }

    public boolean assertEquals(String msg, long a, long b) {
        Assertions.assertEquals(a, b, msg);
        return true;
    }

    public boolean assertNotEquals(String msg, Object a, Object b) {
        Assertions.assertNotEquals(a, b, msg);
        return true;
    }
    public boolean assertNotNull(String msg, Object a) {
        Assertions.assertNotNull(a, msg);
        return true;
    }

    public boolean assertTrue(String msg, boolean a) {
        Assertions.assertTrue(a, msg);
        return true;
    }

    public boolean assertFalse(String msg, boolean a) {
        Assertions.assertFalse(a, msg);
        return true;
    }

    public boolean isVerbose() {
        return logger.isLoggable(Level.FINE);
    }

    public static final int LOG = 0;
    public static final int WARN = 1;
    public static final int ERR = 2;

    public void msg(String message, int level, boolean incCount, boolean newln) {
        if (level == ERR) {
            errln(message);
        } else if(level == WARN) {
            warnln(message);
        } else {
            logln(message);
        }
    }

    static final private boolean LOG_KNOWN_ISSUE = Boolean.parseBoolean(System.getProperty("LOG_KNOWN_ISSUE", "true"));

    protected boolean logKnownIssue(String a, String b) {
        if (LOG_KNOWN_ISSUE == true) {
            System.err.println("-DLOG_KNOWN_ISSUE=true (set to false to fail): " + a + ", "+ b);
            return true;
        } else {
            return false;
        }
    }
}
