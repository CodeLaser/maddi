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

package org.e2immu.analyzer.modification.analyzer.modification;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/*
Semantic audit 2026-07-18, unsound mechanism #1 (guava CompactHashMap.EntrySetView.remove): calling a
modifying method on the ENCLOSING instance — explicitly via Outer.this or implicitly — must make the inner
class's method modifying: the enclosing instance is reachable from the inner this via the synthetic outer
reference, so its object graph is part of the inner receiver's field graph.
 */
public class TestModificationOuterThis extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class X {
                private int count;

                public void increment() {
                    count++;
                }

                public int getCount() {
                    return count;
                }

                class View {
                    public void touchExplicit() {
                        X.this.increment();
                    }

                    public void touchImplicit() {
                        increment();
                    }

                    public int readOnly() {
                        return X.this.getCount();
                    }
                }
            }
            """;

    @DisplayName("modifying call on the enclosing instance makes the inner method modifying")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        TypeInfo view = X.findSubType("View");
        MethodInfo increment = X.findUniqueMethod("increment", 0);
        assertTrue(increment.isModifying());
        MethodInfo touchExplicit = view.findUniqueMethod("touchExplicit", 0);
        assertTrue(touchExplicit.isModifying(), "X.this.increment() modifies the enclosing instance");
        MethodInfo touchImplicit = view.findUniqueMethod("touchImplicit", 0);
        assertTrue(touchImplicit.isModifying(), "implicit outer call increment() modifies the enclosing instance");
        MethodInfo readOnly = view.findUniqueMethod("readOnly", 0);
        assertTrue(readOnly.isNonModifying(), "reading through the outer reference stays non-modifying");
    }
}
