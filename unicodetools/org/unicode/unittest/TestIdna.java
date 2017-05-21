package org.unicode.unittest;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.text.Utilities;

import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.idna.Uts46;
import org.unicode.idna.Uts46.Errors;
import org.unicode.idna.Uts46.IdnaChoice;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Bidi_Class_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Idn_Status_Values;
import org.unicode.text.utility.Settings;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;

public class TestIdna extends TestFmwkPlus{
    public static void main(String[] args) {
        new TestIdna().run(args);
    }

    static IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);
    static UnicodeMap<Bidi_Class_Values> BIDI_CLASS = iup.loadEnum(UcdProperty.Bidi_Class, Bidi_Class_Values.class);

    public void testBackwardsCompatibility() {
        UnicodeMap<String> idnaMapping = iup.load(UcdProperty.Idn_Mapping);
        UnicodeMap<Idn_Status_Values> idnaStatus = iup.loadEnum(UcdProperty.Idn_Status, Idn_Status_Values.class);

        IndexUnicodeProperties iupLast = IndexUnicodeProperties.make(Settings.lastVersion);
        UnicodeMap<String> idnaMappingLast = iupLast.load(UcdProperty.Idn_Mapping);
        UnicodeMap<Idn_Status_Values> idnaStatusLast = iupLast.loadEnum(UcdProperty.Idn_Status, Idn_Status_Values.class);

        UnicodeMap<General_Category_Values> gcOld = iupLast.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
        UnicodeSet oldAssigned = new UnicodeSet(gcOld.getSet(General_Category_Values.Unassigned)).complement().freeze();

        for (String x : oldAssigned) {
            String versionString = " expected=v" + Settings.latestVersion + " actual=v" + Settings.lastVersion;
            assertEquals("mapping" + versionString, idnaMappingLast.get(x), idnaMapping.get(x));
            assertEquals("status" + versionString, idnaStatusLast.get(x), idnaStatus.get(x));
        }
    }
    public static final Splitter semi = Splitter.on(';').trimResults();

    public void testBroken() {
        String[][] tests = {
                {"B;    0à.\u05D0;  ;   xn--0-sfa.xn--4db   #   0à.א", "error"},
                {"B;    à.\u05D00\u0660\u05D0;  [B4];   [B4]    #   à.א0٠א", "noerror"},
                {"T;    \u200D。。\u06B9\u200C;   [C2 A4_2 B3 C1];    xn--skb #   ..ڹ", "error"}
        };
        for (String[] test : tests) {
            check(test[0], test[1].equals("error"));
        }
    }

    /*
#  Column 1: type - T for transitional, N for nontransitional, B for both
#  Column 2: source - the source string to be tested
#  Column 3: toUnicode - the result of applying toUnicode to the source, using nontransitional. A blank value means the same as the source value.
#  Column 4: toASCII - the result of applying toASCII to the source, using the specified type. A blank value means the same as the toUnicode value.
#  Column 5: NV8 - present if the toUnicode value would not be a valid domain name under IDNA2008. Not a normative field.
     */

    enum Type {T, N, B};
    private void check(String test, boolean errors) {
        List<String> parts = semi.splitToList(test);
        Type type = Type.valueOf(parts.get(0));
        String source = parts.get(1);
        String toUnicode = parts.get(2);
        if (toUnicode.isEmpty()) {
            toUnicode = source;
        }
        String toAscii = parts.get(3);
        if (toAscii.isEmpty()) {
            toAscii = toUnicode;
        }
        //boolean NV8 = parts.

        System.out.println(test + " — ICU says errors = " + errors);
        System.out.println("source: " + showBidi(source));

        final Set<Errors> toUnicodeErrors = new LinkedHashSet<Errors>();
        final String unicode = Uts46.SINGLETON.toUnicode(source, IdnaChoice.nontransitional, toUnicodeErrors);
        System.out.println("toUnicode: " + showBidi(unicode) + "; " + toUnicodeErrors + "; ");

        if (type == Type.B || type == Type.T) {
            final Set<Errors> transitionalErrors = new LinkedHashSet<Errors>();
            final String transitional = Uts46.SINGLETON.toASCII(source, IdnaChoice.transitional, transitionalErrors);
            System.out.println("toAsciiT: " + transitional + "; " + transitionalErrors);
        }

        if (type == Type.B || type == Type.N) {
            final Set<Errors> nonTransitionalErrors = new LinkedHashSet<Errors>();
            final String nontransitional = Uts46.SINGLETON.toASCII(source, IdnaChoice.nontransitional, nonTransitionalErrors);
            System.out.println("toAsciiN: " + nontransitional + "; " + nonTransitionalErrors);
        }
        System.out.println();
    }

    static final char LRM = '\u200E';

    private String showBidi(String source) {
        StringBuilder b = new StringBuilder();
        for (int cp : CharSequences.codePoints(source)) {
            if (b.length() != 0) {
                b.append(' ');
            }
            b.append(LRM).appendCodePoint(cp).append(LRM)
            .append('[').append(Utility.hex(cp))
            .append('/').append(BIDI_CLASS.get(cp).getShortName())
            .append(']')
            ;
        }
        return b.toString();
    }

    /**
  An RTL label is a label that contains at least one character of type
   R, AL, or AN.

   An LTR label is any label that is not an RTL label.

      The following rule, consisting of six conditions, applies to labels
   in Bidi domain names.  The requirements that this rule satisfies are
   described in Section 3.  All of the conditions must be satisfied for
   the rule to be satisfied.

   1.  The first character must be a character with Bidi property L, R,
       or AL.  If it has the R or AL property, it is an RTL label; if it
       has the L property, it is an LTR label.

   2.  In an RTL label, only characters with the Bidi properties R, AL,
       AN, EN, ES, CS, ET, ON, BN, or NSM are allowed.

   3.  In an RTL label, the end of the label must be a character with
       Bidi property R, AL, EN, or AN, followed by zero or more
       characters with Bidi property NSM.

   4.  In an RTL label, if an EN is present, no AN may be present, and
       vice versa.

   5.  In an LTR label, only characters with the Bidi properties L, EN,
       ES, CS, ET, ON, BN, or NSM are allowed.

   6.  In an LTR label, the end of the label must be a character with
       Bidi property L or EN, followed by zero or more characters with
       Bidi property NSM.
     */
    
    public void testFinalDot() {
        String source = "a.b．c。d｡";
        String expected = "a.b.c.d.";
        final Set<Errors> toUnicodeErrors = new LinkedHashSet<Errors>();
        final String actual = Uts46.SINGLETON.toUnicode(source, IdnaChoice.nontransitional, toUnicodeErrors);
        assertEquals("toUnicode(" + source + "):", expected, actual);
    }
}
