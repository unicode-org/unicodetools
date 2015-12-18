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

    private UnicodeDataFile (String directory, String filename, boolean isHTML) throws IOException {
        fileType = isHTML ? ".html" : ".txt";
        final String newSuffix = UnicodeDataFile.getFileSuffix(true, fileType);
        newFile = directory + filename + newSuffix;
        out = Utility.openPrintWriterGenDir(newFile, Utility.UTF8_UNIX);
        mostRecent = Utility.getMostRecentUnicodeDataFile(
                UnicodeDataFile.fixFile(filename), Default.ucd().getVersion(), true, true, fileType);
        this.filename = filename;

        if (!isHTML) {
            out.println("# " + filename + UnicodeDataFile.getFileSuffix(false));
            out.println(generateDateLine());
            out.println("# © " + Default.getYear() + " Unicode®, Inc.");
            out.println("# For terms of use, see http://www.unicode.org/terms_of_use.html");
            out.println("#");
            out.println("# Unicode Character Database");
            out.println("# For documentation, see http://www.unicode.org/reports/tr44/");
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
        Utility.renameIdentical(mostRecent, Utility.getOutputName(newFile), null, skipCopyright);
    }

    public static String generateDateLine() {
        return "# Date: " + Default.getDate();
    }

    public static String getHTMLFileSuffix(boolean withDVersion) {
        return getFileSuffix(withDVersion, ".html");
    }

    public static String getFileSuffix(boolean withDVersion) {
        return getFileSuffix(withDVersion, ".txt");
    }

    public static String getFileSuffix(boolean withDVersion, String suffix) {
        return (!MakeUnicodeFiles.SHOW_VERSION_IN_FILE ? ""
                : "-" + Default.ucd().getVersion()
                + ((withDVersion && MakeUnicodeFiles.dVersion >= 0) ? ("d" + MakeUnicodeFiles.dVersion) : ""))
                + suffix;
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

