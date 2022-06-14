package org.unicode.propstest;

import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UTF16;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.Timer;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropNormalizationData;
import org.unicode.props.PropNormalizationData.Type;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.utility.Utility;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestPropNormalization extends TestFmwkMinusMinus {

    static PropNormalizationData pnd =
            new PropNormalizationData(IndexUnicodeProperties.make("7.0"));

    @Test
    public void TestTime() {
        Normalizer2 n = Normalizer2.getNFKCInstance();
        Timer t = new Timer();
        t.start();
        for (int i = 0; i < 0x110000; ++i) {
            n.normalize(UTF16.valueOf(i));
        }
        t.stop();
        logln(t.toString());
        t.start();
        for (int i = 0; i < 0x110000; ++i) {
            pnd.normalize(i, Type.nfkc);
        }
        t.stop();
        logln(t.toString());
        Normalizer oldNfkc = Default.nfkc();
        t.start();
        for (int i = 0; i < 0x110000; ++i) {
            oldNfkc.normalize(i);
        }
        t.stop();
        logln(t.toString());
    }

    public void xTestMain() {
        Normalizer oldNfd = Default.nfd();
        Normalizer oldNfkd = Default.nfkd();
        Normalizer oldNfc = Default.nfc();
        Normalizer oldNfkc = Default.nfkc();
        checkDecomp(pnd, Type.nfc, oldNfc, 0x00c0);

        for (int i = 0; i < 0x110000; ++i) {
            short newCcc = pnd.getCcc(i);
            short oldCcc = Default.ucd().getCombiningClass(i);
            if (newCcc != oldCcc) {
                errln("Failed ccc\t " + Utility.hex(i) + "\t" + oldCcc + "\t" + newCcc);
            }
            checkDecomp(pnd, Type.nfd, oldNfd, i);
            checkDecomp(pnd, Type.nfkd, oldNfkd, i);
            checkDecomp(pnd, Type.nfc, oldNfc, i);
            checkDecomp(pnd, Type.nfkc, oldNfkc, i);
        }
        //        for (String s : pnd.nfkd.keySet()){
        //            String nfdStr = pnd.nfd.get(s);
        //            String nfkdStr = pnd.nfkd.get(s);
        //            System.out.println(Utility.hex(s)
        //                    + "\t" + (nfdStr == null ? null : Utility.hex(nfdStr))
        //                    + "\t" + Utility.hex(nfkdStr)
        //                    );
        //        }
    }

    public void checkDecomp(PropNormalizationData pnd, Type type, Normalizer oldNfd, int i) {
        String newNfd0 = pnd.normalize(UTF16.valueOf(i), type);
        boolean oldNorm = oldNfd.isNormalized(i);
        if (oldNorm == (newNfd0 == null)) {
            return;
        }
        if (newNfd0 == null) {
            newNfd0 = UTF16.valueOf(i);
        }
        String oldNfd0 = oldNfd.normalize(i);
        if (!Objects.equals(oldNfd0, newNfd0)) {
            errln(
                    "Failed "
                            + type
                            + "\t"
                            + Utility.hex(i)
                            + "\t"
                            + Utility.hex(oldNfd0)
                            + "\t"
                            + Utility.hex(newNfd0));
        }
    }
}
