package org.unicode.tools.emoji.unittest;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;

import com.ibm.icu.text.UnicodeSet;

public class TestEmojiData extends TestFmwkPlus {
    public static void main(String[] args) {
        new TestEmojiData().run(args);
    }
    public void TestFlags() {
        UnicodeSet shouldBeFlagEmoji = new UnicodeSet();
        Validity validity = Validity.getInstance();
        Map<Status, Set<String>> regionData = validity.getData().get(LstrType.region);
        for (Entry<Status, Set<String>> e : regionData.entrySet()) {
            switch(e.getKey()) {
            case regular: 
                for (String region : e.getValue()) {
                    String flagEmoji = Emoji.getHexFromFlagCode(region);
                    shouldBeFlagEmoji.add(flagEmoji);
                }
            }
        }
        UnicodeSet shouldNOTBeFlagEmoji = new UnicodeSet();
        for (String s : Emoji.REGIONAL_INDICATORS) {
            for (String t : Emoji.REGIONAL_INDICATORS) {
                final String regionalPair = s+t;
                if (shouldNOTBeFlagEmoji.contains(regionalPair)) {
                    shouldNOTBeFlagEmoji.add(regionalPair);
                }
            }
        }
        assertRelation("Contains all regions", true, EmojiData.EMOJI_DATA.getChars(), TestFmwkPlus.CONTAINS_US, shouldBeFlagEmoji);
        assertRelation("Contains all regions", false, EmojiData.EMOJI_DATA.getChars(), TestFmwkPlus.CONTAINS_US, shouldNOTBeFlagEmoji);
    }
}
