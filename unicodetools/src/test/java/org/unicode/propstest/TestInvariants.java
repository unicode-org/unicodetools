package org.unicode.propstest;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;
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
            IUPS.put(
                    age,
                    age == Age_Values.V2_1
                            ? IndexUnicodeProperties.make(VersionInfo.UNICODE_2_1_2)
                            : IndexUnicodeProperties.make(age.getShortName()));
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
                        UcdProperty.Extended_Pictographic,
                        UcdProperty.Names_List_Cross_Ref,
                        UcdProperty.Names_List_Comment,
                        UcdProperty.Names_List_Subheader,
                        UcdProperty.Names_List_Subheader_Notice));
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
            if (ucdProperty == UcdProperty.Idn_Status && Settings.latestVersion.equals("16.0.0")) {
                // Until Unicode 15.1, we had conditional Status values
                // disallowed_STD3_valid and disallowed_STD3_mapped.
                // At runtime, if UseSTD3ASCIIRules=true, they resolved to disallowed;
                // if UseSTD3ASCIIRules=false, they resolved to valid or mapped, respectively.
                // Unicode 16 replaces them with valid/mapped and handles UseSTD3ASCIIRules=true
                // while checking the Validity Criteria.
                switch (value) {
                    case "disallowed_STD3_valid":
                    case "disallowed_STD3_mapped":
                        continue;
                    case "valid":
                    case "mapped":
                        UnicodeSet disallowedSTD3 = oldStatus.getSet("disallowed_STD3_" + value);
                        oldSet.addAll(disallowedSTD3);
                        break;
                    default:
                        break;
                }
            }
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
                        && value.equals("Inclusion")
                        && Settings.latestVersion.equals("17.0.0")
                        && missing.size() == 1
                        && missing.containsAll("\u0375")) {
                    // Intentional removal of characters from Identifier_Type=Inclusion:
                    // [183-A75]: Change the Identifier_Type of U+0375 to Technical.
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
                if (ucdProperty == UcdProperty.Identifier_Type
                        && value.equals("Recommended")
                        && Settings.latestVersion.equals("17.0.0")
                        && missing.size() == 1110
                        && missing.containsAll(
                                new UnicodeSet(
                                        "[\\u0114\\u0115\\u012C\\u012D\\u0138\\u014E\\u014F\\u0156\\u0157\\u0162\\u0163\\u01D5-\\u01DC\\u01DE-\\u01E3\\u01EA-\\u01ED\\u01F0\\u01F4\\u01F5\\u01FA-\\u0217\\u021E\\u021F\\u0226-\\u0233\\u02BB\\u02BC\\u02EC\\u030F-\\u0311\\u0313\\u0314\\u0324\\u0325\\u032D\\u032E\\u0330\\u0335\\u0338\\u0339\\u0342\\u0345\\u037B-\\u037D\\u03FC-\\u0400\\u040D\\u0450\\u045D\\u048A-\\u048F\\u049C\\u049D\\u04A6\\u04A7\\u04B8\\u04B9\\u04C1-\\u04CE\\u04DA\\u04DB\\u04EA-\\u04ED\\u04F6\\u04F7\\u04FA-\\u04FF\\u0510-\\u0523\\u0526-\\u0529\\u052E\\u052F\\u0559\\u05B4\\u05EF-\\u05F2\\u063B\\u063C\\u063E\\u063F\\u0653\\u0671\\u0690\\u069B-\\u069E\\u06A3\\u06AC\\u06B2\\u06B4\\u06B8\\u06B9\\u06BF\\u06E5\\u06E6\\u06FA-\\u06FC\\u0750\\u0753-\\u0755\\u0757-\\u075F\\u0761\\u0764\\u0765\\u0769\\u076B-\\u076D\\u0772-\\u077F\\u0870-\\u0887\\u0889-\\u088E\\u08A1\\u08AA-\\u08AC\\u08B2\\u08B5-\\u08BA\\u08C3-\\u08C6\\u08C8\\u0904\\u090C\\u0929\\u0934\\u093D\\u0944\\u0950\\u0960-\\u0963\\u0971\\u0979\\u097A\\u097D\\u098C\\u09BD\\u09D7\\u09E0-\\u09E3\\u09FE\\u0A01\\u0A03\\u0A66-\\u0A6F\\u0A72-\\u0A74\\u0A81\\u0ABD\\u0AD0\\u0AE0-\\u0AE3\\u0AFA-\\u0AFF\\u0B0C\\u0B35\\u0B3D\\u0B55\\u0B57\\u0B60\\u0B61\\u0B66-\\u0B6F\\u0B82\\u0BD0\\u0BD7\\u0BE6-\\u0BEF\\u0C01\\u0C04\\u0C0C\\u0C31\\u0C3C\\u0C3D\\u0C55\\u0C56\\u0C5D\\u0C60\\u0C61\\u0C66-\\u0C6F\\u0C80\\u0C8C\\u0CB1\\u0CBC\\u0CBD\\u0CC4\\u0CD5\\u0CD6\\u0CDD\\u0CE0-\\u0CE3\\u0CF1-\\u0CF3\\u0D00\\u0D0C\\u0D29\\u0D3A\\u0D3D\\u0D4C\\u0D4E\\u0D54-\\u0D56\\u0D60\\u0D61\\u0D66-\\u0D6F\\u0D8E\\u0D9E\\u0E4E\\u0E86\\u0E89\\u0E8C\\u0E8E-\\u0E93\\u0E98\\u0EA0\\u0EA8\\u0EA9\\u0EAC\\u0EAF\\u0EBA\\u0ECE\\u0EDE\\u0EDF\\u0F00\\u0F35\\u0F37\\u0F3E\\u0F3F\\u0F6A-\\u0F6C\\u0F82\\u0F83\\u0F86-\\u0F8F\\u0FAE-\\u0FB0\\u0FC6\\u1050-\\u1059\\u1065-\\u1074\\u108B-\\u108E\\u1090-\\u109D\\u10F7-\\u10FA\\u10FD-\\u10FF\\u1207\\u1287\\u12AF\\u12F8-\\u12FF\\u130F\\u131F\\u1347\\u135A\\u135D-\\u135F\\u1380-\\u138F\\u179D\\u179E\\u17A9\\u17D7\\u17DC\\u1E00-\\u1E0B\\u1E0E-\\u1E11\\u1E14-\\u1E1F\\u1E22\\u1E23\\u1E26-\\u1E35\\u1E38-\\u1E3B\\u1E40\\u1E41\\u1E4C-\\u1E59\\u1E5C-\\u1E61\\u1E64-\\u1E6B\\u1E6E\\u1E6F\\u1E72-\\u1E8B\\u1E8E-\\u1E91\\u1E94-\\u1E99\\u1F00-\\u1F15\\u1F18-\\u1F1D\\u1F20-\\u1F45\\u1F48-\\u1F4D\\u1F50-\\u1F57\\u1F59\\u1F5B\\u1F5D\\u1F5F-\\u1F70\\u1F72\\u1F74\\u1F76\\u1F78\\u1F7A\\u1F7C\\u1F80-\\u1F9F\\u1FB0\\u1FB1\\u1FB6-\\u1FBA\\u1FBC\\u1FC2-\\u1FC4\\u1FC6-\\u1FC8\\u1FCA\\u1FCC\\u1FD0-\\u1FD2\\u1FD6-\\u1FDA\\u1FE0-\\u1FE2\\u1FE4-\\u1FEA\\u1FF2-\\u1FF4\\u1FF6-\\u1FF8\\u1FFA\\u1FFC\\u2D27\\u2D2D\\u2D80-\\u2D96\\u2DA0-\\u2DA6\\u2DA8-\\u2DAE\\u2DB0-\\u2DB6\\u2DB8-\\u2DBE\\u2DC0-\\u2DC6\\u2DC8-\\u2DCE\\u2DD0-\\u2DD6\\u2DD8-\\u2DDE\\u3099\\u309A\\uA67F\\uA717-\\uA71F\\uA788\\uA792\\uA793\\uA7C0-\\uA7CA\\uA7D0\\uA7D1\\uA7D3\\uA7D5-\\uA7D9\\uA9E7-\\uA9FE\\uAA60-\\uAA76\\uAA7A\\uAA7C-\\uAA7F\\uAB01-\\uAB06\\uAB09-\\uAB0E\\uAB11-\\uAB16\\uAB20-\\uAB26\\uAB28-\\uAB2E\\uAB66\\uAB67\\U0001133B\\U0001B11F-\\U0001B122\\U0001B132\\U0001B150-\\U0001B152\\U0001B155\\U0001B164-\\U0001B167\\U0001DF00-\\U0001DF1E\\U0001DF25-\\U0001DF2A\\U0001E08F]"))) {
                    // Intentional changes of Identifier_Type:
                    // [183-A72] Change Identifier_Type of 392 characters
                    // [183-A75] Change Identifier_Type of 753 characters
                    level = LOG;
                }
                if (ucdProperty == UcdProperty.Identifier_Type
                        && value.equals("Obsolete|Not_XID")
                        && Settings.latestVersion.equals("16.0.0")
                        && missing.size() == 1
                        && missing.containsAll("\u2E30")) {
                    // Changed from Obsolete to Exclusion due to
                    // [178-C39] Consensus: Add 51 entries to ScriptExtensions.txt
                    // as proposed in L2/23-280
                    // (And IdentifierInfo.java for some reason
                    // removes Obsolete when there is also Exclusion.)
                    level = LOG;
                }
                if (ucdProperty == UcdProperty.Identifier_Type
                        && value.equals("Limited_Use|Exclusion")
                        && Settings.latestVersion.equals("16.0.0")
                        && missing.size() == 1
                        && missing.containsAll("\uA9CF")) {
                    // PAG issue 217 -->
                    // [179-Cxx] Change the Identifier_Type of A9CF to Limited_Use Uncommon_Use,
                    // removing Exclusion.
                    level = LOG;
                }
                if (ucdProperty == UcdProperty.Identifier_Type
                        && value.equals("Uncommon_Use")
                        && Settings.latestVersion.equals("17.0.0")
                        && missing.size() == 22
                        && missing.containsAll(
                                new UnicodeSet(
                                        "[\\u0181\\u0186\\u0189\\u018A\\u018E\\u0190-\\u0192\\u0194\\u0196-\\u0199\\u019D\\u01B2-\\u01B4\\u01B7\\u01DD\\u0244\\u024C\\u024D]"))) {
                    // [183-A72] Change Identifier_Type of 392 characters
                    // [183-A75] Change Identifier_Type of 753 characters
                    level = LOG;
                }
                if (ucdProperty == UcdProperty.Identifier_Type
                        && value.equals("Uncommon_Use|Technical")
                        && Settings.latestVersion.equals("17.0.0")
                        && missing.size() == 12
                        && missing.containsAll(
                                "\u0253\u0254\u0256\u0257\u025B\u0263\u0268\u0269\u0272\u0289\u0292\u0DA6")) {
                    // [183-A72] Change Identifier_Type of 392 characters
                    // [183-A75] Change Identifier_Type of 753 characters
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
                if (ucdProperty == UcdProperty.Standardized_Variant
                                && Settings.latestVersion.equals("16.0.0")
                                && (value.equals("rotated 90 degrees")
                                        && missing.equals(
                                                new UnicodeSet(
                                                        "[{\\U00013092\\uFE00}{\\U0001333B\\uFE00}{\\U00013403\\uFE00}]")))
                        || (value.equals("rotated 180 degrees")
                                && missing.equals(new UnicodeSet("[{\\U000130A9\\uFE01}]")))) {
                    // [177-C18] Consensus: Rescind three Egyptian Hieroglyph variation
                    //                      sequences as described in document L2/23-254
                    //                      for Unicode version 16.0.
                    // UTC #180 should rescind another one, see
                    // https://github.com/unicode-org/sah/issues/378
                    // recommending that UTC Rescind the Egyptian Hieroglyph standardized variation
                    // sequence 1333B FE00 as described in document L2/24-177 for Unicode version
                    // 16.0.
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
                                + ":] doesn’t contain "
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
                                + ":] doesn’t contain "
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
