package org.unicode.draft;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;

public class Test3 {
    public void method(int... stuff) {
        System.out.println("int...\t" + Arrays.asList(stuff));
    }

    public void method2(int[] stuff) {
        System.out.println("int[]\t" + Arrays.asList(stuff));
    }

    java.util.Currency foo;

    static class MyCurrency extends Currency {

        protected MyCurrency(String theISOCode) {
            super(Currency.getInstance(theISOCode).getCurrencyCode());
        }

        @Override
        public String getName(ULocale locale, int nameStyle, boolean[] isChoiceFormat) {
            final String currencyCode = getCurrencyCode();
            if ("USD".equals(currencyCode)) {
                return "US$";
            }
            return super.getSymbol();
        }
    }

    static final Currency USD = Currency.getInstance("USD");

    static class MyCurrencyFormat extends DecimalFormat {
        public MyCurrencyFormat(Locale locale) {
            final DecimalFormat format = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
            applyPattern(format.toPattern());
            setDecimalFormatSymbols(format.getDecimalFormatSymbols());
        }

        @Override
        public void setCurrency(Currency theCurrency) {
            super.setCurrency(theCurrency);
            if (theCurrency.equals(USD)) {
                final DecimalFormatSymbols symbols = getDecimalFormatSymbols();
                if ("$".equals(symbols.getCurrencySymbol())) {
                    symbols.setCurrencySymbol("US$");
                }
            }
        }
    }

    public static void main(String[] args) {
        // Currency.getAvailableLocales()
        for (final Locale locale : new Locale[] {Locale.US, Locale.CANADA}) {
            final DecimalFormat format = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);

            String pattern = format.toPattern();
            pattern += " [\u00a4\u00a4]";
            format.applyPattern(pattern);

            format.setCurrency(Currency.getInstance("USD"));
            if (format.getCurrency().equals(USD)) {
                final DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
                if ("$".equals(symbols.getCurrencySymbol())) {
                    symbols.setCurrencySymbol("US$");
                }
            }
            String result = format.format(12345.678);
            System.out.println(locale.getDisplayName(locale) + "\t\t\t" + result);

            format.setCurrency(Currency.getInstance("CAD"));
            result = format.format(12345.678);
            System.out.println(locale.getDisplayName(locale) + "\t\t\t" + result);
        }

        final Test3 test = new Test3();
        test.method(1, 2, 3);
        test.method(new int[] {1, 2, 3});

        // test.method2(1,2,3); // fails to compile
        test.method2(new int[] {1, 2, 3});

        final int foo = new Integer(1).hashCode();
    }

    ByteBuffer b;
}
