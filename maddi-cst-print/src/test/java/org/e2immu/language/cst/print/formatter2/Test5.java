/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.language.cst.print.formatter2;

import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.cst.print.formatter.TestFormatter5;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Test5 {
    private final Runtime runtime = new RuntimeImpl();

    @Test
    public void test0() {
        OutputBuilder outputBuilder = TestFormatter5.createExample0();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        String expect = """
                /*line 1 line 2*/ @ImmutableContainer /*IMPLIED*/ @NotNull /*OK*/
                """;
        assertEquals(expect, string);
    }

    @Test
    public void test1() {
        OutputBuilder outputBuilder = TestFormatter5.createExample1();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        String expect = """
                /*
                    line 1 is much longer than in the previous example, we want to force everything
                    on multiple lines. So therefore, line 2 is also rather long
                    */
                    @ImmutableContainer /*IMPLIED*/
                    @NotNull /*OK*/
                """;
        assertEquals(expect, string);
    }

    @Language("java")
    static final String EXPECT_2 = """
            /*
            should raise a warning that the condition is always false, plus that b is never used
            as a consequence, default always returns "c" so we have @NotNull
            */
            @ImmutableContainer /*IMPLIED*/
            @NotNull /*OK*/
            public static String method(char c, String b) {
                return switch(c) { a -> "a"; b -> "b"; default -> c == 'a' || c == 'b' ? b : "c";
                    }; /*inline conditional evaluates to constant*/
            }
            """;

    @Test
    public void test2() {
        OutputBuilder outputBuilder = TestFormatter5.createExample2();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        // NOTE: this is not wrong, there are no guides in the whole return statement, so the splitting is handled
        // by the handleElement algorithm
        assertEquals(EXPECT_2, string);
    }


    @Language("java")
    static final String EXPECT_2b = """
            package org.e2immu.analyser.parser.conditional.testexample;
            import org.e2immu.annotation.ImmutableContainer;
            import org.e2immu.annotation.NotNull;
            @ImmutableContainer
            public class SwitchExpression_1 {
                /*
                should raise a warning that the condition is always false, plus that b is never used
                as a consequence, default always returns "c" so we have @NotNull
                */
                @ImmutableContainer /*IMPLIED*/
                @NotNull /*OK*/
                public static String method(char c, String b) {
                    return switch(c) {
                        a -> "a";
                        b -> "b";
                        default -> c == 'a' || c == 'b' ? b : "c";
                    }; /*inline conditional evaluates to constant*/
                }
            }
            """;


    @Test
    public void test2b() {
        OutputBuilder outputBuilder = TestFormatter5.createExample2b();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(70).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        assertEquals(EXPECT_2b, string);
    }
}
