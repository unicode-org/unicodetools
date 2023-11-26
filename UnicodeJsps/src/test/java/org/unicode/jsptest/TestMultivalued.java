package org.unicode.jsptest;

import com.ibm.icu.text.UnicodeSet;
import org.junit.jupiter.api.Test;
import org.unicode.jsp.UnicodeSetUtilities;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestMultivalued extends TestFmwkMinusMinus {
    @Test
    public void TestScx1Script() {
        // As of 2023-11-24, scx was not working properly
        String unicodeSetString = "\\p{scx=deva}";
        UnicodeSet parsed = UnicodeSetUtilities.parseUnicodeSet(unicodeSetString);

        UnicodeSet mustContain = new UnicodeSet("[ᳵ।]"); // one character B&D, other B&D&D&G&...
        assertTrue(unicodeSetString + " contains " + mustContain, parsed.containsAll(mustContain));

        UnicodeSet mustNotContain = new UnicodeSet("[ক]"); // one Bengali character
        assertFalse(
                unicodeSetString + " !contains " + mustNotContain,
                parsed.containsAll(mustNotContain));
    }

    @Test
    public void TestScxMulti() {
        // As of 2023-11-24, scx was not working properly
        String unicodeSetString = "\\p{scx=beng,deva}";
        String exceptionMessage = null;
        try {
            UnicodeSet parsed = UnicodeSetUtilities.parseUnicodeSet(unicodeSetString);
        } catch (Exception e) {
            exceptionMessage = e.getMessage();
        }
        assertEquals(
                "Expected exception",
                "Multivalued property values can't contain commas.",
                exceptionMessage);
    }

    @Test
    public void TestExemplars() {
        String unicodeSetString = "\\p{exem=da}";
        UnicodeSet parsed = UnicodeSetUtilities.parseUnicodeSet(unicodeSetString);

        UnicodeSet mustContain = new UnicodeSet("[æ]");
        assertTrue(unicodeSetString + " contains " + mustContain, parsed.containsAll(mustContain));

        UnicodeSet mustNotContain = new UnicodeSet("[ç]");
        assertFalse(
                unicodeSetString + " !contains " + mustNotContain,
                parsed.containsAll(mustNotContain));
    }
}
