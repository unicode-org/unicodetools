package org.unicode.jsptest;

import com.ibm.icu.text.UnicodeSet;
import org.junit.jupiter.api.Test;
import org.unicode.jsp.UnicodeSetUtilities;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestScriptExtensions extends TestFmwkMinusMinus {
    @Test
    public void TestBasic() {
        // As of 2023-11-24, scx was not working properly
        String setA = "\\p{scx=deva}";
        UnicodeSet deva = UnicodeSetUtilities.parseUnicodeSet(setA);
        assertTrue(setA + "contains \\u1CD5", deva.contains(0x1cd5));
    }
}
