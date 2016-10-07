package org.unicode.tools.emoji;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
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
import org.unicode.cldr.util.Annotations.AnnotationSet;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiAnnotations.Status;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row.R2;

/**
 * Generates csvs for spreadsheet, to be parsed with ParseSpreadsheetAnnotations
 * @author markdavis
 *
 */
public class GenerateMissingAnnotations {
    private final static Factory FACTORY = CLDRConfig.getInstance().getCldrFactory();
    public final static CLDRFile ENGLISH_CLDR = CLDRConfig.getInstance().getEnglish();
    public final static AnnotationSet ENGLISH_ANNOTATIONS = Annotations.getDataSet("en");
    public final static Set<String> SORTED_EMOJI;
    public final static Map<String,String> CHARACTER_LABELS_AND_PATHS;
    public final static Map<String,String> CHARACTER_LABEL_PATTERNS_AND_PATHS;
    static {
        TreeSet<String> sorted = ENGLISH_ANNOTATIONS.keySet().addAllTo(new TreeSet<>(EmojiOrder.STD_ORDER.codepointCompare));
        sorted.remove("🔟"); // keycaps done differently
        SORTED_EMOJI = ImmutableSet.copyOf(sorted);
        CHARACTER_LABELS_AND_PATHS = loadLabels(ENGLISH_CLDR, "//ldml/characterLabels/characterLabel[");
        CHARACTER_LABEL_PATTERNS_AND_PATHS = loadLabels(ENGLISH_CLDR, "//ldml/characterLabels/characterLabelP");
    }
    public static final Set<String> LABELS = ImmutableSet.of(
            "person",
            "body",
            "place",
            "plant",
            "nature",
            "animal",
            "smiley",
            "female",
            "male",
            "weather",
            "travel",
            "sport",
            "flag",
            "building",
            "heart"
            );

    static final boolean DO_MISSING = true;
    public static void main(String[] args) throws IOException {
        if (DO_MISSING) {
            generateMissing();
        } else {
            countWords();
        }
    }

    private static void countWords() {
        Multimap<String,String> orderWordToCps = HashMultimap.create();
        Counter<String> orderCounter = new Counter<>();

        for (Entry<String, Set<String>> entry : EmojiOrder.STD_ORDER.orderingToCharacters.keyValuesSet()) {
            final String orderWords = entry.getKey();
            final Set<String> cps = entry.getValue();
            //System.out.println(orderWords + "\t" + cps);
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
            if (count < 5) continue;
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


        final String emojiDir = Settings.GEN_DIR + "emoji/";
        final String annotationDir = emojiDir + "annotations-v4.0/";
        try (PrintWriter out = FileUtilities.openUTF8Writer(emojiDir, "images.txt")) {
            for (String s : SORTED_EMOJI) {
                out.println(getKey(s) 
                        + "\t" + s
                        + "\t" + ENGLISH_ANNOTATIONS.getShortName(s));
            }
        }

        Set<String> skipped = new LinkedHashSet<>();
        Map<String, Counts> countMap = new TreeMap<>();
        Counts totals = new Counts();

        for (String s : locales) {
            if (!Annotations.getAvailableLocales().contains(s) 
                    || s.equals("en") 
                    //|| s.equals("ga")
                    || (s.contains("_") && !s.equals("zh_Hant")) // hack for now to reduce data size. Redo once base locales are populated
                    ) {
                skipped.add(s);
                continue;
            }
            Counts counts = new Counts();
            final String fileName = s + ".tsv";
            try (PrintWriter out = FileUtilities.openUTF8Writer(annotationDir, fileName)) {
                System.out.println(s + "\t" + config.getEnglish().getName(s));
                doAnnotations(s, out, SORTED_EMOJI, counts);
            }
            if (counts.isEmpty()) {
                System.out.println("No missing items, removing: " + fileName);
                new File(annotationDir, fileName).delete();
            } else {
                countMap.put(s, counts);
                totals.add(counts);
            }
        }
        for (String skip : skipped) {
            System.out.println("Skipping " + skip + " ("+  config.getEnglish().getName(skip) + "), no current CLDR annotation data");
        }
        for (Entry<String, Counts> entry : countMap.entrySet()) {
            String locale = entry.getKey();
            System.out.println(locale + "\t"+  config.getEnglish().getName(locale) + "\t" + entry.getValue());
        }
        System.out.println("Totals:\t\t" + totals);
    }

    static final class Counts {
        int emojiCount;
        //        int charCount;

        void add(String tts, Set<String> keywords) {
            ++emojiCount;
            //            // rough char count
            //            Set<String> foo = new LinkedHashSet<>();
            //            foo.add(tts);
            //            foo.addAll(keywords);
            //            String combined = CollectionUtilities.join(foo, " ");
            //            charCount += Math.min(combined.length() / 6, 6);
        }
        public boolean isEmpty() {
            // TODO Auto-generated method stub
            return emojiCount == 0;
        }
        public void add(Counts other) {
            emojiCount += other.emojiCount;
            //            charCount += other.charCount;
        }
        @Override
        public String toString() {
            return String.valueOf(emojiCount)
                    //                    + "\t" + charCount
                    ;
        }
    }

    public static String getKey(String s) {
        return "_" + Utility.hex(s,"_").toLowerCase(Locale.ROOT).replace("_fe0f", "");
    }

    private static void doAnnotations(final String localeStr, PrintWriter out, Set<String> sorted, Counts counts) {
        UnicodeMap<Annotations> em = Annotations.getDataSet(localeStr).getExplicitValues();
        CLDRFile main = FACTORY.make(localeStr, true);
        Set<String> missing = new LinkedHashSet<>();
        int maxLen = 32;

        int count = 1;
        for (String s : sorted) {
            if (s.equals("🕵‍♂️")) {
                int debug = 0;
            }
            final Annotations annotations = em.get(s);
            Set<String> keywords = annotations == null ? null : annotations.getKeywords();
            String tts = annotations == null ? null : annotations.getShortName();
            if (annotations == null || tts == null || keywords == null || keywords.isEmpty()) {
                tts = tts == null ? "???" : tts;
                keywords = keywords == null || keywords.isEmpty() ? Collections.singleton("???") : keywords;
                ++count;
                final String engShortName = ENGLISH_ANNOTATIONS.getShortName(s);
                final Set<String> engKeys = ENGLISH_ANNOTATIONS.getKeywords(s);
                showMissingLine(missing, count, getKey(s), engShortName, engKeys, 
                        "", "", tts, keywords);
                counts.add(engShortName, engKeys);
            }
        }
        // labels
        for (Entry<String, String> labelAndPath : CHARACTER_LABELS_AND_PATHS.entrySet()) {
            final String stringValue = ENGLISH_CLDR.getStringValue(labelAndPath.getValue());
            String nativeKeys = "n/a".equals("n/a") ? "n/a" : "";
            showMissingLine(missing, ++count, labelAndPath.getKey(), stringValue, Collections.singleton("n/a"), 
                    "", nativeKeys, "n/a", Collections.singleton("n/a"));
            counts.add(stringValue, null);
        }
        for (Entry<String, String> labelAndPath : CHARACTER_LABEL_PATTERNS_AND_PATHS.entrySet()) {
            final String stringValue = ENGLISH_CLDR.getStringValue(labelAndPath.getValue());
            String nativeKeys = "n/a".equals("n/a") ? "n/a" : "";
            showMissingLine(missing, ++count, labelAndPath.getKey(), stringValue, Collections.singleton("n/a"), 
                    "", nativeKeys, "n/a", Collections.singleton("n/a"));
            counts.add(stringValue, null);
        }
        if (!missing.isEmpty()) {
            out.println(localeStr
                    + "\tImage\tEnglish Name\tEnglish Keywords\tNative Name\tNative Keywords\tFYI Name (constructed!)\tFYI Native Keywords (constructed!)\t-\tInternal GTN\tInternal GTK");
            for (String s : missing) {
                out.println(s);
            }
        }
    }
    
    static ImmutableMap<String, String> loadLabels(CLDRFile main, String prefix) {
        Map<String,String> result = new TreeMap<>();
        for (String path : With.in(ENGLISH_CLDR.iterator(prefix))) {
            String source = main.getSourceLocaleID(path, null);
            if (source.equals(XMLSource.ROOT_ID) || source.equals(XMLSource.CODE_FALLBACK_ID)) {
                continue;
            }
            String value = main.getStringValue(path);
            if (value == null) {
                continue;
            }
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String type = parts.getAttributeValue(-1, "type");
            result.put(type, value);
        }
        return ImmutableMap.copyOf(result);
    }

    static final Splitter BAR = Splitter.on('|').trimResults();

    static void showMissingLine(Set<String> missing, int count, String emoji, String engShortName, Set<String> engKeys,
            String nativeShortName, String nativeKeys,
            String fyiShortName, Set<String> fyiKeywords) {
        final String image = CHARACTER_LABELS_AND_PATHS.keySet().contains(emoji)  || CHARACTER_LABEL_PATTERNS_AND_PATHS.keySet().contains(emoji) ? "n/a" : "=vlookup(A" + count + ",Internal!A:C,2,0)";
        missing.add(emoji 
                + "\t" + image 
                + "\t" + engShortName 
                + "\t" + CollectionUtilities.join(engKeys, " | ") 
                + "\t" + nativeShortName
                + "\t" + nativeKeys
                + "\t" + fyiShortName 
                + "\t" + CollectionUtilities.join(fyiKeywords, " | ")
                + "\t" + "-"
                + "\t" + (nativeShortName.isEmpty() ? "=googletranslate(E" + count + ",A$1,\"en\")" : "n/a")
                + "\t" + (nativeKeys.isEmpty() ? "=googletranslate(regexreplace(F" + count + ",\"\\s*\\|\\s*\",\"/ \"),A$1,\"en\")" : "n/a")
                );
    }
}
