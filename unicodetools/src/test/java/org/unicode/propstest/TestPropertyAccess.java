package org.unicode.propstest;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyType;
import org.unicode.props.UcdProperty;
import org.unicode.props.UnicodePropertyException;
import org.unicode.props.ValueCardinality;
import org.unicode.unittest.TestFmwkMinusMinus;

import com.ibm.icu.dev.util.UnicodeMap;

public class TestPropertyAccess extends TestFmwkMinusMinus {

    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);

    @Test
    public void TestEmoji() {
        UnicodeMap<String> map = iup.load(UcdProperty.Emoji_Component);
        System.out.println(map);
    }

    @Test
    public void TestLoad() {
        for (ValueCardinality cardinality : ValueCardinality.values()) {
            for (PropertyType propertyType : PropertyType.values()) {
                if (propertyType != getMainType(propertyType)) {
                    continue;
                }
                System.out.println("\nCARDINALITY: " + cardinality.toString() + ", PROPERTY_TYPE: " + propertyType.toString() + "\n");

                for (UcdProperty prop : iup.getAvailableUcdProperties()) {
                    try {
                        if (prop.getCardinality() != cardinality || getMainType(prop.getType()) != propertyType) {
                            continue;
                        }
                        System.out.println(prop);
                        switch (propertyType) {
                        case Numeric: {
                            logShow(prop, iup.loadDouble(prop));
                            break;
                        }
                        case Binary:
                        case Catalog:
                        case Enumerated:
                            switch (cardinality) {
                            case Singleton: {
                                if (prop == UcdProperty.Canonical_Combining_Class) {
                                    logShow(prop, iup.loadInt(prop));
                                }
                                logShow(prop, iup.loadEnum(prop, prop.getEnumClass()));
                                break;
                            }
                            case Unordered: {
                                logShow(prop, iup.loadEnumSet(prop, prop.getEnumClass()));
                                break;
                            }
                            case Ordered: {
                                System.err.println(prop + "\t" + cardinality);
                                break;
                            }
                            }
                            break;
                        case Miscellaneous:
                        case String: {
                            switch (cardinality) {
                            case Singleton: {
                                logShow(prop, iup.load(prop));
                                break;
                            }
                            case Unordered: {
                                logShow(prop, iup.loadSet(prop));
                                break;
                            }
                            case Ordered: {
                                logShow(prop, iup.loadList(prop));
                                break;
                            }
                            }
                            break;
                        }
                        }
                    } catch (UnicodePropertyException e) {
                        errln("Failed to load: " + prop + ", " + e.getMessage());
                    }
                }
            }
        }
    }
    private void logShow(UcdProperty prop, UnicodeMap temp) {
        if (isVerbose()) {
            logln(prop + "\t" + show(temp));
        }
    }

    PropertyType getMainType(PropertyType input) {
        switch (input) {
        case Binary:
        case Catalog:
            return PropertyType.Enumerated;
        case Miscellaneous:
        case String:
            return PropertyType.String;
        default:
            return input;
        }
    }

    private <T> String show(UnicodeMap<T> map) {
        Collection<T> values = map.getAvailableValues();
        if (values.isEmpty()) {
            return "*** MISSING ***";
        }
        StringBuilder b = new StringBuilder();
        for (T value : values) {
            if (b.length() != 0) {
                b.append(", ");
            }
            b.append(value.toString());
            if (b.length() > 150) {
                break;
            }
        }
        return b.toString();
    }
}