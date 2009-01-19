package jsp;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

class Subheader {
  static final boolean DEBUG = true;
  Matcher subheadMatcher = Pattern.compile("(@+)\\s+(.*)").matcher("");
  Matcher hexMatcher = Pattern.compile("([A-Z0-9]+).*").matcher("");
  Map<Integer, String> codePoint2Subblock = new HashMap<Integer, String>();
  Map<String, UnicodeSet> subblock2UnicodeSet = new TreeMap<String, UnicodeSet>();
  Map<String,Set<String>> block2subblock = new TreeMap<String, Set<String>>();
  Map<String,Set<String>> subblock2block = new TreeMap<String, Set<String>>();

  Subheader(String unicodeDataDirectory) throws IOException {
    String filename = unicodeDataDirectory + "NamesList.txt";
    getDataFromFile(filename);

    for (String subblock : subblock2UnicodeSet.keySet()) {
      final UnicodeSet uset = subblock2UnicodeSet.get(subblock);
      for (UnicodeSetIterator it = new UnicodeSetIterator(uset); it.next();) {
        codePoint2Subblock.put(it.codepoint, subblock);

        String block = UCharacter.getStringPropertyValue(UProperty.BLOCK, it.codepoint, UProperty.NameChoice.LONG).toString().replace('_', ' ').intern();

        Set<String> set = block2subblock.get(block);
        if (set == null) {
          block2subblock.put(block, set = new TreeSet<String>());
        }
        set.add(subblock);

        set = subblock2block.get(subblock);
        if (set == null) {
          subblock2block.put(subblock, set = new TreeSet<String>());
        }
        set.add(block);
      }
    }
  }

  private String getDataFromFile(String filename) throws FileNotFoundException, IOException {
    String subblock = "?";
    BufferedReader in = new BufferedReader(new FileReader(filename));
    while (true) {
      String line = in.readLine();
      if (line == null) break;
      if (subheadMatcher.reset(line).matches()) {
        subblock = subheadMatcher.group(1).equals("@") ? subheadMatcher.group(2) : "?";
        continue;
      }
      if (subblock.length() != 0 && hexMatcher.reset(line).matches()) {
        int cp = Integer.parseInt(hexMatcher.group(1), 16);
        UnicodeSet uset = subblock2UnicodeSet.get(subblock);
        if (uset == null) {
          subblock2UnicodeSet.put(subblock, uset = new UnicodeSet());
        }
        uset.add(cp);
      }
    }
    in.close();
    return subblock;
  }

  String getSubheader(int codepoint) {
    return codePoint2Subblock.get(codepoint);
  }
}