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
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class XPropertyFactory extends UnicodeProperty.Factory {
  private static XPropertyFactory singleton = null;
  public static synchronized XPropertyFactory make() {
    if (singleton != null) return singleton;
    singleton = new XPropertyFactory();
    return singleton;
  }


  {
    ICUPropertyFactory base = ICUPropertyFactory.make();
    for (String propertyAlias : (List<String>)base.getInternalAvailablePropertyAliases(new ArrayList())) {
      add(base.getProperty(propertyAlias));
    }
    for (int i = UnicodeUtilities.XSTRING_START; i < UnicodeUtilities.XSTRING_LIMIT; ++i) {
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
    add(new StringTransformProperty(new UnicodeSetUtilities.NFKC_CF(), false).setMain("NFKC_Casefold", "NFKC_CF", UnicodeProperty.STRING, "1.1").addName("toNFKC_CF"));
    
    add(new UnicodeSetProperty().set(UnicodeUtilities.isCaseFolded).setMain("isCaseFolded", "caseFolded", UnicodeProperty.BINARY, "1.1"));
    add(new UnicodeSetProperty().set(UnicodeUtilities.isUppercase).setMain("isUppercase", "uppercase", UnicodeProperty.BINARY, "1.1"));
    add(new UnicodeSetProperty().set(UnicodeUtilities.isLowercase).setMain("isLowercase", "lowercase", UnicodeProperty.BINARY, "1.1"));
    add(new UnicodeSetProperty().set(UnicodeUtilities.isTitlecase).setMain("isTitlecase", "titlecase", UnicodeProperty.BINARY, "1.1"));
    add(new UnicodeSetProperty().set(UnicodeUtilities.isCased).setMain("isCased", "cased", UnicodeProperty.BINARY, "1.1"));
    
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
    Matcher charsetMatcher = Pattern.compile("ISO-8859-\\d*|GB2312|Shift_JIS|GBK|Big5").matcher("");
    for (String name : charsets.keySet()) {
      if (!charsetMatcher.reset(name).matches()) {
        continue;
      }
      Charset charset = charsets.get(name);
      EncodingProperty prop = new EncodingProperty(charset);
      prop.setType(UnicodeProperty.STRING);
      prop.setName("enc_" + name);
      for (String alias : charset.aliases()) {
        prop.addName("enc_" + alias);
      }
      add(prop);
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
      setName(UnicodeUtilities.XPROPERTY_NAMES.get(i - UnicodeUtilities.XSTRING_START));
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
      return UnicodeUtilities.getXStringPropertyValue(fakeEnumValue, codepoint, NameChoice.LONG);
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

  private static class HanType extends XEnumUnicodeProperty {
    enum HanTypeValues {na, simplified_only, traditional_only, both;
    public static HanTypeValues getValue(int codepoint) {
      if (UnicodeSetUtilities.simpOnly.contains(codepoint)) return simplified_only;
      if (UnicodeSetUtilities.tradOnly.contains(codepoint)) return traditional_only;
      if (UnicodeSetUtilities.bothSimpTrad.contains(codepoint)) return both;
      return na;
    }
    }
    public HanType() {
      super("HanType", HanTypeValues.values());
      setType(UnicodeProperty.EXTENDED_ENUMERATED);
    }

    @Override
    protected String _getValue(int codepoint) {
      return HanTypeValues.getValue(codepoint).toString();
    }
  }

  private static class StringTransformProperty extends SimpleProperty {
      StringTransform transform;

      public StringTransformProperty(StringTransform transform, boolean hasUniformUnassigned) {
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
