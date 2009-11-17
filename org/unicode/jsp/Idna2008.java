package org.unicode.jsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.UnicodeMap;

public class Idna2008 {
  
  static final Matcher DATALINE = Pattern.compile(
          "([0-9a-fA-F]{4,6})" +
          "(?:\\.\\.([0-9a-fA-F]{4,6}))?" +
          "\\s*;\\s*" +
          "(PVALID|DISALLOWED|UNASSIGNED|CONTEXTJ|CONTEXTO)" +
          "\\s*#\\s*" +
          "(.*)").matcher("");

  public enum Idna2008Type {
    PVALID, DISALLOWED, UNASSIGNED, CONTEXTJ, CONTEXTO
  }

  public static UnicodeMap<Idna2008Type>  getTypeMapping() {

    try {
      BufferedReader in = new BufferedReader(
              new InputStreamReader(
              Idna2008.class.getResourceAsStream("tables.txt")));
//    BagFormatter.openReader("/Users/markdavis/Documents/workspace/DATA/IDN/",
//            "draft-faltstrom-idnabis-tables-05.txt", "ascii");
      boolean inTable = false;
      UnicodeMap<Idna2008Type> patrik = new UnicodeMap();
      int count = 0;
      while (true) {
        String line = in.readLine();
        if (line == null)
          break;
//        if ((count++ % 100) == 0) {
//          System.out.println(count + " " + line);
//        }
        line = line.trim();

        if (line.startsWith("Appendix B.1.")) {
          inTable = true;
          continue;
        }
        if (line.startsWith("Author's Address")) {
          break;
        }
        if (!inTable)
          continue;
        if (line.length() == 0 || line.startsWith("Faltstrom") || line.startsWith("Internet-Draft"))
          continue;
        // we now have real data
        if (!DATALINE.reset(line).matches()) {
          System.out.println("Error: line doesn't match: " + line);
          continue;
        }
        final int startChar = Integer.parseInt(DATALINE.group(1), 16);
        final int endChar = DATALINE.group(2) == null ? startChar : Integer.parseInt(DATALINE
                .group(2), 16);
        final Idna2008Type idnaType = Idna2008Type.valueOf(DATALINE.group(3));
        patrik.putAll(startChar, endChar, idnaType);
      }
      in.close();
      patrik.freeze();
      return patrik;
    } catch (Exception e) {
      return null;
    }
  }
}
