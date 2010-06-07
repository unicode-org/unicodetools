package org.unicode.text.UCD;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.TreeSet;

import org.unicode.jsp.Idna2003;
import org.unicode.jsp.StringPrepData;
import org.unicode.jsp.Idna.IdnaType;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.dev.test.util.BagFormatter.NameLabel;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class GenerateIdna {

  public static UnicodeSet U32;
  public static UnicodeSet VALID_ASCII;
  static ToolUnicodePropertySource properties;
  static UnicodeSet cn;
  static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'", ULocale.US);
  static {
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  public static void main(String[] args) throws IOException {

    switch (args.length){
    case 0: 
      break;
    case 1: 
      Default.setUCD(args[0]);
      break;
    default: throw new IllegalArgumentException("Only single argument allowed:\t" + Arrays.asList(args));
    }
    
    U32 = new UnicodeSet("[:age=3.2:]").freeze();
    VALID_ASCII = new UnicodeSet("[\\u002Da-zA-Z0-9]").freeze();
    properties = ToolUnicodePropertySource.make(Default.ucdVersion());
    cn = properties.getSet("gc=Cn").freeze();


    UnicodeMap<String> mappings = new UnicodeMap<String>(); 
    UnicodeMap<IdnaType> types = new UnicodeMap<IdnaType>();
    StringPrepData.getIdna2003Tables(mappings, types);

    UnicodeMap<Row.R2<IdnaType, String>> mappingTable = createMappingTable(mappings, types);

    verifyDifferences(mappings, types, mappingTable);

    UnicodeMap<String> stringMappingTable = new UnicodeMap<String>();
    for (int cp = 0; cp <= 0x10FFFF; ++cp) {
      Row.R2<IdnaType, String> value = mappingTable.get(cp);
      if (value == null) {
        throw new IllegalArgumentException("Expected value for " + Utility.hex(cp));
      }
      String status = value.get0().toString();
      String mapping = value.get1();
      if (status.equals("mapped") || status.equals("deviation")) {
        status += Utility.repeat(" ", 10-status.length()) + " ; ";
        if (mapping != null && mapping.length() != 0) {
          status += Utility.hex(mapping);
        }
      } else {
        if (mapping != null) {
          throw new IllegalArgumentException("bad mapping:\t" + value);
        }
      }
      stringMappingTable.put(cp, status);
    }

    writeDataFile(stringMappingTable);
    System.out.println("After running, copy the data file to the jsp directory, and run org.unicode.jsptest.TestGenerate to generate the differences table.");
  }

  private static void verifyDifferences(UnicodeMap<String> mappings, UnicodeMap<IdnaType> types, UnicodeMap<Row.R2<IdnaType, String>> mappingTable) {
    System.out.println("Verifying Differences");
    UnicodeMap<Row.R3<String, R2<IdnaType, String>, R2<IdnaType, String>>> diff = new UnicodeMap();
    for (int i = 0; i <= 0x10FFFF; ++i) {
      if (!U32.contains(i)) continue;
      R2<IdnaType, String> data46 = mappingTable.get(i);
      R2<IdnaType, String> data2003 = Row.of(types.get(i), mappings.get(i));
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

  private static UnicodeMap<Row.R2<IdnaType, String>> createMappingTable(UnicodeMap<String> mappings, UnicodeMap<IdnaType> types) {

    UnicodeMap<String> nfkc_cfMap = properties.getProperty("NFKC_CF").getUnicodeMap();
    UnicodeMap<String> baseMapping = new UnicodeMap<String>().putAll(nfkc_cfMap);
    baseMapping.put(0xFF0E, "\u002E");
    baseMapping.put(0x3002, "\u002E");
    baseMapping.put(0xFF61, "\u002E");
    baseMapping.freeze();


    UnicodeSet labelSeparator = new UnicodeSet("[\\u002E \\uFF0E \\u3002 \\uFF61]").freeze();

    UnicodeSet baseValidSet = new UnicodeSet(0,0x10FFFF)
    .removeAll(properties.getSet("Changes_When_NFKC_Casefolded=true"))
    .removeAll(properties.getSet("gc=Cc"))
    .removeAll(properties.getSet("gc=Cf"))
    .removeAll(cn)
    .removeAll(properties.getSet("gc=Co"))
    .removeAll(properties.getSet("gc=Cs"))
    .removeAll(properties.getSet("gc=Zl"))
    .removeAll(properties.getSet("gc=Zp"))
    .removeAll(properties.getSet("gc=Zs"))
    .removeAll(properties.getSet("Block=Ideographic_Description_Characters"))
    .removeAll(new UnicodeSet("[\\u0000-\\u007F]"))
    //.addAll(0x200c, 0x200d)
    .addAll(VALID_ASCII).freeze()
    ;

    System.out.println("Base Valid Set & nfkcqc=n" + new UnicodeSet("[:nfkcqc=n:]").retainAll(baseValidSet));

    R2<UnicodeSet, UnicodeSet> baseExclusionSetInfo = computeBaseExclusionSet(baseMapping, baseValidSet); 
    UnicodeSet disallowedExclusionSet = baseExclusionSetInfo.get0();
    UnicodeSet mappingChanged = baseExclusionSetInfo.get1();
    UnicodeSet baseExclusionSet = new UnicodeSet(disallowedExclusionSet).addAll(mappingChanged);
    UnicodeSet baseExclusionSet2 = new UnicodeSet("[" +
            "\\u04C0 \\u10A0-\\u10C5 \\u2132 \\u2183" +
            "\\U0002F868  \\U0002F874 \\U0002F91F \\U0002F95F \\U0002F9BF" +
            "\u3164 \uFFA0 \u115F \u1160 \u17B4 \u17B5 \u1806 \uFFFC \uFFFD" +
            "[\\u200E\\u200F\\u202A-\\u202E\\u2061-\\u2063\\u206A-\\u206F\\U0001D173-\\U0001D17A\\U000E0001\\U000E0020-\\U000E007F]" +
            "[\u200B\u2060\uFEFF]" +
    "]").freeze(); //.addAll(cn)

    System.out.println("computed base exclusion disallowed:\t" + disallowedExclusionSet);
    System.out.println("computed base exclusion mapping changed:\t" + mappingChanged);

    if (false && !baseExclusionSet.equals(baseExclusionSet2)) {
      System.out.println("computed-static:\t" + new UnicodeSet(baseExclusionSet).removeAll(baseExclusionSet2));
      System.out.println("static-computed:\t" + new UnicodeSet(baseExclusionSet2).removeAll(baseExclusionSet));
      throw new IllegalArgumentException();
    }

    System.out.println("***Overlap with baseValidSet and baseExclusionSet:\t" + new UnicodeSet(
            baseValidSet).retainAll(baseExclusionSet));

    UnicodeSet deviationSet = new UnicodeSet("[\u200C \u200D \u00DF \u03C2]").freeze(); // \u200C \u200D 

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
      } else if (cn.contains(cp)) { // do this in a different order just for debuggin
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
    UnicodeSet excluded = new UnicodeSet();
    do {
      excluded.clear();
      UnicodeSet validSet = mappingTable.getSet(validResult);
      UnicodeSet disallowedSet = mappingTable.getSet(disallowedResult);
      UnicodeSet ignoredSet = mappingTable.getSet(ignoredResult);
      for (String valid : validSet) {
        String nfd = Default.nfd().normalize(valid);
        if (!validSet.containsAll(nfd)) {
          excluded.add(valid);
        }
      }
      UnicodeSet mappedSet = new UnicodeSet(0,0x10FFFF).removeAll(validSet)
      .removeAll(disallowedSet).removeAll(ignoredSet);
      for (String mapped : mappedSet) {
        R2<IdnaType, String> mappedValue = mappingTable.get(mapped);
        String mapResult = mappedValue.get1();
        String nfd = Default.nfd().normalize(mapResult);
        if (!validSet.containsAll(nfd)) {
          excluded.add(mapped);
        }
      }
      mappingTable.putAll(excluded, disallowedResult);
      System.out.println("***InvalidDecomposition Exclusion: " + excluded);
    } while (excluded.size() != 0);

    // detect errors, where invalid character doesn't have at least one invalid in decomposition
    UnicodeSet invalidSet = mappingTable.getSet(disallowedResult).freeze();
    for (String valid : invalidSet) {
      String nfd = Default.nfd().normalize(valid);
      if (invalidSet.containsNone(nfd)) {
        System.out.println("SUSPICOUS: " + valid + "\t" + nfd);
      }
    }


    return mappingTable.freeze();
  }


  private static R2<UnicodeSet,UnicodeSet> computeBaseExclusionSet(UnicodeMap<String> baseMapping, UnicodeSet baseValidSet) {
    UnicodeSet disallowed = new UnicodeSet(); 
    UnicodeSet mappingChanged = new UnicodeSet(); 
    for (UnicodeSetIterator it = new UnicodeSetIterator(U32); it.next();) {
      int i = it.codepoint;
      IdnaType type = Idna2003.SINGLETON.types.get(i);
      switch (type) {
      case disallowed:
        if (baseValidSet.contains(i)) {
          disallowed.add(i);
          break;
        }
        String base2 = baseMapping.get(i);
        if (base2 != null && baseValidSet.containsAll(base2)) {
          disallowed.add(i);
        }
        break;
      default:
        String idna2003 = Idna2003.SINGLETON.mappings.get(i);
        String base = baseMapping.get(i);
        if (base == idna2003) continue;
        if (base == null) base = UTF16.valueOf(i);
        if (idna2003 == null) idna2003 = UTF16.valueOf(i);
        if (!base.equals(idna2003)) {
          mappingChanged.add(i);
        }
        break;
      }
    }
    return Row.of(disallowed.freeze(), mappingChanged.freeze());
  }

  private static void writeDataFile(UnicodeMap<String> mappingTable) throws IOException {
    String filename = "IdnaMappingTable-" + Default.ucdVersion() + ".txt";
    String unversionedFileName = "IdnaMappingTable.txt";
    PrintWriter writer = BagFormatter.openUTF8Writer("/Users/markdavis/Documents/workspace35/draft/reports/tr46/" + Default.ucdVersion(), unversionedFileName);
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

    UnicodeProperty ASSIGNED = new UnicodeProperty.SimpleProperty() {
      @Override
      protected String _getValue(int codepoint) {
        return cn.contains(codepoint) ? "Cn" : "As";
      }
    };
    UnicodeProperty age = properties.getProperty("age");
    NameLabel name = new BagFormatter.NameLabel(properties);
    BagFormatter bf = new BagFormatter();
    bf.setLabelSource(age);
    bf.setRangeBreakSource(ASSIGNED);
    bf.setShowCount(false);
    bf.setNameSource(name);

    bf.setValueSource(new UnicodeProperty.UnicodeMapProperty().set(mappingTable));
    writer.println(bf.showSetNames(mappingTable.keySet()));
    writer.close();
  }

}
