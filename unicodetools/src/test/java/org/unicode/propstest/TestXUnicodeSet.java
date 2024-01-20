package org.unicode.propstest;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.UCD.Default;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestXUnicodeSet extends TestFmwkMinusMinus {
    private static final UnicodeProperty name;
    private static final UnicodeProperty age;
    private static IndexUnicodeProperties IUP = IndexUnicodeProperties.make(Default.ucdVersion());

    static {
        name = IUP.getProperty("name");
        age = IUP.getProperty("age");
        for (String prop : IUP.getAvailableNames()) {
            // System.out.println(prop);
            UnicodeProperty property = IUP.getProperty(prop);
        }
    }
}
