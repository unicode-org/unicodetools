package org.unicode.bidi;

/*
 * (C) Copyright IBM Corp. 1999, All Rights Reserved
 *
 * version 1.0
 */

import java.io.PrintWriter;

/**
 * A class that maps ASCII characters to bidi direction types, used for testing purposes.
 * This class should not be used as a model for access to or storage of this information.
 *
 * @author Doug Felt
 */
public abstract class BidiReferenceTestCharmap {

    /** Charmap instance that maps portions of ASCII to strong format codes. */
    public static final BidiReferenceTestCharmap TEST_ENGLISH = new TestEnglish();

    /** Charmap instance that maps portions of ASCII to AL, AN. */
    public static final BidiReferenceTestCharmap TEST_MIXED = new TestMixed();

    /** Charmap instance that maps portions of ASCII to R. */
    public static final BidiReferenceTestCharmap TEST_HEBREW = new TestHebrew();

    /** Charmap instance that maps portions of ASCII to AL, AN, R. */
    public static final BidiReferenceTestCharmap TEST_ARABIC = new TestArabic();

    private static final byte L = BidiReference.L;
    private static final byte LRE = BidiReference.LRE;
    private static final byte LRO = BidiReference.LRO;
    private static final byte R = BidiReference.R;
    private static final byte AL = BidiReference.AL;
    private static final byte RLE = BidiReference.RLE;
    private static final byte RLO = BidiReference.RLO;
    private static final byte PDF = BidiReference.PDF;
    private static final byte EN = BidiReference.EN;
    private static final byte ES = BidiReference.ES;
    private static final byte ET = BidiReference.ET;
    private static final byte AN = BidiReference.AN;
    private static final byte CS = BidiReference.CS;
    private static final byte NSM = BidiReference.NSM;
    private static final byte BN = BidiReference.BN;
    private static final byte B = BidiReference.B;
    private static final byte S = BidiReference.S;
    private static final byte WS = BidiReference.WS;
    private static final byte ON = BidiReference.ON;

    private static final byte TYPE_MIN = BidiReference.TYPE_MIN;
    private static final byte TYPE_MAX = BidiReference.TYPE_MAX;

    private static final String[] typenames = BidiReference.typenames;

    /**
     * Return the name of this mapping.
     */
    public abstract String getName();

    /**
     * Return the bidi direction codes corresponding to the ASCII characters in the string.
     * @param str the string
     * @return an array of bidi direction codes
     */
    public final byte[] getCodes(String str) {
        return getCodes(str.toCharArray());
    }

    /**
     * Return the bidi direction codes corresponding to the ASCII characters in the array.
     * @param chars the array of ASCII characters
     * @return an array of bidi direction codes
     */
    public final byte[] getCodes(char[] chars) {
        return getCodes(chars, 0, chars.length);
    }

    /**
     * Return the bidi direction codes corresponding to the ASCII characters in the subrange
     * of the array.
     * @param chars the array of ASCII characters
     * @param charstart the start of the subrange to use
     * @param count the number of characters in the subrange to use
     * @return an array of bidi direction codes
     */
    public final byte[] getCodes(char[] chars, int charstart, int count) {
        byte[] result = new byte[count];
        convert(chars, charstart, result, 0, count);
        return result;
    }        

    /**
     * Display the mapping from ASCII to bidi direction codes using the provided PrintWriter.
     */
    public abstract void dumpInfo(PrintWriter w);

    /**
     * Convert a subrange of characters to direction codes and place into the code array.
     *
     * @param chars the characters to convert
     * @param charStart the start position in the chars array
     * @param codes the destination array for the direction codes
     * @param codeStart the start position in the codes array
     * @param count the number of characters to convert to direction codes
     */
    public abstract void convert(char[] chars, int charStart, byte[] codes, int codeStart, int count);

    /**
     * Constructor for subclass use.
     */
    protected BidiReferenceTestCharmap() {
  // don't know why the compiler default constructor isn't acceptable
    }

    //
    // Default implementation classes
    //

    /**
     * Default implementation that maps ASCII to all bidi types.
     *
     * This is the base class for TestArabic, TestHebrew, and TestMixed mappings.
     */
    public static class DefaultCharmap extends BidiReferenceTestCharmap {
  protected String name;
        protected byte[] map;

  /**
   * Initialize to default mapping, and define name.
   */
        public DefaultCharmap(String name) {

      this.name = name;

            map = (byte[])baseMap.clone();

            // steal some printable characters for format controls, etc
            // finalize basic mapping
            setMap(RLO, "}");
            setMap(LRO, "{");
            setMap(PDF, "^");
            setMap(RLE, "]");
            setMap(LRE, "[");
            setMap(NSM, "~");
            setMap( BN, "`");
            setMap(  B, "|"); // visible character for convenience
            setMap(  S, "_"); // visible character for convenience
        }

  /**
   * Utility used to change the mapping.
   */
        protected void setMap(byte value, String chars) {
            for (int i = 0; i < chars.length(); ++i) {
                map[chars.charAt(i)] = value;
            }
        }

        /**
   * Standard character mapping for Latin-1.  Protected so that it can be
   * directly accessed by subclasses.
   */
        protected static final byte[] baseMap = {
            ON,  ON,  ON,  ON,  ON,  ON,  ON,  ON,  // 00-07 c0 c0 c0 c0 c0 c0 c0 c0
            ON,   S,   B,   S,   B,   B,  ON,  ON,  // 08-0f c0 HT LF VT FF CR c0 c0
            ON,  ON,  ON,  ON,  ON,  ON,  ON,  ON,  // 10-17 c0 c0 c0 c0 c0 c0 c0 c0
            ON,  ON,  ON,  ON,   B,   B,   B,   S,  // 18-1f c0 c0 c0 c0 FS GS RS US
            WS,  ON,  ON,  ET,  ET,  ET,  ON,  ON,  // 20-27     !  "  #  $  %  &  '
            ON,  ON,  ON,  ET,  CS,  ET,  CS,  ES,  // 28-2f  (  )  *  +  ,  -  .  /
            EN,  EN,  EN,  EN,  EN,  EN,  EN,  EN,  // 30-37  0  1  2  3  4  5  6  7
            EN,  EN,  CS,  ON,  ON,  ON,  ON,  ON,  // 38-3f  8  9  :  ;  <  =  >  ?
            ON,   L,   L,   L,   L,   L,   L,   L,  // 40-47  @  A  B  C  D  E  F  G
             L,   L,   L,   L,   L,   L,   L,   L,  // 48-4f  H  I  J  K  L  M  N  O
             L,   L,   L,   L,   L,   L,   L,   L,  // 50-57  P  Q  R  S  T  U  V  W
             L,   L,   L,  ON,  ON,  ON,  ON,   S,  // 58-5f  X  Y  Z  [  \  ]  ^  _
            ON,   L,   L,   L,   L,   L,   L,   L,  // 60-67  `  a  b  c  d  e  f  g
             L,   L,   L,   L,   L,   L,   L,   L,  // 68-6f  h  i  j  k  l  m  n  o
             L,   L,   L,   L,   L,   L,   L,   L,  // 70-77  p  q  r  s  t  u  v  w
             L,   L,   L,  ON,  ON,  ON,  ON,  ON   // 78-7f  x  y  z  {  |  }  ~  DEL
        };

  /**
   * Return the name.
   */
  public String getName() {
      return name;
  }

  /**
   * Standard implementation of dumpInfo that displays, for each bidi direction type,
   * the characters that are mapped to that type.
   */
        public void dumpInfo(PrintWriter w) {
            // dump mapping table
            // organized by type and coalescing printable characters

      w.print(name);
            for (byte t = TYPE_MIN; t < TYPE_MAX; ++t) {
    w.println();
                w.print("   ".substring(typenames[t].length()) + typenames[t] + ": ");
                int runStart = 0;
                boolean first = true;
                while (runStart < map.length) {
                    while (runStart < map.length && map[runStart] != t) {
                        ++runStart;
                    }
                    if (runStart < map.length) {
                        int runEnd = runStart + 1;
                        while (runEnd < map.length && map[runEnd] == t) {
                            ++runEnd;
                        }
                        if (first) {
                            first = false;
                        } else {
                            w.print(",");
                        }
                        switch (runEnd - runStart) {
                        case 1: 
                            dumpChar(runStart, w); 
                            break;
                        case 2: 
                            dumpChar(runStart, w); 
                            w.print(","); 
                            dumpChar(runEnd - 1, w); 
                            break;
                        default:
                            // only use ranges for a-z, A-Z, 0-9, c0 (hex display)
                            if ((runStart >= 'a' && (runEnd - 1 <= 'z')) ||
                                (runStart >= 'A' && (runEnd - 1 <= 'Z')) ||
                                (runStart >= '0' && (runEnd - 1 <= '9')) ||
                                (runStart >= 0x0 && (runEnd - 1 <= 0x1f))) {

                                dumpChar(runStart, w); 
                                w.print("-"); 
                                dumpChar(runEnd - 1, w);
                            } else {
                                dumpChar(runStart, w);
                                runEnd = runStart + 1;
                            }
                            break;
                        }
                        
                        runStart = runEnd;
                    }
                }
            }
            w.println();
      w.println();
        }

  /**
   * Utility used to output a 'name' of single character, passed as an integer.  Printable
   * characters are represented as themselves, non-printable characters as hex values.  Comma,
   * hyphen, and space are represented as strings surrounded by square brackets.
   *
   * @param i the integer value of the character
   * @param w the PrintWriter on which to output the representation of the character
   */
        protected static void dumpChar(int i, PrintWriter w) {
            char c = (char)i;

            if (c == ',') {
                w.print("[comma]");
            } else if (c == '-') {
                w.print("[hyphen]");
            } else if (c == ' ') {
                w.print("[space]");
            } else if (i > 0x20 && i < 0x7f) {
                w.print(c);
            } else {
                w.print("0x" + Integer.toHexString(i));
            }
        }

  /**
   * Standard implementation of convert.
   */
        public void convert(char[] chars, int charStart, byte[] codes, int codeStart, int count) {
            for (int i = 0; i < count; ++i) {
                codes[codeStart + i] = map[chars[charStart + i]];
            }
        }
    }

    // 'English' mapping just implements the default, naming it "English."
    // Not too interesting, as there are no AL, R, or AN characters.  It does provide
    // mappings to the explicit format codes.

    private static class TestEnglish extends DefaultCharmap {
  private TestEnglish() {
      super("English");
  }
    }

    // Mixed arabic and hebrew test character mapping.
    //
    // In practice, this is not so convenient for experimenting with the algorithm, as
    // it is easy to forget the boundaries between the hebrew and arabic ranges of the
    // upper case letters and the english and arabic ranges of the numbers.

    private static class TestMixed extends DefaultCharmap {
        private TestMixed() {
            super("Mixed Arabic/Hebrew");

            setMap(AL, "ABCDEFGHIJKLM");
            setMap(R, "NOPQRSTUVWXYZ");
            setMap(AN, "56789");
        }
    }

    // Hebrew test character mapping.

    private static class TestHebrew extends DefaultCharmap {
        private TestHebrew() {
            super ("Test Hebrew");

            setMap(R, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }
    }

    // Arabic mapping with arabic numbers

    private static class TestArabic extends DefaultCharmap {
        private TestArabic() {
            super("Test Arabic");

            setMap(AL, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
            setMap(AN, "0123456789");
        }
    }
}
