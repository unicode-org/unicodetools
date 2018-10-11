package org.unicode.tools.emoji.unittest;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.unicode.tools.emoji.EmojiDataSourceCombined;

public class TestCombinedEmojiData extends TestEmojiData {

    public TestCombinedEmojiData() {
        super(new EmojiDataSourceCombined());
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
                    errln("Missing methods from TestEmojiData. Need to add these so hack works:\n");
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
    public void TestOrderRules() {
        super.TestOrderRules();
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

}
