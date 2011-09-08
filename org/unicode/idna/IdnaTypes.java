package org.unicode.idna;

import java.util.regex.Pattern;

import com.ibm.icu.text.UnicodeSet;

public class IdnaTypes {

  static UnicodeSet OTHER_DOT_SET = new UnicodeSet("[． 。｡]").freeze();
  static final UnicodeSet ASCII = new UnicodeSet("[:ASCII:]").freeze();
  static final UnicodeSet LABEL_ASCII = new UnicodeSet("[\\-0-9a-zA-Z]").freeze();
  static Pattern DOT = Pattern.compile("[.]");
  static Pattern DOTS = Pattern.compile("[.． 。｡]");
  public static UnicodeSet U32 = new UnicodeSet("[:age=3.2:]").freeze();
  public static UnicodeSet VALID_ASCII = new UnicodeSet("[\\u002Da-zA-Z0-9]").freeze();
  public static UnicodeSet COMBINING_MARK = new UnicodeSet("[:M:]");
}
