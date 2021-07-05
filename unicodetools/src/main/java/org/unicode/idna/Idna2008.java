package org.unicode.idna;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class Idna2008 extends Idna {

    public static final UnicodeSet GRANDFATHERED_VALID = new UnicodeSet().add(0x19DA).freeze();

    public enum Idna2008Type {
        UNASSIGNED, DISALLOWED, PVALID, CONTEXTJ, CONTEXTO
    }

    static final UnicodeMap<Idna2008Type> IDNA2008Computed;

    static {
        // A: General_Category(cp) is in {Ll, Lu, Lo, Nd, Lm, Mn, Mc}
        final UnicodeSet LetterDigits = new UnicodeSet("[[:Ll:][:Lu:][:Lo:][:Nd:][:Lm:][:Mn:][:Mc:]]").freeze();

        // B: toNFKC(toCaseFold(toNFKC(cp))) != cp
        final UnicodeSet Unstable = new UnicodeSet();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            final String s = UTF16.valueOf(i);
            final String nfkc = NFKC.transform(s);
            final String cased = CASEFOLD.transform(nfkc);
            final String full = NFKC.transform(cased);
            if (!s.equals(full)) {
                Unstable.add(i);
            }
        }
        Unstable.freeze();

        // C: Default_Ignorable_Code_Point(cp) = True or
        // White_Space(cp) = True or
        // Noncharacter_Code_Point(cp) = True
        final UnicodeSet IgnorableProperties = new UnicodeSet("[[:Default_Ignorable_Code_Point:]" +
                "[:White_Space:]" +
                "[:Noncharacter_Code_Point:]]").freeze();

        // Block(cp) is in {Combining Diacritical Marks for Symbols,
        // Musical Symbols, Ancient Greek Musical Notation}
        final UnicodeSet IgnorableBlocks = new UnicodeSet("[[:block=Combining Diacritical Marks for Symbols:]" +
                "[:block=Musical Symbols:]" +
                "[:block=Ancient Greek Musical Notation:]]").freeze();

        // E: cp is in {002D, 0030..0039, 0061..007A}
        final UnicodeSet LDH = new UnicodeSet("[\u002D\u0030-\u0039\u0061-\u007A]").freeze();

        // F: cp is in {00B7, 00DF, 0375, 03C2, 05F3, 05F4, 0640, 0660,
        // 0661, 0662, 0663, 0664, 0665, 0666, 0667, 0668,
        // 0669, 06F0, 06F1, 06F2, 06F3, 06F4, 06F5, 06F6,
        // 06F7, 06F8, 06F9, 06FD, 06FE, 07FA, 0F0B, 3007,
        // 302E, 302F, 3031, 3032, 3033, 3034, 3035, 303B,
        // 30FB}

        final UnicodeMap<Idna2008Type> Exceptions = new UnicodeMap<Idna2008Type>()
                .putAll(new UnicodeSet("[\u00DF\u03C2\u06FD\u06FE\u0F0B\u3007]"), Idna2008Type.PVALID)
                .putAll(new UnicodeSet("[\u00B7\u0375\u05F3\u05F4\u30FB\u0660-\u0669\u06F0-\u06F9]"), Idna2008Type.CONTEXTO)
                .putAll(new UnicodeSet("[\u0640\u07FA\u302E\u302F\u3031\u3032\u3033\u3034\u3035\u303B]"), Idna2008Type.DISALLOWED)
                .freeze();

        // G: cp is in {}

        final UnicodeMap<Idna2008Type> BackwardCompatible = new UnicodeMap<Idna2008Type>().freeze();

        // H: Join_Control(cp) = True

        final UnicodeSet JoinControl = new UnicodeSet("[:Join_Control:]");

        // Hangul_Syllable_Type(cp) is in {L, V, T}

        final UnicodeSet OldHangulJamo = new UnicodeSet("[[:Hangul_Syllable_Type=L:]" +
                "[:Hangul_Syllable_Type=V:]" +
                "[:Hangul_Syllable_Type=T:]]");

        // J: General_Category(cp) is in {Cn} and
        // Noncharacter_Code_Point(cp) = False

        final UnicodeSet Unassigned = new UnicodeSet("[[:Cn:]-[:Noncharacter_Code_Point:]]");

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

        
        final UnicodeMap<Idna2008Type> Incompatible = new UnicodeMap<Idna2008Type>().putAll(GRANDFATHERED_VALID, Idna2008Type.PVALID).freeze();

        IDNA2008Computed = new UnicodeMap<Idna2008Type>();

        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Idna2008Type value;
            if (Incompatible.containsKey(cp)) {
                value = Incompatible.get(cp);
            } else if (Exceptions.containsKey(cp)) {
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
            IDNA2008Computed.put(cp, value);
        }
        IDNA2008Computed.freeze();
    }

    public static Idna2008                SINGLETON = new Idna2008();

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
}
