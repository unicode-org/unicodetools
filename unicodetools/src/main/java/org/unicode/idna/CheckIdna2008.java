package org.unicode.idna;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class CheckIdna2008 {

    static UnicodeSet patriks2008 = new UnicodeSet();
    static UnicodeMap<String> patriksData = new UnicodeMap();
    static UnicodeSet uts46 = new UnicodeSet();
    static UnicodeMap<String> utsData = new UnicodeMap<String>();
    static IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);

    public static void main(String[] args) {

        for (final String line : FileUtilities.in(Settings.UnicodeTools.DATA_DIR + "/idna/", "f.txt")) {
            if (line.startsWith("Codepoints")) {continue;}
            final String[] parts = line.split(";");
            // 0041;DISALLOWED;Y;AB;LATIN CAPITAL LETTER A
            final int cp = Integer.parseInt(parts[0], 16);
            final String status = parts[1];
            if (status.equals("DISALLOWED") || status.equals("UNASSIGNED")) {
                // skip
            } else {
                patriks2008.add(cp);
            }
            patriksData.put(cp, allButFirst(parts));
        }

        String idnaDir = Settings.UnicodeTools.getDataPathStringForLatestVersion("idna");
        for (String line : FileUtilities.in(idnaDir + "/", "IdnaMappingTable.txt")) {
            final int pos2 = line.indexOf('#');
            if (pos2 >= 0) {
                line = line.substring(0,pos2);
            }
            line = line.trim();
            if (line.length() == 0) {continue;}
            final String[] parts = line.split("\\s*;\\s*");
            // 00A1..00A7    ; valid                  ;      ; NV8    # 1.1  INVERTED EXCLAMATION MARK..SECTION SIGN
            try {
                final int pos = parts[0].indexOf("..");
                int start, end;
                if (pos < 0) {
                    start = end = Integer.parseInt(parts[0], 16);
                } else {
                    start = Integer.parseInt(parts[0].substring(0,pos), 16);
                    end = Integer.parseInt(parts[0].substring(pos+2), 16);
                }
                final String status = parts[1];
                final String nv8 = parts.length >= 4 ? parts[3] : null;
                if (status.equals("valid")
                        && !"NV8".equals(nv8)) {
                    uts46.addAll(start, end);
                }
                utsData.putAll(start, end, allButFirst(parts));
            } catch (final Exception e) {
                throw new IllegalArgumentException(line, e);
            }
        }
        //UnicodeMap<String> gc = iup.load(UcdProperty.General_Category);
        // gc.getSet(PropertyValues.General_Category_Values.Unassigned.toString());
        final UnicodeSet cn = new UnicodeSet();
        if (!uts46.equals(patriks2008)) {
            show("\nUTS46 - IDNA2008\n", new UnicodeSet(uts46).removeAll(patriks2008).removeAll(cn));
            show("\nIDNA2008 - UTS46\n", new UnicodeSet(patriks2008).removeAll(uts46).removeAll(cn));
        }
    }

    private static String allButFirst(String[] parts) {
        final StringBuilder result = new StringBuilder();
        for (int i = 1; i < parts.length; ++i) {
            if (result.length() != 0) {
                result.append("; ");
            }
            result.append(parts[i]);
        }
        return result.toString();
    }

    private static void show(String string, UnicodeSet diff) {
        System.out.println(string);
        for (final String s : diff) {
            final int cp = s.codePointAt(0);
            final String p = patriksData.get(cp);
            final String u = utsData.get(cp);
            final String name = iup.getResolvedValue(UcdProperty.Name, cp);
            System.out.println(Utility.hex(s) + "\t" + name + "\n\tIDNA2008:\t" + p + "\n\tUTS46:\t\t" + u);
        }

    }
}
