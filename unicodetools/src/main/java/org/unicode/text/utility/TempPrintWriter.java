package org.unicode.text.utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import org.unicode.cldr.draft.FileUtilities;

public class TempPrintWriter extends PrintWriter {
    static {
    }

    final String filename;

    public TempPrintWriter(String dir, String filename, String encoding) throws IOException {
        super(getBuffer(dir, filename, encoding));
        this.filename = filename;
        throw new IllegalArgumentException("USE org.unicode.tools.emoji.");
    }

    private static BufferedWriter getBuffer(String dirString, String filename, String encoding)
            throws IOException {
        File file = File.createTempFile(filename, null, new File(dirString));
        if (FileUtilities.SHOW_FILES) {
            System.out.println("Creating File: " + file.getCanonicalPath());
        }
        String parentName = file.getParent();
        if (parentName != null) {
            File parent = new File(parentName);
            parent.mkdirs();
        }
        return new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), encoding), 4 * 1024);
    }

    @Override
    public void close() {
        super.close();
    }
}
