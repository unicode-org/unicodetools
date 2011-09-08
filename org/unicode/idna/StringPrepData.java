/**
 * 
 */
package org.unicode.idna;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.idna.Idna.IdnaType;
import org.unicode.jsp.FileUtilities;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class StringPrepData {
    private static final boolean DEBUG = getDebugFlag(StringPrepData.class);
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
     * @param STD3 TODO
     */

    public static void getIdna2003Tables(UnicodeMap<String> mappings, UnicodeMap<IdnaType> types, boolean STD3) {
        UnicodeSet prohibited = new UnicodeSet();
        getNamePrepData32(EnumSet.of(
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
        ), mappings, prohibited, STD3);

        types.putAll(0,0x10FFFF, IdnaType.disallowed);
        types.putAll(IdnaTypes.U32, IdnaType.valid);
        types.putAll(prohibited, IdnaType.disallowed);
        UnicodeSet ignored = mappings.getSet("");
        UnicodeSet hasMapping = mappings.keySet();
        types.putAll(ignored, IdnaType.ignored);
        types.putAll(new UnicodeSet(hasMapping).removeAll(ignored), IdnaType.mapped);
        //mappings.putAll(ignored, null);

        // special handling for separators
        mappings.putAll(IdnaTypes.OTHER_DOT_SET,".");
        // special old exceptions

        mappings.put(0x2F868, UTF16.valueOf(0x2136A));
        mappings.put(0x2F874, UTF16.valueOf(0x5F33));
        mappings.put(0x2F91F, UTF16.valueOf(0x43AB));
        mappings.put(0x2F95F, UTF16.valueOf(0x7AAE));
        mappings.put(0x2F9BF, UTF16.valueOf(0x4D57));

        types.putAll(IdnaTypes.OTHER_DOT_SET,IdnaType.mapped);
        types.put('.',IdnaType.valid);

        mappings.freeze();
        types.freeze();
    }

    private static boolean getDebugFlag(Class<?> class1) {
        return getDebugFlag(class1, "debug");
    }

    private static boolean getDebugFlag(Class<?> class1, String flagName) {
        String className = class1.getName().toLowerCase(Locale.ROOT);
        int lastPart = className.lastIndexOf('.');
        if (lastPart >= 0) {
            className = className.substring(lastPart+1);
        }
        return System.getProperty(className+"_" + flagName) != null;
    }



    //static Normalizer normalizer32 = new Normalizer(UCD_Types.NFKC, "3.2.0");

    //  private static String normalizeAndCheckString(String inputString, UnicodeMap<R3<StringPrepData.Idna2003Table, String, String>> rawIdna2003Data) {
    //    String string = Normalizer.normalize(inputString, Normalizer.NFKC);
    //    int cp;
    //    for (int i = 0; i < string.length(); i += Character.charCount(cp)) {
    //      cp = string.codePointAt(i);
    //      R3<StringPrepData.Idna2003Table, String, String> data = rawIdna2003Data.get(cp);
    //      StringPrepData.Idna2003Table type = data == null ? Idna2003Table.none : data.get0();
    //      switch (type) {
    //      case A_1: case C_1_2: case C_2_1: case C_2_2: case C_3: case C_4: case C_5: case C_6: case C_7: case C_8: case C_9:
    //        return null;
    //      }
    //    }
    //    return string;
    //  }

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

    private static void getNamePrepData32(EnumSet<StringPrepData.Idna2003Table> allowed, UnicodeMap<String> mappings, UnicodeSet prohibited, boolean STD3) {
        try {

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
                int startCode = Utility.fromHex(lineMatcher.group(1),4," ").codePointAt(0);
                String endCodeString = lineMatcher.groupCount() < 2 ? null : lineMatcher.group(2);
                String group3 = lineMatcher.groupCount() < 3 ? null : lineMatcher.group(3);
                String group4 = lineMatcher.groupCount() < 4 ? null : lineMatcher.group(4);
                int endCode = endCodeString == null ? startCode : Utility.fromHex(endCodeString,4," ").codePointAt(0);
                String comment, mapValueString;
                if (isMapping) {
                    comment = group4;
                    try {
                        mapValueString = group3.length() == 0 ? "" : Utility.fromHex(group3,4," ");
                    } catch (RuntimeException e) {
                        throw e;
                    }
                } else {
                    comment = group3;
                    mapValueString = null;
                }
                // check for duplicates

                for (int i = startCode; i <= endCode; ++i) {
                    if (mapValueString != null) {
                        String oldValue = mappings.get(i);
                        if (oldValue != null && !UnicodeProperty.equals(mapValueString, oldValue)) {
                            throw new IllegalArgumentException("Duplicates: " + Utility.hex(i) + "\told: " + oldValue + "\t skipping new: " + mapValueString);
                        }
                        mappings.put(i, mapValueString);
                    } else {
                        prohibited.add(i);
                    }
                }
            }
            in.close();

            // fix ASCII
            if (STD3) {
                prohibited.addAll(0, 0x7F);
                prohibited.removeAll(IdnaTypes.VALID_ASCII);
            } else {
                prohibited.add(".");
            }


            for (int i = 'A'; i <= 'Z'; ++i) {
                mappings.put(i, UTF16.valueOf(i-'A'+'a'));
            }
            // fix up mappings

            // add normalization maps for all unmapped characters
            UnicodeSet addedMappings = new UnicodeSet();
            for (UnicodeSetIterator it = new UnicodeSetIterator(IdnaTypes.U32); it.next();) {
                int i = it.codepoint;
                String mapValue = mappings.get(i);
                if (mapValue == null) {
                    if (Idna.NFKC_3_2.isTransformed(i)) {
                        continue;
                    }
                    addedMappings.add(i);
                    mappings.put(i, Idna.NFKC_3_2.transform(i)); // Normalizer.normalize(i, Normalizer.NFKC, Normalizer.UNICODE_3_2));
                } else if (!Idna.NFKC_3_2.isTransformed(mapValue)) { // (!Normalizer.isNormalized(mapValue, Normalizer.NFKC, Normalizer.UNICODE_3_2)) {
                    String newValue = Idna.NFKC_3_2.transform(mapValue); // Normalizer.normalize(mapValue, Normalizer.NFKC, Normalizer.UNICODE_3_2);
                    if (DEBUG) System.out.println("Change for NFKC mapping of " + Utility.hex(i) + ", \t" + Utility.hex(mapValue) + " \t => \t" + Utility.hex(newValue));
                    addedMappings.add(i);
                    mappings.put(i, newValue);
                }
            }
            if (DEBUG) System.out.println("Adding NFKC mapping for " + addedMappings.toPattern(false) + ",\t" + addedMappings);

            // remove identical mapping
            UnicodeSet identicals = new UnicodeSet();
            for (String source : mappings) {
                String mapping = mappings.get(source);
                if (UnicodeProperty.equals(source, mapping)) {
                    identicals.add(source);
                }
            }
            if (DEBUG) System.out.println("Removing Identical mapping for " + identicals.toPattern(false) + ",\t" + identicals);
            mappings.putAll(identicals, null);

            // fix the prohibition according to the resulting characters
            for (String source : mappings) {
                int  cpSource = source.codePointAt(0);
                boolean shouldBeProhibited = false;
                String mapping = mappings.get(source);
                for (int i = 0; i < mapping.length(); i += Character.charCount(cpSource)) {
                    int cpInMapping = mapping.codePointAt(i);
                    String otherMap = mappings.get(cpInMapping);
                    if (otherMap != null) {
                        throw new IllegalArgumentException("Recursive mapping\t" + Utility.hex(source) + ",\t" + Utility.hex(mapping) + ",\t" + Utility.hex(cpInMapping)  + ",\t" + Utility.hex(otherMap));
                    }
                    if (prohibited.contains(cpInMapping)) {
                        shouldBeProhibited = true;
                    }
                }
                boolean wasProhibited = prohibited.contains(cpSource);
                if (wasProhibited != shouldBeProhibited) {
                    if (shouldBeProhibited) {
                        if (DEBUG) System.out.println("Changing to prohibited for " + source + "\t" + Utility.hex(cpSource) + ",\t" + shouldBeProhibited);
                        prohibited.add(cpSource);
                    } else {
                        if (DEBUG) System.out.println("Removing from prohibited for " + source + "\t" + Utility.hex(cpSource) + ",\t" + shouldBeProhibited);
                        prohibited.remove(cpSource);
                    }
                }
            }
            // now remove all prohibited from the set
            mappings.putAll(prohibited, null);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}