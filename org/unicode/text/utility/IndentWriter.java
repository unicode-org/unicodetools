/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/utility/IndentWriter.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.utility;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public class IndentWriter extends Writer {
    public IndentWriter(Writer writer) {
        this.writer = writer;
        width = 30000;
        separator = " ";
    }
    public IndentWriter(OutputStream writer, String encoding)
            throws UnsupportedEncodingException{
        this.writer = new OutputStreamWriter(writer, encoding);
        width = 30000;
        separator = " ";
    }
    public void setSeparator(String separator) {
        this.separator = separator;
    }
    public String getSeparator() {
        return separator;
    }
    public void setWidth(int width) {
        this.width = width;
    }
    public int getWidth() {
        return width;
    }
    public void indentBy(int indentDelta) throws IOException {
        indent += indentDelta;
        flush();
    }
    public void setIndent(int indent) {
        this.indent = indent;
    }
    public int getIndent() {
        return indent;
    }
    /*
    public void write(String cbuf, int off, int len) throws IOException {
        if (buffer.length() + len > width) {
            flushLine();
            buffer.append("                                       ".substring(0,indent));
            buffer.append("(" + indent + ") ");
        } else {
            buffer.append(separator);
        }
	    buffer.append(cbuf, off, len);
    }
    public void write(String string) throws IOException {
        write(string,0,string.length());
    }
     */
    public void write(int indent, String string) throws IOException {
        setIndent(indent);
        write(string,0,string.length());
    }
    public void writeln(int indent, String string) throws IOException {
        write(indent, string);
        flushLine();
    }
    public void writeln(String string) throws IOException {
        write(string);
        flushLine();
    }
    public void writeln() throws IOException {
        flushLine();
    }

    @Override
    public void write(char cbuf[], int off, int len) throws IOException {
        if (buffer.length() == 0) {
            bufferIndent = indent;
        } else if (bufferIndent + buffer.length() + separator.length() + len > width) {
            flushLine();
        } else {
            buffer.append(separator);
        }
        buffer.append(cbuf, off, len);
    }

    public void flushLine() throws IOException {
        if (buffer.length() != 0) { // indent
            writer.write("                                       ",0,bufferIndent);
            writer.write(buffer.toString());
            writer.write(EOL);
            buffer.setLength(0);
        }
    }

    @Override
    public void flush() throws IOException {
        flushLine();
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        flush();
        writer.close();
    }
    private final Writer writer;
    private final StringBuffer buffer = new StringBuffer(200);
    private int width;
    private int indent;
    private int bufferIndent;
    private String separator;
    private static String EOL;
    static { // gets platform-specific eol
        final StringWriter foo = new StringWriter();
        final PrintWriter fii = new PrintWriter(foo);
        fii.println();
        fii.flush();
        EOL = foo.toString();
    }
}