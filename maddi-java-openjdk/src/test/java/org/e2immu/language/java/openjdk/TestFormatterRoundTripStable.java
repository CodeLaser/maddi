package org.e2immu.language.java.openjdk;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.impl.info.ImportComputerImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.cst.print.formatter2.Formatter2Impl;
import org.junit.jupiter.api.Test;

/*
Formatting must never corrupt tokens. At any page width the formatted output, re-parsed and
re-printed at a canonical width, must equal the canonical print of the original. This catches
width-dependent token merges/splits (e.g. a lost separator space gluing `throws` onto the next
type, or a break inside an identifier) that would silently produce invalid or different Java.
 */
public class TestFormatterRoundTripStable extends CommonTest {

    private static final String SRC = """
            package a.b;
            import java.util.*;
            import java.util.function.*;
            import java.util.stream.*;
            class X {
                static <T extends Comparable<T>> Map<String, List<T>> build(List<T> in, Function<T, String> f, int n)
                        throws RuntimeException, IllegalStateException {
                    Map<String, List<T>> r = new HashMap<>();
                    String s = in.stream().filter(x -> x != null).map(f).sorted().distinct()
                        .collect(Collectors.joining(", ", "[", "]"));
                    int q = n > 100 ? (n > 1000 ? 3 : 2) : (n > 10 ? 1 : 0);
                    long z = 1L + 2L + 3L + 4L + 5L + 6L + 7L + 8L + 9L + 10L + 11L + 12L + 13L + 14L + 15L;
                    for (int i = 0; i < n; i++) {
                        if (i % 2 == 0 && i > 4 || i == 1) {
                            r.computeIfAbsent(s, k -> new ArrayList<>()).add(in.get(i % in.size()));
                        }
                    }
                    return r;
                }
            }
            """;

    private String printAt(TypeInfo ti, org.e2immu.language.cst.api.runtime.Runtime rt, int width,
                           FormattingOptions.WrapStyle style) {
        OutputBuilder ob = rt.newCompilationUnitPrinter(ti.compilationUnit(), true)
                .print(new ImportComputerImpl(), rt.qualificationQualifyFromPrimaryType());
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(width).setSpacesInTab(4).setWrapStyle(style).build();
        return new Formatter2Impl(rt, options).write(ob);
    }

    // Parse in a FRESH inspector each time: a given fqn can only be inspected once per runtime.
    private String parseAndPrint(String src, int width, FormattingOptions.WrapStyle style) {
        TestFormatterRoundTripStable h = new TestFormatterRoundTripStable();
        TypeInfo ti = h.scan("X", src);
        return h.printAt(ti, h.runtime, width, style);
    }

    @Test
    public void formattingNeverCorruptsTokens() {
        // The canonical meaning of the program, printed wide with the default style.
        String canonical = parseAndPrint(SRC, 120, FormattingOptions.WrapStyle.CHOP_DOWN);

        StringBuilder problems = new StringBuilder();
        for (FormattingOptions.WrapStyle style : FormattingOptions.WrapStyle.values()) {
            for (int width : new int[]{16, 20, 24, 30, 40, 50, 60, 72, 80, 100, 120}) {
                String formatted = parseAndPrint(SRC, width, style);
                String label = style + " width " + width;
                try {
                    String reprinted = parseAndPrint(formatted, 120, FormattingOptions.WrapStyle.CHOP_DOWN);
                    if (!canonical.equals(reprinted)) {
                        problems.append(label)
                                .append(": reprint differs from canonical.\n--- formatted ---\n")
                                .append(formatted).append("\n--- reprinted@120 ---\n").append(reprinted)
                                .append("\n--- canonical@120 ---\n").append(canonical).append('\n');
                    }
                } catch (Throwable t) {
                    problems.append(label)
                            .append(": formatted output failed to re-parse: ").append(t)
                            .append("\n--- formatted ---\n").append(formatted).append('\n');
                }
            }
        }
        if (!problems.isEmpty()) {
            org.junit.jupiter.api.Assertions.fail(problems.toString());
        }
    }
}
