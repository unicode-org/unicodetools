/** */
package org.unicode.draft;

import java.io.DataOutput;
import java.io.IOException;

public final class CompressedDataOutput implements DataOutput {

    private DataOutput out;

    public CompressedDataOutput set(DataOutput out) {
        this.out = out;
        return this;
    }

    @Override
    public void writeLong(long longValue) throws IOException {
        final boolean negative = longValue < 0;
        if (negative) {
            longValue = ~longValue;
        }
        longValue <<= 1;
        if (negative) {
            longValue |= 1;
        }
        writeUnsignedLong(longValue);
    }

    /**
     * Write a long as a series of 7-bits, with the last one having the top bit on.
     *
     * @param longValue
     * @throws IOException
     */
    public void writeUnsignedLong(long longValue) throws IOException {
        while (true) {
            final int bottom = 0x7F & (int) longValue;
            longValue >>>= 7;
            if (longValue == 0) {
                out.write(bottom | 0x80); // write byte
                return;
            } else {
                out.write(bottom); // write byte
            }
        }
    }

    @Override
    public void writeUTF(String string) throws IOException {
        writeUnsignedLong(Character.codePointCount(string, 0, string.length()));
        int last = 0x40;
        int cp;
        for (int i = 0; i < string.length(); i += Character.charCount(cp)) {
            cp = Character.codePointAt(string, i);
            writeLong(cp - last);
            last = (cp & ~0x3F) | 0x40; // set to middle of range
        }
    }

    /*
     * Bit 63 (the bit that is selected by the mask
     * <code>0x8000000000000000L</code>) represents the sign of the
     * floating-point number. Bits
     * 62-52 (the bits that are selected by the mask
     * <code>0x7ff0000000000000L</code>) represent the exponent. Bits 51-0
     * (the bits that are selected by the mask
     * <code>0x000fffffffffffffL</code>) represent the significand
     * (sometimes called the mantissa) of the floating-point number.
     */
    @Override
    public final void writeDouble(double v) throws IOException {
        //    long bits = Double.doubleToRawLongBits(v);
        //    int exponent = ((int) (bits >>> 52) & 0x7FF) - 1023;
        //    writeLong(exponent);
        //    long mantissa = bits & 0x000fffffffffffffL;
        //    if (v < 0) {
        //      mantissa |= 0x0010000000000000L;
        //    }
        //    mantissa <<= 11;
        //    // we now have the mantissa in the top. Peel off the 7-bit chunks from the top.
        //    while (true) {
        //      long b = mantissa >>> 57;
        //      mantissa = mantissa << 7;
        //      if (mantissa != 0) {
        //        out.write((int)b & 0x7F);
        //        break;
        //      } else {
        //        out.write(0x80 | (int)b);
        //        break;
        //      }
        //    }
        //    writeLong(bits);
        out.writeDouble(v);
    }

    @Override
    public final void writeShort(int v) throws IOException {
        writeLong(v);
    }

    @Override
    public final void writeChar(int v) throws IOException {
        writeUnsignedLong(v);
    }

    /**
     * Doesn't do the intracharacter compression that writeUTF does, because the caller may assume
     * that it is equivalent to calling writeChar multiple times.
     */
    @Override
    public final void writeChars(String s) throws IOException {
        for (int i = 0; i < s.length(); ++i) {
            writeUnsignedLong(s.charAt(i));
        }
    }

    @Override
    public final void writeInt(int v) throws IOException {
        writeLong(v);
    }

    @Override
    public final void writeFloat(float v) throws IOException {
        out.writeFloat(v);
    }

    // DELEGATED

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        out.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        out.writeByte(v);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        out.writeBytes(s);
    }
}
