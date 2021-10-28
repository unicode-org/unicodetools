package org.unicode.props;

public class UnicodePropertyException extends RuntimeException {
    private static final long serialVersionUID = 7687343321645291439L;
    public UnicodePropertyException(String message, Throwable cause) {
        super(message, cause);
    }
    public UnicodePropertyException(String message) {
        super(message);
    }
    public UnicodePropertyException(Throwable cause) {
        super(cause);
    }
    public UnicodePropertyException() {
        super();
    }
}
