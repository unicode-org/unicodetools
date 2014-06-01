package org.unicode.text.UCD;

import java.text.ParsePosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.dev.util.UnicodeProperty.Factory;
import com.ibm.icu.text.UnicodeSet;

/**
 * Syntax for reading a UnicodeMap from a string.
 * <pre>
 * um           := { um_list }
 * umList       := umItem (addItem | removeItem | retainItem)*
 * addList      := [,|] umItem
 * removeItem   := '-' (umItem | string)
 * retainItem   := '&' (umItem | string)
 * umItem       := string '=' value
 * umItem       := '\m{' propertyName '}'
 * string       := (unicodeSet | codepoint+)
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

    private static final UnicodeSet END_VALUE = new UnicodeSet("[\\}\\-\\&,|]").freeze();
    private static final UnicodeSet END_KEY = new UnicodeSet("[=]").freeze();
    private static final UnicodeSet WHITESPACE = new UnicodeSet("[:pattern_whitespace:]").freeze();

    public interface ValueParser<V> {
        public V parse(String source, ParsePosition pos);
        public V parse(String source);
    }

    private final ValueParser<V> valueParser;
    private final Factory unicodePropertyFactory;

    private UnicodeMapParser(ValueParser<V> valueParser, Factory unicodePropertyFactory) {
        this.valueParser = valueParser;
        this.unicodePropertyFactory = unicodePropertyFactory == null ? ToolUnicodePropertySource.make("") : unicodePropertyFactory;
    }

    public static <V> UnicodeMapParser<V> create(ValueParser<V> valueParser, Factory unicodePropertyFactory) {
        return new UnicodeMapParser<V>(valueParser, unicodePropertyFactory);
    }

    public static <V> UnicodeMapParser<V> create(ValueParser<V> valueParser) {
        return new UnicodeMapParser<V>(valueParser, null);
    }

    public static UnicodeMapParser<String> create() {
        return create(STRING_VALUE_PARSER);
    }

    public UnicodeMap<V> parse(String source, ParsePosition pos) {
        pos.setErrorIndex(-1);
        UnicodeMap result = new UnicodeMap();
        int cp = eatSpaceAndPeek(source, pos);
        if (cp != '{') {
            return setError(pos);
        }
        skipCodePoint(pos, cp);
        boolean doAssignment = true;
        Operation op = Operation.ADD;
        main:
        while (true) {
            UnicodeSet us = null;
            String key = null;
            cp = eatSpaceAndPeek(source, pos);
            if (cp == '[') {
                us = new UnicodeSet().applyPattern(source, pos, unicodePropertyFactory.getXSymbolTable(), 0);
                if (op != Operation.ADD) {
                    if (op == Operation.REMOVE) {
                        result.putAll(us, null);
                    } else {
                        result.putAll(us.complement(), null);
                    }
                    doAssignment = false;
                }
            } else if (matches(source, pos, "\\m")) {
                addUnicodeMap(source, pos, op, result);
                doAssignment = false;
            } else {
                if (op != Operation.ADD) {
                    return setError(pos);
                }
                key = parseTo(source, END_KEY, pos).trim();
            }
            if (pos.getErrorIndex() >= 0) {
                return null;
            }
            cp = eatSpaceAndPeek(source, pos);
            if (doAssignment) {
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
            } else {
                doAssignment = true; // for next time.
            }
            switch(cp) {
            case '}':
                skipCodePoint(pos, cp);
                break main;
            case ',': case '|':
                op = Operation.ADD;
                break;
            case '&':
                op = Operation.RETAIN;
                break;
            case '-':
                op = Operation.REMOVE;
                break;
            default:
                pos.setErrorIndex(pos.getIndex());
                return null;
            }
            skipCodePoint(pos, cp);
        }
        return result;
    }

    enum Operation {ADD, REMOVE, RETAIN}

    private void addUnicodeMap(String source, ParsePosition pos, Operation op, UnicodeMap<V> result) {
        final int current = pos.getIndex();
        if (source.length() - current < 5) {
            pos.setErrorIndex(current);
            return;
        }
        if (source.charAt(current+2) != '{') {
            pos.setErrorIndex(current+2);
            return;
        }
        int term = source.indexOf('}', current+3);
        if (term < 0) {
            pos.setErrorIndex(current+4);
            return;
        }
        String propName = source.substring(current+3, term);
        UnicodeProperty prop = unicodePropertyFactory.getProperty(propName);
        if (prop == null) {
            pos.setErrorIndex(current+4);
            return;
        }
        UnicodeMap<String> um = prop.getUnicodeMap();
        for (EntryRange entry : um.entryRanges()) {
            if (entry.value == null) {
                continue;
            }
            final V value = op == Operation.REMOVE ? null : valueParser.parse((String)entry.value);
            if (entry.string == null) {
                result.putAll(entry.codepoint, entry.codepointEnd, value);
            } else {
                result.put(entry.string, value);
            }
        }
        pos.setIndex(term+1);
    }

    private boolean matches(String source, ParsePosition pos, String string) {
        return source.regionMatches(pos.getIndex(), string, 0, string.length());
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
        public String parse(String s) {
            return s;
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
