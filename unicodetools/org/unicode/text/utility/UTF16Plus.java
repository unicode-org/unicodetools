/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/utility/UTF16Plus.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.utility;

public final class UTF16Plus {
    public static boolean isSingleCodePoint(CharSequence seq) {
        final int length = seq.length();
        return length > 0 && Character.offsetByCodePoints(seq, 0, 1) == length;
    }
}
