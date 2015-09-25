package org.unicode.text.UCA;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class NamesList {

    private static final String DOTTED_BOX = "⬚";
    private static final String DOTTED_CIRCLE = "◌";

    static final ToolUnicodePropertySource US = ToolUnicodePropertySource.make(Settings.latestVersion);
    static final UnicodeSet TO_SUPPRESS = new UnicodeSet(US.getProperty("Default_Ignorable_Code_Point").getSet("True"))
    .addAll(US.getProperty("GC").getSet("Cc"))
    .addAll(US.getProperty("GC").getSet("Zs"))
    .addAll(US.getProperty("GC").getSet("Zl"))
    .addAll(US.getProperty("GC").getSet("Zp"))
    ;
    static final UnicodeSet COMBINING = new UnicodeSet()
    .addAll(US.getProperty("GC").getSet("Mc"))
    .addAll(US.getProperty("GC").getSet("Me"))
    .addAll(US.getProperty("GC").getSet("Mn"))
    ;

    public enum Comment {
        comment("•", true), 
        formalAlias("※", false),
        alias("=", true), 
        xref("→", true),
        variation("➲", false),
        canonical("≡", false),
        compatibility("≈", false);
        public final String symbol;
        public final boolean keep;
        Comment(String symbol, boolean keep) {
            this.symbol = symbol;
            this.keep = false;
        }
    }

    static final String CHAR = "(10[A-F0-9]{2,4}|[A-F0-9]{4,5})";
    static final String LCNAME = "[-<>0-9A-Za-z() ]+";
    static final String SP = "\\s+";
    static final String OSP = "\\s*";
    static final Pattern CHAR_PATTERN = Pattern.compile(CHAR);
    //static final Matcher INVISIBLE = UnicodeRegex.compile(TO_SUPPRESS.toPattern(true)).matcher("");

    static String transform(String input, Matcher m, Transform<String,String> transform) {
        StringBuilder result = null;
        m.reset(input);
        for (int start = 0; start < input.length();) {
            if (m.find(start)) {
                if (result == null) {
                    result = new StringBuilder();
                }
                result
                .append(input.substring(start, m.start()))
                .append(transform.transform(m.group()));
                start = m.end();
            } else if (result == null) {
                return input;
            } else {
                result.append(input.substring(start));
                break;
            }
        }
        return result.toString();
    }

    static Transform<String,String> CODE_CHAR = new Transform<String,String>() {
        @Override
        public String transform(String source) {
            final int cp = Integer.parseInt(source, 16);
            return source + " " + (TO_SUPPRESS.contains(cp) ? DOTTED_BOX : 
                (COMBINING.contains(cp) ? DOTTED_CIRCLE : "") + UTF16.valueOf(cp));
        }
    };

    public static final Transform<String,String> CODE = new Transform<String,String>() {
        @Override
        public String transform(String source) {
            return TO_SUPPRESS.contains(source) ? DOTTED_BOX : 
                (COMBINING.contains(source) ? DOTTED_CIRCLE : "") + source;
        }
    };

    static Matcher XREF1 = Pattern.compile(CHAR + SP + LCNAME).matcher("");
    static Matcher XREF2 = Pattern.compile("\\(" + LCNAME + SP + "-" + SP + CHAR + "\\)").matcher("");
    static Matcher XREF3 = CHAR_PATTERN.matcher("");

    class Data {
        final Relation<Comment,String> comments = Relation.of(new EnumMap<Comment,Set<String>>(Comment.class), LinkedHashSet.class);
        int codePoint;
        void set(int lastCodePoint) {
            codePoint = lastCodePoint;
        }
        public String display(String sep, String sep2, String sep3) {
            StringBuilder b = new StringBuilder();
            for (Entry<Comment, Set<String>> entry : comments.keyValuesSet()) {
                for (String x : entry.getValue()) {
                    if (entry.getKey() == Comment.xref) {
                        String realName = Default.ucd().getName(x);
                        x = Utility.hex(x) + " " + CODE.transform(x) + " " + realName;
                    }
                    b.append(sep)
                    .append(entry.getKey().symbol)
                    .append(sep2)
                    .append(x)
                    .append(sep3)
                    ;
                }
            }
            return b.toString();
        }
        public boolean isEmpty() {
            return comments.isEmpty();
        }
        @Override
        public String toString() {
            return comments.toString();
        }
        public void addComment(Comment comment, String string, boolean fixCodePoints) {
            String trim = string.trim();
            if (comment == Comment.xref) {
                Matcher m = match(trim, XREF1, XREF2, XREF3);
                int cp = Integer.parseInt(m.group(1), 16);
                trim = UTF16.valueOf(cp);
                if (discardXref(trim)) {
                    return;
                }
            } else {
                if (trim.startsWith("uppercase is ") || trim.startsWith("lowercase is ")) {
                    System.err.println("Discarding case variant for: " + Utility.hex(codePoint) + " => " + trim);  
                    return;
                }
                if (fixCodePoints) {
                    trim = transform(trim, XREF3, CODE_CHAR);
                }
            }
            comments.put(comment, trim);
        }
        private boolean discardXref(String trim) {
            for (byte width = 0; width < 2; ++width) {
                for (byte caseType = 0; caseType < UCD_Types.LIMIT_CASE ; ++caseType) {
                    String changed = Default.ucd().getCase(codePoint, UCD_Types.FULL, UCD_Types.UPPER);
                    if (changed.equals(trim)) {
                        System.err.println("Discarding case variant for: " + Utility.hex(codePoint) + " => " + Utility.hex(trim));
                        return true;
                    }
                }
            }
            return false;
        }
        void storeData() {
            if (codePoint >= 0) {
                String comment = null;
                String alias = null;
                String xrefs = null;
                for (Entry<Comment, Set<String>> entry : comments.keyValuesSet()) {
                    switch (entry.getKey()) {
                    case comment: 
                        comment = CollectionUtilities.join(entry.getValue(), "\n");
                        break;
                    case alias: 
                        alias = CollectionUtilities.join(entry.getValue(), "\n");
                        break;
                    case xref: 
                        xrefs = CollectionUtilities.join(entry.getValue(), "\n");
                        break;
                    }
                }
                informalAliases.put(codePoint, alias);
                informalComments.put(codePoint, comment);
                informalXrefs.put(codePoint, xrefs);
            }
            comments.clear();
        }
    }

    //UnicodeMap<Data> data = new UnicodeMap<>();

    public UnicodeMap<String> informalAliases = new UnicodeMap<>();
    public UnicodeMap<String> informalComments = new UnicodeMap<>();
    public UnicodeMap<String> informalXrefs = new UnicodeMap<>();

    public UnicodeMap<String> subheads = new UnicodeMap();
    public UnicodeMap<String> subheadComments = new UnicodeMap();

    public Relation<Integer, String> errors = Relation.of(new TreeMap<Integer,Set<String>>(), LinkedHashSet.class);
    public Relation<Integer, String> fileComments = Relation.of(new TreeMap<Integer,Set<String>>(), LinkedHashSet.class);

    int lastCodePoint = -1;
    Data lastDataItem = new Data();

    public NamesList(String file, String ucdVersion) {
        BufferedReader in = Utility.openUnicodeFile(file, ucdVersion, true, Utility.LATIN1_WINDOWS);
        int i = 0;
        String subhead = null;
        String subheadComment = null;
        try {
            while (true) {
                String originalLine = in.readLine();
                if (originalLine == null) {
                    break;
                }
                ++i;
                if (originalLine.isEmpty()) {
                    continue;
                }
                try {
                    boolean fixCodePoints = true;
                    if (originalLine.startsWith("@+")) {
                        fixCodePoints = false;
                        originalLine = originalLine.substring(2);
                    }
                    if (originalLine.startsWith("@")) {
                        final String line = originalLine.substring(1);
                        if (line.equals("@+")) {
                            // skip
                        } else if (line.startsWith("+")) {
                            String temp = line.substring(1).trim();
                            if (!temp.startsWith("*")) {
                                subheadComment = temp;
                            } else {
                                lastDataItem.addComment(Comment.comment, temp.substring(1), fixCodePoints);
                            }
                        } else if (line.startsWith("@@")) {
                            // title
                        } else if (line.startsWith("@")) {
                            verifyBlock(line.substring(1).trim());
                            subheadComment = null;
                            subhead = null;
                        } else {
                            subhead = line.substring(1).trim();
                        }
                    } else {
                        if (originalLine.startsWith("\t")) {
                            final String body = originalLine.trim();
                            final char firstChar = body.charAt(0);
                            switch (firstChar) {
                            case '*': 
                                lastDataItem.addComment(Comment.comment, body.substring(1), fixCodePoints); continue;
                            case '%': 
                                lastDataItem.addComment(Comment.formalAlias, body.substring(1), false); continue;
                            case ':': 
                                verifyCanonical(body.substring(1)); continue;
                            case '#': 
                                verifyCompatibility(body.substring(1)); continue;
                            case 'x': 
                                lastDataItem.addComment(Comment.xref, body.substring(1), false); continue;
                            case '=': 
                                lastDataItem.addComment(Comment.alias, body.substring(1), false); continue;
                            case '~': 
                                lastDataItem.addComment(Comment.variation, body.substring(1), false); continue;
                            case ';': 
                                continue; // file comment
                            default: 
                                originalLine = originalLine.trim();
                                if (fixCodePoints) {
                                    originalLine = transform(originalLine, XREF3, CODE_CHAR);
                                }
                                subheadComment = subheadComment == null ? originalLine 
                                        : subheadComment + "\n" + originalLine;
                                //fileComments.put(0, originalLine);
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
                            //                            if (data.containsKey(lastCodePoint)) {
                            //                                throw new IllegalArgumentException("Duplicate code point");
                            //                            }
                            subheads.put(lastCodePoint, subhead);
                            lastDataItem.storeData();
                            subheadComments.put(lastCodePoint, subheadComment);
                            lastDataItem.set(lastCodePoint);
                            verifyName(originalLine, pos);                  
                        }
                    }
                } catch (final Exception e) {
                    throw new IllegalArgumentException("Error on line: " + originalLine, e);
                }
            }
            lastDataItem.storeData();
            informalAliases.freeze();
            informalComments.freeze();
            informalXrefs.freeze();
            subheads.freeze();
            subheadComments.freeze();
            errors.freeze();
            fileComments.freeze();
        } catch (IOException e1) {
            throw new IllegalArgumentException();
        }
    }

    private <T> void close(UnicodeMap<T> subheads2) {
        T lastValue = null;
        int lastEnd = -1;
        UnicodeMap<T> filler = new UnicodeMap<T>();
        for (EntryRange entryRange : subheads.entryRanges()) {
            if (entryRange.value == null) {
                continue;
            }
            if (entryRange.value.equals(lastValue)) {
                filler.putAll(lastEnd+1, entryRange.codepoint-1, lastValue);
            }
            lastEnd = entryRange.codepointEnd;
            lastValue = (T) entryRange.value;
        }
        System.out.println("size: " + subheads.getRangeCount() + "\n" + subheads2);
        subheads2.putAll(filler);
        System.out.println("size: " + subheads.getRangeCount() + "\n" + subheads2);
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

    static final Pattern BLOCK = Pattern.compile("([A-F0-9]{4,6})\\s+(.*?)\\s+([A-F0-9]{4,6})");

    private void verifyBlock(String string) {
        final Matcher m = BLOCK.matcher(string);
        if (!m.matches()) {
            System.err.println("Bad Match: " + string);
        }
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

    public static Matcher match(String string, Matcher... matchers) {
        for (Matcher m : matchers) {
            if (m.reset(string).matches()) {
                return m;
            }
        }
        //        if (XREF1.matches()) {
        //            m = XREF1;
        //        } else if (XREF2.matches()) {
        //            m = XREF2;
        //        } else if (XREF3.matches()) {
        //            m = XREF3;
        //        } else {
        //            System.err.println("Failed to read: x " + string);
        //        }
        return null;
    }
}
