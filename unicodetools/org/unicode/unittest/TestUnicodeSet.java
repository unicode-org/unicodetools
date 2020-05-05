package org.unicode.unittest;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.props.UnicodePropertySymbolTable;
import org.unicode.idna.GenerateIdnaTest;
import org.unicode.text.utility.UnicodeSetParser;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.UnicodeRegex;
import com.ibm.icu.text.UnicodeSet;

public class TestUnicodeSet extends TestFmwk {
    public static void main(String[] args) {
        GenerateIdnaTest.setUnicodeVersion();
        new TestUnicodeSet().run(args);
    }

    public void TestAge() {
        checkOrder("3.1", "3.2", -1);
        checkOrder("3.2", "3.2", 0);
        checkOrder("4.0", "3.2", 1);
        checkOrder("10.0", "3.2", 1);
        checkOrder("11.0", "3.2", 1);
        checkOrder("NA", "11.0", 1);
        
        final UnicodeSet U32 = new UnicodeSet("[:age=3.2:]").freeze();
        if (!U32.contains(0x01F6)
                || !U32.contains(0x0220)
                ) { 
            throw new IllegalArgumentException("U32 should contain 3.0 and 3.2 characters");
        }
        if (U32.contains(0x0221)
                || U32.contains(0x0560)
                ) {
            throw new IllegalArgumentException("U32 should not contain 4.0 and 11.0 characters");
        }
    }

    private void checkOrder(String d1, String d2, int expected) {
        assertEquals(d1 + " ?< " + d2, expected, 
                UnicodePropertySymbolTable.DOUBLE_STRING_COMPARATOR.compare(d1, d2));
        assertEquals(d2 + " ?< " + d1, -expected, 
                UnicodePropertySymbolTable.DOUBLE_STRING_COMPARATOR.compare(d2, d1));
    }
    
    public void TestHexParser() {
        String[][] tests = {
                {"[☛]", "☛\uFE0F"},
                {"[ab]", "U+0061U+0062"},
                {"[🅐-🅩☛]", "🅐..🅩☛"},
                {"[🅐-🅩☛]", " 🅐 - 🅩 , ☛ "},
                {"Second of range must be greater, at 8: ≠", "🅐-🅩☛-≠"},
                {"[ab]", "0061 0062"},
                {"[ab]", "\\u0061\\u0062"},
                {"[ab]", "\\u{61}\\u{62}"},
                {"[ab]", "\\x61\\x62"},
                {"[ab]", "\\x{61}\\x{62}"},
                {"[ab]", "\\U000061\\U000062"},
        };
        UnicodeSetParser hexParser = new UnicodeSetParser(false);
        UnicodeSet target = new UnicodeSet();
        for (String[] test : tests) {
            final String expectedString = test[0];
            UnicodeSet parsed = null;
            Exception error = null;
            try {
                parsed = hexParser.parse(test[1], target);
            } catch (Exception e) {
                error = e;
            }
            if (test[0].startsWith("[") && parsed != null) {
                assertEquals(test[1], new UnicodeSet(expectedString), parsed);
            } else {
                assertEquals(test[1], expectedString, error == null ? parsed.toPattern(false) : error.getMessage());
            }
        }
    }
    // https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
    // Unicode scripts, blocks, categories and binary properties are written with the \p and \P constructs as in Perl. 
    // \p{prop} matches if the input has the property prop, while \P{prop} does not match if the input has that property.
    public void TestJavaRegexProps () {
        final UnicodeSet DI = new UnicodeSet("\\p{di}");
        for (String s : DI) {
            Pattern pat = Pattern.compile(s + "a");
            Matcher m = pat.matcher("a");
            if (m.matches()) {
                errln("Ignores literal " + Utility.hex(s) + ": " + m.toMatchResult());
            };
        }
    }
    public void TestJavaRegexProps2 () {
        String[] tests = {"[\\x{FE0F}]", "\\p{di}"};
        for (String test : tests) {
            final UnicodeSet DI = new UnicodeSet(test);
            Pattern pat = Pattern.compile(UnicodeRegex.fix("((?:\\h|[:di:])+|,)x"));
            for (String s : DI) {
                Matcher m = pat.matcher(s+"x");
                if (!m.matches()) {
                    errln("Ignores literal " + Utility.hex(s) + ": " + m.toMatchResult());
                };
            }
        }
    }
}
