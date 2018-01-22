package org.unicode.tools.emoji;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;
import org.unicode.tools.emoji.GenerateEmojiData.ZwjType;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
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
        if (!done) {
            countNew();
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
    private static final String TABLE_TOTALS2 = 
            "<h3><a href='#totals' name='totals'>Totals</a></h3>\n"
                    + "<p>Totals for the above emoji. For information on the categories, see <a href='../format.html#col-totals'>Totals</a>.</p>\n";

    public void showCounts(PrintWriter out, boolean showCharacters) {
        out.println(TABLE_TOTALS2 + "<table>\n");

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
            if ((evalue == Category.component || evalue == Category.typical_dup_group | evalue == Category.typical_dup_sign) 
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
    
    enum Category {
        character("char"), 
        keycap_seq,
        flag_seq,
        tag_seq, 
        mod_seq("char&skin"), 
        zwj_seq_gender("zwj:…" + GENDER), 
        zwj_seq_gender_mod("zwj:…" + GENDER + "&skin"),
        zwj_seq_role("zwj:"+WO_MAN+'…'),
        zwj_seq_role_mod("zwj:"+WO_MAN+'…' + "&skin"), 
        zwj_seq_fam("zwj:"+Emoji.NEUTRAL_FAMILY), 
        //zwj_seq_fam_mod("zwj:"+Emoji.NEUTRAL_FAMILY + "&skin"), 
        zwj_seq_other("zwj:other"),
        zwj_seq_mod("zwj:other&skin"),
        component, 
        typical_dup_sign,
        typical_dup_group, 
        ;

        final public String name;
        final public String html;
        Category() {
            this(null);
        }
        Category(String _name) {
            name = _name == null ? name().replace('_', ' ') : _name;
            html = TransliteratorUtilities.toHTML.transform(name);
        }
        @Override
        public String toString() {
            return html;
        }
        public String toStringPlain() {
            return name;
        }
        static Category getBucket(String s) {
            String noVariants = EmojiData.removeEmojiVariants(s);
            Category bucket = null;
            if (EmojiData.isTypicallyDuplicateGroup(s)) {
                bucket = typical_dup_group;
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
                boolean mods = EmojiData.MODIFIERS.containsSome(noVariants);
                String butFirst = noVariants.substring(Character.charCount(noVariants.codePointAt(0)));
                boolean gender = Emoji.GENDER_MARKERS.containsSome(noVariants);
                boolean zwj = noVariants.contains(Emoji.JOINER_STR);
                boolean role = Emoji.MAN_OR_WOMAN.containsSome(noVariants) && !Emoji.FAMILY_MARKERS.containsSome(butFirst);
                boolean family = Emoji.FAMILY_MARKERS.containsSome(s) && !role;
                if (!zwj) {
                    if (mods) {
                        bucket = mod_seq;
                    } else {
                        throw new IllegalArgumentException();
                    }
                } else { // zwj
                    if (gender) {
                        if (mods) {
                            bucket = zwj_seq_gender_mod;
                        } else { //  if (!gender)
                            bucket = zwj_seq_gender;
                        }
                    } else if (role) { // !mods
                        if (mods) {
                            bucket = zwj_seq_role_mod;
                        } else {
                            bucket = zwj_seq_role;
                        }
                    } else if (family) { // !mods
                        if (mods) {
                            throw new IllegalArgumentException("no zwj_seq_fam_mod yet, change when there are");
                            // bucket = zwj_seq_fam_mod;
                        } else {
                            bucket = zwj_seq_fam;
                        }
                    } else { // !mods
                        if (mods) {
                            bucket = zwj_seq_mod;
                        } else {
                            bucket = zwj_seq_other;
                        }
                    }
                }
            }
            return bucket;
        }
    }

    private void countItems(String title, UnicodeSet uset) {
        Set<String> sorted = uset.addAllTo(new TreeSet<>(ORDER.codepointCompare));
        for (String s : sorted) {
            add(s);
        }
        System.out.println("\n" + title);
        PrintWriter pw = new PrintWriter(System.out);
        showCounts(pw, false);
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
