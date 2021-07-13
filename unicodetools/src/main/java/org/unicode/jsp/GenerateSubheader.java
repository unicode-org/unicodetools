package org.unicode.jsp;

import java.io.IOException;

import com.ibm.icu.text.UnicodeSet;

public class GenerateSubheader {
    public static void main(String[] args) throws IOException {
        final String unicodeDataDirectory = "./jsp/";
        final Subheader subheader = new Subheader(unicodeDataDirectory);
        for (final String subhead : subheader) {
            final UnicodeSet result = subheader.getUnicodeSet(subhead);
            System.out.println("{\"" + subhead + "\",\"" + result.toString().replace("\\", "\\\\") + "\"},");
        }
    }
}
