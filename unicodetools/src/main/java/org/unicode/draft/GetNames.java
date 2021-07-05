package org.unicode.draft;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import org.unicode.cldr.draft.FileUtilities;

import com.ibm.icu.util.ULocale;


public class GetNames {
    public static void main(String[] args) throws IOException {
        System.out.println(new File(".").getCanonicalPath());
        final BufferedReader in = FileUtilities.openUTF8Reader("src/", "locales.txt");
        while (true) {
            final String line = in.readLine();
            if (line == null) {
                break;
            }
            final String name = ULocale.getDisplayName(line, "en");
            System.out.println(line + '\t' + name + '\t' + line);
        }
    }
}
