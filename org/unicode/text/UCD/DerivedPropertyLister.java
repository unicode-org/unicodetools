/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/DerivedPropertyLister.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.UCD;
import java.io.PrintWriter;

final class DerivedPropertyLister extends PropertyLister {
	static final boolean BRIDGE = false;

	//static int enum = 0;

	//private int propMask;
	//private DerivedProperty dprop;
	private final UCDProperty uprop;
	int width;
	boolean varies;

	public DerivedPropertyLister(UCD ucd, int propMask, PrintWriter output) {
		//this.propMask = propMask;
		this.output = output;
		ucdData = ucd;
		// this.dprop = new DerivedProperty(ucd);
		uprop = DerivedProperty.make(propMask, ucd);
		varies = uprop.getValueType() < BINARY_PROP;

		width = super.minPropertyWidth();
		switch (propMask) {
		case UCD_Types.GenNFD: case UCD_Types.GenNFC: case UCD_Types.GenNFKD: case UCD_Types.GenNFKC:
			alwaysBreaks = true;
			break;
		case UCD_Types.FC_NFKC_Closure:
			alwaysBreaks = true;
			width = 21;
			break;
		case UCD_Types.QuickNFC: case UCD_Types.QuickNFKC:
			width = 11;
			break;
		}
	}

	@Override
	public String valueName(int cp) {
		return uprop.getListingValue(cp);
	}

	//public String optionalComment(int cp) {
	//    return super.optionalComment(cp) + " [" + ucdData.getCodeAndName(computedValue) + "]";
	//}


	@Override
	public int minPropertyWidth() {
		return width;
	}


	/*
    public String optionalComment(int cp) {
        String id = ucdData.getCategoryID(cp);
        if (UCD.mainCategoryMask(ucdData.getCategory(cp)) == LETTER_MASK) return id.substring(0,1) + "*";
        return id;
    }
	 */
	/*
    public String optionalName(int cp) {
        if ((propMask & 0xFF00) == DECOMPOSITION_TYPE) {
            return Utility.hex(ucdData.getDecompositionMapping(cp));
        } else {
            return "";
        }
    }
	 */

	String last;

	@Override
	public byte status(int cp) {
		if (!uprop.hasUnassigned() && !ucdData.isAssigned(cp)) {
			return EXCLUDE;
		}
		if (!varies) {
			return uprop.hasValue(cp) ? INCLUDE : EXCLUDE;
		}
		final String prop = uprop.getValue(cp);
		if (prop.length() == 0) {
			return EXCLUDE;
		}
		if (prop.equals(last)) {
			return INCLUDE;
		}
		last = prop;
		return BREAK;
	}

	/*
    static Map computedValue = new HashMap();
    static String getComputedValue(int cp) {
        return (String) computedValue.get(new Integer(cp));
    }
    static void setComputedValue(int cp, String value) {
        computedValue.put(new Integer(cp), value);
    }
    static String lastValue = "";
    static String currentValue = "";

    StringBuffer foldBuffer = new StringBuffer();

	 */
}

