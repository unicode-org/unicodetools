package org.unicode.propstest;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.XMLFileReader;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Utility;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import com.ibm.icu.dev.util.UnicodeMap;

public class XMLProperties {

    enum XmlLeaf {
        // Leaf
        RESERVED, SURROGATE, NONCHARACTER, DESCRIPTION, BLOCK, NAMED_SEQUENCE,
        CJK_RADICAL, EMOJI_SOURCE, NORMALIZATION_CORRECTION, STANDARDIZED_VARIANT, NAME_ALIAS,
        // Both
        CHAR,
        // Nonleaf
        GROUP, UCD, REPERTOIRE, BLOCKS, NAMED_SEQUENCES, NORMALIZATION_CORRECTIONS, STANDARDIZED_VARIANTS, CJK_RADICALS, EMOJI_SOURCES
        ;
        static final XmlLeaf GREATEST_LEAF = NAME_ALIAS;
        static final XmlLeaf GREATEST_BOTH = CHAR;

        static XmlLeaf forString(String source) {
            try {
                return XmlLeaf.valueOf(source.toUpperCase().replace('-', '_'));
            } catch (final Exception e) {
                return null;
            }
        }
    }
    static class IntRange {
        int start;
        int end;
    }
    //    <description>Unicode 6.1.0</description>
    //    <repertoire>
    //  <char cp="0000" age="1.1" na="" JSN="" gc="Cc" ccc="0" dt="none" dm="#" nt="None" nv="" bc="BN" Bidi_M="N" bmg="" suc="#" slc="#" stc="#" uc="#" lc="#" tc="#" scf="#" cf="#" jt="U" jg="No_Joining_Group" ea="N" lb="CM" sc="Zyyy" Dash="N" WSpace="N" Hyphen="N" QMark="N" Radical="N" Ideo="N" UIdeo="N" IDSB="N" IDST="N" hst="NA" DI="N" ODI="N" Alpha="N" OAlpha="N" Upper="N" OUpper="N" Lower="N" OLower="N" Math="N" OMath="N" Hex="N" AHex="N" NChar="N" VS="N" Bidi_C="N" Join_C="N" Gr_Base="N" Gr_Ext="N" OGr_Ext="N" Gr_Link="N" STerm="N" Ext="N" Term="N" Dia="N" Dep="N" IDS="N" OIDS="N" XIDS="N" IDC="N" OIDC="N" XIDC="N" SD="N" LOE="N" Pat_WS="N" Pat_Syn="N" GCB="CN" WB="XX" SB="XX" CE="N" Comp_Ex="N" NFC_QC="Y" NFD_QC="Y" NFKC_QC="Y" NFKD_QC="Y" XO_NFC="N" XO_NFD="N" XO_NFKC="N" XO_NFKD="N" FC_NFKC="" CI="N" Cased="N" CWCF="N" CWCM="N" CWKCF="N" CWL="N" CWT="N" CWU="N" NFKC_CF="#" InSC="Other" InMC="NA" isc="" na1="NULL"/>
    Map<UcdProperty,UnicodeMap<String>> property2data= new EnumMap<UcdProperty,UnicodeMap<String>>(UcdProperty.class);
    {
        for (final UcdProperty prop : UcdProperty.values()) {
            property2data.put(prop, new UnicodeMap<String>());
        }
    }
    Set<String> leavesNotHandled = new LinkedHashSet<String>();
    Set<String> leavesNotRecognized = new LinkedHashSet<String>();


    public XMLProperties(String folder, boolean includeUnihan, int maxLines) {
        readFile(folder + "ucd.nounihan.grouped.xml", maxLines);
        if (includeUnihan) {
            readFile(folder + "ucd.unihan.grouped.xml", maxLines);
        }

        for (final UcdProperty prop : property2data.keySet()) {
            final UnicodeMap<String> map = property2data.get(prop);
            map.freeze();
        }
        System.out.println("Element names not recognized:\t" + leavesNotRecognized);
        System.out.println("Element names not handled:\t" + leavesNotHandled);
    }

    public void readFile(String systemID, int maxLines) {
        try {
            System.out.println("Reading: " + systemID);
            final FileInputStream fis = new FileInputStream(systemID);
            final XMLReader xmlReader = XMLFileReader.createXMLReader(false);
            xmlReader.setErrorHandler(new MyErrorHandler());
            xmlReader.setContentHandler(new MyContentHandler(maxLines));
            final InputSource is = new InputSource(fis);
            is.setSystemId(systemID.toString());
            xmlReader.parse(is);
            fis.close();
        } catch (final SAXParseException e) {
            System.out.println("\t" + "Can't read " + systemID);
            System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
        } catch (final SAXException e) {
            System.out.println("\t" + "Can't read " + systemID);
            System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
        } catch (final IOException e) {
            System.out.println("\t" + "Can't read " + systemID);
            System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
        } catch (final SkipException e) {
            System.out.println("Limiting XML file values");
        }
    }

    static class SkipException extends RuntimeException {}

    class MyContentHandler implements ContentHandler {
        IntRange cp = new IntRange();
        HashMap<String,String> attributes = new HashMap<String,String>();
        HashMap<String,String> groupAttributes = new HashMap<String,String>();
        int max;
        private final List<XmlLeaf> lastElements = new ArrayList<XmlLeaf>();

        public MyContentHandler(int max) {
            this.max = max;
        }
        @Override
        public void characters(char[] arg0, int arg1, int arg2) throws SAXException {
            final String chars = String.valueOf(arg0, arg1, arg2).trim();
            if (chars.trim().length() > 0 && lastElements.get(lastElements.size() - 1) != XmlLeaf.DESCRIPTION) {
                throw new IllegalArgumentException("Should have no element content");
            }
        }
        @Override
        public void endElement(String arg0, String arg1, String arg2) throws SAXException {
            try {
                if (lastElements.size() == 0) {
                    System.out.println("endElement: can't remove last element. Args: " + arg0 + ", " + arg1 + ", " + arg2);
                } else {
                    final XmlLeaf removed = lastElements.remove(lastElements.size() - 1);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("endElement: can't remove last element. Args: " + arg0 + ", " + arg1 + ", " + arg2, e);
            }
            //System.out.println("Removed:\t" + removed);
        }
        @Override
        public void endDocument() throws SAXException {}
        @Override
        public void endPrefixMapping(String arg0) throws SAXException {}
        @Override
        public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {}
        @Override
        public void processingInstruction(String arg0, String arg1) throws SAXException {}
        @Override
        public void setDocumentLocator(Locator arg0) {}
        @Override
        public void skippedEntity(String arg0) throws SAXException {}
        @Override
        public void startDocument() throws SAXException {}
        @Override
        public void startPrefixMapping(String arg0, String arg1) throws SAXException {}

        // <group age="1.1" na="" JSN="" gc="Cc" ccc="0" dt="none" dm="#" nt="None" nv="" bc="BN" Bidi_M="N" bmg="" suc="#" slc="#" stc="#" uc="#" lc="#" tc="#" scf="#" cf="#" jt="U" jg="No_Joining_Group" ea="N" lb="CM" sc="Zyyy" Dash="N" WSpace="N" Hyphen="N" QMark="N" Radical="N" Ideo="N" UIdeo="N" IDSB="N" IDST="N" hst="NA" DI="N" ODI="N" Alpha="N" OAlpha="N" Upper="N" OUpper="N" Lower="N" OLower="N" Math="N" OMath="N" Hex="N" AHex="N" NChar="N" VS="N" Bidi_C="N" Join_C="N" Gr_Base="N" Gr_Ext="N" OGr_Ext="N" Gr_Link="N" STerm="N" Ext="N" Term="N" Dia="N" Dep="N" IDS="N" OIDS="N" XIDS="N" IDC="N" OIDC="N" XIDC="N" SD="N" LOE="N" Pat_WS="N" Pat_Syn="N" GCB="CN" WB="XX" SB="XX" CE="N" Comp_Ex="N" NFC_QC="Y" NFD_QC="Y" NFKC_QC="Y" NFKD_QC="Y" XO_NFC="N" XO_NFD="N" XO_NFKC="N" XO_NFKD="N" FC_NFKC="" CI="N" Cased="N" CWCF="N" CWCM="N" CWKCF="N" CWL="N" CWT="N" CWU="N" NFKC_CF="#" InSC="Other" InMC="NA" isc="">
        // <char cp="0000" na1="NULL"/>
        //        <group age="1.1" JSN="" gc="Ll" ccc="0" dt="can" nt="None" nv="" bc="L" Bidi_M="N" bmg="" suc="#" slc="#" stc="#" uc="#" lc="#" tc="#" scf="#" cf="#" jt="U" jg="No_Joining_Group" ea="N" lb="AL" sc="Latn" Dash="N" WSpace="N" Hyphen="N" QMark="N" Radical="N" Ideo="N" UIdeo="N" IDSB="N" IDST="N" hst="NA" DI="N" ODI="N" Alpha="Y" OAlpha="N" Upper="N" OUpper="N" Lower="Y" OLower="N" Math="N" OMath="N" Hex="N" AHex="N" NChar="N" VS="N" Bidi_C="N" Join_C="N" Gr_Base="Y" Gr_Ext="N" OGr_Ext="N" Gr_Link="N" STerm="N" Ext="N" Term="N" Dia="N" Dep="N" IDS="Y" OIDS="N" XIDS="Y" IDC="Y" OIDC="N" XIDC="Y" SD="N" LOE="N" Pat_WS="N" Pat_Syn="N" GCB="XX" WB="LE" SB="LO" CE="N" Comp_Ex="N" NFC_QC="Y" NFD_QC="N" NFKC_QC="Y" NFKD_QC="N" XO_NFC="N" XO_NFD="Y" XO_NFKC="N" XO_NFKD="Y" FC_NFKC="" CI="N" Cased="Y" CWCF="N" CWCM="Y" CWKCF="N" CWL="N" CWT="Y" CWU="Y" NFKC_CF="#" InSC="Other" InMC="NA" isc="" na1="">
        //        <char cp="1E00" na="LATIN CAPITAL LETTER A WITH RING BELOW" gc="Lu" dm="0041 0325" slc="1E01" lc="1E01" scf="1E01" cf="1E01" Upper="Y" Lower="N" SB="UP" CWCF="Y" CWKCF="Y" CWL="Y" CWT="N" CWU="N" NFKC_CF="1E01"/>

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            try {
                final XmlLeaf xmlLeaf = XmlLeaf.forString(qName);
                if (xmlLeaf == null) {
                    leavesNotRecognized.add(qName);
                    return;
                }
                lastElements.add(xmlLeaf);
                //System.out.println("Added:\t" + lastElements);

                if (--max < 0) {
                    throw new SkipException();
                }
                if (xmlLeaf == XmlLeaf.GROUP) {
                    groupAttributes.clear();
                    addAttributes(atts, groupAttributes);
                    return;
                }
                attributes.clear();
                attributes.putAll(groupAttributes);
                addAttributes(atts, attributes);
                String cps;
                switch (xmlLeaf) {
                case CHAR: case RESERVED: case SURROGATE: case NONCHARACTER:
                    parseCp(attributes);
                    for (final Entry<String, String> entry : attributes.entrySet()) {
                        doAttributes(entry.getKey(), entry.getValue());
                    }
                    if (xmlLeaf == XmlLeaf.NONCHARACTER) {
                        property2data.get(UcdProperty.Noncharacter_Code_Point).putAll(cp.start, cp.end, "Yes");
                    }
                    break;
                case BLOCK:
                    //ucd/blocks/block[@first-cp="100000"][@last-cp="10FFFF"][@name="Supplementary Private Use Area-B"]
                    parseCp(attributes);
                    property2data.get(UcdProperty.Block).putAll(cp.start, cp.end, attributes.get("name"));
                    break;
                case NAMED_SEQUENCE:
                    //ucd/named-sequences/named-sequence[@name="BENGALI LETTER KHINYA"][@cps="0995 09CD 09B7"]
                    cps = Utility.fromHex(attributes.get("cps"));
                    property2data.get(UcdProperty.Named_Sequences).put(cps, attributes.get("name"));
                    break;
                case CJK_RADICAL:
                    //ucd/cjk-radicals/cjk-radical[@number="1"][@radical="2F00"][@ideograph="4E00"]
                    final String number = attributes.get("number");
                    setProp(Utility.fromHex(attributes.get("radical")), UcdProperty.CJK_Radical, number);
                    setProp(Utility.fromHex(attributes.get("ideograph")), UcdProperty.CJK_Radical, number);
                    break;
                case EMOJI_SOURCE:
                    //ucd/emoji-sources/emoji-source[@unicode="0023 20E3"][@docomo="F985"][@kddi="F489"][@softbank="F7B0"]
                    cps = Utility.fromHex(attributes.get("unicode"));
                    setProp(cps, UcdProperty.Emoji_DCM, attributes.get("docomo"));
                    setProp(cps, UcdProperty.Emoji_KDDI, attributes.get("kddi"));
                    setProp(cps, UcdProperty.Emoji_SB, attributes.get("softbank"));
                    break;
                case REPERTOIRE:
                case BLOCKS:
                case CJK_RADICALS:
                case EMOJI_SOURCES:
                case NAMED_SEQUENCES:
                case NORMALIZATION_CORRECTIONS:
                case STANDARDIZED_VARIANTS:
                case DESCRIPTION:
                    // non-informational nodes, skip
                    if (atts.getLength() != 0) {
                        throw new IllegalArgumentException("Has attributes");
                    }
                    break;
                case UCD:
                    if (atts.getLength() != 0) {
                        throw new IllegalArgumentException("Has wrong number of attributes: " + attributes.entrySet());
                    }
                    break;
                case NAME_ALIAS:
                    // <char cp="0000" na1="NULL">
                    // <name-alias alias="NUL" type="abbreviation"/>
                    appendProp(cp.start, UcdProperty.Name_Alias, attributes.get("alias"));
                    leavesNotHandled.add("name-alias type=");
                    break;
                case STANDARDIZED_VARIANT: {
                    //<standardized-variant cps="0023 FE0E" desc="text style" when=""/>
                    //ucd/standardized-variants/standardized-variant[@cps="1820 180B"][@desc="second form"][@when="isolate medial final"]
                    String desc = attributes.get("desc");
                    final String when = attributes.get("when");
                    if (!when.isEmpty()) {
                        desc = desc + "(" + when + ")";
                    }
                    cps = Utility.fromHex(attributes.get("cps"));
                    appendProp(cps, UcdProperty.Standardized_Variant, desc);
                    break;
                }
                case NORMALIZATION_CORRECTION:
                    //ucd/normalization-corrections/normalization-correction[@cp="F951"][@old="96FB"][@new="964B"][@version="3.2.0"
                default:
                    leavesNotHandled.add(qName);
                    break;
                case GROUP:
                    break; // handled above
                }
            } catch (final SkipException e) {
                throw e;
            } catch (final Exception e) {
                System.out.println("Exception: " + qName + "\t" + e.getClass().getName() + "\t" + e.getMessage());
            }
        }

        public void addAttributes(Attributes atts, Map<String, String> map) {
            for (int i = 0; i < atts.getLength(); ++i) {
                map.put(atts.getQName(i), atts.getValue(i));
            }
        }

        public void setProp(String cps, UcdProperty ucdProperty, String docomo) {
            if (docomo != null) {
                property2data.get(ucdProperty).put(cps, docomo);
            }
        }

        public void setProp(int cps, UcdProperty ucdProperty, String docomo) {
            if (docomo != null) {
                property2data.get(ucdProperty).put(cps, docomo);
            }
        }

        public void appendProp(int cps, UcdProperty ucdProperty, String docomo) {
            final UnicodeMap<String> unicodeMap = property2data.get(ucdProperty);
            final String former = unicodeMap.get(cps);
            unicodeMap.put(cps, former == null ? docomo : former + "; " + docomo);
        }

        public void appendProp(String cps, UcdProperty ucdProperty, String docomo) {
            final UnicodeMap<String> unicodeMap = property2data.get(ucdProperty);
            final String former = unicodeMap.get(cps);
            unicodeMap.put(cps, former == null ? docomo : former + "; " + docomo);
        }

        public void parseCp(HashMap<String, String> attributes2) {
            final String cpString = attributes2.get("cp");
            if (cpString != null) {
                cp.start = cp.end = Integer.parseInt(cpString, 16);
            } else {
                cp.start = Integer.parseInt(attributes2.get("first-cp"), 16);
                cp.end = Integer.parseInt(attributes2.get("last-cp"), 16);
            }
        }

        public UnicodeMap<String> doAttributes(String key, String value) {
            UcdProperty prop = UcdProperty.forString(key);
            if (prop == UcdProperty.Deprecated && cp.start > 0xE0000 && cp.start < 0xE00FF) {
                System.out.println(Utility.hex(cp.start) + "," + Utility.hex(cp.end) + "\t" + key + "\t" + value);
            }
            if (prop == null) {
                if (key.endsWith("cp")) {
                    if (key.equals("cp")
                            || key.equals("last-cp")
                            || key.equals("first-cp")
                            ) {
                        return null;
                    }
                } else if (key.equals("InSC")) {
                    prop = UcdProperty.Indic_Syllabic_Category;
                } else if (key.equals("InMC")) {
                    prop = UcdProperty.Indic_Syllabic_Category;
                }
                if (prop == null) {
                    return null;
                }
            }
            final UnicodeMap<String> data = property2data.get(prop);
            if (data == null) {
                System.out.println("can't get data for " + key);
                return null;
            }
            data.putAll(cp.start, cp.end, value.intern());
            return data;
        }

    }
    static class MyErrorHandler implements ErrorHandler {
        @Override
        public void error(SAXParseException exception) throws SAXException {
            //System.out.println("\nerror: " + XMLFileReader.showSAX(exception));
            throw exception;
        }
        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            //System.out.println("\nfatalError: " + XMLFileReader.showSAX(exception));
            throw exception;
        }
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            //System.out.println("\nwarning: " + XMLFileReader.showSAX(exception));
            throw exception;
        }
    }

    public UnicodeMap<String> getMap(UcdProperty prop) {
        return property2data.get(prop);
    }

    static String show(String ival) {
        if (ival == null) {
            return "null";
        }
        if (ival.isEmpty()) {
            return "<empty>";
        }
        return "[" + ival + "]";
    }

    //    private static final String NO_VALUE = IndexUnicodeProperties.DefaultValueType.NO_VALUE.toString();
    //    private static final String NAN = IndexUnicodeProperties.DefaultValueType.NaN.toString();

    static final boolean HACK_XML_DEFAULTS = false;

    public static String getXmlResolved(UcdProperty property, int codePoint, String propertyValue) {
        switch (property.getType()) {
        case Binary:
            if (HACK_XML_DEFAULTS) {
                if (propertyValue == null) {
                    propertyValue = "No";
                } else {
                    propertyValue = IndexUnicodeProperties.normalizeValue(property, propertyValue);
                }
                break;
            }
            //$FALL-THROUGH$
        case Enumerated: case Catalog:
            if (propertyValue != null) {
                propertyValue = IndexUnicodeProperties.normalizeValue(property, propertyValue);
            }
            break;
        case Numeric:
            //            if (HACK_XML_DEFAULTS) {
            //                if (propertyValue == null || propertyValue.isEmpty()) {
            //                    propertyValue = "NaN";
            //                }
            //            }
            break;
        case Miscellaneous:
            if (propertyValue != null) {
                switch (property) {
                case Script_Extensions:
                    propertyValue = IndexUnicodeProperties.normalizeValue(property, propertyValue);
                    break;
                case Name:
                    break;
                default:
                    propertyValue = propertyValue.replace("#", Utility.hex(codePoint));
                }
            }
            break;
        case String:
            if (propertyValue != null) {
                propertyValue = propertyValue.replace("#", Utility.hex(codePoint));
                propertyValue = Utility.fromHex(propertyValue);
            }
            break;
        }
        return propertyValue;
        //return propertyValue == null ? "<none>" : propertyValue;
    }
}
