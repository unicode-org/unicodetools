package org.unicode.text.UCD;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.ICUPropertyFactory;
import com.ibm.icu.dev.test.util.Tabber;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.dev.test.util.UnicodeProperty.PatternMatcher;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class TestUnicodeInvariants {

  private static final String LATEST_VERSION = "5.2.0"; // UCD.latestVersion;
  private static final String LAST_VERSION = "5.1.0"; // UCD.lastVersion;
  private static final boolean ICU_VERSION = false; // ignore the above if this is true
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

    errorLister = new BagFormatter()
    .setMergeRanges(doRange)
    .setLabelSource(null)
    .setUnicodePropertyFactory(ToolUnicodePropertySource.make(LATEST_VERSION))
    .setTableHtml("<table class='e'>")
    .setShowLiteral(toHTML);
    errorLister.setShowTotal(false);
    if (doHtml) errorLister.setTabber(new Tabber.HTMLTabber());

    showLister = new BagFormatter()
    .setMergeRanges(doRange)
    .setLabelSource(null)
    .setUnicodePropertyFactory(ToolUnicodePropertySource.make(LATEST_VERSION))
    .setTableHtml("<table class='s'>")
    .setShowLiteral(toHTML);
    showLister.setShowTotal(false);
    if (showScript) {
      showLister.setValueSource(ToolUnicodePropertySource.make(LATEST_VERSION).getProperty("script"));
    }
    if (doHtml) showLister.setTabber(new Tabber.HTMLTabber());

    symbolTable = new ChainedSymbolTable();
    //      new ChainedSymbolTable(new SymbolTable[] {
    //            ToolUnicodePropertySource.make(UCD.lastVersion).getSymbolTable("\u00D7"),
    //            ToolUnicodePropertySource.make(Default.ucdVersion()).getSymbolTable("")});
    ParsePosition pp = new ParsePosition(0);
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
    eatWhitespace(line, pp);
    relation = line.charAt(pp.getIndex());
    if (!INVARIANT_RELATIONS.contains(relation)) {
      throw new ParseException("Invalid relation, must be one of " + INVARIANT_RELATIONS.toPattern(false),
              pp.getIndex());
    }
    pp.setIndex(pp.getIndex()+1); // skip char
    eatWhitespace(line, pp);
    int start = pp.getIndex();
    rightSet = new UnicodeSet(line, pp, symbolTable);
    rightSide = line.substring(start,pp.getIndex());
    eatWhitespace(line, pp);
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
    println("## Expected " + expected + ", got: " + segment.size());
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
    println("**** PARSE ERROR:\t" + line);
    println();

    printErrorLine("Parse Error", Side.START, parseErrorCount);
    
    e.printStackTrace(out);

    println(e.getMessage());
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
  private static int testFailureCount;
  private static int parseErrorCount;
  private static PrintWriter out;
  private static BagFormatter errorLister;
  private static BagFormatter showLister;
  private static ChainedSymbolTable symbolTable;
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
   * @param line
   * @param pp
   */
  private static void eatWhitespace(String line, ParsePosition pp) {
    int cp = 0;
    int i;
    for (i = pp.getIndex(); i < line.length(); i += UTF16.getCharCount(cp)) {
      cp = UTF16.charAt(line, i);
      if (!com.ibm.icu.lang.UCharacter.isUWhiteSpace(cp)) {
        break;
      }
    }
    pp.setIndex(i);
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

    final PatternMatcher matcher = new UnicodeProperty.RegexMatcher();

    public boolean applyPropertyAlias(String propertyName,
            String propertyValue, UnicodeSet result) {
      final String version;
      if (propertyName.contains(":")) {
        String[] names = propertyName.split(":");
        if (names.length != 2 || !names[0].startsWith("U")) {
          throw new IllegalArgumentException("Too many ':' fields in " + propertyName);
        }
        if (names[0].equalsIgnoreCase("U-1")) {
          version = LAST_VERSION;
        } else {
          version = names[0].substring(1);
        }
        propertyName = names[1];
      } else {
        version=LATEST_VERSION;
      };

      UnicodeProperty.Factory propSource = ICU_VERSION 
      ? ICUPropertyFactory.make() 
              : ToolUnicodePropertySource.make(version);      

      UnicodeSet set;
      if (propertyValue.length() == 0) {
        set = propSource.getSet(propertyName + "=true");
      } else if (propertyValue.startsWith("/") && propertyValue.endsWith("/")) {
        final String body = propertyValue.substring(1,propertyValue.length()-1);
        set = propSource.getSet(propertyName + "=" + body, matcher, null);
      } else {
        set = propSource.getSet(propertyName + "=" + propertyValue);
      }
      result.clear();
      result.addAll(set);
      return true;
    }
  }
}
