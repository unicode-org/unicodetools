/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/utility/DualWriter.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.utility;

import java.io.IOException;
import java.io.Writer;

public final class DualWriter extends Writer {
    private static final String copyright = "(C) Copyright IBM Corp. 1998 - All Rights Reserved";
    // Abstract class for writing to character streams.
    // The only methods that a subclass must implement are
    // write(char[], int, int), flush(), and close().

    private boolean autoflush;
    private final Writer a;
    private final Writer b;

    public DualWriter(Writer a, Writer b) {
        this.a = a;
        this.b = b;
    }

    public DualWriter(Writer a, Writer b, boolean autoFlush) {
        this.a = a;
        this.b = b;
        autoflush = autoFlush;
    }

    public void setAutoFlush(boolean value) {
        autoflush = value;
    }

    public boolean getAutoFlush() {
        return autoflush;
    }

    @Override
    public void write(char cbuf[], int off, int len) throws IOException {
        a.write(cbuf, off, len);
        b.write(cbuf, off, len);
        if (autoflush) {
            flush();
        }
    }

    @Override
    public void close() throws IOException {
        a.close();
        b.close();
    }

    @Override
    public void flush() throws IOException {
        a.flush();
        b.flush();
    }
}
