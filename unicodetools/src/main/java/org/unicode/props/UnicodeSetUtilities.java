package org.unicode.props;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class UnicodeSetUtilities {
    // should be method on UnicodeSet, much more efficient that way

    public static Set<String> getMulticharacterStrings(UnicodeSet source) {
        final Set<String> result = new LinkedHashSet<String>();
        for (final UnicodeSetIterator it = new UnicodeSetIterator(source); it.next(); ) {
            if (it.codepoint == UnicodeSetIterator.IS_STRING) {
                result.add(it.string);
            }
        }
        return result;
    }
}
