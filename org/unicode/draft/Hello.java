package org.unicode.draft;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ULocale;


public class Hello {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		UnicodeSet s = new UnicodeSet("[:lb=SY:]").complement().complement();
		System.out.println("hi " + Arrays.asList(args) + ", " + s);
		DateFormat df = DateFormat.getPatternInstance(DateFormat.HOUR_MINUTE_GENERIC_TZ, ULocale.FRANCE);
		System.out.println(df.format(new Date()));
		
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
