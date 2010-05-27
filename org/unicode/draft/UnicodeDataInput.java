package org.unicode.draft;

import java.io.DataInput;
import java.io.IOException;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class UnicodeDataInput {
  
  private DataInput input;
  private CompressedDataInput compressedInput = new CompressedDataInput();
  
  public UnicodeDataInput set(DataInput input, boolean compressed) {
    this.input = compressed ? compressedInput.set(input) : input;
    return this;
  }
  
  public DataInput get() {
    return input;
  }
  
  /**
   * Reads a UnicodeSet in the format of writeUnicodeSet.
   * @param input
   * @return set read
   * @throws IOException
   */
  public UnicodeSet readUnicodeSet() throws IOException {
    UnicodeSet result = new UnicodeSet();
    int rangeCount = input.readInt();
    int last = 0;
    while (rangeCount-- > 0) {
      int codePoint = last = input.readInt() + last;
      int codePointEnd = last = input.readInt() + last;
      result.addAll(codePoint, codePointEnd);
    }
    int stringCount = input.readInt();
    while (stringCount-- > 0) {
      String s = input.readUTF();
      result.add(s);
    }
    return result;
  }

  public static abstract class ItemReader<T> {
    public abstract T read(DataInput in) throws IOException;
    
    public T[] readArray(DataInput input) throws IOException {
      int valueSize = input.readInt();
      T[] values = (T[]) new Object[valueSize];
      for (int i = 0; i < valueSize; ++i) {
        values[i] = read(input);
      }
      return values;
    }
  }
  
  public static class StringReader extends ItemReader<String> {
    public String read(DataInput in) throws IOException {
      return in.readUTF();
    }
  }
  
  public <T> UnicodeMap<T> readUnicodeMap(ItemReader<T> reader) throws IOException {
    UnicodeMap<T> result = new UnicodeMap<T>();

    // values
    T[] values = reader.readArray(input);
    // transitions
    int transitionCount = input.readInt();
    int[] transitions = new int[transitionCount+1];
    int last = 0;
    for (int i = 0; i < transitionCount; ++i) {
      transitions[i] = last = input.readInt() + last;
    }
    transitions[transitionCount] = 0x110000;
    // values
    for (int i = 0; i < transitionCount; ++i) {
      int valueIndex = input.readInt();
      if (valueIndex < 0) continue; // no value
      T value = values[valueIndex];
      result.putAll(transitions[i], transitions[i+1]-1, value);
    }
    // strings
    int stringCount = input.readInt();
    for (int i = 0; i < stringCount; ++i) {
      String string = input.readUTF();
      int valueIndex = input.readInt();
      T value = values[valueIndex];
      result.put(string, value);
    }
    return result;
  }

}
