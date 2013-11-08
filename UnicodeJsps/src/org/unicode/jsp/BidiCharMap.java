/**
 * 
 */
package org.unicode.jsp;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

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

  static byte mapIcuToRefNum[] = null;
  static UnicodeSet[] umap = new UnicodeSet[BidiReference.typenames.length];
  static UnicodeMap asciiHackMap = new UnicodeMap();

  static {
    mapIcuToRefNum = new byte[BidiReference.typenames.length];
    // generate permutation from names
    for (byte i = 0; i < mapIcuToRefNum.length; ++i) {
      int icuValue = UCharacter.getPropertyValueEnum(UProperty.BIDI_CLASS, BidiReference.typenames[i]);
      mapIcuToRefNum[icuValue] = i;
    }

    for (int i = 0; i < BidiReference.typenames.length; ++i) {
      umap[i] = new UnicodeSet();
    }

    for (UnicodeSetIterator it = new UnicodeSetIterator(new UnicodeSet("[[:ascii:]-[[:cc:]-[:whitespace:]]]")); it.next();) {
      asciiHackMap.put(it.codepoint, mapIcuToRefNum[UCharacter.getIntPropertyValue(it.codepoint, UProperty.BIDI_CLASS)]);
    }
    // override
    asciiHackMap.put(']', LRE);
    asciiHackMap.put('[', RLE);
    asciiHackMap.put('}', LRO);
    asciiHackMap.put('{', RLO);
    asciiHackMap.put('|', PDF);
    asciiHackMap.putAll(new UnicodeSet("[A-M]"), R);
    asciiHackMap.putAll(new UnicodeSet("[N-Z]"), AL);
    asciiHackMap.putAll(new UnicodeSet("[5-9]"), AN);
    asciiHackMap.put('>', L);
    asciiHackMap.put('<',R);
    asciiHackMap.put('"',NSM);
    asciiHackMap.put('_',BN);
  }

  boolean asciiHack;

  public BidiCharMap (boolean asciiHack) {
    this.asciiHack = asciiHack;
  }

  public static UnicodeSet getAsciiHack(byte i) {
    return asciiHackMap.keySet(i);
  }

  public static byte getBidiClass(int codepoint, boolean asciiHack2) {
    if (asciiHack2) {
      Byte result = (Byte) asciiHackMap.getValue(codepoint);
      if (result != null) {
        return result;
      }
    }
    return mapIcuToRefNum[UCharacter.getIntPropertyValue(codepoint, UProperty.BIDI_CLASS)];
  }

  public byte getBidiClass(int codepoint) {
    return getBidiClass(codepoint, asciiHack);
  }
}