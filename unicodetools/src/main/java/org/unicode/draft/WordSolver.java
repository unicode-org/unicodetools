package org.unicode.draft;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WordSolver {

    private static final boolean SHOW = true;

    private final int count;
    private final String[] items;
    private final Alphagram[] alphagrams;
    private final Map<String, Integer> string2id = new LinkedHashMap<String, Integer>();
    private final int[][] shared;
    private final int[] bits;
    private final int[] guessResult;
    private final boolean[] rejected;
    private int stringLength;
    private final Distribution distribution;

    public int getStringLength() {
        return stringLength;
    }

    public WordSolver(String[] itemsIn) {
        items = itemsIn.clone();
        count = items.length;
        alphagrams = new Alphagram[count];
        guessResult = new int[count];
        shared = new int[count][];
        bits = new int[count];
        rejected = new boolean[count];

        for (int i = 0; i < count; ++i) {
            final String item = items[i].toUpperCase();
            items[i] = item; // set the case, if needed
            if (stringLength != item.length()) {
                if (stringLength != 0) {
                    throw new IllegalArgumentException("Strings must be same length");
                }
                stringLength = item.length();
            }
            if (string2id.containsKey(item)) {
                throw new IllegalArgumentException("duplicate: " + items);
            }
            string2id.put(item, i);
            alphagrams[i] = new Alphagram(item);
            guessResult[i] = -1;
        }

        distribution = new Distribution(stringLength);

        for (int i = 0; i < count; ++i) {
            shared[i] = new int[count];
            for (int j = 0; j < count; ++j) {
                final int share = shared[i][j] = countShared(i, j);
                bits[i] |= (1<<share);
            }
        }
    }

    private int countShared(int i, int j) {
        final String a = items[i];
        final String b = items[j];
        int correct = 0;
        for (int k = 0; k < a.length(); ++k) {
            if (a.charAt(k) == b.charAt(k)) {
                ++correct;
            }
        }
        return correct;
        // return alphagrams[i].countShared(alphagrams[j]);
    }

    public void recordGuess(String a, int numberRight) {
        a = a.toUpperCase();
        final int id = string2id.get(a);
        guessResult[id] = numberRight;
        rejected[id] = true;
        for (int i = 0; i < count; ++i) {
            if (shared[id][i] != numberRight) {
                rejected[i] = true;
            }
        }
    }

    @Override
    public String toString() {
        final StringWriter endResult = new StringWriter();
        final PrintWriter result = new PrintWriter(endResult);
        result.print("guess\tstatus\tword");
        for (int i = 0; i < count; ++i) {
            result.print("\t" + items[i]);
        }
        result.println();
        for (int i = 0; i < count; ++i) {
            result.print(rejected[i] ? "X" : "");
            result.print("\t" + (guessResult[i] < 0 ? "?" : guessResult[i]));
            result.print("\t" + items[i]);
            for (int j = 0; j < count; ++j) {
                result.print("\t" + shared[i][j]);
                if (rejected[j]) {
                    result.print("x");
                }
            }
            if (!rejected[i]) {
                result.print(distribution.fill(i));
            }
            result.println();
        }
        result.flush();
        return endResult.toString();
    }

    public String guessNext() {
        // collect the distributions
        int best = -1;
        int bestMode = Integer.MAX_VALUE;
        double bestAverage = 0; // Double.MAX_VALUE;
        int bestMinItemCount = 0;
        int bestMinItem = Integer.MAX_VALUE;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < count; ++i) {
            if (rejected[i]) {
                continue;
            }
            distribution.fill(i);
            final double average = distribution.getAverage();
            final int mode = distribution.getMode();
            final int minItem = distribution.getMinItem();
            final int minItemCount = distribution.getMinItemCount();

            final double score = distribution.getScore();

            if (score <= bestScore) {
                continue;
            }

            //      if (minItem > bestMinItem) {
            //        continue;
            //      } else if (minItem == bestMinItem) {
            //        if (minItemCount <= bestMinItemCount) {
            //          continue;
            //        }
            //      }
            //      if (max > bestMax) {
            //        continue;
            //      } else if (max == bestMax) {
            //        if (average <= bestAverage) {
            //          continue;
            //        }
            //      }
            // we have a new best!
            best = i;
            bestMode = mode;
            bestAverage = average;
            bestMinItem = minItem;
            bestMinItemCount = minItemCount;
            bestScore = score;
        }
        return items[best];
    }

    private int match(String guess, String target) {
        guess = guess.toUpperCase();
        target = target.toUpperCase();
        return shared[string2id.get(guess)][string2id.get(target)];
    }

    private class Distribution {
        private final double minItemFactor = 0;
        private final double modeFactor = -1;
        private final double averageFactor = 0;
        private final double minItemCountFactor = 0;

        private final int[] distribution;
        private double average;
        private int mode;
        private int minItemCount;
        private int minItem;

        public int getMinItemCount() {
            return minItemCount;
        }

        public int getMinItem() {
            return minItem;
        }

        public double getAverage() {
            return average;
        }

        public int getMode() {
            return mode;
        }

        public double getScore() {
            return 1000 + minItemCountFactor*minItemCount + averageFactor*average + modeFactor*mode + minItemFactor*minItem;
        }

        public Distribution(int stringLength) {
            distribution = new int[stringLength+1];
        }

        private Distribution fill(int row) {
            // clear
            for (int j = 0; j < distribution.length; ++j) {
                distribution[j] = 0;
            }

            for (int j = 0; j < count; ++j) {
                if (!rejected[j]) {
                    ++distribution[shared[row][j]];
                }
            }
            int total = 0;
            int count = 0;
            mode = 0;
            minItemCount = 0;
            minItem = -1;
            for (int j = 0; j < distribution.length; ++j) {
                if (distribution[j] != 0) {
                    total += distribution[j];
                    ++count;
                    if (distribution[j] > mode) {
                        mode = distribution[j];
                    }
                    if (minItem == -1) {
                        minItem = j;
                        minItemCount = distribution[j];
                    }
                }
            }
            average = total/(double)count;
            return this;
        }

        @Override
        public String toString() {
            final StringBuffer result = new StringBuffer();
            for (int j = 0; j < distribution.length; ++j) {
                if (distribution[j] != 0) {
                    result.append("\t" + distribution[j] + "*" + j);
                }
            }
            result.append("\tave: " + (int)(getAverage()*100)/100.0);
            result.append("\tmode: " + getMode());
            result.append("\tmin: " + getMinItemCount() + "*" + getMinItem());
            result.append("\tscore: " + (int)(getScore()*100)/100.0);
            return result.toString();
        }
    }

    static String testString =
            "knuckles informed knightly androids engulfed engineer entrance invented involved kneecaps inserted intended inferior invaders injected infested infected infamous uncommon sneaking"
            //"BENEFITS SURPRISE GENETICS EXPLAINS PILGRIMS LUNATICS THROWING STARTING GRASPING PLOTTING SPEEDING SPENDING SHUTTING SCOUTING SHOOTING SHACKLES SYNOPSES"
            ;

    public static void main(String[] args) {
        final String[] tests = testString.trim().split("\\s+");
        if (false) {
            checkManual(tests);
            return;
        }
        int total = 0;
        int count = 0;
        final List<Integer> guessCount = new ArrayList<Integer>();

        for (final String target : tests) {
            System.out.println("Target: " + target);
            System.out.println();
            final WordSolver wordSolver = new WordSolver(tests);

            // now try to guess
            int guessNumber = 0;
            while (true) {
                if (SHOW) {
                    System.out.println(wordSolver);
                }
                ++guessNumber;
                final String guess = wordSolver.guessNext();
                System.out.println("Guess: " + guess);
                final int match = wordSolver.match(guess, target);
                System.out.println("Match: " + match);
                if (match == wordSolver.getStringLength()) {
                    System.out.println("DONE, with " + guessNumber + " guesses");
                    System.out.println();
                    guessCount.add(guessNumber);
                    total += guessNumber;
                    ++count;
                    break;
                }
                wordSolver.recordGuess(guess, match);
            }
        }
        System.out.println(guessCount);
        System.out.println("Average Guesses Needed: " + (total/(double)count));
    }

    private static void checkManual(String[] tests) {
        final WordSolver wordSolver = new WordSolver(tests);
        System.out.println(wordSolver);
        String guess = "sneaking"; // wordSolver.guessNext();
        System.out.println("Guess: " + guess);

        wordSolver.recordGuess(guess, 2);
        System.out.println(wordSolver);
        guess = wordSolver.guessNext();
        System.out.println("Guess: " + guess);

        wordSolver.recordGuess(guess, 2);
        System.out.println(wordSolver);
        guess = wordSolver.guessNext();
        System.out.println("Guess: " + guess);

        wordSolver.recordGuess(guess, 1);
        System.out.println(wordSolver);
        guess = wordSolver.guessNext();
        System.out.println("Guess: " + guess);

    }
}
