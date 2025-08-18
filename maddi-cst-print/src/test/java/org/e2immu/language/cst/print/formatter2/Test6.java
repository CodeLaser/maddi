package org.e2immu.language.cst.print.formatter2;

import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.cst.print.formatter.TestFormatter4;
import org.e2immu.language.cst.print.formatter.TestFormatter6;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Test6 {
    private final Runtime runtime = new RuntimeImpl();

    @Test
    public void test1() {
        OutputBuilder outputBuilder = TestFormatter6.create1();
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
        OutputBuilder outputBuilder = TestFormatter6.create1();
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
        OutputBuilder outputBuilder = TestFormatter6.create1();
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


    @Test
    public void test2() {
        OutputBuilder outputBuilder = TestFormatter6.create2();
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
        OutputBuilder outputBuilder = TestFormatter6.create2();
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
