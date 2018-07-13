package org.unicode.text.tools;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

import org.unicode.tools.emoji.EmojiData;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class StringTree {
    boolean alsoTerminates;
    private UnicodeMap<StringTree> data = new UnicodeMap<>();

    public StringTree(boolean terminates) {
        alsoTerminates = terminates;
    }

    public StringTree() {
        alsoTerminates = false;
    }

    public StringTree addAll(Collection<String> sources) {
        for (String s : sources) {
            add(s);
        }
        return this;
    }

    public StringTree add(String... sources) {
        return addAll(Arrays.asList(sources));
    }

    public StringTree add(String s) {
        int base = s.codePointAt(0);
        int endBase = Character.charCount(base);
        StringTree old = data.get(base);
        boolean noRemainder = endBase == s.length();
        if (old == null) {
            old = new StringTree(noRemainder);
            data.put(base, old);
        }
        if (noRemainder) {
            old.alsoTerminates = true;
        } else {
            old.add(s.substring(endBase));
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        toString(result, "\n");
        return SHOW.transform(result.toString());
    }

    static final Transliterator SHOW = Transliterator.createFromRules("foo", "([[:c:][:z:][:di:]-[\\ \\x0A]]) > &hex($1);", Transliterator.FORWARD);

    private void toString(StringBuilder result, String indent) {
        for (EntryRange<StringTree> entry : data.entryRanges()) {
            result.append(indent);
            if (entry.codepoint == entry.codepointEnd) {
                result.appendCodePoint(entry.codepoint);
            } else {
                result.append('[');
                result.appendCodePoint(entry.codepoint);
                result.append('-');
                result.appendCodePoint(entry.codepoint);
                result.append(']');
            }
            if (entry.value.alsoTerminates) {
                result.append("*");
            }
            if (!entry.value.data.isEmpty()) {
                entry.value.toString(result, indent + "  ");
            }
        }        
    }

    public String getRegex() {
        StringBuilder result = new StringBuilder();
        getRegex(result);
        return result.toString();
    }

    private void getRegex(StringBuilder result) {
        // find out what's there
        int countWithChildren = 0;
        UnicodeSet singles = new UnicodeSet();

        for (EntryRange<StringTree> entry : data.entryRanges()) {
            if (entry.value.data.isEmpty()) {
                singles.add(entry.codepoint, entry.codepointEnd);
            } else if (!entry.value.data.isEmpty()) {
                ++countWithChildren;
            }
        }
        boolean paren = countWithChildren + (singles.isEmpty() ? 0 : 1) > 1;
        if (paren) {
            result.append('(');
        }
        boolean needBar = false;
        if (!singles.isEmpty()) {
            if (singles.size() == 1) {
                int cp = singles.charAt(0);
                addCodePoint(result, cp);
            } else {
                addCodePoint(result, singles);
            }
            --countWithChildren;
            needBar = true;
        }
        if (countWithChildren >= 0) {
            for (EntryRange<StringTree> entry : data.entryRanges()) {
                if (entry.value.data.isEmpty()) {
                    continue;
                }
                if (needBar) {
                    result.append('|');
                }
                needBar = true;
                if (entry.codepoint == entry.codepointEnd) {
                    addCodePoint(result, entry.codepoint);
                } else {
                    result.append('[');
                    addCodePoint(result, entry.codepoint);
                    result.append('-');
                    addCodePoint(result, entry.codepointEnd);
                    result.append(']');
                }
                if (entry.value.alsoTerminates) {
                    result.append('(');
                }
                entry.value.getRegex(result);
                if (entry.value.alsoTerminates) {
                    result.append(")?");
                }
            }
        }
        if (paren) {
            result.append(')');
        }
    }

    private void addCodePoint(StringBuilder result, UnicodeSet singles) {
        result.append('[');
        for (UnicodeSetIterator entry = new UnicodeSetIterator(singles); entry.nextRange();) {
            addCodePoint(result, entry.codepoint);
            if (entry.codepoint != entry.codepointEnd) {
                result.append('-');
                addCodePoint(result, entry.codepointEnd);
            }
        }
        result.append(']');
    }

    private StringBuilder addCodePoint(StringBuilder result, int cp) {
        switch (cp) {
        case '*' : result.append('\\');
        }
        return result.appendCodePoint(cp);
    }

    public static void main(String[] args) {
        Collection<String> tests = Arrays.asList("a", "xyz", "bc", "bce", "bcd", "p", "q", "r");
        check(tests);
        HashSet<String> tests2 = EmojiData.EMOJI_DATA.getAllEmojiWithoutDefectives().addAllTo(new HashSet<String>());
        LinkedHashSet<String> tests3 = new LinkedHashSet<>();
        LinkedHashSet<String> tests4 = new LinkedHashSet<>();
        tests2.forEach(s -> {
            if (s.startsWith("👯")) tests3.add(s); 
            tests4.add(EmojiData.EMOJI_DATA.addEmojiVariants(s));
        });
        check(tests3);
        check(tests4);
    }

    private static void check(Collection<String> tests) {
        StringTree s = new StringTree().addAll(tests);
        System.out.println(s.toString());
        String pattern = s.getRegex();
        System.out.println(pattern);
        Pattern p = Pattern.compile(pattern);
        UnicodeSet failures = new UnicodeSet();
        for (String test : tests) {
            if (!p.matcher(test).matches()) {
                failures.add(test);
            }
        }
        if (failures.size() > 0) {
            System.out.println("Fails: " + SHOW.transform(failures.toPattern(false)));
        }
    }
}
