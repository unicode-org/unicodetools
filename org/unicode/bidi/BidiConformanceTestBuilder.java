package org.unicode.bidi;

/*
 * (C) Copyright Google Inc. 2013, All Rights Reserved
 *
 * Distributed under the Terms of Use in http://www.unicode.org/copyright.html.
 */

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class BidiConformanceTestBuilder {

    private static final byte L = BidiReference.L;
    private static final byte LRE = BidiReference.LRE;
    private static final byte LRO = BidiReference.LRO;
    private static final byte R = BidiReference.R;
    private static final byte AL = BidiReference.AL;
    private static final byte RLE = BidiReference.RLE;
    private static final byte RLO = BidiReference.RLO;
    private static final byte PDF = BidiReference.PDF;
    private static final byte EN = BidiReference.EN;
    private static final byte ES = BidiReference.ES;
    private static final byte ET = BidiReference.ET;
    private static final byte AN = BidiReference.AN;
    private static final byte CS = BidiReference.CS;
    private static final byte NSM = BidiReference.NSM;
    private static final byte BN = BidiReference.BN;
    private static final byte B = BidiReference.B;
    private static final byte S = BidiReference.S;
    private static final byte WS = BidiReference.WS;
    private static final byte ON = BidiReference.ON;
    private static final byte RLI = BidiReference.RLI;
    private static final byte LRI = BidiReference.LRI;
    private static final byte FSI = BidiReference.FSI;
    private static final byte PDI = BidiReference.PDI;

    // Test data provided by Behdad Esfahbod, Dov Grobgeld, Aharon Lanin, and Roozbeh Pournader
    private static byte[][] extraTests = {
            { AL, AL, R, WS, R, R, WS, L, L, L, WS, L, L, L, WS, R, R, WS, AL, R, R, R, R, R, R },
            { AL, L, AL, WS, L, R, L, LRO, WS, AL, L, R, L, R, CS, WS, EN, EN, EN, CS, AN, AN, AN, WS, L, R, L, R, PDF },
            { AL, L, L, WS, RLE, WS, EN, EN, EN, CS, AN, AN, AN, LRO, R, R, AL, RLO, WS, L, L, L, L, PDF, WS, L, R, AL, CS },
            { AL, ON, FSI, L, PDI, LRI, L, PDI, RLI, R, PDI, ON, ET, EN },
            { AL, R, AL, R, R, AL, WS, ON, EN, EN, ON },
            { AL, R, R, R, R, AL, R, WS, AL, R, R, WS, R, AL, AL, R, WS, R, R, R, AL, R, CS, WS, AL, EN, EN, EN, CS, EN, EN, AN },
            { AL, R, WS, AL, R, AL, AL, R, AL, WS, LRE, PDF, WS, EN, EN, EN, ON, EN, EN, AN, WS, R, R, AL, AL, WS, R, R, AL, ON },
            { AL, R, WS, AL, R, AL, AL, R, AL, WS, LRE, WS, PDF, WS, EN, EN, EN, ON, EN, EN, AN, WS, R, AL, R, R },
            { AL, WS, R, AL, AL, R, WS, AL, R, R, LRE, PDF, WS, AL, R, R, ON },
            { AL, WS, R, AL, R, R, WS, AL, R, R, WS, R, AL, AL, R, WS, R, R, R, AL, R, CS, WS, EN, EN, EN, ET, CS, EN, EN, AN },
            { AN, ON, FSI, L, PDI, LRI, L, PDI, RLI, L, PDI, ON, AL },
            { EN, ES, LRI, PDI, EN, ES, RLI, PDI, EN, ES, FSI, PDI, EN },
            { ET, LRI, PDI, EN, RLI, PDI, ET, FSI, PDI, EN },
            { FSI, ON, AN, AN, PDI },
            { FSI, ON, AN, L, R, PDI },
            { FSI, ON, EN, EN, PDI },
            { FSI, ON, EN, R, L, PDI },
            { FSI, ON, FSI, L, PDI, LRI, L, PDI, R, PDI },
            { FSI, ON, FSI, R, PDI, RLI, R, PDI, L, PDI },
            { FSI, ON, LRE, R, PDF, L, PDI },
            { FSI, ON, LRO, R, PDF, L, PDI },
            { FSI, ON, RLE, L, PDF, R, PDI },
            { FSI, ON, RLO, L, PDF, R, PDI },
            { L, FSI, R, WS, PDI, WS, RLI, WS, LRI, WS, PDI, PDI, WS },
            { L, L, L, L, LRO, WS, L, L, PDF, PDF, PDF, WS, RLO, WS, L, WS, L, L, L, L, WS, L, L, L, PDF },
            { L, L, L, L, WS, L, L, L, WS, AN, AN, RLE, PDF, AN, AN, WS, L, L, L, L, L, L, L, ON },
            { L, L, L, L, WS, L, L, WS, LRO, R, R, R, R, PDF },
            { L, L, L, WS, L, L, WS, R, R, AL, WS, AL, AL, R, WS, L, L, WS, L, L, L, L, L, L },
            { L, L, L, WS, R, AL, R, R, S, R, EN, WS, S, S, L, L, L },
            { L, LRI, R, PDI, FSI, R, PDI, RLI, R, PDI, R },
            { L, L, WS, L, L, L, L, WS, ON, R, R, WS, R, R, WS, EN, EN, EN, CS, EN, EN, AN, CS, WS, R, R, ON },
            { L, L, WS, L, L, L, L, WS, ON, R, R, WS, R, R, WS, EN, EN, EN, CS, WS, EN, EN, AN, CS, WS, R, R, ON },
            { L, L, WS, L, L, L, L, WS, ON, R, R, WS, R, R, WS, AN, AN, AN, CS, AN, AN, AN, CS, WS, R, R, ON },
            { L, L, WS, L, L, L, L, WS, ON, R, R, WS, R, R, WS, AN, AN, AN, CS, WS, AN, AN, AN, CS, WS, R, R, ON },
            { L, L, WS, L, L, L, L, WS, ON, R, R, WS, R, R, WS, ON, EN, EN, EN, CS, EN, EN, AN, ON, CS, WS, R, R, ON },
            { L, L, WS, L, L, L, L, WS, ON, R, R, WS, R, R, WS, ON, EN, EN, EN, CS, WS, EN, EN, AN, ON, CS, WS, R, R, ON },
            { L, L, WS, L, L, L, L, WS, ON, R, R, WS, R, R, WS, ON, AN, AN, AN, CS, AN, AN, AN, ON, CS, WS, R, R, ON },
            { L, L, WS, L, L, L, L, WS, ON, R, R, WS, R, R, WS, ON, AN, AN, AN, CS, WS, AN, AN, AN, ON, CS, WS, R, R, ON },
            { L, ON, FSI, R, PDI, RLI, R, PDI, LRI, AL, PDI, ON, L },
            { LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE,
                    LRE, ON, RLO, L, LRE, RLI, LRE, RLE, LRO, RLO, PDI, PDF, L, PDF, ON },
            { LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE,
                    LRE, ON, RLO, L, LRI, L, RLE, LRE, RLO, LRO, L, PDI, L, PDF, ON },
            { LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE,
                    ON, RLO, LRI, RLE, LRE, RLO, LRO, ON, PDI, L, PDI, L, PDF, ON },
            { LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE,
                    RLE, ON, LRO, R, LRI, R, LRE, RLE, LRO, RLO, R, PDI, R, PDF, ON },
            { LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE, LRE,
                    RLE, ON, LRO, R, RLI, ON, LRO, RLE, RLO, LRE, ON, PDI, R, PDF, ON },
            { LRE, RLE, LRO, RLO, LRI, ON, RLO, L, PDF, ON, PDF, ON, PDI, L },
            { L, WS, LRE, L, L, L, L, L, L, WS, RLO, L, L, R, R, PDF, WS, L, L, PDF, L, L },
            { L, WS, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO, LRO,
                    LRO, LRO, LRO, LRO, RLO, L, L, L },
            { L, WS, R, R, R, AL, WS, ON, WS, LRO, PDF, R, AL, R, R, ES },
            { ON, EN, EN, ON, WS, AL, R, AL, R, R, AL },
            { ON, EN, WS, AL, AL, R, R, R, R, R, WS, R, R, WS, AL, R, R, AL },
            { ON, FSI, L, PDI, LRI, L, PDI, R },
            { ON, FSI, R, AL, PDI, RLI, R, AL, PDI },
            { ON, L, EN, EN, EN, ON, R, AL, R, AL, AL, R, ON, ES, L, EN, EN, EN, ON },
            { ON, LRE, ON, LRI, ON, LRO, R, PDF, ON, PDI, ON, PDF, ON },
            { ON, LRE, ON, RLE, ON, LRO, R, RLO, L, PDI, L, PDF, R },
            { ON, LRE, ON, RLI, ON, LRO, R, PDF, ON, PDI, ON, PDF, ON },
            { ON, LRI, ON, RLI, ON, FSI, ON, PDI, ON, PDI, ON, PDI, ON },
            { ON, R, EN, EN, EN, ON, L, L, L, L, L, L, ON, ES, R, EN, EN, EN, ON },
            { ON, RLE, ON, FSI, ON, L, RLO, L, PDF, ON, PDI, ON, PDF, ON },
            { ON, RLE, ON, FSI, ON, R, RLO, L, PDF, ON, PDI, ON, PDF, ON },
            { ON, RLE, ON, LRI, ON, RLO, L, PDF, ON, PDI, ON, PDF, ON },
            { ON, RLE, ON, RLI, ON, RLO, L, PDF, ON, PDI, ON, PDF, ON },
            { ON, RLI, ON, FSI, ON, R, LRI, ON, PDI, ON, PDI, ON, PDI, ON },
            { R, AL, R, WS, AL, WS, LRO, R, AL, AL, WS, L, L, L, L, L, L, L },
            { R, AL, WS, R, AL, R, AL, WS, ON, L, L, WS, L, L, WS, AN, AN, AN, CS, WS, AN, AN, AN, CS, WS, L, L, ON },
            { R, AL, WS, R, AL, R, AL, WS, ON, L, L, WS, L, L, WS, EN, EN, EN, CS, WS, EN, EN, AN, CS, WS, L, L, ON },
            { R, AL, WS, R, AL, R, AL, WS, ON, L, L, WS, L, L, WS, L, WS, L, L, L, ON, L, ON, WS, AL, R, AL, WS, R, AL, R },
            { R, AL, WS, R, AL, R, AL, WS, ON, L, L, WS, L, L, WS, L, WS, L, L, L, ON, ON, WS, AL, R, AL, WS, R, AL, R },
            { R, FSI, L, PDI, LRI, L, PDI, RLI, L, PDI, L },
            { R, FSI, L, WS, PDI, WS, LRI, WS, RLI, WS, PDI, PDI, WS },
            { RLE, LRE, RLO, LRO, RLI, ON, LRO, R, PDF, ON, PDF, ON, PDI, R },
            { RLE, LRE, RLO, LRO, FSI, R, LRO, R, PDF, ON, PDF, ON, PDI, R },
            { RLO, RLE, WS, L, L, L, WS, L, L, L, WS, L, L, L, L, WS, LRO, R, R, AL, PDF, WS, R, R, WS },
            { R, ON, FSI, L, PDI, LRI, L, PDI, RLI, L, PDI, ON, R },
            { R, ON, LRI, L, PDI, FSI, L, PDI, RLI, L, PDI, ON, EN },
            { R, R, AL, WS, R, AL, R, R, AL, WS, R, R, WS, EN, ES, EN, ES, ES, EN },
            { R, R, AL, WS, RLE, L, L, L, L, WS, LRE, R, R, AL, WS, L, L, L },
            { R, R, R, AL, WS, R, AL, AL, R, WS, R, R, R, AL, R, CS, WS, L, L, L, L, L, L, EN, EN, EN, CS, EN, EN, AN },
            { R, R, R, R, AL, WS, EN, ON, EN, WS, EN, ON, EN, WS, EN, ES, EN, WS, EN, ET, EN },
            { R, R, R, WS, ET, EN, EN },
    };

    private static final int R_DEFAULT = -2;

    private static final int BIDI_START_LEVEL = -1;

    public static int MAX_SIZE = 4;

    private static BitSet SKIPS = new BitSet();
    static {
        // skip RLE, LRE, RLO, LRO, PDF, and BN
        SKIPS.set(BidiReference.RLE);
        SKIPS.set(BidiReference.LRE);
        SKIPS.set(BidiReference.RLO);
        SKIPS.set(BidiReference.LRO);
        SKIPS.set(BidiReference.PDF);
        SKIPS.set(BidiReference.BN);
    }

    // have an iterator to get all possible variations less than a given size,
    // then continue with a fixed set of interesting cases
    static class Sample {
        private byte[] byte_array = new byte[0];
        private final List<Byte> items = new ArrayList<Byte>();
        private final int maxSize;
        private int extraPointer = 0;
        private byte[] extraTest;

        public Sample(int maxSize) {
            this.maxSize = maxSize;
        }

        boolean next() {
            if (extraPointer == 0) {
                for (int i = items.size() - 1; i >= 0; --i) {
                    final Byte oldValue = items.get(i);
                    if (oldValue < BidiReference.TYPE_MAX) {
                        items.set(i, (byte) (oldValue + 1));
                        return true;
                    }
                    items.set(i, BidiReference.TYPE_MIN); // first value
                }
                if (items.size() < maxSize) {
                    items.add(0, BidiReference.TYPE_MIN);
                    return true;
                }
            }
            if (extraPointer < extraTests.length) {
                extraTest = extraTests[extraPointer];
                for (int i = 0; i < extraTest.length; ++i) {
                    if (i < items.size()) {
                        items.set(i, extraTest[i]);
                    } else {
                        items.add(i, extraTest[i]);
                    }
                }
                while (items.size() > extraTest.length) {
                    items.remove(items.size() - 1);
                }
                ++extraPointer;
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            final StringBuilder result = new StringBuilder();
            for (int i = 0; i < items.size(); ++i) {
                if (i != 0) {
                    result.append(" ");
                }
                result.append(BidiReference.typenames[items.get(i)]);
            }
            return result.toString();
        }

        public byte[] getArray() {
            if (byte_array.length != items.size()) {
                byte_array = new byte[items.size()];
            }
            for (int i = 0; i < items.size(); ++i) {
                byte_array[i] = items.get(i);
            }
            return byte_array;
        }
    }

    public static void write(PrintWriter out) throws FileNotFoundException {
        final int[] linebreaks = new int[1];

        final Map<String, Set<String>> resultToSource = new TreeMap<String, Set<String>>(SHORTEST_FIRST);
        final Map<String, Integer> condensed = new HashMap<String, Integer>();
        final Sample sample = new Sample(MAX_SIZE);

        main: while (sample.next()) {
            // make sure B doesn't occur in any but the last
            for (int i = 0; i < sample.items.size() - 1; ++i) {
                if (sample.items.get(i) == BidiReference.B) {
                    continue main;
                }
            }

            final String typeString = sample.toString();
            final byte[] TYPELIST = sample.getArray();
            linebreaks[0] = TYPELIST.length;
            condensed.clear();
            for (byte paragraphEmbeddingLevel = BIDI_START_LEVEL; paragraphEmbeddingLevel <= 1; ++paragraphEmbeddingLevel) {

                final String reorderedIndexes = reorderedIndexes(TYPELIST, paragraphEmbeddingLevel, linebreaks);
                Integer bitmask = condensed.get(reorderedIndexes);
                if (bitmask == null) {
                    bitmask = 0;
                }
                final int reordered = paragraphEmbeddingLevel == R_DEFAULT ? 3 : paragraphEmbeddingLevel + 1;
                bitmask |= 1 << (reordered);
                condensed.put(reorderedIndexes, bitmask);
            }
            for (final String reorderedIndexes : condensed.keySet()) {
                final Integer bitset = condensed.get(reorderedIndexes);
                addResult(resultToSource, typeString + "; " + Integer.toHexString(bitset).toUpperCase(Locale.ENGLISH), reorderedIndexes);
            }
        }
/*
        for (int i = BidiReference.TYPE_MIN; i < BidiReference.TYPE_MAX; ++i)
        {
            UnicodeSet data = new UnicodeSet("[:bidi_class=" +
                    BidiReference.typenames[i] + ":]");
            data.complement().complement();
            out.println("@Type:\t" + BidiReference.typenames[i] + ":\t" + data);
        }
*/
        int totalCount = 0;
        for (final String reorderedIndexes : resultToSource.keySet()) {
            out.println();
            final String[] parts = reorderedIndexes.split(";");
            out.println("@Levels:\t" + parts[0].trim());
            out.println("@Reorder:\t" + (parts.length < 2 ? "" : parts[1].trim()));
            int count = 0;
            for (final String sources : resultToSource.get(reorderedIndexes)) {
                out.println(sources);
                ++totalCount;
                ++count;
            }
            out.println();
            out.println("#Count:\t" + count);
        }
        out.println();
        out.println("#Total Count:\t" + totalCount);
        out.println();
        out.print("# EOF");
        System.out.println("#Total Count:\t" + totalCount);
        System.out.println("#Max Length:\t" + MAX_SIZE);
        out.close();
        System.out.println("Done");
    }

    private static void addResult(Map<String, Set<String>> resultToSource, final String source,
            final String reorderedIndexes) {
        Set<String> sources = resultToSource.get(reorderedIndexes);
        if (sources == null) {
            resultToSource.put(reorderedIndexes, sources = new LinkedHashSet());
        }
        sources.add(source);
    }

    private static String reorderedIndexes(byte[] types, byte paragraphEmbeddingLevel, int[] linebreaks) {

        final StringBuilder result = new StringBuilder();
        final BidiReference bidi = new BidiReference(types, paragraphEmbeddingLevel);

        final byte[] levels = bidi.getLevels(linebreaks);
        for (int i = 0; i < levels.length; ++i) {
            if (SKIPS.get(types[i])) {
                result.append(" x");
            } else {
                result.append(' ').append(levels[i]);
            }
        }
        result.append(";");

        final int[] reordering = bidi.getReordering(linebreaks);

        int lastItem = -1;
        boolean LTR = true;

        for (final int item : reordering) {
            if (item < lastItem) {
                LTR = false;
            }
            lastItem = item;
            if (SKIPS.get(types[item])) {
                continue;
            }
            if (result.length() != 0) {
                result.append(" ");
            }
            result.append(item);
        }
        return result.toString();
    }

    static Comparator<String> SHORTEST_FIRST = new Comparator<String>() {

        @Override
        public int compare(String o1, String o2) {
            final int result = o1.length() - o2.length();
            if (result != 0) {
                return result;
            }
            return o1.compareTo(o2);
        }

    };
}
