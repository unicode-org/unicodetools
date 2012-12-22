package org.unicode.draft;
import java.util.Random;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;


public class CheckPunycode {
  
  private static final UnicodeSet LDH = new UnicodeSet("[-0-9a-z]");
  private static final UnicodeSet APPROX_STRINGPREP = new UnicodeSet("[[-0-9a-z][:l:][:m:][:nd:]-[:nfkcqc=n:]-[:Lu:]-[:Lt:]]");
  static Random r = new Random(0);

  public static void main(String[] args) throws StringPrepParseException {
      showPunycode("ÖBB");
      showPunycode("I♥NY");
      showPunycode("ÖBB".toLowerCase());
      if (true) return;

    System.out.println("singles");
    for (UnicodeSetIterator it = new UnicodeSetIterator(LDH); it.next();) {
      checkPunycode(it.getString());
    }
    System.out.println("doubles");
    for (UnicodeSetIterator it = new UnicodeSetIterator(LDH); it.next();) {
      for (UnicodeSetIterator it2 = new UnicodeSetIterator(LDH); it2.next();) {
        checkPunycode(it.getString() + it2.getString());
      }
    }
    System.out.println("triples");
    for (UnicodeSetIterator it = new UnicodeSetIterator(LDH); it.next();) {
      for (UnicodeSetIterator it2 = new UnicodeSetIterator(LDH); it2.next();) {
        for (UnicodeSetIterator it3 = new UnicodeSetIterator(LDH); it3.next();) {
        checkPunycode(it.getString() + it2.getString() + it3.getString());
        }
      }
    }    
    checkNewVsOld(1000);
    System.out.println("DONE");
  }

  private static void checkPunycode(String source) {
    String x = "";
    String y = "";
    String status = "OK";
    try {
      x = OldPunycode.decode(new StringBuffer(source), null).toString();
      if (LDH.containsAll(x)) {
        status = "ASCII";
        return;
      } else {
        if (!APPROX_STRINGPREP.containsAll(x)) {
          return;
        }
      y = OldPunycode.encode(new StringBuffer(x), null).toString();
      if (!source.equals(y)) {
        status = "NO-RT";
      }
      }
    } catch (Exception e) {
      status = "FAIL";
      return;
    }
    System.out.println(status + " " + source + " => <" + x + "> " + Utility.hex(x) + " " + 
            " => <" + y + "> " + Utility.hex(y));
  }

  private static void checkNewVsOld(int count) throws StringPrepParseException {
    Punycode puny = new Punycode();
    for (int j = 0; j < count; ++j) {
      String unicode = randomString();
      System.out.println(unicode);
        OldPunycode.showProgress = puny.showProgress = (j == 37);

      try {
        String result = OldPunycode.encode(new StringBuffer(unicode), null).toString();
        String result2 = puny.encode(unicode, new StringBuilder()).toString();
        if (!result.equals(result2)) {
          System.out.println("Encode Failure at: " + unicode + ", " + result + ", " + result2);
        }
        String back = OldPunycode.decode(new StringBuffer(result), null).toString();
        String back2 = puny.decode(result, new StringBuffer()).toString();
        if (!back.equals(back2)) {
          System.out.println("Decode Failure at: " + unicode + ", " + result + ", " + back + ", " + back2);
        }
      } catch (Exception e) {
        throw (RuntimeException) new IllegalArgumentException(j + " Error " + unicode).initCause(e);
      }
    }
  }

  private static final UnicodeSet ASSIGNED = new UnicodeSet("[:assigned:]");
  private static final int ASSIGNED_SIZE = ASSIGNED.size();
  
  private static String randomString() {
    int len = r.nextInt(20);
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < len; ++i) {
      int c;
      while (true) {
        final double nextDouble = r.nextDouble();
        int index = (int)(ASSIGNED_SIZE*nextDouble*nextDouble*nextDouble);
        System.out.println(index);
        if (index > ASSIGNED_SIZE) {
          continue;
        }
        c = ASSIGNED.charAt(index);
        if (c >= 0xd800 && c <= 0xdFFF) {
          continue;
        }     
        break;
      }
      b.appendCodePoint(c);
    }
    String unicode = b.toString();
    return unicode;
  }

  private static void showPunycode(final String unicode) throws StringPrepParseException {
    System.out.println(unicode);
    String result = OldPunycode.encode(new StringBuffer(unicode), null).toString();
    System.out.println("=> " + result);
    String back = OldPunycode.decode(new StringBuffer(result), null).toString();
    System.out.println("=> " + back);
  }
}
