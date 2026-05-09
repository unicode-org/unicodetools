package org.unicode.text.UCD;

import org.unicode.text.utility.UnicodeTransform;
import org.unicode.text.utility.UnicodeTransform.Type;

public class ToolUnicodeTransformFactory implements UnicodeTransform.Factory {

    @Override
    public UnicodeTransform getInstance(Type type) {
        switch (type) {
            case NFC:
                return new ToolUnicodeNormalizer(
                        Normalizer.getOrMakeNfcInstance(Default.ucdVersion()));
            case NFKC:
                return new ToolUnicodeNormalizer(
                        Normalizer.getOrMakeNfkcInstance(Default.ucdVersion()));
            case NFD:
                return new ToolUnicodeNormalizer(
                        Normalizer.getOrMakeNfdInstance(Default.ucdVersion()));
            case NFKD:
                return new ToolUnicodeNormalizer(
                        Normalizer.getOrMakeNfkdInstance(Default.ucdVersion()));
            case CASEFOLD:
                return new CaseFolder();
            default:
                throw new IllegalArgumentException();
        }
    }

    private static class ToolUnicodeNormalizer extends UnicodeTransform {
        org.unicode.text.UCD.Normalizer normalizer;

        public ToolUnicodeNormalizer(Normalizer norm) {
            normalizer = norm;
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
