package org.unicode.propstest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.props.UnicodeProperty;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.text.UCD.Default;
import org.unicode.unittest.TestFmwkMinusMinus;

import com.ibm.icu.text.UnicodeSet;

public class TestXUnicodeSet extends TestFmwkMinusMinus {
    private static final UnicodeProperty name;
    private static final UnicodeProperty age;
    private static IndexUnicodeProperties IUP = IndexUnicodeProperties.make(Default.ucdVersion());

    static {
        name = IUP.getProperty("name");
        age = IUP.getProperty("age");
        for (String prop : IUP.getAvailableNames()) {
            //System.out.println(prop);
            UnicodeProperty property = IUP.getProperty(prop);
        }
    }


    @Disabled("Broken")
    @Test
    public void TestAge() {
        try {
            org.unicode.jsp.MySymbolTable.setDefaultXSymbolTable(IUP);

            UnicodeSet v70 = new UnicodeSet("[:age=7.0:]").complement().complement();
            UnicodeSet v63 = new UnicodeSet("[:age=6.3:]").complement().complement();
            assertNotEquals("", UnicodeSet.EMPTY, v70);
            assertNotEquals("", 0, v70.size());
            assertNotEquals("", 0, v63.size());
            //        for (String s : new UnicodeSet("[[:age=7.0:]-[:age=6.3:]]")) {
            //            System.out.println(Utility.hex(s) + "\t" + name.getValue(s.codePointAt(0)));
            //        }
            //        for (String s : new UnicodeSet("[:name=/MARK/:]")) {
            //            System.out.println(Utility.hex(s) + "\t" + age.getValue(s.codePointAt(0)) + "\t" + name.getValue(s.codePointAt(0)));
            //        }
        } finally {
            org.unicode.jsp.MySymbolTable.setDefaultXSymbolTable(null);
        }
    }
}
