package org.unicode.draft;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;
import java.util.Set;
import java.util.TreeSet;

public class GenerateCasedPairs {
    public static void main(String[] args) {
        final UnicodeSet set =
                new UnicodeSet(
                        "[[:script=Cyrillic:]"
                                + "-[:block=Phonetic_Extensions:]"
                                + "-[:block=Cyrillic_Extended_A:]"
                                + "-[:block=Cyrillic_Extended_B:]"
                                + "-[:block=Cyrillic_Supplement:]"
                                + "]");
        final Set<String> lower = new TreeSet(Collator.getInstance(ULocale.ENGLISH));
        final Set<String> blocks = new TreeSet();
        for (final UnicodeSetIterator it = new UnicodeSetIterator(set); it.next(); ) {
            lower.add(UCharacter.toLowerCase(ULocale.ENGLISH, it.getString()));
            final String block =
                    UCharacter.getStringPropertyValue(
                            UProperty.BLOCK, it.codepoint, UProperty.NameChoice.LONG);
            blocks.add(block);
        }
        System.out.println("Blocks: " + blocks);
        for (final String s : lower) {
            final int ch = UTF16.charAt(s, 0);
            String name = UCharacter.getExtendedName(ch);
            name = name.replace("CYRILLIC ", "");
            name = name.replace("SMALL LETTER ", "");
            name = name.replace("LETTER ", "");
            name = name.toLowerCase();
            if (UTF16.countCodePoint(s) > 1) {
                name += "...";
            }
            System.out.println(name + "\t" + s + " " + UCharacter.toUpperCase(ULocale.ENGLISH, s));
        }
    }
}
