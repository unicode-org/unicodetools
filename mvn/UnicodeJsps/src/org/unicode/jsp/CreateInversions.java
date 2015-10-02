package org.unicode.jsp;

import java.io.IOException;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMapIterator;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class CreateInversions {

  // testing

  public static void main(String[] args) {
    UnicodeSet ignorables = new UnicodeSet("[[:Cn:][:Cs:][:Co:]]").freeze(); // exclude unassigned, surrogates, and private use
    CreateInversions createInversions = new CreateInversions().setIgnorables(ignorables).setDelta(true);

    // check the code (by inspection) to make sure it works
    // later do unit test
    UnicodeSet[] tests = { 
            new UnicodeSet("[abcxyz]"), 
            new UnicodeSet("[:whitespace:]"),
            new UnicodeSet("[:deprecated:]"),
    };
    for (UnicodeSet test : tests) {
      showSet(createInversions, test);
    }

    UnicodeMap testMap = new UnicodeMap();
    testMap.putAll(new UnicodeSet("[abcxyz]"), "foo");
    showMap(createInversions, testMap);

    // check with names
    for (UnicodeSet test : tests) {
      testMap.clear();
      for (UnicodeSetIterator it = new UnicodeSetIterator(test); it.next();) {
        testMap.put(it.codepoint, UCharacter.getName(it.codepoint));
      }
      showMap(createInversions, testMap);
    }

    // check with properties
    ICUPropertyFactory propFactory = ICUPropertyFactory.make();
    UnicodeMap[] testProperties = {
            propFactory.getProperty("numeric_type").getUnicodeMap(),
            propFactory.getProperty("block").getUnicodeMap(),
            propFactory.getProperty("word_break").getUnicodeMap(),
            propFactory.getProperty("grapheme_cluster_break").getUnicodeMap().putAll(new UnicodeSet(0xAC00,0xD7A3), "LVT"),
            // note: separating out the LV from LVT can be done more compactly with an algorithm.
            // it is periodic: AC00, AC1C, AC38...
    };
    for (UnicodeMap test : testProperties) {
      showMap(createInversions, test);
    }

    // further compaction can be done by assigning each property value to a number, and using that instead.
    UnicodeMap source = propFactory.getProperty("grapheme_cluster_break").getUnicodeMap().putAll(new UnicodeSet(0xAC00,0xD7A3), "LVT");
    UnicodeMap target = new UnicodeMap();
    int numberForValue = 0;
    // iterate through the values, assigning each a number
    for (Object value : source.getAvailableValues()) {
      target.putAll(source.keySet(value), numberForValue++);
    }
    showMap(createInversions, target);
  }

  private static void showSet(CreateInversions createInversions, UnicodeSet test) {
    System.out.println("** Source:");
    System.out.println(test);
    System.out.println("** Result:");
    System.out.println(createInversions.create("testName", test));
    System.out.println("Inversions: " + createInversions.getInversions());
    System.out.println();
  }

  private static void showMap(CreateInversions createInversions, UnicodeMap testMap) {
    System.out.println("** Source:");
    System.out.println(testMap);
    System.out.println("** Result:");
    System.out.println(createInversions.create("testName", testMap));
    System.out.println("Inversions: " + createInversions.getInversions());
    System.out.println();
  }

  // guts

  private UnicodeSet ignorables;

  private boolean delta;

  private int inversions;

  private int getInversions() {
    return inversions;
  }

  private CreateInversions setDelta(boolean b) {
    delta = b;
    return this;
  }

  private CreateInversions setIgnorables(UnicodeSet ignorables) {
    this.ignorables = ignorables;
    return this;
  }

  public String create(String name, UnicodeSet source) {
    try {
      return create(name, source, new StringBuilder()).toString();
    } catch (IOException e) {
      throw (RuntimeException) new IllegalArgumentException("Should not happen").initCause(e);
    }
  }

  public String create(String name, UnicodeMap source) {
    try {
      return create(name, source, new StringBuilder()).toString();
    } catch (IOException e) {
      throw (RuntimeException) new IllegalArgumentException("Should not happen").initCause(e);
    }
  }

  // public String createInversions(UnicodeSet source, String name, String
  // filename) throws IOException {
  // return createInversions(source, name, new StringBuilder()).close();
  // }
  //  
  // public String createInversions(UnicodeMap source, String name, String
  // filename) throws IOException {
  // return createInversions(source, name, new StringBuilder()).toString();
  // }

  public Appendable create(String name, UnicodeSet source, Appendable target)
  throws IOException {
    initShortestForm();
    target.append("var " + name + " = new Inversion([\n");
    boolean first = true;
    for (UnicodeSetIterator it = new UnicodeSetIterator(source); it.nextRange();) {
      if (first) {
        first = false;
      } else {
        target.append(",\n"); // the linebreak is not needed, but easier to read
      }
      target.append(shortestForm(it.codepoint, delta));
      if (it.codepointEnd != 0x10FFFF) {
        target.append(",").append(shortestForm(it.codepointEnd + 1, delta));
      }
    }
    target.append("\n]");
    if (delta) {
      target.append(",true");
    }
    target.append(");");
    return target;
  }

  public Appendable create(String name, UnicodeMap source, Appendable target)
  throws IOException {
    initShortestForm();
    target.append("var " + name + " = new Inversion([\n");
    StringBuilder valueArray = new StringBuilder();
    boolean first = true;
    for (UnicodeMapIterator it = new UnicodeMapIterator(source); it.nextRange();) {
      // skip ignorable range
      if (ignorables.contains(it.codepoint, it.codepointEnd)) {
        continue;
      }
      // also skip adjacent rows with same value
      final String valueString = shortestForm(source.getValue(it.codepoint));
      if (lastValue == valueString || lastValue != null && lastValue.equals(valueString)) {
        continue;
      }
      lastValue = valueString;
      if (first) {
        first = false;
      } else {
        target.append(",\n"); // the linebreak is not needed, but easier to read
        valueArray.append(",\n"); // the linebreak is not needed, but easier to
        // read
      }
      target.append(shortestForm(it.codepoint, delta));
      valueArray.append(valueString);
    }
    target.append("\n],[\n").append(valueArray).append("\n]");
    if (delta) {
      target.append(",true");
    }
    target.append(");");
    return target;
  }

  long lastNumber;
  String lastValue;

  private void initShortestForm() {
    lastNumber = 0;
    inversions = 0;
    lastValue = null;
  }

  private String shortestForm(Object value) {
    String result;
    if (value == null) {
      result = "null";
    } else if (value instanceof Byte || value instanceof Short || value instanceof Integer
            || value instanceof Long) {
      --inversions; // don't add inversion in this case
      result = shortestForm(((Number) value).longValue(), false);
    } else if (value instanceof Float || value instanceof Double) {
      result = value.toString();
    } else {
      result = value.toString();
      // TODO optimize this
      result.replace("\b", "\\\b"); // quote
      result.replace("\t", "\\\t"); // quote
      result.replace("\n", "\\\n"); // quote
      result.replace("\u000B", "\\v"); // quote
      result.replace("\f", "\\\f"); // quote
      result.replace("\r", "\\\r"); // quote
      result.replace("\"", "\\\""); // quote
      result.replace("\\", "\\\\"); // quote
      result = "\"" + result + "\"";
    }
    return result;
  }

  private String shortestForm(long number, boolean useDelta) {
    if (useDelta) {
      long temp = number;
      number -= lastNumber;
      lastNumber = temp;
    }
    ++inversions;
    String decimal = String.valueOf(number);
    String hex = "0x" + Long.toHexString(number);
    return decimal.length() < hex.length() ? decimal : hex;
  }
}
