package org.unicode.text.tools;

import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.Counter;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.TestData;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Types;

import com.ibm.icu.text.UnicodeSet;

public class ShowUnicodeGrowth {
	private static final int LATEST = UCD_Types.AGE_VERSIONS.length - 1;

	enum Type {format, whitespace, number, punctuation, symbol, mark, hangul, han, other_letter, surrogate, private_use, noncharacter, unassigned};
	static UCD ucd = UCD.make("");

	public static void main(String[] args) {
		TestData.countChars();
		foo();
	}

	static void foo() {
		final int[][] data = new int[Type.values().length][UCD_Types.AGE_VERSIONS.length];
		final Set<String> ages = new TreeSet();
		for (int age = LATEST; age > 0; --age) {
			data[Type.unassigned.ordinal()][age] = 0x110000;
		}
		for (int cp = 0; cp <= 0x10FFFF; ++cp) {
			final Type type = getType(cp);
			for (int age = LATEST; age > 0; --age) {
				final UCD ucd = UCD.make(UCD_Types.AGE_VERSIONS[age]);
				if (ucd.isAllocated(cp)) {
				} else {
					break;
				}
				data[type.ordinal()][age]++;
				data[Type.unassigned.ordinal()][age]--;
			}
		}
		for (int age = 1; age < UCD_Types.AGE_VERSIONS.length; ++age) {
			System.out.print("\t" + UCD_Types.AGE_VERSIONS[age]);
		}
		System.out.println();
		for (final Type type : Type.values()) {
			System.out.print(type);
			for (int age = 1; age < UCD_Types.AGE_VERSIONS.length; ++age) {
				System.out.print("\t" + data[type.ordinal()][age]);
			}
			System.out.println();
		}
		final Counter<String> counter = new Counter();
		final ToolUnicodePropertySource toolSource52 = ToolUnicodePropertySource.make("5.2.0");
		final ToolUnicodePropertySource toolSource51 = ToolUnicodePropertySource.make("5.1.0");
		final UnicodeSet current = new UnicodeSet(toolSource51.getSet("gc=cn")).removeAll(toolSource52.getSet("gc=cn"));

		for (final String s : current) {
			final int cp = s.codePointAt(0);
			final String script = Default.ucd().getScriptID(cp);
			if (script.equals("COMMON") || script.equals("INHERITED")) {
				final String category = Default.ucd().getCategoryID(cp);
				counter.add(""+category.charAt(0) + "*", 1);
				continue;
			}
			counter.add(script, 1);
		}
		System.out.println("***New Characters");
		for (final String key : counter) {
			String status = "";
			if (!key.contains("*")) {
				final UnicodeSet old = toolSource51.getSet("script=" + key);
				if (old.size() == 0) {
					status = "\tNEW";
				}
			}
			System.out.println(key + "\t" + counter.get(key) + status);
		}
	}

	private static Type getType(int i) {
		if (ucd.isNoncharacter(i)) {
			return Type.noncharacter;
		} else {
			switch (ucd.getCategory(i)) {
			case UCD_Types.UNASSIGNED:
			case UCD_Types.UPPERCASE_LETTER:
			case UCD_Types.LOWERCASE_LETTER:
			case UCD_Types.TITLECASE_LETTER:
			case UCD_Types.MODIFIER_LETTER:
			case UCD_Types.OTHER_LETTER:
				final byte script = ucd.getScript(i);
				if (script == UCD_Types.HANGUL_SCRIPT) {
					return  Type.hangul;
				} else if (script == UCD_Types.HAN_SCRIPT) {
					return  Type.han;
				} else {
					return  Type.other_letter;
				}

			case UCD_Types.NON_SPACING_MARK:
			case UCD_Types.ENCLOSING_MARK:
			case UCD_Types.COMBINING_SPACING_MARK:
				return  Type.mark;
			case UCD_Types.DECIMAL_DIGIT_NUMBER:
			case UCD_Types.LETTER_NUMBER:
			case UCD_Types.OTHER_NUMBER:
				return  Type.number;
			case UCD_Types.SPACE_SEPARATOR:
			case UCD_Types.LINE_SEPARATOR:
			case UCD_Types.PARAGRAPH_SEPARATOR:
				return  Type.whitespace;
			case UCD_Types.CONTROL:
			case UCD_Types.FORMAT:
				return  Type.format;
			case UCD_Types.UNUSED_CATEGORY:
			case UCD_Types.PRIVATE_USE:
				return  Type.private_use;
			case UCD_Types.SURROGATE:
				return  Type.surrogate;
			case UCD_Types.DASH_PUNCTUATION:
			case UCD_Types.START_PUNCTUATION:
			case UCD_Types.END_PUNCTUATION:
			case UCD_Types.CONNECTOR_PUNCTUATION:
			case UCD_Types.OTHER_PUNCTUATION:
			case UCD_Types.INITIAL_PUNCTUATION:
			case UCD_Types.FINAL_PUNCTUATION:
				return  Type.punctuation;
			case UCD_Types.MATH_SYMBOL:
			case UCD_Types.CURRENCY_SYMBOL:
			case UCD_Types.MODIFIER_SYMBOL:
			case UCD_Types.OTHER_SYMBOL:
				return  Type.symbol;
			}
		}
		return null;
	}
}
