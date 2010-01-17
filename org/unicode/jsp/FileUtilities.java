package org.unicode.jsp;

import java.io.BufferedReader;
import java.util.regex.Pattern;

public final class FileUtilities {
  public final static Pattern SEMI = Pattern.compile("\\s*;\\s*");

  public static abstract class SemiFileReader {

    protected abstract void handleLine(int start, int end, String[] items);
    protected boolean isCodePoint() {
      return true;
    }

    public SemiFileReader process(Class classLocation, String fileName) {
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
          if (line.startsWith("\uFEFF")) {
            line = line.substring(1);
          }
          line = line.trim();
          if (line.length() == 0) {
            continue;
          }
          String[] parts = SEMI.split(line);
          int start, end;
          if (isCodePoint()) {
            String source = parts[0];
            int range = source.indexOf("..");
            if (range >= 0) {
              start = Integer.parseInt(source.substring(0,range),16);
              end = Integer.parseInt(source.substring(range+2),16);
            } else {
              start = end = Integer.parseInt(source, 16);
            }
          } else {
            start = end = -1;
          }
          handleLine(start, end, parts);
        }
        in.close();
      } catch (Exception e) {
        throw (RuntimeException) new IllegalArgumentException(line).initCause(e);
      }
      return this;
    }
  }
  //
  //  public static SemiFileReader fillMapFromSemi(Class classLocation, String fileName, SemiFileReader handler) {
  //    return handler.process(classLocation, fileName);
  //  }

}
