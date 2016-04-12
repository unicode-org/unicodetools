package org.unicode.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.IdUsage;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.util.ULocale;

public class ShowScriptCategories {
    static final SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();
    static final Relation<String, String> core = SDI.getContainmentCore();
    static final LocaleDisplayNames ldn = LocaleDisplayNames.getInstance(ULocale.ENGLISH);

    public static void main(String[] args) {
        Map<String, List<String>> rows = getRows(Collections.singletonList("001"), "001", new TreeMap<>());
        for (Entry<String, List<String>> s : rows.entrySet()) {
            System.out.println(s.getKey() + "\t" + s.getValue());
        }
        for (String script : ScriptMetadata.getScripts()) {
            Info info = ScriptMetadata.getInfo(script);
            String country = info.originCountry;
            List<String> row = rows.get(country);
            if (row == null) {
                //System.out.println("**NO info for " + script);
                continue;
            }
            final String continent = row.get(1);
            final String subcontinent = row.get(2);
            final String continentString = ldn.regionDisplayName(continent) + " (" + continent + ")";
            final String subcontinentString = ldn.regionDisplayName(subcontinent) + " (" + subcontinent + ")";
            final String countryString = ldn.regionDisplayName(country) + " (" + country + ")";
            final String scriptName = ldn.scriptDisplayName(script) + " (" + script + ")";
            final String usageName = script.equals("Brai") || script.equals("Sgnw") ? "Symbol" : usageName(info.idUsage);
            System.out.println( 
                    (continent.equals("142") 
                            ? subcontinentString 
                                    : continentString)
                    + "\t" + usageName
                    + "\t" + scriptName
                    + "\t\t" + continentString
                    + "\t" + subcontinentString
                    + "\t" + countryString
                    );
        }
    }

    private static String usageName(IdUsage idUsage) {
        switch(idUsage) {
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

    private static Map<String, List<String>> getRows(List<String> list, String territoryCode, Map<String,List<String>> target) {
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
