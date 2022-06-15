package org.unicode.text.UCD;

public class ToolIdna2008 {
    //
    //    public enum Idna2008Type {
    //        UNASSIGNED, DISALLOWED, PVALID, CONTEXTJ, CONTEXTO
    //    }
    //
    //    static ToolUnicodePropertySource source =
    // ToolUnicodePropertySource.make(Default.ucdVersion());
    //
    //    private static final UnicodeSet IDNA2008Valid;
    //
    //    static final UnicodeMap<Idna2008Type> IDNA2008Computed;
    //
    //    static {
    //        // A: General_Category(cp) is in {Ll, Lu, Lo, Nd, Lm, Mn, Mc}
    //        UnicodeSet LetterDigits = new UnicodeSet() //
    // "[[:Ll:][:Lu:][:Lo:][:Nd:][:Lm:][:Mn:][:Mc:]]")
    //        .addAll(source.getSet("gc=Ll"))
    //        .addAll(source.getSet("gc=Lu"))
    //        .addAll(source.getSet("gc=Lo"))
    //        .addAll(source.getSet("gc=Nd"))
    //        .addAll(source.getSet("gc=Lm"))
    //        .addAll(source.getSet("gc=Mn"))
    //        .addAll(source.getSet("gc=Mc"))
    //        .freeze();
    //
    //        // B: toNFKC(toCaseFold(toNFKC(cp))) != cp
    //        UnicodeSet Unstable = new UnicodeSet();
    //        Normalizer toNfkc = Default.nfkc();
    //        for (int i = 0; i <= 0x10FFFF; ++i) {
    //            String s = UTF16.valueOf(i);
    //            String nfkc = toNfkc.normalize(s);
    //            String cased = Default.ucd().getCase(s, UCD_Types.FULL, UCD_Types.FOLD);
    //            String full = toNfkc.normalize(cased);
    //            if (!s.equals(full)) {
    //                Unstable.add(i);
    //            }
    //        }
    //        Unstable.freeze();
    //
    //        // C: Default_Ignorable_Code_Point(cp) = True or
    //        // White_Space(cp) = True or
    //        // Noncharacter_Code_Point(cp) = True
    //        UnicodeSet IgnorableProperties = new UnicodeSet()
    //        .addAll(source.getSet("Default_Ignorable_Code_Point=Yes"))
    //        .addAll(source.getSet("White_Space=Yes"))
    //        .addAll(source.getSet("Noncharacter_Code_Point=Yes"))
    //                //"[[:Default_Ignorable_Code_Point:]" +
    //                //"[:White_Space:]" +
    //                //"[:Noncharacter_Code_Point:]]")
    //        .freeze();
    //
    //        // Block(cp) is in {Combining Diacritical Marks for Symbols,
    //        // Musical Symbols, Ancient Greek Musical Notation}
    //        UnicodeSet IgnorableBlocks = new UnicodeSet()
    //        .addAll(source.getSet("block=Combining Diacritical Marks for Symbols"))
    //        .addAll(source.getSet("block=Musical Symbols"))
    //        .addAll(source.getSet("block=Ancient Greek Musical Notation"))
    //                // "[[:block=Combining Diacritical Marks for Symbols:]" +
    //                //"[:block=Musical Symbols:]" +
    //                //"[:block=Ancient Greek Musical Notation:]]")
    //                .freeze();
    //
    //        // E: cp is in {002D, 0030..0039, 0061..007A}
    //        UnicodeSet LDH = new UnicodeSet("[\u002D\u0030-\u0039\u0061-\u007A]").freeze();
    //
    //        // F: cp is in {00B7, 00DF, 0375, 03C2, 05F3, 05F4, 0640, 0660,
    //        // 0661, 0662, 0663, 0664, 0665, 0666, 0667, 0668,
    //        // 0669, 06F0, 06F1, 06F2, 06F3, 06F4, 06F5, 06F6,
    //        // 06F7, 06F8, 06F9, 06FD, 06FE, 07FA, 0F0B, 3007,
    //        // 302E, 302F, 3031, 3032, 3033, 3034, 3035, 303B,
    //        // 30FB}
    //
    //        UnicodeMap<Idna2008Type> Exceptions = new UnicodeMap<Idna2008Type>()
    //                .putAll(new UnicodeSet("[\u00DF\u03C2\u06FD\u06FE\u0F0B\u3007]"),
    // Idna2008Type.PVALID)
    //                .putAll(new
    // UnicodeSet("[\u00B7\u0375\u05F3\u05F4\u30FB\u0660-\u0669\u06F0-\u06F9]"),
    // Idna2008Type.CONTEXTO)
    //                .putAll(new
    // UnicodeSet("[\u0640\u07FA\u302E\u302F\u3031\u3032\u3033\u3034\u3035\u303B]"),
    // Idna2008Type.DISALLOWED)
    //                .freeze();
    //
    //        // G: cp is in {}
    //
    //        UnicodeMap<Idna2008Type> BackwardCompatible = new UnicodeMap<Idna2008Type>().freeze();
    //
    //        // H: Join_Control(cp) = True
    //
    //        UnicodeSet JoinControl = new UnicodeSet(source.getSet("Join_Control=Yes")).freeze();
    //        //"[:Join_Control:]");
    //
    //        // Hangul_Syllable_Type(cp) is in {L, V, T}
    //
    //        UnicodeSet OldHangulJamo = new UnicodeSet()
    //        .addAll(source.getSet("Hangul_Syllable_Type=L"))
    //        .addAll(source.getSet("Hangul_Syllable_Type=V"))
    //        .addAll(source.getSet("Hangul_Syllable_Type=T"))
    ////                "[[:Hangul_Syllable_Type=L:]" +
    ////                "[:Hangul_Syllable_Type=V:]" +
    ////                "[:Hangul_Syllable_Type=T:]]")
    //        .freeze();
    //
    //
    //        // J: General_Category(cp) is in {Cn} and
    //        // Noncharacter_Code_Point(cp) = False
    //
    //        UnicodeSet Unassigned = new UnicodeSet()
    //        .addAll(source.getSet("gc=Cn"))
    //        .addAll(source.getSet("Noncharacter_Code_Point=Yes"))
    //                //"[[:Cn:]-[:Noncharacter_Code_Point:]]");
    //        .freeze();
    //
    //        // If .cp. .in. Exceptions Then Exceptions(cp);
    //        // Else If .cp. .in. BackwardCompatible Then BackwardCompatible(cp);
    //        // Else If .cp. .in. Unassigned Then UNASSIGNED;
    //        // Else If .cp. .in. LDH Then PVALID;
    //        // Else If .cp. .in. JoinControl Then CONTEXTJ;
    //        // Else If .cp. .in. Unstable Then DISALLOWED;
    //        // Else If .cp. .in. IgnorableProperties Then DISALLOWED;
    //        // Else If .cp. .in. IgnorableBlocks Then DISALLOWED;
    //        // Else If .cp. .in. OldHangulJamo Then DISALLOWED;
    //        // Else If .cp. .in. LetterDigits Then PVALID;
    //        // Else DISALLOWED;
    //
    //        UnicodeMap<Idna2008Type> Incompatible = new UnicodeMap<Idna2008Type>().put(0x19DA,
    // Idna2008Type.PVALID).freeze();
    //
    //        IDNA2008Computed = new UnicodeMap<Idna2008Type>();
    //
    //        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
    //            Idna2008Type value;
    //            if (Incompatible.containsKey(cp)) {
    //                value = Incompatible.get(cp);
    //            } else if (Exceptions.containsKey(cp)) {
    //                value = Exceptions.get(cp);
    //            } else if (BackwardCompatible.containsKey(cp)) {
    //                value = BackwardCompatible.get(cp);
    //            } else if (Unassigned.contains(cp)) {
    //                value = Idna2008Type.UNASSIGNED;
    //            } else if (LDH.contains(cp)) {
    //                value = Idna2008Type.PVALID;
    //            } else if (JoinControl.contains(cp)) {
    //                value = Idna2008Type.CONTEXTJ;
    //            } else if (Unstable.contains(cp)) {
    //                value = Idna2008Type.DISALLOWED;
    //            } else if (IgnorableProperties.contains(cp)) {
    //                value = Idna2008Type.DISALLOWED;
    //            } else if (IgnorableBlocks.contains(cp)) {
    //                value = Idna2008Type.DISALLOWED;
    //            } else if (OldHangulJamo.contains(cp)) {
    //                value = Idna2008Type.DISALLOWED;
    //            } else if (LetterDigits.contains(cp)) {
    //                value = Idna2008Type.PVALID;
    //            } else {
    //                value = Idna2008Type.DISALLOWED;
    //            }
    //            IDNA2008Computed.put(cp, value);
    //        }
    //        IDNA2008Computed.freeze();
    //        IDNA2008Valid = new UnicodeSet()
    //        .addAll(IDNA2008Computed.getSet(Idna2008Type.PVALID))
    //        .addAll(IDNA2008Computed.getSet(Idna2008Type.CONTEXTJ))
    //        .addAll(IDNA2008Computed.getSet(Idna2008Type.CONTEXTO))
    //        .freeze();
    //    }
    //
    //    public static ToolIdna2008                SINGLETON = new ToolIdna2008();
    //
    //    public static UnicodeMap<Idna2008Type> getTypeMapping() {
    //        return IDNA2008Computed;
    //    }
    //
    //    public static UnicodeSet getValid() {
    //        return IDNA2008Valid;
    //    }
}
