package org.unicode.draft;
import java.util.Random;

import org.unicode.cldr.util.Counter;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.text.UnicodeSet;


public class TestRandomIDNA {
	static final boolean SHOW = false;
	static final int interval = 100000;
	static final int MAX_LENGTH = 4;
	static NumberFormat percent = NumberFormat.getPercentInstance();
	static NumberFormat number = NumberFormat.getNumberInstance();
	static {
		percent.setMaximumFractionDigits(3);
		percent.setMinimumFractionDigits(3);
		percent.setMinimumIntegerDigits(2);
	}

	public static void main(String[] args) throws StringPrepParseException {
		showPunycode("가나다댯라럈마먔ﾲ뱟사샷악얐ﾸ쟛차챴카컀");
		showPunycode("aא");
		//"aא"
		checkString("mrk-qla", true);
		final IncrementalString incrementalString = new IncrementalString();
		final Counter<PunyType>[] data = new Counter[21];
		for (int i = 0; i < data.length; ++i) {
			data[i] = new Counter<PunyType>(true);
		}
		final int total = 0;
		for (int i = 0; ; ++i) {
			final String testString = incrementalString.next();
			final int len = testString.length();
			if (len > MAX_LENGTH) {
				break;
			}
			data[len].increment(checkString(testString, SHOW && (i%interval)==0));
		}
		final RandomString randomString = new RandomString(MAX_LENGTH, 20);
		for (int i = 0; i < 1000000; ++i) {
			final String testString = randomString.next();
			final int len = testString.length();
			data[len].increment(checkString(testString, SHOW && (i%interval)==0));
		}
		for (int i = 0; i < data.length; ++i) {
			if (data[i].size() > 0) {
				showCounter(i, data[i]);
			}
		}
	}

	private static void showPunycode(String input) throws StringPrepParseException {
		final StringBuffer encodeOut = OldPunycode.encode(new StringBuffer(input), null);
		System.out.println(input + " => " + encodeOut + ", " + encodeOut.length());
	}

	private static void showCounter(int len, Counter<PunyType> data) {
		System.out.println("length: " + len);
		for (final PunyType key : data.getKeysetSortedByCount(false)) {
			System.out.println("\t" + percent.format(((double)data.getCount(key))/data.getTotal()) + "\t:\t" + key + "\t(" + number.format(data.getCount(key)) + ")");
		}
	}

	enum PunyType {illegal_punycode, non_LMN, all_ascii, unassigned, non_folded, otherwise_ok, null_punydecode, no_roundtrip, illegal_char_found, invalid_char_found};
	static final UnicodeSet ASCII = new UnicodeSet("[:ASCII:]");
	static final UnicodeSet LMN = new UnicodeSet("[[:L:][:M:][:N:]\\-]");
	static final UnicodeSet UNASSIGNED = new UnicodeSet("[:cn:]");

	private static PunyType checkString(final String testString, boolean show) {
		StringBuffer decodeOut = null;
		StringBuffer encodeOut = null;
		try {
			decodeOut = OldPunycode.decode(new StringBuffer(testString), null);
			if (decodeOut == null) {
				return PunyType.null_punydecode;
			}
			final String decodedString = decodeOut.toString();
			encodeOut = OldPunycode.encode(new StringBuffer(decodeOut), null);
			final String encodedString = encodeOut.toString();
			if (!encodedString.equals(testString)) {
				return PunyType.no_roundtrip;
			}
			if (show) {
				System.out.println("OK\t" + testString + "\t=>\t" + decodeOut + ";" + Utility.hex(decodeOut) + "\t=>\t" + encodedString);
			}
			if (UNASSIGNED.containsSome(decodedString)) {
				return PunyType.unassigned;
			}
			if (!LMN.containsAll(decodedString)) {
				return PunyType.non_LMN;
			}
			if (ASCII.containsAll(decodedString)) {
				return PunyType.all_ascii;
			}
			final String cased0 = UCharacter.foldCase(decodedString, true);
			final String folded = Normalizer.normalize(cased0, Normalizer.NFKC, 0);
			final String cased = UCharacter.foldCase(folded, true);
			final String folded2 = Normalizer.normalize(cased, Normalizer.NFKC, 0);
			if (!decodedString.equals(folded2)) {
				return PunyType.non_folded;
			}
			return PunyType.otherwise_ok;
		} catch (final StringPrepParseException e) {
			if (show) {
				System.out.println("BAD\t" + testString + "\t=>\t" + decodeOut + ";" + Utility.hex(decodeOut == null ? "" : decodeOut.toString()) + "\t=>\t" + e.getMessage());
			}
			PunyType result = PunyType.valueOf(PrepErrorName[e.getError()].toLowerCase());
			if (result == PunyType.invalid_char_found || result == PunyType.illegal_char_found) {
				result = PunyType.illegal_punycode;
			}
			return result;
		}
	}

	public static String[] PrepErrorName = {
		"INVALID_CHAR_FOUND",
		"ILLEGAL_CHAR_FOUND",
		"PROHIBITED_ERROR",
		"UNASSIGNED_ERROR",
		"CHECK_BIDI_ERROR",
		"STD3_ASCII_RULES_ERROR",
		"ACE_PREFIX_ERROR",
		"VERIFICATION_ERROR",
		"LABEL_TOO_LONG_ERROR",
		"BUFFER_OVERFLOW_ERROR",
		"ZERO_LENGTH_LABEL",
		"DOMAIN_NAME_TOO_LONG_ERROR"
	};

	public static class RandomString {
		Random random = new Random(0);
		StringBuffer result = new StringBuffer();
		private final int minLen;
		private final int maxLen;
		RandomString(int minLen, int maxLen) {
			this.minLen = minLen;
			this.maxLen = maxLen - minLen;
		}
		String next() {
			result.setLength(0);
			//result.append("xn-");
			final int len = minLen + random.nextInt(maxLen);
			for (int i = 0; i <= len; ++i) {
				final int index = random.nextInt(37);
				if (index < 26) {
					result.append((char)('a'+index));
				} else if (index < 27) {
					result.append('-');
				} else {
					result.append((char)('0' + index-27));
				}
			}
			return result.toString();
		}
	}

	public static class IncrementalString {
		long counter;
		StringBuffer result = new StringBuffer();
		String next() {
			result.setLength(0);
			long current = counter;
			counter++;
			while (current != 0) {
				if (current <= 38) {
					--current;
				}
				final long index = current % 37;
				current = current / 37;
				if (index < 26) {
					result.append((char)('a'+index));
				} else if (index < 27) {
					result.append('-');
				} else {
					result.append((char)('0' + index-27));
				}
			}
			return result.toString();
		}
	}
}
