package org.unicode.tools.emoji;

import com.google.common.base.Objects;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import java.io.IOException;
import java.io.PrintWriter;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.UCD.NamesList;
import org.unicode.text.utility.Utility;
import org.unicode.tools.emoji.Emoji.Source;

public class GenerateExtPict {
    static {
        System.setProperty("ALLOW_UNICODE_NAME", "T");
    }

    enum Cat {
        E,
        T,
        X
    }

    public static void main(String[] args) {
        IndexUnicodeProperties iup = IndexUnicodeProperties.make();
        UnicodeSet unassigned =
                iup.loadEnumSet(UcdProperty.General_Category, General_Category_Values.Unassigned);
        UnicodeSet emoji = iup.loadEnumSet(UcdProperty.Emoji, Binary.Yes);
        UnicodeSet emojiPres = iup.loadEnumSet(UcdProperty.Emoji_Presentation, Binary.Yes);
        UnicodeSet extpict =
                new UnicodeSet()
                        .addAll(iup.loadEnumSet(UcdProperty.Extended_Pictographic, Binary.Yes))
                        .removeAll(unassigned)
                        .freeze();
        UnicodeMap<Block_Values> blockMap = iup.loadEnum(UcdProperty.Block, Block_Values.class);
        NamesList namesList = GenerateEmoji.NAMESLIST;

        String filename = "plainVcolor.html";
        try (PrintWriter out = FileUtilities.openUTF8Writer(Emoji.TR51_INTERNAL_DIR, filename)) {
            ChartUtilities.writeHeader(
                    filename,
                    out,
                    "Plain vs Color Comparison",
                    null,
                    false,
                    ""
                            + "<p>Available color glyphs vs plain (black &amp; white). "
                            //                    + "Note: the Emoji Style images are those
                            // available on some platform, and many go beyond the"
                            //                    + "Emoji property."
                            + "</p>\n",
                    Emoji.DATA_DIR_PRODUCTION,
                    Emoji.TR51_HTML);
            out.println("<table " + "border='1'" + ">");
            out.println(
                    "<tr>"
                            + "<th>Hex</th>"
                            + "<th>Char</th>"
                            + "<th>Text Style</th>"
                            + "<th>Emoji Style</th>"
                            + "<th>E/TPres</th>"
                            + "<th>Name</th>"
                            + "</tr>");
            String lastBlock = "";
            for (String s : extpict) {
                Cat cat = emojiPres.contains(s) ? Cat.E : emoji.contains(s) ? Cat.T : Cat.X;
                String plain = GenerateEmoji.getImage(Source.plain, s, s, true, "");

                String color = cat == Cat.X ? "" : GenerateEmoji.getBestImage(s, true, ""); //
                String block = blockMap.get(s).toString();
                String subHead = namesList.subheads.get(s);
                if (subHead != null) {
                    block = block + " > " + subHead;
                }
                block = TransliteratorUtilities.toHTML.transform(block);

                if (!Objects.equal(block, lastBlock)) {
                    lastBlock = block;
                    out.println("<tr><th colSpan='6'>" + block + "</th></tr>");
                }

                out.println(
                        "<tr>"
                                + "<td class='code'>"
                                + Utility.hex(s)
                                + "</td>"
                                + "<td class='chars'>"
                                + s
                                + "</td>"
                                + "<td class='andr'>"
                                + plain
                                + "</td>"
                                + "<td class='andr'"
                                + (cat != Cat.X ? "" : "style='background-color:LightGray'")
                                + ">"
                                + color
                                + "</td>"
                                + "<td class='name' "
                                + (cat == Cat.E
                                        ? " style='background-color:LightGreen'>EPres"
                                        : cat == Cat.T
                                                ? ">TPres"
                                                : " style='background-color:LightGray'>n/a")
                                + "</td>"
                                + "<td class='name'>"
                                + iup.getName(s, "+")
                                + "</td>"
                                + "</tr>");
            }
            out.println("</table></body></html>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
