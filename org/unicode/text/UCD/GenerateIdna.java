package org.unicode.text.UCD;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class GenerateIdna {
  
  public enum IdnaType {valid, ignored, mapped, deviation, disallowed}
  public static UnicodeSet U32 = new UnicodeSet("[:age=3.2:]").freeze();
  public static UnicodeSet VALID_ASCII = new UnicodeSet("[\\u002Da-zA-Z0-9]").freeze();
  
  public static void main(String[] args) throws IOException {
    UnicodeMap<R2<IdnaType, String>> idna2003MappingTable = getIdna2003Table();

    UnicodeMap<Row.R2<IdnaType, String>> mappingTable = createMappingTable(idna2003MappingTable);
    
    verifyDifferences(idna2003MappingTable, mappingTable);
    
    UnicodeMap<String> stringMappingTable = new UnicodeMap<String>();
    for (int cp = 0; cp <= 0x10FFFF; ++cp) {
      Row.R2<IdnaType, String> value = mappingTable.get(cp);
      if (value == null) {
        throw new IllegalArgumentException("Expected value for " + Utility.hex(cp));
      }
      String status = value.get0().toString();
      String mapping = value.get1();
      if (mapping != null) {
        status += Utility.repeat(" ", 10-status.length()) + " ; " + Utility.hex(value.get1());
      }
      stringMappingTable.put(cp, status);
    }

    writeDataFile(stringMappingTable);
  }

  private static void verifyDifferences(UnicodeMap<R2<IdnaType, String>> idna2003MappingTable, UnicodeMap<Row.R2<IdnaType, String>> mappingTable) {
    System.out.println("Verifying Differences");
    UnicodeMap<Row.R3<String, R2<IdnaType, String>, R2<IdnaType, String>>> diff = new UnicodeMap();
    for (int i = 0; i <= 0x10FFFF; ++i) {
      if (!U32.contains(i)) continue;
      R2<IdnaType, String> data46 = mappingTable.get(i);
      R2<IdnaType, String> data2003 = idna2003MappingTable.get(i);
      if (!equals(data46, data2003)) {
        diff.put(i, Row.of(
                (data2003.get0() == IdnaType.disallowed ? "D" : "-")
                + (data46.get0() == IdnaType.disallowed ? "D" : "-")
                , data2003, data46));
        //System.out.println(Utility.hex(i) + " - ust46: " + data46 + "\t idna2003: " + data2003 + "\t" + UCharacter.getExtendedName(i) + status);
      }
    }
    for (R3<String, R2<IdnaType, String>, R2<IdnaType, String>> item : new TreeSet<R3<String, R2<IdnaType, String>, R2<IdnaType, String>>>(diff.values())) {
      UnicodeSet set = diff.getSet(item);
      String ok = item.get0();
      R2<IdnaType, String> data2003 = item.get1();
      R2<IdnaType, String> data46 = item.get2();
      
      System.out.println(ok + "\tidna2003: " + data2003 + "\tust46: " + data46 + "\t" + set.size() + "\t" + set);
    }
  }
  
  private static boolean equals(Object a, Object b) {
    if (a == null) return b == null;
    return a.equals(b);
  }

  private static UnicodeMap<Row.R2<IdnaType, String>> createMappingTable(UnicodeMap<R2<IdnaType, String>> idna2003MappingTable) {
    ToolUnicodePropertySource properties = ToolUnicodePropertySource.make(Default.ucdVersion());
    ToolUnicodePropertySource propertiesU32 = ToolUnicodePropertySource.make(Default.ucdVersion());

    UnicodeMap<String> nfkc_cfMap = properties.getProperty("NFKC_CF").getUnicodeMap();
    UnicodeMap<String> baseMapping = new UnicodeMap<String>().putAll(nfkc_cfMap);
    baseMapping.put(0xFF0E, "\u002E");
    baseMapping.put(0x3002, "\u002E");
    baseMapping.put(0xFF61, "\u002E");
    
    UnicodeSet labelSeparator = new UnicodeSet("[\\u002E \\uFF0E \\u3002 \\uFF61]");
    
    UnicodeSet baseValidSet = new UnicodeSet(0,0x10FFFF)
    .removeAll(properties.getSet("Changes_When_NFKC_Casefolded=true"))
    .removeAll(properties.getSet("gc=Cc"))
    .removeAll(properties.getSet("gc=Cf"))
    .removeAll(properties.getSet("gc=Cn"))
    .removeAll(properties.getSet("gc=Co"))
    .removeAll(properties.getSet("gc=Cs"))
    .removeAll(properties.getSet("gc=Zl"))
    .removeAll(properties.getSet("gc=Zp"))
    .removeAll(properties.getSet("gc=Zs"))
    .removeAll(properties.getSet("Block=Ideographic_Description_Characters"))
    .removeAll(new UnicodeSet("[\\u0000-\\u007F]"))
    .addAll(VALID_ASCII)
    ;
    
    UnicodeSet baseExclusionSet = new UnicodeSet("[" +
    		"\\u04C0 \\u10A0-\\u10C5 \\u2132 \\u2183" +
    		"\\U0002F868  \\U0002F874 \\U0002F91F \\U0002F95F \\U0002F9BF" +
    		"\u3164 \uFFA0 \u115F \u1160 \u17B4 \u17B5 \u1806 \uFFFC \uFFFD" +
    		"]");
    
    UnicodeSet deviationSet = new UnicodeSet("[\u200C \u200D \u00DF \u03C2]");

    /**
     * 1. If the code point is in the deviation set the status is deviation and
     * the mapping value is the base mapping value for that code point<br>
     * 2. Otherwise, if (a) the code point is in the base exclusion set, or if
     * (b) any code point in its base mapping value is not in the base valid set
     * the status is disallowed and there is no mapping value in the table<br>
     * 3. Otherwise, if the base mapping value is an empty string the status is
     * ignored and there is no mapping value in the table<br>
     * 4. Otherwise, if the base mapping value is the same as the code point the
     * status is valid and there is no mapping value in the table<br>
     * 5. Otherwise, the status is mapping and the mapping value is the base
     * mapping value for that code point
     */
    UnicodeMap<Row.R2<IdnaType, String>> mappingTable = new UnicodeMap<R2<IdnaType, String>>();
    R2<IdnaType, String> disallowedResult = Row.of(IdnaType.disallowed, (String)null);
    R2<IdnaType, String> ignoredResult = Row.of(IdnaType.ignored, (String)null);
    R2<IdnaType, String> validResult = Row.of(IdnaType.valid, (String)null);
    
    for (int cp = 0; cp <= 0x10FFFF; ++cp) {
      String cpString = UTF16.valueOf(cp);
      Row.R2<IdnaType, String> result;
      String baseMappingValue = baseMapping.get(cp);
      if (baseMappingValue == null) {
        baseMappingValue = cpString;
      }
      if (deviationSet.contains(cp)) {
        result = Row.of(IdnaType.deviation, baseMappingValue);
      } else if (baseExclusionSet.contains(cp)) {
        result = disallowedResult;
      } else if (!labelSeparator.contains(cp) && !baseValidSet.containsAll(baseMappingValue)) {
        result = disallowedResult;
      } else if (baseMappingValue.length() == 0) {
        result = ignoredResult;
      } else if (baseMappingValue.equals(cpString)) {
        result = validResult;
      } else {
        result = Row.of(IdnaType.mapped, baseMappingValue);
      }
      //if (0==(cp&0xFFF)) System.out.println(cp + " = " + result);
      mappingTable.put(cp, result);
    }
    return mappingTable;
  }
  
  
  private static void writeDataFile(UnicodeMap<String> mappingTable) throws IOException {
    String filename = "IdnaMappingTable-" + Default.ucdVersion() + ".txt";
    String unversionedFileName = "IdnaMappingTable.txt";
    PrintWriter writer = BagFormatter.openUTF8Writer(UCD_Types.GEN_DIR + "/idna/" + Default.ucdVersion(), unversionedFileName);
    writer.println("# " + filename + "- DRAFT\n" +
            "# Date: " + dateFormat.format(new Date()) + " [MD]\n" +
            "#\n" +
            "# Unicode IDNA Compatible Preprocessing (UTS #46)\n" +
            "# Copyright (c) 1991-2009 Unicode, Inc.\n" +
            "# For terms of use, see http://www.unicode.org/terms_of_use.html\n" +
    "# For documentation, see http://www.unicode.org/reports/tr46/\n");

    //    # IdnaMappingTable-5.1.0.txt - DRAFT
    //    # Date: 2009-11-14 08:10:42 GMT [MD]
    //    #
    //    # Unicode IDNA Compatible Preprocessing (UTS #46)
    //    # Copyright (c) 1991-2009 Unicode, Inc.
    //    # For terms of use, see http://www.unicode.org/terms_of_use.html
    //    # For documentation, see http://www.unicode.org/reports/tr46/

    BagFormatter bf = new BagFormatter();
    bf.setLabelSource(null);
    bf.setRangeBreakSource(null);
    bf.setShowCount(false);

    bf.setValueSource(new UnicodeProperty.UnicodeMapProperty().set(mappingTable));
    writer.println(bf.showSetNames(mappingTable.keySet()));
    writer.close();
  }
  
  static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'", ULocale.US);
  static {
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }
  
  /**
3. Mapping
   This profile specifies mapping using the following tables from
   [STRINGPREP]:
   Table B.1
   Table B.2
4. Normalization
   This profile specifies using Unicode normalization form KC, as
   described in [STRINGPREP].
5. Prohibited Output
   Table C.1.2
   Table C.2.2
   Table C.3
   Table C.4
   Table C.5
   Table C.6
   Table C.7
   Table C.8
   Table C.9
   */
  private static UnicodeMap<R2<IdnaType, String>> getIdna2003Table() throws IOException {
    UnicodeMap<R2<IdnaType, String>> result = new UnicodeMap<R2<IdnaType, String>>();
    UnicodeMap<R3<Idna2003Table, String, String>> rawIdna2003Data = getNamePrepData32(EnumSet.of(
            Idna2003Table.B_1, 
            Idna2003Table.B_2, 
            Idna2003Table.C_1_2
            , Idna2003Table.C_2_2
            , Idna2003Table.C_3
            , Idna2003Table.C_4
            , Idna2003Table.C_5
            , Idna2003Table.C_6
            , Idna2003Table.C_7
            , Idna2003Table.C_8
            , Idna2003Table.C_9
            ));
    IdnaType status;
    for (int i = 0; i <= 0x10FFFF; ++i) {
      R3<Idna2003Table, String, String> data = rawIdna2003Data.get(i);
      Idna2003Table type = data == null ? Idna2003Table.none : data.get0();
      String mapping = null;
      switch (type) {
      case A_1: case C_1_2: case C_2_1: case C_2_2: case C_3: case C_4: case C_5: case C_6: case C_7: case C_8: case C_9:
        status = IdnaType.disallowed;
        break;
      case B_1: case B_2:
        mapping = data.get1();
        // fall through
      default:
        String original = UTF16.valueOf(i);
        if (U32.contains(i)) {
          mapping = normalizeAndCheckString(mapping != null ? mapping : original, rawIdna2003Data);
        }
        status = mapping == null ? IdnaType.disallowed :
          mapping.length() == 0 ? IdnaType.ignored :
            mapping.equals(original) ? IdnaType.valid :
              IdnaType.mapped;
        if (status == IdnaType.valid || status == IdnaType.ignored) {
          mapping = null;
        }
      }
      result.put(i, Row.of(status,mapping));
    }
    return result.freeze();
  }
  
  static Normalizer normalizer32 = new Normalizer(UCD_Types.NFKC, "3.2.0");

  private static String normalizeAndCheckString(String inputString, UnicodeMap<R3<Idna2003Table, String, String>> rawIdna2003Data) {
    int cp;
    String string = normalizer32.normalize(inputString);
    for (int i = 0; i < string.length(); i += Character.charCount(cp)) {
      cp = string.codePointAt(i);
      R3<Idna2003Table, String, String> data = rawIdna2003Data.get(cp);
      Idna2003Table type = data == null ? Idna2003Table.none : data.get0();
      switch (type) {
      case A_1: case C_1_2: case C_2_1: case C_2_2: case C_3: case C_4: case C_5: case C_6: case C_7: case C_8: case C_9:
        return null;
      }
    }
    return string;
  }
  
  enum Idna2003Table {none, A_1, B_1, B_2, B_3, C_1_1, C_1_2, C_2_1, C_2_2, C_3, C_4, C_5, C_6, C_7, C_8, C_9, D_1, D_2}

  /**
  A.1 Unassigned code points in Unicode 3.2
     ----- Start Table A.1 -----
     0221
  B.1 Commonly mapped to nothing
     ----- Start Table B.1 -----
     00AD; ; Map to nothing
  B.2 Mapping for case-folding used with NFKC
     ----- Start Table B.2 -----
     0041; 0061; Case map
  B.3 Mapping for case-folding used with no normalization
     ----- Start Table B.3 -----
     0041; 0061; Case map
  C.1.1 ASCII space characters
     ----- Start Table C.1.1 -----
     0020; SPACE
  C.1.2 Non-ASCII space characters
     ----- Start Table C.1.2 -----
     00A0; NO-BREAK SPACE
  C.2.1 ASCII control characters
     ----- Start Table C.2.1 -----
     0000-001F; [CONTROL CHARACTERS]
  C.2.2 Non-ASCII control characters
     ----- Start Table C.2.2 -----
     0080-009F; [CONTROL CHARACTERS]
  C.2.2 Non-ASCII control characters
     ----- Start Table C.2.2 -----
     0080-009F; [CONTROL CHARACTERS]
  C.3 Private use
     ----- Start Table C.3 -----
     E000-F8FF; [PRIVATE USE, PLANE 0]
  C.4 Non-character code points
     ----- Start Table C.4 -----
     FDD0-FDEF; [NONCHARACTER CODE POINTS]
  C.5 Surrogate codes
     ----- Start Table C.5 -----
     D800-DFFF; [SURROGATE CODES]
  C.6 Inappropriate for plain text
     ----- Start Table C.6 -----
     FFF9; INTERLINEAR ANNOTATION ANCHOR
  C.7 Inappropriate for canonical representation
     ----- Start Table C.7 -----
     2FF0-2FFB; [IDEOGRAPHIC DESCRIPTION CHARACTERS]
  C.8 Change display properties or are deprecated
     ----- Start Table C.8 -----
     0340; COMBINING GRAVE TONE MARK
  C.9 Tagging characters
     ----- Start Table C.9 -----
     E0001; LANGUAGE TAG
  D.1 Characters with bidirectional property "R" or "AL"
     ----- Start Table D.1 -----
     05BE
  D.2 Characters with bidirectional property "L"
     ----- Start Table D.2 -----
     0041-005A
  */ 
  
  static Pattern TABLE_DELIMITER = Pattern.compile("\\Q-----\\E\\s*(Start|End)\\s*Table\\s*(\\S+)\\s*\\Q-----\\E");
  static Pattern MAP_LINE = Pattern.compile("([A-Z0-9]{4,6})" +
      "(?:-([A-Z0-9]{4,6}))?" +
      "(?:\\s*;\\s*((?:[A-Z0-9]{4,6}\\s*)*))?" +
      "(?:\\s*;\\s*.*)?");
  static Pattern SET_LINE = Pattern.compile("([A-Z0-9]{4,6})" +
          "(?:-([A-Z0-9]{4,6}))?" +
          "(?:\\s*;\\s*.*)?");

  private static UnicodeMap<Row.R3<Idna2003Table, String, String>> getNamePrepData32(EnumSet<Idna2003Table> allowed) throws IOException {
    UnicodeMap<Row.R3<Idna2003Table, String, String>> rawMapping = new UnicodeMap<Row.R3<Idna2003Table, String, String>>();

    Matcher tableDelimiter = TABLE_DELIMITER.matcher("");
    Matcher mapLine = MAP_LINE.matcher("");
    Matcher setLine = SET_LINE.matcher("");
    BufferedReader in = BagFormatter.openUTF8Reader(UCD_Types.BASE_DIR + "idna/", "nameprep.txt");
    Idna2003Table table = null;
    boolean inTable = false;
    boolean isMapping = false;
    while (true) {
      String line = in.readLine();
      if (line == null) break;
      line = line.trim();
      if (line.length() == 0 || line.startsWith("Hoffman") || line.startsWith("RFC")) continue;
      if (line.startsWith("-----")) {
        if (!tableDelimiter.reset(line).matches()) {
          throw new IllegalArgumentException("Bad syntax: " + line);
        }
        inTable = tableDelimiter.group(1).equals("Start");
        Idna2003Table newTable = Idna2003Table.valueOf(tableDelimiter.group(2).replace(".","_"));
        if (inTable) {
          if (table != null) {
            throw new IllegalArgumentException("Table not terminated: " + table + "; " + line);
          }
          table = newTable;
          isMapping = newTable.toString().startsWith("B");
        } else {
          if (newTable != table) {
            throw new IllegalArgumentException("Bad table end: " + newTable + " != " +  table + "; " + line);
          }
          table = null;
          isMapping = false;
        }
        continue;
      }
      if (!inTable) {
        System.out.println("Ignoring: " + line);
        continue;
      }
      if (!allowed.contains(table)) { // skip for now
        continue;
      }
      Matcher lineMatcher = isMapping ? mapLine : setLine;
      if (!lineMatcher.reset(line).matches()) {
        throw new IllegalArgumentException("Illegal range-value syntax: " + line);
      }
      int startCode = Utility.fromHex(lineMatcher.group(1)).codePointAt(0);
      String endCodeString = lineMatcher.groupCount() < 2 ? null : lineMatcher.group(2);
      String group3 = lineMatcher.groupCount() < 3 ? null : lineMatcher.group(3);
      String group4 = lineMatcher.groupCount() < 4 ? null : lineMatcher.group(4);
      int endCode = endCodeString == null ? startCode : Utility.fromHex(endCodeString).codePointAt(0);
      String comment, mapValueString;
      if (isMapping) {
        comment = group4;
        mapValueString = Utility.fromHex(group3);
      } else {
        comment = group3;
        mapValueString = null;
      }
      // check for duplicates
      R3<Idna2003Table, String, String> newValue = Row.of(table, (String)mapValueString, (String)comment);

      for (int i = startCode; i <= endCode; ++i) {
        R3<Idna2003Table, String, String> oldValue = rawMapping.get(i);
        if (oldValue != null) {
          System.out.println("Duplicates: " + Utility.hex(i) + "\told: " + oldValue + "\t skipping new: " + newValue);
        } else {
          rawMapping.put(i, newValue);
        }
      }
    }
    in.close();
    R3<Idna2003Table, String, String> badValue = Row.of(Idna2003Table.C_9, (String)null, (String)null);

    // fix ASCII
    rawMapping.putAll(0, 0x7F, badValue);
    rawMapping.putAll(VALID_ASCII, null);

    for (int i = 'A'; i <= 'Z'; ++i) {
      R3<Idna2003Table, String, String> alphaMap = Row.of(Idna2003Table.B_1, UTF16.valueOf(i-'A'+'a'), (String)null);
      rawMapping.put(i, alphaMap);
    }
    return rawMapping.freeze();
  }
}
