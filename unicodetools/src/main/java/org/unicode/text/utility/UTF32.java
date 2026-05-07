/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/utility/UTF32.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.utility;

/**
 * Utility class for dealing with UTF-16 strings and code points. Provides only methods that are not
 * available in Java itself, nor in ICU.
 */
public final class UTF32 {
    /**
     * Convenience method corresponding to String.valueOf(char). It returns a one or two char string
     * containing the UTF-32 value. If the input value can't be converted, it substitutes the
     * replacement character U+FFFD.
     *
     * @return string value of char32
     * @param ch the input character.
     * @deprecated Try to use Character.toString(char32), but that throws an exception for illegal
     *     code points.
     */
    @Deprecated
    public static String valueOf32(int char32) {
        try {
            return Character.toString(char32);
        } catch (IllegalArgumentException e) {
            return "\uFFFD";
        }
    }

    /** Prevent instance from being created. */
    private UTF32() {}
}
;
