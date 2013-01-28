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

	private static final String[] SPECIAL_NAMES = {
		"SPACE", "PUNCTUATION", "SYMBOL", "CURRENCY", "DIGIT"
	};

	/**
	 * Sample characters for collation-specific reordering groups.
	 * See the comments on {@link #getSampleCharacter(int)}.
	 */
	private static final String[] SPECIAL_SAMPLES = {
		"\u00A0", "\u201C", "\u263A", "\u20AC", "4"
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
			return UCD.getScriptID_fromIndex((byte) reorderCode);
		} else {
			return SPECIAL_NAMES[reorderCode - FIRST];
		}
	}

	public static final String getShortName(int reorderCode) {
		if (reorderCode < FIRST) {
			return UCD.getScriptID_fromIndex((byte) reorderCode, UCD_Types.SHORT);
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
			final String scriptName = UCD.getScriptID_fromIndex((byte) reorderCode, UCD_Types.SHORT);
			final ScriptMetadata.Info info = ScriptMetadata.getInfo(scriptName);
			return info.sampleChar;
		} else {
			return SPECIAL_SAMPLES[reorderCode - FIRST];
		}
	}
}
