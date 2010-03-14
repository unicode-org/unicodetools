package org.unicode.jsp;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.ibm.icu.text.UnicodeSet;

public class Typology {
  //static UnicodeMap<String> reasons = new UnicodeMap<String>();
  static Map<String,UnicodeSet> label_to_uset = new TreeMap<String,UnicodeSet>();

  static class MyReader extends FileUtilities.SemiFileReader {
    //0000  Cc  [Control] [X] [X] [X] <control>
    public final static Pattern SPLIT = Pattern.compile("\\s*\t\\s*");
    public final static Pattern NON_ALPHANUM = Pattern.compile("[^0-9A-Za-z]+");
    
    protected String[] splitLine(String line) {
      return SPLIT.split(line);
    }

    @Override
    protected void handleLine(int start, int end, String[] items) {
      for (int i = 2; i < 6; ++i) {
        String item = items[i];
        if (item.equals("[X]")) continue;

        if (!item.startsWith("[") || !item.endsWith("]")) {
          throw new IllegalArgumentException(i + "\t" + item);
        }
        item = item.substring(1, item.length()-1);
        item = NON_ALPHANUM.matcher(item).replaceAll("_");
        UnicodeSet uset = label_to_uset.get(item);
        if (uset == null) {
          label_to_uset.put(item, uset = new UnicodeSet());
        }
        uset.add(start, end);
      }
    }
  }
  static {
    new MyReader().process(XIDModifications.class, "09421-u52m09xxxx.txt");
    for (String key : label_to_uset.keySet()) {
      UnicodeSet uset = label_to_uset.get(key);
      uset.freeze();
    }
    label_to_uset = Collections.unmodifiableMap(label_to_uset);
  }
  
  public static UnicodeSet getSet(String label) {
    return label_to_uset.get(label);
  }
  
  public static Set<String> getLabels() {
    return label_to_uset.keySet();
  }
}
