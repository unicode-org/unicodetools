package org.unicode.text.UCD;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.IdUsage;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.text.UCD.GenerateConfusables.FakeBreak;
import org.unicode.text.UCD.GenerateConfusables.FakeBreak2;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

class IdentifierInfo {

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
    
    private static Integer MARK_NOT_NFC = new Integer(50);
    private static Integer MARK_NFC = new Integer(40);
    private static Integer MARK_INPUT_LENIENT = new Integer(30);
    private static Integer MARK_INPUT_STRICT = new Integer(20);
    private static Integer MARK_OUTPUT = new Integer(10);
    private static Integer MARK_ASCII = new Integer(10);


    //        private final boolean mergeRanges = true;

    private UnicodeSet removalSet;
    UnicodeSet remainingOutputSet;
    UnicodeSet inputSet_strict;
    private UnicodeSet inputSet_lenient;
    private UnicodeSet nonstarting;
    UnicodeSet propNFKCSet;
    //UnicodeSet notInXID;
    UnicodeSet xidPlus;

    private final UnicodeMap<String> additions = new UnicodeMap();
    private final UnicodeMap<String> remap = new UnicodeMap();
    private final UnicodeMap<IdentifierInfo.Reason> removals = new UnicodeMap<IdentifierInfo.Reason>();
    private final UnicodeMap<String> recastRemovals = new UnicodeMap<String>();

    private UnicodeMap reviews, removals2;
    UnicodeMap lowerIsBetter;

    private UnicodeSet isCaseFolded;

    private IdentifierInfo() throws IOException {
        isCaseFolded = new UnicodeSet();
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            final int cat = GenerateConfusables.DEFAULT_UCD.getCategory(cp);
            if (cat == UCD_Types.Cn || cat == UCD_Types.Co || cat == UCD_Types.Cs) {
                continue;
            }
            final String source = UTF16.valueOf(cp);
            final String cf = GenerateConfusables.DEFAULT_UCD.getCase(source, UCD_Types.FULL, UCD_Types.FOLD);
            if (cf.equals(source)) {
                isCaseFolded.add(cp);
            }
        }

        propNFKCSet = GenerateConfusables.ups.getSet("NFKC_QuickCheck=N").complement();
        final UnicodeSet propXIDContinueSet = GenerateConfusables.ups.getSet("XID_Continue=Yes");

        //removals.putAll(propNFKCSet.complement(), PROHIBITED + "compat variant");
        loadFileData();
        xidPlus = new UnicodeSet(propXIDContinueSet).addAll(additions.keySet()).retainAll(propNFKCSet);

        GenerateConfusables.getIdentifierSet();
        //notInXID = new UnicodeSet(IDNOutputSet).removeAll(xidPlus);
        //removals.putAll(notInXID, PROHIBITED + NOT_IN_XID);
        //UnicodeSet notNfkcXid = new UnicodeSet(xidPlus).removeAll(removals.keySet()).removeAll(propNFKCSet);
        //removals.putAll(notNfkcXid, PROHIBITED + "compat variant");
        removalSet = new UnicodeSet();
        for (final IdentifierInfo.Reason value : removals.values()) {
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
            final String cf = GenerateConfusables.DEFAULT_UCD.getCase(nss, UCD_Types.FULL, UCD_Types.FOLD);
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
            final String cf = GenerateConfusables.DEFAULT_UCD.getCase(ss, UCD_Types.FULL, UCD_Types.FOLD);
            if (usi.codepoint == 0x2126 || usi.codepoint == 0x212B) {
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
        reviews = new UnicodeMap();
        //reviews.putAll(removals);
        for (final IdentifierInfo.Reason value : removals.values()) {
            reviews.putAll(removals.getSet(value), value.propertyFileFormat());
        }
        reviews.putAll(remainingOutputSet, "output");
        reviews.putAll(inputSet_strict, "input");
        reviews.putAll(inputSet_lenient, "input-lenient");
        reviews.putAll(specialRemove, GenerateConfusables.PROHIBITED + "output-disallowed");

        lowerIsBetter = new UnicodeMap();

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

        final UnicodeMap nonstartingmap = new UnicodeMap().putAll(nonstarting,
                "nonstarting");
        final UnicodeMap.Composer composer = new UnicodeMap.Composer() {
            @Override
            public Object compose(int codepoint, String string, Object a, Object b) {
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
        final UnicodeMap.Composer composer2 = new UnicodeMap.Composer() {
            @Override
            public Object compose(int codepoint, String string, Object a, Object b) {
                if (b == null) {
                    return a;
                }
                return "remap-to-" + Utility.hex(b.toString());
            }
        };
        //reviews.composeWith(remap, composer2);
        removals2 = new UnicodeMap().putAll(recastRemovals);
        removals2.putAll(GenerateConfusables.ups.getSet("XID_Continue=Yes").complement(),
                GenerateConfusables.PROHIBITED + GenerateConfusables.NOT_IN_XID);
        removals2.setMissing("future?");

        additions.freeze();
        remap.freeze();

        for (final IdentifierInfo.Reason value : removals.values()) {
            recastRemovals.putAll(removals.getSet(value), value.propertyFileFormat());
        }
        recastRemovals.freeze();
        removals.freeze();
        reviews.freeze();
        removals2.freeze();
    }

    enum Reason {
        recommended("Recommended"),
        inclusion("Inclusion"),
        aspirational("Aspirational"),
        limited_use("Limited_Use"),
        uncommon_use("Uncommon_Use"),
        technical("Technical"),
        obsolete("Obsolete"),
        exclusion("Exclusion"),
        not_xid("Not_XID"),
        not_nfkc("Not_NFKC"),
        default_ignorable("Default_Ignorable"),
        deprecated("Deprecated"),
        not_chars("Not_Characters"),
        ;
        final String name;
        private Reason(String name) {
            this.name = name;
        }

        private static IdentifierInfo.Reason fromString(String string) {
            String rawReason = string.trim().replace("-","_").toLowerCase(Locale.ENGLISH);
            if (rawReason.equals("allowed")) {
                return Reason.recommended;
                //rawReason = GenerateConfusables.recommended_scripts;
            } else if (rawReason.equals("historic")) {
                return Reason.obsolete;
            } else if (rawReason.equals("limited_use")) {
                return Reason.uncommon_use;
            }
            return valueOf(rawReason);
        }
        public boolean isRestricted() {
            return this != Reason.inclusion && this != Reason.recommended;
        }
        @Override
        public String toString() {
            return name().replace("_","-");
        }
        public String propertyFileFormat() {
            return (isRestricted() ? GenerateConfusables.PROHIBITED : GenerateConfusables.UNPROHIBITED) + name;
        }
        public boolean replaceBy(IdentifierInfo.Reason possibleReplacement) {
            return compareTo(possibleReplacement) < 0
//                    || this == historic && possibleReplacement == limited_use
                    ; // && this != historic;
        }
    }
    /**
     * 
     */
    private void loadFileData() throws IOException {
        BufferedReader br;
        String line;
        // get all the removals.
        br = BagFormatter.openUTF8Reader(GenerateConfusables.indir, "removals.txt");
        removals.putAll(new UnicodeSet("[^[:gc=cn:][:gc=co:][:gc=cs:][:gc=cc:]-[:whitespace:]]"),
                Reason.recommended);

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
            if (true) {
                System.out.println(line);
            }
            try {
                sources.clear();
                final String[] pieces = Utility.split(line, ';');
                if (pieces.length < 2) {
                    throw new IllegalArgumentException(counter + " Missing line " + line);
                }
                final String codelist = pieces[0].trim();
                final IdentifierInfo.Reason reasons = Reason.fromString(pieces[1]);
                if (pieces[0].startsWith("[")) {
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
                removals.putAll(sources, reasons);
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
        for (final String script : ScriptMetadata.getScripts()) {
            if (ScriptMetadata.getInfo(script).idUsage == IdUsage.RECOMMENDED) {
                final UnicodeSet us = GenerateConfusables.IDENTIFIER_INFO.getSetWith(script);
                if (us != null) {
                    hasRecommendedScript.addAll(us);
                }
            }
        }
        hasRecommendedScript.freeze();

        for (final String script : ScriptMetadata.getScripts()) {
            final Info scriptInfo = ScriptMetadata.getInfo(script);
            final IdUsage idUsage = scriptInfo.idUsage;
            IdentifierInfo.Reason status;
            switch(idUsage) {
            case ASPIRATIONAL:
                status = Reason.aspirational;
                break;
            case LIMITED_USE:
                status = Reason.limited_use;
                break;
            case EXCLUSION:
                status = Reason.exclusion;
                break;
            case RECOMMENDED:
            default:
                status = null;
                break; // do nothing;
            }
            if (status != null) {
                final UnicodeSet us = GenerateConfusables.IDENTIFIER_INFO.getSetWith(script);
                //final UnicodeSet us = new UnicodeSet().applyPropertyAlias("script", script);
                for (final String s : us) {
                    if (hasRecommendedScript.contains(s)) {
                        continue; // skip those that have at least one recommended script
                    }
                    final IdentifierInfo.Reason old = removals.get(s);
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
            System.out.println("*Removal Collision\t" + value + "\n\t" + removalCollision.getSet(value).toPattern(false));
        }
        removals.freeze();
        //removals.putAll(getNonIICore(), PROHIBITED + "~IICore");
        br.close();

        //      // get the word chars
        //      br = BagFormatter.openUTF8Reader(indir,
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
        //              System.out.println("Already in XID continue: "
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

    void printIDNStuff() throws IOException {
        final PrintWriter out;
        printIDModifications();
        writeIDChars();
        writeIDReview();
        GenerateConfusables.generateDecompFile();
    }

    /**
     * 
     */
    private void writeIDReview() throws IOException {
        final BagFormatter bf = GenerateConfusables.makeFormatter()
                .setUnicodePropertyFactory(GenerateConfusables.ups)
                .setLabelSource(null)
                .setShowLiteral(GenerateConfusables.EXCAPE_FUNNY)
                .setMergeRanges(true);

        final PrintWriter out = GenerateConfusables.openAndWriteHeader(GenerateConfusables.reformatedInternal, "review.txt", "Review List for IDN");
        //			PrintWriter out = BagFormatter.openUTF8Writer(outdir, "review.txt");
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
    private void writeIDChars() throws IOException {
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
            System.out.println(bf.showSetNames(fixed));
        }
    }

    /**
     * 
     */
    private void printIDModifications() throws IOException {
        final BagFormatter bf = GenerateConfusables.makeFormatter();
        bf.setLabelSource(null);
        bf.setShowLiteral(GenerateConfusables.EXCAPE_FUNNY);
        bf.setMergeRanges(true);

        PrintWriter out = GenerateConfusables.openAndWriteHeader(GenerateConfusables.DRAFT_OUT, "xidmodifications.txt", "Security Profile for General Identifiers");
        /* PrintWriter out = BagFormatter.openUTF8Writer(outdir, "xidmodifications.txt");

		out.println("# Security Profile for General Identifiers");
		out.println("# $Revision: 1.32 $");
		out.println("# $Date: 2010-06-19 00:29:21 $");
         */

        //String skipping = "[^[:gc=cn:][:gc=co:][:gc=cs:][:gc=cc:]-[:whitespace:]]";
        //UnicodeSet skippingSet = new UnicodeSet(skipping);

        out.println("#  All code points not explicitly listed ");
        out.println("#  have the values: Restricted; Not-Characters");
        out.println("# @missing: 0000..10FFFF; Restricted ; Not-Characters");
        out.println("");
        /*
         * for (Iterator it = values.iterator(); it.hasNext();) { String
         * reason1 = (String)it.next(); bf.setValueSource(reason1);
         * out.println(""); bf.showSetNames(out, removals.getSet(reason1)); }
         */
        bf.setValueSource((new UnicodeProperty.UnicodeMapProperty() {
        })
        .set(recastRemovals)
        .setMain("Removals", "GCB", UnicodeProperty.ENUMERATED, "1.0"));

        final Set<String> fullListing = new HashSet<String>(Arrays.asList("technical limited-use historic discouraged obsolete".split("\\s+")));
//        final Set<String> sortedValues = new TreeSet<String>(GenerateConfusables.UCAComparator);
//        sortedValues.addAll(recastRemovals.values());
//        System.out.println("Restriction Values: " + sortedValues);
        for (Reason value : Reason.values()) {
            if (value == Reason.not_chars) {
                continue;
            }
            final UnicodeSet uset = recastRemovals.getSet(value.propertyFileFormat());
            if (uset == null) {
                throw new IllegalArgumentException("internal error");
            }
            out.println("");
            out.println("#\tStatus/Type:\t" + value.name);
            out.println("");
            //bf.setMergeRanges(Collections.disjoint(fullListing, Arrays.asList(value.split("[\\s;]+"))));
            //bf.setMergeRanges(value.propertyFileFormat());
            bf.showSetNames(out, uset);
        }

        //      out.println("");
        //      out.println("# Characters added");
        //      out.println("");
        //      bf.setMergeRanges(false);
        //      bf.setValueSource("addition");
        //      bf.showSetNames(out, additions.keySet());

        //showRemapped(out, "Characters remapped on input", remap);

        out.close();

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

        final UnicodeMap someRemovals = new UnicodeMap();
        final UnicodeMap.Composer myComposer = new UnicodeMap.Composer() {
            @Override
            public Object compose(int codePoint, String string, Object a, Object b) {
                if (b == null) {
                    return null;
                }
                String x = (String)b;
                if (false) {
                    if (!GenerateConfusables.IDNOutputSet.contains(codePoint)) {
                        return "~IDNA";
                    }
                    if (!xidPlus.contains(codePoint)) {
                        return "~Unicode Identifier";
                    }
                }
                if (x.startsWith(GenerateConfusables.PROHIBITED)) {
                    x = x.substring(GenerateConfusables.PROHIBITED.length());
                }
                //if (!propNFKCSet.contains(codePoint)) x += "*";
                if (GenerateConfusables.GC_LOWERCASE.contains(codePoint)) {
                    final String upper = GenerateConfusables.DEFAULT_UCD.getCase(codePoint, UCD_Types.FULL, UCD_Types.UPPER);
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
        out = BagFormatter.openUTF8Writer(GenerateConfusables.reformatedInternal, "draft-restrictions.txt");
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

        bf.setRangeBreakSource(new FakeBreak2());
        if (true) {
            final Set values = new TreeSet(someRemovals.getAvailableValues());
            for (final Iterator it = values.iterator(); it.hasNext();) {
                final String reason1 = (String) it.next();
                bf.setValueSource(reason1);
                final UnicodeSet keySet = someRemovals.keySet(reason1);
                if (reason1.contains("recommended")) {
                    System.out.println("Recommended: " + keySet.toPattern(false));
                    UnicodeSet current = GenerateConfusables.AGE.getSet(GenerateConfusables.VERSION_PROP_VALUE);
                    System.out.println("Current: " + current.toPattern(false));
                    UnicodeSet newRecommended = new UnicodeSet(keySet).retainAll(current);
                    for (String s : newRecommended) {
                        // [:script=Phag:] ; historic # UAX31 T4 #     Phags Pa
                        System.out.println(Utility.hex(s) 
                                + "\t;\thistoric\t#\t" 
                                + GenerateConfusables.DEFAULT_UCD.getName(s));
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
}