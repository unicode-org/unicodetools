package org.unicode.test;

import java.text.ParsePosition;

import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.text.UCD.UnicodeMapParser;
import org.unicode.text.UCD.UnicodeMapParser.ValueParser;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Objects;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.text.UnicodeSet;

public class TestUnicodeMapParser extends TestFmwkPlus{

    private static final IndexUnicodeProperties INDEX_PROPS = IndexUnicodeProperties.make(Settings.latestVersion);
    private static final IndexUnicodeProperties LAST_INDEX_PROPS = IndexUnicodeProperties.make(Settings.lastVersion);
    private static final UnicodeProperty NAME = INDEX_PROPS.getProperty("name");
    private static final UnicodeProperty AGE = INDEX_PROPS.getProperty("age");

    public static void main(String[] args) {
        new TestUnicodeMapParser().run(args);
    }

    private UnicodeMapParser<String> ump = UnicodeMapParser.create(
            UnicodeMapParser.STRING_VALUE_PARSER, 
            INDEX_PROPS, LAST_INDEX_PROPS);

    public void testBasic() {
        UnicodeMap<String> expected = new UnicodeMap<String>()
                .put('a', " ")
                .put('c', "d")
                .putAll(new UnicodeSet("[:whitespace:]"),"x");

        String test = "{\\u{61}=\\u{20},c=,[:whitespace:]=x}";
        check(ump, test, null, 17);


        test = " { a = \\u{20} , c = d , [:whitespace:] = x } ";
        check(ump, test, expected, -1);


        ValueParser<Integer> integerParser = new IntegerParser();
        UnicodeMapParser<Integer> ump2 = UnicodeMapParser.create(integerParser, INDEX_PROPS);
        UnicodeMap<Integer> expected2 = new UnicodeMap<Integer>().put('a', 1)
                .put('c', 2)
                .putAll(new UnicodeSet("[:whitespace:]"), 33);

        String test2 = "{a=1,c=2,[:whitespace:]=33}";
        check(ump2, test2, expected2, -1);

        test2 = " { a = 1 , c = 2 , [:whitespace:] = 33 } ";
        check(ump2, test2, expected2, -1);
    }

    public void testUnihan() {
        UnicodeMapParser<String> ump = UnicodeMapParser.create(
                UnicodeMapParser.STRING_VALUE_PARSER, 
                INDEX_PROPS);
        UnicodeMap<String> expected = new UnicodeMap<String>()
                .putAll(INDEX_PROPS.getProperty("kRSUnicode").getUnicodeMap());

        String test = "{\\m{kRSUnicode}}";
        check(ump, test, expected, -1);
    }

    public void testAProperty() {
        UnicodeMap<String> expected = new UnicodeMap<String>()
                .putAll(INDEX_PROPS.getProperty("Script").getUnicodeMap())
                .put('a', "HUH?");

        String test = "{\\m{script},a=HUH?}";
        check(ump, test, expected, -1);
    }


    public void testStability() {
        String test1 = "{\\m{Simple_Titlecase_Mapping}-\\p{Age=7.0}}";
        String test2 = "{\\m{*Simple_Titlecase_Mapping}-\\p{Age=7.0}}";
        areEqual(ump, test1, test2);
    }

    public void testRetain2() {
        String test1 = "{a=b, c=d, q=r}";
        String test2 = "{a=b, e=f, q=s}";
        areEqual(ump, test1, test2);
    }

    public void testEmbedding() {
        UnicodeMap<String> expected = new UnicodeMap<String>()
                .put('a', "b");

        String test = "{a=b,c=d&{q=r,a=b}}";
        check(ump, test, expected, -1);
    }

    public void testRemove() {
        UnicodeMap<String> expected = new UnicodeMap<String>()
                .putAll(INDEX_PROPS.getProperty("Script").getUnicodeMap())
                .putAll(0x100,0x10FFFF, null);
        String test = "{\\m{script}&[\\u0000-\\u00FF]}";
        check(ump, test, expected, -1);
    }

    public void testRetain() {
        UnicodeMap<String> expected = new UnicodeMap<String>()
                .putAll(INDEX_PROPS.getProperty("Script").getUnicodeMap())
                .putAll(0,0xFF, null);
        String test = "{\\m{script}-[\\u0000-\\u00FF]}";
        check(ump, test, expected, -1);
    }

    static class IntegerParser implements UnicodeMapParser.ValueParser<Integer> {
        @Override
        public Integer parse(String source, ParsePosition pos) {
            int result = 0;
            for (int i = pos.getIndex(); i < source.length(); ++i) {
                int cp = source.charAt(i);
                if (cp < '0' || cp > '9') {
                    if (i == pos.getIndex()) {
                        pos.setErrorIndex(i);
                    }
                    pos.setIndex(i);
                    break;
                }
                result *= 10;
                result += (cp - '0');
            }
            return result;
        }
        public Integer parse(String source) {
            return Integer.parseInt(source, 16);
        }
    }

    private <V> void check(UnicodeMapParser<V> ump, String test, UnicodeMap<V> expected, int expectedError) {
        ParsePosition end = new ParsePosition(0);
        UnicodeMap<V> um = ump.parse(test, end);
        if (!assertEquals(test + " errorIndex:", expectedError, end.getErrorIndex())) {
            showError(test, end.getErrorIndex());
        } else if (end.getErrorIndex() >= 0) {
            // nothing
        } else if (!assertEquals(test + " consumed all:", test.length(), end.getIndex())) {
            showError(test, end.getIndex());
        } else {
            assertEquals(test, expected, um);
        }
    }

    public void showError(String test, int end) {
        if (end >= 0) {
            errln(test.substring(0,end) + "$$" + test.substring(end));
        }
    }

    private <V> void areEqual(UnicodeMapParser<V> ump, String test1, String test2) {
        ParsePosition end = new ParsePosition(0);
        UnicodeMap<V> um1 = ump.parse(test1, end);
        if (!assertEquals("errorIndex:", -1, end.getErrorIndex())) {
            showError(test1, end.getErrorIndex());
            return;
        } else if (!assertEquals("consumed all:", test1.length(), end.getIndex())) {
            showError(test1, end.getIndex());
            return;
        }
        end.setIndex(0);
        UnicodeMap<V> um2 = ump.parse(test2, end);
        if (!assertEquals("errorIndex:", -1, end.getErrorIndex())) {
            showError(test2, end.getErrorIndex());
            return;
        } else if (!assertEquals("consumed all:", test2.length(), end.getIndex())) {
            showError(test2, end.getIndex());
            return;
        }
        if (!um1.equals(um2)) {
            UnicodeSet allKeys = new UnicodeSet(um1.keySet()).addAll(um2.keySet());
            for (String s : allKeys) {
                V v1 = um1.get(s);
                V v2 = um2.get(s);
                if (!Objects.equal(v1, v2)) {
                    errln(getCodeAndName(s) + " => " + v1 + " != " + v2);
                }
            }
//            final UnicodeMap um1MinusUm2 = UnicodeMapParser.removeAll(new UnicodeMap().putAll(um1), um2);
//            errln("In " + test1 + " but not " + test2 + "\n\t" 
//            + um1MinusUm2.toString());
//            errln("In " + test1 + " and in " + test2 + "\n\t" 
//                    + UnicodeMapParser.retainAll(new UnicodeMap().putAll(um1), um2).toString());
//            errln("Not in " + test1 + " but in " + test2 + "\n\t" 
//                    + UnicodeMapParser.removeAll(new UnicodeMap().putAll(um2), um1).toString());
        }
    }

    private String getCodeAndName(String s) {
        return "U+" + Utility.hex(s, ", U+") 
                + " " + AGE.getValue(s.codePointAt(0)) 
                + " (" + s + ") " 
                + NAME.getValue(s.codePointAt(0));
    }
}
