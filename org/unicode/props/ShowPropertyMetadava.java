package org.unicode.props;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.text.UnicodeSet;

public class ShowPropertyMetadava {

	static final EnumSet<UcdProperty> PROVISIONAL_PROPERTY;
	private static final Comparator<Set<PropertyType>> COMPARABLE = new Comparator<Set<PropertyType>>() {
		@Override
		public int compare(Set<PropertyType> arg0, Set<PropertyType> arg1) {
			// TODO Auto-generated method stub
			return UnicodeSet.compare(arg0, arg1);
		}
	};
	static {
		EnumSet<UcdProperty> temp2 = EnumSet.allOf(UcdProperty.class);
		temp2.removeAll(IndexUnicodeProperties.NORMATIVE_PROPERTY);
		temp2.removeAll(IndexUnicodeProperties.INFORMATIVE_PROPERTY);
		temp2.removeAll(IndexUnicodeProperties.CONTRIBUTORY_PROPERTY);
		PROVISIONAL_PROPERTY = temp2;
	}

	static EnumSet<UcdProperty> soFar = EnumSet.noneOf(UcdProperty.class);
	
	enum PropertyType {
		Deprecated(IndexUnicodeProperties.DEPRECATED_PROPERTY, 
				IndexUnicodeProperties.STABLIZED_PROPERTY, 
				IndexUnicodeProperties.OBSOLETE_PROPERTY),
		Immutable(UcdProperty.Jamo_Short_Name, IndexUnicodeProperties.IMMUTABLE_PROPERTY), 
		Normative(IndexUnicodeProperties.NORMATIVE_PROPERTY), 
		Informative(IndexUnicodeProperties.INFORMATIVE_PROPERTY), 
		Provisional(IndexUnicodeProperties.PROVISIONAL_PROPERTY), 
		Contributory(IndexUnicodeProperties.CONTRIBUTORY_PROPERTY);
//		Stabilized(), 
//		Obsolete();

		EnumSet<UcdProperty> set;
		
		PropertyType(EnumSet<UcdProperty>... set) {
			this(null, set);
		}
		PropertyType(UcdProperty skip, EnumSet<UcdProperty>... set) {
			this.set = EnumSet.noneOf(UcdProperty.class);
			for (EnumSet<UcdProperty> i : set) {
				for (UcdProperty j : i) {
					if (soFar.contains(j) || j == skip) {
						continue;
					}
					this.set.add(j);
					soFar.add(j);
				}
			}
		}
	}

	public static void main(String[] args) {

		Relation<UcdProperty,PropertyType> propToType = 
				Relation.of(new TreeMap<UcdProperty, Set<PropertyType>>(),
				TreeSet.class);
		for (PropertyType ptype : PropertyType.values()) {
			for (UcdProperty prop : ptype.set) {
				propToType.put(prop,ptype);
			}
		}
		
		// invert
		Relation<Set<PropertyType>,UcdProperty> typesToProperties = 
				Relation.of(new TreeMap<Set<PropertyType>, Set<UcdProperty>>(COMPARABLE),
				TreeSet.class);
		for (Entry<UcdProperty, Set<PropertyType>> e : propToType.keyValuesSet()) {
			typesToProperties.put(e.getValue(), e.getKey());
		}

		for (Entry<Set<PropertyType>, UcdProperty> e : typesToProperties.keyValueSet()) {
			System.out.println(e.getValue() + ";\t" + CollectionUtilities.join(e.getKey(), ";\t"));
		}
	}
}
