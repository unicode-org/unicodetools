/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /home/cvsroot/unicodetools/org/unicode/text/UCA/AbbreviatedUnicodeSetIterator.java,v $ 
*
*******************************************************************************
*/

package org.unicode.text.UCA;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class AbbreviatedUnicodeSetIterator extends UnicodeSetIterator {

    private boolean abbreviated;
    private int perRange;

    public AbbreviatedUnicodeSetIterator() {
        super();
        abbreviated = false;
    }

    public void reset(UnicodeSet newSet) {
        reset(newSet, false);
    }

    public void reset(UnicodeSet newSet, boolean abb) {
        reset(newSet, abb, 100);
    }

    public void reset(UnicodeSet newSet, boolean abb, int density) {
        super.reset(newSet);
        abbreviated = abb;
        perRange = newSet.getRangeCount();
        if (perRange != 0) {
            perRange = density / perRange;
        }
    }

    protected void loadRange(int myRange) {
        super.loadRange(myRange);
        if (abbreviated && (endElement > nextElement + perRange)) {
            endElement = nextElement + perRange;
        }
    }
}
