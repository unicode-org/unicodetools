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
    private boolean skipCopyright;

    public DiffingPrintWriter(String dir, String filename) {
        this(new File(dir, filename));
    }

    public DiffingPrintWriter(File file) {
        this(file, null);
    }

    /**
     * Reuse referenceFile when there are no substantive changes; otherwise compare with the
     * existing output. Dates are ignored. Call {@link #skipCopyright()} to also ignore copyright
     * lines.
     */
    public DiffingPrintWriter(File file, File referenceFile) {
        super();
        this.referenceFile = referenceFile;
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

    /** Ignores copyright lines when comparing files and returns this writer for chaining. */
    public DiffingPrintWriter skipCopyright() {
        skipCopyright = true;
        return this;
    }

    /** Returns a PrintWriter whose close() also compares and finalizes the output file. */
    public PrintWriter toPrintWriter() {
        return new PrintWriter(this);
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
