package org.unicode.text.UCD;

import com.ibm.icu.util.VersionInfo;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.unicode.text.utility.Settings;

public final class Default implements UCD_Types {

    private static String ucdVersion = Settings.latestVersion;
    private static UCD ucd;
    private static Normalizer nfc;
    private static Normalizer nfd;
    private static Normalizer nfkc;
    private static Normalizer nfkd;
    private static String year;

    public static void setUCD(String version) {
        if (!ucdVersion.equals(version)) {
            ucdVersion = version;
            ucd = null;
            nfd = null;
            nfc = null;
            nfkd = null;
            nfkc = null;
        }
    }

    private static boolean inRecursiveCall = false;

    private static void setUCD() {
        if (inRecursiveCall) {
            throw new IllegalArgumentException("Recursive call to setUCD");
        }
        if (ucd != null) {
            return;
        }
        inRecursiveCall = true;
        ucd = UCD.make(ucdVersion);
        System.out.println("Loaded UCD" + ucd().getVersion() + " " + (new Date(ucd().getDate())));
        inRecursiveCall = false;
    }

    // static DateFormat myDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    static DateFormat myDateFormat = new SimpleDateFormat("yyyy-MM-dd', 'HH:mm:ss' GMT'");
    static DateFormat yearFormat = new SimpleDateFormat("yyyy");

    static {
        myDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        year = yearFormat.format(new Date());
    }

    public static String getDate() {
        return myDateFormat.format(new Date());
    }

    public static String getYear() {
        return year;
    }

    public static String ucdVersion() {
        return ucdVersion;
    }

    public static VersionInfo ucdVersionInfo() {
        return VersionInfo.getInstance(ucdVersion());
    }

    public static UCD ucd() {
        if (ucd == null) {
            setUCD();
        }
        return ucd;
    }

    public static Normalizer nfc() {
        if (nfc == null) {
            nfc = Normalizer.getOrMakeNfcInstance(ucdVersion());
        }
        return nfc;
    }

    public static Normalizer nfd() {
        if (nfd == null) {
            nfd = Normalizer.getOrMakeNfdInstance(ucdVersion());
        }
        return nfd;
    }

    public static Normalizer nfkc() {
        if (nfkc == null) {
            nfkc = Normalizer.getOrMakeNfkcInstance(ucdVersion());
        }
        return nfkc;
    }

    public static Normalizer nfkd() {
        if (nfkd == null) {
            nfkd = Normalizer.getOrMakeNfkdInstance(ucdVersion());
        }
        return nfkd;
    }

    /**
     * @param lineValue
     */
    public static void setYear(String lineValue) {
        year = lineValue;
    }
}
