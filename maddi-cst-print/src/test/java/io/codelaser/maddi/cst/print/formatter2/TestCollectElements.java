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

import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCollectElements {
    private final Runtime runtime = new RuntimeImpl();

    // public int method(int p1, int p2) { return p1+p2; }
    //        10|     18|            33|
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
    public void test1() {
        OutputBuilder outputBuilder = createExample1();
        Formatter2Impl formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        String out = formatter.minimal(outputBuilder);
        String expect = """
                
                public int method(
                
                  int p1,
                  int p2){
                
                  return p1+p2;}\
                """;
        assertEquals(expect, out);
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
    public void test2() {
        OutputBuilder outputBuilder = createExample2();
        Formatter2Impl formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        String out = formatter.minimal(outputBuilder);
        String expect = """
                
                public int method(
                
                  int p1,
                  int p2,
                  double somewhatLonger,
                  double d){
                
                  log(

                    p1,
                    p2);
                  return p1+p2;}\
                """;
        assertEquals(expect, out);
    }

    static OutputBuilder createExample3() {
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

    @Test
    public void test3() {
        OutputBuilder outputBuilder = createExample3();
        Formatter2Impl formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        String out = formatter.minimal(outputBuilder);
        String expect = """
                
                try{
                
                  if(a){

                    assert b;}else{

                    assert c;
                    exit(1);}}\
                """;
        assertEquals(expect, out);
    }
}
