package org.unicode.tools.emoji;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.EnumSet;
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
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.Emoji.Source;
import org.unicode.tools.emoji.EmojiAnnotations.Status;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.text.UnicodeSet;

public class GenerateMissingAnnotations {
    public static void main(String[] args) throws IOException {
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
                        + "\tImage\tEnglish Name\tEnglish Keywords\tNative Name\tNative Keywords\tName Name (constructed!)\tNative Keywords (constructed!)\t-");
            }
            for (String s : missing) {
                out.println(s);
            }
        }
        return em;
    }
}
