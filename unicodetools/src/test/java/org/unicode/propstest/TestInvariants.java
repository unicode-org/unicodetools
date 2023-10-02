package org.unicode.propstest;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.UnicodeSet;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyStatus;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Idn_Status_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestInvariants extends TestFmwkMinusMinus {
    static {
        System.setProperty("DISABLE_PROP_FILE_CACHE", "TRUE");
    }

    private static final IndexUnicodeProperties iup =
            IndexUnicodeProperties.make(Settings.latestVersion);
    private static final UnicodeMap<Script_Values> SCRIPTS =
            iup.loadEnum(UcdProperty.Script, Script_Values.class);
    private static final IndexUnicodeProperties iupLast =
            IndexUnicodeProperties.make(Settings.lastVersion);
    private static Map<UcdPropertyValues.Age_Values, IndexUnicodeProperties> IUPS =
            new EnumMap<>(UcdPropertyValues.Age_Values.class);

    static {
        for (Age_Values age : UcdPropertyValues.Age_Values.values()) {
            if (age == Age_Values.Unassigned) {
                continue;
            }
            IUPS.put(age, IndexUnicodeProperties.make(age.getShortName()));
        }
    }

    static final UnicodeMap<String> totalStrokes = iup.load(UcdProperty.kTotalStrokes);
    static final UnicodeMap<String> mandarin = iup.load(UcdProperty.kMandarin);
    static final UnicodeMap<String> radicalStroke = iup.load(UcdProperty.kRSUnicode);
    static final UnicodeMap<String> hanyuPinyin = iup.load(UcdProperty.kHanyuPinyin);
    static final UnicodeSet ideographic =
            iup.loadEnum(UcdProperty.Ideographic, Binary.class).getSet(Binary.Yes);
    static final UnicodeSet unified_Ideograph =
            iup.loadEnum(UcdProperty.Unified_Ideograph, Binary.class).getSet(Binary.Yes);
    static final UnicodeMap<String> haveDecomps = iup.load(UcdProperty.Decomposition_Mapping);
    static final UnicodeSet ideographicSet = unified_Ideograph;
    static final UnicodeSet DECOMPOSABLE = haveDecomps.keySet();

    @Disabled("Broken")
    @Test
    public void TestHanCompleteness() {

        UnicodeSet missing;

        UnicodeSet ideoMinusTangut =
                new UnicodeSet(ideographic)
                        .removeAll(SCRIPTS.getSet(Script_Values.Tangut))
                        .removeAll(SCRIPTS.getSet(Script_Values.Nushu));
        missing = new UnicodeSet(ideoMinusTangut).removeAll(ideographicSet);

        showMissing(
                WARN,
                "In Ideograph (minus Tangut, Nushu) but not Unified_Ideograph: ",
                UcdProperty.Unified_Ideograph,
                missing);

        final UnicodeSet rs = radicalStroke.keySet();
        missing = new UnicodeSet(ideographicSet).removeAll(rs);
        showMissing(
                WARN,
                "In unified_Ideograph but no kRSUnicode value: ",
                UcdProperty.kRSUnicode,
                missing);

        UnicodeSet comparison = new UnicodeSet(rs).removeAll(DECOMPOSABLE);
        missing = new UnicodeSet(comparison).removeAll(totalStrokes.keySet());
        showMissing(
                ERR,
                "Has kRSUnicode value but no kTotalStrokes value: ",
                UcdProperty.kTotalStrokes,
                missing);

        missing = new UnicodeSet(comparison).removeAll(mandarin.keySet());
        showMissing(
                WARN,
                "Has kRSUnicode value but no kMandarin value: ",
                UcdProperty.kMandarin,
                missing);

        missing.retainAll(hanyuPinyin.keySet());
        if (!missing.isEmpty()) {
            messageln(ERR, "kMandarin value could be added from hanyuPinyin: ", missing);
        }
    }

    private void showMissing(
            int warnVsError, String title, final UcdProperty prop, UnicodeSet missing) {
        if (!UnicodeSet.EMPTY.equals(missing)) {
            messageln(warnVsError, title, missing);
            if (DECOMPOSABLE.containsSome(missing)) {
                missing.removeAll(DECOMPOSABLE);
                messageln(warnVsError, "After removing decomps: " + title, missing);
            }
        }
    }

    private void messageln(int warnVsError, String message, UnicodeSet missing) {
        messageln(
                warnVsError,
                message + "count=" + missing.size() + ", values=" + missing.toPattern(false));
    }

    private void messageln(int warnVsError, String stringToShow) {
        msg(stringToShow, warnVsError, true, true);
    }

    @Test
    public void TestUniformUnassigned() {
        UnicodeMap<General_Category_Values> gc =
                iup.loadEnum(
                        UcdProperty.General_Category,
                        UcdPropertyValues.General_Category_Values.class);
        // UnicodeMap<String> map1 = iup.load(UcdProperty.Bidi_Class);

        Relation<General_Category_Values, UcdProperty> exceptions =
                Relation.of(new EnumMap(General_Category_Values.class), HashSet.class);
        exceptions.putAll(
                General_Category_Values.Unassigned,
                Arrays.asList(
                        UcdProperty.NFKC_Casefold,
                        UcdProperty.NFKC_Simple_Casefold,
                        UcdProperty.Age,
                        UcdProperty.Block,
                        UcdProperty.Bidi_Class,
                        UcdProperty.East_Asian_Width,
                        UcdProperty.Grapheme_Cluster_Break,
                        UcdProperty.Line_Break,
                        UcdProperty.Changes_When_NFKC_Casefolded,
                        UcdProperty.Default_Ignorable_Code_Point,
                        UcdProperty.Noncharacter_Code_Point,
                        UcdProperty.Pattern_Syntax,
                        UcdProperty.Vertical_Orientation,
                        UcdProperty.Extended_Pictographic));
        exceptions.putAll(
                General_Category_Values.Private_Use,
                Arrays.asList(UcdProperty.Age, UcdProperty.Block));
        exceptions.put(General_Category_Values.Surrogate, UcdProperty.Block);

        List<UcdProperty> ordered = new ArrayList<>();
        // ordered.add(UcdProperty.Bidi_Class);
        ordered.addAll(Arrays.asList(UcdProperty.values()));
        for (final General_Category_Values cat :
                EnumSet.of(
                        General_Category_Values.Unassigned,
                        General_Category_Values.Private_Use,
                        General_Category_Values.Surrogate)) {
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
                    final int msgType =
                            exceptionSet == null || !exceptionSet.contains(prop) ? ERR : LOG;
                    messageln(
                            msgType,
                            "Mixed values for "
                                    + cat
                                    + ":\t"
                                    + prop
                                    + ":"
                                    + "\t"
                                    + Utility.hex(firstCodePoint)
                                    + "→«"
                                    + firstValue
                                    + "»"
                                    + "\t"
                                    + Utility.hex(otherCodePoint)
                                    + "→«"
                                    + otherValue
                                    + "»");
                }
            }
        }
    }

    public void checkGC() {
        UnicodeMap<General_Category_Values> lastGc = null;
        int total = 0;
        for (Entry<Age_Values, IndexUnicodeProperties> entry : IUPS.entrySet()) {
            UnicodeMap<General_Category_Values> gc =
                    entry.getValue()
                            .loadEnum(
                                    UcdProperty.General_Category,
                                    UcdPropertyValues.General_Category_Values.class);
            int count = 0;
            if (lastGc != null) {
                final Age_Values cat = entry.getKey();
                for (General_Category_Values s : gc.values()) {
                    UnicodeSet keys = gc.getSet(s);
                    UnicodeMap<General_Category_Values> diff =
                            new UnicodeMap<General_Category_Values>()
                                    .putAll(lastGc)
                                    .retainAll(keys);
                    for (General_Category_Values old : diff.values()) {
                        if (s.equals(old) || old.equals(General_Category_Values.Unassigned)) {
                            continue;
                        }
                        final UnicodeSet diffSet = diff.getSet(old);
                        count += diffSet.size();
                        logln(
                                cat.getShortName()
                                        + "\t"
                                        + old
                                        + "\t=>\t"
                                        + s
                                        + "\t"
                                        + diffSet.toPattern(false));
                    }
                }
                logln(cat.getShortName() + "\tCount:\t" + count);
                total += count;
            }
            lastGc = gc;
        }
        logln("ALL\tCount:\t" + total);
    }

    @Test
    public void TestStandarizedVariant() {
        CheckProps("TestStandarizedVariant", WARN, UcdProperty.Standardized_Variant);
        //        UnicodeMap<String> currentStatus = iup.load(UcdProperty.Standardized_Variant);
        //        for (Entry<String, String> s : currentStatus.entrySet()) {
        //            System.out.println(s.getKey() + "\t" + Utility.hex(s.getKey()) + "\t" +
        // s.getValue());
        //        }
    }

    @Test
    public void TestNameAlias() {
        // CheckProps() does not work well for characters with multiple aliases.
        // The UnicodeMap stores multiple values as a string with list, and when a new value is
        // added,
        // then the list is longer but looks to the function like a different value.
        // As a result, it fails with what looks like an instability.
        CheckProps("TestNameAlias", WARN, UcdProperty.Name_Alias, "END OF MEDIUM; EOM");
    }

    @Test
    public void TestIdna() {
        CheckProps(
                "TestIdna", WARN, UcdProperty.Idn_Status, Idn_Status_Values.disallowed.toString());
    }

    @Test
    public void TestSecurity() {
        CheckProps(
                "TestSecurity Identifier_Status",
                WARN,
                UcdProperty.Identifier_Status,
                "Restricted",
                "Allowed");
        CheckProps(
                "TestSecurity Identifier_Type",
                WARN,
                UcdProperty.Identifier_Type,
                "Not_Character",
                "Not_XID",
                "Not_NFKC",
                "Default_Ignorable",
                "Technical",
                "Obsolete",
                "Limited_Use",
                "recommended",
                "historic" // old values
                );
    }

    public void CheckProps(
            String testName, int warnVsError, UcdProperty ucdProperty, String... skip) {
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
                int level = skips.contains(value) ? LOG : ERR;
                if (ucdProperty == UcdProperty.Identifier_Type
                        && value.equals("Inclusion")
                        && Settings.latestVersion.equals("15.0.0")
                        && missing.size() == 2
                        && missing.containsAll("\u200C\u200D")) {
                    // Intentional removal of characters from Identifier_Type=Inclusion:
                    // UTC AI 171-A128: ... remove the Joiner_Control characters ZWJ and ZWNJ
                    // from Identifier_Type=Inclusion and Identifier_Status=Allowed.
                    level = LOG;
                }
                if (ucdProperty == UcdProperty.Identifier_Type
                        && value.equals("Recommended")
                        && Settings.latestVersion.equals("15.0.0")
                        && missing.size() == 3
                        && missing.containsAll("\uA7AE\uA7B8\uA7B9")) {
                    // Intentional removal of characters from Identifier_Type=Recommended:
                    // [172-A61] Change the Identifier_Type of U+A7AE to Technical
                    // [172-A62] Change the Identifier_Type of U+A7B8 and U+A7B9 to Uncommon_Use
                    level = LOG;
                }
                if (ucdProperty == UcdProperty.Idn_Status
                        && value.equals("disallowed_STD3_valid")
                        && Settings.latestVersion.equals("15.1.0")
                        && missing.size() == 3
                        && missing.containsAll("\u2260\u226E\u226F")) {
                    // [175-A86] In IdnaMappingTable.txt,
                    // change U+2260 (≠), U+226E (≮), and U+226F (≯)
                    // from disallowed_STD3_valid to valid, for Unicode 15.1.
                    level = LOG;
                }
                msg(
                        testName
                                + ": Unicode "
                                + Settings.latestVersion
                                + " [:"
                                + ucdProperty
                                + "="
                                + value
                                + ":] does’t contain "
                                + missing.toPattern(true),
                        level,
                        true,
                        true);
            }
            if (!oldSet.containsAll(newSet)) {
                UnicodeSet newOnes = new UnicodeSet(newSet).removeAll(oldSet);
                logln(
                        testName
                                + ": Unicode "
                                + Settings.lastVersion
                                + " [:"
                                + ucdProperty
                                + "="
                                + value
                                + ":] does’t contain "
                                + newOnes.toPattern(true));
            }
            // assertRelation(status.toString(), true, newSet, CONTAINS_US, oldSet);
        }
        if (same) {
            messageln(
                    warnVsError,
                    testName
                            + ": Should have a difference compared to last version: "
                            + ucdProperty);
        } else if (currentStatus.equals(oldStatus)) {
            warnln(testName + ": Unicode map equals needs fixing");
        }
    }

    //    public void TestPropertyStatus() {
    //        final EnumSet<UcdProperty>[] sets = new EnumSet[] {
    //                PropertyStatus.IMMUTABLE_PROPERTY,
    //                PropertyStatus.NORMATIVE_PROPERTY, PropertyStatus.INFORMATIVE_PROPERTY,
    // PropertyStatus.PROVISIONAL_PROPERTY, PropertyStatus.CONTRIBUTORY_PROPERTY,
    //                PropertyStatus.DEPRECATED_PROPERTY, PropertyStatus.STABLIZED_PROPERTY,
    // PropertyStatus.OBSOLETE_PROPERTY};
    //        final String[] names = {
    //                "IMMUTABLE_PROPERTY",
    //                "NORMATIVE_PROPERTY", "INFORMATIVE_PROPERTY", "PROVISIONAL_PROPERTY",
    // "CONTRIBUTORY_PROPERTY",
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
