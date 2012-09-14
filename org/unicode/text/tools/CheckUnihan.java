package org.unicode.text.tools;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.With;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.Relation;

public class CheckUnihan {
    
    public static void main(String[] args) {
        SortedSet<String> props = new TreeSet<String>();
        Relation<String,String> values = Relation.of(new HashMap<String,Set<String>>(), HashSet.class);
        Pattern tabSplitter = Pattern.compile("\t");
        for (File file : new File(Utility.WORKSPACE_DIRECTORY + "DATA/UCD/6.0.0-Update/Unihan").listFiles()) {
            System.out.println(file.getName());
            for (String line : FileUtilities.in(file.getParent(), file.getName())) {
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                //U+3405  kOtherNumeric   5
                String[] parts = tabSplitter.split(line);
                props.add(parts[1]);
                if (!parts[2].contains(" ")) {
                    continue;
                }
                values.put(parts[1], parts[2]);
            }
        }
        for (String prop : props) {
            Set<String> set = values.get(prop);
            if (set == null) {
                System.out.println(prop + "\t" + "NO values with spaces");
            } else {
                System.out.println(prop + "\t" + "values with spaces: " + set.iterator().next());                
            }
        }
    }
}
