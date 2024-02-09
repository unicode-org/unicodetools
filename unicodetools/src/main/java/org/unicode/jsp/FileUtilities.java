package org.unicode.jsp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public final class FileUtilities {

    public abstract static class SemiFileReader {
        public static final Pattern SPLIT = Pattern.compile("\\s*;\\s*");
        private int lineCount;

        protected void handleStart() {}

        protected abstract boolean handleLine(int start, int end, String[] items);

        protected void handleEnd() {}

        public int getLineCount() {
            return lineCount;
        }

        protected boolean isCodePoint() {
            return true;
        }

        protected String[] splitLine(String line) {
            return SPLIT.split(line);
        }

        public SemiFileReader process(Class classLocation, String fileName) {
            BufferedReader in;
            try {
                in = FileUtilities.openFile(classLocation, fileName);
            } catch (final Exception e) {
                throw (RuntimeException)
                        new IllegalArgumentException(classLocation.getName() + ", " + fileName)
                                .initCause(e);
            }
            try {
                return process(in, fileName);
            } catch (final Exception e) {
                throw (RuntimeException)
                        new IllegalArgumentException(lineCount + ":\t" + 0).initCause(e);
            }
        }

        public SemiFileReader process(String directory, String fileName) {
            try {
                final FileInputStream fileStream = new FileInputStream(directory + "/" + fileName);
                final InputStreamReader reader =
                        new InputStreamReader(fileStream, StandardCharsets.UTF_8);
                final BufferedReader bufferedReader = new BufferedReader(reader, 1024 * 64);
                return process(bufferedReader, fileName);
            } catch (final Exception e) {
                throw (RuntimeException)
                        new IllegalArgumentException(lineCount + ":\t" + 0).initCause(e);
            }
        }

        public SemiFileReader process(BufferedReader in, String fileName) {
            handleStart();
            String line = null;
            lineCount = 1;
            try {
                for (; ; ++lineCount) {
                    line = in.readLine();
                    if (line == null) {
                        break;
                    }
                    // Ignore merge conflict markers.
                    if (line.startsWith("<<<<<<<")
                            || line.startsWith("=======")
                            || line.startsWith(">>>>>>>")) {
                        continue;
                    }
                    final int comment = line.indexOf("#");
                    if (comment >= 0) {
                        processComment(line, comment);
                        line = line.substring(0, comment);
                    }
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    line = line.trim();
                    if (line.length() == 0) {
                        continue;
                    }
                    final String[] parts = splitLine(line);
                    int start, end;
                    if (isCodePoint()) {
                        final String source = parts[0];
                        final int range = source.indexOf("..");
                        if (range >= 0) {
                            start = Integer.parseInt(source.substring(0, range), 16);
                            end = Integer.parseInt(source.substring(range + 2), 16);
                        } else {
                            start = end = Integer.parseInt(source, 16);
                        }
                    } else {
                        start = end = -1;
                    }
                    if (!handleLine(start, end, parts)) {
                        break;
                    }
                }
                in.close();
                handleEnd();
            } catch (final Exception e) {
                throw (RuntimeException)
                        new IllegalArgumentException(lineCount + ":\t" + line).initCause(e);
            }
            return this;
        }

        protected void processComment(String line, int comment) {}
    }
    //
    //  public static SemiFileReader fillMapFromSemi(Class classLocation, String fileName,
    // SemiFileReader handler) {
    //    return handler.process(classLocation, fileName);
    //  }

    public static BufferedReader openFile(Class class1, String file) throws IOException {
        // URL path = null;
        // String externalForm = null;
        try {
            //      //System.out.println("Reading:\t" + file1.getCanonicalPath());
            //      path = class1.getResource(file);
            //      externalForm = path.toExternalForm();
            //      if (externalForm.startsWith("file:")) {
            //        externalForm = externalForm.substring(5);
            //      }
            //      File file1 = new File(externalForm);
            //      boolean x = file1.canRead();
            //      final InputStream resourceAsStream = new FileInputStream(file1);
            final InputStream resourceAsStream = class1.getResourceAsStream(file);
            final InputStreamReader reader =
                    new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8);
            final BufferedReader bufferedReader = new BufferedReader(reader, 1024 * 64);
            return bufferedReader;
        } catch (final Exception e) {
            final File file1 = new File(file);
            final String foo = class1.getResource(".").toString();

            throw (RuntimeException)
                    new IllegalArgumentException(
                                    "Bad file name: "
                                            //              + path + "\t" + externalForm + "\t" +
                                            + file1.getCanonicalPath()
                                            + "\n"
                                            + foo
                                            + "\n"
                                            + new File(".").getCanonicalFile()
                                            + " => "
                                            + Arrays.asList(
                                                    new File(".").getCanonicalFile().list()))
                            .initCause(e);
        }
    }

    static String[] splitCommaSeparated(String line) {
        // items are separated by ','
        // each item is of the form abc...
        // or "..." (required if a comma or quote is contained)
        // " in a field is represented by ""
        final List<String> result = new ArrayList<String>();
        final StringBuilder item = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); ++i) {
            final char ch = line.charAt(i); // don't worry about supplementaries
            switch (ch) {
                case '"':
                    inQuote = !inQuote;
                    // at start or end, that's enough
                    // if get a quote when we are not in a quote, and not at start, then add it and
                    // return to inQuote
                    if (inQuote && item.length() != 0) {
                        item.append('"');
                        inQuote = true;
                    }
                    break;
                case ',':
                    if (!inQuote) {
                        result.add(item.toString());
                        item.setLength(0);
                    } else {
                        item.append(ch);
                    }
                    break;
                default:
                    item.append(ch);
                    break;
            }
        }
        result.add(item.toString());
        return result.toArray(new String[result.size()]);
    }

    public static String getFileAsString(BufferedReader in) {
        try {
            final StringBuilder result = new StringBuilder();
            while (true) {
                final String line = in.readLine();
                if (line == null) {
                    break;
                }
                if (result.length() != 0) {
                    result.append('\n');
                }
                result.append(line);
            }
            return result.toString();
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
