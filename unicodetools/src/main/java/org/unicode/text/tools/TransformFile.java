package org.unicode.text.tools;

import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.tools.emoji.CandidateData;

public class TransformFile {
    public static void main(String[] args) {
        Class<?> class1 = CandidateData.class; // TODO generalize
        String transformSource = args.length > 0 ? args[0] : "candidateTransform.txt";
        String sourceFile = args.length > 1 ? args[1] : "candidateData.txt";
        StringBuilder rules = new StringBuilder();
        for (String line : FileUtilities.in(class1, transformSource)) {
            if (rules.length() != 0) {
                rules.append('\n');
            }
            rules.append(line);
        }
        Transform<String, String> trans =
                Transliterator.createFromRules("foo", rules.toString(), Transliterator.FORWARD);

        int countChanged = 0;
        for (String line : FileUtilities.in(class1, sourceFile)) {
            String newLine = trans.transform(line);
            if (!line.equals(newLine)) {
                ++countChanged;
            }
            System.out.println(newLine);
        }
        System.out.println("Lines Changed: " + countChanged);
    }
}
