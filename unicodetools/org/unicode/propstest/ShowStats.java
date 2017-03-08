package org.unicode.propstest;

import java.util.EnumMap;
import java.util.EnumSet;

import org.unicode.cldr.util.Counter;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.props.VersionToAge;
import org.unicode.tools.emoji.EmojiData;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

public class ShowStats {
    private static final IndexUnicodeProperties latest = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    private static final UnicodeSet ideo = latest.loadEnum(UcdProperty.Ideographic, Binary.class).getSet(Binary.Yes);
    private static final UnicodeMap<Age_Values> ageMap = latest.loadEnum(UcdProperty.Age, Age_Values.class);
    private static final UnicodeMap<Script_Values> script = latest.loadEnum(UcdProperty.Script, Script_Values.class);
    private static final UnicodeMap<General_Category_Values> cat = latest.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
    private static final UnicodeSet EMOJI = EmojiData.EMOJI_DATA.getSingletonsWithDefectives();
    
    //    private static final UnicodeMap<String> name = latest.load(UcdProperty.Name);
    //    private static final UnicodeMap<String> block = latest.load(UcdProperty.Block);
    //    private static final UnicodeMap<String> emojiDCM = latest.load(UcdProperty.Emoji_DCM);
    //    private static final UnicodeMap<String> emojiKDDI = latest.load(UcdProperty.Emoji_KDDI);
    //    private static final UnicodeMap<String> emojiSB = latest.load(UcdProperty.Emoji_SB);
    //private static final UnicodeMap<String> svsRaw = latest.load(UcdProperty.Standardized_Variant);

    //    private static final UnicodeSet svs = new UnicodeSet();
    //    static {
    //        for (Entry<String, String> entry : svsRaw.entrySet()) {
    //            String v = entry.getValue();
    //            if (v.equals("text style") || v.equals("emoji style")) {
    //                svs.add(entry.getKey().codePointAt(0));
    //            }
    //        }
    //        svs.freeze();
    //    }

    enum Bucket {
        Ideographic, 
        Other_Letter, 
        Emoji, 
        Other_Symbol, 
        Mark, 
        Number, 
        Punctuation, 
        Format("Format/Control/Sep"), 
        Other;
        final String title;

        private Bucket(String name) {
            this.title = name;
        }
        private Bucket() {
            this.title = name();
        }
        static Bucket getBucket(int cp) {
            if (ideo.contains(cp)) {
                return Bucket.Ideographic;
            }
            if (EMOJI.contains(cp)) {
                return Bucket.Emoji;
            }
            General_Category_Values rawCat = cat.get(cp);
            switch (rawCat) {
            case Modifier_Letter:
            case Lowercase_Letter:
            case Uppercase_Letter:
            case Titlecase_Letter:
            case Other_Letter:
                return Bucket.Other_Letter;

            case Other_Punctuation:
            case Close_Punctuation:
            case Dash_Punctuation:
            case Connector_Punctuation:
            case Final_Punctuation:
            case Initial_Punctuation:
            case Open_Punctuation:
                return Bucket.Punctuation;

            case Math_Symbol:
            case Currency_Symbol:
            case Modifier_Symbol:
            case Other_Symbol:
                return Bucket.Other_Symbol;

            case Space_Separator:
            case Paragraph_Separator:
            case Line_Separator:
            case Control:
            case Format:
                return Bucket.Format;

            case Decimal_Number:
            case Letter_Number:
            case Other_Number:
                return Bucket.Number;

            case Nonspacing_Mark:
            case Enclosing_Mark:
            case Spacing_Mark:
                return Bucket.Mark;

            case Unassigned:
            case Surrogate: // we don't care about these.
            case Private_Use:
                return Bucket.Other;

            case Symbol:
            case Separator:
            case Other:
            case Number:
            case Mark:
            case Cased_Letter:
            case Punctuation:
            case Letter:
            default: throw new IllegalAccessError();
            }
        } 
    }

    //    private static class EmojiData {
    //        final String default7;
    //        final String sv7;
    //
    //        UnicodeSet emoji = new UnicodeSet(); 
    //        public EmojiData(String default7, String sv7) {
    //            this.default7 = default7;
    //            this.sv7 = sv7;
    //        }
    //    }

    public static void main(String[] args) {

        //        showEmoji();
        //        if (true) return;

        Counter<R2<Bucket, Age_Values>> c = new Counter<Row.R2<Bucket, Age_Values>>();
        // Counter<General_Category_Values> catCounter = new Counter<General_Category_Values>();
        EnumMap<Age_Values,EnumSet<Script_Values>> scriptCounter = new EnumMap<>(Age_Values.class);
        //        Counter<Age_Values> emojis = new Counter<Age_Values>();

        for (int i = 0; i <= 0x10ffff; ++i) {
            Bucket catGroup = Bucket.getBucket(i);
            Age_Values ageValue = ageMap.get(i);
            c.add(Row.of(catGroup, ageValue), 1);
            //catCounter.add(catGroup,1);
            //            if (catGroup == General_Category_Values.Symbol && ageValue == Age_Values.V7_0) {
            //                System.out.println(block.get(i) + "\tU+" + Utility.hex(i) + "\t" + rawCat + "\t" + name.get(i));
            //            }

            // add to all older
            Script_Values scriptValue = script.get(i);
            for (Age_Values ageV : Age_Values.values()) {
                if (ageV.compareTo(ageValue) < 0) {
                    continue;
                }
                addScript(scriptCounter, ageV, scriptValue);
//                if (EMOJI.contains(i)) {
//                    //                    emojis.add(ageV, 1);
//                }
            }

        }
        Age_Values[] ages = Age_Values.values();
        Age_Values last = ages[ages.length-2];
        int firstYear = VersionToAge.ucd.getYear(Age_Values.V1_1);
        int lastYear = VersionToAge.ucd.getYear(last);

        System.out.print("Year");
        for (int year = firstYear; year <= lastYear; ++year) {
            System.out.print("\t" + year);
        }
        System.out.print("\n");

        System.out.print("Version");
        for (int year = firstYear; year <= lastYear; ++year) {
            Age_Values age = VersionToAge.ucd.getAge(VersionToAge.getDate(year+1,1)-1);
            System.out.print("\t" + (VersionToAge.ucd.getYear(age) == year ? age.getShortName() : ""));
        }
        System.out.print("\n");

        System.out.print("Scripts");
        int lastScriptCount = 0;
        for (int year = firstYear; year <= lastYear; ++year) {
            Age_Values age = VersionToAge.ucd.getAge(VersionToAge.getDate(year+1,1)-1);
            final int scriptCount = scriptCounter.get(age).size();
            System.out.print("\t" + (scriptCount - lastScriptCount));
            lastScriptCount = scriptCount;
        }
        System.out.print("\n");

        for (Bucket cat : Bucket.values()) {
            if (cat == Bucket.Other) { // || catCounter.get(cat) == 0) {
                continue;
            }
            System.out.print(cat.title);
            
            for (int year = firstYear; year <= lastYear; ++year) {
                Age_Values age = VersionToAge.ucd.getAge(VersionToAge.getDate(year+1,1)-1);
                long value = VersionToAge.ucd.getYear(age) == year ? c.get(Row.of(cat,age)) : 0;
                System.out.print("\t" + value);
            }
            System.out.print("\n");
        }


        //        System.out.print("Emoji");
        //        total = 0;
        //        for (Age_Values age : Age_Values.values()) {
        //            if (age == Age_Values.Unassigned) {
        //                continue;
        //            }
        //            System.out.print("\t" + emojis.get(age));
        //        }
        //        System.out.print("\n");

    }

    //    private static void showEmoji() {
    //        // # Code Point(s)  Proposed 7.0 Default    Proposed 7.0 SVs    Age Char    Names
    //        getEmoji();
    //        UnicodeSet source = new UnicodeSet()
    //        .addAll(emojiDCM.keySet())
    //        .addAll(emojiKDDI.keySet())
    //        .addAll(emojiSB.keySet())
    //        ;
    //        Set<String> emojiSources = new TreeSet();
    //
    //        for (Entry<String, EmojiData> entry : emojiData.entrySet()){
    //            String s = entry.getKey();
    //            EmojiData v = entry.getValue();
    //            System.out.println("U+" + Utility.hex(s," U+")
    //                    + "\t" + v.default7
    //                    + "\t" + v.sv7
    //                    + "\t" + getAge(s)
    //                    + "\t" + s
    //                    + "\t" + getSVs(s)
    //                    + "\t" + getEmojiSources(s, emojiSources)
    //                    + "\t" + getName(s)
    //                    + "\t" + getBlock(s, emojiSources)
    //                    );
    //        }
    //    }

    //    private static String getSVs(String s) {
    //        return svs.containsSome(s) 
    //                ? "SV" 
    //                        : "";
    //    }

    //    private static String getEmojiSources(String s, Set<String> emojiSources) {
    //        emojiSources.clear();
    //        int cp;
    //        for (int i = 0; i < s.length(); i+= Character.charCount(cp)) {
    //            cp = s.codePointAt(i);
    //            getEmojiSources(cp, emojiSources);
    //        }
    //        return CollectionUtilities.join(emojiSources, " ");
    //    }

    //    private static String getBlock(String s, Set<String> emojiSources) {
    //        emojiSources.clear();
    //        int cp;
    //        for (int i = 0; i < s.length(); i+= Character.charCount(cp)) {
    //            cp = s.codePointAt(i);
    //            emojiSources.add(block.get(cp));
    //        }
    //        return CollectionUtilities.join(emojiSources, " ");
    //    }


    //    private static Set<String> getEmojiSources(int cp, Set<String> result) {
    //        addItem(cp, emojiDCM, "dcm", result);
    //        addItem(cp, emojiKDDI, "kddi", result);
    //        addItem(cp, emojiSB, "sb", result);
    //        return result;
    //    }

    //    private static void addItem(int cp, UnicodeMap<String> unicodeMap, 
    //            String name, Set<String> sb) {
    //        String s = unicodeMap.get(cp);
    //        if (s != null) {
    //            sb.add(name);
    //        }
    //    }

    //    private static Age_Values getAge(String s) {
    //        Age_Values biggest = Age_Values.V1_1;
    //        int cp;
    //        for (int i = 0; i < s.length(); i+= Character.charCount(cp)) {
    //            cp = s.codePointAt(i);
    //            Age_Values result = ageMap.get(cp);
    //            if (result.compareTo(biggest) > 0) {
    //                biggest = result;
    //            }
    //        }
    //        return biggest;
    //    }

    // private static final LocaleDisplayNames ldn = LocaleDisplayNames.getInstance(ULocale.ENGLISH);

    //    private static String getName(String s) {
    //        if (s.endsWith("\u20E3")) {
    //            return "keycap " + name.get(s.codePointAt(0));
    //        }
    //        int first = s.codePointAt(0);
    //        if (0x1F1E6 <= first && first <= 0x1F1FF) {
    //            int last = s.codePointAt(2);
    //            return "flag " + ldn.regionDisplayName(
    //                    new StringBuffer()
    //                    .appendCodePoint(first-0x1F1E6+'A')
    //                    .appendCodePoint(last-0x1F1E6+'A').toString());
    //        }
    //        return name.get(s);
    //    }

    private static void addScript(
            EnumMap<Age_Values, EnumSet<Script_Values>> scriptCounter,
            Age_Values ageValue, Script_Values scriptValue) {
        EnumSet<Script_Values> scripts = scriptCounter.get(ageValue);
        if (scripts == null) {
            scriptCounter.put(ageValue, scripts = EnumSet.noneOf(Script_Values.class));
        }
        scripts.add(scriptValue);
    }

    //    private static final UnicodeMap<EmojiData> emojiData = new UnicodeMap<>();

    //    private static class MyPro extends org.unicode.cldr.draft.FileUtilities.FileProcessor {
    //        static Transliterator t = Transliterator.getInstance("hex-any/Unicode");
    //        @Override
    //        protected boolean handleLine(int lineCount, String line) {
    //            String[] parts = line.split("\t");
    //            String unicode = t.transform(parts[0].replace(" ", ""));
    //            emojiData.put(unicode, new EmojiData(parts[1], parts.length > 2 ? parts[2] : ""));
    //            return true;
    //        }
    //    }
    //    private static void getEmoji() {
    //        MyPro x = new MyPro();
    //        x.process(ShowStats.class, "emoji-like.txt");
    //    }
}
