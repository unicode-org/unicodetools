package org.unicode.idna;

import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.text.UnicodeSet;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.unicode.cldr.util.props.UnicodeLabel;
import org.unicode.props.BagFormatter;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UnicodeProperty.UnicodeMapProperty;
import org.unicode.text.UCD.Normalizer;
import org.unicode.text.UCD.UCD;
import org.unicode.text.UCD.VersionedSymbolTable;
import org.unicode.text.utility.DiffingPrintWriter;
import org.unicode.text.utility.Settings;

public class Idna2008 extends Idna {

    public static final UnicodeSet XV8 = new UnicodeSet().add(0x19DA).freeze();

    public enum Idna2008Type {
        UNASSIGNED,
        DISALLOWED,
        PVALID,
        CONTEXTJ,
        CONTEXTO
    }

    static final UnicodeMap<Idna2008Type> IDNA2008Computed;

    static {
        final var symbolTable = VersionedSymbolTable.forDevelopment();
        final var oldDefaultXSymbolTable = UnicodeSet.getDefaultXSymbolTable();
        UnicodeSet.setDefaultXSymbolTable(VersionedSymbolTable.NO_PROPS);
        try {
            IDNA2008Computed = computeTypeMapping(symbolTable);
        } finally {
            UnicodeSet.setDefaultXSymbolTable(oldDefaultXSymbolTable);
        }
    }

    private static UnicodeMap<Idna2008Type> computeTypeMapping(VersionedSymbolTable symbolTable) {
        // A: General_Category(cp) is in {Ll, Lu, Lo, Nd, Lm, Mn, Mc}
        final UnicodeSet LetterDigits =
                new UnicodeSet("[[:Ll:][:Lu:][:Lo:][:Nd:][:Lm:][:Mn:][:Mc:]]", null, symbolTable)
                        .freeze();

        // B: toNFKC(toCaseFold(toNFKC(cp))) != cp
        final UnicodeSet Unstable = new UnicodeSet();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            final String s = Character.toString(i);
            final String nfkc = Normalizer.getNfkcInstance().transform(s);
            final String cased = UCD.makeLatestVersion().getCase(nfkc, UCD.FULL, UCD.FOLD);
            final String full = Normalizer.getNfkcInstance().transform(cased);
            if (!s.equals(full)) {
                Unstable.add(i);
            }
        }
        Unstable.freeze();

        // C: Default_Ignorable_Code_Point(cp) = True or
        // White_Space(cp) = True or
        // Noncharacter_Code_Point(cp) = True
        final UnicodeSet IgnorableProperties =
                new UnicodeSet(
                                "[[:Default_Ignorable_Code_Point:]"
                                        + "[:White_Space:]"
                                        + "[:Noncharacter_Code_Point:]]",
                                null,
                                symbolTable)
                        .freeze();

        // Block(cp) is in {Combining Diacritical Marks for Symbols,
        // Musical Symbols, Ancient Greek Musical Notation}
        final UnicodeSet IgnorableBlocks =
                new UnicodeSet(
                                "[[:block=Combining Diacritical Marks for Symbols:]"
                                        + "[:block=Musical Symbols:]"
                                        + "[:block=Ancient Greek Musical Notation:]]",
                                null,
                                symbolTable)
                        .freeze();

        // E: cp is in {002D, 0030..0039, 0061..007A}
        final UnicodeSet LDH = new UnicodeSet("[\u002D\u0030-\u0039\u0061-\u007A]").freeze();

        // F: cp is in {00B7, 00DF, 0375, 03C2, 05F3, 05F4, 0640, 0660,
        // 0661, 0662, 0663, 0664, 0665, 0666, 0667, 0668,
        // 0669, 06F0, 06F1, 06F2, 06F3, 06F4, 06F5, 06F6,
        // 06F7, 06F8, 06F9, 06FD, 06FE, 07FA, 0F0B, 3007,
        // 302E, 302F, 3031, 3032, 3033, 3034, 3035, 303B,
        // 30FB}

        final UnicodeMap<Idna2008Type> Exceptions =
                new UnicodeMap<Idna2008Type>()
                        .putAll(
                                new UnicodeSet("[\u00DF\u03C2\u06FD\u06FE\u0F0B\u3007]"),
                                Idna2008Type.PVALID)
                        .putAll(
                                new UnicodeSet(
                                        "[\u00B7\u0375\u05F3\u05F4\u30FB\u0660-\u0669\u06F0-\u06F9]"),
                                Idna2008Type.CONTEXTO)
                        .putAll(
                                new UnicodeSet(
                                        "[\u0640\u07FA\u302E\u302F\u3031\u3032\u3033\u3034\u3035\u303B]"),
                                Idna2008Type.DISALLOWED)
                        .freeze();

        // G: cp is in {}

        final UnicodeMap<Idna2008Type> BackwardCompatible = new UnicodeMap<Idna2008Type>().freeze();

        // H: Join_Control(cp) = True

        final UnicodeSet JoinControl = new UnicodeSet("[:Join_Control:]", null, symbolTable);

        // Hangul_Syllable_Type(cp) is in {L, V, T}

        final UnicodeSet OldHangulJamo =
                new UnicodeSet(
                        "[[:Hangul_Syllable_Type=L:]"
                                + "[:Hangul_Syllable_Type=V:]"
                                + "[:Hangul_Syllable_Type=T:]]",
                        null,
                        symbolTable);

        // J: General_Category(cp) is in {Cn} and
        // Noncharacter_Code_Point(cp) = False

        final UnicodeSet Unassigned =
                new UnicodeSet("[[:Cn:]-[:Noncharacter_Code_Point:]]", null, symbolTable);

        // If .cp. .in. Exceptions Then Exceptions(cp);
        // Else If .cp. .in. BackwardCompatible Then BackwardCompatible(cp);
        // Else If .cp. .in. Unassigned Then UNASSIGNED;
        // Else If .cp. .in. LDH Then PVALID;
        // Else If .cp. .in. JoinControl Then CONTEXTJ;
        // Else If .cp. .in. Unstable Then DISALLOWED;
        // Else If .cp. .in. IgnorableProperties Then DISALLOWED;
        // Else If .cp. .in. IgnorableBlocks Then DISALLOWED;
        // Else If .cp. .in. OldHangulJamo Then DISALLOWED;
        // Else If .cp. .in. LetterDigits Then PVALID;
        // Else DISALLOWED;

        final UnicodeMap<Idna2008Type> result = new UnicodeMap<>();

        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Idna2008Type value;
            if (Exceptions.containsKey(cp)) {
                value = Exceptions.get(cp);
            } else if (BackwardCompatible.containsKey(cp)) {
                value = BackwardCompatible.get(cp);
            } else if (Unassigned.contains(cp)) {
                value = Idna2008Type.UNASSIGNED;
            } else if (LDH.contains(cp)) {
                value = Idna2008Type.PVALID;
            } else if (JoinControl.contains(cp)) {
                value = Idna2008Type.CONTEXTJ;
            } else if (Unstable.contains(cp)) {
                value = Idna2008Type.DISALLOWED;
            } else if (IgnorableProperties.contains(cp)) {
                value = Idna2008Type.DISALLOWED;
            } else if (IgnorableBlocks.contains(cp)) {
                value = Idna2008Type.DISALLOWED;
            } else if (OldHangulJamo.contains(cp)) {
                value = Idna2008Type.DISALLOWED;
            } else if (LetterDigits.contains(cp)) {
                value = Idna2008Type.PVALID;
            } else {
                value = Idna2008Type.DISALLOWED;
            }
            result.put(cp, value);
        }
        return result.freeze();
    }

    static final Instant now = Instant.now();
    static final DateTimeFormatter dt =
            DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss' GMT'")
                    .withZone(ZoneId.of("UTC")); // Explicitly set to UTC/GMT
    static final DateTimeFormatter dty =
            DateTimeFormatter.ofPattern("y")
                    .withZone(ZoneId.of("UTC")); // Explicitly set to UTC/GMT

    public static final String DATA_DIR_DEV =
            Settings.UnicodeTools.UNICODETOOLS_REPO_DIR + "/unicodetools/data/idna/dev/";

    static final SimpleFormatter HEADER =
            SimpleFormatter.compile(
                    """
        # Idna2008-{0}.txt
        # Date: {1}
        # Copyright {2} Unicode, Inc.
        # For terms of use and license, see https://www.unicode.org/terms_of_use.html
        #
        #
        # IDNA2008_Category Property
        #
        # This file lists the "IDNA Derived Property" as defined in RFC 5892.
        # It is provided as a convenience for implementers by performing
        # the calculations defined in RFC 5892 concurrent with the release
        # of each version of the Unicode Character Database.
        #
        # The format is two fields separated by a semicolon.
        # Field 0: Unicode code point value or range of code point values
        #            Ranges in this file, unlike in other property files, may cross
        #            script and block boundaries; their extent is only determined
        #            by the range of the common IDNA2008_Category value.
        #            They are indicated in the usual notation using "..".
        # Field 1: IDNA2008_Category, consisting of one of these values
        #            "PVALID"     - Protocol valid (generally Letters, Digits and Hyphen)
        #            "CONTEXTJ"   - Join control
        #            "CONTEXTO"   - Other code points requiring context
        #            "DISALLOWED" - The code point is not allowed in IDNA2008
        #            "UNASSIGNED" - The code point is not assigned in this version
        # Following Field 1 is a comment field that lists the character name
        # (or code point label) for the code point, or the first and last character
        # name for the characters in the code point range.
        #
        # The values of the IDNA2008_Category property are derived from
        # other Unicode properties in the current version of the Unicode
        # Character Database as follows:
        #
        # The precise algorithm for deriving the property is defined in
        # Section 3 "Calculation of the Derived Property" of RFC 5892.
        #
        # Section 2.6 "Exceptions" in RFC 5892 lists code point for which
        # the derivation is overridden by exceptional values. All the exceptions
        # known at the time this data file was created have been applied.
        # However, future updates of the IDNA protocol may add to this list
        # of exceptions, which then would override the values derived here.
        #
        # However, once published, this file will not be updated.
        #
        # A value of the property is given for each code point.
        #
        # For more information, see RFC 5892, "The Unicode Code Points and
        # Internationalized Domain Names for Applications (IDNA)",
        # at https://www.rfc-editor.org/info/rfc5892
        #
        # @missing: 0000..10FFFF; UNASSIGNED
        #""");

    public static void generateIdna2008() {
        final var map = new UnicodeMap<String>();
        for (final var v : Idna2008Type.values()) {
            map.putAll(IDNA2008Computed.keySet(v), v.toString());
        }
        try (final var out = new DiffingPrintWriter(DATA_DIR_DEV, "Idna2008.txt")) {
            out.println(HEADER.format(Settings.latestVersion, dt.format(now), dty.format(now)));
            final BagFormatter bf =
                    new BagFormatter(IndexUnicodeProperties.make())
                            .setLineSeparator("\n")
                            .setValueSource(new UnicodeMapProperty().set(map))
                            .setRangeBreakSource(new UnicodeLabel.Constant(""))
                            .setMinSpacesBeforeSemicolon(-2)
                            .setLabelSource(null)
                            .setMinSpacesBeforeComment(2)
                            .setShowCount(false)
                            .setAlignNames(false)
                            .setShowTotal(false);
            bf.showSetNames(out.tempPrintWriter, UnicodeSet.ALL_CODE_POINTS);
            out.println();
            out.println("# EOF");
            out.flush();
        }
    }

    public static Idna2008 SINGLETON = new Idna2008();

    private Idna2008() {
        for (final Idna2008Type oldType : IDNA2008Computed.values()) {
            final UnicodeSet uset = IDNA2008Computed.getSet(oldType);
            switch (oldType) {
                case UNASSIGNED:
                case DISALLOWED:
                    types.putAll(uset, Idna.IdnaType.disallowed);
                    break;
                case PVALID:
                case CONTEXTJ:
                case CONTEXTO:
                    types.putAll(uset, Idna.IdnaType.valid);
                    break;
            }
        }
        types.put('.', IdnaType.valid);
        types.freeze();
        mappings.freeze();
        mappings_display.freeze();
        validSet = validSet_transitional = types.getSet(IdnaType.valid).freeze();
    }

    public static UnicodeMap<Idna2008Type> getTypeMapping() {
        return IDNA2008Computed;
    }

    public static UnicodeSet getIdna2008Valid() {
        //    IdnaLabelTester tester = getIdna2008Tester();
        //    UnicodeSet valid2008 =
        // UnicodeSetUtilities.parseUnicodeSet(tester.getVariable("$Valid"), TableStyle.simple);
        //    return valid2008;
        UnicodeMap<Idna2008Type> typeMapping = Idna2008.getTypeMapping();
        return new UnicodeSet(typeMapping.getSet(Idna2008Type.PVALID))
                .addAll(typeMapping.getSet(Idna2008Type.CONTEXTJ))
                .addAll(typeMapping.getSet(Idna2008Type.CONTEXTO));
    }
}
