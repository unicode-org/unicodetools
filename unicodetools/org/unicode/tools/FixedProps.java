package org.unicode.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.IdUsage;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyValueSets;
import org.unicode.props.ScriptInfo;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.Decomposition_Type_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.props.VersionToAge;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.utility.UnicodeSetParser;
import org.unicode.text.utility.LengthFirstComparator;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;


public class FixedProps {
    private static final Set<Script_Values> SINGLETON_INHERITED = singleton(Script_Values.Inherited);
    private static final Set<Script_Values> SINGLETON_COMMON = singleton(Script_Values.Common);
    private static final String VERSION = "9.0.0";
    private static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(VERSION);
    private static final UnicodeMap<Age_Values> AGE = iup.loadEnum(UcdProperty.Age, Age_Values.class);
    //private static final UnicodeMap<Script_Values> sc = iup.loadEnum(UcdProperty.Script, Script_Values.class);
    private static final UnicodeMap<Set<Script_Values>> scx = iup.loadEnumSet(UcdProperty.Script_Extensions, Script_Values.class);
    private static final UnicodeMap<String> name = iup.load(UcdProperty.Name);
    private static final UnicodeMap<Binary> emoji = iup.loadEnum(UcdProperty.Emoji, Binary.class);
    private static final UnicodeMap<Decomposition_Type_Values> dt = iup.loadEnum(UcdProperty.Decomposition_Type, Decomposition_Type_Values.class);
    private static final Splitter semi = Splitter.on(';').trimResults();
    private static final Splitter hash = Splitter.on('#').trimResults();


    static final ScriptInfo IDENTIFIER_INFO = new ScriptInfo(Settings.latestVersion);

    public static final class FixedNfkd {
        private static final Normalizer nfkd = new Normalizer(Normalizer.NFKD, VERSION);
        private static final UnicodeMap<String> fixNfkd = new UnicodeMap<>();
        private static final UnicodeSet changes = new UnicodeSet();
        static {

            UnicodeSetParser hp = new UnicodeSetParser(true);
            UnicodeSet sourceRanges = new UnicodeSet();
            StringBuilder targetString = new StringBuilder();

            for (String line : FileUtilities.in(FixedProps.class, "FixedNfkd.txt")) {
                if (line.isEmpty()) {
                    continue;
                }
                List<String> parts1 = hash.splitToList(line);
                line = parts1.get(0);
                if (line.isEmpty()) {
                    continue;
                }
                List<String> parts = semi.splitToList(line);
                if (parts.size() != 2) {
                    throw new IllegalArgumentException(line);
                }
                hp.parse(parts.get(0), sourceRanges);
                final String result = hp.parseString(parts.get(1), targetString).toString();
                fixNfkd.putAll(sourceRanges, result);
                changes.addAll(sourceRanges);
            }
            for (int i = 0; i < 0x10FFFF; ++i) {
                if (!nfkd.isNormalized(i)) {
                    changes.add(i);
                }
            }
            changes.freeze();
            fixNfkd.freeze();
        }
        public static String normalize(String source) {
            return fixNfkd.transform(nfkd.normalize(source));
        }
        public static boolean isNormalized(int source) {
            return !changes.contains(source);
        }
    }

    public static final class FixedGeneralCategory {
        static final UnicodeMap<General_Category_Values> generalCategoryRev = new UnicodeMap<>();
        private static final UnicodeSet changes = new UnicodeSet();
        static {
            final UnicodeMap<General_Category_Values> gc = iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
            generalCategoryRev.putAll(gc);
            EnumSet<General_Category_Values> temp = EnumSet.noneOf(General_Category_Values.class);
            UnicodeSet allButOther = new UnicodeSet(0,0x10FFFF);
            for (General_Category_Values value : PropertyValueSets.CONTROL) {
                allButOther.removeAll(gc.getSet(value));
            }
            for (String s : allButOther) {
                if (s.equals("🉁")) {
                    int debug = 0;
                }
                String nfkded = FixedNfkd.normalize(s);
                if (nfkded.equals(s)) {
                    continue;
                }
                // 〔S〕
                EnumSet<General_Category_Values> gcSet = getSetValues(nfkded, generalCategoryRev, temp);
                if (gcSet.size() == 1) {
                    General_Category_Values first = gcSet.iterator().next();
                    if (first != generalCategoryRev.get(s)) {
                        generalCategoryRev.put(s, first);
                        changes.add(s);
                    }
                }
            }
            generalCategoryRev.freeze();
            changes.freeze();
        }
        public static General_Category_Values get(String source) {
            return generalCategoryRev.get(source);
        }
        public static General_Category_Values get(int source) {
            return generalCategoryRev.get(source);
        }
        public static UnicodeSet getSet(General_Category_Values gcv) {
            return generalCategoryRev.getSet(gcv);
        }
        public static UnicodeSet getSet(Collection<General_Category_Values> gcv) {
            UnicodeSet punctuation = new UnicodeSet();
            for (General_Category_Values v : gcv) {
                punctuation.addAll(FixedProps.FixedGeneralCategory.getSet(v));
            }
            return punctuation.freeze();
        }
    }

    public static final class FixedScriptExceptions {
        static final UnicodeMap<Set<Script_Values>> scriptRev = new UnicodeMap<>();
        static final UnicodeMap<String> reasons = new UnicodeMap<>();
        static {
            Splitter semi = Splitter.on(';').trimResults();
            scriptRev.putAll(scx);

            // add mixed cases
            UnicodeSet inheritedAndCommon = new UnicodeSet(scx.getSet(SINGLETON_COMMON))
            .addAll(scx.getSet(SINGLETON_INHERITED))
            .freeze();
            Normalizer nfkd = new Normalizer(Normalizer.NFKD, VERSION);
            EnumSet<General_Category_Values> temp = EnumSet.noneOf(General_Category_Values.class);

            for (String s : inheritedAndCommon) {
                if (s.equals("🉁")) {
                    int debug = 0;
                }
                String nfkded = nfkd.normalize(s);
                nfkded = FixedNfkd.normalize(nfkded);
                if (nfkded.equals(s)) {
                    continue;
                }
                // 〔S〕
                IDENTIFIER_INFO.setIdentifier(nfkded);
                Set<Script_Values> scripts = IDENTIFIER_INFO.getScripts();
                Set<Set<Script_Values>> altern = IDENTIFIER_INFO.getAlternates();
                scripts.remove(Script_Values.Common);
                scripts.remove(Script_Values.Inherited);
                if (altern.size() == 0 
                        && scripts.size() == 1 
                        && !scripts.equals(scriptRev.get(s))) {
                    scriptRev.put(s, singleton(scripts));
                    reasons.put(s, "ScriptDecomp");
                }
            }

            for (String line : FileUtilities.in(FixedProps.class, "FixedScriptSource.txt")) {
                if (line.isEmpty()) {
                    continue;
                }
                List<String> parts = hash.splitToList(line);
                line = parts.get(0);
                if (parts.get(0).isEmpty()) {
                    continue;
                }
                parts = semi.splitToList(parts.get(0));
                if (parts.size() != 2) {
                    throw new IllegalArgumentException(line);
                }
                String cp = Utility.fromHex(parts.get(0));
                final String scriptCodeName = parts.get(1);
                Script_Values scriptValue = Script_Values.forName(scriptCodeName);
                Set<Script_Values> old = scriptRev.get(cp);
                
                if (!old.equals(SINGLETON_COMMON) 
                        && !old.equals(SINGLETON_INHERITED)) {
                    continue;
                }
                final Set<Script_Values> scriptSingleton = singleton(scriptValue);
                if (!old.equals(scriptSingleton)) {
                    reasons.put(cp, "Exception");
                    scriptRev.put(cp, scriptSingleton);
                }
            }
            // scriptRev.putAll(new UnicodeSet("[µ𝛍𝜇𝝁𝝻𝞵]"), singleton(Script_Values.Greek));
            // scriptRev.putAll(new UnicodeSet("[฿]"), singleton("Zsym"));
            scriptRev.freeze();
            reasons.freeze();
        }
        public static Set<Script_Values> get(String source) {
            return scriptRev.get(source);
        }
        public static Set<Script_Values> get(int source) {
            return scriptRev.get(source);
        }
        public static UnicodeSet getSet(Set<Script_Values> values) {
            return scriptRev.getSet(values);
        }
        public static Collection<Set<Script_Values>> values() {
            return scriptRev.values();
        }
    }



    private static Set<Script_Values> singleton(Script_Values script_Values) {
        return Collections.singleton(script_Values);
    }

    private static <T, U extends Collection<T>> U getSetValues(String source, UnicodeMap<T> map, U target) {
        target.clear();
        for (int cp : CharSequences.codePoints(source)) {
            T result = map.get(cp);
            if (result != null) {
                target.add(result);
            }
        }
        return target;
    }

    private static Set<Script_Values> singleton(Set<Script_Values> script_Values) {
        if (script_Values.size() != 1) {
            throw new IllegalArgumentException();
        }
        return Collections.singleton(script_Values.iterator().next());
    }

    private static Set<Script_Values> singleton(String script_Values) {
        return Collections.singleton(getScript(script_Values));
    }

    private static Script_Values getScript(final String scriptCode) {
        Script_Values scriptValue = Script_Values.forName(scriptCode);
        if (scriptValue == null) {
            switch (scriptCode) {
            case "Punc": scriptValue = Script_Values.Japanese; break;
            case "Symm": scriptValue = Script_Values.Math_Symbols; break;
            case "Syme": scriptValue = Script_Values.Emoji_Symbols; break;
            case "Symo": scriptValue = Script_Values.Other_Symbols; break;
            default: throw new IllegalArgumentException();
            }
        }
        return scriptValue;
    }
    private static String show(Script_Values s) {
        switch (s) {
        case Japanese: return "Punc";
        //        case Korean: return "Zmth";
        //        case Han_with_Bopomofo: return "Zsye";
        //        case Katakana_Or_Hiragana: return "Zsym";
        default: return s.getShortName();
        }
    }

    private static String showLong(Script_Values s) {
        switch (s) {
        case Japanese: return "Punctuation";
        //        case Korean: return "Math Symbol";
        //        case Han_with_Bopomofo: return "Emoji";
        //        case Katakana_Or_Hiragana: return "Other Symbol";
        default: return s.toString();
        }
    }

    private static String show(Set<Script_Values> scripts) {
        StringBuilder sb = new StringBuilder();
        for (Script_Values s : scripts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(show(s));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println("\n# Fixed Nfkd.\n");
        {
            final Normalizer nfkd = new Normalizer(Normalizer.NFKD, VERSION);
            UnicodeMap<String> diff = new UnicodeMap<>();
            for (String s : FixedNfkd.changes) {
                String newValue = FixedNfkd.normalize(s);
                if (!nfkd.normalize(s).equals(newValue)) {
                    diff.put(s, newValue);
                }
            }
            showMap(diff);
        }
        
        System.out.println("\n# Fixed GC.\n");
        {
            final UnicodeMap<General_Category_Values> gc = iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
            UnicodeMap<String> diff = new UnicodeMap<>();
            for (String s : FixedGeneralCategory.changes) {
                final General_Category_Values oldValue = gc.get(s);
                final General_Category_Values newValue = FixedGeneralCategory.get(s);
                if (!newValue.equals(oldValue)) {
                    diff.put(s, oldValue.getShortName() + "; " + newValue.getShortName());
                }
            }
            showMap(diff);
        }

        System.out.println("\n# Fixed Script.\n");
        {
            final UnicodeMap<Set<Script_Values>> scx = iup.loadEnumSet(UcdProperty.Script_Extensions, Script_Values.class);
            UnicodeMap<String> diff = new UnicodeMap<>();
            for (String s : FixedScriptExceptions.reasons.keySet()) {
                final Set<Script_Values> oldValue = scx.get(s);
                final Set<Script_Values> newValue = FixedScriptExceptions.get(s);
                if (!newValue.equals(oldValue)) {
                    diff.put(s, oldValue + "; " + newValue);
                }
            }
            showMap(diff);
        }


//        System.out.println("\n# Changed by Ken's data.\n");
//        showMap(FixedScriptExceptions.kensRev);

        //showOld();
        //showGrowthTable();
    }

    private static void showOld() {
        for (String value : FixedScriptExceptions.reasons.values()) {
            UnicodeSet us = FixedScriptExceptions.reasons.getSet(value);
            System.out.println(value);
            System.out.println("\t" + us.toPattern(false));
        }

//        showMap(FixedScriptExceptions.diff);

        System.out.println("\n# Remaining Common+Inherited - spaces, controls, etc.\n");
        UnicodeSet us = new UnicodeSet(FixedScriptExceptions.getSet(SINGLETON_COMMON))
        .addAll(FixedScriptExceptions.scriptRev.getSet(SINGLETON_INHERITED))
        .removeAll(FixedGeneralCategory.getSet(General_Category_Values.Space_Separator))
        .removeAll(FixedGeneralCategory.getSet(General_Category_Values.Line_Separator))
        .removeAll(FixedGeneralCategory.getSet(General_Category_Values.Paragraph_Separator))
        .removeAll(FixedGeneralCategory.getSet(General_Category_Values.Format))
        .removeAll(FixedGeneralCategory.getSet(General_Category_Values.Control))
        .removeAll(iup.loadEnum(UcdProperty.Variation_Selector, Binary.class).getSet(Binary.Yes))
        ;
        for (General_Category_Values value : General_Category_Values.values()) {
            UnicodeSet us2 = new UnicodeSet(FixedGeneralCategory.getSet(value)).retainAll(us);
            if (us2.size() != 0) {
                showSet(value.toString(), us2);
            }
        }

        System.out.println("\n# Emoji vs Zsye\n");
        UnicodeSet a = emoji.getSet(Binary.Yes);
        UnicodeSet b = FixedScriptExceptions.getSet(singleton(Script_Values.Emoji_Symbols));

        UnicodeSet[] diffs = new UnicodeSet[] {
                new UnicodeSet(a).removeAll(b), 
                new UnicodeSet(b).removeAll(a)
        };
        String[] names = {"Emoji-Zsye", "Zsye-Emoji"};
        int count = 0;
        for (UnicodeSet value : diffs) {
            if (value.size() != 0) {
                showSet(names[count++], value);
            }
        }
    }

    private static void showGrowthTable() {
        UnicodeSet us;
        Map<Script_Values, UnicodeSet> scriptToUset = new TreeMap();
        for (Set<Script_Values> scriptSet : FixedScriptExceptions.values()) {
            us = FixedScriptExceptions.getSet(scriptSet);
            for (Script_Values sv : scriptSet) {
                UnicodeSet s = scriptToUset.get(sv);
                if (s == null) {
                    scriptToUset.put(sv, s = new UnicodeSet());
                }
                s.add(us);
            }
        }
        
        System.out.print("Script\tID Usage");
        for (Age_Values av : Age_Values.values()){
            if (av == av.Unassigned) {
                continue;
            }
            System.out.print("\t" + av.getShortName());
        }
        for (Age_Values av : Age_Values.values()){
            if (av == av.Unassigned) {
                continue;
            }
            System.out.print("\t" + VersionToAge.getYear(av));
        }
        System.out.println();
        for (Entry<Script_Values, UnicodeSet> scriptEntry : scriptToUset.entrySet()) {
            UnicodeSet values = scriptEntry.getValue();
            values.freeze();
            final Script_Values script = scriptEntry.getKey();
            if (script == script.Unknown) {
                continue;
            }
            System.out.print(showLong(script) + "\t");
            Info info = ScriptMetadata.getInfo(show(script));
            switch(info == null ? IdUsage.RECOMMENDED : info.idUsage) {
            case EXCLUSION:
                System.out.print("Historic"); 
                break;
            case ASPIRATIONAL:
            case LIMITED_USE:
                System.out.print("Limited Use"); 
                break;
            case RECOMMENDED:
                System.out.print("Recommended"); 
                break;
            case UNKNOWN:
            default:  
                break;
            }

            for (Age_Values av : Age_Values.values()){
                us = AGE.getSet(av);
                UnicodeSet result = new UnicodeSet(values).retainAll(us);
                System.out.print("\t" + result.size());
            }
            System.out.println();
        }
    }

    private static void showMapSorted(UnicodeMap<String> unicodeMap) {
        TreeSet<String> sorted = new TreeSet<>();
        sorted.addAll(unicodeMap.values());
        for (String value : sorted) {
            UnicodeSet us = unicodeMap.getSet(value);
            showSet(value, us);
        }
    }

    private static <T> void showMap(UnicodeMap<T> unicodeMap) {
        int len = 0;
        for (String value : unicodeMap) {
            if (len < value.length()) {
                len = value.length();
            }
        }
        Tabber tabber = new Tabber.MonoTabber()
        .add(19, Tabber.LEFT)
        .add(len + 1, Tabber.LEFT)
        ;
        for (Entry<String, T> entry : unicodeMap.entrySet()) {
            final String key = entry.getKey();
            final T value = entry.getValue();
            showLine(tabber, key, value);
        }
        System.out.println("# total: " + unicodeMap.size());
        System.out.println("# uset:  " + unicodeMap.keySet().toPattern(false));
    }

    private static <T> void showLine(Tabber tabber, final String key, final T value) {
        System.out.println(tabber.process(
                "U+" + Utility.hex(key) 
                + " ;\t" + value
                + "\t # (" +  key + "→" + value + ") " 
                + name.get(key)));
    }

    private static void showSet(String value, UnicodeSet us) {
        Tabber tabber = new Tabber.MonoTabber()
        .add(19, Tabber.LEFT)
        .add(value.length() + 1, Tabber.LEFT)
        ;
        for (EntryRange range : us.ranges()) {
            if (range.codepoint == range.codepointEnd) {
                System.out.println(tabber.process(
                        "U+" + Utility.hex(range.codepoint) 
                        + " ;\t" + value
                        + "\t # (" + UTF16.valueOf(range.codepoint) + ") " 
                        + name.get(range.codepoint)));
            } else {
                System.out.println(tabber.process(
                        "U+" + Utility.hex(range.codepoint) + "..U+" + Utility.hex(range.codepointEnd)
                        + " ;\t" + value 
                        + "\t # (" + UTF16.valueOf(range.codepoint)  + ".." + UTF16.valueOf(range.codepointEnd) + ") " 
                        + name.get(range.codepoint) + ".." + name.get(range.codepointEnd)));
            }
        }
        System.out.println("# TOTAL code points:\t" + us.size() + "\n");
    }
}
