package org.unicode.text.tools;

import java.util.Comparator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.tools.emoji.EmojiAnnotations;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.UnicodeSet;

public class AnnotationCheck {
    static final Comparator<String> SIZE = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            UnicodeSet us1 = EmojiAnnotations.ANNOTATIONS_TO_CHARS.getUnicodeSet(o1);
            UnicodeSet us2 = EmojiAnnotations.ANNOTATIONS_TO_CHARS.getUnicodeSet(o2);
            return -us1.compareTo(us2);
        }
        
    };
    public static void main(String[] args) {
        Relation<String,String> containerToContains = Relation.of(new TreeMap(SIZE), TreeSet.class);
        for (Entry<String, UnicodeSet> entry1 : EmojiAnnotations.ANNOTATIONS_TO_CHARS.getStringUnicodeSetEntries()) {
            final String key1 = entry1.getKey();
            final UnicodeSet value1 = entry1.getValue();
            if (value1.size() < 10) {
                continue;
            }
            for (Entry<String, UnicodeSet> entry2 : EmojiAnnotations.ANNOTATIONS_TO_CHARS.getStringUnicodeSetEntries()) {
                final String key2 = entry2.getKey();
                final UnicodeSet value2 = entry2.getValue();
                if (value2.size() < 10) {
                    continue;
                }
                if (key1 == key2) {
                    continue;
                }
                if (value1.containsAll(value2)) {
                    containerToContains.put(key1, key2);
                }
            }
        }
        System.out.println(CollectionUtilities.join(containerToContains.keyValuesSet(), "\n"));
    }
}
