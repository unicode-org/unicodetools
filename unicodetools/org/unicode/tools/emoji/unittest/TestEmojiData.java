package org.unicode.tools.emoji.unittest;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiOrder;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;
import org.unicode.tools.emoji.GenerateEmojiData;
import org.unicode.tools.emoji.GenerateEmojiData.ZwjType;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class TestEmojiData extends TestFmwkPlus {
    
    public static void main(String[] args) {
        new TestEmojiData().run(args);
    }
    
    public void TestFlags() {
        UnicodeSet shouldBeFlagEmoji = new UnicodeSet().add(Emoji.getHexFromFlagCode("EU")).add(Emoji.getHexFromFlagCode("UN"));
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
                if (!shouldBeFlagEmoji.contains(regionalPair)) {
                    shouldNOTBeFlagEmoji.add(regionalPair);
                }
            }
        }
        logln("Should be flags: " + shouldBeFlagEmoji.toPattern(false));
        assertEquals("Contains all good regions", UnicodeSet.EMPTY, new UnicodeSet(shouldBeFlagEmoji).removeAll(EmojiData.EMOJI_DATA.getChars()));
        logln("Should not be flags: " + shouldNOTBeFlagEmoji.toPattern(false));
        assertEquals("Contains no bad regions", UnicodeSet.EMPTY, new UnicodeSet(shouldNOTBeFlagEmoji).retainAll(EmojiData.EMOJI_DATA.getChars()));
    }
    
    public void TestZwjCategories () {
        UnicodeMap<String> chars = new UnicodeMap<>();
        for (String s : EmojiData.EMOJI_DATA.getZwjSequencesNormal()) {
            GenerateEmojiData.ZwjType zwjType = ZwjType.getType(s);
            String grouping = EmojiOrder.STD_ORDER.charactersToOrdering.get(s);
            chars.put(s, zwjType + "\t" + grouping);
        }
        for (String value: chars.values()) {
            final UnicodeSet set = chars.getSet(value);
            System.out.println(value + "\t" + set.size() + "\t" + set.toPattern(false));
        }
    }
}
