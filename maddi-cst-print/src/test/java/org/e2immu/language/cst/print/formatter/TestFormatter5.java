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

public class TestFormatter5 {
    private final Runtime runtime = new RuntimeImpl();

    public static OutputBuilder createExample0() {

        GuideImpl.GuideGenerator ggBlock = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator ggComment = GuideImpl.generatorForMultilineComment();

        return new OutputBuilderImpl()
                .add(ggBlock.start())
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(ggComment.start())
                .add(new TextImpl("line 1"))
                .add(SpaceEnum.ONE_IS_NICE_EASY_SPLIT)
                .add(ggComment.mid())
                .add(new TextImpl("line 2"))
                .add(ggComment.end())
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)
                .add(SpaceEnum.ONE_IS_NICE_EASY_SPLIT)
                .add(ggBlock.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("ImmutableContainer"))
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("IMPLIED"))
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)
                .add(SpaceEnum.ONE_IS_NICE_EASY_SPLIT)
                .add(ggBlock.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("NotNull"))
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("OK"))
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)
                .add(ggBlock.end());
    }


    @Test
    public void testExample0() {
        FormattingOptions options = FormattingOptionsImpl.DEFAULT;
        Formatter formatter = new FormatterImpl(runtime, options);
        OutputBuilder example = createExample0();

        assertEquals("/*line 1 line 2*/ @ImmutableContainer /*IMPLIED*/ @NotNull /*OK*/\n", formatter.write(example));
    }

    public static OutputBuilder createExample1() {

        GuideImpl.GuideGenerator ggBlock = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator ggComment = GuideImpl.generatorForMultilineComment();

        return new OutputBuilderImpl()
                .add(ggBlock.start())
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(ggComment.start())
                .add(new TextImpl("line 1 is much longer than in the previous example, we want to force everything"))
                .add(SpaceEnum.ONE_IS_NICE_EASY_SPLIT)
                .add(ggComment.mid())
                .add(new TextImpl("on multiple lines. So therefore, line 2 is also rather long"))
                .add(ggComment.end())
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)
                .add(SpaceEnum.ONE_IS_NICE_EASY_SPLIT)
                .add(ggBlock.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("ImmutableContainer"))
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("IMPLIED"))
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)
                .add(SpaceEnum.ONE_IS_NICE_EASY_SPLIT)
                .add(ggBlock.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("NotNull"))
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("OK"))
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)
                .add(ggBlock.end());
    }


    @Test
    public void testExample1() {
        FormattingOptions options = FormattingOptionsImpl.DEFAULT;
        Formatter formatter = new FormatterImpl(runtime, options);
        OutputBuilder example = createExample1();

        assertEquals("""
                    
                    /*
                    line 1 is much longer than in the previous example, we want to force everything
                    on multiple lines. So therefore, line 2 is also rather long
                    */
                    @ImmutableContainer /*IMPLIED*/
                    @NotNull /*OK*/
                """, formatter.write(example));
    }


    public static OutputBuilder createExample2() {

        GuideImpl.GuideGenerator ggAnnot = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator ggComment = GuideImpl.generatorForMultilineComment();
        GuideImpl.GuideGenerator ggMethodModifiers = GuideImpl.defaultGuideGenerator();
        GuideImpl.GuideGenerator ggParams = GuideImpl.generatorForParameterDeclaration();
        GuideImpl.GuideGenerator ggMethodBlock = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator ggSwitchBlock = GuideImpl.generatorForBlock();

        OutputBuilder defaultBlock = new OutputBuilderImpl()
                .add(new QualifiedNameImpl("c"))
                .add(SymbolEnum.binaryOperator("=="))
                .add(new TextImpl("'a'"))
                .add(SymbolEnum.LOGICAL_OR)
                .add(new QualifiedNameImpl("c"))
                .add(SymbolEnum.binaryOperator("=="))
                .add(new TextImpl("'b'"))
                .add(SymbolEnum.QUESTION_MARK)
                .add(new QualifiedNameImpl("b"))
                .add(SymbolEnum.COLON)
                .add(new TextImpl("\"c\""))
                .add(SymbolEnum.SEMICOLON);

        return new OutputBuilderImpl()
                .add(ggAnnot.start())
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(ggComment.start())
                .add(new TextImpl("should raise a warning that the condition is always false, plus that b is never used"))
                .add(SpaceEnum.ONE_IS_NICE_EASY_SPLIT)
                .add(ggComment.mid())
                .add(new TextImpl("as a consequence, default always returns \"c\" so we have @NotNull"))
                .add(ggComment.end())
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)
                .add(SpaceEnum.ONE_IS_NICE_EASY_SPLIT)
                .add(ggAnnot.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("ImmutableContainer"))
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("IMPLIED")) // 14
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)
                .add(SpaceEnum.ONE_IS_NICE_EASY_SPLIT)
                .add(ggAnnot.mid())
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("NotNull")) // 19
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("OK"))
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)
                .add(SpaceEnum.ONE_IS_NICE_EASY_SPLIT)
                .add(ggAnnot.mid())
                .add(ggMethodModifiers.start()) // 25
                .add(new TextImpl("public"))
                .add(SpaceEnum.ONE)
                .add(ggMethodModifiers.mid())
                .add(new TextImpl("static"))
                .add(ggMethodModifiers.end()) // 30
                .add(SpaceEnum.ONE)
                .add(new TypeNameImpl("String"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("method")) // 34
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(ggParams.start())
                .add(new TypeNameImpl("char")) // 37
                .add(SpaceEnum.ONE)
                .add(new TextImpl("c"))
                .add(SymbolEnum.COMMA)
                .add(ggParams.mid())
                .add(new TypeNameImpl("String"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("b"))
                .add(ggParams.end())
                .add(SymbolEnum.RIGHT_PARENTHESIS) // 46
                .add(SymbolEnum.LEFT_BRACE)
                .add(ggMethodBlock.start())
                .add(new TextImpl("return"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("switch"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new QualifiedNameImpl("c"))
                .add(SymbolEnum.RIGHT_PARENTHESIS) // 54
                .add(SymbolEnum.LEFT_BRACE)
                .add(ggSwitchBlock.start())
                .add(new TextImpl("a")) // 57
                .add(SymbolEnum.LAMBDA)
                .add(new TextImpl("\"a\""))
                .add(SymbolEnum.SEMICOLON)
                .add(ggSwitchBlock.mid()) // 61
                .add(new TextImpl("b"))
                .add(SymbolEnum.LAMBDA)
                .add(new TextImpl("\"b\""))
                .add(SymbolEnum.SEMICOLON)
                .add(ggSwitchBlock.mid())
                .add(new TextImpl("default")) // 67
                .add(SymbolEnum.LAMBDA)
                .add(defaultBlock)
                .add(ggSwitchBlock.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(SymbolEnum.SEMICOLON)
                .add(SymbolEnum.LEFT_BLOCK_COMMENT)
                .add(new TextImpl("inline conditional evaluates to constant"))
                .add(SymbolEnum.RIGHT_BLOCK_COMMENT)
                .add(ggMethodBlock.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(ggAnnot.end());
    }


    @Test
    public void testExample2() {
        Formatter formatter = new FormatterImpl(runtime, FormattingOptionsImpl.DEFAULT);
        OutputBuilder example = createExample2();
        assertEquals(90, example.list().size());

        assertEquals("""
                /*
                should raise a warning that the condition is always false, plus that b is never used
                as a consequence, default always returns "c" so we have @NotNull
                */
                @ImmutableContainer /*IMPLIED*/
                @NotNull /*OK*/
                public static String method(char c, String b) {
                    return switch(c) {
                        a -> "a";
                        b -> "b";
                        default -> c == 'a' || c == 'b' ? b : "c";
                    }; /*inline conditional evaluates to constant*/
                }
                """, formatter.write(example));
    }
    public static OutputBuilder createExample2b() {
        OutputBuilder example = createExample2();
//        assertEquals(90, example.list().size());

        GuideImpl.GuideGenerator ggAnnot = GuideImpl.generatorForAnnotationList();
        GuideImpl.GuideGenerator ggBlock = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator ggCompanion = GuideImpl.generatorForCompanionList();

        // we'll now surround that example with the rest of the method
        OutputBuilder all = new OutputBuilderImpl()
                .add(new TextImpl("package"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("org.e2immu.analyser.parser.conditional.testexample"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(new TextImpl("import"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("org.e2immu.annotation.ImmutableContainer")) // 7
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(new TextImpl("import"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("org.e2immu.annotation.NotNull")) // 12
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(ggAnnot.start()) // 15
                .add(SymbolEnum.AT)
                .add(new TypeNameImpl("ImmutableContainer"))
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT) // 18
                .add(ggAnnot.mid())
                .add(new TextImpl("public"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("class"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("SwitchExpression_1"))
                .add(SymbolEnum.LEFT_BRACE) // 25
                .add(ggBlock.start())
                .add(ggCompanion.start()) // 27
                .add(example)
                .add(ggCompanion.end()) // 118
                .add(ggBlock.end())
                .add(SymbolEnum.RIGHT_BRACE)
                .add(ggAnnot.end()); // 121
        //     assertEquals(122, all.list().size());
        return all;
    }
    @Test
    public void testExample2bis() {
        Formatter formatter = new FormatterImpl(runtime, FormattingOptionsImpl.DEFAULT);
        String output = formatter.write(createExample2b());
        assertEquals("""
                package org.e2immu.analyser.parser.conditional.testexample;
                import org.e2immu.annotation.ImmutableContainer;
                import org.e2immu.annotation.NotNull;
                @ImmutableContainer
                public class SwitchExpression_1 {
                    /*
                    should raise a warning that the condition is always false, plus that b is never used
                    as a consequence, default always returns "c" so we have @NotNull
                    */
                    @ImmutableContainer /*IMPLIED*/
                    @NotNull /*OK*/
                    public static String method(char c, String b) {
                        return switch(c) {
                            a -> "a";
                            b -> "b";
                            default -> c == 'a' || c == 'b' ? b : "c";
                        }; /*inline conditional evaluates to constant*/
                    }
                }
                """, output);
    }
}
