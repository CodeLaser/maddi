package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.StatementIndex;

// suffixes in assignment id; these act as the 3 levels for setProperty
public enum Stage {
    INITIAL(StatementIndex.INIT), // +I, + comes before '-', '.', and the digits
    EVALUATION(StatementIndex.EVAL), // -E the - comes before the digits
    MERGE(StatementIndex.MERGE); // =M, the '=' comes after the digits
    public final String label;

    Stage(String label) {
        this.label = label;
    }

    public static boolean isPresent(String s) {
        return s != null && (s.endsWith(INITIAL.label) || s.endsWith(EVALUATION.label) || s.endsWith(MERGE.label));
    }

    public static String without(String s) {
        if (isPresent(s)) {
            return s.substring(0, s.length() - 2);
        }
        return s;
    }

    public static Stage from(String stage) {
        return switch (stage) {
            case "+I" -> INITIAL;
            case "-E" -> EVALUATION;
            case "=M" -> MERGE;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public String toString() {
        return label;
    }
}
