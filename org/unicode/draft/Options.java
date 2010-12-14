package org.unicode.draft;

public class Options {
    public enum Argument {noArg, argOptional, argRequired};
    
    public interface OptionFeatures {
        getMatch(String option) {
            
        }
    }
    enum Args {
        Pattern fileFilter;
        fileFilter(Argument.argRequired, "Filter the information based on file name, using a regex argument", ),
        pathFilter(Argument.argRequired, "Filter the information based on path name, using a regex argument"),
        contentFilter(Argument.argRequired, "Filter the information based on content name, using a regex argument"),
        special(Argument.noArg, "Filter the information based on content name, using a regex argument");
        Argument argument;
        String helpString;
        Args(Argument argument, String helpString) {
            this.argument = argument;
            this.helpString = helpString;
        }
    }

    Options populate(String[] args) {
        return new Options();
    }
}
