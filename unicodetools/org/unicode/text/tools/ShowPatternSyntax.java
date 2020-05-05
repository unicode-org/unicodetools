package org.unicode.text.tools;

import org.unicode.cldr.util.props.BagFormatter;
import org.unicode.text.UCD.ToolUnicodePropertySource;

import com.ibm.icu.text.UnicodeSet;

public class ShowPatternSyntax {
    public static void main(String[] args) {
        final ToolUnicodePropertySource source = ToolUnicodePropertySource.make("");
        final UnicodeSet syntax = source.getSet("Pattern_Syntax=true");
        final UnicodeSet unassigned = source.getSet("gc=Cn");
        final UnicodeSet unassignedSyntax = new UnicodeSet(syntax).retainAll(unassigned);
        //UnicodeSet spanned = Utility.addDontCareSpans(unassignedSyntax, syntax);
        final BagFormatter bf = new BagFormatter();
        bf.setLabelSource(source.getProperty("Block"));
        bf.setNameSource(null);
        System.out.println("Blocks");
        System.out.println(bf.showSetNames(unassignedSyntax));
    }
}
