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
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestBlockPrinter2 {
    private final Runtime runtime = new RuntimeImpl();

    static OutputBuilder createExample1() {
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
    public void test1a() {
        OutputBuilder outputBuilder = createExample1();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(70).setSpacesInTab(4).build();
        assertFalse(options.compact());
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        String expect = "public int method(int p1, int p2) { return p1 + p2; }\n";
        assertEquals(expect, string);
    }

    @Test
    public void test1b() {
        OutputBuilder outputBuilder = createExample1();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(30).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        String expect = """
                public int method(
                    int p1,
                    int p2) { return p1 + p2; }
                """;
        assertEquals(expect, string);
    }

    @Test
    public void test1c() {
        OutputBuilder outputBuilder = createExample1();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(25).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        String expect = """
                public int method(
                    int p1,
                    int p2) {
                    return p1 + p2;
                }
                """;
        assertEquals(expect, string);
    }

    static OutputBuilder createExample2() {
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
    public void test2a() {
        OutputBuilder outputBuilder = createExample2();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(20).setSpacesInTab(2).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        String expect = """
                public int method(
                  int p1,
                  int p2,
                  double somewhatLonger,
                  double d) {
                  log(p1, p2);
                  return p1 + p2;
                }
                """;
        assertEquals(expect, string);
    }


    private static OutputBuilder createExample3() {
        GuideImpl.GuideGenerator gg2 = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator gg3 = GuideImpl.generatorForAnnotationList();

        return new OutputBuilderImpl()
                .add(gg2.start())
                .add(new TextImpl("package"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("a.b.c"))
                .add(SymbolEnum.SEMICOLON)

                .add(gg2.mid())
                .add(new TextImpl("import"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("java.util.Set"))
                .add(SymbolEnum.SEMICOLON)

                .add(gg2.mid())
                .add(new TextImpl("import"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("java.util.List"))
                .add(SymbolEnum.SEMICOLON)

                .add(gg2.mid())
                .add(gg3.start())
                .add(SymbolEnum.SINGLE_LINE_COMMENT)
                .add(new TextImpl("this is a comment"))
                .add(SpaceEnum.NEWLINE)

                .add(gg3.mid())
                .add(SymbolEnum.SINGLE_LINE_COMMENT)
                .add(new TextImpl("this is a second comment"))
                .add(SpaceEnum.NEWLINE)

                .add(gg3.mid())
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("block comment"))
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)

                .add(gg3.mid())
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("block comment"))
                .add(SpaceEnum.NEWLINE)
                .add(new TextImpl("   on a second line"))
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)

                .add(gg3.mid())
                .add(new TextImpl("record"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Record"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(SymbolEnum.RIGHT_BRACE)
                .add(gg3.end())
                .add(gg2.end());
    }

    @Test
    public void test3() {
        OutputBuilder outputBuilder = createExample3();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(80).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String out = formatter.write(outputBuilder);
        String expect = """
                package a.b.c;
                import java.util.Set;
                import java.util.List;
                //this is a comment
                //this is a second comment
                /*block comment*/
                /*block comment
                   on a second line*/
                record Record() { }
                """;
        assertEquals(expect, out);
    }


    private static OutputBuilder createExample3b() {
        return new OutputBuilderImpl()
                .add(new TextImpl("package"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("a.b.c"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)

                .add(new TextImpl("import"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("java.util.Set"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)

                .add(new TextImpl("import"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("java.util.List"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)

                .add(SymbolEnum.SINGLE_LINE_COMMENT)
                .add(new TextImpl("this is a comment"))
                .add(SpaceEnum.NEWLINE)

                .add(SymbolEnum.SINGLE_LINE_COMMENT)
                .add(new TextImpl("this is a second comment"))
                .add(SpaceEnum.NEWLINE)

                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("block comment"))
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)
                .add(SpaceEnum.NEWLINE)

                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("block comment"))
                .add(SpaceEnum.NEWLINE)
                .add(new TextImpl("   on a second line"))
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)
                .add(SpaceEnum.NEWLINE)

                .add(new TextImpl("record"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Record"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(SymbolEnum.RIGHT_BRACE);
    }

    @Test
    public void test3b() {
        OutputBuilder outputBuilder = createExample3b();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(80).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String out = formatter.write(outputBuilder);
        String expect = """
                package a.b.c;
                import java.util.Set;
                import java.util.List;
                //this is a comment
                //this is a second comment
                /*block comment*/
                /*block comment
                   on a second line*/
                record Record() { }
                """;
        assertEquals(expect, out);
    }



    private static OutputBuilder createExample3c() {
        GuideImpl.GuideGenerator ggFile = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator ggImport = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator ggClass = GuideImpl.generatorForAnnotationList();

        return new OutputBuilderImpl()
                .add(ggFile.start())
                .add(new TextImpl("package"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("a.b.c"))
                .add(SymbolEnum.SEMICOLON)

                .add(ggFile.mid())

                .add(ggImport.start())
                .add(new TextImpl("import"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("java.util.Set"))
                .add(SymbolEnum.SEMICOLON)

                .add(ggImport.mid())
                .add(new TextImpl("import"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("java.util.List"))
                .add(SymbolEnum.SEMICOLON)
                .add(ggImport.end())

                .add(ggFile.mid())

                .add(ggClass.start())
                .add(SymbolEnum.SINGLE_LINE_COMMENT)
                .add(new TextImpl("this is a comment"))
                .add(SpaceEnum.NEWLINE)

                .add(ggClass.mid())
                .add(SymbolEnum.SINGLE_LINE_COMMENT)
                .add(new TextImpl("this is a second comment"))
                .add(SpaceEnum.NEWLINE)

                .add(ggClass.mid())
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("block comment"))
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)

                .add(ggClass.mid())
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("block comment"))
                .add(SpaceEnum.NEWLINE)
                .add(new TextImpl("   on a second line"))
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)

                .add(ggClass.mid())
                .add(new TextImpl("record"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Record"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(SymbolEnum.RIGHT_BRACE)
                .add(ggClass.end())
                .add(ggFile.end());
    }

    @Test
    public void test3c() {
        OutputBuilder outputBuilder = createExample3c();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(15).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String out = formatter.write(outputBuilder);
        String expect = """
                package a.b.c;
                import java.util.Set;
                import java.util.List;
                
                //this is a comment
                //this is a second comment
                /*block comment*/
                /*block comment
                   on a second line*/
                record Record() { }
                """;
        assertEquals(expect, out);
    }


}
