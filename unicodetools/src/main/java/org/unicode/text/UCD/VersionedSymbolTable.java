package org.unicode.text.UCD;

import java.util.Map;
import java.util.TreeMap;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.utility.Settings;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

/**
 * This class implements the semantics of property-query as defined in the
 * UnicodeSet specification.
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

  /** Parses a string prefixed with an optional-version-qualifier.
   * If there is a version-qualifier, returns the corresponding VersionInfo and
   * removes the prefix from the given StringBuilder.
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
          // Extension: we allow a version-qualifier starting with R for retroactive properties, that
          // is, property derivations applied before the property existed.
          case 'U':
            break;
          default:
            throw new IllegalArgumentException("Invalid version-qualifier " + versionQualifier);
        }
        String versionNumber = versionQualifier.substring(1, posColon + 1);
        if (versionNumber.endsWith("dev")) {
          versionNumber = versionNumber.substring(0, versionNumber.length() - 3);
          if (!versionNumber.isEmpty() &&
              VersionInfo.getInstance(versionNumber) != Settings.LATEST_VERSION_INFO) {
            throw new IllegalArgumentException("Invalid version-qualifier " + versionQualifier + " with version-suffix dev: the current dev version is " + Settings.latestVersion);
          }
          return Settings.LATEST_VERSION_INFO;
        } else if (versionNumber.endsWith("α") || versionNumber.endsWith("β")) {
          final String versionSuffix = versionNumber.substring(versionNumber.length() - 1);
          versionNumber = versionNumber.substring(0, versionNumber.length() - 1);
          if (versionSuffix != Settings.latestVersionPhase.toString()) {
            throw new IllegalArgumentException("Invalid version-qualifier " + versionQualifier + " with version-suffix " + versionSuffix + ": the current stage is " + Settings.latestVersionPhase);
          }
          if (!versionNumber.isEmpty() &&
              VersionInfo.getInstance(versionNumber) != Settings.LATEST_VERSION_INFO) {
            throw new IllegalArgumentException("Invalid version-qualifier " + versionQualifier + " with version-suffix " + versionNumber + ": the current " + versionSuffix + " version is " + Settings.latestVersion);
          }
          return Settings.LATEST_VERSION_INFO;
        } else {
          var result = VersionInfo.getInstance(versionNumber);
          if (result == Settings.LATEST_VERSION_INFO && requireSuffixForLatest) {
            throw new IllegalArgumentException("Invalid version-qualifier " + versionQualifier + " version-suffix " + Settings.latestVersionPhase + " required for unpublished version");
          }
          return result;
        }
      }
    }
  }

  @Override
  public boolean applyPropertyAlias(String beforeEquals, String afterEquals, UnicodeSet result) {
    String leftHandSide = beforeEquals;
    String propertyPredicate = afterEquals;
    boolean interiorlyNegated = false;
    int posNotEqual = beforeEquals.indexOf('≠');
    // TODO(egg): We cannot distinguish \p{X=} from \p{X} in this API, both give us an empty string
    // as afterEquals.
    if (posNotEqual >= 0) {
      propertyPredicate = afterEquals.length() == 0
          ? beforeEquals.substring(posNotEqual + 1)
          : beforeEquals.substring(posNotEqual + 1) + "=" + afterEquals;
      leftHandSide = beforeEquals.substring(0, posNotEqual);
      interiorlyNegated = true;
    }

    final var queriedPropertyName = new StringBuilder(leftHandSide);
    final var queriedVersion = parseVersionQualifier(queriedPropertyName);
    final var deducedQueriedVersion = queriedVersion == null ? implicitVersion : queriedVersion;

    final var queriedUcd = IndexUnicodeProperties.make(deducedQueriedVersion);

    var generalCategory = queriedUcd.getProperty(UcdProperty.General_Category);
    var script = queriedUcd.getProperty(UcdProperty.Script);

    UnicodeProperty queriedProperty = queriedUcd.getProperty(queriedPropertyName.toString());
    if (propertyPredicate.length() != 0) {
      if (queriedProperty == null) {
        propertyValue = propertyValue.trim();
      } else if (prop.isTrimmable()) {
        propertyValue = propertyValue.trim();
      } else {
        int debug = 0;
      }
      status = applyPropertyAlias0(prop, propertyValue, result, invert);
    } else {
      try {
        status = applyPropertyAlias0(gcProp, versionlessPropertyName, result, invert);
      } catch (Exception e) {
      }
      ;
      if (!status) {
        try {
          status = applyPropertyAlias0(
              scProp, versionlessPropertyName, result, invert);
        } catch (Exception e) {
        }
        if (!status) {
          if (prop.isType(UnicodeProperty.BINARY_OR_ENUMERATED_OR_CATALOG_MASK)) {
            try {
              status = applyPropertyAlias0(prop, "No", result, !invert);
            } catch (Exception e) {
            }
          }
          if (!status) {
            status = applyPropertyAlias0(prop, "", result, invert);
          }
        }
      }
    }
    //TODO(egg):Something about a factory as a fallback;
    return status;
  }

  private VersionInfo implicitVersion;
  private VersionInfo previousVersion;
  private boolean requireSuffixForLatest;
}
