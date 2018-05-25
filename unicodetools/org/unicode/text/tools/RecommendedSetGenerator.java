package org.unicode.text.tools;

import java.util.Set;

import org.unicode.text.UCD.IdentifierInfo.IdentifierStatus;
import org.unicode.text.UCD.IdentifierInfo.IdentifierType;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

/**
 * Generates the recommended UnicodeSet according to UTS 39. Used for updating the constant in ICU
 * SpoofChecker.
 * 
 * @author Shane Carr
 * @see com.ibm.icu.text.SpoofChecker
 */
public class RecommendedSetGenerator {
  /**
   * Update the directory to use for generating the data:
   */
  private static final String DIRECTORY = "data/security/11.0.0";

  public static void main(String[] args) {
    Sets sets = generateSet();
    System.out.println("# inclusion: \n" + sets.inclusion.toString());
    System.out.println("\n# recommended: \n" + sets.recommended.toString());
    System.out.println("\n\nJava Version:\n\n");
    System.out.println(uniSetToCodeString(sets.inclusion, "INCLUSION", true));
    System.out.println(uniSetToCodeString(sets.recommended, "RECOMMENDED", true));
    System.out.println("\n\nC++ Version:\n\n");
    System.out.println(uniSetToCodeString(sets.inclusion, "inclusionPat", false));
    System.out.println(uniSetToCodeString(sets.recommended, "recommendedPat", false));
  }

  public static String uniSetToCodeString(UnicodeSet uniset, String varName, boolean isJava) {
    String str = uniset.toString().replace("\\", "\\\\");
    StringBuilder result = new StringBuilder();
    if (isJava) {
      result.append("public static final UnicodeSet " + varName + " = new UnicodeSet(");
    } else {
      result.append("static const char16_t *" + varName + " =");
    }
    for (int i = 0; i < str.length();) {
      // split into short lines
      int end = i + 75;
      if (end > str.length()) {
        end = str.length();
      }
      // break before an escape, not in the middle
      // 11 = "\\\\U0010FFFF".length()
      int min = end - 11;
      if (min < i) { min = i; }
      char nextChar = 0;
      for (int j = end; min < j;) {
        char c = str.charAt(--j);
        if (c == '\\') {
          if ((nextChar == 'u' && (end - j) >= 6) || (nextChar == 'U' && (end - j) >= 10)) {
            // The escape sequence is completely on this line.
          } else {
            // Truncate before double escape.
        	if (i < j && str.charAt(j - 1) == '\\') {
        	  --j;
        	}
        	// Do not truncate to nothing.
            if (i < j) {
              end = j;
            }
          }
          break;
        }
        nextChar = c;
      }
      String line = str.substring(i, end);
      if (isJava) {
        result.append("\n    " + (i == 0 ? "\"" : "+ \"") + line + '"');
      } else {
        result.append("\n    u\"" + line + '"');
      }
      i = end;
    }
    result.append(isJava ? "\n).freeze();\n" : ";\n");
    return result.toString();
  }

  public static Sets generateSet() {
    XIDModifications inst = new XIDModifications(DIRECTORY);

    // Compute sets based on status
    UnicodeSet allowedS = new UnicodeSet();
    UnicodeSet restrictedS = new UnicodeSet();
    UnicodeMap<IdentifierStatus> statuses = inst.getStatus();
    for (String range : statuses) {
      IdentifierStatus status = statuses.get(range);
      if (status == IdentifierStatus.allowed) {
        allowedS.add(range);
      } else {
        restrictedS.add(range);
      }
    }
    allowedS.freeze();
    restrictedS.freeze();

    // Compute sets based on types
    UnicodeSet recommendedT = new UnicodeSet();
    UnicodeSet inclusionT = new UnicodeSet();
    UnicodeSet restrictedT = new UnicodeSet();
    UnicodeMap<Set<IdentifierType>> typeses = inst.getType();
    for (String range : typeses) {
      Set<IdentifierType> types = typeses.get(range);
      if (types.contains(IdentifierType.inclusion)) {
        inclusionT.add(range);
      } else if (types.contains(IdentifierType.recommended)) {
        recommendedT.add(range);
      } else {
        restrictedT.add(range);
      }
    }
    recommendedT.freeze();
    inclusionT.freeze();
    restrictedT.freeze();
    assert restrictedS.equals(restrictedT);

    // ALLOWED should be the union of RECOMMENDED and INCLUSION.
    UnicodeSet allowed = recommendedT.cloneAsThawed().addAll(inclusionT).freeze();
    assert allowedS.equals(allowed);

    // Return value
    Sets result = new Sets();
    result.inclusion = inclusionT;
    result.recommended = recommendedT;
    return result;
  }

  public static class Sets {
    public UnicodeSet recommended;
    public UnicodeSet inclusion;
  }
}
