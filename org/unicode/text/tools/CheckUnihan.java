package org.unicode.text.tools;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.Relation;

public class CheckUnihan {

	public static void main(String[] args) {
		final SortedSet<String> props = new TreeSet<String>();
		final Relation<String,String> values = Relation.of(new HashMap<String,Set<String>>(), HashSet.class);
		final Pattern tabSplitter = Pattern.compile("\t");
		for (final File file : new File(Utility.UCD_DIRECTORY + "/Unihan").listFiles()) {
			System.out.println(file.getName());
			for (final String line : FileUtilities.in(file.getParent(), file.getName())) {
				if (line.length() == 0 || line.startsWith("#")) {
					continue;
				}
				//U+3405  kOtherNumeric   5
				final String[] parts = tabSplitter.split(line);
				props.add(parts[1]);
				if (!parts[2].contains(" ")) {
					continue;
				}
				values.put(parts[1], parts[2]);
			}
		}
		for (final String prop : props) {
			final Set<String> set = values.get(prop);
			if (set == null) {
				System.out.println(prop + "\t" + "NO values with spaces");
			} else {
				System.out.println(prop + "\t" + "values with spaces: " + set.iterator().next());
			}
		}
	}
}
