package org.unicode.text.tools;

import java.io.IOException;
import java.util.TreeSet;

import org.unicode.idna.Idna.IdnaType;
import org.unicode.idna.Idna2003;
import org.unicode.idna.Idna2008;
import org.unicode.idna.Idna2008.Idna2008Type;
import org.unicode.idna.Idna2008t;
import org.unicode.idna.Uts46;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

public class CompareIDNA {

    static UnicodeSet skipList = new UnicodeSet("[[:age=6.0:]-[:age=5.2:]]");

    public static void main(String[] args) throws IOException {
        
        UnicodeMap<Idna2008Type> IDNA2008Computed = Idna2008t.getTypeMapping();
        // verify

        UnicodeMap<Idna2008.Idna2008Type> idna2008Map = Idna2008.getTypeMapping();
        UnicodeMap<String> differences = new UnicodeMap<String>();
        for (int i = 0; i < 0x10FFFF; ++i) {
            if (skipList.contains(i)) continue;
            Idna2008Type tableValue = idna2008Map.get(i);
            Idna2008Type computedValue = IDNA2008Computed.get(i);
            if (tableValue != computedValue) {
                differences.put(i, tableValue + "/" + computedValue);
            }
        }

        System.out.println(differences);

        UnicodeMap<String> diff = new UnicodeMap<String>();
        for (int i = 0; i <= 0x10FFFF; ++i) {
//            if (UnicodeUtilities.IGNORE_IN_IDNA_DIFF.contains(i)) {
//                continue;
//            }

            IdnaType type = Uts46.SINGLETON.getType(i);

            Idna2008Type idna2008 = IDNA2008Computed.get(i);
            if (type == IdnaType.ignored) {
                type = IdnaType.mapped;
            }

            IdnaType idna2003 = Idna2003.getIDNA2003Type(i);
            if (idna2003 == IdnaType.ignored) {
                idna2003 = IdnaType.mapped;
            }

            IdnaType idna2008Mapped = 
                (idna2008 == Idna2008Type.UNASSIGNED || idna2008 == Idna2008Type.DISALLOWED) ? IdnaType.disallowed
                        : IdnaType.valid;

            VersionInfo age = UCharacter.getAge(i);
            String ageString = age.getMajor() >= 4 ? "U4+" : "U3.2";
            diff.put(i, ageString + "_" + idna2003 + "_" + type + "_" + idna2008Mapped);
        }

        for (String types : new TreeSet<String>(diff.values())) {
            UnicodeSet set = diff.getSet(types);
            System.out.println(types + " ;\t" + set.size() + " ;\t" + set);
        }
    }
}
