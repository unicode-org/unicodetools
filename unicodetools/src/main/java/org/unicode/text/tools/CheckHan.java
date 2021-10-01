package org.unicode.text.tools;

import java.util.EnumSet;
import java.util.Set;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.NFKC_Quick_Check_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class CheckHan {
    final static IndexUnicodeProperties IUP = IndexUnicodeProperties.make(Settings.latestVersion);
    final static UnicodeSet NOT_NFKC = IUP.loadEnum(UcdProperty.NFKC_Quick_Check, NFKC_Quick_Check_Values.class).getSet(NFKC_Quick_Check_Values.No);
    static final UnicodeMap<String> names = IUP.load(UcdProperty.Name);
    static final UnicodeMap<Set<Script_Values>> scx = IUP.loadEnumSet(UcdProperty.Script_Extensions, Script_Values.class);
    final static UnicodeSet unifiedIdeograph = IUP.loadEnum(UcdProperty.Unified_Ideograph, Binary.class).getSet(Binary.Yes);
    final static UnicodeSet ideographic = IUP.loadEnum(UcdProperty.Ideographic, Binary.class).getSet(Binary.Yes);
    final static UnicodeMap<String> kTotalStrokes = IUP.load(UcdProperty.kTotalStrokes);

    final static UnicodeSet nameIdeographic = getIdeographNames(IUP);
    final static UnicodeSet scxHan = getHanScript(IUP);

    public static void main(String[] args) {
        final EnumSet<Script_Values> sset = EnumSet.of(Script_Values.Bopomofo, Script_Values.Han);
        boolean in = scx.values().contains(sset);
        UnicodeSet check = scx.getSet(sset);
        System.out.println(check.toPattern(false));
        show("〪");

        diff("ideographic", ideographic, "unifiedIdeograph", unifiedIdeograph);
        diff("scxHan", scxHan, "ideographic", ideographic);
        diff("nameIdeograhic", nameIdeographic, "scxHan", scxHan);  
        
        diff("unifiedIdeograph", unifiedIdeograph, "has kTotalStrokes", kTotalStrokes.keySet());  
    }

    private static void show(String string) {
        System.out.println(Utility.hex(string)
                + "\t" + unifiedIdeograph.contains(string)
                + "\t" + ideographic.contains(string)
                + "\t" + names.get(string)
                + "\t" + scx.get(string)
                );
    }

    private static UnicodeSet getHanScript(final IndexUnicodeProperties iup) {
        UnicodeSet han = new UnicodeSet();
        for (Set<Script_Values> sc : scx.values()) {
            if (sc.contains(Script_Values.Han)) {
                han.addAll(scx.getSet(sc));
            }
        }
        han.freeze();
        return han;
    }

    private static UnicodeSet getIdeographNames(final IndexUnicodeProperties iup) {
        UnicodeSet nameIdeographic = new UnicodeSet();
        for (String sc : names.values()) {
            if (sc.contains("IDEOGRAPHIC")) {
                nameIdeographic.addAll(names.getSet(sc));
            }
        }
        nameIdeographic.freeze();
        return nameIdeographic;
    }

    private static void diff(String aName, UnicodeSet a, String bName, UnicodeSet b) {
        System.out.println(aName + ":\t" + a.size());
        System.out.println(bName + ":\t" + b.size());
        show(aName, a, bName, b);
        show(bName, b, aName, a);
        System.out.println();
    }

    private static void show(String aName, UnicodeSet a, String bName, UnicodeSet b) {
        UnicodeSet aMb = new UnicodeSet(a).removeAll(b);
        System.out.println(aName + " - " + bName + ":\t" + aMb.size() + "\t" + aMb.toPattern(false));
        if (aMb.size() > 0) {
            UnicodeSet nfkc = new UnicodeSet(aMb).removeAll(NOT_NFKC);
            System.out.println("\tNFKC:\t" + nfkc.size() + "\t" + nfkc.toPattern(false));
            UnicodeSet notNfkc = new UnicodeSet(aMb).retainAll(NOT_NFKC);
            System.out.println("\t!NFKC:\t" + notNfkc.size() + "\t" + notNfkc.toPattern(false));
        }
    }
}
