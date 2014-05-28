package org.unicode.test;

import java.text.ParsePosition;

import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.text.UCD.UnicodeMapParser;
import org.unicode.text.UCD.UnicodeMapParser.ValueParser;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class TestUnicodeMapParser extends TestFmwkPlus{
    public static void main(String[] args) {
        new TestUnicodeMapParser().run(args);
    }
    
    public void testBasic() {
        UnicodeMapParser<String> ump = UnicodeMapParser.create();
        UnicodeMap<String> expected = new UnicodeMap<String>().put('a', "b")
                .put('c', "d")
                .putAll(new UnicodeSet("[:whitespace:]"),"x");

        String test = "{\\u{61}=\\u{20},c=,[:whitespace:]=x}";
        check(ump, expected, test);
        
        test = " { a = b , c = d , [:whitespace:] = x } ";
        check(ump, expected, test);
        

        ValueParser<Integer> integerParser = new IntegerParser();
        UnicodeMapParser<Integer> ump2 = UnicodeMapParser.create(integerParser);
        UnicodeMap<Integer> expected2 = new UnicodeMap<Integer>().put('a', 1)
                .put('c', 2)
                .putAll(new UnicodeSet("[:whitespace:]"), 33);
        
        String test2 = "{a=1,c=2,[:whitespace:]=33}";
        check(ump2, expected2, test2);
        
        test2 = " { a = 1 , c = 2 , [:whitespace:] = 33 } ";
        check(ump2, expected2, test2);
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
        
    }

    public <V> void check(UnicodeMapParser<V> ump, UnicodeMap<V> expected, String test) {
        ParsePosition end = new ParsePosition(0);
        UnicodeMap<V> um = ump.parse(test, end);
        if (end.getErrorIndex() >= 0) {
            errln(test.substring(0,end.getErrorIndex()) + "$$" + test.substring(end.getErrorIndex()));
        } else {
            assertEquals("", expected, um);
        }
    }
}
