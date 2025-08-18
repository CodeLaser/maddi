package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.MethodPrinter;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.impl.info.MethodPrinterImpl;
import org.e2immu.language.cst.print.FormatterImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
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
                    if(queryString != null) { buff.append("?"); buff.append(queryString); }
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
                        if(queryString != null) { buff.append("?"); buff.append(queryString); }
                        final URL url = new URL(buff.toString());
                        return (HttpURLConnection)url.openConnection();
                    }
                }
                """;
        assertEquals(expected, javaInspector.print2(x));
    }
}
