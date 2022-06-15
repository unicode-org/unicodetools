package org.unicode.tools;

import com.google.common.base.Objects;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

public class NormalizeForMatchDiff {
    public static void main(String[] args) {
        NormalizeForMatch production =
                NormalizeForMatch.load(
                        "/Users/markdavis/Google Drive/workspace/Generated/n4m-old/",
                        "xnfkccf_curated.txt");
        NormalizeForMatch sourceDirectory = NormalizeForMatch.load(null, "XNFKCCF-Curated.txt");
        NormalizeForMatch dir90 =
                NormalizeForMatch.load(
                        Settings.UnicodeTools.DATA_DIR + "n4m/9.0.0/", "XNFKCCF-Curated.txt");
        NormalizeForMatch gen =
                NormalizeForMatch.load(
                        "/Users/markdavis/Google Drive/workspace/Generated/n4m/",
                        "XNFKCCF-Curated.txt");

        UnicodeSet keys =
                new UnicodeSet()
                        .addAll(production.getSourceToTarget())
                        .addAll(sourceDirectory.getSourceToTarget())
                        .addAll(dir90.getSourceToTarget())
                        .addAll(gen.getSourceToTarget());

        System.out.println(
                Utility.hex("Key")
                        + "\t"
                        + "pValue"
                        + "\t"
                        + "sValue"
                        + "\t"
                        + "dValue9"
                        + "\t"
                        + "Age"
                        + "\t"
                        + "Name");

        for (String key : keys) {
            String pValue = production.getSourceToTarget().get(key);
            String sValue = sourceDirectory.getSourceToTarget().get(key);
            String dValue9 = dir90.getSourceToTarget().get(key);
            String gValue = gen.getSourceToTarget().get(key);
            if (Objects.equal(pValue, sValue)
                    && Objects.equal(pValue, dValue9)
                    && Objects.equal(pValue, gValue)) {
                continue;
            }
            System.out.println(
                    Utility.hex(key)
                            + "\t"
                            + pValue
                            + "\t"
                            + sValue
                            + "\t"
                            + dValue9
                            + "\t"
                            + gValue
                            + "\t"
                            + UCharacter.getAge(key.codePointAt(0))
                            + "\t"
                            + UCharacter.getName(key.codePointAt(0)));
        }
    }
}
