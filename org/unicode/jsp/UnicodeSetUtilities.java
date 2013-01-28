package org.unicode.jsp;

import java.text.ParsePosition;

import com.ibm.icu.dev.util.UnicodePropertySymbolTable;
import com.ibm.icu.text.UnicodeSet;

public class UnicodeSetUtilities {
	private static UnicodeSet OK_AT_END = new UnicodeSet("[ \\]\t]").freeze();

	public static UnicodeSet parseUnicodeSet(String input) {
		input = input.trim() + "]]]]]";
		final String parseInput = "[" + input + "]]]]]";
		final ParsePosition parsePosition = new ParsePosition(0);
		final UnicodeSet result = new UnicodeSet(parseInput, parsePosition, fullSymbolTable);
		int parseEnd = parsePosition.getIndex();
		if (parseEnd != parseInput.length() && !UnicodeSetUtilities.OK_AT_END.containsAll(parseInput.substring(parseEnd))) {
			parseEnd--; // get input offset
			throw new IllegalArgumentException("Additional characters past the end of the set, at "
					+ parseEnd + ", ..."
					+ input.substring(Math.max(0, parseEnd - 10), parseEnd)
					+ "|"
					+ input.substring(parseEnd, Math.min(input.length(), parseEnd + 10))
					);
		}
		return result;
	}


	static UnicodeSet.XSymbolTable fullSymbolTable = new UnicodePropertySymbolTable(XPropertyFactory.make());
}
