package org.unicode.text.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.MapComparator;
import org.unicode.text.UCA.UCA;
import org.unicode.text.tools.Emoji.ModifierStatus;
import org.unicode.text.utility.Utility;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UTF16.StringComparator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;

public class EmojiOrder {
    static final EmojiData emojiData = EmojiData.of(Emoji.VERSION_TO_GENERATE);
    public static final StringComparator PLAIN_STRING_COMPARATOR = new UTF16.StringComparator(true, false, 0);
    static final boolean USE_ORDER = true;
    static final ImmutableMap<String,ImmutableList<String>> hack = ImmutableMap.of(
            "👁", ImmutableList.of("👁‍🗨"),
            "💏", ImmutableList.of("👩‍❤️‍💋‍👨", "👨‍❤️‍💋‍👨", "👩‍❤️‍💋‍👩"),
            "💑", ImmutableList.of("👩‍❤️‍👨", "👨‍❤️‍👨", "👩‍❤️‍👩"),
            "👪", ImmutableList.of(
                    "👨‍👩‍👦", "👨‍👩‍👧", "👨‍👩‍👧‍👦", "👨‍👩‍👦‍👦", "👨‍👩‍👧‍👧", 
                    "👨‍👨‍👦", "👨‍👨‍👧", "👨‍👨‍👧‍👦", "👨‍👨‍👦‍👦", "👨‍👨‍👧‍👧",
                    "👩‍👩‍👦", "👩‍👩‍👧", "👩‍👩‍👧‍👦", "👩‍👩‍👦‍👦", "👩‍👩‍👧‍👧")
            );

    //    static {
    //        for (Entry<String, ImmutableList<String>> entry : hack.entrySet()) {
    //            System.out.println(show(entry.getKey()));
    //            for (String s : entry.getValue()) {
    //                System.out.println("\t" + s + "\t\t" + show(s));
    //            }
    //        }
    //    }

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
                        PLAIN_STRING_COMPARATOR);
    }

    Relation<String, String> getOrdering(String sourceFile, MapComparator<String> mapComparator) {
        Relation<String, String> result = Relation.of(new LinkedHashMap<String, Set<String>>(), LinkedHashSet.class);
        Set<String> sorted = new LinkedHashSet<>();
        Output<Set<String>> lastLabel = new Output<Set<String>>(new TreeSet<String>());
        for (String line : FileUtilities.in(EmojiOrder.class, sourceFile)) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            line = Emoji.getLabelFromLine(lastLabel, line);
            line = Emoji.UNESCAPE.transform(line);
            for (int i = 0; i < line.length();) {
                String string = Emoji.getEmojiSequence(line, i);
                i += string.length();
                if (Emoji.skipEmojiSequence(string)) {
                    continue;
                }
                if (!sorted.contains(string)) {
                    add(result, sorted, lastLabel, string);
                    ImmutableList<String> list = hack.get(string);
                    if (list != null) {
                        for (String string2 : list) {
                            //System.err.println("Adding " + show(string2));
                            add(result, sorted, lastLabel, string2); 
                        }
                    }
                    if (emojiData.getModifierBases().contains(string)) {
                        for (String string2 : emojiData.getModifierStatusSet(ModifierStatus.modifier)) {
                            add(result, sorted, lastLabel, string+string2); 
                        }
                    }
                }
            }
        }
        Set<String> missing = emojiData.getSortingChars().addAllTo(new LinkedHashSet<String>());
        missing.removeAll(sorted);
        if (!missing.isEmpty()) {
            result.putAll("other", missing);
            System.err.println("Missing some orderings: ");
            for (String s : missing) {
                System.err.println("\t" + s + "\t\t" + show(s));
            }
            throw new IllegalArgumentException();
        }
        sorted.addAll(missing);
        mapComparator.add(sorted);
        mapComparator.freeze();
        result.freeze();
        return result;
    }

    private static String show(String key) {
        StringBuilder b = new StringBuilder();
        for (int cp : CharSequences.codePoints(key)) {
            if (b.length() != 0) {
                b.append(' ');
            }
            b.append("U+" + Utility.hex(cp) + " " + UTF16.valueOf(cp));
        }
        return b.toString();
    }

    private void add(Relation<String, String> result, Set<String> sorted, Output<Set<String>> lastLabel, String string) {
        sorted.add(string);
        for (String item : lastLabel.value) {
            result.put(item, string);
        }
        charactersToOrdering.put(string, lastLabel.value.iterator().next());
    }

    public static void main(String[] args) {
        //LinkedHashSet<String> foo = Emoji.FLAGS.addAllTo(new LinkedHashSet());
        //System.out.println(CollectionUtilities.join(foo, " "));
        showOrderGroups();
        //        showOrder();
        //        STD_ORDER.show();
        //        ALT_ORDER.show();
    }

    private static void showOrderGroups() {
        String lastPair = "";
        List<String> current = null;
        List<List<String>> segments = new ArrayList<List<String>>();
        int lastNOrder = -1;
        int lastAOrder = -1;
        boolean diff = false;
        for (String cp : STD_ORDER.mp.getOrder()) {
            String std = STD_ORDER.charactersToOrdering.get(cp); 
            String alt = ALT_ORDER.charactersToOrdering.get(cp); 
            String pair = std + "/" + alt;
            if (cp.equals("🕴")) {
                pair += "🕴";
            }
            if (cp.equals("😈")) {
                int debug = 0;
            }
            if (USE_ORDER) {
                int nOrder = STD_ORDER.mp.getNumericOrder(cp);
                int aOrder = ALT_ORDER.mp.getNumericOrder(cp);
                diff = (nOrder - lastNOrder) != (aOrder-lastAOrder);
                lastNOrder = nOrder;
                lastAOrder = aOrder;
            }

            if (!pair.equals(lastPair) || diff) {
                current = new ArrayList<String>();
                segments.add(current);
            }
            current.add(cp);
            lastPair = pair;
        }

        for (List<String> segment : segments) {
            String first = segment.get(0);
            String std = STD_ORDER.charactersToOrdering.get(first); 
            String alt = ALT_ORDER.charactersToOrdering.get(first); 
            int nOrder = STD_ORDER.mp.getNumericOrder(first);
            int aOrder = ALT_ORDER.mp.getNumericOrder(first);
            // Ordered Characters   Unicode subgroup    Apple Group Count   Hex Name
            final String continuation = segment.size() > 0 ? "; …" : "";
            System.out.println(
                    (USE_ORDER ? "\t" + nOrder + "\t" + aOrder + "\t" + (aOrder-nOrder) : "")
                    + "\t" + CollectionUtilities.join(segment, " ")
                    + "\t" + std 
                    + "\t" + alt 
                    + "\t" + segment.size()
                    + "\t" + "U+" + Utility.hex(first) + continuation
                    + "\t" + UCharacter.getName(first, ",") + continuation
                    );
        }
    }

    private static void detailedOrder() {
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

    public <T extends Appendable> T appendCollationRules(T outText, UnicodeSet... characters) {
        try {
            boolean needRelation = true;
            boolean haveFlags = false;
            boolean isFirst = true;
            Set<String> temp = sort(codepointCompare, characters);

            String lastGroup = null;
            for (String s : temp) {
                String group = charactersToOrdering.get(s);
                if (!Objects.equal(group,lastGroup)) {
                    needRelation = true;
                    lastGroup = group;
                }
                boolean multiCodePoint = s.codePointCount(0, s.length()) > 1;
                if (isFirst) {
                    if (multiCodePoint) {
                        throw new IllegalArgumentException("Cannot have first item with > 1 codepoint: " + s);
                    }
                    outText.append("&").append(s);
                    isFirst = false;
                    continue;
                }
                if (multiCodePoint) { // flags and keycaps
                    if (Emoji.isRegionalIndicator(s.codePointAt(0))) {
                        if (!haveFlags) {
                            // put all the 26 regional indicators in order at
                            // this point
                            StringBuilder b = new StringBuilder("\n<*");
                            for (int i = Emoji.FIRST_REGIONAL; i <= Emoji.LAST_REGIONAL; ++i) {
                                b.appendCodePoint(i);
                            }
                            outText.append(b);
                            haveFlags = true;
                        }
                        continue;
                    }
                    // keycaps, zwj sequences, can't use <* syntax
                    String quoted = s.contains("*") || s.contains("#")? "'" + s + "'" : s;
                    String quoted2 = quoted.replaceAll(Emoji.EMOJI_VARIANT_STRING, "");
                    if (!quoted2.equals(quoted)) {
                        outText.append("\n<").append(quoted);
                        outText.append(" = ").append(quoted2);
                    } else {
                        outText.append("\n<").append(quoted);
                    }
                    needRelation = true;
                } else {
                    if (needRelation) {
                        outText.append("\n<*");
                        needRelation = false;
                    }
                    outText.append(s);
                    //                    // break arbitrarily (but predictably)
                    //                    int bottomBits = s.codePointAt(0) & 0xF;
                    //                    needRelation = bottomBits == 0;
                }
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException("Internal Error",e);
        }
        return outText;
    }

    public static Set<String> sort(Comparator<String> comparator, UnicodeSet... characters) {
        TreeSet<String> temp = new TreeSet<String>(comparator);
        for (UnicodeSet uset : characters) {
            uset.addAllTo(temp);
        }
        return Collections.unmodifiableSortedSet(temp);
    }
}