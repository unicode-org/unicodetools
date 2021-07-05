package org.unicode.propstest;

import java.util.Objects;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class ListAssignedWithDefaultValues {
    private static final IndexUnicodeProperties latest = IndexUnicodeProperties.make(Settings.latestVersion); // GenerateEnums.ENUM_VERSION

    public static void main(String[] args) {
        UnicodeMap<General_Category_Values> gc = latest.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
        UnicodeSet cn = gc.getSet(General_Category_Values.Unassigned);
        UnicodeSet unassigned = new UnicodeSet(
                cn)
                .addAll(gc.getSet(General_Category_Values.Private_Use))
                .addAll(gc.getSet(General_Category_Values.Surrogate))
                .freeze();
        UnicodeSet assigned = new UnicodeSet(unassigned).complement()
                .freeze();
        UnicodeSet mark = new UnicodeSet(
                gc.getSet(General_Category_Values.Nonspacing_Mark))
                .addAll(gc.getSet(General_Category_Values.Enclosing_Mark))
                .addAll(gc.getSet(General_Category_Values.Spacing_Mark))
                .freeze();

        for (UcdProperty prop : latest.getAvailableUcdProperties()) {
            switch(prop.getType()) {
            case Binary:
            case Enumerated:
            case Catalog:
                default:
                UnicodeMap<String> map = latest.load(prop);
                String defaultValue = map.get(0x80000);
                String defaultValue2 = IndexUnicodeProperties.getDefaultValue(prop);
                
                UnicodeSet defaultValues = map.getSet(defaultValue);
                UnicodeSet assignedWithDefaultValues = new UnicodeSet(defaultValues).retainAll(assigned);
                UnicodeSet unassignedWithoutDefaultValues = new UnicodeSet(cn).removeAll(defaultValues);
                System.out.println(prop.getType()
                        + "\t" + prop 
                        + "\t" + defaultValue 
                        + "\t" + assignedWithDefaultValues.size()
                        + "\t" + unassignedWithoutDefaultValues.size()
                        + "\t" + (!Objects.equals(defaultValue, defaultValue2) ? defaultValue2 : "")
                        );
                if (prop == UcdProperty.Script) {
                    for (String s : assignedWithDefaultValues) {
                        System.out.println(Utility.hex(s) + "; " + (mark.contains(s) ? "Inherited" : "Common") + "\t" + latest.getName(s, ", "));
                    }
                }
            }

        }
    }
}
