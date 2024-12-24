package org.unicode.tools;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.EntryRange;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.text.utility.Utility;

public class UrlUtilities {

    public static final boolean DEBUG = false;
    public static final boolean USE_CLDR = false;

    public static final String RESOURCE_DIR =
            "/Users/markdavis/github/unicodetools/unicodetools/src/main/resources/org/unicode/tools/";

    public static final Splitter SPLIT_COMMA = Splitter.on(',');
    public static final Splitter SPLIT_TAB = Splitter.on('\t');

    public static final Joiner JOIN_TAB = Joiner.on('\t');
    public static final Joiner JOIN_LF = Joiner.on('\n');
    public static final Joiner JOIN_EMPTY = Joiner.on("");

    static final CLDRFile ENGLISH = USE_CLDR ? CLDRConfig.getInstance().getEnglish() : null;

    public enum LinkTermination {
        Include("[\\p{ANY}]"), // overridden by following
        Hard("[\\p{whitespace}\\p{NChar}[\\p{C}-\\p{Cf}]\\p{deprecated}]"),
        Soft("[\\p{Term}\\p{lb=qu}]"), // was [‘-‛ ‹ › \"“-‟ « »'] instead of \p{lb=qu}
        Close("[\\p{Bidi_Paired_Bracket_Type=Close}[>]]"),
        Open("[\\p{Bidi_Paired_Bracket_Type=Open}[<]]"),
        ;

        final UnicodeSet base;

        private LinkTermination(String uset) {
            this.base = new UnicodeSet(uset).freeze();
        }

        static final UnicodeMap<LinkTermination> Property = new UnicodeMap<>();

        static {
            for (LinkTermination lt : values()) {
                Property.putAll(lt.base, lt);
            }
            Property.freeze();
        }
    }

    public String getGeneralCategory(int property, int codePoint, int nameChoice) {
        return UCharacter.getPropertyValueName(
                property, UCharacter.getIntPropertyValue(codePoint, property), nameChoice);
    }

    static String quote(String s) {
        return TransliteratorUtilities.toHTML.transform(s);
    }

    public static int getOpening(int cp) {
        return cp == '>' ? '<' : UCharacter.getBidiPairedBracket(cp);
    }

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
        static NavigableMap<Part, String> getParts(String source, boolean unescape) {
            Map<Part, String> result = new HashMap<>();
            // quick and dirty
            int partStart = 0;
            int partEnd;
            main:
            for (Part part : Part.values()) {
                switch (part) {
                    case PROTOCOL:
                        partEnd = source.indexOf("://");
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
        String unescape(String substring) {
            return UrlUtilities.unescape(substring, extraQuoted);
        }
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
        int[] codePoints = source.codePoints().toArray();
        int lastSafe = codePointOffset;
        Part part = null;
        Stack<Integer> openingStack = new Stack<>();
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

            LinkTermination lt = LinkTermination.Property.get(cp);
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
        return codePoints.length;
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
                                : LinkTermination.Property.get(cp);
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

    public static final Pattern escapedSequence = Pattern.compile("(%[a-fA-F0-9][a-fA-F0-9])+");

    /** Unescape a string; however, code points in toEscape are escaped back */
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

    public static Set<String> WIKI_LANGUAGES =
            ImmutableSet.copyOf(
                    SPLIT_COMMA.splitToList(
                            "en,ceb,de,fr,sv,nl,ru,es,it,pl,arz,zh,ja,uk,vi,war,ar,pt,fa,ca,id,sr,ko,no,tr,ce,fi,cs,hu,tt,ro,sh,eu,zh-min-nan,ms,he,eo,hy,da,bg,uz,cy,simple,sk,et,be,azb,el,kk,min,hr,lt,gl,ur,az,sl,lld,ka,nn,ta,th,hi,bn,mk,zh-yue,la,ast,lv,af,tg,my,te,sq,mr,mg,bs,oc,be-tarask,ku,br,sw,ml,nds,ky,lmo,jv,pnb,ckb,new,ht,vec,pms,lb,ba,su,ga,is,szl,cv,pa,fy,io,ha,tl,an,mzn,wuu,diq,vo,ig,yo,sco,kn,ne,als,gu,ia,avk,crh,bar,ban,scn,bpy,mn,qu,nv,si,xmf,frr,ps,os,or,tum,sd,bcl,bat-smg,sah,cdo,gd,bug,glk,yi,ilo,am,li,nap,gor,as,fo,mai,hsb,map-bms,shn,zh-classical,eml,ace,ie,wa,sa,hyw,sat,zu,sn,mhr,lij,hif,km,bjn,mrj,mni,dag,ary,hak,pam,rue,roa-tara,ug,zgh,bh,nso,co,tly,so,vls,nds-nl,mi,se,myv,rw,kaa,sc,bo,kw,vep,mt,tk,mdf,kab,gv,gan,fiu-vro,ff,zea,ab,skr,smn,ks,gn,frp,pcd,udm,kv,csb,ay,nrm,lo,ang,fur,olo,lfn,lez,ln,pap,nah,mwl,tw,stq,rm,ext,lad,gom,dty,av,tyv,koi,dsb,lg,cbk-zam,dv,ksh,za,bxr,blk,gag,pfl,bew,szy,haw,tay,pag,pi,awa,tcy,krc,inh,gpe,xh,kge,fon,atj,to,pdc,mnw,arc,shi,om,tn,dga,ki,nia,jam,kbp,wo,xal,nov,kbd,anp,nqo,bi,kg,roa-rup,tpi,tet,guw,jbo,mad,fj,lbe,kcg,pcm,cu,ty,trv,dtp,sm,ami,st,iba,srn,btm,alt,ltg,gcr,ny,kus,mos,ss,chr,ee,ts,got,bbc,gur,bm,pih,ve,rmy,fat,chy,rn,igl,ik,guc,ch,ady,pnt,iu,ann,rsk,pwn,dz,ti,sg,din,tdd,kl,bdr,nr,cr"));

    void showLinkTermination() {
        for (LinkTermination lt : LinkTermination.values()) {
            UnicodeSet value = LinkTermination.Property.getSet(lt);
            String name = lt.toString();
            System.out.println("\n#\tLink_Termination=" + name);
            if (lt == lt.Include) {
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

    public void showLinkPairedOpeners() {
        UnicodeSet value = LinkTermination.Property.getSet(LinkTermination.Close);

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
}
