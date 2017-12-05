package org.unicode.unittest;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.tools.Segmenter;
import org.unicode.tools.Segmenter.Builder;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.UnicodeSet;

public class TestSegmenter extends TestFmwk{
    public static void main(String[] args) {
        IndexUnicodeProperties iup = IndexUnicodeProperties.make();
        UnicodeSet emoji = iup
                .loadEnum(UcdProperty.Emoji, Binary.class)
                .getSet(Binary.Yes);
        System.out.println(emoji.size() + "\t" + emoji.toPattern(false));

        UnicodeSet extended_pictographic = iup
                .loadEnum(UcdProperty.Extended_Pictographic, Binary.class)
                .getSet(Binary.Yes);
        System.out.println(extended_pictographic.size() + "\t" + extended_pictographic.toPattern(false));

        new TestSegmenter().run(args);
    }
    
    Segmenter gcb = Segmenter.make(ToolUnicodePropertySource.make(Default.ucdVersion()), "GraphemeClusterBreak").make();
    
    public void Test11() {
        String[][] tests = {
                {"🛑\u200d🛑", "|---"},
        };
        for (String[] test : tests) {
            String s = test[0];
            String expected = test[1];
            int expectedIndex = 0;
            for (int i = 0; i < s.length(); ++i) {
                boolean expectedBreak = i >= s.length() || Character.isLowSurrogate(s.charAt(i)) ? false : expected.charAt(expectedIndex++) == '|';
                boolean actualBreak = gcb.breaksAt(s, i);
                String title = s.substring(0, i) + "|" + s.substring(i);
                if (!assertEquals(title, expectedBreak, actualBreak)) {
                    gcb.breaksAt(s, i);
                }
            }
        }
    }
}
