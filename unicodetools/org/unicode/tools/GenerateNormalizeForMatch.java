package org.unicode.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CldrUtility;
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
import org.unicode.props.UnicodeRelation;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Settings;
import org.unicode.tools.NormalizeForMatch.SpecialReason;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.ibm.icu.dev.util.CollectionUtilities;
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
    private static final UnicodeMap<String> cpToNFKC = new UnicodeMap<>();
    static {
        Normalizer nfkc = Default.nfkc();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            if (!nfkc.isNormalized(i)) {
                cpToNFKC.put(i, nfkc.normalize(i));
            }
        }
        cpToNFKC.freeze();
    }
    private static final UnicodeMap<String> cpToLower = iup.load(UcdProperty.Lowercase_Mapping);
    private static final UnicodeMap<String> cpToSimpleLower = iup.load(UcdProperty.Simple_Lowercase_Mapping);
    private static final UnicodeSet UNASSIGNED = GC.getSet(General_Category_Values.Unassigned);
    private static final UnicodeMap<Decomposition_Type_Values> DT = iup.loadEnum(UcdProperty.Decomposition_Type, Decomposition_Type_Values.class);
    private static final UnicodeMap<Age_Values> AGE = iup.loadEnum(UcdProperty.Age, Age_Values.class);
    private static final UnicodeMap<Block_Values> BLOCK = iup.loadEnum(UcdProperty.Block, Block_Values.class);
    private static final UnicodeSet TAGS = BLOCK.getSet(Block_Values.Tags);

    private static final UnicodeSet NFKCCF_SET = iup.loadEnumSet(UcdProperty.Changes_When_NFKC_Casefolded, Binary.Yes);
    private static final Normalizer nfc = Default.nfc();

    // Results
    private static final UnicodeMap<String> N4M = gatherData();
    private static final UnicodeMap<String> TRIAL = new UnicodeMap<>();
    private static final UnicodeMap<String> TRIAL_BASE = new UnicodeMap<>();
    private static final UnicodeMap<Set<SpecialReason>> REASONS = new UnicodeMap<>();
    private static final UnicodeMap<Set<SpecialReason>> REASONS_BASE = new UnicodeMap<>();
    private static final NormalizeForMatch ADDITIONS_TO_NFKCCF = NormalizeForMatch.load(null, "XNFKCCF-Curated.txt", true);
    private static final NormalizeForMatch ADDITIONS_TO_NFKC = NormalizeForMatch.load(Settings.UNICODETOOLS_DIRECTORY + "data/cldr/", "NFXC-Curated.txt", true);


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
        computeTrial(Normalizer3.NFKCCF, ADDITIONS_TO_NFKCCF, TRIAL, REASONS);
        computeTrial(Normalizer3.NFKC, ADDITIONS_TO_NFKC, TRIAL_BASE, REASONS_BASE);
        compareTrial(false);
        compareTrial(true);

//        UnicodeMap<String> trialWithoutCase = new UnicodeMap<>();
//        for (String s : TRIAL.keySet()) {
//            String trial = TRIAL.get(s);
//            String lower = Default.ucd().getCase(s, UCD_Types.FULL, UCD_Types.LOWER);
//            if (!trial.equals(lower)) {
//                trialWithoutCase.put(s, trial);
//            }
//        }
//        trialWithoutCase.freeze();
        showSimpleData(TRIAL, REASONS, "XNFKCCF-NFKCCF.txt", "# Cases where XNFKCCF differs from NFKCCF.", cpToNFKCCF);
        showSimpleData(TRIAL_BASE, REASONS_BASE, "NFXC-NFKC.txt", "# Cases where NFXC differs from NFKC.", cpToNFKC);
        showSimpleData(TRIAL_BASE, REASONS_BASE, "NFXC-Curated.txt", "# Curated file of exceptions", null);

        
        showSimpleData(TRIAL, REASONS, "XNFKCCF-NFKC2.txt", "# Cases where XNFKCCF differs from NFKC.", cpToNFKC);
        
        showSimpleData(N4M, REASONS, "N4M-XNFKCCF.txt", "# Cases where N4M differs from XNFKCCF", TRIAL);
        NormalizeForMatch curated = NormalizeForMatch.load(null, "XNFKCCF-Curated.txt", true);
        
        showSimpleData(curated.getSourceToTarget(), curated.getSourceToReason(), "XNFKCCF-Curated.txt", "# Curated file of exceptions", null);
        NormalizeForMatch newCurated = NormalizeForMatch.load(Settings.DATA_DIR + "n4m/9.0.0/", "XNFKCCF-Curated.txt", true);
        checkNewCurated(curated, newCurated);
        //    private static final NormalizeForMatch ADDITIONS_TO_NFKCCF = NormalizeForMatch.load("XNFKCCF-Curated.txt");

        computeCandidateFile(Age_Values.V9_0);
        //if (true) return;
        //        printData();
        //        showItemsIn(new UnicodeSet(N4M.keySet()).addAll(TRIAL.keySet()));
    }

    private static void checkNewCurated(NormalizeForMatch curated, NormalizeForMatch newCurated) {
        UnicodeSet sources = new UnicodeSet(curated.getSourceToTarget().keySet()).addAll(newCurated.getSourceToTarget().keySet());
        int diffCount = 0;
        for (String s : sources) {
            String t1 = curated.getSourceToTarget().get(s);
            String t2 = newCurated.getSourceToTarget().get(s);
            if (!Objects.equal(t1, t2)) {
                System.out.println("Diff: " + t1 + ", " + t2);
                diffCount++;
            }
        }
        System.out.println("Total diff from old to new Curated: " + diffCount + " out of " + sources.size());
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

    private static void compareTrial(boolean ucaOnly) throws IOException {
        try (PrintWriter out = FileUtilities.openUTF8Writer(Settings.GEN_DIR + "n4m/", "XNFKCCF" + (ucaOnly ? "-comp-uca" : "-comp")
                + ".txt")) {
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
                final boolean trial_n4m_nfkccfEqual = Objects.equal(trial, n4m) && Objects.equal(trial, nfkccf);
                if (trial_n4m_nfkccfEqual && Objects.equal(trial, colEquiv)) { // all equal, we don't care
                    continue;
                }
                if (ucaOnly != trial_n4m_nfkccfEqual) {
                    continue;
                }
                String reasons = CollectionUtilities.join(REASONS.get(s), " + ");
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


    private static void computeCandidateFile(Age_Values age) throws IOException {
        UnicodeMap<String> setToCheck  = new UnicodeMap<>();
        UnicodeRelation<String> reasons = new UnicodeRelation<>();
        Matcher nameCheck = Pattern.compile(
                "FINAL|MEDIAL|INITIAL"
                        + "|WIDE|WIDTH|NARROW|CIRCLE"
                        + "|SQUARE|CUBE|CAPITAL|OVER|NEGATIVE"
                        + "|RADICAL|INPUT SYMBOL"
                        + "|PARENTHESIS|PARENTHESIZED|BRACKET").matcher("");
        for (String source : AGE.getSet(age)) {
            String nfkccf = Normalizer3.NFKCCF.normalize(source);
            if (!source.equals(nfkccf)) {
                setToCheck.put(source, nfkccf);
                reasons.add(source, "NFKCCF");
                if (nfkccf.startsWith(" ")) {
                    reasons.add(source, "space");
                }
            } else {
                String uca = COLLATION_MAP.get(source);
                if (uca != null && !source.equals(uca)) {
                    setToCheck.put(source, uca);
                    reasons.add(source, "UCA");
                }
            }
            String name = cpToName.get(source);
            if (nameCheck.reset(name).find()) {
                if (!setToCheck.containsKey(source)) {
                    setToCheck.put(source, "?");
                }
                reasons.add(source, "name");
            }
        }
        setToCheck.freeze();
        reasons.freeze();
        showSimpleData(setToCheck, reasons.asUnicodeMap(), "Candidates.txt", "", null);
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

    private static <T> void showSimpleData(UnicodeMap<String> mapping, UnicodeMap<T> reasons2, String filename, String header, 
            UnicodeMap<String> skipIfSame) throws IOException {
        try (PrintWriter out = FileUtilities.openUTF8Writer(Settings.GEN_DIR + "n4m/", filename)) {
            out.println(header);
            out.println("# Source; \tTarget; \tOther; \tReason(s); \tComments");
            UnicodeSet trialWithoutReason = new UnicodeSet(mapping.keySet()).removeAll(reasons2.keySet());
            if (!trialWithoutReason.isEmpty()) {
                throw new IllegalArgumentException("Unexplained difference between TRIAL and REASONS: " + trialWithoutReason);
            }
            final Set<T> values = reasons2.values();
            Comparator<T> comp = null;
            if (values.iterator().next() instanceof Collection) {
                comp = new CollectionUtilities.CollectionComparator();
            }
            Set<T> sorted = comp == null ? new TreeSet<>() : new TreeSet<>(comp);
            sorted.addAll(values);
            for (T reason : sorted) {
                UnicodeSet set = reasons2.getSet(reason);
                String reasons = reason instanceof Set ? CollectionUtilities.join((Set)reason, " + ") : reason.toString();

                showSimpleSet(out, set, mapping, reasons, skipIfSame);
            }
            if (skipIfSame != null) {
                showSimpleSet(out, new UnicodeSet(mapping.keySet()).complement(), mapping, "other", skipIfSame);
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

    private static <T> void showSimpleSet(PrintWriter out, UnicodeSet set, UnicodeMap<String> mapping, String reason, UnicodeMap<String> skipIfSame) {
        boolean first = true;
        for (String source : set) {
            String target = mapping.get(source);
            if (target == null) {
                target = source;
            }
            String toFilterIfSame = null;
            if (skipIfSame != null) {
                toFilterIfSame = skipIfSame.get(source);
                if (toFilterIfSame == null) {
                    toFilterIfSame = source;
                }
                if (toFilterIfSame.equals(target)) {
                    continue;
                }
            }
            if (first) {
                out.println("\n#@override reason=" + reason + "\n");
                first = false;
            }
            out.println(Utility.hex(source) 
                    + ";\t" + Utility.hex(target, 4, " ") 
                    + (skipIfSame == null ? "" : ";\t" + Utility.hex(toFilterIfSame, 4, " "))
                    + ";\t" + reason
                    + "\t # ( " + source + " → " + target + " )\t"
                    + getName(source) + " → " + getName(target));
        }
    }

    private static void printData() {
        showMapping(ADDITIONS_TO_NFKCCF, Normalizer3.NFKCCF);
    }

    /**
     * Here we try to reverse engineer the derivation, starting with NFKCCasefold
     * @param normalizer3 TODO
     * @param additions TODO
     * @param trial TODO
     * @param reasons TODO
     */
    private static void computeTrial(Normalizer3 normalizer3, NormalizeForMatch additions, UnicodeMap<String> trial, UnicodeMap<Set<SpecialReason>> reasons) {

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
        long out = System.nanoTime();
        main:
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                // Unassigned or strange Cx → no change

                if (CN_CS_CO.contains(cp)) { 
                    continue main;
                }

                if (cp==0x33AE) {
                    int debug = 0;
                }

                String source = UTF16.valueOf(cp);
                String nfkccf = normalizer3.normalize(source);
                String target = source;
                Set<SpecialReason> reason = new LinkedHashSet<>();

                subloop: {
                    if (HANGUL_COMPAT_minus_DI_CN.contains(cp)) {
                        reason.add(SpecialReason.retain_hangul);
                        break subloop;
                    }

                    String remapped = null;
                    remapped = additions.getSourceToTarget().get(source);
                    if (remapped != null) {
                        target = remapped;
                        reason.add(additions.getSourceToReason().get(source));
                        break subloop;
                    }

                    // decomposition type = squared, fraction → Map to NFKC
                    // if the target ends with a digit, and there are no other digits, superscript the last
                    // if there is more than one cp in the target, surround by separators.
                    //                    if (SPECIAL_DECOMP_TYPES.contains(cp)) {
                    //                        target = nfkccf;
                    //                        reason = "07. DT_SQUARE_FRACTION";
                    //                        int lastCp = target.codePointBefore(target.length());
                    //                        String mod = toSuper.get(lastCp);
                    //                        if (mod != null) {
                    //                            String prefix = target.substring(0,target.length() - Character.charCount(lastCp));
                    //                            if (Nd.containsNone(prefix)) {
                    //                                target = prefix + mod;
                    //                                //                                System.out.println(Utility.hex(source) + "; " + Utility.hex(target, 4, " ") + " # " + source + " → " + target);
                    //                                reason += " for superscript-numbers";
                    //                            }
                    //                        }
                    //                        break subloop;
                    //                    }

                    // decomposition type = super, sub → do not map, stop
                    if (NOCHANGE_DECOMP_TYPES.contains(cp)) {
                        reason.add(SpecialReason.forString("retain_"+DT.get(cp))); // "9 skip certain types";
                        break subloop;
                    }
                    if (TAGS.contains(cp)) {
                        reason.add(SpecialReason.retain_tags); // "9 skip certain types";
                        break subloop;
                    }
                    // Get NFKC_CF mapping

                    target = nfkccf;

                    // length(value) ≠1 && contains any of  " ",  "(",  ".",  ",",  "〔" → no change (discard mapping)

                    if (target.codePointCount(0, target.length()) > 1
                            && skipIfInMultiCodepointDecomp.containsSome(target)) {
                        reason.add(SpecialReason.retain_sequences_with_exclusions);
                        //                        ("14 Skip decomp contains «"
                        //                                + new UnicodeSet().addAll(target).retainAll(skipIfInMultiCodepointDecomp)
                        //                                + "» (and isn't singleton)");
                        target=source;
                    } else if (!reasons.containsKey(cp)) {
                        // if we don't have a reason, it is because of NFKC_CF, so add that reason.
                        reason.add(SpecialReason.forString("nfkccf_" + DT.get(cp))); // "16. NFKC_CF-" + DT.get(cp);
                    }
                }
                if (target.contains("\u2044") || target.contains("\u2215")) {
                    target = target.replace('\u2044', '/').replace('\u2215', '/'); // fraction slash #15
                    reason.add(SpecialReason.fix_slash); // " + fix fraction slash";
                }

                if (!target.equals("/") && target.contains("/")) {
                    target = SEPARATOR + target + SEPARATOR;
                    //System.out.println("«" + target + "»");
                    reason.add(SpecialReason.add_separator);
                    //reason += " + add separator";
                }

                target = nfc.normalize(target); // just in case!!
                if (!source.equals(target)) {
                    trial.put(cp, target);
                }
                reasons.put(cp, reason);
            }
        // Recurse on trial
        while (true) {
            UnicodeMap<String> delta = new UnicodeMap<String>();
            UnicodeSet removals = new UnicodeSet();
            for (Entry<String, String> entry : trial.entrySet()) {
                String source = entry.getKey();
                String oldTarget = entry.getValue();
                String newTarget = nfc.normalize(trial.transform(oldTarget));
                if (!newTarget.equals(oldTarget)) {
                    if (newTarget.equals(source)) { // just in case
                        removals.add(source);
                    } else {
                        delta.put(source, newTarget);
                        LinkedHashSet<SpecialReason> reason = new LinkedHashSet<>(reasons.get(source));
                        reason.add(SpecialReason.recursion);
                        reasons.put(source, reason);
                    }
                }
            }
            if (delta.isEmpty()) break;
            trial.putAll(delta);
            //System.out.println("# Recursion " + delta);
        }
        trial.freeze();
        reasons.freeze();
        long out2 = System.nanoTime();
        System.out.println((out2-out)/1000000000.0 + " sec");
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
