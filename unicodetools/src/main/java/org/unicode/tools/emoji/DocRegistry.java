package org.unicode.tools.emoji;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.tools.emoji.DocRegistry.DocRegistryEntry;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;

public class DocRegistry {
    private static final Pattern PROPOSAL = Pattern.compile("(L2/\\d{2}-\\d{3})(R\\d?)?");

    public static final class DocRegistryEntry {
	final String L2;
	final String title;
	final String source;
	final LocalDate other;

	public DocRegistryEntry(String l2, String title, String source, LocalDate other) {
	    L2 = l2;
	    this.title = title;
	    this.source = source;
	    this.other = other;
	}

	public DocRegistryEntry(List<String> parts) {
	    this(parts.get(0), parts.get(1), parts.get(2), LocalDate.parse(parts.get(3)));
	}

	@Override
	public String toString() {
	    return "L2=«" + L2
		    + "» title=«" + title
		    + "» source=«" + source
		    + "» other=" + other
		    ;
	}
    }
    static final Map<String, DocRegistryEntry> map = load();

    private static Map<String, DocRegistryEntry> load() {
	Splitter TAB_SPLITTER = Splitter.on('\t').trimResults();
	Map<String, DocRegistryEntry> _map = new TreeMap<>();
	Matcher L2Matcher = PROPOSAL.matcher("");
	String lastLine = "";
	Set<String> errorLines = new LinkedHashSet<>();
	int lineCount = 0;
	for (String line : FileUtilities.in(DocRegistry.class, "docRegistry.txt")) {
	    ++lineCount;
	    if (line.trim().isEmpty() || line.startsWith("#")) {
		continue;
	    }
	    List<String> parts = TAB_SPLITTER.splitToList(line);
	    if (parts.size() < 3) {
		System.out.println();
		errorLines.add(lineCount + ") Skipping: " + parts + "\n\tlast=" + lastLine);
		continue;
	    }
	    String L2 = parts.get(0);
	    if (!L2Matcher.reset(L2).matches()) {
		System.out.println();
		errorLines.add(lineCount + ") Bad L2: " + line + "\n\tlast=" + lastLine);
		continue;
	    }
	    if (L2Matcher.group(2) != null) {
		L2 = L2Matcher.group(1);
		parts = new ArrayList<>(parts);
		parts.set(0, L2); // fix
	    }
	    _map.put(L2, new DocRegistryEntry(parts));
	    lastLine = line;
	}
	if (errorLines.isEmpty()) {
	    throw new IllegalArgumentException("Bad data\n" + Joiner.on('n').join(errorLines));
	}
	return ImmutableMap.copyOf(_map);
    }

    public static void main(String[] args) {
	Matcher m = Pattern.compile("(?i)working\\s*draft").matcher("");

	for (Entry<String, DocRegistryEntry> entry : DocRegistry.map.entrySet()) {
	    DocRegistryEntry item = entry.getValue();
	    if (m.reset(item.title).find()) {
		System.out.println(item);
	    }
	}
    }

    public static DocRegistryEntry get(String proposal) {
	proposal = proposal.replace('\u2011', '-');
	DocRegistryEntry result = map.get(proposal);
	if (result == null) {
	    Matcher L2Matcher = PROPOSAL.matcher(proposal);
	    if (L2Matcher.matches()) {
		result = map.get(L2Matcher.group(1));
	    }
	}
	return result;
    }

    public static String getProposalForHtml(String proposalItem) {
	DocRegistryEntry item = get(proposalItem);
	String title = item == null ? "" : " title ='" + TransliteratorUtilities.toHTML.transform(item.title + " 👈 " + item.source) + "'";
	return "<a target='e-prop' "
	+ "href='https://www.unicode.org/cgi-bin/GetDocumentLink?" + proposalItem.replace('\u2011', '-')
	+ "'" + title + ">"
	+ proposalItem + "</a>";
    }
}
