package org.unicode.propstest;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.UnicodeSet;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.props.PropertyStatus;
import org.unicode.props.UcdProperty;

public class ShowPropertyMetadata {

    //    static final EnumSet<UcdProperty> PROVISIONAL_PROPERTY;
    //    private static final Comparator<Set<PropertyType>> COMPARABLE = new
    // Comparator<Set<PropertyType>>() {
    //        @Override
    //        public int compare(Set<PropertyType> arg0, Set<PropertyType> arg1) {
    //            // TODO Auto-generated method stub
    //            return UnicodeSet.compare(arg0, arg1);
    //        }
    //    };
    //    static {
    //        final EnumSet<UcdProperty> temp2 = EnumSet.allOf(UcdProperty.class);
    //        temp2.removeAll(PropertyStatus.NORMATIVE_PROPERTY);
    //        temp2.removeAll(PropertyStatus.INFORMATIVE_PROPERTY);
    //        temp2.removeAll(PropertyStatus.CONTRIBUTORY_PROPERTY);
    //        PROVISIONAL_PROPERTY = temp2;
    //    }

    //    static EnumSet<UcdProperty> soFar = EnumSet.noneOf(UcdProperty.class);
    //
    //    enum PropertyType {
    ////        Deprecated(PropertyStatus.Deprecated,
    ////                PropertyStatus.Stabilized,
    ////                PropertyStatus.Obsolete),
    ////                Immutable(UcdProperty.Jamo_Short_Name, PropertyStatus.Immutable),
    ////                Normative(PropertyStatus.Normative),
    ////                Informative(PropertyStatus.Informative),
    ////                Provisional(PropertyStatus.pro),
    ////                Contributory(PropertyStatus.CONTRIBUTORY_PROPERTY);
    ////        //        Stabilized(),
    ////        //        Obsolete();
    //
    //        EnumSet<UcdProperty> set;
    //
    //        PropertyType(EnumSet<UcdProperty>... set) {
    //            this(null, set);
    //        }
    //        PropertyType(UcdProperty skip, EnumSet<UcdProperty>... set) {
    //            this.set = EnumSet.noneOf(UcdProperty.class);
    //            for (final EnumSet<UcdProperty> i : set) {
    //                for (final UcdProperty j : i) {
    //                    if (soFar.contains(j) || j == skip) {
    //                        continue;
    //                    }
    //                    this.set.add(j);
    //                    soFar.add(j);
    //                }
    //            }
    //        }
    //    }

    private static final Comparator<Set<PropertyStatus>> COMPARABLE =
            new Comparator<Set<PropertyStatus>>() {
                @Override
                public int compare(Set<PropertyStatus> arg0, Set<PropertyStatus> arg1) {
                    return UnicodeSet.compare(arg0, arg1);
                }
            };

    public static void main(String[] args) {

        final Relation<Set<PropertyStatus>, UcdProperty> typesToProperties =
                Relation.of(
                        new TreeMap<Set<PropertyStatus>, Set<UcdProperty>>(COMPARABLE),
                        TreeSet.class);
        for (final UcdProperty prop : UcdProperty.values()) {
            EnumSet<PropertyStatus> statuses = PropertyStatus.getPropertyStatusSet(prop);
            typesToProperties.put(statuses, prop);
        }

        // invert
        Set<PropertyStatus> lastSet = null;
        for (final Entry<Set<PropertyStatus>, UcdProperty> e : typesToProperties.keyValueSet()) {
            final Set<PropertyStatus> statusus = e.getKey();
            if (!statusus.equals(lastSet)) {
                System.out.println();
            }
            System.out.println(e.getValue() + ";\t" + CollectionUtilities.join(statusus, ";\t"));
            lastSet = statusus;
        }
    }
}
