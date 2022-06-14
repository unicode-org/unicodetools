package org.unicode.jsp;

/** */
public class BranchStringPrepData {
    //
    //  private static final boolean DEBUG = true;
    //  public static UnicodeSet U32 = new UnicodeSet("[:age=3.2:]").freeze();
    //  public static UnicodeSet VALID_ASCII = new UnicodeSet("[\\u002Da-zA-Z0-9]").freeze();
    //
    //
    //  /**
    // 3. Mapping
    //   This profile specifies mapping using the following tables from
    //   [STRINGPREP]:
    //   Table B.1
    //   Table B.2
    // 4. Normalization
    //   This profile specifies using Unicode normalization form KC, as
    //   described in [STRINGPREP].
    // 5. Prohibited Output
    //   Table C.1.2
    //   Table C.2.2
    //   Table C.3
    //   Table C.4
    //   Table C.5
    //   Table C.6
    //   Table C.7
    //   Table C.8
    //   Table C.9
    //   */
    //
    //  public static void getIdna2003Tables(UnicodeMap<String> mappings, UnicodeMap<IdnaType>
    // types) {
    //    EnumSet<Idna2003Table> allowed = EnumSet.of(
    //            Idna2003Table.B_1,
    //            Idna2003Table.B_2,
    //            Idna2003Table.C_1_2
    //            , Idna2003Table.C_2_2
    //            , Idna2003Table.C_3
    //            , Idna2003Table.C_4
    //            , Idna2003Table.C_5
    //            , Idna2003Table.C_6
    //            , Idna2003Table.C_7
    //            , Idna2003Table.C_8
    //            , Idna2003Table.C_9
    //    );
    //    for (int i = 0; i <= 0x10FFFF; ++i) {
    //      String mapping = getMapping(i, allowed);
    //      boolean isProhibited = mapping == null ? isProhibited(i, allowed) :
    // isProhibited(mapping,allowed);
    //      IdnaType status;
    //      if (isProhibited || !U32.contains(i)) {
    //        status = IdnaType.disallowed;
    //        mapping = null;
    //      } else if (mapping == null) {
    //        status = IdnaType.valid;
    //      } else if (mapping.length() == 0) {
    //        status = IdnaType.ignored;
    //      } else {
    //        status = IdnaType.mapped;
    //      }
    //      mappings.put(i, mapping);
    //      types.put(i, status);
    //    }
    //    // special handling for separators
    //    mappings.putAll(IdnaTypes.OTHER_DOT_SET,".");
    //    types.putAll(IdnaTypes.OTHER_DOT_SET,IdnaType.mapped);
    //    types.put('.',IdnaType.valid);
    //
    //    mappings.freeze();
    //    types.freeze();
    //  }
    //
    //  private static String getMapping(int cp, EnumSet<Idna2003Table> allowed) {
    //    DataSet items = data.get(cp);
    //    String mapping = items == null ? null : items.mapping;
    //    String normalizedMapping = mapping != null ? Normalizer.normalize(mapping,
    // Normalizer.NFKC) : Normalizer.normalize(cp, Normalizer.NFKC);
    //    if (UnicodeProperty.equals(cp, normalizedMapping)) {
    //      return null;
    //    }
    //    return normalizedMapping;
    //  }
    //
    //  private static boolean isProhibited(int cp, EnumSet<Idna2003Table> allowed) {
    //    DataSet items = data.get(cp);
    //    if (items != null) {
    //      return items.isProhibited;
    //    }
    //    return false;
    //  }
    //
    //  private static boolean isProhibited(String string, EnumSet<Idna2003Table> allowed) {
    //    int cp;
    //    for (int i = 0; i < string.length(); i += Character.charCount(cp)) {
    //      cp = string.codePointAt(i);
    //      if (isProhibited(cp, allowed)) {
    //        return true;
    //      }
    //    }
    //    return false;
    //  }
    //
    //  enum Idna2003Table {none, A_1, B_1, B_2, B_3, C_1_1, C_1_2, C_2_1, C_2_2, C_3, C_4, C_5,
    // C_6, C_7, C_8, C_9, D_1, D_2}
    //
    //  static EnumSet<Idna2003Table> PROHIBITED = EnumSet.range(Idna2003Table.C_1_1,
    // Idna2003Table.C_9);
    //  static EnumSet<Idna2003Table> MAPPING = EnumSet.range(Idna2003Table.B_1, Idna2003Table.B_3);
    //
    //
    //  /**
    //  A.1 Unassigned code points in Unicode 3.2
    //     ----- Start Table A.1 -----
    //     0221
    //  B.1 Commonly mapped to nothing
    //     ----- Start Table B.1 -----
    //     00AD; ; Map to nothing
    //  B.2 Mapping for case-folding used with NFKC
    //     ----- Start Table B.2 -----
    //     0041; 0061; Case map
    //  B.3 Mapping for case-folding used with no normalization
    //     ----- Start Table B.3 -----
    //     0041; 0061; Case map
    //  C.1.1 ASCII space characters
    //     ----- Start Table C.1.1 -----
    //     0020; SPACE
    //  C.1.2 Non-ASCII space characters
    //     ----- Start Table C.1.2 -----
    //     00A0; NO-BREAK SPACE
    //  C.2.1 ASCII control characters
    //     ----- Start Table C.2.1 -----
    //     0000-001F; [CONTROL CHARACTERS]
    //  C.2.2 Non-ASCII control characters
    //     ----- Start Table C.2.2 -----
    //     0080-009F; [CONTROL CHARACTERS]
    //  C.2.2 Non-ASCII control characters
    //     ----- Start Table C.2.2 -----
    //     0080-009F; [CONTROL CHARACTERS]
    //  C.3 Private use
    //     ----- Start Table C.3 -----
    //     E000-F8FF; [PRIVATE USE, PLANE 0]
    //  C.4 Non-character code points
    //     ----- Start Table C.4 -----
    //     FDD0-FDEF; [NONCHARACTER CODE POINTS]
    //  C.5 Surrogate codes
    //     ----- Start Table C.5 -----
    //     D800-DFFF; [SURROGATE CODES]
    //  C.6 Inappropriate for plain text
    //     ----- Start Table C.6 -----
    //     FFF9; INTERLINEAR ANNOTATION ANCHOR
    //  C.7 Inappropriate for canonical representation
    //     ----- Start Table C.7 -----
    //     2FF0-2FFB; [IDEOGRAPHIC DESCRIPTION CHARACTERS]
    //  C.8 Change display properties or are deprecated
    //     ----- Start Table C.8 -----
    //     0340; COMBINING GRAVE TONE MARK
    //  C.9 Tagging characters
    //     ----- Start Table C.9 -----
    //     E0001; LANGUAGE TAG
    //  D.1 Characters with bidirectional property "R" or "AL"
    //     ----- Start Table D.1 -----
    //     05BE
    //  D.2 Characters with bidirectional property "L"
    //     ----- Start Table D.2 -----
    //     0041-005A
    //   */
    //
    //  static Pattern TABLE_DELIMITER =
    // Pattern.compile("\\Q-----\\E\\s*(Start|End)\\s*Table\\s*(\\S+)\\s*\\Q-----\\E");
    //  static Pattern MAP_LINE = Pattern.compile("([A-Z0-9]{4,6})" +
    //          "(?:-([A-Z0-9]{4,6}))?" +
    //          "(?:\\s*;\\s*((?:[A-Z0-9]{4,6}\\s*)*))?" +
    //  "(?:\\s*;\\s*.*)?");
    //  static Pattern SET_LINE = Pattern.compile("([A-Z0-9]{4,6})" +
    //          "(?:-([A-Z0-9]{4,6}))?" +
    //  "(?:\\s*;\\s*.*)?");
    //
    //  static class DataSet {
    //    final boolean isProhibited;
    //    final String mapping;
    //    final String comment;
    //
    //    private DataSet(boolean isProhibited2, String mapping2, String comment2) {
    //      isProhibited = isProhibited2;
    //      mapping = mapping2;
    //      comment = comment2;
    //    }
    //
    //    public DataSet add(boolean myisProhibited, String mymapping, String mycomment) {
    //      // now merge
    //      if (isProhibited) {
    //        myisProhibited = true;
    //      }
    //      if (mymapping == null) {
    //        mymapping = mapping;
    //      } else if (mapping != null && !mymapping.equals(mapping)) {
    //        throw new IllegalArgumentException("Conflicting mapping " + Utility.hex(mapping) + ",
    // " + Utility.hex(mymapping));
    //      }
    //      if (mycomment == null) {
    //        mycomment = comment;
    //      } else if (comment != null) {
    //        mycomment = comment + "\n" + mycomment;
    //      }
    //      return new DataSet(myisProhibited, mymapping, mycomment);
    //    }
    //    /**
    //     * If there is a mapping, use the mapping to set the prohibited bit.
    //     * @param codepoint
    //     * @param data
    //     * @return stuff
    //     */
    //    public DataSet fix(int codepoint, UnicodeMap<DataSet> data) {
    //      if (mapping != null) {
    //        boolean newIsProhibited = false;
    //        int cp;
    //        for (int i = 0; i < mapping.length(); i += Character.charCount(cp)) {
    //          cp = mapping.codePointAt(i);
    //          DataSet other = data.get(i);
    //          if (other.mapping != null) {
    //            throw new IllegalArgumentException("Recursive Mapping");
    //          }
    //          if (other.isProhibited) {
    //            newIsProhibited = true;
    //          }
    //        }
    //        DataSet newDataSet = new DataSet(newIsProhibited, mapping, comment);
    //        if (DEBUG) System.out.println("Changing value for " + Utility.hex(codepoint) + ":\t["
    // + this + "] => [" + newDataSet + "]");
    //        return newDataSet;
    //      }
    //      return null;
    //    }
    //
    //    public boolean equals(Object other) {
    //      DataSet that = (DataSet) other;
    //      return isProhibited == that.isProhibited
    //      && UnicodeProperty.equals(mapping, that.mapping)
    //      && UnicodeProperty.equals(comment, that.comment);
    //    }
    //    public int hashCode() {
    //      return (isProhibited ? 1 : 0) ^ (mapping == null ? 0 : mapping.hashCode());
    //    }
    //    public String toString() {
    //      return isProhibited + ", " + Utility.hex(mapping) + ", " + comment;
    //    }
    //  }
    //
    //
    //  private static final UnicodeMap<DataSet> data;
    //
    //  static {
    //    data = new UnicodeMap<DataSet>();
    //    try {
    //      //UnicodeMap<Row.R3<StringPrepData.Idna2003Table, String, String>> rawMapping = new
    // UnicodeMap<Row.R3<StringPrepData.Idna2003Table, String, String>>();
    //
    //      Matcher tableDelimiter = TABLE_DELIMITER.matcher("");
    //      Matcher mapLine = MAP_LINE.matcher("");
    //      Matcher setLine = SET_LINE.matcher("");
    //      BufferedReader in = FileUtilities.openFile(StringPrepData.class, "nameprep.txt");
    //      //BufferedReader in = BagFormatter.openUTF8Reader(UCD_Types.BASE_DIR + "idna/",
    // "nameprep.txt");
    //      StringPrepData.Idna2003Table table = null;
    //      boolean inTable = false;
    //      boolean isMapping = false;
    //      for (int count = 1; ; ++count) {
    //        String line = in.readLine();
    //        if (line == null) break;
    //        line = line.trim();
    //        if (line.length() == 0 || line.startsWith("Hoffman") || line.startsWith("RFC"))
    // continue;
    //        if (line.startsWith("-----")) {
    //          if (!tableDelimiter.reset(line).matches()) {
    //            throw new IllegalArgumentException("Bad syntax: " + line);
    //          }
    //          inTable = tableDelimiter.group(1).equals("Start");
    //          StringPrepData.Idna2003Table newTable =
    // Idna2003Table.valueOf(tableDelimiter.group(2).replace(".","_"));
    //          if (inTable) {
    //            if (table != null) {
    //              throw new IllegalArgumentException("Table not terminated: " + table + "; " +
    // line);
    //            }
    //            table = newTable;
    //            if (DEBUG) System.out.println(count + ")\t*** New Table: " + table);
    //            isMapping = newTable.toString().startsWith("B");
    //          } else {
    //            if (newTable != table) {
    //              throw new IllegalArgumentException("Bad table end: " + newTable + " != " +
    // table + "; " + line);
    //            }
    //            table = null;
    //            isMapping = false;
    //          }
    //          continue;
    //        }
    //        if (!inTable) {
    //          if (DEBUG) System.out.println(count + ")\tIgnoring: " + line);
    //          continue;
    //        }
    //        //        if (!allowed.contains(table)) {
    //        //          if (DEBUG) System.out.println(count + ")\t" + table + "\tSKIPPING line:\t"
    // + line);
    //        //          continue;
    //        //        } else {
    //        //          if (DEBUG) System.out.println(count + ")\t" + table + "\tDoing line:\t" +
    // line);
    //        //        }
    //        Matcher lineMatcher = isMapping ? mapLine : setLine;
    //        if (!lineMatcher.reset(line).matches()) {
    //          throw new IllegalArgumentException("Illegal range-value syntax: " + line);
    //        }
    //        int startCode = Utility.fromHex(lineMatcher.group(1),4," ").codePointAt(0);
    //        String endCodeString = lineMatcher.groupCount() < 2 ? null : lineMatcher.group(2);
    //        String group3 = lineMatcher.groupCount() < 3 ? null : lineMatcher.group(3);
    //        String group4 = lineMatcher.groupCount() < 4 ? null : lineMatcher.group(4);
    //        int endCode = endCodeString == null ? startCode : Utility.fromHex(endCodeString,4,"
    // ").codePointAt(0);
    //        String comment, mapValueString;
    //        if (isMapping) {
    //          comment = group4;
    //          try {
    //            mapValueString = group3.length() == 0 ? "" : Utility.fromHex(group3,4," ");
    //          } catch (RuntimeException e) {
    //            throw e;
    //          }
    //        } else {
    //          comment = group3;
    //          mapValueString = null;
    //        }
    //        if (DEBUG) System.out.println(count + ")\t" + line + ":\t" + Utility.hex(startCode)
    //                + (startCode == endCode ? "" : ".." + Utility.hex(endCode))
    //                + ",\t" + table
    //                + ",\t" + (mapValueString == null ? "null" : Utility.hex(mapValueString))
    //        );
    //
    //        addMapping(startCode, endCode, table, (String)mapValueString, (String)comment);
    //      }
    //      in.close();
    //    } catch (IOException e) {
    //      throw new IllegalArgumentException(e);
    //    }
    //
    //    // fix ASCII
    //
    //    addMapping(0, 0x7F, Idna2003Table.C_9, (String)null, (String)null);
    //    for (UnicodeSetIterator it = new UnicodeSetIterator(VALID_ASCII); it.next();) {
    //      addMapping(0, 0x7F, null, null, null);
    //    }
    //
    //    //rawMapping.putAll(VALID_ASCII, null);
    //
    //    for (int i = 'A'; i <= 'Z'; ++i) {
    //      R3<StringPrepData.Idna2003Table, String, String> alphaMap = Row.of(Idna2003Table.B_1,
    // UTF16.valueOf(i-'A'+'a'), (String)null);
    //      DataSet tableSet = data.get(i);
    //      if (tableSet == null) {
    //        tableSet = new DataSet(PROHIBITED.contains(Idna2003Table.B_1),
    // UTF16.valueOf(i-'A'+'a'), (String)null);
    //      } else {
    //        tableSet = tableSet.add(PROHIBITED.contains(Idna2003Table.B_1),
    // UTF16.valueOf(i-'A'+'a'), (String)null);
    //      }
    //      data.put(i, tableSet);
    //    }
    //    for (String i : data.keySet()) {
    //      DataSet dataSet = data.get(i);
    //      DataSet fixed = dataSet.fix(i.codePointAt(0), data);
    //      if (fixed != null) {
    //        data.put(i, fixed);
    //      }
    //    }
    //    data.freeze();
    //  }
    //
    //  private static void addMapping(int startCode, int endCode, StringPrepData.Idna2003Table
    // type, String mapping, String comment) {
    //    for (int i = startCode; i <= endCode; ++i) {
    //      addData(i, type, mapping, comment);
    //    }
    //  }
    //
    //  private static void addData(int i, StringPrepData.Idna2003Table type, String mapping, String
    // comment) {
    //    try {
    //      if (i == 0x200c) {
    //        System.out.print("");
    //      }
    //      DataSet tableSet = data.get(i);
    //      if (tableSet == null) {
    //        tableSet = new DataSet(PROHIBITED.contains(type), mapping, comment);
    //      } else {
    //        tableSet = tableSet.add(PROHIBITED.contains(type), mapping, comment);
    //      }
    //      data.put(i, tableSet);
    //    } catch (RuntimeException e) {
    //      throw new IllegalArgumentException("Failure with " + Utility.hex(i), e);
    //    }
    //  }
}
