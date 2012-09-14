package org.unicode.text.tools;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;

import com.ibm.icu.dev.util.ICUPropertyFactory;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.dev.util.UnicodeProperty.Factory;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class VerifyXmlUcd {
  public static final boolean USE_ICU = false;
  public static final boolean ABBREVIATED = true;
  private static Factory factory;
  
  static Factory getFactory() {
    if (factory == null) {
      factory = USE_ICU ? ICUPropertyFactory.make() : ToolUnicodePropertySource.make(Default.ucdVersion());
    }
    return factory;
  }
  
  public static void main(String[] args) throws IOException {
    try {
      //checkRegex();
      testFile(org.unicode.text.utility.Utility.WORKSPACE_DIRECTORY + "DATA/UCD/xml/ucd.nounihan.grouped.xml");
      // too many errors to test: testFile("C:/DATA/UCD/xml/ucd.nounihan.grouped.xml");
    } finally {
      System.out.println("DONE");
    }
  }
  
  private static void checkRegex() {
    Factory factory = getFactory();
    List<String> props = factory.getAvailableNames();
    for (String prop : props) {
      UnicodeProperty property = factory.getProperty(prop);
      String shortName = property.getFirstNameAlias();
      String patternString = PROP_REGEX.get(shortName);
      if (patternString == null) continue;
      Matcher matcher = Pattern.compile(patternString.replace("\\U0010FFFF","\uDBFF\uDFFF")).matcher("");
      System.out.format("Testing %s (%s) with /%s/\r\n", shortName, prop, patternString);
      try {
        Collection<String> values = property.getAvailableValues();
        if (values.iterator().next().equals("<string>")) {
          values = new TreeSet();
          for (int cp = 0; cp < 0x10FFFF; ++cp) {
            String value = property.getValue(cp);
            if (value == null) value = "";
            values.add(value);
          }
        }
        int maxFail = 100;
        for (String value : values) {
          if (!matcher.reset(value).matches()) {
            System.out.format("\tFails %s (%s)\r\n", Utility.hex(value), value);
            if (--maxFail < 0) break;
          }
        }
      } catch (RuntimeException e) {
        e.printStackTrace(System.out);
      }
    }
  }
  static Map<String,String> PROP_REGEX = new TreeMap();
  static {
    String cp = "[\\u0000-\\U0010FFFF]"; // "[\\x{0}-\\x{10FFFF}]"; 
    String name = "[A-Z0-9]+(([-\\ ]|\\ -|-\\ )[A-Z0-9]+)*";
    String bname = "[a-zA-Z0-9]+([_\\ ][a-zA-Z0-9]+)*";
    PROP_REGEX.put("nv", "-?[0-9]+\\.[0-9]+");  //  * nv        ; Numeric_Value
    PROP_REGEX.put("bmg", cp);  //    bmg       ; Bidi_Mirroring_Glyph
    PROP_REGEX.put("cf", cp+"+");  //    cf        ; Case_Folding
    PROP_REGEX.put("dm", cp+"+");  //    dm        ; Decomposition_Mapping
    PROP_REGEX.put("FC_NFKC", cp+"+");  //    FC_NFKC   ; FC_NFKC_Closure
    PROP_REGEX.put("lc", cp+"+");  //    lc        ; Lowercase_Mapping
    PROP_REGEX.put("scc", cp+"*");  //    scc       ; Special_Case_Condition
    PROP_REGEX.put("sfc", cp);  //    sfc       ; Simple_Case_Folding
    PROP_REGEX.put("slc", cp);  //    slc       ; Simple_Lowercase_Mapping
    PROP_REGEX.put("stc", cp);  //    stc       ; Simple_Titlecase_Mapping
    PROP_REGEX.put("suc", cp);  //    suc       ; Simple_Uppercase_Mapping
    PROP_REGEX.put("tc", cp+"+");  //    tc        ; Titlecase_Mapping
    PROP_REGEX.put("uc", cp+"+");  //    uc        ; Uppercase_Mapping
    PROP_REGEX.put("isc", name);  //    isc       ; ISO_Comment
    PROP_REGEX.put("na", "("+name + "|\\<control\\>)?");  //    na        ; Name
    PROP_REGEX.put("na1","("+name + "(\\ \\((CR|FF|LF|NEL)\\))?)?");  //    na1       ; Unicode_1_Name
    //PROP_REGEX.put("bmg", ".*");  //    URS       ; Unicode_Radical_Stroke
    PROP_REGEX.put("age", "([0-9]+\\.[0-9]|unassigned)");  //    age       ; Age
    PROP_REGEX.put("blk", bname);  //    blk       ; Block
    PROP_REGEX.put("sc", bname);  //    sc        ; Script
  }
  
  
  
  
  private static void testFile(String file) throws IOException {
    System.out.format("\r\nTesting: %s\r\n\r\n", file);
    final File file2 = new File(file);
    if (!file2.canRead()) {
      System.out.println("Can't read " + file2.getCanonicalPath());
      System.out.println("Current Location: " + new File(".").getCanonicalPath());
    }
    XMLFileReader xmlFileReader = new XMLFileReader();
    MySimpleHandler handler = new MySimpleHandler();
    xmlFileReader.setHandler(handler);
    xmlFileReader.read(file,XMLFileReader.CONTENT_HANDLER,false);
    handler.showSummary();
  }
  
  static Set codepoints = getSet("char", "reserved", "noncharacter", "surrogate");
  static Set skipProperties = getSet("cp", "first-cp", "last-cp");
  //static Set hexValue = getSet("slc", "bmg", "lc", "stc", "suc", "tc", "uc");
  
  static Set<String> getSet(String ... strings) {
    return Collections.unmodifiableSet(new HashSet(Arrays.asList(strings)));  
  }
  
  static class MySimpleHandler extends XMLFileReader.SimpleHandler {
    int maxCount = Integer.MAX_VALUE; // ;
    int pathNumber = 0;
    XPathParts parser = new XPathParts();
    UnicodeSet seenSoFar = new UnicodeSet();
    Factory factory = getFactory();
    // test prop
    private Set accummulatedProperties = new HashSet();
    private Set accummulatedBlocks = new HashSet();
    private Set unhandledElements = new HashSet();
    
    MySimpleHandler() {
      String test = factory.getProperty("lb").getValue(0x10EA0,true);
      System.out.println("test: " + test);
    }
    
    public void showSummary() {
      if (!unhandledElements.contains("blocks")) {
        accummulatedProperties.add("blk");
      }
      
      Set core = new HashSet();
      List<String> rawCore = factory.getAvailableNames();
      for (String s : rawCore) {
        UnicodeProperty p = factory.getProperty(s);
        if ((p.getType() & UnicodeProperty.EXTENDED_MASK) != 0) continue;
        core.add(p.getFirstNameAlias());
      }
      showDifference("core", core, "xml_core", accummulatedProperties);
      showDifference("blocks", Default.ucd().getBlockNames(), "xml_blocks", accummulatedBlocks);
      System.out.format("unhandled elements: %s\r\n", unhandledElements); 
      System.out.format("skipped properties: %s\r\n", bogusPropertiesSkipped);
    }
    
    private void showDifference(String title1, Collection core, String title2, Collection accummulatedProperties) {
      Set core_minus_xml = new HashSet(core);
      core_minus_xml.removeAll(accummulatedProperties);
      Set xml_minus_core = new HashSet(accummulatedProperties);
      xml_minus_core.removeAll(core);
      System.out.format(title1 + " - " + title2 + " %s\r\n", core_minus_xml);
      System.out.format(title2 + " - " + title1 + ": %s\r\n", xml_minus_core);
    }
    
    @Override
    public void handlePathValue(String path, String value) {
      try {
        if ((++pathNumber % 1000) == 0) {
          System.out.println("*** path: " + pathNumber);
        }
        if (path.equals("//ucd/description")) return;
        if (value.length() != 0) {
          throw new IllegalArgumentException(String.format("non-empty value: %s, %s", value, path));
        }
        parser.set(path);
        String finalElement = parser.getElement(-1);
        
        if (codepoints.contains(finalElement)) {
          if (--maxCount <= 0) return;
          Map attributes = parser.getAttributes(-1);
          Map groupAttributes = parser.getElement(-2).equals("group") ? parser.getAttributes(-2) : null;
          if (groupAttributes != null) {
            groupAttributes.putAll(attributes); // add, possibly overriding
            attributes = groupAttributes;
          }
          checkAttributes(path, finalElement, attributes);
        } else if (finalElement.equals("block")) {
          Map<String,String> attributes = parser.getAttributes(-1);
          int cpStart = Integer.parseInt(attributes.get("first-cp"),16);
          int cpEnd = Integer.parseInt(attributes.get("last-cp"),16);
          String blockName = attributes.get("name");
          accummulatedBlocks.add(UnicodeProperty.regularize(blockName,true));
          UnicodeSet xmlBlock = new UnicodeSet(cpStart, cpEnd);
          UnicodeSet toolBlock = Default.ucd().getBlockSet(blockName, new UnicodeSet());
          if (!xmlBlock.equals(toolBlock)) {
            System.out.format("blocks differ: %s, %s != %s\r\n", blockName, xmlBlock, toolBlock);
          } 
        } else {
          unhandledElements.add(finalElement);
        }
      } catch (Exception e) {
        throw (RuntimeException) new IllegalArgumentException("Failure at: " + path).initCause(e);
      }
    }
    
    private void checkAttributes(String path, String element, Map<String, String> attributes) {
      // get the start and end of the range. Would be slightly simpler if first-cp were just cp.
      String cpStartString = attributes.get("cp");
      String cpEndString = null;
      if (cpStartString == null) {
        cpStartString = attributes.get("first-cp");
        cpEndString = attributes.get("last-cp");
      }
      int cpStart = Integer.parseInt(cpStartString,16);
      int cpEnd = cpEndString == null ? cpStart : Integer.parseInt(cpEndString,16);
      if (ABBREVIATED && !element.equals("char")) {
        if (cpEnd > cpStart + 10) cpEnd = cpStart + 10;
      }
      
      // loop over the items, checking the property values
      for (int cp = cpStart; cp <= cpEnd; ++cp) {
        checkAttributes(path, cp, attributes);
      }
      seenSoFar.add(cpStart, cpEnd);
    }
    
    void checkAttributes(String path, int cp, Map<String, String> attributes) {
      boolean okSoFar = true;
      for (String property : attributes.keySet()) {
        if (skipProperties.contains(property)) continue;
        accummulatedProperties.add(property);
        String value = attributes.get(property);
        String originalValue = value;
        
        if (value.indexOf('#') >= 0) {
          // fix up names
          value = value.replace("#", Utility.hex(cp));
          // hack for bad names:
          //value = value.replace("CJK UNIFIED IDEOGRAPHS-", "CJK UNIFIED IDEOGRAPH-");
        }
        
        UnicodeProperty toolProperty = factory.getProperty(property);
        
        String toolValue = matchEricsValues(cp, property, toolProperty);
        if (toolValue == null) continue; // for now
        
        if (!value.equals(toolValue)) {
//        if (property.equals("dm") && Default.ucd().isHangulSyllable(cp)) {
//        // skip
//        } else 
          {
            if (okSoFar) {
              System.out.println(path);
              okSoFar = false;
            }
            System.out.println("cp=" + Utility.hex(cp) + ", " + property + "=<" + value + "> (" + originalValue + ") != <" + toolValue + ">");  
          }
        }
      }
    }
    
    static Set defaultSame = getSet("dm", "bmg",  
        "cf", "lc", "tc", "uc", 
        "sfc", "slc", "stc", "suc");
    static Set bogusProperties = getSet("dm", "ccf", "jamo", "fcf", "tcf", "isc");
    static Set bogusPropertiesSkipped = new HashSet();
    
    private String matchEricsValues(int cp, String property, UnicodeProperty toolProperty) {
      
      if (cp == 0xD7A4 && property.equals("lb")) {
        System.out.println("debug");
      }
      
      // bad property names
      if (bogusProperties.contains(property)) {
        bogusPropertiesSkipped.add(property);
        return null;
      }
      
//      if (property.equals("isc")) {
//        return null; // skip for now, I don't support this property yet.
//      }
      
      // get my values
      if (toolProperty == null) return USE_ICU ? null : "MISSING";
      String toolValue = toolProperty.getValue(cp, true);
      if (toolValue == null) return USE_ICU ? null : ""; // for ICU, only test a subset
      int type = toolProperty.getType();
      
      // differences in values
      
//    if (property.equals("Comp_Ex") || property.equals("CE")) {
//    // all of Eric's values appear wrong. example: cp=0000, Comp_Ex=<y> != <n>
//    // http://unicode.org/Public/UNIDATA/CompositionExclusions.txt
//    // so skip them for now, since they muddy up the list
//    return null;
//    }
      
      //if (type == UnicodeProperty.STRING) {
        // UCD marks no change with "". I reflect the full value, Eric doesn't.
        // however, this is tricky, so I'm still playing with it to get them to match up.
        //if (UTF16.valueOf(cp).equals(toolValue)) {
          //toolValue = "#";
//        } else if (property.equals("lc") || property.equals("uc") || property.equals("tc")) {
//          String basePropertyName = property.equals("tc") ? "uc" : "s" + property;
//          UnicodeProperty baseProperty = factory.getProperty(basePropertyName);
//          String baseValue = baseProperty.getValue(cp, true);
//          if (toolValue.equals(baseValue)) {
//            toolValue = "";
//          } else if (property.equals("tc")){
//            
//          }
        //}
      //}
      
      // differences in format
      
      if (type == UnicodeProperty.BINARY) {
        // Different choices for booleans. We don't make this clear in PropertyValueAliases
        if ("F".equals(toolValue)) toolValue = "N";
        else if ("False".equals(toolValue)) toolValue = "N";
        else if ("T".equals(toolValue)) toolValue = "Y";
        else if ("True".equals(toolValue)) toolValue = "Y";
      }
      
      if (//property.endsWith("_QC") 
          // these are uppercase in in PropertyValueAliases
          //|| 
          property.equals("dt")) 
        // these are not uppercase in in PropertyValueAliases, so Eric is right
      {
        toolValue = toolValue.toLowerCase(Locale.ENGLISH);        
      }
      
      if (type == UnicodeProperty.STRING) {
        // Eric is using hex strings, no disagreement
        String cpString = UTF16.valueOf(cp);
        if (cpString.equals(toolValue) && property.equals("bmg")) {
            toolValue = "";
        } else {
          toolValue = org.unicode.text.utility.Utility.hex(toolValue, " ");
        }
        //if (property.equals("FC_NFKC")) toolValue = toolValue.toLowerCase(); // but formatting problem here, should be uppercase
      }
      
      if (property.equals("nv")) {
        // I'm using the format in http://unicode.org/Public/UNIDATA/extracted/DerivedNumericValues.txt
        toolValue = dumbFraction(toolValue);
      }
      
      
      return toolValue;
    }
    
    // quick and dirty fractionator
    private String dumbFraction(String toolValue) {
      if (toolValue.equals("0.0")) return "0";
      double value = Double.parseDouble(toolValue);
      for (int i = 1; i < 20; ++i) {
        double numerator = value * i;
        long rounded = (long)Math.round(numerator);
        if (Math.abs(numerator - rounded) < 0.000001d) {
          if (i == 1) return rounded + "";
          return rounded + "/" + i;
        }
      }
      return toolValue;
    }
  }
}