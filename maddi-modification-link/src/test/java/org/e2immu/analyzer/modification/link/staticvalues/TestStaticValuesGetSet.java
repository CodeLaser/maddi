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

package org.e2immu.analyzer.modification.link.staticvalues;


import org.e2immu.analyzer.modification.common.getset.ApplyGetSetTranslation;
import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

public class TestStaticValuesGetSet extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import org.e2immu.annotation.Fluent;
            import org.e2immu.annotation.method.GetSet;
            import java.util.Set;
            interface X {
                // normal field
                @GetSet String getS();
                @Fluent @GetSet X setS(String s);
                @GetSet("s") void setS2(String s);
            
                // indexing in a virtual array
                @GetSet("objects") Object get(int i);
                @Fluent @GetSet("objects") X set(int i, Object o);
                @GetSet("objects") void set2(int i, Object o);
                @GetSet("objects") void set3(Object o, int i);
            
                // indexing in a virtual array, same type
                @GetSet("integers") int getI(int i);
                @Fluent @GetSet("integers") X setI(int i, int o);
                @GetSet("integers") void setI2(int i, int o);
                @GetSet("integers") void setI3(int o, int i);
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

            assertTrue(s.isSynthetic());
            MethodInfo get = X.findUniqueMethod("getS", 0);
            assertSame(s, get.getSetField().field());
            MethodLinkedVariables getSv = get.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            // this sv is synthetically created from the @GetSet annotation
            assertEquals("E=this.s", getSv.toString());

            MethodInfo set = X.findUniqueMethod("setS", 1);
            assertSame(s, set.getSetField().field());
            MethodLinkedVariables setSv = get.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);            // this sv is synthetically created from the @GetSet annotation
            assertEquals("E=this this.s=s", setSv.toString());

            MethodInfo set2 = X.findUniqueMethod("setS2", 1);
            assertSame(s, set2.getSetField().field());
            MethodLinkedVariables set2Sv = set2.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            // this sv is synthetically created from the @GetSet annotation
            assertEquals("this.s=s", set2Sv.toString());
        }

        {
            FieldInfo objects = X.getFieldByName("objects", true);

            assertTrue(objects.isSynthetic());
            MethodInfo get = X.findUniqueMethod("get", 1);
            assertSame(objects, get.getSetField().field());
            MethodLinkedVariables getSv = get.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("E=this.objects[i]", getSv.toString());

            MethodInfo set = X.findUniqueMethod("set", 2);
            assertSame(objects, set.getSetField().field());
            MethodLinkedVariables setSv = set.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("E=this objects[i]=o", setSv.toString());

            MethodInfo set2 = X.findUniqueMethod("set2", 2);
            assertSame(objects, set2.getSetField().field());
            MethodLinkedVariables set2Sv = set2.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            ;
            assertEquals("objects[i]=o", set2Sv.toString());

            MethodInfo set3 = X.findUniqueMethod("set3", 2);
            assertSame(objects, set3.getSetField().field());
            MethodLinkedVariables set3Sv = set3.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("objects[i]=o", set3Sv.toString());
        }

        {
            FieldInfo integers = X.getFieldByName("integers", true);

            assertTrue(integers.isSynthetic());
            MethodInfo get = X.findUniqueMethod("getI", 1);
            assertSame(integers, get.getSetField().field());
            MethodLinkedVariables getSv = get.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("E=this.integers[i]", getSv.toString());

            MethodInfo set = X.findUniqueMethod("setI", 2);
            assertSame(integers, set.getSetField().field());
            MethodLinkedVariables setSv = set.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("E=this integers[i]=o", setSv.toString());

            MethodInfo set2 = X.findUniqueMethod("setI2", 2);
            assertSame(integers, set2.getSetField().field());
            MethodLinkedVariables set2Sv = set2.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            assertEquals("integers[i]=o", set2Sv.toString());

            MethodInfo set3 = X.findUniqueMethod("setI3", 2);
            assertSame(integers, set3.getSetField().field());
            MethodLinkedVariables set3Sv = set3.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
            // IMPORTANT: convention is that the first parameter is the index
            assertEquals("integers[o]=i", set3Sv.toString());
        }
    }


    @Language("java")
    private static final String INPUT_2 = """
            package a.b;
            class X {
                interface I { }
                record R(I i) {}
                record Wrapper(R r) {}
                R getter(Wrapper w) {
                    return w.r();
                }
                I extract(Wrapper w) {
                   return w.r().i;
                }
            }
            """;

    /*
            FIXME: the modification area needs to be properly set in the graph algorithm (ComputeLinkCompletion).
            The ExpressionAnalyzer does not do the transitive closure anymore! w <-> w.r <-> w.r.i,
            the graph algorithm must ensure w <-> w.r.i has modification area 0.0
     */
    @DisplayName("getter in field reference")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT_2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);
        {
            MethodInfo getter = X.findUniqueMethod("getter", 1);
            assertEquals("E=w.r", getter.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());
          //  assertEquals("-1-:r, *-4-0:w", getter.analysis().getOrDefault(LINKED_VARIABLES_METHOD, EMPTY)
         //           .toString());
        }
        {
            MethodInfo extract = X.findUniqueMethod("extract", 1);
            Statement s0 = extract.methodBody().statements().getFirst();
            assertEquals("w.r.i", s0.expression().translate(new ApplyGetSetTranslation(runtime)).toString());

            VariableData vd0 = VariableDataImpl.of(s0);
            assertEquals("""
                    a.b.X.R.i#a.b.X.Wrapper.r#a.b.X.extract(a.b.X.Wrapper):0:w, \
                    a.b.X.Wrapper.r#a.b.X.extract(a.b.X.Wrapper):0:w, \
                    a.b.X.extract(a.b.X.Wrapper), \
                    a.b.X.extract(a.b.X.Wrapper):0:w\
                    """, vd0.knownVariableNamesToString());

            VariableInfo viRw = vd0.variableInfo("a.b.X.Wrapper.r#a.b.X.extract(a.b.X.Wrapper):0:w");
            assertNull(viRw.linkedVariables().toString());

            assertEquals("0-4-*:i, *-4-0:w", viRw.linkedVariables().toString());
            VariableInfo viIrw = vd0.variableInfo("a.b.X.R.i#a.b.X.Wrapper.r#a.b.X.extract(a.b.X.Wrapper):0:w");
            assertNull(viIrw.linkedVariables().toString());

            //FIXME assertEquals("*-4-0:r, *-4-0|*-0.0:w", viIrw.linkedVariables().toString());

            assertEquals("E=w.r.i", extract.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());
            //FIXME assertEquals("-1-:i, *-4-0:r, *-4-0|*-0.0:w", extract.analysis().getOrDefault(LINKED_VARIABLES_METHOD, EMPTY).toString());

        }
    }
}