package org.unicode.props;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.locale.XCldrStub.Splitter;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import java.lang.reflect.Method;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.unicode.cldr.util.Rational;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.props.UnicodeProperty.BaseProperty;
import org.unicode.text.utility.Utility;

public class ShimUnicodePropertyFactory extends UnicodeProperty.Factory {

    private static final Splitter BAR_SPLITTER = Splitter.on('|').trimResults();

    private UnicodeProperty scriptProp;

    // Set these from IndexUnicodeProperties to be current
    private UnicodeSet defaultTransparent; // new UnicodeSet("[[:Cf:][:Me:][:Mn:]]");
    private UnicodeSet control; // new UnicodeSet("[:Cc:]");

    public ShimUnicodePropertyFactory(IndexUnicodeProperties factory) {
        scriptProp = factory.getProperty(UcdProperty.Script);
        UnicodeProperty idnaProp = factory.getProperty(UcdProperty.Idn_Status);
        UnicodeProperty gc = factory.getProperty(UcdProperty.General_Category);
        control = gc.getSet(UcdPropertyValues.General_Category_Values.Control).freeze();
        defaultTransparent =
                new UnicodeSet()
                        .addAll(gc.getSet(UcdPropertyValues.General_Category_Values.Format))
                        .addAll(
                                gc.getSet(
                                        UcdPropertyValues.General_Category_Values.Nonspacing_Mark))
                        .addAll(gc.getSet(UcdPropertyValues.General_Category_Values.Enclosing_Mark))
                        .removeAll(
                                new UnicodeSet(
                                        "[\\x{600}-\\x{605} \\x{6DD} \\x{890}-\\x{891} \\x{8E2} \\x{180E} \\x{200C} \\x{2066}-\\x{2069} \\x{110BD} \\x{110CD}]"))
                        .freeze();
        ;

        for (String propName : factory.getAvailableNames()) {
            UnicodeProperty prop = factory.getProperty(propName);
            switch (propName) {
                case "Bidi_Mirroring_Glyph":
                    prop =
                            replaceCpValues(
                                    prop,
                                    (cp, oldValue) ->
                                            oldValue == null ? UTF16.valueOf(cp) : oldValue);
                    break;
                case "Bidi_Paired_Bracket":
                    prop = replaceValues(prop, oldValue -> oldValue == null ? "\u0000" : oldValue);
                    break;
                case "FC_NFKC_Closure":
                    prop =
                            replaceCpValues(
                                    prop, (cp, oldValue) -> fixFC_NFKC_Closure(cp, oldValue));

                    break;
                case "Joining_Type":
                    prop = replaceCpValues(prop, (cp, oldValue) -> fixJoining_Type(cp, oldValue));
                    break;
                case "Joining_Group":
                    prop = modifyJoining_Group(prop);
                    break;
                case "Jamo_Short_Name":
                    prop = modifyJamo_Short_Name(prop);
                    break;
                case "Name":
                    // prop = modifyName(prop);
                    prop = replaceCpValues(prop, (cp, x) -> fixName(cp, x));
                    break;
                case "Numeric_Value":
                    prop = replaceValues(prop, x -> fixNumericValue(x));
                    break;
                case "Script_Extensions":
                    prop = replaceCpValues(prop, (cp, x) -> fixScript_Extensions(cp, x));
                    break;
                case "Unicode_1_Name":
                    prop = replaceValues(prop, oldValue -> oldValue == null ? "" : oldValue);
                    break;
                // The following are "fake" in ToolUnicodeProperty
                // I think they are just present for the types and aliases
                case "ISO_Comment":
                    prop =
                    copyPropReplacingMap(
                            prop,
                            new UnicodeMap<String>().putAll(0, 0x10ffff, "").freeze());
            break;

                case "Name_Alias":
                case "kAccountingNumeric":
                case "kCompatibilityVariant":
                case "kIICore":
                case "kIRG_GSource":
                case "kIRG_HSource":
                case "kIRG_JSource":
                case "kIRG_KPSource":
                case "kIRG_KSource":
                case "kIRG_MSource":
                case "kIRG_SSource":
                case "kIRG_TSource":
                case "kIRG_UKSource":
                case "kIRG_USource":
                case "kIRG_VSource":
                case "kOtherNumeric":
                case "kPrimaryNumeric":
                case "kRSUnicode":
                    prop = replaceValues(prop, oldValue -> "");
                    //prop = replaceValues(prop, oldValue -> oldValue == null ? "" : oldValue);
                    break;
                default:
                    add(prop);
                    continue;
            }
            try {
                System.out.println("adding " + prop.getName());
                add(prop);
            } catch (Exception e) {
                int debug = 0;
            }
        }

        add(
                new UnicodeProperty.UnicodeSetProperty()
                        .set("[\\u0000-\\u007F]")
                        .setMain("ASCII", "ASCII", UnicodeProperty.EXTENDED_BINARY, ""));
// TODO
//SEVERE   Case_Fold_Turkish_I MISSING
//SEVERE  IdnOutput   MISSING
//SEVERE  Non_Break   MISSING
//SEVERE  isNFC   MISSING
//SEVERE  isNFD   MISSING
//SEVERE  isNFKC  MISSING
//SEVERE  isNFKD  MISSING
//SEVERE  toNFC   MISSING
//SEVERE  toNFD   MISSING
//SEVERE  toNFKC  MISSING
//SEVERE  toNFKD  MISSING

        UnicodeProperty prop = new UnicodeProperty.UnicodeMapProperty()
                        .set(makeMap(cp -> "valid".equals(idnaProp.getValue(cp)) ? "Yes" : "No"))
                        .setMain("IdnOutput", "idnOut", UnicodeProperty.EXTENDED_BINARY, "")
                .setMain("IdnOutput", "idnOut", UnicodeProperty.EXTENDED_BINARY, factory.getUcdVersion().getVersionString(2, 2));
                add(prop);
    }

    public String getIdna(int cp) {
        return "Yes"; // ;
    }
    private UnicodeMap<String> makeMap(Function<Integer, String> setter) {
        UnicodeMap<String> newMap = new UnicodeMap<>();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            newMap.put(cp, setter.apply(cp));
        }
        return newMap;
    }
    /**
     * Build a new property from an existing one, with a function that takes a code point and old
     * value. <br>
     * NOTE: doesn't handle mappings from String to result, since we don't need to change properties
     * of strings. These build a copy of a map rather than modifying a map, because it is
     * <b>much</b> faster to set values sequentially in code point order.
     */
    private UnicodeProperty replaceCpValues(
            UnicodeProperty prop, BiFunction<Integer, String, String> replace) {
        UnicodeMap<String> map = prop.getUnicodeMap().freeze();
        // generally much faster to create a new map than alter an old
        UnicodeMap<String> newMap = new UnicodeMap<>();
        // Should add map.entryRangesWithNull() that returns entry ranges with range.value == null
        int gapCp = 0;
        for (UnicodeMap.EntryRange<String> range : map.entryRanges()) {
            // handle null gaps
            for (; gapCp < range.codepoint; ++gapCp) {
                newMap.put(gapCp, replace.apply(gapCp, null));
            }
            for (int cp = range.codepoint; cp <= range.codepointEnd; ++cp) {
                newMap.put(cp, replace.apply(cp, range.value));
            }
            gapCp = range.codepointEnd + 1;
        }
        // handle null gaps
        for (; gapCp < 0x110000; ++gapCp) {
            newMap.put(gapCp, replace.apply(gapCp, null));
        }
        return copyPropReplacingMap(prop, newMap);
    }

    /**
     * Build a new property from an existing one, with a function that takes just the old value.
     * <br>
     * NOTE: doesn't handle mappings from String to result, since we don't need to change properties
     * of strings. These build a copy of a map rather than modifying a map, because it is
     * <b>much</b> faster to set values sequentially in code point order.
     */
    private UnicodeProperty replaceValues(UnicodeProperty prop, Function<String, String> replace) {
        if (prop.getName().equals("kOtherNumeric")) {
            int debug = 0;
        }
        UnicodeMap<String> map = prop.getUnicodeMap().freeze();
        // generally much faster to create a new map than alter an old
        UnicodeMap<String> newMap = new UnicodeMap<>();
        int gapCp = 0;
        for (UnicodeMap.EntryRange<String> range : map.entryRanges()) {
            // handle null gaps
            newMap.putAll(gapCp, range.codepoint - 1, replace.apply(null));
            newMap.putAll(range.codepoint, range.codepointEnd, replace.apply(range.value));
            gapCp = range.codepointEnd + 1;
        }
        // handle null gaps
        newMap.putAll(gapCp, 0x10FFFF, replace.apply(null));
        return copyPropReplacingMap(prop, newMap);
    }

    public String fixScript_Extensions(int cp, String oldValue) {
        String shortScriptValue = getShortName(Script_Values.class, scriptProp.getValue(cp));
        String tupValue = getTUPScript_ExtensionShortNames(oldValue);
        return tupValue.equals(shortScriptValue) ? null : tupValue;
    }

    /**
     * Generalized version of UcdPropertyValues.Script_Values.forName(scriptValue).getShortName();
     */
    public String getShortName(Class vClass, String scriptValue) {
        try {
            Method forName = vClass.getMethod("forName", String.class);
            Enum x = (Enum) forName.invoke(null, scriptValue);
            Method getShortName = x.getClass().getMethod("getShortName");
            return (String) getShortName.invoke(x);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String getTUPScript_ExtensionShortNames(String iupValue) {
        if (!iupValue.contains("|")) {
            return getShortName(Script_Values.class, iupValue);
        }
        return BAR_SPLITTER.splitToList(iupValue).stream()
                .map(x -> getShortName(Script_Values.class, x))
                .collect(Collectors.joining(" "));
    }

    static NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);

    static {
        nf.setGroupingUsed(false);
        nf.setMaximumFractionDigits(8);
        nf.setMinimumFractionDigits(1);
    }

    private String fixNumericValue(String value) {
        if (value == null || value == "") {
            return null;
        } else {
            Rational iupRational;
            iupRational = Rational.of(value);
            if (iupRational.equals(Rational.NaN)) {
                return null;
            } else {
                double doubleValue =
                        iupRational.numerator.doubleValue() / iupRational.denominator.doubleValue();
                return nf.format(doubleValue);
            }
        }
    }

    private String fixName(int cp, String value) {
        if (control.contains(cp)) {
            return "<control-" + Utility.hex(cp, 4) + ">";
        } else if (value != null && value.contains("#")) {
            return value.replace("#", Utility.hex(cp, 4));
        } else {
            return value;
        }
    }

    private String fixFC_NFKC_Closure(int cp, String oldValue) {
        if (oldValue.equals("<code point>") || singletonEquals(cp, oldValue)) {
            return null;
        } else {
            return oldValue;
        }
    }

    // Joining_Type needs fix in IUP
    private String fixJoining_Type(int cp, String oldValue) {
        if (defaultTransparent.contains(cp) && "Non_Joining".equals(oldValue)) {
            return "Transparent";
        } else {
            return oldValue;
        }
    }

    // Jamo_Short_Name needs fix in IUP
    private UnicodeProperty modifyJamo_Short_Name(UnicodeProperty prop) {
        return copyPropReplacingMap(prop, prop.getUnicodeMap().put('ᄋ', ""));
    }

    // Joining_Group needs fix in IUP (really, in UCD data)
    private UnicodeProperty modifyJoining_Group(UnicodeProperty prop) {
        return copyPropReplacingMap(prop, prop.getUnicodeMap().put('ۃ', "Teh_Marbuta_Goal"));
    }

    /** Very useful. May already be in ICU, but not sure. */
    boolean singletonEquals(int codepoint, String value) {
        int first = value.codePointAt(0);
        return first == codepoint && value.length() == Character.charCount(codepoint);
    }

    public BaseProperty copyPropReplacingMap(UnicodeProperty prop, UnicodeMap<String> newMap) {
        return new UnicodeProperty.UnicodeMapProperty()
                .set(newMap.freeze())
                .setMain(
                        prop.getName(),
                        prop.getFirstNameAlias(),
                        prop.getType(),
                        prop.getVersion());
        // TODO: add name aliases
    }
}
