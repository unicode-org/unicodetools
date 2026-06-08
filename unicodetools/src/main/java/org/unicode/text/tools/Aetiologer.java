import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.props.DerivedPropertyStatus;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.segmenter.LocalizedSegmenter;
import com.ibm.icu.segmenter.LocalizedSegmenter.SegmentationType;
import com.ibm.icu.segmenter.Segment;
import com.ibm.icu.segmenter.Segmenter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class Aetiologer {
  private static final Segmenter WORD_BREAK =
          LocalizedSegmenter.builder()
                  .setLocale(ULocale.ENGLISH)
                  .setSegmentationType(SegmentationType.WORD)
                  .build();
  private static final Pattern UTC_DESIGNATION = Pattern.compile("\\[(\\d+-[A-Z]\\d+[a-z]*)\\]");
  private static final Pattern TARGET_VERSION = Pattern.compile("Unicode\\s+(?:[Vv]ersion\\s+)?(\\d+(?:\\.\\d+)*)");
  private static final Pattern CODE_POINTS = Pattern.compile("(?:U\\+)?([0-9A-F]{4,})(?:\\.\\.(?:U\\+)?([0-9A-F]{4,}))?");
  public static final void main(String[] args) throws IOException {
    final Set<String> aliases = new HashSet<>();
    for (final var property : UcdProperty.values()) {
      if (property.getDerivedStatus() == DerivedPropertyStatus.Approved) {
        for (final var name : property.getNames().getAllNames()) {
          aliases.add(name);
        }
      }
    }
    final String resources =
        Settings.UnicodeTools.UNICODETOOLS_RSRC_DIR + "org/unicode/text/tools/";
    final var actions = new BufferedReader(
            new FileReader(new File(resources + "charindex_template.html")));
    for (String line = actions.readLine();
            line != null;
            line = actions.readLine()) {
      final var designation = UTC_DESIGNATION.matcher(line);
      if (!designation.find()) {
        continue;
      }
      final var target = TARGET_VERSION.matcher(line);
      if (!target.find()) {
        continue;
      }
      final var iup = IndexUnicodeProperties.make(target.group(1));
      final var previouIUP = IndexUnicodeProperties.make(Utility.getVersionPreceding(iup.getUcdVersion()));
      Iterable<Segment> words = WORD_BREAK.segment(line).segments()::iterator;
      Set<UcdProperty> candidateProperties = new TreeSet<>();
      final var codePointsMentioned = new UnicodeSet();
      for (final var segment : words) {
        if (aliases.contains(segment.getSubSequence())) {
          candidateProperties.add(UcdProperty.forString((String)segment.getSubSequence()));
        }
        final var range = CODE_POINTS.matcher(segment.getSubSequence());
        if (range.matches()) {
          if (range.group(2) != null) {
            codePointsMentioned.add(Utility.codePointFromHex(range.group(1)), Utility.codePointFromHex(range.group(2)));
          } else {
            codePointsMentioned.add(Utility.codePointFromHex(range.group(1)));
          }
        }
        if (candidateProperties.isEmpty() || codePointsMentioned.isEmpty()) {
          continue;
        }
        System.out.println(line);
        for (final var property : candidateProperties) {
          final var newProperty = iup.getProperty(property);
          final var oldProperty = previouIUP.getProperty(property);
          for (int cp : codePointsMentioned.codePoints()) {
            if (Objects.equals(oldProperty.getValue(cp), newProperty.getValue(cp))) {
              System.out.println("Not " + property + ": value did not change for " + Utility.hex(cp) + " in " + iup.getUcdVersion()):
            }
          }
        }
      }
    }
  }
}