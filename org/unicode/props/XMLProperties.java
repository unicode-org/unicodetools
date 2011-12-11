package org.unicode.props;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.Timer;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.props.IndexUnicodeProperties.PropertyParsingInfo;
import org.unicode.text.utility.Utility;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.text.UnicodeSet;

public class XMLProperties {

    private static final boolean INCLUDE_UNIHAN = true; // false;
    private static final int MAX_LINE_COUNT = Integer.MAX_VALUE; // 4000; // Integer.MAX_VALUE;

    enum XmlLeaf {
        GROUP, CHAR, RESERVED, SURROGATE, NONCHARACTER, DESCRIPTION, BLOCK, NAMED_SEQUENCE, 
        CJK_RADICAL, EMOJI_SOURCE, NORMALIZATION_CORRECTION, STANDARDIZED_VARIANT, NAME_ALIAS
        ;
        static XmlLeaf forString(String source) {
            try {
                return XmlLeaf.valueOf(source.toUpperCase().replace('-', '_'));
            } catch (Exception e) {
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
        for (UcdProperty prop : UcdProperty.values()) {
            property2data.put(prop, new UnicodeMap<String>());
        }
    }
    Set<String> leavesNotHandled = new LinkedHashSet<String>();
    Set<String> leavesNotRecognized = new LinkedHashSet<String>();


    private XMLProperties(String folder) {
        readFile(folder + "ucd.nounihan.grouped.xml");
        if (INCLUDE_UNIHAN) {
            readFile(folder + "ucd.unihan.grouped.xml");
        }

        for (UcdProperty prop : property2data.keySet()) {
            UnicodeMap<String> map = property2data.get(prop);
            map.freeze();
        }
        System.out.println("Element names not recognized:\t" + leavesNotRecognized);
        System.out.println("Element names not handled:\t" + leavesNotHandled);
    }

    public void readFile(String systemID) {
        try {
            FileInputStream fis = new FileInputStream(systemID);
            XMLReader xmlReader = XMLFileReader.createXMLReader(false);
            xmlReader.setErrorHandler(new MyErrorHandler());
            xmlReader.setContentHandler(new MyContentHandler());
            InputSource is = new InputSource(fis);
            is.setSystemId(systemID.toString());
            xmlReader.parse(is);
            fis.close();
        } catch (SAXParseException e) {
            System.out.println("\t" + "Can't read " + systemID);
            System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
        } catch (SAXException e) {
            System.out.println("\t" + "Can't read " + systemID);
            System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
        } catch (IOException e) {
            System.out.println("\t" + "Can't read " + systemID);
            System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
        } catch (SkipException e) {
            System.out.println("Limiting XML file values");
        }
    }

    static class SkipException extends RuntimeException {}

    class MyContentHandler implements ContentHandler {
        IntRange cp = new IntRange();
        HashMap<String,String> attributes = new HashMap<String,String>();
        HashMap<String,String> groupAttributes = new HashMap<String,String>();
        int max = MAX_LINE_COUNT;

        public void characters(char[] arg0, int arg1, int arg2) throws SAXException {}
        public void endDocument() throws SAXException {}
        public void endElement(String arg0, String arg1, String arg2) throws SAXException {}
        public void endPrefixMapping(String arg0) throws SAXException {}
        public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {}
        public void processingInstruction(String arg0, String arg1) throws SAXException {}
        public void setDocumentLocator(Locator arg0) {}
        public void skippedEntity(String arg0) throws SAXException {}
        public void startDocument() throws SAXException {}
        public void startPrefixMapping(String arg0, String arg1) throws SAXException {}

        // <group age="1.1" na="" JSN="" gc="Cc" ccc="0" dt="none" dm="#" nt="None" nv="" bc="BN" Bidi_M="N" bmg="" suc="#" slc="#" stc="#" uc="#" lc="#" tc="#" scf="#" cf="#" jt="U" jg="No_Joining_Group" ea="N" lb="CM" sc="Zyyy" Dash="N" WSpace="N" Hyphen="N" QMark="N" Radical="N" Ideo="N" UIdeo="N" IDSB="N" IDST="N" hst="NA" DI="N" ODI="N" Alpha="N" OAlpha="N" Upper="N" OUpper="N" Lower="N" OLower="N" Math="N" OMath="N" Hex="N" AHex="N" NChar="N" VS="N" Bidi_C="N" Join_C="N" Gr_Base="N" Gr_Ext="N" OGr_Ext="N" Gr_Link="N" STerm="N" Ext="N" Term="N" Dia="N" Dep="N" IDS="N" OIDS="N" XIDS="N" IDC="N" OIDC="N" XIDC="N" SD="N" LOE="N" Pat_WS="N" Pat_Syn="N" GCB="CN" WB="XX" SB="XX" CE="N" Comp_Ex="N" NFC_QC="Y" NFD_QC="Y" NFKC_QC="Y" NFKD_QC="Y" XO_NFC="N" XO_NFD="N" XO_NFKC="N" XO_NFKD="N" FC_NFKC="" CI="N" Cased="N" CWCF="N" CWCM="N" CWKCF="N" CWL="N" CWT="N" CWU="N" NFKC_CF="#" InSC="Other" InMC="NA" isc="">
        // <char cp="0000" na1="NULL"/>
        //        <group age="1.1" JSN="" gc="Ll" ccc="0" dt="can" nt="None" nv="" bc="L" Bidi_M="N" bmg="" suc="#" slc="#" stc="#" uc="#" lc="#" tc="#" scf="#" cf="#" jt="U" jg="No_Joining_Group" ea="N" lb="AL" sc="Latn" Dash="N" WSpace="N" Hyphen="N" QMark="N" Radical="N" Ideo="N" UIdeo="N" IDSB="N" IDST="N" hst="NA" DI="N" ODI="N" Alpha="Y" OAlpha="N" Upper="N" OUpper="N" Lower="Y" OLower="N" Math="N" OMath="N" Hex="N" AHex="N" NChar="N" VS="N" Bidi_C="N" Join_C="N" Gr_Base="Y" Gr_Ext="N" OGr_Ext="N" Gr_Link="N" STerm="N" Ext="N" Term="N" Dia="N" Dep="N" IDS="Y" OIDS="N" XIDS="Y" IDC="Y" OIDC="N" XIDC="Y" SD="N" LOE="N" Pat_WS="N" Pat_Syn="N" GCB="XX" WB="LE" SB="LO" CE="N" Comp_Ex="N" NFC_QC="Y" NFD_QC="N" NFKC_QC="Y" NFKD_QC="N" XO_NFC="N" XO_NFD="Y" XO_NFKC="N" XO_NFKD="Y" FC_NFKC="" CI="N" Cased="Y" CWCF="N" CWCM="Y" CWKCF="N" CWL="N" CWT="Y" CWU="Y" NFKC_CF="#" InSC="Other" InMC="NA" isc="" na1="">
        //        <char cp="1E00" na="LATIN CAPITAL LETTER A WITH RING BELOW" gc="Lu" dm="0041 0325" slc="1E01" lc="1E01" scf="1E01" cf="1E01" Upper="Y" Lower="N" SB="UP" CWCF="Y" CWKCF="Y" CWL="Y" CWT="N" CWU="N" NFKC_CF="1E01"/>

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            try {
                XmlLeaf xmlLeaf = XmlLeaf.forString(qName);
                if (xmlLeaf == null) {
                    leavesNotRecognized.add(qName);
                    return;
                }
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
                    for (Entry<String, String> entry : attributes.entrySet()) {
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
                case NORMALIZATION_CORRECTION:
                    //ucd/normalization-corrections/normalization-correction[@cp="F951"][@old="96FB"][@new="964B"][@version="3.2.0"
                case STANDARDIZED_VARIANT:
                    //ucd/standardized-variants/standardized-variant[@cps="1820 180B"][@desc="second form"][@when="isolate medial final"]   
                default: 
                    leavesNotHandled.add(qName);
                    break;
                }
            } catch (SkipException e) {
                throw e;
            } catch (Exception e) {
                System.out.println("Error path/value " + qName);
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
            if (prop == null) {
                if (key.endsWith("cp")) {
                    if (key.equals("cp")
                            || key.equals("last-cp")
                            || key.equals("first-cp")
                    )
                        return null;
                } else if (key.equals("InSC")) prop = UcdProperty.Indic_Syllabic_Category;
                else if (key.equals("InMC")) prop = UcdProperty.Indic_Syllabic_Category;
                if (prop == null) return null;
            }
            UnicodeMap<String> data = property2data.get(prop);
            if (data == null) {
                System.out.println("can't get data for " + key);
                return null;
            }
            data.putAll(cp.start, cp.end, value.intern());
            return data;
        }

    }
    static class MyErrorHandler implements ErrorHandler {
        public void error(SAXParseException exception) throws SAXException {
            //System.out.println("\r\nerror: " + XMLFileReader.showSAX(exception));
            throw exception;
        }
        public void fatalError(SAXParseException exception) throws SAXException {
            //System.out.println("\r\nfatalError: " + XMLFileReader.showSAX(exception));
            throw exception;
        }
        public void warning(SAXParseException exception) throws SAXException {
            //System.out.println("\r\nwarning: " + XMLFileReader.showSAX(exception));
            throw exception;
        }
    }

    public UnicodeMap<String> getMap(UcdProperty prop) {
        return property2data.get(prop);
    }

    public static void main(String[] args) {
        Timer timer = new Timer();

        System.out.println("Loading Index Props");
        timer.start();
        IndexUnicodeProperties iup = IndexUnicodeProperties.make("6.1.0");
        timer.stop();
        System.out.println(timer);

        System.out.println("Loading XML Props");
        timer.start();
        XMLProperties props = new XMLProperties("/Users/markdavis/Documents/workspace/DATA/UCD/6.1.0-Update/");
        timer.stop();
        System.out.println(timer);

        UnicodeSet empty = new UnicodeSet();
        System.out.println("\nFormat:\nProperty\tcp\txml\tindexed");
        for (UcdProperty prop : UcdProperty.values()) {
            System.out.println("\nTESTING\t" + prop);
            UnicodeMap<String> xmap = props.getMap(prop);
            if (xmap.size() == 0) {
                System.out.println("*No XML Values");
                continue;
            }
            int errors = 0;
            empty.clear();
            for (int i = 0; i <= 0x10ffff; ++i) {
                String xval = getResolved(prop, i, xmap.get(i));
                String ival = iup.getResolvedValue(prop, i);
                if (!UnicodeProperty.equals(xval, ival)) {
                    if (xval == NO_VALUE) {
                        empty.add(i);
                    } else {
                        System.out.println(prop + "\t" + Utility.hex(i) + "\t" + show(xval) + "\t" + show(ival));
                        if (++errors > 10) {
                            break;
                        }
                    }
                }
            }
            if (errors == 0 && empty.size() == 0) {
                System.out.println("*OK*\t" + prop);
            } else if (empty.size() != 0) {
                System.out.println("*Missing Values:\t" + empty);
            }
        }
    }

    private static String show(String ival) {
        if (ival == null) return "null";
        if (ival.isEmpty()) return "<empty>";
        return "[" + ival + "]";
    }

    private static final String NO_VALUE = IndexUnicodeProperties.SpecialValue.NO_VALUE.toString();
    private static final String NAN = IndexUnicodeProperties.SpecialValue.NaN.toString();

    public static String getResolved(UcdProperty property, int codePoint, String propertyValue) {
        boolean dehex = false;
        switch (property.getType()) {
        case Binary: 
            if (propertyValue == null) {
                propertyValue = "No";
            } else {
                propertyValue = normalizeValue(property, propertyValue);
            }
            break;
        case Enumerated: case Catalog:
            if (propertyValue == null) { // error
                propertyValue = NO_VALUE;
            } else {
                propertyValue = normalizeValue(property, propertyValue);
            }
            break;
        case Numeric:
            if (propertyValue == null || propertyValue.isEmpty()) {
                propertyValue = NAN;
            }
            break;
        case Miscellaneous:
            if (propertyValue == null) {
                propertyValue = NO_VALUE;
            } else {
                propertyValue = propertyValue.replace("#", Utility.hex(codePoint));
            }
            break;
        case String:
            if (propertyValue == null) {
                propertyValue = NO_VALUE;
            } else {
                propertyValue = propertyValue.replace("#", Utility.hex(codePoint));
                propertyValue = Utility.fromHex(propertyValue);
            }
            break;
        }
        return propertyValue;
    }

    public static String normalizeValue(UcdProperty property, String propertyValue) {
        PropertyParsingInfo info = IndexUnicodeProperties.getPropertyInfo(property);
        propertyValue = info.normalizeEnum(propertyValue);
        return propertyValue;
    }
}
