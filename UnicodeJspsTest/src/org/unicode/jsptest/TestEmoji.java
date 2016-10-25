package org.unicode.jsptest;

import java.io.IOException;

import org.unicode.jsp.UnicodeJsp;
import org.unicode.jsp.UnicodeUtilities;
import org.unicode.jsp.XPropertyFactory;

import com.ibm.icu.text.UnicodeSet;

public class TestEmoji extends TestFmwk2 {
    public static void main(String[] args) {
        new TestEmoji().run(args);
    }
    static XPropertyFactory factory = XPropertyFactory.make();

    public void TestBasic() throws IOException {
        String[] message = {""};
        UnicodeSet primary = UnicodeUtilities.parseSimpleSet("[:emoji:]", message);
        StringBuilder out = new StringBuilder();
        UnicodeJsp.showSet("gc", "sc", primary, false, false, true, out);
        assertTrue("", out.toString().contains("ASCII"));
        logln(out.toString());

        checkContained("[:emoji:]", "[😀]");
        checkContained("[:emoji:]", "[‼]");
        checkContained("[:emoji:]", "a", false);

        checkContained("[:emoji_presentation:]", "[😀]");
        checkContained("[:emoji_presentation:]", "[‼]", false);

        checkContained("[:emoji_modifier_base:]", "[☝]");
        checkContained("[:emoji_modifier_base:]", "[😀]", false);

        checkContained("[:emoji_modifier:]", "[🏻]");
        checkContained("[:emoji_modifier:]", "[☝]", false);

        checkContained("[:EMOJI_ZWJ_SEQUENCES:]", "[{👁‍🗨}{👨‍❤️‍👨}]");
        checkContained("[:EMOJI_ZWJ_SEQUENCES:]", "[☝]", false);

        checkContained("[:EMOJI_FLAG_SEQUENCES:]", "[{🇦🇨}{🇦🇩}]");
        checkContained("[:EMOJI_FLAG_SEQUENCES:]", "[☝]", false);

        checkContained("[:EMOJI_KEYCAP_SEQUENCES:]", "[{#⃣}{*⃣}]");
        checkContained("[:EMOJI_KEYCAP_SEQUENCES:]", "[☝]", false);

        checkContained("[:EMOJI_MODIFIER_SEQUENCES:]", "[{☝🏻}{☝🏼}]");
        checkContained("[:EMOJI_MODIFIER_SEQUENCES:]", "[☝]", false);

        checkContained("[:EMOJI_DEFECTIVES:]", "[#*0]");
        checkContained("[:EMOJI_DEFECTIVES:]", "[☝]", false);

        checkContained("[:emoji_all:]", "[1]", false);
        checkContained("[:emoji_all:]", "[\\x{1F1E6}]", false);
        checkContained("[:emoji_all:]", "[{#️⃣}]");
        checkContained("[:emoji_all:]", "[{#⃣}]");
        checkContained("[:emoji_all:]", "[{🇦🇨}]");
        checkContained("[:emoji_all:]", "[{☝🏻}]");
        checkContained("[:emoji_all:]", "[{👁‍🗨}]");
    }


}
