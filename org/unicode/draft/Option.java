package org.unicode.draft;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class Option {
    final String tag;
    final Pattern match;
    final String helpString;
    boolean doesOccur;
    String value;

    public String getTag() {
        return tag;
    }

    public Pattern getMatch() {
        return match;
    }

    public String getHelpString() {
        return helpString;
    }

    public String getValue() {
        return value;
    }

    public boolean doesOccur() {
        return doesOccur;
    }

    public Option(String tag, String argumentPattern, String helpString) {
        this.match = argumentPattern == null ? null : Pattern.compile(argumentPattern, Pattern.COMMENTS);
        this.helpString = helpString;
        this.tag = tag;
    }

    public String toString() {
        return "-" + tag + " \t" + (match == null ? "no-arg" : match.pattern()) + " \t" + helpString;
    }

    public boolean matches(String arg, boolean isStringOption, String inputValue) {
        doesOccur = false;
        this.value = null;
        if (arg.equals(tag) || isStringOption && tag.startsWith(arg)) {
            // we match the tag. See if we match the value
            if (match == null) {
                if (inputValue != null) {
                    throw new IllegalArgumentException("The flag '" + arg + "' must have no parameter");
                }
            } else { // match != null
                if (inputValue != null && match.matcher(inputValue).matches()) {
                    this.value = arg;
                    doesOccur = true;
                } else {
                    throw new IllegalArgumentException("The flag '" + arg + "' has the parameter '" + inputValue + "', which must match " + match.pattern());
                }
            }
            return true;
        }
        return false;
    }

    public static class Options implements Iterable<Option> {

        final Set<Option> values = new LinkedHashSet<Option>();
        {
            add("help", null, "Provide the list of possible options");
        }

        public Options add(String string, String argumentPattern, String string2) {
            for (Option option : values) {
                if (option.tag.equals(string)) {
                    throw new IllegalArgumentException("Duplicate tag " + string);
                }
            }
            values.add(new Option(string, argumentPattern, string2));
            return this;
        }

        private void parse(String[] args) {
            for (int i = 0; i < args.length; ++i) {
                String arg = args[i];
                if (!arg.startsWith("-")) {
                    throw new IllegalArgumentException(getHelp());
                }
                // can be of the form -fparam or -f param or --file param
                boolean isStringOption = arg.startsWith("--");
                String value = null;
                if (isStringOption) {
                    arg = arg.substring(2);
                } else if (arg.length() > 2) {
                    value = arg.substring(2);
                    arg = arg.substring(1,2);
                } else {
                    arg = arg.substring(1);
                }
                if (value == null) {
                    value = i < args.length - 1 ? args[i+1] : null;
                    if (value != null && value.startsWith("-")) {
                        value = null;
                    }
                    if (value != null) {
                        ++i;
                    }
                }
                for (Option option : values) {
                    if (option.matches(arg, isStringOption, value)) {
                        break;
                    }
                }
            }
        }

        private String getHelp() {
            StringBuilder buffer = new StringBuilder("Invalid Option - Choices are:");
            for (Option option : values) {
                buffer.append('\n').append(option);
            }
            return buffer.toString();
        }

        @Override
        public Iterator<Option> iterator() {
            return values.iterator();
        }
    }

    // for quick testing
    final static Options myOptions = new Options()
    .add("file", ".*", "Filter the information based on file name, using a regex argument")
    .add("path", ".*", "Filter the information based on path name, using a regex argument")
    .add("content", ".*", "Filter the information based on content name, using a regex argument");

    public static void main(String[] args) {
        if (args.length == 0) {
            args = "-fen.xml -c a* --path p -h none".split("\\s+");
        }
        myOptions.parse(args);
        for (Option option : myOptions) {
            System.out.println(option.doesOccur() + "\t" + option.getValue() + "\t" + option);
        }
    }

}
