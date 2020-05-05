package org.unicode.test;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.IdUsage;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.test.CheckWholeScript.Status;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.IdentifierInfo.Identifier_Status;
import org.unicode.text.UCD.IdentifierInfo.Identifier_Type;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Names;
import org.unicode.text.tools.XIDModifications;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.Confusables;
import org.unicode.tools.Confusables.CodepointToConfusables;
import org.unicode.tools.Confusables.Style;
import org.unicode.tools.ScriptDetector;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;


public class TestSecurity extends TestFmwkPlus {
    private static final String SECURITY = Settings.UNICODE_DRAFT_PUBLIC + "security/";

    // private static final String SECURITY_PUBLIC = Settings.UNICODE_DRAFT_PUBLIC + "security/";
    public static XIDModifications XIDMOD = new XIDModifications(SECURITY + Settings.latestVersion);
    public static final Confusables CONFUSABLES = new Confusables(SECURITY + Settings.latestVersion);


    public static void main(String[] args) {
        new TestSecurity().run(args);
    }


    public void TestSpacing() {
        IndexUnicodeProperties iup = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
        UnicodeMap<General_Category_Values> generalCategory = iup.loadEnum(
                UcdProperty.General_Category, General_Category_Values.class);
        for (Entry<String, EnumMap<Style, String>> data : CONFUSABLES.getChar2data().entrySet()) {
            String source = data.getKey();
            String target = data.getValue().get(Style.MA);
            assertEquals("( " + source + " ) ? ( " + target + " )", isAllNonspacing(source, generalCategory), isAllNonspacing(target, generalCategory));
        }
    }
    private Boolean isAllNonspacing(String source, UnicodeMap<General_Category_Values> generalCategory) {
        for (int codepoint : CharSequences.codePoints(source)) {
            switch(generalCategory.get(codepoint)) {
            case Nonspacing_Mark: case Enclosing_Mark: 
                continue;
            default: 
                return false;
            }
        }
        return true;
    }

    public void TestIdempotence() {
        // Ensure that map(map(code)) = map(code)
        // Also, if map(code) = A+B (multiple characters), then map(map(code)) = map(A)+map(B)

        UnicodeMap<String> transformMap = CONFUSABLES.getRawMapToRepresentative(Style.MA);
        for (Entry<String, String> entry : transformMap.entrySet()) {
            String source = entry.getKey();
            String value = entry.getValue();
            if (source.equals(value)) {
                warnln("Not an error, but data not minimal: " + "U+" + Utility.hex(source)+ " ( " + source + " ) "  + Default.ucd().getName(source));
            }
            checkTransform(transformMap, source, value);
            if (value.codePointCount(0, value.length()) > 1) {
                for (int cp : CharSequences.codePoints(value)) {
                    String v = UTF16.valueOf(cp);
                    checkTransform(transformMap, value, v);
                }
            }
        }
    }
    private void checkTransform(UnicodeMap<String> transformMap, String source, String value) {
        String map_map = transformMap.get(value);
        if (map_map == null) {
            map_map = value;
        }
        if (!value.equals(map_map)) {
            errln("U+" + Utility.hex(source)+ " ( " + source + " ) "  + Default.ucd().getName(source)
                    + "\t\tmap(source):\tU+" + Utility.hex(value, "U+") + " ( " + value + " ) " + Default.ucd().getName(value)
                    + "\t\tmap(map(source)):\tU+" + Utility.hex(map_map, "U+") + " ( " + map_map + " ) " + " " + Default.ucd().getName(map_map));
        }
    }

    public void TestConfusables() {
        Confusables confusablesOld = new Confusables(SECURITY + Settings.lastVersion);
        showDiff("Confusable", confusablesOld.getRawMapToRepresentative(Style.MA), CONFUSABLES.getRawMapToRepresentative(Style.MA), new UTF16.StringComparator(), getLogPrintWriter());
    }

    public void TestXidMod() {
        XIDModifications xidModOld = new XIDModifications(SECURITY + Settings.lastVersion);
        UnicodeMap<Identifier_Status> newStatus = XIDMOD.getStatus();
        showDiff("Status", xidModOld.getStatus(), newStatus, new EnumComparator<Identifier_Status>(), getLogPrintWriter());
        showDiff("Type", xidModOld.getType(), XIDMOD.getType(), new EnumSetComparator<Set<Identifier_Type>>() , getLogPrintWriter());

        UnicodeSet newRecommended = newStatus.getSet(Identifier_Status.allowed);
        UnicodeSet oldRecommended = xidModOld.getStatus().getSet(Identifier_Status.allowed);

        UnicodeSet itemsToCheck = new UnicodeSet(newRecommended).removeAll(oldRecommended);
        System.out.println(itemsToCheck);

        // if it is a new character in an old modern script, mark provisionally as uncommon use
        UCD ucd = Default.ucd();
        UCD oldUcd = UCD.make(Settings.lastVersion);
        System.out.println(ucd.getVersion());

        for (String s : ScriptMetadata.getScripts()) {
            if (s.equals("Jpan") || s.equals("Kore") || s.equals("Hans") || s.equals("Hant") || s.equals("Hanb") || s.equals("Jamo")) {
                continue;
            }
            Info info = ScriptMetadata.getInfo(s);
            if (info.idUsage == IdUsage.RECOMMENDED) {
                logln(s + "\t" + info);
                short currentScriptNumber = Utility.lookupShort(s, UCD_Names.SCRIPT, true);
                boolean first = true;
                UnicodeSet all = new UnicodeSet();
                for (String ss : itemsToCheck) {
                    int i = ss.codePointAt(0);
                    if (i == 0x08B3) {
                        int debug = 0;
                    }
                    if (oldUcd.isAssigned(i)) {
                        continue;
                    }
                    short script = ucd.getScript(i);
                    if (script != currentScriptNumber) {
                        continue;
                    }
                    all.add(i);
                }
                if (s.equals("Hani")) {
                    logln(all + "; uncommon-use");
                } else {
                    for (String i : all) {
                        if (first) {
                            System.out.println("# " + s);
                            first = false;
                        }
                        logln(Utility.hex(i) + "; uncommon-use" + " # " + ucd.getName(i));
                    }
                }
            }
        }
    }

    public IdUsage getScriptUsage(int codepoint) {
        String script = Default.ucd().getScriptID(codepoint);
        Info info = ScriptMetadata.getInfo(script);
        return info.idUsage;
    }

    public void TestCldrConsistency() {
        System.out.println("\nIgnore TestCldrConsistency for now: Need to fix exemplars.closeOver(UnicodeSet.CASE) before this is useful");
        UnicodeMap<Set<String>> fromCLDR = getCLDRCharacters();
        UnicodeSet vi = new UnicodeSet();
        for (String s : fromCLDR) {
            if (s.equals("ə")) {
                logln("ə" + fromCLDR.get('ə'));
            }
            Set<Identifier_Type> itemSet = XIDMOD.getType().get(s);
            for (Identifier_Type item : itemSet) {
                switch (item) {
                case obsolete:
                case technical:
                case uncommon_use:
                    IdUsage idUsage = getScriptUsage(s.codePointAt(0));
                    Set<String> locales = fromCLDR.get(s);
                    warnln("?Overriding with CLDR: "
                            + Utility.hex(s, "+")
                            + "; " + s 
                            + "; " + UCharacter.getName(s," ") 
                            + "; " + item 
                            + " => " + idUsage + "; "
                            + locales);
                    if (locales.contains("vi")) {
                        vi.add(s);
                    }
                    break;
                default: 
                    break;
                }
            }
        }
//        System.out.println("vi: " + vi.toPattern(false));
    }

    public static UnicodeMap<Set<String>> getCLDRCharacters() {
        UnicodeMap<Set<String>> result = new UnicodeMap<>();
        Factory factory = CLDRConfig.getInstance().getCldrFactory();
        //        File[] paths = { new File(CLDRPaths.MAIN_DIRECTORY)
        //        //, new File(CLDRPaths.SEED_DIRECTORY), new File(CLDRPaths.EXEMPLARS_DIRECTORY) 
        //        };
        //        Factory factory = SimpleFactory.make(paths, ".*");
        Set<String> localeCoverage = StandardCodes.make().getLocaleCoverageLocales("cldr");
        Set<String> skipped = new LinkedHashSet<>();
        LanguageTagParser ltp = new LanguageTagParser();
        for (String localeId : localeCoverage) { //  factory.getAvailableLanguages()
            ltp.set(localeId);
            if (!ltp.getRegion().isEmpty()) {
                continue;
            }
            Iso639Data.Type type = Iso639Data.getType(ltp.getLanguage());
            if (type != Iso639Data.Type.Living) {
                skipped.add(localeId);
                continue;
            }
            CLDRFile cldrFile;
            try {
                cldrFile = factory.make(localeId, false);
            } catch (Exception e) {
                if (!localeId.equals("jv")) { // temporary hack
                    throw e;
                }
                System.err.println("Couldn't open: " + localeId);
                continue;
            }
            UnicodeSet exemplars = cldrFile
                    .getExemplarSet("", WinningChoice.WINNING);
            if (exemplars != null) {
                exemplars.closeOver(UnicodeSet.CASE);
                for (String s : flatten(exemplars)) { // flatten
                    Set<String> old = result.get(s);
                    if (old == null) {
                        result.put(s, Collections.singleton(localeId));
                    } else {
                        old = new TreeSet<String>(old);
                        old.add(localeId);
                        result.put(s, Collections.unmodifiableSet(old));
                    }
                }
            }
        }
        System.out.println("Skipped non-living languages " + skipped);
        return result;
    }

    private static UnicodeSet flatten(UnicodeSet result) {
        UnicodeSet result2 = new UnicodeSet();
        for (String s : result) { // flatten
            result2.addAll(s);
        }
        return result2;
    }



    public class EnumComparator<T extends Enum<?>> implements Comparator<T> {
        @Override
        public int compare(T o1, T o2) {
            return o1.ordinal() - o2.ordinal();
        }
    }

    public class EnumSetComparator<T extends Set<Identifier_Type>> implements Comparator<T> {
        @Override
        public int compare(T o1, T o2) {
            Iterator<Identifier_Type> it1 = o1.iterator();
            Iterator<Identifier_Type> it2 = o2.iterator();
            while (it1.hasNext() && it2.hasNext()) {
                Enum<?> item1 = it1.next();
                Enum<?> item2 = it2.next();
                int diff = item1.ordinal() - item2.ordinal();
                if (diff != 0) {
                    return diff;
                }
            }
            if (it1.hasNext()) return 1;
            if (it2.hasNext()) return -1;
            return 0;
        }
    }

    private <T> void showDiff(String title, UnicodeMap<T> mapOld, UnicodeMap<T> mapNew, Comparator<T> comparator, Appendable output) {
        int diffCount;
        try {
            diffCount = 0;
            TreeMap<T, Map<T, UnicodeSet>> diff = new TreeMap<T,Map<T,UnicodeSet>>(comparator);
            for (int i = 0; i <= 0x10FFFF; ++i) {
                T vOld = mapOld.get(i);
                T vNew = mapNew.get(i);
                if (!Objects.equal(vOld, vNew)) {
                    Map<T, UnicodeSet> submap = diff.get(vOld);
                    if (submap == null) {
                        diff.put(vOld, submap = new TreeMap<T,UnicodeSet>(comparator));
                    }
                    UnicodeSet us = submap.get(vNew);
                    if (us == null) {
                        submap.put(vNew, us = new UnicodeSet());
                    }
                    us.add(i);
                    diffCount++;
                }
            }
            for (Entry<T, Map<T, UnicodeSet>> value1 : diff.entrySet()) {
                T vOld = value1.getKey();
                for (Entry<T, UnicodeSet> value2 : value1.getValue().entrySet()) {
                    T vNew = value2.getKey();
                    UnicodeSet intersection = value2.getValue();
                    output.append(title + "\t«" + vOld + "» => «" + vNew + "»:\t" + intersection.size() + "\t" + intersection.toPattern(false) + "\n");
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        //        Collection<T> oldValues = mapOld.values();
        //        Collection<T> newValues = mapNew.values();

        //        for (T oldValue : oldValues) {
        //            for (T newValue : newValues) {
        //                if (newValue.equals(oldValue)) {
        //                    continue;
        //                }
        //                UnicodeSet oldChars = mapOld.getSet(oldValue);
        //                UnicodeSet newChars = mapNew.getSet(newValue);
        //                if (oldChars.containsSome(newChars)) {
        //                    UnicodeSet intersection = new UnicodeSet(oldChars).retainAll(newChars);
        //                    diffCount += intersection.size();
        //                    System.out.println(title + "\t«" + oldValue + "» => «" + newValue + "»:\t" + intersection.size() + "\t" + intersection.toPattern(false));
        //                }
        //            }
        //        }
        System.out.println(title + " total differences: " + diffCount);
        //        LinkedHashSet<T> values = new LinkedHashSet<>(mapNew.values());
        //        values.addAll(mapOld.values());
        //        for (T value : values) {
        //            UnicodeSet oldChars = mapOld.getSet(value);
        //            UnicodeSet newChars = mapNew.getSet(value);
        //            if (oldChars != newChars) {
        //                System.out.println()
        //            }
        //        }
    }

    //    private <K, V, U extends Collection<Row.R2<K,V>>> U addAllTo(Iterable<Entry<K,V>> source, U target) {
    //        for (Entry<K,V> t : source) {
    //            target.add(Row.of(t.getKey(), t.getValue()));
    //        }
    //        return target;
    //    }

    public void TestWholeScriptData() {
        System.out.println("\nIgnore TestWholeScriptData for now: Not yet compete");
        for (Script_Values source : Script_Values.values()) {
            for (Script_Values target : Script_Values.values()) {
                CodepointToConfusables cpToConfusables = CONFUSABLES.getCharsToConfusables(source, target);
                if (cpToConfusables == null) {
                    continue;
                }
                UnicodeSet failures = new UnicodeSet();
                for (Entry<Integer, UnicodeSet> s : cpToConfusables) {
                    if (s.getValue().contains(s.getKey())) {
                        failures.add(s.getKey());
                    }
                }
                if (!failures.isEmpty()) {
                    assertEquals(source + " => " + target, UnicodeSet.EMPTY, failures);
                }
            }
        }
    }

    public void TestScriptDetection() {
        ScriptDetector sd = new ScriptDetector();
        Set<Set<Script_Values>> expected = new HashSet<>();
        String[][] tests = {
                {"℮", "Common"},
                {"ցօօց1℮", "Armenian"},
                {"ցօօց1℮ー", "Armenian; Japanese"},
                {"ー", "Japanese"},
                {"カー", "Japanese"},
                {"\u303C", "Han, Korean, Japanese"},
                {"\u303Cー", "Japanese"},
                {"\u303CA", "Latin; Han, Korean, Japanese"},
                {"\u0300", "Common"},
                {"\u0300.", "Common"},
                {"a\u0300", "Latin"},
                {"ä", "Latin"},
        };
        for (String[] test : tests) {
            String source = test[0];
            expected.clear();
            expected.addAll(parseAlternates(test[1]));
            sd.set(source);
            assertEquals(source, expected, sd.getAll());
        }
    }

    public static Set<Script_Values> parseScripts(String scriptsString) {
        Set<Script_Values> result = EnumSet.noneOf(Script_Values.class);
        for (String item : COMMA.split(scriptsString)) {
            result.add(Script_Values.valueOf(item));
        }
        return result;
    }

    static final Splitter SEMI = Splitter.on(';').trimResults().omitEmptyStrings();
    static final Splitter COMMA = Splitter.on(',').trimResults().omitEmptyStrings();

    public static Set<Set<Script_Values>> parseAlternates(String scriptsSetString) {
        Set<Set<Script_Values>> result = new HashSet<>();
        for (String item : SEMI.split(scriptsSetString)) {
            result.add(parseScripts(item));
        }
        return result;
    }


    public void TestWholeScripts() {
        UnicodeSet withConfusables = CONFUSABLES.getCharsWithConfusables();
        //String list = withConfusables.toPattern(false);
        final String commonUnconfusable = getUnconfusable(withConfusables, ScriptDetector.COMMON_SET);
        final String latinUnconfusable = getUnconfusable(withConfusables, EnumSet.of(Script_Values.Latin));
        Object[][] tests = {
                {"idSet", null}, // anything goes

                {""},
                {commonUnconfusable}, // check that item with no confusables gets nothing
                {latinUnconfusable}, // check that item with no confusables gets nothing
                {"google", Status.SAME, Status.COMMON, Status.OTHER},
                {"ցօօց1℮", Status.COMMON, Status.OTHER},
                {"sex", Status.SAME, Status.COMMON, Status.OTHER},
                {"ѕех", Status.SAME, Status.COMMON, Status.OTHER}, // Cyrillic
                {"scope", Status.SAME, Status.COMMON, Status.OTHER},
                {"1", Status.SAME, Status.OTHER},
                {"NOT", Status.SAME, Status.COMMON, Status.OTHER},
                {"ー", Status.SAME, Status.COMMON, Status.OTHER}, // length mark // should be different
                {"—", Status.SAME, Status.OTHER}, // em dash
                {"コー", Status.SAME},
                {"〳ー", Status.SAME, Status.COMMON, Status.OTHER}, // should be different
                {"乙一", Status.SAME}, // Hani
                {"㇠ー", Status.SAME, Status.OTHER}, // Hiragana
                {"Aー", Status.SAME},
                {"カ", Status.SAME, Status.OTHER}, // KATAKANA LETTER KA // should be confusable with ⼒
                {"⼒", Status.SAME}, // KANGXI RADICAL POWER
                {"力", Status.SAME}, // CJK UNIFIED IDEOGRAPH-529B
                {"!", Status.SAME, Status.OTHER},
                {"\u0300", Status.SAME},
                {"a\u0300", Status.SAME, Status.COMMON, Status.OTHER},
                {"ä", Status.SAME, Status.COMMON, Status.OTHER},

                {"idSet", "[[:L:][:M:][:N:]-[:nfkcqc=n:]]"}, // a typical identifier set

                {"google", Status.SAME},
                {"ցօօց1℮", Status.OTHER},
                {"sex", Status.SAME, Status.OTHER},
                {"scope", Status.SAME, Status.OTHER},
                {"sef", Status.SAME},
                {"1", Status.OTHER},
                {"NO", Status.SAME, Status.OTHER},
                {"ー", Status.SAME, Status.OTHER},
                {"コー", Status.SAME},
                {"〳ー", Status.SAME, Status.OTHER},
                {"乙ー", Status.SAME, Status.OTHER},
                {"Aー", Status.SAME},

                // special cases
                {"idSet", "[rn]"}, // the identifier set, to check that the identifier matching flattens
                {"m", Status.SAME},
        };
        EnumMap<Script_Values, String> examples = new EnumMap<>(Script_Values.class);
        CheckWholeScript checker = null;
        Set<Status> expected = EnumSet.noneOf(Status.class);
        final ScriptDetector scriptDetector = new ScriptDetector();
        for (Object[] test : tests) {
            String source = (String) test[0];
            if (source.equals("\u0300")) {
        	int debug = 0;
            }

            if (source.equals("idSet")) {
                UnicodeSet op = test[1] == null ? null : new UnicodeSet((String) test[1]);
                checker = new CheckWholeScript(op);
                logln("idSet= " + checker.getIncludeOnly());
                continue;
            }
            expected.clear();
            for (int i = 1; i < test.length; ++i) {
                expected.add((Status) test[i]);
            }
            Set<Status> actual = checker.getConfusables(source, examples);
            if (!assertEquals(
                    (isVerbose() ? "" : "idSet=" + checker.getIncludeOnly() + ",")
                    + " source= " + source 
                    + ", scripts= " + scriptDetector.set(source).getAll(), 
                    expected.toString(), actual.toString())) {
                errln("\t\texamples= " + examples);
            } else {
                logln("\t\texamples= " + examples); 
            }
        }
    }
    private String getUnconfusable(UnicodeSet withConfusables, Set<Script_Values> scriptSet) {
        UnicodeSet commonChars = new UnicodeSet(ScriptDetector.getCharactersForScriptExtensions(scriptSet))
        .removeAll(withConfusables)
        .removeAll(new UnicodeSet("[[:Z:][:c:]]"));
        final String commonUnconfusable = commonChars.iterator().next();
        return commonUnconfusable;
    }
}

