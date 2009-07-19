package org.unicode.jsp;

import java.io.IOException;

import com.ibm.icu.text.UnicodeSet;

public class GenerateSubheader {
public static void main(String[] args) throws IOException {
  final String unicodeDataDirectory = "./jsp/";
  Subheader subheader = new Subheader(unicodeDataDirectory);
  for (String subhead : subheader) {
    UnicodeSet result = subheader.getUnicodeSet(subhead);
    System.out.println("{\"" + subhead + "\",\"" + result.toString().replace("\\", "\\\\") + "\"},");
  }
}
}
