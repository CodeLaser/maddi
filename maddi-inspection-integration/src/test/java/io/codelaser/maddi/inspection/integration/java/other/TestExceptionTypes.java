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

package io.codelaser.maddi.inspection.integration.java.other;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.MethodPrinter;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.info.MethodPrinterImpl;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestExceptionTypes extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.io.IOException;
            import java.net.MalformedURLException;
            import java.net.URL;
            import java.net.HttpURLConnection;
            class X {
                static HttpURLConnection openConnection(String baseURL, String queryString) throws MalformedURLException,
                        IOException {
                    final StringBuilder buff = new StringBuilder();
                    buff.append(baseURL);
                    if (queryString != null) { buff.append("?"); buff.append(queryString); }
                    final URL url = new URL(buff.toString());
                    return (HttpURLConnection)url.openConnection();
                }
            }
            """;

    // we wrote this test to try to discover the lack of a space between "throws" and "MalformedURLException" during printing.

    // the issue does not appear with one single exception
    @Test
    public void test1() {
        TypeInfo x = javaInspector.parse(INPUT1);
        MethodInfo mi = x.findUniqueMethod("openConnection", 2);
        MethodPrinter mp = new MethodPrinterImpl(mi.typeInfo(), mi, true);
        assertEquals("""
                static HttpURLConnection openConnection(String baseURL,String queryString) throws MalformedURLException,IOException{\
                final StringBuilder buff=new StringBuilder();buff.append(baseURL);if(queryString!=null){buff.append("?");\
                buff.append(queryString);}final URL url=new URL(buff.toString());return (HttpURLConnection)url.openConnection();}\
                """, mp.print(javaInspector.runtime().qualificationQualifyFromPrimaryType()).toString());
        @Language("java")
        String expected = """
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
                        if (queryString != null) { buff.append("?"); buff.append(queryString); }
                        final URL url = new URL(buff.toString());
                        return (HttpURLConnection) url.openConnection();
                    }
                }
                """;
        assertEquals(expected, javaInspector.print2(x.compilationUnit()));
    }
}
