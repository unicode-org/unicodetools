package org.unicode.draft;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.StandardCodes;

import com.ibm.icu.util.ULocale;

public class GenerateLanguageNames {

    public static void main(String[] args) {
        getNames();
    }

    static String[][] langList =  {
        {"Chinese (Traditional)", "Chinese (Traditional Han)"},
        {"Bhojpuri", "Bihari"},
        {"Divehi", "Dhivehi"},
        {"Haitian", "Haitian Creole"},
        {"Khmer", "Cambodian"},
        {"Kirghiz", "Kyrgyz"},
        {"Lao", "Laothian"},
        {"Scottish Gaelic", "Scots Gaelic"},
        {"Sinhala", "Sinhalese"},
        {"Western Frisian", "Frisian"},

        {"English", "English"},
        {"Chinese", "Chinese (Simplified)"},
        {"Spanish", "Spanish"},
        {"Japanese", "Japanese"},
        {"Portuguese", "Portuguese"},
        {"Russian", "Russian"},
        {"Arabic", "Arabic"},
        {"German", "German"},
        {"French", "French"},
        {"Korean", "Korean"},
        {"Turkish", "Turkish"},
        {"Traditional Chinese", "Chinese (Traditional)"},
        {"Italian", "Italian"},
        {"Polish", "Polish"},
        {"Dutch", "Dutch"},
        {"Thai", "Thai"},
        {"Vietnamese", "Vietnamese"},
        {"Persian", "Persian"},
        {"Hindi", "Hindi"},
        {"Indonesian", "Indonesian"},
        {"Ukrainian", "Ukrainian"},
        {"Romanian", "Romanian"},
        {"Swedish", "Swedish"},
        {"Tagalog", "Filipino"},
        {"Hungarian", "Hungarian"},
        {"Czech", "Czech"},
        {"Greek", "Greek"},
        {"Danish", "Danish"},
        {"Filipino", "Filipino"},
        {"Finnish", "Finnish"},
        {"Hebrew", "Hebrew"},
        {"Slovak", "Slovak"},
        {"Bulgarian", "Bulgarian"},
        {"Norwegian Bokmål", "Norwegian"},
        {"Catalan", "Catalan"},
        {"Croatian", "Croatian"},
        {"Lithuanian", "Lithuanian"},
        {"Slovenian", "Slovenian"},
        {"Latvian", "Latvian"},
        {"Serbian", "Serbian"},
        {"Malay", "Malay"},
        {"Urdu", "Urdu"},
        {"Bengali", "Bengali"},
        {"Tamil", "Tamil"},
        {"Telugu", "Telugu"},
        {"Marathi", "Marathi"},
        {"Gujarati", "Gujarati"},
        {"Malayalam", "Malayalam"},
        {"Swahili", "Swahili"},
        {"Kannada", "Kannada"},
        {"Oriya", "Oriya"},
        {"Estonian", "Estonian"},
        {"Basque", "Basque"},
        {"Icelandic", "Icelandic"},
        {"Amharic", "Amharic"},
        {"Azerbaijani", "Azerbaijani"},
        {"Javanese", "Javanese"},
        {"Punjabi", "Punjabi"},
        {"Kurdish", "Kurdish"},
        {"Belarusian", "Belarusian"},
        {"Sundanese", "Sundanese"},
        {"Yoruba", "Yoruba"},
        {"Uighur", "Uighur"},
        {"Uzbek", "Uzbek"},
        {"Bhojpuri", "Bihari"},
        {"Quechua", "Quechua"},
        {"Albanian", "Albanian"},
        {"Mongolian", "Mongolian"},
        {"Galician", "Galician"},
        {"Pashto", "Pashto"},
        {"Kazakh", "Kazakh"},
        {"Occitan", "Occitan"},
        {"Sindhi", "Sindhi"},
        {"Sinhala", "Sinhalese"},
        {"Haitian", "Haitian Creole"},
        {"Georgian", "Georgian"},
        {"Guarani", "Guarani"},
        {"Tibetan", "Tibetan"},
        {"Macedonian", "Macedonian"},
        {"Nepali", "Nepali"},
        {"Kirghiz", "Kyrgyz"},
        {"Tajik", "Tajik"},
        {"Afrikaans", "Afrikaans"},
        {"Lao", "Laothian"},
        {"Welsh", "Welsh"},
        {"Armenian", "Armenian"},
        {"Western Frisian", "Frisian"},
        {"Breton", "Breton"},
        {"Tatar", "Tatar"},
        {"Corsican", "Corsican"},
        {"Luxembourgish", "Luxembourgish"},
        {"Maltese", "Maltese"},
        {"Maori", "Maori"},
        {"Burmese", "Burmese"},
        {"Irish", "Irish"},
        {"Khmer", "Cambodian"},
        {"Divehi", "Dhivehi"},
        {"Scottish Gaelic", "Scots Gaelic"},
        {"Esperanto", "Esperanto"},
        {"Faroese", "Faroese"},
        {"Inuktitut", "Inuktitut"},
        {"Cherokee", "Cherokee"},
        {"Tonga", "Tonga"},
        {"Syriac", "Syriac"},
        {"Sanskrit", "Sanskrit"},
        {"Latin", "Latin"},
    };

    static String[][] corrections = {
        {"Occitan", "oc"},
        {"Laothian", "Lao"},
        {"Luxembourgish", "lb"},
    };
    static void getNames() {
        Map<String,String> nameToLocale = new TreeMap();
        Map<String,String> localeToName = new TreeMap();
        StandardCodes sc = StandardCodes.make();
        for (String lang : sc.getAvailableCodes("language")) {
            String[] names = sc.getData("language", lang).split("▪");
            for (String name : names) {
                nameToLocale.put(name, lang);
                localeToName.put(lang, name);
            }
        }
        for (String[] pair : corrections) {
            if ("XXXX".equals(pair[1])) continue;
            nameToLocale.put(pair[0], pair[1]);
            localeToName.put(pair[1], pair[0]);
        }
        for (ULocale locale : ULocale.getAvailableLocales()) {
            String lang = locale.toLanguageTag();
            nameToLocale.put(locale.getDisplayName(ULocale.ENGLISH), lang);
            String name = locale.getDisplayNameWithDialect(ULocale.ENGLISH);
            nameToLocale.put(name, lang);
            localeToName.put(lang, name);
        }
        Set<String> locales = new TreeSet<String>(Arrays.asList("ak", "ba", "bh", "bs", "ha", "haw", "ia", "ig", "iw", "lg", "ln", "mfe", "mg", "mo", "nn", "no", "om", "pt-BR", "pt-PT", "rm", "rn", "rw", "sh", "sn", "so", "sr-ME", "st", "ti", "tk", "tl", "tn", "tw", "xh", "yi", "zh-CN", "zh-TW", "zu"));
        Set<String> missing = new TreeSet<String>();
        for (String[] pair : langList) {
            String locale = nameToLocale.get(pair[0]);
            if (locale != null) {
                locales.add(locale);
                nameToLocale.put(pair[1], locale);
                continue;
            }
            locale = nameToLocale.get(pair[1]);
            if (locale != null) {
                locales.add(locale);
                nameToLocale.put(pair[0], locale);
                continue;
            }
            missing.add(pair[1]);            
        }
        for (String miss : missing) {
            System.out.println("{\"" + miss + "\", \"XXXX\"},");
        }

        for (String name : nameToLocale.keySet()) {
            String locale = nameToLocale.get(name);
            if (!locales.contains(locale)) continue;
            System.out.println(name + "\t" + locale + "\t" + localeToName.get(locale));
        }
    }
}
