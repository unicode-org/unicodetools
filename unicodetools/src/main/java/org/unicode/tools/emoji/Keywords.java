package org.unicode.tools.emoji;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.utility.Utility;

public class Keywords {
    private static final Multimap<String, String> keywords;

    static {
        String source = null;
        Splitter barSplit = Splitter.on('|').trimResults();
        final Splitter TAB = Splitter.on('\t');
        ImmutableMultimap.Builder<String, String> _keywords = ImmutableMultimap.builder();
        for (String line : FileUtilities.in(CandidateData.class, "keywordsExtra.txt")) {
            try {
                if (line.startsWith("#")) { // annotation
                    continue;
                } else if (line.startsWith("•")) { // annotation
                    _keywords.putAll(source, barSplit.split(line.substring(1)));
                } else if (line.startsWith("U+")) { // data
                    List<String> parts = TAB.splitToList(line);
                    source = Utility.fromHex(parts.get(0));
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(line, e);
            }
        }
        keywords = _keywords.build();
    }

    public static Collection<String> get(String source) {
        Collection<String> result = keywords.get(source);
        return result == null ? Collections.<String>emptySet() : result;
    }
}
