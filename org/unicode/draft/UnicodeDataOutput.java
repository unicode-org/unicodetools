package org.unicode.draft;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class UnicodeDataOutput {
  
  private DataOutput output;
  private CompressedDataOutput compressedInput = new CompressedDataOutput();
  
  public UnicodeDataOutput set(DataOutput output, boolean compressed) {
    this.output = compressed ? compressedInput.set(output) : output;
    return this;
  }
  
  public DataOutput get() {
    return output;
  }

  /**
   * Writes a UnicodeSet as: the count of ranges (int), each pair of ranges (int), a count of strings, and then the strings.
   * @param output
   * @param toWrite
   * @throws IOException
   */
  public  void writeUnicodeSet(UnicodeSet toWrite) throws IOException {
    int rangeCount = toWrite.getRangeCount();
    int last = 0;
    output.writeInt(rangeCount);
    int count = 0;
    boolean firstString = true;
    for (UnicodeSetIterator it = new UnicodeSetIterator(toWrite); it.nextRange();) {
      if (it.codepoint != it.IS_STRING) {
        output.writeInt(it.codepoint - last);
        last = it.codepoint;
        output.writeInt(it.codepointEnd - last);
        last = it.codepointEnd;
        count += it.codepointEnd - it.codepoint + 1;
      } else {
        if (firstString) {
          output.writeInt(toWrite.size() - count); // write terminator. negatives will not occur above.
          firstString = false;
        }
        output.writeUTF(it.string);
      }
    }
    if (firstString) { // if there were no strings
      output.writeInt(0);
    }
  }
  
  public static abstract class ItemWriter<T> {
    public abstract void write(DataOutput out, T item) throws IOException;
    
    /**
     * Can be overridden for efficiency. The collection is actually a set.
     * @param output
     * @param values
     * @return
     * @throws IOException
     */
    public Map<T, Integer> writeArray(DataOutput output, Collection<T> values) throws IOException {
      Map<T,Integer> valuesToInts = new LinkedHashMap<T,Integer>();
      int index = 0;
      output.writeInt(values.size());
      for (T value : values) {
        valuesToInts.put(value, index++);
        write(output, value);
      }
      return valuesToInts;
    }
  }
  
  public static class StringWriter extends ItemWriter<String> {
    public void write(DataOutput out, String item) throws IOException {
      out.writeUTF(item);
    }
  }
  
  /**
   * Write a UnicodeMap, with format:<br>
   * count of values, and list of values<br>
   * count of transitions, list of transitions<br>
   * list of corresponding value indexes (same count - 1)<br>
   * count of strings, list of string/valueIndex pairs<br>
   * The transitions and corresponding value indexes are written separately to allow compression of the former.
   * 
   * @param <T>
   * @param output
   * @param toWrite
   * @param writer
   * @throws IOException
   */
  public  <T> void writeUnicodeMap(UnicodeMap<T> toWrite, ItemWriter<T> writer) throws IOException {
    // values
    Collection<T> values = toWrite.getAvailableValues();
    Map<T, Integer> valuesToInts = writer.writeArray(output, values);
    // transitions
    int rangeCount = toWrite.getRangeCount();
    output.writeInt(rangeCount);
    int last = 0;
    for (int i = 0; i < rangeCount; ++i) {
      int rangeStart = toWrite.getRangeStart(i);
      output.writeInt(rangeStart - last);
      last = rangeStart;
    }
    // value indexes
    for (int i = 0; i < rangeCount; ++i) {
      T value = toWrite.getRangeValue(i);
      Integer valueIndex = valuesToInts.get(value);
      output.writeInt(valueIndex == null ? -1 : valueIndex);
    }
    // strings
    Set<String> strings = toWrite.getNonRangeStrings();
    if (strings == null) {
      output.writeInt(0);
      return;
    }
    output.writeInt(strings.size());
    for (String string : strings) {
      output.writeUTF(string);
      T value = toWrite.get(string);
      Integer valueIndex = valuesToInts.get(value);
      output.writeInt(valueIndex == null ? -1 : valueIndex);
    }
  }
}
