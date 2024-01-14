package org.unicode.props;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map.Entry;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.Decomposition_Type_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.UCD.NormalizationData;
import org.unicode.text.UCD.UCD;
import org.unicode.text.utility.ChainException;
import org.unicode.text.utility.Utility;

/**
 * This is a modified version of NormalizationDataStandard, making the minimal changes to ensure
 * that the logic is identical for IndexUnicodeProperties as it was for UCD.java
 */
public class NormalizationDataIUP implements NormalizationData {
    private final HashMap<Long, Integer> compTable = new HashMap<Long, Integer>();
    private final BitSet isSecond = new BitSet();
    private final BitSet isFirst = new BitSet();
    private final BitSet canonicalRecompose = new BitSet();
    private final BitSet compatibilityRecompose = new BitSet();

    private final VersionInfo versionInfo;
    private final String version;
    private final UnicodeMap<Decomposition_Type_Values> decompType;
    private final UnicodeMap<String> decompMap;
    private final UnicodeMap<Integer> ccc;
    private final UnicodeMap<General_Category_Values> gc;

    public NormalizationDataIUP(IndexUnicodeProperties factory) {
        versionInfo = factory.getUcdVersion();
        version = versionInfo.getVersionString(2, 2);

        gc = factory.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
        UnicodeSet compExclude = factory.loadEnumSet(UcdProperty.Composition_Exclusion, Binary.Yes);
        decompType =
                factory.loadEnum(UcdProperty.Decomposition_Type, Decomposition_Type_Values.class);
        decompMap = factory.load(UcdProperty.Decomposition_Mapping);
        ccc = factory.loadInt(UcdProperty.Canonical_Combining_Class);

        for (int i = 0; i < 0x10FFFF; ++i) {
            // if (!ucd.isAssigned(i)) {
            final General_Category_Values gcValue = gc.getValue(i);
            if (gcValue == UcdPropertyValues.General_Category_Values.Unassigned) {
                continue;
            }
            //            if (ucd.isPUA(i)) {
            if (gcValue == UcdPropertyValues.General_Category_Values.Private_Use) {
                continue;
            }

            // if (UCD.isNonLeadJamo(i)) {
            if (isNonLeadJamo(i)) {
                isSecond.set(i);
            }
            // if (UCD.isLeadingJamoComposition(i)) {
            if (isLeadingJamoComposition(i)) {
                isFirst.set(i);
            }
            //            final byte dt = ucd.getDecompositionType(i);
            //            if (dt != UCD_Types.CANONICAL) {
            if (decompType.get(i) == Decomposition_Type_Values.Canonical) {
                continue;
            }
            // if (!ucd.getBinaryProperty(i, UCD_Types.CompositionExclusion)) {
            if (compExclude.contains(i)) {
                continue;
            }
                try {
                    // final String s = ucd.getDecompositionMapping(i);
                    final String s = decompMap.get(i);
                    if (s.equals("<code point>")) { // could optimize
                        continue;
                    }
                    final int len = UTF16.countCodePoint(s);
                    if (len != 2) {
                        if (len > 2) {
                            if (versionInfo.compareTo(VersionInfo.getInstance(3))
                                    >= 0) { // version >= 3.0.0
                                throw new IllegalArgumentException("BAD LENGTH: " + len + " for " + Utility.hex(s));
                            }
                        }
                        continue;
                    }
                    final int a = UTF16.charAt(s, 0);
                    // if (ucd.getCombiningClass(a) != 0) {
                    if (ccc.get(a) != 0) {
                        continue;
                    }
                    isFirst.set(a);

                    final int b = UTF16.charAt(s, UTF16.getCharCount(a));
                    isSecond.set(b);

                    // have a recomposition, so set the bit
                    canonicalRecompose.set(i);

                    // set the compatibility recomposition bit
                    // ONLY if the component characters
                    // don't compatibility decompose

                    //                    if (ucd.getDecompositionType(a) <= UCD_Types.CANONICAL
                    //                            && ucd.getDecompositionType(b) <=
                    // UCD_Types.CANONICAL) {
                    Decomposition_Type_Values decompA = decompType.get(a);
                    if (decompA == null || decompA == Decomposition_Type_Values.Canonical) {
                        Decomposition_Type_Values decompB = decompType.get(b);
                        if (decompB == null || decompB == Decomposition_Type_Values.Canonical) {
                            compatibilityRecompose.set(i);
                        }
                    }

                    final long key = (((long) a) << 32) | b;

                    /*if (i == '\u1E0A' || key == 0x004400000307) {
                        System.out.println(Utility.hex(s));
                        System.out.println(Utility.hex(i));
                        System.out.println(Utility.hex(key));
                    }*/
                    compTable.put(new Long(key), new Integer(i));
                } catch (final Exception e) {
                    throw new ChainException("Error: {0}", new Object[] {Utility.hex(i)}, e);
                }

        }
        // process compatibilityRecompose
        // have to do this afterwards, since we don't know whether the pieces
        // are allowable until we have processed all the characters
    }

    /* (non-Javadoc)
     * @see org.unicode.text.UCD.NormalizationData#getUCDVersion()
     */
    @Override
    public String getUCDVersion() {
        return version;
    }

    /* (non-Javadoc)
     * @see org.unicode.text.UCD.NormalizationData#getCanonicalClass(int)
     */
    @Override
    public short getCanonicalClass(int cp) {
        //         return ucd.getCombiningClass(cp);
        return (short) (int) ccc.get(cp);
    }

    /* (non-Javadoc)
     * @see org.unicode.text.UCD.NormalizationData#isTrailing(int)
     */
    @Override
    public boolean isTrailing(int cp) {
        return isSecond.get(cp);
    }

    /* (non-Javadoc)
     * @see org.unicode.text.UCD.NormalizationData#isLeading(int)
     */
    @Override
    public boolean isLeading(int cp) {
        return isFirst.get(cp);
    }

    /* (non-Javadoc)
     * @see org.unicode.text.UCD.NormalizationData#normalizationDiffers(int, boolean, boolean)
     */
    @Override
    public boolean normalizationDiffers(int cp, boolean composition, boolean compat) {
        // final byte dt = ucd.getDecompositionType(cp);
        final Decomposition_Type_Values dt = decompType.get(cp);
        if (!composition) {
            if (compat) {
                // return dt >= UCD_Types.CANONICAL;
                return isCanonicalOrCompat(dt);
            } else {
                //              return dt == UCD_Types.CANONICAL;
                return dt == Decomposition_Type_Values.Canonical;
            }
        } else {
            // almost the same, except that we add back in the characters
            // that RECOMPOSE
            if (compat) {
                // return dt >= UCD_Types.CANONICAL && !compatibilityRecompose.get(cp);
                return isCanonicalOrCompat(dt) && !compatibilityRecompose.get(cp);
            } else {
                // return dt == UCD_Types.CANONICAL && !canonicalRecompose.get(cp);
                return dt == Decomposition_Type_Values.Canonical && !canonicalRecompose.get(cp);
            }
        }
    }

    public static boolean isCanonicalOrCompat(final Decomposition_Type_Values dt) {
        return dt != null;
    }

    public static boolean isCompat(final Decomposition_Type_Values dt) {
        return dt != null && dt != Decomposition_Type_Values.Canonical;
    }

    /* (non-Javadoc)
     * @see org.unicode.text.UCD.NormalizationData#getRecursiveDecomposition(int, java.lang.StringBuffer, boolean)
     */
    @Override
    public void getRecursiveDecomposition(int cp, StringBuffer buffer, boolean compat) {
        // final byte dt = ucd.getDecompositionType(cp);
        final Decomposition_Type_Values dt = decompType.get(cp);

        // we know we decompose all CANONICAL, plus > CANONICAL if compat is TRUE.
        // if (dt == UCD_Types.CANONICAL || dt > UCD_Types.CANONICAL && compat) {
        if (dt == Decomposition_Type_Values.Canonical || isCompat(dt) && compat) {
            final String s = decompMap.get(cp);
            if (s.equals("<code point>") || s.equals(UTF16.valueOf(cp))) {
                throw new IllegalArgumentException("decomp, but no map, " + Utility.hex(cp));
            }
            for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(s, i);
                getRecursiveDecomposition(cp, buffer, compat);
            }
        } else {
            UTF16.append(buffer, cp);
        }
    }

    /* (non-Javadoc)
     * @see org.unicode.text.UCD.NormalizationData#getPairwiseComposition(int, int)
     */
    @Override
    public int getPairwiseComposition(int starterCh, int ch) {
        final int hangulPoss = composeHangul(starterCh, ch);
        if (hangulPoss != 0xFFFF) {
            return hangulPoss;
        }
        final Integer obj = compTable.get(new Long((((long) starterCh) << 32) | ch));
        if (obj == null) {
            return 0xFFFF;
        }
        return obj.intValue();
    }

    /* (non-Javadoc)
     * @see org.unicode.text.UCD.NormalizationData#hasCompatDecomposition(int)
     */
    @Override
    public boolean hasCompatDecomposition(int i) {
        return isCanonicalOrCompat(decompType.get(i));
    }

    /* (non-Javadoc)
     * @see org.unicode.text.UCD.NormalizationData#isNonSpacing(int)
     */
    @Override
    public boolean isNonSpacing(int cp) {
        //        final int cat = ucd.getCategory(cp);
        //        final boolean nonSpacing = cat != UCD_Types.Mn && cat != UCD_Types.Me;
        final General_Category_Values cat = gc.get(cp);
        final boolean nonSpacing =
                cat != General_Category_Values.Nonspacing_Mark
                        && cat != General_Category_Values.Enclosing_Mark;
        return nonSpacing;
    }

    /* (non-Javadoc)
     * @see org.unicode.text.UCD.NormalizationData#getCompositionStatus(java.util.BitSet, java.util.BitSet, java.util.BitSet)
     */
    @Override
    public void getCompositionStatus(BitSet leading, BitSet trailing, BitSet resulting) {
        for (final Entry<Long, Integer> entry : compTable.entrySet()) {
            final Long key = entry.getKey();
            final Integer result = entry.getValue();
            final long keyLong = key.longValue();
            if (leading != null) {
                leading.set((int) (keyLong >>> 32));
            }
            if (trailing != null) {
                trailing.set((int) keyLong);
            }
            if (resulting != null) {
                resulting.set(result.intValue());
            }
        }
        for (int i = UCD.LBase; i < UCD.TLimit; ++i) {
            if (leading != null && isLeadingJamo(i)) {
                leading.set(i); // set all initial Jamo (that form syllables)
            }
            if (trailing != null && isNonLeadJamo(i)) {
                trailing.set(i); // set all final Jamo (that form syllables)
            }
        }
        if (leading != null) {
            for (int i = UCD.SBase; i < UCD.SLimit; ++i) {
                if (isDoubleHangul(i)) {
                    leading.set(i); // set all two-Jamo syllables
                }
            }
        }
    }

    /**
     * We can't just get these from the Hangul Property Values, L, V, T, because some Old values are
     * also included.
     * https://util.unicode.org/UnicodeJsps/list-unicodeset.jsp?a=\p{Block=Hangul%20Jamo} So we copy
     * some of the code.
     */
    private static final int SBase = 0xAC00,
            LBase = 0x1100,
            VBase = 0x1161,
            TBase = 0x11A7,
            TBase2 = 0x11A8,
            LCount = 19,
            VCount = 21,
            TCount = 28,
            NCount = VCount * TCount, // 588
            SCount = LCount * NCount, // 11172
            LLimit = LBase + LCount, // 1113
            VLimit = VBase + VCount, // 1176
            TLimit = TBase + TCount, // 11C3
            TLimitFull = 0x1200,
            SLimit = SBase + SCount; // D7A4

    static int composeHangul(int char1, int char2) {
        if (LBase <= char1 && char1 < LLimit && VBase <= char2 && char2 < VLimit) {
            return (SBase + ((char1 - LBase) * VCount + (char2 - VBase)) * TCount);
        }
        if (SBase <= char1
                && char1 < SLimit
                && TBase2 <= char2
                && char2 < TLimit
                && ((char1 - SBase) % TCount) == 0) {
            return char1 + (char2 - TBase);
        }
        return 0xFFFF; // no composition
    }

    private static boolean isNonLeadJamo(int cp) {
        return (VBase <= cp && cp < VLimit) || (TBase2 <= cp && cp < TLimit);
    }

    private static boolean isLeadingJamoComposition(int char1) {
        return (LBase <= char1 && char1 < LLimit)
                || (SBase <= char1 && char1 < SLimit && ((char1 - SBase) % TCount) == 0);
    }

    static boolean isLeadingJamo(int cp) {
        return (LBase <= cp && cp < LLimit);
    }

    static boolean isDoubleHangul(int s) {
        final int SIndex = s - SBase;
        if (0 > SIndex || SIndex >= SCount) {
            throw new IllegalArgumentException("Not a Hangul Syllable: " + s);
        }
        return (SIndex % TCount) == 0;
    }
}
