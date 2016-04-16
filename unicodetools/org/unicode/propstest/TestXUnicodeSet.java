package org.unicode.propstest;

import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.cldr.util.UnicodeProperty;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.text.UCD.Default;

import com.ibm.icu.text.UnicodeSet;

public class TestXUnicodeSet extends TestFmwkPlus{
    private static final UnicodeProperty name;
    private static final UnicodeProperty age;
    static {
        IndexUnicodeProperties foo = IndexUnicodeProperties.make(Default.ucdVersion());
        org.unicode.jsp.MySymbolTable.setDefaultXSymbolTable(foo);
        name = foo.getProperty("name");
        age = foo.getProperty("age");
        for (String prop : foo.getAvailableNames()) {
            //System.out.println(prop);
            UnicodeProperty property = foo.getProperty(prop);
        }
    }
    public static void main(String[] args) {
        new TestXUnicodeSet().run(args);
    }
    public void TestAge() {
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
    }
}
