package org.unicode.tools.emoji;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ULocale;

public class CountValidEmoji {
    public static void main(String[] args) {
        countInvalid();
    }

    private static void countInvalid() {
        Validity validity = Validity.getInstance();

        countInvalid(validity, null);
        countInvalid(validity, LstrType.region);
        countInvalid(validity, LstrType.subdivision);
    }

    private static void countInvalid(Validity validity, LstrType type) {
        NumberFormat nf = NumberFormat.getIntegerInstance(ULocale.ROOT);
        long syntactic = 0;
        int recommendedSize = 0;
        double otherValidSize = 0;
        double syntactic_but_invalid_size = 0;
        String title = null;
        Set<String> other_valid = Collections.singleton("…");
        Set<String> recommended;
        Set<String> invalid = Collections.emptySet();

        if (type == null) {
            recommended = EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives().addAllTo(new LinkedHashSet<String>());
            for (Iterator<String> it = recommended.iterator(); it.hasNext();) {
               String item = it.next();
               if (!item.contains(Emoji.JOINER_STR)) {
                   it.remove();
               }
            }
            recommendedSize = recommended.size();
            otherValidSize = Double.POSITIVE_INFINITY;
            syntactic_but_invalid_size = Double.POSITIVE_INFINITY;
            title = "Emoji ZWJ Sequence";
        } else {
            Map<String, Status> codeToStatus = validity.getCodeToStatus(type);
            // idStatus="regular", "deprecated", or the "macroregion". However, for macroregions, only UN and EU are valid.
            Multimap<Status,String> inverse = Multimaps.invertFrom(Multimaps.forMap(codeToStatus), TreeMultimap.create());
            recommended = new LinkedHashSet<>(inverse.get(Status.regular));
            other_valid = new LinkedHashSet<>(inverse.get(Status.deprecated));
            switch (type) {
            case region:
                recommended.add("UN");
                recommended.add("EU");
                syntactic = 26*26;
                title = "Emoji Flag Sequence";
                invalid = new LinkedHashSet<>();
                for (char f = 'A'; f <= 'Z'; ++f) {
                    for (char s = 'A'; s <= 'Z'; ++s) {
                        invalid.add(f + "" + s);
                    }
                }
                invalid.removeAll(recommended);
                invalid.removeAll(other_valid);
                break;
            case subdivision:
                other_valid.addAll(recommended);
                recommended.clear();
                recommended.addAll(Arrays.asList("gbeng", "gbsct", "gbwls"));
                other_valid.removeAll(recommended);
                syntactic = (26*26 + 999L) * (35*36*36);
                title = "Emoji Tag Sequence";
                invalid = new LinkedHashSet<>(Arrays.asList("usa", "usb"));
                break;
            default: 
                throw new ICUException();
            }
            recommendedSize = recommended.size();
            otherValidSize = other_valid.size();
            syntactic_but_invalid_size = syntactic - recommendedSize - otherValidSize;
        }

        System.out.println("\n" + title);
        System.out.println("Recommended:\t" + nf.format(recommendedSize) + "\t" + clip(recommended));
        System.out.println("Other Valid:\t" + nf.format(otherValidSize) + "\t" + clip(other_valid));
        System.out.println("Invalid (but WF):\t" + nf.format(syntactic_but_invalid_size) + "\t" + clip(invalid));
    }

    private static String clip(Set<String> other_valid) {
        String s = other_valid.toString();
        return s.length() < 50 ? s : s.substring(0, 200) + "…";
    }
}
