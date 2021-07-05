package org.unicode.propstest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyNames.Named;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class RegexWordBreak {
    static final IndexUnicodeProperties iupLatest = IndexUnicodeProperties.make();
    static final UnicodeMap<Age_Values> ageMap = iupLatest.loadEnum(UcdProperty.Age, Age_Values.class);
    static IndexUnicodeProperties iup;

    public static void main(String[] args) {
        String specVersion = System.getProperty("java.specification.version");
        System.out.println("Java version: " + specVersion);
        final Age_Values age = getJavaAge();
        System.out.println("Java Unicode Version: " + age);
        ImmutableSet<Binary> binary = ImmutableSet.copyOf(Binary.values());
        iup = IndexUnicodeProperties.make(age);

        // [^\p{IsAlphabetic}\p{Mark}\p{Decimal_Number}\p{Join_Control}]

        Multimap<UcdProperty,String> supported = TreeMultimap.create();
        Multimap<UcdProperty,String> unsupported = TreeMultimap.create();

        for (UcdProperty prop : iup.getAvailableUcdProperties()) {
            String propName = prop.name();
            String shortPropName = prop.getShortName();
            Set<Enum> enums = prop.getEnums();
            //          Classes for Unicode scripts, blocks, categories and binary properties
            //            \p{IsLatin} A Latin script character (script)
            //            \p{InGreek} A character in the Greek block (block)
            //            \p{Lu}  An uppercase letter (category)
            //            \p{IsAlphabetic}    An alphabetic character (binary property)
            switch (prop) {
            case Script:
                checkEnumNames(prop, "Is", enums, supported, unsupported);
                break;
            case Block:
                checkEnumNames(prop, "In", enums, supported, unsupported);
                break;
            case General_Category:
                checkEnumNames(prop, "", enums, supported, unsupported);
                break;
            default:
                boolean isbinary = Objects.equal(enums, binary);
                if (isbinary) {
                    checkPattern(prop, Binary.Yes, "Is" + propName, supported, unsupported);
                    checkPattern(prop, Binary.Yes, "Is" + shortPropName, supported, unsupported);
                } else {
                    unsupported.put(prop, "ALL");
                }
                break;
            }
        }
        Map<UcdProperty, Collection<String>> supportedMap = supported.asMap();
        Map<UcdProperty, Collection<String>> unsupportedMap = unsupported.asMap();
        NavigableMap<String, Collection<UcdProperty>> reversed = Multimaps.invertFrom(supported, TreeMultimap.<String,UcdProperty>create()).asMap();

        System.out.println("SUPPORTED: " + supportedMap.size());
        for (Entry<UcdProperty, Collection<String>> entry : supportedMap.entrySet()) {
            UcdProperty key = entry.getKey();
            Collection<String> values = entry.getValue();
            System.out.println(key + "\tOK\t" + values);
            Collection<String> bad = unsupportedMap.get(key);
            if (bad != null && !bad.isEmpty()) {
                System.out.println(key + "\tFAIL\t" + unsupportedMap.get(key));
            }
            for (String value : values) {
                Collection<UcdProperty> reverse = reversed.get(value);
                if (reverse.size() > 1) {
                    System.out.println("\t" + value + "\tAMBIGUOUS\t" + reverse);
                }
            }
        }
        System.out.println("UNSUPPORTED: " + unsupportedMap.size());
        for (Entry<UcdProperty, Collection<String>> entry : unsupportedMap.entrySet()) {
            Collection<String> partial = supportedMap.get(entry.getKey());
            if (partial != null && !partial.isEmpty()) {
                continue; // already done
            }
            System.out.println(entry.getKey() + "\tFAIL\t" + entry.getValue());
        }
    }

    private static void checkEnumNames(UcdProperty prop, String prefix, Set<Enum> enums,
            Multimap<UcdProperty, String> supported, Multimap<UcdProperty, String> unsupported) {
        for (Enum enumValue : enums) {
            String shortEnumName = ((Named)enumValue).getShortName();
            checkPattern(prop, enumValue, prefix + enumValue.name(), supported, unsupported);
            checkPattern(prop, enumValue, prefix + shortEnumName, supported, unsupported);
        }
    }

    private static void checkPattern(UcdProperty prop, Enum enumValue, String propName, 
            Multimap<UcdProperty, String> supported, 
            Multimap<UcdProperty, String> unsupported) {
        Pattern pat = getPattern(propName);
        if (pat != null) {
            if (!check(prop, enumValue, pat)) {
                supported.put(prop, propName + "*");
            } else {
                supported.put(prop, propName);
            }
        } else {
            unsupported.put(prop, propName);
        }
    }

    private static boolean check(UcdProperty prop, Enum enumValue, Pattern pat) {
        Matcher matcher = pat.matcher("");
        UnicodeMap<Enum> map = iup.loadEnum(prop, enumValue.getClass());
        UnicodeSet set = map.keySet(enumValue);
        // verify the same coverage.
        // should do inverse also
        for (String s : set) {
            if (!matcher.reset(s).matches()) {
                return false;
            }
        }
        return true;
    }

    private static Pattern getPattern(String propName) {
        try {
            return Pattern.compile("\\p{" + propName + "}");
        } catch (Exception e) {
            return null;
        }
    }

    public static Age_Values getJavaAge () {
        List<Age_Values> reversedList = new ArrayList<>(Arrays.asList(Age_Values.values()));
        reversedList.remove(Age_Values.Unassigned);
        Collections.reverse(reversedList);
        for (Age_Values age : reversedList) { // 
            UnicodeSet codepointsWithAge = ageMap.getSet(age);
            int first = codepointsWithAge.getRangeStart(0);
            if (Character.getType(first) != Character.UNASSIGNED) {
                return age;
            }
        }
        throw new IllegalArgumentException();
    }
}