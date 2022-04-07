package org.unicode.props;

import org.unicode.props.UnicodeProperty;

public enum PropertyType {
    Numeric(UnicodeProperty.NUMERIC),
    String(UnicodeProperty.STRING),
    Miscellaneous(UnicodeProperty.MISC),
    Catalog(UnicodeProperty.CATALOG),
    Enumerated(UnicodeProperty.ENUMERATED),
    Binary(UnicodeProperty.BINARY),
    Unknown(-1)
    ;

    private final int oldNumber;
    private PropertyType(int oldNumber) {
        this.oldNumber = oldNumber;
    }

    public int getOldNumber() {
        return oldNumber;
    }
}
