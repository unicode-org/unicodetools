package org.unicode.tools;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;

import org.junit.jupiter.api.Test;
import org.unicode.unittest.TestFmwkMinusMinus;

public class AacCheckTest extends TestFmwkMinusMinus {

    @Test
    public void testBasic() {
        Output<String> message = new Output<>();
        Object[][] tests = {
                // input, expected
                {"a", AacCheck.OK, "a"},
                {" a ", AacCheck.OK, "leading/trailing space"},
                {"a\uFE0F", AacCheck.OK, "trailing variation selector"},
                {"#⃣", AacCheck.OK, "emoji keycap sequence"},
                {"🇦🇨", AacCheck.OK, "emoji flag sequence"},
                {"☝🏻", AacCheck.OK, "emoji modifier sequence"},
                {"👩‍❤️‍💋‍👨", AacCheck.OK, "emoji zwj sequence"},
                {"a\u20E3", AacCheck.NOT_REGISTRATABLE, "bad keycap sequence"},
                {"🇦🇦", AacCheck.NOT_REGISTRATABLE, "bad flag sequence"},
                {"😀🏻", AacCheck.NOT_REGISTRATABLE, "bad modifier sequence"},
                {"👨\u200D🍔", AacCheck.NOT_REGISTRATABLE, "bad emoji zwj sequence"},
                {"ab", AacCheck.NOT_REGISTRATABLE, "bad letter sequence"},
                {"", AacCheck.TOO_FEW_CODEPOINTS, "no characters"},
                {" ", AacCheck.NOT_REGISTRATABLE, "space"},
                {"\u0001", AacCheck.NOT_REGISTRATABLE, "control-A"},
                {"[࿕-࿘ 卍 卐]", AacCheck.NOT_REGISTRATABLE, "swastikas"},
        };
        for (Object[] test : tests) {
            String testCase = (String) test[0];
            int expected = (int) test[1];
            if (UnicodeSet.resemblesPattern(testCase, 0)) {
                for (String subcase : new UnicodeSet(testCase)) {
                    String hexSpaceDelimited = Utility.hex(subcase, 1, " ");
                    int result = AacCheck.process(message, hexSpaceDelimited);
                    assertEquals(test[2] + ": " + message.value, expected, result);
                }
            } else {
                String hexSpaceDelimited = Utility.hex(testCase, 1, " ");
                int result = AacCheck.process(message, hexSpaceDelimited);
                assertEquals(test[2] + ": " + message.value, expected, result);
            }
        }
    }

    @Test
    public void testAllSuccess() {
        Output<String> message = new Output<>();
        for (String allowed : AacCheck.ALLOWED) {
            String hexSpaceDelimited = Utility.hex(allowed, 1, " ");
            int result = AacCheck.process(message, hexSpaceDelimited);
            assertEquals(message.value, 0, result);
        }
    }

    @Test
    public void testAllSingleFailures() {
        Output<String> message = new Output<>();
        for (String allowed : new UnicodeSet(0,0x10FFFF).removeAll(AacCheck.ALLOWED)) {
            String hexSpaceDelimited = Utility.hex(allowed, 1, " ");
            int result = AacCheck.process(message, hexSpaceDelimited);
            assertNotEquals(message.value, 0, result);
        }
    }

}
