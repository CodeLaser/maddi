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
import org.e2immu.language.cst.print.formatter.TestFormatter3;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Test3 {
    private final Runtime runtime = new RuntimeImpl();

    @Test
    public void test1() {
        OutputBuilder outputBuilder = TestFormatter3.createExample0();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(130).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        @Language("java")
        String expect = """
                package org.e2immu.analyser.parser.failing.testexample;
                import java.util.stream.Stream;
                import org.e2immu.annotation.NotModified;
                import org.e2immu.annotation.NotNull;
                @E2Container
                @ExtensionClass
                public class Basics_5 {
                    @NotModified
                    @NotNull
                    public static String add(@NotNull String input) {
                        return Stream.of(input).map(s -> { if(s == null) { return "null"; } return s + "something"; }).findAny().get();
                    }
                }
                """;
        assertEquals(expect, string);
    }

    @Test
    public void test2() {
        OutputBuilder outputBuilder = TestFormatter3.createExample0();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(80).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        @Language("java")
        String expect = """
                package org.e2immu.analyser.parser.failing.testexample;
                import java.util.stream.Stream;
                import org.e2immu.annotation.NotModified;
                import org.e2immu.annotation.NotNull;
                @E2Container
                @ExtensionClass
                public class Basics_5 {
                    @NotModified
                    @NotNull
                    public static String add(@NotNull String input) {
                        return Stream
                            .of(input)
                            .map(s -> { if(s == null) { return "null"; } return s + "something"; })
                            .findAny()
                            .get();
                    }
                }
                """;
        assertEquals(expect, string);
    }


    @Test
    public void test3() {
        OutputBuilder outputBuilder = TestFormatter3.createExample0();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(60).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        @Language("java")
        String expect = """
                package org.e2immu.analyser.parser.failing.testexample;
                import java.util.stream.Stream;
                import org.e2immu.annotation.NotModified;
                import org.e2immu.annotation.NotNull;
                @E2Container
                @ExtensionClass
                public class Basics_5 {
                    @NotModified
                    @NotNull
                    public static String add(@NotNull String input) {
                        return Stream
                            .of(input)
                            .map(s -> {
                                if(s == null) { return "null"; }
                                return s + "something";
                            })
                            .findAny()
                            .get();
                    }
                }
                """;
        assertEquals(expect, string);
    }
}
