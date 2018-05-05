package org.unicode.tools.emoji;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.CountEmoji.Category;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;
import org.unicode.tools.emoji.GenerateEmojiData.ZwjType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;

public class CountEmoji {
    private static final EmojiData EMOJI_DATA_PREVIOUS = EmojiData.of(Emoji.VERSION_LAST_RELEASED);
    private static final EmojiData EMOJI_DATA_BETA = EmojiData.of(Emoji.VERSION_BETA);
    private static final EmojiOrder ORDER = EmojiOrder.of(Emoji.VERSION_BETA);

    enum MyOptions {
        nonincrementalCount(new Params()),
        countVs(new Params()),
        invalid(new Params()),
        verbose(new Params().setHelp("verbose debugging messages")), 
        list(new Params()),
        ;

        // BOILERPLATE TO COPY
        final Option option;
        private MyOptions(Params params) {
            option = new Option(this, params);
        }
        private static Options myOptions = new Options();
        static {
            for (MyOptions option : MyOptions.values()) {
                myOptions.add(option, option.option);
            }
        }
        private static Set<String> parse(String[] args, boolean showArguments) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    public static void main(String[] args) {
        MyOptions.parse(args, true);
        boolean done = false;
        if (MyOptions.countVs.option.doesOccur()) {
            countVs();
            done=true;
        }
        //        if (MyOptions.invalid.option.doesOccur()) {
        //            countInvalid();
        //            done=true;
        //        }
        if (MyOptions.nonincrementalCount.option.doesOccur()) {
            countNonincremental();
            done=true;
        }
        if (MyOptions.list.option.doesOccur()) {
            //Category bucket = Category.getBucket("👨‍⚖️");
            UnicodeSet toDisplay = EmojiData.of(Emoji.VERSION_BETA).getAllEmojiWithoutDefectives();
            System.out.println("\nEmoji v11");
            listCategories(toDisplay);

            EmojiData EMOJI_DATA_PREVIOUS = EmojiData.of(Emoji.VERSION_TO_GENERATE_PREVIOUS);
            UnicodeSet onlyNew = new UnicodeSet(EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives())
                    .removeAll(EMOJI_DATA_PREVIOUS.getAllEmojiWithoutDefectives());
            System.out.println("\nEmoji v11-v5");
            listCategories(onlyNew);
            done = true;
        }
        if (!done) {
            countNew();
        }
    }

    private static void listCategories(UnicodeSet toDisplay) {
        Multimap<Category,String> items = TreeMultimap.create(Ordering.natural(), EmojiOrder.STD_ORDER.codepointCompare);
        for (String item : toDisplay) {
            Category cat = Category.getBucket(item);
            items.put(cat, item);
        }
        for (Category cat : Category.values()) {
            Collection<String> set = items.get(cat);
            System.out.println(cat.toStringPlain()
                    + "\t" + CollectionUtilities.join(cat.getAttributes(), " ")
                    + "\t" + set.size()
                    + "\t" + CollectionUtilities.join(set, " "));
        }
    }

    private static void countNonincremental() {
        UnicodeSet all = EMOJI_DATA_BETA.getAllEmojiWithDefectives();

        UnicodeSet missing = new UnicodeSet();
        for (String zwj : EMOJI_DATA_BETA.getZwjSequencesNormal()) {
            int pos = zwj.length();
            while (true) {
                int lastZwjIndex = zwj.lastIndexOf(Emoji.JOINER, pos);
                if (lastZwjIndex < 0) break;
                String prev = zwj.substring(0, lastZwjIndex);
                if (!all.contains(prev) 
                        && !all.contains(prev.replace(Emoji.EMOJI_VARIANT_STRING, "")) 
                        && !missing.contains(prev)) {
                    System.out.println(prev  + "\t" + Utility.hex(prev));
                    missing.add(prev);
                }
                pos = lastZwjIndex-1;
            }
        }
        System.out.println("ZSeq Count: " + EMOJI_DATA_BETA.getZwjSequencesNormal().size());
        System.out.println("NonIncremental count: " + missing.size());
    }

    private static void countNew() {
        UnicodeSet current = new UnicodeSet(EMOJI_DATA_BETA.getAllEmojiWithoutDefectives())
                .addAll(EMOJI_DATA_BETA.getEmojiComponents())
                .freeze();
        UnicodeSet previous = new UnicodeSet(EMOJI_DATA_PREVIOUS.getAllEmojiWithoutDefectives())
                .addAll(EMOJI_DATA_BETA.getEmojiComponents())
                .freeze();
        UnicodeSet ARE_NEW = new UnicodeSet(current)
                .removeAll(previous)
                .freeze();

        String vPrevious = "v" + Emoji.VERSION_LAST_RELEASED.getVersionString(2, 2);
        String vCurrent = "v" + Emoji.VERSION_BETA.getVersionString(2, 2);

        new CountEmoji().countItems(vCurrent + "-" + vPrevious, ARE_NEW);
        new CountEmoji().countItems(vPrevious, previous);
        new CountEmoji().countItems(vCurrent, current);
    }

    final Map<Category, Bucket> buckets = new EnumMap<>(Category.class);

    static class Bucket {
        final Counter<MajorGroup> majors = new Counter<>();
        final UnicodeMap<MajorGroup> sets= new UnicodeMap<>();
        public void add(MajorGroup maj, String cat, String s) {
            majors.add(maj, 1);
            sets.put(s,maj);
        }
    }

    void add(String s) {
        add(s, null);
    }
    void add(String s, CandidateData candidateData) {
        String cat = ORDER.getCategory(s);
        if (cat == null && candidateData != null) {
            cat = candidateData.getCategory(s);
        }
        MajorGroup maj = cat == null ? MajorGroup.Other : ORDER.getMajorGroupFromCategory(cat);
        Category category = Category.getBucket(s);
        Bucket bucket = buckets.get(category);
        if (bucket == null) {
            buckets.put(category, bucket = new Bucket());
        }
        bucket.add(maj, cat, s);
        return;
    }
    public void showCounts(PrintWriter out, boolean showCharacters, String title) {
        out.println("<h3><a href='#totals' name='totals'>Totals</a></h3>\n"
                + "<p>Totals for "
                + title
                + ". For information on the categories, see <a href='../format.html#col-totals'>Totals</a>.</p>\n" + "<table>\n");

        String row = "<tr>";
        String td = "<td class='rchars'>";
        String th = "<th class='rchars'>";
        MajorGroup[] groups = MajorGroup.values();
        out.print(row + th + "</th>");
        for (MajorGroup maj : groups) {
            out.print(th + maj.toHTMLString() + "</th>");
        }
        out.println(th + "Total" + "</th>" + "</tr>");
        boolean didSub = false;

        Counter<MajorGroup> columnCount = new Counter<MajorGroup>();
        boolean doneSubtotal = false;
        for (Category evalue : Category.values()) {
            Bucket bucket = buckets.get(evalue);
            if (bucket == null) {
                continue;
            }
            if ((evalue == Category.component || evalue == Category.typical_dup ) // || evalue == Category.typical_dup_group | evalue == Category.typical_dup_sign) 
                    && !doneSubtotal) {
                showTotalLine(out, "Subtotal", row, th, groups, columnCount);
                doneSubtotal = true;
            }
            out.print(row + th + evalue + "</th>");
            long rowTotal1 = 0;
            for (MajorGroup maj : groups) {
                long count = bucket.majors.get(maj);
                rowTotal1 += count;
                columnCount.add(maj, count);
                out.print(td + (count == 0 ? "" : count) + "</td>");
            }
            out.println(th + rowTotal1 + "</th>" + "</tr>");
        }
        showTotalLine(out, "Total", row, th, groups, columnCount);

        if (showCharacters) {
            out.println();
            for (Category evalue : Category.values()) {
                Bucket bucket = buckets.get(evalue);
                if (bucket == null) {
                    continue;
                }
                out.print(evalue);
                for (MajorGroup maj : groups) {
                    out.print("\t" + bucket.sets.getSet(maj).toPattern(false));
                }
                out.println();
            }
        }
        out.println("</table>\n");
    }

    private void showTotalLine(PrintWriter out, String title2, String row, String th, MajorGroup[] groups,
            Counter<MajorGroup> columnCount) {
        long rowTotal = 0;
        out.print(row + th + title2 + "</th>");
        for (MajorGroup maj : groups) {
            long count = columnCount.get(maj);
            rowTotal += count;
            out.print(th + count + "</th>");
        }
        out.println(th + rowTotal + "</th>" + "</tr>");
    }

    static final String GENDER = Emoji.MALE+'/'+Emoji.FEMALE;
    static final String WO_MAN = new StringBuilder().appendCodePoint(Emoji.MAN).append('/').appendCodePoint(Emoji.WOMAN).toString();

    enum Attribute {
        singleton, zwj, skin, gender, role, family, hair,
    }

    enum Category {
        character("char"), 
        zwj_seq_gender("zwj: " + GENDER, Attribute.zwj, Attribute.gender), 
        zwj_seq_role("zwj: "+WO_MAN+"obj", Attribute.zwj, Attribute.role),
        zwj_seq_fam("zwj: "+Emoji.NEUTRAL_FAMILY, Attribute.zwj, Attribute.family), 
        //zwj_seq_fam_mod("zwj: "+Emoji.NEUTRAL_FAMILY + "&skin"), 
        //zwj_seq_mod("zwj: other&skin", Attribute.zwj, Attribute.skin),
        zwj_seq_other("zwj: other", Attribute.zwj),
        mod_seq("char & skin", Attribute.skin), 
        zwj_seq_gender_mod("zwj: " + GENDER + " & skin", Attribute.zwj, Attribute.gender, Attribute.skin),
        zwj_seq_role_mod("zwj: "+WO_MAN +"obj" + " & skin", Attribute.zwj, Attribute.role, Attribute.skin), 
        zwj_seq_hair("zwj: hair", Attribute.zwj, Attribute.hair),
        zwj_seq_mod_hair("zwj: skin & hair", Attribute.zwj, Attribute.skin, Attribute.hair),
        keycap_seq,
        flag_seq,
        tag_seq, 
        typical_dup, 
        component, 
        //        typical_dup_sign,
        //        typical_dup_group, 
        ;

        final public String displayName;
        final public String html;
        final Set<Attribute> attributes;
        Category() {
            this(null);
        }
        Category(String _name, Attribute... _baseCategories) {
            displayName = _name == null ? name().replace('_', ' ') : _name;
            html = TransliteratorUtilities.toHTML.transform(displayName);
            attributes = _baseCategories.length == 0 ? Collections.emptySortedSet() 
                    : ImmutableSortedSet.copyOf(EnumSet.copyOf(Arrays.asList(_baseCategories)));
        }
        static Map<Set<Attribute>, Category> attributesToCategory;
        static {
            Map<Set<Attribute>, Category> _attributesToCategory = new HashMap<>();
            for (Category cat : Category.values()) {
                _attributesToCategory.put(cat.attributes, cat);
            }
            attributesToCategory = ImmutableMap.copyOf(_attributesToCategory);
        }
        @Override
        public String toString() {
            return html;
        }
        public String toStringPlain() {
            return displayName;
        }
        static public Category getBucket(String s) {
            String noVariants = EmojiData.removeEmojiVariants(s);
            Category bucket = null;
            if (EmojiData.EMOJI_DATA.isTypicallyDuplicateSign(s)) {
                bucket = typical_dup;
                //            } else if (EmojiData.isTypicallyDuplicateSign(s)) {
                //                bucket = typical_dup_sign;
            } else if (noVariants.isEmpty() || CountEmoji.EMOJI_DATA_BETA.getEmojiComponents().contains(noVariants)) {
                bucket = component;
            } else if (CharSequences.getSingleCodePoint(noVariants) < Integer.MAX_VALUE) {
                bucket = character;
            } else if (noVariants.contains(Emoji.KEYCAP_MARK_STRING)) {
                bucket = keycap_seq;
            } else if (noVariants.contains(Emoji.TAG_TERM)) {
                bucket = tag_seq;
            } else if (Emoji.REGIONAL_INDICATORS.containsSome(noVariants)) {
                bucket = flag_seq;
            } else {
                EnumSet<Attribute> attributes = EnumSet.noneOf(Attribute.class);

                boolean mods = EmojiData.MODIFIERS.containsSome(noVariants);
                if (mods) {
                    attributes.add(Attribute.skin);
                }
                boolean hair = Emoji.HAIR_PIECES.containsSome(noVariants);
                if (hair) {
                    attributes.add(Attribute.hair);
                }
                boolean gender = Emoji.GENDER_MARKERS.containsSome(noVariants);
                if (gender) {
                    attributes.add(Attribute.gender);
                }
                boolean zwj = noVariants.contains(Emoji.JOINER_STR);
                if (zwj) {
                    attributes.add(Attribute.zwj);
                }

                int first = noVariants.codePointAt(0);
                String butFirst = noVariants.substring(Character.charCount(first));
                boolean role = Emoji.MAN_OR_WOMAN.contains(first) 
                        && Emoji.PROFESSION_OBJECT.containsSome(noVariants);
                if (role) {
                    attributes.add(Attribute.role);
                }
                boolean family = Emoji.FAMILY_MARKERS.contains(first) && Emoji.FAMILY_MARKERS.containsSome(butFirst);
                if (family) {
                    attributes.add(Attribute.family);
                }
                bucket = getCategory(attributes);
                //                
                //
                //                if (!zwj) {
                //                    if (mods) {
                //                        bucket = mod_seq;
                //                    } else {
                //                        throw new IllegalArgumentException("should never happen");
                //                    }
                //                } else { // zwj
                //                    if (gender) {
                //                        bucket = getVariety(mods, hair, zwj_seq_gender, zwj_seq_gender_mod, null, null);
                //                    } else if (role) {
                //                        bucket = getVariety(mods, hair, zwj_seq_role, zwj_seq_role_mod, null, null);
                //                    } else if (family) {
                //                        bucket = getVariety(mods, hair, zwj_seq_fam, null, null, null);
                //                    } else {
                //                        bucket = getVariety(mods, hair, zwj_seq_other, zwj_seq_mod, zwj_seq_hair, zwj_seq_mod_hair);
                //                    }
                //                }
            }
            return bucket;
        }

        private static Category getCategory(EnumSet<Attribute> attributes) {
            Category result = attributesToCategory.get(attributes);
            if (result == null) {
                throw new IllegalArgumentException("no category available, add??");
            }
            return result;
        }
        private static Category getVariety(boolean mods, boolean hair, 
                Category noSkinNoHair, Category skinNoHair,
                Category noSkinHair, Category skinHair
                ) {
            Category bucket;
            if (mods) {
                if (hair) {
                    bucket = skinHair;
                } else {
                    bucket = skinNoHair;
                }
            } else { //  if (!gender)
                if (hair) {
                    bucket = noSkinHair;
                } else {
                    bucket = noSkinNoHair;
                }
            }
            if (bucket == null) {
                throw new IllegalArgumentException("no category available, add??");
            }
            return bucket;
        }

        public boolean hasBaseCategory(Attribute baseCategory) {
            return attributes.contains(baseCategory);
        }
        public Set<Attribute> getAttributes() {
            return attributes;
        }
    }

    private void countItems(String title, UnicodeSet uset) {
        Set<String> sorted = uset.addAllTo(new TreeSet<>(ORDER.codepointCompare));
        for (String s : sorted) {
            add(s);
        }
        System.out.println("\n" + title);
        PrintWriter pw = new PrintWriter(System.out);
        showCounts(pw, false, "the above emoji");
        pw.close();
    }

    //	private static <T> void showCount(Counter<T> majorsNoSkin) {
    //		for (T maj : majorsNoSkin) {
    //			long count = majorsNoSkin.get(maj);
    //			System.out.println("||" + count + "||" + maj + "||");
    //		}
    //		System.out.println("||TOTAL||" + majorsNoSkin.getTotal() + "||");
    //	}

    public static void countVs() {
        // find the items whose filtered form needs a VS
        int countPlain = 0;
        int countFirst = 0;
        int countFull = 0;
        int countOther = 0;
        for (String itemFull : EMOJI_DATA_BETA.getAllEmojiWithoutDefectives()) {
            String itemWithout = itemFull.replace(Emoji.EMOJI_VARIANT_STRING, "");
            if (itemWithout.equals(itemFull)) {
                countPlain++;
                continue;
            }
            //without=first=full
            //without=first≠full
            //without≠first≠full
            //without≠first=full
            String itemFirst = EMOJI_DATA_BETA.getOnlyFirstVariant(itemFull);
            if (!itemFirst.equals(itemFull)) {
                if (!itemFirst.equals(itemWithout)) {
                    countOther++;
                    showLine(countOther, "without≠first≠full", itemWithout, itemFirst, itemFull);
                }
                countFirst++;
                showLine(countFirst, "without=first≠full", itemWithout, itemFirst, itemFull);
                continue;
            }
            ++countFull;
            showLine(countFull, "without≠first=full", itemWithout, itemFirst, itemFull);
        }
        System.out.println("countPlain: " + countPlain);
        System.out.println("without≠first=full: " + countFull);
        System.out.println("without=first≠full: " + countFirst);
        System.out.println("without≠first≠full: " + countOther);

    }
    private static void showLine(int countFirst, String title, String itemWithout, String itemFirst, String itemFull) {
        System.out.println(title 
                + "\t" + countFirst
                + "\t" + Utility.hex(itemWithout, " ")
                + "\t" + Utility.hex(itemFirst, " ")
                + "\t" + Utility.hex(itemFull, " ")
                + "\t(" + itemFull + ")"
                + "\t" + EMOJI_DATA_BETA.getName(itemFull));
    }

    public void addAll(Iterable<String> chars) {
        for (String s : chars) {
            add(s);
        }        
    }

}
