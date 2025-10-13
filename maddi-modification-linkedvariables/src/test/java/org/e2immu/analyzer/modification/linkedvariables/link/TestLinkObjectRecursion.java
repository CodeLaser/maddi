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

package org.e2immu.analyzer.modification.linkedvariables.link;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl.EMPTY;
import static org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl.LINKED_VARIABLES_METHOD;
import static org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl.STATIC_VALUES_METHOD;
import static org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl.STATIC_VALUES_PARAMETER;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.NO_VALUE;
import static org.junit.jupiter.api.Assertions.*;

public class TestLinkObjectRecursion extends CommonTest {


    @Language("java")
    private static final String INPUT0 = """
            package a.b;
            import java.util.Iterator;
            import java.util.function.Function;
            class X {
                record LL<T>(T head, LL<T> tail) {
                    LL<T> prepend(T t) {
                        LL<T> ll = new LL<>(t, this);
                        return ll;
                    }
                    LL<T> prepend2(T t) {
                        return new LL<>(t, this);
                    }
                }
            }
            """;

    @DisplayName("prep-work, with immutable LL")
    @Test
    public void test0() {
        TypeInfo X = javaInspector.parse(INPUT0);
        TypeInfo LL = X.findSubType("LL");
        LL.analysis().set(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.IMMUTABLE_HC); // cannot compute that yet
        List<Info> analysisOrder = prepWork(X);
        assertEquals("0=T, 1=LL", LL.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE).detailedSortedTypes());

        analyzer.go(analysisOrder, 2);

        This thisVar = runtime.newThis(LL.asParameterizedType());
        MethodInfo prepend = LL.findUniqueMethod("prepend", 1);
        {
            Statement s0 = prepend.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0This = vd0.variableInfo(thisVar);
            assertEquals("0-4-0:ll, 0-4-*:t", vi0This.linkedVariables().toString());
            VariableInfo vi0T = vd0.variableInfo(prepend.parameters().getFirst());
            assertEquals("*-4-0:ll, *-4-0:this", vi0T.linkedVariables().toString());
            VariableInfo vi0LL = vd0.variableInfo("ll");

            // interpretation: t is an integral part of the hidden content of 'll'; 0 is the type of T
            // at the same time, because LL is mutable (we do not analyzer IMMUTABLE_TYPE yet), ll is linked to 'this':
            // only the hidden content type 0 is added as context/label.
            assertEquals("0-4-*:t, 0-4-0:this", vi0LL.linkedVariables().toString());
        }

        MethodInfo prepend2 = LL.findUniqueMethod("prepend2", 1);
        {
            Statement s0 = prepend2.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0This = vd0.variableInfo(thisVar);
            assertEquals("", vi0This.linkedVariables().toString());
            VariableInfo vi0T = vd0.variableInfo(prepend2.parameters().getFirst());
            assertEquals("", vi0T.linkedVariables().toString());
            VariableInfo vi0Rv = vd0.variableInfo(prepend2.fullyQualifiedName());
            assertEquals("0-4-*:t, 0-4-0:this", vi0Rv.linkedVariables().toString());
        }
    }

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Iterator;
            import java.util.function.Function;
            class X {
                record LL<T>(T head, LL<T> tail) {
                    LL<T> prepend(T t) {
                        LL<T> ll = new LL<>(t, this);
                        return ll;
                    }
                    LL<T> prepend2(T t) {
                        return new LL<>(t, this);
                    }
                }
                static <T> void add(LL<T> list, T one) {
                    LL<T> longer = list.prepend(one);
                    assert longer.head != null;
                }
            }
            """;

    @DisplayName("linked list, with mutable LL")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        TypeInfo LL = X.findSubType("LL");
        assertEquals("0=T, 1=LL", LL.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE).detailedSortedTypes());

        analyzer.go(analysisOrder);
        assertTrue(LL.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE).isMutable());

        This thisVar = runtime.newThis(LL.asParameterizedType());
        MethodInfo prepend = LL.findUniqueMethod("prepend", 1);
        {
            Statement s0 = prepend.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0This = vd0.variableInfo(thisVar);
            assertEquals("0-2-0|*-1:ll, 0-4-*:t", vi0This.linkedVariables().toString());
            VariableInfo vi0T = vd0.variableInfo(prepend.parameters().getFirst());
            assertEquals("*-4-0:ll, *-4-0:this", vi0T.linkedVariables().toString());
            VariableInfo vi0LL = vd0.variableInfo("ll");

            // interpretation: t is an integral part of the hidden content of 'll'; 0 is the type of T
            // at the same time, because LL is mutable (we do not analyzer IMMUTABLE_TYPE yet), ll is linked to 'this':
            // only the hidden content type 0 is added as context/label.
            assertEquals("0-4-*:t, 0-2-0|1-*:this", vi0LL.linkedVariables().toString());
        }
        assertEquals("0-4-*:t, 0-2-0|1-*:this", prepend.analysis().getOrDefault(LINKED_VARIABLES_METHOD, EMPTY)
                .toString());

        MethodInfo prepend2 = LL.findUniqueMethod("prepend2", 1);
        {
            Statement s0 = prepend2.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0This = vd0.variableInfo(thisVar);
            assertEquals("", vi0This.linkedVariables().toString());
            VariableInfo vi0T = vd0.variableInfo(prepend2.parameters().getFirst());
            assertEquals("", vi0T.linkedVariables().toString());
            VariableInfo vi0Rv = vd0.variableInfo(prepend2.fullyQualifiedName());
            assertEquals("0-4-*:t, 0-2-0|1-*:this", vi0Rv.linkedVariables().toString());
        }
        assertEquals("0-4-*:t, 0-2-0|1-*:this", prepend2.analysis().getOrDefault(LINKED_VARIABLES_METHOD, EMPTY)
                .toString());
    }


    @DisplayName("immutable HC linked list")
    @Test
    public void test1b() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        TypeInfo LL = X.findSubType("LL");
        assertEquals("0=T, 1=LL", LL.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE).detailedSortedTypes());

        // we're explicitly setting IMMUTABLE_HC because we cannot compute it yet
        LL.analysis().set(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.IMMUTABLE_HC);

        analyzer.go(analysisOrder, 2);

        This thisVar = runtime.newThis(LL.asParameterizedType());
        MethodInfo prepend = LL.findUniqueMethod("prepend", 1);
        {
            Statement s0 = prepend.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0This = vd0.variableInfo(thisVar);
            assertEquals("0-4-0:ll, 0-4-*:t", vi0This.linkedVariables().toString());
            VariableInfo vi0T = vd0.variableInfo(prepend.parameters().getFirst());
            assertEquals("*-4-0:ll, *-4-0:this", vi0T.linkedVariables().toString());
            VariableInfo vi0LL = vd0.variableInfo("ll");
            assertEquals("0-4-*:t, 0-4-0:this", vi0LL.linkedVariables().toString());
        }

        MethodInfo prepend2 = LL.findUniqueMethod("prepend2", 1);
        {
            Statement s0 = prepend2.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0This = vd0.variableInfo(thisVar);
            assertEquals("", vi0This.linkedVariables().toString());
            VariableInfo vi0T = vd0.variableInfo(prepend2.parameters().getFirst());
            assertEquals("", vi0T.linkedVariables().toString());
            VariableInfo vi0Rv = vd0.variableInfo(prepend2.fullyQualifiedName());
            assertEquals("0-4-*:t, 0-4-0:this", vi0Rv.linkedVariables().toString());
        }
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Iterator;
            import java.util.function.Function;
            class X {
            
                interface LoopData {
                    LoopData next();
                }
            
                static class LoopDataImpl {
                    private Iterator<?> loop;
                    private Object loopValue;
                    private Function<LoopData, LoopData> body;
            
                    LoopDataImpl(Iterator<?> loop, Function<LoopData, LoopData> body, Object loopValue) {
                        this.loop = loop;
                        this.loopValue = loopValue;
                        this.body = body;
                    }
            
                    @Override
                    public LoopData next() {
                        if (loop != null) {
                            Object loopValue = loop.next();
                            LoopData nextLd = withLoopValue(loopValue);
                            return body.apply(nextLd);
                        }
                        return body.apply(this);
                    }
            
                    private LoopData withLoopValue(Object loopValue) {
                        return new LoopDataImpl(loop, body, loopValue);
                    }
                }
            }
            """;

    @DisplayName("links between this and self-referencing objects")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        TypeInfo function = javaInspector.compiledTypesManager().get(Function.class);
        MethodInfo apply = function.singleAbstractMethod();
        assertTrue(apply.isModifying());
        ParameterInfo apply0 = apply.parameters().getFirst();
        assertTrue(apply0.isModified());

        TypeInfo loopDataImpl = X.findSubType("LoopDataImpl");
        HiddenContentTypes ldiHct = loopDataImpl.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=Iterator, 1=Object, 2=Function, 3=LoopData", ldiHct.detailedSortedTypes());

        This thisVar = runtime.newThis(loopDataImpl.asParameterizedType());
        FieldInfo loop = loopDataImpl.getFieldByName("loop", true);
        assertFalse(loop.isFinal());
        assertTrue(loop.isPropertyFinal());
        // loop is modified, because next() is called on it
        assertSame(ValueImpl.BoolImpl.FALSE, loop.analysis().getOrDefault(PropertyImpl.UNMODIFIED_FIELD,
                ValueImpl.BoolImpl.FALSE));

        FieldInfo body = loopDataImpl.getFieldByName("body", true);
        FieldReference bodyFr = runtime.newFieldReference(body);

        MethodInfo constructor = loopDataImpl.findConstructor(3);
        ParameterInfo c0 = constructor.parameters().getFirst();
        assertEquals("loop", c0.simpleName());
        // NOTE: we know this SVP value only after analyzing withLoopValue
        assertEquals("E=this.loop", c0.analysis().getOrDefault(STATIC_VALUES_PARAMETER, StaticValuesImpl.NONE).toString());

        MethodInfo withLoopValue = loopDataImpl.findUniqueMethod("withLoopValue", 1);
        assertEquals("Type a.b.X.LoopDataImpl E=new LoopDataImpl(this.loop,this.body,loopValue) this.body=this.body, this.loop=this.loop, this.loopValue=loopValue",
                withLoopValue.analysis().getOrDefault(STATIC_VALUES_METHOD, StaticValuesImpl.NONE).toString());
        Value.Independent wlvIndependent = withLoopValue.analysis().getOrDefault(PropertyImpl.INDEPENDENT_METHOD, ValueImpl.IndependentImpl.DEPENDENT);
        assertTrue(wlvIndependent.isDependent());

        MethodInfo next = loopDataImpl.findUniqueMethod("next", 0);
        {
            Statement s0 = next.methodBody().statements().getFirst();
            {
                Statement s000 = s0.block().statements().getFirst();
                VariableData vd000 = VariableDataImpl.of(s000);
                VariableInfo vi000This = vd000.variableInfo(thisVar);
                assertEquals("", vi000This.linkedVariables().toString());
                VariableInfo vi000LoopValue = vd000.variableInfo("loopValue");
                assertEquals("*-4-0:loop", vi000LoopValue.linkedVariables().toString());
            }
            {
                Statement s001 = s0.block().statements().get(1);
                VariableData vd001 = VariableDataImpl.of(s001);
                VariableInfo vi001This = vd001.variableInfo(thisVar);
                VariableInfo vi001NextLd = vd001.variableInfo("nextLd");
                assertEquals("Type a.b.X.LoopDataImpl E=this", vi001NextLd.staticValues().toString());

                assertEquals("*M-2-3M:this", vi001NextLd.linkedVariables().toString());
                assertEquals("3M-2-*M:nextLd", vi001This.linkedVariables().toString());
            }
            {
                Statement s002 = s0.block().statements().get(2);
                VariableData vd002 = VariableDataImpl.of(s002);
                VariableInfo vi002This = vd002.variableInfo(thisVar);
                VariableInfo vi002NextLd = vd002.variableInfo("nextLd");
                VariableInfo vi002Body = vd002.variableInfo(bodyFr);
                assertEquals("Type a.b.X.LoopDataImpl E=this", vi002NextLd.staticValues().toString());

                assertEquals("*M-4-0;1M:body, *M-2-3M:this", vi002NextLd.linkedVariables().toString());
                assertEquals("3-4-0;1:body, 3M-2-*M:nextLd", vi002This.linkedVariables().toString());
                assertEquals("0;1M-4-*M:nextLd, 0;1-4-3:this", vi002Body.linkedVariables().toString());
            }
            {
                VariableData vd0 = VariableDataImpl.of(s0);
                VariableInfo vi0This = vd0.variableInfo(thisVar);
                assertEquals("3-4-0;1:body", vi0This.linkedVariables().toString());

                VariableInfo vi0Body = vd0.variableInfo(bodyFr);
                assertEquals("0;1-4-3:this", vi0Body.linkedVariables().toString());

                assertFalse(vd0.isKnown("nextLd"));
            }
        }
    }
}
