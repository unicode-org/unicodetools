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
		Date today = new Date();
		List<TimeZone> zones = new ArrayList();
		for (String id : TimeZone.getAvailableIDs()) {
			zones.add(TimeZone.getTimeZone(id));
		}
		System.out.format("Number of Timezones: %d\r\n", zones.size());
		// get names for locale. We do it 4 times with different patterns
		ULocale locale = ULocale.FRANCE;
		
		for (String patternString : new String[] {"VVVV", "VVVV", "v", "vvvv", "V"}) {
			DateFormat pattern = new SimpleDateFormat(patternString, locale);
			long start = System.currentTimeMillis();
			for (TimeZone zone : zones) {
				pattern.setTimeZone(zone);
				String name = pattern.format(today);
				if (false) System.out.format("ID: %s,\t\tLocalized Name:%s\r\n", zone.getID(), name);
			}
			long end = System.currentTimeMillis();
			System.out.format("Pattern: %s,\tMilliseconds:%d\r\n", patternString, end-start);
			System.out.println();
		}
	}
}