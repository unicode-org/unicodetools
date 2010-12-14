package org.unicode.jsp;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.jsp.Idna.IdnaType;
import org.unicode.jsp.UnicodeProperty.BaseProperty;
import org.unicode.jsp.UnicodeProperty.SimpleProperty;

import sun.text.normalizer.UTF16;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.RawCollationKey;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class XPropertyFactory extends UnicodeProperty.Factory {

    static final UnicodeSet ALL = new UnicodeSet("[[:^C:][:Cc:][:Cf:][:noncharactercodepoint:]]").freeze();

    private static final boolean DEBUG_CHARSET_NAMES = false;

    private static XPropertyFactory singleton = null;

    public static synchronized XPropertyFactory make() {
        if (singleton != null) {
            return singleton;
        }
        singleton = new XPropertyFactory();
        return singleton;
    }


    {
        ICUPropertyFactory base = ICUPropertyFactory.make();
        for (String propertyAlias : (List<String>)base.getInternalAvailablePropertyAliases(new ArrayList())) {
            add(base.getProperty(propertyAlias));
        }
        for (int i = Common.XSTRING_START; i < Common.XSTRING_LIMIT; ++i) {
            XUnicodeProperty property = new XUnicodeProperty(i);
            add(property);
        }

        add(new IDNA2003());
        add(new UTS46());
        add(new IDNA2008());
        add(new IDNA2008c());
        add(new Usage());
        add(new HanType());
        add(new UnicodeProperty.UnicodeMapProperty().set(XIDModifications.getReasons()).setMain("identifier-restriction", "idr", UnicodeProperty.ENUMERATED, "1.1"));
        add(new UnicodeProperty.UnicodeMapProperty().set(Confusables.getMap()).setMain("confusable", "confusable", UnicodeProperty.ENUMERATED, "1.1"));
        add(new UnicodeProperty.UnicodeMapProperty().set(Idna2003.SINGLETON.mappings).setMain("toIdna2003", "toIdna2003", UnicodeProperty.STRING, "1.1"));
        add(new UnicodeProperty.UnicodeMapProperty().set(Uts46.SINGLETON.mappings).setMain("toUts46t", "toUts46t", UnicodeProperty.STRING, "1.1"));
        add(new UnicodeProperty.UnicodeMapProperty().set(Uts46.SINGLETON.mappings_display).setMain("toUts46n", "toUts46n", UnicodeProperty.STRING, "1.1"));

        add(new StringTransformProperty(Common.NFKC_CF, false).setMain("NFKC_Casefold", "NFKC_CF", UnicodeProperty.STRING, "1.1").addName("toNFKC_CF"));
        add(new UnicodeSetProperty().set(Common.isNFKC_CF).setMain("isNFKC_Casefolded", "isNFKC_CF", UnicodeProperty.BINARY, "1.1"));

        add(new UnicodeSetProperty().set(Common.isCaseFolded).setMain("isCaseFolded", "caseFolded", UnicodeProperty.BINARY, "1.1"));
        add(new UnicodeSetProperty().set(Common.isUppercase).setMain("isUppercase", "uppercase", UnicodeProperty.BINARY, "1.1"));
        add(new UnicodeSetProperty().set(Common.isLowercase).setMain("isLowercase", "lowercase", UnicodeProperty.BINARY, "1.1"));
        add(new UnicodeSetProperty().set(Common.isTitlecase).setMain("isTitlecase", "titlecase", UnicodeProperty.BINARY, "1.1"));
        add(new UnicodeSetProperty().set(Common.isCased).setMain("isCased", "cased", UnicodeProperty.BINARY, "1.1"));

        add(new CodepointTransformProperty(new Transform<Integer,String>() {
            public String transform(Integer source) {
                return Normalizer.normalize(source, Normalizer.NFC);
            }}, false).setMain("toNFC", "toNFC", UnicodeProperty.STRING, "1.1"));
        add(new CodepointTransformProperty(new Transform<Integer,String>() {
            public String transform(Integer source) {
                return Normalizer.normalize(source, Normalizer.NFD);
            }}, false).setMain("toNFD", "toNFD", UnicodeProperty.STRING, "1.1"));
        add(new CodepointTransformProperty(new Transform<Integer,String>() {
            public String transform(Integer source) {
                return Normalizer.normalize(source, Normalizer.NFKC);
            }}, false).setMain("toNFKC", "toNFKC", UnicodeProperty.STRING, "1.1"));
        add(new CodepointTransformProperty(new Transform<Integer,String>() {
            public String transform(Integer source) {
                return Normalizer.normalize(source, Normalizer.NFKD);
            }}, false).setMain("toNFKD", "toNFKD", UnicodeProperty.STRING, "1.1"));

        add(new StringTransformProperty(new StringTransform() {
            public String transform(String source) {
                return UCharacter.foldCase(source, true);
            }}, false).setMain("toCasefold", "toCasefold", UnicodeProperty.STRING, "1.1"));
        add(new StringTransformProperty(new StringTransform() {
            public String transform(String source) {
                return UCharacter.toLowerCase(ULocale.ROOT, source);
            }}, false).setMain("toLowerCase", "toLowerCase", UnicodeProperty.STRING, "1.1"));
        add(new StringTransformProperty(new StringTransform() {
            public String transform(String source) {
                return UCharacter.toUpperCase(ULocale.ROOT, source);
            }}, false).setMain("toUpperCase", "toUpperCase", UnicodeProperty.STRING, "1.1"));
        add(new StringTransformProperty(new StringTransform() {
            public String transform(String source) {
                return UCharacter.toTitleCase(ULocale.ROOT, source, null);
            }}, false).setMain("toTitleCase", "toTitleCase", UnicodeProperty.STRING, "1.1"));
        
        add(new CodepointTransformProperty(new Transform<Integer,String>() {
            public String transform(Integer source) {
                return UnicodeUtilities.getSubheader().getSubheader(source);
            }}, false).setMain("Subheader", "subhead", UnicodeProperty.STRING, "1.1"));

        add(new UnicodeSetProperty().set("[:^nfcqc=n:]").setMain("isNFC", "isNFC", UnicodeProperty.BINARY, "1.1"));
        add(new UnicodeSetProperty().set("[:^nfdqc=n:]").setMain("isNFD", "isNFD", UnicodeProperty.BINARY, "1.1"));
        add(new UnicodeSetProperty().set("[:^nfkcqc=n:]").setMain("isNFKC", "isNFKC", UnicodeProperty.BINARY, "1.1"));
        add(new UnicodeSetProperty().set("[:^nfkdqc=n:]").setMain("isNFKD", "isNFKD", UnicodeProperty.BINARY, "1.1"));
        add(new UnicodeSetProperty().set("[\\u0000-\\u007F]").setMain("ASCII", "ASCII", UnicodeProperty.BINARY, "1.1"));
        add(new UnicodeSetProperty().set("[\\u0000-\\U0010FFFF]").setMain("ANY", "ANY", UnicodeProperty.BINARY, "1.1"));

        String emojiSource = "[\\u00A9\\u00AE\\u2002\\u2003\\u2005\\u203C\\u2049\\u2122\\u2139\\u2194-\\u2199\\u21A9\\u21AA\\u231A\\u231B\\u23E9-\\u23EC\\u23F0\\u23F3\\u24C2\\u25AA\\u25AB\\u25B6\\u25C0\\u25FB-\\u25FE\\u2600\\u2601\\u260E\\u2611\\u2614\\u2615\\u261D\\u263A\\u2648-\\u2653\\u2660\\u2663\\u2665\\u2666\\u2668\\u267B\\u267F\\u2693\\u26A0\\u26A1\\u26AA\\u26AB\\u26BD\\u26BE\\u26C4\\u26C5\\u26CE\\u26D4\\u26EA\\u26F2\\u26F3\\u26F5\\u26FA\\u26FD\\u2702\\u2705\\u2708-\\u270C\\u270F\\u2712\\u2714\\u2716\\u2728\\u2733\\u2734\\u2744\\u2747\\u274C\\u274E\\u2753-\\u2755\\u2757\\u2764\\u2795-\\u2797\\u27A1\\u27B0\\u27BF\\u2934\\u2935\\u2B05-\\u2B07\\u2B1B\\u2B1C\\u2B50\\u2B55\\u3030\\u303D\\u3297\\u3299\\U0001F004\\U0001F0CF\\U0001F170\\U0001F171\\U0001F17E\\U0001F17F\\U0001F18E\\U0001F191-\\U0001F19A\\U0001F1E6-\\U0001F1FF\\U0001F201\\U0001F202\\U0001F21A\\U0001F22F\\U0001F232-\\U0001F23A\\U0001F250\\U0001F251\\U0001F300-\\U0001F30C\\U0001F30F\\U0001F311\\U0001F313-\\U0001F315\\U0001F319\\U0001F31B\\U0001F31F\\U0001F320\\U0001F330\\U0001F331\\U0001F334\\U0001F335\\U0001F337-\\U0001F34A\\U0001F34C-\\U0001F34F\\U0001F351-\\U0001F37B\\U0001F380-\\U0001F393\\U0001F3A0-\\U0001F3C4\\U0001F3C6\\U0001F3C8\\U0001F3CA\\U0001F3E0-\\U0001F3E3\\U0001F3E5-\\U0001F3F0\\U0001F40C-\\U0001F40E\\U0001F411\\U0001F412\\U0001F414\\U0001F417-\\U0001F429\\U0001F42B-\\U0001F43E\\U0001F440\\U0001F442-\\U0001F464\\U0001F466-\\U0001F46B\\U0001F46E-\\U0001F4AC\\U0001F4AE-\\U0001F4B5\\U0001F4B8-\\U0001F4EB\\U0001F4EE\\U0001F4F0-\\U0001F4F4\\U0001F4F6\\U0001F4F7\\U0001F4F9-\\U0001F4FC\\U0001F503\\U0001F50A-\\U0001F514\\U0001F516-\\U0001F52B\\U0001F52E-\\U0001F53D\\U0001F550-\\U0001F55B\\U0001F5FB-\\U0001F5FF\\U0001F601-\\U0001F606\\U0001F609-\\U0001F60D\\U0001F60F\\U0001F612-\\U0001F614\\U0001F616\\U0001F618\\U0001F61A\\U0001F61C-\\U0001F61E\\U0001F620-\\U0001F625\\U0001F628-\\U0001F62B\\U0001F62D\\U0001F630-\\U0001F633\\U0001F635\\U0001F637-\\U0001F640\\U0001F645-\\U0001F64F\\U0001F680\\U0001F683-\\U0001F685\\U0001F687\\U0001F689\\U0001F68C\\U0001F68F\\U0001F691-\\U0001F693\\U0001F695\\U0001F697\\U0001F699\\U0001F69A\\U0001F6A2\\U0001F6A4\\U0001F6A5\\U0001F6A7-\\U0001F6AD\\U0001F6B2\\U0001F6B6\\U0001F6B9-\\U0001F6BE\\U0001F6C0" +
        "\\u20E3" +
        "\\U0001F1E8\\U0001F1F3  \\U0001F1E9\\U0001F1EA  \\U0001F1EA\\U0001F1F8  \\U0001F1EB\\U0001F1F7  \\U0001F1EC\\U0001F1E7  \\U0001F1EE\\U0001F1F9  \\U0001F1EF\\U0001F1F5  \\U0001F1F0\\U0001F1F7  \\U0001F1F7\\U0001F1FA  \\U0001F1FA\\U0001F1F8 ]";    

        add(new UnicodeSetProperty().set(emojiSource).setMain("emoji", "emoji", UnicodeProperty.BINARY, "6.0"));

        add(new UnicodeSetProperty().set(new UnicodeSet("[\\u0000-\\uFFFF]")).setMain("bmp", "bmp", UnicodeProperty.BINARY, "6.0"));

        addCollationProperty();

        // set up the special script property
        UnicodeProperty scriptProp = base.getProperty("sc");
        UnicodeMap<String> specialMap = new UnicodeMap<String>();
        specialMap.putAll(scriptProp.getUnicodeMap());
        specialMap.putAll(ScriptTester.getScriptSpecialsNames());
        add(new UnicodeProperty.UnicodeMapProperty()
        .set(specialMap)
        .setMain("Script_Specials", "scs", UnicodeProperty.ENUMERATED, "1.1")
        .addValueAliases(ScriptTester.getScriptSpecialsAlternates(), false)
        );

        SortedMap<String, Charset> charsets = Charset.availableCharsets();
        if (DEBUG_CHARSET_NAMES) System.out.println(charsets.keySet());
        Matcher charsetMatcher = Pattern.compile("ISO-8859-\\d*|GB2312|Shift_JIS|GBK|Big5|EUC-KR").matcher("");
        for (String name : charsets.keySet()) {
            if (!charsetMatcher.reset(name).matches()) {
                continue;
            }
            Charset charset = charsets.get(name);
            EncodingProperty prop = new EncodingProperty(charset);
            prop.setType(UnicodeProperty.STRING);
            prop.setName("enc_" + name);

            EncodingPropertyBoolean isProp = new EncodingPropertyBoolean(charset);
            isProp.setType(UnicodeProperty.BINARY);
            isProp.setName("is_enc_" + name);

            for (String alias : charset.aliases()) {
                if (DEBUG_CHARSET_NAMES) System.out.println(name + " => " + alias);
                prop.addName("enc_" + alias);
                isProp.addName("isEnc_" + alias);
            }

            add(prop);
            add(isProp);
        }

        // exemplars
        //    String[] typeName = {"", "aux_"};
        //    for (ULocale locale : ULocale.getAvailableLocales()) {
        //        if (locale.getCountry().length() != 0 || locale.getVariant().length() != 0) {
        //            continue;
        //        }
        //        LocaleData localeData = LocaleData.getInstance(locale);
        //        for (int type = 0; type < LocaleData.ES_COUNT; ++type) {
        //            String name = "exemplars_" + typeName[type] + locale;
        //            UnicodeSet us = localeData.getExemplarSet(UnicodeSet.CASE, type).freeze();
        //            add(new UnicodeSetProperty().set(us).setMain(name, name, UnicodeProperty.BINARY, "1.1"));
        //        }
        //    }
    }

    private void addCollationProperty() {
        RuleBasedCollator c = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);

        UnicodeMap<String> collationMap = new UnicodeMap<String>();
        UnicodeMap<String> collationMap2 = new UnicodeMap<String>();
        RawCollationKey key = new RawCollationKey();
        StringBuilder builder = new StringBuilder();
        StringBuilder builder2 = new StringBuilder();
        UnicodeSet contractions = new UnicodeSet();
        UnicodeSet expansions = new UnicodeSet();
        try {
            c.getContractionsAndExpansions(contractions, expansions, true);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        UnicodeSet stuff = new UnicodeSet(ALL).addAll(contractions).addAll(expansions);
        for (String s : stuff) {
            c.getRawCollationKey(s, key);
            builder.setLength(0);
            builder2.setLength(0);
            int oneCount = 0;
            for (int i = 0; i < key.size; ++i) {
                byte b = key.bytes[i];
                if (b == 1) oneCount++;
                if (oneCount > 1) {
                    break;
                }
                String hex = com.ibm.icu.impl.Utility.hex(0xFF&b, 2);
                // look at both
                if (builder2.length() != 0) {
                    builder2.append(' ');
                }
                builder2.append(hex);
                // only look at primary values
                if (oneCount != 0) {
                    continue;
                }
                if (builder.length() != 0) {
                    builder.append(' ');
                }
                builder.append(hex);
            }
            collationMap.put(s, builder.toString());
            String builderString2 = builder2.toString();
            if (builderString2.endsWith("01")) {
                builderString2 = builderString2.substring(0,builderString2.length() - 2).trim();
            }
            collationMap2.put(s, builderString2);
        }
        add(new UnicodeProperty.UnicodeMapProperty().set(collationMap).setMain("uca", "uca1", UnicodeProperty.ENUMERATED, "1.1"));
        add(new UnicodeProperty.UnicodeMapProperty().set(collationMap2).setMain("uca2", "uca2", UnicodeProperty.ENUMERATED, "1.1"));
    }

    //  public UnicodeProperty getInternalProperty(String propertyAlias) {
    //    UnicodeProperty result = props.get(propertyAlias.toLowerCase(Locale.ENGLISH));
    //    if (result != null) {
    //      return result;
    //    }
    //    return base.getInternalProperty(propertyAlias);
    //  }
    //
    //  public List getInternalAvailablePropertyAliases(List result) {
    //    base.getInternalAvailablePropertyAliases(result);
    //    result.addAll(UnicodeUtilities.XPROPERTY_NAMES);
    //    return result;
    //  }

    private static class XUnicodeProperty extends UnicodeProperty {
        int fakeEnumValue;

        public XUnicodeProperty(int i) {
            setName(Common.XPROPERTY_NAMES.get(i - Common.XSTRING_START));
            fakeEnumValue = i;
            setType(UnicodeProperty.EXTENDED_STRING);
        }

        @Override
        protected List _getAvailableValues(List result) {
            addUnique("<string>", result);
            return result;
        }

        @Override
        protected List _getNameAliases(List result) {
            addUnique(getName(), result);
            return result;
        }

        @Override
        protected String _getValue(int codepoint) {
            return Common.getXStringPropertyValue(fakeEnumValue, codepoint, NameChoice.LONG);
        }

        @Override
        protected List _getValueAliases(String valueAlias, List result) {
            addUnique("<string>", result);
            return result;
        }

        @Override
        protected String _getVersion() {
            return VersionInfo.ICU_VERSION.toString();
        }

    }

    private static abstract class XEnumUnicodeProperty extends UnicodeProperty {
        List<String> values = new ArrayList();

        public XEnumUnicodeProperty(String name, Object[] values) {
            setName(name);
            for (Object item : values) {
                this.values.add(item.toString());
            }
            setType(UnicodeProperty.ENUMERATED);
        }

        @Override
        protected List _getAvailableValues(List result) {
            for (String s : values) addUnique(s, result);
            return result;
        }

        @Override
        protected List _getNameAliases(List result) {
            addUnique(getName(), result);
            return result;
        }

        @Override
        protected List _getValueAliases(String valueAlias, List result) {
            if (values.contains(valueAlias)) {
                addUnique(valueAlias, result);
            }
            return result;
        }

        @Override
        protected String _getVersion() {
            return VersionInfo.ICU_VERSION.toString();
        }

    }

    private static class IDNA2003 extends XEnumUnicodeProperty {
        public IDNA2003() {
            super("idna2003", IdnaType.values());
        }

        @Override
        protected String _getValue(int codepoint) {
            return Idna2003.SINGLETON.getType(codepoint).toString();
        }
        @Override
        protected List _getNameAliases(List result) {
            super._getNameAliases(result);
            result.add("idna");
            return result;
        }
    }

    private static class UTS46 extends XEnumUnicodeProperty {
        public UTS46() {
            super("uts46", IdnaType.values());
        }

        @Override
        protected String _getValue(int codepoint) {
            return Uts46.SINGLETON.getType(codepoint).toString();
        }
    }

    private static class IDNA2008 extends XEnumUnicodeProperty {
        public IDNA2008() {
            super("idna2008", Idna2008.Idna2008Type.values());
        }

        @Override
        protected String _getValue(int codepoint) {
            return Idna2008.getTypeMapping().get(codepoint).toString();
        }
    }

    private static class IDNA2008c extends XEnumUnicodeProperty {
        public IDNA2008c() {
            super("idna2008c", IdnaType.values());
        }

        @Override
        protected String _getValue(int codepoint) {
            return Idna2008.SINGLETON.getType(codepoint).toString();
        }
    }

    private static class Usage extends XEnumUnicodeProperty {
        enum UsageValues {common, historic, deprecated, liturgical, limited, symbol, punctuation, na;
        public static UsageValues getValue(int codepoint) {
            if (UnicodeProperty.SPECIALS.contains(codepoint)) return na;
            if (UnicodeUtilities.DEPRECATED.contains(codepoint)) return deprecated;
            if (UnicodeUtilities.LITURGICAL.contains(codepoint)) return liturgical;
            if (ScriptCategoriesCopy.ARCHAIC.contains(codepoint)) return historic;
            //if (UnicodeUtilities.LIM.contains(codepoint)) return archaic;
            if (UnicodeUtilities.COMMON_USE_SCRIPTS.contains(codepoint)) {
                if (UnicodeUtilities.SYMBOL.contains(codepoint)) return symbol;
                if (UnicodeUtilities.PUNCTUATION.contains(codepoint)) return punctuation;
                return common;
            }
            return limited;
        }
        }
        public Usage() {
            super("Usage", UsageValues.values());
            setType(UnicodeProperty.EXTENDED_ENUMERATED);
        }

        @Override
        protected String _getValue(int codepoint) {
            return UsageValues.getValue(codepoint).toString();
        }
    }

    static class HanType extends XEnumUnicodeProperty {
        enum HanTypeValues {na, Hans, Hant, Han}
        public HanType() {
            super("HanType", HanTypeValues.values());
            setType(UnicodeProperty.EXTENDED_ENUMERATED);
        }

        @Override
        protected String _getValue(int codepoint) {
            return Common.getValue(codepoint).toString();
        }
    }

    private static class StringTransformProperty extends SimpleProperty {
        Transform<String,String> transform;

        public StringTransformProperty(Transform<String,String> transform, boolean hasUniformUnassigned) {
            this.transform = transform;
            setUniformUnassigned(hasUniformUnassigned);
        }
        protected String _getValue(int codepoint) {
            return transform.transform(UTF16.valueOf(codepoint));
        }
    }

    private static class CodepointTransformProperty extends SimpleProperty {
        Transform<Integer,String> transform;

        public CodepointTransformProperty(Transform<Integer,String> transform, boolean hasUniformUnassigned) {
            this.transform = transform;
            setUniformUnassigned(hasUniformUnassigned);
        }
        protected String _getValue(int codepoint) {
            return transform.transform(codepoint);
        }
    }

    public static class EncodingProperty extends SimpleProperty {

        public static final String ERROR = "\uFFFD";

        CharEncoder encoder;
        byte[] temp = new byte[32]; // any more than this and we don't care

        EncodingProperty(Charset charset) {
            encoder = new CharEncoder(charset, false, false);
        }

        protected String _getValue(int codepoint) {
            int len = encoder.getValue(codepoint, temp, 0);
            if (len < 0) {
                return ERROR;
            }
            StringBuffer result = new StringBuffer();
            for (int i = 0; i < len; ++i) {
                if (result.length() > 0) {
                    result.append(' ');
                }
                result.append(hex(temp[i]));
            }
            return result.toString();
        }

        public boolean isDefault(int codepoint) {
            int len = encoder.getValue(codepoint, temp, 0);
            return len < 0;
        }

        private Object hex(byte b) {
            String result = Integer.toHexString(0xFF&b).toUpperCase(Locale.ENGLISH);
            return result.length() == 2 ? result : "0" + result;
        }
    }

    public static class EncodingPropertyBoolean extends SimpleProperty {

        CharEncoder encoder;

        EncodingPropertyBoolean(Charset charset) {
            encoder = new CharEncoder(charset, true, true);
        }

        protected String _getValue(int codepoint) {
            return (encoder.getValue(codepoint, null, 0) > 0) ? "Yes" : "No";
        }
    }


    public static class UnicodeSetProperty extends BaseProperty {
        protected UnicodeSet unicodeSet;
        private static final String[] YESNO_ARRAY = new String[]{"Yes", "No"};
        private static final List YESNO = Arrays.asList(YESNO_ARRAY);

        public UnicodeSetProperty set(UnicodeSet set) {
            unicodeSet = set;
            return this;
        }

        public UnicodeSetProperty set(String string) {
            // TODO Auto-generated method stub
            return set(new UnicodeSet(string).freeze());
        }

        protected String _getValue(int codepoint) {
            return YESNO_ARRAY[unicodeSet.contains(codepoint) ? 0 : 1];
        }

        protected List _getAvailableValues(List result) {
            return YESNO;
        }
    }

}
