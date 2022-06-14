package org.unicode.propstest;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.cldr.util.Counter;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyStatus;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.props.VersionToAge;

public class PropertyGrowth {
    public static void main(String[] args) {
        showScriptGrowth();
        showPropGrowth();
    }

    public static void showPropGrowth() {
        Multimap<UcdProperty, String> cumul = TreeMultimap.create();
        for (int year = 1995; year <= VersionToAge.ucd.maxYear(); ++year) {
            VersionInfo age = VersionToAge.ucd.getVersionInfoForYear(year);
            if (age == VersionToAge.UNASSIGNED) {
                continue;
            }
            IndexUnicodeProperties iup = IndexUnicodeProperties.make(age);
            main:
            for (UcdProperty prop : iup.getAvailableUcdProperties()) {
                switch (prop) {
                    case Name:
                        continue main;
                }
                if (prop.toString().startsWith("k")) {
                    continue; // skip unihan
                }
                PropertyStatus status = PropertyStatus.getPropertyStatus(prop);
                switch (status) {
                    case Immutable:
                    case Informative:
                    case Normative:
                    case Provisional:
                        break;
                    default:
                        continue;
                }
                UnicodeMap<String> map;
                try {
                    map = iup.load(prop);
                    // skip properties that are empty or include everything
                    if (map == null || map.size() < 1) continue;
                    if (map.getRangeCount() == 1 && map.getNonRangeStrings().isEmpty()) {
                        int start = map.getRangeStart(0);
                        int end = map.getRangeEnd(0);
                        if (start == 0 && end == 0x10FFFF) continue;
                    }
                } catch (Exception e) {
                    continue;
                }

                Set<String> values = map.values();
                Set<String> delta = new TreeSet<>(values);
                switch (prop) {
                    case Script_Extensions:
                        UnicodeMap<String> plain = iup.load(UcdProperty.Script);
                        delta.removeAll(plain.values());
                        break;
                }

                Collection<String> old = cumul.asMap().get(prop);
                if (old != null) {
                    delta.removeAll(old);
                }
                if (delta.isEmpty()) {
                    continue;
                }
                cumul.putAll(prop, delta);

                System.out.println(
                        year
                                + "\t"
                                + age.getVersionString(2, 2)
                                + "\t"
                                + prop
                                + "\t"
                                + prop.getType()
                                + "\t"
                                + delta.size()
                                + "\t"
                                + trim(delta));
            }
        }
    }

    private static void showScriptGrowth() {
        Map<Integer, Counter<UcdPropertyValues.Script_Values>> yearToScriptToCount =
                new TreeMap<>();

        IndexUnicodeProperties iup = IndexUnicodeProperties.make();
        UnicodeMap<UcdPropertyValues.Script_Values> plain2 = iup.loadEnum(UcdProperty.Script);
        UnicodeMap<UcdPropertyValues.Age_Values> ages = iup.loadEnum(UcdProperty.Age);
        for (Age_Values age : ages.values()) {
            if (age == Age_Values.Unassigned) {
                continue;
            }
            UnicodeSet uset = ages.getSet(age);
            int year = VersionToAge.ucd.getYear(age);
            if (year > 3000) {
                throw new IllegalArgumentException("Bad data in VersionToAge.java");
            }

            Counter<Script_Values> counter = yearToScriptToCount.get(year);
            if (counter == null) {
                yearToScriptToCount.put(year, counter = new Counter<>());
            }

            for (String s : uset) {
                Script_Values value = plain2.get(s);
                switch (value) {
                    case Unknown:
                        continue;
                    case Inherited:
                        value = Script_Values.Common;
                        break;
                }
                counter.add(value, 1);
            }
        }
        // Script Growth
        Set<Entry<Integer, Counter<Script_Values>>> row = yearToScriptToCount.entrySet();
        for (Entry<Integer, Counter<Script_Values>> info : row) {
            Integer year = info.getKey();
            Counter<Script_Values> counter = info.getValue();
            long remainder = counter.getTotal();
            System.out.println(year + "\t" + remainder);
            Set<Script_Values> keys = counter.getKeysetSortedByCount(false);
            int remainingKeys = keys.size();
            int current = 4;
            for (Script_Values script : keys) {
                long currentCount = counter.get(script);
                System.out.println(script.toString().replace("_", " ") + "\t" + currentCount);
                remainder -= currentCount;
                remainingKeys -= 1;
                current -= 1;
                if (current == 0 && remainingKeys > 1) {
                    System.out.println(remainingKeys + " other scripts\t" + remainder);
                    break;
                }
            }
            System.out.println();
        }
    }

    private static String trim(Set<String> delta) {
        if (delta.size() < 5) return delta.toString();
        StringBuilder b = new StringBuilder("[");
        int count = 0;
        for (String s : delta) {
            if (count != 0) {
                b.append(", ");
            }
            b.append(s);
            if (++count > 4) break;
        }
        return b.append(",…]").toString();
    }
}
