package org.unicode.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.With;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyValueSets;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.Decomposition_Type_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.utility.Settings;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;

public class GenerateNormalizeForMatch {

    private static final String dir = "/Users/markdavis/Google Drive/workspace/DATA/frequency/";
    private static final String GOOGLE_FOLDING_TXT = "google_folding.txt";
    private static final Pattern SPACES = Pattern.compile("[,\\s]+");

    private static final Comparator<String> CODEPOINT = new StringComparator(true, false, StringComparator.FOLD_CASE_DEFAULT);
    private static final Comparator<String> UCA;

    static {
        org.unicode.text.UCA.UCA uca_raw = org.unicode.text.UCA.UCA.buildCollator(null);
        //        uca_raw.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        UCA = new MultiComparator<String>((Comparator<String>)(Comparator<?>) uca_raw, CODEPOINT);
    }
    private static final UnicodeMap<String> COLLATION_MAP = CollatorEquivalences.COLLATION_MAP;

    private static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(Default.ucdVersion());

    static UnicodeSet.XSymbolTable NO_PROPS = new UnicodeSet.XSymbolTable() {
        @Override
        public boolean applyPropertyAlias(String propertyName, String propertyValue, UnicodeSet result) {
            throw new IllegalArgumentException("Don't use any ICU Unicode Properties! " + propertyName + "=" + propertyValue);
        };
    };
    static {
        UnicodeSet.setDefaultXSymbolTable(NO_PROPS);
    }

    private static final UnicodeMap<General_Category_Values> GC = iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
    private static final UnicodeMap<Script_Values> SC = iup.loadEnum(UcdProperty.Script, Script_Values.class);
    private static final UnicodeMap<String> cpToName = iup.load(UcdProperty.Name);
    private static final UnicodeSet NO_NAME = cpToName.getSet(null);

    private static final UnicodeSet DI = iup.loadEnumSet(UcdProperty.Default_Ignorable_Code_Point, Binary.Yes);

    private static final UnicodeMap<String> cpToNFKCCF = iup.load(UcdProperty.NFKC_Casefold);
    private static final UnicodeMap<String> cpToLower = iup.load(UcdProperty.Lowercase_Mapping);
    private static final UnicodeMap<String> cpToSimpleLower = iup.load(UcdProperty.Simple_Lowercase_Mapping);
    private static final UnicodeSet UNASSIGNED = GC.getSet(General_Category_Values.Unassigned);
    private static final UnicodeMap<Decomposition_Type_Values> DT = iup.loadEnum(UcdProperty.Decomposition_Type, Decomposition_Type_Values.class);
    private static final UnicodeMap<Age_Values> AGE = iup.loadEnum(UcdProperty.Age, Age_Values.class);

    private static final UnicodeSet NFKCCF_SET = iup.loadEnumSet(UcdProperty.Changes_When_NFKC_Casefolded, Binary.Yes);
    private static final Normalizer nfc = Default.nfc();

    // Results
    private static final UnicodeMap<String> N4M = gatherData();
    private static final UnicodeMap<String> TRIAL = new UnicodeMap<>();
    private static final UnicodeMap<String> REASONS = new UnicodeMap<>();
    private static final NormalizeForMatch ADDITIONS_TO_NFKCCF = NormalizeForMatch.load("XNFKCCF-Curated.txt");


    private static final UnicodeSet HANGUL_COMPAT_minus_DI_CN 
    = new UnicodeSet(iup.loadEnumSet(UcdProperty.Block, Block_Values.Hangul_Compatibility_Jamo))
    .removeAll(DI)
    .removeAll(UNASSIGNED)
    .freeze();

    private static final UnicodeSet CN_CS_CO = PropertyValueSets.getSet(GC,
            General_Category_Values.Unassigned,
            General_Category_Values.Surrogate,
            General_Category_Values.Private_Use);
    //"[[:Cn:][:Cs:][:Co:]]").freeze(); // -[:di:]

    private static final UnicodeSet SPECIAL_DECOMP_TYPES = PropertyValueSets.getSet(DT, 
            Decomposition_Type_Values.Square, 
            Decomposition_Type_Values.Fraction);
    //            new UnicodeSet("["
    //            + "[:dt=Square:]"
    //            + "[:dt=Fraction:]"
    //            + "]")

    private static final UnicodeSet NOCHANGE_DECOMP_TYPES = PropertyValueSets.getSet(DT, 
            Decomposition_Type_Values.Super, 
            Decomposition_Type_Values.Sub,
            Decomposition_Type_Values.Vertical);
    //            new UnicodeSet("["
    //            + "[:dt=Super:]"
    //            + "[:dt=Sub:]"
    //            + "[:dt=Vertical:]"
    //            + "]")

    private static final UnicodeSet Nd = GC.getSet(General_Category_Values.Decimal_Number);
    //    = new UnicodeSet("["
    //            + "[:gc=Nd:]"
    //            + "]")
    //    .freeze();

    private static final UnicodeSet COMBINING = PropertyValueSets.getSet(GC, PropertyValueSets.MARK);
    // new UnicodeSet("[:m:]").freeze();

    private static final UnicodeSet HIRAGANA = SC.getSet(Script_Values.Hiragana); // new UnicodeSet("[:sc=Hiragana:]").freeze();
    private static final UnicodeSet DECIMAL = PropertyValueSets.getSet(GC, PropertyValueSets.NUMBER);
    // new UnicodeSet("[:N:]").freeze();

    private static final Map<String,String> NAME_TO_CP;
    static {
        Builder<String,String> builder = ImmutableMap.builder();
        for (EntryRange entry : NO_NAME.ranges()) {
            for (int cp = entry.codepoint; cp < entry.codepointEnd; ++cp) {
                final String name = iup.getName(UTF16.valueOf(cp), " + ");
                final String decomp = UTF16.valueOf(cp);
                builder.put(name, decomp);
                if (name.startsWith("CIRCLED NUMBER ")) {
                    final String decompHack = Normalizer3.NFKCCF.normalize(decomp);
                    final String nameHack = name.substring("CIRCLED ".length());
                    builder.put(nameHack, decompHack);
                }
            }
        }
        // add fake numbers that aren't handled with the number hack above
        // see also http://unicode.org/cldr/utility/list-unicodeset.jsp?a=[:name = / NUMBER /:]&[:scx=common:]
        builder.put("NUMBER SIXTY", "60");
        builder.put("NUMBER SEVENTY", "70");
        builder.put("NUMBER EIGHTY", "80");

        NAME_TO_CP = builder.build();
    }

    public static void main(String[] args) throws IOException {
        //        findExtraCaps();
        //        if (true) return;

        //        gatherData();
        computeTrial();
        compareTrial();
        showSimpleData();
        if (true) return;
        computeXFile();
        if (true) return;
        printData();
        //        showItemsIn(new UnicodeSet(N4M.keySet()).addAll(TRIAL.keySet()));
    }

    private static void findExtraCaps() {
        HashMap<String, UnicodeSet> nameToCp = cpToName.addInverseTo(new HashMap<String,UnicodeSet>());
        for (Entry<String, String> entry : cpToName.entrySet()) {
            String cp = entry.getKey();
            String lower = cpToSimpleLower.get(cp);
            lower = lower != null ? lower : cpToLower.get(cp);
            String name = entry.getValue();
            if (lower == null && name.contains("CAPITAL") && !name.startsWith("TAG LATIN ")) {
                String lowName = name.replace("CAPITAL", "SMALL");
                UnicodeSet other = nameToCp.get(lowName);
                if (other != null) {
                    int otherFirst = other.getRangeStart(0);
                    final String otherCp = UTF16.valueOf(otherFirst);
                    final String cpNkfccf = CldrUtility.ifNull(cpToNFKCCF.get(cp), cp);
                    final String otherCpNfkccf = CldrUtility.ifNull(cpToNFKCCF.get(otherCp), otherCp);
                    System.out.println((cpNkfccf.equals(otherCpNfkccf) ? "=" : "≠")
                            + "\t" + cp 
                            + "\t" + Utility.hex(cp, 4, " ") 
                            + "\t" + otherCp 
                            + "\t" + Utility.hex(otherCp, 4, " ") 
                            + "\t" + cpNkfccf
                            + "\t" + otherCpNfkccf
                            + "\t" + name 
                            + "\t" + cpToName.get(otherFirst));
                }
            }
        }
    }

    private static void compareTrial() throws IOException {
        try (PrintWriter out = FileUtilities.openUTF8Writer(Settings.GEN_DIR + "n4m/", "XNFKCCF.txt")) {
            UnicodeSet interest = new UnicodeSet(N4M.keySet())
            .addAll(TRIAL.keySet())
            .addAll(NFKCCF_SET)
            .addAll(COLLATION_MAP.keySet())
            .retainAll(new UnicodeSet(0,0x10FFFF))
            .removeAll(UNASSIGNED);

            out.println("#Code\tAge\tGC\tDT\tName\tN4M\tTrial\tNFKCCF\tUCA\tΔ\tΔ\tΔ\tN4M Hex\tTrial Hex\tNFKCCF Hex\tUCA Hex\tTrial Reasons");
            TreeSet<Row.R4<Integer, Decomposition_Type_Values, String, String>> sorted = new TreeSet<>();
            for (String s : interest) {
                String nfkccf = Normalizer3.NFKCCF.normalize(s);
                String n4m = N4M.get(s);
                if (n4m == null) {
                    n4m = s;
                }
                String trial = TRIAL.get(s);
                if (trial == null) {
                    trial = s;
                }
                String colEquiv = COLLATION_MAP.get(s);
                if (colEquiv == null) {
                    colEquiv = s;
                }
                if (Objects.equal(n4m, trial) && Objects.equal(trial, nfkccf)
//                        && Objects.equal(trial, colEquiv)
                        ) {
                    continue;
                }
                String reasons = REASONS.get(s);
                final int cp = s.codePointAt(0);
                String line = "'" + Utility.hex(s) 
                        + "\t'" + AGE.get(cp).getShortName() // UCharacter.getAge(cp).getVersionString(2, 2)
                        + "\t'" + GC.get(cp).getNames().getShortName()
                        + "\t'" + DT.get(cp).getNames().getShortName()
                        + "\t" + getName(s) 
                        + "\t'" + showEmpty(n4m)
                        + "\t'" + showEmpty(trial)
                        + "\t'" + showEmpty(nfkccf)
                        + "\t'" + showEmpty(colEquiv)
                        + "\t" + (Objects.equal(n4m, trial) ? "" : "Tr≠N4M") 
                        + "\t" + (Objects.equal(trial,nfkccf) ? "" : "Tr≠NF") 
                        + "\t" + (Objects.equal(trial, colEquiv) ? "" : "Tr≠UCA")
                        + "\t'" + Utility.hex(n4m, 4, " ")
                        + "\t'" + Utility.hex(trial, 4, " ")
                        + "\t'" + Utility.hex(nfkccf, 4, " ")
                        + "\t'" + Utility.hex(colEquiv, 4, " ")
                        + "\t" + (reasons == null ? "" : reasons);
                Row.R4<Integer, Decomposition_Type_Values, String, String> row = Row.of(
                        100-GC.get(cp).ordinal(), 
                        DT.get(cp), 
                        (Objects.equal(n4m, trial) ? "a" : "b") +  
                        (Objects.equal(trial,nfkccf) ? "a" : "b") + 
                        (Objects.equal(trial, colEquiv) ? "a" : "b"),
                        line);
                sorted.add(row);
            }
            for (Row.R4<Integer, Decomposition_Type_Values, String, String> row : sorted) {
                out.println(row.get3());
            }
        }
    }

    private static String getName(String s) {
        String result = iup.getName(s, " + ");
        return result == null ? "n/a" : result;
    }

    private static String showEmpty(String source) {
        // TODO Auto-generated method stub
        return source.isEmpty() ? "∅" : source;
    }

    private static final Splitter SPACE_SPLITTER = Splitter.on(' ').trimResults();

    private static final UnicodeMap<String> X_FILE = new UnicodeMap<String>();
    private static final String TEST_NAME_START = "NEGATIVE CIRCLED NUMBER";
    private static final boolean SIMPLE = true;


    private static void computeXFile() {
        for (Entry<String, String> entry : NAME_TO_CP.entrySet()) {
            final String name = entry.getKey();
            final String cp = entry.getValue();

            if (TEST_NAME_START != null && name.startsWith(TEST_NAME_START)) {
                int debug = 0;
            }
            removeString(name, cp, false, " FINAL ");
            removeString(name, cp, false, " WIDE FINAL ");
            removeString(name, cp, false, "HALFWIDTH ");
            removeString(name, cp, true, "CIRCLED ");
            removeString(name, cp, true, "SQUARED ");
            removeString(name, cp, false, "NEGATIVE CIRCLED ");
            removeString(name, cp, false, "DINGBAT CIRCLED SANS-SERIF ");
            removeString(name, cp, false, "DINGBAT NEGATIVE CIRCLED ");
            removeString(name, cp, false, "DINGBAT NEGATIVE CIRCLED SANS-SERIF ");
            removeString(name, cp, true, "NEGATIVE SQUARED ");
            removeString(name, cp, false, "CROSSED NEGATIVE SQUARED ");
            removeString(name, cp, false, "CIRCLED ", " ON BLACK SQUARE");
            removeString(name, cp, false, "DOUBLE CIRCLED ");

        }
        X_FILE.freeze();

        Counter<Status> total = new Counter<>();
        UnicodeSet items = new UnicodeSet(X_FILE.keySet())
        .addAll(ADDITIONS_TO_NFKCCF.getSourceToTarget().keySet())
        .freeze();

        System.out.println("#Source\tNew Vers.\tOld Vers.\tGC\tSrc\tNew\tOld\tSource\tNew\tOld\tStatus");
        for (Status checkStatus : Status.values()) {
            for (String source : items) {
                String oldTarget = ADDITIONS_TO_NFKCCF.getSourceToTarget().get(source);

                if (SIMPLE) {
                    String nfkccf2 = Normalizer3.NFKCCF.normalize(source);
                    oldTarget = oldTarget == null ? "" : oldTarget;
                    if (nfkccf2.equals(oldTarget)) {
                        continue;
                    }
                    System.out.println(
                            Utility.hex(source)
                            + ";\t" + (oldTarget.isEmpty() ? "" : Utility.hex(oldTarget, 4, " ", true, new StringBuilder()))
                            + "#\t" + source 
                            + " →\t" + oldTarget 
                            + ",\t" + iup.getName(source, ", ") 
                            + " →\t" + iup.getName(oldTarget, ", ") 
                            );
                    continue;
                }
                String newTarget = X_FILE.get(source);
                final Status status = Status.get(source, oldTarget, newTarget);
                if (status != checkStatus) continue;
                total.add(status, 1);

                System.out.println(
                        Utility.hex(source)
                        + ";\t" + (newTarget == null ? "source" : Utility.hex(newTarget, 4, " "))
                        + ";\t" + (oldTarget == null ? "source" : Utility.hex(oldTarget, 4, " "))
                        + ";\t" + GC.get(source).getNames().getShortName()
                        + ";\t" + source 
                        + ";\t" + newTarget 
                        + ";\t" + oldTarget 
                        + ";\t" + iup.getName(source, ", ") 
                        + ";\t" + (newTarget == null ? "source" : iup.getName(newTarget, ", "))
                        + ";\t" + (oldTarget == null ? "source" : iup.getName(oldTarget, ", "))
                        + ";\t" + status
                        );
            }
        }
        System.out.println(total);
    }

    enum Status {
        different, missing, extra, same;
        static Status get(String source, String oldTarget, String newTarget) {
            return oldTarget == null ? Status.extra
                    : newTarget == null ? Status.missing
                            : oldTarget.equals(newTarget) ? Status.same
                                    : Status.different;
        }
    }

    private static void removeString(final String name, String cp, boolean hack, final String... stringsToFind) {
        int finalPos = name.indexOf(stringsToFind[0]);
        if (finalPos >= 0) {
            String newName = name;
            for (String s : stringsToFind) {
                newName = newName.replace(s, " ").trim().replace("  ", " ");
            }
            String otherCode = NAME_TO_CP.get(newName);
            if (otherCode != null) {
                final String target = HANGUL_COMPAT_minus_DI_CN.contains(otherCode) ? otherCode : Normalizer3.NFKCCF.normalize(otherCode);
                if (!Normalizer3.NFKCCF.normalize(cp).equals(target)) {
                    X_FILE.put(cp, target);
                }
            } else {
                // hack for SQUARED OK, NEGATIVE SQUARED IC
                if (hack && !newName.contains(" ")) {
                    X_FILE.put(cp, newName.toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    private static void showSimpleData() throws IOException {
        try (PrintWriter out = FileUtilities.openUTF8Writer(Settings.GEN_DIR + "n4m/", "XNFKCCF-delta.txt")) {
            out.println("# Result of using algorithm + curated values."
                    + "\n# To get the full mapping, you need NFKC_CF; these are just overrides of that data.");
            out.println("# Source; \tTarget; \tReason(s); \tComments");
            UnicodeSet trialWithoutReason = new UnicodeSet(TRIAL.keySet()).removeAll(REASONS.keySet());
            if (!trialWithoutReason.isEmpty()) {
                System.out.println("Unexplained difference between TRIAL and REASONS: " + trialWithoutReason);
            }
            TreeSet<String> sorted = new TreeSet<>();
            sorted.addAll(REASONS.values());
            for (String reason : sorted) {
                UnicodeSet set = REASONS.getSet(reason);
                boolean first = true;
                for (String source : set) {
                    String target = TRIAL.get(source);
                    if (target == null) {
                        target = source;
                    }
                    String plainNfkccf = Normalizer3.NFKCCF.normalize(source);
                    if (plainNfkccf.equals(target)) {
                        continue;
                    }
                    if (first) {
                        out.println("\n#" + reason + "\n");
                        first = false;
                    }
                    out.println(Utility.hex(source) 
                            + ";\t" + Utility.hex(target, 4, " ") 
                            + ";\t" + reason
                            + "\t # ( " + source + " → " + target + " )\t"
                            + getName(source) + " → " + getName(target));
                }
            }
        }
        //        for (Entry<String, String> entry : TRIAL.entrySet()) {
        //            final String source = entry.getKey();
        //            final String target = entry.getValue();
        //            String plainNfkccf = nfkccf.normalize(source);
        //            if (plainNfkccf.equals(target)) {
        //                continue;
        //            }
        //            String reason = REASONS.get(source);
        //            if (source.contains(DEBUG_PRINT)) {
        //                int debug = 0;
        //            }
        //            System.out.println(Utility.hex(source) + ";\t" + Utility.hex(target, 4, " ") 
        //                    + "\t # ( " + source + " → " + target + " )\t"
        //                    + UCharacter.getName(source, " + ") + " → " + UCharacter.getName(target, " + ")
        //                    + "\t" + reason);
        //        }
    }

    private static void printData() {
        showMapping(ADDITIONS_TO_NFKCCF, Normalizer3.NFKCCF);
    }

    /**
     * Here we try to reverse engineer the derivation, starting with NFKCCasefold
     */
    private static void computeTrial() {

        UnicodeSet skipIfInMultiCodepointDecomp = new UnicodeSet("[\\u0020<>]");

        UnicodeMap<String> toSuper = new UnicodeMap<>();
        for (String s : DT.getSet(Decomposition_Type_Values.Super)) { // new UnicodeSet("[:dt=Super:]")
            String normal = Normalizer3.NFKCCF.normalize(s);
            if (Nd.contains(normal)) {
                toSuper.put(normal, s);
            }
        }
        toSuper.freeze();

        final char SEPARATOR = ' ';

        main:
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                if (cp==0xA7B6) {
                    int debug = 0;
                }
                String source = UTF16.valueOf(cp);
                String nfkccf = Normalizer3.NFKCCF.normalize(source);


                // Unassigned or strange Cx → no change

                if (CN_CS_CO.contains(cp)) { 
                    continue main;
                }

                String target = source;
                String reason = "??";

                subloop: {
                    if (HANGUL_COMPAT_minus_DI_CN.contains(cp)) {
                        reason = ("02. Skip Hangul Compat");
                        break subloop;
                    }

                    String remapped = null;
                    remapped = ADDITIONS_TO_NFKCCF.getSourceToTarget().get(source);
                    if (remapped != null) {
                        target = remapped;
                        reason = ("04+. " + ADDITIONS_TO_NFKCCF.getSourceToReason().get(source)); // also , #5, #6. #10., #11. 
                        break subloop;
                    }

                    // decomposition type = squared, fraction → Map to NFKC
                    // if the target ends with a digit, and there are no other digits, superscript the last
                    // if there is more than one cp in the target, surround by separators.
                    if (SPECIAL_DECOMP_TYPES.contains(cp)) {
                        target = Normalizer3.NFKCCF.normalize(source);
                        reason = "07. DT_SQUARE_FRACTION";
                        int lastCp = target.codePointBefore(target.length());
                        String mod = toSuper.get(lastCp);
                        if (mod != null) {
                            String prefix = target.substring(0,target.length() - Character.charCount(lastCp));
                            if (Nd.containsNone(prefix)) {
                                target = prefix + mod;
                                //                                System.out.println(Utility.hex(source) + "; " + Utility.hex(target, 4, " ") + " # " + source + " → " + target);
                                reason += " for superscript-numbers";
                            }
                        }
                        break subloop;
                    }
                    // decomposition type = super, sub → do not map, stop
                    if (NOCHANGE_DECOMP_TYPES.contains(cp)) {
                        reason = "9 skip certain types";
                        break subloop;
                    }
                    // Get NFKC_CF mapping

                    target = nfkccf;

                    // length(value) ≠1 && contains any of  " ",  "(",  ".",  ",",  "〔" → no change (discard mapping)

                    if (target.codePointCount(0, target.length()) > 1
                            && skipIfInMultiCodepointDecomp.containsSome(target)) {
                        reason = ("14 Skip decomp contains «"
                                + new UnicodeSet().addAll(target).retainAll(skipIfInMultiCodepointDecomp)
                                + "» (and isn't singleton)");
                        target=source;
                    } else if (!REASONS.containsKey(cp)) {
                        // if we don't have a reason, it is because of NFKC_CF, so add that reason.
                        reason = "16. NFKC_CF-" + DT.get(cp);
                    }
                }
                if (target.contains("⁄")) {
                    target = target.replace('⁄', '/'); // fraction slash #15
                    reason += " + fix fraction slash";
                }

                if (!target.equals("/") && target.contains("/")) {
                    target = SEPARATOR + target + SEPARATOR;
                    //System.out.println("«" + target + "»");
                    reason += " + add separator";
                }

                target = nfc.normalize(target); // just in case!!
                if (!source.equals(target)) {
                    TRIAL.put(cp, target);
                }
                REASONS.put(cp, reason);
            }
        NFKCCF_SET.freeze();
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
                        REASONS.put(source, reason + " + recursion");
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

    private static UnicodeMap<String> gatherData() {
        UnicodeMap<String> N4M = new UnicodeMap<>();
        for (String line : FileUtilities.in(dir, GOOGLE_FOLDING_TXT)) {
            String[] parts = FileUtilities.cleanSemiFields(line);
            if (parts == null) continue;
            String source = Utility.fromHex(parts[0], 1, SPACES);
            String target = parts.length < 2 ? "" : Utility.fromHex(parts[1], 0, SPACES);
            target = nfc.normalize(target); // since NFC is applied afterwards
            N4M.put(source, target);
        }
        N4M.freeze();
        return N4M;
    }

    // The following is just used to print out differences

    private static void showMapping(NormalizeForMatch sourceMap, Normalizer3 nfkccf2) {
        UnicodeSet changed = new UnicodeSet();
        System.out.println("#source ; target ; nfkccf (if ≠) ; uca equiv (if ≠) # (source→target) names");
        for (Entry<String, String> x : sourceMap.getSourceToTarget().entrySet()) {
            String source = x.getKey();
            String target = x.getValue();
            final String nfkccfResult = nfkccf2.normalize(source);
            if (target.equals(nfkccfResult)) {
                continue;
            }
            String colEquiv = COLLATION_MAP.get(source);
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

        for (Entry<String, String> x : COLLATION_MAP.entrySet()) {
            String source = x.getKey();
            if (sourceMap.getSourceToTarget().containsKey(source) || HIRAGANA.containsAll(source)) {
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
            if (DECIMAL.containsAll(source) && Nd.containsAll(target)) {
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
            b.append(iup.getName(UTF16.valueOf(cp), " + "));
        }
        return b.toString();
    }

    //    private static void showItemsIn(UnicodeSet combined) {
    //
    //        Set<Row.R5<Age, Difference, Decomposition_Type_Values, General_Category_Values, String>> sorted = new TreeSet<>();
    //        Counter<Row.R2<Age, Difference>> counter = new Counter<>();
    //        for (String source : combined) {
    //            // Skip anything ≥ Unicode 8.0
    //            int sourceCodePoint = source.codePointAt(0);
    //            final Age_Values ageValue = AGE.get(sourceCodePoint);
    //            if (ageValue.compareTo(Age_Values.V8_0) >= 0) {
    //                continue;
    //            }
    //
    //            String n4mValue = N4M.get(source);
    //            String trialValue = TRIAL.get(source);
    //            if (Objects.equal(n4mValue, trialValue)) {
    //                continue;
    //            }
    //
    //            String reason = REASONS.get(source);
    //            General_Category_Values generalCategory = GC.get(sourceCodePoint); // UCharacter.getIntPropertyValue(sourceCodePoint, UProperty.GENERAL_CATEGORY);
    //            Decomposition_Type_Values decompType = DT.get(sourceCodePoint); // int decompType = UCharacter.getIntPropertyValue(sourceCodePoint, UProperty.DECOMPOSITION_TYPE);
    //
    //            Age age = ageValue.compareTo(Age_Values.V5_1) >= 0 ? Age.from51to70
    //                    : Age.before51;
    //
    //            Difference difference = n4mValue == null ? Difference.trial_only 
    //                    : trialValue == null ? Difference.n4m_only 
    //                            : Difference.different;
    //
    //            String nfkccfValue = NFKCCF.normalize(source);
    //            if (nfkccfValue.equals(source)) {
    //                nfkccfValue = null; // below, null means no change
    //            }
    //            final R5<Age, Difference, Decomposition_Type_Values, General_Category_Values, String> row 
    //            = Row.of(age, difference, decompType, generalCategory, ageValue.getShortName()
    //                    // ageValue.getVersionString(2, 2)
    //                    + SEP + source
    //                    + SEP + hex(source) 
    //                    + SEP + hex(n4mValue)
    //                    + SEP + hex(trialValue)
    //                    + SEP + (Objects.equal(nfkccfValue,trialValue) ? "≣" : hex(nfkccfValue))
    //                    + SEP + (reason == null ? "" : reason)
    //                    + SEP + iup.getName(UTF16.valueOf(sourceCodePoint), " + ")
    //                    );
    //            sorted.add(row);
    //            counter.add(Row.of(age,difference), 1);
    //        }
    //
    //        Age lastAge = null;
    //        Difference lastDifference = null;
    //        System.out.println("#AgeCat" 
    //                + SEP + "Type of difference" 
    //                + SEP + "Decomp type" 
    //                + SEP + "General Category" 
    //                + SEP + "Version"
    //                + SEP + "Source"
    //                + SEP + "Hex"
    //                + SEP + "N4M"
    //                + SEP + "Trial"
    //                + SEP + "NFKC_CF"
    //                + SEP + "Reason for Trial≠NFKC_CF"
    //                + SEP + "Name of Source"
    //                );
    //
    //        for (R5<Age, Difference, Decomposition_Type_Values, General_Category_Values, String> item : sorted) {
    //            final Age age = item.get0();
    //            final Difference difference = item.get1();
    //            final String decompType = item.get2().name();
    //            final String cat = item.get3().name();
    //            final String info = item.get4();
    //            if (age != lastAge || difference != lastDifference) {
    //                System.out.println("\n#" + age + ", " + difference + "\n");
    //                lastAge = age;
    //                lastDifference = difference;
    //            }
    //            System.out.println(age + SEP + difference + SEP + decompType + SEP + cat + SEP + info);
    //        }
    //        System.out.println();
    //        for (R2<Age, Difference> key : counter.getKeysetSortedByKey()) {
    //            System.out.println(key + "\t" + counter.get(key));
    //        }
    //    }

    private static String hex(String n4mValue) {
        return n4mValue == null ? "<unchanged>" : n4mValue.isEmpty() 
                ? "delete" : "U+" + Utility.hex(n4mValue,4,", U+");
    }
}
