package org.unicode.text.UCD;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues;

import com.ibm.icu.text.UnicodeSet;

public class TestVersionedSymbolTable {
  @BeforeEach
  void setUp() {
    UnicodeSet.setDefaultXSymbolTable(VersionedSymbolTable.forDevelopment());
  }

  @AfterEach
  void tearDown() {
    UnicodeSet.setDefaultXSymbolTable(NO_PROPS);
  }

  @Test
  void testIntroductionExamples() {
    assertThatUnicodeSet("\\p{XID_Continue}").containsAll("a", "α", "𒀀").containsNone("'", ",");
  }

  /**
   * Helper class for testing multiple properties of the same UnicodeSet.
   */
  static class UnicodeSetTestFluent {
    UnicodeSetTestFluent(UnicodeSet set) {}
    public <T extends CharSequence> UnicodeSetTestFluent isEqualTo(UnicodeSet collection) {
      assertTrue(set.equals(collection));
      return this;
    }
    public <T extends CharSequence> UnicodeSetTestFluent containsNone(CharSequence... collection) {
      assertTrue(set.containsNone(Arrays.asList(collection)));
      return this;
    }
    public UnicodeSetTestFluent containsAll(CharSequence... collection) {
      assertTrue(set.containsAll(Arrays.asList(collection)));
      return this;
    }
    private UnicodeSet set;
  }

  private UnicodeSetTestFluent assertThatUnicodeSet(String expression) {
    return new UnicodeSetTestFluent(new UnicodeSet(expression));
  }

  static UnicodeSet.XSymbolTable NO_PROPS =
  new UnicodeSet.XSymbolTable() {
      @Override
      public boolean applyPropertyAlias(
              String propertyName, String propertyValue, UnicodeSet result) {
          throw new IllegalArgumentException(
                  "Don't use any ICU Unicode Properties! "
                          + propertyName
                          + "="
                          + propertyValue);
      }
      ;
  };
}
