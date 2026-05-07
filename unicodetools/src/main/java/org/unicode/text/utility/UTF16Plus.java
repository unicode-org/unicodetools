/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/utility/UTF16Plus.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.utility;

public final class UTF16Plus {
    public static boolean isSingleCodePoint(CharSequence s) {
        return s.length() == 1
                || (s.length() == 2 && Character.isSurrogatePair(s.charAt(0), s.charAt(1)));
    }

    public static boolean isCodePointBoundary(CharSequence s, int offset) {
        if (offset < 0 || offset > s.length()) {
            return false;
        }
        if (offset == 0 || offset == s.length()) {
            return true;
        }
        return !Character.isSurrogatePair(s.charAt(offset - 1), s.charAt(offset));
    }

    /** Throws an IllegalArgumentException if !isCodePointBoundary(). */
    public static void checkCodePointBoundary(CharSequence s, int offset) {
        if (!isCodePointBoundary(s, offset)) {
            throw new IllegalArgumentException(
                    "not a code point boundary:\n" + s.toString() + "\nat offset " + offset);
        }
    }

    /** Equivalent to Character.toString(s.codePointAt(offset)). */
    public static String codePointSubstringAt(String s, int offset) {
        int limit = Character.offsetByCodePoints(s, offset, 1);
        return s.substring(offset, limit);
    }

    public static StringBuilder insertCodePoint(StringBuilder sb, int offset, int c) {
        if (Character.isBmpCodePoint(c)) {
            sb.insert(offset, (char) c);
        } else {
            sb.insert(offset++, Character.highSurrogate(c));
            sb.insert(offset, Character.lowSurrogate(c));
        }
        return sb;
    }
}
