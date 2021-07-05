package org.unicode.propstest;

import org.unicode.cldr.util.Timer;
import org.unicode.cldr.util.props.UnicodeProperty;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.ValueCardinality;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;

public class CheckXmlProperties {
    /**
     * TODO Known problems
     * 
Property  cp  xml     unicodetools
Numeric_Value   109F7   [2/12]  [1/6]
... Formatting issue with rationals

Property     cp  xml     unicodetools
Lowercase_Mapping   0041    [a] [A]
Titlecase_Mapping   0061    [A] [a]
Uppercase_Mapping   0061    [A] [a]
...The unicodetools (new version) don't add to the simple mappings

Property     cp  xml     unicodetools
Name    3400    [CJK UNIFIED IDEOGRAPH-#]   [CJK UNIFIED IDEOGRAPH-3400]
... Need to convert # to hex in the test.

The following, including the warnings, are due to differences in the "missing values".

Property     cp  xml     unicodetools
Bidi_Paired_Bracket 0000    [0000]  null
... The missing value according to PropertyValueAliases is "<none>", which the unicodeTools express as null, but ucdxml has cp for.
# @missing: 0000..10FFFF; Bidi_Paired_Bracket; <none>

Property     cp  xml     unicodetools
Canonical_Combining_Class   0378    [Not_Reordered] null
Joining_Group   0000    [No_Joining_Group]  null
Joining_Type    0000    [Non_Joining]   null
... The unicodetools should use Not_Reordered etc. for the missing value.

Warnings (reported as "empty" differences):
kAccountingNumeric
kOtherNumeric
kPrimaryNumeric
kCompatibilityVariant

Failing tests:  9
Warning tests:  4
     */
    private final static String ucdVersion = Utility.searchPath[0];
    static final boolean LONG_TEST = true;
    private static final boolean INCLUDE_UNIHAN = true && LONG_TEST;
    private static final int MAX_LINE_COUNT = Integer.MAX_VALUE; // 4000; // Integer.MAX_VALUE;

    static final UcdProperty[] SHORT_LIST = new UcdProperty[] {
        //                UcdProperty.Lowercase_Mapping,
        UcdProperty.Script_Extensions,
        //                UcdProperty.Titlecase_Mapping,
        //                UcdProperty.Uppercase_Mapping
    };

    public static void main(String[] args) {
        final Timer timer = new Timer();

        System.out.println("Loading Index Props");
        timer.start();
        final IndexUnicodeProperties iup = IndexUnicodeProperties.make(ucdVersion);
        timer.stop();
        System.out.println(timer);

        System.out.println("Loading XML Props");
        timer.start();
        final XMLProperties xmlProps = new XMLProperties(
                Settings.UnicodeTools.DATA_DIR + "/ucdxml/" + ucdVersion + "/",
                INCLUDE_UNIHAN, MAX_LINE_COUNT);
        timer.stop();
        System.out.println(timer);
        int failedTests = 0;
        int warningTests = 0;
        int okTests = 0;

        final UnicodeMap<String> empty = new UnicodeMap<String>();
        final UnicodeMap<String> errorMap = new UnicodeMap<String>();
        final UcdProperty[] testValues = CheckXmlProperties.LONG_TEST ? UcdProperty.values() : SHORT_LIST;
        for (final UcdProperty prop : testValues) {
            System.out.println("\nTESTING\t" + prop);
            final UnicodeMap<String> xmap = xmlProps.getMap(prop);
            if (xmap.size() == 0) {
                System.out.println("*No XML Values*");
                continue;
            }
            int errors = 0;
            empty.clear();
            errorMap.clear();
            for (int i = 0; i <= 0x10ffff; ++i) {
                final String xval = XMLProperties.getXmlResolved(prop, i, xmap.get(i));
                String ival = iup.getResolvedValue(prop, i);
                if (prop.getCardinality() != ValueCardinality.Singleton && prop != UcdProperty.Script_Extensions) {
                    ival = ival == null ? null : ival.replace('|', ' ');
                }
                if (!UnicodeProperty.equals(xval, ival)) {
                    // for debugging
                    final String xx = XMLProperties.getXmlResolved(prop, i, xmap.get(i));
                    final String ii = iup.getResolvedValue(prop, i);

                    if (xval == null || xval.isEmpty()) {
                        empty.put(i, ival);
                    } else {
                        if (errors == 0) {
                            System.out.println("\nProperty\t cp\t xml\t unicodetools");
                        }
                        if (++errors < 11) {
                            System.out.println(prop 
                                    + "\t" + Utility.hex(i) 
                                    + "\t" + XMLProperties.show(xval) 
                                    + "\t" + XMLProperties.show(ival));
                        }
                        errorMap.put(i, XMLProperties.show(xval) 
                                    + "\t" + XMLProperties.show(ival));
                    }
                }
            }
            if (errors == 0 && empty.size() == 0) {
                System.out.println("*OK*\t" + prop);
                okTests++;
            } else {
                if (errors != 0) {
                    System.out.println("*FAIL*\t" + prop + " with " + errors + " errors.");
                    int showCount = 0;
                    for (String value : errorMap.values()) {
                        System.out.println(value + "\t" + errorMap.getSet(value).toPattern(false));
                        if (++showCount > 100) {
                            break;
                        }
                    }
                    failedTests++;
                }
                if (empty.size() != 0) {
                    System.out.println("*WARNING*\t" + prop + " with " + (empty.size()) + " “empty” differences.");
                    warningTests++;
                    if (empty.size() != 0) {
                        System.out.println("*Empty/null XML Values:\t" + empty.size());
                        int maxCount = 0;
                        for (final String ival : empty.values()) {
                            if (++maxCount > 10) {
                                break;
                            }
                            System.out.println(ival + "\t" + empty.getSet(ival));
                        }
                    }
                }
            }
        }
        System.out.println("Failing tests:\t" + failedTests);
        System.out.println("Warning tests:\t" + warningTests);
    }
}
