/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /home/cvsroot/unicodetools/org/unicode/text/utility/LengthFirstComparator.java,v $
* $Date: 2009-08-18 23:38:46 $
* $Revision: 1.4 $
*
*******************************************************************************
*/

package org.unicode.text.utility;

import java.util.Comparator;

public final class LengthFirstComparator implements Comparator {
	public int compare(Object a, Object b) {
		String as = (String) a;
		String bs = (String) b;
		if (as.length() < bs.length()) return -1;
		if (as.length() > bs.length()) return 1;
		return as.compareTo(bs);
	}
}