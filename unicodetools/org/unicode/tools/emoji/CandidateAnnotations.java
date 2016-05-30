package org.unicode.tools.emoji;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CandidateData.Quarter;

import com.ibm.icu.dev.util.CollectionUtilities;

public class CandidateAnnotations {
    public static void main(String[] args) {
        final CandidateData cd = CandidateData.getInstance();
        final TreeSet<String> sorted = cd.keySet().addAllTo(new TreeSet<String>(cd.comparator));
        // Internal sheet
        // U+1F600      =image(C1,4,36,36)              http://unicode.org/reports/tr51/images/android/android_1f600.png
        // Native template
        // U+1F471      =vlookup(A1,Internal!A:B,2,0)   person with blond hair  sarı saçlı adam
        
        System.out.println("Internal");
        int row = 0;
        for (String s : sorted) {
            Quarter q = cd.getQuarter(s);
            if (q.isFuture()) continue;
            ++row;
            final String hex = Utility.hex(s).toLowerCase(Locale.ENGLISH);
            System.out.println("U+" + hex 
                    + "\t=image(C" + row + ",4,36,36)" 
                    + "\thttp://unicode.org/draft/reports/tr51/images/android/android_" + hex + ".png"
                    );
        }

        System.out.println("\n\nTemplate");
        System.out.println("Character Code\tImage\tName\tShort Name\tKeywords");
        row = 0;
        for (String s : sorted) {
            Quarter q = cd.getQuarter(s);
            if (q.isFuture()) continue;
            ++row;
            final String hex = Utility.hex(s).toLowerCase(Locale.ENGLISH);
            final String name = cd.getName(s).toLowerCase(Locale.ENGLISH);
            final Set<String> annotations = new TreeSet<>(cd.getAnnotations(s));
            annotations.addAll(Arrays.asList(name.split(" ")));
            System.out.println("U+" + hex 
                    + "\t=vlookup(A" + row + ",Internal!A:B,2,0)" 
                    + "\t" + name
                    + "\t" + ""
                    + "\t" + CollectionUtilities.join(annotations, " | ")
                    );
        }
    }
}
