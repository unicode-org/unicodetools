package org.unicode.text.utility;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.MakeUnicodeFiles;
import org.unicode.text.utility.Utility.RuntimeIOException;

public class UnicodeDataFile {
    public PrintWriter out;
    private String newFile;
    private String mostRecent;
    private String filename;
    private UnicodeDataFile(){};
    private String fileType = ".txt";
    private boolean skipCopyright = true;

    public static UnicodeDataFile openAndWriteHeader(String directory, String filename) throws IOException {
        return new UnicodeDataFile(directory, filename, false);
    }

    public static UnicodeDataFile openHTMLAndWriteHeader(String directory, String filename) throws IOException {
        return new UnicodeDataFile(directory, filename, true);
    }

    private UnicodeDataFile(String directory, String filename, boolean isHTML) throws IOException {
        fileType = isHTML ? ".html" : ".txt";
        // When we still generated files with version infixes, the following line was:
        // newSuffix = FileInfix.fromFlags(Settings.BUILD_FOR_COMPARE, true).getFileSuffix(fileType);
        // newFile = directory + filename + newSuffix;
        newFile = directory + filename + fileType;
        out = Utility.openPrintWriterGenDir(newFile, Utility.UTF8_UNIX);
        // getMostRecentUnicodeDataFile() is very expensive for output files that do not also
        // exist somewhere in the input data folder:
        // It will look through all versioned folders and their subfolders,
        // and eventually fail to find such a file.
        // We skip this when we won't look at what we find, or when we know that we won't find anything.
        // For known pure output files, we could use a different constructor, or add a parameter.
        boolean skipRecentFile =
                // close() will not even look at mostRecent.
                Settings.BUILD_FOR_COMPARE ||
                // These folders exist only in the tools output, not in the tools input.
                directory.endsWith("/cldr/") ||
                directory.endsWith("/extra/");
        mostRecent = skipRecentFile ? null :
            Utility.getMostRecentUnicodeDataFile(
                UnicodeDataFile.fixFile(filename), Default.ucd().getVersion(), true, true, fileType);
        this.filename = filename;

        if (!isHTML) {
            out.println(Utility.getDataHeader(filename + FileInfix.plain.getFileSuffix(".txt")));
            out.println("#\n# Unicode Character Database"
                    + "\n#   For documentation, see https://www.unicode.org/reports/tr44/");
        }
        try {
            Utility.appendFile(Settings.SRC_UCD_DIR + filename + "Header" + fileType, Utility.UTF8_UNIX, out);
        } catch (final RuntimeIOException e) {
            if (!(e.getCause() instanceof FileNotFoundException)) {
                throw e;
            }
        } catch (final FileNotFoundException e) {

        }
    }

    public void close() throws IOException {
        try {
            Utility.appendFile(filename + "Footer" + fileType, Utility.UTF8_UNIX, out);
        } catch (final RuntimeIOException e) {
            if (!(e.getCause() instanceof FileNotFoundException)) {
                throw e;
            }
        } catch (final FileNotFoundException e) {

        }
        out.close();
        if (!Settings.BUILD_FOR_COMPARE) {
            Utility.renameIdentical(mostRecent, Utility.getOutputName(newFile), null, skipCopyright);
        }
    }

    /**
     * There are three cases:<br>
     * no version (ArabicShaping.txt)<br>
     * plain version (ArabicShaping-11.0.0.txt)<br>
     * d version (ArabicShaping-11.0.0-d31.txt)<br>
     */
    public enum FileInfix {
        none, 
        plain, 
        d;
        // TODO: Switch call sites to getFileSuffix(): Faster for none because it avoids concatenation.
        public String getFileInfix() {
            if (this == none) {
                return "";
            }
            String infix = "-" + Default.ucd().getVersion();
            if (this == d && MakeUnicodeFiles.dVersion >= 0) {
                infix = infix + "d" + MakeUnicodeFiles.dVersion;
            }
            return infix;
        }
        public String getFileSuffix(String fileType) {
            if (this == none) {
                return fileType;  // avoid string concatenation
            }
            return getFileInfix() + fileType;
        }
        public static FileInfix fromFlags(boolean suppress, boolean withDVersion) {
            return suppress ? FileInfix.none : !withDVersion ? FileInfix.plain : FileInfix.d;
        }
    }


    //Remove "d1" from DerivedJoiningGroup-3.1.0d1.txt type names

    public static String fixFile(String s) {
        final int len = s.length();
        if (!s.endsWith(".txt")) {
            return s;
        }
        if (s.charAt(len-6) != 'd') {
            return s;
        }
        final char c = s.charAt(len-5);
        if (c != 'X' && (c < '0' || '9' < c)) {
            return s;
        }
        s = s.substring(0,len-6) + s.substring(len-4);
        System.out.println("Fixing File Name: " + s);
        return s;
    }

    public boolean isSkipCopyright() {
        return skipCopyright;
    }

    public UnicodeDataFile setSkipCopyright(boolean skipCopyright) {
        this.skipCopyright = skipCopyright;
        return this;
    }
}

