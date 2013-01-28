/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCA/RuleComparator.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.UCA;

import org.unicode.text.utility.Utility;

public final class RuleComparator implements java.util.Comparator {

	@Override
	public int compare(Object s, Object t) {
		final String ss = (String)s;
		final String tt = (String)t;

		// compare just the initial portions of each level, FIRST
		// only if there is a difference outside of the initial level do we stop
		// we assume that there are the same number of levels!!

		int si = 0;
		int ti = 0;
		final int result = 0;
		try {
			while (si < ss.length() && ti < tt.length()) {
				final char cs = ss.charAt(si++);
				final char ct = tt.charAt(ti++);

				if (cs == ct) {
					continue;
				}
				/*
                if (cs == 0) {
                    if (result == 0) result = -1;
                    while (ct != 0 && ti < tt.length()) {
                        ct = tt.charAt(ti++);
                    }
                    continue;
                }
                if (ct == 0) {
                    if (result == 0) result = 1;
                    while (cs != 0 && si < ss.length()) {
                        cs = ss.charAt(si++);
                    }
                    continue;
                }
				 */
				if (cs < ct) {
					return -1;
				}
				return  1;
			}
		} catch (final StringIndexOutOfBoundsException e) {
			System.out.println("WHOOPS: ");
			System.out.println(si + ", " + Utility.hex(ss));
			System.out.println(ti + ", " + Utility.hex(tt));
		}
		if (result != 0) {
			return result;
		}
		if (ss.length() > tt.length()) {
			return 1;
		}
		if (ss.length() < tt.length()) {
			return -1;
		}
		return 0;
	}
}