package org.unicode.tools;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.With;

import com.google.common.base.Objects;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R5;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class GenerateNormalizeForMatch {
    private static final String DEBUG_PRINT = UTF16.valueOf(0xE0FDF);
    private static final String dir = "/Users/markdavis/Google Drive/workspace/DATA/frequency/";
    private static final String GOOGLE_FOLDING_TXT = "google_folding.txt";

    private static final Comparator<String> CODEPOINT = new StringComparator(true, false, StringComparator.FOLD_CASE_DEFAULT);
    private static final Comparator<String> UCA;

    static {
        final Collator uca_raw = Collator.getInstance(ULocale.ROOT);
        uca_raw.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        UCA = new MultiComparator<String>((Comparator<String>)(Comparator<?>) uca_raw, CODEPOINT);
    }

    private static final Normalizer2 nfkccf = Normalizer2.getNFKCCasefoldInstance();
    private static final Normalizer2 nfc = Normalizer2.getNFCInstance();

    // Results
    private static final UnicodeMap<String> N4M = new UnicodeMap<>();
    private static final UnicodeMap<String> TRIAL = new UnicodeMap<>();
    private static final UnicodeMap<String> REASONS = new UnicodeMap<>();
    private static final UnicodeMap<String> ADDITIONS_TO_NFKCCF = new UnicodeMap<>();

    public static void main(String[] args) throws IOException {
        gatherData();
        computeTrial();
        //printData();
        showSimpleData();
        //showItemsIn(new UnicodeSet(N4M.keySet()).addAll(TRIAL.keySet()));
    }

    private static void showSimpleData() {
        System.out.println("#Source\tTarget\tMapping\tName-Mapping\tReason");
        for (Entry<String, String> entry : TRIAL.entrySet()) {
            final String source = entry.getKey();
            final String target = entry.getValue();
            String reason = REASONS.get(source);
            if (source.contains(DEBUG_PRINT)) {
                int debug = 0;
            }
            final String sourceName = UCharacter.getName(source, "+");
            System.out.println(Utility.hex(source) + ";\t" + Utility.hex(target, 4, " ") 
                    + (sourceName == null || sourceName.equals("null") ? ";\t\t" 
                            : ";\t # ( " + source + " → " + target + " )\t"
                            + sourceName + " → " + UCharacter.getName(target,"+")
                            )
                            + "\t" + reason);
        }
    }

    private static void printData() {
        showMapping(ADDITIONS_TO_NFKCCF, nfkccf);
    }

    /**
     * Here we try to reverse engineer the derivation, starting with NFKCCasefold
     */
    private static void computeTrial() {

        final UnicodeMap<String> SPECIAL_CASES = new UnicodeMap<String>()
                .put("ß", "ß")
                .put("ẞ", "ß")
                .put("İ", "i")
                .put("\u2044", "/")
                .put("\u2215", "/")
                .freeze();

        final UnicodeSet CN_CS_CO = new UnicodeSet("[[:Cn:]&[:Cs:]&[:Co:]-[:di:]]").freeze();
        final UnicodeSet HANGUL_COMPAT = new UnicodeSet("[\\p{Block=Hangul Compatibility Jamo}-[:di:]-[:cn:]]").freeze();
        final UnicodeSet HANGUL_HALFWIDTH = new UnicodeSet("[[:dt=narrow:]&[:script=Hang:]]").freeze();
        //final UnicodeSet DEPRECATED = new UnicodeSet("\\p{deprecated}").freeze();
        final UnicodeSet SEPARATE_DECOMP_TYPES = new UnicodeSet("["
                + "[:dt=Square:]"
                + "[:dt=Fraction:]"
                + "]")
        .freeze();
        final UnicodeSet NOCHANGE_DECOMP_TYPES = new UnicodeSet("["
                + "[:dt=Super:]"
                + "[:dt=Sub:]"
                + "]")
        .freeze();

        final UnicodeSet SUPER = new UnicodeSet("["
                + "[:dt=Super:]"
                + "]")
        .freeze();

        final UnicodeSet DIGITS = new UnicodeSet("["
                + "[:gc=Nd:]"
                + "]")
        .freeze();

        UnicodeMap<String> toSuper = new UnicodeMap<>();
        for (String s : new UnicodeSet("[:dt=Super:]")) {
            String normal = nfkccf.normalize(s);
            if (DIGITS.contains(normal)) {
                toSuper.put(normal, s);
            }
        }
        toSuper.freeze();

        final char SEPARATOR = ' ';

        main:
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {

                // Unassigned but not Default_Ignorable_Code_Point → no change

                if (CN_CS_CO.contains(cp)) {
                    //reason = ("Cn or Cs or Co");
                    continue;
                }
                if (HANGUL_COMPAT.contains(cp)) {
                    //reason = ("Cn or Cs or Co");
                    continue;
                }
                String source = UTF16.valueOf(cp);
                String target = source;
                String reason = "";

                subloop: {
                    // Special cases
                    String remapped = SPECIAL_CASES.get(cp);
                    if (remapped != null) {
                        target = remapped;
                        reason = ("remapped exceptions");
                        break subloop;
                    }
                    // Decorated numbers: (⓿→0),... // origin, UCA
                    remapped = ADDITIONS_TO_NFKCCF.get(source);
                    if (remapped != null) {
                        target = remapped;
                        reason = ("X-NFKC_CF");
                        break subloop;
                    }

                    // decomposition type = squared, fraction → Map to NFKC
                    // if the target ends with a digit, and there are no other digits, superscript the last
                    // if there is more than one cp in the target, surround by separators.
                    if (SEPARATE_DECOMP_TYPES.contains(cp)) {
                        target = nfkccf.normalize(source);
                        reason = "DT_SQUARE_FRACTION";
                        int lastCp = target.codePointBefore(target.length());
                        String mod = toSuper.get(lastCp);
                        if (mod != null) {
                            String prefix = target.substring(0,target.length() - Character.charCount(lastCp));
                            if (DIGITS.containsNone(prefix)) {
                                target = prefix + mod;
                                reason += ", superscript-numbers";
                            }
                        }
                        if (target.codePointCount(0, target.length()) > 1) {
                            target = SEPARATOR + target + SEPARATOR;
                            reason += ", separate";
                        }
                        break subloop;
                    }
                    // decomposition type = super, sub → do not map, stop
                    if (NOCHANGE_DECOMP_TYPES.contains(cp)) {
                        continue;
                    }
                    // Get NFKC_CF mapping
                    target = nfkccf.normalize(source);

                    // HANGUL_HALFWIDTH
                    if (HANGUL_HALFWIDTH.contains(cp)) {
                        if (!target.isEmpty()) { // exclude filler
                            reason = ("TBD: map to Hangul Compat Jamo");
                            break subloop;
                        }
                    }

                    // length(value) ≠1 && contains any of  " ",  "(",  ".",  ",",  "〔" → no change (discard mapping)

                    if (target.codePointCount(0, target.length()) > 1) {
                        for (String skipIfInDecomp : Arrays.asList(" ", "(", ".", ",", "〔")) {
                            if (target.contains(skipIfInDecomp)) {
                                reason = ("Skip decomp contains «" + skipIfInDecomp 
                                        + "» (and isn't singleton)");
                                continue main;
                            }
                        }
                    }
                    if (!REASONS.containsKey(cp)) {
                        int dti = UCharacter.getIntPropertyValue(cp, UProperty.DECOMPOSITION_TYPE);
                        String suffix = dti == 0 ? "Other" : UCharacter.getPropertyValueName(UProperty.DECOMPOSITION_TYPE, dti, NameChoice.SHORT);
                        reason = "NFKC_CF-" + suffix;
                    }
                }
                if (!source.equals(target)) {
                    TRIAL.put(cp, target);
                    REASONS.put(cp, reason);
                }
            }
        // TODO Recurse on trial
        while (true) {
            UnicodeMap<String> delta = new UnicodeMap<String>();
            UnicodeSet removals = new UnicodeSet();
            for (Entry<String, String> entry : TRIAL.entrySet()) {
                String source = entry.getKey();
                String oldTarget = entry.getValue();
                String newTarget = TRIAL.transform(oldTarget);
                if (!newTarget.equals(oldTarget)) {
                    if (newTarget.equals(source)) { // just in case
                        removals.add(source);
                    } else {
                        delta.put(source, newTarget);
                        String reason = REASONS.get(source);
                        REASONS.put(source, reason + ", recursion");
                    }
                }
            }
            if (delta.isEmpty()) break;
            TRIAL.putAll(delta);
            //System.out.println("# Recursion " + delta);
        }
        TRIAL.freeze();
        REASONS.freeze();
    }

    static final Pattern SPACES = Pattern.compile("[,\\s]+");

    private static void gatherData() {
        // get extended mapping
        Relation<String,String> XNFKCCF2 = Relation.of(new TreeMap<String,Set<String>>(UCA), TreeSet.class);
        for (String line : FileUtilities.in(GenerateNormalizeForMatch.class, "XNFKCCF.txt")) {
            String[] parts = FileUtilities.cleanSemiFields(line);
            if (parts == null) continue;
            String source = Utility.fromHex(parts[0], 1, SPACES);
            String target = parts.length < 2 || parts[1].isEmpty() ? "" : Utility.fromHex(parts[1], 0, SPACES);
            target = nfc.normalize(target); // since NFC is applied afterwards
            ADDITIONS_TO_NFKCCF.put(source, target);
            XNFKCCF2.put(target, source);
        }
        ADDITIONS_TO_NFKCCF.freeze();

        // Gather data
        //    for (String line : FileUtilities.in(dir, GOOGLE_FOLDING_TXT)) {
        //      String[] parts = FileUtilities.cleanSemiFields(line);
        //      if (parts == null) continue;
        //      String source = Utility.fromHex(parts[0], 1, SPACES);
        //      String target = parts.length < 2 ? "" : Utility.fromHex(parts[1], 0, SPACES);
        //      target = nfc.normalize(target); // since NFC is applied afterwards
        //      N4M.put(source, target);
        //    }
        N4M.freeze();
    }

    // The following is just used to print out differences

    enum Difference {trial_only, n4m_only, different}
    enum Age {before51, from51to70}

    private static final String SEP = "\t";

    private static void showMapping(UnicodeMap<String> sourceMap, Normalizer2 nfkccf2) {
        UnicodeSet changed = new UnicodeSet();
        System.out.println("#source ; target ; nfkccf (if ≠) ; uca equiv (if ≠) # (source→target) names");
        for (Entry<String, String> x : sourceMap.entrySet()) {
            String source = x.getKey();
            String target = x.getValue();
            final String nfkccfResult = nfkccf2.normalize(source);
            if (target.equals(nfkccfResult)) {
                continue;
            }
            String colEquiv = CollatorEquivalences.COLLATION_MAP.get(source);
            if (colEquiv == null) {
                colEquiv = source;
            }

            changed.add(source);
            System.out.println(Utility.hex(source)
                    + " ;\t" + Utility.hex(target,4," ") 
                    + " ;\t" + (target.equals(nfkccfResult) ? "" : Utility.hex(nfkccfResult,4," "))
                    + " ;\t" + (target.equals(colEquiv) ? "" : Utility.hex(colEquiv,4," "))
                    + " #\t(" + source + "→" + target + ")\t"
                    + getName(source," + ") + " → " + getName(target," + ")
                    );

        }
        System.out.println("# Total: " + changed.size());
        System.out.println("# " + changed.toPattern(false));

        System.out.println("\n\n# Other collation equivalences");

        changed.clear();
        final UnicodeSet COMBINING = new UnicodeSet("[:m:]").freeze();
        final UnicodeSet HIRAGANA = new UnicodeSet("[:sc=Hiragana:]").freeze();
        final UnicodeSet NUMBER_DECIMAL = new UnicodeSet("[:Nd:]").freeze();
        final UnicodeSet DECIMAL = new UnicodeSet("[:N:]").freeze();

        for (Entry<String, String> x : CollatorEquivalences.COLLATION_MAP.entrySet()) {
            String source = x.getKey();
            if (sourceMap.containsKey(source) || HIRAGANA.containsAll(source)) {
                continue;
            }
            String target = x.getValue();
            if (target.isEmpty()) {
                continue;
            }
            final String nfkccfResult = nfkccf2.normalize(source);
            if (target.equals(nfkccfResult) || target.isEmpty()) {
                continue;
            }
            if (COMBINING.containsAll(source) != COMBINING.containsAll(target)) {
                continue;
            }
            if (DECIMAL.containsAll(source) && NUMBER_DECIMAL.containsAll(target)) {
                continue;
            }

            changed.add(source);
            System.out.println(Utility.hex(source)
                    + " ;\t" + Utility.hex(target,4," ") 
                    + " #\t(" + source + "→" + target + ")\t"
                    + getName(source," + ") + " → " + getName(target," + ")
                    );

        }
        System.out.println("# Total: " + changed.size());
        System.out.println("# " + changed.toPattern(false));
    }

    private static String getName(String best, String separator) {
        StringBuilder b = new StringBuilder();
        for (int cp : With.codePointArray(best)) {
            if (b.length() > 0) {
                b.append(separator);
            }
            b.append(UCharacter.getExtendedName(cp));
        }
        return b.toString();
    }

    private static void showItemsIn(UnicodeSet combined) {

        Set<Row.R5<Age, Difference, Integer, Integer, String>> sorted = new TreeSet<>();
        Counter<Row.R2<Age, Difference>> counter = new Counter<>();
        for (String source : combined) {
            // Skip anything ≥ Unicode 8.0
            int sourceCodePoint = source.codePointAt(0);
            final VersionInfo ageValue = UCharacter.getAge(sourceCodePoint);
            if (ageValue.compareTo(VersionInfo.UNICODE_8_0) >= 0) {
                continue;
            }

            String n4mValue = N4M.get(source);
            String trialValue = TRIAL.get(source);
            if (Objects.equal(n4mValue, trialValue)) {
                continue;
            }

            String reason = REASONS.get(source);
            int generalCategory = UCharacter.getIntPropertyValue(sourceCodePoint, UProperty.GENERAL_CATEGORY);
            int decompType = UCharacter.getIntPropertyValue(sourceCodePoint, UProperty.DECOMPOSITION_TYPE);

            Age age = ageValue.compareTo(VersionInfo.UNICODE_5_1) >= 0 ? Age.from51to70
                    : Age.before51;

            Difference difference = n4mValue == null ? Difference.trial_only 
                    : trialValue == null ? Difference.n4m_only 
                            : Difference.different;

            String nfkccfValue = nfkccf.normalize(source);
            if (nfkccfValue.equals(source)) {
                nfkccfValue = null; // below, null means no change
            }
            sorted.add(Row.of(age, difference, decompType, generalCategory, 
                    ageValue.getVersionString(2, 2)
                    + SEP + source
                    + SEP + hex(source) 
                    + SEP + hex(n4mValue)
                    + SEP + hex(trialValue)
                    + SEP + (Objects.equal(nfkccfValue,trialValue) ? "≣" : hex(nfkccfValue))
                    + SEP + (reason == null ? "" : reason)
                    + SEP + UCharacter.getExtendedName(sourceCodePoint)
                    ));
            counter.add(Row.of(age,difference), 1);
        }

        Age lastAge = null;
        Difference lastDifference = null;
        System.out.println("#AgeCat" 
                + SEP + "Type of difference" 
                + SEP + "Decomp type" 
                + SEP + "General Category" 
                + SEP + "Version"
                + SEP + "Source"
                + SEP + "Hex"
                + SEP + "N4M"
                + SEP + "Trial"
                + SEP + "NFKC_CF"
                + SEP + "Reason for Trial≠NFKC_CF"
                + SEP + "Name of Source"
                );

        for (R5<Age, Difference, Integer, Integer, String> item : sorted) {
            final Age age = item.get0();
            final Difference difference = item.get1();
            final String decompType = UCharacter.getPropertyValueName(
                    UProperty.DECOMPOSITION_TYPE, item.get2(), UProperty.NameChoice.LONG);
            final String cat = UCharacter.getPropertyValueName(
                    UProperty.GENERAL_CATEGORY, item.get3(), UProperty.NameChoice.LONG);
            final String info = item.get4();
            if (age != lastAge || difference != lastDifference) {
                System.out.println("\n#" + age + ", " + difference + "\n");
                lastAge = age;
                lastDifference = difference;
            }
            System.out.println(age + SEP + difference + SEP + decompType + SEP + cat + SEP + info);
        }
        System.out.println();
        for (R2<Age, Difference> key : counter.getKeysetSortedByKey()) {
            System.out.println(key + "\t" + counter.get(key));
        }
    }

    private static String hex(String n4mValue) {
        return n4mValue == null ? "<unchanged>" : n4mValue.isEmpty() 
                ? "delete" : "U+" + Utility.hex(n4mValue,4,", U+");
    }
}
