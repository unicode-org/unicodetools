package org.unicode.props;

/**
 * A property status that can be derived from the data files defining the properties. Contrary to
 * PropertyStatus.java, this does not reflect distinctions such as Normative vs. Informative vs.
 * Contributory vs. Deprecated etc., as all of those are equal in the eyes of PropertyAliases.txt.
 * It does distinguish Provisional properties
 */
public enum DerivedPropertyStatus {
    /**
     * Properties that are part of the UCD and subject to UTC decisions. These are the ones in
     * PropertyAliases.txt. Their actual status may be Normative, Informative, or Contributory.
     */
    Approved,
    /**
     * Provisional properties. These are actual UCD properties, but not in PropertyAliases.txt, and
     * changes to them need not be approved by the UTC. They may be removed entirely from the UCD
     * (though they remain in the tools, as the tools have history).
     */
    Provisional,
    /**
     * Data in UCD files that do not specify character properties. Some of this data is exposed in
     * the form of properties in the tools, because all we have is a hammer.
     */
    UCDNonProperty,
    /**
     * Properties defined outside the UCD, e.g., in UTS #39 or UTS #51. These are explicitly
     * described as properties in these documents.
     */
    NonUCDProperty,
    /** Non-property data defined outside the UCD. */
    NonUCDNonProperty,
}
