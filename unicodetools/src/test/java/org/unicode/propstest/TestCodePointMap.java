package org.unicode.propstest;

import com.ibm.icu.dev.util.UnicodeMap;
import org.junit.jupiter.api.Test;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Settings;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestCodePointMap extends TestFmwkMinusMinus {
    private static final IndexUnicodeProperties iup =
            IndexUnicodeProperties.make(Settings.latestVersion);

    @Test
    public void TestEach() {
        for (UcdProperty prop : UcdProperty.values()) {
            UnicodeMap<String> map = iup.load(prop);
            // CodePointTrie map2;
        }
    }
}
