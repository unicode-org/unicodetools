package org.unicode.jsp;

import java.io.BufferedReader;
import java.util.Map;
import java.util.regex.Pattern;

public class FileUtilities {
  public static Pattern SEMI = Pattern.compile("\\s*;\\s*");
  
  public interface Handler {
    void handle(int start, int end, String[] items);
  }

  public static void fillMapFromSemi(Class classLocation, String fileName, Handler handler) {
    BufferedReader in;
    String line = null;
    try {
      in = UnicodeUtilities.openFile(classLocation, fileName);
      for (int lineCount = 1; ; ++lineCount) {
        line = in.readLine();
        if (line == null) {
          break;
        }
        int comment = line.indexOf("#");
        if (comment >= 0) {
          line = line.substring(0,comment);
        }
        line = line.trim();
        if (line.length() == 0) {
          continue;
        }
        String[] parts = SEMI.split(line);
        String source = parts[0];
        int start, end;
        int range = source.indexOf("..");
        if (range >= 0) {
          start = Integer.parseInt(source.substring(0,range),16);
          end = Integer.parseInt(source.substring(range+2),16);
        } else {
          start = end = Integer.parseInt(source, 16);
        }
        handler.handle(start, end, parts);
      }
      in.close();
    } catch (Exception e) {
      throw (RuntimeException) new IllegalArgumentException(line).initCause(e);
    }
  }

}
