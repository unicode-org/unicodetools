package org.unicode.propstest;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Settings;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.UnicodeMap;

public class TestCodePointMap extends TestFmwk {
    public static void main(String[] args) {
        new TestCodePointMap().run(args);
    }
    private static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);

    public void TestEach () {
        for (UcdProperty prop : UcdProperty.values()) {
            UnicodeMap<String> map = iup.load(prop);
            //CodePointTrie map2;
        }
    }
}
