package org.unicode.props;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Timer;
import org.unicode.idna.Regexes;
import org.unicode.props.IndexUnicodeProperties.PropertyInfo;
import org.unicode.props.Properties.UcdProperty;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class IndexUnicodeProperties {
    public final static Pattern SEMICOLON = Pattern.compile("\\s*;\\s*");
    public final static Pattern TAB = Pattern.compile("[ ]*\t[ ]*");

    enum FileType {Field, PropertyValue, List}
    enum SpecialProperty {None, Skip1FT, Skip1ST, SkipAny4}
    
    static class PropertyInfo implements Comparable<PropertyInfo>{

        public PropertyInfo(String... propertyInfo) {
            this.file = propertyInfo[0];
            this.property = UcdProperty.forName(propertyInfo[1]);
            int temp = 1;
            try {
                temp = Integer.parseInt(propertyInfo[2]);
            } catch (Exception e) {}
            this.fieldNumber = temp;
            this.special = propertyInfo.length < 4 
            ? SpecialProperty.None : SpecialProperty.valueOf(propertyInfo[3]);
        }
        public String toString() {
            return file + " ; " + property + " ; " + fieldNumber;
        }
        final String file;
        final UcdProperty property;
        final int fieldNumber;
        final SpecialProperty special;
        UnicodeMap<String> data;
        final Set<String> errors = new LinkedHashSet<String>();
        @Override
        public int compareTo(PropertyInfo arg0) {
            int result;
            if (0 != (result = file.compareTo(arg0.file))) {
                return result;
            }
            if (0 != (result = property.toString().compareTo(arg0.property.toString()))) {
                return result;
            }
            return fieldNumber - arg0.fieldNumber;
        }

        public void put(int start, int end, String string) {
            if (property.type == Properties.PropertyType.Enumerated || property.type == Properties.PropertyType.Binary) {
                Enum item = property.forValueName(string);
                if (item == null) {
                    errors.add("Bad enum value: " + string);
                }
            }
            for (int codepoint = start; codepoint <= end; ++codepoint) {
                try {
                    putNew(data, codepoint, string);
                } catch (Exception e) {
                    System.err.println(property + ":\t" + e.getMessage());
                }
            }
        }
    }

    static EnumMap<UcdProperty, PropertyInfo> property2PropertyInfo = new EnumMap<UcdProperty,PropertyInfo>(UcdProperty.class);
    static Relation<String,PropertyInfo> file2PropertyInfoSet = Relation.of(new HashMap<String,Set<PropertyInfo>>(), HashSet.class);
    static Map<String,FileType> file2Type = new HashMap<String,FileType>();

    static {
        Matcher semicolon = SEMICOLON.matcher("");
        for (String line : FileUtilities.in(IndexUnicodeProperties.class, "IndexUnicodeProperties.txt")) {
            if (line.startsWith("#") || line.isEmpty()) continue;

            String[] parts = Regexes.split(semicolon, line.trim());
            // file ; property name ; derivation info
            //System.out.println(Arrays.asList(parts));
            if (parts[0].equals("FileType")) {
                putNew(file2Type, parts[1], FileType.valueOf(parts[2]));
            } else {
                try {
                    final UcdProperty prop = UcdProperty.forName(parts[1]);
                    final PropertyInfo propertyInfo = new PropertyInfo(parts);
                    putNew(property2PropertyInfo, prop, propertyInfo);
                    file2PropertyInfoSet.put(parts[0], propertyInfo);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Can't find property for <" + parts[1] + ">", e);
                }
            }
        }
        for (String file : new TreeSet<String>(file2PropertyInfoSet.keySet())) {
            Set<PropertyInfo> props = file2PropertyInfoSet.getAll(file);
            for (PropertyInfo prop : new TreeSet<PropertyInfo>(props)) {
                System.out.println(prop);
            }
        }
        for (UcdProperty x : org.unicode.props.Properties.UcdProperty.values()) {
            if (property2PropertyInfo.containsKey(x.toString())) continue;
            System.out.println("Missing: " + x);
        }
    }

    static final <K, V, M extends Map<K,V>> M putNew(M map, K key, V value) {
        V oldValue = map.get(key);
        if (oldValue != null) {
            throw new IllegalArgumentException("Key already present in map: " + key + ",\told: " + oldValue + ",\tnew: " + value);
        }
        map.put(key, value);
        return map;
    }

    static final <V> UnicodeMap<V> putNew(UnicodeMap<V> map, int key, V value) {
        V oldValue = map.get(key);
        if (oldValue != null) {
            throw new IllegalArgumentException("Key already present in map: " + Utility.hex(key) + ",\told: " + oldValue + ",\tnew: " + value);
        }
        map.put(key, value);
        return map;
    }

    static final <V, C extends Collection<V>> C addNew(C collection, V value) {
        if (collection.contains(value)) {
            throw new IllegalArgumentException("Value already present in map: " + value);
        }
        collection.add(value);
        return collection;
    }

    public static UnicodeMap<String> load(UcdProperty prop2) {
        PropertyInfo fileInfo = property2PropertyInfo.get(prop2);
        if (fileInfo.data != null) {
            return fileInfo.data;
        }
        String fullFilename = Utility.getMostRecentUnicodeDataFile(fileInfo.file, Default.ucdVersion(), true, false);
        Set<PropertyInfo> props = file2PropertyInfoSet.get(fileInfo.file);

        FileType fileType = file2Type.get(fileInfo.file);
        if (fileType == null) {
            fileType = FileType.Field;
        }

        for (PropertyInfo prop : props) {
            if (prop.data != null) {
                throw new InternalError("Double-assigned property: " + prop2);
            }
            prop.data = new UnicodeMap<String>();
            //if (prop.hackRange)
        }
        Matcher semicolon = SEMICOLON.matcher("");
        Matcher tab = TAB.matcher("");
        IntRange intRange = new IntRange();
        for (String line : FileUtilities.in("", fullFilename)) {
            int hashPos = line.indexOf('#');
            if (hashPos >= 0) {
                line = line.substring(0,hashPos);
            }
            line = line.trim();
            if (line.isEmpty()) continue;
            //HACK
            String[] parts = Regexes.split(line.startsWith("U+") ? tab : semicolon, line.trim());
            //System.out.println(line + "\t\t" + Arrays.asList(parts));
            // HACK RANGE
            // 3400;<CJK Ideograph Extension A, First>;Lo;0;L;;;;;N;;;;;
            // 4DB5;<CJK Ideograph Extension A, Last>;Lo;0;L;;;;;N;;;;;
            // U+4F70   kAccountingNumeric  100
            intRange.set(parts[0]);
            switch(fileType) {
            case PropertyValue: {
                PropertyInfo prop = property2PropertyInfo.get(UcdProperty.forName(parts[1]));
                if (!props.contains(prop)) {
                    throw new IllegalArgumentException("Property not listed for file: " + prop);
                }
                switch(prop.property.type) {
                case Binary:
                    prop.put(intRange.start, intRange.end, "Yes"); break;
                default:
                    prop.put(intRange.start, intRange.end, parts[2]); break;
                }
                break;
            }
            case Field: {
                for (PropertyInfo prop : props) {
                    switch(prop.special) {
                    case None: break;
                    case Skip1ST: 
                        if ("FT".contains(parts[1])) {
                            continue;
                        }
                        break;
                    case Skip1FT: 
                        if ("ST".contains(parts[1])) {
                            continue;
                        }
                        break;
                    case SkipAny4: 
                        if (!parts[4].isEmpty()) {
                            continue;
                        }
                        break;
                    default: throw new IllegalArgumentException();
                    }
                    prop.put(intRange.start, intRange.end, parts[prop.fieldNumber]);
                }
                break;
            }
            case List: {
                if (props.size() != 1) {
                    throw new IllegalArgumentException("List files must have only one property, and must be Boolean");
                }
                PropertyInfo prop = props.iterator().next();
                //prop.data.putAll(UnicodeSet.ALL_CODE_POINTS, "No");
                prop.put(intRange.start, intRange.end, "Yes");
                break;
            }
            default: throw new IllegalArgumentException();
            }
        }
        for (PropertyInfo prop : props) {
            prop.data.freeze();
        }
        return fileInfo.data;
    }

    private static class IntRange {
        int start;
        int end;
        public IntRange set(String source) {
            if (source.startsWith("U+")) {
                source = source.substring(2);
            }
            int range = source.indexOf("..");
            if (range >= 0) {
                start = Integer.parseInt(source.substring(0,range),16);
                end = Integer.parseInt(source.substring(range+2),16);
            } else {
                start = end = Integer.parseInt(source, 16);
            }
            return this;
        }
    }

    public static void main(String[] args) {
        for (Entry<String, PropertyInfo> entry : file2PropertyInfoSet.keyValueSet()) {
            System.out.println(entry.getKey() + " ; " + entry.getValue());
        }
        Timer timer = new Timer();
        show(UcdProperty.Lowercase_Mapping, timer);

        for (UcdProperty prop : UcdProperty.values()) {
            show(prop, timer);
        }
        //        show("Joining_Type", timer);
        //        show("Joining_Group", timer);
        //        show("General_Category", timer);
        //        show("Canonical_Combining_Class", timer);        
        //        //show("Name", timer);        
        //        show("Bidi_Class", timer);        
        //        show("kMandarin", timer);        
    }

    public static void show(UcdProperty prop, Timer timer) {
        System.out.println(prop);
        timer.start();
        UnicodeMap<String> map = load(prop);
        timer.stop();
        final Collection<String> values = map.values();
        String sample = values.toString();
        if (sample.length() > 20) {
            sample = sample.substring(0,20) + "…";
        }
        Set<String> errors = property2PropertyInfo.get(prop).errors;
        System.out.println("\ttime: " + timer + "\tcodepoints: " + map.size() + "\tvalues: " + values.size() + "\tsample: " + sample + "\terrors: " + errors);
        //        for (String value : map.getAvailableValues()) {
        //            System.out.println("\t" + value + " " + map.getSet(value));
        //        }
    }
}
