package org.unicode.draft;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import java.io.DataInput;
import java.io.IOException;

public class UnicodeDataInput {

    private DataInput input;
    private final CompressedDataInput compressedInput = new CompressedDataInput();

    public UnicodeDataInput set(DataInput input, boolean compressed) {
        this.input = compressed ? compressedInput.set(input) : input;
        return this;
    }

    public DataInput get() {
        return input;
    }

    /**
     * Reads a UnicodeSet in the format of writeUnicodeSet.
     *
     * @param input
     * @return set read
     * @throws IOException
     */
    public UnicodeSet readUnicodeSet() throws IOException {
        final UnicodeSet result = new UnicodeSet();
        int rangeCount = input.readInt();
        int last = 0;
        while (rangeCount-- > 0) {
            final int codePoint = last = input.readInt() + last;
            final int codePointEnd = last = input.readInt() + last;
            result.addAll(codePoint, codePointEnd);
        }
        int stringCount = input.readInt();
        while (stringCount-- > 0) {
            final String s = input.readUTF();
            result.add(s);
        }
        return result;
    }

    public abstract static class ItemReader<T> {
        public abstract T read(DataInput in) throws IOException;

        public T[] readArray(DataInput input) throws IOException {
            final int valueSize = input.readInt();
            final T[] values = (T[]) new Object[valueSize];
            for (int i = 0; i < valueSize; ++i) {
                values[i] = read(input);
            }
            return values;
        }
    }

    public static class StringReader extends ItemReader<String> {
        @Override
        public String read(DataInput in) throws IOException {
            return in.readUTF();
        }
    }

    public static class StringReaderInterning extends ItemReader<String> {
        @Override
        public String read(DataInput in) throws IOException {
            return in.readUTF().intern();
        }
    }

    public <T> UnicodeMap<T> readUnicodeMap(ItemReader<T> reader) throws IOException {
        return readUnicodeMap(reader, input);
    }

    public static <T> UnicodeMap<T> readUnicodeMap(ItemReader<T> reader, DataInput dataInput)
            throws IOException {
        final UnicodeMap<T> result = new UnicodeMap<T>();

        // values
        final T[] values = reader.readArray(dataInput);
        // transitions
        final int transitionCount = dataInput.readInt();
        final int[] transitions = new int[transitionCount + 1];
        int last = 0;
        for (int i = 0; i < transitionCount; ++i) {
            transitions[i] = last = dataInput.readInt() + last;
        }
        transitions[transitionCount] = 0x110000;
        // values
        for (int i = 0; i < transitionCount; ++i) {
            final int valueIndex = dataInput.readInt();
            if (valueIndex < 0) {
                continue; // no value
            }
            final T value = values[valueIndex];
            result.putAll(transitions[i], transitions[i + 1] - 1, value);
        }
        // strings
        final int stringCount = dataInput.readInt();
        for (int i = 0; i < stringCount; ++i) {
            final String string = dataInput.readUTF();
            final int valueIndex = dataInput.readInt();
            final T value = values[valueIndex];
            result.put(string, value);
        }
        return result;
    }
}
