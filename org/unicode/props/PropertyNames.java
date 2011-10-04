package org.unicode.props;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PropertyNames<T extends Enum> {
    public enum PropertyType {
        Numeric, String, Miscellaneous, Catalog, Enumerated, Binary,
    }

    final static Map<Class, Map<String, Enum>> CLASS2NAME2ENUM = new HashMap<Class, Map<String, Enum>>();

    private final Map<String, Enum>            name2enum;
    private final String                       shortName;
    private final List<String>                 otherNames;
    private final T                            enumItem;

    public PropertyNames(Class<T> classItem, T enumItem, String shortName, String... otherNames) {
        this.enumItem = enumItem;
        this.shortName = shortName == null ? enumItem.toString() : shortName;
        this.otherNames = Arrays.asList(otherNames);
        Map<String, Enum> name2enumExisting = CLASS2NAME2ENUM.get(classItem);
        if (name2enumExisting == null) {
            CLASS2NAME2ENUM.put(classItem, name2enumExisting = new HashMap<String, Enum>());
        }
        this.name2enum = Collections.unmodifiableMap(name2enumExisting);
        name2enumExisting.put(shortName, enumItem);
        name2enumExisting.put(minimalize(shortName), enumItem);
        name2enumExisting.put(minimalize(enumItem.toString()), enumItem);
        for (String other : otherNames) {
            name2enumExisting.put(minimalize(other), enumItem);
        }
    }

    public T forString(String name) {
        return name2enum == null ? null : (T) name2enum.get(minimalize(name));
    }

    public String getShortName() {
        return shortName;
    }

    public List<String> getOtherNames() {
        return otherNames;
    }

    public List<String> getAllNames() {
        ArrayList<String> result = new ArrayList();
        result.add(toString());
        result.add(shortName);
        result.addAll(otherNames);
        return result;
    }

    public String toString() {
        return "{long: " + enumItem + ", short: " + shortName + ", others: " + otherNames + "}";
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

    public static String minimalize(String source) {
        return source.toLowerCase(Locale.ENGLISH).replace(' ', '_').replace('-', '_');
    }
}