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
            return other instanceof FoldingType
                    && ((FoldingType) other).level == level
                    && ((FoldingType) other).alternate == alternate;
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

    static Comparator<String> markedness(final UCA uca, Alternate alternate) {
        // Treat normal kana as unmarked, https://www.unicode.org/reports/tr10/#Asymmetric_Search.
        // We treat katakana as the least marked for folding, see the comments on kanaMap in
        // unisift.c.
        return Comparator.<String, String>comparing(
                s -> {
                    final var collationElements = uca.getCEList(s, true);
                    int[] transformed = new int[collationElements.length()];
                    for (int i = 0; i < collationElements.length(); ++i) {
                        final int elements = collationElements.at(i);
                        final int tertiary = CEList.getTertiary(elements);
                        transformed[i] =
                                tertiary & ~CEList.TERTIARY_MAX
                                        | (tertiary == 0
                                                ? 0
                                                : tertiary == 0x11
                                                        ? 2
                                                        : tertiary == 0x0E ? 3 : tertiary + 2);
                    }
                    return uca.getSortKey(
                            new CEList(transformed), "", alternate, true, AppendToCe.tieBreaker);
                });
    }

    public static Map<FoldingType, UnicodeMap<String>> getFoldings(VersionInfo version) {
        if (version.compareTo(VersionInfo.UNICODE_2_1_9) < 0) {
            return Map.of();
        }
        final UCA uca = UCA.buildDucetCollator(version);
        final UCAContents ucaContents = uca.getContents(null);
        final Map<FoldingType, UnicodeMap<long[]>> stringToElementsByType = new HashMap<>();
        final Map<FoldingType, TreeMap<long[], UnicodeSet>> elementsToStringsByType =
                new HashMap<>();
        final long[] masks = {
            0, 0xFFFF_0000_0000L, 0xFFFF_FF80_0000L, 0xFFFF_FFFF_0000L, 0xFFFF_FFFF_FFFFL
        };
        final long start = System.currentTimeMillis();
        for (String s = ucaContents.next(); s != null; s = ucaContents.next()) {
            CEList collationElements = ucaContents.getCEs();
            for (final var type : FOLDING_TYPES) {
                long[] maskedElements = new long[collationElements.length()];
                for (int i = 0; i < collationElements.length(); ++i) {
                    maskedElements[i] =
                            masks[type.level]
                                    & addQuaternary(
                                            uca,
                                            type.alternate,
                                            collationElements.at(i),
                                            i == 0 ? null : collationElements.at(i - 1));
                }
                long[] levelElements = Arrays.stream(maskedElements).filter(i -> i != 0).toArray();
                stringToElementsByType
                        .computeIfAbsent(type, k -> new UnicodeMap<>())
                        .put(s, levelElements);
                elementsToStringsByType
                        .computeIfAbsent(type, k -> new TreeMap<>(Arrays::compare))
                        .computeIfAbsent(levelElements, k -> new UnicodeSet())
                        .add(s);
            }
        }

        final Map<FoldingType, UnicodeMap<String>> collationFoldings = new HashMap<>();
        for (final var type : FOLDING_TYPES) {
            final Map<long[], String> representatives = new TreeMap<>(Arrays::compare);
            for (final var entry : elementsToStringsByType.get(type).entrySet()) {
                final long[] elements = entry.getKey();
                final UnicodeSet strings = entry.getValue();
                representatives.put(
                        elements, strings.stream().min(markedness(uca, type.alternate)).get());
            }
            final UnicodeMap<String> collationFolding =
                    collationFoldings.computeIfAbsent(type, k -> new UnicodeMap<>());
            foldExpansions:
            for (final var entry : elementsToStringsByType.get(type).entrySet()) {
                final long[] elements = entry.getKey();
                final UnicodeSet strings = entry.getValue();
                if (elements.length > 1) {
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
                                        masks[type.level]
                                                & addQuaternary(
                                                        uca,
                                                        type.alternate,
                                                        cpElements.at(j),
                                                        /* preceding= */ null);
                            }
                            if (maskedElements[0] != elements[i]
                                    || maskedElements[1] != elements[i + 1]) {
                                collationFolding.putAll(
                                        strings.cloneAsThawed()
                                                .remove(representatives.get(elements)),
                                        representatives.get(elements));
                                continue foldExpansions;
                            }
                            ++i;
                            folding.append(Character.toString(cp));
                        } else {
                            String representative = representatives.get(new long[] {elements[i]});
                            if (representative == null) {
                                collationFolding.putAll(
                                        strings.cloneAsThawed()
                                                .remove(representatives.get(elements)),
                                        representatives.get(elements));
                                continue foldExpansions;
                            }
                            folding.append(representative);
                        }
                    }
                    collationFolding.putAll(
                            strings.cloneAsThawed().remove(folding.toString()), folding.toString());
                } else {
                    collationFolding.putAll(
                            strings.cloneAsThawed().remove(representatives.get(elements)),
                            representatives.get(elements));
                }
                if (collationFolding.stringKeys() != null) {
                    collationFolding.removeAll(
                            new UnicodeSet().addAll(collationFolding.stringKeys()));
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

    public static Map<Alternate, UnicodeMap<String>> getNext(VersionInfo version) {
        if (version.compareTo(VersionInfo.UNICODE_2_1_9) < 0) {
            return Map.of();
        }
        final UCA uca = UCA.buildDucetCollator(version);
        final Map<Alternate, UnicodeMap<String>> next =
                Map.of(
                        Alternate.SHIFTED,
                        new UnicodeMap<>(),
                        Alternate.NON_IGNORABLE,
                        new UnicodeMap<>());
        for (final var alternate : Alternate.values()) {
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
                    next.get(alternate).put(preceding, Character.toString(cp));
                }
                preceding = cp;
            }
            next.get(alternate)
                    .put(
                            totalOrder.lastEntry().getValue(),
                            Character.toString(totalOrder.lastEntry().getValue()));
        }
        return next;
    }

    public static UnicodeMap<String> getTertiaryWeights(VersionInfo version) {
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
            final int tertiaryWeight = CEList.getTertiary(collationElements.at(0));
            for (int i = 1; i < collationElements.length(); ++i) {
                if (CEList.getTertiary(collationElements.at(0)) != tertiaryWeight) {
                    throw new IllegalArgumentException(
                            "Mixed tertiaries: " + Utility.hex(cp) + " " + collationElements);
                }
            }
            if (tertiaryWeight != 2) {
                result.put(cp, Utility.hex(tertiaryWeight));
            }
        }
        return result;
    }
}
