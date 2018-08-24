package org.unicode.propstest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.unicode.cldr.util.Timer;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.Binary;
import org.unicode.props.UcdPropertyValues.Block_Values;

import com.google.common.base.Objects;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.CodePointMap;
import com.ibm.icu.util.CodePointMap.Range;
import com.ibm.icu.util.CodePointTrie.Type;
import com.ibm.icu.util.CodePointTrie.ValueWidth;
import com.ibm.icu.util.MutableCodePointTrie;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.VersionInfo;

/**
 * Quick investigation for doing immutable UnicodeMap based on CodePointMap
 * @author markdavis
 */

public class TestImmutableUnicodeMap extends TestFmwk {
    IndexUnicodeProperties iup = IndexUnicodeProperties.make(VersionInfo.UNICODE_11_0);
    UnicodeMap<Age_Values> age = iup.loadEnum(UcdProperty.Age, Age_Values.class);
    UnicodeMap<Binary> whitespace = iup.loadEnum(UcdProperty.White_Space, Binary.class);
    UnicodeMap<Block_Values> block = iup.loadEnum(UcdProperty.Block, Block_Values.class);

    public static void main(String[] args) {
        new TestImmutableUnicodeMap().run(args);
    }

    public void testGet() {
        UnicodeCPMap<Age_Values> cpMap = new UnicodeCPMap<>(age, Type.FAST);
        for (EntryRange<Age_Values> range : age.entryRanges()) {
            Age_Values expected = range.value;
            if (range.codepoint < 0) {
                Age_Values actual = cpMap.get(range.string);
                check(range.string, expected, actual);
            } else for (int cp = range.codepoint; cp <= range.codepointEnd; ++cp) {
                check(cp, expected, (Object) cpMap.get(cp));
            }
        }
    }
    
    private <T> void check(String source, T expected, T actual) {
        if (!Objects.equal(expected, actual)) {
            errln("String failure: «" + source + "» + expected:" + expected + ", actual: " + actual);
        }
    }

    private <T> void check(int source, T expected, T actual) {
        if (!Objects.equal(expected, actual)) {
            errln("String failure: «" + source + "» + expected:" + expected + ", actual: " + actual);
        }
    }



    public void testSpeed() {
        checkTime(UcdProperty.Age, age);
        checkTime(UcdProperty.White_Space, whitespace);
        checkTime(UcdProperty.Block, block);
    }

    private static <T> void checkTime(Object title, UnicodeMap<T> uMap) {
        System.out.println("\nChecking Time for " + title
                + ", values: " + uMap.values().size()
                + ", ranges: " + uMap.getRangeCount());


        {
            int iterations = 200;
            Timer t = new Timer();
            for (int i = iterations; i > 0; --i) for (int cp = 0; cp < 0x110000; ++cp) {
                T v = uMap.get(cp);
            }
            t.stop();
            System.out.println("UnicodeMap.get: \t\t" + t.toString(iterations));

            for (Type type : Arrays.asList(Type.SMALL, Type.FAST)) {
                UnicodeCPMap<T> temp = new UnicodeCPMap<>(uMap, type);
                CodePointMap cpMap = temp.cpData;
                Timer t2 = new Timer();
                for (int i = iterations; i > 0; --i) for (int cp = 0; cp < 0x110000; ++cp) {
                    int v = cpMap.get(cp);
                }
                t2.stop();
                System.out.println(type + " CodePointMap.get:\t\t"
                        + t2.toString(iterations, t.getDuration())
                        + "\twidth: " + temp.valueWidth);
            }
        }
        {
            int iterations = 2000;
            Timer t = new Timer();
            int rangeCount = 0;
            for (EntryRange<T> range : uMap.entryRanges()) {
                ++rangeCount;
            }
            for (int i = iterations; i > 0; --i) for (EntryRange<T> range : uMap.entryRanges()) {
                T v = range.value;
            }
            t.stop();
            System.out.println("UnicodeMap.entryRanges:\t\t" + t.toString());

            for (Type type : Arrays.asList(Type.SMALL, Type.FAST)) {
                CodePointMap cpMap = new UnicodeCPMap<>(uMap, type).cpData;
                rangeCount = 0;
                EntryRange<Age_Values> range = new EntryRange<Age_Values>();
                Range cpMapRange = new Range();
                Output<Age_Values> value = new Output<>();

                for (int cp = 0; cp <= 0x01FFFF; cp = cpMapRange.getEnd()+1) {
                    boolean b = cpMap.getRange(cp, null, cpMapRange);
                    ++rangeCount;
                }

                Timer t2 = new Timer();
                for (int i = iterations; i > 0; --i) for (int cp = 0; cp <= 0x01FFFF; cp = cpMapRange.getEnd()+1) {
                    boolean b = cpMap.getRange(cp, null, cpMapRange);
                    int v = cpMapRange.getValue();

                    //                if (!cpMap.getRange(cp, range, range2)) {
                    //                    break;
                    //                }
                    //                cp = range.codepointEnd+1;
                    //                Age_Values v = range.value;
                }
                t2.stop();
                System.out.println(type + " CodePointMap.getRange:\t"
                        + t2.toString(t) 
                        + "\trangeCount:\t" + rangeCount);
            }
        }
    }


    static class UnicodeCPMap<T> {
        final CodePointMap cpData;
        final Map<String, T> stringData = new LinkedHashMap<>();
        final T[] intToData;
        final ValueWidth valueWidth;


        public UnicodeCPMap(UnicodeMap<T> source, Type type) {
            super();
            Map<T,Integer> valueMap = new LinkedHashMap<>();
            MutableCodePointTrie builder = new MutableCodePointTrie(-1, -1);
            for (EntryRange<T> range : source.entryRanges()) {
                if (range.string != null) {
                    stringData.put(range.string, range.value);
                } else {
                    Integer intValue = valueMap.get(range.value);
                    if (intValue == null) {
                        intValue = valueMap.size();
                        valueMap.put(range.value, intValue);
                    }
                    builder.setRange(range.codepoint, range.codepointEnd, intValue);
                }
            }
            valueWidth = valueMap.size() < 0x100 ? ValueWidth.BITS_8 : valueMap.size() < 0x10000 ? ValueWidth.BITS_16 : ValueWidth.BITS_32;
            this.cpData = builder.buildImmutable(type, valueWidth);
            T[] temp = (T[]) new Object[valueMap.size()];
            this.intToData = valueMap.keySet().toArray(temp);
        }

        public T get(int cp) {
            int result = cpData.get(cp);
            return result < 0 ? null : intToData[result];
        }

        public T get(CharSequence str) {
            int cp = str == null ? Integer.MAX_VALUE : UnicodeSet.getSingleCodePoint(str);
            return cp != Integer.MAX_VALUE ? get(cp) : stringData.get(str);
        }

        public int getRange(int start, Output<T> rangeValue, Range range2) {
            cpData.getRange(start, null, range2);
            rangeValue.value = range2.getValue() < 0 ? null : intToData[range2.getValue()];
            return range2.getEnd() + 1;
        }
    }

}
