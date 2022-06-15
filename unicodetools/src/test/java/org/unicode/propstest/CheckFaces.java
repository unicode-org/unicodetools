package org.unicode.propstest;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import java.util.HashMap;
import java.util.Map;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Utility;

public class CheckFaces {
    public static void main(String[] args) {
        IndexUnicodeProperties iup = IndexUnicodeProperties.make();
        UnicodeMap<String> names = iup.load(UcdProperty.Name);
        Map<String, UnicodeSet> nameToUset = new HashMap<>();

        UnicodeSet faces = new UnicodeSet();
        for (String name : names.values()) {
            UnicodeSet uset = names.getSet(name);
            nameToUset.put(name, uset.freeze());
            if (name.contains("FACE")) {
                faces.addAll(uset);
            }
        }
        for (String s : faces) {
            String name = names.get(s);
            String noface = name.replace("FACE", "").trim().replace("  ", " ");
            UnicodeSet others = nameToUset.get(noface);
            if (others != null) {
                System.out.println(
                        "U+"
                                + Utility.hex(s)
                                + "\t"
                                + name
                                + "\t"
                                + "U+"
                                + Utility.hex(others.iterator().next())
                                + "\t"
                                + noface);
            }
        }
    }
}
