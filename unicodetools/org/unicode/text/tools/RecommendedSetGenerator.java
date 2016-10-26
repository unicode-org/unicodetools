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
  private static final String DIRECTORY = "data/security/9.0.0";

  public static void main(String[] args) {
    Sets sets = generateSet();
    System.out.println(uniSetToCodeString(sets.inclusion, "INCLUSION", true));
    System.out.println(uniSetToCodeString(sets.recommended, "RECOMMENDED", true));
    System.out.println("\n\nC++ Version:\n\n");
    System.out.println(uniSetToCodeString(sets.inclusion, "inclusionPat", false));
    System.out.println(uniSetToCodeString(sets.recommended, "recommendedPat", false));
  }

  public static String uniSetToCodeString(UnicodeSet uniset, String varName, boolean isJava) {
    String str = uniset.toString();
    StringBuilder result = new StringBuilder();
    result.append(isJava ? "public static final UnicodeSet " + varName + " = new UnicodeSet("
        : "static const char *" + varName + " = ");
    for (int i = 0; i < str.length(); i += 75) {
      String line = str.substring(i, Math.min(i + 75, str.length()));
      line = line.replace("\\", "\\\\");
      result.append("\n    " + (i == 0 || !isJava ? "  " : "+ ") + "\"" + line + "\"");
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
