package org.unicode.draft;

import com.ibm.icu.util.ULocale;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.unicode.unittest.TestFmwkMinusMinus;

public class TestCalendar extends TestFmwkMinusMinus {

    private static final int INCREMENT = 4 * 60 * 59 * 1000;

    static final long future = new Date(2015 - 1900, 12 - 1, 1).getTime();
    static final long past = new Date(1000 - 1900, 1 - 1, 1).getTime();
    static final java.util.TimeZone javaGMT = java.util.TimeZone.getTimeZone("GMT");

    @Disabled("Broken")
    @Test
    public void TestDateParsing() throws java.text.ParseException {
        final ULocale locale = new ULocale("en_US");
        final com.ibm.icu.util.TimeZone gmt = com.ibm.icu.util.TimeZone.getTimeZone("GMT");
        final com.ibm.icu.util.Calendar newCalendar =
                com.ibm.icu.util.Calendar.getInstance(gmt, locale);
        final com.ibm.icu.util.Calendar otherCalendar =
                com.ibm.icu.util.Calendar.getInstance(gmt, locale);

        for (final boolean leniency : new boolean[] {true, false}) {
            final String inputText = "1/2/3";
            final com.ibm.icu.text.SimpleDateFormat dateFormat =
                    (com.ibm.icu.text.SimpleDateFormat)
                            com.ibm.icu.text.DateFormat.getDateInstance(
                                    com.ibm.icu.text.DateFormat.SHORT, locale);
            dateFormat.setTimeZone(gmt);
            logln("inputPattern: " + inputText + ", dateFormat pattern: " + dateFormat.toPattern());
            // dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            dateFormat.setLenient(leniency);

            // look at the internal calls
            final ParsePosition pos = new ParsePosition(0);
            newCalendar.clear();
            dateFormat.parse(inputText, newCalendar, pos);
            final long time = newCalendar.getTimeInMillis();
            logln(show(newCalendar));

            otherCalendar.setTimeInMillis(time);
            logln(show(newCalendar));

            final java.util.Date date = dateFormat.parse(inputText);
            logln(show(date));
            final long dateMillis = date.getTime();
            assertEquals(
                    "long calendar parse for " + time + " ?= " + dateMillis,
                    getDaysFromMillis(time),
                    getDaysFromMillis(dateMillis));

            // offset for java date oddities
            checkDate(3, 1, 2, date);
        }
    }

    @Disabled("Broken")
    @Test
    public void TestShorterParsing() throws ParseException {
        final String dateString = "1/2/3";
        final Locale usLocale = new Locale("en", "US");
        final com.ibm.icu.text.DateFormat dateFormat =
                com.ibm.icu.text.DateFormat.getDateInstance(
                        com.ibm.icu.text.DateFormat.SHORT, usLocale);
        dateFormat.setLenient(false);

        final com.ibm.icu.util.TimeZone timeZone =
                com.ibm.icu.util.TimeZone.getTimeZone(java.util.TimeZone.getDefault().getID());
        dateFormat.setTimeZone(timeZone);

        final java.util.Date date = dateFormat.parse(dateString);
        assertEquals("year", 3, date.getYear() + 1900);
        assertEquals("month", 1, date.getMonth() + 1);
        assertEquals("day", 2, date.getDay());
    }

    static final com.ibm.icu.util.TimeZone GMT = com.ibm.icu.util.TimeZone.getTimeZone("GMT");

    @Test
    public void TestLongerParsing() throws ParseException {
        final Locale usLocale = new Locale("en", "US");
        final com.ibm.icu.util.Calendar newCalendar =
                com.ibm.icu.util.Calendar.getInstance(GMT, usLocale);
        final ParsePosition parsePosition = new ParsePosition(0);

        final String dateString = "1/2/3";
        final com.ibm.icu.text.DateFormat dateFormat =
                com.ibm.icu.text.DateFormat.getDateInstance(
                        com.ibm.icu.text.DateFormat.SHORT, usLocale);
        // dateFormat.setLenient(false);

        newCalendar.clear();
        parsePosition.setIndex(0);
        dateFormat.parse(dateString, newCalendar, parsePosition);
        assertEquals("year", 3, newCalendar.get(com.ibm.icu.util.Calendar.YEAR));
        assertEquals("month", 1, newCalendar.get(com.ibm.icu.util.Calendar.MONTH) + 1);
        assertEquals("day", 2, newCalendar.get(com.ibm.icu.util.Calendar.DAY_OF_MONTH));
    }

    @Disabled("Broken")
    @Test
    public void TestDate() throws ParseException {
        final long[][] tests = {
            {-62072611200000L, 3, 1, 2},
        };
        for (final long[] test : tests) {
            final java.util.Date date = new Date(test[0]);
            logln(date.toGMTString());
            logln(show(date));
            checkDate(test[1], test[2], test[3], date);
        }
    }

    private void checkDate(
            long expectedYear, long expectedMonth, long expectedDay, java.util.Date date) {
        assertEquals("check year", expectedYear, date.getYear() + 1900);
        assertEquals("check month", expectedMonth, date.getMonth() + 1);
        assertEquals("check dayOM", expectedDay, date.getDate());
    }

    private String show(Date date) {
        return "year: "
                + (date.getYear() + 1900)
                + ", month: "
                + (date.getMonth() + 1)
                + ", dayOfMonth: "
                + date.getDate()
                + ", hour: "
                + date.getHours()
                + ", minute: "
                + date.getMinutes()
                + ", second: "
                + date.getSeconds();
    }

    @Disabled("Broken")
    @Test
    public void TestJavaDateRoundtrip() {

        int lastYear = 9999;
        int errors = 0;

        for (long millis = future;
                millis > past;
                millis -= INCREMENT) { // by just less than a day, back from epoch

            // check that java Date roundtrips

            final Date javaDate = new Date(millis);

            final int dateYear = javaDate.getYear();
            final int dateMonth = javaDate.getMonth();
            final int dateDayOfMonth = javaDate.getDate();
            final int dateHour = javaDate.getHours();
            final int dateMinute = javaDate.getMinutes();
            final int dateSecond = javaDate.getSeconds();
            final Date javaDate2 =
                    new Date(dateYear, dateMonth, dateDayOfMonth, dateHour, dateMinute, dateSecond);

            final long javaDateMillis = javaDate2.getTime();

            errors +=
                    assertEquals(
                                    "Date source vs java millis for "
                                            + getDaysFromMillis(millis)
                                            + "?="
                                            + getDaysFromMillis(javaDateMillis),
                                    millis,
                                    javaDateMillis)
                            ? 0
                            : 1;
            if (errors > 10) {
                break;
            }

            if (dateYear < lastYear) {
                logln("Year: " + dateYear + 1900);
                lastYear = dateYear;
            }
        }
    }

    @Test
    public void TestJavaCalendarRoundtrip() {
        final Locale javaLocale = new Locale("en", "US");
        final java.util.Calendar javaCal = java.util.Calendar.getInstance(javaGMT, javaLocale);
        final java.util.Calendar javaCal2 = java.util.Calendar.getInstance(javaGMT, javaLocale);

        int lastYear = 9999;
        final boolean hack = true;
        int errors = 0;

        for (long millis = future;
                millis > past;
                millis -= INCREMENT) { // by just less than a day, back from epoch

            // check that java calendar roundtrips

            javaCal.setTimeInMillis(millis);
            final int era = javaCal.get(java.util.Calendar.ERA);
            final int year = javaCal.get(java.util.Calendar.YEAR);
            final int month = javaCal.get(java.util.Calendar.MONTH);
            final int dayOfMonth = javaCal.get(java.util.Calendar.DAY_OF_MONTH);
            final int hour = javaCal.get(java.util.Calendar.HOUR_OF_DAY);
            final int minute = javaCal.get(java.util.Calendar.MINUTE);
            final int second = javaCal.get(java.util.Calendar.SECOND);
            final int millisecond = javaCal.get(java.util.Calendar.MILLISECOND);

            javaCal2.clear();
            assertEquals("Timezone after clear", javaGMT, javaCal2.getTimeZone());
            javaCal2.setTimeZone(javaGMT);
            javaCal2.set(java.util.Calendar.ERA, era);
            javaCal2.set(java.util.Calendar.YEAR, year);
            javaCal2.set(java.util.Calendar.MONTH, month);
            javaCal2.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
            javaCal2.set(java.util.Calendar.HOUR_OF_DAY, hour);
            javaCal2.set(java.util.Calendar.MINUTE, minute);
            javaCal2.set(java.util.Calendar.SECOND, second);
            javaCal2.set(java.util.Calendar.MILLISECOND, millisecond);
            final long javaMillis = javaCal2.getTimeInMillis();

            errors +=
                    assertEquals(
                                    "Calendar source vs java millis for " + millis,
                                    getDaysFromMillis(millis),
                                    getDaysFromMillis(javaMillis))
                            ? 1
                            : 0;
            if (errors > 10) {
                break;
            }

            if (year < lastYear) {
                logln(era + ";" + year);
                lastYear = year;
            }
        }
    }

    @Test
    public void TestIcuCalenderRoundrip() {
        final ULocale icuLocale = new ULocale("en_US");
        final com.ibm.icu.util.TimeZone icuGMT = com.ibm.icu.util.TimeZone.getTimeZone("GMT");
        final com.ibm.icu.util.Calendar icuCal =
                com.ibm.icu.util.Calendar.getInstance(icuGMT, icuLocale);
        final com.ibm.icu.util.Calendar icuCal2 =
                com.ibm.icu.util.Calendar.getInstance(icuGMT, icuLocale);

        int lastYear = 9999;

        final boolean hack = true;
        int errors = 0;

        for (long millis = future;
                millis > past;
                millis -= INCREMENT) { // by just less than a day, back from epoch

            // check that ICU calendar roundtrips

            icuCal.setTimeInMillis(millis);
            final int era2 = icuCal.get(com.ibm.icu.util.Calendar.ERA);
            final int year2 = icuCal.get(com.ibm.icu.util.Calendar.YEAR);
            final int month2 = icuCal.get(com.ibm.icu.util.Calendar.MONTH);
            final int dayOfMonth2 = icuCal.get(com.ibm.icu.util.Calendar.DAY_OF_MONTH);
            final int hour2 = icuCal.get(com.ibm.icu.util.Calendar.HOUR_OF_DAY);
            final int minute2 = icuCal.get(com.ibm.icu.util.Calendar.MINUTE);
            final int second2 = icuCal.get(com.ibm.icu.util.Calendar.SECOND);
            final int millisecond2 = icuCal.get(com.ibm.icu.util.Calendar.MILLISECOND);

            // assertEquals("calendar fields", era, era2, year, year2, month, month2, dayOfMonth,
            // dayOfMonth2, hour, hour2, minute, minute2, second, second2, millisecond,
            // millisecond2);

            icuCal2.clear();
            icuCal2.set(com.ibm.icu.util.Calendar.ERA, era2);
            icuCal2.set(com.ibm.icu.util.Calendar.YEAR, year2);
            icuCal2.set(com.ibm.icu.util.Calendar.MONTH, month2);
            icuCal2.set(com.ibm.icu.util.Calendar.DAY_OF_MONTH, dayOfMonth2);
            icuCal2.set(com.ibm.icu.util.Calendar.HOUR_OF_DAY, hour2);
            icuCal2.set(com.ibm.icu.util.Calendar.MINUTE, minute2);
            icuCal2.set(com.ibm.icu.util.Calendar.SECOND, second2);
            icuCal2.set(com.ibm.icu.util.Calendar.MILLISECOND, millisecond2);
            final long icuMillis = icuCal.getTimeInMillis();

            errors +=
                    assertEquals(
                                    "source vs icu millis for " + millis,
                                    getDaysFromMillis(millis),
                                    getDaysFromMillis(icuMillis))
                            ? 1
                            : 0;
            if (errors > 10) {
                break;
            }

            if (year2 < lastYear) {
                logln(era2 + ";" + year2);
                lastYear = year2;
            }
        }
    }

    private String show(com.ibm.icu.util.Calendar newCalendar) {
        return "era: "
                + newCalendar.get(com.ibm.icu.util.Calendar.ERA)
                + ", year: "
                + newCalendar.get(com.ibm.icu.util.Calendar.YEAR)
                + ", month: "
                + (newCalendar.get(com.ibm.icu.util.Calendar.MONTH) + 1)
                + ", dayOfMonth: "
                + newCalendar.get(com.ibm.icu.util.Calendar.DAY_OF_MONTH)
                + ", hour: "
                + newCalendar.get(com.ibm.icu.util.Calendar.HOUR_OF_DAY)
                + ", minute: "
                + newCalendar.get(com.ibm.icu.util.Calendar.MINUTE)
                + ", second: "
                + newCalendar.get(com.ibm.icu.util.Calendar.SECOND);
    }

    private String getDaysFromMillis(long millis) {
        return millis / (24 * 60 * 60 * 1000.0) + "d";
    }

    public <T> int assertEquals(String message, T... pairs) {
        return ASSERT_EQUALS.assertRelation(message, pairs);
    }

    AssertRelation<Object> ASSERT_EQUALS =
            new AssertRelation<Object>() {
                @Override
                public int argsConsumed() {
                    return 2;
                }

                @Override
                public boolean is(int start, Object... items) {
                    return items[start].equals(items[start + 1]);
                }
            };

    public abstract class AssertRelation<T> {
        abstract boolean is(int start, T... items);

        abstract int argsConsumed();

        public int assertRelation(String message, T... items) {
            final int count = argsConsumed();
            int errors = 0;
            for (int i = 0; i < items.length; i += count) {
                if (is(i, items)) {
                    logln(message + Arrays.asList(items).subList(i, i + count));
                } else {
                    errln(message + Arrays.asList(items).subList(i, i + count));
                    ++errors;
                }
            }
            return errors;
        }
    }
}
