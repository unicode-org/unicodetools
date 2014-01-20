package org.unicode.text.utility;

import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.DerivedProperty;
import org.unicode.text.UCD.UCDProperty;
import org.unicode.text.UCD.UCD_Types;

import com.ibm.icu.text.UTF16;

public class UtilityBase  implements UCD_Types {

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

}
