/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/UnifiedProperty.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.UCD;

import com.ibm.icu.text.UnicodeSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.text.utility.ChainException;
import org.unicode.text.utility.Utility;

public final class UnifiedProperty extends UCDProperty {
    int majorProp;

    // DerivedProperty dp;

    public static UCDProperty make(int propMask) {
        return make(propMask, Default.ucd());
    }

    public static UCDProperty make(int propMask, UCD ucd) {
        if ((propMask & 0xFF00) == (BINARY_PROPERTIES & 0xFF00)) {
            return UnifiedBinaryProperty.make(propMask, ucd);
        }
        if ((propMask & 0xFF00) == DERIVED) {
            return DerivedProperty.make(propMask & 0xFF, ucd);
        }
        if (!isDefined(propMask, ucd)) {
            return null;
        }
        return getCached(propMask, ucd);
    }

    public static UCDProperty make(String propID, UCD ucd) {
        return make(getPropmask(propID, ucd), ucd);
    }

    public static UnicodeSet getSet(int propMask, UCD ucd) {
        final UCDProperty up = make(propMask, ucd);
        return up.getSet();
    }

    public static UnicodeSet getSet(String propID, UCD ucd) {
        return getSet(getPropmask(propID, ucd), ucd);
    }

    private static Map propNameCache = null;
    private static Set availablePropNames = new TreeSet();

    public static Collection getAvailablePropertiesAliases(Collection result, UCD ucd) {
        if (propNameCache == null) {
            cacheNames(ucd);
        }
        result.addAll(availablePropNames);
        return result;
    }

    public static int getPropmask(String propID, UCD ucd) {

        // cache the names
        if (propNameCache == null) {
            cacheNames(ucd);
        }

        propID = Utility.getSkeleton(propID);
        final Integer indexObj = (Integer) propNameCache.get(propID);
        if (indexObj == null) {
            throw new IllegalArgumentException("No property found for " + propID);
        }
        return indexObj.intValue();
    }

    private static void cacheNames(UCD ucd) {
        // System.out.println("Caching Property Names");
        propNameCache = new HashMap();

        for (int i = 0; i < LIMIT_ENUM; ++i) {
            final UCDProperty up = UnifiedProperty.make(i, ucd);
            if (up == null) {
                continue;
            }
            if (!up.isStandard()) {
                continue;
            }
            // if (up.getValueType() < BINARY_PROP) continue;
            final Integer result = new Integer(i);

            final String longRaw = up.getPropertyName(LONG);
            final String longName = Utility.getSkeleton(longRaw);
            final String shortRaw = up.getPropertyName(SHORT);
            final String shortName = Utility.getSkeleton(shortRaw);
            // System.out.println("Caching Names: " + longRaw + ", " + shortRaw);
            if (longName != null && !propNameCache.keySet().contains(longName)) {
                propNameCache.put(longName, result);
            }

            if (shortName != null && !propNameCache.keySet().contains(shortName)) {
                propNameCache.put(shortName, result);
            }

            final String key = longRaw != null ? longRaw : shortRaw;
            if (key.isEmpty()) {
                throw new IllegalArgumentException();
            }
            availablePropNames.add(key);
        }
        // System.out.println("Done Caching");
    }

    static Map cache = new HashMap();
    static UCD lastUCD = null;
    static int lastPropMask = -1;
    static UnifiedProperty lastValue = null;
    static Clump probeClump = new Clump();

    static class Clump {
        int prop;
        UCD ucd;

        @Override
        public boolean equals(Object other) {
            final Clump that = (Clump) other;
            return (that.prop == prop && ucd.equals(that));
        }
    }

    private static UnifiedProperty getCached(int propMask, UCD ucd) {

        // System.out.println(ucd);
        if (ucd.equals(lastUCD) && propMask == lastPropMask) {
            return lastValue;
        }
        probeClump.prop = propMask;
        probeClump.ucd = ucd;
        UnifiedProperty dp = (UnifiedProperty) cache.get(probeClump);
        if (dp == null) {
            dp = new UnifiedProperty(propMask, ucd);
            cache.put(probeClump, dp);
            probeClump = new Clump();
        }
        lastUCD = ucd;
        lastValue = dp;
        lastPropMask = propMask;
        return dp;
    }

    /////////////////////////////////

    private UnifiedProperty(int propMask, UCD ucdin) {
        ucd = ucdin;
        majorProp = propMask >> 8;

        // System.out.println("A: " + getValueType());
        if (majorProp <= (JOINING_GROUP >> 8)
                || majorProp == SCRIPT >> 8
                || majorProp == (HANGUL_SYLLABLE_TYPE >> 8)) {
            setValueType(FLATTENED_BINARY_PROP);
            // System.out.println("B: " + getValueType());
        }

        name = UCD_Names.UNIFIED_PROPERTIES[majorProp];
        shortName = UCD_Names.SHORT_UNIFIED_PROPERTIES[majorProp];
    }

    private static boolean isDefined(int propMask, UCD ucd) {
        final int majorProp = propMask >> 8;
        switch (majorProp) {
            case CATEGORY >> 8:
            case COMBINING_CLASS >> 8:
            case BIDI_CLASS >> 8:
            case DECOMPOSITION_TYPE >> 8:
            case NUMERIC_TYPE >> 8:
            case EAST_ASIAN_WIDTH >> 8:
            case LINE_BREAK >> 8:
            case JOINING_TYPE >> 8:
            case JOINING_GROUP >> 8:
            case SCRIPT >> 8:
            case AGE >> 8:
            case HANGUL_SYLLABLE_TYPE >> 8:
                return true;
                /*
                 case DERIVED>>8:
                   UnicodeProperty up = DerivedProperty.make(propValue, ucd);
                   if (up == null) break;
                   return up.hasValue(cp);
                */
        }
        return false;
    }

    @Override
    public boolean hasValue(int cp) {
        throw new ChainException(
                "Can't call 'hasValue' on non-binary property {0}",
                new Object[] {new Integer(majorProp)});
    }

    @Override
    public String getFullName(byte style) {
        String pre = "";
        final String preShort = getPropertyName(SHORT);
        final String preLong = getPropertyName(LONG);
        if (style < LONG) {
            pre = preShort;
        } else if (style == LONG || preShort.equals(preLong)) {
            pre = preLong;
        } else {
            pre = preShort + "(" + preLong + ")";
        }
        return pre;
    }

    @Override
    public String getValue(int cp, byte style) {
        switch (majorProp) {
            case CATEGORY >> 8:
                return UCD.getCategoryID_fromIndex(ucd.getCategory(cp), style);
            case COMBINING_CLASS >> 8:
                return UCD.getCombiningClassID_fromIndex(ucd.getCombiningClass(cp), style);
            case BIDI_CLASS >> 8:
                return UCD.getBidiClassID_fromIndex(ucd.getBidiClass(cp), style);
            case DECOMPOSITION_TYPE >> 8:
                return UCD.getDecompositionTypeID_fromIndex(ucd.getDecompositionType(cp), style);
            case NUMERIC_TYPE >> 8:
                return UCD.getNumericTypeID_fromIndex(ucd.getNumericType(cp), style);
            case EAST_ASIAN_WIDTH >> 8:
                return UCD.getEastAsianWidthID_fromIndex(ucd.getEastAsianWidth(cp), style);
            case LINE_BREAK >> 8:
                return UCD.getLineBreakID_fromIndex(ucd.getLineBreak(cp), style);
            case JOINING_TYPE >> 8:
                return UCD.getJoiningTypeID_fromIndex(ucd.getJoiningType(cp), style);
            case JOINING_GROUP >> 8:
                return UCD.getJoiningGroupID_fromIndex(ucd.getJoiningGroup(cp), style);
            case SCRIPT >> 8:
                return UCD.getScriptID_fromIndex(ucd.getScript(cp), style);
            case AGE >> 8:
                return UCD.getAgeID_fromIndex(ucd.getAge(cp), style);
            case HANGUL_SYLLABLE_TYPE >> 8:
                return ucd.getHangulSyllableTypeID(cp, style);
            default:
                throw new IllegalArgumentException("Internal Error");
        }
    }
}
