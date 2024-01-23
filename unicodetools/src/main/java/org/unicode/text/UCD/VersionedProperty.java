package org.unicode.text.UCD;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.SymbolTable;
import com.ibm.icu.text.UnicodeSet;
import java.text.ParsePosition;
import java.util.Map;
import java.util.Set;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UnicodeProperty;
import org.unicode.props.UnicodeProperty.Factory;
import org.unicode.props.UnicodeProperty.PatternMatcher;
import org.unicode.text.utility.Settings;

public class VersionedProperty {
    private String propertyName;
    private String version;
    private UnicodeProperty.Factory propSource;
    private UnicodeProperty property;
    private final transient PatternMatcher matcher = new UnicodeProperty.RegexMatcher();

    private static final Set<String> TOOL_ONLY_PROPERTIES =
            Set.of("toNFC", "toNFD", "toNFKC", "toNFKD");

    private static boolean isTrivial(UnicodeMap<String> map) {
        return map.isEmpty()
                || (map.values().size() == 1
                        && map.getSet(map.values().iterator().next())
                                .equals(UnicodeSet.ALL_CODE_POINTS));
    }

    public UnicodeProperty getProperty() {
        return property;
    }

    public VersionedProperty set(String xPropertyName) {
        xPropertyName = xPropertyName.trim();
        boolean allowRetroactive = false;
        if (xPropertyName.contains(":")) {
            final String[] names = xPropertyName.split(":");
            if (names.length != 2) {
                throw new IllegalArgumentException("Too many ':' fields in " + xPropertyName);
            }
            if (names[0].isEmpty()) {
                throw new IllegalArgumentException("Empty version field in " + xPropertyName);
            }
            switch (names[0].charAt(0)) {
                case 'U':
                    break;
                case 'R':
                    allowRetroactive = true;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Version field should start with U or R in " + xPropertyName);
            }
            if (names[0].substring(1).equals("-1")) {
                version = Settings.lastVersion;
            } else {
                version = names[0].substring(1);
            }
            xPropertyName = names[1];
        } else {
            version = Settings.latestVersion;
        }
        ;
        propertyName = xPropertyName;
        propSource = getIndexedProperties(version);
        property = propSource.getProperty(xPropertyName);
        if ((property == null && TOOL_ONLY_PROPERTIES.contains(xPropertyName))
                || (isTrivial(property.getUnicodeMap()) && allowRetroactive)) {
            propSource = ToolUnicodePropertySource.make(version);
            property = propSource.getProperty(xPropertyName);
        }
        if (property == null || isTrivial(property.getUnicodeMap())) {
            throw new IllegalArgumentException(
                    "Can't create property from name: "
                            + propertyName
                            + " and version: "
                            + version);
        }
        return this;
    }

    public UnicodeSet getSet(
            String propertyValue, SymbolTable symbolTable, Map<String, char[]> variables) {
        UnicodeSet set;
        if (propertyValue.length() == 0) {
            set = property.getSet("true");
        } else if (propertyValue.startsWith("/") && propertyValue.endsWith("/")) {
            String body = propertyValue.substring(1, propertyValue.length() - 1);
            for (final String variableMinus : variables.keySet()) {
                final String variable = "$" + variableMinus;
                if (body.contains(variable)) {
                    final String replacement = String.copyValueOf(variables.get(variableMinus));
                    final UnicodeSet value = parseUnicodeSet(replacement, symbolTable);
                    final String valueString = value.complement(0).complement(0).toPattern(false);
                    body = body.replace(variable, valueString);
                }
            }
            matcher.set(body);
            set = property.getSet(matcher);
        } else if (propertyValue.equals("∅")) {
            set = property.getSet(NULL_MATCHER, null);
        } else {
            set = property.getSet(propertyValue);
        }
        return set;
    }

    private static Factory getIndexedProperties(String version2) {
        return IndexUnicodeProperties.make(version2);
    }

    public static UnicodeSet parseUnicodeSet(String line, SymbolTable symbolTable) {
        final ParsePosition pp = new ParsePosition(0);
        final UnicodeSet result = new UnicodeSet(line, pp, symbolTable);
        final int lengthUsed = pp.getIndex();
        if (lengthUsed != line.length()) {
            throw new IllegalArgumentException(
                    "Text after end of set: "
                            + line.substring(0, lengthUsed)
                            + "XXX"
                            + line.substring(lengthUsed));
        }
        return result;
    }

    static final UnicodeProperty.PatternMatcher NULL_MATCHER =
            new UnicodeProperty.PatternMatcher() {
                @Override
                public boolean test(String o) {
                    return o == null || "".equals(o);
                }

                @Override
                public PatternMatcher set(String pattern) {
                    return this;
                }
            };
}
