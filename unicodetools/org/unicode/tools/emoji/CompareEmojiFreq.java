package org.unicode.tools.emoji;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map.Entry;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.tools.emoji.ListEmojiGroups.CountInfo;
import org.unicode.tools.emoji.ListEmojiGroups.GBoardCounts;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class CompareEmojiFreq {
    private static final int MINIMUM_RAW_COUNT_FOR_LOCALE = 0;
    private static final int MINIMUM_RAW_COUNT_FOR_EMOJI = 5000;
    private static final int MAXIMUM_RANK = 30;
    private static final int MINIMUM_RANK_BETTER = 3;
    private static final double MINIMUM_PERCENT_BETTER = 1;
    
    static CountInfo worldCounts = GBoardCounts.localeToCountInfo.get("001");

    public static void main(String[] args) throws IOException {
        Multimap<Long,String> ascending = TreeMultimap.create();
        

        for (String locale : GBoardCounts.localeToCountInfo.keySet()) {
            CountInfo counts = GBoardCounts.localeToCountInfo.get(locale);
            if (counts.rawTotal < MINIMUM_RAW_COUNT_FOR_LOCALE) {
                continue;
            }
            ascending.put(counts.rawTotal, locale);
        }
        try (PrintWriter out = FileUtilities.openUTF8Writer(
                "/Users/markdavis/Google Drive/workspace/Generated/emoji/frequency", 
                "emoji-by-locale.txt");
                PrintWriter outWorse = FileUtilities.openUTF8Writer(
                        "/Users/markdavis/Google Drive/workspace/Generated/emoji/frequency", 
                        "emoji-by-locale-worse.txt")) {
        for (Entry<Long, String> entry : ascending.entries()) {
            Long totalCount = entry.getKey();
            String locale = entry.getValue();
            CountInfo counts = GBoardCounts.localeToCountInfo.get(locale);
            show(out, locale, counts, false);
            show(outWorse, locale, counts, true);
//
//            System.out.println(totalCount 
//                    + "\t" + locale 
//                    + "\t" + ULocale.getDisplayName(locale, "en"));
        }
        }
    }

    static final NumberFormat pf = NumberFormat.getPercentInstance(Locale.ENGLISH);
    static final NumberFormat nf = NumberFormat.getIntegerInstance(Locale.ENGLISH);
    static {
        pf.setMaximumFractionDigits(2);
        pf.setMinimumFractionDigits(2);
    }
    
    private static void show(PrintWriter out, String locale, CountInfo counts, boolean worse) {
        Counter<String> other_ = new Counter<>();
        for (String emoji : worldCounts.keyToCount.keySet()) {
            other_.add(emoji, worldCounts.getRaw(emoji) - counts.getRaw(emoji));
        }
        CountInfo other = new CountInfo(other_);
        String localeName = name(locale);
        boolean haveCounts = false;
        boolean isWorld = locale.equals("001");

        for (Entry<String, Long> entry : counts.keyToCount.entrySet()) {
            Long localeCount = entry.getValue();
            String emoji = entry.getKey();
            long rawCount = counts.getRaw(emoji);
            if (rawCount < MINIMUM_RAW_COUNT_FOR_EMOJI) {
                break;
            }
            int rank = counts.keyToRank.get(emoji);
            long otherCount = other.keyToCount.get(emoji);
            int otherRank = other.keyToRank.get(emoji);

            long amountBetter = worse ? otherCount-localeCount : localeCount-otherCount;
            int rankBetter = worse ? rank - otherRank: otherRank - rank;
            
            if (amountBetter >= MINIMUM_PERCENT_BETTER*CountInfo.SCALE/100 && rankBetter >= 1 
                    || rankBetter >= MINIMUM_RANK_BETTER
                    || isWorld) {
//                if (!haveCounts) {
//                    out.println(localeName + "\t" + nf.format(counts.rawTotal));
//                    haveCounts = true;
//                }
                haveCounts = true;
                out.println(
                        localeName 
                        + "\t" + fixLocale(locale)
                        + "\t" + fix(emoji)
                        + "\t" + rank 
                        + "\t" + pf.format(localeCount/(double)CountInfo.SCALE)
                        + "\t" + "+" + pf.format(amountBetter/(double)CountInfo.SCALE)
                        + "\t" + "+" + rankBetter
                        + "\t" + nf.format(counts.rawTotal)
                        );
           }
           if (rank >= MAXIMUM_RANK) break;
        }
        if (haveCounts) {
            out.println();
        }
    }

    private static String fixLocale(String locale) {
        switch (locale) {
        case "001": return "mul";
        case "zz": return "und";
        default:
            return locale;
        }

    }

    private static String fix(String emoji) {
        return EmojiData.EMOJI_DATA.addEmojiVariants(emoji);
    }

    private static String name(String locale) {
        switch (locale) {
        case "001": return "World";
        case "zz": return "Unknown";
        default:
            return ULocale.getDisplayName(locale, "en");
        }
    }
}
