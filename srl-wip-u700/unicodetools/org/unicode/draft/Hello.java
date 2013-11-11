package org.unicode.draft;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.TimeUnitFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeFilter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.TimeUnit;
import com.ibm.icu.util.TimeUnitAmount;
import com.ibm.icu.util.ULocale;


public class Hello {

    /**
     * @param args
     */
    public static void main(String[] args) {

        for (final String test : new String [] {"en", "ja", "de", "da", "ru"}) {
            for (final TimeUnit timeUnit : new TimeUnit [] {TimeUnit.YEAR, TimeUnit.MONTH, TimeUnit.WEEK, TimeUnit.DAY, TimeUnit.HOUR, TimeUnit.MINUTE, TimeUnit.SECOND}) {
                for (final int style : new int[] {TimeUnitFormat.ABBREVIATED_NAME, TimeUnitFormat.FULL_NAME}) {
                    final TimeUnitFormat format = new TimeUnitFormat(new ULocale(test), style);
                    for (final double amount : new double[]{1d, 2d}) {
                        // create time unit amount instance - a combination of Number and time unit
                        final TimeUnitAmount source = new TimeUnitAmount(amount, timeUnit);
                        System.out.print(format.format(source) + "\t\t");
                    }
                }
                System.out.println();
            }
        }
        if (true) {
            return;
        }

        checkTranslit();

        final UnicodeSet foo;
        final ULocale locale = new ULocale("fr");
        final NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
        String pattern = ((DecimalFormat)nf).toPattern();
        pattern = pattern.replace("¤", "¤¤¤");
        ((DecimalFormat)nf).applyPattern(pattern);
        final CurrencyAmount ca = new CurrencyAmount(1.99, Currency.getInstance("USD"));
        final String formatted = nf.format(ca);
        System.out.println(formatted);



        final int foo1 = UScript.getCodeFromName("Grek");
        final int foo2 = UScript.getCodeFromName("Greek");
        checkCollator(ULocale.ENGLISH);
        checkCollator(ULocale.CHINA);
        if (true) {
            return;
        }
        // TODO Auto-generated method stub
        final UnicodeSet junk = new UnicodeSet("[\\U0001F1FF {\\U0001F1E8 \\U0001F1F3} {\\U0001F1E9 \\U0001F1EA} {\\U0001F1EA \\U0001F1F8} {\\U0001F1EB \\U0001F1F7} {\\U0001F1EC \\U0001F1E7} {\\U0001F1EE \\U0001F1F9} {\\U0001F1EF \\U0001F1F5} {\\U0001F1F0 \\U0001F1F7} {\\U0001F1F7 \\U0001F1FA} {\\U0001F1FA \\U0001F1F8} ]");
        junk.toString();
        System.out.println(junk);

        final UnicodeSet emoji = new UnicodeSet("[\\U00002600 \\U00002601 \\U0001F300 \\U0001F301 \\U0001F302 \\U0001F303 \\U0001F304 \\U0001F305 \\U0001F306 \\U0001F307 \\U0001F308 \\U0001F309 \\U0001F30A \\U0001F30B \\U0001F30C \\U000026C4 \\U000026C5 \\U00002614 \\U000026A1 \\U0001F30F \\U0001F311 \\U0001F314 \\U0001F313 \\U0001F319 \\U0001F315 \\U0001F31B \\U0001F31F \\U0001F320 \\U0001F550 \\U0001F551 \\U0001F552 \\U0001F553 \\U0001F554 \\U0001F555 \\U0001F556 \\U0001F557 \\U0001F558 \\U0001F559 \\U0001F55A \\U0001F55B \\U0000231A \\U0000231B \\U000023F0 \\U000023F3 \\U00002648 \\U00002649 \\U0000264A \\U0000264B \\U0000264C \\U0000264D \\U0000264E \\U0000264F \\U00002650 \\U00002651 \\U00002652 \\U00002653 \\U000026CE \\U0001F340 \\U0001F337 \\U0001F331 \\U0001F341 \\U0001F338 \\U0001F339 \\U0001F342 \\U0001F343 \\U0001F33A \\U0001F33B \\U0001F334 \\U0001F335 \\U0001F33E \\U0001F33D \\U0001F344 \\U0001F330 \\U0001F33C \\U0001F33F \\U0001F352 \\U0001F34C \\U0001F34E \\U0001F34A \\U0001F353 \\U0001F349 \\U0001F345 \\U0001F346 \\U0001F348 \\U0001F34D \\U0001F347 \\U0001F351 \\U0001F34F \\U0001F440 \\U0001F442 \\U0001F443 \\U0001F444 \\U0001F445 \\U0001F484 \\U0001F485 \\U0001F486 \\U0001F487 \\U0001F488 \\U0001F464 \\U0001F466 \\U0001F467 \\U0001F468 \\U0001F469 \\U0001F46A \\U0001F46B \\U0001F46E \\U0001F46F \\U0001F470 \\U0001F471 \\U0001F472 \\U0001F473 \\U0001F474 \\U0001F475 \\U0001F476 \\U0001F477 \\U0001F478 \\U0001F479 \\U0001F47A \\U0001F47B \\U0001F47C \\U0001F47D \\U0001F47E \\U0001F47F \\U0001F480 \\U0001F481 \\U0001F482 \\U0001F483 \\U0001F40C \\U0001F40D \\U0001F40E \\U0001F414 \\U0001F417 \\U0001F42B \\U0001F418 \\U0001F428 \\U0001F412 \\U0001F411 \\U0001F419 \\U0001F41A \\U0001F41B \\U0001F41C \\U0001F41D \\U0001F41E \\U0001F420 \\U0001F421 \\U0001F422 \\U0001F424 \\U0001F425 \\U0001F426 \\U0001F423 \\U0001F427 \\U0001F429 \\U0001F41F \\U0001F42C \\U0001F42D \\U0001F42F \\U0001F431 \\U0001F433 \\U0001F434 \\U0001F435 \\U0001F436 \\U0001F437 \\U0001F43B \\U0001F439 \\U0001F43A \\U0001F42E \\U0001F430 \\U0001F438 \\U0001F43E \\U0001F432 \\U0001F43C \\U0001F43D \\U0000263A \\U0001F620 \\U0001F629 \\U0001F632 \\U0001F61E \\U0001F635 \\U0001F630 \\U0001F612 \\U0001F60D \\U0001F624 \\U0001F61C \\U0001F61D \\U0001F60B \\U0001F618 \\U0001F61A \\U0001F637 \\U0001F633 \\U0001F603 \\U0001F605 \\U0001F606 \\U0001F601 \\U0001F602 \\U0001F60A \\U0001F604 \\U0001F622 \\U0001F62D \\U0001F628 \\U0001F623 \\U0001F621 \\U0001F60C \\U0001F616 \\U0001F614 \\U0001F631 \\U0001F62A \\U0001F60F \\U0001F613 \\U0001F625 \\U0001F62B \\U0001F609 \\U0001F63A \\U0001F638 \\U0001F639 \\U0001F63D \\U0001F63B \\U0001F63F \\U0001F63E \\U0001F63C \\U0001F640 \\U0001F645 \\U0001F646 \\U0001F647 \\U0001F648 \\U0001F64A \\U0001F649 \\U0001F64B \\U0001F64C \\U0001F64D \\U0001F64E \\U0001F64F \\U0001F3E0 \\U0001F3E1 \\U0001F3E2 \\U0001F3E3 \\U0001F3E5 \\U0001F3E6 \\U0001F3E7 \\U0001F3E8 \\U0001F3E9 \\U0001F3EA \\U0001F3EB \\U0001F3EC \\U0001F3EF \\U0001F3F0 \\U0001F3ED \\U0001F3EE \\U00002693 \\U000026EA \\U000026F2 \\U0001F5FB \\U0001F5FC \\U0001F5FD \\U0001F5FE \\U0001F5FF \\U0001F45E \\U0001F45F \\U0001F460 \\U0001F461 \\U0001F462 \\U0001F463 \\U0001F453 \\U0001F455 \\U0001F456 \\U0001F451 \\U0001F454 \\U0001F452 \\U0001F457 \\U0001F458 \\U0001F459 \\U0001F45A \\U0001F45B \\U0001F45C \\U0001F45D \\U0001F4B0 \\U0001F4B1 \\U0001F4B9 \\U0001F4B2 \\U0001F4B3 \\U0001F4B4 \\U0001F4B5 \\U0001F4B8 \\U0001F1E6 \\U0001F1E7 \\U0001F1E8 \\U0001F1E9 \\U0001F1EA \\U0001F1EB \\U0001F1EC \\U0001F1ED \\U0001F1EE \\U0001F1EF \\U0001F1F0 \\U0001F1F1 \\U0001F1F2 \\U0001F1F3 \\U0001F1F4 \\U0001F1F5 \\U0001F1F6 \\U0001F1F7 \\U0001F1F8 \\U0001F1F9 \\U0001F1FA \\U0001F1FB \\U0001F1FC \\U0001F1FD \\U0001F1FE \\U0001F1FF {\\U0001F1E8 \\U0001F1F3} {\\U0001F1E9 \\U0001F1EA} {\\U0001F1EA \\U0001F1F8} {\\U0001F1EB \\U0001F1F7} {\\U0001F1EC \\U0001F1E7} {\\U0001F1EE \\U0001F1F9} {\\U0001F1EF \\U0001F1F5} {\\U0001F1F0 \\U0001F1F7} {\\U0001F1F7 \\U0001F1FA} {\\U0001F1FA \\U0001F1F8} \\U0001F525 \\U0001F526 \\U0001F527 \\U0001F528 \\U0001F529 \\U0001F52A \\U0001F52B \\U0001F52E \\U0001F52F \\U0001F530 \\U0001F531 \\U0001F489 \\U0001F48A \\U0001F170 \\U0001F171 \\U0001F18E \\U0001F17E \\U0001F17F \\U0001F380 \\U0001F381 \\U0001F382 \\U0001F384 \\U0001F385 \\U0001F38C \\U0001F386 \\U0001F388 \\U0001F389 \\U0001F38D \\U0001F38E \\U0001F393 \\U0001F392 \\U0001F38F \\U0001F387 \\U0001F390 \\U0001F383 \\U0001F38A \\U0001F38B \\U0001F391 \\U0000260E \\U0001F4DF \\U0001F4DE \\U0001F4F1 \\U0001F4F2 \\U0001F4DD \\U0001F4E0 \\U0001F4E8 \\U0001F4E9 \\U0001F4EA \\U0001F4EB \\U0001F4EE \\U0001F4F0 \\U0001F4E2 \\U0001F4E3 \\U0001F4E1 \\U0001F4E4 \\U0001F4E5 \\U0001F4E6 \\U0001F4E7 \\U0001F520 \\U0001F521 \\U0001F522 \\U0001F523 \\U0001F524 \\U00002702 \\U00002709 \\U0000270F \\U00002712 \\U00002714 \\U00002716 \\U0001F4BA \\U0001F4BB \\U0001F4CE \\U0001F4BC \\U0001F4BD \\U0001F4BE \\U0001F4BF \\U0001F4C0 \\U0001F4CD \\U0001F4C3 \\U0001F4C4 \\U0001F4C5 \\U0001F4C1 \\U0001F4C2 \\U0001F4D3 \\U0001F4D6 \\U0001F4D4 \\U0001F4D5 \\U0001F4D7 \\U0001F4D8 \\U0001F4D9 \\U0001F4DA \\U0001F4DB \\U0001F4DC \\U0001F4CB \\U0001F4C6 \\U0001F4CA \\U0001F4C8 \\U0001F4C9 \\U0001F4C7 \\U0001F4CC \\U0001F4D2 \\U0001F4CF \\U0001F4D0 \\U0001F4D1 \\U000026F3 \\U000026F5 \\U000026FA \\U000026FD \\U0001F3BD \\U000026BE \\U0001F3BE \\U000026BD \\U0001F3BF \\U0001F3C0 \\U0001F3C1 \\U0001F3C2 \\U0001F3C3 \\U0001F3C4 \\U0001F3C6 \\U0001F3C8 \\U0001F3CA \\U000024C2 \\U0001F683 \\U0001F687 \\U0001F684 \\U0001F685 \\U0001F697 \\U0001F699 \\U0001F68C \\U0001F68F \\U0001F6A2 \\U0001F689 \\U0001F680 \\U0001F6A4 \\U0001F695 \\U0001F69A \\U0001F692 \\U0001F691 \\U0001F693 \\U0001F6A5 \\U0001F6A7 \\U0001F6A8 \\U00002668 \\U00002708 \\U0001F3A0 \\U0001F3A1 \\U0001F3A2 \\U0001F3A3 \\U0001F3A4 \\U0001F3A5 \\U0001F3A6 \\U0001F3A7 \\U0001F3A8 \\U0001F3A9 \\U0001F3AA \\U0001F3AB \\U0001F3AC \\U0001F3AD \\U0001F004 \\U0001F3AE \\U0001F3AF \\U0001F3B0 \\U0001F3B1 \\U0001F3B2 \\U0001F3B3 \\U0001F3B4 \\U0001F0CF \\U0001F3B5 \\U0001F3B6 \\U0001F3B7 \\U0001F3B8 \\U0001F3B9 \\U0001F3BA \\U0001F3BB \\U0001F3BC \\U0000303D \\U0001F4F7 \\U0001F4F9 \\U0001F4FA \\U0001F4FB \\U0001F4FC \\U0001F48B \\U0001F48C \\U0001F48D \\U0001F48E \\U0001F48F \\U0001F490 \\U0001F491 \\U0001F492 \\U000000A9 \\U000000AE \\U00002122 \\U00002139 \\U0001F51E {\\U00000023 \\U000020E3} {\\U00000031 \\U000020E3} {\\U00000032 \\U000020E3} {\\U00000033 \\U000020E3} {\\U00000034 \\U000020E3} {\\U00000035 \\U000020E3} {\\U00000036 \\U000020E3} {\\U00000037 \\U000020E3} {\\U00000038 \\U000020E3} {\\U00000039 \\U000020E3} {\\U00000030 \\U000020E3} \\U0001F51F \\U0001F4F6 \\U0001F4F3 \\U0001F4F4 \\U0001F354 \\U0001F359 \\U0001F370 \\U0001F35C \\U0001F35E \\U0001F373 \\U0001F366 \\U0001F35F \\U0001F361 \\U0001F358 \\U0001F35A \\U0001F35D \\U0001F35B \\U0001F362 \\U0001F363 \\U0001F371 \\U0001F372 \\U0001F367 \\U0001F356 \\U0001F365 \\U0001F360 \\U0001F355 \\U0001F357 \\U0001F368 \\U0001F369 \\U0001F36A \\U0001F36B \\U0001F36C \\U0001F36D \\U0001F36E \\U0001F36F \\U0001F364 \\U0001F374 \\U00002615 \\U0001F378 \\U0001F37A \\U0001F375 \\U0001F376 \\U0001F377 \\U0001F37B \\U0001F379 \\U00002194 \\U00002195 \\U00002197 \\U00002198 \\U00002196 \\U00002199 \\U00002B06 \\U00002B07 \\U00002B05 \\U000027A1 \\U00002934 \\U00002935 \\U000025B6 \\U000025C0 \\U000023E9 \\U000023EA \\U000023EB \\U000023EC \\U0001F53A \\U0001F53B \\U0001F53C \\U0001F53D \\U00002B55 \\U0000274C \\U0000274E \\U00002757 \\U00002753 \\U00002754 \\U00002755 \\U00003030 \\U0000203C \\U00002049 \\U000027B0 \\U000027BF \\U00002764 \\U0001F493 \\U0001F494 \\U0001F495 \\U0001F496 \\U0001F497 \\U0001F498 \\U0001F499 \\U0001F49A \\U0001F49B \\U0001F49C \\U0001F49D \\U0001F49E \\U0001F49F \\U00002665 \\U00002660 \\U00002666 \\U00002663 \\U0000267B \\U0000267F \\U000026A0 \\U000026D4 \\U0001F6AC \\U0001F6AD \\U0001F6A9 \\U0001F6B2 \\U0001F6B6 \\U0001F6B9 \\U0001F6BA \\U0001F6C0 \\U0001F6BB \\U0001F6BD \\U0001F6BE \\U0001F6BC \\U0001F6AA \\U0001F6AB \\U0001F191 \\U0001F192 \\U0001F193 \\U0001F194 \\U0001F195 \\U0001F196 \\U0001F197 \\U0001F198 \\U0001F199 \\U0001F19A \\U0001F201 \\U0001F202 \\U0001F21A \\U0001F22F \\U0001F232 \\U0001F233 \\U0001F234 \\U0001F235 \\U0001F236 \\U0001F237 \\U0001F238 \\U0001F239 \\U0001F23A \\U00003299 \\U00003297 \\U0001F250 \\U0001F251 \\U00002795 \\U00002796 \\U00002797 \\U0001F4A0 \\U0001F4A1 \\U0001F4A2 \\U0001F4A3 \\U0001F4A4 \\U0001F4A5 \\U0001F4A6 \\U0001F4A7 \\U0001F4A8 \\U0001F4A9 \\U0001F4AA \\U0001F4AB \\U0001F4AC \\U00002728 \\U00002734 \\U00002733 \\U00002744 \\U00002747 \\U000026AA \\U000026AB \\U00002B50 \\U0001F534 \\U0001F535 \\U0001F532 \\U0001F533 \\U000025AB \\U000025AA \\U000025FD \\U000025FE \\U000025FB \\U000025FC \\U0001F536 \\U0001F537 \\U0001F538 \\U0001F539 \\U00002B1B \\U00002B1C \\U0001F4AE \\U0001F4AF \\U000021A9 \\U000021AA \\U0001F503 \\U0001F50A \\U0001F50B \\U0001F50C \\U0001F50D \\U0001F50E \\U0001F512 \\U0001F513 \\U0001F50F \\U0001F510 \\U0001F511 \\U0001F514 \\U0001F518 \\U0001F516 \\U0001F517 \\U00002611 \\U0001F519 \\U0001F51A \\U0001F51B \\U0001F51C \\U0001F51D \\U00002003 \\U00002002 \\U00002005 \\U00002705 \\U0000270A \\U0000270B \\U0000270C \\U0001F44A \\U0001F44D \\U0001F446 \\U0001F447 \\U0001F448 \\U0001F449 \\U0001F44B \\U0001F44F \\U0001F44C \\U0001F44E \\U0001F450 \\U0000261D ]");
        System.out.println(emoji);
        final UnicodeSet s = new UnicodeSet("[:lb=SY:]").complement().complement();
        System.out.println("hi " + Arrays.asList(args) + ", " + s);
        final DateFormat df = DateFormat.getPatternInstance(DateFormat.HOUR_MINUTE_GENERIC_TZ, ULocale.FRANCE);
        System.out.println(df.format(new Date()));

    }

    private static void checkTranslit() {
        final String[] rules = {
                ":: NFKD;",
                ":: [:Latin:] NFKD;",
                ":: [[:Mn:][:Me:]] remove;",
                ":: Latin-Greek;",
                ":: NFKD;\n" +
                        ":: [[:Mn:][:Me:]] remove;\n" +
                        ":: NFC;"
        };
        for (final String rule : rules) {
            System.out.println("Rules:\n" + rule);
            final Transliterator trans = Transliterator.createFromRules("temp", rule, Transliterator.FORWARD);
            final UnicodeSet source = getSourceSet(trans);
            final UnicodeSet target = trans.getTargetSet();
            System.out.println("Source:\t" + source.toPattern(false));
            System.out.println("Target:\t" + target.toPattern(false));
        }
    }

    static UnicodeSet getSourceSet(Transliterator t) {
        final Transliterator[] subTransliterators = t.getElements();
        if (subTransliterators.length == 1 && subTransliterators[0] == t) {
            return t.getSourceSet();
        } else {
            final UnicodeSet sources = new UnicodeSet();
            for (final Transliterator s : subTransliterators) {
                final UnicodeSet source = getSourceSet(s);
                sources.addAll(source);
            }
            // TODO: if s1 produces ABC, what about chaining?
            final UnicodeFilter filter = t.getFilter();
            if (filter != null) {
                sources.retainAll((UnicodeSet)filter); // TODO fix for arbitrary filters
            }
            return sources;
        }
    }

    private static void checkCollator(ULocale locale) {
        System.out.println("Locale:\t" + locale);
        final NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);
        nf.setGroupingUsed(true);
        Collator c = Collator.getInstance(locale);
        final int iterations = 10000000;

        long start = System.nanoTime();
        for (int i = 0; i < iterations; ++i) {
            c = Collator.getInstance(locale);
        }
        long end = System.nanoTime();
        System.out.println(".getInstance nanos: " + nf.format((end-start)/(double)iterations) + " ns");

        Collator d;
        try {
            d = (Collator) c.clone();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        start = System.nanoTime();
        for (int i = 0; i < iterations; ++i) {
            try {
                d = (Collator) c.clone();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
        end = System.nanoTime();
        System.out.println(".clone nanos: " + nf.format((end-start)/(double)iterations) + " ns");

        try {
            start = System.nanoTime();
            for (int i = 0; i < iterations; ++i) {
                d = (Collator) c.clone();
            }
            end = System.nanoTime();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println(".clone (no try) nanos: " + nf.format((end-start)/(double)iterations) + " ns");

    }

    DateFormat foo;
    Locale foo1;
    ULocale foo2;
    Calendar foo3;

    //	public final static DateFormat getPatternInstance(String pattern)
    //	{return null;}
    //	public final static DateFormat getPatternInstance(String pattern, Locale locale)
    //	{return null;}
    //	public final static DateFormat getPatternInstance(String pattern, ULocale locale)
    //	{return null;}
    //	public final static DateFormat getPatternInstance(Calendar calendar, String pattern, Locale locale)
    //	{return null;}
    //	public final static DateFormat getPatternInstance(Calendar calendar, pattern, ULocale locale)
    //	{return null;}

    public static final String
    MINUTE_SECOND = "m:ss",
    HOUR_MINUTE = "H:mm",
    HOUR_MINUTE_SECOND = "H:mm:ss",
    HOUR12_MINUTE = "h:mm",
    HOUR12_MINUTE_SECOND = "H:mm:ss",

    DAY = "d",
    MONTH = "L",
    ABBR_MONTH = "LLL",
    YEAR = "yyyy",

    MONTH_DAY = "MMMM d",
    ABBR_MONTH_DAY = "MMM d",
    NUM_MONTH_DAY = "M/d",
    WEEKDAY_MONTH_DAY = "E MMMM d",
    WEEKDAY_ABBR_MONTH_DAY = "E MMM d",
    WEEKDAY_NUM_MONTH_DAY ="E, M-d",

    MONTH_YEAR = "MMMM yyyy",
    NUM_MONTH_YEAR = "M/yyyy",
    ABBR_MONTH_YEAR = "MMM yyyy",
    WEEKDAY_NUM_MONTH_DAY_YEAR = "EEE, M/d/yyyy",
    WEEKDAY_ABBR_MONTH_DAY_YEAR = "EEE, MMM d yyyy",

    QUARTER_YEAR = "QQQ yyyy",
    ABBR_QUARTER_YEAR = "Q yyyy";

    public static final class CurrencyFilter {
        public static CurrencyFilter onRegion(String region) { return new CurrencyFilter(); }
        public static CurrencyFilter onCurrency(String currency) { return new CurrencyFilter(); }
        public static CurrencyFilter onFromDate(Date date) { return new CurrencyFilter(); }
        public static CurrencyFilter onToDate(Date date) { return new CurrencyFilter(); }

        public CurrencyFilter withRegion(String region) { return this; }
        public CurrencyFilter withCurrency(String currency) { return this; }
        public CurrencyFilter withFromDate(Date date) { return this; }
        public CurrencyFilter withToDate(Date date) { return this; }
    }

}
