package org.unicode.jsp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.UnicodeSet;

public class Typology {
    //static UnicodeMap<String> reasons = new UnicodeMap<String>();
    public static Map<String,UnicodeSet> label_to_uset = new TreeMap<String,UnicodeSet>();
    public static Map<String,UnicodeSet> path_to_uset = new TreeMap<String,UnicodeSet>();
    //static Map<List<String>,UnicodeSet> path_to_uset = new TreeMap<List<String>,UnicodeSet>();
    public static Relation<String, String> labelToPath = new Relation(new TreeMap(), TreeSet.class);
    //public static Relation<String, String> pathToList = new Relation(new TreeMap(), TreeSet.class);

    static class MyReader extends FileUtilities.SemiFileReader {
        //0000  Cc  [Control] [X] [X] [X] <control>
        public final static Pattern SPLIT = Pattern.compile("\\s*\t\\s*");
        public final static Pattern NON_ALPHANUM = Pattern.compile("[^0-9A-Za-z]+");

        protected String[] splitLine(String line) {
            return SPLIT.split(line);
        }

        @Override
        protected boolean handleLine(int startRaw, int endRaw, String[] items) {
            String path = "";
            for (int i = 2; i < 6; ++i) {
                String item = items[i];
                if (item.equals("[X]")) continue;

                if (!item.startsWith("[") || !item.endsWith("]")) {
                    throw new IllegalArgumentException(i + "\t" + item);
                }
                item = item.substring(1, item.length()-1);
                if (item.length() == 0) continue;
                item = NON_ALPHANUM.matcher(item).replaceAll("_");

                UnicodeSet uset = label_to_uset.get(item);
                if (uset == null) {
                    label_to_uset.put(item, uset = new UnicodeSet());
                }
                uset.add(startRaw, endRaw);

                //labelToPath.put(item, path);

                uset = path_to_uset.get(path);
                if (uset == null) {
                    path_to_uset.put(path, uset = new UnicodeSet());
                }
                path = (path + "/" + item).intern();
                uset.addAll(startRaw, endRaw);
            }
            return true;
        }

        Map<List<String>,List<String>> listCache = new HashMap<List<String>,List<String>>();
        Map<Set<String>,Set<String>> setCache = new HashMap<Set<String>,Set<String>>();

        private <T> T intern(Map<T,T> cache, T list) {
            T old = cache.get(list);
            if (old != null) return old;
            cache.put(list,list);
            return list;
        }
    }

    static {
        new MyReader().process(XIDModifications.class, "Categories.txt"); // "09421-u52m09xxxx.txt"

        // fix the paths
        Map<String, UnicodeSet> temp= new TreeMap<String, UnicodeSet>();
        for (int i = 0; i < UCharacter.CHAR_CATEGORY_COUNT; ++i) {
            UnicodeSet same = new UnicodeSet()
            .applyIntPropertyValue(UProperty.GENERAL_CATEGORY, i);
            String prefix = UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, i, NameChoice.SHORT).substring(0,1);

            for (String path : path_to_uset.keySet()) {
                UnicodeSet uset = path_to_uset.get(path);
                if (!same.containsSome(uset)) continue;
                String path2 = prefix + path;
                temp.put(path2, new UnicodeSet(uset).retainAll(same));
                String[] labels = path2.split("/");
                labelToPath.put(labels[labels.length -1], path2);
            }
        }

        label_to_uset = freezeMapping(label_to_uset);
        path_to_uset = freezeMapping(temp);

        // invert
    }

    private static Map<String, UnicodeSet> freezeMapping(Map<String, UnicodeSet> map) {
        for (String key : map.keySet()) {
            UnicodeSet uset = map.get(key);
            uset.freeze();
        }
        return Collections.unmodifiableMap(map);
    }

    public static UnicodeSet getSet(String label) {
        return label_to_uset.get(label);
    }

    public static Set<String> getLabels() {
        return label_to_uset.keySet();
    }
}
