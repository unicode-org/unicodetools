package org.unicode.jsp;

import com.ibm.icu.text.UnicodeSet;
import java.io.IOException;

public class GenerateSubheader {
    public static void main(String[] args) throws IOException {
        final String unicodeDataDirectory = "./jsp/";
        Subheader subheader = new Subheader(unicodeDataDirectory);
        for (String subhead : subheader) {
            UnicodeSet result = subheader.getUnicodeSet(subhead);
            System.out.println(
                    "{\"" + subhead + "\",\"" + result.toString().replace("\\", "\\\\") + "\"},");
        }
    }
}
