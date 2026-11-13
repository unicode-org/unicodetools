package org.unicode.propstest;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.impl.Utility;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyType;
import org.unicode.props.UcdProperty;
import org.unicode.props.UnicodeProperty;
import org.unicode.props.UnicodePropertyException;
import org.unicode.props.ValueCardinality;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestPropertyAccess extends TestFmwkMinusMinus {

    static final IndexUnicodeProperties iup =
            IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);

    @Test
    public void TestEmoji() {
        UnicodeProperty property = iup.getProperty(UcdProperty.Emoji_Component);
        assertEquals("Skin tone modifier", "Yes", property.getValue(0x1F3FB));
        assertEquals("Latin letter", "No", property.getValue('A'));
    }

    @Test
    public void TestLoad() {
        for (ValueCardinality cardinality : ValueCardinality.values()) {
            for (PropertyType propertyType : PropertyType.values()) {
                if (propertyType != getMainType(propertyType)) {
                    continue;
                }
                System.out.println(
                        "\nCARDINALITY: "
                                + cardinality.toString()
                                + ", PROPERTY_TYPE: "
                                + propertyType.toString()
                                + "\n");

                for (UcdProperty prop : iup.getAvailableUcdProperties()) {
                    try {
                        if (prop.getCardinality() != cardinality
                                || getMainType(prop.getType()) != propertyType) {
                            continue;
                        }
                        System.out.println(prop);
                        checkPropertyValues(prop);
                        switch (propertyType) {
                            case Numeric:
                                {
                                    logShow(prop, iup.loadDouble(prop));
                                    break;
                                }
                            case Binary:
                            case Catalog:
                            case Enumerated:
                                switch (cardinality) {
                                    case Singleton:
                                        {
                                            if (prop == UcdProperty.Canonical_Combining_Class) {
                                                logShow(prop, iup.loadInt(prop));
                                            }
                                            logShow(prop, iup.loadEnum(prop, prop.getEnumClass()));
                                            break;
                                        }
                                    case Unordered:
                                        {
                                            logShow(
                                                    prop,
                                                    iup.loadEnumSet(prop, prop.getEnumClass()));
                                            break;
                                        }
                                    case Ordered:
                                        {
                                            System.err.println(prop + "\t" + cardinality);
                                            break;
                                        }
                                }
                                break;
                            default:
                                break;
                        }
                    } catch (UnicodePropertyException e) {
                        errln("Failed to load: " + prop + ", " + e.getMessage());
                    }
                }
            }
        }
    }

    private void checkPropertyValues(UcdProperty prop) {
        UnicodeProperty property = iup.getProperty(prop);
        // Read values even when logging is disabled: getProperty alone does not load the data.
        for (int cp :
                new int[] {
                    0, 'A', 0x0300, 0x2F00, 0x4E00, 0x908A, 0xAC00, 0xFE00, 0x1F3FB, 0x20000,
                    0x10FFFF
                }) {
            for (String value : property.getValues(cp)) {
                if (isVerbose()) {
                    logln(prop + "[" + Utility.hex(cp) + "]\t" + value);
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
