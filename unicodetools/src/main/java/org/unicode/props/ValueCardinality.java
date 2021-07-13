package org.unicode.props;

public enum ValueCardinality {
    Singleton, Unordered, Ordered;
    public boolean isBreakable(String string) {
        return (this == ValueCardinality.Unordered || this == ValueCardinality.Ordered);
    }
}