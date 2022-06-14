package org.unicode.tools;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.IdUsage;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Utility;

public class ShowScriptCategories {
    static final SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();
    static final Relation<String, String> core = SDI.getContainmentCore();
    static final LocaleDisplayNames ldn = LocaleDisplayNames.getInstance(ULocale.ENGLISH);
    public static final IndexUnicodeProperties IUP =
            IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    public static UnicodeMap<Script_Values> SCRIPT =
            IUP.loadEnum(UcdProperty.Script, Script_Values.class);
    public static UnicodeMap<Age_Values> VERSION = IUP.loadEnum(UcdProperty.Age, Age_Values.class);
    public static UnicodeMap<String> NAME = IUP.load(UcdProperty.Name);

    public static void main(String[] args) {
        Map<String, List<String>> rows =
                getRows(Collections.singletonList("001"), "001", new TreeMap<>());
        //        for (Entry<String, List<String>> s : rows.entrySet()) {
        //            System.out.println(s.getKey() + "\t" + s.getValue());
        //        }
        for (Script_Values scriptv : SCRIPT.values()) {
            if (scriptv == Script_Values.Unknown) {
                continue;
            }
            String script = scriptv.getShortName();
            Info info = ScriptMetadata.getInfo(script);
            if (info == null) {
                System.err.println("Missing info for: " + scriptv);
                continue;
            }
            String country = info.originCountry;
            List<String> row = rows.get(country);
            String continent = row == null ? "ZZ" : row.get(1);
            String subcontinent = row == null ? "ZZ" : row.get(2);

            int size = SCRIPT.getSet(scriptv).size();

            final String continentString =
                    ldn.regionDisplayName(continent) + " (" + continent + ")";
            final String subcontinentString =
                    ldn.regionDisplayName(subcontinent) + " (" + subcontinent + ")";
            final String countryString = ldn.regionDisplayName(country) + " (" + country + ")";
            String scriptName = ldn.scriptDisplayName(script);
            if (scriptName.equals(script)) {
                scriptName = scriptv.toString().replace('_', ' ');
            }
            final String usageName =
                    script.equals("Brai") || script.equals("Sgnw")
                            ? "Symbol"
                            : usageName(info.idUsage);
            System.out.println(
                    (continent.equals("142") ? subcontinentString : continentString)
                            + "\t"
                            + usageName
                            + "\t"
                            + scriptName
                            + "\t"
                            + scriptv.toString()
                            + "\t"
                            + script
                            + "\t"
                            + size
                            + "\t"
                            + continentString
                            + "\t"
                            + subcontinentString
                            + "\t"
                            + countryString);
        }
        System.out.println();
        for (String s :
                new UnicodeSet(VERSION.getSet(Age_Values.V9_0))
                        .retainAll(SCRIPT.getSet(Script_Values.Common))) {
            System.out.println("U+" + Utility.hex(s) + "\t" + NAME.get(s));
        }
    }

    private static String usageName(IdUsage idUsage) {
        switch (idUsage) {
            case ASPIRATIONAL:
            case LIMITED_USE:
                return "Limited Use";
            case EXCLUSION:
                return "Historic";
            case RECOMMENDED:
            case UNKNOWN:
            default:
                return "Modern";
        }
    }

    private static Map<String, List<String>> getRows(
            List<String> list, String territoryCode, Map<String, List<String>> target) {
        Set<String> contained = core.get(territoryCode);
        if (contained == null) {
            target.put(territoryCode, Collections.unmodifiableList(list));
        } else {
            for (String s : contained) {
                ArrayList<String> newList = new ArrayList<>(list);
                newList.add(s);
                getRows(newList, s, target);
            }
        }
        return target;
    }
}
