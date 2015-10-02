package org.unicode.props;

import com.ibm.icu.dev.util.UnicodeProperty;

public enum PropertyType {
    Numeric(UnicodeProperty.NUMERIC), 
    String(UnicodeProperty.STRING), 
    Miscellaneous(UnicodeProperty.MISC), 
    Catalog(UnicodeProperty.CATALOG), 
    Enumerated(UnicodeProperty.ENUMERATED), 
    Binary(UnicodeProperty.BINARY);

    private final int oldNumber;
    private PropertyType(int oldNumber) {
        this.oldNumber = oldNumber;
    }
    
    public int getOldNumber() {
        return oldNumber;
    }
}