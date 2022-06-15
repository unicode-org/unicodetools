/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/Normalizer.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.UCD;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import java.util.BitSet;
import java.util.HashMap;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

/**
 * Implements Unicode Normalization Forms C, D, KC, KD.<br>
 * See UTR#15 for details.<br>
 * Copyright © 1998-1999 Unicode, Inc. All Rights Reserved.<br>
 * The Unicode Consortium makes no expressed or implied warranty of any kind, and assumes no
 * liability for errors or omissions. No liability is assumed for incidental and consequential
 * damages in connection with or arising out of the use of the information here.
 *
 * @author Mark Davis
 */
public final class Normalizer implements Transform<String, String>, UCD_Types {
    public static final String copyright =
            "Copyright (C) 2000, IBM Corp. and others. All Rights Reserved.";

    static final boolean SHOW_ADJUSTING = false;

    public static boolean SHOW_PROGRESS = false;

    /** Create a normalizer for a given form. */
    public Normalizer(byte form, String unicodeVersion) {
        this.form = form;
        composition = (form & NF_COMPOSITION_MASK) != 0;
        compatibility = (form & NF_COMPATIBILITY_MASK) != 0;
        data = getData(unicodeVersion);
    }

    /** Create a normalizer for a given form. */
    // public Normalizer(byte form) {
    //    this(form,"");
    // }

    /** Return string name */
    public static String getName(byte form) {
        return UCD_Names.NF_NAME[form];
    }

    /** Return string name */
    public String getName() {
        return getName(form);
    }

    /** Return string name */
    public String getUCDVersion() {
        return data.getUCDVersion();
    }

    /** Does compose? */
    public boolean isComposition() {
        return composition;
    }

    /** Does compose? */
    public boolean isCompatibility() {
        return compatibility;
    }

    /**
     * Normalizes text according to the chosen form, replacing contents of the target buffer.
     *
     * @param source the original text, unnormalized
     * @param target the resulting normalized text
     */
    public StringBuffer normalize(CharSequence source, StringBuffer target) {

        // First decompose the source into target,
        // then compose if the form requires.

        if (source.length() != 0) {
            internalDecompose(source, target, true, compatibility);
            if (composition) {
                internalCompose(target);
            }
        }
        return target;
    }

    /**
     * Normalizes text according to the chosen form, replacing contents of the target buffer.
     *
     * @param source the original text, unnormalized
     * @param target the resulting normalized text
     */
    public boolean isFCD(String source) {
        if (source.length() == 0) {
            return true;
        }
        final StringBuffer noReorder = new StringBuffer();
        final StringBuffer reorder = new StringBuffer();

        internalDecompose(source, noReorder, false, false);
        internalDecompose(source, reorder, true, false);

        return reorder.toString().equals(noReorder.toString());
    }

    /**
     * Normalizes text according to the chosen form
     *
     * @param source the original text, unnormalized
     * @return target the resulting normalized text
     */
    public String normalize(CharSequence source) {
        return normalize(source, new StringBuffer()).toString();
    }

    /**
     * Normalizes text according to the chosen form
     *
     * @param newLocaleID the original text, unnormalized
     * @return target the resulting normalized text
     */
    public String normalize(int cp) {
        return normalize(UTF16.valueOf(cp));
    }

    /**
     * private StringBuffer hasDecompositionBuffer = new StringBuffer();
     *
     * <p>public boolean hasDecomposition(int cp) { hasDecompositionBuffer.setLength(0);
     * normalize(UTF16.valueOf(cp), hasDecompositionBuffer); if (hasDecompositionBuffer.length() !=
     * 1) return true; return cp != hasDecompositionBuffer.charAt(0); }
     */

    /**
     * Does a quick check to see if the string is in the current form. Checks canonical order and
     * isAllowed().
     *
     * @param newLocaleID source text
     * @return YES, NO, MAYBE
     */
    /*
    public static final int NO = 0, YES = 1, MAYBE = -1;

    public int quickCheck(String source) {
        short lastCanonicalClass = 0;
        int result = YES;
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            short canonicalClass = data.getCanonicalClass(ch);
            if (lastCanonicalClass > canonicalClass && canonicalClass != 0) {
                return NO;
            }
            int check = isAllowed(ch);
            if (check == NO) return NO;
            if (check == MAYBE) result = MAYBE;
        }
        return result;
    }

    /**
     * Find whether the given character is allowed in the current form.
     * @return YES, NO, MAYBE
     */
    /*
    public int isAllowed(char ch) {
        if (composition) {
            if (compatibility) {
                if (data.isCompatibilityExcluded(ch)) {
                    return NO;
                }
            } else {
                if (data.isExcluded(ch)) {
                    return NO;
                }
            }
            if (data.isTrailing(ch)) {
                return MAYBE;
            }
        } else { // decomposition: both NFD and NFKD
            if (data.normalizationDiffers(compatibility,ch)) return NO;
        }
        return YES;
    }

    /**
     * Utility: Gets the combining class of a character from the
     * Unicode Character Database. Only a byte is needed, but since they are signed in Java
     * return an int to forstall problems.
     * @param   ch      the source character
     * @return          value from 0 to 255
     */

    public short getCanonicalClass(int ch) {
        return data.getCanonicalClass(ch);
    }

    /**
     * Utility: Checks whether there is a recursive decomposition of a character from the Unicode
     * Character Database. It is compatibility or canonical according to the particular normalizer.
     *
     * @param ch the source character
     */
    public boolean isNormalized(int ch) {
        return !data.normalizationDiffers(ch, composition, compatibility);
    }

    /**
     * Utility: Checks whether there is a recursive decomposition of a character from the Unicode
     * Character Database. It is compatibility or canonical according to the particular normalizer.
     *
     * @param ch the source character
     */
    public boolean isNormalized(CharSequence s) {
        if (Character.codePointCount(s, 0, s.length()) == 1) {
            return !data.normalizationDiffers(UTF16.charAt(s, 0), composition, compatibility);
        }
        return s.equals(normalize(s)); // TODO: OPTIMIZE LATER
    }

    /**
     * Utility: Gets recursive decomposition of a character from the Unicode Character Database.
     *
     * @param compatibility If false selects the recursive canonical decomposition, otherwise
     *     selects the recursive compatibility AND canonical decomposition.
     * @param ch the source character
     * @param buffer buffer to be filled with the decomposition
     */
    public void getRecursiveDecomposition(char ch, StringBuffer buffer) {
        data.getRecursiveDecomposition(ch, buffer, compatibility);
    }

    /**
     * Utility: Gets composition mapping.
     *
     * @return IntEnumeration with the pair -> value mapping, where the pair is firstChar << 16 |
     *     secondChar. Will need to be fixed for surrogates.
     */
    public void getCompositionStatus(BitSet leading, BitSet trailing, BitSet resulting) {
        data.getCompositionStatus(leading, trailing, resulting);
    }

    public boolean isTrailing(int cp) {
        return composition ? data.isTrailing(cp) : false;
    }

    public boolean isLeading(int cp) {
        return composition ? data.isLeading(cp) : false;
    }

    public int getComposition(int first, int second) {
        return data.getPairwiseComposition(first, second);
    }

    // ======================================
    //                  PRIVATES
    // ======================================

    /** The current form. */
    private final byte form;

    private final boolean composition;
    private final boolean compatibility;
    private UnicodeMap substituteMapping;

    /**
     * Decomposes text, either canonical or compatibility, replacing contents of the target buffer.
     *
     * @param form the normalization form. If NF_COMPATIBILITY_MASK bit is on in this byte, then
     *     selects the recursive compatibility decomposition, otherwise selects the recursive
     *     canonical decomposition.
     * @param source the original text, unnormalized
     * @param target the resulting normalized text
     */
    private void internalDecompose(
            CharSequence source, StringBuffer target, boolean reorder, boolean compat) {
        final StringBuffer buffer = new StringBuffer();
        int ch32;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(ch32)) {
            buffer.setLength(0);
            ch32 = UTF16.charAt(source, i);
            final String sub =
                    substituteMapping == null ? null : (String) substituteMapping.getValue(ch32);
            if (sub != null) {
                buffer.append(sub);
            } else {
                data.getRecursiveDecomposition(ch32, buffer, compat);
            }

            // add all of the characters in the decomposition.
            // (may be just the original character, if there was
            // no decomposition mapping)

            int ch;
            for (int j = 0; j < buffer.length(); j += UTF16.getCharCount(ch)) {
                ch = UTF16.charAt(buffer, j);
                final int chClass = data.getCanonicalClass(ch);
                int k = target.length(); // insertion point
                if (chClass != 0 && reorder) {

                    // bubble-sort combining marks as necessary

                    int ch2;
                    for (; k > 0; k -= UTF16.getCharCount(ch2)) {
                        ch2 = UTF16.charAt(target, k - 1);
                        if (data.getCanonicalClass(ch2) <= chClass) {
                            break;
                        }
                    }
                }
                target.insert(k, UTF16.valueOf(ch));
            }
        }
    }

    /**
     * Composes text in place. Target must already have been decomposed. Uses UTF16, which is a
     * utility class for supplementary character support in Java.
     *
     * @param target input: decomposed text. output: the resulting normalized text.
     */
    private void internalCompose(StringBuffer target) {
        int starterPos = 0;
        int starterCh = UTF16.charAt(target, 0);
        int compPos = UTF16.getCharCount(starterCh); // length of last composition
        int lastClass = data.getCanonicalClass(starterCh);
        if (lastClass != 0) {
            lastClass = 256; // fix for strings staring with a combining mark
        }
        int oldLen = target.length();

        // Loop on the decomposed characters, combining where possible

        int ch;
        for (int decompPos = compPos;
                decompPos < target.length();
                decompPos += UTF16.getCharCount(ch)) {
            ch = UTF16.charAt(target, decompPos);
            if (SHOW_PROGRESS) {
                System.out.println(
                        Utility.hex(target)
                                + ", decompPos: "
                                + decompPos
                                + ", compPos: "
                                + compPos
                                + ", ch: "
                                + Utility.hex(ch));
            }
            final int chClass = data.getCanonicalClass(ch);
            final int composite = data.getPairwiseComposition(starterCh, ch);
            if (composite != NormalizationData.NOT_COMPOSITE
                    && (lastClass < chClass || lastClass == 0)) {
                UTF16.setCharAt(target, starterPos, composite);
                // we know that we will only be replacing non-supplementaries by non-supplementaries
                // so we don't have to adjust the decompPos
                starterCh = composite;
            } else {
                if (chClass == 0) {
                    starterPos = compPos;
                    starterCh = ch;
                }
                lastClass = chClass;
                UTF16.setCharAt(target, compPos, ch);
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

    /**
     * Contains normalization data from the Unicode Character Database. use false for the minimal
     * set, true for the real set.
     */
    private final NormalizationData data;

    private static HashMap versionCache = new HashMap();

    private static NormalizationData getData(String version) {
        if (version.length() == 0) {
            version = Settings.latestVersion;
        }
        NormalizationData result = (NormalizationData) versionCache.get(version);
        if (result == null) {
            result = new NormalizationDataStandard(version);
            versionCache.put(version, result);
        }
        return result;
    }

    public UnicodeMap getSubstituteMapping() {
        return substituteMapping;
    }

    public Normalizer setSubstituteMapping(UnicodeMap substituteMapping) {
        this.substituteMapping = substituteMapping;
        return this;
    }

    static UnicodeMap spacingMap;
    ;

    public void setSpacingSubstitute() {
        if (spacingMap == null) {
            makeSpacingMap();
        }
        setSubstituteMapping(spacingMap);
    }

    private void makeSpacingMap() {
        spacingMap = new UnicodeMap();
        final StringBuffer b = new StringBuffer();
        main:
        for (int i = 0; i <= 0x10FFFF; ++i) {
            final boolean compat = data.hasCompatDecomposition(i);
            if (!compat) {
                continue;
            }
            b.setLength(0);
            data.getRecursiveDecomposition(i, b, true);
            if (b.length() == 1) {
                continue;
            }
            final char firstChar = b.charAt(0);
            if (firstChar != 0x20 && firstChar != '\u0640') {
                continue;
            }
            // if rest are just Mn or Me marks, then add to substitute mapping
            int cp;
            for (int j = 1; j < b.length(); j += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(b, j);
                if (data.isNonSpacing(cp)) {
                    continue main;
                }
            }
            spacingMap.put(i, UTF16.valueOf(i));
        }
        final String[][] specials = {
            {"[\\u0384\\u1FFD]", "\u00B4"},
            {"[\\uFFE3]", "\u00AF"},
            {"[\\uFE49-\\uFE4C]", "\u203E"},
            {"[\\u1FED]", "\u00A8\u0300"},
            {"[\\u1FEE\\u0385]", "\u00A8\u0301"},
            {"[\\u1FC1]", "\u00A8\u0342"},
            {"[\\u1FBD]", "\u1FBF"},
            {"[\\u1FCD]", "\u1FBF\u0300"},
            {"[\\u1FCE]", "\u1FBF\u0301"},
            {"[\\u1FCF]", "\u1FBF\u0342"},
            {"[\\u1FDD]", "\u1FFE\u0300"},
            {"[\\u1FDE]", "\u1FFE\u0301"},
            {"[\\u1FDF]", "\u1FFE\u0342"},
            {"[\\uFC5E]", "\uFE72\u0651"},
            {"[\\uFC5F]", "\uFE74\u0651"},
            {"[\\uFC60]", "\uFE76\u0651"},
            {"[\\uFC61]", "\uFE78\u0651"},
            {"[\\uFC62]", "\uFE7A\u0651"},
            {"[\\uFC63]", "\uFE7C\u0670"},
            {"[\\uFCF2]", "\uFE77\u0651"},
            {"[\\uFCF3]", "\uFE79\u0651"},
            {"[\\uFCF4]", "\uFE7B\u0651"},
        };
        int count = 0;
        final UnicodeSet mappedChars = spacingMap.keySet();
        for (final String[] special : specials) {
            final UnicodeSet source = new UnicodeSet(special[0]);
            if (!mappedChars.containsAll(source)) {
                throw new InternalError("Remapping character that doesn't need it!" + source);
            }
            spacingMap.putAll(source, special[1]);
            count += source.size();
        }
        spacingMap.freeze();
    }

    @Override
    public String transform(String source) {
        return normalize(source);
    }

    /** Just accessible for testing. */
    /*
    boolean isExcluded (char ch) {
        return data.isExcluded(ch);
    }

    /**
     * Just accessible for testing.
     */
    /*
    String getRawDecompositionMapping (char ch) {
        return data.getRawDecompositionMapping(ch);
    }
    //*/
}
