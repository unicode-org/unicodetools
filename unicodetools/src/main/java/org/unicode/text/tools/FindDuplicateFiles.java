package org.unicode.text.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.text.utility.Settings;

import com.google.common.base.Objects;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Pair;

public class FindDuplicateFiles {
    private static final String SUFFIX = ".java";
    private static Map<String,String> DIRS = new LinkedHashMap<>();
    static {
        String[] dirs = {
                Settings.UnicodeTools.UNICODETOOLS_REPO_DIR,
                CLDRPaths.BASE_DIRECTORY
        };
        for (String dir : dirs) {
            File dirFile = new File(dir);
            DIRS.put(dir, "{" + dirFile.getName() + "}/");
        }
    }

    public static void main(String[] args) throws IOException {
        Multimap<String,File> duplicates = TreeMultimap.create();
        for (String dir : DIRS.keySet()) {
            addNames(dir, duplicates);
        }
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        LinkedHashSet<String> files = new LinkedHashSet<>();
        for (Entry<String, Collection<File>> entry : duplicates.asMap().entrySet()) {
            Collection<File> dirs = entry.getValue();
            if (dirs.size() <= 1) {
                continue;
            }
            keys.clear();
            files.clear();
            for (File dirFile : dirs) {
                String name = dirFile.getCanonicalPath();
                for (Entry<String, String> dirAndKey : DIRS.entrySet()) {
                    if (name.startsWith(dirAndKey.getKey())) {
                        keys.add(dirAndKey.getValue());
                        files.add(dirAndKey.getValue() + name.substring(dirAndKey.getKey().length()));
                        break;
                    }
                }
            }
            System.out.println(dirs.size() 
                    + "\t" + entry.getKey() 
                    + "\t" + CollectionUtilities.join(keys, " ")
                    + "\t" + CollectionUtilities.join(files, "\t")
                    );
            List<Pair<File,Iterable<String>>> pairs = new ArrayList<>();
            for (File dirFile : dirs) {
                pairs.add(Pair.of(dirFile, FileUtilities.in(FileUtilities.openFile(dirFile))));
            }
            for (int i = 0; i < pairs.size(); ++i) {
                Pair<File, Iterable<String>> pair1 = pairs.get(i);
                for (int j = i+1; j < pairs.size(); ++j) {
                    Pair<File, Iterable<String>> pair2 = pairs.get(j);
                    compare(pair1, pair2);
                }
            }
        }
    }

    private static void compare(Pair<File, Iterable<String>> pair1, Pair<File, Iterable<String>> pair2) {
        Iterator<String> it1 = pair1.second.iterator();
        Iterator<String> it2 = pair2.second.iterator();
        while (true) {
            String s1 = getNext(it1);
            String s2 = getNext(it2);
            if (!Objects.equal(s1, s2)) {
                System.out.println("\t\tDiff1: " + s1);
                System.out.println("\t\tDiff2: " + s2);
                return;
            } else if (s1 == null) {
                System.out.println("EQUAL"); 
                return;
            }
        }
    }

    private static String getNext(Iterator<String> it1) {
        while(it1.hasNext()) {
            String item = it1.next().trim();
            if (item.isEmpty() 
                    || item.startsWith("package ")
                    || item.startsWith("import ")
                    || item.equals("@Override")
                    ) {
                continue;
            }
            return item;
        }
        return null;
    }

    private static void addNames(String dir, Multimap<String, File> duplicates) throws IOException {
        File dirFile = new File(dir);
        if (!dirFile.isDirectory()) {
            String name = dirFile.getName();
            if (name.endsWith(SUFFIX) 
                    && !name.contains("Test") 
                    && !name.equals("Main.java")
                    && !dirFile.toString().contains("/jsp")) {
                duplicates.put(name, dirFile);
            }
        } else {
            for (String subDir : dirFile.list()) {
                addNames(dir + "/" + subDir, duplicates);
            }
        }
    }
}
