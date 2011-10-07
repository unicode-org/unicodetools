package org.unicode.props;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class PropertyNames<T extends Enum> {
    public enum PropertyType {
        Numeric, String, Miscellaneous, Catalog, Enumerated, Binary,
    }

    final static Map<Class, NameMatcher> CLASS2NAME2ENUM = new HashMap<Class, NameMatcher>();

    private final NameMatcher            name2enum;
    private final String                       shortName;
    private final List<String>                 otherNames;
    private final T                            enumItem;

    public PropertyNames(Class<T> classItem, T enumItem, String shortName, String... otherNames) {
        this.enumItem = enumItem;
        this.shortName = shortName == null ? enumItem.toString() : shortName;
        this.otherNames = Arrays.asList(otherNames);
        NameMatcher name2enumExisting = CLASS2NAME2ENUM.get(classItem);
        if (name2enumExisting == null) {
            CLASS2NAME2ENUM.put(classItem, name2enumExisting = new NameMatcher());
        }
        this.name2enum = name2enumExisting;
        //name2enumExisting.put(this.shortName, enumItem);
        name2enumExisting.put(this.shortName, enumItem);
        name2enumExisting.put(enumItem.toString(), enumItem);
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

    public List<String> getAllNames() {
        ArrayList<String> result = new ArrayList<String>();
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

    public static NameMatcher getNameToEnums(Class classItem) {
        return CLASS2NAME2ENUM.get(classItem);
    }

    static Pattern FLUFF = Pattern.compile("[-_ ]");
    
    public static class NameMatcher<T extends Enum> {
        Map<String, T> string2Enum = new HashMap<String, T>();
        T get(String string) {
            return string2Enum.get(minimalize(string));
        }
        void put(String s, T value) {
            string2Enum.put(minimalize(s), value);
        }
        public static String minimalize(String source) {
            return FLUFF.matcher(source.toLowerCase(Locale.ENGLISH)).replaceAll("");
        }
    }
}