package org.unicode.props;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.util.BagFormatter;

public class GenerateEnums {
    public static final String SOURCE_DIR = "/Users/markdavis/Documents/workspace/DATA/UCD/6.1.0-Update";
    private static class Locations {
        private static Set<String> files = addAll(new HashSet<String>(), new File(SOURCE_DIR));
        public static boolean contains(String file) {
            return files.contains(file.replace("_",""));
        }
        private static Set<String> addAll(HashSet<String> result, File sourceDir) {
            for (String file : sourceDir.list()) {
                if (!file.endsWith(".txt")) {
                    final File subDir = new File(file);
                    if (subDir.isDirectory()) {
                        addAll(result, subDir);
                    }
                    continue;
                }
                // ArabicShaping-6.1.0d2.txt
                file = file.substring(0,file.length()-4);
                int pos = file.indexOf('-');
                if (pos >= 0) {
                    file = file.substring(0,pos);
                }
                result.add(file);
            }
            return result;
        }
    }

    public static final String PROPERTY_FILE = "/Users/markdavis/Documents/workspace/unicodetools2/org/unicode/props/Properties.java";

    enum PropertyType {
        Numeric, String, Miscellaneous, Catalog, Enumerated, Binary;
        //Set<String> props = new TreeSet<String>();
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
    public static void main(String[] args) throws IOException {

        Map<PropName, List<String[]>> values = new TreeMap<PropName, List<String[]>>();

        addPropertyAliases(values, FileUtilities.in("", Utility.getMostRecentUnicodeDataFile("PropertyAliases", Default.ucdVersion(), true, true)));
        addPropertyAliases(values, FileUtilities.in(GenerateEnums.class, "ExtraPropertyAliases.txt"));

        PrintWriter output = BagFormatter.openUTF8Writer("", PROPERTY_FILE);

        output.print("//Machine generated: GenerateEnums.java\n" +
                "package org.unicode.props;\n" +
                "import java.util.EnumMap;\n" +
                "import java.util.LinkedHashMap;\n" +
                "import java.util.Map;\n" +
                "public class Properties {\n" +
        "\tpublic enum PropertyType {");
        for (PropertyType pt : PropertyType.values()) {
            output.print(pt + ", ");
        }
        output.println("}\n");

        output.println(
                "\tprivate static <T> void addNames(LinkedHashMap<String, T> map, String[] otherNames, T item) {\n" +
                "\t\tmap.put(item.toString(), item);\n" +
                "\t\tfor (String other : otherNames) {\n" +
                "\t\t\tmap.put(other, item);\n" +
                "\t\t}\n" +
                "\t}\n"
        );


        output.println("\tprivate static final LinkedHashMap<String," + "UcdProperty" + "> " + "UcdProperty" + "_Names = new LinkedHashMap<String," + "UcdProperty" + ">();\n");

        output.println("\tpublic enum " + "UcdProperty" + " {");
        for (PropertyType pt : PropertyType.values()) {
            int count = 0;
            output.println("\n\t\t// " + pt);
            for (Entry<String, PropName> i : lookupMain.entrySet()) {
                if (i.getValue().propertyType != pt) {
                    continue;
                }
                //                if (count++ > 7) {
                //                    output.println();
                //                    count = 0;
                //                }
                output.print("\t\t" + i.getKey());
                PropName pname = i.getValue();
                LinkedHashSet names = new LinkedHashSet();
                writeOtherNames(output, "PropertyType." + pt, pname.longName, pname.shortName);
                output.print(",\n");
            }
        }
        output.println("\t\t;");
        boolean first = true;
        output.println(";\n" +
                "\t\tfinal PropertyType type;\n\t\tfinal LinkedHashMap<String, Enum> enumNames = new LinkedHashMap<String, Enum>();\n"  +
                "\t\tprivate UcdProperty(PropertyType type, String...otherNames) {\n" +
                "\t\t\tthis.type = type;\n" +
                "\t\t\taddNames(UcdProperty_Names, otherNames, this);\n" +
                "\t\t}\n" +
                "\t\tstatic UcdProperty forName(String name) {\n" +
                "\t\t\treturn UcdProperty_Names.get(name);\n" +
                "\t\t}\n" +
                "\t\tpublic Enum forValueName(String name) {\n" +
                "\t\t\treturn enumNames.get(name);\n" +
                "\t\t}\n" +
                "\t}"
        );

        //[Alpha, N, No, F, False]
        addPropertyValueAliases(values, FileUtilities.in("", Utility.getMostRecentUnicodeDataFile("PropertyValueAliases", Default.ucdVersion(), true, true)));

        for (Entry<PropName, List<String[]>> value : values.entrySet()) {
            final List<String[]> partList = value.getValue();
            final PropName propName = value.getKey();
            if (partList.size() == 0) {
                output.println("\t\t// " + propName.longName);
                continue;
            }
            output.println("\tpublic enum " + (propName.longName + "_Values") + " {");
            StringBuilder constants = new StringBuilder();
            first = true;
            for (String[] parts : partList) {
                final String longName = parts[2];
                if (first) {
                    first = false;
                    output.print("\t\t");
                } else {
                    output.print(",\n\t\t");
                }
                output.print(fix(longName));
                writeOtherNames(output, null, parts);

                for (int i = 1; i < parts.length; ++i) {
                    final String otherName = parts[i];
                    if (i == 2 || otherName.equals("n/a") || otherName.equals(longName) || otherName.contains("-") || otherName.charAt(0) < 'A') {
                        continue;
                    }
                    if (constants.length() != 0) {
                        constants.append(",");
                    }
                    constants.append("\n\t\t" + otherName + "=" + longName);
                }
            }
            String enumName = propName.longName;
            output.println(";\n"  +
                    "\t\tprivate " + enumName + "_Values (" +
                    "String...otherNames) {\n" +
                    "\t\t\taddNames(UcdProperty." + enumName +".enumNames, otherNames, this);\n" +
                    "\t\t}\n" +
            "\t}");
        }
        output.println("\n}");
        output.close();
    }


    public static void writeOtherNames(PrintWriter output, String string, String... otherNames) {
        boolean haveFirst = false;
        output.print("(");
        if (string != null) {
            output.print(string);
            haveFirst = true;
        }
        boolean skip = true;
        for (String otherName : otherNames) {
            if (skip) {
                skip = false;
                continue;
            }
            if (otherName.equals("n/a") || otherName.equals(otherNames[0])) {
                continue;
            }
            if (haveFirst) output.print(", ");
            output.print("\"" + otherName + "\"");
            haveFirst = true;
        }
        output.print(")");
    }


    public static void addPropertyValueAliases(Map<PropName, List<String[]>> values, Iterable<String> lines) {
        for (String line : lines) {
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
    }

    public static void addPropertyAliases(Map<PropName, List<String[]>> values, Iterable<String> lines) {
        Matcher propType = Pattern.compile("#\\s+(\\p{Alpha}+)\\s+Properties\\s*").matcher("");
        PropertyType type = null;
        for (String line : lines) {
            //System.out.println(line);
            if (propType.reset(line).matches()) {
                type = PropertyType.valueOf(propType.group(1));
            }
            String[] parts = FileUtilities.cleanSemiFields(line);
            if (parts == null) {
                continue;
            }
            PropName propName = new PropName(type, parts);
            values.put(propName, new ArrayList<String[]>());
            //System.out.println(propNames);
            //            if (!Locations.contains(propName.longName)) {
            //                System.out.println("Missing file: " + propName.longName);
            //            }
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
