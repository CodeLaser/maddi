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
import org.e2immu.language.cst.api.output.element.Symbol;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTextBlockFormatter {
    private final Runtime runtime = new RuntimeImpl();

    private static OutputBuilder createExample3() {
        GuideImpl.GuideGenerator gg2 = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator gg3 = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator gg4 = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator gg5 = GuideImpl.generatorForParameterDeclaration();

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
                .add(new TextImpl("class"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("JavaUtil"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg3.start())

                .add(new TextImpl("public"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("static"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("final"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("String"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("INPUT_1"))
                .add(SpaceEnum.ONE)
                .add(SymbolEnum.EQUALS)
                .add(new TextImpl("abc\n123\n   456\n", new TextBlockFormattingImpl.Builder().build()))
                .add(SymbolEnum.SEMICOLON)
                .add(gg3.mid())

                .add(gg4.start())
                .add(new TextImpl("@TestInfo"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(gg5.start())
                .add(new TextImpl("\"this is a simple test\""))
                .add(SymbolEnum.COMMA)
                .add(gg5.mid())
                .add(new TextImpl("v = \"1:23\""))
                .add(gg5.end())
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(gg4.end())
                .add(SpaceEnum.ONE_IS_NICE_EASY_SPLIT)
                .add(new TextImpl("public"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("static"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("final"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("String"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("INPUT_2"))
                .add(SpaceEnum.ONE)
                .add(SymbolEnum.EQUALS)
                .add(new TextImpl("abc\n123\n   456", new TextBlockFormattingImpl.Builder().setTrailingClosingQuotes(true).build()))
                .add(SymbolEnum.SEMICOLON)

                .add(gg3.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(gg3.end())
                .add(gg2.end());
    }

    @Test
    public void test1() {
        OutputBuilder outputBuilder = createExample3();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(80).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String out = formatter.write(outputBuilder);

        @Language("java")
        String expect = """
                package a.b.c;
                import java.util.Set;
                import java.util.List;
                class JavaUtil() {
                    public static final String INPUT_1 = ""\"
                            abc
                            123
                               456
                            ""\";

                    @TestInfo("this is a simple test", v = "1:23")
                        public static final String INPUT_2 = ""\"
                            abc
                            123
                               456""\";
                }
                """;
        assertEquals(expect, out);
    }
}
