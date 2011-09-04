package org.unicode.props;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

import com.sun.tools.jdi.LinkedHashMap;

public class GenerateEnums {
    enum PropertyType {Numeric, String, Miscellaneous, Catalog, Enumerated, Binary;
    Set<String> props = new TreeSet<String>();
    }
    static Map<String,PropName> lookup = new HashMap<String,PropName>();
    static Map<String,PropName> lookupMain = new TreeMap<String,PropName>();

    static class PropName implements Comparable<PropName>{
        final PropertyType propertyType;
        final String shortName;
        final String longName;
        final List<String> others;
        final Map<String, PropName> subnames = new TreeMap<String, PropName>();
        PropName(PropertyType type, String...strings) {
            propertyType = type;
            shortName = strings[0];
            longName = strings[1];
            if (strings.length == 2) {
                others = Collections.emptyList();
            } else {
                List<String> temp = Arrays.asList(strings);
                others = Collections.unmodifiableList(temp.subList(2, strings.length));
            }
            for (String name : strings) {
                if (lookup.containsKey(name)) {
                    throw new IllegalArgumentException("Duplicate propName");
                }
            }
            for (String name : strings) {
                lookup.put(name, this);
            }
            lookupMain.put(longName, this);
        }
        public String toString() {
            return "{" + propertyType + ",\t" + longName + ",\t" + shortName + ",\t" + others + "}";
        }
        @Override
        public int compareTo(PropName arg0) {
            return longName.compareTo(arg0.longName);
        }
    }
    public static void main(String[] args) {

        Map<PropName, List<String[]>> values = new TreeMap<PropName, List<String[]>>();

        Matcher propType = Pattern.compile("#\\s+(\\p{Alpha}+)\\s+Properties\\s*").matcher("");
        PropertyType type = null;
        for (String line : FileUtilities.in("", Utility.getMostRecentUnicodeDataFile("PropertyAliases", Default.ucdVersion(), true, true))) {
            //System.out.println(line);
            if (propType.reset(line).matches()) {
                type = PropertyType.valueOf(propType.group(1));
            }
            String[] parts = FileUtilities.cleanSemiFields(line);
            if (parts == null) {
                continue;
            }
            PropName propName = new PropName(type, parts);
            values.put(propName, new ArrayList());
            //System.out.println(propNames);
        }
        System.out.println("enum UcdProperty {");
        for (PropertyType pt : PropertyType.values()) {
            int count = 0;
            System.out.println("// " + pt);
            for (Entry<String, PropName> i : lookupMain.entrySet()) {
                if (i.getValue().propertyType != pt) {
                    continue;
                }
                if (count++ > 7) {
                    System.out.println();
                    count = 0;
                }
                System.out.print(i.getKey() + ",\t");
            }
            System.out.println();
        }
        System.out.println(";");
        System.out.println("static final UcdProperty");
        boolean first = true;
        for (PropName v : lookupMain.values()) {
            if (!v.shortName.equals(v.longName)) {
                if (first) {
                    first = false;
                } else {
                    System.out.print(",\n");
                }
                System.out.print("\t" + v.shortName + " = " + v.longName);
            }
            for (String other : v.others){
                if (!other.equals(v.longName)) {
                    if (first) {
                        first = false;
                    } else {
                        System.out.print(",\n");
                    }
                    System.out.print("\t" + other + " = " + v.longName);
                }
            }
        }
        System.out.println(";\n}");

        //[Alpha, N, No, F, False]
        for (String line : FileUtilities.in("", Utility.getMostRecentUnicodeDataFile("PropertyValueAliases", Default.ucdVersion(), true, true))) {
            String[] parts = FileUtilities.cleanSemiFields(line);
            if (parts == null) {
                continue;
            }
            PropName propName = lookup.get(parts[0]);
            if (propName == null) {
                throw new IllegalArgumentException("Missing Prop Name in " + Arrays.asList(parts));
            }
            List<String[]> set = values.get(propName);
            set.add(parts);
            //System.out.println(propName.longName + "\t" + Arrays.asList(parts));
        }
        for (Entry<PropName, List<String[]>> value : values.entrySet()) {
            final List<String[]> partList = value.getValue();
            if (partList.size() == 0) {
                System.out.println("// " + value.getKey().longName);
                continue;
            }
            System.out.print("enum " + value.getKey().longName + " {\n\t");
            StringBuilder constants = new StringBuilder();
            first = true;
            for (String[] parts : partList) {
                final String longName = parts[2];
                if (first) {
                    first = false;
                } else {
                    System.out.print(", ");
                }
                System.out.print(fix(longName));
                for (int i = 1; i < parts.length; ++i) {
                    final String otherName = parts[i];
                    if (i == 2 || otherName.equals("n/a") || otherName.equals(longName) || otherName.contains("-") || otherName.charAt(0) < 'A') {
                        continue;
                    }
                    if (constants.length() != 0) {
                        constants.append(",");
                    }
                    constants.append("\n\t" + otherName + "=" + longName);
                }
            }
            if (constants.length() > 0) {
                System.out.print(";\n\tstatic final " + value.getKey().longName);
                System.out.print(constants);
                System.out.print(";");
            }
            System.out.println("\n}");
        }
    }
    private static String fix(String string) {
        char ch = string.charAt(0);
        if ('0' <= ch && ch <= '9') {
            return "_" + string.replace('.', '_');
        }
        return string;
    }
}
