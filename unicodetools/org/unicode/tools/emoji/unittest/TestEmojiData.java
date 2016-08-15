package org.unicode.tools.emoji.unittest;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiOrder;
import org.unicode.tools.emoji.GenerateEmojiData;
import org.unicode.tools.emoji.GenerateEmojiData.ZwjType;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;

public class TestEmojiData extends TestFmwkPlus {
    
    public static void main(String[] args) {
        new TestEmojiData().run(args);
    }
    
    public void TestFlags() {
        UnicodeSet shouldBeFlagEmoji = new UnicodeSet().add(Emoji.getHexFromFlagCode("EU")).add(Emoji.getHexFromFlagCode("UN"));
        Validity validity = Validity.getInstance();
        Map<Status, Set<String>> regionData = validity.getStatusToCodes(LstrType.region);
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
    
    public void TestOrderRules() throws Exception {
        int SKIPTO = 400;
        RuleBasedCollator ruleBasedCollator;
        ruleBasedCollator = new RuleBasedCollator("&a <*🍱🍘🍙🍚🍛🍜🍝🍠🍢🍣🍤🍥🍡");
        UnicodeSet ruleSet = new UnicodeSet();
        for (String s : EmojiData.EMOJI_DATA.getEmojiForSortRules()) {
            // skip modifiers not in zwj, as hack
            if (s.contains(Emoji.JOINER_STR) || EmojiData.MODIFIERS.containsNone(s)) {
                ruleSet.add(s);
            }
        }
        StringBuilder outText = new StringBuilder();
        EmojiOrder.STD_ORDER.appendCollationRules(outText, ruleSet);
        String rules = outText.toString();
        try {
            ruleBasedCollator = new RuleBasedCollator(rules);
            Set<String> testSet = new TreeSet<>(EmojiOrder.STD_ORDER.codepointCompare);
            EmojiData.EMOJI_DATA.getAllEmojiWithDefectives().addAllTo(testSet);
            String lastItem = "";
            for (String item : testSet) {
                if (ruleBasedCollator.compare(lastItem, item) > 0) {
                    errln("Out of order: " + lastItem + ">" + item);
                } else {
                    logln(lastItem + "≤" + item);
                }
                lastItem = item;
            }
        } catch (Exception e) {
            errln("Can't build rules: analysing problem…");
            // figure out where the problem is
            String[] list = rules.split("\n");
            rules = "";
            String oldRules = "";
            int i = 0;
            for (String line : list) {
                ++i;
                logln(i + "\t" + line);
                if (i > 1) {
                    rules += "\n";
                }
                rules += line;
                if (i <= SKIPTO) {
                    continue;
                }
                try {
                    ruleBasedCollator = new RuleBasedCollator(rules);
                } catch (Exception e2) {
                    errln("Fails when adding line " + line);
                    errln(showSorting(oldRules));
                    errln(oldRules);
                    throw (e2);
                }
                oldRules = rules;
            }
            throw (e);
        }
        logln(showSorting(rules));
        logln(rules);
    }

    private String showSorting(String oldRules) throws Exception {
        RuleBasedCollator ruleBasedCollator = new RuleBasedCollator(oldRules);
        UnicodeSet chars = ruleBasedCollator.getTailoredSet();
        StringBuilder buffer = new StringBuilder();
        StringBuilder pbuffer = new StringBuilder();
        StringBuilder sbuffer = new StringBuilder();
        StringBuilder tbuffer = new StringBuilder();
        for (String s : chars) {
            CollationElementIterator it = ruleBasedCollator.getCollationElementIterator(s);
            for (int element = it.next(); element != CollationElementIterator.NULLORDER; element = it.next()) {
                if (element == CollationElementIterator.IGNORABLE) {
                    continue;
                }
                int primary = CollationElementIterator.primaryOrder(element);
                pbuffer.append(Utility.hex(primary,4));
                int secondary = CollationElementIterator.secondaryOrder(element);
                sbuffer.append(Utility.hex(secondary,4));
                int tertiary = CollationElementIterator.tertiaryOrder(element);
                tbuffer.append(Utility.hex(tertiary,4));

            }
            buffer.append(s 
                    + "\t0x" + Utility.hex(s, " 0x")
                    + "\t0x" + pbuffer
                    + "\t0x" + sbuffer
                    + "\t0x" + tbuffer
                    + "\n");
            pbuffer.setLength(0);
            sbuffer.setLength(0);
            tbuffer.setLength(0);
        }
        return buffer.toString();
    }
}
