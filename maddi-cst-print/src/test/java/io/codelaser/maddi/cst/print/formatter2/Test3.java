/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.print.formatter2;

import io.codelaser.maddi.cst.api.output.Formatter;
import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.output.*;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Test3 {
    private final Runtime runtime = new RuntimeImpl();


    public static OutputBuilder createExample0() {
        GuideImpl.GuideGenerator gg31 = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator gg21 = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator gg30 = GuideImpl.generatorForAnnotationList();

        GuideImpl.GuideGenerator gg22 = GuideImpl.defaultGuideGenerator();
        GuideImpl.GuideGenerator gg23 = GuideImpl.generatorForParameterDeclaration();
        GuideImpl.GuideGenerator gg24 = GuideImpl.generatorForBlock();

        GuideImpl.GuideGenerator gg25 = GuideImpl.defaultGuideGenerator(); // fluent method call sequence
        GuideImpl.GuideGenerator gg28 = GuideImpl.generatorForBlock(); // opening of lambda
        GuideImpl.GuideGenerator gg29 = GuideImpl.generatorForBlock();

        return new OutputBuilderImpl().add(new TextImpl("package"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("org.e2immu.analyser.parser.failing.testexample"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(new TextImpl("import"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("java.util.stream.Stream"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(new TextImpl("import"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("io.codelaser.maddi.annotation.NotModified"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(new TextImpl("import"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("io.codelaser.maddi.annotation.NotNull"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(gg31.start()) // priority=false, startNL=false, endNL=false
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("E2Container"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(gg31.mid()) // priority=false, startNL=false, endNL=false
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("ExtensionClass"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(gg31.mid()) // priority=false, startNL=false, endNL=false
                .add(new TextImpl("public"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("class"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Basics_5"))
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg21.start()) // priority=true, startNL=true, endNL=true
                .add(gg30.start()) // priority=false, startNL=false, endNL=false
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("NotModified"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(gg30.mid()) // priority=false, startNL=false, endNL=false
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("NotNull"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(gg30.mid()) // priority=false, startNL=false, endNL=false
                .add(gg22.start()) // priority=false, startNL=false, endNL=false
                .add(new TextImpl("public"))
                .add(SpaceEnum.ONE)
                .add(gg22.mid()) // priority=false, startNL=false, endNL=false
                .add(new TextImpl("static"))
                .add(gg22.end()) // priority=false, startNL=false, endNL=false
                .add(SpaceEnum.ONE)
                .add(new TextImpl("String"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("add"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(gg23.start()) // priority=false, startNL=true, endNL=false
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("NotNull"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("String"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("input"))
                .add(gg23.end()) // priority=false, startNL=true, endNL=false
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg24.start()) // priority=true, startNL=true, endNL=true
                .add(new TextImpl("return"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Stream"))
                .add(gg25.start()) // priority=false, startNL=false, endNL=false
                .add(SymbolEnum.DOT)
                .add(new TextImpl("of"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new QualifiedNameImpl("input"))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(gg25.mid()) // priority=false, startNL=false, endNL=false
                .add(SymbolEnum.DOT)
                .add(new TextImpl("map"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new QualifiedNameImpl("s"))
                .add(SymbolEnum.binaryOperator("->"))
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg28.start()) // priority=false, startNL=false, endNL=false
                .add(new TextImpl("if"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new QualifiedNameImpl("s"))
                .add(SymbolEnum.binaryOperator("=="))
                .add(new TextImpl("null"))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg29.start()) // priority=true, startNL=true, endNL=true
                .add(new TextImpl("return"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("\"null\""))
                .add(SymbolEnum.SEMICOLON)
                .add(gg29.end()) // priority=true, startNL=true, endNL=true
                .add(SymbolEnum.RIGHT_BRACE)
                .add(gg28.mid()) // priority=false, startNL=false, endNL=false
                .add(new TextImpl("return"))
                .add(SpaceEnum.ONE)
                .add(new QualifiedNameImpl("s"))
                .add(SymbolEnum.binaryOperator("+"))
                .add(new TextImpl("\"something\""))
                .add(SymbolEnum.SEMICOLON)
                .add(gg28.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(gg25.mid()) // priority=false, startNL=false, endNL=false
                .add(SymbolEnum.DOT)
                .add(new TextImpl("findAny"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(gg25.mid()) // priority=false, startNL=false, endNL=false
                .add(SymbolEnum.DOT)
                .add(new TextImpl("get"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(gg25.end()) // priority=false, startNL=false, endNL=false
                .add(SymbolEnum.SEMICOLON)
                .add(gg24.end()) // priority=true, startNL=true, endNL=true
                .add(SymbolEnum.RIGHT_BRACE)
                .add(gg30.end()) // priority=false, startNL=false, endNL=false
                .add(gg21.end()) // priority=true, startNL=true, endNL=true
                .add(SymbolEnum.RIGHT_BRACE)
                .add(gg31.end()); // priority=false, startNL=false, endNL=false
    }

    @Test
    public void test1() {
        OutputBuilder outputBuilder = createExample0();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(130).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        @Language("java")
        String expect = """
                package org.e2immu.analyser.parser.failing.testexample;
                import java.util.stream.Stream;
                import io.codelaser.maddi.annotation.NotModified;
                import io.codelaser.maddi.annotation.NotNull;
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
        OutputBuilder outputBuilder = createExample0();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(80).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        @Language("java")
        String expect = """
                package org.e2immu.analyser.parser.failing.testexample;
                import java.util.stream.Stream;
                import io.codelaser.maddi.annotation.NotModified;
                import io.codelaser.maddi.annotation.NotNull;
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
        OutputBuilder outputBuilder = createExample0();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(60).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        @Language("java")
        String expect = """
                package org.e2immu.analyser.parser.failing.testexample;
                import java.util.stream.Stream;
                import io.codelaser.maddi.annotation.NotModified;
                import io.codelaser.maddi.annotation.NotNull;
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
