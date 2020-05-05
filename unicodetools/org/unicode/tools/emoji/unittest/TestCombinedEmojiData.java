package org.unicode.tools.emoji.unittest;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.unicode.tools.emoji.Emoji;
import org.unicode.tools.emoji.EmojiData;
import org.unicode.tools.emoji.EmojiDataSourceCombined;
import org.unicode.tools.emoji.EmojiOrder;

/**
 * This is a bit of a hack to run TestEmojiData on two sets of data. One is the Beta version of EmojiData, 
 * and the other is the EmojiDataSourceCombined, which adds in CandidateData. 
 * The latter is needed where we are first using the CandidateData to generate the emoji-data files.
 * @author markdavis
 */
public class TestCombinedEmojiData extends TestEmojiData {

    public TestCombinedEmojiData() {
        // super(new EmojiDataSourceCombined(EmojiData.of(Emoji.VERSION_TO_TEST)), EmojiOrder.of(Emoji.VERSION_TO_TEST));
        super(new EmojiDataSourceCombined(TestAll.DATA_TO_TEST), TestAll.ORDER_TO_TEST);
    }

    public static void main(String[] args) {
        new TestCombinedEmojiData().run(args);
    }

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

    @Override
    public void TestPublicEmojiTest() {
        super.TestPublicEmojiTest();
    }

    @Override
    public void TestHandshake() {
        super.TestHandshake();
    }

    @Override
    public void TestCompoundNames() {
        super.TestCompoundNames();
    }

    @Override
    public void TestDefectives() {
        super.TestDefectives();
    }

    @Override
    public void TestFlags() {
        super.TestFlags();
    }

//    @Override
//    public void TestZwjCategories() {
//        super.TestZwjCategories();
//    }

    @Override
    public void TestOrderRulesWithoutSkin() {
        super.TestOrderRulesWithoutSkin();
    }

    @Override
    public void TestOrderRulesWithSkin() {
        super.TestOrderRulesWithSkin();
    }

    @Override
    public void TestAnnotationsCompleteness() {
        super.TestAnnotationsCompleteness();
    }
    
    @Override
    public void TestGroupEmoji() {
        super.TestGroupEmoji();
    }
    
    @Override
    public void TestExplicitGender() {
        super.TestExplicitGender();
    }

    @Override
    public void TestCombinations() {
        super.TestCombinations();
    }

    @Override
    public void TestBuckets() {
        super.TestBuckets();
    }
    
    @Override
    public void TestOrderRulesSimple() {
        super.TestOrderRulesSimple();
    }

    @Override
    public void TestOrderVariants() {
        super.TestOrderVariants();
    }

}
