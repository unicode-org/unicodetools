/*
 *******************************************************************************
 * Copyright (C) 2002-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.parse;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.SymbolTable;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeMatcher;
import com.ibm.icu.text.UnicodeSet;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.unicode.cldr.util.InternalCldrException;
import org.unicode.text.utility.Utility;

public class Tokenizer {
    static final boolean DEBUG = true;

    protected String source;

    protected StringBuffer buffer = new StringBuffer();
    protected long number;
    protected int codePoint;
    protected UnicodeSet unicodeSet = null;
    protected int index;
    boolean backedup = false;
    protected int lastIndex = -1;
    protected int nextIndex;
    Result lastValue = Result.BACKEDUP_TOO_FAR;
    TokenSymbolTable symbolTable = new TokenSymbolTable();

    private static final char QUOTE = '\'', BSLASH = '\\';

    private static final UnicodeSet QUOTERS = new UnicodeSet().add(QUOTE).add(BSLASH).freeze();
    private static final UnicodeSet WHITESPACE =
            new UnicodeSet("[" + "\\u0009-\\u000D\\u0020\\u0085\\u200E\\u200F\\u2028\\u2029" + "]")
                    .freeze();
    private static final UnicodeSet SYNTAX =
            new UnicodeSet(
                            "["
                                    + "\\u0021-\\u002F\\u003A-\\u0040\\u005B-\\u0060\\u007B-\\u007E"
                                    + "\\u00A1-\\u00A7\\u00A9\\u00AB-\\u00AC\\u00AE"
                                    + "\\u00B0-\\u00B1\\u00B6\\u00B7\\u00BB\\u00BF\\u00D7\\u00F7"
                                    + "\\u2010-\\u2027\\u2030-\\u205E\\u2190-\\u2BFF"
                                    + "\\u3001\\u3003\\u3008-\\u3020\\u3030"
                                    + "\\uFD3E\\uFD3F\\uFE45\\uFE46"
                                    + "]")
                    .removeAll(QUOTERS)
                    .remove('$')
                    .freeze();
    private static final UnicodeSet NEWLINE =
            new UnicodeSet("[\\u000A\\u000D\\u0085\\u2028\\u2029]").freeze();
    // private static final UnicodeSet DECIMAL = new UnicodeSet("[:Nd:]");
    private static final UnicodeSet NON_STRING = new UnicodeSet().addAll(WHITESPACE).addAll(SYNTAX);

    protected UnicodeSet whiteSpace = WHITESPACE;
    protected UnicodeSet syntax = SYNTAX;
    private UnicodeSet non_string = NON_STRING;

    private void fixSets() {
        if (syntax.containsSome(QUOTERS) || syntax.containsSome(whiteSpace)) {
            syntax =
                    ((UnicodeSet) syntax.clone()).removeAll(QUOTERS).removeAll(whiteSpace).freeze();
        }
        if (whiteSpace.containsSome(QUOTERS)) {
            whiteSpace = ((UnicodeSet) whiteSpace.clone()).removeAll(QUOTERS).freeze();
        }
        non_string = new UnicodeSet(syntax).addAll(whiteSpace).freeze();
    }

    public Tokenizer setSource(String source) {
        this.source = source;
        this.index = 0;
        return this; // for chaining
    }

    public Tokenizer setIndex(int index) {
        this.index = index;
        return this; // for chaining
    }

    public enum Result {
        CODEPOINT,
        IDENTIFIER,
        NUMBER,
        STRING,
        UNICODESET,
        DONE,
        UNTERMINATED_QUOTE,
        BACKEDUP_TOO_FAR,
        ILLEGAL_CHARACTER,
        INCOMPLETE_BACKSLASH,
    }

    public String toStringFull() {
        StringBuilder result = new StringBuilder(source.substring(0, index)).append("$$$");
        return toString(result).append(source.substring(index)).toString();
    }

    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    private StringBuilder toString(StringBuilder result) {
        result.append(lastValue);
        switch (lastValue) {
            case STRING:
            case IDENTIFIER:
                result.append(":«").append(getString()).append('»');
                break;
            case NUMBER:
                result.append(':').append(getNumber());
                break;
            case UNICODESET:
                result.append(':').append(getUnicodeSet().toPattern(false));
                break;
            case CODEPOINT:
                result.append(':').appendCodePoint(getCodePoint());
                break;
            case ILLEGAL_CHARACTER:
                result.append("\\x{").append(Utility.hex(getCodePoint())).append("}");
                break;
            default:
                break;
        }
        return result.append(backedup ? "@" : "");
    }

    public void backup() {
        if (backedup) {
            throw new IllegalArgumentException("backup too far");
        }
        backedup = true;
        nextIndex = index;
        index = lastIndex;
    }

    /*
    public int next2() {
        boolean backedupBefore = backedup;
        int result = next();
        System.out.println(toString(result, backedupBefore));
        return result;
    }
     */

    public Result next() {
        if (backedup) {
            backedup = false;
            index = nextIndex;
            if (DEBUG) {
                System.out.println("Tokenizer: " + this);
            }
            return lastValue;
        }
        int cp = 0;
        boolean inComment = false;
        // clean off any leading whitespace or comments
        while (true) {
            if (index >= source.length()) {
                lastValue = Result.DONE;
                if (DEBUG) {
                    System.out.println("Tokenizer: " + this);
                }
                return lastValue;
            }
            cp = nextChar();
            if (inComment) {
                if (NEWLINE.contains(cp)) inComment = false;
            } else {
                if (cp == '#') {
                    inComment = true;
                } else if (!whiteSpace.contains(cp)) {
                    break;
                }
            }
        }
        // record the last index in case we have to backup
        lastIndex = index;

        if (cp == '[' || cp == '\\') {
            ParsePosition pos = new ParsePosition(index - 1);
            unicodeSet = new UnicodeSet(source, pos, symbolTable);
            if (unicodeSet == null) {
                throw new NullPointerException();
            }
            index = pos.getIndex();
            lastValue = Result.UNICODESET;
            if (DEBUG) {
                System.out.println("Tokenizer: " + this);
            }
            return lastValue;
        }
        // get syntax character
        if (syntax.contains(cp)) {
            codePoint = cp;
            lastValue = Result.CODEPOINT;
            if (DEBUG) {
                System.out.println("Tokenizer: " + this);
            }
            return lastValue;
        }

        // get number, if there is one
        if (UCharacter.getType(cp) == Character.DECIMAL_DIGIT_NUMBER) {
            number = UCharacter.getNumericValue(cp);
            while (index < source.length()) {
                cp = nextChar();
                if (UCharacter.getType(cp) != Character.DECIMAL_DIGIT_NUMBER) {
                    index -= UTF16.getCharCount(cp); // BACKUP!
                    break;
                }
                number *= 10;
                number += UCharacter.getNumericValue(cp);
            }
            lastValue = Result.NUMBER;
            if (DEBUG) {
                System.out.println("Tokenizer: " + this);
            }
            return lastValue;
        }
        buffer.setLength(0);
        if (cp == QUOTE) {
            Result result = Result.UNTERMINATED_QUOTE;
            while (index < source.length()) {
                cp = nextChar();
                switch (result) {
                    case UNTERMINATED_QUOTE:
                        if (cp == QUOTE) {
                            lastValue = Result.STRING;
                            if (DEBUG) {
                                System.out.println("Tokenizer: " + this);
                            }
                            return lastValue;
                        } else if (cp == BSLASH) {
                            result = Result.INCOMPLETE_BACKSLASH;
                        } else {
                            UTF16.append(buffer, cp);
                        }
                        break;
                    case INCOMPLETE_BACKSLASH:
                        switch (cp) {
                            case 'n':
                                cp = '\n';
                                break;
                            case 'r':
                                cp = '\r';
                                break;
                            case 't':
                                cp = '\t';
                                break;
                            default:
                                break;
                        }
                        UTF16.append(buffer, cp);
                        result = Result.UNTERMINATED_QUOTE;
                        break;
                    default:
                        throw new InternalCldrException("Internal error");
                }
            }
            lastValue = result;
            if (DEBUG) {
                System.out.println("Tokenizer: " + this);
            }
            return lastValue;
        }

        if (UCharacter.isUnicodeIdentifierStart(cp) || cp == '$') {
            buffer.appendCodePoint(cp);
            while (index < source.length()) {
                cp = nextChar();
                if (UCharacter.isUnicodeIdentifierPart(cp) || cp == '$') {
                    buffer.appendCodePoint(cp);
                } else if (!UCharacter.isIdentifierIgnorable(cp)) {
                    index -= UTF16.getCharCount(cp); // BACKUP!
                    break;
                }
            }
            lastValue = Result.IDENTIFIER;
            if (DEBUG) {
                System.out.println("Tokenizer: " + this);
            }
            return lastValue;
        }
        codePoint = cp;
        lastValue = Result.ILLEGAL_CHARACTER;
        if (DEBUG) {
            System.out.println("Tokenizer: " + this);
        }
        return lastValue;
    }

    public String getString() {
        return lastValue != Result.STRING && lastValue != Result.IDENTIFIER
                ? null
                : buffer.toString();
    }

    public long getNumber() {
        return lastValue != Result.NUMBER ? Long.MIN_VALUE : number;
    }

    public UnicodeSet getUnicodeSet() {
        return lastValue != Result.UNICODESET ? null : unicodeSet;
    }

    public int getCodePoint() {
        return lastValue != Result.CODEPOINT && lastValue != Result.ILLEGAL_CHARACTER
                ? -1
                : codePoint;
    }

    public int nextCodePoint() {
        Tokenizer.Result type = next();
        return type != Result.CODEPOINT ? 0 : getCodePoint();
    }

    private int nextChar() {
        int cp = UTF16.charAt(source, index);
        index += UTF16.getCharCount(cp);
        return cp;
    }

    public int getIndex() {
        return index;
    }

    public String getSource() {
        return source;
    }

    public UnicodeSet getSyntax() {
        return syntax;
    }

    public UnicodeSet getWhiteSpace() {
        return whiteSpace;
    }

    public void setSyntax(UnicodeSet set) {
        syntax = set;
        fixSets();
    }

    public void setWhiteSpace(UnicodeSet set) {
        whiteSpace = set;
        fixSets();
    }

    public Set<String> getLookedUpItems() {
        return symbolTable.itemsLookedUp;
    }

    public void addSymbol(String var, String value, int start, int limit) {
        // the limit is after the ';', so remove it
        --limit;
        char[] body = new char[limit - start];
        value.getChars(start, limit, body, 0);
        symbolTable.add(var, body);
    }

    public class TokenSymbolTable implements SymbolTable {
        Map<String, char[]> contents = new HashMap<>();
        Set<String> itemsLookedUp = new HashSet<>();

        public void add(String var, char[] body) {
            // start from 1 to avoid the $
            contents.put(var.substring(1), body);
        }

        /* (non-Javadoc)
         * @see com.ibm.icu.text.SymbolTable#lookup(java.lang.String)
         */
        public char[] lookup(String s) {
            itemsLookedUp.add('$' + s);
            return (char[]) contents.get(s);
        }

        /* (non-Javadoc)
         * @see com.ibm.icu.text.SymbolTable#lookupMatcher(int)
         */
        public UnicodeMatcher lookupMatcher(int ch) {
            // TODO Auto-generated method stub
            return null;
        }

        /* (non-Javadoc)
         * @see com.ibm.icu.text.SymbolTable#parseReference(java.lang.String, java.text.ParsePosition, int)
         */
        public String parseReference(String text, ParsePosition pos, int limit) {
            int cp;
            int start = pos.getIndex();
            int i;
            for (i = start; i < limit; i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(text, i);
                if (!com.ibm.icu.lang.UCharacter.isUnicodeIdentifierPart(cp)) {
                    break;
                }
            }
            pos.setIndex(i);
            return text.substring(start, i);
        }
    }
}
