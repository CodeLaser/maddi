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
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.cst.print.FormatterImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFormatter3 {
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
                .add(new TextImpl("org.e2immu.annotation.NotModified"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(new TextImpl("import"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("org.e2immu.annotation.NotNull"))
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
    public void testExample0() {
        FormattingOptions options = FormattingOptionsImpl.DEFAULT;
        Formatter formatter = new FormatterImpl(runtime, options);
        OutputBuilder example = createExample0();

        assertEquals("""
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
                  """, formatter.write(example));
    }
}
