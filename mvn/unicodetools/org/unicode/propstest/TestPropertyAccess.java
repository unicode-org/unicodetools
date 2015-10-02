package org.unicode.propstest;

import java.util.Collection;

import org.unicode.cldr.unittest.TestFmwkPlus;
import org.unicode.props.GenerateEnums;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyType;
import org.unicode.props.UcdProperty;
import org.unicode.props.UnicodePropertyException;
import org.unicode.props.ValueCardinality;

import com.ibm.icu.dev.util.UnicodeMap;

public class TestPropertyAccess extends TestFmwkPlus {
    public static void main(String[] args) {
        new TestPropertyAccess().run(args);
    }

    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(GenerateEnums.ENUM_VERSION);

    public void TestLoad() {
        for (ValueCardinality cardinality : ValueCardinality.values()) {
            for (PropertyType propertyType : PropertyType.values()) {
                if (propertyType != getMainType(propertyType)) {
                    continue;
                }
                logln("\n\nCARDINALITY: " + cardinality.toString() + ", PROPERTY_TYPE: " + propertyType.toString() + "\n");

                for (UcdProperty prop : iup.getAvailableUcdProperties()) {
                    try {
                        if (prop.getCardinality() != cardinality || getMainType(prop.getType()) != propertyType) {
                            continue;
                        }
                        switch (propertyType) {
                        case Numeric: {
                            logln(prop + "\t" + show(iup.loadDouble(prop)));
                            break;
                        }
                        case Binary:
                        case Catalog:
                        case Enumerated:
                            switch (cardinality) {
                            case Singleton: {
                                if (prop == UcdProperty.Canonical_Combining_Class) {
                                    logln(prop + "\t" + show(iup.loadInt(prop)));
                                }
                                logln(prop + "\t" + show(iup.loadEnum(prop, prop.getEnumClass())));
                                break;
                            }
                            case Unordered: {
                                logln(prop + "\t" + show(iup.loadEnumSet(prop, prop.getEnumClass())));
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
                                logln(prop + "\t" + show(iup.load(prop)));
                                break;
                            }
                            case Unordered: {
                                logln(prop + "\t" + show(iup.loadSet(prop)));
                                break;
                            }
                            case Ordered: {
                                logln(prop + "\t" + show(iup.loadList(prop)));
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