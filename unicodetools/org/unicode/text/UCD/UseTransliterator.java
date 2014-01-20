package org.unicode.text.UCD;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.unicode.text.utility.Settings;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.TransliteratorUtilities;
import com.ibm.icu.text.Transliterator;

public class UseTransliterator {
    public static void main(String[] args) throws IOException {
        try {
            final String filename = args[0];
            final File f2 = new File("org/unicode/text/UCD/");
            System.out.println(f2.getAbsolutePath());
            TransliteratorUtilities.registerTransliteratorFromFile(Settings.SRC_UCD_DIR, "any-temp");
            final Transliterator t = Transliterator.getInstance("any-temp");
            final File f = new File(filename);
            final String fileContents = TransliteratorUtilities.getFileContents(f.getParent() + File.separator, f.getName());
            final String newContents = t.transliterate(fileContents);
            final PrintWriter pw = BagFormatter.openUTF8Writer(f.getParent() + File.separator, "new-" + f.getName());
            pw.write(newContents);
            pw.close();
        } finally {
            // TODO Auto-generated catch block
            System.out.println("Done");
        }
    }
}