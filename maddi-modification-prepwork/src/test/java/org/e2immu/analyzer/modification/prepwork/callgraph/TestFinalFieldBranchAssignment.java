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

package org.e2immu.analyzer.modification.prepwork.callgraph;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// effectively-final-field detection: a field assigned anywhere in the primary type must not be reported final.
// The interesting cases are assignments inside lambda / anonymous / local types enclosed by the field's owner:
// those methods have no field->method edge in the call graph (their typeInfo is not the owner), so finality
// detection must inspect them directly. See ComputePartOfConstructionFinalField.computeEffectivelyFinalFields.
public class TestFinalFieldBranchAssignment extends CommonTest {

    private boolean isFinal(@Language("java") String input) {
        TypeInfo X = javaInspector.parse(ABX, input);
        new PrepAnalyzer(runtime).doPrimaryType(X);
        FieldInfo i = X.getFieldByName("i", true);
        // guard against the default-value trap: the property must actually have been computed
        assertTrue(i.analysis().haveAnalyzedValueFor(PropertyImpl.FINAL_FIELD), "FINAL_FIELD not computed");
        return i.analysis().getOrDefault(PropertyImpl.FINAL_FIELD, FALSE).isTrue();
    }

    @DisplayName("field assigned in an early-return branch is not final")
    @Test
    public void testEarlyReturnBranch() {
        assertFalse(isFinal("""
                package a.b;
                class X {
                    private int i;
                    public X(int i) { this.i = i; }
                    public void mutate(boolean c) { if (c) { this.i = 3; return; } System.out.println("x"); }
                }
                """));
    }

    @DisplayName("field assigned inside a switch-expression branch is not final")
    @Test
    public void testSwitchExpressionBranch() {
        assertFalse(isFinal("""
                package a.b;
                class X {
                    private int i;
                    public X(int i) { this.i = i; }
                    public void mutate(int v) {
                        int x = switch (v) { case 1 -> { this.i = 1; yield 1; } default -> 2; };
                        System.out.println(x);
                    }
                }
                """));
    }

    @DisplayName("field assigned inside a lambda body is not final")
    @Test
    public void testLambdaBody() {
        assertFalse(isFinal("""
                package a.b;
                class X {
                    private int i;
                    public X(int i) { this.i = i; }
                    public void mutate() {
                        Runnable r = () -> { this.i = 1; };
                        r.run();
                    }
                }
                """));
    }

    @DisplayName("field assigned inside an anonymous-class method is not final")
    @Test
    public void testAnonymousClassMethod() {
        assertFalse(isFinal("""
                package a.b;
                class X {
                    private int i;
                    public X(int i) { this.i = i; }
                    public void mutate() {
                        Runnable r = new Runnable() { public void run() { i = 1; } };
                        r.run();
                    }
                }
                """));
    }

    private boolean isFinalInInner(@Language("java") String input) {
        TypeInfo X = javaInspector.parse(ABX, input);
        new PrepAnalyzer(runtime).doPrimaryType(X);
        FieldInfo i = X.findSubType("Inner").getFieldByName("i", true);
        assertTrue(i.analysis().haveAnalyzedValueFor(PropertyImpl.FINAL_FIELD), "FINAL_FIELD not computed");
        return i.analysis().getOrDefault(PropertyImpl.FINAL_FIELD, FALSE).isTrue();
    }

    @DisplayName("nested type's field assigned by the enclosing type is not final")
    @Test
    public void testAssignmentFromEnclosingType() {
        assertFalse(isFinalInInner("""
                package a.b;
                class X {
                    static class Inner {
                        private int i;
                        Inner(int i) { this.i = i; }
                        int get() { return i; }
                    }
                    void renumber(Inner inner) { inner.i = 42; }
                }
                """));
    }

    @DisplayName("nested type's field assigned by a sibling nested type is not final")
    @Test
    public void testAssignmentFromSiblingType() {
        assertFalse(isFinalInInner("""
                package a.b;
                class X {
                    static class Inner {
                        private int i;
                        Inner(int i) { this.i = i; }
                        int get() { return i; }
                    }
                    static class Renumberer {
                        void renumber(Inner inner) { inner.i = 42; }
                    }
                }
                """));
    }

    @DisplayName("positive control: field assigned only in the constructor stays final")
    @Test
    public void testFinalControl() {
        assertTrue(isFinal("""
                package a.b;
                class X {
                    private int i;
                    public X(int i) { this.i = i; }
                    public int get() { return i; }
                }
                """));
    }
}
