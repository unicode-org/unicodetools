package org.unicode.text.utility;

import com.ibm.icu.impl.UnicodeRegex;
import com.ibm.icu.text.UnicodeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lenient parsing of Unicode values.
 *
 * <pre> set   := range (sep? range)+
 * range := code rangSep code | code rangeSep code
 * code  :=
 *          literal     – non-ASCII, non-whitespace
 *        | U+XXXX..    – must not be followed by X (\b)
 *        | XXXX        – no a-f, must not be followed by X (\b)
 *        | \\uXXXX
 *        | \\UXXXXXX
 *        | \\x{X...}
 *        | \\u{X...}
 * rSep  := {whitespace}* ..? {whitespace}*
 * sep   := {whitespace}* ,? {whitespace}*</pre>
 *
 * @author markdavis
 */
public class UnicodeSetParser {
    private static final Pattern HEX_PATTERN =
            Pattern.compile(
                    UnicodeRegex.fix("((?:\\h|[:di:])+|,)")
                            + "|(\\.\\.|-)"
                            + "|([A-F0-9]{4,6})(?![A-F0-9])" // code points
                            + "|U\\+([a-fA-F0-9]{4,6})(?![a-fA-F0-9])"
                            + "|\\\\x([a-fA-F0-9]{2})"
                            + "|\\\\u([a-fA-F0-9]{4})"
                            + "|\\\\U([a-fA-F0-9]{6})"
                            + "|\\\\u\\{([a-fA-F0-9]{1,6})\\}"
                            + "|\\\\x\\{([a-fA-F0-9]{1,6})\\}"
                            + "|([^\\h\\v\\x{20}-\\x{7F}])" // any non-space, ASCII
                    );

    private static final Pattern HEX_PATTERN_ANY =
            Pattern.compile(
                    UnicodeRegex.fix("((?:\\h|[:di:])+|,)")
                            + "|(\\.\\.|-)"
                            + "|([A-F0-9]{4,6})(?![A-F0-9])" // code points
                            + "|U\\+([a-fA-F0-9]{4,6})(?![a-fA-F0-9])"
                            + "|\\\\x([a-fA-F0-9]{2})"
                            + "|\\\\u([a-fA-F0-9]{4})"
                            + "|\\\\U([a-fA-F0-9]{6})"
                            + "|\\\\u\\{([a-fA-F0-9]{1,6})\\}"
                            + "|\\\\x\\{([a-fA-F0-9]{1,6})\\}"
                            + "|([^\\h])" // any non-space, ASCII
                    );
    static final int SEP = 1, RANGE = 2, START = 3, ANY = 10;

    final Pattern hexPattern;

    public UnicodeSetParser(boolean allowAny) {
        hexPattern = allowAny ? HEX_PATTERN_ANY : HEX_PATTERN;
    }

    /**
     * Convert a string with a mixture of hex and normal characters. Anything like the following is
     * converted from hex to chars and all spaces are removed hexChar = \b[A-F0-9]{4,6}\b |
     * U+[a-fA-F0-9]{4,6} | \\u[a-fA-F0-9]{4} | \\U[a-fA-F0-9]{6} | \\u{[a-fA-F0-9]{1,6}
     *
     * @param hexOrChars
     * @return
     */
    public UnicodeSet parse(String hexOrChars, UnicodeSet target) {
        final Matcher hex = hexPattern.matcher("");
        target.clear();
        hex.reset(hexOrChars);
        int lastOffset = 0;
        int lastCp = -1;
        boolean range = false;
        while (hex.find()) {
            if (hex.start() != lastOffset) {
                // skipped something, fail
                throw new IllegalArgumentException(
                        "Unexpected characters at "
                                + lastOffset
                                + ": "
                                + hexOrChars.substring(lastOffset, hex.start()));
            }
            lastOffset = hex.end();
            if (hex.group(SEP) != null) {
                continue;
            }
            if (hex.group(RANGE) != null) {
                if (lastCp < 0) {
                    throw new IllegalArgumentException(
                            "Illegal range at" + lastOffset + ": " + hex.group(0));
                }
                range = true;
                continue;
            }
            for (int i = START; i <= hex.groupCount(); ++i) {
                final String group = hex.group(i);
                if (group != null) {
                    int num = i == ANY ? group.codePointAt(0) : Integer.parseInt(group, 16);
                    if (range) {
                        if (lastCp >= num) {
                            throw new IllegalArgumentException(
                                    "Second of range must be greater, at "
                                            + lastOffset
                                            + ": "
                                            + hex.group(0));
                        }
                        target.add(lastCp + 1, num);
                        range = false;
                        lastCp = -1;
                    } else {
                        target.add(num);
                        lastCp = num;
                    }
                    break;
                }
            }
        }
        if (lastOffset != hexOrChars.length()) {
            throw new IllegalArgumentException(
                    "Unexpected characters at "
                            + lastOffset
                            + ": "
                            + hexOrChars.substring(lastOffset, hex.start()));
        }
        return target;
    }

    public StringBuilder parseString(String hexOrChars, StringBuilder target) {
        final Matcher hex = hexPattern.matcher("");
        target.setLength(0);
        hex.reset(hexOrChars);
        int lastOffset = 0;
        while (hex.find()) {
            if (hex.start() != lastOffset || hex.group(RANGE) != null) {
                // skipped something, fail
                throw new IllegalArgumentException(
                        "Unexpected characters at "
                                + lastOffset
                                + ": "
                                + hexOrChars.substring(lastOffset, hex.start()));
            }
            lastOffset = hex.end();
            if (hex.group(SEP) != null) {
                continue;
            }
            for (int i = START; i <= hex.groupCount(); ++i) {
                final String group = hex.group(i);
                if (group != null) {
                    if (i == ANY) {
                        target.append(group);
                    } else {
                        target.appendCodePoint(Integer.parseInt(group, 16));
                    }
                    break;
                }
            }
        }
        if (lastOffset != hexOrChars.length()) {
            throw new IllegalArgumentException(
                    "Unexpected characters at "
                            + lastOffset
                            + ": "
                            + hexOrChars.substring(lastOffset, hex.start()));
        }
        return target;
    }
}
