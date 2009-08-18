package org.unicode.text.tools;

import org.unicode.text.UCD.ToolUnicodePropertySource;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.text.UnicodeSet;

public class ShowPatternSyntax {
  public static void main(String[] args) {
    ToolUnicodePropertySource source = ToolUnicodePropertySource.make("");
    UnicodeSet syntax = source.getSet("Pattern_Syntax=true");
    UnicodeSet unassigned = source.getSet("gc=Cn");
    UnicodeSet unassignedSyntax = new UnicodeSet(syntax).retainAll(unassigned);
    //UnicodeSet spanned = Utility.addDontCareSpans(unassignedSyntax, syntax);
    BagFormatter bf = new BagFormatter();
    bf.setLabelSource(source.getProperty("Block"));
    bf.setNameSource(null);
    System.out.println("Blocks");
    System.out.println(bf.showSetNames(unassignedSyntax));
  }
}
