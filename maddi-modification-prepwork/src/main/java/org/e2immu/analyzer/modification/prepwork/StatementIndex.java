package org.e2immu.analyzer.modification.prepwork;

import java.util.regex.Pattern;

public class StatementIndex {

    // pre-digits+dot: +,-.
    // post-digits: :;<=>?@ and at the end |~

    public static final char PLUS = '+';
    public static final char DASH = '-';
    public static final char DOT = '.';
    public static final char COLON = ':';
    public static final char SEMICOLON = ';';
    public static final char EQUALS = '=';
    public static final char END = '~';

    public static final String INIT = PLUS + "I"; // name of the stage +I
    public static final String EVAL_INIT = PLUS + "E"; // initializer in for/forEach +E
    public static final String EVAL = DASH + "E";  // normal expression; condition/iterable for loops -E
    public static final String EVAL_UPDATE = COLON + "E"; // still part of the loop, but after the statements :E
    public static final String EVAL_AFTER_UPDATE = SEMICOLON + "E"; // repeat of condition in for
    public static final String MERGE = EQUALS + "M"; // at the end =M

    public static final Pattern STAGE_PATTERN = Pattern.compile("(.*)(\\+I|[+-=]E|=M)");

    private StatementIndex() {
    }


    public static final String DOT_ZERO = DOT + "0";
    public static final String BEFORE_METHOD = "" + DASH;
    public static final String ENCLOSING_METHOD = "" + PLUS;
    public static final String END_OF_METHOD = "" + END;


    public static boolean seenBy(String s, String index) {
        int dot = s.lastIndexOf(StatementIndex.DOT);
        if (dot < 0) {
            return s.compareTo(index) < 0;
        }
        String sDot = s.substring(0, dot);
        return index.startsWith(sDot) && s.compareTo(index) < 0;
    }
}
