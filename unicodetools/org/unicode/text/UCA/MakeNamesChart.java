package org.unicode.text.UCA;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.text.utility.UtilityBase;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

public class MakeNamesChart {

    static int lastCodePoint = -1;
    static boolean lastCodePointIsOld = false;
    static int lastDecompType = UCD.NONE;

    static final String chartPrefix = "c_";
    static final String namePrefix = "n_";

    static UnicodeSet skipChars;// = new UnicodeSet("[[:gc=cn:]-[:noncharactercodepoint:]]");
    static UnicodeSet rtl;// = new UnicodeSet("[[:bidiclass=r:][:bidiclass=al:]]");
    static UnicodeSet usePicture;// = new UnicodeSet("[[:whitespace:][:defaultignorablecodepoint:]]");

    static UCD lastUCDVersion;

    static final String NAMESLIST_DIR = Settings.CHARTS_GEN_DIR + "nameslist/";

    public static void main(String[] args) throws Exception {
        //	  checkFile();
        //	  if (true) return;
        //ConvertUCD.main(new String[]{"5.0.0"});
        final BlockInfo blockInfo = new BlockInfo(Default.ucdVersion(), "NamesList");

        // http://www.unicode.org/~book/incoming/kenfiles/U50M051010.lst
        //Default.setUCD("5.0.0");
        lastUCDVersion = UCD.make(Settings.lastVersion);
        final ToolUnicodePropertySource up = ToolUnicodePropertySource.make(Default.ucdVersion());
        skipChars = new UnicodeSet(up.getSet("gc=cn")).removeAll(up.getSet("gc=cn"));
        //"[[:gc=cn:]-[:noncharactercodepoint:]]");
        rtl = new UnicodeSet(up.getSet("bidiclass=r")).addAll(up.getSet("bidiclass=al"));// "[[:bidiclass=r:][:bidiclass=al:]]");
        usePicture = new UnicodeSet().addAll(up.getSet("defaultignorablecodepoint=Yes"));// new UnicodeSet("[[:whitespace:][:defaultignorablecodepoint:]]");
        isWhiteSpace = new UnicodeSet(up.getSet("whitespace=Yes"));


        Utility.copyTextFile(Settings.SRC_UCA_DIR + "nameslist_index.html", Utility.UTF8, NAMESLIST_DIR + "index.html");
        Utility.copyTextFile(Settings.SRC_UCA_DIR + "charts.css", Utility.LATIN1, NAMESLIST_DIR + "charts.css");
        Utility.copyTextFile(Settings.SRC_UCA_DIR + "nameslist_help.html", Utility.UTF8, NAMESLIST_DIR + "help.html");
        Utility.copyTextFile(Settings.SRC_UCA_DIR + "nameslist.css", Utility.LATIN1, NAMESLIST_DIR + "nameslist.css");

        final List nameList = new ArrayList();
        final ArrayList lines = new ArrayList();
        final UnicodeSet collectedCodePoints = new UnicodeSet();
        final BitSet nameListNew = new BitSet();

        final int limit = Integer.MAX_VALUE;
        for (int count = 0; count < limit; ++count) {
            if (!blockInfo.next(lines)) {
                break;
            }

            String firstLine = (String)lines.get(0);
            if (firstLine.startsWith("@@@") || firstLine.startsWith("; charset=UTF-8")) {
                continue;
            }

            if (firstLine.contains("dame, Dame")) {
                firstLine = firstLine;
            }
            String[] lineParts = firstLine.split("\t");
            final String fileName = lineParts[1] + ".html";
            nameList.add(firstLine);
            //            System.out.println();
            //            System.out.println("file: " + chartPrefix + fileName);
            //PrintWriter out = FileUtilities.openUTF8Writer("C:/DATA/GEN/charts/namelist/", chartPrefix + fileName);
            PrintWriter out = Utility.openPrintWriter(NAMESLIST_DIR, chartPrefix + fileName, Utility.UTF8_WINDOWS);
            final String heading = TransliteratorUtilities.toHTML.transliterate(getHeading(lineParts[2]));
                        
            out.println("<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN' 'http://www.w3.org/TR/html4/loose.dtd'>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n" +
                    "<title>" + heading + "</title>\n" +
                    "<link rel='stylesheet' type='text/css' href='charts.css'>\n" +
                    "<base target='names'>\n" +
                    "<script type='text/javascript'>\n" +
                    "  function codeAddress() {\n" +
                    "    window.open('" + namePrefix + fileName + "', 'names');\n" +
                    "  }\n" +
                    "  window.onload = codeAddress;\n" +
                    "</script>\n" +
                    "</head>\n" +
                    "<body>");

            // header
            out.println("<table class='headerTable'>\n" +
                    "<tr>\n" +
                    "<td class='headerLeft'>" + lineParts[1] +  "</td>\n" +
                    "<td class='headerCenter'>" + heading + "</td>\n" +
                    "<td class='headerRight'>" + lineParts[3] + "</td>\n" +
                    "</tr>\n" +
                    "</table>");

            if ("Unassigned".equals(lineParts[2])) {
                System.out.println("debug");
            }

            // first pass through and collect all the code points
            collectedCodePoints.clear();
            for (int i = 1; i < lines.size(); ++i) {
                final String line = (String)lines.get(i);
                final int cp1 = line.charAt(0);
                if (cp1 == ';') {
                    continue;
                }
                if (cp1 != '@' && cp1 != '\t') {
                    final int cp = Integer.parseInt(line.split("\t")[0],16);
                    collectedCodePoints.add(cp);
                }
            }
            collectedCodePoints.removeAll(skipChars);

            if (collectedCodePoints.size() == 0) {
                out.println("<p align='center'>No Names List</p>");
            } else {
                out.println("<div align='center'><table class='chart'><tr>");
                int counter = 0;
                for (final UnicodeSetIterator it = new UnicodeSetIterator(collectedCodePoints); it.next();) {
                    if ((counter % 16) == 0 && counter != 0) {
                        out.println("</tr><tr>");
                    }
                    String tdclass = "cell";
                    if (counter < 16) {
                        tdclass = "cellw";
                    }
                    if (it.codepoint == 0x242) {
                        System.out.println("debug");
                    }
                    final boolean isNew = isNew(it.codepoint);
                    if (isNew) {
                        tdclass += "new";
                    }
                    final String hexcp = Utility.hex(it.codepoint, 4);
                    String title = "";
                    final String name = Default.ucd().getName(it.codepoint);
                    if (name != null) {
                        title = " title='" + TransliteratorUtilities.toHTML.transliterate(name.toLowerCase()) + "'";
                    }
                    out.println("<td class='" + tdclass + "'"
                            + title
                            + ">\u00A0"
                            + showChar(it.codepoint, true) 
                            + "\u00A0<br><tt><a href='" + namePrefix + fileName + "#"+ hexcp + "'>" +
                            hexcp + "</a></tt></td>");
                    counter++;
                }
                if (counter > 16) {
                    counter &= 0xF;
                    if (counter != 0) {
                        for (; counter < 16; ++counter) {
                            out.println("<td class='cell'>\u00A0</td>");
                        }
                    }
                }
                out.println("</tr></table></div>");
            }
            out.println("</body>\n</html>");
            out.close();
            //out = FileUtilities.openUTF8Writer("C:/DATA/GEN/charts/namelist/", namePrefix + fileName);
            out = Utility.openPrintWriter(NAMESLIST_DIR, namePrefix + fileName, Utility.UTF8_WINDOWS);
            out.println("<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN' 'http://www.w3.org/TR/html4/loose.dtd'>\n" +
                    UtilityBase.HTML_HEAD +
                    "<title>none</title>\n" +
                    "<link rel='stylesheet' type='text/css' href='nameslist.css'>\n" +
                    "</head>\n" +
                    "<body>");
            out.println("<h1>" + heading + "</h1>\n<p><table>");
            // now do the characters
            boolean inTable = true;
            boolean firstInTable = true;
            for (int i = 1; i < lines.size(); ++i) {
                String line = (String)lines.get(i);
                try {
                    if (line.startsWith("@") && !line.startsWith("@+\t*")) {
                        finishItem(out);
                        //						if (inTable) {
                        //							//out.println("</table>");
                        //							inTable = false;
                        //						}
                        line = line.substring(1);
                        if (line.equals("@+")) {
                            // skip
                        } else if (line.startsWith("+")) {
                            line = line.substring(1).trim();
                            out.println("<tr><td class='comment' colspan='4'>"
                                    + line
                                    + "</td></tr>");
                        } else if (line.startsWith("@")) {
                            System.err.println("*** Can't handle line: " + i + "\t" + line);
                        } else {
                            line = line.trim();
                            out.println("<tr><td colspan='4'><h2>"
                                    + line
                                    + "</h2></td></tr>");
                        }
                    } else {
                        boolean convertHex = true;
                        if (line.startsWith("@+\t*")) {
                            line = line.substring(2);   // handle like regular informative note
                            convertHex = false;         // but without converting hex numbers
                        }
                        if (!inTable) {
                            out.println("<table>");
                            inTable = true;
                            firstInTable = true;
                        }
                        if (line.startsWith("\t")) {
                            String body = line.trim();
                            if (false && line.indexOf(body) != 1) {
                                System.out.println("Format error: too much inital whitespace: <" + line + ">");
                            }
                            final char firstChar = body.charAt(0);
                            switch (firstChar) {
                            case '*': body = "\u2022 " + body.substring(2); break;
                            case '%': body = "\u203B " + body.substring(2); break;
                            case ':': body = checkCanonical(lastCodePoint, body); break;
                            case '#': body = checkCompatibility(lastCodePoint, body); break;
                            case 'x': body = getOther(body); break;
                            case '=': break;
                            case ';': continue;
                            case '~': continue;
                            default: throw new IllegalArgumentException("Huh? " + body);
                            }
                            final char firstDisplayChar = body.charAt(0);
                            body = body.substring(1).trim();
                            out.println("<tr><td"
                                    + ">\u00A0</td>"
                                    + "<td class='char'"
                                    + ">\u00A0</td>"
                                    + "<td class='c'>"
                                    + firstDisplayChar
                                    + "</td><td>"
                                    + maybeNameStyle(showTextConvertingHex(body, convertHex && firstChar != '=' && firstChar != '%'), firstChar == '=')
                                    + "</td></tr>");
                            convertHex = true;
                        } else if (line.startsWith(";")) {
                            System.err.println("*** Ignoring:" + line);
                            continue;
                        } else {
                            finishItem(out);
                            lineParts = line.split("\t");
                            final String x = lineParts[0];
                            lastCodePoint = Integer.parseInt(x,16);
                            final boolean lastCodePointIsNew = isNew(lastCodePoint);
                            if (lastCodePointIsNew) {
                                nameListNew.set(nameList.size()-1, true);
                            }
                            out.println("<tr><td"
                                    + (lastCodePointIsNew ? " class='new'" : "")
                                    + (firstInTable ? " width='1pt'" : "")
                                    + "><code><a name='" + x + "'>" + x + "</a></code></td>"
                                    + "<td class='c'" + (rtl.contains(lastCodePoint) ? " dir='rtl'" : "")
                                    + ">\u00A0"
                                    + showChar(lastCodePoint, true) + "\u00A0</td>"
                                    + "<td colSpan='2'"
                                    + (lastCodePointIsNew ? " class='new'" : "") + ">"
                                    + nameStyle(showTextConvertingHex(lineParts[1], false)) + "</td></tr>");
                            lastDecompType = Default.ucd().getDecompositionType(lastCodePoint);
                        }
                        firstInTable = false;
                    }
                } catch (final Exception e) {
                    throw (IllegalArgumentException) new IllegalArgumentException("Error on line: " + line)
                    .initCause(e);
                }
            }
            finishItem(out);
            out.println("</table>\n" +
                    "<p><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br></p>\n" +
                    "</body>\n</html>");
            out.close();
        }
        blockInfo.in.close();
        // PrintWriter out = FileUtilities.openUTF8Writer("C:/DATA/GEN/charts/namelist/", "mainList.html");
        final PrintWriter out = Utility.openPrintWriter(NAMESLIST_DIR, "mainList.html", Utility.UTF8_WINDOWS);
        FileUtilities.appendFile(WriteCharts.class, "nameslist_chart_header.html", out);
        //		out.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>" +
        //				"<title>Main List</title><link rel='stylesheet' type='text/css' href='nameslist.css'>" +
        //				"<base target='chart'></head><body><table>");
        for (int i = 0; i < nameList.size(); ++i) {
            final String line = (String) nameList.get(i);
            final String[] lineParts = line.split("\t");
            final String fileName = lineParts[1] + ".html";
            out.println("<tr><td><code>" + lineParts[1] +
                    "</code></td><td"
                    + (nameListNew.get(i) ? " class='new'" : "")
                    + "><a href='" + chartPrefix + fileName + "'>" + getHeading(lineParts[2]) + "</a></td><td><code>" +
                    lineParts[3] +"</code></td></tr>");
        }
        out.println("</table>");
        WriteCharts.closeIndexFile(out, "", WriteCharts.NAMELIST, true);

        //out.close();
        //final BagFormatter bf = new BagFormatter();
        //System.out.println(bf.showSetDifferences("Has name in decomps", hasName, "Has no name in decomps", hasNoName));
        System.out.println("Name differences: Canonical");
        showNameDifferences(hasNameCan, hasNoNameCan);
        System.out.println("Name differences: Compatibility");
        showNameDifferences(hasNameComp, hasNoNameComp);
        //		System.out.println("Characters with names in decomps: " + hasName.toPattern(true));
        //		System.out.println("Characters without names in decomps: " + hasNoName.toPattern(true));
        //		System.out.println("Characters sometimes with, sometimes without names in decomps: " + both.toPattern(true));
        System.out.println("Done");
    }

    private static void checkFile() throws IOException {
        final BufferedReader in = Utility.openUnicodeFile("NamesList", Default.ucdVersion(), true, Utility.LATIN1_WINDOWS);
        final Set<LineMatcher> missing = new TreeSet(EnumSet.allOf(LineMatcher.class));
        final Map<LineMatcher,String> examples = new TreeMap();
        while (true) {
            final String line = in.readLine();
            if (line == null) {
                break;
            }
            System.out.println(line);
            final LineMatcher lineMatcher = LineMatcher.match(line);
            if (lineMatcher == null) {
                System.out.println("\t*** Failed match with: <" + line + ">");
            } else {
                System.out.println("\t" + lineMatcher);
                missing.remove(lineMatcher);
                examples.put(lineMatcher, lineMatcher + " <= " + line);
            }
        }
        System.out.println("Missing: " + missing);
        for (final LineMatcher lineMatcher : examples.keySet()) {
            System.out.println(examples.get(lineMatcher));
        }
        in.close();
    }

    private static boolean isNew(int codepoint) {
        return Default.ucd().isNew(codepoint, lastUCDVersion);
    }

    private static void showNameDifferences(Map hasName, Map hasNoName) {
        final Set both = new TreeSet(hasNoName.keySet());
        both.retainAll(hasName.keySet());
        //hasNoName.removeAll(both);
        //hasName.removeAll(both);
        for (final Iterator it = both.iterator(); it.hasNext();) {
            final String decomp = (String) it.next();
            System.out.println();
            System.out.println("decomp: " + Utility.hex(decomp));
            System.out.println("Has name in: " + Utility.hex(hasName.get(decomp)));
            System.out.println("Has no name in: " + Utility.hex(hasNoName.get(decomp)));
        }
        System.out.println("Count: " + both.size());
    }

    //	static TestIdentifiers ti;
    //	static {
    //		try {
    //			ti = new TestIdentifiers("L");
    //		} catch (IOException e) {
    //			// TODO Auto-generated catch block
    //			e.printStackTrace();
    //		}
    //	}

    private static void finishItem(PrintWriter out) {
        if (lastCodePoint < 0) {
            return;
        }
        if (lastDecompType != UCD_Types.NONE) {
            System.out.println("Alert: missing decomp for " + Utility.hex(lastCodePoint));
        }
        final String str = UTF16.valueOf(lastCodePoint);
        final String upper = showForm(out, str, null, null, Default.ucd().getCase(str,UCD_Types.FULL,UCD_Types.UPPER), "\u2191");
        showForm(out, str, upper, null, Default.ucd().getCase(str,UCD_Types.FULL,UCD_Types.TITLE), "\u2195");
        final String lower = showForm(out, str, null, null, Default.ucd().getCase(str,UCD_Types.FULL,UCD_Types.LOWER), "\u2193");
        showForm(out, lower, null, null, Default.ucd().getCase(str,UCD_Types.FULL,UCD_Types.FOLD), "\u2194");

        final String dc = Default.ucd().getDecompositionMapping(lastCodePoint);
        final String nfd = showForm(out, dc, str, null, Default.nfd().normalize(lastCodePoint), "\u21DB");
        //String nfc = showForm(out, dc, null, Default.nfc().normalize(lastCodePoint), "\u21DB");
        final String nfkd = showForm(out, dc, str, nfd, Default.nfkd().normalize(lastCodePoint), "\u21DD");

        //		if (nfkd.equals(str)) {
        //			Set s = ti.getConfusables(lastCodePoint, "MA");
        //			if (s.size() > 1) {
        //				sortedSet.clear();
        //				for (Iterator it = s.iterator(); it.hasNext();) {
        //					sortedSet.add(Default.nfkd().normalize((String)it.next()));
        //				}
        //				sortedSet.remove(nfkd); // remove me
        //				for (Iterator it = sortedSet.iterator(); it.hasNext();) {
        //					String other = (String)it.next();
        //					if (nfkd.equals(Default.nfkd().normalize(other))) continue;
        //					out.println("<tr><td>\u00A0</td><td>\u00A0</td><td class='conf'>\u279F\u00A0"
        //							+ showTextConvertingHex(Utility.hex(other, 4, " + "), true)
        //							+ " "
        //							+ Default.ucd().getName(other, UCD.NORMAL, " + ").toLowerCase()
        //							// maybeNameStyle(showTextConvertingHex(upper, firstChar != '='), firstChar == '=')
        //							+ "</td></tr>");
        //				}
        //			}
        //		}
        lastCodePoint = -1;
    }

    static Set sortedSet = new TreeSet(Collator.getInstance(ULocale.ENGLISH));

    private static String showForm(PrintWriter out, String str, String str2, String str3, String transformed, String symbol) {
        if (!transformed.equals(str) && !transformed.equals(str2) && !transformed.equals(str3)) {
            out.println("<tr><td>\u00A0</td><td>\u00A0</td><td class='c'>" + symbol + "</td><td>"
                    + showTextConvertingHex(Utility.hex(transformed, 4, " + "), true)
                    + (UTF16.countCodePoint(transformed) != 1 ? "" :
                        " " + Default.ucd().getName(transformed, UCD_Types.NORMAL, " + ").toLowerCase())
                        // maybeNameStyle(showTextConvertingHex(upper, firstChar != '='), firstChar == '=')
                        + "</td></tr>");
        }
        return transformed;
    }

    static public String getHeading(String name) {
        final int pos = name.lastIndexOf(" (");
        if (pos < 0) {
            return name;
        }
        return name.substring(0, pos);
    }

    private static String maybeNameStyle(String string, boolean b) {
        if (b && string.equals(string.toUpperCase(Locale.ENGLISH))) {
            return nameStyle(string);
        }
        return string;
    }


    private static String nameStyle(String string) {
        // TODO Auto-generated method stub
        String result = "<span class='name'>" + Default.ucd().getCase(string, UCD_Types.FULL, UCD_Types.TITLE) + "</span>";
        // if it has any &xxx;, then restore them.
        int position = 0;
        while (true) {
            if (!escapeMatch.reset(result).find(position)) {
                break;
            }
            final int start = escapeMatch.start();
            position = escapeMatch.end();
            result = result.substring(0,start)
                    + result.substring(start, position).toLowerCase()
                    + result.substring(position);
        }
        return result;
    }

    static Matcher escapeMatch = Pattern.compile("\\&[A-Z][a-z]*\\;").matcher("");

    private static String showTextConvertingHex(String body, boolean addCharToHex) {
        body = TransliteratorUtilities.toHTML.transliterate(body);
        if (addCharToHex) {
            int position = 0;
            while (position < body.length()) {
                if (!findHex.reset(body).find(position)) {
                    break;
                }
                position = findHex.end();
                final int start = findHex.start();
                final int len = position - start;
                if (len < 4 || len > 6) {
                    continue;
                }
                final int cp = Integer.parseInt(findHex.group(),16);
                if (cp > 0x10FFFF) {
                    continue;
                }
                final String insert = "\u00A0" + showChar(cp, true);
                final String beginning = body.substring(0,start)
                        + "<code>" + body.substring(start, position) + "</code>"
                        + insert;
                body = beginning + body.substring(position);
                position = beginning.length();
            }
        }
        return body;
    }

    /*
CROSS_REF:  TAB "x" SP CHAR SP LCNAME LF    
        | TAB "x" SP CHAR SP "<" LCNAME ">" LF
            // x is replaced by a right arrow

        | TAB "x" SP "(" LCNAME SP "-" SP CHAR ")" LF    
        | TAB "x" SP "(" "<" LCNAME ">" SP "-" SP CHAR ")" LF  
            // x is replaced by a right arrow;
            // (second type as used for control and noncharacters)

            // In the forms with parentheses the "(","-" and ")" are removed
            // and the order of CHAR and LCNAME is reversed;
            // i.e. all inputs result in the same order of output

        | TAB "x" SP CHAR LF
            // x is replaced by a right arrow
            // (this type is the only one without LCNAME 
            // and is used for ideographs)
     */
    static Matcher pointer = Pattern.compile("x \\((.*) - ([0-9A-F]+)\\)").matcher("");
    static Matcher pointer1 = Pattern.compile("x ([0-9A-F]{4,6}) (.*)").matcher("");
    static Matcher pointer2 = Pattern.compile("x ([0-9A-F]{4,6})").matcher("");
    static Matcher findHex = Pattern.compile("[0-9A-F]+").matcher("");

    private static String getOther(String body) {
        // of form: 	x (hyphenation point - 2027)
        // => arrow 2027 X hyphenation point
        int cp;
        String name = null;
        if (pointer.reset(body).matches()) {
            cp = Integer.parseInt(pointer.group(2),16);
            name = checkName(body, cp, pointer.group(1));
        } else if (pointer1.reset(body).matches()) {
            cp = Integer.parseInt(pointer1.group(1),16);
            name = checkName(body, cp, pointer1.group(2));
        } else if (pointer2.reset(body).matches()) {
            cp = Integer.parseInt(pointer2.group(1),16);
            // name = UCharacter.getName(cp).toLowerCase();
            // System.out.println("Irregular format: " + body);
        } else {
            String mismatch = RegexUtilities.showMismatch(pointer, body);
            String mismatch2 = RegexUtilities.showMismatch(pointer2, body);
            throw new IllegalArgumentException("Bad format:\n\t" + mismatch + "\n\t" + mismatch2);
        }
        return "\u2192 " + Utility.hex(cp,4) /*+ " " + showChar(cp)*/ + (name != null ? " " + name : "");
    }

    public static String checkName(String body, int cp, String name) {
        String name2 = Default.ucd().getName(cp);
        if (name2 == null) {
            name2 = "<not a character>";
        }
        if (!name.equalsIgnoreCase(name2)) {
            System.out.println("Mismatch in name for " + body + " in " + Utility.hex(lastCodePoint));
            System.out.println("\tName is: " + name2);
        }
        return name;
    }

    static String showChar(int cp, boolean addRlmIfNeeded) {
        if (cp < 0x20 || cp == 0x7F) {
            int rep = '?';
            if (cp <= 0x20) {
                rep = 0x2400 + cp;
            } else if (cp == 0x7F) {
                rep = 0x2421;
            }
            return "<span class='inv'>" + (char)rep + "</span>";
        }

        if (usePicture.contains(cp)) {
            return "<span class='inv'>⬚</span>";
            //String hex = Utility.hex(cp);
            //return "<img alt='" + hex + "' src='http://www.unicode.org/cgi-bin/refglyph?24-" + hex + "'>";
        }
        if (isWhiteSpace.contains(cp)) {
            return "<span class='inv'>␣</span>";
        }

        final int type = Default.ucd().getCategory(cp);
        if (type == UCD_Types.Cn || type == UCD_Types.Co || type == UCD_Types.Cs) {
            return "\u2588";
        }
        String result = TransliteratorUtilities.toHTML.transliterate(UTF16.valueOf(cp));
        if (type == UCD_Types.Me || type == UCD_Types.Mn) {
            result = "\u25CC" + result;
        } else if (addRlmIfNeeded && rtl.contains(cp)) {
            result = "\u200E" + result + "\u200E";
        }
        return result;
    }

    //static final UnicodeSet noname = new UnicodeSet("[[:ascii:][:ideographic:]]");
    static final Map hasNoNameCan = new TreeMap();
    static final Map hasNameCan = new TreeMap();
    static final Map hasNoNameComp = new TreeMap();
    static final Map hasNameComp = new TreeMap();
    private static UnicodeSet isWhiteSpace;

    private static String checkCanonical(int codePoint, String body) {
        body = body.substring(2);
        if (lastDecompType != UCD_Types.CANONICAL) {
            System.out.println("Mismatching Decomposition Type: " + body + " in " + Utility.hex(codePoint));
        }
        final String lastDecomp = Default.ucd().getDecompositionMapping(lastCodePoint);
        final String hexed = Utility.hex(lastDecomp, 4, " ");
        String hexed2 = hexed;
        if (UTF16.countCodePoint(lastDecomp) == 1) {
            hexed2 += " " + Default.ucd().getName(lastDecomp).toLowerCase();
        }
        if (hexed.equalsIgnoreCase(body)) {
            hasNoNameCan.put(lastDecomp, UTF16.valueOf(codePoint));
        } else if (hexed2.equalsIgnoreCase(body)) {
            hasNameCan.put(lastDecomp, UTF16.valueOf(codePoint));
        } else {
            System.out.println("Mismatching Decomposition: " + body + " in " + Utility.hex(codePoint));
            System.out.println("\tShould be: " + hexed);
        }
        lastDecompType = UCD_Types.NONE;
        return "\u2261 " + body;
    }

    private static String checkCompatibility(int codePoint, String body) {
        body = body.substring(2);
        if (lastDecompType <= UCD_Types.CANONICAL) {
            System.out.println("Mismatching Decomposition Type: " + body + " in " + Utility.hex(codePoint));
        }
        final String lastDecomp = Default.ucd().getDecompositionMapping(lastCodePoint);
        String hexed = Utility.hex(lastDecomp, 4, " ");
        if (lastDecompType != UCD_Types.COMPAT_UNSPECIFIED) {
            final String lastDecompID = Default.ucd().getDecompositionTypeID(lastCodePoint);
            hexed = "<" + lastDecompID + "> " + hexed;
        }
        String hexed2 = hexed;
        if (UTF16.countCodePoint(lastDecomp) == 1) {
            hexed2 += " " + Default.ucd().getName(lastDecomp).toLowerCase();
        }
        if (hexed.equalsIgnoreCase(body)) {
            hasNoNameComp.put(lastDecomp, UTF16.valueOf(codePoint));
        } else if (hexed2.equalsIgnoreCase(body)) {
            hasNameComp.put(lastDecomp, UTF16.valueOf(codePoint));
        } else {
            System.out.println("Mismatching Decomposition: " + body + " in " + Utility.hex(codePoint));
            System.out.println("\tShould be: " + hexed);
        }
        lastDecompType = UCD_Types.NONE;
        return "\u2248 " + body;
    }

    static class BlockInfo {
        BufferedReader in;
        String lastLine;
        BlockInfo (String version, String filename) throws IOException {
            in = Utility.openUnicodeFile(filename, version, true, Utility.LATIN1_WINDOWS);
            //in = FileUtilities.openUTF8Reader(dir, filename);
        }
        boolean next(List inout) throws IOException {
            inout.clear();
            if (lastLine != null) {
                inout.add(lastLine);
                lastLine = null;
            }
            while (true) {
                final String line = in.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("@@\t")) {
                    lastLine = line;
                    break;
                }
                inout.add(line);
            }
            return inout.size() > 0;
        }

    }

    public static final String[][] LINE_MATCHER_VARIABLES = {
        {"$char", "[0-9A-F]{4,6}"},
        {"$name", "[0-9A-Z](?:[0-9A-Z\\- ]*[0-9A-Z])?"}, // alphanumeric, separated by spaces and '-'
        {"$lcname", "[0-9a-zA-Z](?:[0-9a-zA-Z \\-]*[0-9a-zA-Z])?"}, // lowercase alphanumeric, separated by spaces and '-'
        // NOTE: lcname can contain uppercase characters
        {"$comment", "\\([A-Za-z](?:[0-9A-Za-z, \\-]*[0-9A-Za-z])?\\)"}, // '(' alphanumeric (upper or lower) separated by spaces ')'
    };

    enum LineMatcher {
        //NAME_LINE:  CHAR <tab> NAME LF
        // NOTE: sometimes <tab>, sometimes TAB
        //      // The CHAR and the corresponding image are echoed,
        //      // followed by the name as given in NAME
        NAME_LINE("($char)\t($name)?(?: (\\*))?"),
        // NOTE: missing *
        //
        //    CHAR TAB "<" LCNAME ">" LF
        //      // Control and non-characters use this form of
        //      // lower case, bracketed pseudo character name
        NAME_LINE2("($char)\t<($lcname)>(?: (\\*))?"),
        // NOTE: missing *
        //    CHAR TAB NAME SP COMMENT LF
        //      // Names may have a comment, which is stripped off
        //      // unless the file is parsed for an ISO style list
        NAME_LINE3("($char)\t($name) ($comment)(?: (\\*))?"),
        // NOTE: COMMENT should be "(" ... ")"
        //
        //RESERVED_LINE:  CHAR TAB <reserved>
        //      // The CHAR is echoed followed by an icon for the
        //      // reserved character and a fixed string e.g. <reserved>
        //
        RESERVED_LINE("$char\t(<reserved>)"),
        //COMMENT_LINE: <tab> "*" SP EXPAND_LINE
        //      // * is replaced by BULLET, output line as comment
        //    <tab> EXPAND_LINE
        //      // Output line as comment
        COMMENT_LINE("\t\\* (.*)"),
        //
        //ALIAS_LINE: <tab> "=" SP LINE
        //      // Replace = by itself, output line as alias
        ALIAS_LINE("\t= (.*)"),
        //
        //FORMALALIAS_LINE: <tab> "%" SP LINE
        //      // Replace % by U+203B, output line as formal alias
        FORMALALIAS_LINE("\t% (.*)"),
        //
        //CROSS_REF:  <tab> "X" SP CHAR SP LCNAME
        //      // X is replaced by a right arrow
        CROSS_REF1("\tx ($char) ($name)"),
        CROSS_REF_SPACE("\tx ($char)(?: ($name))?"),
        //NOTE: "  x 5382" doesn't have name
        //    <tab> "X" SP "(" LCNAME SP "-" SP CHAR ")"
        CROSS_REF2("\tx \\(($lcname) - ($char)\\)"),
        CROSS_REF3("\tx \\(<($lcname)> - ($char)\\)"),
        // NOTE: may have < ... > Explicit in NAME_LINE but not here
        //      // X is replaced by a right arrow,
        //      // the "(", "-", ")" are removed, and the
        //      // order of CHAR and LCNAME is reversed;
        //      // i.e. both inputs result in the same output
        CROSS_REF_XTRATAB1("\t\tx ($char) ($name)"),
        CROSS_REF_XTRATAB2("\t\tx \\(($lcname) - ($char)\\)"),
        //NOTE: is "x", not "X"
        //
        //FILE_COMMENT: ";"  LINE
        FILE_COMMENT(";(.*)"),
        //EMPTY_LINE: LF
        //      // Empty and ignored lines as well as
        //      // file comments are ignored
        EMPTY_LINE(""),
        //
        //SIDEBAR_LINE:   ";;" LINE
        //      // Skip ';;' characters, output line
        //      // as marginal note
        SIDEBAR_LINE(";;(.*)"),
        //
        //IGNORED_LINE: <tab> ";" EXPAND_LINE
        //      // Skip ':' character, ignore text
        // NOTE: : is wrong
        IGNORED_LINE("\t;(.*)"),
        //
        //DECOMPOSITION:  <tab> ":" SP EXPAND_LINE
        //      // Replace ':' by EQUIV, expand line into
        //      // decomposition
        DECOMPOSITION("\t: (.*)"),
        //
        //COMPAT_MAPPING: <tab> "#" SP EXPAND_LINE
        //COMPAT_MAPPING: <tab> "#" SP "<" LCTAG ">" SP EXPAND_LINE
        //      // Replace '#' by APPROX, output line as mapping;
        //      // check the <tag> for balanced < >
        COMPAT_MAPPING2("\t# <($lctag)> (.*)"),
        COMPAT_MAPPING1("\t# (.*)"),
        //NOTE: out of order
        //
        //NOTICE_LINE:  "@+" <tab> LINE
        //      // Skip '@+', output text as notice
        //    "@+" TAB * SP LINE
        NOTICE_LINE_XTRATAB2("@\\+\t\t\\* (.*)"),
        NOTICE_LINE_XTRATAB1("@\\+\t ?\t(.*)"),
        NOTICE_LINE2("@\\+\t\\* (.*)"),
        NOTICE_LINE1("@\\+\t(.*)"),
        // NOTE: @+    Italic symbols already encoded in the Letterlike Symbols block are omitted here to avoid duplicate encoding.
        // has TAB SP TAB
        // NOTE: out of order
        //      // Skip '@', output text as notice
        //      // "*" expands to a bullet character
        //      // Notices following a character code apply to the
        //      // character and are indented. Notices not following
        //      // a character code apply to the page/block/column
        //      // and are italicized, but not indented
        //
        //SUBTITLE: "@@@+" <tab> LINE
        //      // Skip "@@@+", output text as subtitle
        SUBTITLE("@@@\\+\t(.*)"),
        //
        //SUBHEADER:  "@" <tab> LINE
        //      // Skip '@', output line as text as column header
        SUBHEADER_XTRATAB("@\t\t(.*)"),
        SUBHEADER("@\t(.*)"),
        // NOTE: has 2 tabs
        //
        //BLOCKHEADER:  "@@" <tab> BLOCKSTART <tab> BLOCKNAME <tab> BLOCKEND
        //      // Skip "@@", cause a page break and optional
        //      // blank page, then output one or more charts
        //      // followed by the list of character names.
        //      // Use BLOCKSTART and BLOCKEND to define
        //      // what characters belong to a block.
        //      // Use blockname in page and table headers
        //    "@@" <tab> BLOCKSTART <tab> BLOCKNAME COMMENT <tab> BLOCKEND
        BLOCKHEADER2("@@\t($char)\t([^\t\\(]*\\($name\\))\t($char)"),
        BLOCKHEADER("@@\t($char)\t([^\t]*) ?\t($char)"),
        // NOTE: out of order
        // NOTE: comment is (....)? -- also, needs SP
        // NOTE: @@  1380  Ethiopic Supplement   139F has SP TAB
        //      // If a comment is present it replaces the blockname
        //      // when an ISO-style namelist is laid out
        //
        //BLOCKNAME:    LABEL
        //    LABEL SP "(" LABEL ")"
        BLOCKNAME2("@@\t(.*)\\(.*\\)"),
        BLOCKNAME("@@\t(.*)"),
        // NOTE: missing @@ SP
        //      // If an alternate label is present it replaces
        //      // the blockname when an ISO-style namelist is
        //      // laid out; it is ignored in the Unicode charts
        //
        //BLOCKSTART: CHAR  // First character position in block
        BLOCKSTARTOREND("$char"),
        //BLOCKEND:   CHAR  // Last character position in block
        //PAGE_BREAK: "@@"  // Insert a (column) break
        PAGE_BREAK("$char"),
        //INDEX_TAB:    "@@+" // Start a new index tab at latest BLOCKSTART
        INDEX_TAB("@@\\+"),
        //
        //TITLE:    "@@@" <tab> LINE
        TITLE("@@@\t(.*)"),
        //      // Skip "@@@", output line as text
        //      // Title is used in page headers
        //
        //EXPAND_LINE:  {CHAR | STRING}+ LF
        //      // All instances of CHAR *) are replaced by
        //      // CHAR NBSP x NBSP where x is the single Unicode
        //      // character corresponding to CHAR.
        //      // If character is combining, it is replaced with
        //      // CHAR NBSP <circ> x NBSP where <circ> is the
        //      // dotted circle
        NO_DEFINITION("\t(.*)"),
        // NOTE: this is not defined. Example: "  Final Unicode 5.1 names list."
        // "00AB  LEFT-POINTING DOUBLE ANGLE QUOTATION MARK *" is not defined

        ;
        Matcher matcher;

        private LineMatcher(String regexPattern) {
            for (final String[] pair : LINE_MATCHER_VARIABLES) {
                regexPattern = regexPattern.replace(pair[0], pair[1]);
            }
            matcher = Pattern.compile(regexPattern).matcher("");
        }

        public static LineMatcher match(String input) {
            for (final LineMatcher matcher : LineMatcher.values()) {
                if (matcher.matcher.reset(input).matches()) {
                    return matcher;
                }
            }
            return null;
        }
        public String group() {
            return matcher.group();
        }
        public String group(int arg0) {
            return matcher.group(arg0);
        }
        public int groupCount() {
            return matcher.groupCount();
        }
        @Override
        public String toString() {
            final StringBuilder result = new StringBuilder(name());
            try {
                for (int i = 1; i <= matcher.groupCount(); ++i) {
                    String group = matcher.group(i);
                    if (group == null) {
                        continue;
                    }
                    if (!group.equals(group.trim())) {
                        group += "~~~";
                    }
                    result.append(" {").append(group).append("}");
                }
            } catch (final RuntimeException e) {
            }
            return result.toString();
        }
    }
}