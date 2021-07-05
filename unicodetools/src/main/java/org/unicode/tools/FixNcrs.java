package org.unicode.tools;

import java.io.IOException;
import java.io.PrintWriter;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.TransliteratorUtilities;

import com.ibm.icu.text.Transliterator;

public class FixNcrs {
    // TODO add command line support later
    public static void main(String[] args) throws IOException {
//	String relativeFileName = FileUtilities.getRelativeFileName(FixNcrs.class, "entity.txt/../");
//	TransliteratorUtilities.registerTransliteratorFromFile(relativeFileName, "entity");
	Transliterator te = Transliterator.getInstance("hex-any/xml; hex-any/xml10");

	String dir = "/Users/markdavis/eclipse-workspace/unicode-draft/emoji/";
	
	try (PrintWriter out = FileUtilities.openUTF8Writer(dir, "frequency2.html")) {
	    for (String line : FileUtilities.in(dir, "frequency.html")) {
		String original = line;
		if (line.contains("&")) {
		    String newLine = te.transform(line)
			    .replace("&zwj;", "\u200D")
			    .replace("&hellip;", "\u2026")
			    .replace("&spades;", "\u2660")
			    .replace("&frac14;", "\u00BC")
			    .replace("&frac12;", "\u00BD")
			    .replace("&nbsp;", "\u00A0")
			    .replace("&hearts;", "\u2665")
			    .replace("&clubs;", "\u2663")
			    .replace("&harr;", "\u2194")
			    .replace("&diams;", "\u2666")
			    ;
		    // System.out.println(original + " => " + line);
			line = newLine;
		}
		if (line.contains("&")) {
		    System.out.println(original + " => " + line);
		}
		out.println(line);
	    }
	}
    }
}
