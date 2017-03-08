package org.unicode.propstest;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyStatus;
import org.unicode.props.UcdProperty;
import org.unicode.props.VersionToAge;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.util.VersionInfo;

public class PropertyGrowth {
    public static void main(String[] args) {
        Multimap<UcdProperty, String> cumul = TreeMultimap.create();
        
        for (int year = 1995; year <= 2017; ++year) {
            VersionInfo age = VersionToAge.ucd.getVersionInfoForYear(year);
            if (age == VersionToAge.UNASSIGNED) {
                continue;
            }
            System.out.println();
            IndexUnicodeProperties iup = IndexUnicodeProperties.make(age);
            for (UcdProperty prop : iup.getAvailableUcdProperties()) {
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
                if (prop == UcdProperty.Script_Extensions) {
                    UnicodeMap<String> plain = iup.load(UcdProperty.Script);
                    delta.removeAll(plain.values());
                }
                Collection<String> old = cumul.asMap().get(prop);
                if (old != null) {
                    delta.removeAll(old);
                    if (delta.isEmpty()) {
                        continue;
                    }
                }
                cumul.putAll(prop, delta);
                System.out.println(year + "\t" + age.getVersionString(2, 2) + "\t" + prop + "\t" + prop.getType() + "\t" + delta.size() + "\t" + trim(delta));
            }
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
