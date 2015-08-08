package org.unicode.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
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
import org.unicode.text.UCD.IdentifierInfo.IdentifierStatus;
import org.unicode.text.UCD.IdentifierInfo.IdentifierType;
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
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;


public class TestSecurity extends TestFmwkPlus {
    private static final String SECURITY_PUBLIC = Settings.UNICODE_DRAFT_PUBLIC + "security/";
    private static final String SECURITY = Settings.UNICODETOOLS_DIRECTORY + "data/security/";

    public static void main(String[] args) {
        new TestSecurity().run(args);
    }

    static XIDModifications XIDMOD = new XIDModifications(SECURITY_PUBLIC + Settings.latestVersion);

    public static final Confusables CONFUSABLES = new Confusables(SECURITY_PUBLIC + Settings.latestVersion);

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
        for (Entry<String, EnumMap<Style, String>> entry : CONFUSABLES.getChar2data().entrySet()) {
            String code = entry.getKey();
            EnumMap<Style, String> map = entry.getValue();
            for (Entry<Style, String> codeToValue : map.entrySet()) {
                Style style = codeToValue.getKey();
                boolean warningOnly = style != Style.MA;

                UnicodeMap<String> transformMap = CONFUSABLES.getStyle2map().get(style);
                String value = codeToValue.getValue();
                String value2 = transformMap.transform(value);
                if (!value2.equals(value)) {
                    final String message = style
                            + "\tU+" + Utility.hex(code)+ " ( " + code + " ) "  + Default.ucd().getName(code)
                            + "\t\texpect:\tU+" + Utility.hex(value2, "U+") + " ( " + value2 + " ) " + Default.ucd().getName(value2)
                            + "\t\tactual:\tU+" + Utility.hex(value, "U+") + " ( " + value + " ) " + " " + Default.ucd().getName(value)
                            ;
                    if (warningOnly) {
                        warnln(message);
                    } else { 
                        errln(message); 
                    }
                }
            }
        }
    }

    public void TestConfusables() {
        Confusables confusablesOld = new Confusables(SECURITY + Settings.lastVersion);
        showDiff("Confusable", confusablesOld.getStyle2map().get(Style.MA), CONFUSABLES.getStyle2map().get(Style.MA), new UTF16.StringComparator(), getLogPrintWriter());
    }

    public void TestXidMod() {
        XIDModifications xidModOld = new XIDModifications(SECURITY + Settings.lastVersion);
        UnicodeMap<IdentifierStatus> newStatus = XIDMOD.getStatus();
        showDiff("Status", xidModOld.getStatus(), newStatus, new EnumComparator<IdentifierStatus>(), getLogPrintWriter());
        showDiff("Type", xidModOld.getType(), XIDMOD.getType(), new EnumComparator<IdentifierType>(), getLogPrintWriter());

        UnicodeSet newRecommended = newStatus.getSet(IdentifierStatus.allowed);
        UnicodeSet oldRecommended = xidModOld.getStatus().getSet(IdentifierStatus.allowed);

        UnicodeSet itemsToCheck = new UnicodeSet(newRecommended).removeAll(oldRecommended);
        System.out.println(itemsToCheck);

        // if it is a new character in an old modern script, mark provisionally as uncommon use
        UCD ucd = Default.ucd();
        UCD oldUcd = UCD.make(Settings.lastVersion);
        System.out.println(ucd.getVersion());

        for (String s : ScriptMetadata.getScripts()) {
            if (s.equals("Jpan") || s.equals("Kore") || s.equals("Hans") || s.equals("Hant")) {
                continue;
            }
            Info info = ScriptMetadata.getInfo(s);
            if (info.idUsage == IdUsage.RECOMMENDED) {
                //System.out.println(s + "\t" + info);
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
                    System.out.println(all + "; uncommon-use");
                } else {
                    for (String i : all) {
                        if (first) {
                            System.out.println("# " + s);
                            first = false;
                        }
                        System.out.println(Utility.hex(i) + "; uncommon-use" + " # " + ucd.getName(i));
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
        UnicodeMap<Set<String>> fromCLDR = getCLDRCharacters();
        UnicodeSet vi = new UnicodeSet();
        for (String s : fromCLDR) {
            if (s.equals("ə")) {
                System.out.println("ə" + fromCLDR.get('ə'));
            }
            IdentifierType item = XIDMOD.getType().get(s);
            switch (item) {
            case obsolete:
            case technical:
            case uncommon_use:
                IdUsage idUsage = getScriptUsage(s.codePointAt(0));
                Set<String> locales = fromCLDR.get(s);
                System.out.println("?Overriding with CLDR: "
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
        System.out.println("vi: " + vi.toPattern(false));
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
            CLDRFile cldrFile = factory.make(localeId, false);
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
        for (Script_Values source : Script_Values.values()) {
            for (Script_Values target : Script_Values.values()) {
                CodepointToConfusables cpToConfusables = CONFUSABLES.getCharsToConfusables(source, target);
                if (cpToConfusables == null) {
                    continue;
                }
                logln(source + " => " + target);
                for (Entry<Integer, UnicodeSet> s : cpToConfusables) {
                    assertFalse(Utility.hex(s.getKey()), s.getValue().contains(s.getKey()));
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
                {"ցօօց1℮ー", "Armenian; Hiragana, Katakana"},
                {"ー", "Hiragana, Katakana"},
                {"カー", "Katakana"},
                {"\u303Cー", "Hiragana, Katakana"},
                {"\u303C", "Han, Hiragana, Katakana"},
                {"A\u303C", "Latin; Han, Hiragana, Katakana"},
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
            assertEquals("", expected, sd.getAll());
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
        //        for (Entry<Script_Values, Map<Script_Values, UnicodeSet>> entry : CONFUSABLES.getScriptToScriptToUnicodeSet().entrySet()) {
        //            final String name1 = entry.getKey().name();
        //            for (Entry<Script_Values, UnicodeSet> entry2 : entry.getValue().entrySet()) {
        //                System.out.println(name1
        //                        + "\t" + entry2.getKey().name()
        //                        + "\t" + entry2.getValue().toPattern(false)
        //                        );
        //            }
        //        }
        UnicodeSet withConfusables = CONFUSABLES.getCharsWithConfusables();
        String list = withConfusables.toPattern(false);
        UnicodeSet commonChars = new UnicodeSet(ScriptDetector.getCharactersForScriptExtensions(ScriptDetector.COMMON_SET))
        .removeAll(withConfusables)
        .removeAll(new UnicodeSet("[[:Z:][:c:]]"));
        final String commonUnconfusable = commonChars.iterator().next();
        Object[][] tests = {
                {"[:any:]"}, // anything goes


                {"google", Status.SAME},
                {"ցօօց1℮", Status.COMMON},
                {"sex", Status.SAME},
                {"scope", Status.SAME},
                {"sef", Status.SAME},
                {"1", Status.SAME},
                {"NO", Status.SAME},
                {"ー", Status.SAME}, // length mark // should be different
                {"—", Status.SAME}, // em dash
                {"コー", Status.NONE},
                {"〳ー", Status.SAME}, // should be different
                {"乙一", Status.OTHER}, // Hani
                {"㇠ー", Status.OTHER}, // Hiragana
                {"Aー", Status.NONE},
                {"カ", Status.NONE}, // KATAKANA LETTER KA // should be added
                {"⼒", Status.SAME}, // KANGXI RADICAL POWER
                {"力", Status.SAME}, // CJK UNIFIED IDEOGRAPH-529B
                {commonUnconfusable, Status.NONE}, // check that 
                {"!", Status.NONE},
                {"\u0300", Status.NONE},
                {"a\u0300", Status.SAME},
                {"ä", Status.SAME},

                {"[[:L:][:M:][:N:]-[:nfkcqc=n:]]"}, // a typical identifier set

                {"google", Status.SAME},
                {"ցօօց1℮", Status.NONE},
                {"sex", Status.OTHER},
                {"scope", Status.OTHER},
                {"sef", Status.SAME},
                {"1", Status.SAME},
                {"NO", Status.OTHER},
                {"ー", Status.OTHER},
                {"コー", Status.NONE},
                {"〳ー", Status.OTHER},
                {"乙ー", Status.NONE},
                {"Aー", Status.NONE},
                // special cases
                {"[rn]"}, // the identifier set, to check that the identifier matching flattens
                {"m", Status.SAME},
        };
        EnumMap<Script_Values, String> examples = new EnumMap<>(Script_Values.class);
        UnicodeSet includeOnly = null;
        CheckWholeScript checker = null;
        for (Object[] test : tests) {
            String source = (String) test[0];
            if (test.length < 2) {
                checker = new CheckWholeScript(source == null ? null : new UnicodeSet(source));
                logln("idSet= " + includeOnly);
                continue;
            }
            Status expected = (Status) test[1];
            Status actual = checker.hasWholeScriptConfusable(source, examples);
            assertEquals(
                    (isVerbose() ? "" : "idSet=" + includeOnly + ",")
                    + " source= " + source 
                    + ", examples= " + examples, expected.toString(), actual.toString());
        }
    }
}

