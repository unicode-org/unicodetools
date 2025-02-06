package org.unicode.xml;

import com.ibm.icu.impl.UnicodeMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Utility;
import org.xml.sax.*;

public class XMLProperties {

    enum XmlLeaf {
        // Leaf
        BLOCK,
        BLOCKS,
        CHAR,
        CJK_RADICAL,
        CJK_RADICALS,
        DESCRIPTION,
        DO_NOT_EMIT,
        EMOJI_SOURCE,
        EMOJI_SOURCES,
        GROUP,
        INSTEAD,
        NAME_ALIAS,
        NAMED_SEQUENCE,
        NAMED_SEQUENCES,
        NONCHARACTER,
        NORMALIZATION_CORRECTION,
        NORMALIZATION_CORRECTIONS,
        PROVISIONAL_NAMED_SEQUENCES,
        REPERTOIRE,
        RESERVED,
        STANDARDIZED_VARIANT,
        STANDARDIZED_VARIANTS,
        SURROGATE,
        UCD;
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

    Map<UcdProperty, UnicodeMap<String>> property2data =
            new EnumMap<UcdProperty, UnicodeMap<String>>(UcdProperty.class);

    {
        for (final UcdProperty prop : UcdProperty.values()) {
            property2data.put(prop, new UnicodeMap<String>());
        }
    }

    Set<String> leavesNotHandled = new LinkedHashSet<String>();

    public XMLProperties(File ucdxmlFile) {
        readFile(ucdxmlFile);

        for (final UcdProperty prop : property2data.keySet()) {
            final UnicodeMap<String> map = property2data.get(prop);
            map.freeze();
        }
    }

    public void readFile(File ucdxmlFile) {
        try {
            System.out.println("Reading: " + ucdxmlFile.toString());
            final FileInputStream fis = new FileInputStream(ucdxmlFile);
            final XMLReader xmlReader = XMLFileReader.createXMLReader(false);
            xmlReader.setErrorHandler(new MyErrorHandler());
            xmlReader.setContentHandler(new MyContentHandler());
            final InputSource is = new InputSource(fis);
            is.setSystemId(ucdxmlFile.toString());
            xmlReader.parse(is);
            fis.close();
        } catch (final IOException | SAXException e) {
            System.out.println("\t" + "Can't read " + ucdxmlFile);
            System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
        }
    }

    class MyContentHandler implements ContentHandler {
        IntRange cp = new IntRange();
        HashMap<String, String> attributes = new HashMap<String, String>();
        HashMap<String, String> groupAttributes = new HashMap<String, String>();
        private final List<XmlLeaf> lastElements = new ArrayList<XmlLeaf>();

        public MyContentHandler() {}

        @Override
        public void characters(char[] arg0, int arg1, int arg2) throws SAXException {
            final String chars = String.valueOf(arg0, arg1, arg2).trim();
            if (!chars.trim().isEmpty()
                    && lastElements.get(lastElements.size() - 1) != XmlLeaf.DESCRIPTION) {
                throw new IllegalArgumentException("Should have no element content");
            }
        }

        @Override
        public void endElement(String arg0, String arg1, String arg2) throws SAXException {
            try {
                if (lastElements.isEmpty()) {
                    System.out.println(
                            "endElement: can't remove last element. Args: "
                                    + arg0
                                    + ", "
                                    + arg1
                                    + ", "
                                    + arg2);
                } else {
                    final XmlLeaf removed = lastElements.remove(lastElements.size() - 1);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException(
                        "endElement: can't remove last element. Args: "
                                + arg0
                                + ", "
                                + arg1
                                + ", "
                                + arg2,
                        e);
            }
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

        @Override
        public void startElement(
                String namespaceURI, String localName, String qName, Attributes atts) {
            try {
                final XmlLeaf xmlLeaf = XmlLeaf.forString(qName);
                if (xmlLeaf == null) {
                    throw new IllegalArgumentException(qName);
                }
                lastElements.add(xmlLeaf);
                // System.out.println("Added:\t" + lastElements);

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
                    case CHAR:
                    case RESERVED:
                    case SURROGATE:
                    case NONCHARACTER:
                        parseCp(attributes);
                        for (final Entry<String, String> entry : attributes.entrySet()) {
                            doAttributes(entry.getKey(), entry.getValue());
                        }
                        if (xmlLeaf == XmlLeaf.NONCHARACTER) {
                            property2data
                                    .get(UcdProperty.Noncharacter_Code_Point)
                                    .putAll(cp.start, cp.end, "Yes");
                        }
                        break;
                    case BLOCK:
                        parseCp(attributes);
                        property2data
                                .get(UcdProperty.Block)
                                .putAll(cp.start, cp.end, attributes.get("name"));
                        break;
                    case NAMED_SEQUENCE:
                        cps = Utility.fromHex(attributes.get("cps"));
                        property2data
                                .get(UcdProperty.Named_Sequences)
                                .put(cps, attributes.get("name"));
                        break;
                    case CJK_RADICAL:
                        final String number = attributes.get("number");
                        setProp(
                                Utility.fromHex(attributes.get("radical")),
                                UcdProperty.CJK_Radical,
                                number);
                        setProp(
                                Utility.fromHex(attributes.get("ideograph")),
                                UcdProperty.CJK_Radical,
                                number);
                        break;
                    case EMOJI_SOURCE:
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
                    case PROVISIONAL_NAMED_SEQUENCES:
                    case NORMALIZATION_CORRECTIONS:
                    case STANDARDIZED_VARIANTS:
                    case DESCRIPTION:
                    case DO_NOT_EMIT:
                        // non-informational nodes, skip
                        if (atts.getLength() != 0) {
                            throw new IllegalArgumentException("Has attributes");
                        }
                        break;
                    case UCD:
                        if (atts.getLength() != 0) {
                            throw new IllegalArgumentException(
                                    "Has wrong number of attributes: " + attributes.entrySet());
                        }
                        break;
                    case NAME_ALIAS:
                        final String alias =
                                attributes.get("alias") + "(" + attributes.get("type") + ")";
                        appendProp(cp.start, UcdProperty.Name_Alias, alias);
                        break;
                    case STANDARDIZED_VARIANT:
                        {
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
                        final String correction =
                                "old: "
                                        + attributes.get("old")
                                        + " new: "
                                        + attributes.get("new")
                                        + " version: "
                                        + attributes.get("version");
                        cps = Utility.fromHex(attributes.get("cp"));
                        appendProp(cps, UcdProperty.NC_Original, correction);
                        break;
                    case INSTEAD:
                        final String instead =
                                "use: "
                                        + attributes.get("use")
                                        + " because: "
                                        + attributes.get("because");
                        cps = attributes.get("of");
                        appendProp(cps, UcdProperty.Do_Not_Emit_Preferred, instead);
                        break;
                    case GROUP:
                        break; // handled above. Leaving case for clarity
                    default:
                        leavesNotHandled.add(qName);
                        break;
                }
            } catch (final Exception e) {
                System.out.println(
                        "Exception: "
                                + qName
                                + "\t"
                                + e.getClass().getName()
                                + "\t"
                                + e.getMessage());
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
            //            if (prop == UcdProperty.Deprecated && cp.start > 0xE0000 && cp.start <
            // 0xE00FF) {
            //                System.out.println(Utility.hex(cp.start) + "," + Utility.hex(cp.end) +
            // "\t" + key + "\t" + value);
            //            }
            if (prop == null) {
                if (key.endsWith("cp")) {
                    if (key.equals("cp") || key.equals("last-cp") || key.equals("first-cp")) {
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
            // System.out.println("\nerror: " + XMLFileReader.showSAX(exception));
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            // System.out.println("\nfatalError: " + XMLFileReader.showSAX(exception));
            throw exception;
        }

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            // System.out.println("\nwarning: " + XMLFileReader.showSAX(exception));
            throw exception;
        }
    }

    public UnicodeMap<String> getMap(UcdProperty prop) {
        return property2data.get(prop);
    }

    public Set<String> getLeavesNotHandled() {
        return leavesNotHandled;
    }

    static String show(String ival) {
        if (ival == null) {
            return "null";
        } else if (ival.isEmpty()) {
            return "<empty>";
        } else if (ival.codePointAt(0) < 0x20) {
            return "\\u{" + Utility.hex(ival, 4) + "}";
        }
        return "«" + ival + "»";
    }

    //    private static final String NO_VALUE =
    // IndexUnicodeProperties.DefaultValueType.NO_VALUE.toString();
    //    private static final String NAN = IndexUnicodeProperties.DefaultValueType.NaN.toString();

    static final boolean HACK_XML_DEFAULTS = false;

    public static String getXmlResolved(UcdProperty property, int codePoint, String propertyValue) {
        if (property == UcdProperty.Name) {
            int debug = 0;
        }
        switch (property.getType()) {
            case Binary:
                if (HACK_XML_DEFAULTS) {
                    if (propertyValue == null) {
                        propertyValue = "No";
                    } else {
                        propertyValue =
                                IndexUnicodeProperties.normalizeValue(property, propertyValue);
                    }
                    break;
                }
                // $FALL-THROUGH$
            case Enumerated:
            case Catalog:
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
                switch (property) {
                    case kOtherNumeric:
                    case kPrimaryNumeric:
                    case kAccountingNumeric:
                        if (propertyValue == null || propertyValue.isEmpty()) {
                            propertyValue = "NaN";
                        }
                        break;
                }
                break;
            case Miscellaneous:
                if (propertyValue != null) {
                    switch (property) {
                        case Script_Extensions:
                            propertyValue =
                                    IndexUnicodeProperties.normalizeValue(property, propertyValue);
                            break;
                            //                case Name:
                            //                    break;
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
            default:
                break;
        }
        return propertyValue;
        // return propertyValue == null ? "<none>" : propertyValue;
    }
}
