package org.unicode.propstest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyStatus;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Idn_Status_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.UnicodeSet;

public class TestInvariants extends TestFmwkPlus{
    static {
        System.setProperty("DISABLE_PROP_FILE_CACHE", "TRUE");
    }
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
    
    public void TestHanCompleteness() {
        final UnicodeMap<String> totalStrokes = iup.load(UcdProperty.kTotalStrokes);
        final UnicodeMap<String> mandarin = iup.load(UcdProperty.kMandarin);
        final UnicodeMap<String> radicalStroke = iup.load(UcdProperty.kRSUnicode);
        final UnicodeMap<String> hanyuPinyin = iup.load(UcdProperty.kHanyuPinyin);
        final UnicodeMap<Binary> ideographic = iup.loadEnum(UcdProperty.Ideographic, Binary.class);
        final UnicodeMap<String> haveDecomps = iup.load(UcdProperty.Decomposition_Mapping);
        UnicodeSet ideographicSet = ideographic.getSet(Binary.Yes);
        UnicodeSet missing;
        
        final UnicodeSet rs = radicalStroke.keySet();
        missing = new UnicodeSet(ideographicSet).removeAll(rs);
        assertEquals(UcdProperty.kRSUnicode.toString(), UnicodeSet.EMPTY, missing);

        UnicodeSet comparison = new UnicodeSet(rs).removeAll(haveDecomps.keySet());
        missing = new UnicodeSet(comparison).removeAll(totalStrokes.keySet());
        assertEquals(UcdProperty.kTotalStrokes.toString(), UnicodeSet.EMPTY, missing);
        
        missing = new UnicodeSet(comparison).removeAll(mandarin.keySet());
        assertEquals(UcdProperty.kMandarin.toString(), UnicodeSet.EMPTY, missing);
        
        missing.retainAll(hanyuPinyin.keySet());
        assertEquals("Could be added from hanyuPinyin", UnicodeSet.EMPTY, missing);

    }

    public void TestUniformUnassigned() {
        UnicodeMap<General_Category_Values> gc = iup.loadEnum(UcdProperty.General_Category, UcdPropertyValues.General_Category_Values.class);
        //UnicodeMap<String> map1 = iup.load(UcdProperty.Bidi_Class);

        Relation<General_Category_Values, UcdProperty> exceptions = Relation.of(new EnumMap(General_Category_Values.class), HashSet.class);
        exceptions.putAll(General_Category_Values.Unassigned, Arrays.asList(
                UcdProperty.NFKC_Casefold, 
                UcdProperty.Age,
                UcdProperty.Block,
                UcdProperty.Bidi_Class,
                UcdProperty.East_Asian_Width,
                UcdProperty.Grapheme_Cluster_Break,
                UcdProperty.Line_Break,
                UcdProperty.Changes_When_NFKC_Casefolded,
                UcdProperty.Default_Ignorable_Code_Point,
                UcdProperty.Noncharacter_Code_Point,
                UcdProperty.Pattern_Syntax
                ));
        exceptions.putAll(General_Category_Values.Private_Use, Arrays.asList(
                UcdProperty.Age,
                UcdProperty.Block
                ));
        exceptions.put(General_Category_Values.Surrogate, 
                UcdProperty.Block
                );

        List<UcdProperty> ordered = new ArrayList<>();
        //ordered.add(UcdProperty.Bidi_Class);
        ordered.addAll(Arrays.asList(UcdProperty.values()));
        for (final General_Category_Values cat : EnumSet.of(
                General_Category_Values.Unassigned, 
                General_Category_Values.Private_Use,
                General_Category_Values.Surrogate
                )) {
            UnicodeSet cn = gc.getSet(cat);
            int firstCodePoint = cn.getRangeStart(0);
            Set<UcdProperty> exceptionSet = exceptions.get(cat);
            for (final UcdProperty prop : ordered) {
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
                    final int msgType = exceptionSet == null || !exceptionSet.contains(prop) ? ERR : LOG;
                    msg("Mixed values for " 
                            + cat + ":\t" 
                            + prop + ":"
                            + "\t" + Utility.hex(firstCodePoint) + "→«" + firstValue + "»"
                            + "\t" + Utility.hex(otherCodePoint) + "→«" + otherValue + "»",
                            msgType, true, true);
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

    public void TestStandarizedVariant() {
        CheckProps(UcdProperty.Standardized_Variant);
        //        UnicodeMap<String> currentStatus = iup.load(UcdProperty.Standardized_Variant);
        //        for (Entry<String, String> s : currentStatus.entrySet()) {
        //            System.out.println(s.getKey() + "\t" + Utility.hex(s.getKey()) + "\t" + s.getValue());
        //        }
    }

    public void TestNameAlias() {
        CheckProps(UcdProperty.Name_Alias);
    }

    public void TestIdna() {
        CheckProps(UcdProperty.Idn_Status, Idn_Status_Values.disallowed.toString());
    }

    public void TestSecurity() {
        CheckProps(UcdProperty.Identifier_Status, "Restricted", "Allowed");
        CheckProps(UcdProperty.Identifier_Type, "Not_Character", "Not_XID",  
                "Not_NFKC", "Default_Ignorable", "Technical", "Obsolete", "Limited_Use",
                "recommended", "historic" // old values
                );
    }

    public void CheckProps(UcdProperty ucdProperty, String... skip) {
        UnicodeMap<String> currentStatus = iup.load(ucdProperty);
        UnicodeMap<String> oldStatus = iupLast.load(ucdProperty);
        Set<String> values = new LinkedHashSet<>();
        values.addAll(currentStatus.getAvailableValues());
        values.addAll(oldStatus.getAvailableValues());
        HashSet<String> skips = new HashSet<>(Arrays.asList(skip));
        boolean same = true;
        for (String value : values) {
            UnicodeSet oldSet = oldStatus.getSet(value);
            UnicodeSet newSet = currentStatus.getSet(value);
            same &= oldSet.equals(newSet);
            if (!newSet.containsAll(oldSet)) {
                UnicodeSet missing = new UnicodeSet(oldSet).removeAll(newSet);
                msg(ucdProperty + " new «" + value + "» does’t contain " + missing.toPattern(false),
                        skips.contains(value) ? LOG : ERR, true, true);
            }
            if (!oldSet.containsAll(newSet)) {
                UnicodeSet newOnes = new UnicodeSet(newSet).removeAll(oldSet);
                logln(ucdProperty + " old «" + value + "» doesn't contain " + newOnes.toPattern(false));
            }
            //assertRelation(status.toString(), true, newSet, CONTAINS_US, oldSet);
        }
        if (same) {
            errln("Should be a difference in " + ucdProperty);
        } else if (currentStatus.equals(oldStatus)) {
            warnln("Unicode map equals needs fixing");
        }
    }

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
