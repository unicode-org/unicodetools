package org.unicode.jsptest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibm.icu.impl.UnicodeRegex;
import com.ibm.icu.text.UnicodeSet;
import org.junit.jupiter.api.Test;
import org.unicode.jsp.UnicodeJsp;
import org.unicode.jsp.UnicodeSetUtilities;

public class TestUnicodeRegex {
    @Test
    public void TestPropertyData() {
        UnicodeRegex regex = UnicodeSetUtilities.getUnicodeRegex();
        assertTrue(new UnicodeSet(regex.transform("\\p{Udev:Other_ID_Start}")).contains(0x2118));
    }

    @Test
    public void TestRandomGenerationPropertyData() {
        assertEquals(
                "<p>\u2118</p>", UnicodeJsp.getBnf("[\\p{Udev:Other_ID_Start}&[\\u2118]]", 1, 1));
    }
}
