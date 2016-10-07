package org.unicode.propstest;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyNames;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.dev.util.UnicodeMap;

public class Collisions {
    static class Entity {
        final String propertyName;
        final String valueName;
        public Entity(String propertyName, String valueName) {
            this.propertyName = propertyName;
            this.valueName = valueName;
        }
        @Override
        public String toString() {
            return propertyName + (valueName == null ? "" : "=" + valueName);
        }
    }
    public static void main(String[] args) {
        Multimap<String,Entity> nameToEntity = HashMultimap.create();
        IndexUnicodeProperties props = IndexUnicodeProperties.make(Age_Values.V9_0);
        for (UcdProperty prop : props.getAvailableUcdProperties()) {
            String propName = prop.toString();
            PropertyNames<UcdProperty> names = prop.getNames();
            for (String name : names.getAllNames()) {
                nameToEntity.put(name, new Entity(propName,null));
            }
            try {
                UnicodeMap<String> data = props.load(prop);
                Set<String> values = data.values();
                System.out.println(propName + ": " + values.size());
                for (String value : values) {
                    nameToEntity.put(value, new Entity(propName,value));
                }
            } catch (Exception e) {
                System.out.println("PROBLEM: " + propName);
            }
        }
        long count1 = 0;
        long count2 = 0;
        long total = 0;
        for (String key : new TreeSet<String>(nameToEntity.keySet())) {
            Collection<Entity> values = nameToEntity.get(key);
            final int size = values.size();
            total += size;
            if (size < 2) {
                continue;
            }
            System.out.println(CldrUtility.toString(values));
            count1++;
            count2+= size;
        }
        System.out.println("Total: " + count1);
        System.out.println("Dups: " + count1);
        System.out.println("Dups2: " + count2);
    }
}
