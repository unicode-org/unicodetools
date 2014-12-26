package org.unicode.text.tools;

import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.With;
import org.unicode.text.UCA.NamesList;
import org.unicode.text.UCA.NamesList.Comment;
import org.unicode.text.UCD.Default;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Objects;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class NamesListPrint {
    
    public static void main(String[] args) {
        NamesList nl = new NamesList("NamesList", Settings.latestVersion);
        NamesList nl2 = new NamesList("NamesList", Settings.lastVersion);
        
        UnicodeMap<String> output = new UnicodeMap();
        compare(nl.informalAliases, nl2.informalAliases, output, Comment.alias.symbol);
        compare(nl.informalComments, nl2.informalComments, output, Comment.comment.symbol);
        compare(nl.informalXrefs, nl2.informalXrefs, output, Comment.xref.symbol);
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

    private static void compare(
            UnicodeMap<String> a,
            UnicodeMap<String> b, 
            UnicodeMap<String> output,
            String sep) {
        UnicodeSet all = new UnicodeSet(a.keySet()).addAll(b.keySet());
        for (String cp : all) {
            String v1 = a.get(cp);
            String v2 = b.get(cp);
            if (v1 == null || v2 == null) continue;
            if (!Objects.equal(v1, v2)) {
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
            String informalComment = nl.informalComments.get(key);
            String informalXref = nl.informalXrefs.get(key);
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

    private static void display(String key, UnicodeMap<String> data, Comment alias) {
        String value = data.get(key);
        if (value != null) {
            if (alias == Comment.xref) {
                for (int cp : With.codePointArray(value)) {
                String realName = Default.ucd().getName(cp);
                System.out.println("\t\t\t" + alias.symbol + "\t" 
                + Utility.hex(cp) + " " + NamesList.CODE.transform(UTF16.valueOf(cp)) + " " + realName);
                }
            } else {
                for (String s : value.split("\n")) {
                    System.out.println("\t\t\t" + alias.symbol + "\t" + s);
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
