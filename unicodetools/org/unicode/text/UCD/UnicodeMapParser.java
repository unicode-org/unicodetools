package org.unicode.text.UCD;

import java.text.ParsePosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.InternalCldrException;

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
 * The value is parsed by a ValueParser. It defaults to a string parser (STRING_VALUE_PARSER)
 * that parses codepoint+, and returns the trimmed value (trimmed <i>before</i> \\u{...} resolution).
 * Alternate ValueParsers can be supplied, eg for Double.
 * Examples:<pre>
 * {[abc]=X, q=Y, \m{Linebreak}}
 * {\m{script},a=HUH?}
 * </pre>
 */
public class UnicodeMapParser<V> {

    /*
     * To add a new operation, add to END_VALUE, and to the switch statement near "OPERATION"
     */
    private static final UnicodeSet END_VALUE = new UnicodeSet("[\\}\\-\\&,|]").freeze();
    private static final UnicodeSet END_KEY = new UnicodeSet("[=]").freeze();
    private static final UnicodeSet WHITESPACE = new UnicodeSet("[:pattern_whitespace:]").freeze();
    private static final Pattern HEX = Pattern.compile("\\{([a-fA-F0-9]+)\\}");

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

    public final UnicodeMap<V> parse(String source, ParsePosition pos) {
        return parse(source, pos, new UnicodeMap<V>());
    }

    public UnicodeMap<V> parse(String source, ParsePosition pos, UnicodeMap<V> resultToAddTo) {
        pos.setErrorIndex(-1);
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
                switch(cp) {
                case '{':
                    if (op == Operation.ADD) {
                        parse(source, pos, resultToAddTo);
                    } else {
                        UnicodeMap<V> um2 = parse(source, pos);
                        addUnicodeMap(op, um2, resultToAddTo);
                    }
                    doAssignment = false;
                    break;
                case '[':
                    us = new UnicodeSet().applyPattern(source, pos, unicodePropertyFactory.getXSymbolTable(), 0);
                    if (op != Operation.ADD) {
                        if (op == Operation.REMOVE) {
                            resultToAddTo.putAll(us, null);
                        } else {
                            resultToAddTo.putAll(us.complement(), null);
                        }
                        doAssignment = false;
                    }
                    break;
                case '\\':
                    if (matches(source, pos, "\\m")) {
                        parsePropertyAndAdd(source, pos, op, resultToAddTo);
                        doAssignment = false;
                        break;
                    }
                    // fall through
                default:
                    if (op != Operation.ADD) {
                        return setError(pos);
                    }
                    key = parseTo(source, END_KEY, pos).trim();
                    break;
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
                        resultToAddTo.putAll(us, value);
                    } else {
                        resultToAddTo.put(key, value);
                    }
                    cp = eatSpaceAndPeek(source, pos);
                } else {
                    doAssignment = true; // for next time.
                }
                // OPERATION
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
        return resultToAddTo;
    }

    enum Operation {ADD, REMOVE, RETAIN}

    private void parsePropertyAndAdd(String source, ParsePosition pos, Operation op, UnicodeMap<V> result) {
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
        addUnicodeMapString(op, (UnicodeMap<String>) prop.getUnicodeMap(), result);
        pos.setIndex(term+1);
    }

    public void addUnicodeMapString(Operation op, UnicodeMap<String> um,
            UnicodeMap<V> result) {
        if (op == Operation.RETAIN) {
            throw new InternalCldrException("Should never happen");
        }
        for (EntryRange<String> entry : um.entryRanges()) {
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
    }

    public void addUnicodeMap(Operation op, UnicodeMap<V> um, UnicodeMap<V> result) {
        switch(op) {
        case REMOVE: 
            result.putAll(um.keySet(), null); 
            break;
        case RETAIN: 
            UnicodeSet toRemove = new UnicodeSet(result.keySet()).removeAll(um.keySet());
            result.putAll(toRemove, null); 
            break;
        case ADD: throw new InternalCldrException("Should never happen");
        }
//        for (EntryRange<V> entry : um.entryRanges()) {
//            if (entry.value == null) {
//                continue;
//            }
//            final V value = op == Operation.REMOVE ? null : entry.value;
//            if (entry.string == null) {
//                result.putAll(entry.codepoint, entry.codepointEnd, value);
//            } else {
//                result.put(entry.string, value);
//            }
//        }
    }

    private boolean matches(String source, ParsePosition pos, String string) {
        return source.regionMatches(pos.getIndex(), string, 0, string.length());
    }

    private UnicodeMap<V> setError(ParsePosition pos) {
        pos.setErrorIndex(pos.getIndex());
        return null;
    }

    private int peekCodePoint(String source, ParsePosition pos) {
        if (pos.getIndex() < source.length()) {
            return source.codePointAt(pos.getIndex());
        }
        return 0x10FFFF;
    }

    private int getCodePoint(String source, ParsePosition pos) {
        if (pos.getIndex() < source.length()) {
            int cp = source.codePointAt(pos.getIndex());
            skipCodePoint(pos, cp);
            return cp;
        }
        return 0x10FFFF;
    }

    private int eatSpaceAndPeek(String source, ParsePosition pos) {
        while (true) {
            int cp = peekCodePoint(source, pos);
            if (!WHITESPACE.contains(cp)) {
                return cp;
            }
            skipCodePoint(pos, cp);
        }
    }

    private void skipCodePoint(ParsePosition pos, int cp) {
        pos.setIndex(pos.getIndex() + Character.charCount(cp));
    }

    private static String parseTo(String source, UnicodeSet stop, ParsePosition pos) {
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
