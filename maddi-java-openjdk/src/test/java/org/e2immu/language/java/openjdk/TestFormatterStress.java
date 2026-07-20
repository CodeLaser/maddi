package org.e2immu.language.java.openjdk;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.impl.info.ImportComputerImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.cst.print.formatter2.Formatter2Impl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

@Tag("slow")
public class TestFormatterStress extends CommonTest {

    // a grab-bag of constructs that stress a formatter: long chains, ternaries, generics,
    // switch expression, lambdas, annotations, array init, nested calls, long binary expressions.
    private static final String SRC = """
            package a.b;
            import java.util.*;
            import java.util.function.*;
            import java.util.stream.*;
            class X {
                @Deprecated
                @SuppressWarnings({"unchecked", "rawtypes"})
                static <T extends Comparable<T>> Map<String, List<T>> build(List<T> in, Function<T, String> f, int n) {
                    Map<String, List<T>> r = new HashMap<>();
                    String s = in.stream().filter(x -> x != null).map(x -> f.apply(x)).sorted().distinct()
                        .collect(Collectors.joining(", ", "[", "]"));
                    int q = n > 100 ? (n > 1000 ? 3 : 2) : (n > 10 ? 1 : 0);
                    long z = 1L + 2L + 3L + 4L + 5L + 6L + 7L + 8L + 9L + 10L + 11L + 12L + 13L + 14L + 15L + 16L;
                    String w = switch (q) {
                        case 0 -> "zero";
                        case 1, 2 -> "small";
                        default -> {
                            yield "big" + q;
                        }
                    };
                    int[] arr = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
                    for (int i = 0; i < arr.length; i++) {
                        if (arr[i] % 2 == 0 && arr[i] > 4 || arr[i] == 1) {
                            r.computeIfAbsent(w, k -> new ArrayList<>()).add(in.get(i % in.size()));
                        }
                    }
                    return r;
                }
            }
            """;

    @Test
    public void noCrashOrMalformedOutputAcrossWidths() {
        TypeInfo ti = scan("X", SRC);
        OutputBuilder ob = runtime.newCompilationUnitPrinter(ti.compilationUnit(), true)
                .print(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
        StringBuilder problems = new StringBuilder();
        for (int width = 8; width <= 160; width++) {
            FormattingOptions options = new FormattingOptionsImpl.Builder()
                    .setLengthOfLine(width).setSpacesInTab(4).build();
            Formatter formatter = new Formatter2Impl(runtime, options);
            String out;
            try {
                out = formatter.write(ob);
            } catch (Throwable t) {
                problems.append("width ").append(width).append(": THREW ").append(t).append('\n');
                continue;
            }
            for (String line : out.split("\n", -1)) {
                if (!line.isEmpty() && line.charAt(line.length() - 1) == ' ') {
                    problems.append("width ").append(width).append(": trailing space on line: [")
                            .append(line).append("]\n");
                    break;
                }
            }
        }
        if (!problems.isEmpty()) {
            org.junit.jupiter.api.Assertions.fail("formatter problems:\n" + problems);
        }
    }
}
