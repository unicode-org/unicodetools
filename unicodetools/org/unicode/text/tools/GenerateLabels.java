package org.unicode.text.tools;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.props.UnicodeRelation;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;

public class GenerateLabels {
    public static void main(String[] args) {
        EmojiData emojiData = EmojiData.of(Emoji.VERSION_LAST_RELEASED);
        final UnicodeSet exclude = new UnicodeSet("[[:c:][:z:][:di:]]").addAll(emojiData.getChars()).addAll(Emoji.REGIONAL_INDICATORS).freeze();
        UnicodeSet missingPunctuation = new UnicodeSet("[[:P:]&[:scx=Common:]]").removeAll(exclude);
        UnicodeSet missingSymbols = new UnicodeSet("[[:S:]&[:scx=Common:]-[:sc:]-[:sk:]]").removeAll(exclude);
        final UnicodeSet terminals = new UnicodeSet("[[:Terminal_Punctuation:]&[:scx=Common:]]").freeze();

        Splitter space = Splitter.on(' ').trimResults();
        Output<Set<String>> lastLabel= new Output<>();
        lastLabel.value = new LinkedHashSet<String>();
        Multimap<String,String> characterToRelation = LinkedHashMultimap.create(); 
        Set<String> others = new HashSet<>();
        
        for (String filename : Arrays.asList("label-symbols.txt")) {
            int lineCount = 0;
            int lineNumber = 0;
            for (String line : FileUtilities.in(EmojiAnnotations.class, filename)) {
                line = line.trim();
                lineNumber++;
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.contains("closed")) {
                    int debug = 0;
                }
                lineCount++;
                String oldLine = line;
                line = Emoji.getLabelFromLine(lastLabel, line);
                if (!line.equals(oldLine)) {
                    for (String label : lastLabel.value) {
                        if (label.endsWith("-Other")) {
                            others.add(label);
                        }
                    }
                }
                line = Emoji.UNESCAPE.transform(line);
                for (int cp : CharSequences.codePoints(line)) {
                    if (cp == ' ') {
                        continue;
                    }
                    if (exclude.contains(cp)) {
                        continue;
                    }
                    for (String label : lastLabel.value) {
                        characterToRelation.put(label, UTF16.valueOf(cp));
                    }
                    missingPunctuation.remove(cp);
                    missingSymbols.remove(cp);
                }
            }
        }
        for (Entry<String, Collection<String>> labelAndValues : characterToRelation.asMap().entrySet()) {
            final String key = labelAndValues.getKey();
            final Collection<String> value = labelAndValues.getValue();
            if (!others.contains(key)) {
                for (String otherLabel : others) {
                    characterToRelation.remove(otherLabel, value);
                }
            }
        }
        final UnicodeSet compat = new UnicodeSet("[:nfkcqc=n:]").freeze();
        characterToRelation.putAll("Punctuation-Compat", new UnicodeSet(missingPunctuation).retainAll(compat));
        extract(characterToRelation, "Punctuation-Other", terminals, "Punctuation-Terminal");

        extract(characterToRelation, "Symbol-Musical", new UnicodeSet("[:block=Byzantine Musical Symbols:]"), "Musical-Byzantine");
        extract(characterToRelation, "Symbol-Musical", new UnicodeSet("[:block=Ancient Greek Musical Notation:]"), "Musical-Ancient");
        
        characterToRelation.putAll("Punctuation-Missing", new UnicodeSet(missingPunctuation).removeAll(terminals).removeAll(compat));
        
        characterToRelation.putAll("Symbol-Currency", new UnicodeSet("[:Sc:]").removeAll(exclude));
        characterToRelation.putAll("Symbol-Modifier", new UnicodeSet("[:Sk:]").removeAll(exclude));
        characterToRelation.putAll("Symbol-RI", Emoji.REGIONAL_INDICATORS);
        characterToRelation.putAll("Symbol-Compat", new UnicodeSet(missingSymbols).retainAll(compat));
        characterToRelation.putAll("Symbol-Missing", new UnicodeSet(missingSymbols).removeAll(compat));
        
        Joiner spaceJoiner = Joiner.on(' ');
        for (Entry<String, Collection<String>> labelAndValues : characterToRelation.asMap().entrySet()) {
            final String key = labelAndValues.getKey();
            final Collection<String> value = labelAndValues.getValue();
            //writeButton(key,sorted);
            writeChars(key, value);
        }
    }

    private static void writeChars(String key, Collection<String> value) {
        Set<String> sorted = EmojiOrder.sort(EmojiOrder.STD_ORDER.codepointCompare, new UnicodeSet().addAll(value));
        int count = key.length() + 1;
        System.out.print(key + "\t");
        for (String s : sorted) {
            if (count > 40) {
                System.out.println();
                count = 0;
            } else {
                System.out.print(" ");
            }
            ++count;
            System.out.print(s);
        }
        System.out.println();
    }

    private static void writeButton(String key, Set<String> sorted) {
        System.out.println("<h1>" + key + "</h1>");
        System.out.println("<script>");
        int start = -2;
        int last = -2;
        for (String s : sorted) {
            // writeButtons(0x1F600,0x1F60F)
            int cp = s.codePointAt(0);
            if (cp == last + 1) {
                last = cp;
                continue;
            }
            if (start >= 0) {
                System.out.println("writeButtons(" + start + "," + last + ");");
            }
            start = last = cp;
        }
        System.out.println("writeButtons(" + start + "," + last + ");");
        System.out.println("</script>");        
    }

    private static void extract(Multimap<String, String> characterToRelation, String oldLabel, UnicodeSet unicodeSet, String newLabel) {
        Collection<String> old = characterToRelation.get(oldLabel);
        if (old.isEmpty()) {
            throw new IllegalArgumentException();
        }
        characterToRelation.putAll(newLabel, new UnicodeSet(unicodeSet).retainAll(old));
        for (String s : unicodeSet) {
            characterToRelation.remove(oldLabel, s);
        }
    }
}
