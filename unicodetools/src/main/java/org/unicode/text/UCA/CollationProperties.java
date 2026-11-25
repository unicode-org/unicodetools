package org.unicode.text.UCA;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.text.UCA.UCA.AppendToCe;
import org.unicode.text.UCA.UCA.UCAContents;
import org.unicode.text.UCA.UCA_Types.Alternate;
import org.unicode.text.utility.Utility;

public class CollationProperties {

    private static long addQuaternary(
            UCA uca, Alternate alternate, int collationElement, Integer preceding) {
        if (alternate == Alternate.NON_IGNORABLE) {
            return (long) collationElement << 16;
        }
        int l1 = CEList.getPrimary(collationElement);
        int l3 = CEList.getTertiary(collationElement);
        if (collationElement == 0) {
            return 0;
        } else if (l1 == 0 && l3 != 0 && preceding != null && uca.isVariable(preceding)) {
            return 0;
        } else if (l1 != 0 && uca.isVariable(collationElement)) {
            return l1;
        } else if (l1 == 0 && l3 != 0 && (preceding == null || !uca.isVariable(preceding))) {
            return ((long) collationElement << 16) | 0xFFFF;
        } else if (l1 != 0 && !uca.isVariable(collationElement)) {
            return ((long) collationElement << 16) | 0xFFFF;
        } else {
            throw new IllegalArgumentException(
                    (preceding == null ? "null" : new CEList(new int[] {preceding}).toString())
                            + new CEList(new int[] {collationElement}).toString());
        }
    }

    private static int removeQuaternary(long collationElement) {
        return (int) (collationElement >> 16);
    }

    public static class FoldingType {
        FoldingType(int level, Alternate alternate) {
            if (level < 1 || level > 4) {
                throw new IllegalArgumentException("Bad level " + level);
            }
            if (level == 4 && alternate == Alternate.NON_IGNORABLE) {
                throw new IllegalArgumentException("Bad level " + level + " for non-ignorable");
            }
            this.level = level;
            this.alternate = alternate;
        }

        @Override
        public boolean equals(Object other) {
            return (other instanceof FoldingType otherType)
                    && otherType.level == level
                    && otherType.alternate == alternate;
        }

        @Override
        public int hashCode() {
            return Objects.hash(level, alternate);
        }

        @Override
        public String toString() {
            return Integer.toString(level) + "_" + alternate;
        }

        int level;
        Alternate alternate;
    }

    static final FoldingType[] FOLDING_TYPES =
            new FoldingType[] {
                new FoldingType(1, Alternate.SHIFTED),
                new FoldingType(2, Alternate.SHIFTED),
                new FoldingType(3, Alternate.SHIFTED),
                new FoldingType(4, Alternate.SHIFTED),
                new FoldingType(1, Alternate.NON_IGNORABLE),
                new FoldingType(2, Alternate.NON_IGNORABLE),
                new FoldingType(3, Alternate.NON_IGNORABLE),
            };

    static int tertiaryMarkedness(int tertiary) {
        // Treat normal kana as unmarked, https://www.unicode.org/reports/tr10/#Asymmetric_Search.
        // We treat Katakana as the least marked for folding as they are a superset of the Hiragana,
        // so that folding a string to the least marked equivalent results in Katakana rather than
        // a mix, see the comments on kanaMap in unisift.c.

        // See https://www.unicode.org/reports/tr10/#Tertiary_Weight_Table.
        final int katakanaWeight = 0x11;
        final int hiraganaWeight = 0x0E;
        return tertiary == 0
                ? 0
                : tertiary == katakanaWeight ? 2 : tertiary == hiraganaWeight ? 3 : tertiary + 2;
    }

    static Comparator<String> markedness(final UCA uca, Alternate alternate) {
        return Comparator.<String, String>comparing(
                s -> {
                    final var collationElements = uca.getCEList(s, true);
                    int[] transformed = new int[collationElements.length()];
                    for (int i = 0; i < collationElements.length(); ++i) {
                        final int elements = collationElements.at(i);
                        final int tertiary = CEList.getTertiary(elements);
                        transformed[i] =
                                elements & ~CEList.TERTIARY_MAX | tertiaryMarkedness(tertiary);
                    }
                    return uca.getSortKey(
                            new CEList(transformed), "", alternate, true, AppendToCe.tieBreaker);
                });
    }

    private static final long[] MASKS = {
        0, 0xFFFF_0000_0000L, 0xFFFF_FF80_0000L, 0xFFFF_FFFF_0000L, 0xFFFF_FFFF_FFFFL
    };

    public static Map<FoldingType, UnicodeMap<String>> getFoldings(VersionInfo version) {
        if (version.compareTo(VersionInfo.UNICODE_2_1_9) < 0) {
            return Map.of();
        }
        final UCA uca = UCA.buildDucetCollator(version);
        final UCAContents ucaContents = uca.getContents(null);

        // First we compute the equivalence classes of all strings explicitly listed in the DUCET
        // for all folding types.
        // Note that this does not yet give us the equivalence classes of all code points: we will
        // see circled ideographs, but not their primary equivalent ideographs, which have implicit
        // weights.
        final Map<FoldingType, TreeMap<long[], UnicodeSet>> elementsToStringsByType =
                new HashMap<>();
        final long start = System.currentTimeMillis();
        for (String s = ucaContents.next(); s != null; s = ucaContents.next()) {
            CEList collationElements = ucaContents.getCEs();
            for (final var type : FOLDING_TYPES) {
                long[] maskedElements = new long[collationElements.length()];
                for (int i = 0; i < collationElements.length(); ++i) {
                    maskedElements[i] =
                            MASKS[type.level]
                                    & addQuaternary(
                                            uca,
                                            type.alternate,
                                            collationElements.at(i),
                                            i == 0 ? null : collationElements.at(i - 1));
                }
                long[] levelElements =
                        Arrays.stream(maskedElements).filter(ce -> ce != 0).toArray();
                elementsToStringsByType
                        .computeIfAbsent(type, k -> new TreeMap<>(Arrays::compare))
                        .computeIfAbsent(levelElements, k -> new UnicodeSet())
                        .add(s);
            }
        }

        final Map<FoldingType, UnicodeMap<String>> collationFoldings = new HashMap<>();
        for (final var type : FOLDING_TYPES) {
            // For each equivalence class in elementsToStringsByType.get(type), pick the least
            // marked element as a representative.
            // This is not the folding: besides the fact that these equivalence classes do not
            // include code points with implicit weights, the primary equivalence class {æ, Æ} does
            // not contain the folding "ae".
            final Map<long[], String> representatives = new TreeMap<>(Arrays::compare);
            final var markedness = markedness(uca, type.alternate);
            for (final var entry : elementsToStringsByType.get(type).entrySet()) {
                final long[] elements = entry.getKey();
                final UnicodeSet strings = entry.getValue();
                representatives.put(elements, strings.stream().min(markedness).get());
            }

            final UnicodeMap<String> collationFolding =
                    collationFoldings.computeIfAbsent(type, k -> new UnicodeMap<>());

            // If a string that has been considered so far maps to a sequence of collation elements
            // at the current level, it might fold to an equivalent sequence of representatives,
            // computed by `getFoldingExpansion`; this handles equivalences involving implicit
            // weights, which result in sequences of collation elements.
            // If that is not the case, it folds to its representative given by `representatives`.
            for (final var entry : elementsToStringsByType.get(type).entrySet()) {
                final long[] elements = entry.getKey();
                final UnicodeSet strings = entry.getValue();
                if (elements.length > 1) {
                    final var folding = getFoldingExpansion(uca, type, representatives, elements);
                    if (folding != null) {
                        collationFolding.putAll(
                                strings.cloneAsThawed().remove(folding).removeAllStrings(),
                                folding);
                        continue;
                    }
                }
                collationFolding.putAll(
                        strings.cloneAsThawed()
                                .remove(representatives.get(elements))
                                .removeAllStrings(),
                        representatives.get(elements));
            }
            final var dt =
                    IndexUnicodeProperties.make(version)
                            .getProperty(UcdProperty.Decomposition_Type);
            // We compute the foldings before decomposing so that we handle contractions properly
            // without having to actually do the special handling of contraction required of an
            // implementation of collation folding of strings: a canonical composite that is not a
            // singleton is a contraction (like й) if it primary folds to itself.
            // As far as equivalences of code points are concerned, folding to the decomposition
            // would work too (we would need to keep the combining breve around in the primary
            // folding), but it would be harder to find the contractions based on properties.
            // Either way, contractions that do not correspond to a single character are have no
            // effect on these mappings.
            // When it comes to folding of strings, contractions require special handling, and the
            // exact special handling would probably depend on this choice.

            // Now add the canonical composites that are not listed in allkeys.txt (the Hangul
            // syllables).
            for (final String cp :
                    dt.getSet(UcdPropertyValues.Decomposition_Type_Values.Canonical)) {
                if (!collationFolding.containsKey(cp)) {
                    collationFolding.put(
                            cp,
                            uca.getNFDNormalizer()
                                    .normalize(cp)
                                    .codePoints()
                                    .mapToObj(
                                            c ->
                                                    Objects.requireNonNullElse(
                                                            collationFolding.get(c),
                                                            Character.toString(c)))
                                    .collect(Collectors.joining()));
                }
            }
        }
        System.out.println(
                "Computed collation foldings for "
                        + version
                        + " in "
                        + (System.currentTimeMillis() - start)
                        + " ms");
        return collationFoldings;
    }

    /**
     * If every element in {@code elements} is equivalent to some code point, returns the
     * concatenation of the representatives of each element. Returns null otherwise. For that
     * purpose, an implicit weight pair is treated atomically, and its representative is deduced
     * from the weight pair. Other representatives are taken from {@code representatives}. This
     * relies on the following properties:
     *
     * <ol>
     *   <li>two distinct code points with implicit weights are primary different;
     *   <li>a code point with an implicit weight is the least marked element of its equivalence
     *       class (that is, any expansion that contains implicit weights has tertiary weights
     *       greater than 2).
     * </ol>
     *
     * {@code elements} and {@code representatives} must be consistent with {@code type}.
     */
    private static String getFoldingExpansion(
            final UCA uca,
            final FoldingType type,
            final Map<long[], String> representatives,
            final long[] elements) {
        final var folding = new StringBuilder();
        for (int i = 0; i < elements.length; ++i) {
            if (UCA.isImplicitLeadCE(removeQuaternary(elements[i]))) {
                final int cp =
                        uca.implicit.codePointForPrimaryPair(
                                CEList.getPrimary(removeQuaternary(elements[i])),
                                CEList.getPrimary(removeQuaternary(elements[i + 1])));
                final CEList cpElements = uca.getCEListForImplicit(cp);
                long[] maskedElements = new long[cpElements.length()];
                for (int j = 0; j < cpElements.length(); ++j) {
                    maskedElements[j] =
                            MASKS[type.level]
                                    & addQuaternary(
                                            uca,
                                            type.alternate,
                                            cpElements.at(j),
                                            /* preceding= */ null);
                }
                if (maskedElements[0] != elements[i] || maskedElements[1] != elements[i + 1]) {
                    return null;
                }
                ++i;
                folding.append(Character.toString(cp));
            } else {
                String representative = representatives.get(new long[] {elements[i]});
                if (representative == null) {
                    return null;
                }
                folding.append(representative);
            }
        }
        return folding.toString();
    }

    /**
     * Returns the total order of code points defined by the UCA at the given version, using either
     * option for variable weighting, expressed as a map from each code point to its successor in
     * the total order. Omits mappings to the very next code point. The last code point is mapped to
     * itself.
     */
    public static Map<Alternate, UnicodeMap<String>> getNext(VersionInfo version) {
        if (version.compareTo(VersionInfo.UNICODE_2_1_9) < 0) {
            return Map.of();
        }
        final UCA uca = UCA.buildDucetCollator(version);
        final Map<Alternate, UnicodeMap<String>> nextByAlternate =
                Map.of(
                        Alternate.SHIFTED,
                        new UnicodeMap<>(),
                        Alternate.NON_IGNORABLE,
                        new UnicodeMap<>());
        for (final var alternate : Alternate.values()) {
            final var next = nextByAlternate.get(alternate);
            final TreeMap<String, Integer> totalOrder = new TreeMap<>();
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                totalOrder.put(
                        uca.getSortKey(
                                Character.toString(cp), alternate, true, AppendToCe.tieBreaker),
                        cp);
            }
            Integer preceding = null;
            for (final int cp : totalOrder.values()) {
                if (preceding != null && cp != preceding + 1) {
                    next.put(preceding, Character.toString(cp));
                }
                preceding = cp;
            }
            final int lastCodePoint = totalOrder.lastEntry().getValue();
            next.put(lastCodePoint, Character.toString(lastCodePoint));
        }
        return nextByAlternate;
    }

    public static UnicodeMap<String> getLeadTertiaryWeights(VersionInfo version) {
        if (version.compareTo(VersionInfo.UNICODE_2_1_9) < 0) {
            return new UnicodeMap<>();
        }
        final UCA uca = UCA.buildDucetCollator(version);
        final UnicodeMap<String> result = new UnicodeMap<>();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            final var collationElements = uca.getCEList(Character.toString(cp), true);
            if (collationElements.length() == 0) {
                result.put(cp, Utility.hex(0));
                continue;
            }
            final int leadTertiary = CEList.getTertiary(collationElements.at(0));
            if (leadTertiary != 2) {
                result.put(cp, Utility.hex(leadTertiary));
            }
        }
        return result;
    }
}
