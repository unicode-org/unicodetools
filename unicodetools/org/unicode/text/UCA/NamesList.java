package org.unicode.text.UCA;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.text.UnicodeSet;

public class NamesList {
    static class Data {
        Relation<Comment,String> comments = Relation.of(new EnumMap<Comment,Set<String>>(Comment.class), LinkedHashSet.class);
        public String display(String sep, String sep2) {
            StringBuilder b = new StringBuilder();
            for (Entry<Comment, Set<String>> entry : comments.keyValuesSet()) {
                b.append(sep).append(entry.getKey());
                for (String x : entry.getValue()) {
                    b.append(sep2).append(x);
                }
            }
            return b.toString();
        }
        public boolean isEmpty() {
            return comments.isEmpty();
        }
    }
    UnicodeMap<Data> data = new UnicodeMap<>();
    UnicodeMap<String> subheads = new UnicodeMap();
    Relation<Integer, String> errors = Relation.of(new TreeMap<Integer,Set<String>>(), LinkedHashSet.class);
    Relation<Integer, String> fileComments = Relation.of(new TreeMap<Integer,Set<String>>(), LinkedHashSet.class);

    Data preface = new Data();
    int lastCodePoint = 0;
    Data lastDataItem = preface;


    enum Comment {name, plus, bullet, bullet2, alias, xref, variation, canonical, compatibility}

    public NamesList(String file) {
        BufferedReader in = Utility.openUnicodeFile(file, Default.ucdVersion(), true, Utility.LATIN1_WINDOWS);
        int i = 0;
        String subhead = "???";
        try {
            while (true) {
                final String originalLine = in.readLine();
                if (originalLine == null) {
                    break;
                }
                ++i;
                if (originalLine.isEmpty()) {
                    continue;
                }
                try {
                    if (originalLine.startsWith("@")) {
                        final String line = originalLine.substring(1);
                        if (line.equals("@+")) {
                            // skip
                        } else if (line.startsWith("+")) {
                            addComment(Comment.plus, line.substring(1));
                        } else if (line.startsWith("@")) {
                            verifyBlock(line.substring(1));
                        } else {
                            subhead = line.substring(1).trim();
                        }
                    } else {
                        if (originalLine.startsWith("\t")) {
                            final String body = originalLine.trim();
                            final char firstChar = body.charAt(0);
                            switch (firstChar) {
                            case '*': addComment(Comment.bullet, body.substring(1)); continue;
                            case '%': addComment(Comment.bullet2, body.substring(1)); continue;
                            case ':': verifyCanonical(body.substring(1)); continue;
                            case '#': verifyCompatibility(body.substring(1)); continue;
                            case 'x': addComment(Comment.xref, body.substring(1)); continue;
                            case '=': addComment(Comment.alias, body.substring(1)); continue;
                            case '~': addComment(Comment.variation, body.substring(1)); continue;
                            case ';': continue; // file comment
                            default: 
                                fileComments.put(0, originalLine);
                                break;
                                // throw new IllegalArgumentException("Huh? " + body);
                            }
                        } else if (originalLine.startsWith(";")) {
                            // file comment
                            continue;
                        } else {
                            int pos = originalLine.indexOf('\t');
                            final String x = originalLine.substring(0,pos);
                            lastCodePoint = Integer.parseInt(x,16);
                            if (data.containsKey(lastCodePoint)) {
                                throw new IllegalArgumentException("Duplicate code point");
                            }
                            data.put(lastCodePoint, lastDataItem = new Data());
                            subheads.put(lastCodePoint, subhead);
                            verifyName(originalLine, pos);                  
                        }
                    }
                } catch (final Exception e) {
                    throw new IllegalArgumentException("Error on line: " + originalLine, e);
                }
            }
        } catch (IOException e1) {
            throw new IllegalArgumentException();
        }
    }

    static final UnicodeSet HEX_AND_SPACE = new UnicodeSet("[0-9A-F\\ ]").freeze();

    public void verifyName(final String originalLine, int pos) {
        String realName = Default.ucd().getName(lastCodePoint);
        final String namelistName = originalLine.substring(pos+1);
        if (!realName.equals(namelistName)) {
            //addError("Bad name:", namelistName);
        }
    }

    public void addError(String message, String arg) {
        errors.put(lastCodePoint, message + ":\t" + arg);
    }

    private void verifyBlock(String string) {
        //errors.put(lastCodePoint, "Bad compat decomp:\t" + string);
        // <font> 0073 latin small letter s
    }

    private void verifyCompatibility(String string) {
//        errors.put(lastCodePoint, "Bad compat decomp:\t" + string);
        // <font> 0073 latin small letter s
    }

    private void verifyCanonical(String string) {
//        String can = Default.ucd().getDecompositionMapping(lastCodePoint);
//        if (HEX_AND_SPACE.containsAll(string)) {
//            string = Utility.fromHex(string.trim());
//            if (!string.equals(can)) {
//                errors.put(lastCodePoint, "Bad canonical decomp:\t" + string);
//            }
//        } else {
//            // 0300 combining grave accent
//            String[] parts = string.split("\\t");
//            if (parts.length == 2)
//            errors.put(lastCodePoint, "Bad canonical decomp:\t" + string);
//        }
    }

    private void addComment(Comment comment, String string) {
        String trim = string.trim();
        if (comment == Comment.xref) {
            //      (notched lower right-shadowed white rightwards arrow - 27AF)

//              TAB "x" SP CHAR SP LCNAME LF    
//            | TAB "x" SP CHAR SP "<" LCNAME ">" LF
//                // x is replaced by a right arrow
//
//            | TAB "x" SP "(" LCNAME SP "-" SP CHAR ")" LF    
//            | TAB "x" SP "(" "<" LCNAME ">" SP "-" SP CHAR ")" LF  
//                // x is replaced by a right arrow;
//                // (second type as used for control and noncharacters)
//
//                // In the forms with parentheses the "(","-" and ")" are removed
//                // and the order of CHAR and LCNAME is reversed;
//                // i.e. all inputs result in the same order of output
//
//            | TAB "x" SP CHAR LF
//                // x is replaced by a right arrow
//                // (this type is the only one without LCNAME 
//                // and is used for ideographs)
        }
        lastDataItem.comments.put(comment, trim);
    }

    public static void main(String[] args) {
        NamesList nl = new NamesList("NamesList");
        System.out.println("Preface" + "\t" + nl.preface);

        for (Entry<String, Data> dataItem : nl.data.entrySet()) {
            final String key = dataItem.getKey();
            String realName = Default.ucd().getName(key);

            final Data value = dataItem.getValue();
            if (value.isEmpty()) {
                continue;
            }
            System.out.print(Utility.hex(key) + "\t" + realName);
            System.out.println(value.display("\n\t", "\n\t\t"));
        }
        
        String lastSubhead = "";
        for (EntryRange dataItem : nl.subheads.entryRanges()) {
            if (dataItem.value == null || dataItem.value.equals(lastSubhead)) {
                continue;
            }
            System.out.println(Utility.hex(dataItem.codepoint) + "\t" + dataItem.value);
            lastSubhead = (String) dataItem.value;
        }

        for (Entry<Integer, Set<String>> dataItem : nl.errors.keyValuesSet()) {
            final Integer key = dataItem.getKey();
            final Set<String> values = dataItem.getValue();
            System.err.println(Utility.hex(key));
            for (String value : values) {
                System.err.println("\t" + value);
            }
        }

    }
}
