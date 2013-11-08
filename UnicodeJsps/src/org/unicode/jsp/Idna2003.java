package org.unicode.jsp;

public class Idna2003 extends Idna {
  
  private Idna2003(boolean STD3) {
    StringPrepData.getIdna2003Tables(mappings, types, STD3);
    mappings_display = mappings;
    validSet = validSet_transitional = types.getSet(IdnaType.valid).freeze();
  }
  
  public static Idna2003 SINGLETON = new Idna2003(true);
  public static Idna2003 SINGLETON_NSTD3 = new Idna2003(false);
  
  static public IdnaType getIDNA2003Type(int cp) {
    return SINGLETON.getType(cp);
  }
  
  public static String toIdna2003(String s) {
    return SINGLETON.transform(s);
  }
  
//  public static UnicodeSet VALID_ASCII = new UnicodeSet("[\\u002Da-zA-Z0-9]").freeze();
//  public static UnicodeSet U32 = new UnicodeSet("[:age=3.2:]").freeze();
//  
//  static UnicodeMap<IdnaType> cache = new UnicodeMap<IdnaType>();
//  static {
//    cache.putAll(UnicodeUtilities.OFF_LIMITS, IdnaType.disallowed);
//    cache.put('-', Idna.IdnaType.valid);
//  }
//
//  static public IdnaType getIDNA2003Type(int cp) {
//    IdnaType result = cache.get(cp);
//    if (result == null) {
//      result = getIDNA2003Type2(cp);
//      cache.put(cp, result);
//    }
//    return result;
//  }
//
//  static private IdnaType getIDNA2003Type2(int cp) {
//    if (cp == '\u3002') return IdnaType.mapped;
////    if (UnicodeUtilities.OFF_LIMITS.contains(cp)) {
////      return IdnaType.disallowed;
////    }
////    if (cp == '-') {
////      return UnicodeUtilities.OUTPUT;
////    }
//    inbuffer.setLength(0);
//    UTF16.append(Idna2003.inbuffer, cp);
//    try {
//      convertWithHack();
//      // DEFAULT
//      if (Idna2003.intermediate.length() == 0) {
//        return Idna.IdnaType.ignored;
//      }
//      Idna2003.outbuffer = IDNA.convertToUnicode(Idna2003.intermediate, IDNA.USE_STD3_RULES);
//    } catch (StringPrepParseException e) {
//      if (e.getMessage().startsWith("Found zero length")) {
//        return Idna.IdnaType.ignored;
//      }
//      return Idna.IdnaType.disallowed;
//    } catch (Exception e) {
//      System.out.println("Failure at: " + Integer.toString(cp, 16));
//      return Idna.IdnaType.disallowed;
//    }
//    if (!UnicodeUtilities.equals(Idna2003.inbuffer, Idna2003.outbuffer)) {
//      return Idna.IdnaType.mapped;
//    }
//    return Idna.IdnaType.valid;
//  }
//
//  private static void convertWithHack() throws StringPrepParseException {
//    try {
//      Idna2003.intermediate = IDNA.convertToASCII(Idna2003.inbuffer, IDNA.USE_STD3_RULES); // USE_STD3_RULES,
//    } catch (StringPrepParseException e) {
//      if (!e.getMessage().contains("BIDI")) {
//        throw e;
//      }
//      inbuffer.append("\\u05D9");
//      Idna2003.intermediate = IDNA.convertToASCII(Idna2003.inbuffer, IDNA.USE_STD3_RULES); // USE_STD3_RULES,
//    }
//  }
//
//  //  static public String getIDNAValue(int cp) {
//  //    if (cp == '-') {
//  //      return "-";
//  //    }
//  //    UnicodeUtilities.inbuffer.setLength(0);
//  //    UTF16.append(UnicodeUtilities.inbuffer, cp);
//  //    try {
//  //      UnicodeUtilities.intermediate = IDNA.convertToASCII(UnicodeUtilities.inbuffer, IDNA.USE_STD3_RULES); // USE_STD3_RULES,
//  //      // DEFAULT
//  //      if (UnicodeUtilities.intermediate.length() == 0) {
//  //        return "";
//  //      }
//  //      UnicodeUtilities.outbuffer = IDNA.convertToUnicode(UnicodeUtilities.intermediate, IDNA.USE_STD3_RULES);
//  //    } catch (StringPrepParseException e) {
//  //      if (e.getMessage().startsWith("Found zero length")) {
//  //        return "";
//  //      }
//  //      return null;
//  //    } catch (Exception e) {
//  //      System.out.println("Failure at: " + Integer.toString(cp, 16));
//  //      return null;
//  //    }
//  //    return UnicodeUtilities.outbuffer.toString();
//  //  }
//
//  static Row.R2<String, String> getIdna2033(String input) {
//    String normInput = Normalizer.normalize(input, Normalizer.NFKC);
//    StringBuffer idna2003 = new StringBuffer();
//    StringBuffer idna2003back = new StringBuffer();
//
//    for (String part : UnicodeUtilities.DOT.split(normInput)) {
//      if (idna2003.length() != 0) {
//        idna2003.append('.');
//        idna2003back.append('.');
//      }
//      try {
//        Idna2003.inbuffer.setLength(0);
//        Idna2003.inbuffer.append(part);
//        convertWithHack();
//        if (Idna2003.intermediate.length() == 0) {
//          throw new IllegalArgumentException();
//        }
//        idna2003.append(UnicodeUtilities.toHTML.transform(Idna2003.intermediate.toString()));
//        idna2003back.append(IDNA.convertToUnicode(Idna2003.intermediate, IDNA.USE_STD3_RULES).toString());
//      } catch (Exception e) {
//        idna2003.append('\uFFFD');
//        idna2003back.append('\uFFFD');
//      }
//    }
//    return Row.of(idna2003.toString(), idna2003back.toString());
//  }
//
//  private IdnaType getIDNA2003Type2(String source) {
//    String idna2003String = Idna2003.toIdna2003(source);
//    if (idna2003String.equals("\uFFFF")) return Idna.IdnaType.disallowed;
//    if (idna2003String.equals(source)) return Idna.IdnaType.valid;
//    if (idna2003String.length() == 0) return Idna.IdnaType.ignored;
//    return Idna.IdnaType.mapped;
//  }
//
//  static String toIdna2003(String s) {
//    String idna2003 = "\uFFFF";
//    try {
//      idna2003 = Idna2003.nameprep.prepare(s, StringPrep.DEFAULT);
//    } catch (StringPrepParseException e) {
//      try {
//        idna2003 = Idna2003.nameprep.prepare(s + "\u05D9", StringPrep.DEFAULT);
//        idna2003 = idna2003.substring(0, idna2003.length()-1);
//      } catch (StringPrepParseException e1) {
//      }
//    }
//    if (Idna2003.BADASCII.containsSome(idna2003)) return "\uFFFF";
//    return idna2003;
//  }
//
//  /**
//   * 
//   */
//
//  static StringPrep nameprep = StringPrep.getInstance(StringPrep.RFC3491_NAMEPREP);
//  static UnicodeSet BADASCII = new UnicodeSet("[[\\u0000-\\u007F]-[\\-0-9A-Za-z]]").freeze();
//  static StringBuffer inbuffer = new StringBuffer();
//  static StringBuffer intermediate;
//  static StringBuffer outbuffer;
//  

}
