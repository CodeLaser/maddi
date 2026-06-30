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

package org.e2immu.language.java.openjdk.other;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A method call's detailed sources must contain an entry keyed by the called method's simple name
 * ({@code mc.source().detailedSources().detail(mc.methodInfo().name())}), with the source span of that name.
 */
public class TestMethodCallDetailedSources extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.List;
            class X {
                void member(List<String> list) {
                    list.add("x");
                }
                void implicitCall() {
                    helper();
                }
                void helper() {
                }
                int staticCall(String s) {
                    return Integer.parseInt(s);
                }
            }
            """;

    private static MethodCall firstCall(MethodInfo methodInfo) {
        Statement s = methodInfo.methodBody().statements().getFirst();
        org.e2immu.language.cst.api.expression.Expression e =
                s instanceof ReturnStatement rs ? rs.expression() : s.expression();
        return (MethodCall) e;
    }

    private static String detailOfName(MethodCall mc) {
        var detail = mc.source().detailedSources().detail(mc.methodInfo().name());
        assertNotNull(detail, "no detailed source for method name '" + mc.methodInfo().name() + "'");
        return detail.toString();
    }

    @DisplayName("object.method(...): detail keyed by the method name, spanning the name")
    @Test
    public void memberSelect() {
        TypeInfo X = scan("a.b.X", INPUT);
        MethodCall mc = firstCall(X.findUniqueMethod("member", 1));
        assertEquals("add", mc.methodInfo().name());
        assertEquals("-@5:14-5:16", detailOfName(mc)); // 'add' on line 5
    }

    @DisplayName("implicit method(...): detail keyed by the method name")
    @Test
    public void implicitCall() {
        TypeInfo X = scan("a.b.X", INPUT);
        MethodCall mc = firstCall(X.findUniqueMethod("implicitCall", 0));
        assertEquals("helper", mc.methodInfo().name());
        assertEquals("-@8:9-8:14", detailOfName(mc)); // 'helper' on line 8
    }

    @DisplayName("Type.staticMethod(...): detail keyed by the method name")
    @Test
    public void staticCall() {
        TypeInfo X = scan("a.b.X", INPUT);
        MethodCall mc = firstCall(X.findUniqueMethod("staticCall", 1));
        assertEquals("parseInt", mc.methodInfo().name());
        assertEquals("-@13:24-13:31", detailOfName(mc)); // 'parseInt' on line 13
    }
}
