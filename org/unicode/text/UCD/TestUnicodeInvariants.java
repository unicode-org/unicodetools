package org.unicode.text.UCD;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.ICUPropertyFactory;
import com.ibm.icu.dev.test.util.Tabber;
import com.ibm.icu.dev.test.util.UnicodeLabel;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.dev.test.util.Tabber.HTMLTabber;
import com.ibm.icu.dev.test.util.UnicodeProperty.Factory;
import com.ibm.icu.dev.test.util.UnicodeProperty.PatternMatcher;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class TestUnicodeInvariants {

  private static final Pattern IN_PATTERN = Pattern.compile("(.*)([≠=])(.*)");
  private static final boolean ICU_VERSION = false; // ignore the versions if this is true
  private static final String LATEST_VERSION = "5.2.0"; // UCD.latestVersion;
  private static final Factory LATEST_PROPS = getProperties(LATEST_VERSION);
  private static final String LAST_VERSION = "5.1.0"; // UCD.lastVersion;
  private static int showRangeLimit = 20;
  static boolean doHtml = true;

  private static final int
  HELP1 = 0,
  FILE = 1,
  RANGE = 2,
  TABLE = 3
  ;

  private static final UOption[] options = {
    UOption.HELP_H(),
    UOption.create("file", 'f', UOption.REQUIRES_ARG),
    UOption.create("norange", 'n', UOption.NO_ARG),
    UOption.create("table", 't', UOption.NO_ARG),
  };


  public static void main(String[] args) throws IOException {
    UOption.parseArgs(args, options);

    String file = "UnicodeInvariantTest.txt";
    if (options[FILE].doesOccur) file = options[FILE].value;
    doHtml = options[TABLE].doesOccur;

    doRange = !options[RANGE].doesOccur;
    System.out.println("File:\t" + file);
    System.out.println("Ranges?\t" + doRange);

    System.out.println("HTML?\t" + doHtml);

    testInvariants(file, doRange);
  }

  static Transliterator toHTML;
  static {

    String BASE_RULES = "'<' > '&lt;' ;" + "'<' < '&'[lL][Tt]';' ;"
    + "'&' > '&amp;' ;" + "'&' < '&'[aA][mM][pP]';' ;"
    + "'>' < '&'[gG][tT]';' ;" + "'\"' < '&'[qQ][uU][oO][tT]';' ; "
    + "'' < '&'[aA][pP][oO][sS]';' ; ";

    String CONTENT_RULES = "'>' > '&gt;' ;";

    String HTML_RULES = BASE_RULES + CONTENT_RULES + "'\"' > '&quot;' ; ";

    String HTML_RULES_CONTROLS = HTML_RULES
    + "[[:di:]-[:cc:]-[:cs:]-[\\u200E\\u200F]] > ; " // remove, should ignore in rendering (but may not be in browser)
    + "[[:nchar:][:cn:][:cs:][:co:][:cc:]-[:whitespace:]-[\\u200E\\u200F]] > \\uFFFD ; "; // should be missing glyph (but may not be in browser)
    //     + "([[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:]-[\\u0020]]) > &hex/xml($1) ; "; // [\\u0080-\\U0010FFFF]

    toHTML = Transliterator.createFromRules("any-xml", HTML_RULES_CONTROLS,
            Transliterator.FORWARD);
  }

  enum Expected {empty, not_empty, irrelevant};

  static final UnicodeSet INVARIANT_RELATIONS = new UnicodeSet("[=\u2282\u2283\u2286\u2287∥≉]");
  static final ParsePosition pp = new ParsePosition(0);

  public static void testInvariants(String outputFile, boolean doRange) throws IOException {
    boolean showScript = false;
    PrintWriter out2 = BagFormatter.openUTF8Writer(UCD_Types.GEN_DIR, "UnicodeTestResults." + (doHtml ? "html" : "txt"));
    StringWriter writer = new StringWriter();
    out = new PrintWriter(writer);
    if (doHtml) {
      out.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
      out.println("<link rel='stylesheet' type='text/css' href='UnicodeTestResults.css'>");
      out.println("<title>Unicode Property Results</title>");
      out.println("</head><body><h1>#Unicode Invariant Results</h1>");
    } else {
      out.write('\uFEFF'); // BOM
    }
    BufferedReader in = BagFormatter.openUTF8Reader("org/unicode/text/UCD/", outputFile);
    final HTMLTabber tabber = new Tabber.HTMLTabber();

    errorLister = new BagFormatter()
    .setMergeRanges(doRange)
    .setLabelSource(null)
    .setUnicodePropertyFactory(LATEST_PROPS)
    //.setTableHtml("<table class='e'>")
    .setShowLiteral(toHTML)
    .setFixName(toHTML);
    errorLister.setShowTotal(false);
    if (doHtml) {
      errorLister.setTabber(tabber);
    }

    showLister = new BagFormatter()
    .setMergeRanges(doRange)
    //.setLabelSource(null)
    .setUnicodePropertyFactory(LATEST_PROPS)
    //.setTableHtml("<table class='s'>")
    .setShowLiteral(toHTML);
    showLister.setShowTotal(false);
    if (showScript) {
      showLister.setValueSource(LATEST_PROPS.getProperty("script"));
    }
    if (doHtml) {
      showLister.setTabber(tabber);
    }

    //symbolTable = new ChainedSymbolTable();
    //      new ChainedSymbolTable(new SymbolTable[] {
    //            ToolUnicodePropertySource.make(UCD.lastVersion).getSymbolTable("\u00D7"),
    //            ToolUnicodePropertySource.make(Default.ucdVersion()).getSymbolTable("")});
    parseErrorCount = 0;
    testFailureCount = 0;
    while (true) {
      String line = in.readLine();
      if (line == null) break;
      try {
        if (line.startsWith("\uFEFF")) line = line.substring(1);
        println(line);
        line = line.trim();
        int pos = line.indexOf('#');
        if (pos >= 0) {
          line = line.substring(0,pos).trim();
        }
        if (line.length() == 0) continue;
        if (line.equalsIgnoreCase("Stop")) {
          break;
        } else if (line.startsWith("Let")) {
          letLine(pp, line);
        } else if (line.startsWith("In")) {
          inLine(pp, line);
        } else if (line.startsWith("ShowScript")) {
          showScript = true;
        } else if (line.startsWith("HideScript")) {
          showScript = false;
        } else if (line.startsWith("Show")) {
          showLine(line, pp);
        } else {
          testLine(line, pp);
        }
      } catch (Exception e) {
        parseErrorCount = parseError(parseErrorCount, line, e);
        continue;
      }
    }
    println();
    println("**** SUMMARY ****");
    println();
    println("# ParseErrorCount=" + parseErrorCount);
    System.out.println("ParseErrorCount=" + parseErrorCount);
    println("# TestFailureCount=" + testFailureCount);
    System.out.println("TestFailureCount=" + testFailureCount);
    if (doHtml) {
      out.println("</body></html>");
    }
    out2.append(writer.getBuffer());
    out2.close();
    out.close();
  }

  static class PropertyComparison {
    UnicodeSet valueSet;
    UnicodeProperty property1;
    boolean shouldBeEqual;
    UnicodeProperty property2;
  }

  private static void inLine(ParsePosition pp, String line) throws ParseException {
    pp.setIndex(2);
    PropertyComparison propertyComparison = getPropertyComparison(pp, line);
    UnicodeMap failures = new UnicodeMap();

    for (UnicodeSetIterator it = new UnicodeSetIterator(propertyComparison.valueSet); it.next();) {
      String value1 = propertyComparison.property1.getValue(it.codepoint);
      String value2 = propertyComparison.property2.getValue(it.codepoint);
      final boolean areEqual = equals(value1, value2);
      if (areEqual != propertyComparison.shouldBeEqual) {
        failures.put(it.codepoint, value1 + (areEqual ? "=" : "≠") + value2);
      }
    }
    final UnicodeSet failureSet = failures.keySet();
    final int failureCount = failureSet.size();
    if (failureCount != 0) {
      testFailureCount++;      
      printErrorLine("Test Failure", Side.START, testFailureCount);
      println("## Got unexpected " + (propertyComparison.shouldBeEqual ? "differences" : "equalities") + ": " + failureCount);
      UnicodeLabel failureProp = new UnicodeProperty.UnicodeMapProperty().set(failures);
      errorLister.setValueSource(failureProp);
      errorLister.showSetNames(out, failureSet);
      errorLister.setValueSource((UnicodeLabel)null);
      printErrorLine("Test Failure", Side.END, testFailureCount);
      println();
    }
  }

  private static PropertyComparison getPropertyComparison(ParsePosition pp, String line) throws ParseException {
    PropertyComparison propertyComparison = new PropertyComparison();

    propertyComparison.valueSet = new UnicodeSet(line, pp, symbolTable);
    propertyComparison.property1 = CompoundProperty.of(LATEST_PROPS, line, pp);
    int cp = line.codePointAt(pp.getIndex());
    if (cp != '=' && cp != 'x') {
      throw new ParseException(line, pp.getIndex());
    }
    propertyComparison.shouldBeEqual = cp == '=';
    pp.setIndex(pp.getIndex()+1);
    propertyComparison.property2 = CompoundProperty.of(LATEST_PROPS, line, pp);
    scan(PATTERN_WHITE_SPACE, line, pp, true);
    if (pp.getIndex() != line.length()) {
      throw new ParseException(line, pp.getIndex());
    }
    return propertyComparison;
  }

  private static boolean equals(Object value1, Object value2) {
    if (value1 == null) {
      return value2 == null;
    } else if (value2 == null) {
      return false;
    }
    return value1.equals(value2);
  }

  static class CompoundProperty extends UnicodeProperty {
    static class FilterOrProp {
      enum Type {filter, prop, stringprop};
      private Type type;
      private UnicodeProperty prop;
      private UnicodeSet filter;
    }
    private static final UnicodeSet PROPCHARS = new UnicodeSet("[a-zA-Z0-9\\:\\-\\_\\u0020\\p{pattern white space}]");
    private List<FilterOrProp> propOrFilters = new ArrayList<FilterOrProp>();

    static UnicodeProperty of(UnicodeProperty.Factory propSource, String line, ParsePosition pp) {
      CompoundProperty result = new CompoundProperty();
      while (true) {
        scan(PATTERN_WHITE_SPACE, line, pp, true);
        if (UnicodeSet.resemblesPattern(line, pp.getIndex())) {
          FilterOrProp propOrFilter = new FilterOrProp();
          propOrFilter.filter = parseUnicodeSet(line, pp);
          propOrFilter.type = FilterOrProp.Type.filter;
          result.propOrFilters.add(propOrFilter);
        } else {
          String propName = scan(PROPCHARS, line, pp, true);
          if (propName.length() > 0) {
            FilterOrProp propOrFilter = new FilterOrProp();
            VersionedProperty xprop = new VersionedProperty().set(propName);
            propOrFilter.prop = xprop.property;
            if (propOrFilter.prop == null) {
              throw new IllegalArgumentException("Can't create property for: " + propName);
            }
            propOrFilter.type = propOrFilter.prop.getType() != UnicodeProperty.STRING 
            ? FilterOrProp.Type.prop : FilterOrProp.Type.stringprop;
            result.propOrFilters.add(propOrFilter);
          } else {
            break;
          }
        }
        scan(PATTERN_WHITE_SPACE, line, pp, true);
        int pos = pp.getIndex();
        if (pos == line.length()) break;
        int cp = line.charAt(pos);
        if (cp != '*') break;
        pp.setIndex(pos+1);
        // keep looping
      }
      return result;
    }

    private CompoundProperty() {
    }

    @Override
    protected List _getAvailableValues(List result) {
      return propOrFilters.get(0).prop.getAvailableValues(result);
    }

    @Override
    protected List _getNameAliases(List result) {
      StringBuffer name = new StringBuffer();
      for (int i = 0; i < propOrFilters.size(); ++i) {
        if (i != 0) {
          name.append("*");
        }
        name.append(propOrFilters.get(i).prop.getFirstNameAlias());
      }
      result.add(name.toString());
      return result;
    }

    @Override
    protected String _getValue(int codepoint) {
      StringBuffer buffer = new StringBuffer();
      String value = UTF16.valueOf(codepoint);
      int cp;

      for (int i = propOrFilters.size() - 1; i >= 0; --i) {
        final FilterOrProp propOrFilter = propOrFilters.get(i);
        switch (propOrFilter.type) {
          case filter:
            buffer.setLength(0);
            for (int j = 0; j < value.length(); j += UTF16.getCharCount(cp)) {
              cp = UTF16.charAt(value,j);
              if (!propOrFilter.filter.contains(cp)) continue;
              buffer.appendCodePoint(cp);
            }
            value = buffer.toString();
            break;
          case stringprop:
            buffer.setLength(0);
            for (int j = 0; j < value.length(); j += UTF16.getCharCount(cp)) {
              cp = UTF16.charAt(value,j);
              String value2 = propOrFilter.prop.getValue(cp);
              buffer.append(value2);
            }
            value = buffer.toString();
            break;
          case prop:
            LinkedHashSet<String> values = new LinkedHashSet<String>();
            for (int j = 0; j < value.length(); j += UTF16.getCharCount(cp)) {
              cp = UTF16.charAt(value,j);
              String value2 = propOrFilter.prop.getValue(cp);
              values.add(value2);
            }
            if (values.size() == 0) {
              value = "";
            } else if (values.size() == 1) {
              value = values.iterator().next();
            } else {
              value = values.toString();
            }
        }
      }
      return value;
    }

    @Override
    protected List _getValueAliases(String valueAlias, List result) {
      return propOrFilters.get(0).prop.getAvailableValues(result);
    }

    @Override
    protected String _getVersion() {
      return propOrFilters.get(0).prop.getVersion();
    }

  }

  private static void letLine(ParsePosition pp, String line) {
    int x = line.indexOf('=');
    final String variable = line.substring(3,x).trim();
    if (!variable.startsWith("$")) {
      throw new IllegalArgumentException("Variable must begin with '$': ");
    }
    final String value = line.substring(x+1).trim();
    pp.setIndex(0);
    UnicodeSet valueSet = new UnicodeSet("[" + value + "]", pp, symbolTable);
    valueSet.complement().complement();

    symbolTable.add(variable.substring(1), valueSet.toPattern(false));
    if (false) System.out.println("Added variable: <" + variable + "><" + value + ">");
    showSet(pp, value);
  }

  private static void showLine(String line, ParsePosition pp) {
    String part = line.substring(4).trim();
    if (part.startsWith("Each")) {
      part = part.substring(4).trim();
      showLister.setMergeRanges(false);
    }
    showSet(pp, part);
    showLister.setMergeRanges(doRange);
  }

  private static void testLine(String line, ParsePosition pp) throws ParseException {
    if (line.startsWith("Test")) {
      line = line.substring(4).trim();
    }

    char relation = 0;
    String rightSide = null;
    String leftSide = null;
    UnicodeSet leftSet = null;
    UnicodeSet rightSet = null;

    pp.setIndex(0);
    leftSet = new UnicodeSet(line, pp, symbolTable);
    leftSide = line.substring(0,pp.getIndex());
    scan(PATTERN_WHITE_SPACE, line, pp, true);
    relation = line.charAt(pp.getIndex());
    if (!INVARIANT_RELATIONS.contains(relation)) {
      throw new ParseException("Invalid relation, must be one of " + INVARIANT_RELATIONS.toPattern(false),
              pp.getIndex());
    }
    pp.setIndex(pp.getIndex()+1); // skip char
    scan(PATTERN_WHITE_SPACE, line, pp, true);
    int start = pp.getIndex();
    rightSet = new UnicodeSet(line, pp, symbolTable);
    rightSide = line.substring(start,pp.getIndex());
    scan(PATTERN_WHITE_SPACE, line, pp, true);
    if (line.length() != pp.getIndex()) {
      throw new ParseException("Extra characters at end", pp.getIndex());
    }

    Expected right_left = Expected.irrelevant;
    Expected rightAndLeft = Expected.irrelevant;
    Expected left_right = Expected.irrelevant;
    switch(relation) {
      case '=': 
        right_left = left_right = Expected.empty;
        break;
      case '\u2282': 
        right_left = Expected.not_empty;
        left_right = Expected.empty;
        break;
      case '\u2283': 
        right_left = Expected.empty;
        left_right = Expected.not_empty;
        break;
      case '\u2286': 
        left_right = Expected.empty;
        break;
      case '\u2287': 
        right_left = Expected.empty;
        break;
      case '∥': 
        rightAndLeft = Expected.empty;
        break;
      case '≉':
        right_left = Expected.not_empty;
        left_right = Expected.not_empty;
        rightAndLeft = Expected.not_empty;
        break;
      default: throw new IllegalArgumentException("Internal Error");
    }

    checkExpected(right_left, new UnicodeSet(rightSet).removeAll(leftSet), "In", rightSide, "But Not In", leftSide);
    checkExpected(rightAndLeft, new UnicodeSet(rightSet).retainAll(leftSet), "In", rightSide, "And In", leftSide);
    checkExpected(left_right, new UnicodeSet(leftSet).removeAll(rightSet), "In", leftSide, "But Not In", rightSide);

  }

  private static void checkExpected(Expected expected, UnicodeSet segment, String rightStatus, String rightSide,
          String leftStatus, String leftSide) {
    switch (expected) {
      case empty: if (segment.size() == 0) return; else break;
      case not_empty: if (segment.size() != 0) return; else break;
      case irrelevant: return;
    }
    testFailureCount++;      
    printErrorLine("Test Failure", Side.START, testFailureCount);
    println("## Expected " + expected + ", got: " + segment.size() + "\t" + segment.toString());
    println("## " + rightStatus + "\t" + rightSide);
    println("## " + leftStatus + "\t" + leftSide);
    errorLister.showSetNames(out, segment);
    printErrorLine("Test Failure", Side.END, testFailureCount);
    println();
  }

  private static void showSet(ParsePosition pp, final String value) {
    pp.setIndex(0);
    UnicodeSet valueSet = new UnicodeSet(value, pp, symbolTable);
    int abbreviated = 0;
    if (showRangeLimit >= 0) {
      UnicodeSet shorter = new UnicodeSet();
      int rangeLimit = showRangeLimit;
      for (UnicodeSetIterator it = new UnicodeSetIterator(valueSet); it.nextRange() && rangeLimit > 0; --rangeLimit) {
        shorter.add(it.codepoint, it.codepointEnd);
      }
      abbreviated = valueSet.size() - shorter.size();
      valueSet = shorter;
    }
    showLister.showSetNames(out, valueSet);
    println("## Total:\t" + valueSet.size() + (abbreviated == 0 ? "" : "\t...(omitting " + abbreviated + " from listing)..."));
    println();
  }

  private static int parseError(int parseErrorCount, String line, Exception e) {
    parseErrorCount++;
    if (e instanceof ParseException) {
      int index = ((ParseException) e).getErrorOffset();
      line = line.substring(0,index) + "☞" + line.substring(index);
    }

    printErrorLine("Parse Error", Side.START, parseErrorCount);
    println("**** PARSE ERROR:\t" + line);
    out.println("<pre>");
    final String message = e.getMessage();
    if (message != null) {
      println("##" + message);
    }
    e.printStackTrace(out);
    out.println("</pre>");
    printErrorLine("Parse Error", Side.END, parseErrorCount);
    println();
    return parseErrorCount;
  }

  enum Side {START, END};

  private static void printErrorLine(String title, Side side, int testFailureCount) {
    title = title + " " + testFailureCount;
    println("**** " + side + " " + title + " ****", title.replace(' ', '_'));
  }

  private static final String BASE_RULES =
    ":: (hex-any/xml);" +
    ":: (hex-any/xml10);" + 
    "'<' > '&lt;' ;" +
    "'<' < '&'[lL][Tt]';' ;" +
    "'&' > '&amp;' ;" +
    "'&' < '&'[aA][mM][pP]';' ;" +
    "'>' < '&'[gG][tT]';' ;" +
    "'\"' < '&'[qQ][uU][oO][tT]';' ; " +
    "'' < '&'[aA][pP][oO][sS]';' ; ";

  private static final String CONTENT_RULES =
    "'>' > '&gt;' ;";

  private static final String HTML_RULES = BASE_RULES + CONTENT_RULES + 
  "'\"' > '&quot;' ; ";

  private static final String HTML_RULES_CONTROLS = HTML_RULES + 
  ":: [[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:] - [\\u0020\\u0009]] hex/unicode ; ";

  public static final Transliterator toHTMLControl = Transliterator.createFromRules(
          "any-html", HTML_RULES_CONTROLS, Transliterator.FORWARD);
  private static final UnicodeSet PATTERN_WHITE_SPACE = (UnicodeSet) new UnicodeSet("\\p{pattern white space}").freeze();
  private static int testFailureCount;
  private static int parseErrorCount;
  private static PrintWriter out;
  private static BagFormatter errorLister;
  private static BagFormatter showLister;
  private static ChainedSymbolTable symbolTable = new ChainedSymbolTable();
  private static boolean doRange;

  private static void println(String line) {
    println(line, null);
  }

  private static void println(String line, String anchor) {
    if (doHtml) {
      if (line.trim().length() == 0) { 
        out.println("<br>");
      } else if (line.equals("##########################")) {
        out.println("<hr>");
      } else {
        line = toHTMLControl.transliterate(line);
        int commentPos = line.indexOf('#');
        if (commentPos >= 0) {
          String aClass = "b";
          if (line.length() > commentPos + 1 && line.charAt(commentPos + 1) == '#') {
            aClass = "bb";
          }
          line = line.substring(0,commentPos) + "<span class='"+ aClass + "'>" + line.substring(commentPos) + "</span>";
        }
        if (line.startsWith("****")) {
          out.println("<h2>" + (anchor == null ? "" : "<a name='" + anchor + "'>") + line + (anchor == null ? "" : "</a>") + "</h2>");
        } else {
          out.println("<p>" + line + "</p>");
        }
      }
    } else {
      out.println(line);
    }
  }

  private static void println() {
    println("");
  }
  
  /**
   * Should add to UnicodeSet
   */

  public static String scan(UnicodeSet unicodeSet, String line, ParsePosition pp, boolean in) {
    int start = pp.getIndex();
    int i = scan(unicodeSet, line, start, in);
    String result = line.substring(start, i);
    pp.setIndex(i);
    return result;
  }

  private static int scan(UnicodeSet allowed, CharSequence line, int start, boolean in) {
    int cp = 0;
    int i;
    for (i = start; i < line.length(); i += UTF16.getCharCount(cp)) {
      cp = Character.codePointAt(line, i);
      if (allowed.contains(cp) != in) break;
    }
    return i;
  }
  
  private static Factory getProperties(final String version) {
    return ICU_VERSION 
    ? ICUPropertyFactory.make() 
            : ToolUnicodePropertySource.make(version);
  }

  static class ChainedSymbolTable extends UnicodeSet.XSymbolTable {

    private static final Comparator<String> LONGEST_FIRST = new Comparator<String>() {
      public int compare(String o1, String o2) {
        int len = o2.length() - o1.length();
        if (len != 0) {
          return len;
        }
        return o1.compareTo(o2);
      }
    };

    Map<String,char[]> variables = new TreeMap<String,char[]>(LONGEST_FIRST);

    public void add(String variable, String value) {
      if (variables.containsKey(variable)) {
        throw new IllegalArgumentException("Attempt to reset variable");
      }
      variables.put(variable, value.toCharArray());
    }

    public char[] lookup(String s) {
      System.out.println("\tlookup: " + s + "\treturns\t" + String.valueOf(variables.get(s)));
      return variables.get(s);
    }

    // Warning: this depends on pos being left alone unless a string is returned!!
    public String parseReference(String text, ParsePosition pos, int limit) {
      //      for (String variable : variables.keySet()) {
      //        final int index = pos.getIndex();
      //        if (text.regionMatches(index, variable, 0, variable.length())) {
      //          pos.setIndex(index + variable.length());
      //          System.out.println("parseReference: " + variable + "\t in\t" + text);
      //          return variable;
      //        }
      //      }
      //      System.out.println("parseReference: missing" + "\t in\t" + text);
      //      return null;
      int start = pos.getIndex();
      int i = start;
      while (i < limit) {
        char c = text.charAt(i);
        if ((i==start && !UCharacter.isUnicodeIdentifierStart(c)) ||
                !UCharacter.isUnicodeIdentifierPart(c)) {
          break;
        }
        ++i;
      }
      if (i == start) { // No valid name chars
        return null;
      }
      pos.setIndex(i);
      return text.substring(start, i);
    }

    final VersionedProperty propertyVersion = new VersionedProperty();
    
    public boolean applyPropertyAlias(String propertyName2,
            String propertyValue, UnicodeSet result) {
      result.clear();
      result.addAll(propertyVersion.set(propertyName2).getSet(propertyValue));
      return true;
    }


  }

  static class VersionedProperty {
    private String propertyName;
    private String version;
    private UnicodeProperty.Factory propSource;
    private UnicodeProperty property;
    final private transient PatternMatcher matcher = new UnicodeProperty.RegexMatcher();

    public VersionedProperty set(String xPropertyName) {
      xPropertyName = xPropertyName.trim();
      if (xPropertyName.contains(":")) {
        String[] names = xPropertyName.split(":");
        if (names.length != 2 || !names[0].startsWith("U")) {
          throw new IllegalArgumentException("Too many ':' fields in " + xPropertyName);
        }
        if (names[0].equalsIgnoreCase("U-1")) {
          version = LAST_VERSION;
        } else {
          version = names[0].substring(1);
        }
        xPropertyName = names[1];
      } else {
        version=LATEST_VERSION;
      };
      propertyName = xPropertyName;
      propSource = getProperties(version);
      property = propSource.getProperty(xPropertyName);
      if (property == null) {
        throw new IllegalArgumentException("Can't create property from name: " + propertyName + " and version: " + version);
      }
      return this;
    }
    
    public UnicodeSet getSet(String propertyValue) {
      UnicodeSet set;
      if (propertyValue.length() == 0) {
        set = property.getSet("true");
      } else if (propertyValue.startsWith("/") && propertyValue.endsWith("/")) {
        final String body = propertyValue.substring(1,propertyValue.length()-1);
        matcher.set(body);
        set = property.getSet(matcher);
      } else {
        set = property.getSet(propertyValue);
      }
      return set;
    }
  }

  public static UnicodeSet parseUnicodeSet(String line, ParsePosition pp) {
    return new UnicodeSet(line, pp, symbolTable);
  }
  
  public static UnicodeSet parseUnicodeSet(String line) {
    ParsePosition pp = new ParsePosition(0);
    UnicodeSet result = new UnicodeSet(line, pp, symbolTable);
    int lengthUsed = pp.getIndex();
    if (lengthUsed != line.length()) {
      throw new IllegalArgumentException("Text after end of set: " + line.substring(0,lengthUsed) + "XXX" + line.substring(lengthUsed));
    }
    return result;
  }
}
