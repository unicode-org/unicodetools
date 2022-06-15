package org.unicode.tools;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.Transform;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.UCD.Default;

public abstract class Normalizer3 implements Transform<String, String> {
    private static final IndexUnicodeProperties iup =
            IndexUnicodeProperties.make(Default.ucdVersion());
    private static final UnicodeMap<String> NFKC_Casefold = iup.load(UcdProperty.NFKC_Casefold);

    public String normalize(String source) {
        return transform(source);
    }

    public static final Normalizer3 NFKCCF =
            new Normalizer3() {
                @Override
                public String transform(String source) {
                    return Default.nfc().normalize(NFKC_Casefold.transform(source));
                }
            };
    public static final Normalizer3 NFKC =
            new Normalizer3() {
                @Override
                public String transform(String source) {
                    return Default.nfkc().normalize(source);
                }
            };
}
