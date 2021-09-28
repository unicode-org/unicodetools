/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/UCD/PropertyLister.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.UCD;

import java.io.PrintWriter;
import java.text.NumberFormat;

import org.unicode.text.utility.ChainException;
import org.unicode.text.utility.Utility;

import com.ibm.icu.text.UnicodeSet;


abstract public class PropertyLister implements UCD_Types {

    static final boolean COMPRESS_NAMES = false;
    static final boolean DROP_INDICATORS = true;


    protected UCD ucdData;
    protected PrintWriter output;
    protected boolean showOnConsole;
    protected boolean usePropertyComment = true;
    protected boolean breakByCategory = true;
    protected int firstRealCp = -2;
    protected int lastRealCp = -2;
    protected boolean alwaysBreaks = false; // set to true if property only breaks
    protected boolean commentOut = false;
    protected boolean useKenName = true; // set to false to get meaningful names
    private final UnicodeSet set = new UnicodeSet();

    public static final byte INCLUDE = 0, BREAK = 1, CONTINUE = 2, EXCLUDE = 3;

    /**
     * @return status. Also have access to firstRealCp, lastRealCp
     */
    abstract public byte status(int cp);

    public String headerString() {
        return "";
    }

    public String valueName(int cp) {
        return "";
    }

    public String missingValueName() {
        return "";
    }

    public String optionalName(int cp) {
        return "";
    }

    public String optionalComment(int cp) {
        if (!usePropertyComment) {
            return "";
        }
        return ucdData.getModCatID_fromIndex(getModCat(cp));
    }

    public int minPropertyWidth() {
        return 1;
    }

    public void format(int startCp, int endCp, int realCount) {
        try {
            set.add(startCp, endCp);
            String prop = valueName(startCp);
            String opt = "";
            String optCom = "";
            String commentSep = " # ";
            if (commentOut) {
                commentSep = "";
            }

            if (prop.length() > 0) {
                prop = "; " + prop;
            }
            opt = optionalName(startCp);
            if (opt.length() > 0) {
                opt = "; " + opt;
            }
            optCom = optionalComment(startCp);
            if (optCom.length() > 0) {
                optCom += " ";
            }

            final String startName = getKenName(startCp);
            String line;
            final String pgap = Utility.repeat(" ", minPropertyWidth() - prop.length() - opt.length());
            if (startCp != endCp) {
                final String endName = getKenName(endCp);
                final int bridge = endCp - startCp + 1 - realCount;
                final String count = (bridge == 0) ? "" + realCount : realCount + "/" + bridge;
                final String countStr = Utility.repeat(" ", 3-count.length()) + "[" + count + "] ";
                final String gap = Utility.repeat(" ", 12 - width(startCp) - width(endCp));

                line = Utility.hex(startCp,4) + ".." + Utility.hex(endCp,4) + gap
                        + prop + opt + pgap + commentSep + optCom
                        + countStr;
                if (startName.length() != 0 || endName.length() != 0) {
                    int com = 0;
                    if (COMPRESS_NAMES) {
                        com = commonInitialWords(startName, endName);
                    }
                    if (com == 0) {
                        line += startName + ".." + endName;
                    } else {
                        line += startName.substring(0,com)
                                + "(" + startName.substring(com) + ".." + endName.substring(com) + ")";
                    }
                }
            } else {
                final String gap = alwaysBreaks
                        ? Utility.repeat(" ", 6 - width(startCp))
                                : Utility.repeat(" ", 14 - width(startCp));
                        final String gap2 = alwaysBreaks
                                ? " "
                                        : "      ";
                        line = Utility.hex(startCp,4) + gap
                                + prop + opt + pgap + commentSep + optCom + gap2
                                + startName;
            }
            if (commentOut) {
                line = "# " + line;
            }
            output.println(line);
            if (showOnConsole) {
                System.out.println(line);
            }
        } catch (final Exception e) {
            throw new ChainException("Format error {0}, {1}",
                    new Object[]{new Integer(startCp), new Integer(endCp)}, e);
        }
    }

    int width(int cp) {
        return cp <= 0xFFFF ? 4
                : cp <= 0xFFFFF ? 5
                        : 6;
    }

    String getKenName(int cp) {
        final String result = ucdData.getName(cp);
        if (!useKenName) {
            return result;
        }
        if (result == null) {
            return "";
        }
        if (DROP_INDICATORS && result.charAt(0) == '<') {
            if (cp < 0xFF) {
                return "<control>";
            }
            return "";
        }
        return result;
    }

    byte getModCat(int cp) {
        final byte result = ucdData.getModCat(cp, breakByCategory ? CASED_LETTER_MASK : 0);
        return result;
    }


    /**
     * @return common initial substring length ending with SPACE or HYPHEN-MINUS. 0 if there is none
     */
    public static int commonInitialWords(String a, String b) {
        if (a.length() > b.length()) {
            final String temp = a;
            a = b;
            b = temp;
        }
        int lastSpace = 0;
        for (int i = 0; i < a.length(); ++i) {
            final char ca = a.charAt(i);
            final char cb = b.charAt(i);
            if (ca != cb) {
                return lastSpace;
            }
            if (ca == ' ' || ca == '-') {
                lastSpace = i + 1;
            }
        }
        if (b.length() == a.length() || b.charAt(a.length()) == ' ' || b.charAt(a.length()) == '-') {
            lastSpace = a.length();
        }
        return lastSpace;
    }

    public int print() {
        set.clear();
        int count = 0;
        firstRealCp = -1;
        byte firstRealCpCat = -1;
        lastRealCp = -1;
        int realRangeCount = 0;

        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            byte s = status(cp);
            if (alwaysBreaks && s == INCLUDE) {
                s = BREAK;
            }
            if (s == INCLUDE && firstRealCp != -1) {
                if (getModCat(cp) != firstRealCpCat) {
                    s = BREAK;
                }
            }

            switch(s) {
            case CONTINUE:
                break; // do nothing
            case INCLUDE:
                if (firstRealCp == -1) {
                    firstRealCp = cp;
                    firstRealCpCat = getModCat(firstRealCp);
                }
                lastRealCp = cp;
                count++;
                realRangeCount++;
                break;
            case BREAK:
                if (firstRealCp != -1) {
                    format(firstRealCp, lastRealCp, realRangeCount);
                }
                lastRealCp = firstRealCp = cp;
                firstRealCpCat = getModCat(firstRealCp);

                realRangeCount = 1;
                count++;
                break;
            case EXCLUDE:
                if (firstRealCp != -1) {
                    format(firstRealCp, lastRealCp, realRangeCount);
                    firstRealCp = -1;
                    realRangeCount = 0;
                }
                break;
            }
        }
        if (firstRealCp != -1) {
            format(firstRealCp, lastRealCp, realRangeCount);
        }

        if (count == 0) {
            output.println("# No values for " + missingValueName());
            System.out.println("ZERO COUNT for " + missingValueName());
        }
        final NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(0);
        nf.setGroupingUsed(false);
        output.println();
        output.println("# Total code points: " + nf.format(count));
        output.println();
        //System.out.println(headerString());
        //System.out.println(set.toPattern(true));
        return count;
    }

}