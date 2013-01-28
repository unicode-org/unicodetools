package org.unicode.draft;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.unicode.cldr.util.Timer;


public class TimeEntry {
  public static void main(String[] args) {
    Random rand = new Random(0);
    Map<Long,Long> samples = new HashMap();
    for (int i = 0 ; i < 1000000; ++i) {
      samples.put(rand.nextLong(), rand.nextLong());
    }
    Timer timer = new Timer();
    for (Long key : samples.keySet()) {
      Long value = samples.get(key);
    }
    timer.stop();
    System.out.println(timer);

    Timer timer2 = new Timer();
    for (Entry<Long,Long> entry : samples.entrySet()) {
      Long key = entry.getKey();
      Long value = entry.getValue();
    }
    timer2.stop();
    System.out.println(timer2.toString(timer));
  }
}
