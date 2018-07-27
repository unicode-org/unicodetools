package org.unicode.tools.emoji;

import java.util.function.IntPredicate;

import org.unicode.cldr.util.With;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;

public class Hexer {
    
    static public String toHex(String string, char prefix, int minLen, IntPredicate noHex) {
        final StringBuilder result = new StringBuilder();
        boolean inBraces = false;
        for (int cp : With.codePointArray(string)) {
            if (noHex != null && noHex.test(cp)) {
                if (inBraces) {
                    result.append('}');
                    inBraces = false;
                }
                result.appendCodePoint(cp);
            } else {
                if (inBraces) {
                    result.append(' ');
                } else {
                    result.append('\\').append(prefix).append('{');
                    inBraces = true;
                }
                result.append(Utility.hex(cp, minLen));
            }
        }
        if (inBraces) {
            result.append('}');            
        }
        return result.toString();
    }
    
    /** 
     * Not optimized...
     * only handles \x{...}, not \\, etc.
     */
    static public String fromHex(String string, char prefix) {
        final StringBuilder result = new StringBuilder();
        final int len = string.length();
        int lastIndex = 0;
        while (true) {
            int startIndex = string.indexOf('\\', lastIndex);
            if (startIndex < 0 || startIndex == len) {
                result.append(string, lastIndex, len);
                break;
            }
            result.append(string, lastIndex, startIndex);
            char nextChar = string.charAt(startIndex++);
            if (nextChar != prefix) {
                result.append(nextChar);
                lastIndex = startIndex;
            } else {
                if (startIndex >= len) {
                    throw new IllegalArgumentException("Bad hex");
                }
                nextChar = string.charAt(startIndex++);
                if (nextChar != '{') {
                    throw new IllegalArgumentException("Bad hex");
                }
                int endIndex = string.indexOf('}', lastIndex);
                if (endIndex < 0) {
                    throw new IllegalArgumentException("Bad hex");
                }
                result.append(fromHex(string, startIndex, endIndex));
                lastIndex = endIndex+1;                
            }
        }
        return result.toString();
    }
    
    static Splitter SPACE_SPLITTER = Splitter.on(' ').trimResults().omitEmptyStrings();
    
    private static CharSequence fromHex(String string, int startIndex, int endIndex) {
        final StringBuilder result = new StringBuilder();
        for (String part : SPACE_SPLITTER.split(string.substring(startIndex, endIndex))) {
            result.append(Integer.parseInt(part, 16));
        }
        return result;
    }

    public static void main(String[] args) {
        IntPredicate noHex = cp -> cp < 0x80;
        String[] tests = {"abc",
                "👽€£a\t",
                "ab👽c👽€",
                "👽ab👽€ab"
                };
        for (String test : tests) {
            System.out.println(test + ":");
            int minLen = 1;
            IntPredicate filter = null;
            for (int i = 0; i < 2; ++i) {
                String actual = toHex(test, 'u', minLen, filter);
                String roundtrip = fromHex(actual, 'u');
                System.out.println("\t«" + actual + "»\t" + (roundtrip.equals(actual) ? "ok" : "\t«" + roundtrip + "»\t"));
                minLen = 4;
                filter = noHex;
            }
//            String actual = toHex(test, 1, 'u', null);
//            roundtrip = fromHex(actual, 'u');
//            System.out.println("\t«" + actual + "»\t" + (roundtrip.equals(actual) ? "ok" : "\t«" + roundtrip + "»\t"));
                    //+ "\n\t«" + toHex(test, 4, 'u', noHex)
        }
    }
}