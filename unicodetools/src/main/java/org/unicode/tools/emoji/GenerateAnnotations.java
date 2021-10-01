package org.unicode.tools.emoji;

import java.awt.ItemSelectable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.Annotations.AnnotationSet;
import org.unicode.text.utility.Utility;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

public class GenerateAnnotations {
    static AnnotationSet english = Annotations.getDataSet("en");

    static CandidateData candidateData = CandidateData.getInstance();

    public static void main(String[] args) {
	System.err.println("OLD, use GenerateCldrData");
	System.exit(-1);
	
	EmojiDataSourceCombined betaData = new EmojiDataSourceCombined();

	//showGenderVariants(betaData);
	//if (true) return;

	//showStats(Emoji.VERSION3, Emoji.VERSION4, Emoji.VERSION5);

	EmojiData lastData = EmojiData.of(Emoji.VERSION_LAST_RELEASED);
	UnicodeSet set = new UnicodeSet()
		.addAll(betaData.getAllEmojiWithoutDefectivesOrModifiers())
		.removeAll(lastData.getAllEmojiWithoutDefectivesOrModifiers())
		.removeAll(betaData.getTagSequences())
		.freeze();

	UnicodeSet full = new UnicodeSet()
		.addAll(betaData.getAllEmojiWithoutDefectives())
		.removeAll(lastData.getAllEmojiWithoutDefectives())
		.freeze();

	TreeSet<String> sorted = set.addAllTo(new TreeSet<>(EmojiOrder.STD_ORDER.codepointCompare));
	UnicodeSet found = new UnicodeSet();
	Set<String> rootList = new LinkedHashSet<>();
	for (String source : sorted) {
	    String name = getName(source);
	    Set<String> annotations = english.getKeywords(source);
	    if (annotations.isEmpty()) {
		if (source.startsWith("🧑")) {
		    String sourcem = source.replace("🧑", "👩");
		    Set<String> annotations2 = english.getKeywords(sourcem);
		    if (annotations2 != null) {
			annotations = new LinkedHashSet<>();
			for (String item : annotations2) {
			    if (!item.contains("woman")) {
				annotations.add(item);
			    }
			}
		    }
		} else {
		    annotations = CandidateData.getInstance().getAnnotations(source);
		}
		if (annotations == null) {
		    annotations = Collections.emptySet();
		}
	    } else {
		// we have annotations already (synthesized), so skip
		continue;
	    }
	    //			if (name == null) {
	    //				name = candidateData.getShorterName(source);
	    //				annotations = candidateData.getAnnotations(source);
	    //			}
	    System.out.println("<annotation cp=\"" + source + "\">" + CollectionUtilities.join(annotations, " | ")
	    + "</annotation>");
	    System.out.println("<annotation cp=\"" + source + "\" type=\"tts\">" + (name == null ? "???" : name)
		    + "</annotation>");
	    System.out.println();
	    found.add(source);
	}
	System.out.println("Add to emoji list (" 
		+ found.size()
		+ "): " + found.toPattern(false));

	for (String s : missed) {
	    System.out.println("**** Fetching from candidateData, not CLDR: " + s + "\t" + Utility.hex(s));

	}

    }

    static private String MAN = UTF16.valueOf(0x1F468);
    static private String ADULT = UTF16.valueOf(0x1F9D1);

    private static void showGenderVariants(EmojiData betaData) {
	Multimap<CountEmoji.ZwjType, String> data = TreeMultimap.create();
	for (String s : betaData.getAllEmojiWithoutDefectivesOrModifiers()) {
	    if (!s.contains(Emoji.JOINER_STR)) {
		continue;
	    }
	    CountEmoji.ZwjType type = CountEmoji.ZwjType.getType(s);
	    if (s.contains(Emoji.MALE)) {
		int first = s.codePointAt(0);
		data.put(type, UTF16.valueOf(first));
	    } else {
		switch(type) {
		case family: break;
		default:
		    if (s.startsWith(MAN)) {
			String sequence = ADULT + s.substring(MAN.length());
			data.put(type, sequence);

		    }
		}
	    }
	}

	int count = 0;
	for (Entry<CountEmoji.ZwjType, String> entry : data.entries()) {
	    CountEmoji.ZwjType type = entry.getKey();
	    String sequence = entry.getValue();
	    String shortName = getName(sequence);
	    System.out.println(sequence + "\t" + shortName);
	}

	for (Entry<CountEmoji.ZwjType, Collection<String>> entry : data.asMap().entrySet()) {
	    CountEmoji.ZwjType type = entry.getKey();
	    UnicodeSet sequence = new UnicodeSet().addAll(entry.getValue());
	    System.out.println(type + "\t" + sequence.toPattern(false));
	}

    }

    static private int ADULT_CP = ADULT.codePointAt(0);

    private static String getName(String source) {
	String name = getShortName(source);
	if (name == null) {
	    int first = source.codePointAt(0);
	    if (first == ADULT_CP) {
		String seq = MAN + source.substring(ADULT.length());
		name = getShortName(seq);
		if (name != null) {
		    name =  name.startsWith("man ") ? name.substring(4) : "??"+name;
		}
	    }
	}
	return name;
    }

    static final UnicodeSet missed = new UnicodeSet();

    private static String getShortName(String source) {
	String result = english.getShortName(source);
	if (result == null) {
	    result = english.getShortName(source, candidateData);
	    if (result != null) {
		missed.add(source);
	    }
	}
	return result;
    }

    private static void showStats(VersionInfo... versions) {
	UnicodeSet last = new UnicodeSet();
	for (VersionInfo version : versions) {
	    EmojiData betaData = EmojiData.of(version);
	    UnicodeSet current = betaData.getAllEmojiWithoutDefectives();
	    System.out.println(version + "\t" + "delta: " + (current.size() - last.size()));
	    System.out.println(version + "\t" + "total: " + current.size());
	    last = current;
	}
    }
}
