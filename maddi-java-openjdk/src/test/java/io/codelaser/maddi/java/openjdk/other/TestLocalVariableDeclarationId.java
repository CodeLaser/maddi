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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CodeLaser/maddi#127: two same-named locals of sibling scopes are equal (a local's FQN is its name), so a tool
 * asking "is the local this expression reads assigned anywhere in the method?" conflated them. The declaration
 * now has an identity of its own.
 */
public class TestLocalVariableDeclarationId extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.Map;
            import java.util.TreeMap;
            class X {
                Map<String, String> method(Map<String, String> ref, Map<String, String> record) {
                    if (ref != null) {
                        Map<String, String> m = ref;
                        if (m.isEmpty()) return m;
                    }
                    Map<String, String> m = new TreeMap<>();
                    for (Map.Entry<String, String> r : record.entrySet()) {
                        m.put(r.getKey(), r.getValue());
                    }
                    return m;
                }
            }
            """;

    @DisplayName("reads share their declaration's id; the two declarations of 'm' do not")
    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        List<LocalVariable> declared = new ArrayList<>();
        List<LocalVariable> read = new ArrayList<>();
        method.methodBody().visit((Element e) -> {
            if (e instanceof LocalVariableCreation lvc && "m".equals(lvc.localVariable().simpleName())) {
                declared.add(lvc.localVariable());
            }
            if (e instanceof VariableExpression ve && ve.variable() instanceof LocalVariable lv
                && "m".equals(lv.simpleName())) {
                read.add(lv);
            }
            return true;
        });
        assertEquals(2, declared.size());
        assertEquals(declared.get(0), declared.get(1), "equals is by name, as before");
        assertNotEquals(declared.get(0).declarationId(), declared.get(1).declarationId());
        // reads: m.isEmpty(), return m (first); m.put(...), return m (second)
        assertEquals(4, read.size());
        assertEquals(declared.get(0).declarationId(), read.get(0).declarationId());
        assertEquals(declared.get(0).declarationId(), read.get(1).declarationId());
        assertEquals(declared.get(1).declarationId(), read.get(2).declarationId());
        assertEquals(declared.get(1).declarationId(), read.get(3).declarationId());
        // copies keep it
        LocalVariable first = declared.getFirst();
        assertSame(first.declarationId(), first.withName("n").declarationId());
        assertSame(first.declarationId(), first.withType(runtime.objectParameterizedType()).declarationId());
        assertSame(first.declarationId(), first.withAssignmentExpression(null).declarationId());
    }
}
