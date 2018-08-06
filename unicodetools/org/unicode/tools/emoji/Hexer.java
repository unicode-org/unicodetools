package org.unicode.tools.emoji;

import java.util.function.IntPredicate;

import org.unicode.cldr.util.With;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.ibm.icu.text.StringTransform;

public class Hexer implements StringTransform {
    
    public String toHex(String string) {
        final StringBuilder result = new StringBuilder();
        boolean inBraces = false;
        for (int cp : With.codePointArray(string)) {
            if (doEscape != null && doEscape.test(cp)) {
                if (inBraces) {
                    result.append(' ');
                } if (cp <= 0xFF) { 
                    result.append('\\').append('x').append(Utility.hex(cp, 2));
                    continue;
                } if (cp <= 0xFFFF) { 
                    result.append('\\').append('u').append(Utility.hex(cp, 4));
                    continue;
                } else {
                    result.append('\\').append(prefix).append('{');
                    inBraces = true;
                }
                result.append(Utility.hex(cp, minLen));
                if (inBraces && !multiPoint) {
                    result.append('}');
                    inBraces = false;
                }
            } else {
                if (inBraces) {
                    result.append('}');
                    inBraces = false;
                }
                result.appendCodePoint(cp);
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
    public String fromHex(String string) {
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
        IntPredicate escapeNonAscii = cp -> cp >= 0x80;
        String[] tests = {
                "abc",
                "👽€£a\t",
                "ab👽c👽€",
                "👽ab👽€ab"
                };
        Hexer hexer1 = new Hexer('u').setMinLength(1).setDoEscape(null);
        Hexer hexer2 = new Hexer('u').setMinLength(4).setDoEscape(escapeNonAscii);
        for (String test : tests) {
            System.out.println(test + ":");
            checkRoundtrip(hexer1, test);
            checkRoundtrip(hexer2, test);
        }
    }

    private static void checkRoundtrip(Hexer hexer, String test) {
        String actual = hexer.toHex(test);
        String roundtrip = hexer.fromHex(actual);
        System.out.println("\t«" + actual + "»\t" + (roundtrip.equals(actual) ? "ok" : "\t«" + roundtrip + "»\t"));
    }

    private final char prefix;
    private final int minLen;
    private final boolean multiPoint;
    private final IntPredicate doEscape;

    /**
     * Defaults to: setMinLength = 1, doEscape = null, multiPoint = false;
     * @param prefix
     */
    public Hexer(char prefix) {
        this(prefix, 1, false, null);
    }
    
    public Hexer setMinLength(int minLen) {
        return new Hexer(prefix, minLen, multiPoint, doEscape);
    }
    
    public Hexer setDoEscape(IntPredicate doEscape) {
        return new Hexer(prefix, minLen, multiPoint, doEscape);
    }
    
    private Hexer(char prefix, int minLen, boolean multiPoint, IntPredicate doEscape) {
        super();
        this.prefix = prefix;
        this.minLen = minLen;
        this.multiPoint = multiPoint;
        this.doEscape = doEscape;
    }

    @Override
    public String transform(String source) {
        return toHex(source);
    }
}