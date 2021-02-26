package org.unicode.propstest;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map.Entry;

import org.unicode.cldr.util.Counter;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyStatus;
import org.unicode.props.PropertyStatus.PropertyOrigin;
import org.unicode.props.PropertyType;
import org.unicode.props.UcdProperty;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class UnicodePropertyPartition {
    static IndexUnicodeProperties UnicodeProperties = IndexUnicodeProperties.make();
    static Comparator<UnicodeSet> SIZE_FIRST = new Comparator<UnicodeSet>() {
	@Override
	public int compare(UnicodeSet o1, UnicodeSet o2) {
	    int diff = o1.size() - o2.size();
	    if (diff != 0) {
		return diff;
	    }
	    return o1.compareTo(o2);
	}
    };

    public static void main(String[] args) {
	Multimap<UnicodeSet, String> info = TreeMultimap.create(SIZE_FIRST, Ordering.natural());
	ImmutableSet<PropertyStatus> skipStatus = ImmutableSet.of(PropertyStatus.Contributory, PropertyStatus.Deprecated, PropertyStatus.Internal, 
		PropertyStatus.Obsolete);
	ImmutableSet<PropertyOrigin> skipOrigin = ImmutableSet.of(PropertyOrigin.Extra, PropertyOrigin.ICU);

	Multimap<PropertyType, UcdProperty> typeInfo = TreeMultimap.create();
	Counter<PropertyType> typeValues = new Counter<>();
	
	for (UcdProperty prop : UnicodeProperties.getAvailableUcdProperties()) {
	    final PropertyStatus propertyStatus = PropertyStatus.getPropertyStatus(prop);
	    final PropertyOrigin origin = PropertyStatus.getOrigin(prop.name());

	    if (skipStatus.contains(propertyStatus) || skipOrigin.contains(origin)) {
		continue;
	    }
	    if (propertyStatus == PropertyStatus.Unknown || origin == PropertyOrigin.Unknown) {
		System.out.println("***\tMissing info for\t" + prop 
			+ "\tstatus: " + propertyStatus 
			+ "\tstatus: " + origin);
		continue;
	    }
	    final PropertyType type = prop.getType();
	    typeInfo.put(type, prop);

		UnicodeMap<String> map = UnicodeProperties.load(prop);
		Collection<String> values = map.getAvailableValues();
		typeValues.add(type, values.size() - 1);

//	    switch (type) {
//	    case Catalog:
//	    case Enumerated:
//		
//		// System.out.println(prop.name() + "\t" + values.size());
////		for (String value : map.getAvailableValues()) {
////		    UnicodeSet set = map.getSet(value);
////		    info.put(set, prop.name() + "=" + value);
////		}
//		break;
//	    default: continue;
//	    }

	}
	for (Entry<PropertyType, Collection<UcdProperty>> entry : typeInfo.asMap().entrySet()) {
	    System.out.println(entry.getKey() + "\t" + entry.getValue().size() + "\t" + typeValues.get(entry.getKey()));
	}
//	int index = 0;
//	for (Entry<UnicodeSet, Collection<String>> entry : info.asMap().entrySet()) {
//	    String shortSet = entry.getKey().toPattern(false);
//	    if (shortSet.length() > 20) {
//		shortSet = shortSet.substring(0,20) + "…";
//	    }
//	    System.out.println(++index + "\t" + entry.getValue() + "\t" + shortSet);
//	}
    }

}
