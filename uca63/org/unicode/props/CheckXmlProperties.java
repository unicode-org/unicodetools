package org.unicode.props;

import org.unicode.cldr.util.Timer;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeProperty;

public class CheckXmlProperties {
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
        Timer timer = new Timer();

        System.out.println("Loading Index Props");
        timer.start();
        IndexUnicodeProperties iup = IndexUnicodeProperties.make("6.2.0");
        timer.stop();
        System.out.println(timer);

        System.out.println("Loading XML Props");
        timer.start();
        XMLProperties props = new XMLProperties(Utility.UCD_DIRECTORY, INCLUDE_UNIHAN, MAX_LINE_COUNT);
        timer.stop();
        System.out.println(timer);

        UnicodeMap<String> empty = new UnicodeMap<String>();
        System.out.println("\nFormat:\nProperty\tcp\txml\tindexed");
        final UcdProperty[] testValues = CheckXmlProperties.LONG_TEST ? UcdProperty.values() : SHORT_LIST;
        for (UcdProperty prop : testValues) {
            System.out.println("\nTESTING\t" + prop);
            UnicodeMap<String> xmap = props.getMap(prop);
            if (xmap.size() == 0) {
                System.out.println("*No XML Values");
                continue;
            }
            int errors = 0;
            empty.clear();
            for (int i = 0; i <= 0x10ffff; ++i) {
                String xval = XMLProperties.getXmlResolved(prop, i, xmap.get(i));
                String ival = iup.getResolvedValue(prop, i);
                if (!UnicodeProperty.equals(xval, ival)) {
                    // for debugging
                    String xx = XMLProperties.getXmlResolved(prop, i, xmap.get(i));
                    String ii = iup.getResolvedValue(prop, i);

                    if (xval == null || xval.isEmpty()) {
                        empty.put(i, ival);
                    } else {
                        System.out.println(prop + "\t" + Utility.hex(i) + "\t" + XMLProperties.show(xval) + "\t" + XMLProperties.show(ival));
                        if (++errors > 10) {
                            break;
                        }
                    }
                }
            }
            if (errors == 0 && empty.size() == 0) {
                System.out.println("*OK*\t" + prop);
            } else {
                System.out.println("*FAIL*\t" + prop + " with " + (errors + empty.size()) + " errors.");
                if (empty.size() != 0) {
                    System.out.println("*Empty/null XML Values:\t" + empty.size());
                    int maxCount = 0;
                    for (String ival : empty.values()) {
                        if (++maxCount > 10) break;
                        System.out.println(ival + "\t" + empty.getSet(ival));
                    }
                }
            }
        }
    }


}
