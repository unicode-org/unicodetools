package org.unicode.propstest;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.props.PropertyStatus;
import org.unicode.props.UcdProperty;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;

public class ShowMetaproperties {
    public static void main(String[] args) {
        final Relation<EnumSet<PropertyStatus>, UcdProperty> map
        = Relation.of(new TreeMap<EnumSet<PropertyStatus>, Set<UcdProperty>>(
                new EnumSetComparator<PropertyStatus>()), TreeSet.class);
        for (final UcdProperty prop : UcdProperty.values()) {
            final EnumSet<PropertyStatus> propertyStatusSet = PropertyStatus.getPropertyStatusSet(prop);
            if (propertyStatusSet.contains(PropertyStatus.Normative) && propertyStatusSet.contains(PropertyStatus.Informative)
                    || propertyStatusSet.contains(PropertyStatus.Normative) && propertyStatusSet.contains(PropertyStatus.Contributory)
                    || propertyStatusSet.contains(PropertyStatus.Informative) && propertyStatusSet.contains(PropertyStatus.Contributory)
                    ) {
                throw new IllegalArgumentException("Conflicting values: " + prop + ",\t" + propertyStatusSet);
            }
            map.put(propertyStatusSet, prop);
            //System.out.println(prop + " ;\t" + IndexUnicodeProperties.getPropertyStatusSet(prop));
        }
        System.out.println("# Table 9 Status+\n" +
                "# http://unicode.org/reports/tr44/proposed.html#Property_List_Table\n" +
                "# http://unicode.org/reports/tr38/proposed.html#AlphabeticalListing\n" +
                "# property ; property_type ; property_status(s)");
        EnumSet<PropertyStatus> lastStatusSet = EnumSet.noneOf(PropertyStatus.class);
        for (final Entry<EnumSet<PropertyStatus>, UcdProperty> entry : map.keyValueSet()) {
            final UcdProperty prop = entry.getValue();
            final EnumSet<PropertyStatus> statusSet = entry.getKey();
            final String statusSetDisplay = CollectionUtilities.join(statusSet, " ");
            if (!lastStatusSet.equals(statusSet)) {
                System.out.println("# " + statusSetDisplay);
                lastStatusSet = statusSet;
            }
            System.out.println(prop + "\t;\t" + prop.getType() + "\t;\t" + statusSetDisplay);
        }
    }

    static class EnumSetComparator<T extends Enum<T>> implements Comparator<EnumSet<T>> {
        @Override
        public int compare(EnumSet<T> arg0, EnumSet<T> arg1) {
            final Iterator<T> it0 = arg0.iterator();
            final Iterator<T> it1 = arg1.iterator();
            while (true) {
                if (!it0.hasNext()) {
                    return it1.hasNext() ? -1 : 0;
                }
                if (!it1.hasNext()) {
                    return 1;
                }
                final int diff = it0.next().compareTo(it1.next());
                if (diff != 0) {
                    return diff;
                }
            }
        }
    }
}
