/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/GenerateData.java,v $ $Date:
 * 2009-08-18 23:38:46 $ $Revision: 1.43 $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.UCD;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.UTF32;
import org.unicode.text.utility.UnicodeDataFile;
import org.unicode.text.utility.Utility;

public class GenerateData implements UCD_Types {

    public static void writeNormalizerTestSuite(String directory, String fileName)
            throws IOException {
        final var nfd = Default.nfd();
        final var nfc = Default.nfc();
        final var nfkd = Default.nfkd();

        final UnicodeDataFile fc =
                UnicodeDataFile.openAndWriteHeader(directory, fileName)
                        .setSkipCopyright(Settings.SKIP_COPYRIGHT);
        final PrintWriter log = fc.out;

        final String[] example = new String[256];

        System.out.println("Writing Part 1");

        for (final String testSuiteCase : testSuiteCases) {
            writeLine(testSuiteCase, log, false);
        }
        // At least one implementation (ICU4X) has an edge case when a character
        // whose decomposition contains multiple starters and ends with a
        // non-starter is followed by a non-starter of lower CCC.
        // See https://github.com/unicode-org/unicodetools/issues/656
        // and https://github.com/unicode-org/icu4x/pull/4530.
        // That implementation also has separate code paths for the BMP and
        // higher planes.  No such decompositions currently exist outside the
        // BMP, but by generating these test cases we ensure that this would be
        // covered.
        // We stick them in Part 0, which is in principle for handcrafted test
        // cases, because there are not many of them, and the edge case feels a
        // tad too weird to describe in the title of a new part.
        final org.unicode.props.UnicodeProperty sc =
                IndexUnicodeProperties.make().getProperty(UcdProperty.Script);
        for (final String cp : UnicodeSet.ALL_CODE_POINTS) {
            final String[] decompositions = new String[] {nfd.normalize(cp), nfkd.normalize(cp)};
            for (final String decomposition : decompositions) {
                final int lastCCC =
                        Default.ucd()
                                .getCombiningClass(
                                        decomposition.codePointBefore(decomposition.length()));
                final long nonStarterCount =
                        decomposition
                                .codePoints()
                                .filter(c -> (Default.ucd().getCombiningClass(c) == 0))
                                .count();
                final String script = sc.getValue(cp.codePointAt(0));
                if (lastCCC > 1 && nonStarterCount > 1) {
                    // Try to pick a trailing nonstarter that might have a
                    // chance of combining with the character if possible,
                    // both for æsthetic reasons and to reproduce the example
                    // ICU4X came across.  If all else fails, use a character
                    // with CCC=1, as low as it gets.
                    if (script.equals("Arabic") && lastCCC > 220) {
                        // ARABIC SUBSCRIPT ALEF.
                        writeLine(cp + "\u0656", log, false);
                    } else if (lastCCC > 220) {
                        // COMBINING DOT BELOW.
                        writeLine(cp + "\u0323", log, false);
                    } else {
                        // COMBINING TILDE OVERLAY.
                        writeLine(cp + "\u0334", log, false);
                    }
                    break;
                }
            }
        }

        System.out.println("Writing Part 2");

        log.println("#");
        log.println("@Part1 # Character by character test");
        log.println(
                "# All characters not explicitly occurring in c1 of Part 1 have identical NFC, D, KC, KD forms.");
        log.println("#");

        for (int ch = 0; ch < 0x10FFFF; ++ch) {
            Utility.dot(ch);
            if (!Default.ucd().isAssigned(ch)) {
                continue;
            }
            if (Default.ucd().isPUA(ch)) {
                continue;
            }
            final String cc = UTF32.valueOf32(ch);
            writeLine(cc, log, true);
        }
        Utility.fixDot();

        System.out.println("Finding Examples");

        for (int ch = 0; ch < 0x10FFFF; ++ch) {
            Utility.dot(ch);
            if (!Default.ucd().isAssigned(ch)) {
                continue;
            }
            if (Default.ucd().isPUA(ch)) {
                continue;
            }
            final int cc = Default.ucd().getCombiningClass(ch);
            if (example[cc] == null) {
                example[cc] = UTF32.valueOf32(ch);
            }
        }

        Utility.fixDot();
        System.out.println("Writing Part 2");

        log.println("#");
        log.println("@Part2 # Canonical Order Test");
        log.println("#");

        for (int ch = 0; ch < 0x10FFFF; ++ch) {

            Utility.dot(ch);
            if (!Default.ucd().isAssigned(ch)) {
                continue;
            }
            if (Default.ucd().isPUA(ch)) {
                continue;
            }
            final short c = Default.ucd().getCombiningClass(ch);
            if (c == 0) {
                continue;
            }

            // add character with higher class, same class, lower class

            String sample = "";
            for (int i = c + 1; i < example.length; ++i) {
                if (example[i] == null) {
                    continue;
                }
                sample += example[i];
                break;
            }
            sample += example[c];
            for (int i = c - 1; i > 0; --i) {
                if (example[i] == null) {
                    continue;
                }
                sample += example[i];
                break;
            }

            writeLine("a" + sample + UTF32.valueOf32(ch) + "b", log, false);
            writeLine("a" + UTF32.valueOf32(ch) + sample + "b", log, false);
        }

        System.out.println("Writing Part 3");
        log.println("#");
        log.println("@Part3 # PRI #29 Test");
        log.println("#");

        final Set prilist = new TreeSet();

        for (int ch = 0; ch < 0x10FFFF; ++ch) {
            Utility.dot(ch);
            if (!Default.ucd().isAssigned(ch)) {
                continue;
            }
            if (Default.ucd().isPUA(ch)) {
                continue;
            }
            if (0xAC00 <= ch && ch <= 0xD7FF) { // skip most
                if (((ch - 0xAC00) % 91) != 0) {
                    continue;
                }
            }
            // also gather data for pri29 test
            if (ch == 0x09CB) {
                System.out.println("debug");
            }
            if (Default.ucd().getDecompositionType(ch) != CANONICAL) {
                continue;
            }
            final String s = Default.ucd().getDecompositionMapping(ch);
            if (UTF16.hasMoreCodePointsThan(s, 2)) {
                continue;
            }
            if (!UTF16.hasMoreCodePointsThan(s, 1)) {
                continue;
            }
            final int c1 = UTF16.charAt(s, 0);
            final int c2 = UTF16.charAt(s, UTF16.getCharCount(c1));
            if (Default.ucd().getCombiningClass(c1) != 0) {
                continue;
            }
            if (Default.ucd().getCombiningClass(c2) != 0) {
                continue;
            }
            prilist.add(UTF16.valueOf(c1) + '\u0334' + UTF16.valueOf(c2));
        }
        Utility.fixDot();

        for (final Iterator it = prilist.iterator(); it.hasNext(); ) {
            writeLine((String) it.next(), log, false);
        }

        Utility.fixDot();

        System.out.println("Writing Part 4");
        log.println("#");
        log.println("@Part4 # Canonical closures (excluding Hangul)");
        log.println("#");
        for (var entry : canonicalDecompositionsByCodepoint.entrySet()) {
            final int cp = entry.getKey();
            final String decomposition = entry.getValue();
            if (cp >= 0xAC00 && cp <= 0xD7A3) {
                continue;
            }
            forAllStringsCanonicallyDecomposingTo(
                    decomposition,
                    s -> {
                        // Skip NFD and single code points (NFC or full
                        // composition exclusions), already covered in Part 1.
                        if (!s.equals(decomposition) && s.codePointCount(0, s.length()) != 1) {
                            writeLine(s, log, true);
                        }
                    });
        }

        System.out.println("Writing Part 5");
        log.println("#");
        log.println("@Part5 # Chained primary composites");
        log.println("#");

        // Not actually Builders of ImmutableMaps because those do not have
        // computeIfAbsent and because we want ImmutableSets too.
        final Map<Integer, Set<Integer>> primaryCompositesByFirstNFDCodePointBuilder =
                new TreeMap<>();
        final Map<Integer, Set<Integer>> primaryCompositesByLastNFDCodePointBuilder =
                new TreeMap<>();
        for (var entry : canonicalDecompositionsByCodepoint.entrySet()) {
            final int cp = entry.getKey();
            final String decomposition = entry.getValue();
            if (nfc.normalize(cp).equals(Character.toString(cp))) {
                int first = decomposition.codePointAt(0);
                int last = decomposition.codePointBefore(decomposition.length());
                primaryCompositesByFirstNFDCodePointBuilder
                        .computeIfAbsent(first, key -> new TreeSet<>())
                        .add(cp);
                primaryCompositesByLastNFDCodePointBuilder
                        .computeIfAbsent(last, key -> new TreeSet<>())
                        .add(cp);
            }
        }
        final ImmutableMap<Integer, ImmutableSet<Integer>> primaryCompositesByFirstNFDCodePoint =
                primaryCompositesByFirstNFDCodePointBuilder.entrySet().stream()
                        .collect(
                                ImmutableMap.toImmutableMap(
                                        entry -> entry.getKey(),
                                        entry -> ImmutableSet.copyOf(entry.getValue())));
        final ImmutableMap<Integer, ImmutableSet<Integer>> primaryCompositesByLastNFDCodePoint =
                primaryCompositesByLastNFDCodePointBuilder.entrySet().stream()
                        .collect(
                                ImmutableMap.toImmutableMap(
                                        entry -> entry.getKey(),
                                        entry -> ImmutableSet.copyOf(entry.getValue())));

        Collection<String> skippedNFCs = new ArrayList<>();
        Set<String> part5NFCs = new TreeSet<>();

        // The set of all sequences of two code points appearing within a
        // canonical decomposition.
        Set<String> links = new TreeSet<>();
        for (String decomposition : canonicalDecompositionsOfSingleCodepoints) {
            int first = decomposition.codePointAt(0);
            int second;
            for (int i = Character.charCount(first);
                    i < decomposition.length();
                    first = second, i += UTF16.getCharCount(first)) {
                second = decomposition.codePointAt(i);
                links.add(Character.toString(first) + Character.toString(second));
            }
        }

        for (String link : links) {
            int first = link.codePointAt(0);
            int second = link.codePointBefore(link.length());
            // Look for primary composites firstCandidate and secondCandidate
            // such that the concatenation of their canonical decompositions has
            // the link at the boundary, e.g., for a link YZ, look for cases
            // like firstDecomposition = XY, secondDecomposition = ZT.
            // In addition to primary composites, allow the candidates to
            // be a single canonically non-decomposable starters, thus
            // firstDecomposition = XY, secondDecomposition = Z with ccc(Z)=0 or
            // firstDecomposition = Y, secondDecomposition = ZT with ccc(Y)=0.
            // We do not allow non-starters, since an initial nonstarter cannot
            // do anything, and a final non-starter only exercises the CRA,
            // which is not what we are looking for in Part 5.

            // Note that if both the first and second candidates are
            // canonically non-decomposable starters, any linking code point
            // will cover the whole string, meaning this is already covered
            // in Parts 1 and 4, and we will find that out, so we do not emit
            // any such test cases.
            Set<Integer> firstCandidates = new TreeSet<>();
            Set<Integer> secondCandidates = new TreeSet<>();
            firstCandidates.addAll(
                    primaryCompositesByLastNFDCodePoint.getOrDefault(first, ImmutableSet.of()));
            secondCandidates.addAll(
                    primaryCompositesByFirstNFDCodePoint.getOrDefault(second, ImmutableSet.of()));
            if (Default.ucd().getCombiningClass(first) == 0) {
                firstCandidates.add(first);
            }
            if (Default.ucd().getCombiningClass(second) == 0) {
                secondCandidates.add(second);
            }
            for (int firstCandidate : firstCandidates) {
                for (int secondCandidate : secondCandidates) {
                    String firstDecomposition = nfd.normalize(firstCandidate);
                    String secondDecomposition = nfd.normalize(secondCandidate);
                    String decomposition = firstDecomposition + secondDecomposition;
                    if (canonicalDecompositionsOfSingleCodepoints.contains(decomposition)) {
                        // Already covered in parts 1 (single code points) and 4
                        // (canonical closures of single code points).
                        continue;
                    }
                    System.out.println(
                            Default.ucd().getName(firstCandidate)
                                    + "+"
                                    + Default.ucd().getName(secondCandidate));
                    // Within the canonical closure of the concatenation of
                    // firstCandidate and secondCandidate, look for strings that
                    // cannot be split between those two characters.
                    // Those are our test cases for Part 5.
                    String normalizedFormC = nfc.normalize(decomposition);
                    forAllStringsCanonicallyDecomposingTo(
                            decomposition,
                            s -> {
                                for (int j = 0; j < s.length(); ++j) {
                                    if (nfd.normalize(s.substring(0, j)).equals(firstDecomposition)
                                            && nfd.normalize(s.substring(j))
                                                    .equals(secondDecomposition)) {
                                        // The string splits into parts
                                        // equivalent to firstCandidate and
                                        // secondCandidate, i.e., it has no
                                        // link.
                                        return;
                                    }
                                }
                                if (s.equals(normalizedFormC)) {
                                    // If the NFC of
                                    // firstCandidate + secondCandidate has a
                                    // link, thus
                                    // NFC(firstCandidate + secondCandidate)
                                    //                            = f′ + l + s′,
                                    // with f′ prefix of firstCandidate and s′
                                    // suffix of secondCandidate, f′ must be
                                    // empty (otherwise the NFC would start with
                                    // the longer first), so we should see this
                                    // canonical equivalence class again for the
                                    // link between l and s′.
                                    // Since we give the NFC of all test cases
                                    // and instruct implementers to run all
                                    // normalizations on that column, we can
                                    // skip this one.
                                    // But in the spirit of “Beware of bugs in
                                    // the above code; I have only proved it
                                    // correct, not tried it.” (Knuth 1977), let
                                    // us check those statements.
                                    String linkDecomposition =
                                            nfd.normalize(normalizedFormC.codePointAt(0));
                                    if (!linkDecomposition.startsWith(firstDecomposition)) {
                                        throw new AssertionError(
                                                "The first code point of NFC("
                                                        + Default.ucd().getName(firstDecomposition)
                                                        + " + "
                                                        + Default.ucd().getName(secondDecomposition)
                                                        + ") does not cover the first part");
                                    }
                                    skippedNFCs.add(normalizedFormC);
                                    return;
                                }
                                part5NFCs.add(normalizedFormC);
                                writeLine(s, log, true);
                                System.out.println(Default.ucd().getName(s));
                            });
                }
            }
        }

        for (String normalizedFormC : skippedNFCs) {
            if (!part5NFCs.contains(normalizedFormC)) {
                throw new AssertionError(
                        "Candidate Part 5 test case "
                                + Default.ucd().getName(normalizedFormC)
                                + " was suppressed but did not appear as the NFC of another test"
                                + " case in Part 5.");
            }
        }

        Utility.fixDot();

        log.println("#");
        log.println("# EOF");
        fc.close();
    }

    private static final ImmutableMap<Integer, String> canonicalDecompositionsByCodepoint;
    private static final ImmutableSet<String> canonicalDecompositionsOfSingleCodepoints;

    static {
        ImmutableMap.Builder<Integer, String> builder = ImmutableMap.builder();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            String decomposition = Default.nfd().normalize(cp);
            if (!decomposition.equals(Character.toString(cp))) {
                builder.put(cp, decomposition);
            }
        }
        canonicalDecompositionsByCodepoint = builder.build();
        canonicalDecompositionsOfSingleCodepoints =
                ImmutableSet.copyOf(canonicalDecompositionsByCodepoint.values());
    }

    private static void forAllStringsCanonicallyDecomposingTo(
            String decomposition, Consumer<String> consumer) {
        final Set<Integer> candidateCharacters = new TreeSet<>();
        decomposition.codePoints().forEach(cp -> candidateCharacters.add(cp));
        for (var entry : canonicalDecompositionsByCodepoint.entrySet()) {
            int cp = entry.getKey();
            String nfd = entry.getValue();
            if (nfd.codePoints()
                    .allMatch(
                            candidateDecompositionCodePoint ->
                                    decomposition.contains(
                                            Character.toString(candidateDecompositionCodePoint)))) {
                candidateCharacters.add(cp);
            }
        }
        for (int length = 1;
                length < decomposition.codePointCount(0, decomposition.length());
                ++length) {
            forAllStrings(
                    candidateCharacters,
                    "",
                    length,
                    s -> {
                        if (Default.nfd().normalize(s).equals(decomposition)) {
                            consumer.accept(s);
                        }
                    });
        }
    }

    static void forAllStrings(
            Collection<Integer> repertoire,
            String prefix,
            int suffixLength,
            Consumer<String> consumer) {
        if (suffixLength == 0) {
            consumer.accept(prefix);
        } else {
            for (int next : repertoire) {
                forAllStrings(
                        repertoire, prefix + Character.toString(next), suffixLength - 1, consumer);
            }
        }
    }

    static void writeLine(String cc, PrintWriter log, boolean check) {
        final String c = Default.nfc().normalize(cc);
        String d = Default.nfd().normalize(cc);
        final String kc = Default.nfkc().normalize(cc);
        final String kd = Default.nfkd().normalize(cc);
        if (check & cc.equals(c) && cc.equals(d) && cc.equals(kc) && cc.equals(kd)) {
            return;
        }

        // consistency check
        final String dc = Default.nfd().normalize(c);
        final String dkc = Default.nfd().normalize(kc);
        if (!dc.equals(d) || !dkc.equals(kd)) {
            Normalizer.SHOW_PROGRESS = true;
            d = Default.nfd().normalize(cc);
            throw new AssertionError("Danger Will Robinson! " + Default.ucd().getName(cc));
        }

        // printout
        log.println(
                Utility.hex(cc, " ")
                        + ";"
                        + Utility.hex(c, " ")
                        + ";"
                        + Utility.hex(d, " ")
                        + ";"
                        + Utility.hex(kc, " ")
                        + ";"
                        + Utility.hex(kd, " ")
                        + "; # ("
                        + comma(cc)
                        + "; "
                        + comma(c)
                        + "; "
                        + comma(d)
                        + "; "
                        + comma(kc)
                        + "; "
                        + comma(kd)
                        + "; "
                        + ") "
                        + Default.ucd().getName(cc));
    }

    static StringBuffer commaResult = new StringBuffer();

    // not recursive!!!
    static final String comma(String s) {
        // if (true) return s;
        commaResult.setLength(0);
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            if (Default.ucd().getCategory(cp) == Mn) {
                commaResult.append('\u25CC');
            }
            UTF16.append(commaResult, cp);
        }
        return commaResult.toString();
    }

    static final String[] testSuiteCases = {
        "\u1E0A",
        "\u1E0C",
        "\u1E0A\u0323",
        "\u1E0C\u0307",
        "D\u0307\u0323",
        "D\u0323\u0307",
        "\u1E0A\u031B",
        "\u1E0C\u031B",
        "\u1E0A\u031B\u0323",
        "\u1E0C\u031B\u0307",
        "D\u031B\u0307\u0323",
        "D\u031B\u0323\u0307",
        "\u00C8",
        "\u0112",
        "E\u0300",
        "E\u0304",
        "\u1E14",
        "\u0112\u0300",
        "\u1E14\u0304",
        "E\u0304\u0300",
        "E\u0300\u0304",
        "\u05B8\u05B9\u05B1\u0591\u05C3\u05B0\u05AC\u059F",
        "\u0592\u05B7\u05BC\u05A5\u05B0\u05C0\u05C4\u05AD",
        "\u1100\uAC00\u11A8",
        "\u1100\uAC00\u11A8\u11A8",
    };
}
