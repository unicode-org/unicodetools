package org.unicode.propstest;

import com.ibm.icu.dev.test.TestFmwk;

public class TestAll extends TestFmwk.TestGroup {
    public static void main(String[] args) throws Exception {
        new TestAll().run(args);
    }

    public TestAll() {
        super(new String[] {
                //"TestNames",
                "TestInvariants",
                "TestProperties",
                "TestPropertyAccess",
                "TestPropNormalization",
                "TestScriptInfo",
                "TestScriptMetadata",
                "TestXUnicodeSet",
                "UnicodeRelationTest",
        });
    }
}
