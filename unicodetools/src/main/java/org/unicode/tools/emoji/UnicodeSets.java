package org.unicode.tools.emoji;

import java.util.List;

import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.text.UnicodeSetSpanner;
import com.ibm.icu.text.UnicodeSetSpanner.CountMethod;

public class UnicodeSets {
    public static String removeAll(UnicodeSet toRemove, String input, List<String> extracted) {
        UnicodeSetSpanner spanner = new UnicodeSetSpanner(toRemove);
        String extractedString = spanner.replaceFrom(input, "", CountMethod.MIN_ELEMENTS, SpanCondition.NOT_CONTAINED);
        for (int cp : CharSequences.codePoints(extractedString)) {
            extracted.add(UTF16.valueOf(cp));
        }
        return spanner.deleteFrom(input);
    }
}
