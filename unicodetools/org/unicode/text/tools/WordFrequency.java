package org.unicode.text.tools;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Counter;
import org.unicode.text.utility.Settings;

import com.google.common.base.Splitter;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;

public class WordFrequency {
    static Map<String,Long> data = new HashMap<>();
    static long total;
    static {
        Splitter tab = Splitter.on("\t").trimResults();
        for (String line : FileUtilities.in(Settings.WORKSPACE_DIRECTORY + "data/words/", "freq.txt")) {
            List<String> list = tab.splitToList(line);
            long count = Long.parseLong(list.get(1));
            data.put(list.get(0), count);
            total += count;
            if (total < 0) {
                throw new IllegalArgumentException();
            }
        }
        data = Collections.unmodifiableMap(data);
    }
    public static Long getFrequency(String word) {
        return data.get(word);
    }
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[] {"animal", "food", "sport"};
        }
        NumberFormat nf = NumberFormat.getPercentInstance(Locale.ENGLISH);
        nf.setMinimumFractionDigits(2);
        for (String arg : args) {
            Info info = new Info();
            info.addCharAnnotations(arg);
            info.addFile(arg);
            for (R2<Long, String> entry : info.fileData.getEntrySetSortedByCount(false, null)) {
                final String item = entry.get1();
                UnicodeSet us = EmojiData.getAnnotationSet(item);
                System.out.println(arg + "\t" + item + "\t" + nf.format(entry.get0() / info.total)
                        + "\t" + (us == null || us.size() == 0 ? "" : us.toPattern(false)));
            }
        }
    }

    static class Info {
        Counter<String> fileData = new Counter();
        double total = 0;

        void addFile(String fileName) {
            for (String line2 : FileUtilities.in(WordFrequency.class, "words/" + fileName + ".txt")) {
                add(line2);
            }
        }

        static final Set<String> SKIP = new HashSet(Arrays.asList("love",
                "open", "water", "baby", "print", "heart", "face",
                "oh", "nature", "eye", "feet", "lady", "object", "peace",
                "mouth", "shell", "smile", "romance",
                "kiss", "joy", "flying", "sad", "cry", "nose", "tropical",
                "surprised", "tears", "smiling",
                "tear", "creature", "spiral", "smiley", "grin", "hump",
                "ironic", "weary", "hatching", "flipper",
                "grinning", "extraterrestrial", "wry", "spouting", "pouting",
                "hot", "travel", "red", "green", "box", "cup", "human", "bar",
                "french", "glass", "japanese", "square", "birthday", "sweet",
                "ball", "plant", "soft", "delicious", "stick", "celebration",
                "shaved", "yum", "um", "fried", "cooked", "slice", "roasted",
                "sliced", "swirl", "frying", "steaming", "savouring",
                "clinking",
                "not", "no", "game", "car", "american", "entertainment",
                "person", "weight", "shirt", "vehicle", "ice", "flag", "pool",
                "mountain", "hole", "eight", "prohibited", "prize", "pole",
                "forbidden", "hoop", "sash", "checkered"));
        
        public void addCharAnnotations(String arg) {
            UnicodeSet chars = EmojiData.getAnnotationSet(arg);
            for (String cp : chars) {
                for (String annote : EmojiData.getData(cp).annotations) {
                    if (!annote.equals(arg) && !SKIP.contains(annote)) {
                        add(annote);
                    }
                }
            }
        }

        public void add(String word) {
            String line = word.toLowerCase(Locale.ENGLISH);
            if (fileData.containsKey(line)) {
                return;
            }
            Long count = getFrequency(line);
            if (count != null) {
                fileData.add(line,count);
                total += count;
                //System.out.println(line + ", " + count);
            }
        }
    }
}
