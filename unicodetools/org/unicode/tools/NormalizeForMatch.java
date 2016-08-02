package org.unicode.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.UCD.DerivedProperty;
import org.unicode.text.utility.Utility;
import org.unicode.tools.NormalizeForMatch.SpecialReason;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;

public class NormalizeForMatch {
    public enum SpecialReason {
        digraph, 
        final_form, 
        fixed_superscript, 
        missing_sequence,
        missing_enclosed,
        missing_case,
        missing_nfkc, 
        fixed_nfkc, 
        radical, 
        retain_cf,
        retain_hangul,
        retain_tags,
        retain_super,
        retain_sub,
        retain_vertical,
        retain_sequences_with_exclusions,
        nfkccf_none,
        nfkccf_canonical,
        nfkccf_circle,
        nfkccf_compat,
        nfkccf_font,
        nfkccf_fraction,
        nfkccf_nobreak,
        nfkccf_square,
        nfkccf_isolated,
        nfkccf_final,
        nfkccf_initial,
        nfkccf_narrow,
        nfkccf_wide,
        nfkccf_medial,
        nfkccf_small,
        fix_slash, 
        add_separator, 
        recursion;
        static final SpecialReason forString(String s) {
            s = s.toLowerCase(Locale.ROOT);
            if (s.equals("retain_superscript")) {
                return retain_super;
            }
            return valueOf(s);
        }
    }

    public static final Splitter SEMI = Splitter.on(";").trimResults();
    public static final Splitter HASH = Splitter.on("#").trimResults();

    private final UnicodeMap<String> sourceToTarget;
    private final UnicodeMap<SpecialReason> sourceToReason;

    public UnicodeMap<String> getSourceToTarget() {
        return sourceToTarget;
    }

    public UnicodeMap<SpecialReason> getSourceToReason() {
        return sourceToReason;
    }

    public NormalizeForMatch(UnicodeMap<String> sourceToTarget2, UnicodeMap<SpecialReason> sourceToReason2) {
        sourceToTarget = sourceToTarget2.freeze();
        sourceToReason = sourceToReason2.freeze();
    }

    static String TEST = Utility.fromHex("1F19B");

    public static NormalizeForMatch load(String directory, String file) {
        UnicodeMap<String> sourceToTarget = new UnicodeMap<>();
        UnicodeMap<SpecialReason> sourceToReason = new UnicodeMap<>();
        NormalizeForMatch.SpecialReason overrideReason = null;
        try (BufferedReader in = directory == null 
                ? FileUtilities.openFile(NormalizeForMatch.class, file) 
                        : FileUtilities.openFile(directory, file)) {
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("#@")) {
                    overrideReason = SpecialReason.forString(line.split("=")[1]);
                    continue;
                }

                String clean = HASH.split(line).iterator().next();
                if (clean.isEmpty()) {
                    continue;
                }

                List<String> parts = SEMI.splitToList(clean);
                String source = Utility.fromHex(parts.get(0));
                if (source.equals(TEST)) {
                    boolean debug = true;
                }
                String target = Utility.fromHex(parts.get(1));
                SpecialReason reason =  parts.size() > 2 ? NormalizeForMatch.SpecialReason.forString(parts.get(2)) : overrideReason;

                sourceToTarget.put(source, target);
                sourceToReason.put(source, reason);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
        return new NormalizeForMatch(sourceToTarget, sourceToReason);
    }

    //    public static void main(String[] args) {
    //        IndexUnicodeProperties latest = IndexUnicodeProperties.make(Age_Values.V9_0);
    //        NormalizeForMatch curated = NormalizeForMatch.load("XNFKCCF-Curated.txt");
    //        for (SpecialReason reason : SpecialReason.values()) {
    //            curated.getSourceToReason().getSet(reason);
    //          System.out.println(Utility.hex(s) + "; " + latest.getName(s, " + ")
    //          + (target == null ? "" : "; " + Utility.hex(target, " ")
    //                  + "; " + latest.getName(target, " + ")));
    //        }
    //        UnicodeSet newCharsOnly = latest.loadEnum(UcdProperty.Age, Age_Values.class).getSet(Age_Values.V9_0);
    //        UnicodeMap<String> NFKC_Casefold = latest.load(UcdProperty.NFKC_Casefold);
    //        UnicodeMap<General_Category_Values> gc = latest.loadEnum(UcdProperty.General_Category, UcdPropertyValues.General_Category_Values.class);

    //        for (General_Category_Values gcv : UcdPropertyValues.General_Category_Values.values()) {
    //            boolean first = true;
    //            for (String s : newCharsOnly) {
    //                if (gc.get(s) != gcv) {
    //                    continue;
    //                }
    //                if (s.equals(TEST)) {
    //                    boolean debug = true;
    //                }
    //
    //                if (first) {
    //                    System.out.println("\n# GC=" + gcv);
    //                    first = false;
    //                }
    //                String target = curated.getSourceToTarget().get(s);
    //                //String target = NFKC_Casefold.get(s);            
    //                System.out.println(Utility.hex(s) + "; " + latest.getName(s, " + ")
    //                        + (target == null ? "" : "; " + Utility.hex(target, " ")
    //                                + "; " + latest.getName(target, " + ")));
    //            }
    //        }
    //    }
}
