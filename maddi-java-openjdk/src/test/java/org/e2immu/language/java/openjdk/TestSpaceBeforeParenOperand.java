package org.e2immu.language.java.openjdk;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.impl.info.ImportComputerImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.cst.print.formatter2.Formatter2Impl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
Regression: a binary operator followed by a parenthesised operand rendered as its own guide
sub-block (e.g. `(a > 0) && (b < 0)`, operator at end of line) dropped the separating space,
producing `(a > 0) &&(b < 0)`. baseSplitLevel derived the boundary's split level from the *next*
sub-block's trailing space (NO_SPACE, after `)`) instead of the *previous* sub-block's trailing
space (SPACE_IS_NICE, after `&&`).
 */
public class TestSpaceBeforeParenOperand extends CommonTest {

    private static final String SRC = """
            package a.b;
            class X {
                boolean m(int a, int b, int c, int d) {
                    boolean r = (a > 0) && (b < 0);
                    boolean s = (a > 0) || (b < 0) || (c > 0) && (d < 0) || (a < 0);
                    return r || s;
                }
            }
            """;

    private String printAt(TypeInfo ti, int width, FormattingOptions.WrapStyle style) {
        OutputBuilder ob = runtime.newCompilationUnitPrinter(ti.compilationUnit(), true)
                .print(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(width).setSpacesInTab(4).setWrapStyle(style).build();
        return new Formatter2Impl(runtime, options).write(ob);
    }

    @Test
    public void binaryOperatorKeepsSpaceBeforeParenOperand() {
        TypeInfo ti = scan("X", SRC);
        for (FormattingOptions.WrapStyle style : FormattingOptions.WrapStyle.values()) {
            String wide = printAt(ti, 120, style);
            assertTrue(wide.contains("(a > 0) && (b < 0)"),
                    "expected a space between && and (: " + style + "\n" + wide);
            // no operator should ever be glued to an opening parenthesis, at any width
            for (int width = 20; width <= 120; width++) {
                String out = printAt(ti, width, style);
                for (String op : new String[]{"&&(", "||("}) {
                    assertFalse(out.contains(op),
                            "operator glued to '(' as '" + op + "' at " + style + " width " + width + ":\n" + out);
                }
            }
        }
    }
}
