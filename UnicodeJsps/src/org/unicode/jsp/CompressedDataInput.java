/**
 * 
 */
package org.unicode.jsp;

import java.io.DataInput;
import java.io.IOException;

final class CompressedDataInput implements DataInput {

    private DataInput in;

    public CompressedDataInput set(DataInput in) {
        this.in = in;
        return this;
    }

    /**
     * Read long using readUnsignedLong. The bottom bit is the sign. If the number was negative, the value is inverted (~).
     */
    @Override
    public long readLong() throws IOException {
        long result = readUnsignedLong();
        // shift the signed bit up
        final boolean negative = (result & 1) != 0;
        result >>>= 1;
        if (negative) {
            result = ~result;
        }
        return result;
    }

    /**
     * Read a long as a series of 7-bits, with the last one having the top bit on.
     * 
     * @throws IOException
     */
    public long readUnsignedLong() throws IOException {
        long result = 0;
        int shift = 0;
        while (true) {
            int byteValue = in.readByte();
            if ((byte) byteValue >= 0) {
                result |= ((long)byteValue << shift);
                shift += 7;
            } else {
                byteValue &= 0x7F;
                result |= ((long)byteValue << shift);
                return result;
            }
        }
    }

    private final StringBuilder toAppendTo = new StringBuilder();

    @Override
    public final String readUTF() throws IOException {
        synchronized (toAppendTo) {
            return readUTF(toAppendTo);
        }
    }

    public String readUTF(StringBuilder toAppendTo) throws IOException {
        toAppendTo.setLength(0);
        int last = 0x40;
        for (int length = (int) readUnsignedLong(); length > 0; --length) {
            final int cp = last + (int) readLong();
            last = (cp & ~0x3F) | 0x40; // set
            toAppendTo.appendCodePoint(cp);
        }
        return toAppendTo.toString();
    }

    @Override
    public final double readDouble() throws IOException {
        //    long bits = 0;
        //    long exponent = readLong() + 1024;
        //    bits |= exponent << 52;
        //
        //    long mantissa = 0;
        //    // we now have the mantissa in the top. Peel off the 7-bit chunks from the top.
        //    while (true) {
        //      long b = in.readByte();
        //      mantissa |= b << 57;
        //      if (b >= 0) {
        //        break;
        //      }
        //      mantissa >>>= 7;
        //    }
        //    boolean negative = (mantissa & 0x0010000000000000L) != 0;
        //    mantissa >>>= 11;
        //
        //    if (negative) {
        //      bits |= 0x8000000000000000L;
        //    }
        //    return Double.longBitsToDouble(bits);
        return in.readDouble();
    }

    @Override
    public final short readShort() throws IOException {
        return (short) readLong();
    }

    @Override
    public final int readUnsignedShort() throws IOException {
        return (int) readUnsignedLong();
    }

    @Override
    public final char readChar() throws IOException {
        return (char) readUnsignedLong();
    }

    @Override
    public final int readInt() throws IOException {
        return (int) readLong();
    }

    @Override
    public final float readFloat() throws IOException {
        return in.readFloat();
    }

    // DELEGATED

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        in.readFully(b, off, len);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return in.skipBytes(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return in.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return in.readByte();
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        in.readFully(b);
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return in.readUnsignedByte();
    }

    @Override
    public String readLine() throws IOException {
        return in.readLine();
    }
}