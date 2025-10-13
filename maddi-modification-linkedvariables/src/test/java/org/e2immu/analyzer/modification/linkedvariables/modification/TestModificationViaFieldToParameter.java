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

package org.e2immu.analyzer.modification.linkedvariables.modification;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestModificationViaFieldToParameter extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class X {
                static class M { int i; M(int i) { this.i = i; } void setI(int i) { this.i = i; } }
                static Go callGo(int i) {
                    M m = new M(i);
                    return new Go(m);
                }
                static class Go {
                    private M m;
                    Go(M m) {
                        this.m = m;
                    }
                    void inc() {
                        this.m.i++;
                    }
                }
                static class Go2 {
                    private M m;
                    Go2(M m) {
                        this.m = m;
                    }
                    int get() {
                        return this.m.i;
                    }
                }
            }
            """;

    @DisplayName("does the modification travel via the field?")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        {
            TypeInfo go = X.findSubType("Go");
            MethodInfo constructor = go.findConstructor(1);
            ParameterInfo p0 = constructor.parameters().getFirst();
            assertFalse(p0.isUnmodified());
        }
        {
            TypeInfo go = X.findSubType("Go2");
            MethodInfo constructor = go.findConstructor(1);
            ParameterInfo p0 = constructor.parameters().getFirst();
            assertTrue(p0.isUnmodified());
        }
    }

}
