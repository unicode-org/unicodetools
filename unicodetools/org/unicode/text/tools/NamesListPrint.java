package org.unicode.text.tools;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.With;
import org.unicode.props.UnicodeRelation;
import org.unicode.text.UCA.NamesList;
import org.unicode.text.UCA.NamesList.Comment;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.UCD.UCD_Types;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Objects;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class NamesListPrint {

    public static void main(String[] args) {
        NamesList nl = new NamesList("NamesList", Settings.latestVersion);
        if (true) {
            printall(nl, 1000);
            return;
        }
        for (Entry<Integer, Set<String>> error : nl.errors.keyValuesSet()) {
            System.err.println(Utility.hex(error.getKey()) + "\t" + error.getValue());
        }
        NamesList nl2 = new NamesList("NamesList", Settings.lastVersion);

        UnicodeMap<String> output = new UnicodeMap<>();
        compare(nl.informalAliases, nl2.informalAliases, output, Comment.alias.displaySymbol);
        compare(nl.informalComments, nl2.informalComments, output, Comment.comment.displaySymbol);
        compare(nl.informalXrefs, nl2.informalXrefs, output, Comment.xref.displaySymbol);
        compare(nl.subheads, nl2.subheads, output, "??");
        compare(nl.subheadComments, nl2.subheadComments, output, "?*");

        for (String k : output) {
            System.out.println(Utility.hex(k) + "\t" + k + "\t" + Default.ucd().getName(k) + "\n" + output.getValue(k));
        }
        if (true) {
            return;
        }
        print(nl);
    }


    private static void printall(NamesList nl, int max) {
        String lastBlock = null;
        String lastSubhead = null;
        String lastSubheadComments = null;
        int count = 0;
        for (String item : nl.codePoints) {
            if (count++ > max) break;
            lastBlock = showValue(nl.blockTitles.get(item), lastBlock, item);
            lastSubhead = showValue(nl.subheads.get(item), lastSubhead, item);
            lastSubheadComments = showValue(nl.subheadComments.get(item), lastSubheadComments, item);
            System.out.println(Utility.hex(item)
                    + "\t" + item
                    + "\t" + Default.ucd().getName(item)
                    );
            for (Comment comment : Comment.values()) {
                Set<String> commentLines = nl.getItem(comment, item);
                if (commentLines != null) {
                    for (String commentLine : commentLines) {
                        System.out.println("\t\t" + comment.displaySymbol + "\t" + commentLine);
                    }
                }
            }
            
        }
    }

    private static String showValue(String newValue, String lastBlock, String item) {
        if (!Objects.equal(lastBlock, newValue)) {
            if (newValue != null) {
                System.out.println(newValue);
            }
        }
        return newValue;
    }

    private static void compare(
            UnicodeRelation<String> subheads,
            UnicodeRelation<String> subheads2, 
            UnicodeMap<String> output,
            String sep) {
        UnicodeSet all = new UnicodeSet(subheads.keySet()).addAll(subheads2.keySet());
        Set<Pair<String,String>> seen = new HashSet();
        for (String cp : all) {
            String v1 = CldrUtility.ifNull(subheads.get(cp),"<empty>").toString();
            String v2 = CldrUtility.ifNull(subheads2.get(cp),"<empty>").toString();
            //if (v1 == null || v2 == null) continue;
            if (!Objects.equal(v1, v2)) {
                Pair<String, String> pair = Pair.of(v1, v2);
                if (seen.contains(pair)) {
                    continue;
                }
                seen.add(pair);
                String old = output.get(cp);
                output.put(cp, (old == null ? "" : old + "\n") 
                        + sep + "\t" + v1 
                        + "\n≠" 
                        + sep + "\t" + v2);
            }
        }
    }

    private static void compare(
            UnicodeMap<String> subheads,
            UnicodeMap<String> subheads2, 
            UnicodeMap<String> output,
            String sep) {
        UnicodeSet all = new UnicodeSet(subheads.keySet()).addAll(subheads2.keySet());
        Set<Pair<String,String>> seen = new HashSet();
        for (String cp : all) {
            String v1 = CldrUtility.ifNull(subheads.get(cp),"<empty>").toString();
            String v2 = CldrUtility.ifNull(subheads2.get(cp),"<empty>").toString();
            //if (v1 == null || v2 == null) continue;
            if (!Objects.equal(v1, v2)) {
                Pair<String, String> pair = Pair.of(v1, v2);
                if (seen.contains(pair)) {
                    continue;
                }
                seen.add(pair);
                String old = output.get(cp);
                output.put(cp, (old == null ? "" : old + "\n") 
                        + sep + "\t" + v1 
                        + "\n≠" 
                        + sep + "\t" + v2);
            }
        }
    }


    public static void print(NamesList nl) {
        String lastSubheadComment = null;
        String lastSubhead = null;
        String lastblock = null;
        for (Entry<Integer, String> fileComment : nl.fileComments.keyValueSet()) {
            System.out.println(fileComment.getKey() + "\t" + fileComment.getValue());
        }
        UnicodeSet all = new UnicodeSet()
        .addAll(nl.informalAliases.keySet())
        .addAll(nl.informalComments.keySet())
        .addAll(nl.informalXrefs.keySet())
        .addAll(nl.subheads.keySet())
        .addAll(nl.subheadComments.keySet())
        ;

        for (String key : all) {
            final int keyCodePoint = key.codePointAt(0);
            String block = Default.ucd().getBlock(keyCodePoint);
            if (!block.equals(lastblock)) {
                if (block != null && !block.equals("No_Block")) {
                    UnicodeSet set = Default.ucd().getBlockSet(block, new UnicodeSet());
                    System.out.print("\n======\n" 
                            + Utility.hex(set.getRangeStart(0))
                            + "\t" + block.replace('_', ' ')
                            + "\t" + Utility.hex(set.getRangeStart(1))
                            + "\n");
                }
                lastblock = block;
            }
            lastSubhead = showChangedItem(nl.subheads, keyCodePoint, lastSubhead);
            lastSubheadComment = showChangedItem(nl.subheadComments, keyCodePoint, lastSubheadComment);

            String realName = Default.ucd().getName(keyCodePoint);


            System.out.println(Utility.hex(key) + "\t" + NamesList.CODE.transform(key) + "\t" + realName);
            Set<String> informalComment = nl.informalComments.get(key);
            Set<String> informalXref = nl.informalXrefs.get(key);
            display(key, nl.informalAliases, Comment.alias);
            display(key, nl.informalComments, Comment.comment);
            display(key, nl.informalXrefs, Comment.xref);
        }

        //        lastSubhead = "";
        //        for (EntryRange dataItem : nl.subheads.entryRanges()) {
        //            if (dataItem.value == null || dataItem.value.equals(lastSubhead)) {
        //                continue;
        //            }
        //            System.out.println(Utility.hex(dataItem.codepoint) + "\t" + dataItem.value);
        //            lastSubhead = (String) dataItem.value;
        //        }

        for (Entry<Integer, Set<String>> dataItem : nl.errors.keyValuesSet()) {
            final Integer key = dataItem.getKey();
            final Set<String> values = dataItem.getValue();
            System.err.println(Utility.hex(key));
            for (String value : values) {
                System.err.println("\t" + value);
            }
        }
    }

    private static void display(String key, UnicodeRelation<String> informalAliases, Comment alias) {
        Set<String> values = informalAliases.get(key);
        if (values != null) {
            for (String value : values) {
                if (alias == Comment.xref) {
                    for (int cp : With.codePointArray(value)) {
                        String realName = Default.ucd().getName(cp);
                        System.out.println("\t\t\t" + alias.displaySymbol + "\t" 
                                + Utility.hex(cp) + " " + NamesList.CODE.transform(UTF16.valueOf(cp)) + " " + realName);
                    }
                } else {
                    for (String s : value.split("\n")) {
                        System.out.println("\t\t\t" + alias.displaySymbol + "\t" + s);
                    }
                }
            }
        }
    }

    public static String showChangedItem(UnicodeMap<String> map, final int keyCodePoint,
            String lastSubhead) {
        String subhead = map.get(keyCodePoint);
        if (!Objects.equal(subhead, lastSubhead)) {
            if (subhead != null) {
                System.out.println(subhead);
            }
            lastSubhead = subhead;
        }
        return lastSubhead;
    }

}
