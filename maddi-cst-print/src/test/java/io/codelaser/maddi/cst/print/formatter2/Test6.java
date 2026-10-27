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

import static io.codelaser.maddi.cst.impl.output.QualifiedNameImpl.Required.NEVER;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Test6 {
    private final Runtime runtime = new RuntimeImpl();

    static OutputBuilder create1() {
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
    public void test1() {
        OutputBuilder outputBuilder = create1();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        @Language("java")
        String expect = """
                package a.b;
                import java.io.IOException;
                import java.net.HttpURLConnection;
                import java.net.MalformedURLException;
                import java.net.URL;
                class X {
                    static HttpURLConnection openConnection(String baseURL, String queryString) throws
                        MalformedURLException,
                        IOException {
                        final StringBuilder buff = new StringBuilder();
                        buff.append(baseURL);
                        if(queryString != null) { buff.append("?"); buff.append(queryString); }
                        final URL url = new URL(buff.toString());
                        return (HttpURLConnection)url.openConnection();
                    }
                }
                """;
        assertEquals(expect, string);
    }

    @Test
    public void test1b() {
        OutputBuilder outputBuilder = create1();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(60).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        @Language("java")
        String expect = """
                package a.b;
                import java.io.IOException;
                import java.net.HttpURLConnection;
                import java.net.MalformedURLException;
                import java.net.URL;
                class X {
                    static HttpURLConnection openConnection(
                        String baseURL,
                        String queryString) throws
                        MalformedURLException,
                        IOException {
                        final StringBuilder buff = new StringBuilder();
                        buff.append(baseURL);
                        if(queryString != null) {
                            buff.append("?");
                            buff.append(queryString);
                        }
                        final URL url = new URL(buff.toString());
                        return (HttpURLConnection)url.openConnection();
                    }
                }
                """;
        assertEquals(expect, string);
    }

    @Test
    public void test1c() {
        OutputBuilder outputBuilder = create1();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(40).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        @Language("java")
        String expect = """
                package a.b;
                import java.io.IOException;
                import java.net.HttpURLConnection;
                import java.net.MalformedURLException;
                import java.net.URL;
                class X {
                    static HttpURLConnection openConnection(
                        String baseURL,
                        String queryString) throws
                        MalformedURLException,
                        IOException {
                        final StringBuilder buff =
                            new StringBuilder();
                        buff.append(baseURL);
                        if(queryString != null) {
                            buff.append("?");
                            buff.append(queryString);
                        }
                
                        final URL url = new URL(buff
                            .toString());
                
                        return (HttpURLConnection)url
                            .openConnection();
                    }
                }
                """;
        assertEquals(expect, string);
    }

    static OutputBuilder create2() {
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

    @Test
    public void test2() {
        OutputBuilder outputBuilder = create2();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        @Language("java")
        String expect = """
                class X {
                    static HttpURLConnection openConnection(String baseURL, String x1234567890) throws
                        MalformedURLException,
                        IOException { }
                }
                """;
        assertEquals(expect, string);
    }

    @Test
    public void test2b() {
        OutputBuilder outputBuilder = create2();
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(60).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String string = formatter.write(outputBuilder);
        @Language("java")
        String expect = """
                class X {
                    static HttpURLConnection openConnection(
                        String baseURL, String x1234567890) throws
                        MalformedURLException,
                        IOException { }
                }
                """;
        assertEquals(expect, string);
    }


}
