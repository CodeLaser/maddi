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

import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.output.*;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
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
