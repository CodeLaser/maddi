package org.e2immu.language.cst.print.formatter;

import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.cst.print.FormatterImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.e2immu.language.cst.impl.output.QualifiedNameImpl.Required.NEVER;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFormatter6 {
    private final Runtime runtime = new RuntimeImpl();

    public static OutputBuilder create1() {
        OutputBuilder ob = new OutputBuilderImpl();
        GuideImpl.GuideGenerator gg616 = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator gg618 = GuideImpl.generatorForParameterDeclaration();
        GuideImpl.GuideGenerator gg619 = GuideImpl.defaultGuideGenerator();
        GuideImpl.GuideGenerator gg620 = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator gg623 = GuideImpl.generatorForBlock();

        ob.add(KeywordImpl.PACKAGE)
                .add(SpaceEnum.ONE)
                .add(new TextImpl("a.b"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(KeywordImpl.IMPORT)
                .add(SpaceEnum.ONE)
                .add(new TextImpl("java.io.IOException"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(KeywordImpl.IMPORT)
                .add(SpaceEnum.ONE)
                .add(new TextImpl("java.net.HttpURLConnection"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(KeywordImpl.IMPORT)
                .add(SpaceEnum.ONE)
                .add(new TextImpl("java.net.MalformedURLException"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(KeywordImpl.IMPORT)
                .add(SpaceEnum.ONE)
                .add(new TextImpl("java.net.URL"))
                .add(SymbolEnum.SEMICOLON)
                .add(SpaceEnum.NEWLINE)
                .add(KeywordImpl.CLASS)
                .add(SpaceEnum.ONE)
                .add(new TextImpl("X"))
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg616.start()) // priority=true, startNL=true, endNL=true
                .add(KeywordImpl.STATIC)
                .add(SpaceEnum.ONE)
                .add(new TypeNameImpl("HttpURLConnection"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("openConnection"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(gg618.start()) // priority=false, startNL=true, endNL=false
                .add(new TypeNameImpl("String"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("baseURL"))
                .add(SymbolEnum.COMMA)
                .add(gg618.mid()) // priority=false, startNL=true, endNL=false
                .add(new TypeNameImpl("String"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("queryString"))
                .add(gg618.end()) // priority=false, startNL=true, endNL=false
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(KeywordImpl.THROWS)
                .add(SpaceEnum.ONE)
                .add(gg619.start()) // priority=false, startNL=false, endNL=false
                .add(new TypeNameImpl("MalformedURLException"))
                .add(SymbolEnum.COMMA)
                .add(gg619.mid()) // priority=false, startNL=false, endNL=false
                .add(new TypeNameImpl("IOException"))
                .add(gg619.end()) // priority=false, startNL=false, endNL=false
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg620.start()) // priority=true, startNL=true, endNL=true
                .add(KeywordImpl.FINAL)
                .add(SpaceEnum.ONE)
                .add(new TypeNameImpl("StringBuilder"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("buff"))
                .add(SymbolEnum.binaryOperator("="))
                .add(KeywordImpl.NEW)
                .add(SpaceEnum.ONE)
                .add(new TypeNameImpl("StringBuilder"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.SEMICOLON)
                .add(gg620.mid()) // priority=true, startNL=true, endNL=true
                .add(new QualifiedNameImpl("buff", null, NEVER))
                .add(SymbolEnum.DOT)
                .add(new TextImpl("append"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new TextImpl("baseURL"))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.SEMICOLON)
                .add(gg620.mid()) // priority=true, startNL=true, endNL=true
                .add(KeywordImpl.IF)
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new TextImpl("queryString"))
                .add(SymbolEnum.binaryOperator("!="))
                .add(KeywordImpl.NULL)
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg623.start()) // priority=true, startNL=true, endNL=true
                .add(new QualifiedNameImpl("buff", null, NEVER))
                .add(SymbolEnum.DOT)
                .add(new TextImpl("append"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new TextImpl("\"?\""))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.SEMICOLON)
                .add(gg623.mid()) // priority=true, startNL=true, endNL=true
                .add(new QualifiedNameImpl("buff", null, NEVER))
                .add(SymbolEnum.DOT)
                .add(new TextImpl("append"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new TextImpl("queryString"))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.SEMICOLON)
                .add(gg623.end()) // priority=true, startNL=true, endNL=true
                .add(SymbolEnum.RIGHT_BRACE)
                .add(gg620.mid()) // priority=true, startNL=true, endNL=true
                .add(KeywordImpl.FINAL)
                .add(SpaceEnum.ONE)
                .add(new TypeNameImpl("URL"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("url"))
                .add(SymbolEnum.binaryOperator("="))
                .add(KeywordImpl.NEW)
                .add(SpaceEnum.ONE)
                .add(new TypeNameImpl("URL"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new QualifiedNameImpl("buff", null, NEVER))
                .add(SymbolEnum.DOT)
                .add(new TextImpl("toString"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.SEMICOLON)
                .add(gg620.mid()) // priority=true, startNL=true, endNL=true
                .add(KeywordImpl.RETURN)
                .add(SpaceEnum.ONE)
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(new TypeNameImpl("HttpURLConnection"))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(new QualifiedNameImpl("url", null, NEVER))
                .add(SymbolEnum.DOT)
                .add(new TextImpl("openConnection"))
                .add(SymbolEnum.OPEN_CLOSE_PARENTHESIS)
                .add(SymbolEnum.SEMICOLON)
                .add(gg620.end()) // priority=true, startNL=true, endNL=true
                .add(SymbolEnum.RIGHT_BRACE)
                .add(gg616.end()) // priority=true, startNL=true, endNL=true
                .add(SymbolEnum.RIGHT_BRACE);
        return ob;
    }

    @Test
    public void test() {
        Formatter formatter = new FormatterImpl(runtime, new FormattingOptionsImpl.Builder().build());
        String out = formatter.write(create1());
        @Language("java")
        String expected = """
                package a.b;
                import java.io.IOException;
                import java.net.HttpURLConnection;
                import java.net.MalformedURLException;
                import java.net.URL;
                class X {
                    static HttpURLConnection openConnection(String baseURL, String queryString) throws MalformedURLException,
                        IOException{
                        final StringBuilder buff = new StringBuilder();
                        buff.append(baseURL);
                        if(queryString != null) { buff.append("?"); buff.append(queryString); }
                        final URL url = new URL(buff.toString());
                        return (HttpURLConnection)url.openConnection();
                    }
                }
                """;
        assertEquals(expected, out);
    }

    public static OutputBuilder create2() {
        OutputBuilder ob = new OutputBuilderImpl();
        GuideImpl.GuideGenerator gg616 = GuideImpl.generatorForBlock();
        GuideImpl.GuideGenerator gg618 = GuideImpl.generatorForParameterDeclaration();
        GuideImpl.GuideGenerator gg619 = GuideImpl.defaultGuideGenerator();

        ob.add(KeywordImpl.CLASS)
                .add(SpaceEnum.ONE)
                .add(new TextImpl("X"))
                .add(SymbolEnum.LEFT_BRACE)
                .add(gg616.start()) // priority=true, startNL=true, endNL=true
                .add(KeywordImpl.STATIC)
                .add(SpaceEnum.ONE)
                .add(new TypeNameImpl("HttpURLConnection"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("openConnection"))
                .add(SymbolEnum.LEFT_PARENTHESIS)
                .add(gg618.start()) // priority=false, startNL=true, endNL=false
                .add(new TypeNameImpl("String"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("baseURL"))
                .add(SymbolEnum.COMMA)
                // .add(gg618.mid()) // priority=false, startNL=true, endNL=false
                .add(new TypeNameImpl("String"))
                .add(SpaceEnum.ONE)
                .add(new TextImpl("x1234567890")) // FIXME one more character here, and the space disappears (from 9 -> 0)
                .add(gg618.end()) // priority=false, startNL=true, endNL=false
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT)
                .add(KeywordImpl.THROWS)
                .add(SpaceEnum.ONE)
                .add(gg619.start()) // priority=false, startNL=false, endNL=false
                .add(new TypeNameImpl("MalformedURLException"))
                .add(SymbolEnum.COMMA)
                .add(gg619.mid()) // priority=false, startNL=false, endNL=false
                .add(new TypeNameImpl("IOException"))
                .add(gg619.end()) // priority=false, startNL=false, endNL=false
                .add(SymbolEnum.LEFT_BRACE)
                .add(SymbolEnum.RIGHT_BRACE)
                .add(gg616.end()) // priority=true, startNL=true, endNL=true
                .add(SymbolEnum.RIGHT_BRACE);
        return ob;
    }
    // space does occur when there are no method parameters
    @Test
    public void test1a() {
        Formatter formatter = new FormatterImpl(runtime, new FormattingOptionsImpl.Builder().build());
        String out = formatter.write(create2());
        @Language("java")
        String expected = """
                class X {
                    static HttpURLConnection openConnection(String baseURL, String x1234567890) throws MalformedURLException,
                        IOException{ }
                }
                """;
        assertEquals(expected, out);
    }
}
