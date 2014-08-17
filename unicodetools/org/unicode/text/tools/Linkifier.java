package org.unicode.text.tools;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.RegexUtilities;

import com.ibm.icu.impl.UnicodeRegex;

public class Linkifier {
    private static final Pattern REGEX;
    private static final String REGEX_STRING;

    static {
        UnicodeRegex unicodeRegex = new UnicodeRegex();
        REGEX_STRING = unicodeRegex.compileBnf(Arrays.asList(
                "$URL =        ($scheme ://)? ($domain) (/ $path)? ($question $query)? ($hash $fragment)?;"
                ,"$scheme =     [-+.a-zA-Z0-9]+ ;"
                ,"$domain =         $label ($IDNSep $label)* $IDNSep? ;"
                ,"$label =      [[^[:C:][:Z:][:ascii:]][-0-9a-zA-Z \\u00AD\\u200B-\\u200D\\u2060\\u2064\\uFEFF\\x{1BCA0}-\\x{0001BCA3}]]+ ;"
                ,"$IDNSep =         [.．。｡] ;"
                ,"$path =       (?:$percentEncodedUtf8Char | $char | /)* ;"
                ,"$char =       [^[:C:][:Z:]] ;" // $inclusionChar&[^$exclusionChar}]
                ,"$query =      (?:$percentEncodedUtf8Char | $char)* ;"
                ,"$fragment =   (?:$percentEncodedUtf8Char | $char)* ;"
                ,"$percentEncodedUtf8Char = (%\\p{XDigit}\\p{XDigit})+ ;"
                //                ,"$inclusionChar = [!-$hash%-'*,./\\:$semi?@¡·¿] ;"
                //                ,"$exclusionChar = [<>] ;"
                ,"$hash = \\u0023 ;"
                ,"$question = \\u003F ;"
                //                ,"$semi = \\u003B ;"
                ));
        REGEX = Pattern.compile(REGEX_STRING, Pattern.COMMENTS);
    }

    private Matcher matcher = REGEX.matcher("");

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
        String[] tests = {
                "google.com",
                "google.com./",
                "google.com./abc/def",
                "αβγ.com/δεζ?η=θ&ι=κ#λμ_ξ",
                "http://google.com/",
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
        "http://yi.wikipedia.org/wiki/װיקיפּעדיע:וויכטיגע_ארטיקלן"};
        Linkifier linkifier = new Linkifier();
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
                for (int i = 1; i <= linkifier.matcher.groupCount(); ++i) {
                    final String group = linkifier.matcher.group(i);
                    if (group != null) {
                        System.out.println("\t" + i + " " + group);
                    }
                }
            }
        }
    }
}
