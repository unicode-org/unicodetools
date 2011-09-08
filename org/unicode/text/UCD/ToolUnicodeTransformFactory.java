package org.unicode.text.UCD;

import com.ibm.icu.dev.test.util.UnicodeTransform;
import com.ibm.icu.dev.test.util.UnicodeTransform.Type;

public class ToolUnicodeTransformFactory implements UnicodeTransform.Factory {


    @Override
    public UnicodeTransform getInstance(Type type) {
            switch (type) {
            case NFC:
                return new ToolUnicodeNormalizer(Normalizer.NFC);
            case NFKC:
                return new ToolUnicodeNormalizer(Normalizer.NFKC);
            case NFD: 
                return new ToolUnicodeNormalizer(Normalizer.NFD);
            case NFKD:
                return new ToolUnicodeNormalizer(Normalizer.NFKD);
            case CASEFOLD:
                return new CaseFolder();
            default:
                throw new IllegalArgumentException();
            }
    }

    private static class ToolUnicodeNormalizer extends UnicodeTransform {
        org.unicode.text.UCD.Normalizer normalizer;
        public ToolUnicodeNormalizer(byte type) {
            normalizer = new Normalizer(type, Default.ucdVersion());
        }
        @Override
        public String transform(String source) {
            return normalizer.transform(source);
        }

    }
    private static class CaseFolder extends UnicodeTransform {
        @Override
        public String transform(String source) {
            // TODO Auto-generated method stub
            return Default.ucd().getCase(source, UCD_Types.FULL, UCD_Types.FOLD);
        }
    }
}
