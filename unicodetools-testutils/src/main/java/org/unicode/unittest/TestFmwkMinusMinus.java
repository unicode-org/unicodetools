package org.unicode.unittest;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.ibm.icu.text.UnicodeSet;

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

    // from TestJsp
    public void assertContains(String stuff, String string) {
        if (!stuff.contains(string)) {
            errln(string + " not contained in " + stuff);
        }
    }

    // from jsp's TestFmwk2
    public boolean assertContains(String test, UnicodeSet expectedSubset, UnicodeSet actual) {
        if (!actual.containsAll(expectedSubset)) {
            UnicodeSet inExpected = new UnicodeSet(expectedSubset).removeAll(actual);
            UnicodeSet has = new UnicodeSet(actual).removeAll(expectedSubset);
            errln(test + " missing: " + toPattern("?", inExpected) + ", has: " + toPattern("*", has));
            return false;
        } else {
            logln("OK\t\t" + test);
            return true;
        }
    }

    public void assertContains(String test, String expectedSubset, String actual) {
        if (!actual.contains(expectedSubset)) {
            errln(test + " - MISSING: «" + expectedSubset + "» in «" + actual + "»");
        } else {
            logln("OK\t\t" + test);
        }
    }

    // public boolean assertEquals(String test, UnicodeSet expected, UnicodeSet actual) {
    //     if (!expected.equals(actual)) {
    //         UnicodeSet inExpected = new UnicodeSet(expected).removeAll(actual);
    //         UnicodeSet inActual = new UnicodeSet(actual).removeAll(expected);
    //         errln(test + " — \tMISSING: " + inExpected + ", \tEXTRA: " + inActual);
    //         return false;
    //     } else {
    //         logln("OK\t\t" + test);
    //         return true;
    //     }
    // }

    public boolean assertEquals(String message, UnicodeSet expected, UnicodeSet actual) {
        if (expected.equals(actual)) {
            logln(message + expected.toPattern(false) + "==" + actual.toPattern(false));
            return true;
        } else {
            final UnicodeSet missing = new UnicodeSet(expected).removeAll(actual);
            if (missing.size() != 0) {
                errln(message + " missing: " + missing.toPattern(false));
            }
            final UnicodeSet unexpected = new UnicodeSet(actual).removeAll(expected);
            if (unexpected.size() != 0) {
                errln(message + " unexpected: " + unexpected.toPattern(false));
            }
            errln(message + expected.toPattern(false) + "!=" + actual.toPattern(false));
            return false;
        }
    }

    public static String toPattern(String title, UnicodeSet primary) {
        return primary == null ? title : primary.toPattern(false);
    }

    /**
     * Copied from TestFmwk. Low level assertion.
     */
    public boolean handleAssert(boolean result, String message,
    Object expected, Object actual, String relation, boolean flip) {
        if (!result || isVerbose()) {
            if (message == null) {
                message = "";
            }
            if (!message.equals("")) {
                message = ": " + message;
            }
            relation = relation == null ? ", got " : " " + relation + " ";
            if (result) {
                logln("OK " + message + ": "
                        + (flip ? expected + relation + actual : expected));
            } else {
                // assert must assume errors are true errors and not just warnings
                // so cannot warnln here
                errln(  message
                        + ": expected"
                        + (flip ? relation + expected : " " + expected
                                + (actual != null ? relation + actual : "")));
            }
        }
        return result;
    }

    private final static Integer inclusion = Integer.parseInt(System.getProperty("UNICODETOOLS_INCLUSION", "5"));
    private final static Boolean verbose = Boolean.parseBoolean(System.getProperty("UNICODETOOLS_VERBOSE", "false"));
    static {
        System.err.println("UNICODETOOLS_INCLUSION=" + inclusion);
        System.err.println("UNICODETOOLS_VERBOSE=" + verbose);
    }
    /**
     * 0 = fewest tests, 5 is normal build, 10 is most tests
     */
    public int getInclusion() {
        return inclusion;
    }

    public boolean getVerbose() {
        return verbose;
    }

}
