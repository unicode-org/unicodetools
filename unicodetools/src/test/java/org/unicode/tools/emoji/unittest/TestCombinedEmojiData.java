package org.unicode.tools.emoji.unittest;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.unicode.tools.emoji.EmojiDataSourceCombined;

/**
 * This is a bit of a hack to run TestEmojiData on two sets of data. One is the Beta version of EmojiData,
 * and the other is the EmojiDataSourceCombined, which adds in CandidateData.
 * The latter is needed where we are first using the CandidateData to generate the emoji-data files.
 * TODO: use a @ParameterizedTest instead
 * @author markdavis
 */
public class TestCombinedEmojiData extends TestEmojiData {
    public TestCombinedEmojiData() {
        // super(new EmojiDataSourceCombined(EmojiData.of(Emoji.VERSION_TO_TEST)), EmojiOrder.of(Emoji.VERSION_TO_TEST));
        super(new EmojiDataSourceCombined(TestAll.DATA_TO_TEST), TestAll.ORDER_TO_TEST);
    }

    @Test
    public void TestA() {
        super.TestA();
        boolean errorShown = false;
        Set<String> myMethods = new HashSet<>();
        for (Method method : TestCombinedEmojiData.class.getMethods()) {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass == TestCombinedEmojiData.class) {
                myMethods.add(method.getName());
            }
        }

        for (Method method : TestEmojiData.class.getMethods()) {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass == TestEmojiData.class) {
                String name = method.getName();
                if (myMethods.contains(name)) {
                    continue;
                }
                String lower = name.toLowerCase(Locale.ROOT);
                if (!lower.contains("test")) {
                    continue;
                }
                if (!errorShown) {
                    errln("TestCombinedEmojiData missing methods from TestEmojiData. Need to add these so hack works:\n");
                    errorShown = true;
                }
                System.out.println("    @Override\n    public void " + name
                        + "() {\n        super." + name
                        + "();\n    }\n");
            }
        };
    }

    @Test
    @Override
    public void TestPublicEmojiTest() {
        super.TestPublicEmojiTest();
    }

    @Test
    @Override
    public void TestHandshake() {
        super.TestHandshake();
    }

    @Test
    @Override
    public void TestCompoundNames() {
        super.TestCompoundNames();
    }

    @Test
    @Override
    public void TestDefectives() {
        super.TestDefectives();
    }

    @Test
    @Override
    public void TestFlags() {
        super.TestFlags();
    }

//    @Override
//    public void TestZwjCategories() {
//        super.TestZwjCategories();
//    }

    @Test
    @Override
    public void TestOrderRulesWithoutSkin() {
        super.TestOrderRulesWithoutSkin();
    }

    @Test
    @Override
    public void TestOrderRulesWithSkin() {
        super.TestOrderRulesWithSkin();
    }

    @Test
    @Override
    public void TestAnnotationsCompleteness() {
        super.TestAnnotationsCompleteness();
    }

    @Test
    @Override
    public void TestGroupEmoji() {
        super.TestGroupEmoji();
    }

    @Test
    @Override
    public void TestExplicitGender() {
        super.TestExplicitGender();
    }

    @Test
    @Override
    public void TestCombinations() {
        super.TestCombinations();
    }

    @Test
    @Override
    public void TestBuckets() {
        super.TestBuckets();
    }

    @Test
    @Override
    public void TestOrderRulesSimple() {
        super.TestOrderRulesSimple();
    }

    @Test
    @Override
    public void TestOrderVariants() {
        super.TestOrderVariants();
    }

}
