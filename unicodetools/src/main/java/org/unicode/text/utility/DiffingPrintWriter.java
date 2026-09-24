package org.unicode.text.utility;

import com.ibm.icu.util.ICUUncheckedIOException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Random;
import org.unicode.cldr.draft.FileUtilities;

public class DiffingPrintWriter extends Writer {
    public final PrintWriter tempPrintWriter;
    final String tempName;
    final String filename;
    private final File referenceFile;
    private final boolean skipCopyright;

    public DiffingPrintWriter(String dir, String filename) {
        this(new File(dir, filename));
    }

    public DiffingPrintWriter(File file) {
        this(file, null, false);
    }

    /**
     * Reuse referenceFile when there are no substantive changes; otherwise compare with the
     * existing output. Dates are ignored, and copyright lines are ignored if skipCopyright is true.
     */
    public DiffingPrintWriter(File file, File referenceFile, boolean skipCopyright) {
        super();
        this.referenceFile = referenceFile;
        this.skipCopyright = skipCopyright;
        final String parentFile = file.getParent();
        this.filename = file.toString();
        Random rand = new Random();
        try {
            File tempFile;
            do {
                tempFile = new File(parentFile, (0xFFFF & rand.nextInt()) + "-" + file.getName());
            } while (tempFile.exists());
            tempName = tempFile.toString();
            tempPrintWriter = FileUtilities.openUTF8Writer(parentFile, tempFile.getName());
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        tempPrintWriter.close();
        try {
            if (referenceFile != null
                    && referenceFile.exists()
                    && Utility.filesAreIdentical(
                            referenceFile.toString(), tempName, skipCopyright, new String[2])) {
                Files.copy(
                        referenceFile.toPath(),
                        Path.of(filename),
                        StandardCopyOption.REPLACE_EXISTING);
                Files.delete(Path.of(tempName));
            } else {
                Utility.replaceDifferentOrDelete(filename, tempName, skipCopyright);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
        tempPrintWriter.write(cbuf, off, len);
    }

    @Override
    public void flush() {
        tempPrintWriter.flush();
    }

    public void println(String line) {
        tempPrintWriter.println(line);
    }

    public void println() {
        tempPrintWriter.println();
    }
}
