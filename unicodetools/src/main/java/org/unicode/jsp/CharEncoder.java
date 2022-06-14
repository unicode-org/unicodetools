/** */
package org.unicode.jsp;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

public class CharEncoder {

    private final boolean verifyRoundtrip;
    private final boolean justCheck;

    private final CharsetEncoder encoder;
    private final CharsetDecoder decoder;

    private final char[] chars = new char[2];
    private final char[] returnChars = new char[2];

    private final CharBuffer charBuffer = CharBuffer.wrap(chars);
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(5);
    private final CharBuffer returnCharBuffer = CharBuffer.wrap(returnChars);

    /**
     * @param charset
     * @param verifyRoundtrip
     * @param justCheck
     */
    public CharEncoder(Charset charset, boolean verifyRoundtrip, boolean justCheck) {
        this.verifyRoundtrip = verifyRoundtrip;
        this.justCheck = justCheck;
        encoder =
                charset.newEncoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT);
        decoder =
                charset.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT);
    }

    public boolean isVerifyRoundtrip() {
        return verifyRoundtrip;
    }

    /**
     * Convert the code point. Return -1 if fails. If justCheck, then return 1 if success. Otherwise
     * return length of the bytes converted, and fill in the destination. In either case, if
     * isVerifyRoundtrip() then check that the roundtrip works.
     *
     * @param codepoint
     * @param destination
     * @param offset
     * @return
     * @throws BufferUnderflowException if the supplied destination is too small.
     */
    public int getValue(int codepoint, byte[] destination, int offset) {
        int byteLen;
        try {
            final int len = Character.toChars(codepoint, chars, 0);
            charBuffer.limit(len);
            charBuffer.position(0);
            byteBuffer.clear();
            final CoderResult encodeResult = encoder.encode(charBuffer, byteBuffer, true);
            if (encodeResult.isError()) {
                return -1;
            }
            if (verifyRoundtrip) {
                byteBuffer.flip();
                returnCharBuffer.clear();
                final CoderResult decodeResult = decoder.decode(byteBuffer, returnCharBuffer, true);
                if (decodeResult.isError()) {
                    return -1;
                }
                final int len2 = returnCharBuffer.position();
                if (len != len2) {
                    return -1;
                }
                if (chars[0] != returnChars[0]) {
                    return -1;
                }
                if (len > 1 && chars[1] != returnChars[1]) {
                    return -1;
                }
            }
            if (justCheck) {
                return 1;
            }
            byteBuffer.flip();
            byteLen = byteBuffer.limit();
            byteBuffer.get(destination, offset, byteLen);
            return byteLen;
        } catch (final Exception e) {
            if (e instanceof BufferUnderflowException) {
                throw (BufferUnderflowException) e;
            }
            return -1;
        }
    }
}
