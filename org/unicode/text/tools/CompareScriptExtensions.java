package org.unicode.text.tools;

import java.util.TreeSet;

import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.lang.UScript;

public class CompareScriptExtensions {
	static final ToolUnicodePropertySource tups = ToolUnicodePropertySource.make(Default.ucdVersion());
	static final UnicodeProperty scriptProp = tups.getProperty("sc");
	static final UnicodeProperty scriptXProp = tups.getProperty("scx");

	public static void main(String[] args) {
		UnicodeMap<String> diffs = new UnicodeMap();

		for (int i = 0; i <= 0x10FFFF; ++i) {
			String spx = scriptXProp.getValue(i);
			if (spx == null) {
				continue;
			}
			String sp = scriptProp.getValue(i);
			if (!sp.equals(spx)) {
				diffs.put(i, sp + "\t" + getLongForm(spx));
			}
		}
		TreeSet<String> s = new TreeSet<String>(diffs.getAvailableValues());
		for (String value : s) {
			System.out.println(value + "\t" + diffs.getSet(value).toPattern(false));
		}
	}

	private static String getLongForm(String spx) {
		String[] items = spx.split(" ");
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < items.length; ++i) {
			if (result.length() != 0) {
				result.append(" ");
			}
			result.append(UScript.getName(UScript.getCodeFromName(items[i])));
		}
		return result.toString();
	}
}
