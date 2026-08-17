/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 */

package org.e2immu.language.cst.print.formatter2;

import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
Demonstrates the opt-in alwaysBreakPriorityBlocks flag. It reuses Test6.create1 (a real class
with a method whose body contains a nested if-block). At width 120 the default (compact) layout
keeps the if-block inline: `if(queryString != null) { buff.append("?"); buff.append(queryString); }`
(see Test6.test1). With the flag on, every priority guide block (generatorForBlock: class body,
method body, if body) is broken onto its own indented lines regardless of whether it fits.
 */
public class TestBreakPriorityBlocks {
    private final Runtime runtime = new RuntimeImpl();

    @Test
    public void priorityBlocksBreakEvenWhenTheyFit() {
        OutputBuilder ob = Test6.create1();
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(120).setSpacesInTab(4)
                .setAlwaysBreakPriorityBlocks(true).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String out = formatter.write(ob);
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
                        if(queryString != null) {
                            buff.append("?");
                            buff.append(queryString);
                        }
                        final URL url = new URL(buff.toString());
                        return (HttpURLConnection)url.openConnection();
                    }
                }
                """;
        assertEquals(expect, out);
    }

    /*
    With the flag OFF (the production default) the same input keeps the if-block inline —
    i.e. we have not changed default behaviour.
     */
    @Test
    public void defaultKeepsCompactInlineBlocks() {
        OutputBuilder ob = Test6.create1();
        FormattingOptions options = new FormattingOptionsImpl.Builder()
                .setLengthOfLine(120).setSpacesInTab(4).build();
        Formatter formatter = new Formatter2Impl(runtime, options);
        String out = formatter.write(ob);
        assertEquals(true, out.contains("if(queryString != null) { buff.append(\"?\"); buff.append(queryString); }"));
    }
}
