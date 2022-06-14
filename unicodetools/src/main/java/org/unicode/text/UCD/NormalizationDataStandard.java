package org.unicode.text.UCD;

import com.ibm.icu.text.UTF16;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map.Entry;
import org.unicode.text.utility.ChainException;

class NormalizationDataStandard implements NormalizationData {
    private final UCD ucd;
    private final HashMap<Long, Integer> compTable = new HashMap<Long, Integer>();
    private final BitSet isSecond = new BitSet();
    private final BitSet isFirst = new BitSet();
    private final BitSet canonicalRecompose = new BitSet();
    private final BitSet compatibilityRecompose = new BitSet();

    NormalizationDataStandard(String version) {
        ucd = UCD.make(version);
        for (int i = 0; i < 0x10FFFF; ++i) {
            if (!ucd.isAssigned(i)) {
                continue;
            }
            if (ucd.isPUA(i)) {
                continue;
            }
            if (UCD.isNonLeadJamo(i)) {
                isSecond.set(i);
            }
            if (UCD.isLeadingJamoComposition(i)) {
                isFirst.set(i);
            }
            final byte dt = ucd.getDecompositionType(i);
            if (dt != UCD_Types.CANONICAL) {
                continue;
            }
            if (!ucd.getBinaryProperty(i, UCD_Types.CompositionExclusion)) {
                try {
                    final String s = ucd.getDecompositionMapping(i);
                    final int len = UTF16.countCodePoint(s);
                    if (len != 2) {
                        if (len > 2) {
                            if (ucd.getCompositeVersion() >= 0x30000) { // version >= 3.0.0
                                throw new IllegalArgumentException(
                                        "BAD LENGTH: " + len + ucd.toString(i));
                            }
                        }
                        continue;
                    }
                    final int a = UTF16.charAt(s, 0);
                    if (ucd.getCombiningClass(a) != 0) {
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
                    if (ucd.getDecompositionType(a) <= UCD_Types.CANONICAL
                            && ucd.getDecompositionType(b) <= UCD_Types.CANONICAL) {
                        compatibilityRecompose.set(i);
                    }

                    final long key = (((long) a) << 32) | b;

                    /*if (i == '\u1E0A' || key == 0x004400000307) {
                        System.out.println(Utility.hex(s));
                        System.out.println(Utility.hex(i));
                        System.out.println(Utility.hex(key));
                    }*/
                    compTable.put(new Long(key), new Integer(i));
                } catch (final Exception e) {
                    throw new ChainException("Error: {0}", new Object[] {ucd.toString(i)}, e);
                }
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
        return ucd.getVersion();
    }

    /* (non-Javadoc)
     * @see org.unicode.text.UCD.NormalizationData#getCanonicalClass(int)
     */
    @Override
    public short getCanonicalClass(int cp) {
        return ucd.getCombiningClass(cp);
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
        final byte dt = ucd.getDecompositionType(cp);
        if (!composition) {
            if (compat) {
                return dt >= UCD_Types.CANONICAL;
            } else {
                return dt == UCD_Types.CANONICAL;
            }
        } else {
            // almost the same, except that we add back in the characters
            // that RECOMPOSE
            if (compat) {
                return dt >= UCD_Types.CANONICAL && !compatibilityRecompose.get(cp);
            } else {
                return dt == UCD_Types.CANONICAL && !canonicalRecompose.get(cp);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.unicode.text.UCD.NormalizationData#getRecursiveDecomposition(int, java.lang.StringBuffer, boolean)
     */
    @Override
    public void getRecursiveDecomposition(int cp, StringBuffer buffer, boolean compat) {
        final byte dt = ucd.getDecompositionType(cp);
        // we know we decompose all CANONICAL, plus > CANONICAL if compat is TRUE.
        if (dt == UCD_Types.CANONICAL || dt > UCD_Types.CANONICAL && compat) {
            final String s = ucd.getDecompositionMapping(cp);
            if (s.equals(UTF16.valueOf(cp))) {
                System.out.println("fix");
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
        final int hangulPoss = UCD.composeHangul(starterCh, ch);
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
        return ucd.getDecompositionType(i) >= UCD_Types.CANONICAL;
    }

    /* (non-Javadoc)
     * @see org.unicode.text.UCD.NormalizationData#isNonSpacing(int)
     */
    @Override
    public boolean isNonSpacing(int cp) {
        final int cat = ucd.getCategory(cp);
        final boolean nonSpacing = cat != UCD_Types.Mn && cat != UCD_Types.Me;
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
            if (leading != null && UCD.isLeadingJamo(i)) {
                leading.set(i); // set all initial Jamo (that form syllables)
            }
            if (trailing != null && UCD.isNonLeadJamo(i)) {
                trailing.set(i); // set all final Jamo (that form syllables)
            }
        }
        if (leading != null) {
            for (int i = UCD.SBase; i < UCD.SLimit; ++i) {
                if (UCD.isDoubleHangul(i)) {
                    leading.set(i); // set all two-Jamo syllables
                }
            }
        }
    }
}
