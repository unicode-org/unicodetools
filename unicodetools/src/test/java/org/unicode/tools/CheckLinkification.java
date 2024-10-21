package org.unicode.tools;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.text.utility.Utility;

public class CheckLinkification {
    public static void main(String[] args) {
        new CheckLinkification().check();
    }

    private void check() {
        checkOverlap();
        showLinkTermination();
        showLinkPairedOpeners();
    }

    enum LinkTermination {
        None("[\\p{ANY}]"), // overridden by following
        Hard("[[\\p{whitespace}\\p{NChar}\\p{C}]-\\p{Cf}]"),
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
                                        : ".." + Utility.hex(range.codepoint));
                System.out.println(
                        rangeString
                                + ";"
                                + " ".repeat(15 - rangeString.length())
                                + lt
                                + "\t# "
                                + "("
                                + UCharacter.getStringPropertyValue(
                                        UProperty.GENERAL_CATEGORY,
                                        range.codepoint,
                                        NameChoice.SHORT)
                                + ") "
                                + quote(UCharacter.getExtendedName(range.codepoint))
                                + (range.codepoint == range.codepointEnd
                                        ? ""
                                        : ".."
                                                + "("
                                                + UCharacter.getStringPropertyValue(
                                                        UProperty.GENERAL_CATEGORY,
                                                        range.codepoint,
                                                        NameChoice.SHORT)
                                                + ") "
                                                + quote(
                                                        UCharacter.getExtendedName(
                                                                range.codepointEnd))));
            }
            System.out.println();
        }
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
            final int value2 = UCharacter.getBidiPairedBracket(cp);
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
}
