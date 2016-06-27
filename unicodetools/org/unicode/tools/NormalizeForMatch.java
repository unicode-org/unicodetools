package org.unicode.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.UCD.DerivedProperty;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;

public class NormalizeForMatch {
    public static final Splitter SEMI = Splitter.on(";").trimResults();
    public static final Splitter HASH = Splitter.on("#").trimResults();

    private final UnicodeMap<String> sourceToTarget;
    private final UnicodeMap<String> sourceToReason;

    public UnicodeMap<String> getSourceToTarget() {
        return sourceToTarget;
    }

    public UnicodeMap<String> getSourceToReason() {
        return sourceToReason;
    }

    public NormalizeForMatch(UnicodeMap<String> sourceToTarget2, UnicodeMap<String> sourceToReason2) {
        sourceToTarget = sourceToTarget2.freeze();
        sourceToReason = sourceToReason2.freeze();
    }

    public static NormalizeForMatch load(String file) {
        UnicodeMap<String> sourceToTarget = new UnicodeMap<>();
        UnicodeMap<String> sourceToReason = new UnicodeMap<>();
        try (BufferedReader in = FileUtilities.openFile(NormalizeForMatch.class, file)) {
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                String clean = HASH.split(line).iterator().next();
                if (clean.isEmpty()) {
                    continue;
                }
                List<String> parts = SEMI.splitToList(clean);
                String source = Utility.fromHex(parts.get(0));
                String target = Utility.fromHex(parts.get(1));
                String reason = parts.get(2);
                sourceToTarget.put(source, target);
                sourceToReason.put(source, reason);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
        return new NormalizeForMatch(sourceToTarget, sourceToReason);
    }

    public static void main(String[] args) {
        NormalizeForMatch.load("XNFKCCF.txt");
        IndexUnicodeProperties latest = IndexUnicodeProperties.make(Age_Values.V9_0);
        UnicodeSet newCharsOnly = latest.loadEnum(UcdProperty.Age, Age_Values.class).getSet(Age_Values.V9_0);
        UnicodeMap<String> NFKC_Casefold = latest.load(UcdProperty.NFKC_Casefold);
        UnicodeMap<General_Category_Values> gc = latest.loadEnum(UcdProperty.General_Category, UcdPropertyValues.General_Category_Values.class);
        for (General_Category_Values gcv : UcdPropertyValues.General_Category_Values.values()) {
            boolean first = true;
            for (String s : newCharsOnly) {
                if (gc.get(s) != gcv) {
                    continue;
                }
                if (first) {
                    System.out.println("\n# GC=" + gcv);
                    first = false;
                }
                String nfkccf = NFKC_Casefold.get(s);            
                System.out.println(Utility.hex(s) + "; " + latest.getName(s, " + ")
                        + (nfkccf == null ? "" : "; " + Utility.hex(nfkccf, " ") + "; " + latest.getName(nfkccf, " + ")));
            }
        }
    }
}
