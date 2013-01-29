package org.unicode.draft;
import java.text.Format;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.DurationFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class MessageFormatCheck extends TestFmwk {
    private static final long
    SECOND = 1000L,
    MINUTE = 60*SECOND,
    HOUR = 60*MINUTE,
    DAY = 24*HOUR,
    MONTH = 31*DAY,
    YEAR = 12*DAY;
    private MessageFormat format;
    private HashMap args;

    public static void main(String[] args) {
        new MessageFormatCheck().run(args);
    }

    @Override
    protected void init() throws Exception {
        format = new MessageFormat("The directory {directory} contains {file_count,number,#,##0.0}KB.");
        args = new HashMap();
        args.put("directory", "foo/bar/");
        args.put("file_count", 3.4567d);
    }

    public void TestDuration() {
        final DurationFormat durationFormat = DurationFormat.getInstance(ULocale.FRANCE);
        final long now = new Date().getTime();
        for (final long duration : new Long[]{2L, 2*SECOND, 2*MINUTE, 2*HOUR, 2*DAY, 2*MONTH, 2*YEAR}) {
            System.out.println();
            System.out.println("formatDurationFrom: " + durationFormat.formatDurationFrom(duration, now));
            System.out.println("formatDurationFromNowTo: " + durationFormat.formatDurationFromNowTo(new Date(now+duration)));
            System.out.println("formatDurationFromNow: " + durationFormat.formatDurationFromNow(duration));
            System.out.println("format: " + durationFormat.format(duration));
        }
    }

    public void TestSettingFormats() {
        final MessageFormat format2 = new MessageFormat("The number {0} is formatted as {0,number,#,##0.0}.");
        assertEquals("Resetting format", "The number 1.234 is formatted as 1.2.", format2.format(new Object[]{new Double(1.2345)}));
    }

    public void TestNamedArguments() {
        assertEquals("Basic test", "The directory foo/bar/ contains 3.5KB.", format.format(args));
        // the following failed on ICU 3.8
        format.setFormatByArgumentName("file_count", NumberFormat.getIntegerInstance(ULocale.FRENCH));
        assertEquals("Resetting format", "The directory foo/bar/ contains 3KB.", format.format(args));
    }

    public void TestGetFormats() {
        final Format[] formats = format.getFormatsByArgumentIndex();
        System.out.println(Arrays.asList(formats));
    }

    public void TestNicerSyntax() {
        final MessageFormat format2 = new MessageFormat("The number {0} is formatted as {0,number,#,##0.0} with {1}.");
        assertEquals("Resetting format", "The number 1.234 is formatted as 1.2.", format2.format(1.2345, "abcd"));
        assertEquals("Resetting format", "The number 1.234 is formatted as 1.2.", format2.format(new Object[]{1.2345, "abcd"}));
        final Entry foo;

        // with named entries
        format = new MessageFormat("The directory {directory} contains {file_count,number,#,##0.0}KB.");
        //format.format(new Args().add("directory", "foo/bar/").add("file_count", 3.4567d));

        MessageFormat.format("The directory {0} contains {1,number,#,##0.0}KB.", "foo/bar", 3.145);
    }


    /*
Problems:

1. The following methods should be deprecated: they will fail if the string is localized and elements are rearranged.
  public Format[] getFormats() {
  public void setFormat(int formatElementIndex, Format newFormat)

2. The following fail silently.
  public void setFormatByArgumentIndex(int argumentIndex, Format newFormat) {
  public void setFormatByArgumentName(String argumentName, Format newFormat) {
That may be ok if the translator eliminated an argument that was somehow unnecessary,
but there should at least be some way to find out what is happening.

3. The following throws an exception if there are named arguments.
    public Format[] getFormatsByArgumentIndex() {
Fine, but that means that there is no way to get a format for a named argument.

To address #2 and #3 I suggest having
  List getArgumentNames() // returns a list of names that occur in the format. Numeric arguments are returned as strings (eg "3")
  getFormatByArgumentName(String name) // returns the first format with a matching name. Numeric arguments are passed by string (eg "3")

4. The way that namedArguments are implemented, the APIs are inconsistent. Sometimes "1" is legal as a named argument, and sometimes not.
That is, there are a bunch of tests like:
        if (!argumentNamesAreNumeric) {
            throw new IllegalArgumentException(
                    "This method is not available in MessageFormat objects " +
                    "that use alphanumeric argument names.");
        }

There are also some awkward pieces of code like:
        for (int j = 0; j <= maxOffset; j++) {
            if (Integer.parseInt(argumentNames[j]) == argumentIndex) {
                formats[j] = newFormat;
            }
        }
(It would be better to change have argumentString = String.valueOf(argumentIndex), and not parse on every iteration.)

I think we need a consistent model of how this is to work. My suggestion is:
  A. {0,xxx} and {count,xxx} are treated identically: "0" and "count" are just names.
  B. When formatting, etc, an array of objects is treated logically just as if it were a Map, eg ["abc", "def"...] is treated as if it were {"0"="abc", "1"="def"...}
  C. Remove the checks on argumentNamesAreNumeric, except where the return parameter doesn't make sense, namely:
    public Format[] getFormatsByArgumentIndex() {
    public Object[] parse(String source, ParsePosition pos) {

5. Ugly Syntax. MessageFormat.format currently has to take arguments like this:
      format2.format(new Object[]{1.2345, "abcd"})
If we add the following method (under a flag), then the format can be cleaner, like this:
      format2.format(1.2345, "abcd")

    public final String format(Object...items) {
        return super.format(items);
    }

6. Ugly Syntax#2. With named arguments, it's worse:
    HashMap args = new HashMap();
    args.put("directory", "foo/bar/");
    args.put("file_count", 3.4567d);
    format.format(args)

I've been playing around with ways to make this more palatable. Perhaps something like:

    format.format(new Args().add("directory", "foo/bar/").add("file_count", 3.4567d));

To do that, we could just define in MessageFormat:
  static class Args extends HashMap<String,Object> {
    public Args add(String key, Object value) {
      put(key, value);
      return this;
    }
  }

     */
}
