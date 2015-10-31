package org.unicode.text.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.MapComparator;
import org.unicode.text.UCA.UCA;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;

public class EmojiOrder {
    public static final UCA                       UCA_COLLATOR                = UCA.buildCollator(null);
    public static final EmojiOrder ALT_ORDER = new EmojiOrder("altOrder.txt");
    public static final EmojiOrder STD_ORDER = new EmojiOrder("emojiOrdering.txt");

    public final MapComparator<String>     mp;
    public final Relation<String, String>  orderingToCharacters;
    public final UnicodeMap<String>  charactersToOrdering = new UnicodeMap<>();
    public final Comparator<String>        codepointCompare;

    public EmojiOrder(String file) {
        mp  = new MapComparator<String>().setErrorOnMissing(false);
        orderingToCharacters            = getOrdering(file, mp);
        codepointCompare           =
                new MultiComparator<String>(
                        mp,
                        EmojiOrder.UCA_COLLATOR,
                        new UTF16.StringComparator(true, false, 0));
    }
    
    Relation<String, String> getOrdering(String sourceFile, MapComparator<String> mapComparator) {
        Relation<String, String> result = new Relation(new LinkedHashMap<String, Set<String>>(), LinkedHashSet.class);
        Set<String> sorted = new LinkedHashSet<>();
        Output<Set<String>> lastLabel = new Output<Set<String>>(new TreeSet<String>());
        for (String line : FileUtilities.in(EmojiOrder.class, sourceFile)) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            line = Emoji.getLabelFromLine(lastLabel, line);
            for (int i = 0; i < line.length();) {
                line = Emoji.UNESCAPE.transform(line);
                String string = Emoji.getEmojiSequence(line, i);
                i += string.length();
                if (Emoji.skipEmojiSequence(string)) {
                    continue;
                }
                if (!sorted.contains(string)) {
                    sorted.add(string);
                    for (String item : lastLabel.value) {
                        result.put(item, string);
                    }
                    charactersToOrdering.put(string, lastLabel.value.iterator().next());
                }
            }
        }
        Set<String> missing = Emoji.EMOJI_CHARS.addAllTo(new LinkedHashSet<String>());
        missing.removeAll(sorted);
        if (!missing.isEmpty()) {
            result.putAll("other", missing);
            //throw new IllegalArgumentException("Missing some orderings: " + new UnicodeSet().addAll(missing));
        }
        sorted.addAll(missing);
        mapComparator.add(sorted);
        mapComparator.freeze();
        result.freeze();
        return result;
    }
    
    public static void main(String[] args) {

        showOrder();
//        STD_ORDER.show();
//        ALT_ORDER.show();
    }

    private static void showOrder() {
        int lastAOrder = -100;
        List<String> current = null;
        List<List<String>> segments = new ArrayList<List<String>>();
        for (String cp : STD_ORDER.mp.getOrder()) {
            int aOrder = ALT_ORDER.mp.getNumericOrder(cp);
            if (aOrder - lastAOrder != 1) {
                current = new ArrayList<String>();
                segments.add(current);
            }
            current.add(cp);
            lastAOrder = aOrder;
        }

        for (List<String> segment : segments) {
            String first = segment.get(0);
            String std = STD_ORDER.charactersToOrdering.get(first); 
            String alt = ALT_ORDER.charactersToOrdering.get(first); 
            int nOrder = STD_ORDER.mp.getNumericOrder(first);
            int aOrder = ALT_ORDER.mp.getNumericOrder(first);

            final String continuation = segment.size() > 0 ? "; …" : "";
            System.out.println(
                    nOrder 
                    + "\t" + std 
                    + "\t" + alt 
                    + "\t" + aOrder 
                    + "\t" + CollectionUtilities.join(segment, " ")
                    + "\t" + segment.size()
                    + "\t" + "U+" + Utility.hex(first) + continuation
                    + "\t" + UCharacter.getName(first, ",") + continuation
                    );
        }
    }

    private void show() {
        for (Entry<String, Set<String>> labelToSet : orderingToCharacters.keyValuesSet()) {
            String label = labelToSet.getKey();
            System.out.print(label + "\t");
            int count = label.length();
            for (String cp : labelToSet.getValue()) {
                if (++count > 32) {
                    count = 0;
                    System.out.println();
                }
                System.out.print(cp + " ");
            }
            System.out.println();
        }
    }
}