/*
 *******************************************************************************
 * Copyright (C) 2002-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.parse;

import java.util.HashSet;
import java.util.Set;

import org.unicode.parse.EBNF.Position;

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

abstract public class Pick {
    private static boolean DEBUG = false;

    // for Building

    public Pick replace(String toReplace, Pick replacement) {
        Replacer visitor = new Replacer(toReplace, replacement);
        return visit(visitor);
    }

    public Pick setName(String nameStr) {
        name = nameStr;
        return this;
    }

    static public Pick.Sequence makeSequence() {
        return new Sequence();
    }

    static public Pick.Alternation makeAlternation() {
        return new Alternation();
    }

    static public Pick repeat(int minCount, int maxCount, Pick item) {
        return new Repeat(minCount, maxCount, item);
    }

    static public Pick codePoint(UnicodeSet source) {
        return new CodePoint(source);
    }
    static public Pick string(String source) {
        return new Literal(source);
    }

    @Override
    public String toString() {
        return name != null ? name : toString(4);
    }

    public abstract String toString(int depth);

    public abstract String getInternal(int depth, Set<String> alreadySeen);
    // Internals

    protected String name;

    public abstract boolean match(String input, int inputPos, Position p);

    public static class Sequence extends ListPick {
        public Sequence and2 (Pick item) {
            addInternal(new Pick[] {item}); // we don't care about perf
            return this; // for chaining
        }
        public Sequence and2 (Pick[] itemArray) {
            addInternal(itemArray);
            return this; // for chaining
        }
        public String getInternal(int depth, Set<String> alreadySeen) {
            String result = checkName(name, alreadySeen);
            if (result.startsWith("$")) return result;
            result = indent(depth) + result + "SEQ(";
            for (int i = 0; i < items.length; ++i) {
                if (i != 0) result += ", ";
                result += items[i].getInternal(depth+1, alreadySeen);
            }
            result += ")";
            return result;
        }

        // keep private
        private Sequence() {
            sep = " ";
        }

        public boolean match(String input, int inputPos, Position p) {
            for (int i = 0; i < items.length; ++i) {
                Pick item = items[i];
                if (!item.match(input, inputPos, p)) {
                    return false;
                }
                inputPos = p.getIndex();
            }
            return true;
        }
    }

    String checkName(String nameStr, Set alreadySeen) {
        if (nameStr == null) return "";
        if (alreadySeen.contains(nameStr)) return nameStr;
        alreadySeen.add(nameStr);
        return "{" + nameStr + "=}";
    }

    public static class Alternation extends ListPick {

        public Alternation or2 (Pick item) {
            addInternal(new Pick[] {item}); // we don't care about perf
            return this; // for chaining
        }
        public Alternation or2 (Pick[] itemArray) {
            addInternal(itemArray);
            return this; // for chaining
        }

        public String getInternal(int depth, Set alreadySeen) {
            String result = checkName(name, alreadySeen);
            if (result.startsWith("$")) return result;
            result = indent(depth) + result + "OR(";
            for (int i = 0; i < items.length; ++i) {
                if (i != 0) result += ", ";
                result += items[i].getInternal(depth+1, alreadySeen);
            }
            return result + ")";
        }
        // keep private
        private Alternation() {
            sep="|";
        }

        // take first matching option
        public boolean match(String input, int inputPos, Position p) {
            Position.State state = p.getState();
            for (int i = 0; i < items.length; ++i) {
                Pick item = items[i];
                if (item.match(input, inputPos, p)) {
                    return true;
                }
                p.restoreState(state);
            }
            return false;
        }
    }

    private static String indent(int depth) {
        String result = "\r\n";
        for (int i = 0; i < depth; ++i) {
            result += " ";
        }
        return result;
    }

    private static class Repeat extends ItemPick {
        final int minCount;
        final int maxCount;

        private Repeat(int minCount, int maxCount, Pick item) {
            super(item);
            this.minCount = minCount;
            this.maxCount = maxCount;
        }

        public String getInternal(int depth, Set alreadySeen) {
            String result = checkName(name, alreadySeen);
            if (result.startsWith("$")) return result;
            result = indent(depth) + result + "REPEAT("
                    + item.getInternal(depth+1, alreadySeen) 
                    + ")";
            return result;
        }

        // match longest, e.g. up to just before a failure
        public boolean match(String input, int inputPos, Position p) {
            int count = 0;
            Position.State state = p.getState();
            for (int i = 0; i < maxCount; ++i) {
                if (!item.match(input, inputPos, p)) {
                    break;
                } 
                inputPos = p.getIndex();
                count++;               
            }
            if (count >= minCount) {
                return true;
            }
            p.restoreState(state);
            return false;
        }
        @Override
        public String toString(int depth) {
            return name != null ? name : 
                item.toString(depth) + 
                    (maxCount == Integer.MAX_VALUE ? 
                            (minCount == 0 ?  "*" 
                                    : minCount == 1 ? "+"
                                            : "{" + minCount + ",}")
                            : maxCount == 1 ? "?"
                                    : maxCount == minCount ? "{" + minCount + "}"
                                            : "{" + minCount + "," + maxCount + "}");
        }
    }

    private static class CodePoint extends FinalPick {
        private UnicodeSet source;

        private CodePoint(UnicodeSet source) {
            this.source = source;
        }
        public boolean match(String s, int inputPos, Position p) {
            if (inputPos >= s.length()) {
                return false;
            }
            int match = source.matchesAt(s, inputPos);
            if (match < inputPos) {
                return false;
            } else {
                p.addIndex(match-inputPos, s.substring(inputPos, match));
                return true;
            }
        }
        public String getInternal(int depth, Set alreadySeen) {
            String result = checkName(name, alreadySeen);
            if (result.startsWith("$")) return result;
            return source.toString();
        }
        public String toString() {
            return source.toString();
        }
        public String toString(int depth) {
            return toString();
        }
    }



    /* Add character if we can
     */
    static int getChar(String newValue, int newIndex, StringBuffer mergeBuffer, boolean copy) {
        if (newIndex >= newValue.length()) return newIndex;
        int cp = UTF16.charAt(newValue,newIndex);
        if (copy) UTF16.append(mergeBuffer, cp);
        return newIndex + UTF16.getCharCount(cp);
    }

    /*   
            // quoted add
            appendQuoted(target, addBuffer.toString(), quoteBuffer);
            // fix buffers
            StringBuffer swapTemp = addBuffer;
            addBuffer = source;
            source = swapTemp;
        }
    }
     */


    private static class Literal extends FinalPick {
        public String toString() {
            return "'" + name + "'";
        }        
        private Literal(String source) {  
            this.name = source;
        }
        public boolean match(String input, int inputPos, Position p) {
            int len = name.length();
            if (input.regionMatches(inputPos, name, 0, len)) {
                p.addIndex(len, name);
                return true;
            }
            return false;
        }
        public String getInternal(int depth, Set alreadySeen) {
            return toString();
        }
        public String toString(int depth) {
            return toString();
        }
    }

    // intermediates

    abstract static class Visitor {
        Set already = new HashSet();
        // Note: each visitor should return the Pick that will replace a (or a itself)
        abstract Pick handle(Pick a);
        boolean alreadyEntered(Pick item) {
            boolean result = already.contains(item);
            already.add(item);
            return result;
        }
        void reset() {
            already.clear();
        }
    }

    protected abstract Pick visit(Visitor visitor);

    static class Replacer extends Visitor {
        String toReplace;
        Pick replacement;
        Replacer(String toReplace, Pick replacement) {
            this.toReplace = toReplace;
            this.replacement = replacement;
        }
        public Pick handle(Pick a) {
            if (toReplace.equals(a.name)) {
                a = replacement;
            } 
            return a;
        }
    }

    abstract private static class FinalPick extends Pick {
        public Pick visit(Visitor visitor) {
            return visitor.handle(this);
        }
    }

    private abstract static class ItemPick extends Pick {
        protected Pick item;

        ItemPick (Pick item) {
            this.item = item;
        }

        public Pick visit(Visitor visitor) {
            Pick result = visitor.handle(this);
            if (visitor.alreadyEntered(this)) return result;
            if (item != null) item = item.visit(visitor);
            return result;
        }
    }

    private abstract static class ListPick extends Pick {
        protected Pick[] items = new Pick[0];
        protected String sep;

        Pick simplify() {
            if (items.length > 1) return this;
            if (items.length == 1) return items[0];
            return null;
        }

        int size() {
            return items.length;
        }

        Pick getLast() {
            return items[items.length-1];
        }

        void setLast(Pick newOne) {
            items[items.length-1] = newOne;
        }

        protected void addInternal(Pick[] objs) {
            int lastLen = items.length;
            items = realloc(items, items.length + objs.length);
            for (int i = 0; i < objs.length; ++i) {
                items[lastLen + i] = objs[i];
            }
        }

        public Pick visit(Visitor visitor) {
            Pick result = visitor.handle(this);
            if (visitor.alreadyEntered(this)) return result;
            for (int i = 0; i < items.length; ++i) {
                items[i] = items[i].visit(visitor);
            }
            return result;
        }

        @Override
        public String toString(int depth) {
            if (name != null) {
                return name;
            } if (items.length == 1) {
                return items[0].toString(depth-1);
            } else if (depth < 0) {
                return "?";
            }
            StringBuilder b = new StringBuilder();
            b.append("(");
            for (Pick item : items) {
                if (b.length() != 1) {
                    b.append(sep);
                }
                b.append(item.toString(depth-1));
            }
            return b.append(")").toString();
        }
    }


    // these utilities really ought to be in Java

    public static double[] realloc(double[] source, int newSize) {
        double[] temp = new double[newSize];
        if (newSize > source.length) newSize = source.length;
        if (newSize != 0) System.arraycopy(source,0,temp,0,newSize);
        return temp;
    }

    public static int[] realloc(int[] source, int newSize) {
        int[] temp = new int[newSize];
        if (newSize > source.length) newSize = source.length;
        if (newSize != 0) System.arraycopy(source,0,temp,0,newSize);
        return temp;
    }

    public static Pick[] realloc(Pick[] source, int newSize) {
        Pick[] temp = new Pick[newSize];
        if (newSize > source.length) newSize = source.length;
        if (newSize != 0) System.arraycopy(source,0,temp,0,newSize);
        return temp;
    }

    // test utilities
    /*private static void append(StringBuffer target, String toAdd, StringBuffer quoteBuffer) {
        Utility.appendToRule(target, (int)-1, true, false, quoteBuffer); // close previous quote
        if (DEBUG) System.out.println("\"" + toAdd + "\"");
        target.append(toAdd);
    }

    private static void appendQuoted(StringBuffer target, String toAdd, StringBuffer quoteBuffer) {
        if (DEBUG) System.out.println("\"" + toAdd + "\"");
        Utility.appendToRule(target, toAdd, false, false, quoteBuffer);
    }*/

    /*
    public static abstract class MatchHandler {
        public abstract void handleString(String source, int start, int limit);
        public abstract void handleSequence(String source, int start, int limit);
        public abstract void handleAlternation(String source, int start, int limit);

    }
     */
    /*
    // redistributes random value
    // values are still between 0 and 1, but with a different distribution
    public interface Spread {
        public double spread(double value);
    }

    // give the weight for the high end.
    // values are linearly scaled according to the weight.
    static public class SimpleSpread implements Spread {
        static final Spread FLAT = new SimpleSpread(1.0);
        boolean flat = false;
        double aa, bb, cc;
        public SimpleSpread(double maxWeight) {   
            if (maxWeight > 0.999 && maxWeight < 1.001) {
                flat = true;
            } else { 
                double q = (maxWeight - 1.0);
                aa = -1/q;
                bb = 1/(q*q);
                cc = (2.0+q)/q;
           }                 
        }
        public double spread(double value) {
            if (flat) return value;
            value = aa + Math.sqrt(bb + cc*value);
            if (value < 0.0) return 0.0;    // catch math gorp
            if (value >= 1.0) return 1.0;
            return value;
        }
    }
    static public int pick(Spread spread, Random random, int start, int end) {
        return start + (int)(spread.spread(random.nextDouble()) * (end + 1 - start));
    }

     */


}