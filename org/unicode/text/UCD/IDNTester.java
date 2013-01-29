package org.unicode.text.UCD;

import java.io.IOException;
import java.io.PrintWriter;

import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.PrettyPrinter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.IDNA;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class IDNTester {
    static StringBuffer inbuffer = new StringBuffer();
    static StringBuffer intermediate, outbuffer;
    static final int OK = 0, DELETED = 1, ILLEGAL = 2, REMAPPED = 3, IDNA_TYPE_LIMIT = 4;
    static UnicodeSet IDNInputOnly = new UnicodeSet();
    static UnicodeSet IDNOutput = new UnicodeSet();
    static boolean initialized = false;
    static UnicodeSet IDInputOnly32 = new UnicodeSet();
    static UnicodeSet IDOutput32 = new UnicodeSet();
    static UnicodeSet IDInputOnly50 = new UnicodeSet();
    static UnicodeSet IDOutput50 = new UnicodeSet();
    static PrettyPrinter pp = new PrettyPrinter().setOrdering(Collator.getInstance(ULocale.ROOT)).setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(Collator.PRIMARY));
    static PrintWriter pw;

    public static void main(String[] args) throws IOException {
        initialize();
        pw = BagFormatter.openUTF8Writer(UCD_Types.GEN_DIR, "idnCount.html");
        pw.println("<html><body>");
        showSet("IDN InputOnly: ", IDNInputOnly);
        showSet("IDN Output: ", IDNOutput);
        showSet("ID InputOnly, U3.2: ", IDInputOnly32);
        showSet("ID Output, U3.2: ", IDOutput32);

        showSet("IDN Output - ID Output, U3.2: ", new UnicodeSet(IDNOutput).removeAll(IDOutput32));
        showSet("IDN Output & ID Output, U3.2: ", new UnicodeSet(IDNOutput).retainAll(IDOutput32));
        showSet("ID Output - IDN Output, U3.2: ", new UnicodeSet(IDOutput32).removeAll(IDNOutput));

        showSet("ID InputOnly, U5.0: ", IDInputOnly50);
        showSet("ID Output, U5.0: ", IDOutput50);
        showSet("ID Output, U5.0 - U3.2: ", new UnicodeSet(IDOutput50).removeAll(IDOutput32));

        pw.println("</body></html>");

        pw.close();
    }

    public static void showSet(String title, UnicodeSet set) {
        pw.println("<h2>" + title + set.size() + "</h2>" + "<p>" + pp.format(set) + "</p>");
        pw.println();
    }

    static UnicodeSet getIDNInput() {
        if (!initialized) {
            initialize();
        }
        return IDNInputOnly;
    }

    static UnicodeSet getIDNOutput() {
        if (!initialized) {
            initialize();
        }
        return IDNInputOnly;
    }

    private static void initialize() {
        final UnicodeSet oddballs = new UnicodeSet("[\u034F \u180B-\u180D \uFE00-\uFE0F _]");
        final UCD U32 = UCD.make("3.2.0");
        final Normalizer nfkc32 = new Normalizer(UCD_Types.NFKC, "3.2.0");
        final UCDProperty xid32 = DerivedProperty.make(UCD_Types.Mod_ID_Continue_NO_Cf,U32);
        final UnicodeSet IDInput32 = xid32.getSet();
        IDInput32.add('-').removeAll(oddballs);

        final UCD U50 = UCD.make("5.0.0");
        final Normalizer nfkc50 = new Normalizer(UCD_Types.NFKC, "5.0.0");
        final UCDProperty xid50 = DerivedProperty.make(UCD_Types.Mod_ID_Continue_NO_Cf,U50);
        final UnicodeSet IDInput50 = xid50.getSet();
        IDInput50.add('-').removeAll(oddballs);

        for (int i = 0; i < 0x10FFFF; ++i) {
            if ((i & 0xFFF) == 0) {
                System.out.println(i);
                System.out.flush();
            }
            final int type = getIDNAType(i);
            if (type == OK) {
                IDNOutput.add(i);
            } else if (type != ILLEGAL) {
                IDNInputOnly.add(i);
            }
            if (IDInput32.contains(i)) {
                splitSet(IDInputOnly32, IDOutput32, U32, nfkc32, i);
            }
            if (IDInput50.contains(i)) {
                splitSet(IDInputOnly50, IDOutput50, U50, nfkc50, i);
            }
        }
        initialized = true;
    }

    private static void splitSet(UnicodeSet inputOnlySet, UnicodeSet outputSet, UCD ucd, Normalizer nfkc, int i) {
        if (i < 0x7F) {
            outputSet.add(i);
            return;
        }
        final String v = UTF16.valueOf(i);
        String s = ucd.getCase(i, UCD_Types.FULL, UCD_Types.FOLD);
        if (s.equals(v)) {
            s = nfkc.normalize(s);
            if (s.equals(v)) {
                s = ucd.getCase(s, UCD_Types.FULL, UCD_Types.FOLD);
                if (s.equals(v)) {
                    outputSet.add(i);
                    return;
                }
            }
        }
        inputOnlySet.add(i);
    }

    static public int getIDNAType(int cp) {
        if (cp == '-') {
            return OK;
        }
        inbuffer.setLength(0);
        UTF16.append(inbuffer, cp);
        try {
            intermediate = IDNA.convertToASCII(inbuffer,
                    IDNA.DEFAULT); // USE_STD3_RULES
            if (intermediate.length() == 0) {
                return DELETED;
            }
            outbuffer = IDNA.convertToUnicode(intermediate,
                    IDNA.USE_STD3_RULES);
        } catch (final StringPrepParseException e) {
            return ILLEGAL;
        } catch (final Exception e) {
            System.out.println("Failure at: " + Utility.hex(cp));
            return ILLEGAL;
        }
        if (!TestData.equals(inbuffer, outbuffer)) {
            return REMAPPED;
        }
        return OK;
    }

}