package org.unicode.tools.emoji;

import java.util.Map.Entry;

import com.ibm.icu.impl.Row.R2;

public class GenerateEmojiFrequency {
    public static void main(String[] args) {
        ListEmojiGroups.main(args);
//        EmojiOrder order = EmojiOrder.of(Emoji.VERSION_LAST_RELEASED);
//
//        for (Entry<String, Long> entry : ListEmojiGroups.EmojiTracker.countInfo.keyToCount.entrySet()) {
//            long count = entry.getValue();
//            String str = entry.getKey();
//
//            String category = order.getCategory(str);
//
//            System.out.println(str
//                    //+ "\tU+" + m.group(1)
//                    + "\t" + EmojiData.EMOJI_DATA.getName(str)
//                    + "\t" + count
//                    + "\t" + order.getMajorGroupFromCategory(category).toPlainString()
//                    + "\t" + category
//                    + "\t" + order.getGroupOrder(category)
//                    );
//
//        }
    }
}
