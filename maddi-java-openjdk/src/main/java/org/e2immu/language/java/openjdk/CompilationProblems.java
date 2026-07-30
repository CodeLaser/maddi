package org.e2immu.language.java.openjdk;

import java.util.List;

/**
 * Thrown when fail-fast parsing meets a javac error it will not carry: see {@link MaddiDiagnosticCollector#isHalt}.
 * <p>
 * It carries the diagnostics. It used to carry nothing at all, which made it a poor instrument for the one job it
 * has — a caller that asks for fail-fast is asking "is this legal Java?", and a bare {@code CompilationProblems}
 * answered "no" without saying why. The messages went to the log and nowhere else, so every use meant reading a log
 * beside the failure, and a test asserting on it could only assert that something, somewhere, was wrong.
 */
public class CompilationProblems extends RuntimeException {
    /** How many diagnostics to name in {@link #getMessage()}; all of them stay in {@link #errors()}. */
    private static final int IN_MESSAGE = 10;

    private final List<MaddiDiagnosticCollector.MaddiDiagnostic> errors;

    public CompilationProblems(List<MaddiDiagnosticCollector.MaddiDiagnostic> errors) {
        super(message(errors));
        this.errors = List.copyOf(errors);
    }

    /** Every error that caused the halt, in the order javac reported them. */
    public List<MaddiDiagnosticCollector.MaddiDiagnostic> errors() {
        return errors;
    }

    private static String message(List<MaddiDiagnosticCollector.MaddiDiagnostic> errors) {
        StringBuilder sb = new StringBuilder(errors.size() + " compilation error(s)");
        errors.stream().limit(IN_MESSAGE).forEach(e -> sb.append("\n  ")
                .append(e.path() == null ? "?" : e.path())
                .append(':').append(e.line()).append(':').append(e.col()).append(": ")
                .append(e.msg() == null ? "" : e.msg().lines().findFirst().orElse("")));
        if (errors.size() > IN_MESSAGE) {
            sb.append("\n  ... and ").append(errors.size() - IN_MESSAGE).append(" more");
        }
        return sb.toString();
    }
}
