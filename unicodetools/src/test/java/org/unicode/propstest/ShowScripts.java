package org.unicode.propstest;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.IdUsage;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.Iso639Data.Type;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Settings;

public class ShowScripts {
    public static void main(String[] args) {
        IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);
        UnicodeMap<Script_Values> script = iup.loadEnum(UcdProperty.Script, Script_Values.class);
        UnicodeMap<Age_Values> age = iup.loadEnum(UcdProperty.Age, Age_Values.class);
        //        UnicodeMap<General_Category_Values> gc =
        // iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
        Map<Script_Values, Info> scriptToInfo = new HashMap<>();
        Counter<Script_Values> scriptCounter = new Counter<>();
        Map<Script_Values, Counter<Age_Values>> scriptToAges = new HashMap<>();

        for (Script_Values scriptValue : script.values()) {
            if (scriptValue == Script_Values.Unknown) {
                continue;
            }
            UnicodeSet uset = script.getSet(scriptValue);
            int size = uset.size();
            Script_Values effectiveSV = scriptValue;
            if (scriptValue == Script_Values.Braille || scriptValue == Script_Values.Inherited) {
                effectiveSV = Script_Values.Common;
            }
            for (String s : uset) {
                Age_Values ageValue = age.get(s);
                Counter<Age_Values> counterAge = scriptToAges.get(effectiveSV);
                if (counterAge == null) {
                    scriptToAges.put(effectiveSV, counterAge = new Counter<>());
                }
                counterAge.add(ageValue, 1);
            }
            scriptCounter.add(effectiveSV, size);
            if (effectiveSV == scriptValue) {
                Info info = ScriptMetadata.getInfo(scriptValue.getShortName());
                scriptToInfo.put(scriptValue, info);
            }
        }

        CLDRFile names = CLDRConfig.getInstance().getEnglish();
        SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        for (Script_Values scriptValue : scriptCounter.getKeysetSortedByCount(false)) {
            Info info = scriptToInfo.get(scriptValue);
            String code = scriptValue.getShortName();
            String likelyLanguage = info.likelyLanguage;
            PopulationData pop = sdi.getBaseLanguagePopulationData(likelyLanguage);
            IdUsage usage = info.idUsage;
            Type type = Iso639Data.getType(likelyLanguage);
            Object popStr =
                    (type == type.Extinct || type == type.Ancient || type == type.Historical)
                            ? "n/a"
                            : pop == null ? "?" : (int) pop.getPopulation();
            System.out.println(
                    code
                            + "\t"
                            + scriptValue
                            + "\t"
                            + scriptCounter.get(scriptValue)
                            + "\t"
                            + getAges(scriptToAges.get(scriptValue))
                            + "\t"
                            + name(usage)
                            + "\t"
                            + likelyLanguage
                            + "\t"
                            + names.getName(CLDRFile.LANGUAGE_NAME, likelyLanguage)
                            + "\t"
                            + type
                    // + "\t" + popStr
                    );
        }

        // oldCount(script, age, gc, scriptCounter);
    }

    private static String getAges(Counter<Age_Values> counter) {
        //        Age_Values biggest = counter.getKeysetSortedByCount(false).iterator().next();
        TreeSet<Age_Values> sorted = new TreeSet<>();
        sorted.addAll(counter.keySet());
        Age_Values first = sorted.iterator().next();
        String result = first.getShortName();
        //        if (first != biggest) {
        //            result += " (" + biggest.getShortName() + ")";
        //        }
        return result;
    }

    private static String name(IdUsage idUsage) {
        switch (idUsage) {
            default:
            case ASPIRATIONAL:
                throw new IllegalArgumentException();
            case EXCLUSION:
                return "very limited use";
            case LIMITED_USE:
                return "limited use";
            case UNKNOWN:
            case RECOMMENDED:
                return "common use";
        }
    }

    private static void oldCount(
            UnicodeMap<Script_Values> script,
            UnicodeMap<Age_Values> age,
            UnicodeMap<General_Category_Values> gc,
            Counter<Script_Values> scriptCounter) {
        Counter<General_Category_Values> gcCounter = new Counter<>();
        Counter<General_Category_Values> gcCounter2 = new Counter<>();
        for (String s : age.getSet(Age_Values.V9_0)) {
            scriptCounter.add(script.get(s), 1);
            gcCounter.add(gc.get(s), 1);
        }

        for (String s : new UnicodeSet(0, 0x10FFFF)) {
            final General_Category_Values gcv = gc.get(s);
            if (gcv == gcv.Unassigned || gcv == gcv.Private_Use || gcv == gcv.Surrogate) {
                continue;
            }
            gcCounter2.add(gcv, 1);
        }

        show(scriptCounter);
        System.out.println();
        show(gcCounter);
        System.out.println();
        show(gcCounter2);

        System.out.println("Properties: " + UcdProperty.values().length);
    }

    private static <T> void show(Counter<T> scriptCounter) {
        for (T s : scriptCounter.getKeysetSortedByCount(false)) {
            System.out.println(scriptCounter.get(s) + "\t" + s);
        }
        System.out.println(scriptCounter.getTotal() + "\tTOTAL");
    }
}
