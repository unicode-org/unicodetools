package org.unicode.tools.emoji;

import com.ibm.icu.util.ICUUncheckedIOException;
import java.io.IOException;
import java.util.Locale;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.utility.UtilityBase;

public class ChartUtilities {

    public static void writeHeader(
            String outFileName,
            Appendable out,
            String title,
            String indexRelLink,
            boolean skipVersion,
            String firstLine,
            String dataDir,
            String tr51Url) {
        final String fullTitle = title + (skipVersion ? "" : ", v" + Emoji.VERSION_STRING);
        String headerLine =
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" "
                        + "\"http://www.w3.org/TR/html4/loose.dtd\">\n"
                        + UtilityBase.HTML_HEAD
                        + "<link rel='stylesheet' type='text/css' href='https://www.unicode.org/webscripts/standard_styles.css'>\n"
                        + "<link rel='stylesheet' type='text/css' href='emoji-list.css'>\n"
                        + "<title>"
                        + fullTitle
                        + (skipVersion ? "" : Emoji.BETA_TITLE_AFFIX)
                        + "</title>\n"
                        + "</head>\n"
                        + "<body>\n"
                        + ChartUtilities.getUnicodeHeader(indexRelLink)
                        + ChartUtilities.getButton()
                        + "\n"
                        + "<h1>"
                        + fullTitle
                        + (skipVersion ? "" : Emoji.BETA_HEADER_AFFIX)
                        + "</h1>\n"
                        + (skipVersion ? "" : ChartUtilities.getPointToOther(outFileName, title))
                        + "<p style='text-align:center'>"
                        + "<a target='text' href='index.html'>Index &amp; Help</a>\n"
                        + " | <a target='rights' href='../images.html'>Images &amp; Rights</a>\n"
                        + " | <a target='doc' href='"
                        + tr51Url
                        + "'>Spec</a>\n"
                        + " | <a target='submitting-emoji' href='../../emoji/proposals.html'>Proposing Additions</a>"
                        + "</p>\n"
                        + firstLine
                        + (dataDir == null
                                ? ""
                                : ""
                                        + "<p>While these charts use a particular version of the <a target='emoji-data' href='"
                                        + dataDir
                                        + "'>Unicode Emoji data files</a>, "
                                        + "the images and format may be updated at any time."
                                        + " For any production usage, consult those data files. "
                                        + " For information about the contents of each column, "
                                        + "such as the <b>CLDR Short Name</b>, click on the column header."
                                        + " For further information, see "
                                        + "<a target='text' href='index.html'>Index &amp; Help</a>.</p>\n");
        try {
            out.append(headerLine);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    static String getPointToOther(String outFileName, String title) {
        return !Emoji.BETA_IS_OPEN && !Emoji.IS_BETA
                ? ""
                : "<blockquote><i>For the "
                        + (Emoji.IS_BETA
                                ? "current released version, see <b><a href='../charts-"
                                        + Emoji.VERSION_LAST_RELEASED_STRING
                                        + "/"
                                        + outFileName
                                        + "'>v"
                                        + Emoji.VERSION_LAST_RELEASED_STRING
                                : "new beta version, see <b><a href='../charts-"
                                        + Emoji.VERSION_BETA_STRING
                                        + "/"
                                        + outFileName
                                        + "'>v"
                                        + Emoji.VERSION_BETA_STRING_WITH_COLOR)
                        + "</a></b>.</i></blockquote>\n";
    }

    static final String UNICODE_HEADER =
            ""
                    + "<div class='icon'>"
                    + "<a href='https://www.unicode.org/'>"
                    + "<img class='logo' alt='[Unicode]' src='https://www.unicode.org/webscripts/logo60s2.gif'></a>"
                    + "<a class='bar' target='text' href='%%CHARTS_LINK%%'>Emoji Charts</a>"
                    + "</div>"
                    + "<div class='gray'>&nbsp;</div>"
                    + "<div class='main'>";

    public static String getUnicodeHeader(String indexRelLink) {
        return FileUtilities.replace(
                UNICODE_HEADER,
                "%%CHARTS_LINK%%",
                (indexRelLink == null ? "index.html" : indexRelLink));
    }

    public static String getButton() {
        return "\n<div class='aacButton' title='Show your support of Unicode'>"
                + "<a target='sponsors' href='../../consortium/adopt-a-character.html'><b>Adopt a Character</b><br>"
                + "<img class='aacImage' alt='AAC Animation' src='../../consortium/images/aac-some-sponsors.gif'></a>"
                + "</div>";
    }

    public static void writeFooter(Appendable out) {
        try {
            out.append(
                    "\n</div><div class='copyright'>"
                            // + "<hr width='50%'>"
                            + "<br><a href='https://www.unicode.org/copyright.html'>"
                            + "<img src='https://www.unicode.org/img/hb_notice.gif' "
                            + "style='border-style: none; width: 216px; height=50px;' "
                            + "alt='Access to Copyright and terms of use'>"
                            + "</a>"
                            + "</div>"
                            + "</body></html>\n");
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public static String getDoubleLink(String href, String anchorText) {
        href = fixAnchor(href);
        return "<a href='#" + href + "' name='" + href + "'>" + anchorText + "</a>";
    }

    public static String fixAnchor(String href) {
        return href.replace(' ', '_').toLowerCase(Locale.ENGLISH);
    }

    public static String getLink(String href, String anchorText, String target) {
        href = fixAnchor(href);
        return "<a"
                + " href='#"
                + href
                + "'"
                + (target == null ? "" : " target='" + target + "'")
                + ">"
                + anchorText
                + "</a>";
    }

    public static String getDoubleLink(String anchor) {
        return getDoubleLink(anchor, anchor);
    }

    public static String htmlSpanForSkintone(String modifier) {
        return "<span style=\"display:inline-block;\">"
                + modifier
                + "</span>";
    }
}
