package org.unicode.parse;

import com.ibm.icu.text.UnicodeSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.unicode.parse.Tokenizer.Result;

public class EBNF {
    boolean DEBUG = true;

    private Map<String, Pick> map = new HashMap<>();
    private Set<String> variables = new HashSet<>();
    private Pick pick = null;
    private Tokenizer t = new Tokenizer();

    public String getInternal() {
        return pick.getInternal(0, new HashSet<>());
    }

    /*
    + "rule = string '=' alternation;";
    + "alternation = sequence (weight? ('|' sequence weight?)+)?;"
    + "sequence = (core quantifier*)+;"
    + "core = string | unicodeSet | '(' alternation ')';"
    + "quantifier = range | '*' | '+' | '?' ;"
    + "range = '{' int (',' int?)? '}'

    + "star = '*' weight*;"
    + "plus = '+' weight*;"
    + "maybe = '?' weight?;"


     *      Match 0 or more times
    +      Match 1 or more times
    ?      Match 1 or 0 times
    {n}    Match exactly n times
    {n,}   Match at least n times
    {n,m}  Match at least n but not more than m times

     */

    public EBNF addRules(String rules) {
        t.setSource(rules);
        while (addRule()) {}
        return this; // for chaining
    }

    public EBNF build() {
        // check that the rules match the variables, except for $root in rules
        Set<String> ruleSet = map.keySet();
        // add also
        variables.add("$root");
        variables.addAll(t.getLookedUpItems());
        if (!ruleSet.equals(variables)) {
            String msg = showDiff(variables, ruleSet);
            if (msg.length() != 0) msg = "Error: Missing definitions for: " + msg;
            String temp = showDiff(ruleSet, variables);
            if (temp.length() != 0) temp = "Warning: Defined but not used: " + temp;
            if (msg.length() == 0) msg = temp;
            else if (temp.length() != 0) {
                msg = msg + "; " + temp;
            }
            error(msg);
        }

        if (!ruleSet.equals(variables)) {
            String msg = showDiff(variables, ruleSet);
            if (msg.length() != 0) msg = "Missing definitions for: " + msg;
            String temp = showDiff(ruleSet, variables);
            if (temp.length() != 0) temp = "Defined but not used: " + temp;
            if (msg.length() == 0) msg = temp;
            else if (temp.length() != 0) {
                msg = msg + "; " + temp;
            }
            error(msg);
        }

        // replace variables by definitions
        Iterator<String> it = ruleSet.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            Pick expression = (Pick) map.get(key);
            Iterator<String> it2 = ruleSet.iterator();
            if (DEBUG && key.equals("$crlf")) {
                System.out.println("debug");
            }
            while (it2.hasNext()) {
                Object key2 = it2.next();
                if (key.equals(key2)) continue;
                Pick expression2 = (Pick) map.get(key2);
                expression2.replace(key, expression);
            }
        }
        pick = (Pick) map.get("$root");
        // TODO remove temp collections
        return this;
    }

    public static class Position {
        private int index;
        private List<String> items = new ArrayList<>();

        public int getIndex() {
            return index;
        }

        public List<String> getItems() {
            return items;
        }

        public Position addIndex(int len, String item) {
            index += len;
            items.add(item);
            return this;
        }

        public Position clear() {
            index = 0;
            items.clear();
            return this;
        }

        public String toString() {
            return "{" + index + ", " + items + "}";
        }

        public State getState() {
            return new State();
        }

        class State {
            int oldPos = getIndex();
            int oldItemSize = getItems().size();
        }

        public Position restoreState(State state) {
            index = state.oldPos;
            while (items.size() > state.oldItemSize) {
                items.remove(state.oldItemSize);
            }
            return this;
        }
    }

    public final boolean match(String s, int inputPos, Position p) {
        return pick.match(s, inputPos, p);
    }

    <T> String showDiff(Set<T> a, Set<T> b) {
        Set<T> temp = new HashSet<>();
        temp.addAll(a);
        temp.removeAll(b);
        if (temp.size() == 0) return "";
        StringBuffer buffer = new StringBuffer();
        Iterator<T> it = temp.iterator();
        while (it.hasNext()) {
            if (buffer.length() != 0) buffer.append(", ");
            buffer.append(it.next().toString());
        }
        return buffer.toString();
    }

    void error(String msg) {
        throw new IllegalArgumentException(msg + "\r\n" + t.toString());
    }

    private boolean addRule() {
        Result type = t.next();
        if (type == Result.DONE) {
            return false;
        }
        if (type != Result.IDENTIFIER) {
            error("missing identifier");
        }
        String s = t.getString();

        if (t.nextCodePoint() != '=') {
            error("missing =");
        }
        int startBody = t.index;
        Pick rule = getAlternation();
        if (rule == null) {
            error("missing expression");
        }
        t.addSymbol(s, t.getSource(), startBody, t.index);
        if (t.nextCodePoint() != ';') {
            error("missing ;");
        }
        return addPick(s, rule.setName(s));
    }

    protected boolean addPick(String s, Pick rule) {
        Pick temp = map.get(s);
        if (temp != null) {
            error("duplicate variable");
        }
        if (rule.name == null) {
            rule.setName(s);
        }
        map.put(s, rule);
        return true;
    }

    public EBNF addSet(String variable, UnicodeSet set) {
        if (set != null) {
            String body = set.toString();
            t.addSymbol(variable, body, 0, body.length());
            addPick(variable, Pick.codePoint(set));
        }
        return this;
    }

    Pick qualify(Pick item) {
        Result result = t.next();
        if (result == Result.CODEPOINT) {
            switch (t.getCodePoint()) {
                case '?':
                    return Pick.repeat(0, 1, item);
                case '*':
                    return Pick.repeat(0, Integer.MAX_VALUE, item);
                case '+':
                    return Pick.repeat(1, Integer.MAX_VALUE, item);
                case '{':
                    if (t.next() != Result.NUMBER) error("missing number");
                    int start = (int) t.getNumber();
                    int end = start;
                    result = t.next();
                    if (t.getCodePoint() == ',') {
                        end = Integer.MAX_VALUE;
                        result = t.next();
                        if (result == Result.NUMBER) {
                            end = (int) t.getNumber();
                            result = t.next();
                        }
                    }
                    if (t.getCodePoint() != '}') {
                        error("missing }");
                    }
                    return Pick.repeat(start, end, item);
            }
        }
        t.backup();
        return item;
    }

    Pick getCore() {
        Result token = t.next();
        if (token == Result.IDENTIFIER) {
            String s = t.getString();
            variables.add(s);
            return Pick.string(s);
        }
        if (token == Result.STRING) {
            String s = t.getString();
            return Pick.string(s);
        }
        if (token == Result.UNICODESET) {
            return Pick.codePoint(t.getUnicodeSet());
        }
        if (t.getCodePoint() != '(') {
            t.backup();
            return null;
        }
        Pick temp = getAlternation();
        token = t.next();
        if (t.getCodePoint() != ')') {
            error("missing )");
        }
        return temp;
    }

    Pick getSequence() {
        Pick.Sequence result = null;
        Pick last = null;
        while (true) {
            Pick item = getCore();
            if (item == null) {
                if (result != null) return result;
                if (last != null) return last;
                error("missing item");
            }
            // qualify it as many times as possible
            Pick oldItem;
            do {
                oldItem = item;
                item = qualify(item);
            } while (item != oldItem);
            // add it in
            if (last == null) {
                last = item;
            } else {
                if (result == null) result = Pick.makeSequence().and2(last);
                result = result.and2(item);
            }
        }
    }

    // for simplicity, we just use recursive descent
    Pick getAlternation() {
        Pick.Alternation result = null;
        Pick last = null;
        while (true) {
            Pick temp = getSequence();
            if (temp == null) {
                error("empty alternation");
            }
            if (last == null) {
                last = temp;
            } else {
                if (result == null) {
                    result = Pick.makeAlternation().or2(last);
                }
                result = result.or2(temp);
            }
            t.next();
            if (t.getCodePoint() != '|') {
                t.backup();
                if (result != null) return result;
                if (last != null) return last;
            }
        }
    }
}
