package io.codelaser.maddi.java.openjdk;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Formatter;
import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import org.junit.jupiter.api.Test;

/*
A wrapped method chain must never contain a blank line between its links. Previously baseSplitLevel
promoted the boundary between two internally-wrapped links to DOUBLE_NEWLINE, producing e.g.
    r
        .computeIfAbsent(...)

        .add(...);
The fix restricts DOUBLE_NEWLINE "breathing space" to statement/declaration blocks and top-level
groupings, so a chain (default guide) never gets it. Statement-level blank lines are unaffected.
 */
public class TestChainNoBlankLine extends CommonTest {

    private static final String SRC = """
            package a.b;
            import java.util.*;
            class X {
                void m(Map<String, List<String>> r, List<String> in, String s, int i) {
                    r.computeIfAbsent(s, k -> new ArrayList<>()).add(in.get(i % in.size()));
                }
            }
            """;

    private String printAt(TypeInfo ti, int width) {
        OutputBuilder ob = runtime.newCompilationUnitPrinter(ti.compilationUnit(), true)
                .print(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(width).setSpacesInTab(4).build();
        return new Formatter2Impl(runtime, options).write(ob);
    }

    @Test
    public void noBlankLineBeforeAChainContinuation() {
        TypeInfo ti = scan("a.b.X", SRC);
        for (int width = 12; width <= 40; width++) {
            String out = printAt(ti, width);
            String[] lines = out.split("\n", -1);
            for (int i = 1; i < lines.length; i++) {
                if (lines[i - 1].isBlank() && lines[i].strip().startsWith(".")) {
                    org.junit.jupiter.api.Assertions.fail(
                            "blank line before a chain continuation at width " + width + ":\n" + out);
                }
            }
        }
    }
}
