package org.unicode.text.UCD;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map.Entry;

import org.unicode.text.utility.ChainException;

import com.ibm.icu.text.UTF16;

class NormalizationDataStandard {
    private UCD ucd;
    private HashMap<Long,Integer> compTable = new HashMap<Long,Integer>();
    private BitSet isSecond = new BitSet();
    private BitSet isFirst = new BitSet();
    private BitSet canonicalRecompose = new BitSet();
    private BitSet compatibilityRecompose = new BitSet();
    public static final int NOT_COMPOSITE = 0xFFFF;

    NormalizationDataStandard(String version) {
        ucd = UCD.make(version);
        for (int i = 0; i < 0x10FFFF; ++i) {
            if (!ucd.isAssigned(i)) continue;
            if (ucd.isPUA(i)) continue;
            if (ucd.isNonLeadJamo(i)) isSecond.set(i);
            if (ucd.isLeadingJamoComposition(i)) isFirst.set(i);
            byte dt = ucd.getDecompositionType(i);
            if (dt != Normalizer.CANONICAL) continue;
            if (!ucd.getBinaryProperty(i, Normalizer.CompositionExclusion)) {
                try {
                    String s = ucd.getDecompositionMapping(i);
                    int len = UTF16.countCodePoint(s);
                    if (len != 2) {
                        if (len > 2) {
                            if (ucd.getVersion().compareTo("3.0.0") >= 0) {
                                throw new IllegalArgumentException("BAD LENGTH: " + len + ucd.toString(i));
                            }
                        }
                        continue;
                    }
                    int a = UTF16.charAt(s, 0);
                    if (ucd.getCombiningClass(a) != 0) continue;
                    isFirst.set(a);

                    int b = UTF16.charAt(s, UTF16.getCharCount(a));
                    isSecond.set(b);

                    // have a recomposition, so set the bit
                    canonicalRecompose.set(i);

                    // set the compatibility recomposition bit
                    // ONLY if the component characters
                    // don't compatibility decompose
                    if (ucd.getDecompositionType(a) <= Normalizer.CANONICAL
                            && ucd.getDecompositionType(b) <= Normalizer.CANONICAL) {
                        compatibilityRecompose.set(i);
                    }

                    long key = (((long)a)<<32) | b;

                    /*if (i == '\u1E0A' || key == 0x004400000307) {
                            System.out.println(Utility.hex(s));
                            System.out.println(Utility.hex(i));
                            System.out.println(Utility.hex(key));
                        }*/
                    compTable.put(new Long(key), new Integer(i));
                } catch (Exception e) {
                    throw new ChainException("Error: {0}", new Object[]{ucd.toString(i)}, e);
                }
            }
        }
        // process compatibilityRecompose
        // have to do this afterwards, since we don't know whether the pieces
        // are allowable until we have processed all the characters
    }

    public String getUCDVersion() {
        return ucd.getVersion();
    }

    public short getCanonicalClass(int cp) {
        return ucd.getCombiningClass(cp);
    }

    public boolean isTrailing(int cp) {
        return isSecond.get(cp);
    }

    public boolean isLeading(int cp) {
        return isFirst.get(cp);
    }

    public boolean normalizationDiffers(int cp, boolean composition, boolean compat) {
        byte dt = ucd.getDecompositionType(cp);
        if (!composition) {
            if (compat) return dt >= Normalizer.CANONICAL;
            else return dt == Normalizer.CANONICAL;
        } else {
            // almost the same, except that we add back in the characters
            // that RECOMPOSE
            if (compat) return dt >= Normalizer.CANONICAL && !compatibilityRecompose.get(cp);
            else return dt == Normalizer.CANONICAL && !canonicalRecompose.get(cp);
        }
    }

    public void getRecursiveDecomposition(int cp, StringBuffer buffer, boolean compat) {
        byte dt = ucd.getDecompositionType(cp);
        // we know we decompose all CANONICAL, plus > CANONICAL if compat is TRUE.
        if (dt == Normalizer.CANONICAL || dt > Normalizer.CANONICAL && compat) {
            String s = ucd.getDecompositionMapping(cp);
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

    public int getPairwiseComposition(int starterCh, int ch) {
        int hangulPoss = UCD.composeHangul(starterCh, ch);
        if (hangulPoss != 0xFFFF) return hangulPoss;
        Integer obj = compTable.get(new Long((((long)starterCh)<<32) | ch));
        if (obj == null) return 0xFFFF;
        return ((Integer)obj).intValue();
    }

    public boolean hasCompatDecomposition(int i) {
        return ucd.getDecompositionType(i) >= UCD_Types.CANONICAL;
    }
    
    public boolean isNonSpacing(int cp) {
        int cat = ucd.getCategory(cp);
        final boolean nonSpacing = cat != UCD_Types.Mn && cat != UCD_Types.Me;
        return nonSpacing;
    }
    
    public void getCompositionStatus(BitSet leading, BitSet trailing, BitSet resulting) {
        for (Entry<Long, Integer> entry : compTable.entrySet()) {
            Long key = entry.getKey();
            Integer result = entry.getValue();
            long keyLong = key.longValue();
            if (leading != null) leading.set((int)(keyLong >>> 32));
            if (trailing != null) trailing.set((int)keyLong);
            if (resulting != null) resulting.set(result.intValue());
        }
        for (int i = UCD.LBase; i < UCD.TLimit; ++i) {
            if (leading != null && UCD.isLeadingJamo(i)) leading.set(i); // set all initial Jamo (that form syllables)
            if (trailing != null && UCD.isNonLeadJamo(i)) trailing.set(i); // set all final Jamo (that form syllables)
        }
        if (leading != null) {
            for (int i = UCD.SBase; i < UCD.SLimit; ++i) {
                if (UCD.isDoubleHangul(i)) leading.set(i); // set all two-Jamo syllables
            }
        }
    }

}