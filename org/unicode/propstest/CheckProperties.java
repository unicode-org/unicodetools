package org.unicode.propstest;

import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Timer;
import org.unicode.cldr.util.With;
import org.unicode.draft.UnicodeDataOutput;
import org.unicode.draft.UnicodeDataOutput.ItemWriter;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyNames;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UnicodeSetUtilities;
import org.unicode.props.IndexUnicodeProperties.DefaultValueType;
import org.unicode.props.IndexUnicodeProperties.PropertyParsingInfo;
import org.unicode.props.PropertyNames.NameMatcher;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.utility.Utility;

import sun.text.normalizer.UTF16;

import com.ibm.icu.dev.util.ICUPropertyFactory;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.Tabber;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;

public class CheckProperties {
    private static final String LAST_RELEASE = Utility.searchPath[1];;
    private static final String JUNK = Utility.UCD_DIRECTORY; // force load

    private static final int DEBUG_CODE_POINT = 0x0600;

    private static final boolean LATEST_ICU = true;

    static LinkedHashSet<String> PROPNAMEDIFFERENCES = new LinkedHashSet<String>();
    static LinkedHashSet<String> SKIPPING = new LinkedHashSet<String>();
    static LinkedHashSet<String> NOT_IN_ICU = new LinkedHashSet<String>();

    enum Action {SHOW, COMPARE, ICU, EMPTY, INFO, SPACES, DETAILS, DEFAULTS, JSON, NAMES}
    enum Extent {SOME, ALL}

    static IndexUnicodeProperties latest;

    public static void main(String[] args) throws Exception {
        EnumSet<Action> actions = EnumSet.noneOf(Action.class);
        final EnumSet<UcdProperty> properties = EnumSet.noneOf(UcdProperty.class);
        Extent extent = null;
        String version = null;

        for (final String arg : args) {
            try {
                actions.add(Action.valueOf(arg.toUpperCase()));
                continue;
            } catch (final Exception e) {}
            try {
                extent = Extent.valueOf(arg.toUpperCase());
                continue;
            } catch (final Exception e) {}
            try {
                properties.add(UcdProperty.forString(arg));
                continue;
            } catch (final Exception e) {}
            if (arg.matches("\\d+\\.\\d+\\.\\d+")) {
                version = arg;
                continue;
            }
            throw new IllegalArgumentException("Illegal Argument: " + arg);
        }
        if (actions.size() == 0) {
            actions = EnumSet.of(Action.COMPARE);
        }
        if (extent == null) {
            extent = Extent.ALL;
        }
        if (version == null) {
            version = LAST_RELEASE;
        }

        final Timer total = new Timer();
        //        showValue(last, UcdProperty.kMandarin, '\u5427');
        //        showValue(last, UcdProperty.General_Category, '\u5427');

        latest = IndexUnicodeProperties.make(version);
        final IndexUnicodeProperties last = IndexUnicodeProperties.make(Utility.getPreviousUcdVersion(version));
        final UnicodeMap<String> gcLast = showValue(last, UcdProperty.General_Category, '\u00A7');

        //        showValue(latest, UcdProperty.General_Category, '\u00A7');
        //        showValue(latest, UcdProperty.kMandarin, '\u5427');

        final UnicodeSet ignore = new UnicodeSet();
        addAll(ignore, gcLast.getSet(null)); // separate for debugging
        addAll(ignore, gcLast.getSet(UcdPropertyValues.General_Category_Values.Unassigned.toString()));
        addAll(ignore, gcLast.getSet(UcdPropertyValues.General_Category_Values.Private_Use.toString()));
        addAll(ignore, gcLast.getSet(UcdPropertyValues.General_Category_Values.Surrogate.toString()));
        //addAll(ignore, gcLast.getSet("Cc"));

        final UnicodeSet retain = new UnicodeSet(ignore).complement().freeze();

        //        compare(UcdProperty.General_Category, last, latest, retain);
        //
        //        latest.show(UcdProperty.General_Category);

        final List<UcdProperty> values =
                extent == null ? new ArrayList(properties)
        : extent == Extent.ALL ? Arrays.asList(UcdProperty.values())
                : Arrays.asList(
                        //UcdProperty.General_Category,
                        UcdProperty.CJK_Radical,
                        UcdProperty.Indic_Matra_Category,
                        UcdProperty.Indic_Syllabic_Category,
                        UcdProperty.Jamo_Short_Name
                        // Bidi_Mirroring_Glyph
                        //                    UcdProperty.CJK_Radical,
                        //                    UcdProperty.Script_Extensions,
                        //                    UcdProperty.Emoji_DoCoMo,
                        //                    UcdProperty.Emoji_KDDI,
                        //                    UcdProperty.Emoji_SoftBank,
                        //                    UcdProperty.Name_Alias_Prov,
                        //                    UcdProperty.Named_Sequences,
                        //                    UcdProperty.Named_Sequences_Prov
                        );
        for (final Action action : actions) {
            switch(action) {
            case NAMES:
                for (final Entry<String, PropertyParsingInfo> entry : IndexUnicodeProperties.getFile2PropertyInfoSet().keyValueSet()) {
                    System.out.println(entry.getKey() + " ; " + entry.getValue());
                }
                break;
            case SHOW:
                for (final UcdProperty prop : values) {
                    show(latest, prop, actions.contains(Action.SPACES), false);
                }
                break;
            case SPACES:
                break;
            case DETAILS:
                for (final UcdProperty prop : values) {
                    show(latest, prop, actions.contains(Action.SPACES), true);
                }
                break;
            case COMPARE:
            {
                final Set<String> summary = new LinkedHashSet();
                for (final UcdProperty prop : values) {
                    compare(prop, last, latest, retain, summary);
                }
                showSummary(summary);
            }
            break;
            case ICU:
            {
                System.out.println("Property\tICU-Value\tDirect-Value\tChars-Affected");
                final Set<String> summary = new LinkedHashSet();
                for (final UcdProperty prop : values) {
                    compareICU(prop, LATEST_ICU ? latest : last, summary);
                }
                showSummary(summary);
            }
            break;
            case DEFAULTS:
                for (final UcdProperty prop : values) {
                    showDefaults(prop);
                }
                break;
            case EMPTY:
                for (final UcdProperty prop : values) {
                    checkEmpty(latest, prop);
                }
                break;
            case JSON: {
                for (final UcdProperty prop : values) {
                    System.out.println(prop);
                    writeJson(latest, prop);
                }
                break;
            }
            case INFO:
                final Tabber tabber = new Tabber.MonoTabber()
                .add(30, Tabber.LEFT)
                .add(30, Tabber.LEFT)
                .add(30, Tabber.LEFT);
                final Relation<String, String> sorted = Relation.of(new TreeMap<String,Set<String>>(), LinkedHashSet.class);
                final Set<UcdProperty> missingRegex = EnumSet.noneOf(UcdProperty.class);
                for (final UcdProperty prop : UcdProperty.values()) {
                    final PropertyParsingInfo propInfo = IndexUnicodeProperties.getPropertyInfo(prop);
                    if (propInfo.originalRegex == null) {
                        continue;
                    }
                    final String line = tabber.process(propInfo.property + " ;\t" + propInfo.getMultivalued() + " ;\t" + propInfo.originalRegex);
                    sorted.put(propInfo.originalRegex, line);
                }
                for (final Entry<String, String> regexLine : sorted.keyValueSet()) {
                    System.out.println(regexLine.getValue());
                }

                for (final UcdProperty prop : UcdProperty.values()) {
                    final PropertyParsingInfo propInfo = IndexUnicodeProperties.getPropertyInfo(prop);
                    System.out.println(propInfo);
                    if (propInfo.getRegex() == null) {
                        switch (prop.getType()) {
                        case Binary: case Catalog: case Enumerated: break;
                        default: missingRegex.add(prop);
                        }
                    }
                }
                System.out.println("\nMissing Regex");
                for (final UcdProperty prop : missingRegex) {
                    final PropertyParsingInfo propInfo = IndexUnicodeProperties.getPropertyInfo(prop);
                    System.out.println(
                            prop + " ;\t"
                                    + propInfo.getMultivalued() + " ;\t"
                                    + propInfo.getRegex()
                            );
                }
                break;
            }
        }

        showInfo("No Differences", SKIPPING);
        showInfo("Property Enum Canonical Form wrong", PROPNAMEDIFFERENCES);
        showInfo("Not In ICU", NOT_IN_ICU);
        showInfo("Cache File Sizes", latest.getCacheFileSize().entrySet());

        final Set<Entry<UcdProperty, Set<String>>> dataLoadingErrors = IndexUnicodeProperties.getDataLoadingErrors().keyValuesSet();
        if (dataLoadingErrors.size() != 0) {
            System.out.println("Data loading errors: " + dataLoadingErrors.size());
            for (final Entry<UcdProperty, Set<String>> s : dataLoadingErrors) {
                System.out.println("\t" + s.getKey());
                int max = 100;
                for (final String value : s.getValue()) {
                    System.out.println("\t\t" + value);
                    if (--max < 0) {
                        System.out.println("…");
                        break;
                    }
                }
            }
        }

        final Set<String> latestFiles = latest.getFileNames();
        final File dir = new File(Utility.UCD_DIRECTORY);
        final List<File> result = new ArrayList<File>();
        checkFiles(latestFiles, dir, result);
        showInfo("Files Not Read", result);

        total.stop();
        System.out.println(total.toString());
    }

    private static void showSummary(Set<String> summary) {
        for (final String s : summary) {
            System.out.println(s);
        }
    }

    private static void writeJson(IndexUnicodeProperties latest, UcdProperty prop) {
        try {
            final UnicodeMap<String> map = latest.load(prop);
            final PrintWriter writer = new PrintWriter(System.out);
            writer.write('[');
            final JsonDataOutput jsonDataOutput = new JsonDataOutput(writer);
            UnicodeDataOutput.writeUnicodeMap(map, JSON_STRING_WRITER, jsonDataOutput);
            writer.write("]\n");
            writer.flush();
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static String jsonQuote(String source) {
        final StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < source.length(); ++i) { // safe, only care about ASCII
            final char ch = source.charAt(i);
            switch (ch) {
            case '\b': result.append('\b'); break;
            case '\f': result.append('\f'); break;
            case '\n': result.append('\n'); break;
            case '\r': result.append('\r'); break;
            case '\t': result.append('\t'); break;
            case '"': case '\\': result.append('\\').append(ch); break;
            default: result.append(ch); break;
            }
        }
        return result.append('"').toString();
    }
    public static class JsonStringWriter extends ItemWriter<String> {
        @Override
        public void write(DataOutput out, String item) throws IOException {
            out.writeChars(jsonQuote(item));
        }
    }
    static JsonStringWriter JSON_STRING_WRITER = new JsonStringWriter();
    public static class JsonDataOutput implements DataOutput {
        PrintWriter writer;

        JsonDataOutput(PrintWriter writer) {
            this.writer = writer;
        }

        @Override
        public void write(int arg0) throws IOException {
            writer.write(arg0);
            writer.write(",\n");
        }

        @Override
        public void write(byte[] arg0) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void write(byte[] arg0, int arg1, int arg2) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeBoolean(boolean arg0) throws IOException {
            writer.print(arg0);
            writer.write(",\n");
        }

        @Override
        public void writeByte(int arg0) throws IOException {
            writer.write(arg0);
            writer.write(",\n");
        }

        @Override
        public void writeBytes(String arg0) throws IOException {
            writer.write(arg0);
            writer.write(",\n");
        }

        @Override
        public void writeChar(int arg0) throws IOException {
            writer.write(arg0);
            writer.write(",\n");
        }

        @Override
        public void writeChars(String arg0) throws IOException {
            writer.write(arg0);
            writer.write(",\n");
        }

        @Override
        public void writeDouble(double arg0) throws IOException {
            writer.print(arg0);
            writer.write(",\n");
        }

        @Override
        public void writeFloat(float arg0) throws IOException {
            writer.print(arg0);
            writer.write(",\n");
        }

        @Override
        public void writeInt(int arg0) throws IOException {
            writer.print(arg0);
            writer.write(",\n");
        }

        @Override
        public void writeLong(long arg0) throws IOException {
            writer.print(arg0);
            writer.write(",\n");
        }

        @Override
        public void writeShort(int arg0) throws IOException {
            writer.print(arg0);
            writer.write(",\n");
        }

        @Override
        public void writeUTF(String arg0) throws IOException {
            writer.write(arg0);
            writer.write(",\n");
        }
    }


    public static <T> void showInfo(String title, Collection<T> collection) {
        if (collection.size() != 0) {
            System.out.println(title + ":\t" + collection.size());
            for (final T s : collection) {
                String display;
                if (s instanceof Entry) {
                    final Entry e = (Entry) s;
                    display = e.getKey().toString() + "\t" + e.getValue().toString();
                } else {
                    display = s.toString();
                }
                System.out.println("\t" + display);
            }
        }
    }

    private static void showDefaults(UcdProperty prop) {
        final PropertyParsingInfo info = IndexUnicodeProperties.getPropertyInfo(prop);
        latest.load(prop); // need to do this to get the @missing!
        System.out.println(
                prop
                + ";\t" + prop.getType()
                + ";\t" + IndexUnicodeProperties.getPropertyStatus(prop)
                + ";\t" + info.getMultivalued()
                + ";\t" + info.getDefaultValue()
                + ";\t" + (info.originalRegex == null ? "<enum>" : info.originalRegex)
                );
    }

    public static void checkEmpty(IndexUnicodeProperties latest, UcdProperty prop) {
        final UnicodeMap<String> map = latest.load(prop);
        final String defaultValue = IndexUnicodeProperties.getDefaultValue(prop);
        final UnicodeSet nullElements = map.getSet(null);

        final UnicodeSet empty = map.getSet("");
        if (defaultValue.isEmpty()) {
            empty.addAll(nullElements);
        }
        if (empty.size() != 0) {
            System.out.println("Empty: " + prop + "\t" + abbreviate(empty, 100, false));
        }

        final String no_value_constant = IndexUnicodeProperties.DefaultValueType.LITERAL.toString();
        final UnicodeSet no_value = map.getSet(no_value_constant);
        if (no_value_constant.equals(defaultValue)) {
            no_value.addAll(nullElements);
        }
        if (no_value.size() != 0) {
            System.out.println("No_Value: " + prop + "\t" + abbreviate(no_value, 100, false));
        }
        //        if (nullElements.size() != 0 && (defaultValue == null || defaultValue.equals(no_value_constant) || defaultValue.isEmpty())) {
        //            System.out.println("Null: " + prop + "\t" + defaultValue + "\t" + abbreviate(nullElements, 100, false));
        //        }
    }

    private static void compareICU(UcdProperty prop, IndexUnicodeProperties direct, Set<String> summary) {
        PropertyNames<UcdProperty> names = prop.getNames();

        if (prop == UcdProperty.Unicode_1_Name) {
            NOT_IN_ICU.add(prop.toString());
            return;
        }

        final ICUPropertyFactory propFactory = ICUPropertyFactory.make();
        final UnicodeProperty icuProp = propFactory.getProperty(prop.toString());
        if (icuProp == null) {
            NOT_IN_ICU.add(prop.toString());
            return;
        }
        final UnicodeMap<String> icuMap = icuProp.getUnicodeMap();
        if (prop == UcdProperty.Numeric_Value) {
            icuMap.setMissing("NaN");
        }

        final UnicodeMap<String> directMap = direct.load(prop);
        showChanges(prop, new UnicodeSet("[^[:cn:][:co:][:cs:]]"), null, icuMap, direct, directMap, summary);
    }

    private static void addAll(UnicodeSet toSet, UnicodeSet set) {
        if (set.contains('\u5427')) {
            final int y = 3;
        }
        toSet.addAll(set);
    }

    public static UnicodeMap<String> showValue(IndexUnicodeProperties last, UcdProperty ucdProperty, int codePoint) {
        final UnicodeMap<String> gcLast = last.load(ucdProperty);
        System.out.println(last.getUcdVersion() + ", " + ucdProperty + "(" + Utility.hex(codePoint) + ")=" + gcLast.get(codePoint));
        return gcLast;
    }

    public static List<File> checkFiles(Set<String> latestFiles, File dir, List<File> result) throws IOException {
        for (final File file : dir.listFiles()) {
            final String canonical = file.getCanonicalPath();
            if (file.isDirectory()) {
                checkFiles(latestFiles, file, result);
                continue;
            } else {
                final String fileName = file.toString();
                if (latestFiles.contains(canonical)
                        || !canonical.endsWith(".txt")
                        || fileName.contains("Test")
                        || fileName.contains("NamesList")
                        || fileName.contains("NormalizationCorrections")
                        || fileName.contains("PropertyValueAliases")
                        || fileName.contains("PropertyAliases")
                        || fileName.contains("ReadMe")
                        || fileName.contains("Index")
                        || fileName.contains("Derived")
                        ) {
                    continue;
                }
            }
            result.add(file);
        }
        return result;
    }

    private static void compare(UcdProperty prop, IndexUnicodeProperties last, IndexUnicodeProperties latest, UnicodeSet retain, Set<String> summary) {
        final UnicodeMap<String> lastMap = last.load(prop);
        final UnicodeMap<String> latestMap = latest.load(prop);
        showChanges(prop, retain, last, lastMap, latest, latestMap, summary);
    }

    public static void showChanges(UcdProperty prop, UnicodeSet retain,
            IndexUnicodeProperties last, UnicodeMap<String> lastMap,
            IndexUnicodeProperties latest, UnicodeMap<String> latestMap, Set<String> summary) {
        // TODO handle strings in maps
        final UnicodeMap<String> changes = new UnicodeMap<String>();
        final UnicodeSet newChars = new UnicodeSet(retain);
        final Set<String> strings = new TreeSet<String>();
        strings.addAll(UnicodeSetUtilities.getMulticharacterStrings(lastMap.keySet()));
        strings.addAll(UnicodeSetUtilities.getMulticharacterStrings(latestMap.keySet()));
        for (final String s : strings) {
            if (retain.containsAll(s)) {
                newChars.add(s);
            }
        }
        for (final String string : newChars) {
            if (string.codePointAt(0) == DEBUG_CODE_POINT) {
                final int x = 0;
            }
            final String lastValue = lastMap.get(string);
            final String latestValue = latestMap.get(string);
            captureChanges(prop, string, last, lastValue, latest, latestValue, changes);
        }
        if (changes.size() == 0) {
            SKIPPING.add(prop.toString());
            return;
        }
        summary.add("\n" + prop + "\t" + IndexUnicodeProperties.getPropertyStatusSet(prop) +
                "\ttotal:\t" + changes.size());
        int limit = 10;

        for (final String value : new TreeSet<String>(changes.values())) {
            if (--limit < 0) {
                System.out.println("\t\t\t\t…");
                break;
            }
            final UnicodeSet chars = changes.getSet(value);
            if (chars.size() == 1) {
                System.out.println(prop + "\t" + value
                        //+ "\t" + FIX_INVISIBLES.transform(chars.toPattern(false))
                        + "\tsubtotal:\t" + chars.size()
                        + "\t" + getHexAndName(chars.iterator().next()));
                continue;
            }
            System.out.print(prop + "\t" + value
                    //+ "\t" + FIX_INVISIBLES.transform(chars.toPattern(false))
                    + "\tsubtotal:\t" + chars.size()
                    + "\t\t\t" + abbreviate(chars, 50, false)
                    );
            System.out.println();
            int nameLimit = 5;
            for (final String s : chars) {
                if (--nameLimit < 0) {
                    System.out.println("\t\t\t\t\t\t…");
                    break;
                }
                System.out.println("\t\t\t\t\t\t" + getHexAndName(s));
            }
        }
    }

    private static String getHexAndName(String s) {
        return "U+" + Utility.hex(s, " U+") + "\t( " + fixInvisibles(s) + " )\t" + getName(s, " ");
    }

    private static String getName(CharSequence s, String separator) {
        final StringBuilder result = new StringBuilder();
        boolean first = true;
        for (final int cp : With.codePointArray(s)) {
            if (first) {
                first = false;
            } else {
                result.append(" ");
            }
            final String string = UTF16.valueOf(cp);
            String name = latest.getResolvedValue(UcdProperty.Name, string);
            if (name == null) {
                name = latest.getResolvedValue(UcdProperty.Name_Alias, string);
                if (name == null) {
                    name = "<no name>";
                }
            }
            result.append(name);
        }
        return result.toString();
    }

    public static void captureChanges(UcdProperty prop, String codepoint,
            IndexUnicodeProperties last, String lastValue,
            IndexUnicodeProperties latest, String latestValue,
            UnicodeMap<String> changes) {
        // if last == null, then it is an ICU property
        lastValue = IndexUnicodeProperties.getResolvedValue(last, prop, codepoint, lastValue);
        latestValue = IndexUnicodeProperties.getResolvedValue(latest, prop, codepoint, latestValue);
        if (UnicodeProperty.equals(lastValue, latestValue)) {
            return;
        }
        switch (prop.getType()) {
        case Numeric:
            if (approximatelyEqual(numericValue(lastValue), numericValue(latestValue), 0.0000001d)) {
                return;
            }
            break;
        case Catalog: case Enumerated:
            if (prop == UcdProperty.Age) {
                final int x = 0; // debug point
            }
            if (PropertyNames.NameMatcher.matches(lastValue, latestValue)) {
                PROPNAMEDIFFERENCES.add(prop + "\t«" + abbreviate(lastValue, 50, true) + "»\t≠\t«" + abbreviate(latestValue, 50, true) + "»");
                return;
            }
            break;

        }
        changes.put(codepoint, "«" + abbreviate(lastValue, 50, true) + "»\t≠\t«" + abbreviate(latestValue, 50, true) + "»");
    }

    private static Double numericValue(String a) {
        if (a == null || a == "<none>") {
            return null;
        }
        final int slashPos = a.indexOf('/');
        if (slashPos >= 0) {
            return Double.parseDouble(a.substring(0,slashPos)) / Double.parseDouble(a.substring(slashPos+1));
        }
        return Double.parseDouble(a);
    }

    private static boolean approximatelyEqual(Double a, Double b, Double epsilon) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return (a >= b - epsilon || a <= b + epsilon);
    }

    public static String getDisplayValue(String value) {
        return value == null ? "null" : value.isEmpty() ? "empty" : value;
    }

    static Transliterator FIX_INVISIBLES;
    static Transliterator FIX_NON_ASCII8;
    static UnicodeSet INVISIBLES;

    static String fixInvisibles(String source) {
        if (FIX_INVISIBLES == null) {
            FIX_INVISIBLES = Transliterator.createFromRules("ID", "(" +
                    getInvisibles() +
                    ") > ❮&hex/plain($1)❯ ;", Transliterator.FORWARD);
        }
        return FIX_INVISIBLES.transform(source);
    }

    public static UnicodeSet getInvisibles() {
        if (INVISIBLES == null) {
            final UnicodeMap<String> gc = latest.load(UcdProperty.General_Category);
            final UnicodeMap<String> di = latest.load(UcdProperty.Default_Ignorable_Code_Point);
            INVISIBLES = new UnicodeSet(gc.getSet(UcdPropertyValues.General_Category_Values.Control.toString()))
            //.addAll(gc.getSet(UcdPropertyValues.General_Category_Values.Format.toString()))
            .addAll(gc.getSet(UcdPropertyValues.General_Category_Values.Surrogate.toString()))
            .addAll(gc.getSet(UcdPropertyValues.General_Category_Values.Private_Use.toString()))
            .addAll(di.getSet(UcdPropertyValues.Binary.Yes.toString()))
            .freeze();
        }
        return INVISIBLES;
    }
    public static String fixNonAscii(String charString) {
        if (FIX_NON_ASCII8 == null) {
            FIX_NON_ASCII8 = Transliterator.createFromRules("ID",
                    "(" +
                            getInvisibles() +
                            ") > ❮&hex/plain($1)❯ ;" +
                            "([^\\u0000-\\u00FF\\u2026]) > $1❮&hex/plain($1)❯ ;",
                            Transliterator.FORWARD);
        }
        return FIX_NON_ASCII8.transform(charString);
    }

    public static String abbreviate(UnicodeSet chars, int maxLength, boolean showNonAscii) {
        return abbreviate(chars.toPattern(false), maxLength, showNonAscii);
    }

    public static String abbreviate(String charString, int maxLength, boolean showNonAscii) {
        charString = getDisplayValue(charString);
        if (charString.length() > maxLength) {
            charString = charString.substring(0,maxLength) + "…";
        }
        if (showNonAscii) {
            final String alt = fixNonAscii(charString);
            if (!alt.equals(charString)) {
                charString = alt;
            }
        } else {
            final String alt = fixInvisibles(charString);
            if (!alt.equals(charString)) {
                charString = alt;
            }
        }
        return charString;
    }

    public static void show(IndexUnicodeProperties iup, UcdProperty prop, boolean onlySpaces, boolean details) {
        final Timer timer = new Timer();
        System.out.println(prop);
        timer.start();
        final UnicodeMap<String> map = iup.load(prop);
        timer.stop();
        Collection<String> values = map.values();
        if (onlySpaces) {
            final LinkedHashSet<String> spaceValues = new LinkedHashSet();
            for (final String value : values) {
                if (value.contains(" ")) {
                    spaceValues.add(value);
                }
            }
            if (spaceValues.size() == 0) {
                return;
            }
            values = spaceValues;
        }
        final String sample = abbreviate(values.toString(), 150, false);
        System.out.println(prop + "\ttime:\t" + timer.getDuration() + "\tcodepoints:\t" + map.size() + "\tvalues:\t" + values.size() + "\tsample:\t" + sample);
        if (details) {
            int maxCodepointLength = 0;
            final List<R2<String, String>> list = new ArrayList<R2<String, String>>();
            final String defaultValue = IndexUnicodeProperties.getDefaultValue(prop);
            for (final EntryRange<String> entryRange : map.entryRanges()) {
                if (entryRange.value == defaultValue) {
                    continue;
                }
                String codepoints = null;
                if (entryRange.string != null) {
                    codepoints = Utility.hex(entryRange.string);
                } else {
                    codepoints = Utility.hex(entryRange.codepoint);
                    if (entryRange.codepoint != entryRange.codepointEnd) {
                        codepoints = codepoints + ".." + Utility.hex(entryRange.codepointEnd);
                    }
                }
                maxCodepointLength = Math.max(maxCodepointLength, codepoints.length());
                String value = entryRange.value;
                final Enum item = prop.getEnum(value);
                if (item != null) {
                    final NameMatcher x = PropertyNames.getNameToEnums(item.getClass());
                    final PropertyNames enumNames = x.getNames();
                    value = enumNames.getShortName();
                }
                final R2<String, String> row = Row.of(codepoints, value);
                list.add(row);
            }
            final String shortName = prop.getNames().getShortName();
            System.out.println("# @missing 0000..10FFFF; " + shortName + "; " + defaultValue);
            for (final R2<String, String> entry : list) {
                final String codepoints = entry.get0();
                System.out.println(
                        codepoints
                        + "; "
                        + Utility.repeat(" ", maxCodepointLength - codepoints.length())
                        + shortName
                        + "; "
                        + entry.get1());
            }
        }
        //        for (String value : map.getAvailableValues()) {
        //            System.out.println("\t" + value + " " + map.getSet(value));
        //        }
    }
    //    public PropertyNames getEnumNames() {
    //        return name2enum == null ? null : name2enum.getNames();
    //    }

}

