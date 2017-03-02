package org.unicode.tools.emoji;

import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.Counter;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.EmojiOrder.MajorGroup;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;

public class CountEmoji {
    enum MyOptions {
        nonincrementalCount(new Params()),
        countVs(new Params()),
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
        if (MyOptions.nonincrementalCount.option.doesOccur()) {
            countNonincremental();
            done=true;
        }
        if (!done) {
            countNew();
        }
    }

    private static void countNonincremental() {
        UnicodeSet all = EmojiData.EMOJI_DATA.getAllEmojiWithDefectives();

        UnicodeSet missing = new UnicodeSet();
        for (String zwj : EmojiData.EMOJI_DATA.getZwjSequencesNormal()) {
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
        System.out.println("ZSeq Count: " + EmojiData.EMOJI_DATA.getZwjSequencesNormal().size());
        System.out.println("NonIncremental count: " + missing.size());
    }

    private static void countNew() {
        System.out.println(EmojiData.EMOJI_DATA.getEmojiComponents().size()
                + "\t" + EmojiData.EMOJI_DATA.getEmojiComponents());
        // TODO Auto-generated method stub
        EmojiData EMOJI_DATA_PREVIOUS = EmojiData.of(Emoji.VERSION_TO_GENERATE_PREVIOUS);
        UnicodeSet current = new UnicodeSet(EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives())
                .addAll(EmojiData.EMOJI_DATA.getEmojiComponents())
                .freeze();
        UnicodeSet previous = new UnicodeSet(EMOJI_DATA_PREVIOUS.getAllEmojiWithoutDefectives())
                .addAll(EmojiData.EMOJI_DATA.getEmojiComponents())
                .freeze();
        UnicodeSet ARE_NEW = new UnicodeSet(current)
                .removeAll(previous)
                .freeze();

        countItems("new", ARE_NEW);
        countItems("v4.0", previous);
        countItems("v5.0", current);
    }

    enum Categories {
        character, 
        keycap_seq,
        flag_seq,
        tag_seq, 
        mod_seq, 
        zwj_seq_gender("zwj seq + gender"), 
        zwj_seq_mod("zwj seq + modifier"),
        zwj_seq_mod_gender("zwj seq + gender, modifier"),
        zwj_seq_other,
        component, 
        typical_dup;

        final String name;
        Categories() {
            this(null);
        }
        Categories(String _name) {
            name = _name;
        }
        @Override
        public String toString() {
            return name == null ? name().replace('_', ' ') : name;
        }
        //final Counter<String> categories = new Counter<>();
        final Counter<MajorGroup> majors = new Counter<>();
        final UnicodeMap<MajorGroup> sets= new UnicodeMap<>();
        static void add(String s) {
            String cat = EmojiOrder.STD_ORDER.getCategory(s);
            MajorGroup maj = cat == null ? MajorGroup.Other : EmojiOrder.STD_ORDER.getMajorGroupFromCategory(cat);
            String noVariants = EmojiData.EMOJI_DATA.removeEmojiVariants(s);
            Categories bucket = null;
            if (EmojiData.isTypicallyDuplicate(s)) {
                bucket = typical_dup;
            } else if (EmojiData.EMOJI_DATA.getEmojiComponents().contains(noVariants)) {
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
                boolean gender = Emoji.GENDER_MARKERS.containsSome(noVariants);
                boolean zwj = noVariants.contains(Emoji.JOINER_STR);
                if (zwj && mods && gender) {
                    bucket = zwj_seq_mod_gender;
                } else if (zwj && mods && !gender) {
                    bucket = zwj_seq_mod;
                } else if (zwj && !mods && gender) {
                    bucket = zwj_seq_gender;
                } else if (zwj && !mods && !gender) {
                    bucket = zwj_seq_other;
                } else if (!zwj && mods && gender) {
                    throw new IllegalArgumentException();
                } else if (!zwj && mods && !gender) {
                    bucket = mod_seq;
                } else if (!zwj && !mods && gender) {
                    throw new IllegalArgumentException();
                } else {
                    throw new IllegalArgumentException();
                }
            }
            //			bucket.categories.add(cat, 1);
            bucket.sets.put(s, maj);
            bucket.majors.add(maj, 1);
            return;
        }

        static void clear() {
            for (Categories evalue : values()) {
                //				evalue.categories.clear();
                evalue.sets.clear();
                evalue.majors.clear();
            }
        }

        static void showCount(String title, String sep) {
            MajorGroup[] groups = MajorGroup.values();
            System.out.println();
            System.out.print(title);
            for (MajorGroup maj : groups) {
                System.out.print(sep + maj);
            }
            System.out.println();

            for (Categories evalue : values()) {
                System.out.print(evalue);
                for (MajorGroup maj : groups) {
                    long count = evalue.majors.get(maj);
                    System.out.print(sep + count);
                }
                System.out.println();
            }
            System.out.println();
            for (Categories evalue : values()) {
                System.out.print(evalue);
                for (MajorGroup maj : groups) {
                    System.out.print(sep + evalue.sets.getSet(maj).toPattern(false));
                }
                System.out.println();
            }

        }
    }

    private static void countItems(String title, UnicodeSet uset) {
        Categories.clear();
        Set<String> sorted = uset.addAllTo(new TreeSet<>(EmojiOrder.STD_ORDER.codepointCompare));
        for (String s : sorted) {
            Categories.add(s);
        }
        Categories.showCount(title, "\t");
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
        for (String itemFull : EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives()) {
            String itemWithout = itemFull.replace(Emoji.EMOJI_VARIANT_STRING, "");
            if (itemWithout.equals(itemFull)) {
                countPlain++;
                continue;
            }
            //without=first=full
            //without=first≠full
            //without≠first≠full
            //without≠first=full
            String itemFirst = EmojiData.EMOJI_DATA.getOnlyFirstVariant(itemFull);
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
                + "\t" + EmojiData.EMOJI_DATA.getName(itemFull));
    }
}
