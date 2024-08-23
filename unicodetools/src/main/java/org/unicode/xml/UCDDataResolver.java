package org.unicode.xml;

import com.ibm.icu.util.VersionInfo;
import java.util.*;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyParsingInfo;
import org.unicode.props.UcdLineParser;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class UCDDataResolver {

    private final IndexUnicodeProperties indexUnicodeProperties;
    private final String namespace;
    private final UCDXMLWriter writer;

    public UCDDataResolver(IndexUnicodeProperties iup, String namespace, UCDXMLWriter writer) {
        indexUnicodeProperties = iup;
        this.namespace = namespace;
        this.writer = writer;
    }

    public void buildSection(UcdSectionDetail.UcdSection ucdSection) throws SAXException {
        VersionInfo minVersion = ucdSection.getMinVersion();
        VersionInfo maxVersion = ucdSection.getMaxVersion();
        String tag = ucdSection.toString();
        String childTag = ucdSection.getChildTag();
        boolean parserWithRange = ucdSection.getParserWithRange();
        boolean parserWithMissing = ucdSection.getParserWithMissing();
        UcdSectionComponent[] ucdSectionComponents =
                ucdSection.getUcdSectionDetail().getUcdSectionComponents();

        if (isCompatibleVersion(minVersion, maxVersion)) {
            writer.startElement(tag);
            {
                for (UcdSectionComponent ucdSectionComponent : ucdSectionComponents) {
                    if (isCompatibleVersion(
                            ucdSectionComponent.getMinVersion(),
                            ucdSectionComponent.getMaxVersion())) {
                        final PropertyParsingInfo fileInfoEVS =
                                PropertyParsingInfo.getPropertyInfo(
                                        ucdSectionComponent.getUcdProperty());
                        String fullFilename =
                                fileInfoEVS.getFullFileName(indexUnicodeProperties.getUcdVersion());
                        UcdLineParser parser =
                                new UcdLineParser(FileUtilities.in("", fullFilename));
                        parser.withRange(parserWithRange);
                        parser.withMissing(parserWithMissing);
                        switch (ucdSection) {
                            case BLOCKS:
                                for (UcdLineParser.UcdLine line : parser) {
                                    if (!line.getOriginalLine().startsWith("#")) {
                                        AttributesImpl attributes =
                                                getBlockAttributes(namespace, line);
                                        writer.startElement(childTag, attributes);
                                        {
                                            writer.endElement(childTag);
                                        }
                                    }
                                }
                                break;
                            case NAMEDSEQUENCES:
                                HashMap<String, String> namedSequences = new HashMap<>();
                                for (UcdLineParser.UcdLine line : parser) {
                                    String[] parts = line.getParts();
                                    namedSequences.put(parts[0], parts[1]);
                                }
                                List<String> names = new ArrayList<>(namedSequences.keySet());
                                Collections.sort(names);
                                for (String name : names) {
                                    AttributesImpl attributes =
                                            getNamedSequenceAttributes(
                                                    namespace, name, namedSequences);
                                    writer.startElement(childTag, attributes);
                                    {
                                        writer.endElement(childTag);
                                    }
                                }
                                break;
                            case PROVISIONALNAMEDSEQUENCES:
                                HashMap<String, String> provisionalNamedSequences = new HashMap<>();
                                for (UcdLineParser.UcdLine line : parser) {
                                    String[] parts = line.getParts();
                                    provisionalNamedSequences.put(parts[0], parts[1]);
                                }
                                List<String> psNames =
                                        new ArrayList<>(provisionalNamedSequences.keySet());
                                Collections.sort(psNames);
                                for (String name : psNames) {
                                    AttributesImpl attributes =
                                            getNamedSequenceAttributes(
                                                    namespace, name, provisionalNamedSequences);
                                    writer.startElement(childTag, attributes);
                                    {
                                        writer.endElement(childTag);
                                    }
                                }
                                break;
                            default:
                                for (UcdLineParser.UcdLine line : parser) {
                                    AttributesImpl attributes =
                                            getAttributes(ucdSection, namespace, line);
                                    writer.startElement(childTag, attributes);
                                    {
                                        writer.endElement(childTag);
                                    }
                                }
                        }
                    }
                }
                writer.endElement(tag);
            }
        }
    }

    private AttributesImpl getAttributes(
            UcdSectionDetail.UcdSection ucdSection, String namespace, UcdLineParser.UcdLine line) {
        switch (ucdSection) {
            case CJKRADICALS:
                return getCJKRadicalAttributes(namespace, line);
            case DONOTEMIT:
                return getDoNotEmitAttributes(namespace, line);
            case EMOJISOURCES:
                return getEmojiSourceAttributes(namespace, line);
            case NORMALIZATIONCORRECTIONS:
                return getNCAttributes(namespace, line);
            case STANDARDIZEDVARIANTS:
                return getSVAttributes(namespace, line);
            default:
                throw new IllegalArgumentException(
                        "getAttributes failed on an unexpected UcdSection");
        }
    }

    private static AttributesImpl getBlockAttributes(String namespace, UcdLineParser.UcdLine line) {
        String[] parts = line.getParts();
        String[] range = parts[0].split("\\.\\.");
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(namespace, "first-cp", "first-cp", "CDATA", range[0]);
        attributes.addAttribute(namespace, "last-cp", "last-cp", "CDATA", range[1]);
        attributes.addAttribute(namespace, "name", "name", "CDATA", parts[1]);
        return attributes;
    }

    private static AttributesImpl getCJKRadicalAttributes(
            String namespace, UcdLineParser.UcdLine line) {
        String[] parts = line.getParts();
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(namespace, "number", "number", "CDATA", parts[0]);
        attributes.addAttribute(namespace, "radical", "radical", "CDATA", parts[1]);
        attributes.addAttribute(namespace, "ideograph", "ideograph", "CDATA", parts[2]);
        return attributes;
    }

    private static AttributesImpl getDoNotEmitAttributes(
            String namespace, UcdLineParser.UcdLine line) {
        String[] parts = line.getParts();
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(namespace, "of", "of", "CDATA", parts[0]);
        attributes.addAttribute(namespace, "use", "use", "CDATA", parts[1]);
        attributes.addAttribute(namespace, "because", "because", "CDATA", parts[2]);
        return attributes;
    }

    private static AttributesImpl getEmojiSourceAttributes(
            String namespace, UcdLineParser.UcdLine line) {
        String[] parts = line.getParts();
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(namespace, "unicode", "unicode", "CDATA", parts[0]);
        attributes.addAttribute(namespace, "docomo", "docomo", "CDATA", parts[1]);
        attributes.addAttribute(namespace, "kddi", "kddi", "CDATA", parts[2]);
        attributes.addAttribute(namespace, "softbank", "softbank", "CDATA", parts[3]);
        return attributes;
    }

    private static AttributesImpl getNamedSequenceAttributes(
            String namespace, String name, HashMap<String, String> namedSequences) {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(namespace, "name", "name", "CDATA", name);
        attributes.addAttribute(namespace, "cps", "cps", "CDATA", namedSequences.get(name));
        return attributes;
    }

    private static AttributesImpl getNCAttributes(String namespace, UcdLineParser.UcdLine line) {
        String[] parts = line.getParts();
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(namespace, "cp", "cp", "CDATA", parts[0]);
        attributes.addAttribute(namespace, "old", "old", "CDATA", parts[1]);
        attributes.addAttribute(namespace, "new", "new", "CDATA", parts[2]);
        attributes.addAttribute(namespace, "version", "version", "CDATA", parts[3]);
        return attributes;
    }

    private static AttributesImpl getSVAttributes(String namespace, UcdLineParser.UcdLine line) {
        String[] parts = line.getParts();
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(namespace, "cps", "cps", "CDATA", parts[0]);
        attributes.addAttribute(namespace, "desc", "desc", "CDATA", parts[1]);
        attributes.addAttribute(
                namespace, "when", "when", "CDATA", parts[2] != null ? parts[2] : "");
        return attributes;
    }

    private boolean isCompatibleVersion(VersionInfo minVersion, VersionInfo maxVersion) {
        return (indexUnicodeProperties.getUcdVersion().compareTo(minVersion) >= 0
                && (maxVersion == null
                        || indexUnicodeProperties.getUcdVersion().compareTo(maxVersion) <= 0));
    }
}
