package org.unicode.propstest;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.unicode.cldr.util.Timer;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UnicodeProperty;
import org.unicode.props.ValueCardinality;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

public class CheckXmlProperties {
    /**
     * TODO Known problems
     *
     * <p>Property cp xml unicodetools Numeric_Value 109F7 [2/12] [1/6] ... Formatting issue with
     * rationals
     *
     * <p>Property cp xml unicodetools Lowercase_Mapping 0041 [a] [A] Titlecase_Mapping 0061 [A] [a]
     * Uppercase_Mapping 0061 [A] [a] ...The unicodetools (new version) don't add to the simple
     * mappings
     *
     * <p>Property cp xml unicodetools Name 3400 [CJK UNIFIED IDEOGRAPH-#] [CJK UNIFIED
     * IDEOGRAPH-3400] ... Need to convert # to hex in the test.
     *
     * <p>The following, including the warnings, are due to differences in the "missing values".
     *
     * <p>Property cp xml unicodetools Bidi_Paired_Bracket 0000 [0000] null ... The missing value
     * according to PropertyValueAliases is "<none>", which the unicodeTools express as null, but
     * ucdxml has cp for. # @missing: 0000..10FFFF; Bidi_Paired_Bracket; <none>
     *
     * <p>Property cp xml unicodetools Canonical_Combining_Class 0378 [Not_Reordered] null
     * Joining_Group 0000 [No_Joining_Group] null Joining_Type 0000 [Non_Joining] null ... The
     * unicodetools should use Not_Reordered etc. for the missing value.
     *
     * <p>Warnings (reported as "empty" differences): kAccountingNumeric kOtherNumeric
     * kPrimaryNumeric kCompatibilityVariant
     *
     * <p>Failing tests: 9 Warning tests: 4
     */
    static final boolean LONG_TEST = true;

    static final boolean VERBOSE =
            false; // should change this into regular JUnit test, but for now just make easier to
    // read.
    static final boolean SHOW_WARNINGS = false;
    static final int MAX_SHOW = 20;

    private static final boolean INCLUDE_UNIHAN = true && LONG_TEST;
    private static final int MAX_LINE_COUNT = Integer.MAX_VALUE; // 4000; // Integer.MAX_VALUE;

    static final UcdProperty[] SHORT_LIST =
            new UcdProperty[] {
                //                UcdProperty.Lowercase_Mapping,
                UcdProperty.Script_Extensions,
                //                UcdProperty.Titlecase_Mapping,
                //                UcdProperty.Uppercase_Mapping
            };

    public static void main(String[] args) {
        final Timer timer = new Timer();
        System.out.println("For details, set VERBOSE, MAX_SHOW, and/or SHOW_WARNINGS");

        System.out.println("Loading Index Props");
        timer.start();
        final IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);
        timer.stop();
        System.out.println(timer);

        System.out.println("Loading XML Props");
        timer.start();
        final String folder = Settings.UnicodeTools.getDataPathStringForLatestVersion("ucdxml");
        final XMLProperties xmlProps = new XMLProperties(folder, INCLUDE_UNIHAN, MAX_LINE_COUNT);
        timer.stop();
        System.out.println(timer);
        int failedTests = 0;
        int warningTests = 0;
        int okTests = 0;

        final UnicodeSet empty = new UnicodeSet();
        final UnicodeMap<String> errorMap = new UnicodeMap<String>();
        final UcdProperty[] testValues =
                CheckXmlProperties.LONG_TEST ? UcdProperty.values() : SHORT_LIST;
        for (final UcdProperty prop : testValues) {
            final UnicodeMap<String> xmap = xmlProps.getMap(prop);
            if (xmap.size() == 0) {
                System.out.println("*No XML Values*\t" + prop);
                continue;
            }
            empty.clear();
            errorMap.clear();
            for (int i = 0; i <= 0x10ffff; ++i) {
                String xval = XMLProperties.getXmlResolved(prop, i, xmap.get(i));
                String upval = iup.getResolvedValue(prop, i);
                if (prop == UcdProperty.Name && upval != null) {
                    upval = upval.replace("#", Utility.hex(i, 4));
                } else if (prop == UcdProperty.Bidi_Paired_Bracket && xval != null) {
                    if (xval.startsWith("\\u00")) {
                        xval = Utility.fromHex(xval.substring(2));
                    }
                }
                if (prop.getCardinality() != ValueCardinality.Singleton
                        && prop != UcdProperty.Script_Extensions
                        && prop != UcdProperty.kEH_Func
                        && prop != UcdProperty.kEH_FVal) {
                    upval = upval == null ? null : upval.replace('|', ' ');
                }
                if (prop == UcdProperty.kEH_Func) {
                    xval = xval == null ? null : xval.replace("/", "|");
                }
                if (prop == UcdProperty.kEH_FVal) {
                    xval = xval == null ? null : xval.replaceAll("\\s*\\|\\s*", "|");
                }
                if (!UnicodeProperty.equals(xval, upval)) {
                    // just for debugging
                    final String xx = XMLProperties.getXmlResolved(prop, i, xmap.get(i));
                    final String ii = iup.getResolvedValue(prop, i);
                    // UCDXML only contains cjkNumeric values when they are not null/NaN.
                    switch (prop) {
                        case kAccountingNumeric:
                        case kOtherNumeric:
                        case kPrimaryNumeric:
                            if (upval != null && upval.equals("NaN") && xval == null) {
                                empty.add(i);
                            } else {
                                errorMap.put(
                                        i,
                                        " codepoints where up=\t"
                                                + XMLProperties.show(upval)
                                                + "\t& xml=\t"
                                                + XMLProperties.show(xval));
                            }
                            break;
                        case Name_Alias:
                            // Name_Alias is sorted for display in UCDXML.
                            List<String> xList = Arrays.asList(xval.split("; "));
                            List<String> upList =
                                    (ii != null ? Arrays.asList(ii.split("\\|")) : null);
                            // Collections.sort(xList);
                            Collections.sort(upList);
                            if (!xList.equals(upList)) {
                                errorMap.put(
                                        i,
                                        " codepoints where up=\t"
                                                + XMLProperties.show(upval)
                                                + "\t& xml=\t"
                                                + XMLProperties.show(xval));
                            }
                            break;
                        case kEH_Core:
                            if (upval != null && upval.equals("None") && xval == null) {
                                empty.add(i);
                            } else {
                                errorMap.put(
                                        i,
                                        " codepoints where up=\t"
                                                + XMLProperties.show(upval)
                                                + "\t& xml=\t"
                                                + XMLProperties.show(xval));
                            }
                            break;
                        case kEH_NoMirror:
                        case kEH_NoRotate:
                            if (upval != null && upval.equals("No") && xval == null) {
                                empty.add(i);
                            } else {
                                errorMap.put(
                                        i,
                                        " codepoints where up=\t"
                                                + XMLProperties.show(upval)
                                                + "\t& xml=\t"
                                                + XMLProperties.show(xval));
                            }
                            break;
                        default:
                            if ((xval == null
                                            || xval.isEmpty()
                                            || xval.equals(Character.toString(i)))
                                    && (upval == null
                                            || upval.isEmpty()
                                            || upval.equals(Character.toString(i)))) {
                                empty.add(i);
                            } else {
                                errorMap.put(
                                        i,
                                        " codepoints where up=\t"
                                                + XMLProperties.show(upval)
                                                + "\t& xml=\t"
                                                + XMLProperties.show(xval));
                            }
                    }
                }
            }
            if (errorMap.size() == 0 && empty.size() == 0) {
                okTests++;
                if (VERBOSE) {
                    System.out.println("*OK*\t" + prop);
                }
            } else {
                if (errorMap.size() != 0) {
                    failedTests++;
                    System.out.println(
                            "*FAIL*\t"
                                    + prop
                                    + " has "
                                    + errorMap.size()
                                    + " codepoints where up != xml.");
                    int showCount = 0;
                    for (String value : errorMap.values()) {
                        System.out.println(
                                "\t" + value + "\t: " + errorMap.getSet(value).toString());
                        if (++showCount > MAX_SHOW) {
                            System.out.println("… " + (errorMap.size() - MAX_SHOW) + " more");
                            break;
                        }
                    }
                }
                if (empty.size() != 0) {
                    warningTests++;
                    if (SHOW_WARNINGS) {
                        System.out.println(
                                "*WARNING*\t"
                                        + prop
                                        + " has "
                                        + (empty.size())
                                        + " codepoints where up=\tnull\t& xml=\t\"\"");
                        System.out.println("\t" + empty.toPattern(false));
                    }
                }
            }
        }
        System.out.println("Failing tests:\t" + failedTests);
        System.out.println("Warning tests:\t" + warningTests);
    }
}
