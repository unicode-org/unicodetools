package org.unicode.text.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;
import org.unicode.tools.Confusables;
import org.unicode.tools.Confusables.Style;

import com.google.common.base.Splitter;
import com.google.common.collect.LinkedHashMultimap;
import com.ibm.icu.dev.util.UnicodeMap;

public class CheckSecurityProposals {
    private static final String SECURITY = Settings.UNICODE_DRAFT_PUBLIC + "security/";
    private static final IndexUnicodeProperties IUP = IndexUnicodeProperties.make(Settings.latestVersion);
    private static final UnicodeMap<Age_Values> AGE = IUP.loadEnum(UcdProperty.Age, UcdPropertyValues.Age_Values.class);

    public static final Confusables CONFUSABLES = new Confusables(SECURITY + Settings.latestVersion);
    public static final UnicodeMap<String> conMap = CONFUSABLES.getRawMapToRepresentative(Style.MA);

    public static Splitter TAB_SPLITTER = Splitter.on('\t').trimResults();
    public static Normalizer NFD = Default.nfd();

    public static void main(String[] args) {

        // /Users/markdavis/Documents/workspace/unicodetools/data/security/10.0.0/data/source/proposals.txt
        // /Users/markdavis/Documents/workspace/unicodetools/10.0.0/data/source/proposals.txt
        LinkedHashMultimap<String, String> confusable = LinkedHashMultimap.create();
        LinkedHashMultimap<String, String> nonconfusable = LinkedHashMultimap.create();
        HashMap<String, String> contributor = new HashMap<>();


        for (String line : FileUtilities.in(Settings.UNICODETOOLS_DIRECTORY + "data/security/" + Settings.latestVersion + "/data/source/", "proposals.txt")) {
            List<String> parts = TAB_SPLITTER.splitToList(line);
            String sourceRaw = parts.get(1);
            String source = NFD.normalize(Utility.fromHex(sourceRaw, true));
            String target = NFD.normalize(Utility.fromHex(parts.get(3), true));
            int sourceLen = source.codePointCount(0, source.length());
            int targetLength = target.codePointCount(0, target.length());
            if (sourceLen > targetLength) {
                if (targetLength > 1) {
                    System.out.println("*Skipping " + line);
                }
                String temp = source;
                source = target;
                target = temp;
            }
            if (source.isEmpty()) {
                System.out.println("*Skipping " + line);
                continue;
            }
            String sourceMapped = skeleton(source);
            String targetMapped = skeleton(target);
            contributor.put(source+"\uFFFF" + target, parts.get(5));
            String type = parts.get(7);
            switch (type) {
            case "Confusable": 
                if (sourceMapped.equals(targetMapped)) {
                    System.out.println("Already present: " + line);
                    continue;
                }
                confusable.put(source, target); 
                break;
            case "Not Confusable": 
                if (!sourceMapped.equals(targetMapped)) {
                    System.out.println("NOT Already present: " + line);
                    continue;
                }
                nonconfusable.put(source, target); 
                break;
            default: throw new IllegalArgumentException();
            }
        }
        show("Confusable", confusable, contributor);
        System.out.println();
        show("Not Confusable", nonconfusable, contributor);

    }

    private static String skeleton(String source) {
        return conMap.transform(NFD.normalize(source));
    }

    private static void show(String type, LinkedHashMultimap<String, String> confusable, HashMap<String, String> contributor) {
        String lastCon = "";
        for (Entry<String, String> entry : confusable.entries()) {
            String source = entry.getKey();
            String target = entry.getValue();
            String con = contributor.get(source+"\uFFFF" + target);
            if (!con.equals(lastCon)) {
                System.out.println("# " + con);
                lastCon = con;
            }
            System.out.println(Utility.hex(source) + "; " + Utility.hex(target)
            + "; " + (source.isEmpty() ? "NULL" : AGE.get(source))
            + "; " + source + " => " + target
            + "; " + IUP.getName(source, " + ") + " => " + IUP.getName(target, " + ")
                    );
        }
    }
}
