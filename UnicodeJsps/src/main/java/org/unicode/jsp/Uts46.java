package org.unicode.jsp;

import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;

public class Uts46 extends Idna {

    public static Uts46 SINGLETON = new Uts46();

    private Uts46() {
        new MyHandler().process(Uts46.class, "IdnaMappingTable.txt");
        types.freeze();
        mappings.freeze();
        mappings_display.freeze();
        validSet = new UnicodeSet(types.getSet(IdnaType.valid)).freeze();
        validSet_transitional = new UnicodeSet(validSet).addAll(types.getSet(IdnaType.deviation)).freeze();
        checkPunycodeValidity = true;
    } // private

    class MyHandler extends FileUtilities.SemiFileReader {

        public boolean handleLine(int start, int end, String[] items) {
            String status = items[1];
            int dash = status.indexOf("_STD3");
            if (dash >= 0) {
                status = status.substring(0, dash);
            }
            IdnaType type = IdnaType.valueOf(status);
            types.putAll(start, end, type);

            String value;
            switch (type) {
            case mapped:
                value = Utility.fromHex(items[2], 4, " ");
                break;
            case deviation:
                if (items.length > 2) {
                    value = Utility.fromHex(items[2], 4, " ");
                } else {
                    value = "";
                }
                break;
            case ignored:
                value = "";
                break;
            case disallowed:
            case valid:
            default:
                value = null;
                break;
            }
            if (mappings != null) {
                mappings.putAll(start, end, value);
                if (type != IdnaType.deviation) {
                    mappings_display.putAll(start, end, value);
                }
            }
            return true;
        }
    }

    // private static final String LABEL_SEPARATOR_STRING =
    // "[\u002E\uFF0E\u3002\uFF61]";
    // static UnicodeSet LABEL_SEPARATORS = new
    // UnicodeSet(LABEL_SEPARATOR_STRING);
    // static UnicodeSet DEVIATIONS = new
    // UnicodeSet("[\\u200C \\u200D \u00DF \u03C2]");
    // //static final String[] IdnaNames = { "valid", "ignored", "mapped",
    // "disallowed" };
    //
    // static UnicodeSet Uts46Chars =
    // UnicodeSetUtilities.parseUnicodeSet("[[:any:]" +
    // " - [:c:] - [:z:]" +
    // " - [:Block=Ideographic_Description_Characters:]" +
    // "- [:ascii:] - [\uFFFC \uFFFD]" +
    // " [\\u002D\\u002Ea-zA-Z0-9]" +
    // "]", TableStyle.simple)
    // .freeze();
    // static UnicodeSet UtsExclusions = UnicodeSetUtilities.parseUnicodeSet("["
    // +
    // "[\u04C0\u10A0-\u10C5\u2132\u2183]" +
    // "[\\U0002F868 \\U0002F874 \\U0002F91F \\U0002F95F \\U0002F9BF]" +
    // "[\\u1806 \\uFFFC \\uFFFD \\u17B4 \\u17B5 \\u115F \\u1160\\u3164\\uFFA0]"
    // +
    // "]", TableStyle.simple);
    // static {
    // for (int i = 0; i <= 0x10FFFF; ++i) {
    // if (LABEL_SEPARATORS.contains(i)) continue;
    // String temp = UnicodeSetUtilities.MyNormalize(i, Normalizer.NFKC);
    // if (LABEL_SEPARATORS.containsSome(temp)) {
    // UtsExclusions.add(i);
    // }
    // }
    // UtsExclusions.freeze();
    // }
    // static UnicodeSet Uts46CharsDisplay =
    // UnicodeSetUtilities.parseUnicodeSet("[\u00DF\u03C2\u200D\u200C]",
    // TableStyle.simple).addAll(Uts46.Uts46Chars).freeze();
    //
    // static StringTransform nfkcCasefold = new UnicodeSetUtilities.NFKC_CF();
    // // Transliterator.getInstance("nfkc; casefold; [:di:] remove; nfkc;");
    // static StringTransform foldDisplay = new
    // UnicodeUtilities.FilteredStringTransform(DEVIATIONS, nfkcCasefold);
    // static StringTransform fixIgnorables =
    // Transliterator.createFromRules("foo",
    // LABEL_SEPARATOR_STRING + "> \\u002E ;" +
    // "[\\u200E\\u200F\\u202A-\\u202E\\u2061-\\u2063\\u206A-\\u206F\\U0001D173-\\U0001D17A\\U000E0001\\U000E0020-\\U000E007F] > \\uFFFF;"
    // +
    // "[᠆] > ;" +
    // "[\\u17B4\\u17B5\\u115F\\u1160\\u3164\\uFFA0] > \\uFFFF",
    // Transliterator.FORWARD);
    //
    // static String toUts46(String line) {
    // String line2 = fixIgnorables.transform(line);
    // return nfkcCasefold.transform(line2);
    // }

    /*
     * http://datatracker.ietf.org/doc/draft-ietf-idnabis-bidi/, version 07 An
     * RTL label is a label that contains at least one character of type R, AL
     * or AN.
     * 
     * An LTR label is any label that is not an RTL label.
     * 
     * A "BIDI domain name" is a domain name that contains at least one RTL
     * label.
     * 
     * 1. The first character must be a character with BIDI property L, R or AL.
     * If it has the R or AL property, it is an RTL label; if it has the L
     * property, it is an LTR label.
     * 
     * 2. In an RTL label, only characters with the BIDI properties R, AL, AN,
     * EN, ES, CS, ET, ON, BN and NSM are allowed.
     * 
     * 3. In an RTL label, the end of the label must be a character with BIDI
     * property R, AL, EN or AN, followed by zero or more characters with BIDI
     * property NSM.
     * 
     * 4. In an RTL label, if an EN is present, no AN may be present, and vice
     * versa.
     * 
     * 5. In an LTR label, only characters with the BIDI properties L, EN, ES,
     * CS. ET, ON, BN and NSM are allowed.
     * 
     * 6. In an LTR label, the end of the label must be a character with BIDI
     * property L or EN, followed by zero or more characters with BIDI property
     * NSM.
     */

    static final UnicodeSet R_AL_AN                       = new UnicodeSet("[[:bc=R:][:bc=AL:][:bc=AN:]]").freeze();
    static final UnicodeSet R_AL                          = new UnicodeSet("[[:bc=R:][:bc=AL:]]").freeze();
    static final UnicodeSet L                             = new UnicodeSet("[[:bc=L:]]").freeze();
    static final UnicodeSet ES_CS_ET_ON_BN_NSM            = new UnicodeSet("[[:bc=ES:][:bc=CS:][:bc=ET:][:bc=ON:][:bc=BN:][:bc=NSM:]]").freeze();
    static final UnicodeSet R_AL_AN_EN                    = new UnicodeSet("[[:bc=R:][:bc=AL:][:bc=AN:][:bc=EN:]]").freeze();
    static final UnicodeSet R_AL_AN_EN_ES_CS_ET_ON_BN_NSM = new UnicodeSet(R_AL_AN_EN).addAll(ES_CS_ET_ON_BN_NSM).freeze();
    static final UnicodeSet L_EN                          = new UnicodeSet("[[:bc=L:][:bc=EN:]]").freeze();
    static final UnicodeSet L_EN_ES_CS_ET_ON_BN_NSM       = new UnicodeSet(L_EN).addAll(ES_CS_ET_ON_BN_NSM).freeze();
    static final UnicodeSet EN                            = new UnicodeSet("[[:bc=EN:]]").freeze();
    static final UnicodeSet AN                            = new UnicodeSet("[[:bc=AN:]]").freeze();
    static final UnicodeSet NSM                           = new UnicodeSet("[[:bc=NSM:]]").freeze();
    static final Pattern    LABEL_SEPARATOR               = Pattern.compile("\\.");

    /**
     * Checks a string for IDNA2008 bidi errors
     * 
     * @param domainName
     *            the string to be tested
     * @param errors
     *            if an error is found, then an error string is added to this
     *            set.
     * @return true if errors are found, otherwise false.
     */
    public static boolean hasBidiError(String domainName, Set<Errors> errors) {
        if (!R_AL_AN.containsSome(domainName)) {
            return false;
        }
        int oldErrorLength = errors.size();

        String[] labels = nonbrokenSplit(LABEL_SEPARATOR, domainName);
        for (String label : labels) {
            if (label.length() <= 0)
                continue; // broken, but for a non-bidi reason
            // #1
            int firstChar = label.codePointAt(0);

            // 1. The first character must be a character with BIDI property L,
            // R
            // or AL. If it has the R or AL property, it is an RTL label; if it
            // has the L property, it is an LTR label.

            boolean RTL = R_AL.contains(firstChar);
            boolean LTR = L.contains(firstChar);
            if (!RTL && !LTR) {
                errors.add(Errors.B1);
            }

            // 2. In an RTL label, only characters with the BIDI properties R,
            // AL,
            // AN, EN, ES, CS, ET, ON, BN and NSM are allowed.

            if (RTL && !R_AL_AN_EN_ES_CS_ET_ON_BN_NSM.containsAll(label)) {
                errors.add(Errors.B2);
            }

            int endExcludingNSM = NSM.spanBack(label, SpanCondition.CONTAINED);
            if (endExcludingNSM == 0) {
                // degenerate case, fails Bs 3 and 6
                errors.add(Errors.B3);
                errors.add(Errors.B6);
                return true;
            }
            int lastChar = Character.codePointBefore(label, endExcludingNSM);

            // 3. In an RTL label, the end of the label must be a character with
            // BIDI property R, AL, EN or AN, followed by zero or more
            // characters with BIDI property NSM.

            if (RTL && !R_AL_AN_EN.contains(lastChar)) {
                errors.add(Errors.B3);
            }

            // 4. In an RTL label, if an EN is present, no AN may be present,
            // and
            // vice versa.
            if (RTL && EN.containsSome(label) && AN.containsSome(label)) {
                errors.add(Errors.B4);
            }
            // 5. In an LTR label, only characters with the BIDI properties L,
            // EN,
            // ES, CS. ET, ON, BN and NSM are allowed.
            if (LTR && !L_EN_ES_CS_ET_ON_BN_NSM.containsAll(label)) {
                errors.add(Errors.B5);
            }

            // 6. In an LTR label, the end of the label must be a character with
            // BIDI property L or EN, followed by zero or more characters with
            // BIDI property NSM.

            if (LTR && !L_EN.contains(lastChar)) {
                errors.add(Errors.B6);
            }
        }
        return errors.size() > oldErrorLength;
    }

    public static String[] nonbrokenSplit(Pattern pattern, String input) {
        ArrayList<String> matchList = new ArrayList<String>();
        Matcher m = pattern.matcher(input);
        int lastPos = 0;
        while (true) {
            boolean found = m.find();
            int start = found ? m.start() : input.length();
            matchList.add(input.substring(lastPos, start));
            if (!found)
                break;
            lastPos = m.end();
        }
        return matchList.toArray(new String[matchList.size()]);
    }

    static final UnicodeSet JOINER_SET = new UnicodeSet("[\u200C\u200D]");
    static final UnicodeSet VIRAMAS    = new UnicodeSet("[:ccc=virama:]");
    static final UnicodeSet T          = new UnicodeSet("[:jt=T:]");
    static final UnicodeSet L_D        = new UnicodeSet("[[:jt=L:][:jt=D:]]");
    static final UnicodeSet R_D        = new UnicodeSet("[[:jt=R:][:jt=D:]]");

    static final Pattern    JOINERS    = Pattern.compile("[\u200C\u200D]");
    static final int        NON_JOINER = 0x200C;
    static final int        JOINER     = 0x200D;

    // U+200C ( ) ZERO WIDTH NON-JOINER
    // U+200D ( ) ZERO WIDTH JOINER

    public static boolean hasContextJError(String domain, Set<Errors> errors) {
        // skip if there are no joiners
        if (!JOINER_SET.containsSome(domain)) {
            return false;
        }
        int oldErrorLength = errors.size();
        // because of the way these test, we don't need to break into labels
        int cp;
        for (int i = 0; i < domain.length(); i += Character.charCount(cp)) {
            cp = domain.codePointAt(i);
            if (cp == NON_JOINER) {
                // Appendix A.1. ZERO WIDTH NON-JOINER
                // Code point:
                // U+200C
                // Overview:
                // This may occur in a formally cursive script (such as Arabic)
                // in a
                // context where it breaks a cursive connection as required for
                // orthographic rules, as in the Persian language, for example.
                // It
                // also may occur in Indic scripts in a consonant conjunct
                // context
                // (immediately following a virama), to control required display
                // of
                // such conjuncts.
                // Lookup:
                // True
                // Rule Set:
                // False;
                // If Canonical_Combining_Class(Before(cp)) .eq. Virama Then
                // True;
                // If RegExpMatch((Joining_Type:{L,D})(Joining_Type:T)*\u200C
                // (Joining_Type:T)*(Joining_Type:{R,D})) Then True;
                //
                if (i == 0) {
                    errors.add(Errors.C1);
                    continue;
                }
                int lastChar = Character.codePointBefore(domain, i);
                // the way the following code is set up, we continue if we
                // *don't* get an error
                if (VIRAMAS.contains(lastChar)) {
                    continue;
                }
                int beforeT = T.spanBack(domain.subSequence(0, i), SpanCondition.CONTAINED);
                if (beforeT > 0) {
                    int previousChar = Character.codePointBefore(domain, beforeT);
                    if (L_D.contains(previousChar)) {
                        int afterT = (i + 1) + T.span(domain.subSequence(i + 1, domain.length()), SpanCondition.CONTAINED);
                        if (afterT < domain.length()) {
                            int nextChar = Character.codePointAt(domain, afterT);
                            if (R_D.contains(nextChar)) {
                                continue; // we win!
                            }
                        }
                    }
                }
                errors.add(Errors.C1);
            } else if (cp == JOINER) {
                // Appendix A.2. ZERO WIDTH JOINER
                // Code point:
                // U+200D
                // Overview:
                // This may occur in Indic scripts in a consonant conjunct
                // context
                // (immediately following a virama), to control required display
                // of
                // such conjuncts.
                // Lookup:
                // True
                // Rule Set:
                // False;
                // If Canonical_Combining_Class(Before(cp)) .eq. Virama Then
                // True;
                if (i == 0) {
                    errors.add(Errors.C2);
                    continue;
                }
                int lastChar = Character.codePointBefore(domain, i);
                if (!VIRAMAS.contains(lastChar)) {
                    errors.add(Errors.C2);
                }
            }
        }
        return errors.size() > oldErrorLength;
    }

    /**
     * Input must start with xn--
     * 
     * @param label
     * @param errors
     * @return
     */
    protected String fromPunycode(String label, Set<Errors> errors) {
        try {
            StringBuffer temp = new StringBuffer();
            temp.append(label.substring(4));
            StringBuffer depuny = Punycode.decode(temp, null);
            return depuny.toString();
        } catch (Exception e) {
            errors.add(Errors.A3);
            return null;
        }
    }

    static final Normalizer2    nfc       = Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.COMPOSE);
    public static final Pattern FULL_STOP = Pattern.compile("\\.");
    static final UnicodeSet     ASCII     = new UnicodeSet("[\\u0000-\\u007F]").freeze();

    public enum IdnaChoice {
        transitional, nontransitional
    }

    public static final int UIDNA_ERROR_INVALID_ACE_LABEL      = 1;
    public static final int UIDNA_ERROR_DISALLOWED             = 2;
    public static final int UIDNA_ERROR_PUNYCODE               = 4;
    public static final int UIDNA_ERROR_CONTEXTJ               = 8;
    public static final int UIDNA_ERROR_LABEL_TOO_LONG         = 16;
    public static final int UIDNA_ERROR_DOMAIN_NAME_TOO_LONG   = 32;
    public static final int UIDNA_ERROR_EMPTY_LABEL            = 64;
    public static final int UIDNA_ERROR_LEADING_HYPHEN         = 128;
    public static final int UIDNA_ERROR_HYPHEN_3_4             = 256;
    public static final int UIDNA_ERROR_TRAILING_HYPHEN        = 512;
    public static final int UIDNA_ERROR_LEADING_COMBINING_MARK = 1024;
    public static final int UIDNA_ERROR_BIDI                   = 2048;
    public static final int UIDNA_ERROR_LABEL_HAS_DOT          = 4096;

    public enum Errors {
        B1(UIDNA_ERROR_BIDI),
        B2(UIDNA_ERROR_BIDI),
        B3(UIDNA_ERROR_BIDI),
        B4(UIDNA_ERROR_BIDI),
        B5(UIDNA_ERROR_BIDI),
        B6(UIDNA_ERROR_BIDI),
        C1(UIDNA_ERROR_CONTEXTJ),
        C2(UIDNA_ERROR_CONTEXTJ),
        P1(UIDNA_ERROR_DISALLOWED),
        V1(UIDNA_ERROR_INVALID_ACE_LABEL),
        V2(UIDNA_ERROR_HYPHEN_3_4),
        V3(UIDNA_ERROR_LEADING_HYPHEN | UIDNA_ERROR_TRAILING_HYPHEN),
        V4(UIDNA_ERROR_LABEL_HAS_DOT),
        V5(UIDNA_ERROR_LEADING_COMBINING_MARK),
        V6(UIDNA_ERROR_INVALID_ACE_LABEL),
        A3(UIDNA_ERROR_INVALID_ACE_LABEL | UIDNA_ERROR_PUNYCODE),
        A4_1(UIDNA_ERROR_DOMAIN_NAME_TOO_LONG),
        A4_2(UIDNA_ERROR_EMPTY_LABEL | UIDNA_ERROR_LABEL_TOO_LONG), ;
        int errorNum;

        Errors(int errorNum) {
            this.errorNum = errorNum;
        }
    }

    public String process(String domainName, IdnaChoice idnaChoice, Set<Errors> errors) {

        domainName = processMap(domainName, idnaChoice, errors);

        // Normalize. Normalize the domain_name string to Unicode Normalization
        // Form C.
        domainName = nfc.normalize(domainName);
        // Break. Break the string into labels at U+002E ( . ) FULL STOP.
        String[] labels = nonbrokenSplit(FULL_STOP, domainName);
        // Convert/Validate. For each label in the domain_name string:
        domainName = processConvertValidateLabels(idnaChoice, errors, labels);
        // Check BIDI
        hasBidiError(domainName, errors);
        // Check ContextJ
        hasContextJError(domainName, errors);
        return domainName;
    }

    private String processMap(String domainName, IdnaChoice idnaChoice, Set<Errors> errors) {
        StringBuilder buffer = new StringBuilder();
        int cp;
        // Map. For each code point in the domain_name string, look up the
        // status value in Section 5, IDNA Mapping Table, and take the following
        // actions:
        for (int i = 0; i < domainName.length(); i += Character.charCount(cp)) {
            cp = Character.codePointAt(domainName, i);
            IdnaType type = types.get(cp);
            // disallowed: Leave the code point unchanged in the string, and
            // record that there was an error.
            switch (type) {
            case disallowed:
                errors.add(Errors.P1);
                buffer.appendCodePoint(cp);
                break;
            // ignored: Remove the code point from the string. This is
            // equivalent to mapping the code point to an empty string.
            case ignored:
                break;
            // mapped: Replace the code point in the string by the value for the
            // mapping in Section 5, IDNA Mapping Table.
            case mapped:
                String mapped = mappings.get(cp);
                buffer.append(mapped);
                break;
            // deviation:
            // For Transitional Processing, replace the code point in the string
            // by the value for the mapping in Section 5, IDNA Mapping Table.
            // For Nontransitional Processing, leave the code point unchanged in
            // the string.
            case deviation:
                if (idnaChoice == IdnaChoice.transitional) {
                    mapped = mappings.get(cp);
                    buffer.append(mapped);
                } else {
                    buffer.appendCodePoint(cp);
                }
                break;
            // valid: Leave the code point unchanged in the string.
            case valid:
                buffer.appendCodePoint(cp);
                break;
            }
        }
        domainName = buffer.toString();
        return domainName;
    }

    private String processConvertValidateLabels(IdnaChoice idnaChoice, Set<Errors> errors, String[] labels) {
        String domainName;
        StringBuilder buffer = new StringBuilder();
        for (String label : labels) {
            // If the label starts with "xn--":
            if (label.startsWith("xn--")) {
                String newLabel = fromPunycode(label, errors);
                // Attempt to convert the rest of the label to Unicode according
                // to Punycode [RFC3492].
                // If that conversion fails, record that there was an error, and
                // continue with the next label.
                if (newLabel != null) { // we recorded an error in the routine
                    // Otherwise replace the original label in the string by the
                    // results of the conversion.
                    // Verify that the label meets the validity criteria in
                    // Section 4.1,
                    // Validity Criteria for Nontransitional Processing.
                    // If any of the validity criteria are not satisfied, record
                    // that there was an error.
                    label = newLabel;
                    checkLabelValidity(label, IdnaChoice.nontransitional, errors);
                }
            } else {
                // If the label does not start with "xn--":
                // Verify that the label meets the validity criteria in Section
                // 4.1, Validity Criteria for the input Processing choice
                // (Transitional or Nontransitional). If any of the validity
                // criteria are not satisfied, record that there was an error.
                checkLabelValidity(label, idnaChoice, errors);
            }
            if (buffer.length() != 0) {
                buffer.append('.');
            }
            buffer.append(label);
        }
        domainName = buffer.toString();
        return domainName;
    }

    static final Pattern    HYPHEN34         = Pattern.compile("..--.*");
    static final Pattern    HYPHEN_START_END = Pattern.compile("(-.*)|(.*-)");
    static final UnicodeSet MARKS            = new UnicodeSet("[:M:]").freeze();

    private void checkLabelValidity(String label, IdnaChoice idnaChoice, Set<Errors> errors) {
        // Each of the following criteria must be satisfied for a label:
        //
        // The label must be in Unicode Normalization Form NFC.
        if (!nfc.isNormalized(label)) {
            errors.add(Errors.V1);
        }
        // The label must not contain a U+002D HYPHEN-MINUS character in both
        // the third position and fourth positions.
        if (HYPHEN34.matcher(label).matches()) {
            errors.add(Errors.V2);
        }
        // The label must neither begin nor end with a U+002D HYPHEN-MINUS
        // character.
        if (HYPHEN_START_END.matcher(label).matches()) {
            errors.add(Errors.V3);
        }
        // The label must not contain a U+002E ( . ) FULL STOP.
        if (FULL_STOP.matcher(label).find()) {
            errors.add(Errors.V4);
        }
        // The label must not begin with a combining mark, that is:
        // General_Category=Mark.
        if (label.length() > 0) {
            int firstChar = label.codePointAt(0);
            if (MARKS.contains(firstChar)) {
                errors.add(Errors.V5);
            }
        }
        // Each code point in the label must only have certain status values
        // according to Section 5, IDNA Mapping Table:
        int cp;
        for (int i = 0; i < label.length(); i += Character.charCount(cp)) {
            cp = Character.codePointAt(label, i);
            IdnaType type = types.get(cp);
            // For Transitional Processing, each value must be valid.
            // For Nontransitional Processing, each value must be either valid
            // or deviation.
            switch (type) {
            case valid:
                break;
            case deviation:
                if (idnaChoice == IdnaChoice.transitional) {
                    errors.add(Errors.V6);
                }
                break;
            default:
                errors.add(Errors.V6);
            }
        }
    }

    public String toASCII(String domainName, IdnaChoice idnaChoice, Set<Errors> errors) {
        // Apply the appropriate processing. This may record an error. The
        // appropriate processing is either:
        // Transitional Processing for transitional handling of Deviation
        // characters, or
        // Nontransitional Processing otherwise
        domainName = process(domainName, idnaChoice, errors);
        // Break the result into labels at U+002E FULL STOP.
        StringBuilder buffer = new StringBuilder();
        String[] labels = nonbrokenSplit(FULL_STOP, domainName);
        for (int i = 0; i < labels.length; ++i) {
            String label = labels[i];
            // Convert each label with non-ASCII characters into Punycode
            // [RFC3492]. This may record an error.
            if (!ASCII.containsAll(label)) {
                try {
                    StringBuffer temp = new StringBuffer();
                    temp.append(label);
                    StringBuffer punycoded = Punycode.encode(temp, null);
                    punycoded.insert(0, "xn--");
                    label = punycoded.toString();
                } catch (Exception e) {
                    errors.add(Errors.A3);
                }
            }
            // The length of each label is from 1 to 63.
            int labelLength = label.codePointCount(0, label.length());
            if (labelLength > 63 || labelLength < 1 && i != labels.length - 1) { // last
                                                                                 // one
                                                                                 // can
                                                                                 // be
                                                                                 // zero
                                                                                 // length
                errors.add(Errors.A4_2);
            }
            if (buffer.length() != 0) {
                buffer.append('.');
            }
            buffer.append(label);
        }
        domainName = buffer.toString();
        // Verify DNS length restrictions. This may record an error. For more
        // information, see [STD13] and [STD3].
        // The length of the domain name, excluding the root label and its dot,
        // is from 1 to 253.
        int labelDomainNameLength = domainName.codePointCount(0, domainName.length());
        if (labelDomainNameLength < 0 || labelDomainNameLength > 254 || labelDomainNameLength == 254 && !domainName.endsWith(".")) {
            errors.add(Errors.A4_1);
        }
        // If an error was recorded, then the operation failed, and no DNS
        // lookup should be done.
        return domainName;
    }

    public String toUnicode(String domainName, IdnaChoice idnaChoice, Set<Errors> errors) {
        return process(domainName, idnaChoice, errors);
    }
}