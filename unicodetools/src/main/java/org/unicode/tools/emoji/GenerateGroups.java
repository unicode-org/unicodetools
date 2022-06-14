package org.unicode.tools.emoji;

import com.ibm.icu.text.UnicodeSet;
import java.util.Map.Entry;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;

public class GenerateGroups {
    public static void main(String[] args) {
        for (Entry<String, Integer> entry : EmojiOrder.BETA_ORDER.categoryToOrder.entrySet()) {
            String minor = entry.getKey();
            int order = entry.getValue();
            MajorGroup major = EmojiOrder.BETA_ORDER.getMajorGroupFromCategory(minor);
            UnicodeSet chars = EmojiOrder.BETA_ORDER.charactersToOrdering.getSet(minor);
            String sampleString = chars.iterator().next();
            System.out.println(major + "\t" + minor + "\t" + order + "\t" + sampleString);
        }
    }
}
