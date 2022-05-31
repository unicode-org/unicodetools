package org.unicode.jsptest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.unicode.jsp.Builder;
import org.unicode.jsp.Builder.EqualAction;
import org.unicode.unittest.TestFmwkMinusMinus;

/**
 * "No, really, a test!"
 * Tests org.unicode.jsp.Builder
 */
public class TestBuilder extends TestFmwkMinusMinus {

  enum Foo {foo1, foo2};
  Dummy one = new Dummy(1);
  Dummy two = new Dummy(2);
  Dummy two2 = new Dummy(2);
  Dummy three = new Dummy(3);

  @Test
  public void TestCollection() {
    SortedSet<Integer> x = Builder.with(new TreeSet<Integer>()).addAll(1, 2, 3).freeze();
    assertTrue("1,2,3", x.size()==3);
    TreeSet<Integer> x12 = Builder.with(new TreeSet<Integer>()).addAll(1,2).get();
    SortedSet<Integer> y = Builder.with(new TreeSet<Integer>()).addAll(x).removeAll(3,4).freeze();
    assertTrue("1,2", y.equals(x12));
    SortedSet<Integer> z = Builder.with(new TreeSet<Integer>()).addAll(x).retainAll(3,4).freeze();
    assertTrue("3", z.size()==1 && z.contains(3));
    Set<Foo> w = Builder.with(EnumSet.noneOf(Foo.class)).add(Foo.foo1).freeze();
    assertTrue("w", w.size()==1 && w.contains(Foo.foo1));
  }
  /**
   * <pre>
 * Operations: A is current contents, B is new collection, x indicates the results
   * A-B   A&B    B-A   Name
   *                    clear()
   * x                  removeAll(B)
   *        x           retainAll(B) -- option 1: keep A, option 2: substitute B
   *               x    keepNew(B)
   * x      x           <no operation>
   *        x      x    clear().addAll(B)
   * x             x    xor(B)
   * x      x      x    addAll(B)
 * </pre>
   */
  @Test
  public void TestCombos() {
    Set<Dummy> dummyNone = Collections.emptySet();
    Set<Dummy> dummy1 = Builder.with(new TreeSet<Dummy>()).addAll(one).freeze();
    Set<Dummy> dummy2 = Builder.with(new TreeSet<Dummy>()).addAll(two).freeze();
    Set<Dummy> dummy3 = Builder.with(new TreeSet<Dummy>()).addAll(three).freeze();
    Set<Dummy> dummy12 = Builder.with(new TreeSet<Dummy>()).addAll(one, two).freeze();
    Set<Dummy> dummy23 = Builder.with(new TreeSet<Dummy>()).addAll(two, three).freeze();
    Set<Dummy> dummy13 = Builder.with(new TreeSet<Dummy>()).addAll(one, three).freeze();
    Set<Dummy> dummy123 = Builder.with(new TreeSet<Dummy>()).addAll(one, two, three).freeze();

    assertEquals("none", dummyNone, Builder.with(new TreeSet<Dummy>(dummy12)).clear().get());
    assertEquals("removeAll", dummy1, Builder.with(new TreeSet<Dummy>(dummy12)).removeAll(dummy23).get());
    assertEquals("retainAll", dummy2, Builder.with(new TreeSet<Dummy>(dummy12)).retainAll(dummy23).get());
    assertEquals("keepNew", dummy3, Builder.with(new TreeSet<Dummy>(dummy12)).keepNew(dummy23).get());
    assertEquals("xor", dummy13, Builder.with(new TreeSet<Dummy>(dummy12)).xor(dummy23).get());
    assertEquals("addAll", dummy123, Builder.with(new TreeSet<Dummy>(dummy12)).addAll(dummy23).get());
  }

  @Test
  public void TestMapCombos() {
    Map<Dummy, Integer> dummyNone = Collections.emptyMap();
    Map<Dummy, Integer> dummy1 = Builder.with(new TreeMap<Dummy, Integer>()).put(one,2).freeze();
    Map<Dummy, Integer> dummy2 = Builder.with(new TreeMap<Dummy, Integer>()).put(two,2).freeze();
    Map<Dummy, Integer> dummy3 = Builder.with(new TreeMap<Dummy, Integer>()).put(three,2).freeze();
    Map<Dummy, Integer> dummy12 = Builder.with(new TreeMap<Dummy, Integer>()).on(one, two).put(2).freeze();
    Map<Dummy, Integer> dummy23 = Builder.with(new TreeMap<Dummy, Integer>()).on(two, three).put(2).freeze();
    Map<Dummy, Integer> dummy13 = Builder.with(new TreeMap<Dummy, Integer>()).on(one, three).put(2).freeze();
    Map<Dummy, Integer> dummy123 = Builder.with(new TreeMap<Dummy, Integer>()).on(one, two, three).put(2).freeze();

    assertEquals("none", dummyNone, Builder.with(new TreeMap<Dummy,Integer>()).putAll(dummy12).clear().get());
    assertEquals("removeAll", dummy1, Builder.with(new TreeMap<Dummy,Integer>()).putAll(dummy12).removeAll(dummy23.keySet()).get());
    assertEquals("retainAll", dummy2, Builder.with(new TreeMap<Dummy,Integer>()).putAll(dummy12).retainAll(dummy23.keySet()).get());
    assertEquals("keepNew", dummy3, Builder.with(new TreeMap<Dummy,Integer>()).putAll(dummy12).keepNew(dummy23).get());
    assertEquals("xor", dummy13, Builder.with(new TreeMap<Dummy,Integer>()).putAll(dummy12).xor(dummy23).get());
    assertEquals("addAll", dummy123, Builder.with(new TreeMap<Dummy,Integer>()).putAll(dummy12).putAll(dummy23).get());
  }

  @Test
  public void TestMap() {
    Map<Integer,String> x = Builder.with(new TreeMap<Integer,String>()).put(1, "a").put(2,"b").put(3,"c").freeze();
    assertTrue("1,2,3", x.size()==3);
    Map<Integer,String> x2 = Builder.with(new TreeMap<Integer,String>()).on(1,2,3).put("a","b","c").freeze();
    assertEquals("1,2,3 either way", x, x2);
    Map<Integer,String> x3 = Builder.with(new TreeMap<Integer,String>()).on(1,2,3).put("a,b,c".split(",")).freeze();
    assertEquals("1,2,3 either way", x, x3);

    Map<Integer,String> x12 = Builder.with(new TreeMap<Integer,String>()).put(1, "a").put(2,"b").get();
    Map<Integer,String> y = Builder.with(new TreeMap<Integer,String>()).putAll(x).removeAll(3,4).freeze();
    assertTrue("1,2", y.equals(x12));
    Map<Integer,String> z = Builder.with(new TreeMap<Integer,String>()).putAll(x).retainAll(3,4).freeze();
    assertTrue("3", z.size()==1 && z.keySet().contains(3));
    Map<Integer,String> z2 = Builder.with(new TreeMap<Integer,String>()).on(1,2,3).put("a").freeze();
    assertTrue("3", z2.size()==3 && z2.containsKey(2));
  }


  @Test
  public void TestOptions() throws InstantiationException, IllegalAccessException {
    checkOptions(TreeSet.class);
    checkOptions(HashSet.class);
    checkOptions(ArrayList.class);
    checkOptions(LinkedHashSet.class);
    checkOptions(ArrayDeque.class);

    checkMapOptions(LinkedHashMap.class);
    checkMapOptions(HashMap.class);
    checkMapOptions(TreeMap.class);
  }

  public void checkOptions(Class<? extends Collection> class1) throws InstantiationException, IllegalAccessException {
    logln(class1.getName());
    Dummy one = new Dummy(1);
    Dummy one1 = new Dummy(1);
    Collection<Dummy> set = Builder.with((Collection<Dummy>) class1.newInstance(), EqualAction.RETAIN).add(one).add(one1).freeze();
    assertTrue("size", set.size() == 1);
    assertTrue("RETAIN", one == set.iterator().next());

    set = Builder.with((Collection<Dummy>) class1.newInstance(), EqualAction.REPLACE).add(one).add(one1).freeze();
    assertTrue("REPLACE", one1 == set.iterator().next());

    boolean ok;
    try {
      set = Builder.with((Collection<Dummy>) class1.newInstance(), EqualAction.THROW).add(one).add(one1).freeze();
      ok = false;
    } catch (Exception e) {
      ok = true;
    }
    assertTrue("throw", ok);
  }

  public void checkMapOptions(Class<? extends Map> class1) throws InstantiationException, IllegalAccessException {
    logln(class1.getName());
    Dummy one = new Dummy(1);
    Dummy one1 = new Dummy(1);
    Map<Dummy, Integer> set = Builder.with((Map<Dummy, Integer>) class1.newInstance(), EqualAction.RETAIN).put(one, 1).put(one1, 2).freeze();
    assertTrue("size", set.size() == 1);
    assertTrue("RETAIN", one == set.keySet().iterator().next());
    assertTrue("RETAIN-get", 1 == set.get(one));

    set = Builder.with((Map<Dummy, Integer>) class1.newInstance(), EqualAction.REPLACE).put(one, 1).put(one1, 2).freeze();
    assertTrue("REPLACE", one1 == set.keySet().iterator().next());
    assertTrue("REPLACE-get", 2 == set.get(one));

    boolean ok;
    try {
      set = Builder.with((Map<Dummy, Integer>) class1.newInstance(), EqualAction.THROW).put(one, 1).put(one1, 2).freeze();
      ok = false;
    } catch (Exception e) {
      ok = true;
    }
    assertTrue("throw", ok);
  }

  static class Dummy implements Comparable<Dummy>, Cloneable {
    int item;

    public Dummy(int item) {
      this.item = item;
    }

    public boolean equals(Object obj) {
      return item == ((Dummy) obj).item;
    }

    public int hashCode() {
      return item;
    }

    public int compareTo(Dummy o) {
      int item2 = ((Dummy) o).item;
      return item < item2 ? -1 : item > item2 ? 1 : 0;
    }

    public Object clone() {
      return clone();
    }

    public String toString() {
      return "<"+item+">";
    }
  }
}
