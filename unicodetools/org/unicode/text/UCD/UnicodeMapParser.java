package org.unicode.text.UCD;

import java.text.ParsePosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.With;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.SymbolTable;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.XSymbolTable;

/**
 * Syntax for reading a UnicodeMap from a string.
 * <pre>
 * um           := { um_list }
 *                 // later um - um, um & um, um - us, um & us
 * umList       := umItem (',' umItem)*
 * umItem       : (unicodeSet | codepoint+) '=' value
 * umItem       := '\p{' property '}'
 * codepoint    := character
 *              |  '\\u' ('{' [A-Fa-f0-9]{1,6} '}'
 * </pre>
 * The value is parsed by a ValueParser. It defaults to a string parser
 * that parse codepoint+, and returns the trimmed value. Alternate ValueParsers
 * can be supplied.
 * Example:<br>
 * ⟪[abc]=X, q=Y, \p{Linebreak}⟫
 * 
 */
public class UnicodeMapParser<V> {

    public interface ValueParser<V> {
        public V parse(String source, ParsePosition pos);
    }

    private final ValueParser<V> valueParser;
    private final XSymbolTable symbolParser;

    private UnicodeMapParser(ValueParser<V> valueParser, XSymbolTable symbolParser) {
        this.valueParser = valueParser;
        this.symbolParser = symbolParser;
    }

    public static <V> UnicodeMapParser<V> create(ValueParser<V> valueParser, XSymbolTable symbolParser) {
        return new UnicodeMapParser<V>(valueParser, symbolParser);
    }

    public static <V> UnicodeMapParser<V> create(ValueParser<V> valueParser) {
        return new UnicodeMapParser<V>(valueParser, null);
    }

    public static UnicodeMapParser<String> create() {
        return new UnicodeMapParser<String>(STRING_VALUE_PARSER, null);
    }

    public UnicodeMap<V> parse(String source, ParsePosition pos) {
        pos.setErrorIndex(0);
        UnicodeMap result = new UnicodeMap();
        int cp = eatSpaceAndPeek(source, pos);
        if (cp != '{') {
            return setError(pos);
        }
        skipCodePoint(pos, cp);
        while (true) {
            UnicodeSet us = null;
            String key = null;
            cp = eatSpaceAndPeek(source, pos);
            if (cp == '[') {
                us = new UnicodeSet().applyPattern(source, pos, symbolParser, 0);
            } else {
                key = parseTo(source, END_KEY, pos).trim();
            }
            if (pos.getErrorIndex() >= 0) {
                return null;
            }
            cp = eatSpaceAndPeek(source, pos);
            if (cp != '=') {
                return setError(pos);
            }
            skipCodePoint(pos, cp);
            eatSpaceAndPeek(source, pos);
            V value = valueParser.parse(source, pos);
            if (pos.getErrorIndex() >= 0) {
                return null;
            }
            if (us != null) {
                result.putAll(us, value);
            } else {
                result.put(key, value);
            }
            cp = eatSpaceAndPeek(source, pos);
            if (cp == '}') {
                skipCodePoint(pos, cp);
                break;
            } else if (cp == ',') {
                skipCodePoint(pos, cp);
            } else {
                pos.setErrorIndex(pos.getIndex());
                return null;
            }
        }
        return result;
    }

    public UnicodeMap<V> backupAndSetError(ParsePosition pos, int cp) {
        pos.setErrorIndex(pos.getIndex()-Character.charCount(cp));
        return null;
    }

    public UnicodeMap<V> setError(ParsePosition pos) {
        pos.setErrorIndex(pos.getIndex());
        return null;
    }

    int peekCodePoint(String source, ParsePosition pos) {
        if (pos.getIndex() < source.length()) {
            return source.codePointAt(pos.getIndex());
        }
        return 0x10FFFF;
    }

    int getCodePoint(String source, ParsePosition pos) {
        if (pos.getIndex() < source.length()) {
            int cp = source.codePointAt(pos.getIndex());
            skipCodePoint(pos, cp);
            return cp;
        }
        return 0x10FFFF;
    }

    static final UnicodeSet WHITESPACE = new UnicodeSet("[:pattern_whitespace:]").freeze();

    int eatSpaceAndPeek(String source, ParsePosition pos) {
        while (true) {
            int cp = peekCodePoint(source, pos);
            if (!WHITESPACE.contains(cp)) {
                return cp;
            }
            skipCodePoint(pos, cp);
        }
    }

    public void skipCodePoint(ParsePosition pos, int cp) {
        pos.setIndex(pos.getIndex() + Character.charCount(cp));
    }

    static final Pattern HEX = Pattern.compile("\\{([a-fA-F0-9]+)\\}");
    
    public static String parseTo(String source, UnicodeSet stop, ParsePosition pos) {
        pos.setErrorIndex(-1);
        StringBuilder result = new StringBuilder();
        int cp;
        int start = pos.getIndex();
        int i = start;
        int end = 0;
        boolean setEnd = false;
        for (; i < source.length(); i += Character.charCount(cp)) {
            cp = source.codePointAt(i);
            
            if (cp == '\\') {
                ++i;
                if (i == source.length()) {
                    pos.setErrorIndex(i);
                    return null;
                }
                cp = source.codePointAt(i);
                if (cp == 'u') {
                    ++i;
                    Matcher m = HEX.matcher(source).region(i, source.length());
                    if (!m.lookingAt()) {
                        pos.setErrorIndex(i);
                        return null;
                    }
                    cp = Integer.parseInt(m.group(1),16);
                    i = m.end() - Character.charCount(cp);
                }
                setEnd = true;
            } else if (stop.contains(cp)) {
                break;
            } else if (!WHITESPACE.contains(cp)) {
                setEnd = true;
            }
            result.appendCodePoint(cp);
            if (setEnd) {
                end = result.length();
                setEnd = false;
            }
        }
        pos.setIndex(i);
        result.setLength(end); // trim
        return result.toString();
    }

    private static final UnicodeSet END_VALUE = new UnicodeSet("[,}]").freeze();
    private static final UnicodeSet END_KEY = new UnicodeSet("[=]").freeze();

    public static ValueParser<String> STRING_VALUE_PARSER = new ValueParser<String>() {
        @Override
        public String parse(String s, ParsePosition pos) {
            String result = parseTo(s, END_VALUE, pos);
            if (pos.getErrorIndex() >= 0) {
                return null;
            }
            if (result.length() == 0) {
                pos.setErrorIndex(pos.getIndex());
                return null;
            }
            return result;
        }
    };

    //    public static String fromCodePointArray(int[] s, int start, int limit) {
    //        if (start >= limit) {
    //            return "";
    //        }
    //        StringBuilder b = new StringBuilder();
    //        for (int i = start; i < limit; ++i) {
    //            b.appendCodePoint(s[i]);
    //        }
    //        return b.toString();
    //    }
}
