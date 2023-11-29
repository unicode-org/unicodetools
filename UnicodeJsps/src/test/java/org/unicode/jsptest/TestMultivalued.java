package org.unicode.jsptest;

import com.ibm.icu.text.UnicodeSet;
import org.junit.jupiter.api.Test;
import org.unicode.jsp.UnicodeSetUtilities;
import org.unicode.jsp.XPropertyFactory;
import org.unicode.props.UnicodeProperty;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestMultivalued extends TestFmwkMinusMinus {

    UnicodeProperty exemplarProp = XPropertyFactory.make().getProperty("exemplar");
    UnicodeProperty scxProp = XPropertyFactory.make().getProperty("scx");

    @Test
    public void TestScx1Script() {
        String x = scxProp.getValue('।');

        String unicodeSetString = "\\p{scx=deva}";
        UnicodeSet parsed = UnicodeSetUtilities.parseUnicodeSet(unicodeSetString);

        UnicodeSet mustContain = new UnicodeSet("[ᳵ।]"); // one character B&D, other B&D&D&G&...
        assertTrue(unicodeSetString + " contains " + mustContain, parsed.containsAll(mustContain));

        UnicodeSet mustNotContain = new UnicodeSet("[ক]"); // one Bangla character
        assertFalse(
                unicodeSetString + " !contains " + mustNotContain,
                parsed.containsAll(mustNotContain));
    }

    @Test
    public void TestScx1ScriptB() {
        String unicodeSetString = "\\p{scx=Arab}";
        UnicodeSet parsed = UnicodeSetUtilities.parseUnicodeSet(unicodeSetString);

        UnicodeSet mustContain = new UnicodeSet("[،ء]"); // one character single script, one multi
        assertTrue(unicodeSetString + " contains " + mustContain, parsed.containsAll(mustContain));

        UnicodeSet mustNotContain = new UnicodeSet("[ক]"); // one Bangla character
        assertFalse(
                unicodeSetString + " !contains " + mustNotContain,
                parsed.containsAll(mustNotContain));
    }

    @Test
    public void TestScxMulti() {
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
        String x = exemplarProp.getValue('æ');

        String unicodeSetString = "\\p{exem=da}";
        UnicodeSet parsed = UnicodeSetUtilities.parseUnicodeSet(unicodeSetString);

        UnicodeSet mustContain = new UnicodeSet("[æ]");
        assertTrue(unicodeSetString + " contains " + mustContain, parsed.containsAll(mustContain));

        UnicodeSet mustNotContain = new UnicodeSet("[ç]");
        assertFalse(
                unicodeSetString + " !contains " + mustNotContain,
                parsed.containsAll(mustNotContain));
    }

    @Test
    public void TestEmpty() {
        assertEquals("exemplar(0x0000)", "", exemplarProp.getValue(0x0000));
        assertEquals("exemplar(α)", "el", exemplarProp.getValue('α'));

        UnicodeSet exem = UnicodeSetUtilities.parseUnicodeSet("\\p{exem}");
        assertTrue("\\p{exem} contains 0", exem.contains(0x0000));
        assertFalse("\\p{exem} contains α", exem.contains('α'));
        UnicodeSet exem3 = UnicodeSetUtilities.parseUnicodeSet("\\p{exem=el}");
        assertFalse("\\p{exem=el} contains 0", exem3.contains(0x0000));
        assertTrue("\\p{exem=el} contains α", exem3.contains('α'));

        String unicodeSetString = "[\\p{Greek}&\\p{exem}]";
        UnicodeSet parsed = UnicodeSetUtilities.parseUnicodeSet(unicodeSetString);

        String first = parsed.iterator().next();
        String firstValue = exemplarProp.getValue(first.codePointAt(0));
        assertEquals(unicodeSetString, "", firstValue);

        String unicodeSetString2 = "[\\p{Greek}&\\P{exem}]";
        UnicodeSet parsed2 = UnicodeSetUtilities.parseUnicodeSet(unicodeSetString2);

        String first2 = parsed2.iterator().next();
        String firstValue2 = exemplarProp.getValue(first2.codePointAt(0));
        assertEquals(unicodeSetString2, "el", firstValue2);

        //        UnicodeSet mustContain = new UnicodeSet("[æ]");
        //        assertTrue(unicodeSetString + " contains " + mustContain,
        // parsed.containsAll(mustContain));
        //
        //        UnicodeSet mustNotContain = new UnicodeSet("[ç]");
        //        assertFalse(
        //                unicodeSetString + " !contains " + mustNotContain,
        //                parsed.containsAll(mustNotContain));
    }
}
