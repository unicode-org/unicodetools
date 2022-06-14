package org.unicode.draft;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import org.unicode.cldr.util.Timer;

public class TimeEntry {
    public static void main(String[] args) {
        final Random rand = new Random(0);
        final Map<Long, Long> samples = new HashMap();
        for (int i = 0; i < 1000000; ++i) {
            samples.put(rand.nextLong(), rand.nextLong());
        }
        final Timer timer = new Timer();
        for (final Long key : samples.keySet()) {
            final Long value = samples.get(key);
        }
        timer.stop();
        System.out.println(timer);

        final Timer timer2 = new Timer();
        for (final Entry<Long, Long> entry : samples.entrySet()) {
            final Long key = entry.getKey();
            final Long value = entry.getValue();
        }
        timer2.stop();
        System.out.println(timer2.toString(timer));
    }
}
