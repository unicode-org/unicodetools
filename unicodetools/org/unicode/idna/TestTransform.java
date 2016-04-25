package org.unicode.idna;

import org.unicode.text.utility.UnicodeTransform;
import org.unicode.text.utility.UnicodeTransform.Type;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.UnicodeSet;

public class TestTransform extends TestFmwk{
    public static void main(String[] args) {
        new TestTransform().run(args);
    }
    public void TestBasic() {
        final UnicodeTransform filteredTransform = new FilteredUnicodeTransform(UnicodeTransform.getInstance(Type.CASEFOLD), new UnicodeSet("[N-Z]"));
        assertEquals("case A-M only", "ABCKLMnopxyz", filteredTransform.transform("ABCKLMNOPXYZ"));
    }
}
