package org.unicode.propstest;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.unicode.props.UnicodeRelation;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.text.UnicodeSet;

public class UnicodeRelationTest extends TestFmwk{
    public static void main(String[] args) {
        new UnicodeRelationTest().run(args);
    }

    enum Foo {aa, bb, cc, dd, ee}

    static UnicodeRelation.SetMaker<Foo> FOOSETMAKER = new UnicodeRelation.SetMaker<Foo>()  {
        @Override
        public Set<Foo> make() {
            return EnumSet.noneOf(Foo.class);
        }
    };

    public void TestBasic() {
        UnicodeRelation<Foo> ur = new UnicodeRelation<>(FOOSETMAKER);
        ur.addAll("a", Arrays.asList(Foo.aa, Foo.bb, Foo.cc));
        ur.addAll("b", Arrays.asList(Foo.bb, Foo.cc, Foo.dd));
        check(ur);
        logln(ur.toString());
        assertTrue("", ur.containsKey("a"));
        assertFalse("", ur.containsKey("c"));
        assertTrue("", ur.containsValue(Foo.bb));
        assertFalse("", ur.containsValue(Foo.ee));

        assertEquals("", EnumSet.of(Foo.aa, Foo.bb, Foo.cc), ur.get("a"));
        assertEquals("", new UnicodeSet("[ab]"), ur.getKeys(Foo.bb));

        ur.remove("b", Foo.bb);
        assertEquals("", EnumSet.of(Foo.cc, Foo.dd), ur.get("b"));
        assertEquals("", new UnicodeSet("[a]"), ur.getKeys(Foo.bb));

        check(ur);
    }

    Random random = new Random(0);
    final List<String> keys = Arrays.asList("a", "b", "c", "d", "ab");
    final UnicodeSet ukeys = new UnicodeSet().addAll(keys).freeze();
    final List<Foo> values = Arrays.asList(Foo.values());

    public void TestMonkey() {
        UnicodeRelation<Foo> ur = new UnicodeRelation<>(FOOSETMAKER);
        Relation<String,Foo> rr = Relation.of(new HashMap<String,Set<Foo>>(), HashSet.class);
        for (int i = 0; i < 1000; ++i) {
            String key = random(keys);
            int choice = random.nextInt(6);
            Foo value;
            switch(choice) {
            case 0: 
                value = random(values);
                ur.add(key, value);
                rr.put(key, value);
                break;
            case 1: 
                ur.remove(key);
                rr.removeAll(key);
                break;
            case 2: 
                Set<Foo> values2 = ur.get(key);
                value = values2 == null ? null : getNth(values2, random.nextInt(values2.size()));
                ur.remove(key, value);
                rr.remove(key, value);
                break;
            case 3:
                ur.addAll(key, values);
                rr.putAll(key, values);
                break;
            case 4:
                ur.removeAll(key, values);
                rr.removeAll(key, values);
                break;
            case 5:
                assertEquals(choice + "keySet: ", ur.keySet(), new UnicodeSet().addAll(rr.keySet()));
                break;
            case 6:
                value = random(values);
                ur.addAll(ukeys, value);
                rr.putAll(keys, value);
                break;
            }
            checkSame(choice, ur,rr);
        }
    }

    private <T> T getNth(Collection<T> values2, int index) {
        for (T value : values2) {
            if (index == 0) {
                return value;
            }
            index--;
        }
        return null;
    }

    private void checkSame(int i, UnicodeRelation<Foo> ur, Relation<String, Foo> rr) {
        UnicodeSet urkeys = ur.keySet();
        UnicodeSet rrkeys = new UnicodeSet().addAll(rr.keySet());
        if (assertEquals(i + " keys ", urkeys, rrkeys)) {
            for (String s : urkeys) {
                Set<Foo> uValue = ur.get(s);
                Set<Foo> rValue = rr.get(s);
                assertEquals("\tvalues ", uValue, rValue);
            }
        }
    }

    private <T> T random(List<T> source) {
        return source.get(random.nextInt(source.size()));
    }

    // verify the values are not null, and are not modifiable
    private <T> void check(UnicodeRelation<T> ur) {
        for (Entry<String, Set<T>> entry : ur.keyValues()) {
            Set<T> value = entry.getValue();
            try {
                value.remove(value.iterator().next());
                logln("Value modifiable for " + entry.getKey());
            } catch (java.lang.UnsupportedOperationException e) {}
        }
    }
}
