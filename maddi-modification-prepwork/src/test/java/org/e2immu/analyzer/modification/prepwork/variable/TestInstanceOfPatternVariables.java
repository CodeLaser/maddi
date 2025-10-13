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
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestInstanceOfPatternVariables extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Objects;
            public class X {
                String getId() {
                    return Objects.toIdentityString(this);
                }
                interface HibernateProxy {
                    Class<?> getPersistentClass();
                }
                @Override
                  public final boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null) return false;
                    Class<?> oEffectiveClass = o instanceof HibernateProxy hp ? hp.getPersistentClass() : o.getClass();
                    Class<?> thisEffectiveClass = this instanceof HibernateProxy hp
                      ? hp.getPersistentClass() : this.getClass();
                    if (thisEffectiveClass != oEffectiveClass) return false;
                    X x = (X)o;
                    return getId() != null && Objects.equals(getId(), x.getId());
                  }
            }
            """;


    @DisplayName("field of getter should exist, analysis order")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);
        MethodInfo equals = X.findUniqueMethod("equals", 1);
        {
            LocalVariableCreation lvc = (LocalVariableCreation) equals.methodBody().statements().get(2);
            VariableData vd = VariableDataImpl.of(lvc);
            VariableInfo vi = vd.variableInfo("hp");
            assertNotNull(vi);
            assertEquals("D:2, A:[2]", vi.assignments().toString());
        }
        {
            LocalVariableCreation lvc = (LocalVariableCreation) equals.methodBody().statements().get(3);
            VariableData vd = VariableDataImpl.of(lvc);
            VariableInfo vi = vd.variableInfo("hp");
            assertEquals("D:3, A:[3]", vi.assignments().toString());
        }
        {
            IfElseStatement stmt = (IfElseStatement) equals.methodBody().statements().get(4);
            VariableData vd = VariableDataImpl.of(stmt);
            assertFalse(vd.isKnown("hp"));
        }
    }

}
