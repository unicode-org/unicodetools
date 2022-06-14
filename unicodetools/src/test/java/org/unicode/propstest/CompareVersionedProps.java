package org.unicode.propstest;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyType;
import org.unicode.props.UcdProperty;
import org.unicode.props.ValueCardinality;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiOrder;

public class CompareVersionedProps {

    //    public static String TEST1 = Utility.fromHex("1F9D1 1F3FD 200D 1F3EB");
    //    public static String TEST2 = Utility.fromHex("1F9D1 1F3FE 200D 1F3EB");
    //    static {
    //        System.out.println("hex: " + Utility.hex(TEST1));
    //        IndexUnicodeProperties iup = IndexUnicodeProperties.make("12.1");
    //        UnicodeMap<String> map = iup.load(UcdProperty.Emoji_Zwj_Sequence);
    //        System.out.println(map.get(TEST1));
    //        System.out.println(map.get(TEST2));
    //    }

    // TODO add better command line arguments

    public static void main(String[] args) {
        Matcher propFilter = null;
        Map<VersionInfo, IndexUnicodeProperties> versionToIups = new TreeMap<>();

        // first argument is pattern; remaining are version numbers

        for (String arg : args) {
            if (propFilter == null) {
                System.out.println("Property Filter:\t" + arg);
                propFilter = Pattern.compile(arg).matcher(arg);
            } else {
                VersionInfo v = VersionInfo.getInstance(arg);
                System.out.println("Version:\t" + v.getVersionString(2, 4));
                versionToIups.put(v, IndexUnicodeProperties.make(v));
            }
        }

        EmojiOrder order = EmojiOrder.of(Emoji.VERSION_BETA);

        if (propFilter == null || versionToIups.size() < 2) {
            throw new IllegalArgumentException(
                    "Command line: <property name regex filter> versionA, versionB, ...");
        }

        List<VersionInfo> versionList = new ArrayList<>(versionToIups.keySet());
        VersionInfo mostRecent = versionList.get(versionList.size() - 1);

        for (UcdProperty prop : UcdProperty.values()) {
            boolean gotMatch = false;
            for (String name : prop.getNames().getAllNames()) {
                if (propFilter.reset(name).find()) {
                    gotMatch = true;
                }
            }
            if (!gotMatch) {
                continue;
            }

            Map<VersionInfo, UnicodeMap<String>> maps = new LinkedHashMap<>();
            Set<String> values = new TreeSet<>();
            for (Entry<VersionInfo, IndexUnicodeProperties> versionAndIups :
                    versionToIups.entrySet()) {
                UnicodeMap<String> map = versionAndIups.getValue().load(prop);
                maps.put(versionAndIups.getKey(), map);
                values.addAll(map.getAvailableValues());
            }

            // find differences

            UnicodeSet allDiffs = new UnicodeSet();
            for (String value : values) {
                UnicodeSet intersection = new UnicodeSet();
                UnicodeSet union = new UnicodeSet();
                boolean first = true;
                for (UnicodeMap<String> map : maps.values()) {
                    UnicodeSet uset = map.getSet(value);
                    if (first) {
                        intersection.addAll(map);
                        first = false;
                    } else {
                        intersection.retainAll(uset);
                    }
                    union.addAll(uset);
                }
                union.removeAll(intersection);
                allDiffs.addAll(union);
            }
            if (allDiffs.isEmpty()) {
                System.out.println(prop + "\tALL EQUAL");
                continue;
            }

            // show differences
            System.out.println(prop + "\tDifferences:\t" + allDiffs.size());
            Set<String> sorted = allDiffs.addAllTo(new TreeSet<>(order.codepointCompare));
            PropertyType type = prop.getType();
            ValueCardinality cardinality = prop.getCardinality();
            //            if (cardinality == ValueCardinality.Unordered) {
            //                // compare everything to latest
            //                for (Entry<VersionInfo, UnicodeMap<String>> map : maps.entrySet()) {
            //                }
            //            } else {
            for (String item : sorted) {
                for (Entry<VersionInfo, UnicodeMap<String>> map : maps.entrySet()) {
                    String value = map.getValue().get(item);
                    if (value == null) {
                        continue;
                    }
                    VersionInfo version = map.getKey();
                    String prefix = show(version, mostRecent, prop, type, item, value);
                }
            }
            //            }
            System.out.flush();
        }
    }

    private static String show(
            VersionInfo version,
            VersionInfo mostRecent,
            UcdProperty prop,
            PropertyType type,
            String item,
            String value) {
        String prefix = version.equals(mostRecent) ? "➕" : "➖";
        String reduced = EmojiData.removeEmojiVariants(item);
        if (type == PropertyType.Binary) {
            if (value.contentEquals("Yes")) {
                System.out.println(
                        prefix
                                + "\t"
                                + Utility.hex(item)
                                + " ;\t"
                                + "vendor_"
                                + Utility.hex(reduced, "_").toLowerCase(Locale.ROOT)
                                + ".png"
                                + " ;\t"
                                + prop
                                + "\t# "
                                + version.getVersionString(2, 2)
                                + " "
                                + item
                                + " "
                                + EmojiData.EMOJI_DATA_BETA.getName(item));
            }
        } else {
            System.out.println(
                    prefix
                            + "\t"
                            + Utility.hex(item)
                            + " ;\t"
                            + prop
                            + " ;\t"
                            + value
                            + "\t# "
                            + version.getVersionString(2, 2)
                            + " "
                            + item
                            + " "
                            + EmojiData.EMOJI_DATA_BETA.getName(item));
        }
        return prefix;
    }
}
