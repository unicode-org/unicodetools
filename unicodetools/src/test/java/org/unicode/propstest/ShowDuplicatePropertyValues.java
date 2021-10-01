package org.unicode.propstest;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.Counter;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Joining_Group_Values;
import org.unicode.props.UcdPropertyValues.Joining_Type_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;

public class ShowDuplicatePropertyValues {
    private static final IndexUnicodeProperties latest = IndexUnicodeProperties.make(Settings.latestVersion); // GenerateEnums.ENUM_VERSION
    private static final UnicodeMap<Joining_Group_Values> groupValues = latest.loadEnum(UcdProperty.Joining_Group, Joining_Group_Values.class);
    private static final UnicodeMap<Joining_Type_Values> typeValues = latest.loadEnum(UcdProperty.Joining_Type, Joining_Type_Values.class);
    private static final UnicodeMap<Script_Values> scriptValues = latest.loadEnum(UcdProperty.Script, Script_Values.class);
    private static final UnicodeMap<Age_Values> ageValues = latest.loadEnum(UcdProperty.Age, Age_Values.class);
    
    enum Behavior {non_singular, distinct_singular, no_group}

    public static void main(String[] args) {
        Multimap<Age_Values, String> correctNonSingular = TreeMultimap.create();
        Map<Script_Values, Counter<Behavior>> scriptToBehavior = new TreeMap<>();

        Counter<Joining_Group_Values> counter = new Counter<>();
        for (Joining_Group_Values key : groupValues.values()) {
            if (key == Joining_Group_Values.No_Joining_Group) {
                continue;
            }
            UnicodeSet set = groupValues.getSet(key);
            counter.add(key, set.size());
        }
        
        for (Joining_Type_Values key : typeValues.values()) {
            if (key == Joining_Type_Values.Non_Joining || key == Joining_Type_Values.Transparent) {
                continue;
            }
            UnicodeSet set = typeValues.getSet(key);
            for (String s : set) {
                Joining_Group_Values groupValue = groupValues.get(s);
                Script_Values scriptValue = scriptValues.get(s);
                Counter<Behavior> behaviorCounter = scriptToBehavior.get(scriptValue);
                if (behaviorCounter == null) {
                    scriptToBehavior.put(scriptValue, behaviorCounter = new Counter<Behavior>());
                }

                if (groupValue == Joining_Group_Values.No_Joining_Group) {
                    Age_Values ageValue = ageValues.get(s);
                    correctNonSingular.put(ageValue, s);
                    behaviorCounter.add(Behavior.no_group, 1);
                } else {
                    behaviorCounter.add(counter.get(groupValue) == 1 ? Behavior.distinct_singular : Behavior.non_singular, 1);
                }
            }
        }

        int lastCount = -1;
        Set<Joining_Group_Values> remaining = EnumSet.allOf(Joining_Group_Values.class);

        Multimap<Age_Values, Joining_Group_Values> ageToJg = TreeMultimap.create(Collections.reverseOrder(), Comparator.naturalOrder());
        Set<Joining_Group_Values> nonsingular = EnumSet.noneOf(Joining_Group_Values.class);
        
        for (R2<Long, Joining_Group_Values> entry : counter.getEntrySetSortedByCount(false, null)) {
            Joining_Group_Values key = entry.get1();
            remaining.remove(key);
            UnicodeSet set = groupValues.getSet(key);
            int count = set.size();
            if (count != lastCount) {
                //System.out.println("\n# " + count);
                lastCount = count;
            }
            for (String cp : set) {
                Age_Values ageValue = ageValues.get(cp);
                System.out.println(count 
                        + "\t" + ageValue.getShortName() 
                        + "\tU+" + Utility.hex(cp) 
                        + "\t" + key 
                        + "\t" + latest.getName(cp, ", "));
                if (count == 1) {
                    ageToJg.put(ageValue, key);
                } else {
                    nonsingular.add(key);
                }
            }
        }
        int count = 0;
        for (Entry<Age_Values, Collection<Joining_Group_Values>> e : ageToJg.asMap().entrySet()) {
            System.out.println(e.getKey().getShortName() + "\t" + e.getValue().size() + "\t" + CollectionUtilities.join(e.getValue(), ", "));
            count += e.getValue().size();
        }
        System.out.println("Total\t" + count);
        System.out.println("Nondegenerate" + "\t" + nonsingular.size() + "\t" + CollectionUtilities.join(nonsingular, ", "));
        System.out.println("Correct Singular" + "\t" + correctNonSingular.size());
        for (Entry<Age_Values, Collection<String>> entry : correctNonSingular.asMap().entrySet()) {
            Age_Values ageValue = entry.getKey();
            UnicodeSet uset = new UnicodeSet().addAll(entry.getValue());
            for (EntryRange range : uset.ranges()) {
                if (range.codepoint == range.codepointEnd) {
                    System.out.println(ageValue.getShortName() 
                    + "\tU+" + Utility.hex(range.codepoint) 
                    + "\t" + 1
                    + "\t" + typeValues.get(range.codepoint)
                    + "\t" + latest.getName(range.codepoint));
                } else {
                    System.out.println(ageValue.getShortName() 
                    + "\tU+" + Utility.hex(range.codepoint) + ".." + Utility.hex(range.codepointEnd)
                    + "\t" + (range.codepointEnd - range.codepoint + 1)
                    + "\t" + typeValues.get(range.codepoint) + ".."
                    + "\t" + latest.getName(range.codepoint) + ".." + latest.getName(range.codepointEnd));
                }
            }
        }

        for (Entry<Script_Values, Counter<Behavior>> entry : scriptToBehavior.entrySet()) {
            System.out.print(entry.getKey());
            for (Behavior item : Behavior.values()) {
                System.out.print("\t" + entry.getValue().get(item));
            }
            System.out.println();
        }
        
        if (remaining.size() != 0) {
            //System.out.println("\n# " + 0);
            for (Joining_Group_Values key : remaining) {
                System.out.println("?" + ";\t" + key + "\t# ");
            }
        }
    }
}
