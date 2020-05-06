package org.unicode.text.UCD;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.IdUsage;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.util.props.BagFormatter;
import org.unicode.cldr.util.props.UnicodeProperty;
import org.unicode.cldr.util.props.UnicodeProperty.Factory;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.ScriptInfo;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Identifier_Type_Values;
import org.unicode.props.UcdPropertyValues.NFKC_Quick_Check_Values;
import org.unicode.text.UCD.GenerateConfusables.FakeBreak;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableSet;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ICUException;

public class IdentifierInfo {

    private static final Set<Identifier_Type> SINGLETON_INCLUSION = Collections.singleton(Identifier_Type.inclusion);

    private static final boolean MAIN_CODE = true;

    private static final boolean DEBUG = false;
    private static final UCD DEFAULT_UCD = Default.ucd();
    private static final Factory UPS = ToolUnicodePropertySource.make(Settings.latestVersion);

    private static IdentifierInfo info;

    static IdentifierInfo getIdentifierInfo() {
	try {
	    if (info == null) {
		info = new IdentifierInfo();
	    }
	    return info;
	} catch (final Exception e) {
	    throw (RuntimeException) new IllegalArgumentException("Unable to access data").initCause(e);
	}
    }

    private static Integer MARK_NOT_NFC = Integer.valueOf(50);
    private static Integer MARK_NFC = Integer.valueOf(40);
    private static Integer MARK_INPUT_LENIENT = Integer.valueOf(30);
    private static Integer MARK_INPUT_STRICT = Integer.valueOf(20);
    private static Integer MARK_OUTPUT = Integer.valueOf(10);
    private static Integer MARK_ASCII = Integer.valueOf(10);
    static final String NOT_IN_XID = "not in XID+";

    static final IndexUnicodeProperties LATEST = IndexUnicodeProperties
	    .make(Default.ucdVersion());

    private static final UnicodeMap<General_Category_Values> GENERAL_CATEGORY = LATEST
	    .loadEnum(UcdProperty.General_Category, General_Category_Values.class);

    private static final UnicodeSet NON_CHARACTERS = GENERAL_CATEGORY
	    .getSet(General_Category_Values.Unassigned);
    private static final UnicodeSet DEPRECATED = LATEST
	    .loadEnum(UcdProperty.Deprecated, Binary.class)
	    .getSet(Binary.Yes);
    private static final UnicodeSet DEFAULT_IGNORABLE = LATEST
	    .loadEnum(UcdProperty.Default_Ignorable_Code_Point, Binary.class)
	    .getSet(Binary.Yes);
    private static final UnicodeSet WHITESPACE = LATEST
	    .loadEnum(UcdProperty.White_Space, Binary.class)
	    .getSet(Binary.Yes);
    private static final UnicodeSet NOT_NFKC = LATEST
	    .loadEnum(UcdProperty.NFKC_Quick_Check, NFKC_Quick_Check_Values.class)
	    .getSet(NFKC_Quick_Check_Values.No);

    //        private final boolean mergeRanges = true;

    private UnicodeSet removalSet;
    UnicodeSet remainingOutputSet;
    UnicodeSet inputSet_strict;
    private UnicodeSet inputSet_lenient;
    private UnicodeSet nonstarting;
    UnicodeSet propNFKCSet;
    //UnicodeSet notInXID;
    UnicodeSet xidPlus;

    private final UnicodeMap<String> additions = new UnicodeMap<>();
    private final UnicodeMap<String> remap = new UnicodeMap<>();
    private final UnicodeMap<IdentifierInfo.Identifier_Type> removals = new UnicodeMap<IdentifierInfo.Identifier_Type>();
    private final UnicodeMap<Set<IdentifierInfo.Identifier_Type>> identifierTypesMap = new UnicodeMap<>();
    private final UnicodeMap<String> recastRemovals = new UnicodeMap<String>();

    private UnicodeMap<String> reviews, removals2;
    UnicodeMap<Integer> lowerIsBetter;

    private UnicodeSet isCaseFolded;

    public static void main(String[] args) throws IOException {
	final IdentifierInfo info = IdentifierInfo.getIdentifierInfo();
	// show singletons
	Multimap<Identifier_Type,Set<Identifier_Type>> singleToSets = HashMultimap.create();
	for (Set<Identifier_Type> value : info.identifierTypesMap.getAvailableValues()) {
	    for (Identifier_Type v : value) {
		singleToSets.put(v, value);
	    }
	}
	for (Identifier_Type value : Identifier_Type.values()) {
	    Collection<Set<Identifier_Type>> sets = singleToSets.get(value);
	    System.out.println(value + ":\t " + CollectionUtilities.join(sets, " "));
	}
	System.out.println(info.identifierTypesMap.getSet(singleToSets.get(Identifier_Type.not_characters).iterator().next()));

	info.printIDNStuff();
    }

    private IdentifierInfo() throws IOException {
	isCaseFolded = new UnicodeSet();
	for (int cp = 0; cp <= 0x10FFFF; ++cp) {
	    Utility.dot(cp);
	    final int cat = DEFAULT_UCD.getCategory(cp);
	    if (cat == UCD_Types.Cn || cat == UCD_Types.Co || cat == UCD_Types.Cs) {
		continue;
	    }
	    final String source = UTF16.valueOf(cp);
	    final String cf = DEFAULT_UCD.getCase(source, UCD_Types.FULL, UCD_Types.FOLD);
	    if (cf.equals(source)) {
		isCaseFolded.add(cp);
	    }
	}

	propNFKCSet = UPS.getSet("NFKC_QuickCheck=N").complement();
	final UnicodeSet propXIDContinueSet = UPS.getSet("XID_Continue=Yes");

	//removals.putAll(propNFKCSet.complement(), PROHIBITED + "compat variant");
	loadFileData();
	xidPlus = new UnicodeSet(propXIDContinueSet).addAll(additions.keySet()).retainAll(propNFKCSet);

	GenerateConfusables.getIdentifierSet();
	//notInXID = new UnicodeSet(IDNOutputSet).removeAll(xidPlus);
	//removals.putAll(notInXID, PROHIBITED + NOT_IN_XID);
	//UnicodeSet notNfkcXid = new UnicodeSet(xidPlus).removeAll(removals.keySet()).removeAll(propNFKCSet);
	//removals.putAll(notNfkcXid, PROHIBITED + "compat variant");
	removalSet = new UnicodeSet();
	for (final IdentifierInfo.Identifier_Type value : removals.values()) {
	    if (value.isRestricted()) {
		removalSet.addAll(removals.getSet(value));
	    }
	}
	removalSet.freeze();

	remainingOutputSet = new UnicodeSet(GenerateConfusables.IDNOutputSet).removeAll(removalSet);

	final UnicodeSet remainingInputSet1 = new UnicodeSet(GenerateConfusables.IDNInputSet)
		.removeAll(removalSet).removeAll(remainingOutputSet);
	final UnicodeSet remainingInputSet = new UnicodeSet();
	final UnicodeSet specialRemove = new UnicodeSet();
	// remove any others that don't normalize/case fold to something in
	// the output set
	for (final UnicodeSetIterator usi = new UnicodeSetIterator(
		remainingInputSet1); usi.next();) {
	    final String nss = GenerateConfusables.getModifiedNKFC(usi.getString());
	    final String cf = DEFAULT_UCD.getCase(nss, UCD_Types.FULL, UCD_Types.FOLD);
	    final String cf2 = GenerateConfusables.getModifiedNKFC(cf);
	    if (remainingOutputSet.containsAll(cf2)) {
		remainingInputSet.add(usi.codepoint);
	    } else {
		specialRemove.add(usi.codepoint);
	    }
	}
	// filter out the items that are case foldings of items in output
	inputSet_strict = new UnicodeSet();
	for (final UnicodeSetIterator usi = new UnicodeSetIterator(
		remainingInputSet); usi.next();) {
	    final String ss = usi.getString();
	    final String nss = GenerateConfusables.getModifiedNKFC(ss);
	    final String cf = DEFAULT_UCD.getCase(ss, UCD_Types.FULL, UCD_Types.FOLD);
	    if (DEBUG && (usi.codepoint == 0x2126 || usi.codepoint == 0x212B)) {
		System.out.println("check");
	    }
	    //> > 2126 ; retained-input-only-CF # (?) OHM SIGN
	    //> > 212B ; retained-input-only-CF # (?) ANGSTROM SIGN

	    if (!remainingOutputSet.containsAll(nss)
		    && remainingOutputSet.containsAll(cf)) {
		inputSet_strict.add(ss);
	    }
	}
	// hack
	inputSet_strict.remove(0x03F4).remove(0x2126).remove(0x212B);
	inputSet_lenient = new UnicodeSet(remainingInputSet)
		.removeAll(inputSet_strict);
	nonstarting = new UnicodeSet(remainingOutputSet).addAll(
		remainingInputSet).retainAll(new UnicodeSet("[:M:]"));
	reviews = new UnicodeMap<>();
	//reviews.putAll(removals);
	for (final IdentifierInfo.Identifier_Type value : removals.values()) {
	    reviews.putAll(removals.getSet(value), value.propertyFileFormat());
	}
	reviews.putAll(remainingOutputSet, "output");
	reviews.putAll(inputSet_strict, "input");
	reviews.putAll(inputSet_lenient, "input-lenient");
	reviews.putAll(specialRemove, Identifier_Status.restricted + " ; output-disallowed");

	lowerIsBetter = new UnicodeMap<>();

	lowerIsBetter.putAll(propNFKCSet, MARK_NFC); // nfkc is better than the alternative
	lowerIsBetter.putAll(inputSet_lenient, MARK_INPUT_LENIENT);
	lowerIsBetter.putAll(inputSet_strict, MARK_INPUT_STRICT);
	lowerIsBetter.putAll(remainingOutputSet, MARK_OUTPUT);
	lowerIsBetter.putAll(remainingOutputSet, MARK_ASCII);
	lowerIsBetter.setMissing(MARK_NOT_NFC);

	// EXCEPTIONAL CASES
	// added to preserve source-target ordering in output.
	lowerIsBetter.put('\u0259', MARK_NFC);

	lowerIsBetter.freeze();
	// add special values:
	//lowerIsBetter.putAll(new UnicodeSet("["), new Integer(0));

	final UnicodeMap<String> nonstartingmap = new UnicodeMap<String>().putAll(nonstarting,
		"nonstarting");
	final UnicodeMap.Composer<String> composer = new UnicodeMap.Composer<String>() {
	    @Override
	    public String compose(int codepoint, String string, String a, String b) {
		if (a == null) {
		    return b;
		} else if (b == null) {
		    return a;
		} else {
		    return a.toString() + "-" + b.toString();
		}
	    }
	};
	reviews.composeWith(nonstartingmap, composer);
	reviews.putAll(new UnicodeSet(GenerateConfusables.IDNInputSet).complement(), "");
	//	final UnicodeMap.Composer<String> composer2 = new UnicodeMap.Composer<String>() {
	//	    @Override
	//	    public String compose(int codepoint, String string, String a, String b) {
	//		if (b == null) {
	//		    return a;
	//		}
	//		return "remap-to-" + Utility.hex(b.toString());
	//	    }
	//	};
	//reviews.composeWith(remap, composer2);
	removals2 = new UnicodeMap<String>().putAll(recastRemovals);
	removals2.putAll(UPS.getSet("XID_Continue=Yes").complement(),
		Identifier_Status.restricted + " ; " + NOT_IN_XID);
	removals2.setMissing("future?");

	additions.freeze();
	remap.freeze();

	for (final IdentifierInfo.Identifier_Type value : removals.values()) {
	    recastRemovals.putAll(removals.getSet(value), value.propertyFileFormat());
	}
	recastRemovals.freeze();
	removals.freeze();
	reviews.freeze();
	removals2.freeze();
    }

    public enum Identifier_Status {
	allowed("Allowed"),
	restricted("Restricted");
	final String name;
	private Identifier_Status(String name) {
	    this.name = name;
	}
	@Override
	public String toString() {
	    return name;
	}
	public static Identifier_Status fromString(String string) {
	    String rawReason = string.trim().replace("-","_").toLowerCase(Locale.ENGLISH);
	    return valueOf(rawReason);
	}
    }

    public enum Identifier_Type {
	recommended(Identifier_Type_Values.Recommended, Identifier_Status.allowed),
	inclusion(Identifier_Type_Values.Inclusion, Identifier_Status.allowed),
	//aspirational(Identifier_Type_Values.Aspirational, Identifier_Status.restricted),
	limited_use(Identifier_Type_Values.Limited_Use, Identifier_Status.restricted),
	uncommon_use(Identifier_Type_Values.Uncommon_Use, Identifier_Status.restricted),
	technical(Identifier_Type_Values.Technical, Identifier_Status.restricted),
	exclusion(Identifier_Type_Values.Exclusion, Identifier_Status.restricted),
	obsolete(Identifier_Type_Values.Obsolete, Identifier_Status.restricted),
	not_xid(Identifier_Type_Values.Not_XID, Identifier_Status.restricted),
	not_nfkc(Identifier_Type_Values.Not_NFKC, Identifier_Status.restricted),
	default_ignorable(Identifier_Type_Values.Default_Ignorable, Identifier_Status.restricted),
	deprecated(Identifier_Type_Values.Deprecated, Identifier_Status.restricted),
	not_characters(Identifier_Type_Values.Not_Character, Identifier_Status.restricted),
	;

	public final Identifier_Type_Values type;
	public final String name;
	public final Identifier_Status identifierStatus;

	private Identifier_Type(Identifier_Type_Values type, Identifier_Status identifierStatus) {
	    this.type = type;
	    this.name = type.toString();
	    this.identifierStatus = identifierStatus;
	}

	public static IdentifierInfo.Identifier_Type fromString(String string) {
	    String rawReason = string.trim().replace("-","_").toLowerCase(Locale.ENGLISH);
	    if (rawReason.equals("allowed")) {
		return Identifier_Type.recommended;
		//rawReason = GenerateConfusables.recommended_scripts;
	    } else if (rawReason.equals("historic")) {
		return Identifier_Type.obsolete;
	    } else if (rawReason.equals("aspirational")) {
		return Identifier_Type.limited_use;
	    } else if (rawReason.equalsIgnoreCase("not_chars")) {
		return Identifier_Type.not_characters;
	    }
	    try {
		return valueOf(rawReason);
	    } catch (Exception e) {
		e.printStackTrace();
		throw new ICUException(e);
	    }
	}

	public boolean isRestricted() {
	    return this != Identifier_Type.inclusion && this != Identifier_Type.recommended;
	}
	@Override
	public String toString() {
	    return name;
	}
	public String propertyFileFormat() {
	    return identifierStatus + " ; " + name;
	}
	public boolean replaceBy(IdentifierInfo.Identifier_Type possibleReplacement) {
	    return compareTo(possibleReplacement) < 0
		    //                    || this == historic && possibleReplacement == limited_use
		    ; // && this != historic;
	}

	static Splitter SPACE_SPLITTER = Splitter.on(' ').omitEmptyStrings().trimResults();

	public static Set<Identifier_Type> fromStringSet(String strings) {
	    EnumSet<Identifier_Type> results = EnumSet.noneOf(Identifier_Type.class);
	    for (String item : SPACE_SPLITTER.split(strings)) {
		results.add(Identifier_Type.fromString(item));
	    }
	    return results;
	}
    }
    /**
     * 
     */
    private void loadFileData() throws IOException {
	BufferedReader br;
	String line;
	// get all the removals.
	br = FileUtilities.openUTF8Reader(GenerateConfusables.indir, "removals.txt");
	removals.putAll(0,0x10FFFF, // new UnicodeSet("[^[:gc=cn:][:gc=co:][:gc=cs:][:gc=cc:]-[:whitespace:]]"),
		Identifier_Type.recommended);

	UnicodeSet sources = new UnicodeSet();
	line = null;
	int counter = 0;
	final Set<String> badLines = new LinkedHashSet<String>();
	while (true) {
	    line = Utility.readDataLine(br);
	    if (line == null) {
		break;
	    }
	    ++counter;
	    if (line.length() == 0) {
		continue;
	    }
	    if (DEBUG) {
		System.out.println(line);
	    }
	    try {
		sources.clear();
		final String[] pieces = Utility.split(line, ';');
		if (pieces.length < 2) {
		    throw new IllegalArgumentException(counter + " Missing line " + line);
		}
		boolean override = false;
		if (pieces.length == 3) {
		    String overrideString = pieces[2].trim();
		    if (!overrideString.isEmpty()) {
			if (!overrideString.equalsIgnoreCase("override")) {
			    throw new IllegalArgumentException(line);
			}
			override = true;
		    }
		}
		final String codelist = pieces[0].trim();
		if (UnicodeSet.resemblesPattern(pieces[0], 0)) {
		    sources = TestUnicodeInvariants.parseUnicodeSet(codelist); //.retainAll(allocated);
		    if (sources.contains("ᢰ")) {
			int x = 0;
		    }
		} else {
		    final String[] codes = Utility.split(codelist, ' ');
		    for (final String code : codes) {
			if (code.length() == 0) {
			    continue;
			}
			final String[] range = code.split("\\.\\.");
			final int start = Integer.parseInt(range[0], 16);
			int end = start;
			if (range.length > 1) {
			    end = Integer.parseInt(range[1], 16);
			}
			sources.add(start, end);
		    }
		}
		// removals.putAll(sources, reasons);
		// in case of conflict, we only replace the old reason if the new one is worse

		Set<Identifier_Type> reasonSet = Identifier_Type.fromStringSet(pieces[1]);

		for (String s : sources) {
		    for (Identifier_Type reasons : reasonSet) {
			Identifier_Type oldReason = removals.get(s);

			addToRemovalSets(s, reasons);

			if (oldReason == Identifier_Type.inclusion 
				|| oldReason == reasons) {
			    continue; // always ok
			}
			if (override 
				|| oldReason == null 
				|| oldReason.compareTo(reasons) < 0
				|| reasons == Identifier_Type.inclusion) {
			    removals.put(s, reasons);
			} else {
			    if (DEBUG) System.out.println("Skipping: " + Utility.hex(s) + " " + s + ", old: " + oldReason + " new: " + reasons);
			}
		    }
		}
		//                    if (reasons == Reason.recommended) {
		//                        removals.putAll(sources, UNPROHIBITED + recommended_scripts);
		//                    } else if (reasons.equals("inclusion")) {
		//                        removals.putAll(sources, UNPROHIBITED + reasons);
		//                    } else {
		//                        removals.putAll(sources, PROHIBITED + reasons);
		//                    }
	    } catch (final Exception e) {
		badLines.add(counter + ")\t" + line + "\t" + e.getMessage());
	    }
	}

	if (badLines.size() != 0) {
	    throw new RuntimeException(
		    "Failure on lines " + CollectionUtilities.join(badLines, "\t\n"));
	}
	final UnicodeMap<String> removalCollision = new UnicodeMap<String>();

	// first find all the "good" scripts
	UnicodeSet hasRecommendedScript = new UnicodeSet();
	Set<String> scripts = LATEST.load(UcdProperty.Script).values();
	for (final String script : scripts) {
	    String shortName = UcdPropertyValues.Script_Values.forName(script).getShortName();
	    Info scriptInfo = ScriptMetadata.getInfo(shortName);
	    if (scriptInfo == null) {
		System.out.println("No script metadata info for: " + script);
	    }
	    if (scriptInfo != null && scriptInfo.idUsage == IdUsage.RECOMMENDED) {
		final UnicodeSet us = ScriptInfo.IDENTIFIER_INFO.getSetWith(script);
		if (us != null) {
		    hasRecommendedScript.addAll(us);
		}
	    }
	}
	hasRecommendedScript.freeze();

	for (final String script : scripts) {
	    String shortName = UcdPropertyValues.Script_Values.forName(script).getShortName();
	    Info scriptInfo = ScriptMetadata.getInfo(shortName);
	    final IdUsage idUsage = scriptInfo != null 
		    ? scriptInfo.idUsage 
			    : IdUsage.EXCLUSION;
	    IdentifierInfo.Identifier_Type status;
	    switch(idUsage) {
	    //            case ASPIRATIONAL:
	    //                status = Identifier_Type.aspirational;
	    //                break;
	    case LIMITED_USE:
		status = Identifier_Type.limited_use;
		break;
	    case EXCLUSION:
		status = Identifier_Type.exclusion;
		break;
	    case RECOMMENDED:
	    default:
		status = null;
		break; // do nothing;
	    }
	    if (status != null) {
		final UnicodeSet us = ScriptInfo.IDENTIFIER_INFO.getSetWith(script);
		//final UnicodeSet us = new UnicodeSet().applyPropertyAlias("script", script);
		for (final String s : us) {
		    if (hasRecommendedScript.contains(s)) {
			continue; // skip those that have at least one recommended script
		    }
		    addToRemovalSets(s, status);
		    final IdentifierInfo.Identifier_Type old = removals.get(s);
		    if (old == null) {
			removals.put(s, status);
		    } else if (!old.equals(status)){
			if (old.replaceBy(status)) {
			    removalCollision.put(s, "REPLACING " + old + "\t!= (script metadata)\t" + status);
			    removals.put(s, status);
			} else {
			    removalCollision.put(s, "Retaining " + old + "\t!= (script metadata)\t" + status);
			}
		    }
		}
	    }
	}
	for (final String value : removalCollision.values()) {
	    if (DEBUG) System.out.println("*Removal Collision\t" + value + "\n\t" + removalCollision.getSet(value).toPattern(false));
	}
	removals.freeze();
	
	// pick up all the explict inclusions
	UnicodeSet inclusions = identifierTypesMap.getSet(SINGLETON_INCLUSION);
	
	// Clean up values by setting to singletons. ORDER is important!!
	identifierTypesMap.putAll(NOT_NFKC, Collections.singleton(Identifier_Type.not_nfkc));
	identifierTypesMap.putAll(DEFAULT_IGNORABLE, Collections.singleton(Identifier_Type.default_ignorable));
	identifierTypesMap.putAll(DEPRECATED, Collections.singleton(Identifier_Type.deprecated));
	identifierTypesMap.putAll(NON_CHARACTERS, Collections.singleton(Identifier_Type.not_characters));
	identifierTypesMap.putAll(GENERAL_CATEGORY.getSet(General_Category_Values.Private_Use), Collections.singleton(Identifier_Type.not_characters));
	identifierTypesMap.putAll(GENERAL_CATEGORY.getSet(General_Category_Values.Surrogate), Collections.singleton(Identifier_Type.not_characters));
	UnicodeSet controlNonWhitespace = new UnicodeSet(GENERAL_CATEGORY.getSet(General_Category_Values.Control))
		.removeAll(WHITESPACE);
	identifierTypesMap.putAll(controlNonWhitespace, Collections.singleton(Identifier_Type.not_characters));

	// restore inclusions
	UnicodeSet inclusions2 = identifierTypesMap.getSet(SINGLETON_INCLUSION);
	System.out.println("Restoring inclusions: " + new UnicodeSet(inclusions).removeAll(inclusions2));
	identifierTypesMap.putAll(inclusions, SINGLETON_INCLUSION);

	identifierTypesMap.putAll(identifierTypesMap.getSet(null), Collections.singleton(Identifier_Type.recommended));
	
	// make immutable
	// special hack for Exclusion + Obsolete!!
	for (Set<Identifier_Type> value : identifierTypesMap.getAvailableValues()) {
	    if (value.contains(Identifier_Type.exclusion) && value.contains(Identifier_Type.obsolete)) {
		UnicodeSet set = identifierTypesMap.getSet(value);
		EnumSet<Identifier_Type> value2 = EnumSet.copyOf(value);
		value2.remove(Identifier_Type.obsolete);
		identifierTypesMap.putAll(set, ImmutableSet.copyOf(value2));
	    }
	}
	identifierTypesMap.freeze();
	//removals.putAll(getNonIICore(), PROHIBITED + "~IICore");
	br.close();

	//      // get the word chars
	//      br = FileUtilities.openUTF8Reader(indir,
	//      "wordchars.txt");
	//      try {
	//        while (true) {
	//          line = Utility.readDataLine(br);
	//          if (line == null)
	//            break;
	//          if (line.length() == 0)
	//            continue;
	//          String[] pieces = Utility.split(line, ';');
	//          int code = Integer.parseInt(pieces[0].trim(), 16);
	//          if (pieces[1].trim().equals("remap-to")) {
	//            remap.put(code, UTF16.valueOf(Integer.parseInt(
	//                    pieces[2].trim(), 16)));
	//          } else {
	//            if (XIDContinueSet.contains(code)) {
	//              if (GenerateConfusables.DEBUG) System.out.println("Already in XID continue: "
	//                      + line);
	//              continue;
	//            }
	//            additions.put(code, "addition");
	//            removals.put(code, UNPROHIBITED + "inclusion");
	//          }
	//        }
	//      } catch (Exception e) {
	//        throw (RuntimeException) new RuntimeException(
	//                "Failure on line " + line).initCause(e);
	//      }
	//      br.close();

    }


    private void addToRemovalSets(String codepoint, final IdentifierInfo.Identifier_Type identifierType) {
	Set<Identifier_Type> oldSet = identifierTypesMap.get(codepoint);
	if (oldSet == null || identifierType == Identifier_Type.recommended) {
	    identifierTypesMap.put(codepoint, Collections.singleton(identifierType));
	} else if (identifierType == Identifier_Type.inclusion && !oldSet.contains(Identifier_Type.recommended)) {
	    identifierTypesMap.put(codepoint, Collections.singleton(identifierType));
	} else if (!oldSet.contains(identifierType)) {
	    EnumSet<Identifier_Type> newSet = EnumSet.copyOf(oldSet);
	    newSet.add(identifierType);
	    identifierTypesMap.put(codepoint, Collections.unmodifiableSet(newSet));
	}
    }

    enum Style {flat, byValue};
    void printIDNStuff() throws IOException {
	printIdentifierTypes(Style.byValue);
	printIdentifierTypes(Style.flat);
	printIdentifierStatus();

	printModificationsInternal();
	writeIDCharsInternal();
	writeIDReviewInternal();
    }

    /**
     * 
     */
    private void writeIDReviewInternal() throws IOException {
	final BagFormatter bf = GenerateConfusables.makeFormatter()
		.setUnicodePropertyFactory(UPS)
		.setLabelSource(null)
		.setShowLiteral(GenerateConfusables.EXCAPE_FUNNY)
		.setMergeRanges(true);

	final PrintWriter out = GenerateConfusables.openAndWriteHeader(GenerateConfusables.reformatedInternal, "review.txt", "Review List for IDN");
	//			PrintWriter out = FileUtilities.openUTF8Writer(outdir, "review.txt");
	//reviews.putAll(UNASSIGNED, "");
	//			out.print("\uFEFF");
	//			out.println("# Review List for IDN");
	//			out.println("# $Revision: 1.32 $");
	//			out.println("# $Date: 2010-06-19 00:29:21 $");
	//			out.println("");

	final UnicodeSet fullSet = reviews.keySet("").complement();

	bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
	}).set(reviews).setMain("Reviews", "GCB",
		UnicodeProperty.ENUMERATED, "1.0"));
	//bf.setMergeRanges(false);

	final FakeBreak fakeBreak = new FakeBreak();
	bf.setRangeBreakSource(fakeBreak);
	out.println("");
	out.println("# Characters allowed in IDNA");
	out.println("");
	bf.showSetNames(out, new UnicodeSet(fullSet)); // .removeAll(bigSets)
	//bf.setMergeRanges(true);
	//			out.println("");
	//			out.println("# Large Ranges");
	//			out.println("");
	//			bf.showSetNames(out, new UnicodeSet(fullSet).retainAll(bigSets));
	out.println("");
	out.println("# Characters disallowed in IDNA");
	out
	.println("# The IDNA spec doesn't allow any of these characters,");
	out
	.println("# so don't report any of them as being missing from the above list.");
	out
	.println("# Some possible future additions, once IDNA updates to Unicode 4.1, are given.");
	out.println("");
	//bf.setRangeBreakSource(UnicodeLabel.NULL);
	bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
	}).set(removals2).setMain("Removals", "GCB",
		UnicodeProperty.ENUMERATED, "1.0"));
	//bf.setValueSource(UnicodeLabel.NULL);
	bf.showSetNames(out, new UnicodeSet(GenerateConfusables.IDNInputSet).complement()
		.removeAll(GenerateConfusables.UNASSIGNED));
	out.close();
    }

    /**
     * 
     */
    private void writeIDCharsInternal() throws IOException {
	final BagFormatter bf = GenerateConfusables.makeFormatter();
	bf.setLabelSource(null);
	bf.setShowLiteral(GenerateConfusables.EXCAPE_FUNNY);
	bf.setMergeRanges(true);

	final UnicodeSet letters = new UnicodeSet("[[:Alphabetic:][:Mark:][:Nd:]]");

	final PrintWriter out = GenerateConfusables.openAndWriteHeader(GenerateConfusables.reformatedInternal, "idnchars.txt", "Recommended Identifier Profiles for IDN");

	out.println("# Allowed as output characters");
	out.println("");
	bf.setValueSource("output");
	bf.showSetNames(out, remainingOutputSet);
	showExtras(bf, remainingOutputSet, letters);

	/*
		out.println("");

		out.println("");
		out.println("# Input Characters");
		out.println("");
		bf.setValueSource("input");
		bf.showSetNames(out, inputSet_strict);
		showExtras(bf, inputSet_strict, letters);

		out.println("");
		out.println("# Input Characters (lenient)");
		out.println("");
		bf.setValueSource("input-lenient");
		bf.showSetNames(out, inputSet_lenient);
		showExtras(bf, inputSet_lenient, letters);
	 */

	out.println("");
	out.println("# Not allowed at start of identifier");
	out.println("");
	bf.setValueSource("nonstarting");
	bf.showSetNames(out, nonstarting);

	//out.println("");

	//showRemapped(out, "Characters remapped on input in GUIs -- Not required by profile!", remap);

	out.close();
    }


    /**
     * 
     */
    private void showExtras(BagFormatter bf, UnicodeSet source, UnicodeSet letters) {
	final UnicodeSet extra = new UnicodeSet(source).removeAll(letters);
	if (extra.size() != 0) {
	    final UnicodeSet fixed = new UnicodeSet();
	    for (final UnicodeSetIterator it = new UnicodeSetIterator(extra); it.next();) {
		if (!letters.containsAll(GenerateConfusables.NFKD.normalize(it.getString()))) {
		    fixed.add(it.codepoint);
		}
	    }
	    if (DEBUG) System.out.println(bf.showSetNames(fixed));
	}
    }

    private void printModificationsInternal() throws IOException {
	final BagFormatter bf = GenerateConfusables.makeFormatter();
	bf.setLabelSource(null);
	bf.setShowLiteral(GenerateConfusables.EXCAPE_FUNNY);
	bf.setMergeRanges(true);

	PrintWriter out;
	//        PrintWriter out = GenerateConfusables.openAndWriteHeader(GenerateConfusables.DRAFT_OUT, "xidmodifications.txt", "Security Profile for General Identifiers");
	//        /* PrintWriter out = FileUtilities.openUTF8Writer(outdir, "xidmodifications.txt");
	//
	//		out.println("# Security Profile for General Identifiers");
	//		out.println("# $Revision: 1.32 $");
	//		out.println("# $Date: 2010-06-19 00:29:21 $");
	//         */
	//
	//        //String skipping = "[^[:gc=cn:][:gc=co:][:gc=cs:][:gc=cc:]-[:whitespace:]]";
	//        //UnicodeSet skippingSet = new UnicodeSet(skipping);
	//
	//        out.println("#  All code points not explicitly listed ");
	//        out.println("#  have the values: Restricted; Not-Characters");
	//        out.println("# @missing: 0000..10FFFF; Restricted ; Not-Characters");
	//        out.println("");
	//        /*
	//         * for (Iterator it = values.iterator(); it.hasNext();) { String
	//         * reason1 = (String)it.next(); bf.setValueSource(reason1);
	//         * out.println(""); bf.showSetNames(out, removals.getSet(reason1)); }
	//         */
	//        bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
	//        })
	//        .set(recastRemovals)
	//        .setMain("Removals", "GCB", UnicodeProperty.ENUMERATED, "1.0"));
	//
	//        final Set<String> fullListing = new HashSet<String>(Arrays.asList("technical limited-use historic discouraged obsolete".split("\\s+")));
	//        //        final Set<String> sortedValues = new TreeSet<String>(GenerateConfusables.UCAComparator);
	//        //        sortedValues.addAll(recastRemovals.values());
	//        //        if (GenerateConfusables.DEBUG) System.out.println("Restriction Values: " + sortedValues);
	//        for (Identifier_Type value : Identifier_Type.values()) {
	//            if (value == Identifier_Type.not_characters) {
	//                continue;
	//            }
	//            final UnicodeSet uset = recastRemovals.getSet(value.propertyFileFormat());
	//            if (uset == null) {
	//                throw new IllegalArgumentException("internal error");
	//            }
	//            out.println("");
	//            out.println("#\tStatus/Type:\t" + value.name);
	//            out.println("");
	//            //bf.setMergeRanges(Collections.disjoint(fullListing, Arrays.asList(value.split("[\\s;]+"))));
	//            //bf.setMergeRanges(value.propertyFileFormat());
	//            bf.showSetNames(out, uset);
	//        }
	//
	//        //      out.println("");
	//        //      out.println("# Characters added");
	//        //      out.println("");
	//        //      bf.setMergeRanges(false);
	//        //      bf.setValueSource("addition");
	//        //      bf.showSetNames(out, additions.keySet());
	//
	//        //showRemapped(out, "Characters remapped on input", remap);
	//
	//        out.close();

	out = GenerateConfusables.openAndWriteHeader(GenerateConfusables.reformatedInternal, "xidAllowed.txt", "Security Profile for General Identifiers");
	final UnicodeSet allowed = new UnicodeSet(xidPlus).removeAll(removals.keySet());
	final UnicodeSet cfAllowed = new UnicodeSet().addAll(allowed).retainAll(isCaseFolded).retainAll(propNFKCSet);
	allowed.removeAll(cfAllowed);
	bf.setValueSource("case_folded");
	out.println("# XID characters allowed (no uppercase)");
	out.println("");
	bf.showSetNames(out, cfAllowed);
	bf.setValueSource("not_case_folded");
	out.println("");
	out.println("# XID characters allowed (uppercase)");
	out.println("");
	bf.showSetNames(out, allowed);
	out.close();

	final UnicodeMap<String> someRemovals = new UnicodeMap<>();
	final UnicodeMap.Composer<String> myComposer = new UnicodeMap.Composer<String>() {
	    @Override
	    public String compose(int codePoint, String string, String a, String b) {
		if (b == null) {
		    return null;
		}
		String x = (String)b;
		//		if (ALT) {
		//		    if (!GenerateConfusables.IDNOutputSet.contains(codePoint)) {
		//			return "~IDNA";
		//		    }
		//		    if (!xidPlus.contains(codePoint)) {
		//			return "~Unicode Identifier";
		//		    }
		//		}
		if (x.startsWith(Identifier_Status.restricted.toString())) {
		    x = x.substring(Identifier_Status.restricted.toString().length());
		}
		//if (!propNFKCSet.contains(codePoint)) x += "*";
		if (GenerateConfusables.GC_LOWERCASE.contains(codePoint)) {
		    final String upper = DEFAULT_UCD.getCase(codePoint, UCD_Types.FULL, UCD_Types.UPPER);
		    if (upper.equals(UTF16.valueOf(codePoint))
			    && x.equals("technical symbol (phonetic)")) {
			x = "technical symbol (phonetic with no uppercase)";
		    }
		}
		return x;
	    }
	};
	someRemovals.composeWith(recastRemovals, myComposer);
	final UnicodeSet nonIDNA = new UnicodeSet(GenerateConfusables.IDNOutputSet).addAll(GenerateConfusables.IDNInputSet).complement();
	someRemovals.putAll(nonIDNA, "~IDNA");
	someRemovals.putAll(new UnicodeSet(xidPlus).complement(), "~Unicode Identifier");
	someRemovals.putAll(GenerateConfusables.UNASSIGNED, null); // clear extras
	//someRemovals = removals;
	out = FileUtilities.openUTF8Writer(GenerateConfusables.reformatedInternal, "draft-restrictions.txt");
	out.println("# Characters restricted in domain names");
	out.println("# $Revision: 1.32 $");
	out.println("# $Date: 2010-06-19 00:29:21 $");
	out.println("#");
	out.println("# This file contains a draft list of characters for use in");
	out.println("#     UTR #36: Unicode Security Considerations");
	out.println("#     http://unicode.org/draft/reports/tr36/tr36.html");
	out.println("# According to the recommendations in that document, these characters");
	out.println("# would be restricted in domain names: people would only be able to use them");
	out.println("# by using lenient security settings.");
	out.println("#");
	out.println("# If you have any feedback on this list, please use the submission form at:");
	out.println("#     http://unicode.org/reporting.html.");
	out.println("#");
	out.println("# Notes:");
	out.println("# - Characters are listed along with a reason for their removal.");
	out.println("# - Characters listed as ~IDNA are excluded at this point in domain names,");
	out.println("#   in many cases because the international domain name specification does not contain");
	out.println("#   characters beyond Unicode 3.2. At this point in time, feedback on those characters");
	out.println("#   is not relevant.");
	out.println("# - Characters listed as ~Unicode Identifiers are restricted because they");
	out.println("#   do not fit the specification of identifiers given in");
	out.println("#      UAX #31: Identifier and Pattern Syntax");
	out.println("#      http://unicode.org/reports/tr31/");
	out.println("# - Characters listed as ~IICore are restricted because they are Ideographic,");
	out.println("#   but not part of the IICore set defined by the IRG as the minimal set");
	out.println("#   of required ideographs for East Asian use.");
	out.println("# - The files in this directory are 'live', and may change at any time.");
	out.println("#   Please include the above Revision number in your feedback.");

	bf.setRangeBreakSource(new GenerateConfusables.FakeBreak2());
	if (MAIN_CODE) {
	    final Set<String> values = new TreeSet<>(someRemovals.getAvailableValues());
	    for (final Iterator<String> it = values.iterator(); it.hasNext();) {
		final String reason1 = (String) it.next();
		bf.setValueSource(reason1);
		final UnicodeSet keySet = someRemovals.keySet(reason1);
		if (reason1.contains("recommended")) {
		    if (DEBUG) System.out.println("Recommended: " + keySet.toPattern(false));
		    UnicodeSet current = GenerateConfusables.AGE.getSet(GenerateConfusables.VERSION_PROP_VALUE);
		    if (DEBUG) System.out.println("Current: " + current.toPattern(false));
		    UnicodeSet newRecommended = new UnicodeSet(keySet).retainAll(current);
		    for (String s : newRecommended) {
			// [:script=Phag:] ; historic # UAX31 T4 #     Phags Pa
			if (DEBUG) System.out.println(Utility.hex(s) 
				+ "\t;\thistoric\t#\t" 
				+ DEFAULT_UCD.getName(s));
		    }
		}
		out.println("");
		bf.showSetNames(out, keySet);
	    }
	} else {
	    bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
	    }).set(someRemovals).setMain("Removals", "GCB",
		    UnicodeProperty.ENUMERATED, "1.0"));
	    bf.showSetNames(out, someRemovals.keySet());
	}
	out.close();
    }


    private void printIdentifierTypes(Style status) throws IOException {
	final UnicodeMap<String> tempMap = new UnicodeMap<String>();
	final Map<String,Set<Identifier_Type>> sortingMap = new HashMap<>();
	for (Set<Identifier_Type> value : identifierTypesMap.values()) {
	    if (value.contains(Identifier_Type.not_characters)) {
		continue;
	    }
	    UnicodeSet set = identifierTypesMap.getSet(value);
	    String valueString = CollectionUtilities.join(value, " ");
	    sortingMap.put(valueString, value);
	    tempMap.putAll(set, valueString);
	}
	final Comparator<String> tempComp = new Comparator<String>() {
	    @Override
	    public int compare(String o1, String o2) {
		Set<Identifier_Type> set0 = sortingMap.get(o1);
		Set<Identifier_Type> set1 = sortingMap.get(o2);
		return CollectionUtilities.compare(set0.iterator(), set1.iterator());
	    }

	};
	final BagFormatter bf2 = GenerateConfusables.makeFormatter();
	bf2.setMergeRanges(true);
	final ToolUnicodePropertySource properties = ToolUnicodePropertySource.make(Default.ucdVersion());
	final UnicodeProperty age = properties.getProperty("age");
	bf2.setLabelSource(age);

	final String propName = "Identifier_Type";
	final String filename = status == Style.byValue ? "IdentifierType.txt" : "IdentifierTypeFlat.txt";
	try (PrintWriter out2 = GenerateConfusables.openAndWriteHeader(GenerateConfusables.DRAFT_OUT, 
		filename, "Security Profile for General Identifiers: "
			+ propName)) {
	    out2.println("# Format"
		    + "\n#"
		    + "\n# Field 0: code point"
		    + "\n# Field 1: set of Identifier_Type values (see Table 1 of http://www.unicode.org/reports/tr39)"
		    + "\n#"
		    + "\n# Any missing code points have the " + propName + " value Not_Character");

            out2.println("#\n"
                    + "# For the purpose of regular expressions, the property " + propName + " is defined as\n"
                    + "# mapping each code point to a set of enumerated values.\n"
                    + "# The short name of " + propName + " is the same as the long name.\n"
                    + "# The possible values are:\n"
                    + "#   Not_Character, Deprecated, Default_Ignorable, Not_NFKC, Not_XID,\n"
                    + "#   Exclusion, Obsolete, Technical, Uncommon_Use, Limited_Use, Inclusion, Recommended\n"
                    + "# The short name of each value is the same as its long name.\n"
                    + "# The default property value for all Unicode code points U+0000..U+10FFFF\n"
                    + "# not mentioned in this data file is Not_Character.\n"
                    + "# As usual, sets are unordered, with no duplicate values.\n");

	    bf2.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
	    }).set(tempMap).setMain(propName, "IDT",
		    UnicodeProperty.EXTENDED_MISC, "1.0"));
	    if (status == Style.byValue) {
		TreeSet<String> sorted = new TreeSet<>(tempComp);
		sorted.addAll(tempMap.values());

		for (String value : sorted) {
		    out2.println("");
		    out2.println("#\t"
			    + propName
			    + ":\t" + value);
		    out2.println("");
		    bf2.showSetNames(out2, tempMap.getSet(value));                
		}
	    } else {
		out2.println("");
		bf2.showSetNames(out2, tempMap.keySet());                
	    }
	}
    }

    private void printIdentifierStatus() throws IOException {
	final UnicodeMap<String> tempMap = new UnicodeMap<String>();
	tempMap.putAll(0, 0x10FFFF, Identifier_Status.allowed.toString());
	for (Set<Identifier_Type> value : identifierTypesMap.values()) {
	    if (!value.contains(Identifier_Type.recommended) && !value.contains(Identifier_Type.inclusion)) {
		UnicodeSet set = identifierTypesMap.getSet(value);
		tempMap.putAll(set, Identifier_Status.restricted.toString());
	    }
	}

	final BagFormatter bf2 = GenerateConfusables.makeFormatter();
	bf2.setMergeRanges(true);
	final ToolUnicodePropertySource properties = ToolUnicodePropertySource.make(Default.ucdVersion());
	final UnicodeProperty age = properties.getProperty("age");
	bf2.setLabelSource(age);

	final String propName = "Identifier_Status";
	try (PrintWriter out2 = GenerateConfusables.openAndWriteHeader(GenerateConfusables.DRAFT_OUT, 
		"IdentifierStatus.txt", "Security Profile for General Identifiers: " + propName)) {
	    out2.println("# Format"
		    + "\n#"
		    + "\n# Field 0: code point"
		    + "\n# Field 1: Identifier_Status value (see Table 1 of http://www.unicode.org/reports/tr39)"
		    + "\n#"
		    + "\n# Any missing code points have the " + propName + " value Restricted");

	    out2.println("#\n"
	            + "# For the purpose of regular expressions, the property " + propName + " is defined as\n"
	            + "# an enumerated property of code points.\n"
	            + "# The short name of " + propName + " is the same as the long name.\n"
                    + "# The possible values are:\n"
                    + "#   Allowed, Restricted\n"
                    + "# The short name of each value is the same as its long name.\n"
                    + "# The default property value for all Unicode code points U+0000..U+10FFFF\n"
                    + "# not mentioned in this data file is Restricted.\n");

	    bf2.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
	    }).set(tempMap).setMain(propName, "IDS",
		    UnicodeProperty.EXTENDED_MISC, "1.0"));

	    for (Identifier_Status value : Identifier_Status.values()) {
		if (value == Identifier_Status.restricted) {
		    continue;
		}
		out2.println("");
		out2.println("#\t" + propName + ":\t" + value);
		out2.println("");
		bf2.showSetNames(out2, tempMap.getSet(value.toString()));                
	    }
	}
    }

}