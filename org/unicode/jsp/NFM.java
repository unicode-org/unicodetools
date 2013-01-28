package org.unicode.jsp;

import java.util.regex.Pattern;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;

public class NFM {
	public static final UnicodeMap<String> nfm = new UnicodeMap<String>();

	static {
		new MySemiFileReader().process(NFM.class, "nfm.txt");
		nfm.freeze();
	}
	static final class MySemiFileReader extends FileUtilities.SemiFileReader {
		Pattern spaces = Pattern.compile("\\s+");
		@Override
		protected boolean handleLine(int start, int end, String[] items) {
			String results;
			switch (items.length) {
			default:
				throw new IllegalArgumentException();
			case 2:
				results = Utility.fromHex(items[1], 1, spaces);
				nfm.putAll(start, end, results);
				break;
			case 1:
				nfm.putAll(start, end, "");
				break;
			}
			return true;
		}
	}
}
