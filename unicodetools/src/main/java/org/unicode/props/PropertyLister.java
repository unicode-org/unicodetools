package org.unicode.props;

import com.google.common.base.Objects;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import java.io.IOException;
import org.unicode.cldr.util.Tabber;

public class PropertyLister {
    final IndexUnicodeProperties iup;

    public PropertyLister(IndexUnicodeProperties iup) {
        this.iup = iup;
    }

    public <T, O extends Appendable> O listMapValues(UnicodeMap<T> unicodeMap, T suppress, O out) {
        Tabber t = new Tabber.MonoTabber();
        t.add(16, Tabber.LEFT);
        int valueLen = 0;
        for (T v : unicodeMap.values()) {
            int len = v.toString().length();
            if (len > valueLen) {
                valueLen = len;
            }
        }
        t.add(valueLen, Tabber.LEFT);

        for (EntryRange<T> entry : unicodeMap.entryRanges()) {
            if (Objects.equal(entry.value, suppress)) {
                continue;
            }
            String lead, trail;
            if (entry.string != null) {
                lead = Utility.hex(entry.string);
                trail = iup.getName(entry.string, " + ");
            } else if (entry.codepoint == entry.codepointEnd) {
                lead = Utility.hex(entry.codepoint);
                trail = iup.getName(entry.codepoint);
            } else {
                lead = Utility.hex(entry.codepoint) + ".." + Utility.hex(entry.codepointEnd);
                trail = iup.getName(entry.codepoint) + ".." + iup.getName(entry.codepointEnd);
            }
            try {
                out.append(t.process(lead + "; \t" + entry.value + " # " + trail + "\n"));
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        return out;
    }

    public <T, O extends Appendable> O listSet(UnicodeSet unicodeSet, T value, O out) {
        Tabber t = new Tabber.MonoTabber();
        t.add(16, Tabber.LEFT);
        String valueString = value.toString();
        t.add(valueString.length(), Tabber.LEFT);
        int maxCount = 1;
        for (UnicodeSet.EntryRange entry : unicodeSet.ranges()) {
            if (entry.codepoint != entry.codepointEnd) {
                int subcount = entry.codepointEnd - entry.codepoint + 1;
                if (maxCount < subcount) {
                    maxCount = subcount;
                }
            }
        }
        t.add(String.valueOf(maxCount).length() + 7, Tabber.RIGHT);
        int count = 0;
        try {
            for (UnicodeSet.EntryRange entry : unicodeSet.ranges()) {
                String lead, trail;
                if (entry.codepoint == entry.codepointEnd) {
                    lead = Utility.hex(entry.codepoint);
                    trail =
                            "("
                                    + UTF16.valueOf(entry.codepoint)
                                    + ") "
                                    + iup.getName(entry.codepoint);
                } else {
                    lead = Utility.hex(entry.codepoint) + ".." + Utility.hex(entry.codepointEnd);
                    trail =
                            "("
                                    + UTF16.valueOf(entry.codepoint)
                                    + ".."
                                    + UTF16.valueOf(entry.codepointEnd)
                                    + ") "
                                    + iup.getName(entry.codepoint)
                                    + ".."
                                    + iup.getName(entry.codepointEnd);
                }
                int subcount = entry.codepointEnd - entry.codepoint + 1;
                out.append(process(t, valueString, lead, trail, subcount));
                count += subcount;
            }
            for (String entry : unicodeSet.strings()) {
                String lead, trail;
                lead = Utility.hex(entry);
                trail = "(" + entry + ") " + iup.getName(entry, " + ");
                out.append(process(t, valueString, lead, trail, 1));
                ++count;
            }
            /*
             * 1F9D1..1F9DD  ; Emoji_Modifier_Base  # 10.0 [13] (🧑..🧝)    adult..elf
             */
            out.append("\n# Total elements: " + count);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
        return out;
    }

    private String process(Tabber t, String valueString, String lead, String trail, int subcount) {
        return t.process(lead + "\t; " + valueString + " #\t [" + subcount + "]\t " + trail + "\n");
    }
}
