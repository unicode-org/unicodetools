package org.unicode.tools.emoji.unittest;

import java.util.concurrent.ConcurrentHashMap;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.ibm.icu.util.VersionInfo;

import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiOrder;

/**
 * This class fails to static init.
 * TODO: move static init to functions, so they can be tested independently.
 */
public class TestAll {

    static ConcurrentHashMap<VersionInfo, EmojiOrder> cache = new ConcurrentHashMap<>();

    static EmojiOrder get(VersionInfo v) {
        return cache.computeIfAbsent(v, vv -> EmojiOrder.of(vv));
    }

    public static EmojiOrder getOrderToTest() {
        return get(Emoji.VERSION_TO_TEST);
    }
    public static EmojiOrder getOrderToTestPrevious() {
        return get(Emoji.VERSION_TO_TEST_PREVIOUS);
    }

    public static EmojiData getDataToTest() {
        return getOrderToTest().emojiData;
    }
    public static EmojiData getDataToTestPrevious() {
        return getOrderToTestPrevious().emojiData;
    }

    public static final String VERSION_TO_TEST_STRING = Emoji.VERSION_TO_TEST.getVersionString(2, 4);
    public static final String VERSION_TO_TEST_PREVIOUS_STRING = Emoji.VERSION_TO_TEST_PREVIOUS.getVersionString(2, 4);

}
