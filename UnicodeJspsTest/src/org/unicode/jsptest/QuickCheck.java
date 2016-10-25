package org.unicode.jsptest;

import java.io.IOException;

import org.unicode.jsp.UnicodeJsp;

import com.ibm.icu.text.UnicodeSet;

public class QuickCheck {
public static void main(String[] args) throws IOException {
    //   public static void showSet(String grouping, UnicodeSet a, boolean abbreviate, boolean ucdFormat, Appendable out) throws IOException {
//   public static String getSimpleSet(String setA, UnicodeSet a, boolean abbreviate, boolean escape) {

    StringBuilder out = new StringBuilder();
    UnicodeSet a = new UnicodeSet();

    String outer = UnicodeJsp.getSimpleSet("[:emoji=yes:]", a, false, false);
    //UnicodeJsp.showSet("", a, true, false, out);
    //String outer = out.toString();
    System.out.println(outer);
}
}
