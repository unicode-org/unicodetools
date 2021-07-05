package org.unicode.text.tools;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;

public class GenerateSubtagNames {
    public static void main(String[] args) {
        CLDRConfig config = CLDRConfig.getInstance();
        SupplementalDataInfo sdi = config.getSupplementalDataInfo();
        StandardCodes sc = StandardCodes.make();
        CLDRFile english = config.getEnglish();
        Map<String,String> seen = new TreeMap();
        Set<String> CODE_OK = new HashSet(Arrays.asList("QO", "UK", "ZZ"));
        for (Entry<LstrType, Map<String, Map<LstrField, String>>> entry : StandardCodes
                .getEnumLstreg().entrySet()) {
            LstrType type = entry.getKey();
            int cldrType = -1;
            switch(type) {
            case language: cldrType = CLDRFile.LANGUAGE_NAME; break;
            case script: cldrType = CLDRFile.SCRIPT_NAME; break;
            case region: cldrType = CLDRFile.TERRITORY_NAME; break;
            case variant:
                break;
            case extlang:
            case redundant:
            case legacy:
                continue;
            }
            
            for (Entry<String, Map<LstrField, String>> entry2 : entry.getValue().entrySet()) {
                String code = entry2.getKey();
                final Map<LstrField, String> fieldToValue = entry2.getValue();
                String scope = fieldToValue.get(LstrField.Scope);
                if (scope != null 
                        && scope.equals("private-use") 
                        && !CODE_OK.contains(code)) {
                    continue;
                }
                String description = fieldToValue.get(LstrField.Description);
                if (description != null 
                        && description.equalsIgnoreCase("Private use") 
                        && !CODE_OK.contains(code)) {
                    continue;
                }
                if (seen.containsKey(code)) {
                    throw new IllegalArgumentException();
                }
                String name = cldrType == -1 ? null : english.getName(cldrType, code);
                if (name == null) {
                    if (description.contains("▪")) {
                        description = description.split("▪")[0];
                    }
                    name = description;
                }
                seen.put(code, name);
            }
        }
        System.out.println("# GenerateSubtagNames");
        for (Entry<String, String> entry2 : seen.entrySet()) {
            System.out.println(entry2.getKey() + ";" + entry2.getValue());
        }
    }
}
