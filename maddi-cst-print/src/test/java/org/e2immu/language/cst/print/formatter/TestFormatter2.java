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

public class TestFormatter2 {
    private final Runtime runtime = new RuntimeImpl();

    private OutputBuilder createExample0() {
        GuideImpl.GuideGenerator type = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator field = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator paramsConstructor = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator constructor = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator method = GuideImpl.generatorForAnnotationList();

        GuideImpl.GuideGenerator blockType = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator blockConstructor = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator blockMethod = GuideImpl.generatorForBlock();

        GuideImpl.GuideGenerator modifiersField = GuideImpl.defaultGuideGenerator();

        return new OutputBuilderImpl()
                .add(type.start())
                .add(new TextImpl("public"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("class"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Basics_1"))
                .add(SymbolEnum.LEFT_BRACE)
                .add(blockType.start())
                .add(field.start())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Linked"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new TextImpl("to"))
                .add(SymbolEnum.binaryOperator("="))
                .add(SymbolEnum.LEFT_BRACE)
                .add(new TextImpl("\"p0\""))
                .add(SymbolEnum.RIGHT_BRACE)
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(field.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("NotModified"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(field.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Nullable"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(field.mid())
                .add(modifiersField.start())
                .add(new TextImpl("public"))
                .add(SpaceEnum.ONE)
                .add(modifiersField.mid())
                .add(new TextImpl("final"))
                .add(modifiersField.end())
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Set<String>"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("f1"))
                .add(SymbolEnum.SEMICOLON)
                .add(field.end())
                .add(blockType.mid())
                .add(constructor.start())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Dependent"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(constructor.mid())
                .add(new TextImpl("public"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Basics_1"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(paramsConstructor.start())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("NotModified"))
                .add(SpaceEnum.ONE)
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Nullable"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Set<String>"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("p0"))
                .add(SymbolEnum.COMMA)
                .add(paramsConstructor.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("NotModified"))
                .add(SpaceEnum.ONE)
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Nullable"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Set<String>"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("p1"))
                .add(SymbolEnum.COMMA)
                .add(paramsConstructor.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Nullable"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("String"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("p2"))
                .add(paramsConstructor.end())
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(blockConstructor.start())
                .add(SpaceEnum.NONE)
                .add(SpaceEnum.NONE)
                .add(new TextImpl("Set<String>"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("s1"))
                .add(SymbolEnum.binaryOperator("="))
                .add(new QualifiedNameImpl("p0"))
                .add(SymbolEnum.SEMICOLON)
                .add(blockConstructor.mid())
                .add(new QualifiedNameImpl("this"))
                .add(SymbolEnum.DOT)
                .add(new QualifiedNameImpl("f1"))
                .add(SymbolEnum.binaryOperator("="))
                .add(new QualifiedNameImpl("s1"))
                .add(SymbolEnum.SEMICOLON)
                .add(blockConstructor.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(constructor.end())
                .add(blockType.mid())
                .add(method.start())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Independent"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(method.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("NotModified"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(method.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Nullable"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(method.mid())
                .add(new TextImpl("public"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Set<String>"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("getF1"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(blockMethod.start())
                .add(new TextImpl("return"))
                .add(SpaceEnum.ONE)
                .add(new QualifiedNameImpl("f1"))
                .add(SymbolEnum.SEMICOLON)
                .add(blockMethod.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(method.end())
                .add(blockType.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(type.end());
    }

    @Test
    public void testExample0() {
        FormattingOptions options = FormattingOptionsImpl.DEFAULT;
        Formatter formatter = new FormatterImpl(runtime, options);
        OutputBuilder example = createExample0();

        assertEquals("""
                public class Basics_1 {
                    @Linked(to = { "p0" }) @NotModified @Nullable public final Set<String> f1;
                    
                    @Dependent
                    public Basics_1(@NotModified @Nullable Set<String> p0, @NotModified @Nullable Set<String> p1, @Nullable String p2) {
                        Set<String> s1 = p0;
                        this.f1 = s1;
                    }
                    
                    @Independent @NotModified @Nullable public Set<String> getF1() { return f1; }
                }
                """, formatter.write(example));
    }

    private OutputBuilder createExample1() {
        GuideImpl.GuideGenerator type = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator field = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator paramsConstructor = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator constructor = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator method = GuideImpl.generatorForAnnotationList();

        GuideImpl.GuideGenerator blockType = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator blockConstructor = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator blockMethod = GuideImpl.generatorForBlock();

        GuideImpl.GuideGenerator modifiersField = GuideImpl.defaultGuideGenerator();

        return new OutputBuilderImpl()
                .add(new TextImpl("package"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("org.e2immu.analyser.parser.failing.testexample"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(new TextImpl("import"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("java.util.Set"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(type.start())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Dependent"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(type.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("E1Container"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(type.mid())
                .add(new TextImpl("public"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("class"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Basics_1"))
                .add(SymbolEnum.LEFT_BRACE)
                .add(blockType.start())
                .add(field.start())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Linked"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new TextImpl("to"))
                .add(SymbolEnum.binaryOperator("="))
                .add(SymbolEnum.LEFT_BRACE)
                .add(new TextImpl("\"p0\""))
                .add(SymbolEnum.RIGHT_BRACE)
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(field.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("NotModified"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(field.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Nullable"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(field.mid())
                .add(modifiersField.start())
                .add(new TextImpl("public"))
                .add(SpaceEnum.ONE)
                .add(modifiersField.mid())
                .add(new TextImpl("final"))
                .add(modifiersField.end())
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Set<String>"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("f1"))
                .add(SymbolEnum.SEMICOLON)
                .add(field.end())
                .add(blockType.mid())
                .add(constructor.start())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Dependent"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(constructor.mid())
                .add(new TextImpl("public"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Basics_1"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(paramsConstructor.start())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("NotModified"))
                .add(SpaceEnum.ONE)
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Nullable"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Set<String>"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("p0"))
                .add(SymbolEnum.COMMA)
                .add(paramsConstructor.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("NotModified"))
                .add(SpaceEnum.ONE)
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Nullable"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Set<String>"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("p1"))
                .add(SymbolEnum.COMMA)
                .add(paramsConstructor.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Nullable"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("String"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("p2"))
                .add(paramsConstructor.end())
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(blockConstructor.start())
                .add(SpaceEnum.NONE)
                .add(SpaceEnum.NONE)
                .add(new TextImpl("Set<String>"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("s1"))
                .add(SymbolEnum.binaryOperator("="))
                .add(new QualifiedNameImpl("p0"))
                .add(SymbolEnum.SEMICOLON)
                .add(blockConstructor.mid())
                .add(new QualifiedNameImpl("this"))
                .add(SymbolEnum.DOT)
                .add(new QualifiedNameImpl("f1"))
                .add(SymbolEnum.binaryOperator("="))
                .add(new QualifiedNameImpl("s1"))
                .add(SymbolEnum.SEMICOLON)
                .add(blockConstructor.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(constructor.end())
                .add(blockType.mid())
                .add(method.start())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Independent"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(method.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("NotModified"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(method.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Nullable"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(method.mid())
                .add(new TextImpl("public"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("Set<String>"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("getF1"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(blockMethod.start())
                .add(new TextImpl("return"))
                .add(SpaceEnum.ONE)
                .add(new QualifiedNameImpl("f1"))
                .add(SymbolEnum.SEMICOLON)
                .add(blockMethod.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(method.end())
                .add(blockType.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(type.end());
    }

    @Test
    public void testExample1() {
        FormattingOptions options = FormattingOptionsImpl.DEFAULT;
        assertEquals("""
                        package org.e2immu.analyser.parser.failing.testexample;
                        import java.util.Set;
                        @Dependent
                        @E1Container
                        public class Basics_1 {
                            @Linked(to = { "p0" }) @NotModified @Nullable public final Set<String> f1;
                            
                            @Dependent
                            public Basics_1(@NotModified @Nullable Set<String> p0, @NotModified @Nullable Set<String> p1, @Nullable String p2) {
                                Set<String> s1 = p0;
                                this.f1 = s1;
                            }
                            
                            @Independent @NotModified @Nullable public Set<String> getF1() { return f1; }
                        }
                        """,
                new FormatterImpl(runtime, options).write(createExample1()));
    }

    private OutputBuilder createExample2() {
        GuideImpl.GuideGenerator gg59 = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator gg62 = GuideImpl.generatorForBlock();

        return new OutputBuilderImpl()
                .add(new TextImpl("static"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("int"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("test"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg59.start())
                .add(SpaceEnum.NONE)
                .add(SpaceEnum.NONE)
                .add(new TextImpl("List<String>"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("list"))
                .add(SymbolEnum.binaryOperator("="))
                .add(new TextImpl("new"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("ArrayList"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.SEMICOLON)
                .add(gg59.mid())
                .add(new TextImpl("if"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new QualifiedNameImpl("list"))
                .add(SymbolEnum.DOT)
                .add(new TextImpl("size"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.binaryOperator(">"))
                .add(new TextImpl("0"))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("Condition in 'if' or 'switch' statement evaluates to constant"))
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("Unreachable statement"))
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg62.start())
                .add(new TextImpl("return"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("3"))
                .add(SymbolEnum.SEMICOLON)
                .add(gg62.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(gg59.mid())
                .add(new TextImpl("return"))
                .add(SpaceEnum.ONE)
                .add(new QualifiedNameImpl("list"))
                .add(SymbolEnum.DOT)
                .add(new TextImpl("size"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.binaryOperator("+"))
                .add(new TextImpl("4"))
                .add(SymbolEnum.SEMICOLON)
                .add(gg59.end())
                .add(SymbolEnum.RIGHT_BRACE);
    }

    @Test
    public void testExample2() {
        FormattingOptions options = FormattingOptionsImpl.DEFAULT;
        assertEquals("""
                        static int test() {
                            List<String> list = new ArrayList();
                            
                            if(list.size() > 0) /*Condition in 'if' or 'switch' statement evaluates to constant*/ /*Unreachable statement*/ {
                                return 3;
                            }
                            
                            return list.size() + 4;
                        }
                        """,
                new FormatterImpl(runtime, options).write(createExample2()));
    }

    @Test
    public void testExample2Skip() {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setSkipComments(true).build();
        assertEquals("""
                        static int test() {List<String> list = new ArrayList(); if(list.size() > 0) { return 3; } return list.size() + 4; }
                        """,
                new FormatterImpl(runtime, options).write(createExample2()));
    }

    // slightly larger version of 2
    private OutputBuilder createExample3() {
        GuideImpl.GuideGenerator gg57 = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator gg59 = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator gg62 = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator gg64 = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator gg65 = GuideImpl.generatorForAnnotationList();

        return new OutputBuilderImpl().add(new TextImpl("package"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("org.e2immu.analyser.parser.failing.testexample"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(new TextImpl("import"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("java.util.ArrayList"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(new TextImpl("import"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("java.util.List"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(new TextImpl("import"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("org.e2immu.annotation.Constant"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(gg65.start())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("E2Container"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(gg65.mid())
                .add(new TextImpl("public"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("class"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("BasicCompanionMethods_0"))
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg57.start())
                .add(gg64.start())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("Constant"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new TextImpl("\"4\""))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(gg64.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("NotModified"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(gg64.mid())
                .add(new TextImpl("static"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("int"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("test"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg59.start())
                .add(SpaceEnum.NONE)
                .add(SpaceEnum.NONE)
                .add(new TextImpl("List<String>"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("list"))
                .add(SymbolEnum.binaryOperator("="))
                .add(new TextImpl("new"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("ArrayList"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.SEMICOLON)
                .add(gg59.mid())
                .add(new TextImpl("if"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new QualifiedNameImpl("list"))
                .add(SymbolEnum.DOT)
                .add(new TextImpl("size"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.binaryOperator(">"))
                .add(new TextImpl("0"))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("Condition in 'if' or 'switch' statement evaluates to constant"))
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("Unreachable statement"))
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg62.start())
                .add(new TextImpl("return"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("3"))
                .add(SymbolEnum.SEMICOLON)
                .add(gg62.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(gg59.mid())
                .add(new TextImpl("return"))
                .add(SpaceEnum.ONE)
                .add(new QualifiedNameImpl("list"))
                .add(SymbolEnum.DOT)
                .add(new TextImpl("size"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.binaryOperator("+"))
                .add(new TextImpl("4"))
                .add(SymbolEnum.SEMICOLON)
                .add(gg59.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(gg64.end())
                .add(gg57.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(gg65.end());
    }


    @Test
    public void testExample3() {
        assertEquals("""
                        package org.e2immu.analyser.parser.failing.testexample;
                        import java.util.ArrayList;
                        import java.util.List;
                        import org.e2immu.annotation.Constant;
                        @E2Container
                        public class BasicCompanionMethods_0 {
                            @Constant("4")
                            @NotModified
                            static int test() {
                                List<String> list = new ArrayList();
                                
                                if(list.size() > 0) /*Condition in 'if' or 'switch' statement evaluates to constant*/ /*Unreachable statement*/ {
                                    return 3;
                                }
                                
                                return list.size() + 4;
                            }
                        }
                        """,
                new FormatterImpl(runtime, FormattingOptionsImpl.DEFAULT).write(createExample3()));
    }
}
