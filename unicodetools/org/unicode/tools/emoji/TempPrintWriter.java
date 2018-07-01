package org.unicode.tools.emoji;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Random;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.utility.Utility;

import com.ibm.icu.util.ICUUncheckedIOException;

public class TempPrintWriter extends Writer {
    final PrintWriter tempPrintWriter;
    final String tempName;
    final String filename;

    public TempPrintWriter(String dir, String filename) {
        this(new File(dir, filename));
    }

    public TempPrintWriter(File file) {
        final String parentFile = file.getParent();
        this.filename = file.toString();
        Random rand = new Random();
        try {
            File tempFile;
            do {
                tempFile = new File(parentFile, (0x7FFFFFFF & rand.nextInt()) + "-" + file.getName());
            } while (tempFile.exists());
            tempName = tempFile.toString();
            tempPrintWriter = FileUtilities.openUTF8Writer(parentFile, tempFile.getName());
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        tempPrintWriter.close();
        Utility.replaceDifferentOrDelete(filename, tempName, false);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        tempPrintWriter.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        tempPrintWriter.flush();
    }

    public void println(String line) throws IOException {
        tempPrintWriter.println(line);
    }
    
    public void println() throws IOException {
        tempPrintWriter.println();
    }
}