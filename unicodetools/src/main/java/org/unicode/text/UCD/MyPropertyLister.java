/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/MyPropertyLister.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.UCD;
import java.io.PrintWriter;

final class MyPropertyLister extends PropertyLister {

    static final boolean BRIDGE = false;

    private final int propMask;

    private boolean isDefaultValue = false;

    private final UCDProperty up;

    public MyPropertyLister(UCD ucd, int propMask, PrintWriter output) {
        this.propMask = propMask;
        this.output = output;
        ucdData = ucd;
        up = UnifiedBinaryProperty.make(propMask, ucd);
        if (propMask < COMBINING_CLASS)
        {
            usePropertyComment = false; // skip gen cat
        }
        isDefaultValue = up.isDefaultValue();
    }

    @Override
    public String headerString() {
        final int main = (propMask & 0xFF00);
        if (main == COMBINING_CLASS) {
            String s = UCD.getCombiningClassID_fromIndex((short)(propMask & 0xFF), LONG);
            if (s.charAt(0) <= '9') {
                s = "Other Combining Class";
            }
            return "# " + s;
        } else if (main == BINARY_PROPERTIES) {
            return "";
        } else if (main == JOINING_GROUP) {
            return "";
        } else {
            return "";
            /*
            String shortID = up.getName(SHORT);
            String longID = up.getName(LONG);
            return "# ???? " + shortID + (shortID.equals(longID) ? "" : "\t(" + longID + ")");
             */
        }
    }

    @Override
    public String valueName(int cp) {
        if (up.getValueType() == BINARY_PROP) {
            return up.getName();
        }
        return up.getValue(cp);
    }

    @Override
    public String missingValueName() {
        return up.getValue(NORMAL);
    }

    @Override
    public String optionalComment(int cp) {
        if (propMask < COMBINING_CLASS)
        {
            return ""; // skip gen cat
        }
        final int cat = ucdData.getCategory(cp);
        if (cat == Lt || cat == Ll || cat == Lu) {
            return "L&";
        }
        return ucdData.getCategoryID(cp);
    }

    /*
    public String optionalName(int cp) {
        if ((propMask & 0xFF00) == DECOMPOSITION_TYPE) {
            return Utility.hex(ucdData.getDecompositionMapping(cp));
        } else {
            return "";
        }
    }
     */

    @Override
    public byte status(int cp) {
        //if (cp == 0xFFFF) {
        //    System.out.println("# " + Utility.hex(cp));
        //}
        final byte cat = ucdData.getCategory(cp);
        //if (cp == 0x0385) {
        //    System.out.println(Utility.hex(firstRealCp));
        //}

        if (isDefaultValue
                && cat == Cn
                && propMask != (BINARY_PROPERTIES | Noncharacter_Code_Point)
                && propMask != (BINARY_PROPERTIES | Other_Default_Ignorable_Code_Point)
                && propMask != (CATEGORY | Cn)) {
            if (BRIDGE) {
                return CONTINUE;
            } else {
                return EXCLUDE;
            }
        }

        final boolean inSet = up.hasValue(cp);

        /*
        if (cp >= 0x1D400 && cp <= 0x1D7C9 && cat != Cn) {
            if (propMask == (SCRIPT | LATIN_SCRIPT)) inSet = cp <= 0x1D6A3;
            else if (propMask == (SCRIPT | GREEK_SCRIPT)) inSet = cp > 0x1D6A3;
        }
         */
        /* HACK
1D400;MATHEMATICAL BOLD CAPITAL A;Lu;0;L;<font> 0041;;;;N;;;;;
1D6A3;MATHEMATICAL MONOSPACE SMALL Z;Ll;0;L;<font> 007A;;;;N;;;;;
1D6A8;MATHEMATICAL BOLD CAPITAL ALPHA;Lu;0;L;<font> 0391;;;;N;;;;;
1D7C9;MATHEMATICAL SANS-SERIF BOLD ITALIC PI SYMBOL;Ll;0;L;<font> 03D6;;;;N;;;;;
         */

        if (!inSet) {
            return EXCLUDE;
        }
        return INCLUDE;
    }


}

