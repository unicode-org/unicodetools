package org.unicode.propstest;

import com.google.common.base.Objects;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import org.unicode.cldr.util.Pair;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.Normalizer;

public class ListPropsNfcDiff {
    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make();

    public static void main(String[] args) {
        System.out.println(
                "Property\tType\tCount\tSource\tValue(source)\tValue(nfc(source)) or Value(nfkc(source))");
        UnicodeMap<String> nfcDiff = new UnicodeMap<>();
        UnicodeMap<String> nfkcDiff = new UnicodeMap<>();
        getChanges(nfcDiff, nfkcDiff);

        EnumSet<UcdProperty> toTest = EnumSet.noneOf(UcdProperty.class);
        EnumSet<UcdProperty> toSkip = EnumSet.noneOf(UcdProperty.class);
        filterProps(toTest, toSkip);

        EnumSet<UcdProperty> nfcHasDiffs = EnumSet.noneOf(UcdProperty.class);
        Set<String> nfcTotals = new LinkedHashSet<>();
        EnumSet<UcdProperty> nfkcHasDiffs = EnumSet.noneOf(UcdProperty.class);
        Set<String> nfkcTotals = new LinkedHashSet<>();

        for (UcdProperty prop : toTest) {

            TreeMultimap<Pair<String, String>, String> nfcDiffMap =
                    findDiffs(nfcDiff, prop); // TreeMultimap.create();
            TreeMultimap<Pair<String, String>, String> nfkcDiffMap = findDiffs(nfkcDiff, prop);

            showDiffs(prop, "NFC", nfcDiffMap, nfcTotals, nfcHasDiffs);
            showDiffs(prop, "NFKC ≠ NFC", nfkcDiffMap, nfkcTotals, nfkcHasDiffs);
        }

        for (String s : nfcTotals) {
            System.out.println(s);
        }
        for (String s : nfkcTotals) {
            System.out.println(s);
        }

        EnumSet<UcdProperty> noDiffs = EnumSet.copyOf(toTest);
        noDiffs.removeAll(nfcHasDiffs);
        noDiffs.removeAll(nfkcHasDiffs);

        System.out.println(
                "NFC ≠ CP:\tTOTAL\t" + nfcDiff.size() + "\t" + nfcDiff.keySet().toPattern(false));
        System.out.println(
                "NFKC ≠ NFC:\tTOTAL\t"
                        + nfkcDiff.size()
                        + "\t"
                        + nfkcDiff.keySet().toPattern(false));

        showSet("Props Skipped:", toSkip);
        showSet("Props w/o Diffs:", noDiffs);
        showSet("Props with NFC Diffs:", nfcHasDiffs);
        showSet("Props with NFKC ≠ NFC Diffs:", nfkcHasDiffs);
    }

    private static void showDiffs(
            UcdProperty prop,
            String title,
            TreeMultimap<Pair<String, String>, String> nfcDiffMap,
            Set<String> totals,
            EnumSet<UcdProperty> hasDiffs) {
        for (Entry<Pair<String, String>, Collection<String>> entry :
                nfcDiffMap.asMap().entrySet()) {
            UnicodeSet us = new UnicodeSet().addAll(entry.getValue());
            System.out.println(
                    prop
                            + "\t"
                            + title
                            + "\t"
                            + us.size()
                            + "\t"
                            + us.toPattern(false)
                            + "\t"
                            + entry.getKey().getFirst()
                            + "\t"
                            + entry.getKey().getSecond());
        }
        UnicodeSet diffSet = new UnicodeSet().addAll(nfcDiffMap.values());
        if (!diffSet.isEmpty()) {
            hasDiffs.add(prop);
            totals.add(
                    prop
                            + "\t"
                            + title
                            + "\t"
                            + diffSet.size()
                            + "\t"
                            + diffSet.toPattern(false)
                            + "\t"
                            + "TOTAL");
        }
    }

    private static TreeMultimap<Pair<String, String>, String> findDiffs(
            UnicodeMap<String> nfcDiff, UcdProperty prop) {
        TreeMultimap<Pair<String, String>, String> diffs = TreeMultimap.create();
        for (Entry<String, String> entry : nfcDiff.entrySet()) {
            String source = entry.getKey();
            String target = entry.getValue();
            String sourceValue = iup.getResolvedValue(prop, source);
            String targetValue = iup.getResolvedValue(prop, target);
            if (!Objects.equal(sourceValue, targetValue)) {
                if (source.equals(sourceValue) && target.equals(targetValue)) {
                    continue;
                }
                diffs.put(new Pair<>(sourceValue, targetValue), source);
            }
        }
        return diffs;
    }

    private static void filterProps(EnumSet<UcdProperty> toTest, EnumSet<UcdProperty> toSkip) {
        for (UcdProperty prop : iup.getAvailableUcdProperties()) {
            switch (prop) {
                default:
                    if (!prop.toString().startsWith("k")) {
                        break;
                    }
                case Age:
                case Block:
                case Name:
                case Unicode_1_Name:
                case Decomposition_Mapping:
                case NFC_Quick_Check:
                case NFD_Quick_Check:
                case NFKC_Quick_Check:
                case NFKD_Quick_Check:
                case Changes_When_NFKC_Casefolded:
                case Full_Composition_Exclusion:
                case Decomposition_Type:
                    toSkip.add(prop);
                    continue;
            }
            toTest.add(prop);
        }
    }

    private static void getChanges(UnicodeMap<String> nfcDiff, UnicodeMap<String> nfkcDiff) {
        Normalizer nfc = Default.nfc();
        Normalizer nfkc = Default.nfkc();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            String s = UTF16.valueOf(cp);
            String nfcValue = nfc.transform(s);
            int singleCodePoint = UnicodeSet.getSingleCodePoint(nfcValue);
            if (singleCodePoint != Integer.MAX_VALUE && singleCodePoint != cp) {
                nfcDiff.put(cp, nfcValue);
            }
            if (nfkc.isNormalized(cp)) {
                continue;
            }
            String nfkcValue = nfkc.transform(s);
            if (nfkcValue.equals(nfcValue)) {
                continue;
            }
            int singleCodePoint2 = UnicodeSet.getSingleCodePoint(nfkcValue);
            if (singleCodePoint2 != Integer.MAX_VALUE && singleCodePoint2 != singleCodePoint) {
                nfkcDiff.put(cp, nfcValue);
            }
        }
        nfcDiff.freeze();
        nfkcDiff.freeze();
    }

    private static void showSet(String title, EnumSet<UcdProperty> toSkip) {
        System.out.println(title + "\tTOTAL\t" + toSkip.size() + "\t" + toSkip);
    }
}
