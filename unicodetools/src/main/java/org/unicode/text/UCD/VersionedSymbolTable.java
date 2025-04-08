package org.unicode.text.UCD;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UnicodeProperty;
import org.unicode.props.UnicodePropertySymbolTable;
import org.unicode.text.utility.Settings;

/**
 * This class implements the semantics of property-query as defined in the UnicodeSet specification.
 */
public class VersionedSymbolTable extends UnicodeSet.XSymbolTable {
    private VersionedSymbolTable() {}

    public static VersionedSymbolTable forReview() {
        var result = new VersionedSymbolTable();
        result.requireSuffixForLatest = true;
        result.implicitVersion = Settings.LAST_VERSION_INFO;
        result.previousVersion = Settings.LAST2_VERSION_INFO;
        return result;
    }

    public static VersionedSymbolTable forDevelopment() {
        var result = new VersionedSymbolTable();
        result.requireSuffixForLatest = false;
        result.implicitVersion = Settings.LATEST_VERSION_INFO;
        result.previousVersion = Settings.LAST_VERSION_INFO;
        return result;
    }

    public static VersionedSymbolTable frozenAt(VersionInfo version) {
        var result = new VersionedSymbolTable();
        result.requireSuffixForLatest = false;
        result.implicitVersion = version;
        // TODO(egg): We should have a programmatic “previous version of Unicode”.
        // For now this ensures we fail.
        result.previousVersion = VersionInfo.getInstance(0);
        return result;
    }

    public VersionedSymbolTable setUnversionedExtensions(UnicodeProperty.Factory factory) {
        unversionedExtensions = factory;
        return this;
    }

    @Override
    public boolean applyPropertyAlias(String beforeEquals, String afterEquals, UnicodeSet result) {
        result.clear();
        String leftHandSide = beforeEquals;
        String propertyPredicate = afterEquals;
        boolean interiorlyNegated = false;
        int posNotEqual = beforeEquals.indexOf('≠');
        // TODO(egg): We cannot distinguish \p{X=} from \p{X} in this API, both give us an empty
        // string as afterEquals.  This is an @internal API, so we could change it to pass null in
        // the unary case.
        if (posNotEqual >= 0) {
            propertyPredicate =
                    afterEquals.isEmpty()
                            ? beforeEquals.substring(posNotEqual + 1)
                            : beforeEquals.substring(posNotEqual + 1) + "=" + afterEquals;
            leftHandSide = beforeEquals.substring(0, posNotEqual);
            interiorlyNegated = true;
        }
        if (interiorlyNegated) {
            final var complement = getNonNegatedPropertyQuerySet(leftHandSide, propertyPredicate);
            result.addAll(complement.complement().removeAllStrings());
        } else {
            result.addAll(getNonNegatedPropertyQuerySet(leftHandSide, propertyPredicate));
        }
        return true;
    }

    private UnicodeSet getNonNegatedPropertyQuerySet(
            String leftHandSide, String propertyPredicate) {
        final var mutableLeftHandSide = new StringBuilder(leftHandSide);
        final var queriedVersion = parseVersionQualifier(mutableLeftHandSide);
        final String unqualifiedLeftHandSide = mutableLeftHandSide.toString();
        final var deducedQueriedVersion = queriedVersion == null ? implicitVersion : queriedVersion;

        final var queriedProperties = IndexUnicodeProperties.make(deducedQueriedVersion);

        if (propertyPredicate.isEmpty()) {
            return computeUnaryQuery(queriedProperties, unqualifiedLeftHandSide);
        } else {
            return computeBinaryQuery(
                    queriedProperties, unqualifiedLeftHandSide, propertyPredicate);
        }
    }

    private UnicodeSet computeUnaryQuery(
            IndexUnicodeProperties queriedProperties, String unqualifiedQuery) {
        // Either unary-property-query, or binary-property-query with an empty property-value.
        final var script = queriedProperties.getProperty(UcdProperty.Script);
        final var generalCategory = queriedProperties.getProperty(UcdProperty.General_Category);
        if (script.isValidValue(unqualifiedQuery)) {
            return script.getSet(unqualifiedQuery);
        }
        if (generalCategory.isValidValue(unqualifiedQuery)) {
            return getGeneralCategorySet(queriedProperties, unqualifiedQuery);
        }
        UnicodeProperty queriedProperty = queriedProperties.getProperty(unqualifiedQuery);
        if (queriedProperty == null && unversionedExtensions != null) {
            queriedProperty = unversionedExtensions.getProperty(unqualifiedQuery);
        }
        if (queriedProperty == null) {
            throw new IllegalArgumentException(
                    "Invalid unary-query-expression; could not find property " + unqualifiedQuery);
        }
        if (!queriedProperty.isType(UnicodeProperty.BINARY_MASK)) {
            // TODO(egg): Remove when we can tell this is a unary query.
            if (queriedProperty.isType(UnicodeProperty.STRING_OR_MISC_MASK)) {
                return queriedProperty.getSet("");
            }
            throw new IllegalArgumentException(
                    "Invalid unary-query-expression for non-binary property "
                            + queriedProperty.getName());
        }
        return queriedProperty.getSet(UcdPropertyValues.Binary.Yes);
    }

    private UnicodeSet computeBinaryQuery(
            IndexUnicodeProperties queriedProperties,
            String unqualifiedLeftHandSide,
            String propertyPredicate) {
        // We have a binary-property-query.
        UnicodeProperty queriedProperty = queriedProperties.getProperty(unqualifiedLeftHandSide);
        if (queriedProperty == null && unversionedExtensions != null) {
            queriedProperty = unversionedExtensions.getProperty(unqualifiedLeftHandSide);
        }
        if (queriedProperty == null) {
            throw new IllegalArgumentException(
                    "Invalid binary-query-expression; could not find property "
                            + unqualifiedLeftHandSide);
        }
        final boolean isAge = queriedProperty.getName().equals("Age");
        final boolean isName = queriedProperty.getName().equals("Name");
        final boolean isPropertyComparison =
                propertyPredicate.startsWith("@") && propertyPredicate.endsWith("@");
        final boolean isRegularExpressionMatch =
                propertyPredicate.startsWith("/") && propertyPredicate.endsWith("/");
        if (isPropertyComparison) {
            if (isAge) {
                throw new IllegalArgumentException(
                        "Invalid binary-query-expression with property-comparison for Age");
            }
            final var unqualifiedRightHandSide =
                    new StringBuilder(
                            propertyPredicate.substring(1, propertyPredicate.length() - 1));
            final var comparisonVersion = parseVersionQualifier(unqualifiedRightHandSide);
            if (UnicodeProperty.equalNames(unqualifiedRightHandSide.toString(), "code point")) {
                if (comparisonVersion != null) {
                    throw new IllegalArgumentException(
                            "Invalid binary-query-expression with comparison version on identity query");
                }
                if (!queriedProperty.isType(UnicodeProperty.STRING_MASK)) {
                    throw new IllegalArgumentException(
                            "Invalid binary-query-expression with identity query for "
                                    + queriedProperty.getTypeName()
                                    + " property");
                }
                return getIdentitySet(queriedProperty);
            } else if (UnicodeProperty.equalNames(unqualifiedRightHandSide.toString(), "none")) {
                if (comparisonVersion != null) {
                    throw new IllegalArgumentException(
                            "Invalid binary-query-expression with comparison version on null query");
                }
                if (!queriedProperty.isType(UnicodeProperty.STRING_OR_MISC_MASK)) {
                    throw new IllegalArgumentException(
                            "Invalid binary-query-expression with null query for "
                                    + queriedProperty.getTypeName()
                                    + " property");
                }
                return queriedProperty.getSet((String) null);
            } else {
                UnicodeProperty comparisonProperty =
                        IndexUnicodeProperties.make(
                                        comparisonVersion == null
                                                ? implicitVersion
                                                : comparisonVersion)
                                .getProperty(unqualifiedRightHandSide.toString());
                if (comparisonProperty == null && unversionedExtensions != null) {
                    comparisonProperty =
                            unversionedExtensions.getProperty(unqualifiedRightHandSide.toString());
                }
                if (comparisonProperty == null) {
                    throw new IllegalArgumentException(
                            "Invalid binary-query-expression; could not find comparison property "
                                    + unqualifiedRightHandSide);
                }
                return compareProperties(queriedProperty, comparisonProperty);
            }
        } else if (isRegularExpressionMatch) {
            if (isAge) {
                throw new IllegalArgumentException(
                        "Invalid binary-query-expression with regular-expression-match for Age");
            }
            return queriedProperty.getSet(
                    new UnicodeProperty.RegexMatcher()
                            .set(propertyPredicate.substring(1, propertyPredicate.length() - 1)));
        } else {
            String propertyValue = propertyPredicate;
            // Validation.  For Name, validation entails computing the query, so we return here.
            if (isName) {
                var result = queriedProperty.getSet(propertyValue);
                if (result.isEmpty()) {
                    result =
                            queriedProperties
                                    .getProperty(UcdProperty.Name_Alias)
                                    .getSet(propertyValue);
                }
                if (result.isEmpty()) {
                    throw new IllegalArgumentException(
                            "No character name nor name alias matches " + propertyValue);
                }
                return result;
            } else if (queriedProperty.getName().equals("Name_Alias")) {
                var result = queriedProperty.getSet(propertyValue);
                if (result.isEmpty()) {
                    throw new IllegalArgumentException("No name alias matches " + propertyValue);
                }
                return result;
            } else if (queriedProperty.isType(UnicodeProperty.NUMERIC_MASK)) {
                if (UnicodeProperty.equalNames(propertyValue, "NaN")
                        || !RATIONAL_PATTERN.matcher(propertyValue).matches()) {
                    throw new IllegalArgumentException(
                            "Invalid value '"
                                    + propertyValue
                                    + "' for numeric property "
                                    + queriedProperty.getName());
                }
            } else if (queriedProperty.isType(
                    UnicodeProperty.BINARY_OR_ENUMERATED_OR_CATALOG_MASK)) {
                if (!queriedProperty.isValidValue(propertyValue)) {
                    throw new IllegalArgumentException(
                            "The value '"
                                    + propertyValue
                                    + "' is illegal. Values for "
                                    + queriedProperty.getName()
                                    + " must be in "
                                    + queriedProperty.getAvailableValues()
                                    + " or in "
                                    + queriedProperty.getValueAliases());
                }
            } else {
                // TODO(egg): Check for unescaped :, @, =, etc. and unescape.
            }
            if (isAge) {
                return queriedProperty.getSet(
                        new UnicodePropertySymbolTable.ComparisonMatcher<VersionInfo>(
                                UnicodePropertySymbolTable.parseVersionInfoOrMax(propertyValue),
                                UnicodePropertySymbolTable.Relation.geq,
                                Comparator.nullsFirst(Comparator.naturalOrder()),
                                UnicodePropertySymbolTable::parseVersionInfoOrMax));
            }
            if (queriedProperty.getName().equals("General_Category")) {
                return getGeneralCategorySet(queriedProperties, propertyValue);
            }
            return queriedProperty.getSet(propertyValue);
        }
    }

    /**
     * Parses a string prefixed with an optional-version-qualifier. If there is a version-qualifier,
     * returns the corresponding VersionInfo and removes the prefix from the given StringBuilder.
     * Otherwise returns null.
     */
    private VersionInfo parseVersionQualifier(StringBuilder qualified) {
        int posColon = qualified.indexOf(":", 0);
        if (posColon < 0) {
            return null;
        }
        final String versionQualifier = qualified.substring(0, posColon + 1);
        qualified.delete(0, posColon + 1);
        if (versionQualifier.equals("U-1")) {
            return previousVersion;
        } else {
            switch (versionQualifier.charAt(0)) {
                case 'R':
                    // Extension: we allow a version-qualifier starting with R for retroactive
                    // properties, that is, property derivations applied before the property
                    // existed.
                    // TODO(egg): Actually support that.
                case 'U':
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Invalid version-qualifier " + versionQualifier);
            }
            String versionNumber = versionQualifier.substring(1, posColon);
            if (versionNumber.endsWith("dev")) {
                versionNumber = versionNumber.substring(0, versionNumber.length() - 3);
                if (!versionNumber.isEmpty()
                        && VersionInfo.getInstance(versionNumber) != Settings.LATEST_VERSION_INFO) {
                    throw new IllegalArgumentException(
                            "Invalid version-qualifier "
                                    + versionQualifier
                                    + " with version-suffix dev: the current dev version is "
                                    + Settings.latestVersion);
                }
                return Settings.LATEST_VERSION_INFO;
            } else if (versionNumber.endsWith("α") || versionNumber.endsWith("β")) {
                final String versionSuffix = versionNumber.substring(versionNumber.length() - 1);
                versionNumber = versionNumber.substring(0, versionNumber.length() - 1);
                if (versionSuffix != Settings.latestVersionPhase.toString()) {
                    throw new IllegalArgumentException(
                            "Invalid version-qualifier "
                                    + versionQualifier
                                    + " with version-suffix "
                                    + versionSuffix
                                    + ": the current stage is "
                                    + Settings.latestVersionPhase);
                }
                if (!versionNumber.isEmpty()
                        && VersionInfo.getInstance(versionNumber) != Settings.LATEST_VERSION_INFO) {
                    throw new IllegalArgumentException(
                            "Invalid version-qualifier "
                                    + versionQualifier
                                    + " with version-suffix "
                                    + versionNumber
                                    + ": the current "
                                    + versionSuffix
                                    + " version is "
                                    + Settings.latestVersion);
                }
                return Settings.LATEST_VERSION_INFO;
            } else {
                var result = VersionInfo.getInstance(versionNumber);
                if (result == Settings.LATEST_VERSION_INFO && requireSuffixForLatest) {
                    throw new IllegalArgumentException(
                            "Invalid version-qualifier "
                                    + versionQualifier
                                    + " version-suffix "
                                    + Settings.latestVersionPhase
                                    + " required for unpublished version");
                }
                return result;
            }
        }
    }

    private static Map<UcdPropertyValues.General_Category_Values, Set<String>>
            COARSE_GENERAL_CATEGORIES =
                    Map.of(
                            UcdPropertyValues.General_Category_Values.Other,
                            Set.of("Cc", "Cf", "Cn", "Co", "Cs"),
                            UcdPropertyValues.General_Category_Values.Letter,
                            Set.of("Ll", "Lm", "Lo", "Lt", "Lu"),
                            UcdPropertyValues.General_Category_Values.Cased_Letter,
                            Set.of("Ll", "Lt", "Lu"),
                            UcdPropertyValues.General_Category_Values.Mark,
                            Set.of("Mc", "Me", "Mn"),
                            UcdPropertyValues.General_Category_Values.Number,
                            Set.of("Nd", "Nl", "No"),
                            UcdPropertyValues.General_Category_Values.Punctuation,
                            Set.of("Pc", "Pd", "Pe", "Pf", "Pi", "Po", "Ps"),
                            UcdPropertyValues.General_Category_Values.Symbol,
                            Set.of("Sc", "Sk", "Sm", "So"),
                            UcdPropertyValues.General_Category_Values.Separator,
                            Set.of("Zl", "Zp", "Zs"));

    /**
     * Similar to iup.getProperty(UcdProperty.General_Category).getSet(propertyValue), but takes the
     * groupings into account. Implements both unary-query-expression for a General_Category alias
     * and binary-query-expression with a property-value where the queried property is
     * General_Category.
     */
    private static UnicodeSet getGeneralCategorySet(
            IndexUnicodeProperties iup, String propertyValue) {
        var gc = iup.getProperty(UcdProperty.General_Category);
        for (var entry : COARSE_GENERAL_CATEGORIES.entrySet()) {
            final var aliases = entry.getKey().getNames().getAllNames();
            if (aliases.stream().anyMatch(a -> UnicodeProperty.equalNames(propertyValue, a))) {
                UnicodeSet result = new UnicodeSet();
                for (var value : entry.getValue()) {
                    gc.getSet(value, result);
                }
                return result;
            }
        }
        return gc.getSet(propertyValue);
    }

    private static UnicodeSet getIdentitySet(UnicodeProperty queriedProperty) {
        final var result = new UnicodeSet();
        // Note that while UnicodeProperty, can return strings from getSet, which is an extension of
        // the UnicodeSet property-query specification, identity queries exclude any strings of
        // length other than 1, otherwise we would end up with infinite sets, e.g., the set of all
        // strings that normalize to themselves.
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            if (UnicodeProperty.equals(cp, queriedProperty.getValue(cp))) {
                result.add(cp);
            }
        }
        return result;
    }

    private static UnicodeSet compareProperties(
            UnicodeProperty queriedProperty, UnicodeProperty comparisonProperty) {
        if (!((queriedProperty.isType(UnicodeProperty.BINARY_MASK)
                        && comparisonProperty.isType(UnicodeProperty.BINARY_MASK))
                || (queriedProperty.isType(UnicodeProperty.NUMERIC_MASK)
                        && comparisonProperty.isType(UnicodeProperty.NUMERIC_MASK))
                || (queriedProperty.isType(UnicodeProperty.STRING_MASK)
                        && comparisonProperty.isType(UnicodeProperty.STRING_MASK))
                || (queriedProperty.isType(UnicodeProperty.ENUMERATED_OR_CATALOG_MASK)
                        && comparisonProperty.isType(UnicodeProperty.ENUMERATED_OR_CATALOG_MASK)
                        && queriedProperty
                                .getAvailableValues()
                                .equals(comparisonProperty.getAvailableValues()))
                || queriedProperty.getName().equals(comparisonProperty.getName()))) {
            throw new IllegalArgumentException(
                    "Invalid property comparison between "
                            + queriedProperty.getTypeName()
                            + " property "
                            + queriedProperty.getName()
                            + " and "
                            + comparisonProperty.getTypeName()
                            + " property "
                            + comparisonProperty.getName());
        }
        final var result = new UnicodeSet();
        // Note that while UnicodeProperty, can return strings from getSet, which is an extension of
        // the UnicodeSet property-query specification, property comparisons exclude any strings of
        // length other than 1.  Extending them to include those leads to messy questions of
        // defining the value of character properties for string (null?) and avoiding infinite sets.
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            if (UnicodeProperty.equals(
                    queriedProperty.getValue(cp), comparisonProperty.getValue(cp))) {
                result.add(cp);
            }
        }
        return result;
    }

    private VersionInfo implicitVersion;
    private VersionInfo previousVersion;
    private boolean requireSuffixForLatest;
    private UnicodeProperty.Factory unversionedExtensions;
    private static Pattern RATIONAL_PATTERN = Pattern.compile("[+-]?[0-9]+(/[0-9]*[1-9][0-9]*)?");
}
