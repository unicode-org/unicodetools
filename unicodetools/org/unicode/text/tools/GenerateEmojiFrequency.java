package org.unicode.text.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Row.R2;

public class GenerateEmojiFrequency {

    static final EmojiAnnotations ANNOTATIONS_TO_CHARS = new EmojiAnnotations(
            GenerateEmoji.CODEPOINT_COMPARE, 
            "emojiAnnotations.txt",
            "emojiAnnotationsGroups.txt",
            "emojiAnnotationsFlags.txt"
            );

    static final Set<String> KEEP_ANNOTATIONS = new LinkedHashSet<>(Arrays.asList(
            "animal",
            "sport",
            "person",
            "plant",
            "emotion",
            "face",
            "body",
            "time",
            "vehicle",
            "travel",
            "clothing",
            "food",
            "drink",
            "entertainment",
            "office",
            
            //"people",
            "places",
            "nature",
            "objects",
            "symbols",
            "flag",
            "other"));
    
    static final Set<String> DROP_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "default-text-style",
            "fitz-minimal",
            "fitz-optional",
            "nature-android",
            "nature-apple",
            "objects-android",
            "objects-apple",
            "people-android",
            "people-apple",
            "places-android",
            "places-apple",
            "symbols-android",
            "symbols-apple",
            "other-android"));


    public static void main(String[] args) {
        Splitter semi = Splitter.on(';').trimResults();
        Counter<String> frequency = new Counter<>();

        for (String line : FileUtilities.in(GenerateEmojiFrequency.class, "emojiTracker.txt")) {
            List<String> parts = semi.splitToList(line);
            String code = Utility.fromHex(parts.get(0));
            long count = Long.parseLong(parts.get(1));
            frequency.add(code, count);
        }
        Counter<String> mainCount = new Counter<>();
        Map<String,Buckets> mainList = new HashMap<>();
        
        System.out.println("%\tCP\tMain Category\tAnnotations");
        for (R2<Long, String> x : frequency.getEntrySetSortedByCount(false, GenerateEmoji.CODEPOINT_COMPARE)) {
            long count = x.get0();
            String code = x.get1();
            Set<String> annotations = ANNOTATIONS_TO_CHARS.getKeys(code);
            Set<String> keep = new LinkedHashSet<>(annotations);
            if (keep.contains("clock")) {
                keep.remove("face");
            }
            String main = "?";
            for (String s : KEEP_ANNOTATIONS) {
                if (keep.contains(s)) {
                    main = s;
                    break;
                }
            }
            keep.removeAll(DROP_ANNOTATIONS);
            double percent = count/(double)frequency.getTotal();

            System.out.println(percent + "\t" + code + "\t" + main + "\t" + CollectionUtilities.join(keep, " "));
            mainCount.add(main, count);
            Buckets list = mainList.get(main);
            if (list == null) {
                mainList.put(main, list = new Buckets());
            }
            list.add(code, percent);
        }
        
        System.out.println("%\tMain Category\t≥1%\t≥0.1%\t≥0.01%\tOthers");
        for (R2<Long, String> x : mainCount.getEntrySetSortedByCount(false,null)) {
            Long count = x.get0();
            String main = x.get1();
            double percent = count/(double)mainCount.getTotal();
            Buckets list = mainList.get(main);
            System.out.println(percent + "\t" + main + "\t" + list);
        }
    }

    static class Buckets {
        List<String> L1 = new ArrayList<>();
        List<String> L01 = new ArrayList<>();
        List<String> L001 = new ArrayList<>();
        List<String> Rest = new ArrayList<>();
        public void add(String code, double percent) {
            if (percent >= 0.01) {
                L1.add(code);
            } else if (percent >= 0.001) {
                L01.add(code);
            } else if (percent >= 0.0001) {
                L001.add(code);
            } else {
                Rest.add(code);
            }
        }
        @Override
        public String toString() {
            return CollectionUtilities.join(L1, " ")
                    + "\t" + CollectionUtilities.join(L01, " ")
                    + "\t" + CollectionUtilities.join(L001, " ")
                    + "\t" + CollectionUtilities.join(Rest, " ")
                    ;
        }
    }
}
