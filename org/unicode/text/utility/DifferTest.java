/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /home/cvsroot/unicodetools/org/unicode/text/utility/DifferTest.java,v $
*
*******************************************************************************
*/

package org.unicode.text.utility;

import com.ibm.icu.impl.Differ;


public class DifferTest {
    public static final String copyright =
      "Copyright (C) 2000, IBM Corp. and others. All Rights Reserved.";

    static final void main(String[] args) { // for testing

        String[] as = {"a", "b", "20D4", "0344", "20D5", "20D6", "20D7", "20D8", "20D9"};
        String[] bs = {"a", "b", "20D4", "20D5", "0344", "20D6", "20D7", "20D8", "20D9"};
        Differ differ = new Differ(100,30);
        int max = as.length;
        if (max < bs.length) max = bs.length;
        for (int j = 0; j <= max; ++j) {
            if (j < as.length) differ.addA(as[j]);
            if (j < bs.length) differ.addB(bs[j]);
            differ.checkMatch(j == max);

            int aCount = differ.getACount();
            int bCount = differ.getBCount();
            if (aCount != 0 || bCount != 0) {
                System.out.println("a: " + differ.getALine(-1) + " " + differ.getA(-1) + "\t" + "b: " + differ.getBLine(-1) + " " + differ.getB(-1));

                if (aCount != 0) {
                    for (int i = 0; i < aCount; ++i) {
                        System.out.println("a: " + differ.getALine(i) + " " + differ.getA(i));
                    }
                }
                if (bCount != 0) {
                    if (aCount != 0) System.out.println();
                    for (int i = 0; i < bCount; ++i) {
                        System.out.println("b: " + differ.getBLine(i) + " " + differ.getB(i));
                    }
                }
                System.out.println("a: " + differ.getALine(aCount) + " " + differ.getA(aCount) + "\t" + "b: " + differ.getBLine(bCount) + " " + differ.getB(bCount));
            }
            System.out.println("----");
            //differ.flush();
        }
    }
}