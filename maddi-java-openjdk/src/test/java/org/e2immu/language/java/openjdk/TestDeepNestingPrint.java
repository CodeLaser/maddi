package org.e2immu.language.java.openjdk;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.impl.info.ImportComputerImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.cst.print.formatter2.Formatter2Impl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
Hardening regression: deeply-nested blocks printed on a narrow page must degrade gracefully.
Before the MIN_CONTENT_WIDTH floor in BlockPrinter, a 10-deep if-nest at width 40/30 went off the
rails: dangling `if(` at end of line, expressions shattered one-token-per-line (`p =` / `p +` / `9;`),
and spurious blank lines between statements. With the floor, each statement stays intact on its own
(deeply-indented) line.
 */
public class TestDeepNestingPrint extends CommonTest {

    private static String deeplyNested(int n) {
        StringBuilder sb = new StringBuilder("class X {\n  void m(int p) {\n");
        for (int i = 0; i < n; i++) {
            sb.append("    ".repeat(i + 1)).append("if (p > ").append(i).append(") {\n");
            sb.append("    ".repeat(i + 2)).append("p = p + ").append(i).append(";\n");
        }
        for (int i = n - 1; i >= 0; i--) {
            sb.append("    ".repeat(i + 1)).append("}\n");
        }
        sb.append("  }\n}\n");
        return sb.toString();
    }

    private String printAt(TypeInfo ti, int width) {
        OutputBuilder ob = runtime.newCompilationUnitPrinter(ti.compilationUnit(), true)
                .print(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(width).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        return formatter.write(ob);
    }

    @Test
    public void deepNestingDoesNotShatterOnNarrowPage() {
        TypeInfo ti = scan("X", deeplyNested(10));
        for (int width : new int[]{40, 30, 20}) {
            String out = printAt(ti, width);
            String msg = "width " + width + ":\n" + out;

            // no spurious blank lines inside the body
            assertFalse(out.contains("\n\n"), "unexpected blank line at " + msg);

            for (String line : out.split("\n")) {
                String t = line.strip();
                // no dangling opening constructs
                assertFalse(t.endsWith("if(") || t.endsWith("if ("), "dangling if-open at " + msg);
                assertFalse(t.equals("=") || t.endsWith("=") || t.equals("+") || t.endsWith("+"),
                        "operator stranded at end of line at " + msg);
            }

            // every statement and condition stays intact on a single line
            for (int i = 0; i < 10; i++) {
                assertTrue(out.contains("if(p > " + i + ") {"), "condition " + i + " was broken at " + msg);
                assertTrue(out.contains("p = p + " + i + ";"), "statement " + i + " was broken at " + msg);
            }
        }
    }
}
