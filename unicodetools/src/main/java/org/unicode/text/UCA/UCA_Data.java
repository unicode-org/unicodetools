/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/UCA/UCA_Data.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.UCA;

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.text.UCA.UCA.Remap;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.UCD.UCD;
import org.unicode.text.utility.IntStack;
import org.unicode.text.utility.UTF16Plus;

public class UCA_Data {
    static final boolean DEBUG = false;
    static final boolean DEBUG_SHOW_ADD = false;

    private final Normalizer toD;
    private final UCD ucd;
    private final Remap primaryRemap;

    public int variableLow = '\uFFFF';
    public int nonVariableLow = '\uFFFF'; // HACK '\u089A';
    public int variableHigh = '\u0000';

    UCA_Statistics statistics = new UCA_Statistics();

    public UCA_Data(Normalizer toD, UCD ucd, Remap primaryRemap) {
        this.toD = toD;
        this.ucd = ucd;
        this.primaryRemap = primaryRemap;
        if (primaryRemap != null) {
            variableHigh = primaryRemap.getVariableHigh();
            statistics.firstDucetNonVariable = primaryRemap.getFirstDucetNonVariable();
        }
    }

    /**
     * Maps characters and strings to collation element sequences.<br>
     * Contractions: multi-code point strings<br>
     * Expansions: CEList.length() > 1
     */
    private final Map<String, CEList> cesMap = new HashMap<String, CEList>();

    /**
     * Maps single-character strings to one of the longest mappings (single-character or
     * contractions) starting with those characters. (null for no mappings)
     */
    private final Map<String, String> longestMap = new HashMap<String, String>();

    /**
     * Set of single-character strings that start contractions which end with a non-zero combining
     * class.
     */
    private final Set<String> hasDiscontiguousContractions = new HashSet<String>();

    /**
     * Set of all contraction strings. Same as all multi-character strings in cesMap, except in code
     * point order. Used only for API that accesses this set.
     */
    private final Set<String> contractions =
            new TreeSet<String>(
                    new UTF16.StringComparator(
                            true, false, UTF16.StringComparator.FOLD_CASE_DEFAULT));

    {
        checkConsistency();
    }

    /** Returns true if there is an explicit mapping for ch, or one that starts with ch. */
    public boolean codePointHasExplicitMappings(int ch) {
        final String s = Character.toString((char) ch);
        final String longest = longestMap.get(s);
        return longest != null;
    }

    public Remap getPrimaryRemap() {
        return primaryRemap;
    }

    private boolean endsWithZeroCC(String s) {
        return toD.getCanonicalClass(s.codePointBefore(s.length())) == 0;
    }

    public void add(CharSequence source, IntStack ces) {
        if (source.length() < 1 || ces.isEmpty()) {
            throw new IllegalArgumentException("String or CEs too short");
        }
        final String sourceString = source.toString();
        checkForIllegal(sourceString);
        final int firstCodePoint = sourceString.codePointAt(0);

        if (primaryRemap != null) {
            final IntStack charRemap = primaryRemap.getRemappedCharacter(firstCodePoint);
            if (charRemap != null) {
                ces = charRemap;
            } else {
                for (int i = 0; i < ces.length(); ++i) {
                    int value = ces.get(i);
                    final char primary = CEList.getPrimary(value);
                    final Integer remap = primaryRemap.getRemappedPrimary(primary);
                    if (remap != null) {
                        value = (remap << 16) | (value & 0xFFFF);
                        ces.set(i, value);
                    }
                }
            }
        }

        // gather statistics
        if ("a".contentEquals(source)) {
            statistics.firstScript = CEList.getPrimary(ces.get(0));
        }

        for (int i = 0; i < ces.length(); ++i) {
            final int ce = ces.get(i);
            final int key1 = CEList.getPrimary(ce);
            final int key2 = CEList.getSecondary(ce);
            final int key3 = CEList.getTertiary(ce);
            if (Implicit.isImplicitLeadPrimary(key1)) {
                // No statistics for the primary weight, and skip the trail unit.
                ++i;
            } else {
                statistics.setPrimary(key1);
                if (i == 0) {
                    StringBuilder reps = statistics.representativePrimary.get(key1);
                    if (reps == null) {
                        statistics.representativePrimary.put(key1, reps = new StringBuilder());
                    }
                    reps.appendCodePoint(firstCodePoint);
                } else {
                    StringBuilder reps = statistics.representativePrimarySeconds.get(key1);
                    if (reps == null) {
                        statistics.representativePrimarySeconds.put(
                                key1, reps = new StringBuilder());
                    }
                    reps.appendCodePoint(firstCodePoint);
                }
            }
            statistics.setSecondary(key2);
            statistics.secondaryCount[key2]++;
            statistics.setTertiary(key3);
            statistics.tertiaryCount[key3]++;

            // statistics
            statistics.count1++;
            if (key1 != statistics.oldKey1) {
                statistics.oldKey1 = key1;
                if (statistics.count2 > statistics.max2) {
                    statistics.max2 = statistics.count2;
                }
                if (statistics.count3 > statistics.max3) {
                    statistics.max3 = statistics.count3;
                }
                statistics.count2 = statistics.count3 = 1;
            } else {
                statistics.count2++;
                if (key2 != statistics.oldKey2) {
                    statistics.oldKey2 = key2;
                    if (statistics.count3 > statistics.max3) {
                        statistics.max3 = statistics.count3;
                    }
                    statistics.count3 = 1;
                } else {
                    statistics.count3++;
                }
            }
            // gather some statistics
            if (key1 != 0 && key1 < statistics.MIN1) {
                statistics.MIN1 = (char) key1;
            }
            if (key2 != 0 && key2 < statistics.MIN2) {
                statistics.MIN2 = (char) key2;
            }
            if (key3 != 0 && key3 < statistics.MIN3) {
                statistics.MIN3 = (char) key3;
            }
            if (key1 > statistics.MAX1) {
                statistics.MAX1 = (char) key1;
            }
            if (key2 > statistics.MAX2) {
                statistics.MAX2 = (char) key2;
            }
            if (key3 > statistics.MAX3) {
                statistics.MAX3 = (char) key3;
            }
        }

        if (DEBUG_SHOW_ADD) {
            System.out.println(
                    "Adding: " + ucd.getCodeAndName(sourceString) + CEList.toString(ces));
        }

        cesMap.put(sourceString, new CEList(ces));

        final int firstLimit = Character.charCount(firstCodePoint);
        final String firstChar = sourceString.substring(0, firstLimit);
        final String longest = longestMap.get(firstChar);
        if (longest == null || longest.length() < sourceString.length()) {
            // The sourceString is longer than previous mappings that start with firstChar.
            longestMap.put(firstChar, sourceString);
        }

        if (sourceString.length() > firstLimit) {
            contractions.add(sourceString);
            if (!endsWithZeroCC(sourceString)) {
                // The sourceString contraction ends with a non-zero combining class:
                // Discontiguous contractions are possible.
                hasDiscontiguousContractions.add(firstChar);
            }
        }
        // if (DEBUG) checkConsistency();
    }

    /**
     * Returns the CEs for the longest matching buffer substring starting at i and moves the index
     * to just after that substring. Discontiguously matched combining marks are removed from the
     * buffer.
     *
     * <p>If there is no mapping for any character or substring at i, then the index is unchanged
     * and null is returned.
     */
    public CEList fetchCEs(StringBuffer buffer, int[] index) {
        final int i = index[0];
        // Lookup for the first character at i.
        int j = buffer.offsetByCodePoints(i, 1);
        String s = buffer.substring(i, j);
        final String longest = longestMap.get(s);
        if (longest == null) {
            // No mapping starts with the character at i.
            return null;
        }
        final boolean maybeDiscontiguous = hasDiscontiguousContractions.contains(s);

        // Contiguous-contraction matching:
        // Find the longest matching substring starting at i.
        CEList ces = cesMap.get(s);
        while ((j - i) < longest.length() && j < buffer.length()) {
            final int next = buffer.offsetByCodePoints(j, 1);
            final String nextString = buffer.substring(i, next);
            final CEList nextCEs = cesMap.get(nextString);
            if (nextCEs != null) {
                s = nextString;
                ces = nextCEs;
            }
            j = next;
        }
        j = i + s.length(); // Limit of the longest contiguous match.

        // Discontiguous-contraction matching.
        if (maybeDiscontiguous && s.length() < longest.length() && j < buffer.length()) {
            // j does not move any more:
            // Discontiguously matching combining marks will be removed from the buffer,
            // and then the caller continues with the skipped combining marks starting at j.
            int nextCodePoint = buffer.codePointAt(j);
            short cc = toD.getCanonicalClass(nextCodePoint);
            if (cc != 0) {
                int k = j + Character.charCount(nextCodePoint);
                short prevCC = cc;
                while (k < buffer.length()) {
                    nextCodePoint = buffer.codePointAt(k);
                    cc = toD.getCanonicalClass(nextCodePoint);
                    if (cc == 0) { // stop with any zero (non-accent)
                        break;
                    }
                    final int next = k + Character.charCount(nextCodePoint);
                    if (cc == prevCC) { // blocked if same class as last
                        k = next;
                        continue;
                    }
                    prevCC = cc; // remember for next time
                    // nextString = s + nextCodePoint
                    final String nextString =
                            new StringBuilder(s).appendCodePoint(nextCodePoint).toString();
                    final CEList nextCEs = cesMap.get(nextString);
                    if (nextCEs == null) {
                        k = next;
                        continue;
                    }
                    // Remove the nextCodePoint from the buffer.
                    buffer.delete(k, next);
                    s = nextString;
                    ces = nextCEs;
                    if (s.length() >= longest.length()) {
                        break;
                    }
                }
            }
        }

        if (ces != null) {
            index[0] = j;
        }
        return ces;
    }

    private static final UnicodeSet ILLEGAL_CODE_POINTS =
            new UnicodeSet("[:cs:]").freeze(); // doesn't depend on version

    private void checkForIllegal(String string) {
        if (ILLEGAL_CODE_POINTS.containsSome(string)) {
            throw new IllegalArgumentException(
                    "String contains illegal characters: <"
                            + string
                            + "> "
                            + new UnicodeSet().addAll(string).retainAll(ILLEGAL_CODE_POINTS));
        }
    }

    void checkConsistency() {
        // pass
    }

    private class MappingComparator implements Comparator<String> {
        private final UTF16.StringComparator cmp =
                new UTF16.StringComparator(true, false, UTF16.StringComparator.FOLD_CASE_DEFAULT);

        @Override
        public int compare(String left, String right) {
            // Note: add() enforces that mapping strings are not empty strings.
            // Sort single-character strings before contractions.
            final boolean leftIsSingle = UTF16Plus.isSingleCodePoint(left);
            final boolean rightIsSingle = UTF16Plus.isSingleCodePoint(right);
            if (leftIsSingle != rightIsSingle) {
                return leftIsSingle ? -1 : 1;
            }
            // Otherwise sort in code point order.
            return cmp.compare(left, right);
        }
    }

    /** Returns an immutable Map of sorted strings (characters &amp; contractions) to CEs. */
    SortedMap<String, CEList> getSortedMappings() {
        final SortedMap<String, CEList> sorted =
                new TreeMap<String, CEList>(new MappingComparator());
        sorted.putAll(cesMap);
        return Collections.unmodifiableSortedMap(sorted);
    }

    Set<String> getContractions() {
        return Collections.unmodifiableSet(contractions);
    }

    int getContractionCount() {
        return contractions.size();
    }
}
