package org.unicode.text.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UnicodeProperty;
import org.unicode.text.utility.Utility;
import org.unicode.tools.Segmenter;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;

public class Indexer {

    static Transliterator toHTML;
    static Transliterator toHTMLInput;
    static String HTML_RULES_CONTROLS;

    static {
        String BASE_RULES =
                "'<' > '&lt;' ;"
                        + "'<' < '&'[lL][Tt]';' ;"
                        + "'&' > '&amp;' ;"
                        + "'&' < '&'[aA][mM][pP]';' ;"
                        + "'>' < '&'[gG][tT]';' ;"
                        + "'\"' < '&'[qQ][uU][oO][tT]';' ; "
                        + "'' < '&'[aA][pP][oO][sS]';' ; ";

        String CONTENT_RULES = "'>' > '&gt;' ;";

        String HTML_RULES = BASE_RULES + CONTENT_RULES + "'\"' > '&quot;' ; '' > '&apos;' ;";

        toHTMLInput = Transliterator.createFromRules("any-xml", HTML_RULES, Transliterator.FORWARD);

        HTML_RULES_CONTROLS =
                HTML_RULES
                        // + "\\u0000 > \uFFFD ; "
                        + "[\\uD800-\\uDB7F] > '<span class=\"high-surrogate\"><span>'\uFFFD'</span></span>' ; "
                        + "[\\uDB80-\\uDBFF] > '<span class=\"private-surrogate\"><span>'\uFFFD'</span></span>' ; "
                        + "[\\uDC00-\\uDFFF] > '<span class=\"low-surrogate\"><span>'\uFFFD'</span></span>' ; "
                        + "([[:cn:][:co:][:cc:]-[:White_Space:]]) > '<span class=\"control\">'$1'</span>' ; ";
        toHTML =
                Transliterator.createFromRules(
                        "any-xml", HTML_RULES_CONTROLS, Transliterator.FORWARD);
    }

    public static void main(String[] args) throws IOException {
      class IndexKey {
        IndexKey(String key, UnicodeProperty source) {
          this.key = key;
          this.source = source;
        }
        String key;
        UnicodeProperty source;
      };
      class IndexKeyComparator implements Comparator<IndexKey> {
        @Override
        public int compare(IndexKey left, IndexKey right) {
          final int primary = Collator.getInstance().compare(left.key, right.key);
          if (primary != 0) {
            return primary;
          }
          return left.source.getName().compareTo(right.source.getName());
        }
      }
      Map<IndexKey, UnicodeSet> index = new TreeMap<>(new IndexKeyComparator());
      final var iup = IndexUnicodeProperties.make();
      final var name = iup.getProperty(UcdProperty.Name);
      final var nameAlias = iup.getProperty(UcdProperty.Name_Alias);
      final var informalAlias = iup.getProperty(UcdProperty.Names_List_Alias);
      final var block = iup.getProperty(UcdProperty.Block);
      final var subheader = iup.getProperty(UcdProperty.Names_List_Subheader);
      final var subheader_notice = iup.getProperty(UcdProperty.Names_List_Subheader_Notice);
      final var comment = iup.getProperty(UcdProperty.Names_List_Comment);
      for (int cp = 0; cp <= 0x10FFFF; ++cp) {
        String characterName = name.getValue(cp);
        if (characterName != null) {
          characterName = characterName.replace(Utility.hex(cp), "#");
          index.computeIfAbsent(new IndexKey(characterName, name), k -> new UnicodeSet()).add(cp);
        }
        for (var prop : new UnicodeProperty[] { nameAlias, informalAlias, block, subheader, comment, subheader_notice }) {
          for (String key : prop.getValues(cp)) {
            if (key == null) {
              continue;
            }
            if (prop.getName().equals("Block")) {
              key = key.replace("_", " ");
            }
            index.computeIfAbsent(new IndexKey(key, prop), k -> new UnicodeSet()).add(cp);
          }
        }
        if (cp % 0x10000 == 0xFFFF) {
          System.err.println("Leaves for plane " + cp / 0x10000);
        }
      }
      var file = new PrintStream(new File("index.html"));
      file.println("<table>");
      Map<String, UnicodeSet> wordIndex = new TreeMap<>(Collator.getInstance());
      final var wordBreak = BreakIterator.getWordInstance();
      for (var entry : index.entrySet()) {
        //System.out.println(entry.getKey().key);
        file.println("<tr><td>" + toHTML.transform(entry.getKey().key) + "</td><td>" + htmlIndexValue(entry.getValue())+"</tr>");
        wordBreak.setText(entry.getKey().key);
        int start = 0;
        for (int end = wordBreak.next();
             end != BreakIterator.DONE;
             start = end, end = wordBreak.next()) {
          if (wordBreak.getRuleStatus() >= BreakIterator.WORD_LETTER) {
            wordIndex.computeIfAbsent(entry.getKey().key.substring(start, end),
            k-> new UnicodeSet()).addAll(entry.getValue());
          }
        }
      }
      System.err.println("Exctracted words");
      for (var entry : wordIndex.entrySet().stream().sorted((left, right) -> Integer.compare(left.getValue().size(), right.getValue().size())).toList()) {
        System.err.println(entry.getKey() + " " + entry.getValue().size() + " characters in " + blockCount(entry.getValue()) + " blocks");
      }
      file.println("</table>");
      file.close();
    }

    static int blockCount(UnicodeSet characters) {
      final var iup = IndexUnicodeProperties.make();
      final var block = iup.getProperty(UcdProperty.Block);
      UnicodeSet remainder = characters.cloneAsThawed();
      int count = 0;
      while (!remainder.isEmpty()) {
        ++count;
        remainder.removeAll(block.getSet(block.getValue(remainder.charAt(0))));
      }
      return count;
    }

    public static String htmlIndexValue(UnicodeSet characters) {
      final var iup = IndexUnicodeProperties.make();
      final var block = iup.getProperty(UcdProperty.Block);
      final boolean showBlocks = !block.getSet(block.getValue(characters.charAt(0))).containsAll(characters);
      var result = new StringBuilder();
      if (showBlocks) {
        result.append("<ul>");
      }
      for (var range : characters.ranges()) {
        if (range.codepointEnd == range.codepoint) {
          final int blockStart = block.getSet(block.getValue(range.codepoint)).getRangeStart(0);
          if (showBlocks) {
            result.append("<li>In " + block.getValue(range.codepoint) + ": ");
          }
          result.append("<a href='https://www.unicode.org/charts/PDF/U");
          result.append(Utility.hex(blockStart));
          result.append(".pdf'>U+");
          result.append(Utility.hex(range.codepoint));
          result.append("&nbsp;");
          result.append(toHTML.transform(Character.toString(range.codepoint)));
          result.append("</a>&nbsp;(<a href='https://util.unicode.org/UnicodeJsps/character.jsp?a=");
          result.append(Utility.hex(range.codepoint));
          result.append("'>properties</a>) ");
          if (showBlocks) {
            result.append("</li>");
          }
        } else {
          UnicodeSet remainder = new UnicodeSet(range.codepoint, range.codepointEnd);
          while (!remainder.isEmpty()) {
            final var currentBlock = block.getSet(block.getValue(remainder.charAt(0)));
            if (showBlocks) {
              result.append("<li>In " + block.getValue(remainder.charAt(0)) + ": ");
            }
            final int blockStart = currentBlock.getRangeStart(0);
            final var subrange = remainder.cloneAsThawed().retainAll(currentBlock);
            remainder.removeAll(currentBlock);
            result.append("<a href='https://www.unicode.org/charts/PDF/U");
            result.append(Utility.hex(blockStart));
            result.append(".pdf'>U+");
            result.append(Utility.hex(subrange.getRangeStart(0)));
            result.append("..U+");
            result.append(Utility.hex(subrange.getRangeEnd(0)));
            result.append("</a> ");
            if (showBlocks) {
              result.append("</li>");
            }
          }
        }
      }
      return result.toString();
    }
}
