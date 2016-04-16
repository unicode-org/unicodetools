package org.unicode.props;

import java.util.BitSet;
import java.util.HashMap;

import org.unicode.cldr.util.UnicodeProperty;
import org.unicode.props.UcdPropertyValues.Canonical_Combining_Class_Values;
import org.unicode.text.UCD.NormalizationData;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;


public class PropNormalizationData implements org.unicode.text.UCD.NormalizationData {
    private static final boolean SHOW_PROGRESS = false;
    private static final boolean SHOW_ADJUSTING = false;
    final String version;
    final UnicodeMap<Short> canonical = new UnicodeMap<>();
    final UnicodeMap<String> nfd = new UnicodeMap<>();
    final UnicodeMap<String> nfkd = new UnicodeMap<>();
    final UnicodeMap<HashMap<Integer, Integer>> pairwiseComposition;
    // TODO find out why UnicodeMap[] fails hashCode

    public PropNormalizationData(IndexUnicodeProperties properties) {
        version = properties.getUcdVersion().toString();
        UnicodeProperty canonicalProp = properties.getProperty("ccc");
        canonical.setMissing(Short.valueOf((short)0));
        UnicodeSet nullValues = new UnicodeSet();
        for (String s : new UnicodeSet(canonicalProp.getSet(Canonical_Combining_Class_Values.Not_Reordered.toString())).complement()) {
            final int cp = s.codePointAt(0);
            String v = canonicalProp.getValue(cp);
            if (v == null) {
                // nullValues.add(s);
                // TODO find out why these are there
                continue;
            }
            Canonical_Combining_Class_Values vv = Canonical_Combining_Class_Values.valueOf(v);
            canonical.put(cp, Short.valueOf(vv.getNames().getShortName()));
        }
        if (SHOW_PROGRESS) {
            System.out.println("Nulls: " + nullValues);
        }
        canonical.freeze();
        
        UnicodeProperty dtp = properties.getProperty("dt");
        UnicodeProperty dmp = properties.getProperty("dm");
        UnicodeProperty ce = properties.getProperty("composition_exclusion");
        UnicodeSet none = dtp.getSet("None");
        pairwiseComposition = new UnicodeMap();
        StringBuilder buffer = new StringBuilder();
        for (String s : new UnicodeSet(none).complement()) {
            int cp = s.codePointAt(0);
            if (dtp.getValue(cp).equals(UcdPropertyValues.Decomposition_Type_Values.Canonical.toString())) {
                continue;
            }
            String dmpStr = dmp.getValue(cp);
            if (dmpStr == null) {
                // skip
            } else {
                final int first = dmpStr.codePointAt(0);
                if (ce.getValue(cp).equals("Yes")
                        || UTF16.countCodePoint(dmpStr) == 1
                        || canonical.getValue(first) != 0
                        ) {
                    // skip
                } else {
                    final int second = dmpStr.codePointAt(Character.charCount(first));
                    HashMap<Integer,Integer> um2 = pairwiseComposition.get(first);
                    if (um2 == null) {
                        um2 = new HashMap<>();
                        pairwiseComposition.put(first, um2);
                    }
                    um2.put(second, cp);        
                }
            }
            
            buffer.setLength(0);
            getRecursiveDecomposition2(cp, false, dtp, dmp, buffer);
            String nfdstr = buffer.toString();
            if (!nfdstr.equals(s)) {
                nfd.put(cp, nfdstr);
            }
            buffer.setLength(0);
            getRecursiveDecomposition2(cp, true, dtp, dmp, buffer);
            String nfkdstr = buffer.toString();
            nfkd.put(cp, nfkdstr.equals(nfdstr) ? nfdstr : nfkdstr);
        }
        nfd.freeze();
        nfkd.freeze();
        pairwiseComposition.freeze(); // later do deep freeze
    }
    
    public short getCcc(int cp) {
        return canonical.get(cp);
    }

    public  void getRecursiveDecomposition2(int cp,
            boolean compat, UnicodeProperty dtp, UnicodeProperty dmp, StringBuilder buffer) {
        // we know we decompose all CANONICAL, plus > CANONICAL if compat is TRUE.
        String dt = dtp.getValue(cp);
        if (dt.equals("Canonical") || compat && !dt.equals("None")) {
            final String s = dmp.getValue(cp);
            for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
                cp = s.codePointAt(i);
                getRecursiveDecomposition2(cp, compat, dtp, dmp, buffer);
            }
        } else {
            buffer.appendCodePoint(cp);
        }
    }


    @Override
    public String getUCDVersion() {
        return version;
    }

    @Override
    public short getCanonicalClass(int cp) {
        return canonical.getValue(cp);
    }

    @Override
    public boolean isTrailing(int cp) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLeading(int cp) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean normalizationDiffers(int cp, boolean composition,
            boolean compat) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void getRecursiveDecomposition(int cp, StringBuffer buffer,
            boolean compat) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getPairwiseComposition(int starterCh, int ch) {
        HashMap<Integer, Integer> um2 = pairwiseComposition.get(starterCh);
        if (um2 == null) {
            return -1;
        }
        Integer result = um2.get(ch);
        return result == null ? -1 : result;
    }

    @Override
    public boolean hasCompatDecomposition(int i) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isNonSpacing(int cp) {
        // TODO Auto-generated method stub
        return false;
    }
    
    public enum Type {nfd, nfc, nfkd, nfkc}

    public String normalize(CharSequence source, Type type) {
        StringBuilder target = new StringBuilder();
        switch (type) {
        case nfd: 
            internalDecompose(source, target, false);
        break;
        case nfkd: 
            internalDecompose(source, target, true);
        break;
        case nfc: 
            internalDecompose(source, target, false);
            internalCompose(target);
        break;
        case nfkc: 
            internalDecompose(source, target, true);
            internalCompose(target);
        break;
        }
        return target.toString();
    }
    
    public String normalize(int source, Type type) {
        StringBuilder target = new StringBuilder();
        
        String buffer = type == Type.nfkd || type == Type.nfkc 
                ? nfkd.get(source) 
                        : nfd.get(source);
        if (buffer == null) {
            target.append(source);
        } else {
            target.append(buffer);
        }

        switch (type) {
        case nfc: 
            internalCompose(target);
        break;
        case nfkc: 
            internalCompose(target);
        break;
        }
        return target.toString();
    }

    /**
     * Decomposes text, either canonical or compatibility,
     * replacing contents of the target buffer.
     * @param   form        the normalization form. If NF_COMPATIBILITY_MASK
     *                      bit is on in this byte, then selects the recursive
     *                      compatibility decomposition, otherwise selects
     *                      the recursive canonical decomposition.
     * @param   source      the original text, unnormalized
     * @param   target      the resulting normalized text
     */
    private void internalDecompose(CharSequence source, StringBuilder target, boolean compat) {
        int ch32;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(ch32)) {
            ch32 = Character.codePointAt(source, i);
            String buffer = compat ? nfkd.get(ch32) : nfd.get(ch32);
            if (buffer == null) {
                buffer = UTF16.valueOf(ch32);
            }

            // add all of the characters in the decomposition.
            // (may be just the original character, if there was
            // no decomposition mapping)

            int ch;
            for (int j = 0; j < buffer.length(); j += UTF16.getCharCount(ch)) {
                ch = UTF16.charAt(buffer, j);
                final int chClass = canonical.getValue(ch);
                int k = target.length(); // insertion point
                if (chClass != 0) {

                    // bubble-sort combining marks as necessary

                    int ch2;
                    for (; k > 0; k -= UTF16.getCharCount(ch2)) {
                        ch2 = UTF16.charAt(target, k-1);
                        if (canonical.getValue(ch2) <= chClass) {
                            break;
                        }
                    }
                }
                target.insert(k, UTF16.valueOf(ch));
            }
        }
    }

    /**
     * Composes text in place. Target must already
     * have been decomposed.
     * Uses UTF16, which is a utility class for supplementary character support in Java.
     * @param   target      input: decomposed text.
     *                      output: the resulting normalized text.
     */
    private void internalCompose(StringBuilder target) {
        int starterPos = 0;
        int starterCh = UTF16.charAt(target,0);
        int compPos = UTF16.getCharCount(starterCh); // length of last composition
        int lastClass = canonical.getValue(starterCh);
        if (lastClass != 0)
        {
            lastClass = 256; // fix for strings staring with a combining mark
        }
        int oldLen = target.length();

        // Loop on the decomposed characters, combining where possible

        int ch;
        for (int decompPos = compPos; decompPos < target.length(); decompPos += UTF16.getCharCount(ch)) {
            ch = UTF16.charAt(target, decompPos);
            if (SHOW_PROGRESS) {
                System.out.println(Utility.hex(target)
                        + ", decompPos: " + decompPos
                        + ", compPos: " + compPos
                        + ", ch: " + Utility.hex(ch)
                        );
            }
            final int chClass = canonical.getValue(ch);
            int composite = NormalizationData.NOT_COMPOSITE;
            final HashMap<Integer, Integer> um2 = pairwiseComposition.getValue(starterCh);
            if (um2 != null) {
                Integer temp = um2.get(ch);
                if (temp != null) {
                  composite = temp;  
                }
            }
            if (composite != NormalizationData.NOT_COMPOSITE
                    && (lastClass < chClass || lastClass == 0)) {
                setCharAt(target, starterPos, composite);
                // we know that we will only be replacing non-supplementaries by non-supplementaries
                // so we don't have to adjust the decompPos
                starterCh = composite;
            } else {
                if (chClass == 0) {
                    starterPos = compPos;
                    starterCh  = ch;
                }
                lastClass = chClass;
                setCharAt(target, compPos, ch);
                if (target.length() != oldLen) { // MAY HAVE TO ADJUST!
                    if (SHOW_ADJUSTING) {
                        System.out.println("ADJUSTING: " + Utility.hex(target));
                    }
                    decompPos += target.length() - oldLen;
                    oldLen = target.length();
                }
                compPos += UTF16.getCharCount(ch);
            }
        }
        target.setLength(compPos);
    }

    @Override
    public void getCompositionStatus(BitSet leading, BitSet trailing,
            BitSet resulting) {
        throw new UnsupportedOperationException();
    }

    // TODO add to UTF16
    public static void setCharAt(StringBuilder target, int offset16, int char32) {
        int count = 1;
        char single = target.charAt(offset16);

        if (Character.isSurrogate(single)) {
            // pairs of the surrogate with offset16 at the lead char found
            if (Character.isHighSurrogate(single) && (target.length() > offset16 + 1)
                    && Character.isLowSurrogate(target.charAt(offset16 + 1))) {
                count++;
            } else {
                // pairs of the surrogate with offset16 at the trail char
                // found
                if (Character.isLowSurrogate(single) && (offset16 > 0)
                        && Character.isHighSurrogate(target.charAt(offset16 - 1))) {
                    offset16--;
                    count++;
                }
            }
        }
        target.replace(offset16, offset16 + count, UTF16.valueOf(char32));
    }
}
