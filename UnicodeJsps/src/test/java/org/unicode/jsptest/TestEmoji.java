package org.unicode.jsptest;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.unicode.jsp.UnicodeJsp;
import org.unicode.jsp.UnicodeUtilities;
import org.unicode.jsp.XPropertyFactory;

import com.ibm.icu.text.UnicodeSet;

public class TestEmoji extends TestFmwk2 {
    static XPropertyFactory factory = XPropertyFactory.make();

    @Test
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

        checkContained("[:Emoji_Zwj_Sequenceβ:]", "[{👨‍❤️‍👨}]");
        checkContained("[:Emoji_Zwj_Sequenceβ:]", "[☝]", false);

        checkContained("[:Emoji_Flag_Sequenceβ:]", "[{🇦🇨}{🇦🇩}]");
        checkContained("[:Emoji_Flag_Sequenceβ:]", "[☝]", false);

        checkContained("[:Emoji_Keycap_Sequenceβ:]", "[{#️⃣}{9️⃣}]");
        checkContained("[:Emoji_Keycap_Sequenceβ:]", "[☝]", false);

        checkContained("[:Emoji_Modifier_Sequenceβ:]", "[{☝🏻}{☝🏼}]");
        checkContained("[:Emoji_Modifier_Sequenceβ:]", "[☝]", false);

        checkContained("[:Emoji_Componentβ:]", "[#*0]");
        checkContained("[:Emoji_Componentβ:]", "[☝]", false);

        checkContained("[:Emoji_Tag_Sequenceβ:]", "[{🏴󠁧󠁢󠁳󠁣󠁴󠁿}]");
        checkContained("[:Emoji_Tag_Sequenceβ:]", "[☝]", false);
    }


}
