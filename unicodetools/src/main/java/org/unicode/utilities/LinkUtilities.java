package org.unicode.utilities;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.ibm.icu.impl.IDNA2003;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.text.UnicodeSet.XSymbolTable;
import com.ibm.icu.util.VersionInfo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdPropertyValues.Idn_Status_Values;
import org.unicode.props.UnicodeProperty;
import org.unicode.props.UnicodeProperty.UnicodeMapProperty;
import org.unicode.text.UCD.VersionedSymbolTable;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.ibm.icu.impl.IDNA2003;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.text.SymbolTable;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.util.VersionInfo;

public class LinkUtilities {
    private static final boolean SHOW_NON_ASCII_TLDS = true;

    // allow changing UnicodeSet to use the current IndexUnicodeProperties
    public static final IndexUnicodeProperties IUP = IndexUnicodeProperties.make();

    public static final boolean DEBUG = false;
    public static final boolean USE_CLDR = false;

    public static final String RESOURCE_DIR =
            Settings.UnicodeTools.UNICODETOOLS_REPO_DIR
                    + "/unicodetools/src/main/resources/org/unicode/tools/";

    public static final String DATA_DIR =
            Settings.UnicodeTools.UNICODETOOLS_REPO_DIR + "/unicodetools/data/links";

    public static final Splitter SPLIT_COMMA = Splitter.on(',');
    public static final Splitter SPLIT_TAB = Splitter.on('\t');

    public static final Joiner JOIN_TAB = Joiner.on('\t');
    public static final Joiner JOIN_LF = Joiner.on('\n');
    public static final Joiner JOIN_EMPTY = Joiner.on("");

    public static final CLDRFile ENGLISH = USE_CLDR ? CLDRConfig.getInstance().getEnglish() : null;

    private static final UnicodeSet SOFAR = new UnicodeSet();

    /** Defines the LinkTermination property */
    public enum LinkTermination {
        Hard("[\\p{whitespace}\\p{NChar}[\\p{C}-\\p{Cf}]\\p{deprecated}]"),
        Soft("[\\p{Term}\\p{lb=qu}-\\p{deprecated}]"),
        Close("[\\p{Bidi_Paired_Bracket_Type=Close}[>]-\\p{deprecated}]"),
        Open("[\\p{Bidi_Paired_Bracket_Type=Open}[<]-\\p{deprecated}]"),
        Include(null), // all else
        ;

        public final UnicodeSet base;

        public UnicodeSet getBase() {
            return base;
        }

        public static final Set<LinkTermination> NON_MISSING =
                ImmutableSet.copyOf(
                        Sets.difference(EnumSet.allOf(LinkTermination.class), Set.of(Hard)));

        private LinkTermination(String uset) {
            if (uset == null) { // only called with Include, the "none of the above" option
                this.base = SOFAR.complement().freeze();
            } else {
                java.text.ParsePosition parsePosition = new java.text.ParsePosition(0);
				this.base = new UnicodeSet(uset, parsePosition, VersionedSymbolTable.frozenAt(VersionInfo.UNICODE_17_0)).freeze();
                SOFAR.addAll(this.base);
            }
        }

        public static final UnicodeMap<LinkTermination> PROPERTY_MAP = new UnicodeMap<>();
        public static final UnicodeProperty PROPERTY;

        static {
            // Verify consistency
            for (LinkTermination pv1 : values()) {
                for (LinkTermination pv2 : values()) {
                    if (pv1.compareTo(pv2) <= 0) {
                        continue;
                    }
                    if (pv1.base.containsSome(pv2.base)) {
                        throw new IllegalArgumentException(
                                "Values in LinkTermination overlap! "
                                        + pv1
                                        + ", "
                                        + pv2
                                        + ": "
                                        + new UnicodeSet(pv1.base).retainAll(pv2.base));
                    }
                }
            }
            for (LinkTermination lt : values()) {
                PROPERTY_MAP.putAll(lt.base, lt);
            }
            PROPERTY_MAP.freeze();
            UnicodeMap<String> temp = new UnicodeMap<>();
            for (UnicodeMap.EntryRange<LinkTermination> entry : PROPERTY_MAP.entryRanges()) {
                temp.putAll(entry.codepoint, entry.codepointEnd, entry.value.toString());
            }
            PROPERTY =
                    new UnicodeMapProperty()
                            .set(temp)
                            .setMain(
                                    LinkTermination.class.getSimpleName(),
                                    "LinkTerm",
                                    UnicodeProperty.ENUMERATED,
                                    IUP.getUcdVersion().getVersionString(2, 2));
        }
    }

    private static String getGeneralCategory(int property, int codePoint, int nameChoice) {
        return UCharacter.getPropertyValueName(
                property, UCharacter.getIntPropertyValue(codePoint, property), nameChoice);
    }

    private static String quote(String s) {
        return TransliteratorUtilities.toHTML.transform(s);
    }

    private static int getOpening(int cp) {
        return cp == '>' ? '<' : UCharacter.getBidiPairedBracket(cp);
    }

    private static UnicodeProperty LINK_PAIRED_OPENER;

    public static UnicodeProperty getLinkPairedOpener() {
        if (LINK_PAIRED_OPENER == null) {
            UnicodeMap<String> temp = new UnicodeMap<>();
            for (int cp : LinkTermination.Close.base.codePoints()) {
                temp.put(cp, Utility.hex(getOpening(cp), 4));
            }

            LINK_PAIRED_OPENER =
                    new UnicodeMapProperty()
                            .set(temp)
                            .setMain(
                                    "LinkPairedOpener",
                                    "LinkPO",
                                    UnicodeProperty.STRING,
                                    IUP.getUcdVersion().getVersionString(2, 2));
        }
        return LINK_PAIRED_OPENER;
    }

    /** Parallels the spec parts table */
    public enum Part {
        PROTOCOL('\u0000', "[{//}]", "[]", "[]"),
        HOST('\u0000', "[/?#]", "[]", "[]"),
        PATH('/', "[?#]", "[/]", "[]"),
        QUERY('?', "[#]", "[=\\&]", "[+]"),
        FRAGMENT('#', "[]", "[]", "[]");
        final int initiator;
        final UnicodeSet terminators;
        final UnicodeSet clearStack;
        final UnicodeSet extraQuoted;

        private Part(char initiator, String terminators, String clearStack, String extraQuoted) {
            this.initiator = initiator;
            this.terminators = new UnicodeSet(terminators).freeze();
            this.clearStack = new UnicodeSet(clearStack).freeze();
            this.extraQuoted =
                    new UnicodeSet(extraQuoted)
                            .addAll(this.clearStack)
                            .addAll(this.terminators)
                            .freeze();
        }

        static Part fromInitiator(int cp) {
            for (Part part : Part.values()) {
                if (part.initiator == cp) {
                    return part;
                }
            }
            return null;
        }

        /**
         * Pull apart a URL string into Parts. <br>
         * TODO: unescape the %escapes.
         *
         * @param source
         * @param unescape TODO
         * @return
         */
        public static NavigableMap<Part, String> getParts(String source, boolean unescape) {
            Map<Part, String> result = new HashMap<>();
            // quick and dirty
            int partStart = 0;
            int partEnd;
            main:
            for (Part part : Part.values()) {
                switch (part) {
                    case PROTOCOL:
                        partEnd = source.indexOf("://"); // TODO fix for mailto
                        if (partEnd > 0) {
                            partEnd += 3;
                            result.put(Part.PROTOCOL, source.substring(0, partEnd));
                            partStart = partEnd;
                        }
                        break;
                    default:
                        partEnd =
                                part.terminators.span(
                                        source, partStart, SpanCondition.NOT_CONTAINED);
                        if (partStart != partEnd) {
                            result.put(part, part.unescape(source.substring(partStart, partEnd)));
                        }
                        if (partEnd == source.length()) {
                            break main;
                        }
                        partStart = partEnd;
                        break;
                }
            }
            return ImmutableSortedMap.copyOf(result);
        }

        /**
         * Unescape a part. But don't unescape interior characters or terminators because they are
         * content! For example "a/b%2Fc" as a path should not be turned into a/b/c, because that
         * b/c is a path-part.
         *
         * @param substring
         * @return
         */
        public String unescape(String substring) {
            return LinkUtilities.unescape(substring, extraQuoted);
        }
    }

    private static final UnicodeSet idnMapped =
            IUP.getSet("Idn_Status=" + Idn_Status_Values.mapped);
    private static final UnicodeSet idnValid = IUP.getSet("Idn_Status=" + Idn_Status_Values.valid);
    static final Pattern protocol = Pattern.compile("(https?://|mailto:)");
    public static final UnicodeSet validHost =
            new UnicodeSet(idnValid)
                    .addAll(idnMapped)
                    .removeAll(new UnicodeSet("[:Block=Basic_Latin:]"))
                    .addAll(new UnicodeSet("[-a-zA-Z0-9..]"))
                    .freeze();
    public static final UnicodeSet validHostNoDot =
            new UnicodeSet(validHost).remove('.').remove('.').freeze();

    /** Status of link found. Note that if the link is imputed, https or mailto will be returned. */
    enum LinkStatus {
        bogus,
        http,
        https,
        mailto,
        tld
    }

    /** Immutable class for returning the start/end of link that is found, plus some information */
    public static final class LinkFound {
        public final int start;
        public final int limit;
        public final LinkStatus linkStatus;

        public LinkFound(int start, int limit, LinkStatus linkStatus) {
            this.start = start;
            this.limit = limit;
            this.linkStatus = linkStatus;
        }

        public String substring(String source) {
            return source.substring(start, limit);
        }
    }

    /**
     * Parses a restricted set of URLs, for testing the PathQueryFragment portion. That is, it is of
     * the form <host><domain_name><pathqueryfragment>? <br>
     * The host is currently just http, https, and mailto. <br>
     * The domain_name is just approximated for testing.
     *
     * @return null if we run out of string, otherwise a LinkFound. If what is found is malformed in
     *     some way, indicate with LinkFound.bogus.
     */
    public static LinkFound parseLink(String source, int startCodePointOffset) {
        Matcher findStartMatcher = protocol.matcher(source);

        findStartMatcher.region(startCodePointOffset, source.length());
        if (!findStartMatcher.find()) {
            return null; // we are at the end
        }
        // if we found something, and there was a character before it,
        // and that character was not soft, then exit
        int start = findStartMatcher.start();
        int protocolEnd = findStartMatcher.end();
        if (start != 0) {
            LinkTermination lt =
                    LinkTermination.PROPERTY_MAP.get(UCharacter.codePointBefore(source, start));
            if (lt == LinkTermination.Include) {
                return new LinkFound(start, protocolEnd, LinkStatus.bogus);
            }
        }

        String protocolValue = findStartMatcher.group(1);
        if (protocolValue.equals("mailto")) {
            return parseRestOfMailto(source, start, protocolEnd);
        }

        // dumb search for end of host, doesn't handle .. or edge cases, but this does not have to
        // be production-quality
        int hostLimit = findEndOfDomain(source, protocolEnd);
        if (protocolEnd == hostLimit) {
            return new LinkFound(start, protocolEnd, LinkStatus.bogus);
        }
        int limit = parsePathQueryFragment(source, hostLimit);
        return new LinkFound(
                start, limit, LinkStatus.valueOf(source.substring(start, protocolEnd)));
    }

    private static LinkFound parseRestOfMailto(String source, int start, int hostStart) {
        // basic implementation at first
        int atPosition = source.indexOf('@', hostStart);
        if (atPosition == -1) {
            return new LinkFound(start, hostStart, LinkStatus.bogus);
        }
        // TBD we could be in the middle of a quoted string, check for that later

        // see if what is in front of the @ looks ok

        int limit = parsePathQueryFragment(source, atPosition + 1);
        return new LinkFound(start, limit, LinkStatus.mailto);
    }

    // Simple implementation for testing, since the spec doesn't define the content
    private static int findEndOfDomain(String source, int protocolLimit) {
        return validHost.span(source, protocolLimit, SpanCondition.CONTAINED);
    }

    /**
     * Set lastSafe to 0 — this marks the last code point that is definitely included in the
     * linkification.<br>
     * Set closingStack to empty<br>
     * Set the current code point position i to 0<br>
     * Loop from i = 0 to n<br>
     * Set LT to LinkTermination(cp[i])<br>
     * If LT == none, set lastSafe to be i+1, continue loop<br>
     * If LT == soft, continue loop<br>
     * If LT == hard, stop linkification and return lastSafe<br>
     * If LT == opening, push cp[i] onto closingStack<br>
     * If LT == closing, set open to the pop of closingStack, or 0 if the closingStack is empty<br>
     * If LinkPairedOpeners(cp[i]) == open, set lastSafe to be i+1, continue loop.<br>
     * Otherwise, stop linkification and return lastSafe<br>
     * If lastSafe == n+1, then the entire part is safe; continue to the next part<br>
     * Otherwise, stop linkification and return lastSafe<br>
     */
    public static int parsePathQueryFragment(String source, int codePointOffset) {
        // For simplicity, and to match the spec, we just get the code points
        // Production code would be optimized, of course.

        int[] codePoints = source.codePoints().toArray();
        int lastSafe = codePointOffset;
        Part part = null;
        Stack<Integer> openingStack = new Stack<>();
        LinkTermination lt = LinkTermination.Soft;
        for (int i = codePointOffset; i < codePoints.length; ++i) {
            int cp = codePoints[i];
            if (part == null) {
                part = Part.fromInitiator(cp);
                if (part == null) {
                    return i; // failed, don't move cursor
                }
                lastSafe = i + 1;
                continue;
            }

            lt = LinkTermination.PROPERTY_MAP.get(cp);
            switch (lt) {
                case Include:
                    if (part.terminators.contains(cp)) {
                        lastSafe = i;
                        part = Part.fromInitiator(cp);
                        if (part == null) {
                            return lastSafe;
                        }
                    }
                    lastSafe = i + 1;
                    break;
                case Soft: // no action
                    break;
                case Hard:
                    return lastSafe;
                case Open:
                    openingStack.push(cp);
                    lastSafe = i + 1;
                    break;
                case Close:
                    if (openingStack.empty()) {
                        return lastSafe;
                    }
                    int matchingOpening = getOpening(cp);
                    Integer topOfStack = openingStack.pop();
                    if (matchingOpening == topOfStack) {
                        lastSafe = i + 1;
                        break;
                    } // else failed to match
                    return lastSafe;
            }
        }
        // if we hit the end, it acts like we hit a hard character ***
        return lt == LinkTermination.Soft ? lastSafe : codePoints.length;
    }

    /**
     * Minimally escape. Presumes that the parts have had necessary interior quoting.<br>
     *
     * @param atEndOfText TODO
     * @param escapedCounter TODO
     */
    public static String minimalEscape(
            NavigableMap<Part, String> parts,
            boolean atEndOfText,
            Counter<Integer> escapedCounter) {
        StringBuilder output = new StringBuilder();
        // get the last part
        List<Entry<Part, String>> ordered = List.copyOf(parts.entrySet());
        Part lastPart = ordered.get(ordered.size() - 1).getKey();
        // process all parts
        for (Entry<Part, String> partEntry : ordered) {
            Part part = partEntry.getKey();
            final String string = partEntry.getValue();
            if (string.isEmpty()) {
                throw new IllegalArgumentException();
            }
            if (part == Part.HOST || part == Part.PROTOCOL) {
                output.append(string);
                continue;
            }
            int[] cps = string.codePoints().toArray();
            int n = cps.length;
            if (cps[0] != part.initiator) {
                output.appendCodePoint(part.initiator);
            }
            ;
            int copiedAlready = 0;
            Stack<Integer> openingStack = new Stack<>();
            for (int i = 0; i < n; ++i) {
                final int cp = cps[i];
                LinkTermination lt =
                        part.terminators.contains(cp)
                                ? LinkTermination.Hard
                                : LinkTermination.PROPERTY_MAP.get(cp);
                switch (lt) {
                    case Include:
                        appendCodePointsBetween(output, cps, copiedAlready, i);
                        output.appendCodePoint(cp);
                        copiedAlready = i + 1;
                        break;
                    case Hard:
                        appendCodePointsBetween(output, cps, copiedAlready, i);
                        appendPercentEscaped(output, cp, escapedCounter);
                        copiedAlready = i + 1;
                        continue;
                    case Soft: // fix
                        continue;
                    case Open:
                        openingStack.push(cp);
                        appendCodePointsBetween(output, cps, copiedAlready, i);
                        output.appendCodePoint(cp);
                        copiedAlready = i + 1;
                        continue; // fix
                    case Close: // fix
                        if (openingStack.empty()) {
                            appendCodePointsBetween(output, cps, copiedAlready, i);
                            appendPercentEscaped(output, cp, escapedCounter);
                        } else {
                            Integer topOfStack = openingStack.pop();
                            int matchingOpening = getOpening(cp);
                            if (matchingOpening == topOfStack) {
                                appendCodePointsBetween(output, cps, copiedAlready, i);
                                output.appendCodePoint(cp);
                            } else { // failed to match
                                appendCodePointsBetween(output, cps, copiedAlready, i);
                                appendPercentEscaped(output, cp, escapedCounter);
                            }
                        }
                        copiedAlready = i + 1;
                        continue;
                    default:
                        throw new IllegalArgumentException();
                }
            } // fix
            if (atEndOfText || part != lastPart) {
                appendCodePointsBetween(output, cps, copiedAlready, n);
            } else if (copiedAlready < n) {
                appendCodePointsBetween(output, cps, copiedAlready, n - 1);
                appendPercentEscaped(output, cps[n - 1], escapedCounter);
            }
        }
        return output.toString();
    }

    private static void appendCodePointsBetween(
            StringBuilder output, int[] cp, int copyEnd, int notToCopy) {
        for (int i = copyEnd; i < notToCopy; ++i) {
            output.appendCodePoint(cp[i]);
        }
    }

    /** Regex for percent escaping */
    public static final Pattern escapedSequence = Pattern.compile("(%[a-fA-F0-9][a-fA-F0-9])+");

    /** Unescape a string; however, code points in toEscape are escaped back. */
    public static String unescape(String stringWithEscapes, UnicodeSet toEscape) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = escapedSequence.matcher(stringWithEscapes);
        int current = 0;
        while (matcher.find(current)) {
            result.append(
                    stringWithEscapes.substring(
                            current, matcher.start())); // append intervening text
            String unescaped = percentUnescape(matcher.group());
            unescaped
                    .chars()
                    .forEach(
                            x -> {
                                if (toEscape.contains(x)) {
                                    // quote it
                                    appendPercentEscaped(result, x, null);
                                } else {
                                    result.appendCodePoint(x);
                                }
                            });
            current = matcher.end();
        }
        result.append(stringWithEscapes.substring(current, stringWithEscapes.length()));
        return result.toString();
    }

    private static void appendPercentEscaped(
            StringBuilder output, int cp, Counter<Integer> escaped) {
        if (escaped != null) {
            escaped.add(cp, 1);
        }
        byte[] bytes = Character.toString(cp).getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; ++i) {
            output.append('%');
            output.append(Utility.hex(bytes[i]));
        }
    }

    /** We are guaranteed that string is all percent escaped utf8, %a3%c0 ... */
    private static String percentUnescape(String escapedSource) {
        byte[] temp = new byte[escapedSource.length() / 3];
        int tempOffset = 0;
        for (int i = 0; i < escapedSource.length(); i += 3) {
            if (escapedSource.charAt(i) != '%') {
                throw new IllegalArgumentException();
            }
            byte b = (byte) Integer.parseInt(escapedSource.substring(i + 1, i + 3), 16);
            temp[tempOffset++] = b;
        }
        return new String(temp, StandardCharsets.UTF_8);
    }

    /**
     * The wikipedia languages are not all BCP47. Convert the ones that are not. See:<br>
     * https://meta.wikimedia.org/wiki/Special_language_codes <br>
     * https://meta.wikimedia.org/wiki/List_of_Wikipedias#Nonstandard_language_codes
     *
     * @param languageCode
     * @return
     */
    public static String fixWiki(String languageCode) {
        switch (languageCode) {
            case "als":
                return "gsw";
            case "roa-rup":
                return "rup";
            case "bat-smg":
                return "sgs";
            case "simple":
                return "en";
            case "fiu-vro":
                return "vro";
            case "zh-classical":
                return "lzh";
            case "zh-min-nan":
                return "nan";
            case "zh-yue":
                return "yue";
            case "cbk-zam":
                return "cbk";
            case "map-bms":
                return "map";
            case "nrm":
                return "nrf";
            case "roa-tara":
                return "nap";
            default:
                return languageCode;
        }
    }

    /** Hard-coded list of wikilanguages, for testing */
    public static Set<String> WIKI_LANGUAGES =
            ImmutableSet.copyOf(
                    SPLIT_COMMA.splitToList(
                            "en,ceb,de,fr,sv,nl,ru,es,it,pl,arz,zh,ja,uk,vi,war,ar,pt,fa,ca,id,sr,ko,no,tr,ce,fi,cs,hu,tt,ro,sh,eu,zh-min-nan,ms,he,eo,hy,da,bg,uz,cy,simple,sk,et,be,azb,el,kk,min,hr,lt,gl,ur,az,sl,lld,ka,nn,ta,th,hi,bn,mk,zh-yue,la,ast,lv,af,tg,my,te,sq,mr,mg,bs,oc,be-tarask,ku,br,sw,ml,nds,ky,lmo,jv,pnb,ckb,new,ht,vec,pms,lb,ba,su,ga,is,szl,cv,pa,fy,io,ha,tl,an,mzn,wuu,diq,vo,ig,yo,sco,kn,ne,als,gu,ia,avk,crh,bar,ban,scn,bpy,mn,qu,nv,si,xmf,frr,ps,os,or,tum,sd,bcl,bat-smg,sah,cdo,gd,bug,glk,yi,ilo,am,li,nap,gor,as,fo,mai,hsb,map-bms,shn,zh-classical,eml,ace,ie,wa,sa,hyw,sat,zu,sn,mhr,lij,hif,km,bjn,mrj,mni,dag,ary,hak,pam,rue,roa-tara,ug,zgh,bh,nso,co,tly,so,vls,nds-nl,mi,se,myv,rw,kaa,sc,bo,kw,vep,mt,tk,mdf,kab,gv,gan,fiu-vro,ff,zea,ab,skr,smn,ks,gn,frp,pcd,udm,kv,csb,ay,nrm,lo,ang,fur,olo,lfn,lez,ln,pap,nah,mwl,tw,stq,rm,ext,lad,gom,dty,av,tyv,koi,dsb,lg,cbk-zam,dv,ksh,za,bxr,blk,gag,pfl,bew,szy,haw,tay,pag,pi,awa,tcy,krc,inh,gpe,xh,kge,fon,atj,to,pdc,mnw,arc,shi,om,tn,dga,ki,nia,jam,kbp,wo,xal,nov,kbd,anp,nqo,bi,kg,roa-rup,tpi,tet,guw,jbo,mad,fj,lbe,kcg,pcm,cu,ty,trv,dtp,sm,ami,st,iba,srn,btm,alt,ltg,gcr,ny,kus,mos,ss,chr,ee,ts,got,bbc,gur,bm,pih,ve,rmy,fat,chy,rn,igl,ik,guc,ch,ady,pnt,iu,ann,rsk,pwn,dz,ti,sg,din,tdd,kl,bdr,nr,cr"));

    /** Show termination values, for generating property files */
    void showLinkTermination() {
        for (LinkTermination lt : LinkTermination.values()) {
            UnicodeSet value = LinkTermination.PROPERTY_MAP.getSet(lt);
            String name = lt.toString();
            System.out.println("\n#\tLink_Termination=" + name);
            if (lt == LinkTermination.Include) {
                System.out.println("#   " + "(All code points without other values)");
                continue;
            } else {
                System.out.println("#   draft = " + lt.base);
            }
            if (lt == LinkTermination.Hard) {
                value.removeAll(new UnicodeSet("[\\p{Cn}\\p{Cs}]"));
                System.out.println("#   (not listing Unassigned or Surrogates)");
            }
            System.out.println();
            for (EntryRange range : value.ranges()) {
                final String rangeString =
                        Utility.hex(range.codepoint)
                                + (range.codepoint == range.codepointEnd
                                        ? ""
                                        : ".." + Utility.hex(range.codepointEnd));
                System.out.println(
                        rangeString
                                + ";"
                                + " ".repeat(15 - rangeString.length())
                                + lt
                                + "\t# "
                                + "("
                                + getGeneralCategory(
                                        UProperty.GENERAL_CATEGORY,
                                        range.codepoint,
                                        NameChoice.SHORT)
                                + ") "
                                + quote(UCharacter.getExtendedName(range.codepoint))
                                + (range.codepoint == range.codepointEnd
                                        ? ""
                                        : ".."
                                                + "("
                                                + getGeneralCategory(
                                                        UProperty.GENERAL_CATEGORY,
                                                        range.codepointEnd,
                                                        NameChoice.SHORT)
                                                + ") "
                                                + quote(
                                                        UCharacter.getExtendedName(
                                                                range.codepointEnd))));
            }
            System.out.println();
        }
    }

    /** Show paired openers, for generating property files */
    public void showLinkPairedOpeners() {
        UnicodeSet value = LinkTermination.PROPERTY_MAP.getSet(LinkTermination.Close);

        System.out.println("\n#\tLink_Paired_Opener");
        System.out.println(
                "#   draft = BidiPairedBracket + (“&gt;” GREATER-THAN SIGN 🡆  “&lt;” LESS-THAN SIGN)");
        System.out.println();

        for (String cpString : value) {
            int cp = cpString.codePointAt(0);
            String hex = Utility.hex(cp);
            final int value2 = getOpening(cp);
            System.out.println(
                    hex
                            + ";"
                            + " ".repeat(7 - hex.length())
                            + Utility.hex(value2)
                            + "\t#"
                            + " “"
                            + quote(UTF16.valueOf(cp))
                            + "” "
                            + UCharacter.getExtendedName(cp)
                            + " 🡆 "
                            + " “"
                            + quote(UTF16.valueOf(value2))
                            + "” "
                            + UCharacter.getExtendedName(value2));
        }
    }

    /**
     * Regex to scan for possible TLDs. The result needs to be checked that there is no
     * validHostNoDot before and after
     */
    public static final Pattern TLD_SCANNER;

    private static final Comparator<String> LENGTH_FIRST =
            new Comparator<>() {

                @Override
                public int compare(String o1, String o2) {
                    return ComparisonChain.start()
                            .compare(o2.length(), o1.length())
                            .compare(o1, o2)
                            .result();
                }
            };

    static {
        final UnicodeSet ASCII_TLD = new UnicodeSet("[a-zA-Z]").freeze();
        try {
            final Path filePath =
                    Path.of(LinkUtilities.RESOURCE_DIR + "tlds-alpha-by-domain.txt").toRealPath();
            List<String> allLines = Files.readAllLines(filePath);
            Set<String> core = new TreeSet<>(LENGTH_FIRST);
            Set<String> nonAscii = new TreeSet<>();
            allLines.stream()
                    .filter(x -> !x.startsWith("#"))
                    .forEach(
                            x -> {
                                // For some reason, Java isn't honoring the (?u). So as a
                                // workaround, add the lower cases
                                core.add(x);
                                core.add(UCharacter.toLowerCase(x));
                                String y = toUnicode2(x);
                                if (!x.equals(y)) {
                                    core.add(y);
                                    core.add(UCharacter.toLowerCase(y));
                                    nonAscii.add(y);
                                }
                            });
            String pattern = "(?u)[.。](" + Joiner.on('|').join(core) + ")";
            TLD_SCANNER = Pattern.compile(pattern);
            if (SHOW_NON_ASCII_TLDS) {
                System.out.println(nonAscii);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String toUnicode2(String x) {
        try {
            if (x.startsWith("XN--")) {
                x = IDNA2003.convertIDNToUnicode(x, 0).toString();
            }
            return x;
        } catch (StringPrepParseException e) {
            throw new UncheckedExecutionException(e);
        }
    }

    /**
     * Scan backwards from limit to the first character that uset::contains != doesContain. Returns
     * the offset *after* that character. Stops at start;
     */
    public static int scanBackward(
            UnicodeSet uset, boolean doesContain, String source, int start, int limit) {
        while (limit > start) {
            int cp = source.codePointBefore(limit);
            if (uset.contains(cp) != doesContain) {
                break;
            }
            limit -= UCharacter.charCount(cp);
        }
        return limit;
    }

    /** Scans a string for links */
    public static class LinkScanner {
        private static final UnicodeSet DIGITS = new UnicodeSet("[0-9]").freeze();
        private final String source;
        private final Matcher m;
        private final int limit;
        private int hardStart; // ::next can't backup before this value
        private int linkStart;
        private int linkEnd;

        public LinkScanner(String source, int start, int limit) {
            this.source = source;
            this.hardStart = start;
            this.limit = limit;
            // Note: this regex pattern doesn't check for validHost, because
            // it is messy to add huge UnicodeSet patterns to regular expressions
            m = LinkUtilities.TLD_SCANNER.matcher(source);
            m.region(start, limit);
        }

        /** call only if next() is true */
        public int getLinkStart() {
            return linkStart;
        }

        /** call only if next() is true */
        public int getLinkEnd() {
            return linkEnd;
        }

        /**
         * check for next item. If there is one, the result is true, and getLinkStart/getLinkEnd are
         * set to valid offsets
         */
        public boolean next() {
            while (true) {
                // scan forward until we reach what might be a TLD
                if (!m.find()) {
                    return false;
                }
                // we found something of the form ".com", so check for longer label
                // it is ok to have trailing dot after TLD (Fully Qualified Domain Name), but
                // bad cases are .comx or .com.x
                linkEnd = m.end();
                if (linkEnd < limit) {
                    int nextCp = source.codePointAt(linkEnd);
                    if (nextCp == '.') {
                        linkEnd++;
                        nextCp = linkEnd < limit ? source.codePointAt(linkEnd) : 0;
                        // 0 is just after the end of the string, makes the logic simpler
                    }
                    if (validHost.contains(nextCp)) {
                        // scan again. We found something like ".comx", which could be part of a
                        // domain name

                        continue;
                    }
                    if (nextCp == ':') {
                        // ugly, we want to only go to limit. But for testing...
                        int limitDigits = DIGITS.span(source, linkEnd + 1, SpanCondition.CONTAINED);
                        if (limitDigits > linkEnd + 6) {
                            // too long, scan again.
                            continue;
                        }
                        int possiblePort = Integer.parseInt(source, linkEnd + 1, limitDigits, 10);
                        if (possiblePort > 0xFFFF) {
                            continue; // too big, scan again.
                        }
                        linkEnd = limitDigits;
                    }
                }
                // the linkEnd is ok, scan backwards to get the start of the domain name
                int domainStart =
                        LinkUtilities.scanBackward(validHost, true, source, hardStart, m.start());
                if (m.start() - domainStart < 1) {
                    // we don't have enough for a link
                    continue;
                }
                // TBD, check edge conditions like abc..def.com

                if (domainStart > hardStart) {
                    int cpBefore = source.codePointBefore(domainStart);
                    if (cpBefore == '@') {
                        // TBD do dumb parse backwards; fix later
                        // scan backwards to start of local-part
                        int mailToStart =
                                LinkUtilities.scanBackward(
                                        validHost, true, source, hardStart, domainStart - 1);

                        // check for mailto: beforehand
                        linkStart = backupIfAfter("mailto:", mailToStart);
                        hardStart = linkEnd; // prepare for next next()
                        return true;
                    }
                }

                // we are either http or https

                int temp = backupIfAfter("https://", domainStart);
                linkStart = temp != domainStart ? temp : backupIfAfter("http://", domainStart);

                // check for a port;
                if (linkEnd < limit)
                    // Extend end for path, query, etc.
                    linkEnd = parsePathQueryFragment(source, linkEnd);

                hardStart = linkEnd; // prepare for next next()
                return true;
            }
        }

        private int backupIfAfter(String string, int currentPosition) {
            int len = string.length();
            if (hardStart <= currentPosition - len) {
                if (source.regionMatches(currentPosition - len, string, 0, len)) {
                    currentPosition -= len;
                }
            }
            return currentPosition;
        }
    }
}
