package org.unicode.propstest;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import java.util.BitSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.draft.ScriptMetadata.Trinary;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Line_Break_Values;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.UCD.Default;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestScriptMetadata extends TestFmwkMinusMinus {
    static final IndexUnicodeProperties LATEST = IndexUnicodeProperties.make(Default.ucdVersion());

    static final UnicodeMap<String> LB = LATEST.load(UcdProperty.Line_Break);
    static final UnicodeMap<String> SCRIPT = LATEST.load(UcdProperty.Script);
    static final UnicodeProperty SCRIPT_PROPERTY =
            LATEST.getProperty(UcdProperty.Script.toString());
    static final UnicodeProperty GENERAL_CATEGORY =
            LATEST.getProperty(UcdProperty.General_Category.toString());
    static final UnicodeSet LB_COMPLEX_OR_ID =
            new UnicodeSet()
                    .addAll(LB.getSet(Line_Break_Values.Ideographic.toString()))
                    .addAll(LB.getSet(Line_Break_Values.Complex_Context.toString()))
                    .retainAll(
                            GENERAL_CATEGORY.getSet(
                                    UcdPropertyValues.General_Category_Values.Other_Letter
                                            .toString()))
                    .freeze();

    private static final UnicodeSet LB_EXCEPTION = new UnicodeSet("[\u3131-\u318E]").freeze();

    @Test
    @Disabled("Broken")
    public void TestScriptOfSample() {
        BitSet bitset = new BitSet();
        main:
        for (String script : new TreeSet<String>(ScriptMetadata.getScripts())) {
            switch (script) {
                case "Kore":
                case "Hant":
                case "Hans":
                case "Jpan":
                    continue main;
            }
            Info info0 = ScriptMetadata.getInfo(script);
            assertEquals(
                    script + " Sample must be single character",
                    1,
                    UTF16.countCodePoint(info0.sampleChar));
            String infoScript =
                    script.equals("Kore") ? "Hang" : script.equals("Japn") ? "Kana" : script;

            UnicodeSet withScript;
            try {
                withScript = SCRIPT_PROPERTY.getSet(script);
            } catch (Exception e) {
                errln("Failure with script value: " + script);
                continue;
            }

            int sampleCodepoint = info0.sampleChar.codePointAt(0);
            String sampleCharScript = SCRIPT.get(sampleCodepoint);

            assertTrue(
                    script + " Must have single, valid script ",
                    withScript.contains(sampleCodepoint));

            String lbString = LB.get(sampleCodepoint);
            Line_Break_Values lbEnum = UcdPropertyValues.Line_Break_Values.valueOf(lbString);
            boolean isComplex =
                    (lbEnum == Line_Break_Values.Complex_Context
                            || lbEnum == Line_Break_Values.Ideographic);

            assertEquals(
                    "LB for " + script + " is complex?", isComplex, info0.lbLetters == Trinary.YES);

            if (script.equals("Zyyy")) {
                continue; // skip Common
            }
            UnicodeSet scriptIntersectComplex =
                    new UnicodeSet(withScript).retainAll(LB_COMPLEX_OR_ID).removeAll(LB_EXCEPTION);
            if (!assertEquals(
                    "LB for sample in " + script + " matches intersection?",
                    scriptIntersectComplex.size() != 0,
                    isComplex)) {
                int x = 0;
            }
        }
    }
}
