package org.unicode.jsp;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

public class Builder<T, U extends Collection<T>> {

  static <T, U extends Collection<T>> Builder<T,U> with(U set) {
    return new Builder<T,U>(set);
  }

  public Builder<T,U> add(T e) {
    collection.add(e);
    return this;
  }
  public Builder<T,U> addAll(Collection<? extends T> c) {
    collection.addAll(c);
    return this;
  }
  public Builder<T, U> remove(Object o) {
    collection.remove(o);
    return this;
  }
  public Builder<T,U> removeAll(Collection<? extends T> c) {
    collection.removeAll(c);
    return this;
  }
  public Builder<T,U> retainAll(Collection<? extends T> c) {
    collection.retainAll(c);
    return this;
  }
  public U get() {
    U temp = collection;
    collection = null;
    return temp;
  }
  
  @SuppressWarnings("unchecked")
  public U freeze() {
    U temp;
    if (collection instanceof SortedSet) {
      temp = (U)Collections.unmodifiableSortedSet((SortedSet<T>) collection);
    } else if (collection instanceof Set) {
      temp = (U)Collections.unmodifiableSet((Set<T>) collection);
    } else if (collection instanceof SortedSet) {
      temp = (U)Collections.unmodifiableList((List<T>) collection);
    } else {
      temp = (U)Collections.unmodifiableSortedSet((SortedSet<T>) collection);
    }
    collection = null;
    return temp;
  }
  
  // ====
  private Builder(U set2) {
    this.collection = set2;
  }
  private U collection;
}
