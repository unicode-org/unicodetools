package org.unicode.text.utility;

import com.ibm.icu.text.UTF16;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.DerivedProperty;
import org.unicode.text.UCD.UCDProperty;
import org.unicode.text.UCD.UCD_Types;

public class UtilityBase implements UCD_Types {

    public static String getDisplay(int cp) {
        String result = UTF16.valueOf(cp);
        final byte cat = Default.ucd().getCategory(cp);
        if (cat == Mn || cat == Me) {
            result = String.valueOf(DOTTED_CIRCLE) + result;
        } else if (cat == Cf || cat == Cc || cp == 0x034F || cp == 0x00AD || cp == 0x1806) {
            result = "\u25A1";
        } else {
            if (UtilityBase.defaultIgnorable == null) {
                UtilityBase.defaultIgnorable = DerivedProperty.make(DefaultIgnorable);
            }
            if (UtilityBase.defaultIgnorable.hasValue(cp)) {
                result = "\u25A1";
            }
        }
        return result;
    }

    static UCDProperty defaultIgnorable = null;

    public static final String HTML_HEAD =
            "<html>\n"
                    + "<head>\n"
                    + "<script async src='https://www.googletagmanager.com/gtag/js?id=UA-19876713-1'>"
                    + "</script>\n"
                    + "<script>\nwindow.dataLayer = window.dataLayer || [];\n"
                    + "function gtag(){dataLayer.push(arguments);}\n"
                    + "gtag('js', new Date());\n"
                    + "gtag('config', 'UA-19876713-1');\n"
                    + "</script>\n"
                    + "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>\n";
}
