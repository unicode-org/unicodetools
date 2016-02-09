package org.unicode.props;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.util.VersionInfo;

public class GenerateEnums {
    public static final String ENUM_VERSION = Default.ucdVersion();
    public static final VersionInfo ENUM_VERSION_INFO = VersionInfo.getInstance(GenerateEnums.ENUM_VERSION);

    public static final String PROPERTY_FILE_OUTPUT = Settings.UNICODETOOLS_DIRECTORY + "/org/unicode/props/UcdProperty.java";
    public static final String PROPERTY_VALUE_OUTPUT = Settings.UNICODETOOLS_DIRECTORY + "/org/unicode/props/UcdPropertyValues.java";

    //    private static class Locations {
    //        private static Set<String> files = addAll(new HashSet<String>(), new File(SOURCE_DIR));
    //        public static boolean contains(String file) {
    //            return files.contains(file.replace("_",""));
    //        }
    //        private static Set<String> addAll(HashSet<String> result, File sourceDir) {
    //            for (String file : sourceDir.list()) {
    //                if (!file.endsWith(".txt")) {
    //                    final File subDir = new File(file);
    //                    if (subDir.isDirectory()) {
    //                        addAll(result, subDir);
    //                    }
    //                    continue;
    //                }
    //                // ArabicShaping-6.1.0d2.txt
    //                file = file.substring(0,file.length()-4);
    //                final int pos = file.indexOf('-');
    //                if (pos >= 0) {
    //                    file = file.substring(0,pos);
    //                }
    //                result.add(file);
    //            }
    //            return result;
    //        }
    //    }


    static Map<String,PropName> lookup = new HashMap<String,PropName>();
    static Map<String,PropName> lookupMain = new TreeMap<String,PropName>();

    private static final Pattern PROPERTY_LONG_NAME = Pattern.compile("[A-Z]+[0-9]?[a-z]*(_[A-Z0-9]+[a-z]*)*");
    private static final Pattern PROPERTY_VALUE_LONG_NAME = Pattern.compile("[A-Z]+[0-9]?[a-z]*(_[A-Z0-9]+[a-z]*)*(\\d*)");
    private static final Pattern PROPER_CJK_LONG_NAME = Pattern.compile("(cj)?k[A-Z][a-z]*(_?[A-Z0-9][a-z]*)*");

    static class PropName implements Comparable<PropName>{
        final PropertyType propertyType;
        final String shortName;
        final String longName;
        final List<String> others;
        final Map<String, PropName> subnames = new TreeMap<String, PropName>();
        PropName(PropertyType type, String...strings) {
            shortName = strings[0];
            longName = strings[1];
            propertyType = longName.equals("Script_Extensions") ? PropertyType.Catalog : type; // HACK
            final String badName = isProperLongName(longName, PROPERTY_LONG_NAME, true);
            if (badName != null) {
                addWarning("Improper Property long name: " + badName);
            }
            if (strings.length == 2) {
                others = Collections.emptyList();
            } else {
                final List<String> temp = Arrays.asList(strings);
                others = Collections.unmodifiableList(temp.subList(2, strings.length));
            }
            for (final String name : strings) {
                if (lookup.containsKey(name)) {
                    throw new UnicodePropertyException("Duplicate propName");
                }
            }
            for (final String name : strings) {
                lookup.put(name, this);
            }
            lookupMain.put(longName, this);
        }
        private static String isProperLongName(String longName2, Pattern pattern, boolean allowCjk) {
            boolean result = pattern.matcher(longName2).matches();
            if (result == false && allowCjk) {
                result = PROPER_CJK_LONG_NAME.matcher(longName2).matches();
            }
            if (result == false) {
                return RegexUtilities.showMismatch(pattern, longName2);
            }
            return null;
        }
        @Override
        public String toString() {
            return "{" + propertyType + ",\t" + longName + ",\t" + shortName + ",\t" + others + "}";
        }
        @Override
        public int compareTo(PropName arg0) {
            return longName.compareTo(arg0.longName);
        }
    }

    public static void main(String[] args) throws IOException {

        final Map<PropName, List<String[]>> values = new TreeMap<PropName, List<String[]>>();

        addPropertyAliases(values, FileUtilities.in("", Utility.getMostRecentUnicodeDataFile("PropertyAliases", ENUM_VERSION, true, true)));
        addPropertyAliases(values, FileUtilities.in(GenerateEnums.class, "ExtraPropertyAliases.txt"));

        writeMainUcdFile();

        writeValueEnumFile(values);
        for (final String s : WARNINGS) {
            System.out.println(s);
        }
        
        System.out.println("lookupMain: " + lookupMain.size());
        System.out.println(CollectionUtilities.join(lookupMain.values(), "\n"));
    }

    public static String getNameStuff2(final String enumName) {
        return ";\n"  +
                "        private final PropertyNames<" + enumName + "> names;\n"+
                "        private " + enumName + " (String shortName, String...otherNames) {\n"+
                "            names = new PropertyNames<" + enumName + ">(\n"
                + "                " + enumName + ".class, this, shortName, otherNames);\n"+
                "        }\n"+
                "        public PropertyNames<" + enumName + "> getNames() {\n"+
                "            return names;\n"+
                "        }\n" +
                "        public String getShortName() {\n" +
                "            return names.getShortName();\n" +
                "        }\n" +
                "    }\n";
    }

    public static void writeValueEnumFile(Map<PropName, List<String[]>> values) throws IOException {
        final PrintWriter output = BagFormatter.openUTF8Writer("", PROPERTY_VALUE_OUTPUT);
        output.println("package org.unicode.props;\n"
                + "import org.unicode.props.PropertyNames.Named;\n\n"
                + "public class UcdPropertyValues {");

        //[Alpha, N, No, F, False]
        addPropertyValueAliases(values, FileUtilities.in("", Utility.getMostRecentUnicodeDataFile("PropertyValueAliases", ENUM_VERSION, true, true))); 
        addPropertyValueAliases(values, FileUtilities.in(GenerateEnums.class, "ExtraPropertyValueAliases.txt"));

        output.println(
                "\n    public enum Binary implements Named {\n"+
                        "        No(\"N\", \"F\", \"False\"),\n"+
                        "        Yes(\"Y\", \"T\", \"True\")" +
                        getNameStuff2("Binary")
                        //                        ";\n"+
                        //                        
                        //                        "        private final PropertyNames<Binary> names;\n"+
                        //                        "        private Binary (String shortName, String...otherNames) {\n"+
                        //                        "            names = new PropertyNames(Binary.class, this, shortName, otherNames);\n"+
                        //                        "        }\n"+
                        //                        "        public PropertyNames<Binary> getNames() {\n"+
                        //                        "            return names;\n"+
                        //                        "        }\n"+
                        //                        "    }\n"
                );

        for (final Entry<PropName, List<String[]>> value : values.entrySet()) {
            final PropName propName = value.getKey();
            System.out.println("Writing:\t" + propName.longName);
            if (propName.propertyType == PropertyType.Binary) {
                continue;
            }
            final List<String[]> partList = value.getValue();
            if (partList.size() == 0) {
                output.println("\t\t// " + propName.longName);
                continue;
            }
            output.println("\tpublic enum " + (propName.longName + "_Values") + " implements Named {");
            final StringBuilder constants = new StringBuilder();
            boolean first = true;
            for (final String[] parts : partList) {
                String longName = parts[2];

                // HACK
                if (propName.shortName.equals("ccc")) {
                    longName = parts[3];
                }
                final String badName = PropName.isProperLongName(longName, PROPERTY_VALUE_LONG_NAME, false);
                if (badName != null) {
                    addWarning("Improper long value name for " + parts[0] + ": " + badName);
                }
                if (first) {
                    first = false;
                    output.print("        ");
                } else {
                    output.print(",\n        ");
                }
                output.print(fix(longName));
                writeOtherNames2(output, longName, parts);

                for (int i = 1; i < parts.length; ++i) {
                    final String otherName = parts[i];
                    if (i == 2 || otherName.equals("n/a") || otherName.equals(longName) || otherName.contains("-") || otherName.charAt(0) < 'A') {
                        continue;
                    }
                    if (constants.length() != 0) {
                        constants.append(",");
                    }
                    constants.append("\n        " + otherName + "=" + longName);
                }
            }
            final String enumName = propName.longName;

            output.println(getNameStuff2(enumName+"_Values"));
        }
        output.println("}");
        output.close();
    }


    static Set<String> WARNINGS = new LinkedHashSet<String>();
    private static void addWarning(String string) {
        WARNINGS.add(string);
    }

    // otherNames are x, short, long, others
    // we don't need to do x or long
    public static void writeOtherNames2(PrintWriter output, String longName, String... otherNames) {
        output.print("(");
        boolean haveOne = false;
        for (int i = 1; i < otherNames.length; ++i) {
            final String name = otherNames[i];
            if (i != 1) {
                if (longName.equals(name)) {
                    continue;
                }
                output.print(", ");
            }
            if (name.equals("n/a")) {
                output.print("null");
            } else {
                output.print("\"" + name + "\"");
            }
            haveOne = true;
        }
        if (!haveOne) {
            output.print("null");
        }
        output.print(")");
    }

    static Map<String,ValueCardinality> NAME2CARD = new HashMap<>();
    static {
        Splitter SEMICOLON = Splitter.on(";").trimResults();
        for (final String line : FileUtilities.in(IndexUnicodeProperties.class, "IndexPropertyRegex.txt")) {
            if (line.startsWith("$") || line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            List<String> parts = SEMICOLON.splitToList(line);
            // Bidi_Mirroring_Glyph ;        SINGLE_VALUED ;               $codePoint

            final String propertyName = parts.get(0).toLowerCase(Locale.ENGLISH);
            final String multivalued = parts.get(1);
            switch(multivalued) {
            case "ORDERED": NAME2CARD.put(propertyName, ValueCardinality.Ordered); break;
            case "MULTI_VALUED": NAME2CARD.put(propertyName, ValueCardinality.Unordered); break;
            case "SINGLE_VALUED": NAME2CARD.put(propertyName, ValueCardinality.Singleton); break;
            case "EXTENSIBLE": NAME2CARD.put(propertyName, ValueCardinality.Singleton); break;
            default: throw new UnicodePropertyException("IndexPropertyRegex: didn't expect " + multivalued);
            }
        }
    }

    public static void writeMainUcdFile() throws IOException {
        final PrintWriter output = BagFormatter.openUTF8Writer("", PROPERTY_FILE_OUTPUT);

        output.print(
                "//Machine generated: GenerateEnums.java\n" +
                        "package org.unicode.props;\n" +
                        "import java.util.EnumSet;\n" +
                        "import java.util.Set;\n"+
                        "import org.unicode.props.PropertyNames.NameMatcher;\n" +
                        "import org.unicode.props.UcdPropertyValues.*;\n\n"
                );
        //        "\tpublic enum PropertyType {");
        //        for (PropertyType pt : PropertyType.values()) {
        //            output.print(pt + ", ");
        //        }
        //        output.println("}\n");
        //
        //        output.println(
        //                "\tprivate static <T> void addNames(LinkedHashMap<String, T> map, String[] otherNames, T item) {\n" +
        //                "\t\tmap.put(item.toString(), item);\n" +
        //                "\t\tfor (String other : otherNames) {\n" +
        //                "\t\t\tmap.put(other, item);\n" +
        //                "\t\t}\n" +
        //                "\t}\n"
        //        );
        //
        //
        //        output.println("\tprivate static final LinkedHashMap<String," + "UcdProperty" + "> " + "UcdProperty" + "_Names = new LinkedHashMap<String," + "UcdProperty" + ">();\n");

        output.println("public enum " + "UcdProperty" + " {");
        Set<String> missingCardinality = new TreeSet<>();
        Set<String> extraCardinality = new TreeSet<>(NAME2CARD.keySet());

        for (final PropertyType pt : PropertyType.values()) {
            final int count = 0;
            output.println("\n\t\t// " + pt);
            for (final Entry<String, PropName> i : lookupMain.entrySet()) {
                if (i.getValue().propertyType != pt) {
                    continue;
                }
                //                if (count++ > 7) {
                //                    output.println();
                //                    count = 0;
                //                }
                output.print("    " + i.getKey());
                final PropName pname = i.getValue();
                final String type = "PropertyType." + pt;
                String classItem = null;
                String lcPropName = pname.longName.toLowerCase(Locale.ENGLISH);
                ValueCardinality cardinality = NAME2CARD.get(lcPropName);
                if (cardinality == null) {
                    missingCardinality.add(lcPropName);
                    cardinality = ValueCardinality.Singleton;
                } else {
                    extraCardinality.remove(lcPropName);
                }

                switch (pt) {
                case Binary:
                    classItem = //"UcdPropertyValues." +
                    "Binary.class";
                    break;
                case Enumerated:
                case Catalog:
                    classItem = // "UcdPropertyValues." + 
                    ("Script_Extensions".equals(pname.longName) ? "Script" : pname.longName) + "_Values.class"; // HACK!
                    break;
                }
                writeOtherNames(output, type, classItem, cardinality, pname.longName, pname.shortName);
                output.print(",\n");
            }
        }
        if (!missingCardinality.isEmpty()) {
            System.err.println("No Cardinality for " + missingCardinality);
        }
        if (!missingCardinality.isEmpty()) {
            System.err.println("Extra Cardinality for " + extraCardinality);
        }
        output.println("\t\t;");
        final boolean first = true;
        output.println("\n" +
                "private final PropertyType type;\n"+
                "\tprivate final PropertyNames<UcdProperty> names;\n"+
                "\t// for enums\n"+
                "\tprivate final NameMatcher name2enum;\n"+
                "\tprivate final EnumSet enums;\n"+
                "\tprivate final Class enumClass;\n"+
                "\tprivate final ValueCardinality cardinality;\n"+
                "\t\n"+
                "\tprivate UcdProperty(PropertyType type, String shortName, String...otherNames) {\n"+
                "\t\tthis.type = type;\n"+
                "\t\tnames = new PropertyNames<UcdProperty>(UcdProperty.class, this, shortName, otherNames);\n"+
                "\t\tname2enum = null;\n"+
                "\t\tenums = null;\n"+
                "\t\tenumClass = null;\n"+
                "\t\tcardinality = ValueCardinality.Singleton;\n"+
                "\t}\n"+
                "\tprivate UcdProperty(PropertyType type, Class classItem, ValueCardinality _cardinality, String shortName, String...otherNames) {\n"+
                "\t\tthis.type = type;\n"+
                "\t\tnames = new PropertyNames<UcdProperty>(UcdProperty.class, this, shortName, otherNames);\n"+
                "\t\tcardinality = _cardinality == null ? ValueCardinality.Singleton : _cardinality;\n"+
                "\t\tif (classItem == null) {\n" +
                "\t\t\tname2enum = null;\n"+
                "\t\t\tenums = null;\n"+
                "\t\t\tenumClass = null;\n"+
                "\t\t} else {\n" +
                "\t\t\tenums = EnumSet.allOf(classItem);\n"+
                "\t\t\tname2enum = PropertyNames.getNameToEnums(classItem);\n"+
                "\t\t\tenumClass = classItem;\n"+
                "\t\t}\n" +
                "\t}\n"+
                "\t\n"+
                "\tpublic ValueCardinality getCardinality() {\n"+
                "\t\treturn cardinality;\n"+
                "\t}\n"+
                "\tpublic Class<Enum> getEnumClass() {\n"+
                "\t\treturn enumClass;\n"+
                "\t}\n"+
                "\tpublic PropertyType getType() {\n"+
                "\t\treturn type;\n"+
                "\t}\n"+
                "\tpublic PropertyNames<UcdProperty> getNames() {\n"+
                "\t\treturn names;\n"+
                "\t}\n"+
                "\tpublic String getShortName() {\n" +
                "\t\treturn names.getShortName();\n" +
                "\t}\n" +
                "\tpublic static UcdProperty forString(String name) {\n"+
                "\t\treturn Numeric_Value.names.forString(name);\n"+
                "\t}\n"+
                "\tpublic Enum getEnum(String name) {\n"+
                "\t\treturn name2enum == null ? null : name2enum.get(name);\n"+
                "\t}\n"+
                "\tpublic PropertyNames getEnumNames() {\n"+
                "\t\treturn name2enum == null ? null : name2enum.getNames();\n"+
                "\t}\n"    +
                "\tpublic Set<Enum> getEnums() {\n"+
                "\t\treturn enums;\n"+
                "\t}\n"
                );

        output.println("\n}");
        output.close();
    }


    public static void writeOtherNames(PrintWriter output, String type, 
            String classItem, ValueCardinality cardinality, String... otherNames) {
        output.print("(");
        //if (shortName != null) {
        output.print(type);
        if (classItem != null || cardinality != ValueCardinality.Singleton) {
            output.print(", " + classItem + ", "
                    + (cardinality == ValueCardinality.Singleton ? "null" 
                            : "ValueCardinality." + cardinality.toString()));
        }
        boolean first = true;
        for (final String otherName : otherNames) {
            if (first) {
                first = false;
                continue;
            }
            output.print(", \"" + otherName + "\"");
        }
        output.print(")");
    }


    public static void addPropertyValueAliases(Map<PropName, List<String[]>> values, Iterable<String> lines) {
        for (final String line : lines) {
            final String[] parts = FileUtilities.cleanSemiFields(line);
            if (parts == null) {
                continue;
            }
            final PropName propName = lookup.get(parts[0]);
            if (propName == null) {
                throw new UnicodePropertyException("Missing Prop Name in " + Arrays.asList(parts));
            }
            final List<String[]> set = values.get(propName);
            set.add(parts);
            //System.out.println(propName.longName + "\t" + Arrays.asList(parts));
        }
    }

    public static void addPropertyAliases(Map<PropName, List<String[]>> values, Iterable<String> lines) {
        final Matcher propType = Pattern.compile("#\\s+(\\p{Alpha}+)\\s+Properties\\s*").matcher("");
        PropertyType type = null;
        for (final String line : lines) {
            System.out.println(line);
            if (propType.reset(line).matches()) {
                type = PropertyType.valueOf(propType.group(1));
            }
            final String[] parts = FileUtilities.cleanSemiFields(line);
            if (parts == null) {
                continue;
            }
            final PropName propName = new PropName(type, parts);
            values.put(propName, new ArrayList<String[]>());
            System.out.println(propName);
            //            if (!Locations.contains(propName.longName)) {
            //                System.out.println("Missing file: " + propName.longName);
            //            }
        }
    }
    private static String fix(String string) {
        final char ch = string.charAt(0);
        if ('0' <= ch && ch <= '9') {
            return "_" + string.replace('.', '_');
        }
        return string;
    }
}
