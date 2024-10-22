package org.unicode.tools;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import java.util.Stack;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.text.utility.Utility;

public class CheckLinkification {
    public static void main(String[] args) {
        new CheckLinkification().check();
    }

    private void check() {
        checkParsePathQueryFragment();
        checkOverlap();
        showLinkTermination();
        showLinkPairedOpeners();
    }

    public enum LinkTermination {
        None("[\\p{ANY}]"), // overridden by following
        Hard("[\\p{whitespace}\\p{NChar}\\p{C}]"),
        Soft("[\\p{Term}[‘-‛ ‹ › \"“-‟ « »']]"),
        Closing("\\p{Bidi_Paired_Bracket_Type=Close}"),
        Opening("\\p{Bidi_Paired_Bracket_Type=Open}"),
        ;

        final UnicodeSet base;

        private LinkTermination(String uset) {
            this.base = new UnicodeSet(uset).freeze();
        }

        static final UnicodeMap<LinkTermination> Property = new UnicodeMap<>();

        static {
            for (LinkTermination lt : values()) {
                Property.putAll(lt.base, lt);
            }
            Property.freeze();
        }
    }

    void checkOverlap() {
        for (LinkTermination lt : LinkTermination.values()) {
            if (lt == lt.None) {
                continue;
            }
            UnicodeSet propValue = lt.Property.getSet(lt);
            if (!propValue.equals(lt.base)) {
                System.out.print("Overlap");
            }
        }
    }

    void showLinkTermination() {
        for (LinkTermination lt : LinkTermination.values()) {
            UnicodeSet value = LinkTermination.Property.getSet(lt);
            String name = lt.toString();
            System.out.println("\n#\tLinkTermination=" + name);
            if (lt == lt.None) {
                System.out.println("#   " + "(All code points without other values)");
                continue;
            } else {
                System.out.println("#   draft = " + lt.base);
            }
            if (lt == LinkTermination.Hard) {
                value.removeAll(new UnicodeSet("[\\p{Cn}\\p{Cs}]"));
                System.out.println("#   (not listing Unassigned or Surrogates)");
            }
            System.out.println();
            for (EntryRange range : value.ranges()) {
                final String rangeString =
                        Utility.hex(range.codepoint)
                                + (range.codepoint == range.codepointEnd
                                        ? ""
                                        : ".." + Utility.hex(range.codepointEnd));
                System.out.println(
                        rangeString
                                + ";"
                                + " ".repeat(15 - rangeString.length())
                                + lt
                                + "\t# "
                                + "("
                                + getGeneralCategory(
                                        UProperty.GENERAL_CATEGORY,
                                        range.codepoint,
                                        NameChoice.SHORT)
                                + ") "
                                + quote(UCharacter.getExtendedName(range.codepoint))
                                + (range.codepoint == range.codepointEnd
                                        ? ""
                                        : ".."
                                                + "("
                                                + getGeneralCategory(
                                                        UProperty.GENERAL_CATEGORY,
                                                        range.codepointEnd,
                                                        NameChoice.SHORT)
                                                + ") "
                                                + quote(
                                                        UCharacter.getExtendedName(
                                                                range.codepointEnd))));
            }
            System.out.println();
        }
    }

    public String getGeneralCategory(int property, int codePoint, int nameChoice) {
        //        return UCharacter.getStringPropertyValue(
        //                UProperty.GENERAL_CATEGORY, codePoint, NameChoice.SHORT);
        return UCharacter.getPropertyValueName(
                property, UCharacter.getIntPropertyValue(codePoint, property), nameChoice);
    }

    static String quote(String s) {
        return TransliteratorUtilities.toHTML.transform(s);
    }

    void showLinkPairedOpeners() {
        UnicodeSet value = LinkTermination.Property.getSet(LinkTermination.Closing);

        System.out.println("\n#\tLinkPairedOpeners");
        System.out.println("#   draft = BidiPairedBracket");
        System.out.println();

        for (String cpString : value) {
            int cp = cpString.codePointAt(0);
            String hex = Utility.hex(cp);
            final int value2 = getOpening(cp);
            System.out.println(
                    hex
                            + ";"
                            + " ".repeat(7 - hex.length())
                            + Utility.hex(value2)
                            + "\t#"
                            + " “"
                            + quote(UTF16.valueOf(cp))
                            + "” "
                            + UCharacter.getExtendedName(cp)
                            + " 🡆 "
                            + " “"
                            + quote(UTF16.valueOf(value2))
                            + "” "
                            + UCharacter.getExtendedName(value2));
        }
    }

    public int getOpening(int cp) {
        return UCharacter.getBidiPairedBracket(cp);
    }

    public enum Part {
        PATH('/', "[?#]"),
        QUERY('?', "[#]"),
        FRAGMENT('#', "[]");
        final int initiator;
        final UnicodeSet terminators;

        private Part(char initiator, String terminators) {
            this.initiator = initiator;
            this.terminators = new UnicodeSet(terminators);
        }

        static Part fromInitiator(int cp) {
            for (Part part : Part.values()) {
                if (part.initiator == cp) {
                    return part;
                }
            }
            return null;
        }
    }

    /**
     * Set lastSafe to 0 — this marks the last code point that is definitely included in the
     * linkification.<br>
     * Set closingStack to empty<br>
     * Set the current code point position i to 0<br>
     * Loop from i = 0 to n<br>
     * Set LT to LinkTermination(cp[i])<br>
     * If LT == none, set lastSafe to be i+1, continue loop<br>
     * If LT == soft, continue loop<br>
     * If LT == hard, stop linkification and return lastSafe<br>
     * If LT == opening, push cp[i] onto closingStack<br>
     * If LT == closing, set open to the pop of closingStack, or 0 if the closingStack is empty<br>
     * If LinkPairedOpeners(cp[i]) == open, set lastSafe to be i+1, continue loop.<br>
     * Otherwise, stop linkification and return lastSafe<br>
     * If lastSafe == n+1, then the entire part is safe; continue to the next part<br>
     * Otherwise, stop linkification and return lastSafe<br>
     */
    int parsePathQueryFragment(String source, int codePointOffset) {
        int[] codePoints = source.codePoints().toArray();
        int lastSafe = codePointOffset;
        Part part = null;
        Stack<Integer> openingStack = new Stack<>();
        for (int i = codePointOffset; i < codePoints.length; ++i) {
            int cp = codePoints[i];
            if (part == null) {
                part = Part.fromInitiator(cp);
                if (part == null) {
                    return i; // failed, don't move cursor
                }
                lastSafe = i + 1;
                continue;
            }
            LinkTermination lt = LinkTermination.Property.get(cp);
            switch (lt) {
                case None:
                    if (part.terminators.contains(cp)) {
                        lastSafe = i;
                        part = Part.fromInitiator(cp);
                        if (part == null) {
                            return lastSafe;
                        }
                    }
                    lastSafe = i + 1;
                    break;
                case Soft: // no action
                    break;
                case Hard:
                    return lastSafe;
                case Opening:
                    openingStack.push(cp);
                    break;
                case Closing:
                    if (openingStack.empty()) {
                        return lastSafe;
                    }
                    int matchingOpening = getOpening(cp);
                    Integer topOfStack = openingStack.pop();
                    if (matchingOpening == topOfStack) {
                        lastSafe = i + 1;
                        break;
                    } // else failed to match
                    return lastSafe;
            }
        }
        return codePoints.length;
    }

    /** TODO: change into test */
    public void checkParsePathQueryFragment() {
        String[][] tests = {
            {"abc", "|abc"},
            {"/abc", "/abc|"},
            {"?abc", "?abc|"},
            {"#abc", "#abc|"},
            // complex
            {"/abc/qrs?def#ghi", "/abc/qrs?def#ghi|"},
            // soft vs hard
            {"/abc/qrs?d.ef#ghi", "/abc/qrs?d.ef#ghi|"},
            {"/abc/qrs?d ef#ghi", "/abc/qrs?d| ef#ghi"},
            {"/abc/qrs?d. ef#ghi", "/abc/qrs?d|. ef#ghi"},
            // ordering
            {"/a/bc?d/e?f#g/h?i#j", "/a/bc?d/e?f#g/h?i#j|"},
            // opening/closing
            {"/a(c)", "/a(c)|"},
            {"/ac)", "/ac|)"},
            {"/ab(c/q)rs?def#ghi", "/ab(c/q)rs?def#ghi|"},
        };
        for (String[] test : tests) {
            String source = test[0];
            String expected = test[1];
            int parseResult = parsePathQueryFragment(source, 0);
            String actual = source.substring(0, parseResult) + "|" + source.substring(parseResult);
            System.out.println(
                    (expected.equals(actual) ? "OK" : "ERROR")
                            + " "
                            + source
                            + " expected: "
                            + expected
                            + " actual: "
                            + actual);
        }
    }
}
