package org.unicode.unittest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.unicode.cldr.draft.FileUtilities;

import com.ibm.icu.dev.test.TestFmwk;

public class TestAll extends TestFmwk.TestGroup {
    
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
                if (!s.startsWith("TestAll.")
                	&& !s.contains("$") 
                	&& s.toLowerCase(Locale.ROOT).contains("test")) {
                    result.add(s.substring(0, s.lastIndexOf('.')));
                }
            }
        };
        return result.toArray(new String[result.size()]);
    }
}
