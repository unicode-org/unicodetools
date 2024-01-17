package org.unicode.jsp;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.RawCollationKey;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.idna.Idna.IdnaType;
import org.unicode.idna.Idna2003;
import org.unicode.idna.Idna2008;
import org.unicode.idna.Uts46;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UnicodeProperty;
import org.unicode.props.UnicodeProperty.AliasAddAction;
import org.unicode.props.UnicodeProperty.BaseProperty;
import org.unicode.props.UnicodeProperty.SimpleProperty;
import org.unicode.text.UCD.VersionedProperty;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

public class XPropertyFactory extends UnicodeProperty.Factory {

    private static final Joiner JOIN_COMMAS = Joiner.on(",");
    private static final boolean DEBUG_MULTI = false;

    static final UnicodeSet ALL =
            new UnicodeSet("[[:^C:][:Cc:][:Cf:][:noncharactercodepoint:]]").freeze();

    static final class XPropertyFactoryHelper {
        XPropertyFactory factory = null;

        XPropertyFactoryHelper() {
            factory = new XPropertyFactory();
        }

        static XPropertyFactoryHelper INSTANCE = new XPropertyFactoryHelper();
    }

    public static XPropertyFactory make() {
        return XPropertyFactoryHelper.INSTANCE.factory;
    }

    public UnicodeProperty getProperty(String propertyAlias) {
        var versioned = VersionedProperty.forJSPs().set(propertyAlias);
        if (versioned != null) {
            return versioned.getProperty();
        }
        return super.getProperty(propertyAlias);
    }

    {
        IndexUnicodeProperties latest = IndexUnicodeProperties.make(Settings.latestVersion);
        // Contract the unassigned set as much as possible (based on latest rather than last), so
        // that dev/α/β property lookups are correct.
        UnicodeProperty.contractUNASSIGNED(
                latest.getProperty("General_Category").getSet("Unassigned"));
        IndexUnicodeProperties last = IndexUnicodeProperties.make(Settings.lastVersion);
        for (UcdProperty property : last.getAvailableUcdProperties()) {
            add(last.getProperty(property));
        }
        for (int i = Common.XSTRING_START; i < Common.XSTRING_LIMIT; ++i) {
            XUnicodeProperty property = new XUnicodeProperty(i);
            add(property);
        }

        add(new IDNA2003());
        add(new UTS46());
        add(new IDNA2008());
        add(new IDNA2008c());
        // add(new Usage());
        add(new HanType());
        add(
                new UnicodeProperty.UnicodeMapProperty()
                        .set(Idna2003.SINGLETON.mappings)
                        .setMain("toIdna2003", "toIdna2003", UnicodeProperty.STRING, "1.1"));
        add(
                new UnicodeProperty.UnicodeMapProperty()
                        .set(Uts46.SINGLETON.mappings)
                        .setMain("toUts46t", "toUts46t", UnicodeProperty.STRING, "1.1"));
        add(
                new UnicodeProperty.UnicodeMapProperty()
                        .set(Uts46.SINGLETON.getMappingsDisplay())
                        .setMain("toUts46n", "toUts46n", UnicodeProperty.STRING, "1.1"));

        add(
                new CodepointTransformProperty(
                                new Transform<Integer, String>() {
                                    @Override
                                    public String transform(Integer source) {
                                        return Normalizer.normalize(source, Normalizer.NFC);
                                    }
                                },
                                false)
                        .setMain("toNFC", "toNFC", UnicodeProperty.STRING, "1.1"));
        add(
                new CodepointTransformProperty(
                                new Transform<Integer, String>() {
                                    @Override
                                    public String transform(Integer source) {
                                        return Normalizer.normalize(source, Normalizer.NFD);
                                    }
                                },
                                false)
                        .setMain("toNFD", "toNFD", UnicodeProperty.STRING, "1.1"));
        add(
                new CodepointTransformProperty(
                                new Transform<Integer, String>() {
                                    @Override
                                    public String transform(Integer source) {
                                        return Normalizer.normalize(source, Normalizer.NFKC);
                                    }
                                },
                                false)
                        .setMain("toNFKC", "toNFKC", UnicodeProperty.STRING, "1.1"));
        add(
                new CodepointTransformProperty(
                                new Transform<Integer, String>() {
                                    @Override
                                    public String transform(Integer source) {
                                        return Normalizer.normalize(source, Normalizer.NFKD);
                                    }
                                },
                                false)
                        .setMain("toNFKD", "toNFKD", UnicodeProperty.STRING, "1.1"));

        add(
                new StringTransformProperty(
                                new StringTransform() {
                                    @Override
                                    public String transform(String source) {
                                        return UCharacter.foldCase(source, true);
                                    }
                                },
                                false)
                        .setMain("toCasefold", "toCF", UnicodeProperty.STRING, "1.1"));
        add(
                new StringTransformProperty(
                                new StringTransform() {
                                    @Override
                                    public String transform(String source) {
                                        return UCharacter.toLowerCase(ULocale.ROOT, source);
                                    }
                                },
                                false)
                        .setMain("toLowercase", "toLC", UnicodeProperty.STRING, "1.1"));
        add(
                new StringTransformProperty(
                                new StringTransform() {
                                    @Override
                                    public String transform(String source) {
                                        return UCharacter.toUpperCase(ULocale.ROOT, source);
                                    }
                                },
                                false)
                        .setMain("toUppercase", "toUC", UnicodeProperty.STRING, "1.1"));
        add(
                new StringTransformProperty(
                                new StringTransform() {
                                    @Override
                                    public String transform(String source) {
                                        return UCharacter.toTitleCase(ULocale.ROOT, source, null);
                                    }
                                },
                                false)
                        .setMain("toTitlecase", "toTC", UnicodeProperty.STRING, "1.1"));

        add(
                new StringTransformProperty(
                                new StringTransform() {
                                    @Override
                                    public String transform(String source) {
                                        String result = NFM.nfm.get(source);
                                        return result == null ? source : result;
                                    }
                                },
                                false)
                        .setMain("toNFM", "toNFM", UnicodeProperty.STRING, "1.1"));
        // add(new UnicodeProperty.UnicodeMapProperty().set(NFM.nfm).setMain("toNFM", "toNFM",
        // UnicodeProperty.STRING, "1.1"));
        add(
                new UnicodeSetProperty()
                        .set(NFM.nfm.getSet(null))
                        .setMain("isNFM", "isNFM", UnicodeProperty.BINARY, "1.1"));

        add(
                new CodepointTransformProperty(
                                new Transform<Integer, String>() {
                                    @Override
                                    public String transform(Integer source) {
                                        return UnicodeUtilities.getSubheader().getSubheader(source);
                                    }
                                },
                                false)
                        .setMain("subhead", "subhead", UnicodeProperty.STRING, "1.1"));

        add(
                new UnicodeSetProperty()
                        .set("[:^nfcqc=n:]")
                        .setMain("isNFC", "isNFC", UnicodeProperty.BINARY, "1.1"));
        add(
                new UnicodeSetProperty()
                        .set("[:^nfdqc=n:]")
                        .setMain("isNFD", "isNFD", UnicodeProperty.BINARY, "1.1"));
        add(
                new UnicodeSetProperty()
                        .set("[:^nfkcqc=n:]")
                        .setMain("isNFKC", "isNFKC", UnicodeProperty.BINARY, "1.1"));
        add(
                new UnicodeSetProperty()
                        .set("[:^nfkdqc=n:]")
                        .setMain("isNFKD", "isNFKD", UnicodeProperty.BINARY, "1.1"));
        add(
                new UnicodeSetProperty()
                        .set("[\\u0000-\\u007F]")
                        .setMain("ASCII", "ASCII", UnicodeProperty.BINARY, "1.1"));
        add(
                new UnicodeSetProperty()
                        .set("[\\u0000-\\U0010FFFF]")
                        .setMain("ANY", "ANY", UnicodeProperty.BINARY, "1.1"));

        add(
                new UnicodeSetProperty()
                        .set(new UnicodeSet("[\\u0000-\\uFFFF]"))
                        .setMain("bmp", "bmp", UnicodeProperty.BINARY, "6.0"));

        addCollationProperty();
        addExamplarProperty(LocaleData.ES_STANDARD, "exem", "exemplar");
        addExamplarProperty(LocaleData.ES_AUXILIARY, "exema", "exemplar_aux");
        addExamplarProperty(LocaleData.ES_PUNCTUATION, "exemp", "exemplar_punct");

        UnicodeSet Basic_Emoji =
                getProperty("Basic_Emoji").getSet("Yes", null); // TODO: was .getTrueSet();
        UnicodeSet Emoji_Keycap_Sequence =
                getProperty("RGI_Emoji_Keycap_Sequence")
                        .getSet("Yes", null); // TODO: was .getTrueSet();
        UnicodeSet RGI_Emoji_Modifier_Sequence =
                getProperty("RGI_Emoji_Modifier_Sequence")
                        .getSet("Yes", null); // TODO: was .getTrueSet();
        UnicodeSet RGI_Emoji_Tag_Sequence =
                getProperty("RGI_Emoji_Tag_Sequence")
                        .getSet("Yes", null); // TODO: was .getTrueSet();
        UnicodeSet RGI_Emoji_Flag_Sequence =
                getProperty("RGI_Emoji_Flag_Sequence")
                        .getSet("Yes", null); // TODO: was .getTrueSet();
        UnicodeSet RGI_Emoji_Zwj_Sequence =
                getProperty("RGI_Emoji_Zwj_Sequence")
                        .getSet("Yes", null); // TODO: was .getTrueSet();
        UnicodeSet RGI_Emoji =
                new UnicodeSet()
                        .add(Basic_Emoji)
                        .add(Emoji_Keycap_Sequence)
                        .add(RGI_Emoji_Modifier_Sequence)
                        .add(RGI_Emoji_Flag_Sequence)
                        .add(RGI_Emoji_Tag_Sequence)
                        .add(RGI_Emoji_Zwj_Sequence)
                        .freeze();
        add(
                new UnicodeSetProperty()
                        .set(RGI_Emoji)
                        .setMain("RGI_Emoji", "RGI_Emoji", UnicodeProperty.BINARY, "13.0"));
    }

    private void addExamplarProperty(
            int exemplarType, String propertyAbbreviation, String propertyName) {
        Multimap<Integer, String> data = TreeMultimap.create();
        Set<String> localeSet = new TreeSet<>();

        for (ULocale ulocale : ULocale.getAvailableLocales()) {
            if (!ulocale.getCountry().isEmpty() || !ulocale.getVariant().isEmpty()) {
                continue;
                // we want to skip cases where characters are in the parent locale, but there is no
                // ULocale parentLocale = ulocale.getParent();
            }
            UnicodeSet exemplarSet = LocaleData.getExemplarSet(ulocale, 0, exemplarType);
            if (!ulocale.getScript().isEmpty()) {
                // we can't find out the parent locale or defaultContent locale in ICU, so we hack
                // it
                String langLocale = ulocale.getLanguage();
                UnicodeSet langExemplarSet =
                        LocaleData.getExemplarSet(new ULocale(langLocale), 0, exemplarType);
                if (langExemplarSet.equals(exemplarSet)) {
                    continue;
                }
            }
            String locale = ulocale.toLanguageTag();
            localeSet.add(locale);
            for (UnicodeSetIterator it = new UnicodeSetIterator(exemplarSet); it.nextRange(); ) {
                if (it.codepoint == UnicodeSetIterator.IS_STRING) {
                    // flatten
                    int cp = 0;
                    for (int i = 0; i < it.string.length(); i += Character.charCount(cp)) {
                        cp = it.string.codePointAt(i);
                        data.put(cp, locale);
                    }
                } else {
                    for (int cp = it.codepoint; cp <= it.codepointEnd; ++cp) {
                        data.put(cp, locale);
                    }
                }
            }
        }

        // convert to UnicodeMap
        UnicodeMap<String> unicodeMap = new UnicodeMap<>();
        unicodeMap.putAll(0, 0x10FFFF, ""); // default is empty string
        for (Entry<Integer, Collection<String>> entry : data.asMap().entrySet()) {
            String value = JOIN_COMMAS.join(entry.getValue()).intern();
            unicodeMap.put(entry.getKey(), value);
        }
        if (DEBUG_MULTI) {
            System.out.println("\n" + propertyName);
            for (UnicodeMap.EntryRange<String> entry : unicodeMap.entryRanges()) {
                System.out.println(
                        Utility.hex(entry.codepoint)
                                + (entry.codepoint == entry.codepointEnd
                                        ? ""
                                        : "-" + Utility.hex(entry.codepointEnd))
                                + " ;\t"
                                + entry.value);
            }
        }

        // put locales into right format
        String[][] locales = new String[localeSet.size()][];
        int i = 0;
        for (String locale : localeSet) {
            locales[i++] = new String[] {locale, locale}; // abbreviations are the same
        }

        add(
                new UnicodeProperty.UnicodeMapProperty()
                        .set(unicodeMap)
                        .setMain(propertyName, propertyAbbreviation, UnicodeProperty.STRING, "1.1")
                        .addValueAliases(locales, AliasAddAction.ADD_MAIN_ALIAS)
                        .setMultivalued(true));
    }

    private void addCollationProperty() {
        RuleBasedCollator c = UnicodeSetUtilities.RAW_COLLATOR;
        // (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
        // c.setCaseLevel(true);

        UnicodeMap<String> collationMap0 = new UnicodeMap<String>();
        UnicodeMap<String> collationMap1 = new UnicodeMap<String>();
        UnicodeMap<String> collationMap2 = new UnicodeMap<String>();
        UnicodeMap<String> collationMap3 = new UnicodeMap<String>();
        RawCollationKey key = new RawCollationKey();
        StringBuilder[] builder = {
            new StringBuilder(), new StringBuilder(), new StringBuilder(), new StringBuilder()
        };
        UnicodeSet contractions = new UnicodeSet();
        UnicodeSet expansions = new UnicodeSet();
        try {
            c.getContractionsAndExpansions(contractions, expansions, true);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        UnicodeSet stuff =
                new UnicodeSet(ALL)
                        .addAll(contractions)
                        .addAll(expansions)
                        .removeAll(new UnicodeSet("[:unified_ideograph:]"));
        for (String s : stuff) {
            // c.getRawCollationKey(s, key);
            builder[0].setLength(0);
            builder[1].setLength(0);
            builder[2].setLength(0);
            builder[3].setLength(0);
            CollationElementIterator it = c.getCollationElementIterator(s);
            int primary = 0;
            int secondary = 0;
            int tertiary = 0;
            int caseLevel = 0;

            int nextCe = it.next();
            while (true) {
                // we need to peek
                int ce = nextCe;
                if (ce == CollationElementIterator.NULLORDER) {
                    break;
                }
                nextCe = it.next();
                if (ce == 0) {
                    continue;
                }
                primary = CollationElementIterator.primaryOrder(ce);
                secondary = CollationElementIterator.secondaryOrder(ce);
                tertiary = CollationElementIterator.tertiaryOrder(ce);
                caseLevel = tertiary & (0x80 + 0x40);
                tertiary ^= caseLevel;
                caseLevel |= 1; // fake 1 bit

                while (nextCe != CollationElementIterator.NULLORDER
                        && (nextCe & 0xC0) == 0xC0) { // Continuation!!
                    ce = nextCe;
                    nextCe = it.next();
                    primary = (primary << 16) | CollationElementIterator.primaryOrder(ce);
                    secondary = (secondary << 8) | CollationElementIterator.secondaryOrder(ce);
                    tertiary =
                            (tertiary << 8) | (CollationElementIterator.tertiaryOrder(ce) & 0x3F);
                }
                addBytes(builder[0], primary);
                addBytes(builder[1], secondary);
                addBytes(builder[2], caseLevel);
                addBytes(builder[3], tertiary);
            }
            collationMap0.put(s, builder[0].toString());
            collationMap1.put(s, builder[1].toString());
            collationMap2.put(s, builder[2].toString());
            collationMap3.put(s, builder[3].toString());
        }
        //        System.out.println(collationMap0.values().size());
        //        System.out.println(collationMap1.values().size());
        //        System.out.println(collationMap2.values().size());
        //        System.out.println(collationMap3.values().size());
        add(
                new UnicodeProperty.UnicodeMapProperty()
                        .set(collationMap0)
                        .setMain("uca", "uca1", UnicodeProperty.ENUMERATED, "1.1"));
        add(
                new UnicodeProperty.UnicodeMapProperty()
                        .set(collationMap1)
                        .setMain("uca2", "uca2", UnicodeProperty.ENUMERATED, "1.1"));
        add(
                new UnicodeProperty.UnicodeMapProperty()
                        .set(collationMap2)
                        .setMain("uca2.5", "uca2.5", UnicodeProperty.ENUMERATED, "1.1"));
        add(
                new UnicodeProperty.UnicodeMapProperty()
                        .set(collationMap3)
                        .setMain("uca3", "uca3", UnicodeProperty.ENUMERATED, "1.1"));
    }

    private void addBytes(StringBuilder builder, int bytes) {
        boolean first = true;
        for (int shift = 24; shift >= 0; shift -= 8) {
            int b = (bytes >>> shift) & 0xFF;
            if (b == 0) {
                continue;
            }
            String hex = com.ibm.icu.impl.Utility.hex(b, 2);
            if (first && builder.length() != 0) {
                builder.append(' ');
            }
            first = false;
            builder.append(hex);
        }
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

    private abstract static class XEnumUnicodeProperty extends UnicodeProperty {
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

    private static class IcuEnumProperty extends XEnumUnicodeProperty {
        final int propNum;

        public IcuEnumProperty(int propNum) {
            super(
                    UCharacter.getPropertyName(propNum, NameChoice.LONG),
                    getValues(propNum).toArray());
            this.propNum = propNum;
        }

        private static List<String> getValues(int propNum) {
            List<String> valueList = new ArrayList<String>();
            for (int i = UCharacter.getIntPropertyMinValue(propNum);
                    i <= UCharacter.getIntPropertyMaxValue(propNum);
                    ++i) {
                valueList.add(UCharacter.getPropertyValueName(propNum, i, NameChoice.LONG));
            }
            return valueList;
        }

        @Override
        protected String _getValue(int codepoint) {
            int propValue = UCharacter.getIntPropertyValue(codepoint, propNum);
            try {
                return UCharacter.getPropertyValueName(propNum, propValue, NameChoice.LONG);
            } catch (Exception e) {
                return "n/a";
            }
        }
    }

    //    private static class IcuBidiPairedBracket extends SimpleProperty {
    //        final int propNum;
    //        public IcuBidiPairedBracket() {
    //            setName(UCharacter.getPropertyName(UProperty.BIDI_PAIRED_BRACKET,
    // NameChoice.LONG));
    //            this.propNum = UProperty.BIDI_PAIRED_BRACKET;
    //        }
    //        @Override
    //        public List _getNameAliases(List result) {
    //            return Arrays.asList(UCharacter.getPropertyName(propNum, NameChoice.LONG),
    // UCharacter.getPropertyName(propNum, NameChoice.SHORT));
    //        }
    //
    //        @Override
    //        protected String _getValue(int codepoint) {
    //            return UTF16.valueOf(UCharacter.getBidiPairedBracket(codepoint));
    //        }
    //        @Override
    //        protected UnicodeMap _getUnicodeMap() {
    //            // TODO Auto-generated method stub
    //            return super._getUnicodeMap();
    //        }
    //    }

    //    private static class Usage extends XEnumUnicodeProperty {
    //        enum UsageValues {common, historic, deprecated, liturgical, limited, symbol,
    // punctuation, na;
    //        public static UsageValues getValue(int codepoint) {
    //            if (UnicodeProperty.SPECIALS.contains(codepoint)) return na;
    //            if (UnicodeUtilities.DEPRECATED.contains(codepoint)) return deprecated;
    //            if (UnicodeUtilities.LITURGICAL.contains(codepoint)) return liturgical;
    //            //if (ScriptCategoriesCopy.ARCHAIC.contains(codepoint)) return historic;
    //            //if (UnicodeUtilities.LIM.contains(codepoint)) return archaic;
    //            if (UnicodeUtilities.COMMON_USE_SCRIPTS.contains(codepoint)) {
    //                if (UnicodeUtilities.SYMBOL.contains(codepoint)) return symbol;
    //                if (UnicodeUtilities.PUNCTUATION.contains(codepoint)) return punctuation;
    //                return common;
    //            }
    //            return limited;
    //        }
    //        }
    //        public Usage() {
    //            super("Usage", UsageValues.values());
    //            setType(UnicodeProperty.EXTENDED_ENUMERATED);
    //        }
    //
    //        @Override
    //        protected String _getValue(int codepoint) {
    //            return UsageValues.getValue(codepoint).toString();
    //        }
    //    }

    static class HanType extends XEnumUnicodeProperty {
        enum HanTypeValues {
            na,
            Hans,
            Hant,
            Han
        }

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
        Transform<String, String> transform;

        public StringTransformProperty(
                Transform<String, String> transform, boolean hasUniformUnassigned) {
            this.transform = transform;
            setUniformUnassigned(hasUniformUnassigned);
        }

        @Override
        protected String _getValue(int codepoint) {
            return transform.transform(UTF16.valueOf(codepoint));
        }
    }

    private static class CodepointTransformProperty extends SimpleProperty {
        Transform<Integer, String> transform;

        public CodepointTransformProperty(
                Transform<Integer, String> transform, boolean hasUniformUnassigned) {
            this.transform = transform;
            setUniformUnassigned(hasUniformUnassigned);
        }

        @Override
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

        @Override
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

        @Override
        public boolean isDefault(int codepoint) {
            int len = encoder.getValue(codepoint, temp, 0);
            return len < 0;
        }

        private Object hex(byte b) {
            String result = Integer.toHexString(0xFF & b).toUpperCase(Locale.ENGLISH);
            return result.length() == 2 ? result : "0" + result;
        }
    }

    public static class EncodingPropertyBoolean extends SimpleProperty {

        CharEncoder encoder;

        EncodingPropertyBoolean(Charset charset) {
            encoder = new CharEncoder(charset, true, true);
        }

        @Override
        protected String _getValue(int codepoint) {
            return (encoder.getValue(codepoint, null, 0) > 0) ? "Yes" : "No";
        }
    }

    public static class UnicodeSetProperty extends BaseProperty {
        protected UnicodeSet unicodeSet;
        private static final String[] YESNO_ARRAY = new String[] {"Yes", "No"};
        private static final List YESNO = Arrays.asList(YESNO_ARRAY);

        public XPropertyFactory.UnicodeSetProperty set(UnicodeSet set) {
            unicodeSet = set;
            return this;
        }

        @Override
        protected UnicodeMap<String> _getUnicodeMap() {
            UnicodeMap<String> result = new UnicodeMap<String>();
            result.putAll(unicodeSet, "Yes");
            result.freeze();
            return result;
        }

        public XPropertyFactory.UnicodeSetProperty set(String string) {
            // TODO Auto-generated method stub
            return set(new UnicodeSet(string).freeze());
        }

        @Override
        protected String _getValue(int codepoint) {
            return YESNO_ARRAY[unicodeSet.contains(codepoint) ? 0 : 1];
        }

        @Override
        protected List _getAvailableValues(List result) {
            return YESNO;
        }
    }
}
