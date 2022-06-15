/**
 * ****************************************************************************** Copyright (C)
 * 1996-2001, International Business Machines Corporation and * others. All Rights Reserved. *
 * ******************************************************************************
 *
 * <p>$Source: /home/cvsroot/unicodetools/org/unicode/text/utility/Main.java,v $
 *
 * <p>******************************************************************************
 */
package org.unicode.text.utility;

public class Main {

    public static class CollatorStyle extends EnumBase {
        public static CollatorStyle
                ZEROED = (CollatorStyle) makeNext(new CollatorStyle(), "ZEROED"),
                SHIFTED = (CollatorStyle) makeNext(new CollatorStyle(), "SHIFTED"),
                NON_IGNORABLE = (CollatorStyle) makeNext(new CollatorStyle(), "NON_IGNORABLE");

        public CollatorStyle next() {
            return (CollatorStyle) internalNext();
        }

        private CollatorStyle() {}
    }

    public static class NormalizerType extends EnumBase {
        public static NormalizerType NFC = (NormalizerType) makeNext(new NormalizerType(), "NFC"),
                NFD = (NormalizerType) makeNext(new NormalizerType(), "NFD"),
                NFKC = (NormalizerType) makeNext(new NormalizerType(), "NFKC"),
                NFKD = (NormalizerType) makeNext(new NormalizerType(), "NFKD");

        public NormalizerType next() {
            return (NormalizerType) internalNext();
        }

        private NormalizerType() {}
    }

    public static class Length extends EnumBase {
        public static Length SHORT = (Length) makeNext(new Length(), "SHORT"),
                NORMAL = (Length) makeNext(new Length(), "NORMAL"),
                LONG = (Length) makeNext(new Length(), "LONG");

        public Length next() {
            return (Length) internalNext();
        }

        private Length() {}
    }

    public static void main(String[] args) {
        for (final String arg : args) {}
        if (true) {
            return;
        }

        for (CollatorStyle i = CollatorStyle.ZEROED; i != null; i = i.next()) {
            System.out.println(i);
        }
        for (NormalizerType i = NormalizerType.NFC; i != null; i = i.next()) {
            System.out.println(i);
        }

        final NormalizerType foo = new NormalizerType();
    }
}
