/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/utility/LengthFirstComparator.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.utility;

import java.util.Comparator;

public final class LengthFirstComparator implements Comparator {
    @Override
    public int compare(Object a, Object b) {
        final String as = (String) a;
        final String bs = (String) b;
        if (as.length() < bs.length()) {
            return -1;
        }
        if (as.length() > bs.length()) {
            return 1;
        }
        return as.compareTo(bs);
    }
}