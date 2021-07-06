package org.unicode.idna;

import com.ibm.icu.text.UnicodeSet;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.unicode.text.utility.UnicodeTransform;
import org.unicode.text.utility.UnicodeTransform.Type;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestTransform extends TestFmwkMinusMinus{
    @Test
    @Disabled("Broken - Requested array size exceeds VM limit in org.unicode.idna.FilteredUnicodeTransform.transform(FilteredUnicodeTransform.java:31)    ")
    public void TestBasic() {
        final UnicodeTransform filteredTransform = new FilteredUnicodeTransform(UnicodeTransform.getInstance(Type.CASEFOLD), new UnicodeSet("[N-Z]"));
        assertEquals("case A-M only", "ABCKLMnopxyz", filteredTransform.transform("ABCKLMNOPXYZ"));
    }
}
