package org.unicode.jsp;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.MultiComparator;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

public class Subheader implements Iterable<String> {
    static final boolean DEBUG = false;
    Matcher subheadMatcher = Pattern.compile("(@+)\\s+(.*)").matcher("");
    Matcher hexMatcher = Pattern.compile("([A-Z0-9]+).*").matcher("");
    Map<Integer, String> codePoint2Subblock = new HashMap<Integer, String>();
    Map<String, UnicodeSet> subblock2UnicodeSet = new TreeMap<String, UnicodeSet>();
    Map<String,Set<String>> block2subblock = new TreeMap<String, Set<String>>();
    Map<String,Set<String>> subblock2block = new TreeMap<String, Set<String>>();

    public Subheader(String unicodeDataDirectory) {
        try {
            subblock2UnicodeSet = getDataFromFile(unicodeDataDirectory + "NamesList.txt");
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        fillTables();
    }

    //  Subheader(String[][] data) {
    //    subblock2UnicodeSet = new TreeMap<String, UnicodeSet>();
    //    for (String[] pair : data) {
    //      subblock2UnicodeSet.put(pair[0], new UnicodeSet(pair[1]));
    //    }
    //    fillTables();
    //  }

    public Subheader(InputStream resourceAsStream) {
        try {
            subblock2UnicodeSet = getDataFromStream(resourceAsStream);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        fillTables();
    }

    static final Comparator<CharSequence> SHORTEST_FIRST = new Comparator<CharSequence>() {
        @Override
        public int compare(CharSequence arg0, CharSequence arg1) {
            return arg0.length() - arg1.length();
        }
    };

    static final MultiComparator SHORTEST = new MultiComparator(SHORTEST_FIRST, Collator.getInstance(ULocale.ENGLISH));

    private void fillTables() {
        // fix plurals & casing
        final Relation<String,String> caseless = new Relation(new TreeMap<String,String>(), TreeSet.class, SHORTEST);

        for (final String subhead : subblock2UnicodeSet.keySet()) {
            final String norm = getSkeleton(subhead);
            caseless.put(norm, subhead);
        }

        for (final String norm : caseless.keySet()) {
            final Set<String> set = caseless.getAll(norm);
            if (set.size() == 1) {
                continue;
            }
            if (DEBUG) {
                System.out.println("***Merging similar names:\t" + set + "\tskeleton:" + norm);
            }

            UnicodeSet best = null;
            String bestName = null;
            for (final String name : set) {
                if (best == null) {
                    best = subblock2UnicodeSet.get(name);
                    bestName = name;
                } else {
                    final UnicodeSet other = subblock2UnicodeSet.get(name);
                    best.addAll(other);
                    subblock2UnicodeSet.remove(name);
                }
            }
        }

        // protect the core data, since we allow iteration
        for (final String subhead : subblock2UnicodeSet.keySet()) {
            final UnicodeSet unicodeSet = subblock2UnicodeSet.get(subhead);
            unicodeSet.freeze();
            if (DEBUG) {
                System.out.println("\t" + subhead + "\t" + unicodeSet.toPattern(false));
            }
        }

        for (final String subblock : subblock2UnicodeSet.keySet()) {
            final UnicodeSet uset = subblock2UnicodeSet.get(subblock);
            for (final UnicodeSetIterator it = new UnicodeSetIterator(uset); it.next();) {
                codePoint2Subblock.put(it.codepoint, subblock);

                final String block = UCharacter.getStringPropertyValue(UProperty.BLOCK, it.codepoint, UProperty.NameChoice.LONG).toString().replace('_', ' ').intern();

                Set<String> set = block2subblock.get(block);
                if (set == null) {
                    block2subblock.put(block, set = new TreeSet<String>());
                }
                set.add(subblock);

                set = subblock2block.get(subblock);
                if (set == null) {
                    subblock2block.put(subblock, set = new TreeSet<String>());
                }
                set.add(block);
            }
        }
    }

    static final Pattern NON_ALPHANUM = Pattern.compile("[^" +
            "\\p{Ll}\\p{Lu}\\p{Lt}\\p{Lo}\\p{Lm}" +
            "\\p{Me}\\p{Mc}\\p{Mn}" +
            "\\p{Nd}" +
            "]+");

    static final Pattern TERMINATION = Pattern.compile("(ies|es|s|y)_");
    static final Pattern INITIAL_GORP = Pattern.compile("$[A-Z]\\.");

    private String getSkeleton(String input) {
        String result = INITIAL_GORP.matcher(input).replaceAll("_");

        result = NON_ALPHANUM.matcher(result).replaceAll("_").toLowerCase(Locale.ENGLISH);
        if (!result.endsWith("_")) {
            result += "_";
        }
        if (!result.startsWith("_")) {
            result = "_" + result;
        }
        result = TERMINATION.matcher(result).replaceAll("_");

        result = result.replace("_mark_and_sign_", "_mark_");
        result = result.replace("_sign_", "_mark_");
        result = result.replace("_symbol_", "_mark_");
        result = result.replace("_additional_", "_");
        result = result.replace("_extended_", "_");
        result = result.replace("_extensions_for_", "_");
        result = result.replace("_further_", "_");
        result = result.replace("_other_", "_");
        result = result.replace("_glyphs_for_", "_");

        result = result.replace("_poetry_", "_poetic_");

        result = result.replace("_ancient_", "_historic_");
        result = result.replace("_archaic_", "_historic_");
        result = result.replace("_general_use_", "_general_");



        return result;
    }

    private Map<String, UnicodeSet> getDataFromFile(String filename) throws FileNotFoundException, IOException {
        final InputStream is = new FileInputStream(filename);
        return getDataFromStream(is);
    }

    private Map<String, UnicodeSet> getDataFromStream(InputStream is) throws IOException {
        final Reader reader = new InputStreamReader(is);
        final BufferedReader in = new BufferedReader(reader);
        final Map<String, UnicodeSet> subblock2UnicodeSet2 = new TreeMap<String, UnicodeSet>();
        String subblock = "?";
        while (true) {
            final String line = in.readLine();
            if (line == null) {
                break;
            }
            if (subheadMatcher.reset(line).matches()) {
                subblock = subheadMatcher.group(1).equals("@") ? subheadMatcher.group(2) : "?";
                continue;
            }
            if (subblock.length() != 0 && hexMatcher.reset(line).matches()) {
                final int cp = Integer.parseInt(hexMatcher.group(1), 16);
                UnicodeSet uset = subblock2UnicodeSet2.get(subblock);
                if (uset == null) {
                    subblock2UnicodeSet2.put(subblock, uset = new UnicodeSet());
                }
                uset.add(cp);
            }
        }
        in.close();
        return subblock2UnicodeSet2;
    }

    public String getSubheader(int codepoint) {
        return codePoint2Subblock.get(codepoint);
    }

    @Override
    public Iterator<String> iterator() {
        return subblock2UnicodeSet.keySet().iterator();
    }

    public UnicodeSet getUnicodeSet(String subhead) {
        return subblock2UnicodeSet.get(subhead);
    }
}