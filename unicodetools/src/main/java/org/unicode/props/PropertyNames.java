package org.unicode.props;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.dev.util.CollectionUtilities;

/**
 * PropertyNames is a list of long, short, and other names.
 * @author markdavis
 *
 * @param <T>
 */
public class PropertyNames<T extends Enum> {

    public interface Named {
        //        public PropertyNames getName();
        //        public PropertyNames getShortName();
        public PropertyNames getNames();
        public String getShortName();
    }

    final static Map<Class, NameMatcher> CLASS2NAME2ENUM = new HashMap<Class, NameMatcher>();

    private final NameMatcher                  name2enum;
    private final String                       shortName;
    private final List<String>                 otherNames;
    private final T                            enumItem;

    public PropertyNames(Class<T> classItem, T enumItem, String shortName, String... otherNames) {
        this.enumItem = enumItem;
        this.shortName = shortName == null ? enumItem.toString() : shortName;
        this.otherNames = Arrays.asList(otherNames);
        NameMatcher name2enumExisting = CLASS2NAME2ENUM.get(classItem);
        if (name2enumExisting == null) {
            CLASS2NAME2ENUM.put(classItem, name2enumExisting = new NameMatcher(this));
        }
        this.name2enum = name2enumExisting;
        //name2enumExisting.put(this.shortName, enumItem);
        name2enumExisting.put(this.shortName, enumItem);
        name2enumExisting.put(enumItem.toString(), enumItem);
        for (final String other : otherNames) {
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
        final LinkedHashSet<String> result = new LinkedHashSet<String>();
        result.add(shortName);  // UCD code expects the first name to be the short one
        result.add(enumItem.toString());
        result.addAll(otherNames);
        return ImmutableList.copyOf(result);
    }

    @Override
    public String toString() {
        return "{long: " + enumItem 
                + ", short: " + shortName 
                + (otherNames.size() == 0 ? "" : ", others: " + CollectionUtilities.join(otherNames, ", "))
                + "}";
    }

    public static <T extends Enum> List<T> getValues(Class<T> y) {
        try {
            return Arrays.asList((T[]) y.getMethod("values").invoke(null));
        } catch (final Exception e) {
            return null;
        }
    }

    public static <T extends Enum> NameMatcher<T> getNameToEnums(Class<T> classItem) {
        return CLASS2NAME2ENUM.get(classItem);
    }

    static Pattern FLUFF = Pattern.compile("[-_ ]");

    public static class NameMatcher<T extends Enum> {
        private final Map<String, T> string2Enum = new HashMap<String, T>();
        private final PropertyNames<T> propertyNames;

        public NameMatcher(PropertyNames<T> propertyNames) {
            this.propertyNames = propertyNames;
        }
        public T get(String string) {
            return string2Enum.get(minimalize(string));
        }
        void put(String s, T value) {
            if (s != null) {
                string2Enum.put(minimalize(s), value);
            }
        }
        public PropertyNames<T> getNames() {
            return propertyNames;
        }
        public static String minimalize(String source) {
            if (source.startsWith("Fixed")) {
                source = source.substring(5);
            }
            final String result = FLUFF.matcher(source.toLowerCase(Locale.ENGLISH)).replaceAll("");
            return result;
        }
        public static boolean matches(String lastValue, String latestValue) {
            if (lastValue == null) {
                return latestValue == null;
            } else if (latestValue == null) {
                return false;
            }
            return minimalize(lastValue).equals(minimalize(latestValue));
        }
    }
}