package org.unicode.jsptest;

import java.io.IOException;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.unicode.idna.Idna2008;
import org.unicode.idna.Idna2008.Idna2008Type;
import org.unicode.idna.IdnaTypes;
import org.unicode.jsp.FileUtilities;
import org.unicode.unittest.TestFmwkMinusMinus;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class TestIdna extends TestFmwkMinusMinus {
  static class MyHandler extends FileUtilities.SemiFileReader {
    UnicodeSet wideNarrow = new UnicodeSet("[[:dt=wide:][:dt=narrow:]]").freeze();

    UnicodeMap<Idna2008Type> types = Idna2008.getTypeMapping();

    UnicodeSet sourceNotAllowed = new UnicodeSet();
    UnicodeSet targetNotAllowed = new UnicodeSet();
    UnicodeSet equalsIdnabis = new UnicodeSet();
    UnicodeMap<String> diffIdnaBis = new UnicodeMap<String>();
    Normalizer2 nfc = Normalizer2.getNFCInstance();
    Normalizer2 nfkc = Normalizer2.getNFKCInstance();

    public boolean handleLine(int start, int end, String[] items) {
      String type = items[1];
      String value;
      if (type.equals("mapped")) {
        value = Utility.fromHex(items[2], 1, " ");
      } else if (type.equals("ignored")) {
        value = "";
      } else {
        return true;
      }
      for (int i = start; i <= end; ++i) {
        addMapping(i, value);
      }
      return true;
    }

    private void addMapping(int source, String targetChars) {
      Idna2008Type type = types.get(source);
      if (type != Idna2008Type.DISALLOWED) {
        sourceNotAllowed.add(source);
        return;
      } else {
        int cp;
        for (int i = 0; i < targetChars.length(); i += Character.charCount(cp)) {
          cp = targetChars.codePointAt(i);
          Idna2008Type type2 = types.get(cp);
          if (type2 == Idna2008Type.DISALLOWED && cp != '.' && cp != '-') {
            targetNotAllowed.add(source);
            return;
          }
        }
      }
      String idnabismapping = getIdnabisMapping(source);
      if (idnabismapping.equals(targetChars)) {
        equalsIdnabis.add(source);
        return;
      }
      diffIdnaBis.put(source, targetChars);
    }

    private String getIdnabisMapping(int source) {
      String idnabisMapping;
      idnabisMapping = UCharacter.toLowerCase(Locale.ROOT, UTF16.valueOf(source));
      if (wideNarrow.containsSome(idnabisMapping)) {
        StringBuilder temp = new StringBuilder();
        int cp;
        for (int i = 0; i < idnabisMapping.length(); i += Character.charCount(cp)) {
          cp = idnabisMapping.codePointAt(i);
          if (wideNarrow.contains(cp)) {
            temp.append(nfkc.getDecomposition(cp));
          } else {
            temp.appendCodePoint(cp);
          }
        }
        idnabisMapping = temp.toString();
      }
      idnabisMapping = nfc.normalize(idnabisMapping);
      return idnabisMapping;
    }
  }

  @Test
  public void TestExtract() throws IOException {

    MyHandler handler = new MyHandler();
    handler.process(IdnaTypes.class, "IdnaMappingTable.txt");

    logln("sourceNotAllowed: " + handler.sourceNotAllowed.size() + "\t" + handler.sourceNotAllowed);
    logln("targetNotAllowed: " + handler.targetNotAllowed.size() + "\t" + handler.targetNotAllowed);
    logln("equalsIdnabis: " + handler.equalsIdnabis.size() + "\t" + handler.equalsIdnabis);
    logln("diffIdnabis: " + handler.diffIdnaBis.size());
    assertNotEquals("diffIdnaBis", 0, handler.diffIdnaBis.keySet().size());
    for (String source : handler.diffIdnaBis.keySet()) {
      CharSequence targetChars = handler.diffIdnaBis.get(source);
      logln("\t" + Utility.hex(source) + " ( " + source + " ) => "
              + Utility.hex(targetChars) + " ( " + targetChars + " ) # " + UCharacter.getName(source.codePointAt(0)));
    }
  }


  /*
   1.  Upper case characters are mapped to their lower case equivalents
       by using the algorithm for mapping case in Unicode characters.

   2.  Full-width and half-width characters (those defined with
       Decomposition Types <wide> and <narrow>) are mapped to their
       decomposition mappings as shown in the Unicode character
       database.

   3.  All characters are mapped using Unicode Normalization Form C
       (NFC).

   4.  [I-D.ietf-idnabis-protocol] is specified such that the protocol
       acts on the indvidual labels of the domain name.  If an
       implementation of this mapping is also performing the step of
       separation of the parts of a domain name into labels by using the
       FULL STOP character (U+002E), the following character can be
       mapped to the FULL STOP before label separation occurs:

   *  IDEOGRAPHIC FULL STOP (U+3002)

       There are other characters that are used as "full stops" that one
       could consider mapping as label separators, but their use as such
       has not been investigated thoroughly.

   Definitions for the rules in this algorithm can be found in
   [Unicode51].  Specifically:

   o  Unicode Normalization Form C can be found in Annex #15 of
      [Unicode51].

   o  In order to map upper case characters to their lower case
      equivalents (defined in section 3.13 of [Unicode51]), first map
      characters to the "Lowercase_Mapping" property (the "<lower>"
      entry in the second column) in
      <http://www.unicode.org/Public/UNIDATA/SpecialCasing.txt>, if any.
      Then, map characters to the "Simple_Lowercase_Mapping" property
      (the fourteenth column) in
      <http://www.unicode.org/Public/UNIDATA/UnicodeData.txt>, if any.

   o  In order to map full-width and half-width characters to their
      decomposition mappings, map any character whose
      "Decomposition_Type" (contained in the first part of of the sixth
      column) in <http://www.unicode.org/Public/UNIDATA/UnicodeData.txt>
      is either "<wide>" or "<narrow>" to the "Decomposition_Mapping" of
      that character (contained in the second part of the sixth column)
      in <http://www.unicode.org/Public/UNIDATA/UnicodeData.txt>.
   */
}
