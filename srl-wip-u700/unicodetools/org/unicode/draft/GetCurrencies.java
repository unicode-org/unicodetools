package org.unicode.draft;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;


public class GetCurrencies {

    enum Type {IN_COUNTRY, ALL}

    public static void main(String[] args) {
        final Date today = new Date();
        final CurrenciesLocalizations localizations = new CurrenciesLocalizations();
        final Set<String> modernCurrencies = new TreeSet<String>();
        for (final ULocale locale : ULocale.getAvailableLocales()) {
            final String[] availableCurrencyCodes = Currency.getAvailableCurrencyCodes(locale, today);
            if (availableCurrencyCodes == null) {
                //System.out.println(locale + "\t" + "none");
                continue;
            }
            final List<String> currencies = Arrays.asList(availableCurrencyCodes);
            //System.out.println(locale + "\t" + currencies);
            for (final String currency : availableCurrencyCodes) {
                localizations.add(currency, Currency.getInstance(currency).getSymbol(locale), Type.IN_COUNTRY);
            }
            modernCurrencies.addAll(currencies);
        }
        for (final ULocale locale : ULocale.getAvailableLocales()) {
            for (final String currency : modernCurrencies) {
                localizations.add(currency, Currency.getInstance(currency).getSymbol(locale), Type.ALL);
            }
        }

        showLine("Currency", "Root", "In-Country", "Count", "Other", "Count");

        for (final String currency : modernCurrencies) {
            final Set<String> inCountry = localizations.getSymbols(currency, Type.IN_COUNTRY);
            final Set<String> other = localizations.getSymbols(currency, Type.ALL);
            showLine(currency, Currency.getInstance(currency).getSymbol(ULocale.ROOT),
                    inCountry.toString(), ""+inCountry.size(),
                    other.toString(), ""+other.size());
        }
    }

    private static void showLine(String currency, String symbolInRoot, String symbolInCountry, String count,
            String symbolInOther, String countOther) {
        System.out.println(
                currency
                + "\t" + symbolInRoot
                + "\t" + symbolInCountry
                + "\t" + count
                + "\t" + symbolInOther
                + "\t" + countOther
                );
    }

    static class CurrenciesLocalizations {
        Map<String,CurrencyLocalizations> currencyToData = new TreeMap<String,CurrencyLocalizations>();

        public void add(String currency, String symbol, Type type) {
            if (currency.equals(symbol)) {
                return;
            }
            CurrencyLocalizations locs = currencyToData.get(currency);
            if (locs == null) {
                currencyToData.put(currency, locs = new CurrencyLocalizations());
            }
            locs.used.add(symbol);
            if (type == Type.IN_COUNTRY) {
                locs.usedInCountriesWithCurrency.add(symbol);
            }
        }

        public Set<String> getSymbols(String currency, Type type) {
            final CurrencyLocalizations data = currencyToData.get(currency);
            if (data == null) {
                return Collections.emptySet();
            }
            Set<String> set;
            if (type == Type.IN_COUNTRY) {
                set = data.usedInCountriesWithCurrency;
            } else {
                set = new TreeSet<String>(data.used);
                set.removeAll(data.usedInCountriesWithCurrency);
            }
            return set;
        }
    }

    static class CurrencyLocalizations {
        Set<String> usedInCountriesWithCurrency = new TreeSet<String>();
        Set<String> used = new TreeSet<String>();
    }
}
