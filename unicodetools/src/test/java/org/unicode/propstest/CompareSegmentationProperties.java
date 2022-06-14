package org.unicode.propstest;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;

public class CompareSegmentationProperties {
    public static void main(String[] args) {
        IndexUnicodeProperties V8 = IndexUnicodeProperties.make(Age_Values.V8_0);
        // UnicodeSet UNASSIGNED8 = V8.loadEnumSet(UcdProperty.General_Category,
        // General_Category_Values.Unassigned);
        IndexUnicodeProperties V10 = IndexUnicodeProperties.make(Age_Values.V10_0);
        UnicodeMap<Enum> temp = new UnicodeMap<>();
        UcdProperty[] querySet = {
            UcdProperty.Grapheme_Cluster_Break, UcdProperty.Word_Break, UcdProperty.Line_Break
        };
        // UcdProperty.values();
        for (UcdProperty prop : querySet) {
            UnicodeMap<Enum> property8 = V8.loadEnum(prop);
            UnicodeMap<Enum> property10 = V10.loadEnum(prop);
            // for each value that didn't exist in v8, get the old values
            for (Enum value8 : prop.getEnums()) {
                UnicodeSet set8 = property8.getSet(value8);
                if (!set8.isEmpty()) {
                    continue;
                }
                temp.clear();
                UnicodeSet set10 = property10.getSet(value8);
                for (String item : set10) {
                    temp.put(item, property8.get(item));
                }
                for (Enum value10 : temp.values()) {
                    System.out.println(
                            "||"
                                    + prop
                                    + "||"
                                    + value8
                                    + "||"
                                    + value10
                                    + "||"
                                    + temp.getSet(value10).toPattern(false)
                                    + "||");
                }
            }
        }
    }
}
