package org.unicode.propstest;

import com.ibm.icu.dev.test.TestFmwk;

public class TestAll extends TestFmwk.TestGroup {
    public static void main(String[] args) throws Exception {
        new TestAll().run(args);
    }

    public TestAll() {
        super(new String[] {
            "TestProperties",
            "TestPropNormalization", 
            "TestScriptInfo.java", 
            "TestScriptMetadata", 
            "TestXUnicodeSet.java", 
            "UnicodeRelationTest",
        });
    }

}
