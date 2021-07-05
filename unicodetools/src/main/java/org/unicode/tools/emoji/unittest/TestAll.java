package org.unicode.tools.emoji.unittest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiOrder;

import com.ibm.icu.dev.test.TestFmwk;

public class TestAll extends TestFmwk.TestGroup {
    
    public static EmojiOrder ORDER_TO_TEST = EmojiOrder.of(Emoji.VERSION_TO_TEST);
    public static EmojiOrder ORDER_TO_TEST_PREVIOUS = EmojiOrder.of(Emoji.VERSION_TO_TEST_PREVIOUS);

    public static EmojiData DATA_TO_TEST = ORDER_TO_TEST.emojiData;
    public static EmojiData DATA_TO_TEST_PREVIOUS = ORDER_TO_TEST_PREVIOUS.emojiData;

    public static final String VERSION_TO_TEST_STRING = Emoji.VERSION_TO_TEST.getVersionString(2, 4);
    public static final String VERSION_TO_TEST_PREVIOUS_STRING = Emoji.VERSION_TO_TEST_PREVIOUS.getVersionString(2, 4);

    public static void main(String[] args) throws Exception {
        new TestAll().run(args);
    }

    public TestAll() {
        super(getDirNames(TestAll.class));
    }

    private static String[] getDirNames(Class<?> class1) {
        String dirName = FileUtilities.getRelativeFileName(TestAll.class, ".");
        List<String> result = new ArrayList<>();
        for (String s : new File(dirName).list()) {
            if (s.endsWith(".java") || s.endsWith(".class")) {
                if (!s.startsWith("TestAll.") && s.toLowerCase(Locale.ROOT).contains("test")) {
                    result.add(s.substring(0, s.lastIndexOf('.')));
                }
            }
        };
        return result.toArray(new String[result.size()]);
    }
}
