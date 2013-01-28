package org.unicode.idna;

import java.util.regex.Pattern;

import com.ibm.icu.text.UnicodeSet;

public class IdnaTypes {

	public static final UnicodeSet OTHER_DOT_SET = new UnicodeSet("[． 。｡]").freeze();
	public static final UnicodeSet ASCII = new UnicodeSet("[:ASCII:]").freeze();
	public static final UnicodeSet LABEL_ASCII = new UnicodeSet("[\\-0-9a-zA-Z]").freeze();
	public static final Pattern DOT = Pattern.compile("[.]");
	public static final Pattern DOTS = Pattern.compile("[.． 。｡]");
	public static final UnicodeSet U32 = new UnicodeSet("[:age=3.2:]").freeze();
	public static final UnicodeSet VALID_ASCII = new UnicodeSet("[\\u002Da-zA-Z0-9]").freeze();
	public static final UnicodeSet COMBINING_MARK = new UnicodeSet("[:M:]");
}
