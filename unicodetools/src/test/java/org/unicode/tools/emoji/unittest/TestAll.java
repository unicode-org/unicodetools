package org.unicode.tools.emoji.unittest;

import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiOrder;

/**
 * This class fails to static init.
 * TODO: move static init to functions, so they can be tested independently.
 */
public class TestAll {

    public static EmojiOrder ORDER_TO_TEST = EmojiOrder.of(Emoji.VERSION_TO_TEST);
    public static EmojiOrder ORDER_TO_TEST_PREVIOUS = EmojiOrder.of(Emoji.VERSION_TO_TEST_PREVIOUS);

    public static EmojiData DATA_TO_TEST = ORDER_TO_TEST.emojiData;
    public static EmojiData DATA_TO_TEST_PREVIOUS = ORDER_TO_TEST_PREVIOUS.emojiData;

    public static final String VERSION_TO_TEST_STRING = Emoji.VERSION_TO_TEST.getVersionString(2, 4);
    public static final String VERSION_TO_TEST_PREVIOUS_STRING = Emoji.VERSION_TO_TEST_PREVIOUS.getVersionString(2, 4);

}
