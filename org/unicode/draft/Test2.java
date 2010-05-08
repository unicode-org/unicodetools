package org.unicode.draft;
import java.awt.Font;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.util.ULocale;


public class Test2 {
    public static void main(String[] args) {
        Character.isLowSurrogate('a');
        testEnsurePlus();
        Font f;
        if (true) return;
        foo(Enum2.b);
        timeStrings();
        StringTransform t = Transliterator.getInstance("any-publishing");
        String result = t.transform("\"he said 'I won't, and can't!' as he stormed out--and slammed the door.\"");
        System.out.println(result);
    }

    private static void testEnsurePlus() {
        for (ULocale locale : NumberFormat.getAvailableULocales()) {
            if (locale.getCountry().length() > 0) {
                continue; // skip country locales
            }
            NumberFormat nf = NumberFormat.getPercentInstance(locale);
            nf.setMinimumFractionDigits(2);
            String oldPositive = nf.format(0.1234);
            String oldNegative = nf.format(-0.1234);
            nf = ensurePlus(nf);
            String newPositive = nf.format(0.1234);
            System.out.println(locale + "\t" + locale.getDisplayName(ULocale.ENGLISH)
                    + "\t" + newPositive
                    + "\t" + oldPositive
                    + "\t" + oldNegative
                    );
        }
    }

    private static NumberFormat ensurePlus(NumberFormat numberFormat) {
        DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
        String positivePrefix = decimalFormat.getPositivePrefix();
        String positiveSuffix = decimalFormat.getPositiveSuffix();
        if (!positivePrefix.contains("+") && !positiveSuffix.contains("+")) {
              decimalFormat.setPositivePrefix("+" + positivePrefix);
        }
        return decimalFormat;
    }
    
    static class Foo extends AbstractList<String> {

        @Override
        public String get(int index) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int size() {
            // TODO Auto-generated method stub
            return 0;
        }


    }
    
    public LanguageCode fromOther(ULocale uLocale) {
        // for invalid codes, get the best fit.
        while (true) {
            try {
                return LanguageCode.forString(uLocale.toLanguageTag());
            } catch (InvalidLanguageCode e) {
                uLocale = uLocale.getFallback();
                // keep going until we fail completely
                if (uLocale == null) {
                    throw e; 
                }
            }
        }
    }
    
    public LanguageCode fromOther(Locale locale) {
        // for invalid codes, get the best fit.
        String localeString = locale.toString();
        while (true) {
            try {
                return LanguageCode.forString(localeString);
            } catch (InvalidLanguageCode e) {
                // keep going until we fail completely
                int lastUnder = localeString.lastIndexOf('_');
                if (lastUnder < 0) {
                    throw e; 
                }
                localeString = localeString.substring(0,lastUnder);
            }
        }
    }
    
    static void timeStrings() {
        List<CharSequence> data1 = new ArrayList<CharSequence>();
        List<CharSequence> data2 = new ArrayList<CharSequence>();
        for (String lang : ULocale.getISOLanguages()) {
            data1.add(ULocale.getDisplayName(lang, ULocale.ENGLISH));
            for (String reg : ULocale.getISOCountries()) {
                data1.add(ULocale.getDisplayName(lang + "_" + reg, ULocale.ENGLISH));
            }
        }
        for (CharSequence a : data1) {
            data2.add(new Hasher(a));
        }
        HashSet<CharSequence> s1 = new HashSet<CharSequence>();

        double time1 = time(s1, data1, 100);
        System.out.println("Hash\t" + time1);
        double time2 = time(s1, data2, 100);
        System.out.println("Hash\t" + time2 + "\t\t" + 100*time2/time1 + "%");
    }
    
    static class Hasher implements CharSequence, Comparable<CharSequence> {
        CharSequence s;
        int hashCode;
        Hasher(CharSequence s) {
            this.s = s;
            hashCode = s.hashCode();
        }
        public char charAt(int index) {
            return s.charAt(index);
        }
        public int compareTo(CharSequence other) {
            int length = s.length();
            int diff = length - other.length();
            if (diff != 0) return diff;
            for (int i = 0; i < length; ++i) {
                diff = s.charAt(i) - other.charAt(i);
                if (diff != 0) return diff;
            }
            return 0;
        }
        
        public int compareTo(Hasher other) {
            return compareTo(other.s);
        }
        
        public boolean equals(CharSequence anObject) {
            return s.equals(anObject);
        }
        public int hashCode() {
            return s.hashCode();
        }
        public int length() {
            return s.length();
        }
        public CharSequence subSequence(int start, int end) {
            return s.subSequence(start, end);
        }
    };
    
    private static double time(Set<CharSequence> s1, List<CharSequence> data, int count) {
        double start = System.currentTimeMillis();
        boolean in = false;
        for (int i = count; i > 0; --i) {
            s1.clear();
            s1.addAll(data);
            for (CharSequence item : data) {
                in ^= s1.contains(item);
            }
        }
        return (System.currentTimeMillis() - start)/count;
    }
    static class LanguageCode {

        public static LanguageCode forString(String string) {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    
    enum Enum1 {a, b, c}
    enum Enum2 {b, c, d}
    static void foo(Enum x) {
        System.out.println(x.compareTo(Enum2.b));
        System.out.println(x.name());
        System.out.println(x.ordinal());
        System.out.println(Enum1.valueOf(Enum1.a.name()));
        System.out.println(Arrays.asList(Enum1.values()));
        System.out.println(Enum.valueOf(Enum2.class, "b"));
    }
    
    static class InvalidLanguageCode extends RuntimeException {
        
    }

}
