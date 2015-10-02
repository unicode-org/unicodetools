package org.unicode.draft;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class TestZoneName {
    public static void main(String[] args) {
        // setup
        final Date today = new Date();
        final List<TimeZone> zones = new ArrayList();
        for (final String id : TimeZone.getAvailableIDs()) {
            zones.add(TimeZone.getTimeZone(id));
        }
        System.out.format("Number of Timezones: %d\n", zones.size());
        // get names for locale. We do it 4 times with different patterns
        final ULocale locale = ULocale.FRANCE;

        for (final String patternString : new String[] {"VVVV", "VVVV", "v", "vvvv", "V"}) {
            final DateFormat pattern = new SimpleDateFormat(patternString, locale);
            final long start = System.currentTimeMillis();
            for (final TimeZone zone : zones) {
                pattern.setTimeZone(zone);
                final String name = pattern.format(today);
                if (false) {
                    System.out.format("ID: %s,\t\tLocalized Name:%s\n", zone.getID(), name);
                }
            }
            final long end = System.currentTimeMillis();
            System.out.format("Pattern: %s,\tMilliseconds:%d\n", patternString, end-start);
            System.out.println();
        }
    }
}