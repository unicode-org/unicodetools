package org.unicode.jsp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.XEquivalenceClass;
import org.unicode.jsp.AlternateIterator.Builder;
import org.unicode.jsp.ScriptTester.CompatibilityLevel;
import org.unicode.jsp.ScriptTester.ScriptSpecials;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.Normalizer.Mode;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class Confusables implements Iterable<String>{
    public enum ScriptCheck {same, none};

    private static final XEquivalenceClass<String, Integer> equivalents = new XEquivalenceClass<String, Integer>();
    private final String source;
    private Mode normalizationCheck;
    private ScriptCheck scriptCheck = ScriptCheck.none;
    private UnicodeSet allowedCharacters = null;

    public UnicodeSet getAllowedCharacters() {
        return allowedCharacters;
    }

    public static UnicodeMap<String> getMap() {
        final UnicodeMap<String> result = new UnicodeMap<String>();
        for (final String s : equivalents) {
            final Set<String> others = new TreeSet<String>(equivalents.getEquivalences(s));
            final String list = "\u2051" + CollectionUtilities.join(others, "\u2051") + "\u2051";
            for (final String other : others) {
                result.put(other, list);
            }
        }
        result.freeze();
        return result;
    }

    public static Set<String> getEquivalents(String string) {
        final Set<String> result = equivalents.getEquivalences(string);
        return Collections.unmodifiableSet(result);
    }

    public Confusables setAllowedCharacters(UnicodeSet allowedCharacters) {
        this.allowedCharacters = allowedCharacters;
        return this;
    }

    public Mode getNormalizationCheck() {
        return normalizationCheck;
    }

    public Confusables setNormalizationCheck(Mode normalizationCheck) {
        this.normalizationCheck = normalizationCheck;
        return this;
    }

    static class MyReader extends FileUtilities.SemiFileReader {
        @Override
        protected boolean handleLine(int start, int end, String[] items) {
            final String type = items[2];
            if (!type.equals("MA")) {
                return true;
            }
            final String result = Utility.fromHex(items[1], 4, " ");
            for (int i = start; i <= end; ++i) {
                equivalents.add(UTF16.valueOf(i), result);
            }
            return true;
        }
    }

    static {
        new MyReader().process(Confusables.class, "confusables.txt");
    }

    public Confusables(String source) {
        this.source = Normalizer.normalize(source,Normalizer.NFD);
    }

    public double getMaxSize() {
        final AlternateIterator build = buildIterator();
        if (build == null) {
            return 0;
        }
        return build.getMaxSize();
    }

    @Override
    public Iterator<String> iterator() {
        final AlternateIterator build = buildIterator();
        if (build == null) {
            final Set<String> empty = Collections.emptySet();
            return empty.iterator();
        }
        return new MyFilteredIterator(build);
    }

    private AlternateIterator buildIterator() {
        final Builder builder = AlternateIterator.start();
        final List<Set<String>> table = new ArrayList<Set<String>>();
        int cp;
        for (int i = 0; i < source.length(); i += Character.charCount(cp)) {
            cp = source.codePointAt(i);
            final String cps = UTF16.valueOf(cp);
            final Set<String> confusables = equivalents.getEquivalences(cps);
            final Set<String> items = new HashSet<String>();
            for (final String confusable : confusables) {
                if (normalizationCheck != null && !Normalizer.isNormalized(confusable, normalizationCheck, 0)) {
                    continue;
                }
                if (allowedCharacters != null && !allowedCharacters.containsAll(confusable)) {
                    continue;
                }
                items.add(confusable);
            }
            if (items.size() == 0) {
                return null;
            }
            table.add(items);
        }

        // now filter for multiple scripts, if set
        if (scriptCheck != ScriptCheck.none) {
            if (!scriptTester.filterTable(table)) {
                return null;
            }
        }
        for (final Set<String> items : table) {
            builder.add(items);
        }
        final AlternateIterator build = builder.build();
        return build;
    }


    public List<Collection<String>> getAlternates() {
        final AlternateIterator build = buildIterator();
        if (build == null) {
            return Collections.emptyList();
        }
        return build.getAlternates();
    }

    public ScriptCheck getScriptCheck() {
        return scriptCheck;
    }

    public Confusables setScriptCheck(ScriptCheck scriptCheck) {
        this.scriptCheck = scriptCheck;
        return this;
    }

    public static boolean scriptOk(String confusable, ScriptCheck scriptCheck) {
        return scriptCheck == ScriptCheck.none
                || scriptTester.isOk(confusable);
    }

    public static ScriptTester scriptTester = ScriptTester.start(CompatibilityLevel.Highly_Restrictive, ScriptSpecials.on).get();

    class MyFilteredIterator extends FilteredIterator<String>{
        Set<String> alreadySeen;;

        public MyFilteredIterator(Iterator<String> base) {
            super(base);
        }

        @Override
        public String allow(String confusable) {
            if (alreadySeen == null) {
                alreadySeen = new HashSet<String>();
            }
            if (alreadySeen.contains(confusable)) {
                return null;
            }
            alreadySeen.add(confusable);

            final String nfcConfusable = Normalizer.normalize(confusable, Normalizer.NFC);
            if (!nfcConfusable.equals(confusable)) {
                if (alreadySeen.contains(nfcConfusable)) {
                    return null;
                }
                alreadySeen.add(nfcConfusable);
            }

            if (allowedCharacters != null && !allowedCharacters.containsAll(nfcConfusable)) {
                return null;
            }
            if (!scriptOk(nfcConfusable, scriptCheck)) {
                return null;
            }
            if (normalizationCheck != null && !Normalizer.isNormalized(nfcConfusable, normalizationCheck, 0)) {
                return null;
            }
            return nfcConfusable;
        }

    }

    public static class FilteredIterator<T> implements Iterator<T> {
        Iterator<T> base;
        T nextItem = null;

        public FilteredIterator(Iterator<T> base) {
            this.base = base;
            load();
        }

        @Override
        public boolean hasNext() {
            return nextItem != null;
        }

        @Override
        public T next() {
            final T temp = nextItem;
            load();
            return temp;
        }

        private void load() {
            while (base.hasNext()) {
                nextItem = allow(base.next());
                if (nextItem != null) {
                    return;
                }
            }
            nextItem = null;
        }

        public T allow(T item) {
            return item;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    public String getOriginal() {
        return source;
    }
}
