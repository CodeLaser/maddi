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

package org.e2immu.analyzer.modification.link.impl;


import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

// complementary to TestStaticValuesGetSet
public class TestGetSet extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import org.e2immu.annotation.Fluent;
            import org.e2immu.annotation.method.GetSet;
            import java.util.Set;
            class X {
                // normal field
                private String s;
                public String getS() { return s; }
                @Fluent @GetSet X setS(String s) {
                    this.s = s;
                    return this;
                }
                private Object[] objects;
                public Object getObject(int i) { return objects[i]; }
                @Fluent X set(int i, Object o) {
                    objects[i] = o;
                    return this;
                }
                private int[] integers;
                public int getInteger(int i) { return integers[i]; }
                @Fluent X setI(int i, int o) {
                    integers[i] = o;
                    return this;
                }
            }
            """;

    @DisplayName("modification of an array component element")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse(INPUT);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);
        {
            FieldInfo s = X.getFieldByName("s", true);

            assertFalse(s.isSynthetic());
            MethodInfo get = X.findUniqueMethod("getS", 0);
            assertSame(s, get.getSetField().field());
            MethodLinkedVariables getSv = get.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            // this sv is synthetically created from the @GetSet annotation
            assertEquals("[] --> getS←this.s", getSv.toString());

            MethodInfo setS = X.findUniqueMethod("setS", 1);
            assertSame(s, setS.getSetField().field());
            MethodLinkedVariables setSv = setS.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);            // this sv is synthetically created from the @GetSet annotation
            assertEquals("[0:s→this*.s] --> setS.s←this*.s,setS.s←0:s,setS←this*", setSv.toString());
        }
        {
            FieldInfo objects = X.getFieldByName("objects", true);

            assertFalse(objects.isSynthetic());
            MethodInfo get = X.findUniqueMethod("getObject", 1);
            assertSame(objects, get.getSetField().field());
            MethodLinkedVariables getSv = get.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("[-] --> getObject←this.objects[0:i],getObject∈this.objects", getSv.toString());

            MethodInfo set = X.findUniqueMethod("set", 2);
            assertSame(objects, set.getSetField().field());
            MethodLinkedVariables setSv = set.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("""
                    [-, 1:o→this.objects*[0:i],1:o∈this.objects*] --> set.objects[0:i]←this.objects*[0:i],\
                    set.objects[0:i]←1:o,set.objects[0:i]∈set.objects,\
                    set.objects[0:i]∈this.objects*,set.objects←this.objects*,\
                    set.objects∋this.objects*[0:i],set.objects∋1:o,set←this*\
                    """, setSv.toString());
        }
        {
            FieldInfo integers = X.getFieldByName("integers", true);

            assertFalse(integers.isSynthetic());
            MethodInfo get = X.findUniqueMethod("getInteger", 1);
            assertSame(integers, get.getSetField().field());
            MethodLinkedVariables getSv = get.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("[-] --> getInteger←this.integers[0:i],getInteger∈this.integers", getSv.toString());

            MethodInfo set = X.findUniqueMethod("setI", 2);
            assertSame(integers, set.getSetField().field());
            MethodLinkedVariables setSv = set.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("""
                    [-, 1:o→this.integers*[0:i],1:o∈this.integers*] --> setI.integers[0:i]←this.integers*[0:i],\
                    setI.integers[0:i]←1:o,setI.integers[0:i]∈setI.integers,\
                    setI.integers[0:i]∈this.integers*,setI.integers←this.integers*,\
                    setI.integers∋this.integers*[0:i],setI.integers∋1:o,setI←this*\
                    """, setSv.toString());
        }
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            class B {
                private int i;
                B setI(int i) { this.i = i; return this; }
                B setI2(int i, boolean condition) {
                    if (condition) {
                        System.out.println("true, i = "+i);
                        return this;
                    } else {
                        System.out.println("false, i = "+i);
                        B b = this;
                        return b;
                    }
                }
            }
            """;

    @DisplayName("modification in @Fluent setter")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo setI = X.findUniqueMethod("setI", 1);
        MethodLinkedVariables mlvSetI = setI.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(setI));
        assertEquals("[0:i→this*.i] --> setI.i←this*.i,setI.i←0:i,setI←this*", mlvSetI.toString());
    }

}