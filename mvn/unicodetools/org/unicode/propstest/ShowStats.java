package org.unicode.propstest;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.Counter;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class ShowStats {
    static final IndexUnicodeProperties latest = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);
    static final UnicodeMap<String> ideo = latest.load(UcdProperty.Ideographic);
    static final UnicodeMap<String> age = latest.load(UcdProperty.Age);
    static final UnicodeMap<String> script = latest.load(UcdProperty.Script);
    static final UnicodeMap<String> cat = latest.load(UcdProperty.General_Category);
    static final UnicodeMap<String> name = latest.load(UcdProperty.Name);
    static final UnicodeMap<String> block = latest.load(UcdProperty.Block);
    static final UnicodeMap<String> emojiDCM = latest.load(UcdProperty.Emoji_DCM);
    static final UnicodeMap<String> emojiKDDI = latest.load(UcdProperty.Emoji_KDDI);
    static final UnicodeMap<String> emojiSB = latest.load(UcdProperty.Emoji_SB);
    static final UnicodeMap<String> svsRaw = latest.load(UcdProperty.Standardized_Variant);
    static final UnicodeSet svs = new UnicodeSet();

    static {
        for (Entry<String, String> entry : svsRaw.entrySet()) {
            String v = entry.getValue();
            if (v.equals("text style") || v.equals("emoji style")) {
                svs.add(entry.getKey().codePointAt(0));
            }
        }
        svs.freeze();
    }

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
    }

    static EnumMap<General_Category_Values, Bucket> CAT = new EnumMap(General_Category_Values.class);
    static {
        for (General_Category_Values cat : UcdPropertyValues.General_Category_Values.values()) {
            switch (cat) {
            case Modifier_Letter:
            case Lowercase_Letter:
            case Uppercase_Letter:
            case Titlecase_Letter:
            case Other_Letter:
                CAT.put(cat, Bucket.Other_Letter); break;

            case Other_Punctuation:
            case Close_Punctuation:
            case Dash_Punctuation:
            case Connector_Punctuation:
            case Final_Punctuation:
            case Initial_Punctuation:
            case Open_Punctuation:
                CAT.put(cat,  Bucket.Punctuation); break;

            case Math_Symbol:
            case Currency_Symbol:
            case Modifier_Symbol:
            case Other_Symbol:
                CAT.put(cat,  Bucket.Other_Symbol); break;

            case Space_Separator:
            case Paragraph_Separator:
            case Line_Separator:
            case Control:
            case Format:
                CAT.put(cat, Bucket.Format); break;

            case Decimal_Number:
            case Letter_Number:
            case Other_Number:
                CAT.put(cat, Bucket.Number); break;

            case Nonspacing_Mark:
            case Enclosing_Mark:
            case Spacing_Mark:
                CAT.put(cat, Bucket.Mark); break;

            case Unassigned:
            case Surrogate: // we don't care about these.
            case Private_Use:
                CAT.put(cat, Bucket.Other); break;

            case Symbol:
            case Separator:
            case Other:
            case Number:
            case Mark:
            case Cased_Letter:
            case Punctuation:
            case Letter:
                break;
            default: throw new IllegalAccessError();
            }
        } 
    }

    private static class EmojiData {
        final String default7;
        final String sv7;

        UnicodeSet emoji = new UnicodeSet(); 
        public EmojiData(String default7, String sv7) {
            this.default7 = default7;
            this.sv7 = sv7;
        }
    }

    public static void main(String[] args) {

        //        showEmoji();
        //        if (true) return;

        Counter c = new Counter<Row.R2<Bucket, Age_Values>>();
        Counter catCounter = new Counter<General_Category_Values>();
        EnumMap<Age_Values,EnumSet<Script_Values>> scriptCounter = new EnumMap(Age_Values.class);
//        Counter<Age_Values> emojis = new Counter<Age_Values>();

        for (int i = 0; i <= 0x10ffff; ++i) {
            General_Category_Values rawCat = General_Category_Values.valueOf(cat.get(i));
            Bucket catGroup = CAT.get(rawCat);
            Binary ideographic = Binary.valueOf(ideo.get(i));
            if (ideographic == Binary.Yes) {
                catGroup = Bucket.Ideographic;
            }
            if (org.unicode.text.tools.Emoji.EMOJI_CHARS.contains(i)) {
                catGroup = Bucket.Emoji;
            }
            Age_Values ageValue = Age_Values.valueOf(age.get(i));
            c.add(Row.of(catGroup, ageValue), 1);
            catCounter.add(catGroup,1);
            //            if (catGroup == General_Category_Values.Symbol && ageValue == Age_Values.V7_0) {
            //                System.out.println(block.get(i) + "\tU+" + Utility.hex(i) + "\t" + rawCat + "\t" + name.get(i));
            //            }

            // add to all older
            Script_Values scriptValue = Script_Values.valueOf(script.get(i));
            for (Age_Values ageV : Age_Values.values()) {
                if (ageV.compareTo(ageValue) < 0) {
                    continue;
                }
                addScript(scriptCounter, ageV, scriptValue);
                if (org.unicode.text.tools.Emoji.EMOJI_CHARS.contains(i)) {
//                    emojis.add(ageV, 1);
                }
            }

        }
        System.out.print("Script");
        for (Age_Values age : Age_Values.values()) {
            if (age == Age_Values.Unassigned) {
                continue;
            }
            System.out.print("\t" + scriptCounter.get(age).size());
        }
        System.out.print("\n");
        for (Age_Values age : Age_Values.values()) {
            System.out.print("\t" + age.getShortName());
        }
        System.out.print("\n");
        
        for (Bucket cat : Bucket.values()) {
            if (cat == Bucket.Other || catCounter.get(cat) == 0) {
                continue;
            }
            System.out.print(cat.title);
            long total = 0;
            for (Age_Values age : Age_Values.values()) {
                if (age == Age_Values.Unassigned) {
                    continue;
                }
                total += c.get(Row.of(cat,age));
                System.out.print("\t" + total);
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

    private static void showEmoji() {
        // # Code Point(s)  Proposed 7.0 Default    Proposed 7.0 SVs    Age Char    Names
        getEmoji();
        UnicodeSet source = new UnicodeSet()
        .addAll(emojiDCM.keySet())
        .addAll(emojiKDDI.keySet())
        .addAll(emojiSB.keySet())
        ;
        Set<String> emojiSources = new TreeSet();

        for (Entry<String, EmojiData> entry : emojiData.entrySet()){
            String s = entry.getKey();
            EmojiData v = entry.getValue();
            System.out.println("U+" + Utility.hex(s," U+")
                    + "\t" + v.default7
                    + "\t" + v.sv7
                    + "\t" + getAge(s)
                    + "\t" + s
                    + "\t" + getSVs(s)
                    + "\t" + getEmojiSources(s, emojiSources)
                    + "\t" + getName(s)
                    + "\t" + getBlock(s, emojiSources)
                    );
        }
    }

    private static String getSVs(String s) {
        return svs.containsSome(s) 
                ? "SV" 
                        : "";
    }

    private static String getEmojiSources(String s, Set<String> emojiSources) {
        emojiSources.clear();
        int cp;
        for (int i = 0; i < s.length(); i+= Character.charCount(cp)) {
            cp = s.codePointAt(i);
            getEmojiSources(cp, emojiSources);
        }
        return CollectionUtilities.join(emojiSources, " ");
    }

    private static String getBlock(String s, Set<String> emojiSources) {
        emojiSources.clear();
        int cp;
        for (int i = 0; i < s.length(); i+= Character.charCount(cp)) {
            cp = s.codePointAt(i);
            emojiSources.add(block.get(cp));
        }
        return CollectionUtilities.join(emojiSources, " ");
    }


    private static Set<String> getEmojiSources(int cp, Set<String> result) {
        addItem(cp, emojiDCM, "dcm", result);
        addItem(cp, emojiKDDI, "kddi", result);
        addItem(cp, emojiSB, "sb", result);
        return result;
    }

    public static void addItem(int cp, UnicodeMap<String> unicodeMap, 
            String name, Set<String> sb) {
        String s = unicodeMap.get(cp);
        if (s != null) {
            sb.add(name);
        }
    }

    public static String getAge(String s) {
        String biggest = "";
        int cp;
        for (int i = 0; i < s.length(); i+= Character.charCount(cp)) {
            cp = s.codePointAt(i);
            String result = age.get(cp);
            if (result.compareTo(biggest) > 0) {
                biggest = result;
            }
        }
        return biggest;
    }

    static final LocaleDisplayNames ldn = LocaleDisplayNames.getInstance(ULocale.ENGLISH);

    public static String getName(String s) {
        if (s.endsWith("\u20E3")) {
            return "keycap " + name.get(s.codePointAt(0));
        }
        int first = s.codePointAt(0);
        if (0x1F1E6 <= first && first <= 0x1F1FF) {
            int last = s.codePointAt(2);
            return "flag " + ldn.regionDisplayName(
                    new StringBuffer()
                    .appendCodePoint(first-0x1F1E6+'A')
                    .appendCodePoint(last-0x1F1E6+'A').toString());
        }
        return name.get(s);
    }

    public static void addScript(
            EnumMap<Age_Values, EnumSet<Script_Values>> scriptCounter,
            Age_Values ageValue, Script_Values scriptValue) {
        EnumSet<Script_Values> scripts = scriptCounter.get(ageValue);
        if (scripts == null) {
            scriptCounter.put(ageValue, scripts = EnumSet.noneOf(Script_Values.class));
        }
        scripts.add(scriptValue);
    }

    static final UnicodeMap<EmojiData> emojiData = new UnicodeMap();

    private static class MyPro extends org.unicode.cldr.draft.FileUtilities.FileProcessor {
        static Transliterator t = Transliterator.getInstance("hex-any/Unicode");
        @Override
        protected boolean handleLine(int lineCount, String line) {
            String[] parts = line.split("\t");
            String unicode = t.transform(parts[0].replace(" ", ""));
            emojiData.put(unicode, new EmojiData(parts[1], parts.length > 2 ? parts[2] : ""));
            return true;
        }
    }
    private static void getEmoji() {
        MyPro x = new MyPro();
        x.process(ShowStats.class, "emoji-like.txt");
    }
}
