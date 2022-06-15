package org.unicode.draft;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralFormat;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.ULocale;
import java.text.ChoiceFormat;
import java.text.Format;
import java.util.Date;

public class SimpleFormatRegistry implements FormatRegistry {

    /* (non-Javadoc)
     * @see FormatRegistry2#getFormatForObject(java.lang.Object, com.ibm.icu.util.ULocale)
     */
    @Override
    public Format getFormatForObject(Class classType, ULocale ulocale) {
        if (classType.isAssignableFrom(Number.class)) {
            // format number if can
            return NumberFormat.getInstance(ulocale);
        } else if (classType.isAssignableFrom(Date.class)) {
            // format a Date if can
            return DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.SHORT, ulocale); // fix
        } else {
            return null;
        }
    }

    // UGLY CODE, move somewhere else
    /**
     * if (subFormatter instanceof ChoiceFormat || subFormatter instanceof PluralFormat) { arg =
     * formats[i].format(obj); // TODO: This should be made more robust. // Does this work with '{'
     * in quotes? if (arg.indexOf('{') >= 0) { subFormatter = new MessageFormat(arg, ulocale); obj =
     * arguments; arg = null; } }
     */
    // for now, just hard-code
    /* (non-Javadoc)
     * @see FormatRegistry2#getKey(java.text.Format, com.ibm.icu.util.ULocale)
     */
    // for now, just hard-code
    @Override
    public String getKey(Format formats, ULocale ulocale) {
        if (formats == null) {
            return "";
        } else if (formats instanceof DecimalFormat) {
            if (formats.equals(NumberFormat.getInstance(ulocale))) {
                return ("number");
            } else if (formats.equals(NumberFormat.getCurrencyInstance(ulocale))) {
                return ("number,currency");
            } else if (formats.equals(NumberFormat.getPercentInstance(ulocale))) {
                return ("number,percent");
            } else if (formats.equals(NumberFormat.getIntegerInstance(ulocale))) {
                return ("number,integer");
            } else {
                return ("number," + ((DecimalFormat) formats).toPattern());
            }
        } else if (formats instanceof SimpleDateFormat) {
            if (formats.equals(DateFormat.getDateInstance(DateFormat.DEFAULT, ulocale))) {
                return ("date");
            } else if (formats.equals(DateFormat.getDateInstance(DateFormat.SHORT, ulocale))) {
                return ("date,short");
                // This code will never be executed [alan]
                //            } else if
                // (inputFormat.equals(DateFormat.getDateInstance(DateFormat.DEFAULT,ulocale))) {
                //                return ("date,medium");
            } else if (formats.equals(DateFormat.getDateInstance(DateFormat.LONG, ulocale))) {
                return ("date,long");
            } else if (formats.equals(DateFormat.getDateInstance(DateFormat.FULL, ulocale))) {
                return ("date,full");
            } else if (formats.equals(DateFormat.getTimeInstance(DateFormat.DEFAULT, ulocale))) {
                return ("time");
            } else if (formats.equals(DateFormat.getTimeInstance(DateFormat.SHORT, ulocale))) {
                return ("time,short");
                // This code will never be executed [alan]
                //            } else if
                // (inputFormat.equals(DateFormat.getTimeInstance(DateFormat.DEFAULT,ulocale))) {
                //                return ("time,medium");
            } else if (formats.equals(DateFormat.getTimeInstance(DateFormat.LONG, ulocale))) {
                return ("time,long");
            } else if (formats.equals(DateFormat.getTimeInstance(DateFormat.FULL, ulocale))) {
                return ("time,full");
            } else {
                return ("date," + ((SimpleDateFormat) formats).toPattern());
            }
        } else if (formats instanceof ChoiceFormat) {
            return ("choice," + ((ChoiceFormat) formats).toPattern());
        } else if (formats instanceof PluralFormat) {
            String pat = ((PluralFormat) formats).toPattern();
            // TODO: PluralFormat doesn't do the single quote thing, just reapply
            if (pat.indexOf('\'') != 0) {
                final StringBuffer buf = new StringBuffer();
                for (int j = 0; j < pat.length(); ++j) {
                    final char ch = pat.charAt(j);
                    if (ch == '\'') {
                        buf.append(ch); // double it
                    }
                    buf.append(ch);
                }
                pat = buf.toString();
            }
            return ("plural," + pat);
        } else {
            return "";
        }
    }

    private static final String[] typeList = {
        "", "number", "date", "time", "choice", "spellout", "ordinal", "duration", "plural"
    };

    private static final int TYPE_EMPTY = 0,
            TYPE_NUMBER = 1,
            TYPE_DATE = 2,
            TYPE_TIME = 3,
            TYPE_CHOICE = 4,
            TYPE_SPELLOUT = 5,
            TYPE_ORDINAL = 6,
            TYPE_DURATION = 7,
            TYPE_PLURAL = 8;

    private static final String[] modifierList = {"", "currency", "percent", "integer"};

    private static final int MODIFIER_EMPTY = 0,
            MODIFIER_CURRENCY = 1,
            MODIFIER_PERCENT = 2,
            MODIFIER_INTEGER = 3;

    private static final String[] dateModifierList = {"", "short", "medium", "long", "full"};

    private static final int DATE_MODIFIER_EMPTY = 0,
            DATE_MODIFIER_SHORT = 1,
            DATE_MODIFIER_MEDIUM = 2,
            DATE_MODIFIER_LONG = 3,
            DATE_MODIFIER_FULL = 4;

    private static final int findKeyword(String s, String[] list) {
        s = s.trim().toLowerCase();
        for (int i = 0; i < list.length; ++i) {
            if (s.equals(list[i])) {
                return i;
            }
        }
        return -1;
    }

    /* (non-Javadoc)
     * @see FormatRegistry2#getFormat(java.lang.String, java.lang.String, com.ibm.icu.util.ULocale, boolean[])
     */
    @Override
    public Format getFormat(String mainType, String subType, ULocale ulocale) {
        Format newFormat = null;
        switch (findKeyword(mainType, typeList)) {
            case TYPE_EMPTY:
                break;
            case TYPE_NUMBER:
                switch (findKeyword(subType, modifierList)) {
                    case MODIFIER_EMPTY:
                        newFormat = NumberFormat.getInstance(ulocale);
                        break;
                    case MODIFIER_CURRENCY:
                        newFormat = NumberFormat.getCurrencyInstance(ulocale);
                        break;
                    case MODIFIER_PERCENT:
                        newFormat = NumberFormat.getPercentInstance(ulocale);
                        break;
                    case MODIFIER_INTEGER:
                        newFormat = NumberFormat.getIntegerInstance(ulocale);
                        break;
                    default: // pattern
                        newFormat = new DecimalFormat(subType, new DecimalFormatSymbols(ulocale));
                        break;
                }
                break;
            case TYPE_DATE:
                switch (findKeyword(subType, dateModifierList)) {
                    case DATE_MODIFIER_EMPTY:
                        newFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, ulocale);
                        break;
                    case DATE_MODIFIER_SHORT:
                        newFormat = DateFormat.getDateInstance(DateFormat.SHORT, ulocale);
                        break;
                    case DATE_MODIFIER_MEDIUM:
                        newFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, ulocale);
                        break;
                    case DATE_MODIFIER_LONG:
                        newFormat = DateFormat.getDateInstance(DateFormat.LONG, ulocale);
                        break;
                    case DATE_MODIFIER_FULL:
                        newFormat = DateFormat.getDateInstance(DateFormat.FULL, ulocale);
                        break;
                    default:
                        newFormat = new SimpleDateFormat(subType, ulocale);
                        break;
                }
                break;
            case TYPE_TIME:
                switch (findKeyword(subType, dateModifierList)) {
                    case DATE_MODIFIER_EMPTY:
                        newFormat = DateFormat.getTimeInstance(DateFormat.DEFAULT, ulocale);
                        break;
                    case DATE_MODIFIER_SHORT:
                        newFormat = DateFormat.getTimeInstance(DateFormat.SHORT, ulocale);
                        break;
                    case DATE_MODIFIER_MEDIUM:
                        newFormat = DateFormat.getTimeInstance(DateFormat.DEFAULT, ulocale);
                        break;
                    case DATE_MODIFIER_LONG:
                        newFormat = DateFormat.getTimeInstance(DateFormat.LONG, ulocale);
                        break;
                    case DATE_MODIFIER_FULL:
                        newFormat = DateFormat.getTimeInstance(DateFormat.FULL, ulocale);
                        break;
                    default:
                        newFormat = new SimpleDateFormat(subType, ulocale);
                        break;
                }
                break;
            case TYPE_CHOICE:
                try {
                    newFormat = new ChoiceFormat(subType);
                } catch (final Exception e) {
                    throw new IllegalArgumentException("Choice Pattern incorrect", e);
                }
                break;
            case TYPE_SPELLOUT:
                {
                    final RuleBasedNumberFormat rbnf =
                            new RuleBasedNumberFormat(ulocale, RuleBasedNumberFormat.SPELLOUT);
                    final String ruleset = subType.trim();
                    if (ruleset.length() != 0) {
                        try {
                            rbnf.setDefaultRuleSet(ruleset);
                        } catch (final Exception e) {
                            // warn invalid ruleset
                        }
                    }
                    newFormat = rbnf;
                }
                break;
            case TYPE_ORDINAL:
                {
                    final RuleBasedNumberFormat rbnf =
                            new RuleBasedNumberFormat(ulocale, RuleBasedNumberFormat.ORDINAL);
                    final String ruleset = subType.trim();
                    if (ruleset.length() != 0) {
                        try {
                            rbnf.setDefaultRuleSet(ruleset);
                        } catch (final Exception e) {
                            // warn invalid ruleset
                        }
                    }
                    newFormat = rbnf;
                }
                break;
            case TYPE_DURATION:
                {
                    final RuleBasedNumberFormat rbnf =
                            new RuleBasedNumberFormat(ulocale, RuleBasedNumberFormat.DURATION);
                    final String ruleset = subType.trim();
                    if (ruleset.length() != 0) {
                        try {
                            rbnf.setDefaultRuleSet(ruleset);
                        } catch (final Exception e) {
                            // warn invalid ruleset
                        }
                    }
                    newFormat = rbnf;
                }
                break;
            case TYPE_PLURAL:
                {
                    // PluralFormat does not handle quotes.
                    // Remove quotes.
                    // TODO: Should PluralFormat handle quotes?
                    final StringBuffer unquotedPattern = new StringBuffer();
                    final String quotedPattern = subType;
                    boolean inQuote = false;
                    for (int i = 0; i < quotedPattern.length(); ++i) {
                        final char ch = quotedPattern.charAt(i);
                        if (ch == '\'') {
                            if (i + 1 < quotedPattern.length()
                                    && quotedPattern.charAt(i + 1) == '\'') {
                                unquotedPattern.append(ch);
                                ++i;
                            } else {
                                inQuote = !inQuote;
                            }
                        } else {
                            unquotedPattern.append(ch);
                        }
                    }

                    final PluralFormat pls = new PluralFormat(ulocale, unquotedPattern.toString());
                    newFormat = pls;
                }
                break;
            default:
                throw new IllegalArgumentException("unknown format type at ");
        }
        return newFormat;
    }
}
