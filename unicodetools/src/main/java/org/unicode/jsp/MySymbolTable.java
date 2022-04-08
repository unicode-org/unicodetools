package org.unicode.jsp;

import java.util.Comparator;
import java.util.List;

import org.unicode.props.UnicodeProperty;

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class MySymbolTable extends UnicodeSet.XSymbolTable {
    private UnicodeRegex unicodeRegex;
    private UnicodeProperty.Factory factory;

    public MySymbolTable(UnicodeProperty.Factory propertyFactory) {
        factory = propertyFactory;
        unicodeRegex = new UnicodeRegex().setSymbolTable(this);
    }


    //    public boolean applyPropertyAlias0(String propertyName,
    //            String propertyValue, UnicodeSet result) {
    //      if (!propertyName.contains("*")) {
    //        return applyPropertyAlias(propertyName, propertyValue, result);
    //      }
    //      String[] propertyNames = propertyName.split("[*]");
    //      for (int i = propertyNames.length - 1; i >= 0; ++i) {
    //        String pname = propertyNames[i];
    //
    //      }
    //      return null;
    //    }

    public boolean applyPropertyAlias(String propertyName,
            String propertyValue, UnicodeSet result) {
        boolean status = false;
        boolean invert = false;
        int posNotEqual = propertyName.indexOf('\u2260');
        int posColon = propertyName.indexOf(':');
        if (posNotEqual >= 0 || posColon >= 0) {
            if (posNotEqual < 0) posNotEqual = propertyName.length();
            if (posColon < 0) posColon = propertyName.length();
            int opPos = posNotEqual < posColon ? posNotEqual : posColon;
            propertyValue = propertyValue.length() == 0 ? propertyName.substring(opPos+1)
                    : propertyName.substring(opPos+1) + "=" + propertyValue;
            propertyName = propertyName.substring(0,opPos);
            if (posNotEqual < posColon) {
                invert = true;
            }
        }
        if (propertyName.endsWith("!")) {
            propertyName = propertyName.substring(0, propertyName.length() - 1);
            invert = !invert;
        }
        propertyValue = propertyValue.trim();
        if (propertyValue.length() != 0) {
            status = applyPropertyAlias0(propertyName, propertyValue, result);
        } else {
            try {
                status = applyPropertyAlias0("gc", propertyName, result);
            } catch (Exception e) {};
            if (!status) {
                try {
                    status = applyPropertyAlias0("sc", propertyName, result);
                } catch (Exception e) {};
                if (!status) {
                    try {
                        status = applyPropertyAlias0(propertyName, "Yes", result);
                    } catch (Exception e) {};
                    if (!status) {
                        status = applyPropertyAlias0(propertyName, "", result);
                    }
                }
            }
        }
        if (status && invert) {
            result.complement();
        }
        return status;
    }

    public boolean applyPropertyAlias0(String propertyName,
            String propertyValue, UnicodeSet result) {
        result.clear();
        UnicodeProperty.PatternMatcher patternMatcher = null;
        if (propertyValue.length() > 1 && propertyValue.startsWith("/") && propertyValue.endsWith("/")) {
            String fixedRegex = unicodeRegex.transform(propertyValue.substring(1, propertyValue.length() - 1));
            patternMatcher = new UnicodeProperty.RegexMatcher().set(fixedRegex);
        }
        UnicodeProperty otherProperty = null;
        boolean testCp = false;
        if (propertyValue.length() > 1 && propertyValue.startsWith("@") && propertyValue.endsWith("@")) {
            String otherPropName = propertyValue.substring(1, propertyValue.length() - 1).trim();
            if ("cp".equalsIgnoreCase(otherPropName)) {
                testCp = true;
            } else {
                otherProperty = factory.getProperty(otherPropName);
            }
        }
        boolean isAge = UnicodeProperty.equalNames("age", propertyName);
        UnicodeProperty prop = factory.getProperty(propertyName);
        if (prop != null) {
            UnicodeSet set;
            if (testCp) {
                set = new UnicodeSet();
                for (int i = 0; i <= 0x10FFFF; ++i) {
                    if (UnicodeProperty.equals(i, prop.getValue(i))) {
                        set.add(i);
                    }
                }
            } else if (otherProperty != null) {
                set = new UnicodeSet();
                for (int i = 0; i <= 0x10FFFF; ++i) {
                    String v1 = prop.getValue(i);
                    String v2 = otherProperty.getValue(i);
                    if (UnicodeProperty.equals(v1, v2)) {
                        set.add(i);
                    }
                }
            } else if (patternMatcher == null) {
                if (!isValid(prop, propertyValue)) {
                    throw new IllegalArgumentException("The value '" + propertyValue + "' is illegal. Values for " + propertyName
                            + " must be in "
                            + prop.getAvailableValues() + " or in " + prop.getValueAliases());
                }
                if (isAge) {
                    set = prop.getSet(new ComparisonMatcher(propertyValue, ComparisonMatcher.Relation.geq));
                } else {
                    set = prop.getSet(propertyValue);
                }
            } else if (isAge) {
                set = new UnicodeSet();
                List<String> values = prop.getAvailableValues();
                for (String value : values) {
                    if (patternMatcher.test(value)) {
                        for (String other : values) {
                            if (other.compareTo(value) <= 0) {
                                set.addAll(prop.getSet(other));
                            }
                        }
                    }
                }
            } else {
                set = prop.getSet(patternMatcher);
            }
            result.addAll(set);
            return true;
        }
        throw new IllegalArgumentException("Illegal property: " + propertyName);
    }



    private boolean isValid(UnicodeProperty prop, String propertyValue) {
        //      if (prop.getName().equals("General_Category")) {
        //        if (propertyValue)
        //      }
        return prop.isValidValue(propertyValue);
    }

    public static class ComparisonMatcher implements UnicodeProperty.PatternMatcher {
        Relation relation;
        enum Relation {less, leq, equal, geq, greater}
        static Comparator comparator = new UTF16.StringComparator(true, false,0);

        String pattern;

        public ComparisonMatcher(String pattern, Relation comparator) {
            this.relation = comparator;
            this.pattern = pattern;
        }

        @Override
        public boolean test(String value) {
            int comp = comparator.compare(pattern, value.toString());
            switch (relation) {
            case less: return comp < 0;
            case leq: return comp <= 0;
            default: return comp == 0;
            case geq: return comp >= 0;
            case greater: return comp > 0;
            }
        }

        public UnicodeProperty.PatternMatcher set(String pattern) {
            this.pattern = pattern;
            return this;
        }
    }

    public static void setDefaultXSymbolTable(UnicodeProperty.Factory factory) {
        UnicodeSet.setDefaultXSymbolTable(new MySymbolTable(factory));
        UnicodeProperty.ResetCacheProperties();
    }
}
