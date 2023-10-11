package org.unicode.props;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.VersionInfo;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

public class GenerateEnums {
    public static final String ENUM_VERSION = Settings.latestVersion;
    public static final VersionInfo ENUM_VERSION_INFO =
            VersionInfo.getInstance(GenerateEnums.ENUM_VERSION);

    public static final String PROPERTY_FILE_OUTPUT =
            Settings.UnicodeTools.UNICODETOOLS_JAVA_DIR + "/org/unicode/props/UcdProperty.java";
    public static final String PROPERTY_VALUE_OUTPUT =
            Settings.UnicodeTools.UNICODETOOLS_JAVA_DIR
                    + "/org/unicode/props/UcdPropertyValues.java";

    //    private static class Locations {
    //        private static Set<String> files = addAll(new HashSet<String>(), new
    // File(SOURCE_DIR));
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

    static Map<String, PropName> lookup = new HashMap<String, PropName>();
    static Map<String, PropName> lookupMain = new TreeMap<String, PropName>();

    private static final Pattern PROPERTY_LONG_NAME =
            Pattern.compile("[A-Z]+[0-9]?[a-z]*(_[A-Z0-9]+[a-z]*)*");
    private static final Pattern PROPERTY_VALUE_LONG_NAME =
            Pattern.compile("[A-Z]+[0-9]?[a-z]*(_[A-Z0-9]+[a-z]*)*(\\d*)");
    private static final Pattern PROPER_CJK_LONG_NAME =
            Pattern.compile("(cj)?k[A-Z][a-z]*(_?[A-Z0-9][a-z]*)*");

    static class PropName implements Comparable<PropName> {
        final PropertyType propertyType;
        final String shortName;
        final String longName;
        final List<String> others;
        final Map<String, PropName> subnames = new TreeMap<String, PropName>();

        PropName(PropertyType type, OverrideChoice override, String... strings) {
            shortName = strings[0];
            longName = strings[1];
            propertyType =
                    longName.equals("Script_Extensions") ? PropertyType.Catalog : type; // HACK
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
            if (override != OverrideChoice.allow) {
                for (final String name : strings) {
                    if (lookup.containsKey(name)) {
                        throw new UnicodePropertyException("Duplicate propName");
                    }
                }
            }
            for (final String name : strings) {
                lookup.put(name, this);
            }
            lookupMain.put(longName, this);
        }

        private static String isProperLongName(
                String longName2, Pattern pattern, boolean allowCjk) {
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
            return "{"
                    + propertyType
                    + ",    "
                    + longName
                    + ",    "
                    + shortName
                    + ",    "
                    + others
                    + "}";
        }

        @Override
        public int compareTo(PropName arg0) {
            if (longName.contains("10")) {
                int debug = 0;
            }
            return COL.compare(longName, arg0.longName);
        }
    }

    static final RuleBasedCollator COL = (RuleBasedCollator) Collator.getInstance(Locale.ROOT);

    static {
        COL.setNumericCollation(true);
        COL.freeze();
    }

    private static final Comparator<String[]> ARRAY_SORT =
            new Comparator<String[]>() {
                @Override
                public int compare(String[] o1, String[] o2) {
                    int min = o1.length < o2.length ? o1.length : o2.length;
                    for (int i = 0; i < min; ++i) {
                        int diff = COL.compare(o1[i], o2[i]);
                        if (diff != 0) {
                            return diff;
                        }
                    }
                    return o1.length - o2.length;
                }
            };

    enum OverrideChoice {
        allow,
        disallow
    }

    public static void main(String[] args) throws IOException {

        final Map<PropName, Set<String[]>> values = new TreeMap<PropName, Set<String[]>>();

        addPropertyAliases(
                values,
                FileUtilities.in(
                        "",
                        Utility.getMostRecentUnicodeDataFile(
                                "PropertyAliases", ENUM_VERSION, true, true)),
                OverrideChoice.disallow);
        addPropertyAliases(
                values,
                FileUtilities.in(GenerateEnums.class, "ExtraPropertyAliases.txt"),
                OverrideChoice.allow);

        writeMainUcdFile();

        writeValueEnumFile(values);
        for (final String s : WARNINGS) {
            System.out.println(s);
        }

        System.out.println("lookupMain: " + lookupMain.size());
        System.out.println(CollectionUtilities.join(lookupMain.values(), "\n"));
    }

    public static String getNameStuff2(final String enumName) {
        return ";\n"
                + "        private final PropertyNames<"
                + enumName
                + "> names;\n"
                + "\n"
                + "        private "
                + enumName
                + "(String shortName, String... otherNames) {\n"
                + "            names = new PropertyNames<"
                + enumName
                + ">(\n"
                + "                "
                + enumName
                + ".class, this, shortName, otherNames);\n"
                + "        }\n"
                + "\n"
                + "        @Override\n"
                + "        public PropertyNames<"
                + enumName
                + "> getNames() {\n"
                + "            return names;\n"
                + "        }\n"
                + "\n"
                + "        @Override\n"
                + "        public String getShortName() {\n"
                + "            return names.getShortName();\n"
                + "        }\n"
                + "\n"
                + "        private static final NameMatcher<"
                + enumName
                + "> NAME_MATCHER = PropertyNames.getNameToEnums("
                + enumName
                + ".class);\n"
                + "\n"
                + "        public static "
                + enumName
                + " forName(String name) {\n"
                + "            return NAME_MATCHER.get(name);\n"
                + "        }\n"
                + "    }\n";
    }

    public static void writeValueEnumFile(Map<PropName, Set<String[]>> values) throws IOException {
        final PrintWriter output = FileUtilities.openUTF8Writer("", PROPERTY_VALUE_OUTPUT);
        output.println(
                "package org.unicode.props;\n\n"
                        + "import org.unicode.props.PropertyNames.NameMatcher;\n"
                        + "import org.unicode.props.PropertyNames.Named;\n"
                        + "\n"
                        + "/**\n"
                        + " * Machine-generated file for property values, produced by GenerateEnums.java from\n"
                        + " * PropertyValueAliases.txt and ExtraPropertyValueAliases.txt. The ordering of property value enums\n"
                        + " * is alphabetical (ASCII), but the order of the values for the enums is based on the order within\n"
                        + " * those two files with the ones in PropertyValueAliases coming first.\n"
                        + " */\n"
                        + "public class UcdPropertyValues {");

        // [Alpha, N, No, F, False]
        addPropertyValueAliases(
                values,
                FileUtilities.in(
                        "",
                        Utility.getMostRecentUnicodeDataFile(
                                "PropertyValueAliases", ENUM_VERSION, true, true)));
        addPropertyValueAliases(
                values, FileUtilities.in(GenerateEnums.class, "ExtraPropertyValueAliases.txt"));

        output.println(
                "\n    public enum Binary implements Named {\n"
                        + "        No(\"N\", \"F\", \"False\"),\n"
                        + "        Yes(\"Y\", \"T\", \"True\")"
                        + getNameStuff2("Binary")
                //                        ";\n"+
                //
                //                        "        private final PropertyNames<Binary> names;\n"+
                //                        "        private Binary (String shortName,
                // String...otherNames) {\n"+
                //                        "            names = new PropertyNames(Binary.class, this,
                // shortName, otherNames);\n"+
                //                        "        }\n"+
                //                        "        public PropertyNames<Binary> getNames() {\n"+
                //                        "            return names;\n"+
                //                        "        }\n"+
                //                        "    }\n"
                );

        for (final Entry<PropName, Set<String[]>> value : values.entrySet()) {
            final PropName propName = value.getKey();
            System.out.println("Writing:    " + propName.longName);
            if (propName.propertyType == PropertyType.Binary) {
                continue;
            }
            final Set<String[]> partList = value.getValue();
            if (partList.size() == 0) {
                output.println("        // " + propName.longName);
                continue;
            }
            output.println(
                    "    public enum " + (propName.longName + "_Values") + " implements Named {");
            final StringBuilder constants = new StringBuilder();
            boolean first = true;
            for (final String[] parts : partList) {
                String longName = parts[2];

                // HACK
                if (propName.shortName.equals("ccc")) {
                    longName = parts[3];
                }
                final String badName =
                        PropName.isProperLongName(longName, PROPERTY_VALUE_LONG_NAME, false);
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
                if (propName.longName.equals("Age") && longName.equals("V13_1")) {
                    output.println(", // TODO: there is no Unicode 13.1, see");
                    output.println("// https://github.com/unicode-org/unicodetools/issues/100");
                    first = true;
                }

                for (int i = 1; i < parts.length; ++i) {
                    final String otherName = parts[i];
                    if (i == 2
                            || otherName.equals("n/a")
                            || otherName.equals(longName)
                            || otherName.contains("-")
                            || otherName.charAt(0) < 'A') {
                        continue;
                    }
                    if (constants.length() != 0) {
                        constants.append(",");
                    }
                    constants.append("\n        " + otherName + "=" + longName);
                }
            }
            final String enumName = propName.longName;

            output.println(getNameStuff2(enumName + "_Values"));
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

    static Map<String, ValueCardinality> NAME2CARD = new HashMap<>();

    static {
        Splitter SEMICOLON = Splitter.on(";").trimResults();
        for (String line :
                FileUtilities.in(IndexUnicodeProperties.class, "IndexPropertyRegex.txt")) {
            line = line.trim();
            if (line.startsWith("$") || line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            List<String> parts = SEMICOLON.splitToList(line);
            // Bidi_Mirroring_Glyph ;        SINGLE_VALUED ;               $codePoint

            final String propertyName = parts.get(0).toLowerCase(Locale.ENGLISH);
            String multivalued;
            try {
                multivalued = parts.get(1);
            } catch (Exception e) {
                throw new ICUException("Bad line " + line, e);
            }
            switch (multivalued) {
                case "ORDERED":
                    NAME2CARD.put(propertyName, ValueCardinality.Ordered);
                    break;
                case "MULTI_VALUED":
                    NAME2CARD.put(propertyName, ValueCardinality.Unordered);
                    break;
                case "SINGLE_VALUED":
                    NAME2CARD.put(propertyName, ValueCardinality.Singleton);
                    break;
                case "EXTENSIBLE":
                    NAME2CARD.put(propertyName, ValueCardinality.Singleton);
                    break;
                default:
                    throw new UnicodePropertyException(
                            "IndexPropertyRegex: didn't expect " + multivalued);
            }
        }
    }

    public static void writeMainUcdFile() throws IOException {
        final PrintWriter output = FileUtilities.openUTF8Writer("", PROPERTY_FILE_OUTPUT);

        output.print("package org.unicode.props;\n\n");
        TreeSet<String> imports = new TreeSet<>();
        imports.add("import java.util.EnumSet;");
        imports.add("import java.util.Set;");
        imports.add("import org.unicode.props.PropertyNames.NameMatcher;");
        imports.add("import org.unicode.props.UcdPropertyValues.Binary;");
        for (final Entry<String, PropName> i : lookupMain.entrySet()) {
            final PropName pname = i.getValue();
            switch (pname.propertyType) {
                case Enumerated:
                case Catalog:
                    final String longName = pname.longName;
                    if (!pname.longName.equals(
                            "Script_Extensions")) { // exception, since uses Script_Values
                        imports.add(
                                "import org.unicode.props.UcdPropertyValues."
                                        + longName
                                        + "_Values;");
                    }
                    //                final ValueCardinality cardinality =
                    // NAME2CARD.get(longName.toLowerCase(Locale.ENGLISH));
                    //                if (true || cardinality == null || cardinality ==
                    // ValueCardinality.Singleton) {
                    //                    imports.add(longName);
                    //                }
                    break;
                default:
                    break;
            }
        }
        for (String s : imports) {
            output.println(s);
        }
        output.println();

        output.println(
                "/**\n"
                        + " * Machine-generated file for properties, produced by GenerateEnums.java from PropertyAliases.txt\n"
                        + " * and ExtraPropertyAliases.txt. The ordering of properties is first by category, then alphabetical\n"
                        + " * (ASCII order).\n"
                        + " */\n"
                        + "public enum "
                        + "UcdProperty"
                        + " {");
        Set<String> missingCardinality = new TreeSet<>();
        Set<String> extraCardinality = new TreeSet<>(NAME2CARD.keySet());

        for (final PropertyType pt : PropertyType.values()) {
            final int count = 0;
            output.println("\n        // " + pt);
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
                        classItem = // "UcdPropertyValues." +
                                "Binary.class";
                        break;
                    case Enumerated:
                    case Catalog:
                        classItem = // "UcdPropertyValues." +
                                ("Script_Extensions".equals(pname.longName)
                                                ? "Script"
                                                : pname.longName)
                                        + "_Values.class"; // HACK!
                        break;
                    default:
                        break; // leave classItem = null
                }
                writeOtherNames(
                        output, type, classItem, cardinality, pname.shortName, pname.others);
                output.print(",\n");
            }
        }
        if (!missingCardinality.isEmpty()) {
            System.err.println("No Cardinality for " + missingCardinality);
        }
        if (!missingCardinality.isEmpty()) {
            System.err.println("Extra Cardinality for " + extraCardinality);
        }
        output.println("        ;");
        output.println(
                "\n"
                        + "private final PropertyType type;\n"
                        + "    private final PropertyNames<UcdProperty> names;\n"
                        + "    // for enums\n"
                        + "    private final NameMatcher name2enum;\n"
                        + "    private final EnumSet enums;\n"
                        + "    private final Class enumClass;\n"
                        + "    private final ValueCardinality cardinality;\n"
                        + "    \n"
                        + "    private UcdProperty(PropertyType type, String shortName, String... otherNames) {\n"
                        + "        this.type = type;\n"
                        + "        names = new PropertyNames<UcdProperty>(UcdProperty.class, this, shortName, otherNames);\n"
                        + "        name2enum = null;\n"
                        + "        enums = null;\n"
                        + "        enumClass = null;\n"
                        + "        cardinality = ValueCardinality.Singleton;\n"
                        + "    }\n"
                        + "\n"
                        + "    private UcdProperty(\n"
                        + "            PropertyType type,\n"
                        + "            Class classItem,\n"
                        + "            ValueCardinality _cardinality,\n"
                        + "            String shortName,\n"
                        + "            String... otherNames) {\n"
                        + "        this.type = type;\n"
                        + "        names = new PropertyNames<UcdProperty>(UcdProperty.class, this, shortName, otherNames);\n"
                        + "        cardinality = _cardinality == null ? ValueCardinality.Singleton : _cardinality;\n"
                        + "        if (classItem == null) {\n"
                        + "            name2enum = null;\n"
                        + "            enums = null;\n"
                        + "            enumClass = null;\n"
                        + "        } else {\n"
                        + "            enums = EnumSet.allOf(classItem);\n"
                        + "            name2enum = PropertyNames.getNameToEnums(classItem);\n"
                        + "            enumClass = classItem;\n"
                        + "        }\n"
                        + "    }\n"
                        + "    \n"
                        + "    public ValueCardinality getCardinality() {\n"
                        + "        return cardinality;\n"
                        + "    }\n"
                        + "\n"
                        + "    public Class<Enum> getEnumClass() {\n"
                        + "        return enumClass;\n"
                        + "    }\n"
                        + "\n"
                        + "    public PropertyType getType() {\n"
                        + "        return type;\n"
                        + "    }\n"
                        + "\n"
                        + "    public PropertyNames<UcdProperty> getNames() {\n"
                        + "        return names;\n"
                        + "    }\n"
                        + "\n"
                        + "    public String getShortName() {\n"
                        + "        return names.getShortName();\n"
                        + "    }\n"
                        + "\n"
                        + "    public static UcdProperty forString(String name) {\n"
                        + "        return Numeric_Value.names.forString(name);\n"
                        + "    }\n"
                        + "\n"
                        + "    public Enum getEnum(String name) {\n"
                        + "        return name2enum == null ? null : name2enum.get(name);\n"
                        + "    }\n"
                        + "\n"
                        + "    public PropertyNames getEnumNames() {\n"
                        + "        return name2enum == null ? null : name2enum.getNames();\n"
                        + "    }\n"
                        + "\n"
                        + "    public Set<Enum> getEnums() {\n"
                        + "        return enums;\n"
                        + "    }");

        output.println("}");
        output.close();
    }

    public static void writeOtherNames(
            PrintWriter output,
            String type,
            String classItem,
            ValueCardinality cardinality,
            String shortName,
            List<String> otherNames) {
        output.print("(");
        // if (shortName != null) {
        output.print(type);
        if (classItem != null || cardinality != ValueCardinality.Singleton) {
            output.print(
                    ", "
                            + classItem
                            + ", "
                            + (cardinality == ValueCardinality.Singleton
                                    ? "null"
                                    : "ValueCardinality." + cardinality.toString()));
        }
        output.print(", \"" + shortName + "\"");
        for (final String otherName : otherNames) {
            output.print(", \"" + otherName + "\"");
        }
        output.print(")");
    }

    public static void addPropertyValueAliases(
            Map<PropName, Set<String[]>> values, Iterable<String> lines) {
        for (final String line : lines) {
            final String[] parts = FileUtilities.cleanSemiFields(line);
            if (parts == null) {
                continue;
            }
            final PropName propName = lookup.get(parts[0]);
            if (propName == null) {
                throw new UnicodePropertyException("Missing Prop Name in " + Arrays.asList(parts));
            }
            final Set<String[]> set = values.get(propName);
            set.add(parts);
            // System.out.println(propName.longName + "    " + Arrays.asList(parts));
        }
    }

    public static void addPropertyAliases(
            Map<PropName, Set<String[]>> values, Iterable<String> lines, OverrideChoice override) {
        final Matcher propType =
                Pattern.compile("#\\s+(\\p{Alpha}+)\\s+Properties\\s*").matcher("");
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
            final PropName propName = new PropName(type, override, parts);
            values.put(
                    propName,
                    propName.longName.equals("Age")
                            ? new TreeSet<>(ARRAY_SORT)
                            : new LinkedHashSet<>());
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
