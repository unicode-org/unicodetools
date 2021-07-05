package org.unicode.tools.emoji.unittest;

import org.unicode.cldr.util.UnicodeSetPrettyPrinter;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.TestLog;
import com.ibm.icu.text.UnicodeSet;

public class Asserts {
    // TODO move these into common place
    
    public static void assertEqualsUS(TestLog log, String message, String s1Name, UnicodeSet s1, String s2Name, UnicodeSet s2) {
        if (s1.equals(s2)) {
            return;
        }
        assertContains(log, message, s1Name, s1, s2Name, s2);
        assertContains(log, message, s2Name, s2, s1Name, s1);
    }

    public static void assertContains(TestLog log, String message, String s1Name, UnicodeSet s1, String s2Name, UnicodeSet s2) {
        if (!s1.containsAll(s2)) {
            UnicodeSet s2minuss1 = new UnicodeSet(s2).removeAll(s1);
            log.errln(message + ", " + s1Name + " ⊉ " + s2Name + ": " + s2minuss1.toPattern(false) + " ≠ ∅");
        }
    }
    
    public static void assertContains(TestLog log, String message, String s1Name, UnicodeSet s1, String s2Name, String s2char) {
        if (!s1.contains(s2char)) {
            log.errln(message + ", " + s1Name + " ∌ " + s2Name + ": " + s2char + ", " + Utility.hex(s2char));
        }
    }

    public static String flat(UnicodeSet source) {
        UnicodeSetPrettyPrinter pp = new UnicodeSetPrettyPrinter().setCompressRanges(false);
        return pp.format(source);
    }
}
