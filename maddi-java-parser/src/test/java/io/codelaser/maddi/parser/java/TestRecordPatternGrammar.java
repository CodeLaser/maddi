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

package io.codelaser.maddi.parser.java;

import org.junit.jupiter.api.Test;
import org.parsers.java.JavaParser;

// Regression for the CongoCC grammar update (record patterns + unnamed variables). CompilationUnit() throws
// org.parsers.java.ParseException on a syntax the grammar rejects, so a clean return is the assertion.
public class TestRecordPatternGrammar {

    static void assertParses(String code) {
        JavaParser p = new JavaParser(code);
        p.setParserTolerant(false);
        p.CompilationUnit();
    }

    @Test
    public void unnamedVarInRecordPattern() {
        // the specific gap that blocked JDK 26 sources (e.g. java.net.http QuicEndpoint): an unnamed variable
        // '_' inside a record deconstruction pattern in a switch.
        assertParses("class X { record R(int a, int b, int c) {} Object m(Object o){ return switch(o){ "
                + "case R(var a, var _, var _) -> a; default -> 0; }; } }");
    }

    @Test
    public void assortedPatterns() {
        assertParses("class X { record R(int a, int b) {} void m(Object o){ switch(o){ "
                + "case R(int a, int b) -> {} case R r when r.a() > 0 -> {} default -> {} } } }");
    }

    /*
     * The JEP 456 spelling matrix, added 2026-08-19: the trino corpus @ e51d8cb9db9 fails the
     * detailed-source pre-scan on 36 files, and the two spellings the campaign record names are a
     * statement-level `var _ = ...` (BaseStrictSymbolsMatcher) and a bare `_` as the FIRST component of a
     * record deconstruction (QuantileDigestParametricType). One test per spelling, so a failure names the
     * production rather than the feature.
     */

    @Test
    public void unnamedLocalVar() {
        assertParses("class X { int m(){ var _ = hashCode(); return 1; } }");
    }

    @Test
    public void unnamedLocalTyped() {
        assertParses("class X { int m(){ int _ = hashCode(); return 1; } }");
    }

    @Test
    public void bareUnderscoreFirstComponent() {
        // trino QuantileDigestParametricType:36 -- underscore FIRST, named component after the comma
        assertParses("class X { record R(int a, int b) {} int m(Object o){ "
                + "if (o instanceof R(_, int b)) return b; return 0; } }");
    }

    @Test
    public void bareUnderscoreLastComponent() {
        assertParses("class X { record R(int a, int b) {} int m(Object o){ "
                + "if (o instanceof R(int a, _)) return a; return 0; } }");
    }

    @Test
    public void bareUnderscoreInCase() {
        assertParses("class X { record R(int a, int b) {} int m(Object o){ return switch(o){ "
                + "case R(_, int b) -> b; default -> 0; }; } }");
    }

    @Test
    public void unnamedCatchParameter() {
        assertParses("class X { int m(){ try { return hashCode(); } catch (RuntimeException _) { return 0; } } }");
    }

    @Test
    public void unnamedLambdaParameters() {
        assertParses("class X { void m(java.util.Map<String,String> map){ "
                + "map.forEach((_, _) -> hashCode()); "
                + "java.util.function.Function<String,Integer> f = _ -> 1; } }");
    }

    @Test
    public void unnamedEnhancedForVariable() {
        assertParses("class X { int m(java.util.List<String> in){ int n = 0; "
                + "for (var _ : in) { n++; } for (String _ : in) { n++; } return n; } }");
    }

    @Test
    public void unnamedBasicForVariable() {
        assertParses("class X { void m(){ for (int i = 0, _ = hashCode(); i < 3; i++) { } } }");
    }

    @Test
    public void unnamedTryWithResources() {
        assertParses("class X { void m(AutoCloseable a) throws Exception { try (var _ = a) { } } }");
    }

    @Test
    public void unnamedTypePatternInCase() {
        assertParses("class X { int m(Object o){ return switch(o){ case String _ -> 1; default -> 0; }; } }");
    }
}
