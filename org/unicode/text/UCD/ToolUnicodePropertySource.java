package org.unicode.text.UCD;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.jsp.ScriptTester.ScriptExtensions;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.dev.test.util.UnicodeProperty.SimpleProperty;
import com.ibm.icu.dev.test.util.UnicodeProperty.UnicodeMapProperty;
import com.ibm.icu.impl.StringUCharacterIterator;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.IDNA;
import com.ibm.icu.text.StringPrep;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

/**
 * Class that provides all of the properties for formatting in the Unicode
 * standard data files. Note that many of these are generated directly from UCD,
 * and many from {@link DerivedProperty}. So fixes to some will go there.
 * 
 * @author markdavis
 * 
 */
public class ToolUnicodePropertySource extends UnicodeProperty.Factory {
  private static final String[] MAYBE_VALUES = {"M", "Maybe", "U", "Undetermined"};

  private static final String[] NO_VALUES = {"N", "No", "F", "False"};

  private static final String[] YES_VALUES = {"Y", "Yes", "T", "True"};

  static final boolean   DEBUG        = false;

  private UCD            ucd;

  private Normalizer     nfc, nfd, nfkd, nfkc;

  private static boolean needAgeCache = true;

  private static UCD[]   ucdCache     = new UCD[UCD_Types.LIMIT_AGE];

  private static HashMap factoryCache = new HashMap();

  private boolean special = false;

  public static synchronized ToolUnicodePropertySource make(String version) {
    ToolUnicodePropertySource result = (ToolUnicodePropertySource) factoryCache.get(version);
    if (result != null)
      return result;
    result = new ToolUnicodePropertySource(version);
    factoryCache.put(version, result);
    return result;
  }

  private ToolUnicodePropertySource(String version) {
    ucd = UCD.make(version);
    nfc = new Normalizer(Normalizer.NFC, ucd.getVersion());
    nfd = new Normalizer(Normalizer.NFD, ucd.getVersion());
    nfkc = new Normalizer(Normalizer.NFKC, ucd.getVersion());
    nfkd = new Normalizer(Normalizer.NFKD, ucd.getVersion());

    version = ucd.getVersion(); // regularize

    // first the special cases
    if (DEBUG)
      System.out.println("Adding Simple Cases");

    //    add(new UnicodeProperty.SimpleProperty() {
    //      public String _getValue(int codepoint) {
    //        if (!nfc.isNormalized(codepoint))
    //          return "No";
    //        else if (nfc.isTrailing(codepoint))
    //          return "Maybe";
    //        else
    //          return "Yes";
    //      }
    //
    //      public int getMaxWidth(boolean isShort) {
    //        return 15;
    //      }
    //    }.setMain("NFC", null, UnicodeProperty.STRING, version));


    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        int catMask = ucd.getCategoryMask(codepoint);
        if (((1 << UCD_Types.Cc) & catMask) != 0) {
          return "<control-" + Utility.hex(codepoint, 4) + ">";
          // return "<control>";
        }
        if ((ODD_BALLS & catMask) != 0) {
          return null;
        }
        return ucd.getName(codepoint);
      }
    }.setValues("<string>").setMain("Name", "na", UnicodeProperty.MISC, version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return UCharacter.getName1_0(codepoint);
      }
    }.setValues("<string>").setMain("Unicode_1_Name", "na1", UnicodeProperty.MISC, version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return "";
      }
    }.setValues("<string>").setMain("ISO_Comment", "isc", UnicodeProperty.MISC, version));

    // add(new UnicodeProperty.SimpleProperty() {
    // public String _getValue(int codepoint) {
    // return "";
    // }
    // }.setValues("<string>").setMain("Jamo_Short_Name", "JSN",
    // UnicodeProperty.MISC, version));

    addFakeProperty(version, UnicodeProperty.MISC, "<none>", "Name_Alias", "Name_Alias");
    addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkRSUnicode", "cjkRSUnicode", "kRSUnicode", "Unicode_Radical_Stroke", "URS");
    addFakeProperty(version, UnicodeProperty.NUMERIC, "<none>", "cjkAccountingNumeric", "cjkAccountingNumeric", "kAccountingNumeric");
    addFakeProperty(version, UnicodeProperty.NUMERIC, "<none>", "cjkOtherNumeric", "cjkOtherNumeric", "kOtherNumeric");
    addFakeProperty(version, UnicodeProperty.NUMERIC, "<none>", "cjkPrimaryNumeric", "cjkPrimaryNumeric", "kPrimaryNumeric");
    addFakeProperty(version, UnicodeProperty.STRING, "<none>", "cjkCompatibilityVariant", "cjkCompatibilityVariant", "kCompatibilityVariant");
    addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIICore", "cjkIICore", "kIICore");
    addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_GSource", "cjkIRG_GSource", "kIRG_GSource");
    addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_HSource", "cjkIRG_HSource", "kIRG_HSource");
    addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_JSource", "cjkIRG_JSource", "kIRG_JSource");
    addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_KPSource", "cjkIRG_KPSource", "kIRG_KPSource");
    addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_KSource", "cjkIRG_KSource", "kIRG_KSource");
    addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_TSource", "cjkIRG_TSource", "kIRG_TSource");
    addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_USource", "cjkIRG_USource", "kIRG_USource");
    addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_VSource", "cjkIRG_VSource", "kIRG_VSource");
    addFakeProperty(version, UnicodeProperty.MISC, "<none>", "cjkIRG_MSource", "cjkIRG_MSource", "kIRG_MSource");

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return ucd.getDecompositionMapping(codepoint);
      }
    }.setValues("<string>").setMain("Decomposition_Mapping", "dm", UnicodeProperty.STRING, version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return ucd.getCase(codepoint, UCD.FULL, UCD.LOWER);
      }
    }.setValues("<string>").setMain("Lowercase_Mapping", "lc", UnicodeProperty.STRING, version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return ucd.getCase(codepoint, UCD.FULL, UCD.LOWER);
      }
    }.setValues("<string>").setMain("Lowercase_Mapping", "lc", UnicodeProperty.STRING, version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return ucd.getCase(codepoint, UCD.SIMPLE, UCD.LOWER);
      }
    }.setValues("<string>").setMain("Simple_Lowercase_Mapping", "slc", UnicodeProperty.STRING,
            version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return ucd.getCase(codepoint, UCD.FULL, UCD.UPPER);
      }
    }.setValues("<string>").setMain("Uppercase_Mapping", "uc", UnicodeProperty.STRING, version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return ucd.getCase(codepoint, UCD.SIMPLE, UCD.UPPER);
      }
    }.setValues("<string>").setMain("Simple_Uppercase_Mapping", "suc", UnicodeProperty.STRING,
            version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return ucd.getCase(codepoint, UCD.FULL, UCD.TITLE);
      }
    }.setValues("<string>").setMain("Titlecase_Mapping", "tc", UnicodeProperty.STRING, version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return ucd.getCase(codepoint, UCD.SIMPLE, UCD.TITLE);
      }
    }.setValues("<string>").setMain("Simple_Titlecase_Mapping", "stc", UnicodeProperty.STRING,
            version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return ucd.getCase(codepoint, UCD.FULL, UCD.FOLD);
      }
    }.setValues("<string>").setMain("Case_Folding", "cf", UnicodeProperty.STRING, version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return ucd.getCase(codepoint, UCD.SIMPLE, UCD.FOLD);
      }
    }.setValues("<string>").setMain("Simple_Case_Folding", "scf", UnicodeProperty.STRING, version)
    .addName("sfc"));

    /*
     * cp=00FD, isc=<> != <MISSING> cp=00FD, lc=<> != <MISSING> cp=00FD, slc=<>
     * != <MISSING> cp=00FD, stc=<00DD> != <MISSING> cp=00FD, suc=<00DD> !=
     * <MISSING> cp=00FD, tc=<> != <MISSING> cp=00FD, uc=<> != <MISSING>
     */

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        if (DEBUG && codepoint == 0x1D100) {
          System.out.println("here");
        }
        // if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
        return ucd.getBlock(codepoint);
      }

      protected UnicodeMap _getUnicodeMap() {
        return ucd.blockData;
      }
    }
    .setValues(ucd.getBlockNames(null))
    .setMain("Block", "blk", UnicodeProperty.CATALOG, version)
    .addValueAliases(
            new String[][] {
                    { "Basic_Latin", "ASCII" },
                    { "Latin_1_Supplement", "Latin_1" },
                    { "Unified_Canadian_Aboriginal_Syllabics", "Canadian_Syllabics" },
                    { "Greek_And_Coptic", "Greek" },
                    { "Private_Use_Area", "Private_Use" },
                    { "Combining_Diacritical_Marks_For_Symbols", "Combining_Marks_For_Symbols" },
                    { "Arabic_Presentation_Forms_A", "Arabic_Presentation_Forms-A" },
            }, true)
            // .swapFirst2ValueAliases()
    );

    // add(new UnicodeProperty.SimpleProperty() {
    // public String _getValue(int codepoint) {
    // return "";
    // }
    // }.setValues("<string>").setMain("Jamo_Short_Name", "JSN",
    // UnicodeProperty.MISC, version));
    // UCD_Names.JAMO_L_TABLE[LIndex] + UCD_Names.JAMO_V_TABLE[VIndex] +
    // UCD_Names.JAMO_T_TABLE[TIndex]
    // LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7
    Set tempValues = new LinkedHashSet();
    tempValues.addAll(Arrays.asList(UCD_Names.JAMO_L_TABLE));
    tempValues.addAll(Arrays.asList(UCD_Names.JAMO_V_TABLE));
    tempValues.addAll(Arrays.asList(UCD_Names.JAMO_T_TABLE));
    tempValues.remove("");
    // tempValues.add("none");

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        int temp;
        temp = codepoint - UCD.TBase;
        if (temp > 0) { // skip first
          return temp >= UCD_Names.JAMO_T_TABLE.length ? null : UCD_Names.JAMO_T_TABLE[temp];
        }
        temp = codepoint - UCD.VBase;
        if (temp >= 0) {
          return temp >= UCD_Names.JAMO_V_TABLE.length ? null : UCD_Names.JAMO_V_TABLE[temp];
        }
        temp = codepoint - UCD.LBase;
        if (temp >= 0) {
          return temp >= UCD_Names.JAMO_L_TABLE.length ? null : UCD_Names.JAMO_L_TABLE[temp];
        }
        return null;
      }
    }.setValues(new ArrayList(tempValues)).setMain("Jamo_Short_Name", "JSN", UnicodeProperty.MISC,
            version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        // if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
        return ucd.getBidiMirror(codepoint);
      }
    }.setValues("<string>").setMain("Bidi_Mirroring_Glyph", "bmg", UnicodeProperty.STRING, version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        // if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
        return ucd.getCase(codepoint, UCD_Types.FULL, UCD_Types.FOLD);
      }
    }.setValues("<string>").setMain("Case_Folding", "cf", UnicodeProperty.STRING, version));

    add(new UnicodeProperty.SimpleProperty() {
      IdnaInfo info;
      {
        try {
          info = new IdnaInfo();
        } catch (IOException e) {
          throw new IllegalArgumentException("Can't find data");
        }
      }

      public String _getValue(int codepoint) {
        // if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
        return info.getIDNAType(codepoint) == IdnaInfo.IdnaType.OK ? UCD_Names.YES : UCD_Names.NO;
      }
    }.setMain("IdnOutput", "idnOut", UnicodeProperty.EXTENDED_BINARY, version));

    add(new UnicodeProperty.SimpleProperty() {
      NumberFormat nf = NumberFormat.getInstance();
      {
        nf.setGroupingUsed(false);
        nf.setMaximumFractionDigits(8);
        nf.setMinimumFractionDigits(1);
      }

      public String _getValue(int codepoint) {

        double num = ucd.getNumericValue(codepoint);
        if (Double.isNaN(num))
          return null;
        return nf.format(num);
      }
    }.setMain("Numeric_Value", "nv", UnicodeProperty.NUMERIC, version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int cp) {
        if (!ucd.isRepresented(cp)) {
          return null;
        }
        if (cp == '\u1E9E') {
          System.out.println("@#$ debug");
        }
        String b = nfkc.normalize(ucd.getCase(cp, UCD_Types.FULL, UCD_Types.FOLD));
        String c = nfkc.normalize(ucd.getCase(b, UCD_Types.FULL, UCD_Types.FOLD));
        if (c.equals(b)) {
          return null;
        }
        String d = nfkc.normalize(ucd.getCase(b, UCD_Types.FULL, UCD_Types.FOLD));
        if (!d.equals(c)) {
          throw new IllegalArgumentException("Serious failure in FC_NFKC!!!");
        }
        return c;
      }

      public int getMaxWidth(boolean isShort) {
        return 14;
      }
    }.setMain("FC_NFKC_Closure", "FC_NFKC", UnicodeProperty.STRING, version)
    // .addName("FNC")
    );
    
    add(new UnicodeProperty.SimpleProperty() {
        public String _getValue(int cp) {
          if (!ucd.isRepresented(cp)) {
            return null;
          }
          String b = nfd.normalize(cp);
          if (b.codePointAt(0) == cp && b.length() == Character.charCount(cp)) {
            return null;
          }
          return b;
        }

        public int getMaxWidth(boolean isShort) {
          return 5;
        }
      }.setMain("toNFD", "toNFD", UnicodeProperty.EXTENDED_STRING, version)
      );

    add(new UnicodeProperty.SimpleProperty() {
        public String _getValue(int cp) {
          if (!ucd.isRepresented(cp)) {
            return null;
          }
          String b = nfc.normalize(cp);
          if (b.codePointAt(0) == cp && b.length() == Character.charCount(cp)) {
            return null;
          }
          return b;
        }

        public int getMaxWidth(boolean isShort) {
          return 5;
        }
      }.setMain("toNFC", "toNFC", UnicodeProperty.EXTENDED_STRING, version)
      );

    add(new SimpleIsProperty("isNFD", "isNFD", version, getProperty("toNFD"), false).setExtended());
    add(new SimpleIsProperty("isNFC", "isNFC", version, getProperty("toNFC"), false).setExtended());



    add(new UnicodeProperty.SimpleProperty() {
      UnicodeSet ignorable = null;
      public String _getValue(int cp) {
        if (!ucd.isRepresented(cp)) {
          return null;
        }
        // lazy eval
        if (ignorable == null) {
          ignorable = getProperty("DefaultIgnorableCodePoint").getSet(UCD_Names.YES);
        }
        if (ignorable.contains(cp)) {
          return "";
        }
        final String case1 = ucd.getCase(cp, UCD_Types.FULL, UCD_Types.FOLD);
        String b = nfkc.normalize(case1);
        if (equals(cp,b)) {
          return null;
        }
        String c = trans(b);
        if (c.equals(b)) {
          return c;
        }
        //System.out.println("NFKC_CF requires multiple passes:\tU+" + Utility.hex(cp) + "\t" + Default.ucd().getName(cp));
        String d = trans(c);
        if (d.equals(c)) {
          return d;
        }
        throw new IllegalArgumentException("NFKC_CF requires THREE passes:\tU+" + Utility.hex(cp) + "\t" + Default.ucd().getName(cp));
      }
      private String trans(String b) {
        String bb = removeFrom(b, ignorable);
        final String case2 = ucd.getCase(bb, UCD_Types.FULL, UCD_Types.FOLD);
        String c = nfkc.normalize(case2);
        return c;
      }
      public int getMaxWidth(boolean isShort) {
        return 14;
      }
    }.setMain("NFKC_Casefold", "NFKC_CF", UnicodeProperty.STRING, version));

    add(new SimpleIsProperty("Changes_When_NFKC_Casefolded", "CWKCF", version, getProperty("NFKC_Casefold"), false).setCheckUnassigned());

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        if (!nfd.isNormalized(codepoint))
          return UCD_Names.NO;
        else if (nfd.isTrailing(codepoint))
          throw new IllegalArgumentException("Internal Error!");
        else
          return UCD_Names.YES;
      }

      public int getMaxWidth(boolean isShort) {
        return 15;
      }
    }.setValues(LONG_YES_NO, YES_NO).swapFirst2ValueAliases().setMain("NFD_Quick_Check", "NFD_QC",
            UnicodeProperty.ENUMERATED, version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        if (!nfc.isNormalized(codepoint))
          return "No";
        else if (nfc.isTrailing(codepoint))
          return "Maybe";
        else
          return "Yes";
      }

      public int getMaxWidth(boolean isShort) {
        return 15;
      }
    }.setValues(LONG_YES_NO_MAYBE, YES_NO_MAYBE).swapFirst2ValueAliases().setMain(
            "NFC_Quick_Check", "NFC_QC", UnicodeProperty.ENUMERATED, version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        if (!nfkd.isNormalized(codepoint))
          return UCD_Names.NO;
        else if (nfkd.isTrailing(codepoint))
          throw new IllegalArgumentException("Internal Error!");
        else
          return UCD_Names.YES;
      }

      public int getMaxWidth(boolean isShort) {
        return 15;
      }
    }.setValues(LONG_YES_NO, YES_NO).swapFirst2ValueAliases().setMain("NFKD_Quick_Check",
            "NFKD_QC", UnicodeProperty.ENUMERATED, version));

    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        if (!nfkc.isNormalized(codepoint))
          return "No";
        else if (nfkc.isTrailing(codepoint))
          return "Maybe";
        else
          return "Yes";
      }

      public int getMaxWidth(boolean isShort) {
        return 15;
      }
    }.setValues(LONG_YES_NO_MAYBE, YES_NO_MAYBE).swapFirst2ValueAliases().setMain(
            "NFKC_Quick_Check", "NFKC_QC", UnicodeProperty.ENUMERATED, version));

    /*
     * add(new UnicodeProperty.SimpleProperty() { public String _getValue(int
     * codepoint) { if (!nfx.isNormalized(codepoint)) return NO; else if
     * (nfx.isTrailing(codepoint)) return MAYBE; else return ""; }
     * }.setMain("NFD_QuickCheck", "nv", UnicodeProperty.NUMERIC, version)
     * .setValues("<number>"));
     */

    // Now the derived properties
    if (DEBUG)
      System.out.println("Derived Properties");
    for (int i = 0; i < DerivedProperty.DERIVED_PROPERTY_LIMIT; ++i) {
      UCDProperty prop = DerivedProperty.make(i);
      if (prop == null)
        continue;
      if (!prop.isStandard())
        continue;
      String name = prop.getName();
      if (getProperty(name) != null) {
        if (DEBUG)
          System.out.println("Iterated Names: " + name + ", ALREADY PRESENT*");
        continue; // skip if already there
      }
      int type = prop.getValueType();
      if (i == UCD_Types.FC_NFKC_Closure)
        type = UnicodeProperty.STRING;
      else if (i == UCD_Types.FullCompExclusion)
        type = UnicodeProperty.BINARY;
      else
        type = remapUCDType(type);

      if (DEBUG)
        System.out.println(prop.getName());
      add(new UCDPropertyWrapper(prop, type, false));
    }

    // then the general stuff

    if (DEBUG)
      System.out.println("Other Properties");
    List names = new ArrayList();
    UnifiedProperty.getAvailablePropertiesAliases(names, ucd);
    Iterator it = names.iterator();
    while (it.hasNext()) {
      String name = (String) it.next();
      if (getProperty(name) != null) {
        if (DEBUG)
          System.out.println("Iterated Names: " + name + ", ALREADY PRESENT");
        continue; // skip if already there
      }
      if (DEBUG)
        System.out.println("Iterated Names: " + name);
      add(new ToolUnicodeProperty(name));
    }

    int compositeVersion = ucd.getCompositeVersion();
    if (compositeVersion >= 0x040000)
      add(new UnicodeProperty.UnicodeMapProperty() {
        {
          unicodeMap = new UnicodeMap();
          unicodeMap.setErrorOnReset(true);
          unicodeMap.put(0xD, "CR");
          unicodeMap.put(0xA, "LF");
          UnicodeProperty cat = getProperty("General_Category");
          UnicodeSet temp = cat.getSet("Line_Separator").addAll(cat.getSet("Paragraph_Separator"))
          .addAll(cat.getSet("Control")).addAll(cat.getSet("Format")).remove(0xD).remove(
                  0xA).remove(0x200C).remove(0x200D);
          unicodeMap.putAll(temp, "Control");
          UnicodeSet graphemeExtend = getProperty("Grapheme_Extend").getSet(UCD_Names.YES);
          unicodeMap.putAll(graphemeExtend, "Extend");
          unicodeMap
          .putAll(new UnicodeSet("[[\u0e30-\u0e3a\u0e45\u0eb0-\u0ebb]-[:cn:]]"), "Extend");
          UnicodeSet graphemePrepend = getProperty("Logical_Order_Exception").getSet(UCD_Names.YES);
          unicodeMap.putAll(graphemePrepend, "Prepend");
          unicodeMap.putAll(cat.getSet("Spacing_Mark").removeAll(unicodeMap.keySet("Extend")),
          "SpacingMark");
          UnicodeProperty hangul = getProperty("Hangul_Syllable_Type");
          unicodeMap.putAll(hangul.getSet("L"), "L");
          unicodeMap.putAll(hangul.getSet("V"), "V");
          unicodeMap.putAll(hangul.getSet("T"), "T");
          unicodeMap.putAll(hangul.getSet("LV"), "LV");
          unicodeMap.putAll(hangul.getSet("LVT"), "LVT");
          unicodeMap.setMissing("Other");
        }
      }.setMain("Grapheme_Cluster_Break", "GCB", UnicodeProperty.ENUMERATED, version)
      .addValueAliases(
              new String[][] { { "Control", "CN" }, { "Extend", "EX" },
                      { "Prepend", "PP" }, { "Other", "XX" }, { "SpacingMark", "SM" }, }, true)
                      .swapFirst2ValueAliases());

    if (compositeVersion >= 0x040000)
      add(new UnicodeProperty.UnicodeMapProperty() {
        {
          unicodeMap = new UnicodeMap();
          unicodeMap.setErrorOnReset(true);
          UnicodeProperty cat = getProperty("General_Category");
          //unicodeMap.put(0x200B, "Other");
          unicodeMap.putAll(new UnicodeSet("[\\u000D]"), "CR");
          unicodeMap.putAll(new UnicodeSet("[\\u000A]"), "LF");
          unicodeMap.putAll(new UnicodeSet("[\\u0085\\u000B\\u000C\\u000C\\u2028\\u2029]"),
          "Newline");
          unicodeMap.putAll(getProperty("Grapheme_Extend").getSet(UCD_Names.YES).addAll(
                  cat.getSet("Spacing_Mark")), "Extend");
          unicodeMap.putAll(cat.getSet("Format").remove(0x200C).remove(0x200D).remove(0x200B), "Format");
          UnicodeProperty script = getProperty("Script");
          unicodeMap
          .putAll(
                  script
                  .getSet("Katakana")
                  .addAll(
                          new UnicodeSet(
                          "[\u3031\u3032\u3033\u3034\u3035\u309B\u309C\u30A0\u30FC\uFF70]")),
          "Katakana"); // \uFF9E\uFF9F
          Object foo = unicodeMap.keySet("Katakana");
          // UnicodeSet graphemeExtend =
          // getProperty("Grapheme_Extend").getSet(UCD_Names.YES).remove(0xFF9E,0xFF9F);
          UnicodeProperty lineBreak = getProperty("Line_Break");
          unicodeMap.putAll(getProperty("Alphabetic").getSet(UCD_Names.YES).add(0x05F3).removeAll(
                  getProperty("Ideographic").getSet(UCD_Names.YES))
                  .removeAll(unicodeMap.keySet("Katakana"))
                  // .removeAll(script.getSet("Thai"))
                  // .removeAll(script.getSet("Lao"))
                  .removeAll(lineBreak.getSet("SA")).removeAll(script.getSet("Hiragana"))
                  .removeAll(unicodeMap.keySet("Extend")), "ALetter");
          unicodeMap
          .putAll(new UnicodeSet(
          "[\\u00B7\\u05F4\\u2027\\u003A\\u0387\\u0387\\uFE13\\uFE55\\uFF1A]"),
          "MidLetter");
          /*
           * 0387 ( · ) GREEK ANO TELEIA FE13 ( ︓ ) PRESENTATION FORM FOR
           * VERTICAL COLON FE55 ( ﹕ ) SMALL COLON FF1A ( ： ) FULLWIDTH COLON
           */
          unicodeMap.putAll(lineBreak.getSet("Infix_Numeric").add(0x066C).add(0xFE50).add(0xFE54)
                  .add(0xFF0C).add(0xFF1B).remove(0x002E).remove(0x003A).remove(0xFE13), "MidNum");
          /*
           * 066C ( ٬ ) ARABIC THOUSANDS SEPARATOR
           * 
           * FE50 ( ﹐ ) SMALL COMMA FE54 ( ﹔ ) SMALL SEMICOLON FF0C ( ， )
           * FULLWIDTH COMMA FF1B ( ； ) FULLWIDTH SEMICOLON
           */
          unicodeMap.putAll(new UnicodeSet(
          "[\\u0027\\u002E\\u2018\\u2019\\u2024\\uFE52\\uFF07\\uFF0E]"), "MidNumLet");

          unicodeMap.putAll(new UnicodeSet(lineBreak.getSet("Numeric")).remove(0x066C), "Numeric"); // .remove(0x387)
          unicodeMap.putAll(cat.getSet("Connector_Punctuation").remove(0x30FB).remove(0xFF65),
          "ExtendNumLet");
          // unicodeMap.putAll(graphemeExtend, "Other"); // to verify that none
          // of the above touch it.
          unicodeMap.setMissing("Other");
          // 0387 Wordbreak = Other → MidLetter
        }
      }.setMain("Word_Break", "WB", UnicodeProperty.ENUMERATED, version).addValueAliases(
              new String[][] { { "Format", "FO" }, { "Katakana", "KA" }, { "ALetter", "LE" },
                      { "MidLetter", "ML" }, { "MidNum", "MN" }, { "MidNumLet", "MB" },
                      { "MidNumLet", "MB" }, { "Numeric", "NU" }, { "ExtendNumLet", "EX" },
                      { "Other", "XX" }, { "Newline", "NL" } }, true).swapFirst2ValueAliases());

    if (compositeVersion >= 0x040000)
      add(new UnicodeProperty.UnicodeMapProperty() {
        {
          unicodeMap = new UnicodeMap();
          unicodeMap.setErrorOnReset(true);
          unicodeMap.putAll(new UnicodeSet("[\\u000D]"), "CR");
          unicodeMap.putAll(new UnicodeSet("[\\u000A]"), "LF");
          UnicodeProperty cat = getProperty("General_Category");
          unicodeMap.putAll(getProperty("Grapheme_Extend").getSet(UCD_Names.YES).addAll(
                  cat.getSet("Spacing_Mark")), "Extend");
          unicodeMap.putAll(new UnicodeSet("[\\u0085\\u2028\\u2029]"), "Sep");
          unicodeMap.putAll(cat.getSet("Format").remove(0x200C).remove(0x200D), "Format");
          unicodeMap.putAll(getProperty("Whitespace").getSet(UCD_Names.YES).removeAll(
                  unicodeMap.keySet("Sep")).removeAll(unicodeMap.keySet("CR")).removeAll(
                          unicodeMap.keySet("LF")), "Sp");
          UnicodeSet graphemeExtend = getProperty("Grapheme_Extend").getSet(UCD_Names.YES);
          unicodeMap.putAll(getProperty("Lowercase").getSet(UCD_Names.YES)
                  .removeAll(graphemeExtend), "Lower");
          unicodeMap.putAll(getProperty("Uppercase").getSet(UCD_Names.YES).addAll(
                  cat.getSet("Titlecase_Letter")), "Upper");
          UnicodeSet temp = getProperty("Alphabetic").getSet(UCD_Names.YES)
          // .add(0x00A0)
          .add(0x05F3).removeAll(unicodeMap.keySet("Lower")).removeAll(
                  unicodeMap.keySet("Upper")).removeAll(unicodeMap.keySet("Extend"));
          unicodeMap.putAll(temp, "OLetter");
          UnicodeProperty lineBreak = getProperty("Line_Break");
          unicodeMap.putAll(lineBreak.getSet("Numeric"), "Numeric");
          unicodeMap.putAll(new UnicodeSet("[\\u002E\\u2024\\uFE52\\uFF0E]"), "ATerm");
          unicodeMap.putAll(getProperty("STerm").getSet(UCD_Names.YES).removeAll(
                  unicodeMap.keySet("ATerm")), "STerm");
          unicodeMap.putAll(cat.getSet("Open_Punctuation").addAll(cat.getSet("Close_Punctuation"))
                  .addAll(lineBreak.getSet("Quotation")).remove(0x05F3).removeAll(
                          unicodeMap.keySet("ATerm")).removeAll(unicodeMap.keySet("STerm")),
          "Close");
          unicodeMap.putAll(new UnicodeSet("[\\u002C\\u3001\\uFE10\\uFE11\\uFF0C"
                  + "\\uFE50\\uFF64\\uFE51\\uFE51\\u055D\\u060C\\u060D\\u07F8\\u1802\\u1808" + // new
                  // from
                  // L2/08-029
                  "\\u003A\\uFE13\\uFF1A" + "\\uFE55" + // new from L2/08-029
                  // "\\u003B\\uFE14\\uFF1B" +
                  "\\u2014\\uFE31\\u002D\\uFF0D" + "\\u2013\\uFE32\\uFE58\\uFE63" + // new
                  // from
                  // L2/08-029
          "]"), "SContinue");
          // unicodeMap.putAll(graphemeExtend, "Other"); // to verify that none
          // of the above touch it.
          unicodeMap.setMissing("Other");
        }
      }.setMain("Sentence_Break", "SB", UnicodeProperty.ENUMERATED, version).addValueAliases(
              new String[][] { { "Sep", "SE" }, { "Format", "FO" }, { "Sp", "SP" },
                      { "Lower", "LO" }, { "Upper", "UP" }, { "OLetter", "LE" }, { "Numeric", "NU" },
                      { "ATerm", "AT" }, { "STerm", "ST" }, { "Extend", "EX" }, { "SContinue", "SC" },
                      { "Close", "CL" }, { "Other", "XX" }, }, false).swapFirst2ValueAliases());

    // ========================

    /*
     * # As defined by Unicode Standard Definition D120 # C has the Lowercase or
     * Uppercase property or has a General_Category value of Titlecase_Letter.
     */
    add(new SimpleBinaryProperty("Cased", "Cased", version, new UnicodeSet()
    .addAll(getProperty("Lowercase").getSet(UCD_Names.YES))
    .addAll(getProperty("Uppercase").getSet(UCD_Names.YES))
    .addAll(getProperty("GeneralCategory").getSet("Lt"))));

    /*
     * # As defined by Unicode Standard Definition
     * D121 # C is defined to be case-ignorable if C has the value MidLetter or
     * the value MidNumLet # for the Word_Break property or its General_Category
     * is one of # Nonspacing_Mark (Mn), Enclosing_Mark (Me), Format (Cf),
     * Modifier_Letter (Lm), or Modifier_Symbol (Sk).
     */
    add(new SimpleBinaryProperty("Case_Ignorable", "CI", version, new UnicodeSet()
    .addAll(getProperty("WordBreak").getSet("MidNumLet"))
    .addAll(getProperty("WordBreak").getSet("MidLetter"))
    .addAll(getProperty("GeneralCategory").getSet("Mn"))
    .addAll(getProperty("GeneralCategory").getSet("Me"))
    .addAll(getProperty("GeneralCategory").getSet("Cf"))
    .addAll(getProperty("GeneralCategory").getSet("Lm"))
    .addAll(getProperty("GeneralCategory").getSet("Sk"))));

    /*
     * Property: Is_Lowercased # As defined by Unicode Standard
     * Definition D124
     */
    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return changesWhenCased(codepoint, UCD_Types.LOWER);
      }
    }
    .setMain("Changes_When_Lowercased", "CWL", UnicodeProperty.BINARY, version)
    .swapFirst2ValueAliases());


    /* Property: Is_Uppercased # As defined by Unicode Standard
     * Definition D125
     */
    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return changesWhenCased(codepoint, UCD_Types.UPPER);
      }
    }
    .setMain("Changes_When_Uppercased", "CWU", UnicodeProperty.BINARY, version)
    .swapFirst2ValueAliases());


    /* Property: Is_Titlecased # As defined by Unicode Standard
     * Definition D126
     */
    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return changesWhenCased(codepoint, UCD_Types.TITLE);
      }
    }
    .setMain("Changes_When_Titlecased", "CWT", UnicodeProperty.BINARY, version)
    .swapFirst2ValueAliases());


    /* Property: Is_Casefolded # As defined by Unicode Standard
     * Definition D127
     */
    add(new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return changesWhenCased(codepoint, UCD_Types.FOLD);
      }
    }
    .setMain("Changes_When_Casefolded", "CWCF", UnicodeProperty.BINARY, version)
    .swapFirst2ValueAliases());


    /* Property: Is_Cased # As defined by Unicode Standard Definition
     * D128
     * isCased(X) when isLowercase(X) is false, or isUppercase(X) is false, or
isTitlecase(X) is false. 
     */
    add(new SimpleBinaryProperty("Changes_When_Casemapped", "CWCM", version, new UnicodeSet()
    .addAll(getProperty("Changes_When_Lowercased").getSet(UCD_Names.YES))
    .addAll(getProperty("Changes_When_Uppercased").getSet(UCD_Names.YES))
    .addAll(getProperty("Changes_When_Titlecased").getSet(UCD_Names.YES))));

    // ========================

    try {
      String x = Utility.getMostRecentUnicodeDataFile("ScriptExtensions", Default.ucdVersion(), true, true);
      if (x == null) {
        System.out.println("ScriptExtensions not available for version");
      } else {
        File f = new File(x);
        ScriptExtensions extensions = ScriptExtensions.make(f.getParent(), f.getName());
        Collection<BitSet> values = extensions.getAvailableValues();
        TreeSet<BitSet> sortedValues = new TreeSet<BitSet>(ScriptExtensions.COMPARATOR);
        sortedValues.addAll(values);
        UnicodeMap<String> umap = new UnicodeMap<String>();
        for (BitSet set : sortedValues) {
          UnicodeSet uset = extensions.getSet(set);
          umap.putAll(uset, ScriptExtensions.getNames(set, UProperty.NameChoice.SHORT, " "));
        }
        UnicodeMapProperty prop2 = new UnicodeMapProperty()
        .set(umap);
        prop2.setMain("Script_Extensions", "SE", UnicodeProperty.EXTENDED_ENUMERATED, version);
        prop2.addValueAliases(new String[][] {}, false); // hack
        //      for (BitSet set : sortedValues) {
        //        prop2.addValueAlias(ScriptExtensions.getNames(set, UProperty.NameChoice.SHORT, " "), 
        //                ScriptExtensions.getNames(set, UProperty.NameChoice.LONG, " "),
        //                false);
        //      }
        add(prop2);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } 
  }

  private void addFakeProperty(String version, int unicodePropertyType, String defaultValue, String name, String abbr, String... alts) {
    final SimpleProperty item = new UnicodeProperty.SimpleProperty() {
      public String _getValue(int codepoint) {
        return "";
      }
    };
    item.setValues(defaultValue);
    item.setMain(name, abbr, unicodePropertyType, version);
    for (String alt : alts) {
      item.addName(alt);
    }
    add(item);
  }

  static String[] YES_NO_MAYBE      = { "N", "M", "Y" };

  static String[] LONG_YES_NO_MAYBE = { "No", "Maybe", "Yes" };

  static String[] YES_NO            = { "N", "Y" };

  static String[] LONG_YES_NO       = { "No", "Yes" };

  /*
   * "Bidi_Mirroring_Glyph", "Block", "Case_Folding", "Case_Sensitive",
   * "ISO_Comment", "Lowercase_Mapping", "Name", "Numeric_Value",
   * "Simple_Case_Folding", "Simple_Lowercase_Mapping",
   * "Simple_Titlecase_Mapping", "Simple_Uppercase_Mapping",
   * "Titlecase_Mapping", "Unicode_1_Name", "Uppercase_Mapping", "isCased",
   * "isCasefolded", "isLowercase", "isNFC", "isNFD", "isNFKC", "isNFKD",
   * "isTitlecase", "isUppercase", "toNFC", "toNFD", "toNFKC", "toNKFD" });
   */

  /*
   * private class NameProperty extends UnicodeProperty.SimpleProperty {
   * {set("Name", "na", "<string>", UnicodeProperty.STRING);} public String
   * getPropertyValue(int codepoint) { if ((ODD_BALLS &
   * ucd.getCategoryMask(codepoint)) != 0) return null; return
   * ucd.getName(codepoint); } }
   */

  static class UCDPropertyWrapper extends UnicodeProperty {
    UCDProperty ucdProperty;

    boolean     yes_no_maybe;

    UCDPropertyWrapper(UCDProperty ucdProperty, int type, boolean yes_no_maybe) {
      this.ucdProperty = ucdProperty;
      setType(type);
      String name = ucdProperty.getName(UCDProperty.LONG);
      if (name == null)
        ucdProperty.getName(UCDProperty.SHORT);
      setName(name);
      this.yes_no_maybe = yes_no_maybe;
      setUniformUnassigned(false);
    }

    protected String _getVersion() {
      return ucdProperty.getUCD().getVersion();
    }

    protected String _getValue(int codepoint) {
      String result = ucdProperty.getValue(codepoint, UCDProperty.LONG);
      if (result.length() == 0) {
        return UCD_Names.NO;
      }
      return result;
    }

    protected List _getNameAliases(List result) {
      addUnique(ucdProperty.getName(UCDProperty.SHORT), result);
      String name = getName();
      addUnique(name, result);
      if (name.equals("White_Space"))
        addUnique("space", result);
      return result;
    }

    protected List _getValueAliases(String valueAlias, List result) {
      if (isType(BINARY_MASK)) {
        lookup(valueAlias, UCD_Names.YN_TABLE_LONG, UCD_Names.YN_TABLE, YNTF, result);
        // if (valueAlias.equals(UCD_Names.YES)) {
        // addUnique(UCD_Names.Y, result);
        // addUnique("True", result);
        // addUnique("T", result);
        // }
        // else if (valueAlias.equals(UCD_Names.NO)) {
        // addUnique(UCD_Names.N, result);
        // addUnique("False", result);
        // addUnique("F", result);
        // }
        addUnique(valueAlias, result);
      }
      if (yes_no_maybe) {
        lookup(valueAlias, UCD_Names.YN_TABLE_LONG, UCD_Names.YN_TABLE, YNTF, result);
        // if (valueAlias.equals("Yes"))
        // addUnique("Y", result);
        // else if (valueAlias.equals("No"))
        // addUnique("N", result);
        // else if (valueAlias.equals("Maybe"))
        // addUnique("M", result);
        // addUnique(valueAlias, result);
      }
      return result;
    }

    protected List _getAvailableValues(List result) {
      if (isType(BINARY_MASK)) {
        addUnique(UCD_Names.YES, result);
        addUnique(UCD_Names.NO, result);
      }
      if (yes_no_maybe) {
        addUnique("No", result);
        addUnique("Maybe", result);
        addUnique("Yes", result);
      }
      return result;
    }
  }

  static final int ODD_BALLS = (1 << UCD_Types.Cn) | (1 << UCD_Types.Cs) | (1 << UCD_Types.Co); // |
  // (1
  // <<
  // UCD.Cc)

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.ibm.icu.dev.test.util.UnicodePropertySource#getPropertyAliases(java
   * .util.Collection)
   */
  
  private static final Relation<String, String> ALIAS_JOINING_GROUP 
      = new Relation(new HashMap(), LinkedHashSet.class);
  static {
      ALIAS_JOINING_GROUP.put("Teh_Marbuta_Goal", "Hamza_On_Heh_Goal");
      ALIAS_JOINING_GROUP.freeze();
  }
  
  private class ToolUnicodeProperty extends UnicodeProperty {
    org.unicode.text.UCD.UCDProperty up;

    int                              propMask;

    static final int                 EXTRA_START = 0x10000;


    private ToolUnicodeProperty(String propertyAlias) {
      propMask = UnifiedProperty.getPropmask(propertyAlias, ucd);
      up = UnifiedProperty.make(propMask, ucd);
      if (up == null)
        throw new IllegalArgumentException("Not found: " + propertyAlias);
      if (propertyAlias.equals("Case_Fold_Turkish_I")) {
        System.out.println(propertyAlias + " " + getTypeName(getType()));
      }
      setType(getPropertyTypeInternal());
      setName(propertyAlias);
      //if (up.hasUnassigned || (propMask >> 8) == (UCD_Types.AGE >> 8) ) {
      // always skip
      setUniformUnassigned(false);
      //}
    }

    public List _getAvailableValues(List result) {
      if (result == null)
        result = new ArrayList();
      int type = getType() & CORE_MASK;
      if (type == STRING || type == MISC)
        result.add("<string>");
      else if (type == NUMERIC)
        result.add("<number>");
      else if (type == BINARY) {
        result.add(UCD_Names.YES);
        result.add(UCD_Names.NO);
      } else if (type == ENUMERATED || type == CATALOG) {
        byte style = UCD_Types.LONG;
        int prop = propMask >> 8;
  String temp = null;
  boolean titlecase = false;
  for (int i = 0; i < 256; ++i) {
    boolean check = false;
    try {
      switch (prop) {
      case UCD_Types.CATEGORY >> 8:
        temp = (ucd.getCategoryID_fromIndex((byte) i, style));
  break;
  case UCD_Types.COMBINING_CLASS >> 8:
    temp = (ucd.getCombiningClassID_fromIndex((short) i, style));
  break;
  case UCD_Types.BIDI_CLASS >> 8:
    temp = (ucd.getBidiClassID_fromIndex((byte) i, style));
  break;
  case UCD_Types.DECOMPOSITION_TYPE >> 8:
    temp = (ucd.getDecompositionTypeID_fromIndex((byte) i, style));
  // check = temp != null;
  break;
  case UCD_Types.NUMERIC_TYPE >> 8:
    temp = (ucd.getNumericTypeID_fromIndex((byte) i, style));
  titlecase = true;
  break;
  case UCD_Types.EAST_ASIAN_WIDTH >> 8:
    temp = (ucd.getEastAsianWidthID_fromIndex((byte) i, style));
  break;
  case UCD_Types.LINE_BREAK >> 8:
    temp = (ucd.getLineBreakID_fromIndex((byte) i, style));
  break;
  case UCD_Types.JOINING_TYPE >> 8:
    temp = (ucd.getJoiningTypeID_fromIndex((byte) i, style));
  break;
  case UCD_Types.JOINING_GROUP >> 8:
    temp = (ucd.getJoiningGroupID_fromIndex((byte) i, style));
  break;
  case UCD_Types.SCRIPT >> 8:
    temp = (ucd.getScriptID_fromIndex((byte) i, style));
  titlecase = true;
  if (UnicodeProperty.UNUSED.equals(temp))
    continue;
  if (temp != null)
    temp = UCharacter.toTitleCase(Locale.ENGLISH, temp, null);
  break;
  case UCD_Types.AGE >> 8:
    temp = (ucd.getAgeID_fromIndex((byte) i, style));
  break;
  case UCD_Types.HANGUL_SYLLABLE_TYPE >> 8:
    temp = (ucd.getHangulSyllableTypeID_fromIndex((byte) i, style));
  break;
  default:
    throw new IllegalArgumentException("Internal Error: " + prop);
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      continue;
    }
    if (check)
      System.out.println("Value: " + temp);
    if (temp != null && temp.length() != 0 && !temp.equals(UNUSED)) {
      result.add(Utility.getUnskeleton(temp, titlecase));
    }
    if (check)
      System.out.println("Value2: " + temp);
  }
  // if (prop == (UCD_Types.DECOMPOSITION_TYPE>>8)) result.add("none");
  // if (prop == (UCD_Types.JOINING_TYPE>>8)) result.add("Non_Joining");
  // if (prop == (UCD_Types.NUMERIC_TYPE>>8)) result.add("None");
      }
      return result;
    }

    public List _getNameAliases(List result) {
      if (result == null)
        result = new ArrayList();
      addUnique(Utility.getUnskeleton(up.getName(UCD_Types.SHORT), false), result);
      String longName = up.getName(UCD_Types.LONG);
      addUnique(Utility.getUnskeleton(longName, true), result);
      // hack
      if (longName.equals("White_Space"))
        addUnique("space", result);
      return result;
    }

    public List _getValueAliases(String valueAlias, List result) {
      if (result == null)
        result = new ArrayList();
      int type = getType() & CORE_MASK;
      if (type == STRING || type == MISC || type == NUMERIC) {
        UnicodeProperty.addUnique(valueAlias, result);
        return result;
      } else if (type == BINARY) {
        // UnicodeProperty.addUnique(valueAlias, result);
        return lookup(valueAlias, UCD_Names.YN_TABLE_LONG, UCD_Names.YN_TABLE, YNTF, result);
      } else if (type == ENUMERATED || type == CATALOG) {
        byte style = UCD_Types.LONG;
        int prop = propMask >> 8;
        boolean titlecase = false;
        for (int i = 0; i < 256; ++i) {
          try {
            switch (prop) {
            case UCD_Types.CATEGORY >> 8:
              return lookup(valueAlias, UCD_Names.LONG_GENERAL_CATEGORY,
                      UCD_Names.GENERAL_CATEGORY, UCD_Names.EXTRA_GENERAL_CATEGORY, result);
        case UCD_Types.COMBINING_CLASS >> 8:
          addUnique(String.valueOf(0xFF & Utility.lookup(valueAlias,
                  UCD_Names.LONG_COMBINING_CLASS, true)), result);
        return lookup(valueAlias, UCD_Names.LONG_COMBINING_CLASS,
                UCD_Names.COMBINING_CLASS, null, result);
        case UCD_Types.BIDI_CLASS >> 8:
          return lookup(valueAlias, UCD_Names.LONG_BIDI_CLASS, UCD_Names.BIDI_CLASS, null,
                  result);
        case UCD_Types.DECOMPOSITION_TYPE >> 8:
          lookup(valueAlias, UCD_Names.LONG_DECOMPOSITION_TYPE, FIXED_DECOMPOSITION_TYPE,
                  null, result);
        return lookup(valueAlias, UCD_Names.LONG_DECOMPOSITION_TYPE,
                UCD_Names.DECOMPOSITION_TYPE, null, result);
        case UCD_Types.NUMERIC_TYPE >> 8:
          return lookup(valueAlias, UCD_Names.LONG_NUMERIC_TYPE, UCD_Names.NUMERIC_TYPE,
                  null, result);
        case UCD_Types.EAST_ASIAN_WIDTH >> 8:
          return lookup(valueAlias, UCD_Names.LONG_EAST_ASIAN_WIDTH,
                  UCD_Names.EAST_ASIAN_WIDTH, null, result);
        case UCD_Types.LINE_BREAK >> 8:
          lookup(valueAlias, UCD_Names.LONG_LINE_BREAK, UCD_Names.LINE_BREAK, null, result);
        if (valueAlias.equals("Inseparable"))
          addUnique("Inseperable", result);
        // Inseparable; Inseperable
        return result;
        case UCD_Types.JOINING_TYPE >> 8:
          return lookup(valueAlias, UCD_Names.LONG_JOINING_TYPE, UCD_Names.JOINING_TYPE,
                  null, result);
        case UCD_Types.JOINING_GROUP >> 8:
          return lookup(valueAlias, UCD_Names.JOINING_GROUP, null, ALIAS_JOINING_GROUP, result);
        case UCD_Types.SCRIPT >> 8:
          return lookup(valueAlias, UCD_Names.LONG_SCRIPT, UCD_Names.SCRIPT,
                  UCD_Names.EXTRA_SCRIPT, result);
        case UCD_Types.AGE >> 8:
          return lookup(valueAlias, UCD_Names.AGE, null, null, result);
        case UCD_Types.HANGUL_SYLLABLE_TYPE >> 8:
          return lookup(valueAlias, UCD_Names.LONG_HANGUL_SYLLABLE_TYPE,
                  UCD_Names.HANGUL_SYLLABLE_TYPE, null, result);
        default:
          throw new IllegalArgumentException("Internal Error: " + prop);
            }
          } catch (ArrayIndexOutOfBoundsException e) {
            continue;
          }
        }
      }
      throw new ArrayIndexOutOfBoundsException("not supported yet");
    }

    public String _getValue(int codepoint) {
      byte style = UCD_Types.LONG;
      String temp = null;
      boolean titlecase = false;
      switch (propMask >> 8) {
      case UCD_Types.CATEGORY >> 8:
        temp = (ucd.getCategoryID_fromIndex(ucd.getCategory(codepoint), style));
      break;
      case UCD_Types.COMBINING_CLASS >> 8:
        temp = (ucd.getCombiningClassID_fromIndex(ucd.getCombiningClass(codepoint), style));
      // if (temp.startsWith("Fixed_")) temp = temp.substring(6);
      break;
      case UCD_Types.BIDI_CLASS >> 8:
        temp = (ucd.getBidiClassID_fromIndex(ucd.getBidiClass(codepoint), style));
      break;
      case UCD_Types.DECOMPOSITION_TYPE >> 8:
        temp = (ucd.getDecompositionTypeID_fromIndex(ucd.getDecompositionType(codepoint), style));
      if (temp == null || temp.length() == 0)
        temp = "none";
      break;
      case UCD_Types.NUMERIC_TYPE >> 8:
        temp = (ucd.getNumericTypeID_fromIndex(ucd.getNumericType(codepoint), style));
      titlecase = true;
      if (temp == null || temp.length() == 0)
        temp = "None";
      break;
      case UCD_Types.EAST_ASIAN_WIDTH >> 8:
        temp = (ucd.getEastAsianWidthID_fromIndex(ucd.getEastAsianWidth(codepoint), style));
      break;
      case UCD_Types.LINE_BREAK >> 8:
        temp = (ucd.getLineBreakID_fromIndex(ucd.getLineBreak(codepoint), style));
      break;
      case UCD_Types.JOINING_TYPE >> 8:
        temp = (ucd.getJoiningTypeID_fromIndex(ucd.getJoiningType(codepoint), style));
      if (temp == null || temp.length() == 0)
        temp = "Non_Joining";
      break;
      case UCD_Types.JOINING_GROUP >> 8:
        temp = (ucd.getJoiningGroupID_fromIndex(ucd.getJoiningGroup(codepoint), style));
      break;
      case UCD_Types.SCRIPT >> 8:
        temp = (ucd.getScriptID_fromIndex(ucd.getScript(codepoint), style));
      if (temp != null)
        temp = UCharacter.toTitleCase(Locale.ENGLISH, temp, null);
      titlecase = true;
      break;
      case UCD_Types.AGE >> 8:
        temp = getAge(codepoint);
      break;
      case UCD_Types.HANGUL_SYLLABLE_TYPE >> 8:
        temp = (ucd
                .getHangulSyllableTypeID_fromIndex(ucd.getHangulSyllableType(codepoint), style));
      break;
      }
      if (temp != null)
        return Utility.getUnskeleton(temp, titlecase);
      if (isType(BINARY_MASK)) {
        return up.hasValue(codepoint) ? "Yes" : "No";
      }
      throw new IllegalArgumentException("Failed to find value for " + Utility.hex(codepoint));
    }

    public String getAge(int codePoint) {
      if (codePoint == 0x1FFFE) {
        System.out.println("debug point");
      }
      if (needAgeCache) {
        for (int i = UCD_Types.AGE11; i < UCD_Types.LIMIT_AGE; ++i) {
          String version = UCD_Names.AGE_VERSIONS[i];
          if (version.compareTo(ucd.getVersion()) > 0) {
            break;
          }
          ucdCache[i] = UCD.make(version);
        }
        needAgeCache = false;
      }
      for (int i = UCD_Types.AGE11; i < UCD_Types.LIMIT_AGE; ++i) {
        if (ucdCache[i] == null) {
          break;
        }
        if (ucdCache[i].isAllocated(codePoint)) {
          return UCD_Names.AGE[i];
        }
      }
      return UCD_Names.AGE[UCD_Types.UNKNOWN];
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.icu.dev.test.util.UnicodePropertySource#getPropertyType()
     */
    private int getPropertyTypeInternal() {

      switch (propMask) {
      case UCD_Types.BINARY_PROPERTIES | UCD_Types.CaseFoldTurkishI:
      case UCD_Types.BINARY_PROPERTIES | UCD_Types.Non_break:
        return EXTENDED_BINARY;
      }

      switch (propMask >> 8) {
      case UCD_Types.SCRIPT >> 8:
      case UCD_Types.AGE >> 8:
        return CATALOG;
      }
      int mask = 0;
      if (!up.isStandard())
        mask = EXTENDED_MASK;
      return remapUCDType(up.getValueType()) | mask;
    }

    public String _getVersion() {
      return up.ucd.getVersion();
    }

  }

  private int remapUCDType(int result) {
    switch (result) {
    case UCD_Types.NUMERIC_PROP:
      result = UnicodeProperty.NUMERIC;
      break;
    case UCD_Types.STRING_PROP:
      result = UnicodeProperty.STRING;
      break;
    case UCD_Types.MISC_PROP:
      result = UnicodeProperty.STRING;
      break;
    case UCD_Types.CATALOG_PROP:
      result = UnicodeProperty.ENUMERATED;
      break;
    case UCD_Types.FLATTENED_BINARY_PROP:
    case UCD_Types.ENUMERATED_PROP:
      result = UnicodeProperty.ENUMERATED;
      break;
    case UCD_Types.BINARY_PROP:
      result = UnicodeProperty.BINARY;
      break;
    case UCD_Types.UNKNOWN_PROP:
    default:
      result = UnicodeProperty.STRING;
      // throw new IllegalArgumentException("Type: UNKNOWN_PROP");
    }
    return result;
  }

  static public boolean equals(int codepoint, String string) {
    return UTF16.valueOf(codepoint).equals(string);
  }

  static List<String> lookup(String valueAlias, String[] main, String[] aux,
          com.ibm.icu.dev.test.util.Relation<String, String> aux2, List result) {
    // System.out.println(valueAlias + "=>");
    // System.out.println("=>" + aux[pos]);
    if (aux != null) {
      int pos = 0xFF & Utility.lookup(valueAlias, main, true);
      UnicodeProperty.addUnique(aux[pos], result);
    }
    UnicodeProperty.addUnique(valueAlias, result);
    if (aux2 != null) {
      Set<String> xtra = aux2.getAll(valueAlias);
      if (xtra != null) {
        for (String extraItem : xtra) {
          UnicodeProperty.addUnique(extraItem, result);
        }
      }
    }
    return result;
  }

  /*
   * static class DerivedPropertyWrapper extends UnicodeProperty { UCDProperty
   * derivedProperty; UCD ucd;
   * 
   * DerivedPropertyWrapper(int derivedPropertyID, UCD ucd) { this.ucd = ucd;
   * derivedProperty = DerivedProperty.make(derivedPropertyID, ucd); } protected
   * String _getVersion() { return ucd.getVersion(); }
   * 
   * protected String _getValue(int codepoint) { return
   * derivedProperty.getValue(codepoint, UCD_Types.LONG); } protected List
   * _getNameAliases(List result) { if (result != null) result = new
   * ArrayList(1); addUnique(derivedProperty.getName(UCD_Types.SHORT), result);
   * addUnique(derivedProperty.getName(UCD_Types.LONG), result); return null; }
   * 
   * protected List _getValueAliases(String valueAlias, List result) { // TODO
   * Auto-generated method stub return null; } protected List
   * _getAvailableValues(List result) { // TODO Auto-generated method stub
   * return null; }
   * 
   * }
   */

  public static class IdnaInfo {
    public enum IdnaType {
      OK, DELETED, ILLEGAL, REMAPPED
    };

    private UCD                      ucdIdna = UCD.make();                      // latest
    private StringPrep               namePrep;
    private StringUCharacterIterator uci     = new StringUCharacterIterator("");

    IdnaInfo() throws IOException {
      namePrep = StringPrep.getInstance(StringPrep.RFC3491_NAMEPREP);
      // InputStream stream =
      // ICUData.getRequiredStream(ICUResourceBundle.ICU_BUNDLE+"/uidna.spp");
      // namePrep = new StringPrep(stream);
      // stream.close();
    }

    public IdnaType getIDNAType(int cp) {
      if (ucdIdna.isPUA(cp) || !ucdIdna.isAllocated(cp)) {
        return IdnaType.ILLEGAL;
      }
      if (cp == '-')
        return IdnaType.OK;
      String source = UTF16.valueOf(cp);
      uci.setText(source);
      StringBuffer outbuffer = null;
      try {
        outbuffer = namePrep.prepare(uci, IDNA.DEFAULT);
      } catch (StringPrepParseException e) {
        return IdnaType.ILLEGAL;
      } catch (Exception e) {
        System.out.println("Failure at: " + Utility.hex(cp));
        return IdnaType.ILLEGAL;
      }
      if (!TestData.equals(source, outbuffer)) {
        return IdnaType.REMAPPED;
      }
      return IdnaType.OK;
    }
  }

  static final Pattern WELL_FORMED_LANGUAGE_TAG = Pattern.compile("..."); // ...
  // is
  // ugly
  // mess
  // that
  // someone
  // supplies

  static boolean isWellFormedLanguageTag(String tag) {
    return WELL_FORMED_LANGUAGE_TAG.matcher(tag).matches();
  }

  public static final Relation<String, String> YNTF                     = new Relation(
          new TreeMap(),
          LinkedHashSet.class);
  static {
    YNTF.putAll("Yes", Arrays.asList(YES_VALUES));
    YNTF.putAll("No", Arrays.asList(NO_VALUES));
    YNTF.putAll("Maybe", Arrays.asList(MAYBE_VALUES));
  }

  private static final String[]                FIXED_DECOMPOSITION_TYPE = new String[UCD_Names.DECOMPOSITION_TYPE.length];
  static {
    for (int i = 0; i < UCD_Names.DECOMPOSITION_TYPE.length; ++i) {
      FIXED_DECOMPOSITION_TYPE[i] = Utility.getUnskeleton(UCD_Names.DECOMPOSITION_TYPE[i], true);
    }
  }

  static class SimpleBinaryProperty extends UnicodeProperty.SimpleProperty {
    UnicodeSet items;

    SimpleBinaryProperty(String name, String shortName, String version, UnicodeSet items) {
      this.items = items;
      setValues(LONG_YES_NO, YES_NO).swapFirst2ValueAliases();
      setMain(name, shortName, UnicodeProperty.BINARY, version);
    }

    protected String _getValue(int codepoint) {
      return items.contains(codepoint) ? UCD_Names.YES : UCD_Names.NO;
    }
  }

  static class SimpleIsProperty extends UnicodeProperty.SimpleProperty {
    private UnicodeProperty property;
    private boolean samePolarity;

    SimpleIsProperty(String name, String shortName, String version, UnicodeProperty property, boolean samePolarity) {
      this.property = property;
      setValues(LONG_YES_NO, YES_NO).swapFirst2ValueAliases();
      setMain(name, shortName, UnicodeProperty.BINARY, version);
      this.samePolarity = samePolarity;
    }

    protected String _getValue(int codepoint) {
      final String value = property.getValue(codepoint);
      return value == null ? samePolarity ? UCD_Names.YES : UCD_Names.NO // null means same value, by convention
              : equals(codepoint, value) == samePolarity ? UCD_Names.YES : UCD_Names.NO;
    }

    SimpleIsProperty setExtended() {
        setType(UnicodeProperty.EXTENDED_BINARY);
        return this;
    }
    
    SimpleIsProperty setCheckUnassigned() {
      setUniformUnassigned(false);
      return this;
    }
  }

  public String removeFrom(String b, UnicodeSet ignorable2) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < b.length();) {
      int end = ignorable2.matchesAt(b,i);
      if (end > i) {
        i = end;
      } else {
        result.append(b.charAt(i));
        ++i;
      }
    }
    return result.toString();
  }

  private String changesWhenCased(int codepoint, byte caseType) {
    String nfdCodepoint = nfd.normalize(codepoint);
    return !nfdCodepoint.equals(ucd.getCase(nfdCodepoint, UCD_Types.FULL, caseType)) 
    ? UCD_Names.YES
            : UCD_Names.NO;
  }
}
