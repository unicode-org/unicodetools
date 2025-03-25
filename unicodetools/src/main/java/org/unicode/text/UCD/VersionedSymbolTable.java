package org.unicode.text.UCD;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;
import java.util.Map;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UnicodeProperty;
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

    /**
     * Parses a string prefixed with an optional-version-qualifier. If there is a version-qualifier,
     * returns the corresponding VersionInfo and removes the prefix from the given StringBuilder.
     */
    private VersionInfo parseVersionQualifier(StringBuilder qualified) {
        int posColon = qualified.indexOf(":", 0);
        if (posColon < 0) {
            return null;
        } else {
            final String versionQualifier = qualified.substring(0, posColon + 1);
            qualified.delete(0, posColon + 1);
            if (versionQualifier.equals("U-1")) {
                return previousVersion;
            } else {
                switch (versionQualifier.charAt(0)) {
                    case 'R':
                        // Extension: we allow a version-qualifier starting with R for retroactive
                        // properties, that
                        // is, property derivations applied before the property existed.
                    case 'U':
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Invalid version-qualifier " + versionQualifier);
                }
                String versionNumber = versionQualifier.substring(1, posColon + 1);
                if (versionNumber.endsWith("dev")) {
                    versionNumber = versionNumber.substring(0, versionNumber.length() - 3);
                    if (!versionNumber.isEmpty()
                            && VersionInfo.getInstance(versionNumber)
                                    != Settings.LATEST_VERSION_INFO) {
                        throw new IllegalArgumentException(
                                "Invalid version-qualifier "
                                        + versionQualifier
                                        + " with version-suffix dev: the current dev version is "
                                        + Settings.latestVersion);
                    }
                    return Settings.LATEST_VERSION_INFO;
                } else if (versionNumber.endsWith("α") || versionNumber.endsWith("β")) {
                    final String versionSuffix =
                            versionNumber.substring(versionNumber.length() - 1);
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
                            && VersionInfo.getInstance(versionNumber)
                                    != Settings.LATEST_VERSION_INFO) {
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
    }

    private static Map<UcdPropertyValues.General_Category_Values, String[]>
            COARSE_GENERAL_CATEGORIES =
                    Map.of(
                            UcdPropertyValues.General_Category_Values.Other,
                            new String[] {"Cc", "Cf", "Cn", "Co", "Cs"},
                            UcdPropertyValues.General_Category_Values.Letter,
                            new String[] {"Ll", "Lm", "Lo", "Lt", "Lu"},
                            UcdPropertyValues.General_Category_Values.Cased_Letter,
                            new String[] {"Ll", "Lt", "Lu"},
                            UcdPropertyValues.General_Category_Values.Mark,
                            new String[] {"Mc", "Me", "Mn"},
                            UcdPropertyValues.General_Category_Values.Number,
                            new String[] {"Nd", "Nl", "No"},
                            UcdPropertyValues.General_Category_Values.Punctuation,
                            new String[] {"Pc", "Pd", "Pe", "Pf", "Pi", "Po", "Ps"},
                            UcdPropertyValues.General_Category_Values.Symbol,
                            new String[] {"Sc", "Sk", "Sm", "So"},
                            UcdPropertyValues.General_Category_Values.Separator,
                            new String[] {"Zl", "Zp", "Zs"});

    /**
     * Similar to iup.getProperty(UcdProperty.General_Category).getSet(propertyValue), but takes the
     * groupings into account. Implements both unary-query for a General_Category alias and
     * binary-query with a property-value where the queried property is General_Category.
     */
    private UnicodeSet getGeneralCategorySet(IndexUnicodeProperties iup, String propertyValue) {
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

    @Override
    public boolean applyPropertyAlias(String beforeEquals, String afterEquals, UnicodeSet result) {
        String leftHandSide = beforeEquals;
        String propertyPredicate = afterEquals;
        boolean interiorlyNegated = false;
        int posNotEqual = beforeEquals.indexOf('≠');
        // TODO(egg): We cannot distinguish \p{X=} from \p{X} in this API, both give us an empty
        // string
        // as afterEquals.  This is an @internal API, so we could change it to pass null in the
        // unary
        // case.
        if (posNotEqual >= 0) {
            propertyPredicate =
                    afterEquals.length() == 0
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
        final var unqualifiedLeftHandSide = new StringBuilder(leftHandSide);
        final var queriedVersion = parseVersionQualifier(unqualifiedLeftHandSide);
        final var deducedQueriedVersion = queriedVersion == null ? implicitVersion : queriedVersion;

        final var queriedProperties = IndexUnicodeProperties.make(deducedQueriedVersion);

        if (propertyPredicate.length() == 0) {
            // Either unary-property-query, or binary-property-query with an empty property-value.
            try {
                return queriedProperties
                        .getProperty(UcdProperty.Script)
                        .getSet(unqualifiedLeftHandSide.toString());
            } catch (Exception e) {
            }
            try {
                return getGeneralCategorySet(queriedProperties, unqualifiedLeftHandSide.toString());
            } catch (Exception e) {
            }
            UnicodeProperty queriedProperty =
                    queriedProperties.getProperty(unqualifiedLeftHandSide.toString());
            if (queriedProperty != null) {
                if (!queriedProperty.isType(UnicodeProperty.BINARY)) {
                    if (queriedProperty.isType(UnicodeProperty.STRING_OR_MISC_MASK)) {
                        return queriedProperty.getSet("");
                    }
                    throw new IllegalArgumentException(
                            "Invalid unary-query-expression for non-binary property "
                                    + queriedProperty.getName());
                }
                return queriedProperty.getSet(UcdPropertyValues.Binary.Yes);
            }
        } else {
            if (queriedProperty == null) {
                propertyValue = propertyValue.trim();
            } else if (prop.isTrimmable()) {
                propertyValue = propertyValue.trim();
            } else {
                int debug = 0;
            }
            status = applyPropertyAlias0(prop, propertyValue, result, invert);
        }
        // TODO(egg):Something about a factory as a fallback;
    }

    private VersionInfo implicitVersion;
    private VersionInfo previousVersion;
    private boolean requireSuffixForLatest;
}
