package org.unicode.parse;

import java.util.regex.Pattern;

public class EBNF {
static final Pattern TOKEN = Pattern.compile(
        "(?<literal>\"[^\"]+\")"
        + "|(?<variable>[A-Za-z][A-Za-z0-9_]*)"
                +"|");
}
