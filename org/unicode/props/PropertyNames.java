package org.unicode.props;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyNames<T extends Enum> {
    public enum PropertyType {Numeric, String, Miscellaneous, Catalog, Enumerated, Binary, }
    
    final static Map<Class, Map<String, Enum>> CLASS2NAME2ENUM = new HashMap<Class, Map<String,Enum>>();
    
    private final Map<String, Enum> name2enum;
    private final String shortName;
    private final List<String> otherNames;
    
    public PropertyNames (Class<T> classItem, T enumItem, String shortName, String...otherNames) {
        this.shortName = shortName == null ? enumItem.toString() : shortName;
        this.otherNames = Arrays.asList(otherNames);
        Map<String, Enum> name2enumExisting = CLASS2NAME2ENUM.get(classItem);
        if (name2enumExisting == null) {
            CLASS2NAME2ENUM.put(classItem, name2enumExisting = new HashMap<String, Enum>());
        }
        this.name2enum = Collections.unmodifiableMap(name2enumExisting);
        name2enumExisting.put(enumItem.toString(), enumItem);
        name2enumExisting.put(shortName, enumItem);
        for (String other : otherNames) {
            name2enumExisting.put(other, enumItem);
        }
    }
    public T forString(String name) {
        return name2enum == null ? null : (T) name2enum.get(name);
    }
    public String getShortName() {
        return shortName;
    }
    public List<String> getOtherNames() {
        return otherNames;
    }
    public Map<String, Enum> getNameToEnum() {
        return name2enum;
    }
    public String toString() {
        return "{long: " + name2enum.get(shortName) + ", short: " + shortName + ", others: " + otherNames + "}";
    }
    public static List<Enum> getValues(Class y) {
        try {
            return Arrays.asList((Enum[]) y.getMethod("values").invoke(null));
        } catch (Exception e) {
            return null;
        }
    }
    public static Map<String, Enum> getNameToEnums(Class classItem) {
        return Collections.unmodifiableMap(CLASS2NAME2ENUM.get(classItem));
    }
}