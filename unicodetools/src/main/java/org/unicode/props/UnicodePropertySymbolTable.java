/*
 *******************************************************************************
 * Copyright (C) 1996-2012, Google, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.props;

import com.ibm.icu.util.VersionInfo;
import java.util.Comparator;
import java.util.function.Function;
import org.unicode.props.UnicodeProperty.PatternMatcher;

/**
 * Allows for overriding the parsing of UnicodeSet property patterns.
 *
 * <p>WARNING: If this UnicodePropertySymbolTable is used with {@code
 * UnicodeSet.setDefaultXSymbolTable}, and the Unassigned characters (gc=Cn) are different than in
 * ICU other than in ICU, you MUST call {@code UnicodeProperty.ResetCacheProperties} afterwards. If
 * you then call {@code UnicodeSet.setDefaultXSymbolTable} with null to clear the value, you MUST
 * also call {@code UnicodeProperty.ResetCacheProperties}.
 *
 * @author markdavis
 */
public class UnicodePropertySymbolTable {
    public enum Relation {
        less,
        leq,
        equal,
        geq,
        greater
    }

    public static class ComparisonMatcher<T> implements PatternMatcher {
        final Relation relation;
        final Comparator<T> comparator;
        final Function<String, T> parser;
        T expected;

        public ComparisonMatcher(
                T expected,
                Relation relation,
                Comparator<T> comparator,
                Function<String, T> parser) {
            this.relation = relation;
            this.expected = expected;
            this.comparator = comparator;
            this.parser = parser;
        }

        @Override
        public boolean test(String value) {
            int comp = comparator.compare(expected, parser.apply(value));
            switch (relation) {
                case less:
                    return comp < 0;
                case leq:
                    return comp <= 0;
                default:
                    return comp == 0;
                case geq:
                    return comp >= 0;
                case greater:
                    return comp > 0;
            }
        }

        @Override
        public PatternMatcher set(String pattern) {
            this.expected = parser.apply(pattern);
            return this;
        }
    }

    public static VersionInfo parseVersionInfoOrMax(String s) {
        if (s == null) {
            return null;
        }
        try {
            return VersionInfo.getInstance(s);
        } catch (IllegalArgumentException e) {
            return VersionInfo.getInstance(255, 255, 255, 255);
        }
    }
}
