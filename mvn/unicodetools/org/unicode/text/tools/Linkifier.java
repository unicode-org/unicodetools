package org.unicode.text.tools;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.RegexUtilities;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.UnicodeRegex;
import com.ibm.icu.text.IDNA;
import com.ibm.icu.text.IDNA.Info;
import com.ibm.icu.text.UnicodeSet;

public class Linkifier {
    static final UnicodeSet SCHEME = new UnicodeSet("[-+.a-zA-Z0-9]").freeze();
    static final UnicodeSet INCLUSIONS = new UnicodeSet("[, ; \\: ! ¡ ¿ . · ' \" @ * \\\\ \\& % \\u200C \\u200D]").freeze();
    static final UnicodeSet DOTS = new UnicodeSet("[.．。｡]").freeze();
    static final String SCHEME_END = "://";
    static UnicodeSet NORMAL = new UnicodeSet("[\\p{L}\\p{N}\\p{M}\\p{S}\\p{Pd}\\p{Pc}]").freeze();
//    static UnicodeSet DOMAIN = UProperty.
//            private static final Normalizer2 uts46Norm2=
//            Normalizer2.getInstance(null, "uts46", Normalizer2.Mode.COMPOSE);  // uts46.nrm
//    static {
//        uts46Norm2.
//    }

    private static String[] GROUPS = {"scheme", "domain", "path", "query", "fragment"};
    private static String[] REGEX_SOURCE = {
        "$URL =        (:<scheme> $scheme ://)? (:<domain> $domain) (:<path> / $path)? (:<query> [?] $query)? (:<fragment> \\x{23} $fragment)? ;",
        "$scheme =     [-+.a-zA-Z0-9]+ ;",
        "$domain =     $label (?:$IDNSep $label)* $IDNSep? ;",
        "$label =      [[^\\p{C}\\p{Z}\\x{0}-\\x{7F}][-0-9a-zA-Z \\u00AD\\u200B-\\u200D\\u2060\\u2064\\uFEFF\\x{1BCA0}-\\x{0001BCA3}]]+ ;",
        "$IDNSep =     [.．。｡] ;",
        "$path =       (?: $percentEncodedUtf8Char | $char | /)* ;",
        "$query =      (?: $percentEncodedUtf8Char | $char | [/?])* ;",
        "$fragment =   (?: $percentEncodedUtf8Char | $char | [/?\\x{23}])* ;",
        "$percentEncodedUtf8Char = ( %\\p{XDigit}\\p{XDigit} )+ ;",
        "$char =       [\\p{L}\\p{N}\\p{M}\\p{S}\\p{Pd}\\p{Pc}$inclusionChar&[^$exclusionChar]] ;", // [^\\p{C}\\p{Z}] ;", // 
        "$inclusionChar = [, ; \\: ! ¡ ¿ . · ' \" @ * \\\\ \\& % \\u200C \\u200D];",
        "$exclusionChar = [<>] ;",
        //        "$hash =    \\u0023 ;",
        //        "$question = \\u003F ;",
        //        "$semi = \\u003B ;",
    };
    
    enum ScanStatus {
        /**
         * The scan made it to the end, and there is a valid token.
         */
        AT_END, 
        /**
         * The scan didn't make it to the end, but there is a valid token.
         */
        VALID, 
        STOPPED, 
        INVALID}
    
    private class ScanData {
        String source;
        int start;
        int lastValidLimit;
        int lastLimit;
    }

    private abstract class Scanner {
        /**
         * Scans the input for a valid token. Returns true if scanned to the end.
         * @param input
         * @param index
         * @return
         */
        public abstract ScanStatus scan(ScanData input);
    }

    static class TldMatcher {
        static final List<String> TLDS = Arrays.asList("com", "org", "de", "香港");
        boolean matches(String s, int limit) {
            for (String item : TLDS) {
                if (s.regionMatches(limit-item.length(), item, 0, item.length())) {
                    return true;
                }
            }
            return false;
        }
    }
    
    private class UrlScanner extends Scanner {
        private static final char FRAGMENT_START = '#';
        private static final char QUERY_START = '?';
        private static final char PATH_START = '/';
        Scanner schemeScanner = new SimpleScanner(new UnicodeSet("[-+.a-zA-Z0-9]"), UnicodeSet.EMPTY);
        Scanner domainScanner = new DomainScanner();
        Scanner pathScanner = new SimpleScanner(new UnicodeSet(NORMAL).add(PATH_START), INCLUSIONS);
        Scanner queryScanner = new SimpleScanner(new UnicodeSet(NORMAL).add(QUERY_START), INCLUSIONS);
        Scanner fragmentScanner = new SimpleScanner(new UnicodeSet(NORMAL).add(FRAGMENT_START), INCLUSIONS);
        TldMatcher tldMatcher = new TldMatcher();
        public ScanStatus scan(ScanData input) {
            int start = input.lastValidLimit;
            ScanStatus status = schemeScanner.scan(input);
            if (status == ScanStatus.AT_END) {
                return status;
            }
            if (input.source.regionMatches(input.lastValidLimit, SCHEME_END, 0, SCHEME_END.length())) {
                // we have a scheme, look for a domain
                input.lastValidLimit += SCHEME_END.length();
                status = domainScanner.scan(input);
                if (status == ScanStatus.INVALID) {
                    return status;
                }
            } else { // no scheme. Look for domain from the beginning, but has to end with known TLD
                input.lastValidLimit = start;
                status = domainScanner.scan(input);
                if (status.equals(ScanStatus.INVALID)) {
                    return status;
                }
                if (!tldMatcher.matches(input.source, input.lastValidLimit)) {
                    return ScanStatus.INVALID;
                }
            }
            return status;
        }
    }
    private class SimpleScanner extends Scanner {
        private UnicodeSet ok;
        private UnicodeSet softBreak;
        public SimpleScanner(UnicodeSet ok, UnicodeSet softBreak) {
            this.ok = ok.freeze();
            this.softBreak = softBreak.freeze();
        }
        public ScanStatus scan(ScanData data) {
            int cp;
            int codePointLength;
            for (int i = data.start; i < data.source.length(); i += codePointLength) {
                cp = data.source.codePointAt(i);
                codePointLength = Character.charCount(cp);
                if (ok.contains(cp)) {
                    data.lastValidLimit = i;
                } else if (!softBreak.contains(cp)) {
                    data.lastLimit = i;
                    return data.lastValidLimit >= 0 ? ScanStatus.STOPPED : ScanStatus.INVALID;
                }
            }
            data.lastLimit = data.source.length();
            return ScanStatus.AT_END;
        }
    }
    
    class DomainScanner extends SimpleScanner {

        DomainScanner() {
            super(NORMAL, DOTS);
        }
        
    }
    
    private static final String REGEX_STRING;
    private static final Pattern REGEX;
    static {
        UnicodeRegex unicodeRegex = new UnicodeRegex();
        REGEX_STRING = unicodeRegex.compileBnf(Arrays.asList(REGEX_SOURCE));
        REGEX = Pattern.compile(REGEX_STRING, Pattern.COMMENTS);
    }

    private Matcher matcher = REGEX.matcher("");
    private IDNA idna = IDNA.getUTS46Instance(0);
    
    public Linkifier reset(String source) {
        matcher.reset(source);
        return this;
    }

    public boolean find() {
        return matcher.find();
    }

    public int start () {
        return matcher.start(0);
    }

    public int end () {
        return matcher.end(0);
    }

    private String findMismatch(String source) {
        return RegexUtilities.showMismatch(matcher, source);
    }

    public static void main(String[] args) {
        
        Pattern pat = Pattern.compile("(?<login>\\w+) (?<id>\\d+)");
        Matcher m = pat.matcher("TEST 123");
        m.matches();
        System.out.println("1" + "\t" + m.group(1));
        System.out.println("2" + "\t" + m.group(2));
        System.out.println("login" + "\t" + m.group("login"));
        System.out.println("id" + "\t" + m.group("id"));
        
        
        String[] tests = {
                "google.com",
                "xn--a.com",
                "google.com./",
                "google.com./abc/def",
                "αβγ｡com/δεζ?η=θ?&ι=κ#λμ_?#ξ",
                "http://google.com/",
                "αβγ@δεζ.com",
                "mailto:αβγ@δεζ.com",
                "file:///c:/WINDOWS/clock.avi",
                "http://be.wikipedia.org/wiki/Вікіпедыя:Артыкулы,_якія_мусяць_быць_у_кожнай_Вікіпедыі",
                "http://bpy.wikipedia.org/wiki/উইকিপিডিয়া:থানা_থকিসে_নিবন্ধ",
                "http://bug.wikipedia.org/wiki/Wikipedia:Daftar_artikel_ᨆᨊᨛᨂ_ᨅᨔ_ᨄᨑᨛᨒᨘ_ᨂᨛᨀ",
                "http://fa.wikipedia.org/wiki/ویکی‌پدیا:فهرست_نوشتارهایی_که_هر_ویکی‌پدیا_باید_بدارد",
                "http://gu.wikipedia.org/wiki/વિકિપીડિયા:દરેક_ભાષાના_વિકિપીડિયામાં_હોય_એવા_પ્રારંભિક_લેખોની_યાદી",
                "http://hi.wikipedia.org/wiki/विकिपीडिया:कुछ_प्रारंभिक_लेख_जो_कि_हर_भाषा_के_विकिपीडिया_में_होने_चाहिए",
                "http://kn.wikipedia.org/wiki/ವಿಕಿಪೀಡಿಯ:ಅಗತ್ಯ_ಲೇಖನಗಳು",
                "http://ml.wikipedia.org/wiki/വിക്കിപീഡിയ:അവശ്യലേഖനങ്ങള്‍",
                "http://mr.wikipedia.org/wiki/विकिपीडिया:लेख_संपादन_स्पर्धा/लेखांची_यादी",
                "http://zh-min-nan.wikipedia.org/wiki/Wikipedia:Só͘-ū_ê_Wikipedia_pán-pún_lóng_èng-kai_ū_ê_bûn-chiuⁿ",
                "http://new.wikipedia.org/wiki/विकिपिडियाःहलिमसफूया_ख्यःत",
                "http://os.wikipedia.org/wiki/Википеди:Алы_æвзагыл_дæр_чи_хъуамæ_уа,_уыцы_статьятæ",
                "http://ru.wikipedia.org/wiki/Википедия:Список_статей,_которые_должны_быть_во_всех_языковых_версиях",
                "http://ta.wikipedia.org/wiki/Wikipedia:அனைத்து_மொழி_விக்கிபீடியாக்களிலும்_இருக்க_வேண்டிய_கட்டுரைகளின்_பட்டியல்",
                "http://te.wikipedia.org/wiki/Wikipedia:వికీపీడియాలో_తప్పకుండా_ఉండవలసిన_వ్యాసాలు",
                "http://th.wikipedia.org/wiki/วิกิพีเดีย:รายชื่อบทความที่วิกิพีเดียทุกภาษาควรมี",
                "http://uk.wikipedia.org/wiki/Вікіпедія:Статті,_які_повинні_бути_у_всіх_вікіпедіях",
                "http://yi.wikipedia.org/wiki/װיקיפּעדיע:וויכטיגע_ארטיקלן"
        };
        Linkifier linkifier = new Linkifier();
        System.out.println(CollectionUtilities.join(REGEX_SOURCE, "\n\t"));
        System.out.println(Linkifier.REGEX_STRING);
        for (String test : tests) {
            String source = "(" + test + ")";
            linkifier.reset(source);
            boolean found = linkifier.find();
            if (!found) {
                System.out.println(found + "\t" + linkifier.findMismatch(source));
            } else {
                int start = linkifier.start();
                int end = linkifier.end();
                final int fromEnd = source.length()-end;
                if (start != 1 || fromEnd != 1) {
                    found = false;
                }
                System.out.println(found + "\t" + start + "\t" + fromEnd
                        + "\t«" + source.substring(0,start)
                        + "❴❴❴" + source.substring(start, end)
                        + "❵❵❵" + source.substring(end, source.length())
                        + "»");
                for (String name : GROUPS) {
                    final String group = linkifier.matcher.group(name);
                    if (group != null) {
                        System.out.println("\t" + name + " " + group + "\t" + linkifier.check(name, group));
                    }
                }
            }
        }
    }

    private String check(String groupName, String group) {
            switch (groupName) {
            case "domain": 
                Info info = new Info();
                StringBuilder result = idna.nameToUnicode(group, new StringBuilder(), info);
                if (info.hasErrors()) {
                    result.append("\tErrors: ").append(info.getErrors().toString());
                }
                return result.toString();
            }
            return "";
    }
}
