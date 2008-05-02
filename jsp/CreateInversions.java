package jsp;

import java.io.IOException;

import com.ibm.icu.dev.test.util.ICUPropertyFactory;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.dev.test.util.UnicodeMap.MapIterator;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class CreateInversions {

  public static void main(String[] args) {
    // check the code (by inspection) to make sure it works
    // later do unit test
    UnicodeSet[] tests = { 
            new UnicodeSet("[abcxyz]"), 
            new UnicodeSet("[:whitespace:]"),
            new UnicodeSet("[:deprecated:]"),
            };
    for (UnicodeSet test : tests) {
      showSet(test);
    }
    UnicodeMap testMap = new UnicodeMap();
    testMap.putAll(new UnicodeSet("[abcxyz]"), "foo");
    showMap(testMap);
    
    // check with names
    for (UnicodeSet test : tests) {
      testMap.clear();
      for (UnicodeSetIterator it = new UnicodeSetIterator(test); it.next();) {
        testMap.put(it.codepoint, UCharacter.getName(it.codepoint));
      }
      showMap(testMap);
    }
    
    // check with properties
    ICUPropertyFactory propFactory = ICUPropertyFactory.make();
    UnicodeProperty[] testProperties = {
            propFactory.getProperty("numeric_type"),
            propFactory.getProperty("block"),
            };
    for (UnicodeProperty test : testProperties) {
      showMap(test.getUnicodeMap());
    }
  }

  private static void showSet(UnicodeSet test) {
    System.out.println(test);
    System.out.println(createInversions(test, "testName"));
    System.out.println();
  }

  private static void showMap(UnicodeMap testMap) {
    System.out.println(testMap);
    System.out.println(createInversions(testMap, "testName"));
    System.out.println();
  }

  public static String createInversions(UnicodeSet source, String name) {
    try {
      return createInversions(source, name, new StringBuilder()).toString();
    } catch (IOException e) {
      throw (RuntimeException) new IllegalArgumentException("Should not happen").initCause(e);
    }
  }

  public static String createInversions(UnicodeMap source, String name) {
    try {
      return createInversions(source, name, new StringBuilder()).toString();
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

  public static Appendable createInversions(UnicodeSet source, String name, Appendable target)
          throws IOException {
    target.append("var " + name + " = new Inversion([\n");
    boolean first = true;
    for (UnicodeSetIterator it = new UnicodeSetIterator(source); it.nextRange();) {
      if (first) {
        first = false;
      } else {
        target.append(",\n"); // the linebreak is not needed, but easier to read
      }
      target.append(shortestForm(it.codepoint));
      if (it.codepointEnd != 0x10FFFF) {
        target.append(",").append(shortestForm(it.codepointEnd + 1));
      }
    }
    target.append("\n]);");
    return target;
  }

  public static Appendable createInversions(UnicodeMap source, String name, Appendable target)
          throws IOException {
    target.append("var " + name + " = new Inversion([\n");
    StringBuilder valueArray = new StringBuilder();
    boolean first = true;
    for (MapIterator it = new UnicodeMap.MapIterator(source); it.nextRange();) {
      if (first) {
        first = false;
      } else {
        target.append(",\n"); // the linebreak is not needed, but easier to read
        valueArray.append(",\n"); // the linebreak is not needed, but easier to
                                  // read
      }
      target.append(shortestForm(it.codepoint));
      valueArray.append(shortestForm(source.getValue(it.codepoint)));
    }
    target.append("\n],[\n").append(valueArray).append("\n]);");
    return target;
  }

  private static Object shortestForm(Object value) {
    if (value == null) {
      return "null";
    } else if (value instanceof Byte || value instanceof Short || value instanceof Integer
            || value instanceof Long) {
      return shortestForm(((Number) value).longValue());
    } else if (value instanceof Float || value instanceof Double) {
      return value.toString();
    }
    String result = value.toString();
    // TODO optimize this
    result.replace("\b", "\\\b"); // quote
    result.replace("\t", "\\\t"); // quote
    result.replace("\n", "\\\n"); // quote
    result.replace("\u000B", "\\v"); // quote
    result.replace("\f", "\\\f"); // quote
    result.replace("\r", "\\\r"); // quote
    result.replace("\"", "\\\""); // quote
    result.replace("\\", "\\\\"); // quote
    return "\"" + result + "\"";
  }

  private static CharSequence shortestForm(long number) {
    String decimal = String.valueOf(number);
    String hex = "0x" + Long.toHexString(number);
    return decimal.length() < hex.length() ? decimal : hex;
  }
}
