/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/DiffPropertyLister.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.UCD;
import java.io.PrintWriter;

import com.ibm.icu.text.UnicodeSet;

class DiffPropertyLister extends PropertyLister {
    private UCD oldUCD;
    private final UnicodeSet set = new UnicodeSet();
    private static final int NOPROPERTY = -1;

    public DiffPropertyLister(String oldUCDName, String newUCDName, PrintWriter output, int property) {
        this.output = output;
        ucdData = UCD.make(newUCDName);
        if (property != NOPROPERTY) {
            newProp = DerivedProperty.make(property, ucdData);
        }

        if (oldUCDName != null) {
            oldUCD = UCD.make(oldUCDName);
            if (property != NOPROPERTY) {
                oldProp = DerivedProperty.make(property, oldUCD);
            }
        }
        breakByCategory = property != NOPROPERTY;
        useKenName = false;
        usePropertyComment = false;
    }

    public DiffPropertyLister(String oldUCDName, String newUCDName, PrintWriter output) {
        this(oldUCDName, newUCDName, output, NOPROPERTY);
    }

    public UnicodeSet getSet() {
        return set;
    }

    @Override
    public String valueName(int cp) {
        return major_minor_only(ucdData.getVersion());
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

    UCDProperty newProp = null;
    UCDProperty oldProp = null;
    String value = "";

    @Override
    public String optionalComment(int cp) {
        String normal = super.optionalComment(cp);
        if (oldUCD != null && breakByCategory) {
            final byte modCat = oldUCD.getModCat(cp, breakByCategory ? CASED_LETTER_MASK : 0);
            normal = oldUCD.getModCatID_fromIndex(modCat) + "/" + normal;
        }
        return normal;
    }


    @Override
    byte getModCat(int cp) {
        final byte result = ucdData.getModCat(cp, breakByCategory ? CASED_LETTER_MASK : -1);
        //System.out.println(breakByCategory + ", " + ucdData.getModCatID_fromIndex(result));
        return result;
    }


    @Override
    public byte status(int cp) {
        if (newProp == null) {
            if (ucdData.isAllocated(cp) && (oldUCD == null || !oldUCD.isAllocated(cp))) {
                set.add(cp);
                return INCLUDE;
            } else {
                return EXCLUDE;
            }
        }

        // just look at property differences among allocated characters

        if (!ucdData.isAllocated(cp)) {
            return EXCLUDE;
        }
        if (!oldUCD.isAllocated(cp)) {
            return EXCLUDE;
        }

        final String val = newProp.getValue(cp);
        final String oldVal = oldProp.getValue(cp);
        if (!oldVal.equals(val)) {
            set.add(cp);
            return INCLUDE;
        }
        return EXCLUDE;

        /*if (cp == 0xFFFF) {
            System.out.println("# " + Utility.hex(cp));
        }
         */
    }

    @Override
    public String headerString() {
        String result;
        if (oldUCD != null) {
            result = "# Differences between "
                    + major_minor_only(ucdData.getVersion())
                    + " and "
                    + major_minor_only(oldUCD.getVersion());
        } else {
            result = "# Designated as of "
                    + major_minor_only(ucdData.getVersion())
                    + " [excluding removed Hangul Syllables]";
        }
        //System.out.println("hs: " + result);
        return result;
    }

    /*
    public int print() {
        String status;
        if (oldUCD != null) {
            status = "# Differences between " + ucdData.getVersion() + " and " + oldUCD.getVersion();
        } else {
            status = "# Allocated as of " + ucdData.getVersion();
        }
        output.println();
        output.println();
        output.println(status);
        output.println();
        System.out.println(status);
        int count = super.print();
        output.println();
        if (oldUCD != null) {
            output.println("# Total " + count + " new code points allocated in " + ucdData.getVersion());
        } else {
            output.println("# Total " + count + " code points allocated in " + ucdData.getVersion());
        }

        output.println();
        return count;
    }
     */

    private String major_minor_only(String s) {
        if (newProp != null) {
            return s;
        }

        return s.substring(0, s.lastIndexOf('.'));
    }

}

