package org.unicode.utilities;

import com.ibm.icu.text.UnicodeSet;

public interface StringSetBuilder<T> {

    void addAll(StringSetBuilder<T> other);		// A = A ∪ other
    void retainAll(StringSetBuilder<T> other);		// A = A ∩ other
    void removeAll(StringSetBuilder<T> other);		// A = A ∖ other
    void complementAll(StringSetBuilder<T> other);	// A = A ⊖ other
    
    void add(int cp);					// A = A ∪ {cp}
    void addAll(int cpStart, int cpEnd);		// A = A ∪ {cpStart .. cpEnd}
    void add(CharSequence stringToAdd);			// A = A ∪ {stringToAdd}
    void complement();					// A = ¬A ∪ B = 𝕊 ∖ A
    void setToProperty(String propertyString);		// A = propertySet

    T buildSet();
    
    // debugging
    void setSkipValidity(boolean skipValidity);
    boolean isSkipValidity();

    public interface StringSetBuilderFactory<U> {
	U make();
    }

    // *******

    static class UnicodeSetBuilder implements StringSetBuilder<UnicodeSet> {
	private boolean skipValidity = false;
	private UnicodeSet set = new UnicodeSet();

	@Override
	public void addAll(StringSetBuilder<UnicodeSet> other) {
	    set.addAll(other.buildSet());
	}

	@Override
	public void retainAll(StringSetBuilder<UnicodeSet> other) {
	    set.retainAll(other.buildSet());
	}

	@Override
	public void removeAll(StringSetBuilder<UnicodeSet> other) {
	    set.removeAll(other.buildSet());
	}

	@Override
	public void complementAll(StringSetBuilder<UnicodeSet> other) {
	    set.complementAll(other.buildSet()); 
	}

	@Override
	public void add(CharSequence other) {
	    set.add(other);
	}

	@Override
	public void addAll(int cpStart, int cpEnd) {
	    set.add(cpStart, cpEnd);
	}

	@Override
	public void add(int cp) {
	    set.add(cp);
	}

	@Override
	public void complement() {
	    set.complement();
	}

	static final UnicodeSet RELATION = new UnicodeSet("[\\=≠{!=}]");
	
	@Override
	public void setToProperty(String propertyString) {
	    if (skipValidity) {
		set.applyPropertyAlias("sc", "runr");
		return;
	    }
	    int equals = propertyString.indexOf('=');
	    if (equals < 0) {
		set.applyPropertyAlias(propertyString, "true");
	    } else {
		set.applyPropertyAlias(propertyString.substring(0,equals), propertyString.substring(equals+1));
	    }
	}

	@Override
	public UnicodeSet buildSet() {
	    return set;
	}
	
	@Override
	public String toString() {
	    return set.toPattern(false);
	}

	public void setSkipValidity(boolean skipValidity) {
	    this.skipValidity = skipValidity;
	}

	@Override
	public boolean isSkipValidity() {
	    return skipValidity;
	}	
    }

    static class UnicodeSetBuilderFactory implements StringSetBuilderFactory<UnicodeSetBuilder> {
	private boolean skipValidity = false;

	@Override
	public UnicodeSetBuilder make() {
	    UnicodeSetBuilder result = new UnicodeSetBuilder();
	    if (skipValidity) {
		result.setSkipValidity(true);
	    }
	    return result;
	}
	
	public void setSkipValidity(boolean skipValidity) {
	    this.skipValidity = skipValidity;
	}	
    }
}
