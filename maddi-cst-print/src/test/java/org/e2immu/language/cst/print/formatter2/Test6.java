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
