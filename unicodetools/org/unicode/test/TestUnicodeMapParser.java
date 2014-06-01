package org.unicode.test;

import java.text.ParsePosition;

import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.text.UCD.UnicodeMapParser;
import org.unicode.text.UCD.UnicodeMapParser.ValueParser;
import org.unicode.text.utility.Settings;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class TestUnicodeMapParser extends TestFmwkPlus{
    
    private static final IndexUnicodeProperties INDEX_PROPS = IndexUnicodeProperties.make(Settings.latestVersion);

    public static void main(String[] args) {
        new TestUnicodeMapParser().run(args);
    }
    
    UnicodeMapParser<String> ump = UnicodeMapParser.create(
            UnicodeMapParser.STRING_VALUE_PARSER, 
            INDEX_PROPS);

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
        UnicodeMap<String> expected = new UnicodeMap<String>()
                .putAll(INDEX_PROPS.getProperty("Script").getUnicodeMap())
                .put('a', "HUH?");

        String test = "{\\m{script},a=HUH?}";
        check(ump, test, expected, -1);
    }
    
    public void testEmbedding() {
        UnicodeMap<String> expected = new UnicodeMap<String>()
                .put('a', "b")
                ;

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

    public <V> void check(UnicodeMapParser<V> ump, String test, UnicodeMap<V> expected, int expectedError) {
        ParsePosition end = new ParsePosition(0);
        UnicodeMap<V> um = ump.parse(test, end);
        if (!assertEquals("errorIndex:", expectedError, end.getErrorIndex())) {
            if (end.getErrorIndex() >= 0) {
                errln(test.substring(0,end.getErrorIndex()) + "$$" + test.substring(end.getErrorIndex()));
            }
        } else {
            assertEquals(test, expected, um);
        }
    }
}
