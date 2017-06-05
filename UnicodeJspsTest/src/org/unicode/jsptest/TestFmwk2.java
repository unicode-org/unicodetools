package org.unicode.jsptest;

import org.unicode.jsp.UnicodeUtilities;

import com.ibm.icu.dev.test.AbstractTestLog;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.UnicodeSet;

public class TestFmwk2 extends TestFmwk {
    
    public void checkContained(final String setPattern, final String containedPattern) {
        checkContained(setPattern, containedPattern, true);
    }

    public void checkContained(final String setPattern, final String containedPattern, boolean expected) {
        String[] message = {""};
        UnicodeSet container = UnicodeUtilities.parseSimpleSet(setPattern, message);
        UnicodeSet contained = UnicodeUtilities.parseSimpleSet(containedPattern, message);
        if (container == null) {
            errln(setPattern + " fails to parse");
        } else if (contained == null) {
            errln(containedPattern + " fails to parse");
        } else if (container.containsAll(contained) != expected) {
                errln(toPattern(setPattern, container) + " doesn't contain " + toPattern(containedPattern, contained));
        } else {
            logln(toPattern(setPattern, container) + " contains " + toPattern(containedPattern, contained));
        }
    }

    private static String toPattern(String title, UnicodeSet primary) {
        return primary == null ? title : primary.toPattern(false);
    }
    
    @Override
    public void msg(String message, int level, boolean incCount, boolean newln) {
        super.msg(message.length() > 200 ? message.substring(0,200) + "…" : message, level, incCount, newln);
    }

    public static boolean assertEquals(AbstractTestLog testFmwk, String test, UnicodeSet expected, UnicodeSet actual) {
        if (!expected.equals(actual)) {
            UnicodeSet inExpected = new UnicodeSet(expected).removeAll(actual);
            UnicodeSet inActual = new UnicodeSet(actual).removeAll(expected);
            testFmwk.errln(test + " — \tMISSING: " + inExpected + ", \tEXTRA: " + inActual);
            return false;
        } else {
            testFmwk.logln("OK\t\t" + test);
            return true;
        }
    }

    public static boolean assertContains(AbstractTestLog testFmwk, String test, UnicodeSet expectedSubset, UnicodeSet actual) {
        if (!actual.containsAll(expectedSubset)) {
            UnicodeSet inExpected = new UnicodeSet(expectedSubset).removeAll(actual);
            UnicodeSet has = new UnicodeSet(actual).removeAll(expectedSubset);
            testFmwk.errln(test + " missing: " + toPattern("?", inExpected) + ", has: " + toPattern("*", has));
            return false;
        } else {
            testFmwk.logln("OK\t\t" + test);
            return true;
        }
    }

    public boolean assertEquals(String test, UnicodeSet expected, UnicodeSet actual) {
        return assertEquals(this, test, expected, actual);
    }
    
    public boolean assertContains(String test, UnicodeSet expectedSubset, UnicodeSet actual) {
        return assertContains(this, test, expectedSubset, actual);
    }

    public void assertContains(String test, String expectedSubset, String actual) {
        if (!actual.contains(expectedSubset)) {
            errln(test + " - MISSING: «" + expectedSubset + "» in «" + actual + "»");
        } else {
            logln("OK\t\t" + test);
        }
    }
}
