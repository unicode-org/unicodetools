package org.unicode.draft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.props.ICUPropertyFactory;
import org.unicode.cldr.util.props.UnicodeProperty;
import org.unicode.draft.UnicodeDataInput.StringReader;
import org.unicode.draft.UnicodeDataOutput.StringWriter;
import org.unicode.unittest.TestFmwkMinusMinus;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class TestCompressed extends TestFmwkMinusMinus {

    @Test
    public void TestAString() throws IOException {

        final String[] strings = {
                "Unicode - это уникальный код для любого символа",
                "ユニコードは、すべての文字に固有の番号を付与します",
        "Unicode spécifie un numéro unique pour chaque caractère"};
        for (final String string : strings) {
            checkString(string);
        }
    }

    public void checkString(String string) throws IOException {
        logln(string.length() + " " + string);
        final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(outBytes);
        final CompressedDataOutput output = new CompressedDataOutput();
        output.set(out).writeUTF(string);
        out.close();
        byte[] bytes = outBytes.toByteArray();
        logln("Byte length: " + showBytes(bytes, 20));
        final ByteArrayInputStream inBytes = new ByteArrayInputStream(bytes);
        final DataInputStream in = new DataInputStream(inBytes);
        final CompressedDataInput input = new CompressedDataInput().set(in);
        input.readUTF();
        final ByteArrayOutputStream outBytes2 = new ByteArrayOutputStream();
        final DataOutputStream out2 = new DataOutputStream(outBytes2);
        out2.writeUTF(string);
        out2.close();
        bytes = outBytes2.toByteArray();
        logln("UTF8 Byte length: " + showBytes(bytes, 20));
    }

    enum Compressed {compressed, uncompressed}

    @Test
    public void TestSetStreamer () throws IOException {
        final String[] tests = {"[:Ll:]", "[a-c]", "[]", "[a-ce-fq-z]", "[{abc}]", "[a-cmq-z{abc}]"};
        final UnicodeDataOutput unicodeDataOutput = new UnicodeDataOutput();
        final UnicodeDataInput unicodeDataInput = new UnicodeDataInput();
        for (final String test : tests) {
            for (final boolean compressed : new boolean[] {false, true}) {
                final UnicodeSet item = new UnicodeSet(test);
                final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();

                final DataOutputStream out = new DataOutputStream(outBytes);
                unicodeDataOutput.set(out, compressed).writeUnicodeSet(item);
                out.close();

                final byte[] bytes = outBytes.toByteArray();
                final ByteArrayInputStream inBytes = new ByteArrayInputStream(bytes);

                final DataInputStream in = new DataInputStream(inBytes);
                final UnicodeSet newItem = unicodeDataInput.set(in, compressed).readUnicodeSet();
                checkAtEnd("Stream not read completely: " + test, in);
                in.close();
                assertEquals((compressed ? "Compressed" : "Uncompressed") +
                        " Original: " + test + " => " + showBytes(bytes, 20), item, newItem);
                final byte[] zipped = zip(bytes);
                logln("\tGZipped:\t" + showBytes(zipped, 20));

                //                ByteArrayOutputStream outBytes2 = new ByteArrayOutputStream();
                //                DataOutputStream out2 = new DataOutputStream(outBytes2);
                //                writeSimpleUnicodeSetOutput(item, out2);
                //                out2.close();
                //                bytes = outBytes2.toByteArray();
                //                logln("Plain Byte length: " + showBytes(bytes, 20));
            }
        }
    }

    byte[] zip(byte[] source) throws IOException {
        final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        final GZIPOutputStream foo = new GZIPOutputStream(outBytes);
        try {
            foo.write(source);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        foo.close();
        final byte[] bytes = outBytes.toByteArray();
        return bytes;
    }

    void writeSimpleUnicodeSetOutput(UnicodeSet set, DataOutputStream out) throws IOException {
        out.write(set.getRangeCount());
        for (final UnicodeSetIterator it = new UnicodeSetIterator(set); it.nextRange();) {
            /*          if (it.codepoint == it.IS_STRING) {
            item.put(it.string, values[count++]);
          } else {
             */
            out.write(it.codepoint);
            out.write(it.codepointEnd + 1);
        }
    }


    @Test
    public void TestMapStreamer () throws IOException {
        final String[] tests = {"[a-c]", "[]", "[a-ce-fq-zA-BD-F]", "[{abc}]", "[a-cmq-z{abc}{huh?}]"};
        final String[] values = {"The", "Quick", "Brown"};
        final StringReader stringReader = new StringReader();
        final StringWriter stringWriter = new StringWriter();
        final UnicodeDataOutput unicodeDataOutput = new UnicodeDataOutput();
        final UnicodeDataInput unicodeDataInput = new UnicodeDataInput();

        for (final String test : tests) {
            // make a testing map
            final UnicodeSet set = new UnicodeSet(test);
            final UnicodeMap<String> item = new UnicodeMap();
            int count = 0;
            for (final UnicodeSetIterator it = new UnicodeSetIterator(set); it.nextRange();) {
                if (it.codepoint == UnicodeSetIterator.IS_STRING) {
                    item.put(it.string, values[count++]);
                } else {
                    item.putAll(it.codepoint, it.codepointEnd, values[count++]);
                }
                if (count >= values.length){
                    count = 0;
                }
            }

            verifyMap(stringReader, stringWriter, unicodeDataOutput, unicodeDataInput, test, item);

        }
    }

    int total;
    int totalCompressed;

    private void verifyMap(StringReader stringReader, StringWriter stringWriter, UnicodeDataOutput unicodeDataOutput, UnicodeDataInput unicodeDataInput,
            String test, UnicodeMap<String> item) throws IOException {
        int totalLocal = 0;
        for (final boolean compressed : new boolean[] {false, true}) {
            final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            final DataOutputStream out = new DataOutputStream(outBytes);
            unicodeDataOutput.set(out, compressed).writeUnicodeMap(item, stringWriter);
            out.close();
            final byte[] bytes = outBytes.toByteArray();
            if (compressed) {
                totalCompressed += bytes.length;
            } else {
                total += bytes.length;
                totalLocal = bytes.length;
            }
            final ByteArrayInputStream inBytes = new ByteArrayInputStream(bytes);
            final DataInputStream in = new DataInputStream(inBytes);
            final UnicodeMap<String> newItem = unicodeDataInput.set(in, compressed).readUnicodeMap(stringReader);
            checkAtEnd("Stream not read completely: " + test, in);
            in.close();
            final String comp = compressed ? "\t" + percent.format(bytes.length/(double)totalLocal - 1) : "";
            if (!item.equals(newItem)) {
                errln("Original: " + test + " =>\t" + showBytes(bytes, 20) + comp + ":\t" + item + "\t!=\t" + newItem);
            } else {
                logln("Original: " + test + " =>\t" + showBytes(bytes, 20) + comp);
            }
        }
    }


    private void checkAtEnd(String msg, DataInput in) throws IOException {
        try {
            final byte nextByte = in.readByte();
            errln(msg);
        } catch (final EOFException e) {
            // ok
        }
    }

    @Test
    public void TestXBasic() throws IOException {
        final Object[] tests =
            {
                "a",
                "Mark Davis",
                "abc", "xyz", "\uFFFF\u0000", "\ud800\udc00\ud800\udc02",
                "Εκτός αυτού",
                "ユニコードとは何か",
                "\ud800\udc00",
                "abc",
                0,
                -1,
                1,
                100,
                -100,
                -1000,
                1000,
                Byte.MAX_VALUE, Byte.MIN_VALUE,
                Short.MAX_VALUE, Byte.MIN_VALUE,
                Character.MAX_VALUE, Character.MIN_VALUE,
                Integer.MAX_VALUE, Integer.MIN_VALUE,
                Long.MAX_VALUE, Long.MIN_VALUE,
                1.0,
                -5.0,
                0.0,
                0.0d,
                1.0d,
                -1.0,
                -1.0d,
                Float.MIN_VALUE, Float.MAX_VALUE,
                Double.MIN_VALUE, Double.MAX_VALUE,
                Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
            };
        for (final Object item : tests) {
            checkObject(item);
        }
    }

    static final DecimalFormat nf = (DecimalFormat) NumberFormat.getIntegerInstance();
    static final DecimalFormat percent = (DecimalFormat) NumberFormat.getPercentInstance();
    static {
        percent.setPositivePrefix("+");
    }

    private void checkObject(Object item) throws IOException {
        final ByteArrayOutputStream oldOutBytes = new ByteArrayOutputStream();
        final DataOutputStream oldOut = new DataOutputStream(oldOutBytes);
        writeObject(item, oldOut);
        oldOut.close();
        final byte[] oldBytes = oldOutBytes.toByteArray();

        final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(outBytes);
        final CompressedDataOutput compressed = new CompressedDataOutput().set(out);
        writeObject(item, compressed);
        out.close();
        final byte[] bytes = outBytes.toByteArray();
        final ByteArrayInputStream inBytes = new ByteArrayInputStream(bytes);
        final DataInputStream in = new DataInputStream(inBytes);
        final CompressedDataInput compressedIn = new CompressedDataInput().set(in);
        final Object newItem = readObject(compressedIn, item);
        checkAtEnd("Stream not read completely: " + item, in);
        in.close();

        if (item.equals(newItem)) {
            logln(item + "\t" + item.getClass().getName()
                    + "\t" + percent.format(bytes.length/(double)oldBytes.length - 1) + "\t" + showBytes(bytes, 20) + "\tOld\t" + showBytes(oldBytes, 20)
                    );
        } else {
            errln("No round trip "
                    + "\t" + item + "\t" + item.getClass().getName()
                    + "\t" + percent.format(bytes.length/(double)oldBytes.length - 1) + "\t" + showBytes(bytes, 20) + "\tOld\t" + showBytes(oldBytes, 20)
                    + "\t" + newItem + "\t" + newItem.getClass().getName());
        }
    }

    private Object readObject(CompressedDataInput in, Object item) throws IOException {
        if (item instanceof Byte) {
            return in.readByte();
        } else if (item instanceof Short) {
            return in.readShort();
        } else if (item instanceof Character) {
            return in.readChar();
        } else if (item instanceof Integer) {
            return in.readInt();
        } else if (item instanceof Long) {
            return in.readLong();
        } else if (item instanceof Float) {
            return in.readFloat();
        } else if (item instanceof Double) {
            return in.readDouble();
        } else if (item instanceof CharSequence) {
            return in.readUTF();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private String showBytes(byte[] bytes, int limit) {
        final StringBuilder result = new StringBuilder();
        result.append('[').append(nf.format(bytes.length)).append(']');
        for (final byte byteValue : bytes) {
            //      if (result.length() != 0) {
            result.append(' ');
            //      }
            result.append(Utility.hex(byteValue & 0xFF, 2));
            if (limit-- < 0) {
                result.append("...");
                break;
            }
        }
        return result.toString();
    }

    private void writeObject(Object item, DataOutput out) throws IOException {
        if (item instanceof Byte) {
            out.writeByte((Byte)item);
        } else if (item instanceof Short) {
            out.writeShort((Short)item);
        } else if (item instanceof Character) {
            out.writeChar((Character)item);
        } else if (item instanceof Integer) {
            out.writeInt((Integer)item);
        } else if (item instanceof Long) {
            out.writeLong((Long)item);
        } else if (item instanceof Float) {
            out.writeFloat((Float)item);
        } else if (item instanceof Double) {
            out.writeDouble((Double)item);
        } else if (item instanceof CharSequence) {
            out.writeUTF(item.toString());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private final Map<String,UnicodeMap<String>> cache = new LinkedHashMap();

    /**
     * Do this just to isolate the time.
     * @throws IOException
     */
    @Test
    public void TestUnicodePropAccess () throws IOException {
        final UnicodeProperty.Factory f = ICUPropertyFactory.make();
        for (final String property : (Iterable<String>) f.getAvailableNames()) {
            final UnicodeProperty prop = f.getProperty(property);
            if (prop.isType(UnicodeProperty.STRING_OR_MISC_MASK)) {
                continue;
            }
            final UnicodeMap<String> map = prop.getUnicodeMap();
            cache.put(property, map);
        }
    }

    @Test
    public void TestUnicodeProperties () throws IOException {
        final StringReader stringReader = new StringReader();
        final StringWriter stringWriter = new StringWriter();
        final UnicodeDataOutput unicodeDataOutput = new UnicodeDataOutput();
        final UnicodeDataInput unicodeDataInput = new UnicodeDataInput();

        for (final Entry<String, UnicodeMap<String>> propertyPair : cache.entrySet()) {
            final String property = propertyPair.getKey();
            final UnicodeMap<String> map = propertyPair.getValue();
            verifyMap(stringReader, stringWriter, unicodeDataOutput, unicodeDataInput, property, map);
        }
        logln("Total:\t" + total);
        logln("Total-Compressed:\t" + nf.format(totalCompressed) + "\t" + percent.format(totalCompressed/(double)total - 1));
    }
}
