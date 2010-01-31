/**
 * 
 */
package org.unicode.jsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.jsp.Idna.IdnaType;
import org.unicode.text.UCD.GenerateIdna;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.text.UTF16;

public class StringPrepData {
  private static final boolean DEBUG = false;

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

  public static void getIdna2003Tables(UnicodeMap<String> mappings, UnicodeMap<IdnaType> types) {
    UnicodeMap<R3<StringPrepData.Idna2003Table, String, String>> rawIdna2003Data = getNamePrepData32(EnumSet.of(
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
      R3<StringPrepData.Idna2003Table, String, String> data = rawIdna2003Data.get(i);
      StringPrepData.Idna2003Table type = data == null ? Idna2003Table.none : data.get0();
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
        if (GenerateIdna.U32.contains(i)) {
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
      mappings.put(i, mapping);
      types.put(i, status);
    }
    // special handling for separators
    mappings.putAll(Idna.OTHER_DOT_SET,".");
    types.putAll(Idna.OTHER_DOT_SET,IdnaType.mapped);

    mappings.freeze();
    types.freeze();
  }

  static Normalizer normalizer32 = new Normalizer(UCD_Types.NFKC, "3.2.0");

  private static String normalizeAndCheckString(String inputString, UnicodeMap<R3<StringPrepData.Idna2003Table, String, String>> rawIdna2003Data) {
    String string = normalizer32.normalize(inputString);
    int cp;
    for (int i = 0; i < string.length(); i += Character.charCount(cp)) {
      cp = string.codePointAt(i);
      R3<StringPrepData.Idna2003Table, String, String> data = rawIdna2003Data.get(cp);
      StringPrepData.Idna2003Table type = data == null ? Idna2003Table.none : data.get0();
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

  private static UnicodeMap<Row.R3<StringPrepData.Idna2003Table, String, String>> getNamePrepData32(EnumSet<StringPrepData.Idna2003Table> allowed) {
    try {
      UnicodeMap<Row.R3<StringPrepData.Idna2003Table, String, String>> rawMapping = new UnicodeMap<Row.R3<StringPrepData.Idna2003Table, String, String>>();

      Matcher tableDelimiter = TABLE_DELIMITER.matcher("");
      Matcher mapLine = MAP_LINE.matcher("");
      Matcher setLine = SET_LINE.matcher("");
      BufferedReader in = FileUtilities.openFile(StringPrepData.class, "nameprep.txt");
      //BufferedReader in = BagFormatter.openUTF8Reader(UCD_Types.BASE_DIR + "idna/", "nameprep.txt");
      StringPrepData.Idna2003Table table = null;
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
          StringPrepData.Idna2003Table newTable = Idna2003Table.valueOf(tableDelimiter.group(2).replace(".","_"));
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
          if (DEBUG) System.out.println("Ignoring: " + line);
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
        R3<StringPrepData.Idna2003Table, String, String> newValue = Row.of(table, (String)mapValueString, (String)comment);

        for (int i = startCode; i <= endCode; ++i) {
          R3<StringPrepData.Idna2003Table, String, String> oldValue = rawMapping.get(i);
          if (oldValue != null) {
            if (DEBUG) System.out.println("Duplicates: " + Utility.hex(i) + "\told: " + oldValue + "\t skipping new: " + newValue);
          } else {
            rawMapping.put(i, newValue);
          }
        }
      }
      in.close();
      R3<StringPrepData.Idna2003Table, String, String> badValue = Row.of(Idna2003Table.C_9, (String)null, (String)null);

      // fix ASCII
      rawMapping.putAll(0, 0x7F, badValue);
      rawMapping.putAll(GenerateIdna.VALID_ASCII, null);

      for (int i = 'A'; i <= 'Z'; ++i) {
        R3<StringPrepData.Idna2003Table, String, String> alphaMap = Row.of(Idna2003Table.B_1, UTF16.valueOf(i-'A'+'a'), (String)null);
        rawMapping.put(i, alphaMap);
      }
      return rawMapping.freeze();
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}