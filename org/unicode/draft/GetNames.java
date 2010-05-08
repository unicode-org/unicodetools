package org.unicode.draft;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.util.ULocale;


public class GetNames {
public static void main(String[] args) throws IOException {
  System.out.println(new File(".").getCanonicalPath());
  BufferedReader in = BagFormatter.openUTF8Reader("src/", "locales.txt");
  while (true) {
    String line = in.readLine();
    if (line == null) break;
    String name = ULocale.getDisplayName(line, "en");
    System.out.println(line + '\t' + name + '\t' + line);
  }
}
}
