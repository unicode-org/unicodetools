package jsp;

import java.io.IOException;
import java.io.PrintWriter;

import com.ibm.icu.text.UnicodeSet;

public class TestJsp {

  public static void main(String[] args) throws IOException {
      final PrintWriter printWriter = new PrintWriter(System.out);
      String IDNA2008 = "ÖBB\n"
        + "O\u0308BB\n"
        + "Schäffer\n"
        + "ＡＢＣ・フ\n"
        + "I♥NY\n"
        + "faß\n"
        + "βόλος";
      String testLines = UnicodeUtilities.testIdnaLines(IDNA2008, "[]");
      System.out.println(testLines);
      
      System.out.println(UnicodeUtilities.showMapping("[\\u0000-\\u00FF] casefold"));
      
      final String fix = UnicodeRegex.fix("ab[[:ascii:]&[:Ll:]]*c");
      System.out.println(fix);
      System.out.println(UnicodeUtilities.showRegexFind(fix, "abcc abxyzc ab$c"));
      
      //showIDNARemapDifferences(printWriter);
   
      UnicodeUtilities.expectError("][:idna=output:][abc]");
      
      UtfParameters parameters = new UtfParameters("ab%61=%C3%A2%CE%94");
      System.out.println(parameters.getParameter("aba"));
      
      if (!UnicodeUtilities.parseUnicodeSet("[:idna=output:]").contains('-')) {
        System.out.println("FAILURE");
      }
  
      UnicodeUtilities.test("[:subhead=/Mayanist/:]");
      
      UnicodeUtilities.test("[[:script=*latin:]-[:script=latin:]]");
      UnicodeUtilities.test("[[:script=**latin:]-[:script=latin:]]");
      UnicodeUtilities.test("abc-m");
      
      UnicodeUtilities.test("[:archaic=no:]");
  
      UnicodeUtilities.test("[:toNFKC=a:]");
      UnicodeUtilities.test("[:isNFC=false:]");
      UnicodeUtilities.test("[:toNFD=A\u0300:]");
      UnicodeUtilities.test("[:toLowercase= /a/ :]");
      UnicodeUtilities.test("[:toLowercase= /a/ :]");
      UnicodeUtilities.test("[:ASCII:]");
      UnicodeUtilities.test("[:lowercase:]");
      UnicodeUtilities.test("[:toNFC=/\\./:]");
      UnicodeUtilities.test("[:toNFKC=/\\./:]");
      UnicodeUtilities.test("[:toNFD=/\\./:]");
      UnicodeUtilities.test("[:toNFKD=/\\./:]");
      UnicodeUtilities.test("[:toLowercase=/a/:]");
      UnicodeUtilities.test("[:toUppercase=/A/:]");
      UnicodeUtilities.test("[:toCaseFold=/a/:]");
      UnicodeUtilities.test("[:toTitlecase=/A/:]");
       printWriter.flush();
      
      //if (true) return;
      
      UnicodeUtilities.showSet(new UnicodeSet("[\\u0080\\U0010FFFF]"), true, true, printWriter);
      printWriter.flush();
      
      
      UnicodeUtilities.test("[:name=/WITH/:]");
      UnicodeUtilities.showProperties("a", printWriter);
      printWriter.flush();
      
      String[] abResults = new String[3];
      String[] abLinks = new String[3];
      int[] abSizes = new int[3];
      UnicodeUtilities.getDifferences("[:letter:]", "[:idna:]", false, abResults, abSizes, abLinks);
      for (int i = 0; i < abResults.length; ++i) {
        System.out.println(abSizes[i] + "\r\n\t" + abResults[i] + "\r\n\t" + abLinks[i]);
      }
      
      final UnicodeSet unicodeSet = new UnicodeSet();
      System.out.println("simple: " + UnicodeUtilities.getSimpleSet("[a-bm-p\uAc00]", unicodeSet, true, false));
      UnicodeUtilities.showSet(unicodeSet, true, true, printWriter);
      printWriter.flush();
      UnicodeUtilities.test("[:idna:]");
      UnicodeUtilities.test("[:idna=ignored:]");
      UnicodeUtilities.test("[:idna=remapped:]");
      UnicodeUtilities.test("[:idna=disallowed:]");
      UnicodeUtilities.test("[:iscased:]");
  //    String archaic = "[[\u018D\u01AA\u01AB\u01B9-\u01BB\u01BE\u01BF\u021C\u021D\u025F\u0277\u027C\u029E\u0343\u03D0\u03D1\u03D5-\u03E1\u03F7-\u03FB\u0483-\u0486\u05A2\u05C5-\u05C7\u066E\u066F\u068E\u0CDE\u10F1-\u10F6\u1100-\u115E\u1161-\u11FF\u17A8\u17D1\u17DD\u1DC0-\u1DC3\u3165-\u318E\uA700-\uA707\\U00010140-\\U00010174]" +
  //    "[\u02EF-\u02FF\u0363-\u0373\u0376\u0377\u07E8-\u07EA\u1DCE-\u1DE6\u1DFE\u1DFF\u1E9C\u1E9D\u1E9F\u1EFA-\u1EFF\u2056\u2058-\u205E\u2180-\u2183\u2185-\u2188\u2C77-\u2C7D\u2E00-\u2E17\u2E2A-\u2E30\uA720\uA721\uA730-\uA778\uA7FB-\uA7FF]" +
  //    "[\u0269\u027F\u0285-\u0287\u0293\u0296\u0297\u029A\u02A0\u02A3\u02A5\u02A6\u02A8-\u02AF\u0313\u037B-\u037D\u03CF\u03FD-\u03FF]" +
  //"";
      UnicodeUtilities.showSet(UnicodeUtilities.parseUnicodeSet("[:archaic=/.+/:]"),false, false, printWriter);
      printWriter.flush();
      UnicodeUtilities.showPropsTable(printWriter);
      printWriter.flush();
  
    }

}
