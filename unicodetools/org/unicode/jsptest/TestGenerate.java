package org.unicode.jsptest;

import java.io.IOException;
import java.util.Comparator;
import java.util.TreeSet;

import org.unicode.idna.Idna;
import org.unicode.idna.Idna.IdnaType;
import org.unicode.idna.Idna2003;
import org.unicode.idna.Uts46;
import org.unicode.jsp.UnicodeUtilities;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.ToolUnicodePropertySource;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.UnicodeLabel;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodePropertySymbolTable;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.XSymbolTable;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class TestGenerate{

    static {
        final UnicodeSet before = new UnicodeSet("[:gc=Cn:]").complement().complement();
        final ToolUnicodePropertySource toolUPS1 = ToolUnicodePropertySource.make(Default.ucdVersion());
        final XSymbolTable toolUPS = new UnicodePropertySymbolTable(toolUPS1);
        UnicodeSet.setDefaultXSymbolTable(toolUPS);
        final UnicodeSet after = new UnicodeSet("[:gc=Cn:]").complement().complement();
        System.out.println("Before: " + before.toString().substring(0,100));
        System.out.println("After:  " + after.toString().substring(0,100));
    }

    static final String AGE = System.getProperty("age");
    //static final UnicodeSet OVERALL_ALLOWED = new UnicodeSet().applyPropertyAlias("age", AGE == null ? "6.1" : AGE).freeze();


    public static void main(String[] args) throws Exception {
        //int count = new TestUts46().generateTests(1000);
    }

    public static UnicodeSet SKIP = new UnicodeSet("[[\\u0000-\\u007F][:Cc:][:Co:][:Cs:]]");
    static {
        for (int i = 0; i <= 0x10FFFF; ++i) {
            if (!Default.ucd().isAssigned(i)) {
                SKIP.add(i);
            }
        }
        SKIP.freeze();
    }

    public void TestIdnaDifferences() {
        final UnicodeSet remapped = new UnicodeSet();
        final UnicodeMap<String> map = UnicodeUtilities.getIdnaDifferences(remapped, SKIP);
        final TreeSet<String> ordered = new TreeSet<String>(new InverseComparator());
        ordered.addAll(map.values());
        final int max = 200;
        for (final String value : ordered) {
            final UnicodeSet set = map.getSet(value);
            final String prettySet = TestJsp.prettyTruncate(max, set);
            System.out.println(value + "\t" + set.size() + "\t" + set); // prettySet
        }
        final Transliterator name = Transliterator.getInstance("name");
        System.out.println("Code\tUts46\tidna2003\tCode\tUts46\tidna2003");

        for (final String s : remapped) {
            final String uts46 = Uts46.SINGLETON.transform(s);
            final String idna2003 = Idna2003.toIdna2003(s);
            if (!uts46.equals(idna2003)) {
                System.out.println(Utility.hex(s) + "\t" + Utility.hex(uts46) + "\t" + Utility.hex(idna2003)
                        + "\t" + name.transform(s) + "\t" + name.transform(uts46) + "\t" + name.transform(idna2003)
                        );
            }
        }
    }

    public void TestGenerateDataFile() throws IOException {
        //final UnicodeMap<String> results = new UnicodeMap();
        final UnicodeMap<String> hex_results = new UnicodeMap<String>();
        final UnicodeMap<String> hex_results_requiring_nfkc = new UnicodeMap<String>();
        //hex_results.putAll(0,0x10FFFF,"valid");
        //hex_results.putAll(new UnicodeSet("[:cn:]"), "disallowed");
        //hex_results.putAll(new UnicodeSet("[:noncharactercodepoint:]"), "disallowed");
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            final String s = UTF16.valueOf(cp);
            final String nfc = toNfc(s);
            final String nfkc = Normalizer.normalize(s, Normalizer.NFKC);
            final String uts46 = Uts46.SINGLETON.transform(s);
            final IdnaType statusInt = Uts46.SINGLETON.getType(cp);
            String status = statusInt.toString();
            if (Uts46.SINGLETON.getType(cp) == IdnaType.deviation) { //  Uts46.SINGLETON.DEVIATIONS.contains(cp)
                status = "deviation";
            }
            if (statusInt == Idna.IdnaType.mapped) {
                status += Utility.repeat(" ", 10-status.length()) + " ; " + Utility.hex(uts46);
            }
            hex_results.put(cp, status);
            //      hex_results.put(cp, status==UnicodeUtilities.IGNORED ? "ignored"
            //              : UnicodeUtilities. ? "disallowed"
            //                      : s.equals(uts46) ? "valid"
            //                              //: nfc.equals(uts46) ? "needs_nfc"
            //                                      : Utility.hex(uts46, " "));
            //
            //      hex_results_requiring_nfkc.put(cp, Uts46.SINGLETON.length() == 0 ? "ignored"
            //              : !Uts46.SINGLETON.Uts46Chars.containsAll(uts46) ? "disallowed"
            //                      : s.equals(uts46) ? "valid"
            //                              : nfkc.equals(uts46) ? "needs_nfkc"
            //                                      : Utility.hex(uts46, " "));
        }
        final BagFormatter bf = new BagFormatter();
        bf.setLabelSource(null);
        bf.setRangeBreakSource(null);
        bf.setShowCount(false);
        bf.setNameSource(new UnicodeLabel() {

            @Override
            public String getValue(int codepoint, boolean isShort) {
                //String target = results.get(codepoint);
                return UCharacter.getExtendedName(codepoint);
            }

        });

        //    String sourceName = UCharacter.getName(cp);
        //    String targetName = UCharacter.getName(uts46, " + ");
        //    String names = (sourceName != null && targetName != null) ? "#\t" + sourceName + " \u2192 " + targetName : "";
        //    System.out.println(Utility.hex(s) + ";\t" + Utility.hex(uts46)
        //            + ";\t" + names
        //                );
        //writeIdnaDataFile(hex_results, bf, "NFC", "IdnaMappingTable");
        //writeIdnaDataFile(hex_results_requiring_nfkc, bf, "NFKC", "uts46-data-pre-nfkc-5.1.txt");
    }



    private String toNfc(String s) {
        return Normalizer.normalize(s, Normalizer.NFC);
    }

    static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'", ULocale.US);
    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }


    //  private void writeIdnaDataFile(final UnicodeMap<String> hex_results, BagFormatter bf, String normalizationForm2, String filenameStem) throws IOException {
    //    String filename = filenameStem + "-" + AGE + ".0.txt";
    //    PrintWriter writer = FileUtilities.openUTF8Writer(Utility.WORKSPACE_DIRECTORY + "draft/reports/tr46/", filename);
    //    String normalizationForm = normalizationForm2;
    //    writer.println("# " + filename + "- DRAFT\n" +
    //            "# Date: " + dateFormat.format(new Date()) + " [MD]\n" +
    //            "#\n" +
    //            "# Unicode IDNA Compatible Preprocessing (UTS #46)\n" +
    //            "# Copyright (c) 1991-2009 Unicode, Inc.\n" +
    //            "# For terms of use, see http://www.unicode.org/terms_of_use.html\n" +
    //    "# For documentation, see http://www.unicode.org/reports/tr46/\n");
    //
    //    //    # IdnaMappingTable-5.1.0.txt - DRAFT
    //    //    # Date: 2009-11-14 08:10:42 GMT [MD]
    //    //    #
    //    //    # Unicode IDNA Compatible Preprocessing (UTS #46)
    //    //    # Copyright (c) 1991-2009 Unicode, Inc.
    //    //    # For terms of use, see http://www.unicode.org/terms_of_use.html
    //    //    # For documentation, see http://www.unicode.org/reports/tr46/
    //
    //    bf.setValueSource(new UnicodeProperty.UnicodeMapProperty().set(hex_results));
    //    final UnicodeLabel oldLabel = bf.getNameSource();
    //    bf.setNameSource(new UnicodeLabel() {
    //      public String getValue(int codepoint, boolean isShort) {
    //        if (OVERALL_ALLOWED.contains(codepoint)) {
    //          return oldLabel.getValue(codepoint, isShort);
    //        }
    //        return "<reserved-" + Utility.hex(codepoint) + ">";
    //      }
    //    });
    //    writer.println(bf.showSetNames(hex_results.keySet()));
    //    writer.close();
    //  }

    public static class InverseComparator implements Comparator {
        private final Comparator other;

        public InverseComparator() {
            other = null;
        }

        public InverseComparator(Comparator other) {
            this.other = other;
        }

        @Override
        public int compare(Object a, Object b) {
            return other == null
                    ? ((Comparable)b).compareTo(a)
                            : other.compare(b, a);
        }
    }

}
