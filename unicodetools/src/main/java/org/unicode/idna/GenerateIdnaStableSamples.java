package org.unicode.idna;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.XSymbolTable;
import org.unicode.props.UnicodeProperty;
import org.unicode.props.UnicodePropertySymbolTable;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.ToolUnicodeTransformFactory;
import org.unicode.text.utility.UnicodeTransform;

public class GenerateIdnaStableSamples {
    public static void main(String[] args) {

        Default.setUCD("9.0.0");
        UnicodeTransform.setFactory(new ToolUnicodeTransformFactory());
        final ToolUnicodePropertySource toolUPS1 =
                ToolUnicodePropertySource.make(Default.ucdVersion());
        final XSymbolTable toolUPS = new UnicodePropertySymbolTable(toolUPS1);
        UnicodeSet.setDefaultXSymbolTable(toolUPS);
        UnicodeProperty.ResetCacheProperties();

        final String[] samples = {
            "// bidi",
            "[[:bc=R:][:bc=AL:]-[:Cn:]]",
            "[[:bc=L:]-[:Cn:]]",
            "[[:bc=ES:][:bc=CS:][:bc=ET:][:bc=ON:][:bc=BN:][:bc=NSM:]-[:Cn:]]",
            "[[:bc=EN:]-[:Cn:]]",
            "[[:bc=AN:]-[:Cn:]]",
            "[[:bc=NSM:]-[:Cn:]]",
            "// contextj", //
            "[\u200C\u200D]",
            "[[:ccc=virama:]-[:Cn:]]",
            "[[:jt=T:]-[:Cn:]]",
            "[[:jt=L:][:jt=D:]-[:Cn:]]",
            "[[:jt=R:][:jt=D:]-[:Cn:]]",
            "// syntax", //
            "[-]",
            "// changed mapping from 2003", //
            "[[\u04C0 \u10A0-\u10C5 \u2132 \u2183 \u2F868 \u2F874 \u2F91F \u2F95F \u2F9BF \u3164 \uFFA0 \u115F \u1160 \u17B4 \u17B5 \u1806]-[:Cn:]]",
            "// disallowed in 2003", // disallowed in 2003
            "[[\u200E-\u200F \u202A-\u202E \u2061-\u2063 \uFFFC \uFFFD \u1D173-\u1D17A \u206A-\u206F \uE0001 \uE0020-\uE007F]-[:Cn:]]",
            "// Step 7", //
            "[[\u2260 \u226E \u226F \uFE12 \u2488]-[:Cn:]]",
            "// disallowed",
            "[[:S:][:P:][:C:]-[:Cn:][:noncharactercodepoint:][\\U000D0000\\U000E0000\\U000F0000\\U00100000]]",
            "// deviations", //
            "[[\\u200C\\u200D\\u00DF\\u03C2]-[:Cn:]]",
        };

        for (int i = 0; i < samples.length; ++i) {
            String sample = samples[i];
            if (sample.contains("[")) {
                UnicodeSet sampleSet = trim(new UnicodeSet(samples[i]), 20);
                String printable = sampleSet.toPattern(true);
                System.out.println("\"" + printable.replace("\\", "\\\\") + "\",");
            } else {
                System.out.println(sample);
            }
        }
    }

    private static UnicodeSet trim(UnicodeSet source, int maxRanges) {
        // take at most maxRanges
        UnicodeSet result = new UnicodeSet();
        int rangeCount = source.getRangeCount();
        if (rangeCount < maxRanges) {
            result.addAll(source); // don't just return source, won't print right
            return result;
        }
        // take first half and last half, to get a mix
        for (int i = 0; i < maxRanges / 2; ++i) {
            result.addAll(source.getRangeStart(i), source.getRangeEnd(i));
        }
        for (int i = rangeCount - (maxRanges - maxRanges / 2); i < rangeCount; ++i) {
            result.addAll(source.getRangeStart(i), source.getRangeEnd(i));
        }
        if (result.getRangeCount() != maxRanges) {
            throw new IllegalArgumentException();
        }
        return result;
    }
}
