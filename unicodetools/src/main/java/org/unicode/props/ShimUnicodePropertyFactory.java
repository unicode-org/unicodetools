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
import org.unicode.props.UcdPropertyValues.Idn_Status_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.props.UnicodeProperty.BaseProperty;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.UCD.Normalizer.NormalizationForm;
import org.unicode.text.utility.Utility;

/**
 * ShimUnicodePropertyFactory is designed to assist with deprecating UCD.java and the parsing code
 * that it uses, and changing ToolUnicodePropertySource.java to just depend on
 * IndexUnicodeProperies.java. <br>
 * This file is expected to be removed once the deprecation is complete.
 */
public class ShimUnicodePropertyFactory extends UnicodeProperty.Factory {

    private static final Splitter BAR_SPLITTER = Splitter.on('|').trimResults();

    private UnicodeProperty scriptProp;

    // Set these from IndexUnicodeProperties to be current
    private UnicodeSet defaultTransparent; // new UnicodeSet("[[:Cf:][:Me:][:Mn:]]");
    private UnicodeSet control; // new UnicodeSet("[:Cc:]");
    private UnicodeMap<Idn_Status_Values> idnStatus;

    public ShimUnicodePropertyFactory(IndexUnicodeProperties factory) {

        scriptProp = factory.getProperty(UcdProperty.Script);
        idnStatus = factory.loadEnum(UcdProperty.Idn_Status, Idn_Status_Values.class);
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
                    // The default is <none> in BidiMirroring.txt, but TUP incorrectly has it as
                    // <code point>.
                    prop =
                            replaceCpValues(
                                    prop,
                                    (cp, oldValue) ->
                                            oldValue == null ? UTF16.valueOf(cp) : oldValue);
                    break;
                case "Bidi_Paired_Bracket":
                    // The default is <none> in PropertyValueAliases.txt, but TUP incorrectly
                    // has it as U+0000.
                    prop = replaceValues(prop, oldValue -> oldValue == null ? "\u0000" : oldValue);
                    break;
                case "FC_NFKC_Closure":
                    // The default is <code point> in PropertyValueAliases.txt, but TUP incorrectly
                    // has it as <none>.
                    prop =
                            replaceCpValues(
                                    prop, (cp, oldValue) -> fixFC_NFKC_Closure(cp, oldValue));

                    break;
                case "Name":
                    // TUP reports the special label <control-XXXX> as the value of the Name
                    // property. This is incorrect, the actual value of the Name property is "" for
                    // those, see https://www.unicode.org/versions/latest/ch04.pdf#G135245 and
                    // https://www.unicode.org/reports/tr44/#Name.
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
                    // The following are not really supported in TUP; values are all "". So just
                    // match that.
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
                    // prop = replaceValues(prop, oldValue -> oldValue == null ? "" : oldValue);
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

        // Add properties that are not UCD

        add(
                new UnicodeProperty.UnicodeSetProperty()
                        .set("[\\u0000-\\u007F]")
                        .setMain("ASCII", "ASCII", UnicodeProperty.EXTENDED_BINARY, ""));

        for (NormalizationForm nFormat : NormalizationForm.values()) {
            UnicodeMap<String> toMap = new UnicodeMap<>();
            UnicodeSet isSet = new UnicodeSet();
            Normalizer normalizer = new Normalizer(nFormat, factory);
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                String result = normalizer.normalize(cp);
                toMap.put(cp, result);
                if (equalsString(cp, result)) {
                    isSet.add(cp);
                }
            }
            add(
                    new UnicodeProperty.UnicodeSetProperty()
                            .set(isSet)
                            .setMain(
                                    "is" + nFormat.name(),
                                    "is" + nFormat.name(),
                                    UnicodeProperty.BINARY,
                                    ""));
            add(
                    new UnicodeProperty.UnicodeMapProperty()
                            .set(toMap)
                            .setMain(
                                    "to" + nFormat.name(),
                                    "to" + nFormat.name(),
                                    UnicodeProperty.STRING,
                                    ""));
        }

        // The following two are vacuous, but match TUP

        add(
                new UnicodeProperty.UnicodeMapProperty()
                        .set(makeMap(cp -> "No"))
                        .setMain(
                                "Case_Fold_Turkish_I",
                                "Case_Fold_Turkish_I",
                                UnicodeProperty.STRING,
                                ""));

        add(
                new UnicodeProperty.UnicodeMapProperty()
                        .set(makeMap(cp -> "No"))
                        .setMain("Non_Break", "Non_Break", UnicodeProperty.EXTENDED_BINARY, ""));
    }

    // Unused, as far as I can tell
    //   add(new UnicodeProperty.UnicodeMapProperty()
    //       .set(makeMap(cp -> idnOutput(cp)))
    //       .setMain("IdnOutput", "IdnOutput", UnicodeProperty.EXTENDED_BINARY,""));
    //    String idnOutput(int cp) {
    //        Idn_Status_Values stat = idnStatus.getValue(cp);
    //        return stat == Idn_Status_Values.disallowed_STD3_valid
    //               || stat == Idn_Status_Values.valid ? "Yes" : "No";
    //    }

    /**
     * Build a new property from an existing one, with a function that takes a code point and old
     * value. <br>
     * NOTE: doesn't handle mappings from String to result, since we don't need to change properties
     * of strings. These build a copy of a map rather than modifying a map, because it is
     * <b>much</b> faster to set values sequentially in code point order.
     */
    private static UnicodeProperty replaceCpValues(
            UnicodeProperty prop, BiFunction<Integer, String, String> replace) {
        UnicodeMap<String> map = prop.getUnicodeMap().freeze();
        UnicodeMap<String> newMap = makeModifiedUnicodeMap(map, replace);
        return copyPropReplacingMap(prop, newMap);
    }

    /**
     * Build a new property from an existing one, with a function that takes just the old value.
     * <br>
     * NOTE: doesn't handle mappings from String to result, since we don't need to change properties
     * of strings. These build a copy of a map rather than modifying a map, because it is
     * <b>much</b> faster to set values sequentially in code point order.
     */
    private static UnicodeProperty replaceValues(
            UnicodeProperty prop, Function<String, String> replace) {
        if (prop.getName().equals("kOtherNumeric")) {
            int debug = 0;
        }
        UnicodeMap<String> map = prop.getUnicodeMap().freeze();
        // generally much faster to create a new map than alter an old
        UnicodeMap<String> newMap = makeModifiedMap(map, replace);
        return copyPropReplacingMap(prop, newMap);
    }

    public static BaseProperty copyPropReplacingMap(
            UnicodeProperty prop, UnicodeMap<String> newMap) {
        return new UnicodeProperty.UnicodeMapProperty()
                .set(newMap.freeze())
                .setMain(
                        prop.getName(),
                        prop.getFirstNameAlias(),
                        prop.getType(),
                        prop.getVersion());
        // TODO: add name aliases
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
        } else {
            return value;
        }
    }

    private String fixFC_NFKC_Closure(int cp, String oldValue) {
        if (equalsString(cp, oldValue)) {
            return null;
        } else {
            return oldValue;
        }
    }

    /** Very useful. May already be in ICU, but not sure. */
    public boolean equalsString(int codepoint, String value) {
        return codepoint == value.codePointAt(0)
                && value.length() == Character.charCount(codepoint);
    }

    // UnicodeMap utilities
    /**
     * Make a modified UnicodeMap efficiently, where the new values depend on the codepoint and
     * source value.
     */
    public static <T> UnicodeMap<T> makeModifiedUnicodeMap(
            UnicodeMap<T> sourceMap, BiFunction<Integer, T, T> replacer) {
        // generally much faster to create a new map than alter an old
        UnicodeMap<T> newMap = new UnicodeMap<>();
        // Should add map.entryRangesWithNull() that returns entry ranges with range.value == null
        int gapCp = 0;
        for (UnicodeMap.EntryRange<T> range : sourceMap.entryRanges()) {
            // handle null gaps
            for (; gapCp < range.codepoint; ++gapCp) {
                newMap.put(gapCp, replacer.apply(gapCp, null));
            }
            for (int cp = range.codepoint; cp <= range.codepointEnd; ++cp) {
                newMap.put(cp, replacer.apply(cp, range.value));
            }
            gapCp = range.codepointEnd + 1;
        }
        // handle null gaps
        for (; gapCp < 0x110000; ++gapCp) {
            newMap.put(gapCp, replacer.apply(gapCp, null));
        }
        return newMap;
    }

    /**
     * Make a modified UnicodeMap efficiently, where the new values depend on just the source value.
     */
    public static <T> UnicodeMap<T> makeModifiedMap(
            UnicodeMap<T> sourceMap, Function<T, T> replacer) {
        UnicodeMap<T> newMap = new UnicodeMap<>();
        int gapCp = 0;
        for (UnicodeMap.EntryRange<T> range : sourceMap.entryRanges()) {
            // handle null gaps
            newMap.putAll(gapCp, range.codepoint - 1, replacer.apply(null));
            newMap.putAll(range.codepoint, range.codepointEnd, replacer.apply(range.value));
            gapCp = range.codepointEnd + 1;
        }
        // handle null gaps
        newMap.putAll(gapCp, 0x10FFFF, replacer.apply(null));
        return newMap;
    }

    /** Make a UnicodeMap, from a function that maps a code point to a value. */
    public static <T> UnicodeMap<T> makeMap(Function<Integer, T> setter) {
        UnicodeMap<T> newMap = new UnicodeMap<>();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            newMap.put(cp, setter.apply(cp));
        }
        return newMap;
    }
}
