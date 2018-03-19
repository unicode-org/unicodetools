package org.unicode.parse;

import org.unicode.parse.Tokenizer.Result;

import com.ibm.icu.dev.test.TestFmwk;

public class TestTokenizer extends TestFmwk {
    public static void main(String[] args) {
        new TestTokenizer().run(args);
    }

    public void TestTokens() {
        String[][] tests ={
                {"abc'def'*546.10", "STRING:«abcdef» CODEPOINT:* NUMBER:546 CODEPOINT:. NUMBER:10 DONE"}
        };
        Tokenizer tokenizer = new Tokenizer();
        for (String[] test : tests) {
            tokenizer.setSource(test[0]);
            StringBuilder actual = new StringBuilder();
            while (true) {
                Result result = tokenizer.next();
                if (actual.length() != 0) {
                    actual.append(" ");     
                }
                actual.append(tokenizer);
                if (result == Result.DONE) {
                    break;
                }
            }
            System.out.println("Source: " + test[0]);
            System.out.println(actual.toString());
            assertEquals(test[0], test[1], actual.toString());
        }
    }
}
