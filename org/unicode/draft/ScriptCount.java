package org.unicode.draft;

import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.With;
import org.unicode.text.UCD.UCD_Types;

import sun.text.normalizer.UTF16;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.Normalizer2.Mode;

public class ScriptCount {
	private static final double LOG2 = Math.log(2);
	
    final static Options myOptions = new Options();
    enum MyOptions {
        ranked("(true|false)", "true", "Use ranked frequencies"),
        language(".*", "mul", "Language code (mul for all)."),
        ;
        // boilerplate
        final Option option;
        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }


	public static void main(String[] args) {
		myOptions.parse(MyOptions.ranked, args, true);
		boolean ranked = MyOptions.ranked.option.getValue().equals("true");
		String language = MyOptions.language.option.getValue();

		Counter<Integer> mulCounter = CharacterFrequency.getCodePointCounter(language, ranked);
		//System.out.println(mulCounter.getItemCount());
		Normalizer2 nfkc = Normalizer2.getInstance(null, "nfkc", Mode.COMPOSE);
		Map<String, Counter<Integer>> keyCounter = new TreeMap<String,Counter<Integer>>();
		BitSet bitset = new BitSet();
		for (Integer cp : mulCounter) {
			long count = mulCounter.getCount(cp);
			//            if (count < 10000) break;
			int cat = UCharacter.getType(cp);
			String key = null;
			if (cp > 0xFFFF) {
				addCount(keyCounter, cp, count, "!BMP\tSupplementary");
			}
			if (isLetter(cat)) {
				cp = UCharacter.toLowerCase(cp);
				String norm = nfkc.getDecomposition(cp);
				if (norm == null) {
					addScript(keyCounter, cp, bitset, count);
				} else {
					for (int cp2 : With.codePointArray(norm)) {
						int cat2 = UCharacter.getType(cp2);
						if (isLetter(cat2)) {
							addScript(keyCounter, cp2, bitset, count);
						}
					}
				}
			} else if (UCharacter.isWhitespace(cp) || cat == UCharacter.SPACE_SEPARATOR) {
				key = "*WS\tWhitespace";
				addCount(keyCounter, cp, count, key);
			} else {
				key = "*" + UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, cat, NameChoice.SHORT)
						+ "\t" + UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, cat, NameChoice.LONG);
				addCount(keyCounter, cp, count, key);
			}
		}
		for (Entry<String, Counter<Integer>> entry : keyCounter.entrySet()) {
			String key = entry.getKey();
			Counter<Integer> counter = entry.getValue();
			System.out.println(key + "\t" + Math.log(counter.getTotal()) + "\t" + counter.getItemCount() + "\t" + getTop(32, counter, mulCounter));
		}
		DecimalFormat pf = (DecimalFormat) NumberFormat.getInstance();
		pf.setMaximumFractionDigits(6);
		pf.setMinimumFractionDigits(6);
		//        pf.setMinimumSignificantDigits(3);
		//        pf.setMaximumSignificantDigits(3);
		int counter = 0;
		double max = mulCounter.getTotal();
		PrintWriter out = org.unicode.text.utility.Utility.openPrintWriter(UCD_Types.GEN_DIR + "/frequency-text", 
				"mul.txt", org.unicode.text.utility.Utility.UTF8_WINDOWS);
		for (int ch : mulCounter.getKeysetSortedByCount(false)) {
			long count = mulCounter.get(ch);
			// 0%   忌   U+5FCC  Lo  Hani    CJK UNIFIED IDEOGRAPH-5FCC
			out.println(pf.format(Math.log(count/max)/LOG2) 
					+ "\t" + show(ch) 
					+ "\tU+" + Utility.hex(ch, 4)
					+ "\t" + propValue(ch, UProperty.GENERAL_CATEGORY, UProperty.NameChoice.SHORT)
					+ "\t" + propValue(ch, UProperty.SCRIPT, UProperty.NameChoice.SHORT)
					+ "\t" + UCharacter.getExtendedName(ch));
			//if (count < 10000) break;
		}
		out.close();
	}

	public static boolean isLetter(int cat) {
		return cat == UCharacter.UPPERCASE_LETTER || cat == UCharacter.LOWERCASE_LETTER || cat == UCharacter.MODIFIER_LETTER || cat == UCharacter.TITLECASE_LETTER
				|| cat == UCharacter.OTHER_LETTER || cat == UCharacter.COMBINING_SPACING_MARK;
	}

	public static void addScript(Map<String, Counter<Integer>> keyCounter,
			int cp2, BitSet bitset, long count) {
		UScript.getScriptExtensions(cp2, bitset);
		for (int script = bitset.nextSetBit(0); script >= 0; script = bitset.nextSetBit(script+1)) {
			String key = UScript.getShortName(script) + "\t" + UScript.getName(script);
			addCount(keyCounter, cp2, count, key);
		}
	}

	public static Counter<Integer> addCount(
			Map<String, Counter<Integer>> keyCounter, Integer cp, long count,
			String key) {
		Counter<Integer> counter = keyCounter.get(key);
		if (counter == null) keyCounter.put(key, counter = new Counter<Integer>());
		counter.add(cp, count);
		return counter;
	}

	private static String getTop(int max, Counter<Integer> counter, Counter<Integer> mulCounter) {
		StringBuilder b = new StringBuilder();
		for (int cp : counter.getKeysetSortedByCount(false)) {
			if (--max < 0) {
				break;
			}
			if (b.length() != 0) {
				b.append("\t");
			}
			//b.append('“');
			b.append(show(cp));
			//b.append('”').append("(").append((int)Math.round(100*Math.log(mulCounter.get(0x20)/mulCounter.get(cp)))).append(")");
		}
		return b.toString();
	}

	private static String getExtendedName(String s, String separator) {
		StringBuilder result = new StringBuilder();
		boolean first = true;
		for (int cp : With.codePointArray(s)) {
			if (first) {
				first = false;
			} else {
				result.append(separator);
			}
			result.append(UCharacter.getExtendedName(cp));
		}
		return result.toString();
	}

	private static String propValue(int ch, int propEnum, int nameChoice) {
		return UCharacter.getPropertyValueName(propEnum, UCharacter.getIntPropertyValue(ch, propEnum), nameChoice);
	}

	private static String show(int s) {
		int cat = UCharacter.getType(s);
		if (cat == UCharacter.FORMAT || cat == UCharacter.CONTROL || cat == UCharacter.PRIVATE_USE 
				|| cat == UCharacter.SPACE_SEPARATOR || cat == UCharacter.LINE_SEPARATOR || cat == UCharacter.PARAGRAPH_SEPARATOR) {
			return "U+" + Utility.hex(s);
		}
		if (s == '\'' || s == '"' || s == '=') {
			return "'" + UTF16.valueOf(s);
		}
		return UTF16.valueOf(s);
	}

	private static int getScript(String norm) {
		int cp;
		int result = UScript.INHERITED;
		for (int i = 0; i < norm.length(); i += Character.charCount(cp)) {
			cp = norm.codePointAt(i);
			int script = UScript.getScript(cp);
			if (script == UScript.UNKNOWN) {
				int type = UCharacter.getType(cp);
				if (type == UCharacter.PRIVATE_USE) {
					script = UScript.BLISSYMBOLS;
				}
			}
			if (script == UScript.INHERITED || script == result) continue;
			if (script == UScript.COMMON) {
				if (result == UScript.INHERITED) {
					result = script;
				}
				continue;
			}
			if (result == UScript.COMMON || result == UScript.INHERITED) {
				result = script;
				continue;
			}
			// at this point both are different explicit scripts
			return UScript.COMMON;
		}
		return result;
	}
}
