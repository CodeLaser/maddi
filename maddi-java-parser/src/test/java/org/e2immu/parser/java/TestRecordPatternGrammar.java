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

package org.e2immu.parser.java;

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
}
