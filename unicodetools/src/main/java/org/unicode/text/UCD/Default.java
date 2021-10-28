package org.unicode.text.UCD;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.unicode.text.utility.Settings;

import com.ibm.icu.util.VersionInfo;


public final class Default implements UCD_Types {

    private static String ucdVersion = Settings.latestVersion;
    private static UCD ucd;
    private static Normalizer nfc;
    private static Normalizer nfd;
    private static Normalizer nfkc;
    private static Normalizer nfkd;
    private static Normalizer[] nf = new Normalizer[4];
    private static String year;

    public static void setUCD(String version) {
        ucdVersion = version;
        setUCD();
    }

    private static boolean inRecursiveCall = false;
    
    private static void setUCD() {
        if (inRecursiveCall) {
            throw new IllegalArgumentException("Recursive call to setUCD");
        }
        inRecursiveCall = true;
        ucd = UCD.make(ucdVersion);
        nfd = nf[NFD] = new Normalizer(UCD_Types.NFD, ucdVersion());
        nfc = nf[NFC] = new Normalizer(UCD_Types.NFC, ucdVersion());
        nfkd = nf[NFKD] = new Normalizer(UCD_Types.NFKD, ucdVersion());
        nfkc = nf[NFKC] = new Normalizer(UCD_Types.NFKC, ucdVersion());
        System.out.println("Loaded UCD" + ucd().getVersion() + " " + (new Date(ucd().getDate())));
        inRecursiveCall = false;
    }

    //static DateFormat myDateFormat = new SimpleDateFormat("yyyy-MM-dd");
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
        if (ucd == null) {
            setUCD();
        }
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
        if (ucd == null) {
            setUCD();
        }
        return nfc;
    }
    public static Normalizer nfd() {
        if (ucd == null) {
            setUCD();
        }
        return nfd;
    }
    public static Normalizer nfkc() {
        if (ucd == null) {
            setUCD();
        }
        return nfkc;
    }
    public static Normalizer nfkd() {
        if (ucd == null) {
            setUCD();
        }
        return nfkd;
    }
    public static Normalizer nf(int index) {
        if (ucd == null) {
            setUCD();
        }
        return nf[index];
    }

    /**
     * @param lineValue
     */
    public static void setYear(String lineValue) {
        year = lineValue;
    }
}