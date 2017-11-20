package org.unicode.tools.emoji;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CountEmoji.Category;
import org.unicode.tools.emoji.GenerateEmojiData.ZwjType;

import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.text.UnicodeSetSpanner;

public class ListEmojiGroups {
    static final EmojiOrder order = EmojiOrder.of(Emoji.VERSION_LAST_RELEASED);

    public static void main(String[] args) {
        Counter<String> x = GBoardCounts.counts;
        
        System.out.println("\n\nGB Rank\tEmoji\tCount\tCode");
        int rank = 0;
        for (R2<Long, String> entry : x.getEntrySetSortedByCount(false, null)) {
            System.out.println(entry.get1()
                    + "\t" + (++rank)
                    + "\t" + entry.get0()
                    + "\t" + hex(entry.get1()));
        }
        
        Set<String> sorted = EmojiData.EMOJI_DATA.getAllEmojiWithDefectives().addAllTo(new TreeSet<>(order.codepointCompare));
        int sortOrder = 0;

        System.out.println("\n\nEmoji\tGroup\tSubgroup\tName (cldr)\tText?\tSort Order\tType\tHex");
        for (String s : sorted) {
            String subcategory = order.getCategory(s);
            if (subcategory == null) {
                subcategory = order.getCategory(UTF16.valueOf(s.codePointAt(0)));
                if (subcategory == null) {
                    continue;
                }
            }
            String ep = s.codePointCount(0, s.length()) != 1 ? "" 
                    : EmojiData.EMOJI_DATA.getEmojiPresentationSet().contains(s) ? "" 
                            : "Text";
            System.out.println(
                    s 
                    + "\t" + order.getMajorGroupFromCategory(subcategory).toPlainString()
                    + "\t" + subcategory.toString()
                    + "\t" + EmojiData.EMOJI_DATA.getName(s)
                    + "\t" + ep
                    + "\t" + sortOrder++
                    + "\t" + Category.getBucket(s).toStringPlain()
                    + "\t" + hex(s)
                    );
        }
        UnicodeSet Android_Chrome_TP = new UnicodeSet("[☹ ☠  ❣ ⛑ ☘ ⛰ ⛩ ♨ ⛴ ✈ ⏱ ⏲ ⛈ ☂ ⛱ ☃ ☄ ⛸  ⌨  ✉ ✏ ⛏ ⚒ ⚔ ⚙ ⚗ ⚖ ⛓ ⚰ ⚱ ⚠ ☢ ☣ ⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖ ↕ ↔ ↩ ↪ ⤴ ⤵ ⚛ ✡ ☸ ☯ ✝ ☦ ☪ ☮ ▶ ⏭ ⏯ ◀ ⏮  ⏏ ♀ ♂ ⚕ ♻ ⚜ ☑ ✔ ✖ 〽 ✳ ✴ ❇ ‼ ⁉ 〰 © ® ™ 🅰 🅱 ℹ Ⓜ 🅾 🅿 🈂 🈷 ㊗ ㊙ ▪ ▫ ◻ ◼]");
        UnicodeSet Mac_Chrome_TP = new UnicodeSet("[☺ ❤ ❣ 🗨 ♨ ✈ ☀ ☁ ☂ ❄ ☃ ♠ ♥ ♦ ♣ ☎ ✉ ✏ ✒ ✂ ⚠ ⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖ ↕ ↔ ↩ ↪ ⤴ ⤵ ✡ ☯ ✝ ▶ ◀ ⏏ ♀ ♂ ⚕ ♻ ☑ ✔ ✖ 〽 ✳ ✴ ❇ ‼ ⁉ 〰 © ® ™ 🅰 🅱 ℹ Ⓜ 🅾 🅿 🈂 🈷 ㊗ ㊙ ▪ ▫ ◻ ◼]");
        UnicodeSet Mac_TextMate_TP = new UnicodeSet("[☺☝ ✌✍ ❤ ❣ ♨ ✈ ☀ ☁ ☂ ❄ ☃ ♠ ♥ ♦ ♣ ✉ ✏ ✒ ✂ ⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖ ↕ ↔ ↩ ↪ ⤴ ⤵ ✡ ☯ ✝ ▶ ◀ ⏏ ♀ ♂ ⚕ ♻ ☑ ✔ ✖ 〽 ✳ ✴ ❇ ‼ ⁉ 〰 © ® ™ #⃣ *⃣ 0⃣ 1⃣ 2⃣ 3⃣ 4⃣ 5⃣ 6⃣ 7⃣ 8⃣ 9⃣ 🅰 🅱 ℹ Ⓜ 🅾 🅿 🈂 🈷 ㊗ ㊙ ▪ ▫ ◻ ◼]");
        UnicodeSet Mac_Notes_TP = new UnicodeSet("[☝ ✌ ✍ ❤ ❣ ♨ ✈ ☀ ☁ ☂ ❄ ☃ ♠ ♥ ♦ ♣ ✉ ✏ ✒ ✂ ⚠ ⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖ ↕ ↔ ↩ ↪ ⤴ ⤵ ✡ ☯ ✝ ▶ ◀ ⏏ ♀ ♂ ⚕ ♻ ⚜ ☑ ✔ ✖ 〽 ✳ ✴ ❇ ‼ ⁉ 〰 © ® ™ #⃣ *⃣ 0⃣ 1⃣ 2⃣ 3⃣ 4⃣ 5⃣ 6⃣ 7⃣ 8⃣ 9⃣ 🅰 🅱 ℹ Ⓜ 🅾 🅿 🈂 🈷 ㊗ ㊙ ▪ ▫ ◻ ◼]");
        UnicodeSet Mac_Safari_TP = new UnicodeSet("[☺☝ ✌ ✍ ❤ ❣♨✈☀ ☁☂❄♠ ♥ ♦ ♣☎✉✏ ✒✂⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖↕ ↔ ↩ ↪ ⤴ ⤵✡☯ ✝▶◀⏏ ♀ ♂ ⚕ ♻☑ ✔ ✖ 〽 ✳ ✴ ❇ ‼ ⁉ 〰 © ® ™#⃣ *⃣ 0⃣ 1⃣ 2⃣ 3⃣ 4⃣ 5⃣ 6⃣ 7⃣ 8⃣ 9⃣ 🅰 🅱 ℹ Ⓜ 🅾 🅿 🈂 🈷 ㊗㊙ ▪ ▫ ◻ ◼﻿]");
        System.out.println("\n\nEmoji\tCount\tAndroid Chrome\tMac Chrome\t\tMac Safari\tMac TextMate\tMac Notes\tHex");
        for (String s : EmojiMatcher.nopres) {
            System.out.println(
                    s
                    + "\t" + (Android_Chrome_TP.contains(s) ? "text" : "emoji")
                    + "\t" + (Mac_Chrome_TP.contains(s) ? "text" : "emoji")
                    + "\t" + (Mac_Safari_TP.contains(s) ? "text" : "emoji")
                    + "\t" + (Mac_TextMate_TP.contains(s) ? "text" : "emoji")
                    + "\t" + (Mac_Notes_TP.contains(s) ? "text" : "emoji")
                    + "\t" + hex(s)
                    );
        }
    }

    static class EmojiMatcher {
        static final UnicodeSet fixed;
        static final UnicodeSet nopres = new UnicodeSet(EmojiData.EMOJI_DATA.getSingletonsWithDefectives())
                .removeAll(EmojiData.EMOJI_DATA.getEmojiPresentationSet());
        static {
            final UnicodeSet components = EmojiData.of(Emoji.VERSION_BETA).getEmojiComponents();
            fixed = new UnicodeSet(EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives())
                    .removeAll(components)
                    .removeAll(nopres);
            for (String s : nopres) {
                fixed.add(s + Emoji.EMOJI_VARIANT);
            }
            String heart = "\u2764";
            boolean m = EmojiMatcher.fixed.contains(heart);

            String heartVs = "❤️";
            boolean m2 = EmojiMatcher.fixed.contains(heartVs);

            fixed.freeze();
        }
        static UnicodeSet singletonFailures = new UnicodeSet();
        
        static void parse(String input, List<String> emoji, List<String> noPres, List<String> nonEmoji) {
            int emojiEnd = 0;
            for (int offset = 0; offset < input.length();) {
                int match = matches(fixed, input, offset);
                if (match > offset) {
                    if (emojiEnd < offset) {
                        String str = input.substring(emojiEnd, offset);
                        addNonEmoji(str, nonEmoji, noPres);
                    }
                    emoji.add(input.substring(offset, match));
                    offset = emojiEnd = match;
                } else {
                    ++offset;
                }
            }
            if (emojiEnd < input.length()) {
                String str = input.substring(emojiEnd);
                addNonEmoji(str, nonEmoji, noPres);
            }
        }

        private static boolean addNonEmoji(String str, List<String> nonEmoji, List<String> noPres2) {
            StringBuilder nonEmojiBuffer = new StringBuilder();
            for (int cp : CharSequences.codePoints(str)) {
                if (nopres.contains(cp)) {
                    noPres2.add(UTF16.valueOf(cp));
                } else {
                    nonEmojiBuffer.appendCodePoint(cp);
                }
            }
            return nonEmoji.add(nonEmojiBuffer.toString());
        }
    }

    private static int matches(UnicodeSet unicodeSet, String input, int offset) {
        SortedSet<String> items = (SortedSet<String>) unicodeSet.strings();
        int cp = input.codePointAt(offset);
        SortedSet<String> subset = items.subSet(UTF16.valueOf(cp), UTF16.valueOf(cp+1));
        int bestLength = -1;
        int inputLength = input.length();
        int allowedLength = inputLength - offset;
        if (!subset.isEmpty()) {
            for (String trial : subset) {
                // see if the trial matches the characters in input starting at offset
                int trialLength = trial.length();
                if (bestLength >= trialLength) { // when we start to contract, stop
                    break;
                }
                //            if (trialLength > allowedLength) { // trial is too big, stop
                //                break; // can't match and nothing else will
                //            }
                if (input.regionMatches(offset, trial, 0, trialLength)) {
                    bestLength = trialLength;
                }
            }
        }
        if (bestLength >= 0) {
            return offset + bestLength;
        }
        if (unicodeSet.contains(cp)) {
            return offset + Character.charCount(cp);
        }
        return -1;
    }

    static class GBoardCounts {
        static Counter<String> counts = new Counter<>();
        static Counter<String> countsNonPres = new Counter<>();
        static {
            List<String> emojiSet = new ArrayList<>();
            List<String> nonPresSet = new ArrayList<>();
            List<String> nonEmojiSet = new ArrayList<>();
            // 1    😂  3354042 0x1F602
            for (String line : FileUtilities.in("/Users/markdavis/Google Drive/workspace/DATA/frequency/emoji/","gboard.txt")) {
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\t");
                String string = parts[1];

                long count = Long.parseLong(parts[2]);
                emojiSet.clear();
                nonEmojiSet.clear();
                nonPresSet.clear();
                EmojiMatcher.parse(string, emojiSet, nonPresSet, nonEmojiSet);
                System.out.println(count
                        + "\t" + string 
                        + "\t" + hex(string)
                        + "\t" + emojiSet
                        + "\t" + nonPresSet
                        + "\t" + nonEmojiSet
                        );
                for (String s : emojiSet) {
                    counts.add(s, count);
                }
                for (String s : nonPresSet) {
                    countsNonPres.add(s, count);
                }
            }
        }
        
    }
    public static String hex(String string) {
        return "\\x{" + Utility.hex(string, 1, " ") + "}";
    }
}
