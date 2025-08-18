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
