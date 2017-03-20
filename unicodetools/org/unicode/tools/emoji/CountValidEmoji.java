package org.unicode.tools.emoji;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ULocale;

public class CountValidEmoji {
    public static void main(String[] args) {
        countInvalid();
    }

    private static void countInvalid() {
        Validity validity = Validity.getInstance();

        countInvalid(validity, LstrType.region);
        countInvalid(validity, LstrType.subdivision);
    }

    private static void countInvalid(Validity validity, LstrType type) {
        NumberFormat nf = NumberFormat.getIntegerInstance(ULocale.ROOT);
        Map<String, Status> codeToStatus = validity.getCodeToStatus(type);
        // idStatus="regular", "deprecated", or the "macroregion". However, for macroregions, only UN and EU are valid.
        Multimap<Status,String> inverse = Multimaps.invertFrom(Multimaps.forMap(codeToStatus), TreeMultimap.create());
        Set<String> recommended = new LinkedHashSet<>(inverse.get(Status.regular));
        Set<String> other_valid = new LinkedHashSet<>(inverse.get(Status.deprecated));
        long syntactic = 0;
        switch (type) {
        case region:
            recommended.add("UN");
            recommended.add("EU");
            syntactic = 26*26 + 999L;
            break;
        case subdivision:
            other_valid.addAll(recommended);
            recommended.clear();
            recommended.addAll(Arrays.asList("gbeng", "gbsct", "gbwls"));
            other_valid.removeAll(recommended);
            syntactic = (26*26 + 999L) * (35*36*36);
            break;
        default: 
            throw new ICUException();
        }
        System.out.println("\n" + type);
        System.out.println("recommended: " + nf.format(recommended.size()));
        System.out.println("other_valid: " + nf.format(other_valid.size()));
        long syntactic_but_invalid = syntactic - recommended.size() - other_valid.size();
        System.out.println("syntactic_but_invalid: " + nf.format(syntactic_but_invalid));
    }
}
