package org.unicode.text.UCA;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Types;

/**
 * Helper class for reorder codes:
 * Script code (0..FF) or reordering group code
 * ({@link ReorderCodes#FIRST}..{@link ReorderCodes#LIMIT}).
 *
 * <p>Note: The <i>names</i> of the special-group constants are the same as in
 * {@link com.ibm.icu.text.Collator.ReorderCodes}
 * but the script and reorder code numeric <i>values</i>
 * are totally different from those in ICU.
 */
public class ReorderCodes {
    public static final int FIRST = 0x100;
    public static final int SPACE = 0x100;
    public static final int PUNCTUATION = 0x101;
    public static final int SYMBOL = 0x102;
    public static final int CURRENCY = 0x103;
    public static final int DIGIT = 0x104;
    public static final int LIMIT = 0x105;

    public static final int REORDER_RESERVED_BEFORE_LATIN = 0x106;
    public static final int REORDER_RESERVED_AFTER_LATIN = 0x107;

    public static final int FULL_LIMIT = 0x108;

    private static final String[] SPECIAL_NAMES = {
        "SPACE", "PUNCTUATION", "SYMBOL", "CURRENCY", "DIGIT", null,
        "REORDER_RESERVED_BEFORE_LATIN", "REORDER_RESERVED_AFTER_LATIN"
    };

    /**
     * Sample characters for collation-specific reordering groups.
     * See the comments on {@link #getSampleCharacter(int)}.
     */
    private static final String[] SPECIAL_SAMPLES = {
        "\u00A0", "\u201C", "\u263A", "\u20AC", "4", null,
        "\uFF21", "\uFF3A"
    };

    public static final int getSpecialReorderCode(int ch) {
        final byte cat = Fractional.getFixedCategory(ch);
        switch (cat) {
        case UCD_Types.SPACE_SEPARATOR:
        case UCD_Types.LINE_SEPARATOR:
        case UCD_Types.PARAGRAPH_SEPARATOR:
        case UCD_Types.CONTROL:
            return SPACE;
        case UCD_Types.DASH_PUNCTUATION:
        case UCD_Types.START_PUNCTUATION:
        case UCD_Types.END_PUNCTUATION:
        case UCD_Types.CONNECTOR_PUNCTUATION:
        case UCD_Types.OTHER_PUNCTUATION:
        case UCD_Types.INITIAL_PUNCTUATION:
        case UCD_Types.FINAL_PUNCTUATION:
            return PUNCTUATION;
        case UCD_Types.OTHER_SYMBOL:
        case UCD_Types.MATH_SYMBOL:
        case UCD_Types.MODIFIER_SYMBOL:
            return SYMBOL;
        case UCD_Types.CURRENCY_SYMBOL:
            return CURRENCY;
        case UCD_Types.DECIMAL_DIGIT_NUMBER:
        case UCD_Types.LETTER_NUMBER:
        case UCD_Types.OTHER_NUMBER:
            return DIGIT;
        default:
            // Lm etc.
            return SYMBOL;
        }
    }

    public static final String getNameForSpecial(int reorderCode) {
        return SPECIAL_NAMES[reorderCode - FIRST];
    }

    public static final String getName(int reorderCode) {
        if (reorderCode < FIRST) {
            String name = UCD.getScriptID_fromIndex((byte) reorderCode);
            if (name != null) {
                return name;
            }
            // TODO:
            // - Remove scripts supported by ICU4J UScript and CLDR ScriptMetadata.
            // - Add scripts not yet supported there.
            switch (reorderCode) {
            case UCD_Types.Ahom:
                return "Ahom";
            case UCD_Types.Anatolian_Hieroglyphs:
                return "Anatolian_Hieroglyphs";
            case UCD_Types.Hatran:
                return "Hatran";
            case UCD_Types.Multani:
                return "Multani";
            case UCD_Types.Old_Hungarian:
                return "Old_Hungarian";
            case UCD_Types.Sign_Writing:
                return "Sign_Writing";
            default:
                throw new UnsupportedOperationException("unknown reorderCode " + reorderCode);
            }
        } else {
            return SPECIAL_NAMES[reorderCode - FIRST];
        }
    }

    public static final String getShortName(int reorderCode) {
        if (reorderCode < FIRST) {
            String name = UCD.getScriptID_fromIndex((short) reorderCode, UCD_Types.SHORT);
            if (name != null) {
                return name;
            }
            // TODO:
            // - Remove scripts supported by ICU4J UScript and CLDR ScriptMetadata.
            // - Add scripts not yet supported there.
            switch (reorderCode) {
            case UCD_Types.Ahom:
                return "Ahom";
            case UCD_Types.Anatolian_Hieroglyphs:
                return "Hluw";
            case UCD_Types.Hatran:
                return "Hatr";
            case UCD_Types.Multani:
                return "Mult";
            case UCD_Types.Old_Hungarian:
                return "Hung";
            case UCD_Types.Sign_Writing:
                return "Sgnw";
            default:
                throw new UnsupportedOperationException("unknown reorderCode " + reorderCode);
            }
        } else {
            return SPECIAL_NAMES[reorderCode - FIRST];
        }
    }

    /**
     * Returns a sample character string for the reorder code.
     * For regular scripts, it is the sample character defined in CLDR ScriptMetadata.txt.
     * For collation-specific codes it is a hardcoded value.
     *
     * <p>It is probably a good idea to use sample characters that map to a single primary CE.
     */
    public static final String getSampleCharacter(int reorderCode) {
        if (reorderCode < FIRST) {
            final String scriptName = UCD.getScriptID_fromIndex((short) reorderCode, UCD_Types.SHORT);
            final ScriptMetadata.Info info = ScriptMetadata.getInfo(scriptName);
            if (info != null) {
                return info.sampleChar;
            }
            // TODO:
            // - Remove scripts supported by ICU4J UScript and CLDR ScriptMetadata.
            // - Add scripts not yet supported there.
            switch (reorderCode) {
            case UCD_Types.Ahom:
                return "𑜗";
            case UCD_Types.Anatolian_Hieroglyphs:
                return "𔐀";
            case UCD_Types.Hatran:
                return "𐣴";
            case UCD_Types.Multani:
                return "𑊏";
            case UCD_Types.Old_Hungarian:
                return "𐲡";
            case UCD_Types.Sign_Writing:
                return "𝡐";
            default:
                throw new UnsupportedOperationException("unknown reorderCode " + reorderCode);
            }
        } else {
            return SPECIAL_SAMPLES[reorderCode - FIRST];
        }
    }
    public static final String getScriptStartString(int reorderCode) {
        String sampleChar = getSampleCharacter(reorderCode);
        // Use the U+FDD0 prefix for reorder-reserved ranges
        // so that their primaries are registered but their mappings are not.
        return (reorderCode < LIMIT ? "\uFDD1" : "\uFDD0") + sampleChar;
    }
}
