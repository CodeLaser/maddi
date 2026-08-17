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

package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link org.e2immu.analyzer.modification.common.getset.GetSetHelper} leading-guard tolerance: a getter/setter
 * preceded by a prefix of <em>inert guard</em> statements is still recognised as the accessor it plainly is. The
 * motivating case is {@code InfoImpl.analysis()} / {@code ParameterInfoImpl.analysis()}, which open with a gated
 * diagnostic ({@code if (ConsumptionEdgeRecorder.ENABLED) record(this);}) before {@code return theField;}.
 * <p>
 * The tolerance is deliberately narrow (see the class comment of {@code GetSetHelper}): the guard must fall
 * through, touch no field of {@code this}, and make no call <em>on</em> {@code this} — while a call that merely
 * <em>passes</em> {@code this} to a static method (the {@code record(this)} shape) is allowed. The negative cases
 * below pin the boundary.
 */
public class TestGetSetGuardTolerance extends CommonTest {

    // GOOD is the exact shape of InfoImpl.analysis(): a gated static call passing 'this', then a bare getter/setter.
    // log(this) is static, so its receiver is a type, not 'this' -> allowed; DEBUG is a compile-time-ish flag.
    @Language("java")
    private static final String GOOD = """
            package a.b;
            class X {
                static final boolean DEBUG = false;
                private int f;
                private static void log(Object o) { }

                int getF() {
                    if (DEBUG) { log(this); }
                    return f;
                }
                void setF(int f) {
                    if (DEBUG) { log(this); }
                    this.f = f;
                }
                X setFluent(int f) {
                    if (DEBUG) { log(this); }
                    this.f = f;
                    return this;
                }
            }
            """;

    @DisplayName("a getter/setter/fluent-setter behind an inert diagnostic guard is recognised on the field")
    @Test
    public void testGuardedAccessorsRecognised() {
        TypeInfo X = javaInspector.parse("a.b.X", GOOD);
        new PrepAnalyzer(runtime).doPrimaryType(X);
        FieldInfo f = X.getFieldByName("f", true);

        assertSame(f, X.findUniqueMethod("getF", 0).getSetField().field(), "guarded getter");
        assertSame(f, X.findUniqueMethod("setF", 1).getSetField().field(), "guarded setter");
        MethodInfo setFluent = X.findUniqueMethod("setFluent", 1);
        assertSame(f, setFluent.getSetField().field(), "guarded fluent setter");
        assertTrue(setFluent.isFluent(), "the fluent setter keeps its fluency");
    }

    // each method carries a guard that violates exactly one requirement, so none is an accessor
    @Language("java")
    private static final String BAD = """
            package a.b;
            class X {
                static final boolean DEBUG = false;
                private int f;
                private int g;
                private void touch() { }

                int writesField() {          // guard writes a sibling field -> not inert
                    if (DEBUG) { g = 1; }
                    return f;
                }
                int earlyExit() {            // guard can exit early -> a second behaviour, not a getter
                    if (DEBUG) { return 0; }
                    return f;
                }
                int readsField() {           // guard references a field of 'this' -> not inert
                    if (f > 0) { }
                    return f;
                }
                int callsOnThis() {          // guard calls a method on 'this' (could mutate) -> not inert
                    if (DEBUG) { touch(); }
                    return f;
                }
            }
            """;

    @DisplayName("a guard that writes/reads a field, calls on this, or can exit early defeats recognition")
    @Test
    public void testUnsoundGuardsRejected() {
        TypeInfo X = javaInspector.parse("a.b.X", BAD);
        new PrepAnalyzer(runtime).doPrimaryType(X);

        assertNull(X.findUniqueMethod("writesField", 0).getSetField().field(), "guard writes a field");
        assertNull(X.findUniqueMethod("earlyExit", 0).getSetField().field(), "guard can exit early");
        assertNull(X.findUniqueMethod("readsField", 0).getSetField().field(), "guard reads a field");
        assertNull(X.findUniqueMethod("callsOnThis", 0).getSetField().field(), "guard calls a method on this");
    }
}
