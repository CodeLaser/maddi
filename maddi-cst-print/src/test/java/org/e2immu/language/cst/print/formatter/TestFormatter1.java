/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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

package org.e2immu.language.cst.print.formatter;

import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.OutputElement;
import org.e2immu.language.cst.api.output.element.Guide;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.cst.print.FormatterImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestFormatter1 {
    private final Runtime runtime = new RuntimeImpl();
    
    // public int method  (17 chars)
    private OutputBuilder createExample0() {
        return new OutputBuilderImpl()
                .add(KeywordImpl.PUBLIC).add(SpaceEnum.ONE)
                .add(new TextImpl("int")).add(SpaceEnum.ONE)
                .add(new TextImpl("method"));
    }

    // public int method(int p1, int p2) { return p1+p2; }
    //        10|     18|            33|
    public static OutputBuilder createExample1() {
        GuideImpl.GuideGenerator gg = GuideImpl.generatorForParameterDeclaration();
        GuideImpl.GuideGenerator gg2 = GuideImpl.generatorForBlock();

        return new OutputBuilderImpl()
                .add(KeywordImpl.PUBLIC).add(SpaceEnum.ONE)
                .add(new TextImpl("int")).add(SpaceEnum.ONE)
                .add(new TextImpl("method"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(gg.start()).add(new TextImpl("int")).add(SpaceEnum.ONE).add(new TextImpl("p1")).add(SymbolEnum.COMMA)
                .add(gg.mid()).add(new TextImpl("int")).add(SpaceEnum.ONE).add(new TextImpl("p2"))
                .add(gg.end())
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg2.start()).add(KeywordImpl.RETURN).add(SpaceEnum.ONE)
                .add(new TextImpl("p1")).add(SymbolEnum.binaryOperator("+")).add(new TextImpl("p2")).add(SymbolEnum.SEMICOLON)
                .add(gg2.end())
                .add(SymbolEnum.RIGHT_BRACE);
    }

    @Test
    public void testLineSplit1() {
        String PACKAGE = "org.e2immu.analyser.output";
        OutputBuilder outputBuilder = new OutputBuilderImpl().add(KeywordImpl.PACKAGE).add(SpaceEnum.ONE).add(new TextImpl(PACKAGE));
        assertEquals("package " + PACKAGE, outputBuilder.toString());
        Assertions.assertEquals("package " + PACKAGE + "\n", new FormatterImpl(runtime, FormattingOptionsImpl.DEFAULT).write(outputBuilder));

        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(20)
                .setSpacesInTab(2).setTabsForLineSplit(2).build();
        Formatter formatter = new FormatterImpl(runtime, options);

        List<ForwardInfo> info = new ArrayList<>();
        Forward.forward(runtime, options, outputBuilder.list(), fi -> {
            info.add(fi);
            System.out.println(fi);
            return false;
        }, 0, options.lengthOfLine() + 20);
        assertEquals(2, info.size());
        assertEquals(" " + PACKAGE, info.get(1).string());

        assertEquals("package\n    " + PACKAGE + "\n", formatter.write(outputBuilder));
    }

    @Test
    public void testLineSplit2() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(8)
                .setSpacesInTab(2).setTabsForLineSplit(1).build();
        OutputBuilder outputBuilder = new OutputBuilderImpl()
                .add(KeywordImpl.PUBLIC).add(SpaceEnum.ONE)
                .add(KeywordImpl.STATIC).add(SpaceEnum.ONE)
                .add(KeywordImpl.ABSTRACT).add(SpaceEnum.ONE)
                .add(new TextImpl("method")).add(SymbolEnum.SEMICOLON);

        List<ForwardInfo> info = new ArrayList<>();
        Forward.forward(runtime, options, outputBuilder.list(), fi -> {
            info.add(fi);
            System.out.println(fi);
            return false;
        }, 0, 120);
        assertEquals(5, info.size());

        assertEquals("public\n  static\n  abstract\n  method;\n",
                new FormatterImpl(runtime, options).write(outputBuilder));
    }

    @Test
    public void testGuide1() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(20)
                .setSpacesInTab(2).setTabsForLineSplit(2).build();
        assertEquals("""
                        public int method(
                          int p1,
                          int p2) {
                          return p1 + p2;
                        }
                        """,
                new FormatterImpl(runtime, options).write(createExample1()));
    }

    @Test
    public void testGuide1LongLine() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120)
                .setSpacesInTab(2).setTabsForLineSplit(2).build();

        // the space is recognized by the forward method
        List<ForwardInfo> info = new ArrayList<>();
        Forward.forward(runtime, options, createExample1().list(), fi -> {
            info.add(fi);
            System.out.println(fi);
            return false;
        }, 0, 120);
        assertEquals(",", info.get(7).string());
        assertNull(info.get(8).string());
        assertEquals(" int", info.get(9).string());

        //assertEquals(53, new FormatterImpl(runtime, options).lookAhead(createExample1().list, 120));

        assertEquals("public int method(int p1, int p2) { return p1 + p2; }\n",
                new FormatterImpl(runtime, options).write(createExample1()));
    }

    @Test
    public void testGuide1Compact() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120)
                .setSpacesInTab(2).setTabsForLineSplit(2).setCompact(true).build();
        assertEquals("public int method(int p1,int p2){return p1+p2;}\n",
                new FormatterImpl(runtime, options).write(createExample1()));
    }

    public static OutputBuilder createExample2() {
        GuideImpl.GuideGenerator gg = GuideImpl.generatorForParameterDeclaration();
        GuideImpl.GuideGenerator gg1 = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator gg2 = GuideImpl.defaultGuideGenerator();

        return new OutputBuilderImpl()
                .add(new TextImpl("public")).add(SpaceEnum.ONE)
                .add(new TextImpl("int")).add(SpaceEnum.ONE)
                .add(new TextImpl("method"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(gg.start()).add(new TextImpl("int")).add(SpaceEnum.ONE).add(new TextImpl("p1")).add(SymbolEnum.COMMA)
                .add(gg.mid()).add(new TextImpl("int")).add(SpaceEnum.ONE).add(new TextImpl("p2")).add(SymbolEnum.COMMA)
                .add(gg.mid()).add(new TextImpl("double")).add(SpaceEnum.ONE).add(new TextImpl("somewhatLonger")).add(SymbolEnum.COMMA)
                .add(gg.mid()).add(new TextImpl("double")).add(SpaceEnum.ONE).add(new TextImpl("d"))
                .add(gg.end())
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg1.start()).add(new TextImpl("log")).add(SymbolEnum.LEFT_PARENTHESIS)
                .add(gg2.start()).add(new TextImpl("p1")).add(SymbolEnum.COMMA)
                .add(gg2.mid()).add(new TextImpl("p2")).add(gg2.end()).add(SymbolEnum.RIGHT_PARENTHESIS).add(SymbolEnum.SEMICOLON)
                .add(gg1.mid()).add(new TextImpl("return")).add(SpaceEnum.ONE)
                .add(new TextImpl("p1")).add(SymbolEnum.binaryOperator("+")).add(new TextImpl("p2")).add(SymbolEnum.SEMICOLON)
                .add(gg1.end())
                .add(SymbolEnum.RIGHT_BRACE);
    }

    @Test
    public void testGuide2() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(20)
                .setSpacesInTab(2).setTabsForLineSplit(2).build();
        assertEquals("""
                        public int method(
                          int p1,
                          int p2,
                          double
                              somewhatLonger,
                          double d) {
                          log(p1, p2);
                          return p1 + p2;
                        }
                        """,
                //      01234567890123456789
                new FormatterImpl(runtime, options).write(createExample2()));
    }

    @Test
    public void testGuide2MidLine() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(80).setCompact(false).build();
        // around 90 characters long
        assertEquals("""
                        public int method(int p1, int p2, double somewhatLonger, double d) {
                            log(p1, p2);
                            return p1 + p2;
                        }
                        """,
                new FormatterImpl(runtime, options).write(createExample2()));
    }

    @Test
    public void testGuide2LongLine() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setCompact(false).build();
        // around 90 characters long
        assertEquals("public int method(int p1, int p2, double somewhatLonger, double d) { log(p1, p2); return p1 + p2; }\n",
                new FormatterImpl(runtime, options).write(createExample2()));
    }

    @Test
    public void testGuide2Compact() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setCompact(true).build();
        // around 90 characters long

        List<ForwardInfo> info = new ArrayList<>();
        Forward.forward(runtime, options, createExample2().list(), fi -> {
            info.add(fi);
            System.out.println(fi);
            return false;
        }, 0, 120);
        assertEquals(41, info.size());
        assertEquals(" somewhatLonger", info.get(14).string());
        assertNull(info.get(16).string()); // ensure that the MID is there

        //assertEquals(89, new FormatterImpl(runtime, options).lookAhead(createExample2().list, 120));

        assertEquals("public int method(int p1,int p2,double somewhatLonger,double d){log(p1,p2);return p1+p2;}\n",
                new FormatterImpl(runtime, options).write(createExample2()));
    }

    public static OutputBuilder createExample3() {
        GuideImpl.GuideGenerator gg = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator gg1 = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator gg2 = GuideImpl.generatorForBlock();

        return new OutputBuilderImpl()
                .add(new TextImpl("try")).add(SymbolEnum.LEFT_BRACE)
                .add(gg.start())
                .add(new TextImpl("if")).add(SymbolEnum.LEFT_PARENTHESIS).add(new TextImpl("a")).add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg1.start()).add(new TextImpl("assert")).add(SpaceEnum.ONE).add(new TextImpl("b")).add(SymbolEnum.SEMICOLON).add(gg1.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(new TextImpl("else")).add(SymbolEnum.LEFT_BRACE)
                .add(gg2.start()).add(new TextImpl("assert")).add(SpaceEnum.ONE).add(new TextImpl("c")).add(SymbolEnum.SEMICOLON)
                .add(gg2.mid()).add(new TextImpl("exit")).add(SymbolEnum.LEFT_PARENTHESIS).add(new TextImpl("1")).add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.SEMICOLON).add(gg2.end()).add(SymbolEnum.RIGHT_BRACE)
                .add(gg.end())
                .add(SymbolEnum.RIGHT_BRACE);
    }

    // check that the lookahead does not go into line split mode with a { ... } guide
    @Test
    public void testGuide3() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(20)
                .setSpacesInTab(2).setTabsForLineSplit(2).build();
        assertEquals("""
                        try {
                          if(a) {
                            assert b;
                          } else {
                            assert c;
                            exit(1);
                          }
                        }
                        """,
                //      01234567890123456789
                //        if(a) { assert b; } -> the end of the guide is within 20...
                new FormatterImpl(runtime, options).write(createExample3()));
    }

    // identical at 30... if the whole statement doesn't fit in 30, then it gets split on {
    @Test
    public void testGuide3ShortLine() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(30)
                .setSpacesInTab(2).setTabsForLineSplit(2).build();
        assertEquals("""
                        try {
                          if(a) {
                            assert b;
                          } else {
                            assert c;
                            exit(1);
                          }
                        }
                        """,
                new FormatterImpl(runtime, options).write(createExample3()));
    }


    @Test
    public void testGuide3Midline() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(80)
                .setSpacesInTab(2).setTabsForLineSplit(2).build();
        assertEquals("try { if(a) { assert b; } else { assert c; exit(1); } }\n",
                new FormatterImpl(runtime, options).write(createExample3()));
    }

    @Test
    public void testGuide3Compact() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setCompact(true).build();
        assertEquals("try{if(a){assert b;}else{assert c;exit(1);}}\n",
                new FormatterImpl(runtime, options).write(createExample3()));
    }

    private OutputBuilder createExample4() {
        GuideImpl.GuideGenerator ggA = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator gg = GuideImpl.generatorForParameterDeclaration();
        GuideImpl.GuideGenerator gg2 = GuideImpl.generatorForBlock();

        return new OutputBuilderImpl()
                .add(ggA.start()).add(SymbolEnum.AT).add(new TextImpl("NotModified")).add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(ggA.mid()).add(SymbolEnum.AT).add(new TextImpl("Independent")).add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(ggA.mid()).add(SymbolEnum.AT).add(new TextImpl("NotNull")).add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(ggA.mid()).add(new TextImpl("public")).add(SpaceEnum.ONE).add(new TextImpl("int")).add(SpaceEnum.ONE)
                .add(new TextImpl("method"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(gg.start()).add(new TextImpl("int")).add(SpaceEnum.ONE).add(new TextImpl("p1")).add(SymbolEnum.COMMA)
                .add(gg.mid()).add(new TextImpl("int")).add(SpaceEnum.ONE).add(new TextImpl("p2"))
                .add(gg.end())
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg2.start()).add(new TextImpl("return")).add(SpaceEnum.ONE)
                .add(new TextImpl("p1")).add(SymbolEnum.binaryOperator("+")).add(new TextImpl("p2")).add(SymbolEnum.SEMICOLON)
                .add(gg2.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(ggA.end());
    }

    @Test
    public void testGuide4() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(20)
                .setSpacesInTab(2).setTabsForLineSplit(2).build();
        assertEquals("""
                        @NotModified
                        @Independent
                        @NotNull
                        public int method(
                          int p1,
                          int p2) {
                          return p1 + p2;
                        }
                        """,
                //      01234567890123456789
                //        if(a) { assert b; } -> the end of the guide is within 20...
                new FormatterImpl(runtime, options).write(createExample4()));
    }

    @Test
    public void testGuide4Compact() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setCompact(true).build();
        Formatter formatter = new FormatterImpl(runtime, options);
        List<OutputElement> list = createExample4().list();
        assertEquals(41, list.size());

        List<ForwardInfo> info = new ArrayList<>();
        Forward.forward(runtime, options, list, fi -> {
            info.add(fi);
            System.out.println(fi);
            return false;
        }, 0, 120);
        assertNull(info.get(30).string()); // end of } guide
        assertEquals("}", info.get(31).string());
        assertNull(info.get(32).string()); // end of annotation guide

        assertEquals("@NotModified @Independent @NotNull public int method(int p1,int p2){return p1+p2;}\n",
                formatter.write(createExample4()));
    }

    private OutputBuilder createExample5(boolean extended) {
        GuideImpl.GuideGenerator ggA = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator gg = GuideImpl.defaultGuideGenerator();
        GuideImpl.GuideGenerator gg2 = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator ggA2 = GuideImpl.generatorForAnnotationList();

        OutputBuilder outputBuilder = new OutputBuilderImpl()
                .add(ggA.start())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("E2Container"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(ggA.mid())
                .add(new TextImpl("public"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("class"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Basics_0"))
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg2.start())
                .add(ggA2.start())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Constant"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new TextImpl("\"abc\""))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(ggA2.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("E2Container"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new TextImpl("absent"))
                .add(SymbolEnum.binaryOperator("="))
                .add(new TextImpl("true"))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(ggA2.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Final"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new TextImpl("absent"))
                .add(SymbolEnum.binaryOperator("="))
                .add(new TextImpl("true"))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(ggA2.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("NotNull"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(ggA2.mid())
                .add(gg.start())
                .add(new TextImpl("private"))
                .add(SpaceEnum.ONE)
                .add(gg.mid())
                .add(new TextImpl("final"))
                .add(gg.end())
                .add(SpaceEnum.ONE)
                .add(new TextImpl("String"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("explicitlyFinal"))
                .add(SymbolEnum.binaryOperator("="))
                .add(new TextImpl("\"abc\""))
                .add(SymbolEnum.SEMICOLON)
                .add(ggA2.end());
        if (extended) {
            outputBuilder
                    .add(gg2.mid())
                    .add(new TextImpl("private"))
                    .add(SpaceEnum.ONE)
                    .add(new TextImpl("String"))
                    .add(SpaceEnum.ONE)
                    .add(new TextImpl("nonFinal"))
                    .add(SymbolEnum.binaryOperator("="))
                    .add(new TextImpl("\"xyz\""))
                    .add(SymbolEnum.SEMICOLON);
        }
        return outputBuilder.add(gg2.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(ggA.end());
    }

    @Test
    public void testGuide5() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setCompact(false).build();
        Formatter formatter = new FormatterImpl(runtime, options);
        List<OutputElement> list = createExample5(false).list();

        // the two guides one after the other should not result in a blank line
        assertInstanceOf(Guide.class, list.get(55));
        assertInstanceOf(Guide.class, list.get(56));
        assertEquals(SymbolEnum.RIGHT_BRACE, list.get(57));
        assertInstanceOf(Guide.class, list.get(58));

        assertEquals("""
                @E2Container
                public class Basics_0 {
                    @Constant("abc")
                    @E2Container(absent = true)
                    @Final(absent = true)
                    @NotNull
                    private final String explicitlyFinal = "abc";
                }
                """, formatter.write(createExample5(false)));
    }

    @Test
    public void testGuide5LongLine() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(400).setCompact(false).build();
        Formatter formatter = new FormatterImpl(runtime, options);

        assertEquals("""
                @E2Container public class Basics_0 { @Constant("abc") @E2Container(absent = true) @Final(absent = true) @NotNull private final String explicitlyFinal = "abc"; }
                """, formatter.write(createExample5(false)));
    }

    @Test
    public void testGuide5CompactShortLine() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(80).setCompact(true).build();
        assertEquals("""
                @E2Container
                public class Basics_0{
                @Constant("abc")
                @E2Container(absent=true)
                @Final(absent=true)
                @NotNull
                private final String explicitlyFinal="abc";
                }
                """, new FormatterImpl(runtime, options).write(createExample5(false)));
    }

    @Test
    public void testGuide6() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setCompact(false).build();
        Formatter formatter = new FormatterImpl(runtime, options);

        assertEquals("""
                @E2Container
                public class Basics_0 {
                    @Constant("abc")
                    @E2Container(absent = true)
                    @Final(absent = true)
                    @NotNull
                    private final String explicitlyFinal = "abc";
                    private String nonFinal = "xyz";
                }
                """, formatter.write(createExample5(true)));
    }


    // variant on example 1
    private OutputBuilder createExample7() {
        GuideImpl.GuideGenerator gg = GuideImpl.generatorForParameterDeclaration();
        GuideImpl.GuideGenerator gg2 = GuideImpl.generatorForBlock();

        return new OutputBuilderImpl()
                .add(new TextImpl("public")).add(SpaceEnum.ONE)
                .add(new TextImpl("int")).add(SpaceEnum.ONE)
                .add(new TextImpl("method"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(gg.start())
                .add(SymbolEnum.AT).add(new TextImpl("E2Immutable")).add(SpaceEnum.ONE)
                .add(new TextImpl("int")).add(SpaceEnum.ONE).add(new TextImpl("p1")).add(SymbolEnum.COMMA)
                .add(gg.mid()).add(new TextImpl("int")).add(SpaceEnum.ONE).add(new TextImpl("p2"))
                .add(gg.end())
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg2.start()).add(new TextImpl("return")).add(SpaceEnum.ONE)
                .add(new TextImpl("p1")).add(SymbolEnum.binaryOperator("+")).add(new TextImpl("p2")).add(SymbolEnum.SEMICOLON)
                .add(gg2.end())
                .add(SymbolEnum.RIGHT_BRACE);
    }

    @Test
    public void testGuide7Long() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(80)
                .setSpacesInTab(2).setTabsForLineSplit(2).build();
        Formatter formatter = new FormatterImpl(runtime, options);
        List<OutputElement> list = createExample7().list();
        assertEquals(30, list.size());

        List<ForwardInfo> info = new ArrayList<>();
        Forward.forward(runtime, options, list, fi -> {
            info.add(fi);
            System.out.println(fi);
            return false;
        }, 0, 120);
        assertEquals("@", info.get(5).string());

        assertEquals("public int method(@E2Immutable int p1, int p2) { return p1 + p2; }\n",
                formatter.write(createExample7()));
    }

    // public method(int p1, int p2);
    @Test
    public void testForward1() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(8)
                .setSpacesInTab(2).setTabsForLineSplit(1).build();
        OutputBuilder outputBuilder = new OutputBuilderImpl()
                .add(new TextImpl("public")) // 0
                .add(SpaceEnum.ONE) //1
                .add(SpaceEnum.ONE) //2
                .add(new TextImpl("method")) // 3
                .add(SymbolEnum.LEFT_PARENTHESIS) // 4
                .add(new TextImpl("int")) //5
                .add(SpaceEnum.ONE) //6
                .add(new TextImpl("p1")) // 7
                .add(SymbolEnum.COMMA) // 8
                .add(new TextImpl("int"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("p2")) // 11
                .add(SymbolEnum.RIGHT_PARENTHESIS) // 12
                .add(SymbolEnum.SEMICOLON); // 13
        List<ForwardInfo> info = new ArrayList<>();
        Forward.forward(runtime, options, outputBuilder.list(), fi -> {
            info.add(fi);
            System.out.println(fi);
            return false;
        }, 0, 100);
        assertEquals("public", info.get(0).string());
        assertEquals(3, info.get(1).pos()); // pos 1, 2 have been skipped
        assertEquals(6, info.get(1).chars()); // 6 chars have been written before this space
    }

    // !a && b == c
    @Test
    public void testForward2() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(8)
                .setSpacesInTab(2).setTabsForLineSplit(1).build();
        OutputBuilder outputBuilder = new OutputBuilderImpl()
                .add(SymbolEnum.UNARY_BOOLEAN_NOT) // 0
                .add(new TextImpl("a")) //1
                .add(SymbolEnum.binaryOperator("&&")) //2
                .add(new TextImpl("b")) // 3
                .add(SymbolEnum.binaryOperator("==")) // 4
                .add(new TextImpl("c")) //5
                .add(SymbolEnum.SEMICOLON); // 6
        List<ForwardInfo> info = new ArrayList<>();
        Forward.forward(runtime, options, outputBuilder.list(), fi -> {
            info.add(fi);
            System.out.println(fi);
            return false;
        }, 0, 100);
        assertEquals("!", info.get(0).string());
    }

    // a = { { "b", "c" }, "d" };
    @Test
    public void testForward3() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(8)
                .setSpacesInTab(2).setTabsForLineSplit(1).build();
        OutputBuilder outputBuilder = new OutputBuilderImpl()
                .add(new TextImpl("a")) // 0
                .add(SymbolEnum.binaryOperator("=")) //1
                .add(SymbolEnum.LEFT_BRACE) // 2
                .add(SymbolEnum.LEFT_BRACE)//3
                .add(new TextImpl("\"b\"")) // 4
                .add(SymbolEnum.COMMA)
                .add(new TextImpl("\"c\"")) // 6
                .add(SymbolEnum.RIGHT_BRACE)
                .add(SymbolEnum.COMMA)
                .add(new TextImpl("\"d\"")) // 9
                .add(SymbolEnum.RIGHT_BRACE)
                .add(SymbolEnum.SEMICOLON); // 11
        List<ForwardInfo> info = new ArrayList<>();
        Forward.forward(runtime, options, outputBuilder.list(), fi -> {
            info.add(fi);
            System.out.println(fi);
            return false;
        }, 0, 100);
        assertEquals("a", info.get(0).string());
    }

    @Test
    public void testForward4() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(15)
                .setSpacesInTab(2).setTabsForLineSplit(1).build();
        List<ForwardInfo> info = new ArrayList<>();
        Forward.forward(runtime, options, createExample0().list(), fi -> {
            info.add(fi);
            System.out.println(fi);
            return false;
        }, 0, 25);
        assertEquals(3, info.size());
    }

    @Test
    public void testForward5() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(15)
                .setSpacesInTab(2).setTabsForLineSplit(1).build();
        List<ForwardInfo> info = new ArrayList<>();
        Forward.forward(runtime, options, createExample0().list(), fi -> {
            info.add(fi);
            System.out.println(fi);
            return false;
        }, 0, 15);
        assertEquals(3, info.size());
    }

    // public method(int p1, int p2); with guides
    @Test
    public void testForward6() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(8)
                .setSpacesInTab(2).setTabsForLineSplit(1).build();
        GuideImpl.GuideGenerator guideGenerator = GuideImpl.defaultGuideGenerator();
        OutputBuilder outputBuilder = new OutputBuilderImpl()
                .add(new TextImpl("public")) // 0
                .add(SpaceEnum.ONE) //1
                .add(new TextImpl("method")) // 2
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(guideGenerator.start())
                .add(new TextImpl("int")); // 5
        List<ForwardInfo> info = new ArrayList<>();
        boolean interrupted = Forward.forward(runtime, options, outputBuilder.list(), fi -> {
            info.add(fi);
            System.out.println(fi);
            return false;
        }, 0, 14);
        assertFalse(interrupted);
        assertEquals(3, info.size()); // excluding the start guide
    }
}
