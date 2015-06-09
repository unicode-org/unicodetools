package org.unicode.propstest;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyStatus;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UnicodePropertyException;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Idn_Status_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class TestInvariants extends TestFmwkPlus{
    public static void main(String[] args) {
        new TestInvariants().run(args);
    }

    private static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);
    private static final IndexUnicodeProperties iupLast = IndexUnicodeProperties.make(Settings.lastVersion);
    private static Map<UcdPropertyValues.Age_Values, IndexUnicodeProperties> IUPS 
    = new EnumMap<>(UcdPropertyValues.Age_Values.class);
    static {
        for (Age_Values age : UcdPropertyValues.Age_Values.values()) {
            if (age == Age_Values.Unassigned) {
                continue;
            }
            IUPS.put(age, IndexUnicodeProperties.make(age.getShortName()));
        }
    }

    public void TestAllUniformUnassigned() {
        UnicodeMap<General_Category_Values> gc = iup.loadEnum(UcdProperty.General_Category, UcdPropertyValues.General_Category_Values.class);
        for (General_Category_Values cat : EnumSet.of(
                General_Category_Values.Unassigned, 
                General_Category_Values.Private_Use,
                General_Category_Values.Surrogate
                )) {
            UnicodeSet cn = gc.getSet(cat);
            int firstCodePoint = cn.getRangeStart(0);
            for (UcdProperty prop : UcdProperty.values()) {
                PropertyStatus status = PropertyStatus.getPropertyStatus(prop);
                if (status.compareTo(PropertyStatus.Provisional) < 0) {
                    continue;
                }
                if (prop == UcdProperty.Bidi_Class) {
                    int debug = 0;
                }
                UnicodeMap<String> map = iup.load(prop);
                String firstValue = map.get(firstCodePoint);
                UnicodeSet trialCn = map.getSet(firstValue);
                if (!trialCn.containsAll(cn)) {
                    UnicodeSet mixed = new UnicodeSet(cn).removeAll(trialCn);
                    final int otherCodePoint = mixed.getRangeStart(0);
                    String otherValue = map.get(otherCodePoint);
                    errln("Mixed values for " + cat + ": " + prop + ":"
                            + "\t" + Utility.hex(firstCodePoint) + "→«" + firstValue + "»"
                            + "\t" + Utility.hex(otherCodePoint) + "→«" + otherValue + "»");

                }
            }
        }
    }

    public void checkGC() {
        UnicodeMap<General_Category_Values> lastGc = null;
        int total = 0;
        for (Entry<Age_Values, IndexUnicodeProperties> entry : IUPS.entrySet()) {
            UnicodeMap<General_Category_Values> gc = entry.getValue().loadEnum(UcdProperty.General_Category, UcdPropertyValues.General_Category_Values.class);
            int count = 0;
            if (lastGc != null) {
                final Age_Values cat = entry.getKey();
                for (General_Category_Values s : gc.values()) {
                    UnicodeSet keys = gc.getSet(s);
                    UnicodeMap<General_Category_Values> diff = new UnicodeMap<General_Category_Values>().putAll(lastGc).retainAll(keys);
                    for (General_Category_Values old : diff.values()) {
                        if (s.equals(old) || old.equals(General_Category_Values.Unassigned)) {
                            continue;
                        }
                        final UnicodeSet diffSet = diff.getSet(old);
                        count += diffSet.size();
                        logln(cat.getShortName() + "\t" + old + "\t=>\t" + s + "\t" + diffSet.toPattern(false));
                    }
                }
                logln(cat.getShortName() + "\tCount:\t" + count);
                total += count;
            }
            lastGc = gc;
        }
        logln("ALL\tCount:\t" + total);
    }

    //    public void TestStandarizedVariant() {
    //        CheckProps(UcdProperty.Standardized_Variant, null);
    //        UnicodeMap<String> currentStatus = iup.load(UcdProperty.Standardized_Variant);
    //        for (Entry<String, String> s : currentStatus.entrySet()) {
    //            System.out.println(s.getKey() + "\t" + Utility.hex(s.getKey()) + "\t" + s.getValue());
    //        }
    //    }

    public void TestNameAlias() {
        CheckProps(UcdProperty.Name_Alias, null);
        //CheckProps(UcdProperty.Identifier_Type, null);
    }

    public void TestIdna() {
        CheckProps(UcdProperty.Idn_Status, Idn_Status_Values.disallowed.toString());

        //        UnicodeMap<String> currentStatus = iup.load(UcdProperty.Idn_Status);
        //        UnicodeMap<String> oldStatus = iupLast.load(UcdProperty.Idn_Status);
        //        if (currentStatus.equals(oldStatus)) {
        //            errln("Should be a difference!");
        //        }
        //        for (Idn_Status_Values status : Idn_Status_Values.values()) {
        //            if (status == Idn_Status_Values.disallowed) {
        //                continue;
        //            }
        //            UnicodeSet oldSet = oldStatus.getSet(status.toString());
        //            UnicodeSet newSet = currentStatus.getSet(status.toString());
        //            if (!newSet.containsAll(oldSet)) {
        //                UnicodeSet missing = new UnicodeSet(oldSet).removeAll(newSet);
        //                errln("Compat problem, «" + status + "» needs to contain " + missing.toPattern(false));
        //            }
        //            //assertRelation(status.toString(), true, newSet, CONTAINS_US, oldSet);
        //        }
    }

    public void TestSecurity() {
        CheckProps(UcdProperty.Identifier_Status, null);
        CheckProps(UcdProperty.Identifier_Type, null);
    }

    public void CheckProps(UcdProperty ucdProperty, String skip) {
        UnicodeMap<String> currentStatus = iup.load(ucdProperty);
        UnicodeMap<String> oldStatus = iupLast.load(ucdProperty);
        if (currentStatus.equals(oldStatus)) {
            errln("Should be a difference in " + ucdProperty);
            System.out.println("old: " + oldStatus.get("↊"));
            System.out.println("new: " + currentStatus.get("↊"));
        }
        Set<String> values = new LinkedHashSet<>();
        values.addAll(currentStatus.getAvailableValues());
        values.addAll(oldStatus.getAvailableValues());
        if (skip != null) {
            values.remove(skip);
        }
        for (String status : values) {
            UnicodeSet oldSet = oldStatus.getSet(status);
            UnicodeSet newSet = currentStatus.getSet(status);
            if (!newSet.containsAll(oldSet)) {
                UnicodeSet missing = new UnicodeSet(oldSet).removeAll(newSet);
                errln("Compat problem, «" + status + "» needs to contain " + missing.toPattern(false));
            } else if (!oldSet.containsAll(newSet)) {
                UnicodeSet newOnes = new UnicodeSet(newSet).removeAll(oldSet);
                logln("FYI: New «" + status + "» doesn't contain " + newOnes.toPattern(false));
            }
            //assertRelation(status.toString(), true, newSet, CONTAINS_US, oldSet);
        }
    }

    private static final boolean SHOW_PROGRESS = false;

//    public void TestPropertyStatus() {
//        final EnumSet<UcdProperty>[] sets = new EnumSet[] {
//                PropertyStatus.IMMUTABLE_PROPERTY,
//                PropertyStatus.NORMATIVE_PROPERTY, PropertyStatus.INFORMATIVE_PROPERTY, PropertyStatus.PROVISIONAL_PROPERTY, PropertyStatus.CONTRIBUTORY_PROPERTY,
//                PropertyStatus.DEPRECATED_PROPERTY, PropertyStatus.STABLIZED_PROPERTY, PropertyStatus.OBSOLETE_PROPERTY};
//        final String[] names = {
//                "IMMUTABLE_PROPERTY",
//                "NORMATIVE_PROPERTY", "INFORMATIVE_PROPERTY", "PROVISIONAL_PROPERTY", "CONTRIBUTORY_PROPERTY",
//                "DEPRECATED_PROPERTY", "STABLIZED_PROPERTY", "OBSOLETE_PROPERTY"};
//
//        final EnumSet<UcdProperty> temp = EnumSet.noneOf(UcdProperty.class);
//        for (int i = 0; i < sets.length; ++i) {
//            final EnumSet<UcdProperty> item = sets[i];
//            final String name = names[i];
//            for (int j = i+1; j < sets.length; ++j) {
//                final EnumSet<UcdProperty> item2 = sets[j];
//                final String name2 = names[j];
//                if (item.containsAll(item2)) {
//                    if (SHOW_PROGRESS) {
//                        System.out.println(name + "\tcontains\t" + name2);
//                    }
//                } else if (item2.containsAll(item)) {
//                    if (SHOW_PROGRESS) {
//                        System.out.println(name2 + "\tcontains\t" + name);
//                    }
//                } else {
//                    temp.clear();
//                    temp.addAll(item);
//                    temp.retainAll(item2);
//                    if (temp.size() != 0) {
//                        if (SHOW_PROGRESS) {
//                            System.out.println(name + "\tintersects\t" + name2 + "\t" + temp);
//                        }
//                    }
//                }
//            }
//        }
//        throw new UnicodePropertyException();
//    }
}
