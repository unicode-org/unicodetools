/**
 * 
 */
package jsp;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.UnicodeSet;

class BidiCharMap {
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
  
    static final byte[] baseMap = {
      ON,  ON,  ON,  ON,  ON,  ON,  ON,  ON,  // 00-07 c0 c0 c0 c0 c0 c0 c0 c0
      ON,   S,   B,   S,   B,   B,  ON,  ON,  // 08-0f c0 HT LF VT FF CR c0 c0
      ON,  ON,  ON,  ON,  ON,  ON,  ON,  ON,  // 10-17 c0 c0 c0 c0 c0 c0 c0 c0
      ON,  ON,  ON,  ON,   B,   B,   B,   S,  // 18-1f c0 c0 c0 c0 FS GS RS US
      WS,  ON,  NSM,  ET,  ET,  ET,  ON,  ON,  // 20-27     !  "  #  $  %  &  '
      ON,  ON,  ON,  ET,  CS,  ET,  CS,  ES,  // 28-2f  (  )  *  +  ,  -  .  /
      EN,  EN,  EN,  EN,  EN,  AN,  AN,  AN,  // 30-37  0  1  2  3  4  5  6  7
      AN,  AN,  CS,  ON,  R,  ON,  L,  ON,  // 38-3f  8  9  :  ;  <  =  >  ?
      ON,   R,   R,   R,   R,   R,   R,   R,  // 40-47  @  A  B  C  D  E  F  G
       R,   R,   R,   R,   R,   R,   AL,   AL,  // 48-4f  H  I  J  K  L  M  N  O
      AL,  AL,  AL,  AL,  AL,  AL,  AL,  AL,  // 50-57  P  Q  R  S  T  U  V  W
      AL,  AL,  AL,  RLE,  ON,  LRE,  ON,   S,  // 58-5f  X  Y  Z  [  \  ]  ^  _
      ON,   L,   L,   L,   L,   L,   L,   L,  // 60-67  `  a  b  c  d  e  f  g
       L,   L,   L,   L,   L,   L,   L,   L,  // 68-6f  h  i  j  k  l  m  n  o
       L,   L,   L,   L,   L,   L,   L,   L,  // 70-77  p  q  r  s  t  u  v  w
       L,   L,   L,  RLO,  PDF,  LRO,  BN,  ON   // 78-7f  x  y  z  {  |  }  ~  DEL
  };


    static byte mapIcuToRefNum[] = null;
    static UnicodeSet[] umap = new UnicodeSet[BidiReference.typenames.length];
    boolean asciiHack;
    
    public BidiCharMap (boolean asciiHack) {
      this.asciiHack = asciiHack;
      if (mapIcuToRefNum == null) {
        mapIcuToRefNum = new byte[BidiReference.typenames.length];
        // generate permutation from names
        for (byte i = 0; i < mapIcuToRefNum.length; ++i) {
          int icuValue = UCharacter.getPropertyValueEnum(UProperty.BIDI_CLASS, BidiReference.typenames[i]);
          mapIcuToRefNum[icuValue] = i;
        }
        for (int i = 0; i < BidiReference.typenames.length; ++i) {
          umap[i] = new UnicodeSet();
        }
        for (int i = 0; i < baseMap.length; ++i) {
          byte t = baseMap[i];
          UnicodeSet s = umap[t];
          s.add(i);
        }
      }
    }
    
    public UnicodeSet getAsciiHack(int i) {
      return umap[i];
    }

    public byte getBidiClass(int codepoint) {
      if (asciiHack && codepoint < 0xFF) {
        return baseMap[codepoint];
      }
      return mapIcuToRefNum[UCharacter.getIntPropertyValue(codepoint, UProperty.BIDI_CLASS)];
    }
  }