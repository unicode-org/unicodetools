/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/utility/UTF32.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.utility;

import com.ibm.icu.text.UTF16;

/**
 * Utility class for dealing with UTF-16 strings and code points.
 * Provides only methods that are not available in Java itself, nor in ICU.
 */
public final class UTF32 {
    /**
     * Determines whether the code point is a surrogate.
     * TODO: Propose again to widen UTF16.isSurrogate(char) to take an int.
     * Or maybe add UTF16.isSurrogateCodePoint(int) or isCodePointSurrogate(int).
     *
     * @return true iff the input character is a surrogate.
     * @param ch the input character.
     */
    public static boolean isSurrogate(int char32) {
        return Character.MIN_SURROGATE <= char32 && char32 <= Character.MAX_SURROGATE;
    }

    /**
     * Convenience method corresponding to String.valueOf(char). It returns a one or two char string containing
     * the UTF-32 value. If the input value can't be converted, it substitutes the replacement character U+FFFD.
     *
     * @return string value of char32
     * @param ch the input character.
     * @deprecated Try to use UTF16.valueOf(char32), but that throws an exception for illegal code points.
     */
    @Deprecated
    public static String valueOf32(int char32) {
        try {
            return UTF16.valueOf(char32);
        } catch (IllegalArgumentException e) {
            return "\uFFFD";
        }
    }

    /**
     * Prevent instance from being created.
     */
    private UTF32() {}
};
