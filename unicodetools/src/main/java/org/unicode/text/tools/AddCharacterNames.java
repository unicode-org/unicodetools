package org.unicode.text.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.UCD.Default;

import com.ibm.icu.text.StringTransform;

public class AddCharacterNames {
    public static void main(String[] args) throws IOException {
        final long time1 = System.nanoTime();
        final String input = args[0];
        final File inputFile = new File(input);
        final String parent = inputFile.getParent();
        final String filename = inputFile.getName();
        final BufferedReader in = FileUtilities.openUTF8Reader(parent, filename);
        final PrintWriter out = FileUtilities.openUTF8Writer(parent, "XXX_" + filename);
        for (int lineCount = 1; ; ++lineCount) {
            final String line = in.readLine();
            if (line == null) {
                break;
            }
            final String result = transform.transform(line);
            if (result != line) {
                System.out.println("- " + lineCount + "\t" + line);
                System.out.println("+ " + lineCount + "\t" + result);
                System.out.println();
            }
            out.println(result);
        }
        in.close();
        out.close();
        for (final String skippedLine : skipped) {
            System.out.println(skippedLine);
        }

        final long time2 = System.nanoTime();

        System.out.println("Done in " + (time2 - time1) + "ns");
    }

    static Set<String> skipped = new LinkedHashSet<String>();

    static StringTransform transform = new StringTransform() {
        StringBuilder result = new StringBuilder();
        Matcher hex = Pattern.compile("U\\+([A-Fa-f0-9]+)").matcher("");
        @Override
        public String transform(String source) {
            result.setLength(0);
            int oldPos = 0;
            hex.reset(source);
            while (true) {
                if (hex.find()) {
                    result.append(source.substring(oldPos, hex.end()));
                    oldPos = hex.end();
                    final String hexString = hex.group(1);
                    final int codepoint = Integer.parseInt(hexString, 16);
                    if (codepoint > 0x10FFFF) {
                        skipped.add("***Illegal code point on line: " + source);
                        continue;
                    }
                    final String name = " " + Default.ucd().getName(codepoint).replace("<", "&lt;").replace(">", "&gt;");
                    if (matchLength(source, oldPos, name) > 3) {
                        skipped.add("***Skipping name for " + hexString + " in: " + source);
                        continue;
                    }
                    result.append(name);
                } else {
                    result.append(source.substring(oldPos, source.length()));
                    break;
                }
            }
            final String resultString = result.toString();
            if (resultString.equals(source)) {
                return source;
            }
            return resultString;
        }

        private int matchLength(String source, int oldPos, String target) {
            int length = target.length();
            if (length > source.length() - oldPos) {
                length = source.length() - oldPos;
            }
            for (int i = 0; i < length; ++i) {
                final int sourceChar = source.codePointAt(i + oldPos);
                final int targetChar = target.codePointAt(i);
                if (sourceChar != targetChar) {
                    return i;
                }
                if (sourceChar > 0xFFFF) {
                    ++i;
                }
            }
            return target.length();
        }
    };


}
