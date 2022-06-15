package org.unicode.tools;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.util.regex.Pattern;

public class Common {

    public static final Splitter DOT_SPLITTER = Splitter.on('.');
    public static final Splitter SPACE_SPLITTER = Splitter.onPattern("\\s+");
    public static final Splitter VBAR_SPLITTER = Splitter.on('|');
    public static final Splitter TAB_SPLITTER = Splitter.on('\t');
    public static final Splitter SEMI_SPLITTER = Splitter.on(';').trimResults();
    public static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults();
    public static final Joiner SPACE_JOINER = Joiner.on(' ');
    public static final Joiner COMMA_JOINER = Joiner.on(", ");
    public static final Joiner CRLF_JOINER = Joiner.on('\n');
    public static final Pattern ADOBE_RS_MATCHER =
            Pattern.compile("[CV]\\+[0-9]{1,5}\\+([1-9][0-9]{0,2})\\.([1-9][0-9]?)\\.([0-9]{1,2})");
}
