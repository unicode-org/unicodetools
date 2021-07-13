package org.unicode.utilities;

import java.io.IOException;
import java.text.ParsePosition;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.impl.PatternProps;
import com.ibm.icu.impl.RuleCharacterIterator;
import com.ibm.icu.impl.StringRange;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.SymbolTable;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeMatcher;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;

public class UnicodeSetParser {
    
    
    //----------------------------------------------------------------
    // Implementation: Pattern parsing
    //----------------------------------------------------------------
    
    public UnicodeSet applyPattern(UnicodeSet source, String pattern) {
	return applyPattern(source, pattern, null, null, UnicodeSet.IGNORE_SPACE);
    }

    /**
     * Parses the given pattern, starting at the given position.  The character
     * at pattern.charAt(pos.getIndex()) must be '[', or the parse fails.
     * Parsing continues until the corresponding closing ']'.  If a syntax error
     * is encountered between the opening and closing brace, the parse fails.
     * Upon return from a successful parse, the ParsePosition is updated to
     * point to the character following the closing ']', and an inversion
     * list for the parsed pattern is returned.  This method
     * calls itself recursively to parse embedded subpatterns.
     *
     * @param pattern the string containing the pattern to be parsed.  The
     * portion of the string from pos.getIndex(), which must be a '[', to the
     * corresponding closing ']', is parsed.
     * @param pos upon entry, the position at which to being parsing.  The
     * character at pattern.charAt(pos.getIndex()) must be a '['.  Upon return
     * from a successful parse, pos.getIndex() is either the character after the
     * closing ']' of the parsed pattern, or pattern.length() if the closing ']'
     * is the last character of the pattern string.
     * @return an inversion list for the parsed substring
     * of <code>pattern</code>
     * @exception java.lang.IllegalArgumentException if the parse fails.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public UnicodeSet applyPattern(UnicodeSet source, String pattern,
            ParsePosition pos,
            SymbolTable symbols,
            int options) {

        // Need to build the pattern in a temporary string because
        // _applyPattern calls add() etc., which set pat to empty.
        boolean parsePositionWasNull = pos == null;
        if (parsePositionWasNull) {
            pos = new ParsePosition(0);
        }

        StringBuilder rebuiltPat = new StringBuilder();
        RuleCharacterIterator chars =
                new RuleCharacterIterator(pattern, symbols, pos);
        applyPattern(source, chars, symbols, rebuiltPat, options, 0);
        if (chars.inVariable()) {
            syntaxError(chars, "Extra chars in variable value");
        }
        String pat = rebuiltPat.toString();
        if (parsePositionWasNull) {
            int i = pos.getIndex();

            // Skip over trailing whitespace
            if ((options & UnicodeSet.IGNORE_SPACE) != 0) {
                i = PatternProps.skipWhiteSpace(pattern, i);
            }

            if (i != pattern.length()) {
                throw new IllegalArgumentException("Parse of \"" + pattern +
                        "\" failed at " + i);
            }
        }
        return source;
    }

    // Add constants to make the applyPattern() code easier to follow.

    private static final int LAST0_START = 0,
            LAST1_RANGE = 1,
            LAST2_SET = 2;

    private static final int MODE0_NONE = 0,
            MODE1_INBRACKET = 1,
            MODE2_OUTBRACKET = 2;

    private static final int SETMODE0_NONE = 0,
            SETMODE1_UNICODESET = 1,
            SETMODE2_PROPERTYPAT = 2,
            SETMODE3_PREPARSED = 3;

    private static final int MAX_DEPTH = 100;

    /**
     * Parse the pattern from the given RuleCharacterIterator.  The
     * iterator is advanced over the parsed pattern.
     * @param chars iterator over the pattern characters.  Upon return
     * it will be advanced to the first character after the parsed
     * pattern, or the end of the iteration if all characters are
     * parsed.
     * @param symbols symbol table to use to parse and dereference
     * variables, or null if none.
     * @param rebuiltPat the pattern that was parsed, rebuilt or
     * copied from the input pattern, as appropriate.
     * @param options a bit mask of zero or more of the following:
     * IGNORE_SPACE, CASE.
     */
    private void applyPattern(UnicodeSet source, RuleCharacterIterator chars, SymbolTable symbols,
            Appendable rebuiltPat, int options, int depth) {
        if (depth > MAX_DEPTH) {
            syntaxError(chars, "Pattern nested too deeply");
        }

        // Syntax characters: [ ] ^ - & { }

        // Recognized special forms for chars, sets: c-c s-s s&s

        int opts = RuleCharacterIterator.PARSE_VARIABLES |
                RuleCharacterIterator.PARSE_ESCAPES;
        if ((options & UnicodeSet.IGNORE_SPACE) != 0) {
            opts |= RuleCharacterIterator.SKIP_WHITESPACE;
        }

        StringBuilder patBuf = new StringBuilder(), buf = null;
        boolean usePat = false;
        UnicodeSet scratch = null;
        Object backup = null;

        // mode: 0=before [, 1=between [...], 2=after ]
        // lastItem: 0=none, 1=char, 2=set
        int lastItem = LAST0_START, lastChar = 0, mode = MODE0_NONE;
        char op = 0;

        boolean invert = false;

        source.clear();
        String lastString = null;

        while (mode != MODE2_OUTBRACKET && !chars.atEnd()) {
            //Eclipse stated the following is "dead code"
            /*
            if (false) {
                // Debugging assertion
                if (!((lastItem == 0 && op == 0) ||
                        (lastItem == 1 && (op == 0 || op == '-')) ||
                        (lastItem == 2 && (op == 0 || op == '-' || op == '&')))) {
                    throw new IllegalArgumentException();
                }
            }*/

            int c = 0;
            boolean literal = false;
            UnicodeSet nested = null;

            // -------- Check for property pattern

            // setMode: 0=none, 1=unicodeset, 2=propertypat, 3=preparsed
            int setMode = SETMODE0_NONE;
            if (resemblesPropertyPattern(chars, opts)) {
                setMode = SETMODE2_PROPERTYPAT;
            }

            // -------- Parse '[' of opening delimiter OR nested set.
            // If there is a nested set, use `setMode' to define how
            // the set should be parsed.  If the '[' is part of the
            // opening delimiter for this pattern, parse special
            // strings "[", "[^", "[-", and "[^-".  Check for stand-in
            // characters representing a nested set in the symbol
            // table.

            else {
                // Prepare to backup if necessary
                backup = chars.getPos(backup);
                c = chars.next(opts);
                literal = chars.isEscaped();

                if (c == '[' && !literal) {
                    if (mode == MODE1_INBRACKET) {
                        chars.setPos(backup); // backup
                        setMode = SETMODE1_UNICODESET;
                    } else {
                        // Handle opening '[' delimiter
                        mode = MODE1_INBRACKET;
                        patBuf.append('[');
                        backup = chars.getPos(backup); // prepare to backup
                        c = chars.next(opts);
                        literal = chars.isEscaped();
                        if (c == '^' && !literal) {
                            invert = true;
                            patBuf.append('^');
                            backup = chars.getPos(backup); // prepare to backup
                            c = chars.next(opts);
                            literal = chars.isEscaped();
                        }
                        // Fall through to handle special leading '-';
                        // otherwise restart loop for nested [], \p{}, etc.
                        if (c == '-') {
                            literal = true;
                            // Fall through to handle literal '-' below
                        } else {
                            chars.setPos(backup); // backup
                            continue;
                        }
                    }
                } else if (symbols != null) {
                    UnicodeMatcher m = symbols.lookupMatcher(c); // may be null
                    if (m != null) {
                        try {
                            nested = (UnicodeSet) m;
                            setMode = SETMODE3_PREPARSED;
                        } catch (ClassCastException e) {
                            syntaxError(chars, "Syntax error");
                        }
                    }
                }
            }

            // -------- Handle a nested set.  This either is inline in
            // the pattern or represented by a stand-in that has
            // previously been parsed and was looked up in the symbol
            // table.

            if (setMode != SETMODE0_NONE) {
                if (lastItem == LAST1_RANGE) {
                    if (op != 0) {
                        syntaxError(chars, "Char expected after operator");
                    }
                    add_unchecked(source, lastChar, lastChar);
                    _appendToPat(patBuf, lastChar, false);
                    lastItem = LAST0_START;
                    op = 0;
                }

                if (op == '-' || op == '&') {
                    patBuf.append(op);
                }

                if (nested == null) {
                    if (scratch == null) scratch = new UnicodeSet();
                    nested = scratch;
                }
                switch (setMode) {
                case SETMODE1_UNICODESET:
                    applyPattern(nested, chars, symbols, patBuf, options, depth + 1);
                    break;
                case SETMODE2_PROPERTYPAT:
                    chars.skipIgnored(opts);
                    applyPropertyPattern(nested, chars, patBuf, symbols);
                    break;
                case SETMODE3_PREPARSED: // `nested' already parsed
                    //                    nested._toPattern(patBuf, false);
                    patBuf.append(nested.toPattern(false));
                    break;
                }

                usePat = true;

                if (mode == MODE0_NONE) {
                    // Entire pattern is a category; leave parse loop
                    source.set(nested);
                    mode = MODE2_OUTBRACKET;
                    break;
                }

                switch (op) {
                case '-':
                    source.removeAll(nested);
                    break;
                case '&':
                    source.retainAll(nested);
                    break;
                case 0:
                    source.addAll(nested);
                    break;
                }

                op = 0;
                lastItem = LAST2_SET;

                continue;
            }

            if (mode == MODE0_NONE) {
                syntaxError(chars, "Missing '['");
            }

            // -------- Parse special (syntax) characters.  If the
            // current character is not special, or if it is escaped,
            // then fall through and handle it below.

            if (!literal) {
                switch (c) {
                case ']':
                    if (lastItem == LAST1_RANGE) {
                        add_unchecked(source, lastChar, lastChar);
                        _appendToPat(patBuf, lastChar, false);
                    }
                    // Treat final trailing '-' as a literal
                    if (op == '-') {
                	add_unchecked(source, op, op);
                        patBuf.append(op);
                    } else if (op == '&') {
                        syntaxError(chars, "Trailing '&'");
                    }
                    patBuf.append(']');
                    mode = MODE2_OUTBRACKET;
                    continue;
                case '-':
                    if (op == 0) {
                        if (lastItem != LAST0_START) {
                            op = (char) c;
                            continue;
                        } else if (lastString != null) {
                            op = (char) c;
                            continue;
                        } else {
                            // Treat final trailing '-' as a literal
                            add_unchecked(source, c, c);
                            c = chars.next(opts);
                            literal = chars.isEscaped();
                            if (c == ']' && !literal) {
                                patBuf.append("-]");
                                mode = MODE2_OUTBRACKET;
                                continue;
                            }
                        }
                    }
                    syntaxError(chars, "'-' not after char, string, or set");
                    break;
                case '&':
                    if (lastItem == LAST2_SET && op == 0) {
                        op = (char) c;
                        continue;
                    }
                    syntaxError(chars, "'&' not after set");
                    break;
                case '^':
                    syntaxError(chars, "'^' not after '['");
                    break;
                case '{':
                    if (op != 0 && op != '-') {
                        syntaxError(chars, "Missing operand after operator");
                    }
                    if (lastItem == LAST1_RANGE) {
                	add_unchecked(source, lastChar, lastChar);
                        _appendToPat(patBuf, lastChar, false);
                    }
                    lastItem = LAST0_START;
                    if (buf == null) {
                        buf = new StringBuilder();
                    } else {
                        buf.setLength(0);
                    }
                    boolean ok = false;
                    while (!chars.atEnd()) {
                        c = chars.next(opts);
                        literal = chars.isEscaped();
                        if (c == '}' && !literal) {
                            ok = true;
                            break;
                        }
                        appendCodePoint(buf, c);
                    }
                    if (buf.length() < 1 || !ok) {
                        syntaxError(chars, "Invalid multicharacter string");
                    }
                    // We have new string. Add it to set and continue;
                    // we don't need to drop through to the further
                    // processing
                    String curString = buf.toString();
                    if (op == '-') {
                        int lastSingle = CharSequences.getSingleCodePoint(lastString == null ? "" : lastString);
                        int curSingle = CharSequences.getSingleCodePoint(curString);
                        if (lastSingle != Integer.MAX_VALUE && curSingle != Integer.MAX_VALUE) {
                            source.add(lastSingle,curSingle);
                        } else {
                            Set<String> strings = new TreeSet<>();
                            try {
                                StringRange.expand(lastString, curString, true, strings);
                                source.addAll(strings);
                            } catch (Exception e) {
                                syntaxError(chars, e.getMessage());
                            }
                        }
                        lastString = null;
                        op = 0;
                    } else {
                        source.add(curString);
                        lastString = curString;
                    }
                    patBuf.append('{');
                    _appendToPat(patBuf, curString, false);
                    patBuf.append('}');
                    continue;
                case SymbolTable.SYMBOL_REF:
                    //         symbols  nosymbols
                    // [a-$]   error    error (ambiguous)
                    // [a$]    anchor   anchor
                    // [a-$x]  var "x"* literal '$'
                    // [a-$.]  error    literal '$'
                    // *We won't get here in the case of var "x"
                    backup = chars.getPos(backup);
                    c = chars.next(opts);
                    literal = chars.isEscaped();
                    boolean anchor = (c == ']' && !literal);
                    if (symbols == null && !anchor) {
                        c = SymbolTable.SYMBOL_REF;
                        chars.setPos(backup);
                        break; // literal '$'
                    }
                    if (anchor && op == 0) {
                        if (lastItem == LAST1_RANGE) {
                            add_unchecked(source, lastChar, lastChar);
                            _appendToPat(patBuf, lastChar, false);
                        }
                        add_unchecked(source, UnicodeMatcher.ETHER);
                        usePat = true;
                        patBuf.append(SymbolTable.SYMBOL_REF).append(']');
                        mode = MODE2_OUTBRACKET;
                        continue;
                    }
                    syntaxError(chars, "Unquoted '$'");
                    break;
                default:
                    break;
                }
            }

            // -------- Parse literal characters.  This includes both
            // escaped chars ("\u4E01") and non-syntax characters
            // ("a").

            switch (lastItem) {
            case LAST0_START:
                if (op == '-' && lastString != null) {
                    syntaxError(chars, "Invalid range");
                }
                lastItem = LAST1_RANGE;
                lastChar = c;
                lastString = null;
                break;
            case LAST1_RANGE:
                if (op == '-') {
                    if (lastString != null) {
                        syntaxError(chars, "Invalid range");
                    }
                    if (lastChar >= c) {
                        // Don't allow redundant (a-a) or empty (b-a) ranges;
                        // these are most likely typos.
                        syntaxError(chars, "Invalid range");
                    }
                    add_unchecked(source, lastChar, c);
                    _appendToPat(patBuf, lastChar, false);
                    patBuf.append(op);
                    _appendToPat(patBuf, c, false);
                    lastItem = LAST0_START;
                    op = 0;
                } else {
                    add_unchecked(source, lastChar, lastChar);
                    _appendToPat(patBuf, lastChar, false);
                    lastChar = c;
                }
                break;
            case LAST2_SET:
                if (op != 0) {
                    syntaxError(chars, "Set expected after operator");
                }
                lastChar = c;
                lastItem = LAST1_RANGE;
                break;
            }
        }

        if (mode != MODE2_OUTBRACKET) {
            syntaxError(chars, "Missing ']'");
        }

        chars.skipIgnored(opts);

        /**
         * Handle global flags (invert, case insensitivity).  If this
         * pattern should be compiled case-insensitive, then we need
         * to close over case BEFORE COMPLEMENTING.  This makes
         * patterns like /[^abc]/i work.
         */
        if ((options & UnicodeSet.CASE) != 0) {
            source.closeOver(UnicodeSet.CASE);
        }
        if (invert) {
            source.complement();
        }

        // Use the rebuilt pattern (pat) only if necessary.  Prefer the
        // generated pattern.
        if (usePat) {
            append(rebuiltPat, patBuf.toString());
        } else {
            appendNewPattern(source, rebuiltPat, false, true);
        }
    }

    private void add_unchecked(UnicodeSet source, char ether) {
	source.add(ether);
    }

    private void add_unchecked(UnicodeSet source, int lastChar, int lastChar2) {
	source.add(lastChar, lastChar2);
	
    }

    private static void syntaxError(RuleCharacterIterator chars, String msg) {
        throw new IllegalArgumentException("Error: " + msg + " at \"" +
                Utility.escape(chars.toString()) +
                '"');
    }

    //----------------------------------------------------------------
    // Property set patterns
    //----------------------------------------------------------------

//    /**
//     * Return true if the given position, in the given pattern, appears
//     * to be the start of a property set pattern.
//     */
//    private static boolean resemblesPropertyPattern(String pattern, int pos) {
//        // Patterns are at least 5 characters long
//        if ((pos+5) > pattern.length()) {
//            return false;
//        }
//
//        // Look for an opening [:, [:^, \p, or \P
//        return pattern.regionMatches(pos, "[:", 0, 2) ||
//                pattern.regionMatches(true, pos, "\\p", 0, 2) ||
//                pattern.regionMatches(pos, "\\N", 0, 2);
//    }

    /**
     * Return true if the given iterator appears to point at a
     * property pattern.  Regardless of the result, return with the
     * iterator unchanged.
     * @param chars iterator over the pattern characters.  Upon return
     * it will be unchanged.
     * @param iterOpts RuleCharacterIterator options
     */
    private static boolean resemblesPropertyPattern(RuleCharacterIterator chars,
            int iterOpts) {
        boolean result = false;
        iterOpts &= ~RuleCharacterIterator.PARSE_ESCAPES;
        Object pos = chars.getPos(null);
        int c = chars.next(iterOpts);
        if (c == '[' || c == '\\') {
            int d = chars.next(iterOpts & ~RuleCharacterIterator.SKIP_WHITESPACE);
            result = (c == '[') ? (d == ':') :
                (d == 'N' || d == 'p' || d == 'P');
        }
        chars.setPos(pos);
        return result;
    }

    /**
     * Parse the given property pattern at the given parse position.
     * @param symbols TODO
     */
    private UnicodeSet applyPropertyPattern(UnicodeSet source, String pattern, ParsePosition ppos, SymbolTable symbols) {
        int pos = ppos.getIndex();

        // On entry, ppos should point to one of the following locations:

        // Minimum length is 5 characters, e.g. \p{L}
        if ((pos+5) > pattern.length()) {
            return null;
        }

        boolean posix = false; // true for [:pat:], false for \p{pat} \P{pat} \N{pat}
        boolean isName = false; // true for \N{pat}, o/w false
        boolean invert = false;

        // Look for an opening [:, [:^, \p, or \P
        if (pattern.regionMatches(pos, "[:", 0, 2)) {
            posix = true;
            pos = PatternProps.skipWhiteSpace(pattern, (pos+2));
            if (pos < pattern.length() && pattern.charAt(pos) == '^') {
                ++pos;
                invert = true;
            }
        } else if (pattern.regionMatches(true, pos, "\\p", 0, 2) ||
                pattern.regionMatches(pos, "\\N", 0, 2)) {
            char c = pattern.charAt(pos+1);
            invert = (c == 'P');
            isName = (c == 'N');
            pos = PatternProps.skipWhiteSpace(pattern, (pos+2));
            if (pos == pattern.length() || pattern.charAt(pos++) != '{') {
                // Syntax error; "\p" or "\P" not followed by "{"
                return null;
            }
        } else {
            // Open delimiter not seen
            return null;
        }

        // Look for the matching close delimiter, either :] or }
        int close = pattern.indexOf(posix ? ":]" : "}", pos);
        if (close < 0) {
            // Syntax error; close delimiter missing
            return null;
        }

        // Look for an '=' sign.  If this is present, we will parse a
        // medium \p{gc=Cf} or long \p{GeneralCategory=Format}
        // pattern.
        int equals = pattern.indexOf('=', pos);
        String propName, valueName;
        if (equals >= 0 && equals < close && !isName) {
            // Equals seen; parse medium/long pattern
            propName = pattern.substring(pos, equals);
            valueName = pattern.substring(equals+1, close);
        }

        else {
            // Handle case where no '=' is seen, and \N{}
            propName = pattern.substring(pos, close);
            valueName = "";

            // Handle \N{name}
            if (isName) {
                // This is a little inefficient since it means we have to
                // parse "na" back to UProperty.NAME even though we already
                // know it's UProperty.NAME.  If we refactor the API to
                // support args of (int, String) then we can remove
                // "na" and make this a little more efficient.
                valueName = propName;
                propName = "na";
            }
        }

        source.applyPropertyAlias(propName, valueName, symbols);

        if (invert) {
            source.complement();
        }

        // Move to the limit position after the close delimiter
        ppos.setIndex(close + (posix ? 2 : 1));

        return source;
    }

    /**
     * Parse a property pattern.
     * @param chars iterator over the pattern characters.  Upon return
     * it will be advanced to the first character after the parsed
     * pattern, or the end of the iteration if all characters are
     * parsed.
     * @param rebuiltPat the pattern that was parsed, rebuilt or
     * copied from the input pattern, as appropriate.
     * @param symbols TODO
     */
    private void applyPropertyPattern(UnicodeSet source, RuleCharacterIterator chars,
            Appendable rebuiltPat, SymbolTable symbols) {
        String patStr = chars.lookahead();
        ParsePosition pos = new ParsePosition(0);
        applyPropertyPattern(source, patStr, pos, symbols);
        if (pos.getIndex() == 0) {
            syntaxError(chars, "Invalid property pattern");
        }
        chars.jumpahead(pos.getIndex());
        append(rebuiltPat, patStr.substring(0, pos.getIndex()));
    }
    
    /**
     * TODO: create class Appendables?
     * @throws IOException
     */
    private static void append(Appendable app, CharSequence s) {
        try {
            app.append(s);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }
    
    /**
     * TODO: create Appendable version of UTF16.append(buf, c),
     * maybe in new class Appendables?
     * @throws IOException
     */
    private static void appendCodePoint(Appendable app, int c) {
        assert 0 <= c && c <= 0x10ffff;
        try {
            if (c <= 0xffff) {
                app.append((char) c);
            } else {
                app.append(UTF16.getLeadSurrogate(c)).append(UTF16.getTrailSurrogate(c));
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    /**
     * Append the <code>toPattern()</code> representation of a
     * string to the given <code>Appendable</code>.
     */
    private static <T extends Appendable> T _appendToPat(T buf, String s, boolean escapeUnprintable) {
        int cp;
        for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
            cp = s.codePointAt(i);
            _appendToPat(buf, cp, escapeUnprintable);
        }
        return buf;
    }

    /**
     * Append the <code>toPattern()</code> representation of a
     * character to the given <code>Appendable</code>.
     */
    private static <T extends Appendable> T _appendToPat(T buf, int c, boolean escapeUnprintable) {
        try {
            if (escapeUnprintable && Utility.isUnprintable(c)) {
                // Use hex escape notation (<backslash>uxxxx or <backslash>Uxxxxxxxx) for anything
                // unprintable
                if (Utility.escapeUnprintable(buf, c)) {
                    return buf;
                }
            }
            // Okay to let ':' pass through
            switch (c) {
            case '[': // SET_OPEN:
            case ']': // SET_CLOSE:
            case '-': // HYPHEN:
            case '^': // COMPLEMENT:
            case '&': // INTERSECTION:
            case '\\': //BACKSLASH:
            case '{':
            case '}':
            case '$':
            case ':':
                buf.append('\\');
                break;
            default:
                // Escape whitespace
                if (PatternProps.isWhiteSpace(c)) {
                    buf.append('\\');
                }
                break;
            }
            appendCodePoint(buf, c);
            return buf;
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }
    
    private <T extends Appendable> T appendNewPattern(UnicodeSet source, 
            T result, boolean escapeUnprintable, boolean includeStrings) {
        try {
            result.append('[');

            int count = source.getRangeCount();

            // If the set contains at least 2 intervals and includes both
            // MIN_VALUE and MAX_VALUE, then the inverse representation will
            // be more economical.
            if (count > 1 &&
        	    source.getRangeStart(0) == UnicodeSet.MIN_VALUE &&
        		    source.getRangeEnd(count-1) == UnicodeSet.MAX_VALUE) {

                // Emit the inverse
                result.append('^');

                for (int i = 1; i < count; ++i) {
                    int start = source.getRangeEnd(i-1)+1;
                    int end = source.getRangeStart(i)-1;
                    _appendToPat(result, start, escapeUnprintable);
                    if (start != end) {
                        if ((start+1) != end) {
                            result.append('-');
                        }
                        _appendToPat(result, end, escapeUnprintable);
                    }
                }
            }

            // Default; emit the ranges as pairs
            else {
                for (int i = 0; i < count; ++i) {
                    int start = source.getRangeStart(i);
                    int end = source.getRangeEnd(i);
                    _appendToPat(result, start, escapeUnprintable);
                    if (start != end) {
                        if ((start+1) != end) {
                            result.append('-');
                        }
                        _appendToPat(result, end, escapeUnprintable);
                    }
                }
            }

            if (includeStrings && hasStrings(source)) {
                for (String s : source.strings()) {
                    result.append('{');
                    _appendToPat(result, s, escapeUnprintable);
                    result.append('}');
                }
            }
            result.append(']');
            return result;
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    /**
     * TODO Add as API
     * @param source
     * @return
     */
    private boolean hasStrings(UnicodeSet source) {
	return source.strings().isEmpty();
    }
    
//    /**
//     * Append a string representation of this set to result.  This will be
//     * a cleaned version of the string passed to applyPattern(), if there
//     * is one.  Otherwise it will be generated.
//     */
//    private <T extends Appendable> T _toPattern(UnicodeSet source, String pat, T result,
//            boolean escapeUnprintable) {
//        if (pat == null) {
//            return appendNewPattern(source, result, escapeUnprintable, true);
//        }
//        try {
//            if (!escapeUnprintable) {
//                result.append(pat);
//                return result;
//            }
//            boolean oddNumberOfBackslashes = false;
//            for (int i=0; i<pat.length(); ) {
//                int c = pat.codePointAt(i);
//                i += Character.charCount(c);
//                if (Utility.isUnprintable(c)) {
//                    // If the unprintable character is preceded by an odd
//                    // number of backslashes, then it has been escaped
//                    // and we omit the last backslash.
//                    Utility.escapeUnprintable(result, c);
//                    oddNumberOfBackslashes = false;
//                } else if (!oddNumberOfBackslashes && c == '\\') {
//                    // Temporarily withhold an odd-numbered backslash.
//                    oddNumberOfBackslashes = true;
//                } else {
//                    if (oddNumberOfBackslashes) {
//                        result.append('\\');
//                    }
//                    appendCodePoint(result, c);
//                    oddNumberOfBackslashes = false;
//                }
//            }
//            if (oddNumberOfBackslashes) {
//                result.append('\\');
//            }
//            return result;
//        } catch (IOException e) {
//            throw new ICUUncheckedIOException(e);
//        }
//    }

}
