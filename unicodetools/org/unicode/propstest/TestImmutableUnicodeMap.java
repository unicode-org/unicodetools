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
import org.unicode.text.utility.Utility;

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
    static final IndexUnicodeProperties iup = IndexUnicodeProperties.make(VersionInfo.UNICODE_11_0);
    static final UnicodeMap<Age_Values> AGE_PROP = iup.loadEnum(UcdProperty.Age, Age_Values.class);
    static final UnicodeMap<Binary> WHITESPACE_PROP = iup.loadEnum(UcdProperty.White_Space, Binary.class);
    static final UnicodeMap<Block_Values> BLOCK_PROP = iup.loadEnum(UcdProperty.Block, Block_Values.class);

    public static void main(String[] args) {
        new TestImmutableUnicodeMap().run(args);
    }

    public void testGet() {
        UnicodeCPMap<Age_Values> cpMap = new UnicodeCPMap<>(AGE_PROP, Type.FAST);
        for (EntryRange<Age_Values> range : AGE_PROP.entryRanges()) {
            Age_Values expected = range.value;
            if (range.codepoint < 0) {
                Age_Values actual = cpMap.get(range.string);
                check(range.string, expected, actual);
            } else for (int cp = range.codepoint; cp <= range.codepointEnd; ++cp) {
                check(cp, expected, (Object) cpMap.get(cp));
            }
        }
    }
    
    public void testGetRange() {
        checkGetRange(UcdProperty.Age, AGE_PROP, Type.FAST);
        checkGetRange(UcdProperty.Age, AGE_PROP, Type.SMALL);
        checkGetRange(UcdProperty.White_Space, WHITESPACE_PROP, Type.FAST);
        checkGetRange(UcdProperty.White_Space, WHITESPACE_PROP, Type.SMALL);
    }
    
    private <T> void checkGetRange(UcdProperty prop, UnicodeMap<T> umap, Type type) {
        UnicodeCPMap<T> cpMap = new UnicodeCPMap<>(umap, type);
        Range cpRange = new Range();
        for (EntryRange<T> range : umap.entryRanges()) {
            T expected = range.value;
            if (range.codepoint >= 0) {
                boolean actual = cpMap.cpData.getRange(range.codepoint, null, cpRange);
                assertEqualHex(prop + ", " + type + ", Range Start: ", range.codepoint, cpRange.getStart());
                assertEqualHex(prop + ", " + type + ", Range End: ", range.codepointEnd, cpRange.getEnd());
                int cpIntValue = cpRange.getValue();
                T cpAgeValue = cpIntValue < 0 ? null : cpMap.intToData[cpIntValue];
                assertEquals(prop + ", " + type + ", Range Value: ", expected, cpAgeValue);
            }
        }
    }

    public void testRanges() {
        // set up the data
        LinkedHashMap outputValueMap = new LinkedHashMap<>();
        MutableCodePointTrie builder = UnicodeCPMap.fromUnicodeMap(AGE_PROP, outputValueMap, null);
        ValueWidth valueWidth = UnicodeCPMap.getValueWidth(outputValueMap.size()+1); // leave room for -1. Would be nice utility
        System.out.println("\nSize: " + outputValueMap.size() + ", ValueWidth: " + valueWidth);
        
        // create two cpMaps from the same builder
        CodePointMap cpMapFast = builder.buildImmutable(Type.FAST, valueWidth);
        CodePointMap cpMapSmall = builder.buildImmutable(Type.SMALL, valueWidth);
        
        // now check identity of all three
        assertCodePointMapEquals("builder vs fast", builder, cpMapFast);
        assertCodePointMapEquals("builder vs small", builder, cpMapSmall);
        assertCodePointMapEquals("fast vs slow", cpMapFast, cpMapSmall);
    }
    
    private void assertCodePointMapEquals(String message, CodePointMap cpMap1, CodePointMap cpMap2) {
        // First by range
        Range cpMapRange1 = new Range();
        Range cpMapRange2 = new Range();
        boolean ok = true;
        for (int cp = 0; ok && cp <= 0x01FFFF; cp = cpMapRange1.getEnd()+1) {
            boolean b1 = cpMap1.getRange(cp, null, cpMapRange1);
            boolean b2 = cpMap2.getRange(cp, null, cpMapRange2);
            ok &= assertEquals(message + ": Return: ", b1, b2);
            ok &= assertEqualHex(message + ": Start: ", cpMapRange1.getStart(), cpMapRange2.getStart());
            ok &= assertEqualHex(message + ": End: ", cpMapRange1.getEnd(), cpMapRange2.getEnd());
            int v1 = cpMapRange1.getValue();
            int v2 = cpMapRange2.getValue();
            ok &= assertEqualHex(message + ": Value: ", v1, v2);
        }
        // Then by values
        for (int cp = 0; ok && cp <= 0x01FFFF; ++cp) {
            int v1 = cpMap1.get(cp);
            int v2 = cpMap2.get(cp);
            ok &= assertEqualHex(message + ": Value(get): ", v1, v2);
        }
    }

    private <T> boolean assertEqualHex(String message, int expected, int actual) {
        if (expected != actual) {
            errln(message + ": expected: " + Utility.hex(expected) + ", actual: " + Utility.hex(actual));
        }
        return expected == actual;
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

    public void txestSpeed() {
        checkTime(UcdProperty.Age, AGE_PROP);
        checkTime(UcdProperty.White_Space, WHITESPACE_PROP);
        checkTime(UcdProperty.Block, BLOCK_PROP);
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
            LinkedHashMap<T,Integer> valueMap = new LinkedHashMap<>();
            MutableCodePointTrie builder = fromUnicodeMap(source, valueMap, stringData);
            int itemCount = valueMap.size();
            valueWidth = getValueWidth(valueMap.size()+1); // leave room for -1!
            this.cpData = builder.buildImmutable(type, valueWidth);
            T[] temp = (T[]) new Object[itemCount];
            this.intToData = valueMap.keySet().toArray(temp);
        }

        private static ValueWidth getValueWidth(int itemCount) {
            return itemCount < 0x100 ? ValueWidth.BITS_8 : itemCount < 0x10000 ? ValueWidth.BITS_16 : ValueWidth.BITS_32;
        }

        private static <T> MutableCodePointTrie fromUnicodeMap(UnicodeMap<T> source, LinkedHashMap<T, Integer> outputValueMap, Map<String, T> outputStringData) {
            MutableCodePointTrie builder = new MutableCodePointTrie(-1, -1);
            for (EntryRange<T> range : source.entryRanges()) {
                if (range.string != null) {
                    outputStringData.put(range.string, range.value);
                } else {
                    Integer intValue = outputValueMap.get(range.value);
                    if (intValue == null) {
                        intValue = outputValueMap.size();
                        outputValueMap.put(range.value, intValue);
                    }
                    builder.setRange(range.codepoint, range.codepointEnd, intValue);
                }
            }
            return builder;
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
