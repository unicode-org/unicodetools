package org.unicode.jsptest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibm.icu.impl.UnicodeRegex;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.unicode.jsp.UnicodeJsp;
import org.unicode.jsp.UnicodeSetUtilities;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.text.utility.Settings;

public class TestUnicodeRegex {
    @Test
    public void TestPropertyData() {
        UnicodeRegex regex = UnicodeSetUtilities.getUnicodeRegex();
        // Other_ID_Start is UCD data that ICU's built-in property lookup does not expose.
        // Version qualifiers also require the unicodetools symbol table.
        for (String qualifier : new String[] {"", "U" + Settings.lastVersion + ":", "Udev:"}) {
            VersionInfo version =
                    qualifier.equals("Udev:")
                            ? Settings.LATEST_VERSION_INFO
                            : Settings.LAST_VERSION_INFO;
            UnicodeSet expected =
                    IndexUnicodeProperties.make(version)
                            .getProperty(UcdProperty.Other_ID_Start)
                            .getSet(Binary.Yes);
            String source = "\\p{" + qualifier + "Other_ID_Start}";
            assertEquals(expected, new UnicodeSet(regex.transform(source)), source);
        }
    }

    @Test
    public void TestBnfPropertyData() {
        UnicodeRegex regex = UnicodeSetUtilities.getUnicodeRegex();
        String bnf = regex.compileBnf("root = \\p{Udev:Other_ID_Start};");
        Pattern compiled = Pattern.compile(regex.transform(bnf));
        assertTrue(compiled.matcher("\u2118").matches());
        assertFalse(compiled.matcher("A").matches());
    }

    @Test
    public void TestRandomGenerationPropertyData() {
        assertEquals(
                "<p>\u2118</p>", UnicodeJsp.getBnf("[\\p{Udev:Other_ID_Start}&[\\u2118]]", 1, 1));
    }
}
