package org.unicode.tools.emoji;

import com.ibm.icu.impl.Row.R2;

public class GenerateEmojiFrequency {
    public static void main(String[] args) {
        EmojiOrder order = EmojiOrder.of(Emoji.VERSION_LAST_RELEASED);

        for (R2<Long, String> entry : ListEmojiGroups.EmojiTracker.counts.getEntrySetSortedByCount(false, null)) {
            long count = entry.get0();
            String str = entry.get1();

            String category = order.getCategory(str);

            System.out.println(str
                    //+ "\tU+" + m.group(1)
                    + "\t" + EmojiData.EMOJI_DATA.getName(str)
                    + "\t" + count
                    + "\t" + order.getMajorGroupFromCategory(category).toPlainString()
                    + "\t" + category
                    + "\t" + order.getGroupOrder(category)
                    );

        }
    }
}
