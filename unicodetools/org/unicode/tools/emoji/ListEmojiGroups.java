package org.unicode.tools.emoji;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CountEmoji.Category;

import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;

public class ListEmojiGroups {
    private static final boolean DEBUG = false;

    static final EmojiOrder order = EmojiOrder.of(Emoji.VERSION_LAST_RELEASED);

    private static final String OUTDIR = "/Users/markdavis/Google Drive/workspace/Generated/emoji/frequency";

    public static void main(String[] args) {
        System.out.println("\n\n***MAIN***\n");
        showCounts("gboardMain.tsv", GBoardCounts.counts);

        System.out.println("\n\n***W/O FE0F***\n");
        showCounts("gboardNoFE0F.tsv", GBoardCounts.countsNonPres);

        System.out.println("\n\n***EmojiTracker***\n");
        showCounts("emojiTracker.tsv", EmojiTracker.counts);

        System.out.println("\n\n***INFO***\n");
        showInfo("emojiInfo.txt");
        
        showTextEmoji("emojiText.txt");
    }

    private static void showTextEmoji(String filename) {
        try (PrintWriter out = FileUtilities.openUTF8Writer(OUTDIR, filename)) {
            UnicodeSet Android_Chrome_TP = new UnicodeSet("[☹ ☠  ❣ ⛑ ☘ ⛰ ⛩ ♨ ⛴ ✈ ⏱ ⏲ ⛈ ☂ ⛱ ☃ ☄ ⛸  ⌨  ✉ ✏ ⛏ ⚒ ⚔ ⚙ ⚗ ⚖ ⛓ ⚰ ⚱ ⚠ ☢ ☣ ⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖ ↕ ↔ ↩ ↪ ⤴ ⤵ ⚛ ✡ ☸ ☯ ✝ ☦ ☪ ☮ ▶ ⏭ ⏯ ◀ ⏮  ⏏ ♀ ♂ ⚕ ♻ ⚜ ☑ ✔ ✖ 〽 ✳ ✴ ❇ ‼ ⁉ 〰 © ® ™ 🅰 🅱 ℹ Ⓜ 🅾 🅿 🈂 🈷 ㊗ ㊙ ▪ ▫ ◻ ◼]");
            UnicodeSet Mac_Chrome_TP = new UnicodeSet("[☺ ❤ ❣ 🗨 ♨ ✈ ☀ ☁ ☂ ❄ ☃ ♠ ♥ ♦ ♣ ☎ ✉ ✏ ✒ ✂ ⚠ ⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖ ↕ ↔ ↩ ↪ ⤴ ⤵ ✡ ☯ ✝ ▶ ◀ ⏏ ♀ ♂ ⚕ ♻ ☑ ✔ ✖ 〽 ✳ ✴ ❇ ‼ ⁉ 〰 © ® ™ 🅰 🅱 ℹ Ⓜ 🅾 🅿 🈂 🈷 ㊗ ㊙ ▪ ▫ ◻ ◼]");
            UnicodeSet Mac_TextMate_TP = new UnicodeSet("[☺☝ ✌✍ ❤ ❣ ♨ ✈ ☀ ☁ ☂ ❄ ☃ ♠ ♥ ♦ ♣ ✉ ✏ ✒ ✂ ⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖ ↕ ↔ ↩ ↪ ⤴ ⤵ ✡ ☯ ✝ ▶ ◀ ⏏ ♀ ♂ ⚕ ♻ ☑ ✔ ✖ 〽 ✳ ✴ ❇ ‼ ⁉ 〰 © ® ™ #⃣ *⃣ 0⃣ 1⃣ 2⃣ 3⃣ 4⃣ 5⃣ 6⃣ 7⃣ 8⃣ 9⃣ 🅰 🅱 ℹ Ⓜ 🅾 🅿 🈂 🈷 ㊗ ㊙ ▪ ▫ ◻ ◼]");
            UnicodeSet Mac_Notes_TP = new UnicodeSet("[☝ ✌ ✍ ❤ ❣ ♨ ✈ ☀ ☁ ☂ ❄ ☃ ♠ ♥ ♦ ♣ ✉ ✏ ✒ ✂ ⚠ ⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖ ↕ ↔ ↩ ↪ ⤴ ⤵ ✡ ☯ ✝ ▶ ◀ ⏏ ♀ ♂ ⚕ ♻ ⚜ ☑ ✔ ✖ 〽 ✳ ✴ ❇ ‼ ⁉ 〰 © ® ™ #⃣ *⃣ 0⃣ 1⃣ 2⃣ 3⃣ 4⃣ 5⃣ 6⃣ 7⃣ 8⃣ 9⃣ 🅰 🅱 ℹ Ⓜ 🅾 🅿 🈂 🈷 ㊗ ㊙ ▪ ▫ ◻ ◼]");
            UnicodeSet Mac_Safari_TP = new UnicodeSet("[☺☝ ✌ ✍ ❤ ❣♨✈☀ ☁☂❄♠ ♥ ♦ ♣☎✉✏ ✒✂⬆ ↗ ➡ ↘ ⬇ ↙ ⬅ ↖↕ ↔ ↩ ↪ ⤴ ⤵✡☯ ✝▶◀⏏ ♀ ♂ ⚕ ♻☑ ✔ ✖ 〽 ✳ ✴ ❇ ‼ ⁉ 〰 © ® ™#⃣ *⃣ 0⃣ 1⃣ 2⃣ 3⃣ 4⃣ 5⃣ 6⃣ 7⃣ 8⃣ 9⃣ 🅰 🅱 ℹ Ⓜ 🅾 🅿 🈂 🈷 ㊗㊙ ▪ ▫ ◻ ◼﻿]");
            out.println("Hex\tEmoji\tAndroid Chrome\tMac Chrome\tMac Safari\tMac TextMate\tMac Notes");
            for (String s : EmojiMatcher.nopres) {
                out.println(
                        hex(s)
                        + "\t" + s
                        + "\t" + (Android_Chrome_TP.contains(s) ? "text" : "emoji")
                        + "\t" + (Mac_Chrome_TP.contains(s) ? "text" : "emoji")
                        + "\t" + (Mac_Safari_TP.contains(s) ? "text" : "emoji")
                        + "\t" + (Mac_TextMate_TP.contains(s) ? "text" : "emoji")
                        + "\t" + (Mac_Notes_TP.contains(s) ? "text" : "emoji")
                        );
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static void showInfo(String filename) {
        Set<String> sorted = EmojiData.EMOJI_DATA.getAllEmojiWithDefectives().addAllTo(new TreeSet<>(order.codepointCompare));
        for (String s : EmojiData.EMOJI_DATA.getAllEmojiWithDefectives()) {
            if (Emoji.isSingleCodePoint(s)) {
                String ex = EmojiData.EMOJI_DATA.addEmojiVariants(s);
                if (!ex.equals(s)) {
                    sorted.add(ex);
                }
            }
        }
        int sortOrder = 0;

        //try (PrintWriter out = FileUtilities.openUTF8Writer(OUTDIR, filename)) {

        try (PrintWriter out = FileUtilities.openUTF8Writer(OUTDIR, filename)) {
            out.println("Hex\tEmoji\tGroup\tSubgroup\tName (cldr)\tNorm?\tSort Order\tType\tYear");
            for (String s : sorted) {
                String subcategory = order.getCategory(s);
                if (subcategory == null) {
                    subcategory = order.getCategory(UTF16.valueOf(s.codePointAt(0)));
                    if (subcategory == null) {
                        continue;
                    }
                }
                String ep = EmojiData.EMOJI_DATA.addEmojiVariants(s).equals(s) ? "" : "Defect";
                out.println(
                        hex(s)
                        + "\t" + s 
                        + "\t" + order.getMajorGroupFromCategory(subcategory).toPlainString()
                        + "\t" + subcategory.toString()
                        + "\t" + EmojiData.EMOJI_DATA.getName(s)
                        + "\t" + ep
                        + "\t" + sortOrder++
                        + "\t" + Category.getBucket(s).toStringPlain()
                        + "\t" + EmojiData.getYear(s)
                        );
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static void showCounts(String filename, Counter<String> x) {
        try (PrintWriter out = FileUtilities.openUTF8Writer(OUTDIR, filename)) {
            out.println("Hex\tCount\tRank\tEmoji");
            int rank = 0;
            for (R2<Long, String> entry : x.getEntrySetSortedByCount(false, null)) {
                out.println(
                        hex(entry.get1())
                        + "\t" + entry.get0()
                        + "\t" + (++rank)
                        + "\t" + entry.get1()
                        );
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    static int matches(UnicodeSet unicodeSet, String input, int offset) {
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
        private static final String FREQ_SOURCE = "/Users/markdavis/Google Drive/workspace/DATA/frequency/emoji/";
        static Counter<String> counts = new Counter<>();
        static Counter<String> countsNonPres = new Counter<>();
        static {
            List<String> emojiSet = new ArrayList<>();
            List<String> nonPresSet = new ArrayList<>();
            List<String> nonEmojiSet = new ArrayList<>();
            //,text,decimal_code_points,count,hex_code_points
            // 8,❤️,"[10084, 65039]",705086,"['0x2764', '0xFE0F']"
            int rankIndex = 0, emojiIndex=1, decIndex=2, countIndex=3, hexIndex=4, limitIndex=5;
            CSVParser csvParser = new CSVParser();
            for (String id : Arrays.asList("20171031_20171113", "20171115_20171128")) {
                for (String line : FileUtilities.in(FREQ_SOURCE + "/emoji_freqs_" + id,"emoji_frequency_" + id + ".csv")) {
                    if (line.isEmpty() || line.startsWith(",text")) {
                        continue;
                    }
                    String emojiString = csvParser.set(line).get(emojiIndex);

                    long rank = Long.parseLong(csvParser.get(rankIndex));
                    long count = Long.parseLong(csvParser.get(countIndex));
                    emojiSet.clear();
                    nonEmojiSet.clear();
                    nonPresSet.clear();
                    EmojiMatcher.parse(emojiString, emojiSet, nonPresSet, nonEmojiSet);
                    if (DEBUG) System.out.println(rank
                            + "\t" + count
                            + "\t" + emojiString 
                            + "\t" + hex(emojiString)
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
    }

    public static String hex(String string) {
        return "\\x{" + Utility.hex(string, 1, " ") + "}";
    }

    public static class CSVParser {
        enum State {start, quote}
        // ab,cd => -1,2,5 that is, point before each comma
        private String line;
        private List<Integer> commaPoints = new ArrayList<>();

        public String get(int item) {
            return line.substring(commaPoints.get(item)+1, commaPoints.get(item+1));
        }

        public CSVParser set(String line) {
            this.line = line;
            commaPoints.clear();
            commaPoints.add(-1);
            State state = State.start;
            int i = 0;
            for (; i < line.length(); ++i) {
                int ch = line.charAt(i);
                switch(state) {
                case start: {
                    switch(ch) {
                    case ',': commaPoints.add(i); break;
                    case '"': state = State.quote; break;
                    }
                    break;
                }
                case quote: {
                    switch(ch) {
                    case '"': state = State.start; break;
                    }
                    break;
                }
                }
            }
            commaPoints.add(i);
            return this;
        }
    }

    static class EmojiTracker {
        static Counter<String> counts = new Counter<>();
        static {
            Matcher m = Pattern.compile("id=\"score-([A-F0-9]+)\">\\s*(\\d+)\\s*</span>").matcher("");
            // <span class="score" id="score-1F602">1872748264</span>
            try (BufferedReader in = FileUtilities.openFile(GenerateEmojiFrequency.class, "emojitracker.txt")) {
                String lastBuffer = "";
                double factor = 0;

                while (true) {
                    String line = in.readLine();
                    if (line == null) break;
                    line = lastBuffer+line;
                    m.reset(line);
                    int pos = 0;

                    while (true) {
                        boolean found = m.find(pos);
                        if (!found) break;
                        int cp = Integer.parseInt(m.group(1),16);
                        String str = UTF16.valueOf(cp);
                        long count = Long.parseLong(m.group(2));
                        if (factor == 0) {
                            factor = 1_000_000_000.0/count;
                        }
                        counts.add(str, count);
                        pos = m.end();
                    }
                    lastBuffer = line.substring(pos);
                }
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
    }
}
