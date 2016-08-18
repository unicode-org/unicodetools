package org.unicode.tools.emoji;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.Emoji.Source;
import org.unicode.tools.emoji.EmojiAnnotations.Status;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UnicodeSet;

public class GenerateMissingAnnotations {
    public static void main(String[] args) throws IOException {
        countWords();
        //generateMissing();
    }

    private static void countWords() {
        Multimap<String,String> orderWordToCps = HashMultimap.create();
        Counter<String> orderCounter = new Counter<>();

        for (Entry<String, Set<String>> entry : EmojiOrder.STD_ORDER.orderingToCharacters.keyValuesSet()) {
            final String orderWords = entry.getKey();
            final Set<String> cps = entry.getValue();
            System.out.println(orderWords + "\t" + cps);
            for (String orderWord : orderWords.split("[-\\s]+")) {
                for (String cp : cps) {
                    if (EmojiData.MODIFIERS.containsSome(cp)) {
                        continue;
                    }
                    if (!cp.equals(EmojiData.EMOJI_DATA.addEmojiVariants(cp))) {
                        continue; // defective
                    }
                    orderCounter.add(orderWord, 1);
                    orderWordToCps.put(orderWord, cp);
                }
            }
        }

        Counter<String> keywordCounter = new Counter<>();
        Multimap<String,String> keywordToCps = HashMultimap.create();
        EmojiAnnotations em = new EmojiAnnotations("en", EmojiOrder.STD_ORDER.codepointCompare);
        for (String cp: em.getStatusKeys()) {
            final Status status = em.getStatus(cp);
            if (status == Status.missing || status == Status.constructed) {
                continue;
            }
            if (EmojiData.MODIFIERS.containsSome(cp)) {
                continue;
            }
            for (String keyword : em.getKeys(cp)) {
                keywordCounter.add(keyword, 1);
                keywordToCps.put(keyword, cp);
            }
        }
        Set<String> sortedKeyCps = new TreeSet<>(EmojiOrder.STD_ORDER.codepointCompare);
        Set<String> sortedOrderCps = new TreeSet<>(EmojiOrder.STD_ORDER.codepointCompare);
        Set<String> same = new TreeSet<>(EmojiOrder.STD_ORDER.codepointCompare);
        System.out.println("\nKeywords\n");
        for (R2<Long, String> s : keywordCounter.getEntrySetSortedByCount(false, null)) {
            final Long count = s.get0();
            if (count < 2) continue;
            final String keyword = s.get1();
            if (keyword.equals("flag")) {
                int debug = 0;
            }
            final Collection<String> keywordCps = keywordToCps.get(keyword);
            sortedKeyCps.clear();
            sortedKeyCps.addAll(keywordCps);
            System.out.print(count + "\t" + keyword + "\t" + sortedKeyCps);
            Collection<String> orderCps = orderWordToCps.get(keyword);
            if (orderCps != null) {
                sortedOrderCps.clear();
                sortedOrderCps.addAll(orderCps);
                if (sortedKeyCps.equals(sortedOrderCps)) {
                    System.out.print("\n\tEQUAL");
                } else {
                    same.clear();
                    same.addAll(sortedKeyCps);
                    same.retainAll(sortedOrderCps);
                    sortedKeyCps.removeAll(same);
                    sortedOrderCps.removeAll(same);
                    System.out.print("\n\tK-O:\t" + sortedKeyCps.size() + "\t" + sortedKeyCps);
                    System.out.print("\n\tO-K:\t" + sortedOrderCps.size() + "\t" + sortedOrderCps);
                }
            }
            System.out.println();
        }
        for (R2<Long, String> s : keywordCounter.getEntrySetSortedByCount(false, null)) {
            final Long count = s.get0();
            if (count < 5) continue;
            final String keyword = s.get1();
            if (!orderWordToCps.containsKey(keyword)) {
                System.out.println("Not in Order:\t" + count + "\t" + keyword);
            }
        }
        for (R2<Long, String> s : orderCounter.getEntrySetSortedByCount(false, null)) {
            final Long count = s.get0();
            final String keyword = s.get1();
            if (!keywordToCps.containsKey(keyword)) {
                System.out.println("Not in Keywords:\t" + count + "\t" + keyword);
            }
        }
    }

    private static void generateMissing() throws IOException {
        final CLDRConfig config = CLDRConfig.getInstance();
        Set<String> locales = config.getStandardCodes().getLocaleCoverageLocales(Organization.google, EnumSet.of(Level.MODERN));

        UnicodeSet items = new UnicodeSet();
        EmojiAnnotations em = new EmojiAnnotations("en", EmojiOrder.STD_ORDER.codepointCompare);

        for (String s : EmojiImageData.getSupported(Source.google)) {
            if ((em.getStatus(s) != Status.found)
                    || Emoji.REGIONAL_INDICATORS.containsSome(s)
                    || EmojiData.MODIFIERS.containsSome(s)
                    || s.contains(Emoji.KEYCAP_MARK_STRING)
                    //|| s.contains(Emoji.JOINER_STRING)
                    ) {
                continue;
            }
            items.add(s);
        }
        items.add("🏳️‍🌈").add("🇺🇳").add("🔟").freeze();

        TreeSet<String> sorted = items.addAllTo(new TreeSet<>(EmojiOrder.STD_ORDER.codepointCompare));
        final String emojiDir = Settings.GEN_DIR + "emoji/";
        final String annotationDir = emojiDir + "annotations-v4.0/";
        try (PrintWriter out = FileUtilities.openUTF8Writer(emojiDir, "images.txt")) {
            for (String s : sorted) {
                out.println(getKey(s) 
                        + "\t" + s
                        + "\t" + em.getShortName(s));
            }
        }

        Set<String> skipped = new LinkedHashSet<>();
        Map<String, Counts> countMap = new TreeMap<>();
        for (String s : locales) {
            if (!Annotations.getAvailableLocales().contains(s) 
                    || s.equals("en") || s.equals("ga")
                    || (s.contains("_") && !s.equals("zh_Hant")) // hack for now to reduce data size. Redo once base locales are populated
                    ) {
                skipped.add(s);
                continue;
            }
            Counts counts = new Counts();
            try (PrintWriter out = FileUtilities.openUTF8Writer(annotationDir, s + ".tsv")) {
                System.out.println(s + "\t" + config.getEnglish().getName(s));
                doAnnotations(s, out, em, sorted, counts);
                countMap.put(s, counts);
            }
        }
        for (String skip : skipped) {
            System.out.println("Skipping " + skip + " ("+  config.getEnglish().getName(skip) + "), no current CLDR annotation data");
        }
        for (Entry<String, Counts> entry : countMap.entrySet()) {
            String locale = entry.getKey();
            System.out.println(locale + "\t"+  config.getEnglish().getName(locale) + "\t" + entry.getValue());
        }
    }

    static final class Counts {
        int emojiCount;
        int charCount;

        void add(String tts, Set<String> keywords) {
            ++emojiCount;
            // rough char count
            Set<String> foo = new LinkedHashSet<>();
            foo.add(tts);
            foo.addAll(keywords);
            String combined = CollectionUtilities.join(foo, " ");
            charCount += Math.min(combined.length() / 6, 6);
        }
        @Override
        public String toString() {
            return emojiCount + "\t" + charCount;
        }
    }

    private static String getKey(String s) {
        return "_" + Utility.hex(s,"_").toLowerCase(Locale.ROOT).replace("_fe0f", "");
    }

    private static EmojiAnnotations doAnnotations(final String localeStr, PrintWriter out, EmojiAnnotations em2, TreeSet<String> sorted, Counts counts) {
        EmojiAnnotations em = new EmojiAnnotations(localeStr, EmojiOrder.STD_ORDER.codepointCompare);
        Set<String> missing = new LinkedHashSet<>();
        int maxLen = 32;

        int count = 1;
        for (String s : sorted) {
            if (s.equals("🕵‍♂️")) {
                int debug = 0;
            }
            Set<String> keywords = em.getKeys(s);
            String tts = em.getShortName(s);
            EmojiAnnotations.Status status = em.getStatus(s);
            final boolean keycapSpecial = s.equals("🔟");
            if (status != EmojiAnnotations.Status.found || keycapSpecial) {
                if (keycapSpecial) {
                    tts = null;
                    keywords = null;
                }
                tts = tts == null ? "???" : tts;
                keywords = keywords == null || keywords.isEmpty() ? Collections.singleton("???") : keywords;
                missing.add(getKey(s) 
                        + "\t" + "=vlookup(A" + ++count + ",Internal!A:C,2,0)" 
                        + "\t" + em2.getShortName(s) 
                        + "\t" + CollectionUtilities.join(em2.getKeys(s), " | ") 
                        + "\t\t\t" + tts 
                        + "\t" + CollectionUtilities.join(keywords, " | ")
                        + "\t" + "-");
                counts.add(em2.getShortName(s), em2.getKeys(s));
            }
        }
        if (!missing.isEmpty()) {
            if (em2 != null) {
                out.println(localeStr
                        + "\tImage\tEnglish Name\tEnglish Keywords\tNative Name\tNative Keywords\tName (constructed!)\tNative Keywords (constructed!)\t-");
            }
            for (String s : missing) {
                out.println(s);
            }
        }
        return em;
    }
}
