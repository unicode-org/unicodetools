package org.unicode.tools;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;

public class IdsFileData {
    public static final UnicodeMap<UnicodeSet> cjkStrokeToExamples = new UnicodeMap<UnicodeSet>();
    public static final Relation<Integer,String> radToCjkRad = Relation.of(new TreeMap<Integer,Set<String>>(), TreeSet.class);
    public static final Relation<String,Integer> cjkRadToRad = Relation.of(new TreeMap<String,Set<Integer>>(), TreeSet.class);
    public static final UnicodeMap<String> cjkRadSupToIdeo = new UnicodeMap<>();
    public static final UnicodeMap<List<Integer>> TOTAL_STROKES = new UnicodeMap<>();

    static {
        for (String line : FileUtilities.in(Settings.OTHER_WORKSPACE_DIRECTORY, "/DATA/ids/ucs-strokes.txt")) { // /Users/markdavis/Google Drive/workspace/DATA/
            if (line.isEmpty()) {
                continue;
            }
            List<String> parts = Common.TAB_SPLITTER.splitToList(line);
            String f1 = parts.get(0);
            if (f1.startsWith("CDP")) {
                continue;
            }
            String cp = Utility.fromHex(f1);
            Builder<Integer> ilb = ImmutableList.builder();
            for (String s : Common.COMMA_SPLITTER.splitToList(parts.get(2))) {
                ilb.add(Integer.parseInt(s));
            }

            TOTAL_STROKES.put(cp, ilb.build());
        }
        TOTAL_STROKES.freeze();
        
        for (String line : FileUtilities.in(Ids.class, "n3063StrokeExamples.txt")) {
            int hashPos = line.indexOf('#');
            if (hashPos >= 0) {
                line= line.substring(0, hashPos).trim();
            }
            if (line.isEmpty()) {
                continue;
            }
            List<String> parts = Common.SEMI_SPLITTER.splitToList(line);
            int cjkStroke = parts.get(0).codePointAt(0);
            final UnicodeSet examples = new UnicodeSet().addAll(parts.get(1)).remove(' ').freeze();
            cjkStrokeToExamples.put(cjkStroke, examples);
        }
        cjkStrokeToExamples.freeze();

        for (String line : FileUtilities.in(Ids.class, "idsCjkRadicals.txt")) {
            int hashPos = line.indexOf('#');
            if (hashPos >= 0) {
                line= line.substring(0, hashPos).trim();
            }
            if (line.isEmpty()) {
                continue;
            }
            List<String> parts = Common.SEMI_SPLITTER.splitToList(line);
            int cjkRad = Integer.parseInt(parts.get(0), 16);
            final String radString = parts.get(1);
            int radNumber = Integer.parseInt(radString);
            radToCjkRad.put(radNumber, UTF16.valueOf(cjkRad));
        }
        radToCjkRad.freeze();
        cjkRadToRad.addAllInverted(radToCjkRad);
        cjkRadToRad.freeze();

        for (String line : FileUtilities.in(Ids.class, "cjkRadicalsSupplementAnnotations.txt")) {
            if (line.startsWith("#")) {
                continue;
            }
            List<String> row = Common.SEMI_SPLITTER.splitToList(line);
            // 2E81 ;   ⺁ ; 5382 ;  厂
            String cjkRadSup = row.get(1);
            String cp = row.get(3);
            if (cp.codePointCount(0, cp.length()) != 1) {
                throw new ICUException(row.toString());
            }
            cjkRadSupToIdeo.put(cjkRadSup, cp);
        }
        cjkRadSupToIdeo.freeze();
    }
}

