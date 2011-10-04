package org.unicode.props;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.unicode.cldr.util.Timer;
import org.unicode.props.CheckProperties.Action;
import org.unicode.props.IndexUnicodeProperties.PropertyInfo;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

import sun.text.normalizer.UTF16;

import com.ibm.icu.dev.test.util.ICUPropertyFactory;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class CheckProperties {
    enum Action {SHOW, COMPARE, CHECK}
    enum Extent {SOME, ALL}

    public static void main(String[] args) throws Exception {
        EnumSet<Action> actions = EnumSet.noneOf(Action.class);
        Extent extent = Extent.SOME;
        for (String arg : args) {
            try {
                actions.add(Action.valueOf(arg.toUpperCase()));
                continue;
            } catch (Exception e) {}
            try {
                extent = Extent.valueOf(arg.toUpperCase());
                continue;
            } catch (Exception e) {}
        }
        if (actions.size() == 0) actions = EnumSet.of(Action.CHECK);

        Timer total = new Timer();
        for (Entry<String, PropertyInfo> entry : IndexUnicodeProperties.getFile2PropertyInfoSet().keyValueSet()) {
            if (IndexUnicodeProperties.SHOW_PROP_INFO) System.out.println(entry.getKey() + " ; " + entry.getValue());
        }
        IndexUnicodeProperties last = IndexUnicodeProperties.make("6.0.0");
        UnicodeMap<String> gcLast = showValue(last, UcdProperty.General_Category, '\u00A7');
        //        showValue(last, UcdProperty.kMandarin, '\u5427');
        //        showValue(last, UcdProperty.General_Category, '\u5427');

        IndexUnicodeProperties latest = IndexUnicodeProperties.make(Default.ucdVersion());
        //        showValue(latest, UcdProperty.General_Category, '\u00A7');
        //        showValue(latest, UcdProperty.kMandarin, '\u5427');

        UnicodeSet ignore = new UnicodeSet();
        addAll(ignore, gcLast.getSet(null)); // separate for debugging
        addAll(ignore, gcLast.getSet("Cn"));
        addAll(ignore, gcLast.getSet("Co"));
        //addAll(ignore, gcLast.getSet("Cc"));
        addAll(ignore, gcLast.getSet("Cs"));

        UnicodeSet retain = new UnicodeSet(ignore).complement().freeze();

        //        compare(UcdProperty.General_Category, last, latest, retain);
        //
        //        latest.show(UcdProperty.General_Category);

        List<UcdProperty> values = extent == Extent.ALL ? Arrays.asList(UcdProperty.values()) : 
            Arrays.asList(
                    UcdProperty.Lowercase_Mapping
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
        for (Action action : actions) {
            switch(action) { 
            case SHOW:
                for (UcdProperty prop : values) {
                    show(latest, prop);
                }
                break;
            case COMPARE:
                for (UcdProperty prop : values) {
                    compare(prop, last, latest, retain);
                }
                break;
            case CHECK:
                System.out.println("Property\tICU-Value\tDirect-Value\tChars-Affected");
                for (UcdProperty prop : values) {
                    compareICU(prop, last);
                }
                break;
            }
        }
        for (String s : SKIPPING) {
            System.out.println(s);
        }
        Set<String> latestFiles = latest.fileNames;
        File dir = new File("/Users/markdavis/Documents/workspace/DATA/UCD/6.1.0-Update");
        checkFiles(latestFiles, dir);
        total.stop();
        System.out.println(total.toString());
    }

    static ArrayList<String> SKIPPING = new ArrayList<String>();
    
    private static void compareICU(UcdProperty prop, IndexUnicodeProperties direct) {
        ICUPropertyFactory propFactory = ICUPropertyFactory.make();
        UnicodeProperty icuProp = propFactory.getProperty(prop.toString());
        if (icuProp == null) {
            SKIPPING.add("Property not in ICU: " + prop);
            return;
        }
        final UnicodeMap<String> icuMap = icuProp.getUnicodeMap();
        UnicodeMap<String> directMap = direct.load(prop);
        showChanges(prop, new UnicodeSet("[^[:cn:][:co:][:cs:]]"), null, icuMap, direct, directMap);
    }

    private static void addAll(UnicodeSet toSet, UnicodeSet set) {
        if (set.contains('\u5427')) {
            int y = 3;
        }
        toSet.addAll(set);
    }

    public static UnicodeMap<String> showValue(IndexUnicodeProperties last, UcdProperty ucdProperty, int codePoint) {
        UnicodeMap<String> gcLast = last.load(ucdProperty);
        System.out.println(last.ucdVersion + ", " + ucdProperty + "(" + Utility.hex(codePoint) + ")=" + gcLast.get(codePoint));
        return gcLast;
    }

    public static void checkFiles(Set<String> latestFiles, File dir) throws IOException {
        for (File file : dir.listFiles()) {
            String canonical = file.getCanonicalPath();
            if (file.isDirectory()) {
                checkFiles(latestFiles, file);
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
            System.out.println("Not read for properties: " + file);
        }
    }

    private static void compare(UcdProperty prop, IndexUnicodeProperties last, IndexUnicodeProperties latest, UnicodeSet retain) {
        UnicodeMap<String> lastMap = last.load(prop);
        UnicodeMap<String> latestMap = latest.load(prop);
        showChanges(prop, retain, last, lastMap, latest, latestMap);
    }

    public static void showChanges(UcdProperty prop, UnicodeSet retain, 
            IndexUnicodeProperties last, UnicodeMap<String> lastMap, 
            IndexUnicodeProperties latest, UnicodeMap<String> latestMap) {
        // TODO handle strings in maps
        UnicodeMap<String> changes = new UnicodeMap<String>();
        for (UnicodeSetIterator it = new UnicodeSetIterator(retain); it.next();) {
            String lastValue = lastMap.get(it.codepoint);
            String latestValue = latestMap.get(it.codepoint);
            captureChanges(prop, it.codepoint, last, lastValue, latest, latestValue, changes);
        }
        if (changes.size() == 0) {
            SKIPPING.add(prop + "\tNO_CHANGES");
            return;
        }
        int limit = 30;
        for (String value : new TreeSet<String>(changes.values())) {
            final UnicodeSet chars = changes.getSet(value);
            String charString = abbreviate(chars.toString(), 50, false);
            System.out.println(prop + "\t" + value + "\t" + FIX_INVISIBLES.transform(chars.toPattern(false)) + "\t" + charString);
            if (--limit < 0) {
                System.out.println("\t\tand more");
                break;
            }
        }
    }

    public static void captureChanges(UcdProperty prop, int codepoint, 
            IndexUnicodeProperties last, String lastValue, IndexUnicodeProperties latest, String latestValue, UnicodeMap<String> changes) {
        lastValue = IndexUnicodeProperties.getResolvedValue(last, prop, codepoint, lastValue);
        latestValue = IndexUnicodeProperties.getResolvedValue(latest, prop, codepoint, latestValue);
        if (UnicodeProperty.equals(lastValue, latestValue)) {
            return;
        }
        changes.put(codepoint, abbreviate(lastValue, 50, true) + "\t≠\t" + abbreviate(latestValue, 50, true));
    }

    public static String getDisplayValue(String value) {
        return (value == null || value.isEmpty()) ? "∅" : value;
    }

    static final Transliterator FIX_INVISIBLES = Transliterator.createFromRules("ID", "([[:c:][:di:]]) > &hex($1);", Transliterator.FORWARD);
    static final Transliterator FIX_NON_ASCII8 = Transliterator.createFromRules("ID", "([[^\\u0000-\\u00FF][:c:][:di:]]) > &hex($1);", Transliterator.FORWARD);

    public static String abbreviate(String charString, int maxLength, boolean showNonAscii) {
        charString = getDisplayValue(charString);
        if (charString.length() > maxLength) {
            charString = charString.substring(0,50) + "…";
        }
        if (showNonAscii) {
            String alt = FIX_NON_ASCII8.transform(charString);
            if (!alt.equals(charString)) {
                charString = alt;
            }
        }
        return charString;
    }

    public static void show(IndexUnicodeProperties iup, UcdProperty prop) {
        Timer timer = new Timer();
        System.out.println(prop);
        timer.start();
        UnicodeMap<String> map = iup.load(prop);
        timer.stop();
        final Collection<String> values = map.values();
        String sample = abbreviate(values.toString(), 20, false);
        System.out.println("\ttime: " + timer + "\tcodepoints: " + map.size() + "\tvalues: " + values.size() + "\tsample: " + sample);
        //        for (String value : map.getAvailableValues()) {
        //            System.out.println("\t" + value + " " + map.getSet(value));
        //        }
    }
}

