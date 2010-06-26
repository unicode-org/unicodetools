package org.unicode.jsptest;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;

import org.unicode.jsp.Builder;
import org.unicode.jsp.CharEncoder;
import org.unicode.jsp.UnicodeProperty;
import org.unicode.jsp.UnicodeSetUtilities;
import org.unicode.jsp.XPropertyFactory;
import org.unicode.jsp.UnicodeSetUtilities.NFKC_CF;

import com.ibm.icu.dev.test.AbstractTestLog;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;

public class TestUnicodeSet  extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new TestUnicodeSet().run(args);
    }

    public void TestEncodingProp() {

        XPropertyFactory factory = XPropertyFactory.make();
        UnicodeProperty prop = factory.getProperty("enc_Latin1");
        UnicodeProperty prop2 = factory.getProperty("enc_Latin2");
        UnicodeMap<String> map = prop.getUnicodeMap();
        UnicodeMap<String> map2 = prop2.getUnicodeMap();
        for (String value : Builder.with(new TreeSet<String>()).addAll(map.values()).addAll(map2.values()).get()) {
            logln(value + "\t" + map.getSet(value) + "\t" + map2.getSet(value));
        }
        UnicodeSet set = UnicodeSetUtilities.parseUnicodeSet("[:enc_Latin1=/61/:]");
        assertNotEquals("Latin1", 0, set.size());
    }

    public void TestPerMill() {
        SortedMap<String, Charset> charsets = Charset.availableCharsets();
        byte[] dest = new byte[50];
        UnicodeSet values = new UnicodeSet();
        Set<String> charsetSet = charsets.keySet();
        int count = (int)(5 + charsetSet.size()*getInclusion()/10.0);
        for (String s : charsetSet) {
            if (--count < 0) break;
            Charset charset = charsets.get(s);
            CharEncoder encoder;
            try {
                encoder = new CharEncoder(charset, false, false);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            // first check that we are an ASCII-based encoding, and skip if not
            int len = encoder.getValue(0x61, dest, 0);
            if (len != 1 || dest[0] != 0x61) {
                continue;
            }

            values.clear();
            byte checkByte = (byte) 0x89;
            for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                len = encoder.getValue(cp, dest, 0);
                if (len > 0) {
                    for (int j = 0; j < len; ++j) {
                        if (dest[j] == checkByte) {
                            values.add(cp);
                            break;
                        }
                    }
                }
            }
            values.remove(0x2030);
            if (values.size() != 0) {
                logln(s + "\tvalues:\t" + values + "\taliases:\t" + charset.aliases());
            }
        }
    }

    public void TestScriptSpecials() {
        //        UnicodeSet set = UnicodeSetUtilities.parseUnicodeSet("[:scs=Hant:]");
        //        assertNotEquals("Hant", 0, set.size());
        UnicodeSet set2 = UnicodeSetUtilities.parseUnicodeSet("[:scs=Arab,Syrc:]");
        assertNotEquals("Arab Syrc", 0, set2.size());

    }

    public void TestGC() {
        Map<String,R2<String,UnicodeSet>> SPECIAL_GC = new LinkedHashMap<String,R2<String,UnicodeSet>>();

        String[][] extras = {
                {"Other", "C", "[[:Cc:][:Cf:][:Cn:][:Co:][:Cs:]]"},
                {"Letter", "L", "[[:Ll:][:Lm:][:Lo:][:Lt:][:Lu:]]"},
                {"Cased_Letter", "LC", "[[:Ll:][:Lt:][:Lu:]]"},
                {"Mark", "M", "[[:Mc:][:Me:][:Mn:]]"},
                {"Number", "N", "[[:Nd:][:Nl:][:No:]]"},
                {"Punctuation", "P", "[[:Pc:][:Pd:][:Pe:][:Pf:][:Pi:][:Po:][:Ps:]]"},
                {"Symbol", "S", "[[:Sc:][:Sk:][:Sm:][:So:]]"},
                {"Separator", "Z", "[[:Zl:][:Zp:][:Zs:]]"},
        };

        String[] gcs = {"General_Category=", "", "gc="};
        /*
gc ; C         ; Other                            # Cc | Cf | Cn | Co | Cs
gc ; Cc        ; Control                          ; cntrl
gc ; L         ; Letter                           # Ll | Lm | Lo | Lt | Lu
gc ; LC        ; Cased_Letter                     # Ll | Lt | Lu
gc ; M         ; Mark                             # Mc | Me | Mn
gc ; N         ; Number                           # Nd | Nl | No
gc ; Nd        ; Decimal_Number                   ; digit
gc ; P         ; Punctuation                      ; punct                            # Pc | Pd | Pe | Pf | Pi | Po | Ps
gc ; S         ; Symbol                           # Sc | Sk | Sm | So
gc ; Z         ; Separator                        # Zl | Zp | Zs
         */
        for (String[] extra : extras) {
            UnicodeSet expected = new UnicodeSet(extra[2]).freeze();
            for (String test : extra) {
                if (test.startsWith("[")) continue;
                for (String gc : gcs) {
                    UnicodeSet set = UnicodeSetUtilities.parseUnicodeSet("[:" + gc + test + ":]");
                    assertEquals("Multiprop:\t" + gc + test, expected, set);
                }
            }
        }
        assertEquals("Coverage:\t", new UnicodeSet("[:any:]"), UnicodeSetUtilities.parseUnicodeSet("[[:C:][:L:][:M:][:N:][:P:][:S:][:Z:]]"));
    }

    public void TestNF() {
        for (String nf : new String[]{"d", "c", "kd", "kc"}) {
            checkSetsEqual("[:isnf" + nf + ":]", "[:nf" + nf + "qc!=N:]");
            checkSetsEqual("[:isnf" + nf + ":]", "[:tonf" + nf + "=@cp@:]");
        }
    }
    
    
    public void TestSets() {

        checkProperties("\\p{tonfkd=/[:alphabetic:]/}", "[:alphabetic:]", "[b]");
        checkProperties("[:toLowercase=a:]", "[Aa]", "[b]");
        checkProperties("[:subhead=/Mayanist/:]", "[\uA726]");

        //checkProperties("[[:script=*latin:]-[:script=latin:]]");
        //checkProperties("[[:script=**latin:]-[:script=latin:]]");
        checkProperties("abc-m", "[d]");

        checkProperties("[:usage=common:]", "[9]");

        checkProperties("[:toNFKC=a:]", "[\u00AA]");
        checkProperties("[:isNFC=false:]", "[\u212B]", "[a]");
        checkProperties("[:toNFD=A\u0300:]", "[\u00C0]");
        checkProperties("[:toLowercase= /a/ :]", "[aA]");
        checkProperties("[:ASCII:]", "[z]");
        checkProperties("[:lowercase:]", "[a]");
        checkProperties("[:toNFC=/\\./:]", "[.]");
        checkProperties("[:toNFKC=/\\./:]", "[\u2024]");
        checkProperties("[:toNFD=/\\./:]", "[.]");
        checkProperties("[:toNFKD=/\\./:]", "[\u2024]");
        checkProperties("[:toLowercase=/a/:]", "[aA]");
        checkProperties("[:toUppercase=/A/:]", "[Aa\u1E9A]");
        checkProperties("[:toCaseFold=/a/:]", "[Aa\u1E9A]");
        checkProperties("[:toTitlecase=/A/:]", "[Aa\u1E9A]");
        checkProperties("[:idna=valid:]", "[\u0308]");
        checkProperties("[:idna=ignored:]", "[\u00AD]");
        checkProperties("[:idna=mapped:]", "[\u00AA]");
        checkProperties("[:idna=disallowed:]", "[\\u0001]");
        checkProperties("[:iscased:]", "[a-zA-Z]");
        checkProperties("[:name=/WITH/:]", "[\u00C0]");
    }

    void checkProperties(String testString, String containsSet) {
        checkProperties(testString, containsSet, null);
    }

    void checkProperties(String testString, String containsSet, String doesntContainSet) {
        UnicodeSet tc1 = UnicodeSetUtilities.parseUnicodeSet(testString);
        if (containsSet != null) {
            UnicodeSet contains = new UnicodeSet(containsSet);
            if (!tc1.containsAll(contains)) {
                UnicodeSet missing = new UnicodeSet(contains).removeAll(tc1);
                errln(tc1 + "\t=\t" + tc1.complement().complement() + "\t\nDoesn't contain " + missing);  
            }
        }
        if (doesntContainSet != null) {
            UnicodeSet doesntContain = new UnicodeSet(doesntContainSet);
            if (!tc1.containsNone(doesntContain)) {
                UnicodeSet extra = new UnicodeSet(doesntContain).retainAll(tc1);
                errln(tc1 + "\t=\t" + tc1.complement().complement() + "\t\nContains some of" + extra);  
            }
        }
    }

    private void checkSetsEqual(String... unicodeSetPatterns) {
        UnicodeSet base = null;
        for (String pattern : unicodeSetPatterns) {
            UnicodeSet current = UnicodeSetUtilities.parseUnicodeSet(pattern);
            if (base == null) {
                base = current;
            } else {
                assertEquals(unicodeSetPatterns[0] + " == " + pattern, base, current);
            }
        }
    }

    public void TestSetSyntax() {
        //System.out.println("Script for A6E6: " + script + ", " + UScript.getName(script) + ", " + script2);
        checkProperties("[:subhead=/Syllables/:]", "[\u1200]");
        //showIcuEnums();
        checkProperties("\\p{ccc:0}", "\\p{ccc=0}", "[\u0308]");
        checkProperties("\\p{isNFC}", "[:ASCII:]", "[\u212B]");
        checkProperties("[:isNFC=no:]", "[\u212B]", "[:ASCII:]");
        checkProperties("[:dt!=none:]&[:toNFD=/^\\p{ccc:0}/:]", "[\u00A0]", "[\u0340]");
        checkProperties("[:toLowercase!=@cp@:]", "[A-Z\u00C0]", "[abc]");
        checkProperties("[:toNfkc!=@toNfc@:]", "[\\u00A0]", "[abc]");

        String trans1 = new NFKC_CF().transform("\u2065");
        XPropertyFactory factory = XPropertyFactory.make();
        UnicodeProperty prop = factory.getProperty("tonfkccf");
        String trans2 = prop.getValue('\u2065');
        if (!trans1.equals(trans2)) {
            errln("mapping of \u2065 " + UCharacter.getName('\u2065') + "," + trans1 + "," + trans2);
        }
        checkProperties("[:tonfkccf=/^$/:]", "[:di:]", "[abc]");
        checkProperties("[:ccc=/3/:]", "[\u0308]");
        checkProperties("[:age=3.2:]", "[\u0308]");
        checkProperties("[:alphabetic:]", "[a]");
        checkProperties("[:greek:]", "[\u0370]");
        checkProperties("[:mn:]", "[\u0308]");
        checkProperties("[:sc!=Latn:]", "[\u0308]");
        checkProperties("[:^sc:Latn:]", "[\u0308]");
        checkProperties("[:sc≠Latn:]", "[\u0308]");
        checkSetsEqual("[:sc≠Latn:]", "[:^sc:Latn:]", "[:^sc=Latn:]", "[:sc!=Latn:]");
        checkSetsEqual("[:sc=Latn:]", "[:sc:Latn:]", "[:^sc≠Latn:]", "[:^sc!=Latn:]", "[:^sc!:Latn:]");

        try {
            checkProperties("[:linebreak:]", "[\u0308]");
            throw new IllegalArgumentException("Exception expected.");
        } catch (Exception e) {
            if (!e.getMessage().contains("must be in")) {
                throw new IllegalArgumentException("Exception expected with 'illegal'", e);
            } else {
                logln(e.getMessage());
            }
        }

        try {
            checkProperties("[:alphabetic=foobar:]", "[\u0308]");
            throw new IllegalArgumentException("Exception expected.");
        } catch (Exception e) {
            if (!e.getMessage().contains("must be in")) {
                throw new IllegalArgumentException("Exception expected with 'illegal'", e);
            }
        }

        checkProperties("[:alphabetic=no:]", "[\u0308]");
        checkProperties("[:alphabetic=false:]", "[\u0308]");
        checkProperties("[:alphabetic=f:]", "[\u0308]");
        checkProperties("[:alphabetic=n:]", "[\u0308]");


        checkProperties("\\p{idna2003=disallowed}", "[\\u0001]");
        checkProperties("\\p{idna=valid}", "[\u0308]");
        checkProperties("\\p{uts46=valid}", "[\u0308]");
        checkProperties("\\p{idna2008=disallowed}", "[A]");
    }



    public static void assertEquals(AbstractTestLog testFmwk, String test, UnicodeSet expected, UnicodeSet actual) {
        if (!expected.equals(actual)) {
            UnicodeSet inExpected = new UnicodeSet(expected).removeAll(actual);
            UnicodeSet inActual = new UnicodeSet(actual).removeAll(expected);
            testFmwk.errln(test + " - MISSING: " + inExpected + ", EXTRA: " + inActual);
        } else {
            testFmwk.logln("OK\t\t" + test);
        }
    }

    public static void assertContains(AbstractTestLog testFmwk, String test, UnicodeSet expectedSubset, UnicodeSet actual) {
        if (!actual.containsAll(expectedSubset)) {
            UnicodeSet inExpected = new UnicodeSet(expectedSubset).removeAll(actual);
            testFmwk.errln(test + " - MISSING: " + inExpected);
        } else {
            testFmwk.logln("OK\t\t" + test);
        }
    }
}
