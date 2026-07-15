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

import org.e2immu.analyzer.modification.common.defaults.ShallowAnalyzer;
import org.e2immu.analyzer.modification.common.getset.ApplyGetSetTranslation;
import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

public class TestStaticValuesRecord extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Set;
            record X(Set<String> set, int n) {}
            """;

    @DisplayName("record")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        FieldInfo setField = X.getFieldByName("set", true);
        FieldReference setFr = runtime.newFieldReference(setField);
        FieldInfo nField = X.getFieldByName("n", true);
        FieldReference nFr = runtime.newFieldReference(nField);

        MethodInfo constructor = X.findConstructor(2);
        MethodLinkedVariables mlvConstructor = constructor.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(constructor));
        ParameterInfo setParam = constructor.parameters().getFirst();
        {
            Statement s0 = realStatements(constructor).getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi0SetField = vd0.variableInfo(setFr);
            assertEquals("this.set←0:set,this.set.§m≡0:set.§m", vi0SetField.linkedVariables().toString());

            VariableInfo vi0SetParam = vd0.variableInfo(setParam);
            assertEquals("0:set→this.set,0:set.§m≡this.set.§m", vi0SetParam.linkedVariables().toString());
        }
        {
            Statement s1 = realStatements(constructor).get(1);
            VariableData vd1 = VariableDataImpl.of(s1);

            VariableInfo vi1SetField = vd1.variableInfo(setFr);
            assertEquals("this.set←0:set,this.set.§m≡0:set.§m", vi1SetField.linkedVariables().toString());
            VariableInfo vi1NField = vd1.variableInfo(nFr);
            assertEquals("this.n←1:n", vi1NField.linkedVariables().toString());
        }
        assertEquals("[0:set→this*.set,0:set.§m≡this*.set.§m, 1:n→this*.n] --> -", mlvConstructor.toString());
        {
            MethodInfo accessorSet = X.findUniqueMethod("set", 0);
            MethodLinkedVariables mlvAccessorSet = accessorSet.analysis().getOrCreate(METHOD_LINKS,
                    () -> tlc.doMethod(accessorSet));
            assertEquals("[] --> set←this.set", mlvAccessorSet.toString());
            Value.FieldValue getSet = accessorSet.analysis().getOrDefault(PropertyImpl.GET_SET_FIELD,
                    ValueImpl.GetSetValueImpl.EMPTY);
            assertEquals(setField, getSet.field());
            VariableData vd = VariableDataImpl.of(accessorSet.methodBody().lastStatement());
            VariableInfo viField = vd.variableInfo(setField.fullyQualifiedName());
            assertEquals("this.set→set", viField.linkedVariables().toString());
        }
        {
            MethodInfo accessorN = X.findUniqueMethod("n", 0);
            MethodLinkedVariables mlvAccessorSet = accessorN.analysis().getOrCreate(METHOD_LINKS,
                    () -> tlc.doMethod(accessorN));
            assertEquals("[] --> n←this.n", mlvAccessorSet.toString());
            Value.FieldValue getSet = accessorN.analysis().getOrDefault(PropertyImpl.GET_SET_FIELD, ValueImpl.GetSetValueImpl.EMPTY);
            assertEquals(nField, getSet.field());
        }
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            class X {
                record R(Set<String> set, int n) {}
                int method(Set<String> in) {
                    R r = new R(in, 3);
                    return r.n;
                }
                R method2(Set<String> in) {
                    R r = new R(in, 3);
                    return r;
                }
            }
            """;

    @DisplayName("values in record")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        TypeInfo R = X.findSubType("R");
        FieldInfo RsetField = R.getFieldByName("set", true);
        MethodInfo Rset = R.findUniqueMethod("set", 0);
        assertSame(RsetField, Rset.getSetField().field());
        assertFalse(R.isExtensible());

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        {
            MethodInfo method = X.findUniqueMethod("method", 1);
            MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
            LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
            LocalVariable r = rLvc.localVariable();
            VariableData vd0 = VariableDataImpl.of(rLvc);
            VariableInfo rVi0 = vd0.variableInfo(r);
            assertEquals("r.n←$_ce1,r.set←0:in,r.set.§m≡0:in.§m", rVi0.linkedVariables().toString());
            assertEquals("[-] --> -", mlv.toString());
        }
        {
            MethodInfo method = X.findUniqueMethod("method2", 1);
            MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
            LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
            LocalVariable r = rLvc.localVariable();
            VariableData vd0 = VariableDataImpl.of(rLvc);
            VariableInfo rVi0 = vd0.variableInfo(r);
            assertEquals("r.n←$_ce3,r.set←0:in,r.set.§m≡0:in.§m", rVi0.linkedVariables().toString());
            assertEquals("[-] --> method2.n←$_ce3,method2.set←0:in,method2.set.§m≡0:in.§m", mlv.toString());
        }
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.Set;
            class X {
                record R(Set<String> set, int n) {}
                int method(Set<String> in, int k) {
                    R r = new R(in, k);
                    R s = r;
                    return s.n;
                }
            }
            """;

    @DisplayName("values in record, extra indirection")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT3);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = X.findUniqueMethod("method", 2);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
        LocalVariable r = rLvc.localVariable();
        VariableData vd0 = VariableDataImpl.of(rLvc);
        VariableInfo rVi0 = vd0.variableInfo(r);
        assertEquals("r.n←1:k,r.set←0:in,r.set.§m≡0:in.§m", rVi0.linkedVariables().toString());

        LocalVariableCreation sLvc = (LocalVariableCreation) method.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(sLvc);
        VariableInfo sVi1 = vd1.variableInfo("s");
        assertEquals("s.n←r.n,s.set←r.set,s.set.§m≡r.set.§m,s.set.§m≡0:in.§m,s←r", sVi1.linkedVariables().toString());

        assertEquals("[-, -] --> -", mlv.toString());
    }


    @Language("java")
    private static final String INPUT3B = """
            package a.b;
            import java.util.Set;
            class X {
                record R<T>(Set<T> set, int n) {}
                static <T> int method(Set<T> in) {
                    R<T> r = new R<>(in, 3);
                    R<T> s = r;
                    return s.n;
                }
            }
            """;

    @DisplayName("values in record, extra indirection; type parameter")
    @Test
    public void test3b() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT3B);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
        LocalVariable r = rLvc.localVariable();

        VariableData vd0 = VariableDataImpl.of(rLvc);
        VariableInfo rVi0 = vd0.variableInfo(r);
        assertEquals("r.n←$_ce1,r.set←0:in,r.set.§m≡0:in.§m", rVi0.linkedVariables().toString());

        LocalVariableCreation sLvc = (LocalVariableCreation) method.methodBody().statements().get(1);
        VariableData vd1 = VariableDataImpl.of(sLvc);
        VariableInfo sVi1 = vd1.variableInfo("s");
        assertEquals("s.n←r.n,s.set←r.set,s.set.§m≡r.set.§m,s.set.§m≡0:in.§m,s←r",
                sVi1.linkedVariables().toString());

        assertEquals("[-] --> -", mlv.toString());
        // anything related to the constant 3 is lost
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.Set;
            class X {
                record R<T>(T t) {}
                Set<String> method(Set<String> in) {
                    R<Set<String>> r = new R<>(in);
                    return r.t();
                }
            }
            """;

    @DisplayName("values in record, @Identity, accessor")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT4);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
        LocalVariable r = rLvc.localVariable();

        VariableData vd0 = VariableDataImpl.of(rLvc);
        VariableInfo rVi0 = vd0.variableInfo(r);

        // IMPORTANT: R is not an abstract type, it is very concrete and can be analyzed!
        // but, because 't' is an unbound type parameter, it can gain virtual fields
        assertEquals("r.t←0:in", rVi0.linkedVariables().toString());

        assertEquals("[-] --> method←0:in", mlv.toString());
        ReturnStatement rs = (ReturnStatement) method.methodBody().statements().get(1);

        // TODO after TypeModIndyAnalyzer, we should have:
        // @Identity method, we return the first parameter
        //assertSame(TRUE, method.analysis().getOrDefault(PropertyImpl.IDENTITY_METHOD, FALSE));
        //assertSame(FALSE, method.analysis().getOrDefault(PropertyImpl.FLUENT_METHOD, FALSE));
    }


    @Language("java")
    private static final String INPUT4b = """
                        package a.b;
                        import org.e2immu.annotation.NotModified;
            
            import java.util.Set;
                        class X {
                            interface R<T> {
                                @NotModified T t();
                                R<T> embed(T t); // modifying, dependent
                            }
                            Set<String> method(Set<String> in, R<Set<String>> rr) {
                                R<Set<String>> r = rr.embed(in);
                                return r.t();
                            }
                        }
            """;

    @DisplayName("values in record, embed in abstract type")
    @Test
    public void test4b() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT4b);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(runtime, Element::annotations, false);
        shallowAnalyzer.go(List.of(X));

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        TypeInfo R = X.findSubType("R");
        assertEquals("§m - T §t", vfc.compute(R).toString());
        MethodInfo Rt = R.findUniqueMethod("t", 0);
        assertFalse(Rt.isModifying());

        MethodInfo embed = R.findUniqueMethod("embed", 1);
        assertTrue(embed.isModifying());
        assertSame(ValueImpl.IndependentImpl.DEPENDENT, embed.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER,
                ValueImpl.IndependentImpl.DEPENDENT));
        MethodLinkedVariables mlvEmbed = embed.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(embed));
        assertEquals("[0:t→this*.§t] --> embed.§t←this*.§t,embed.§m≡this*.§m", mlvEmbed.toString());

        MethodInfo method = X.findUniqueMethod("method", 2);
        ParameterInfo rr = method.parameters().getLast();
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
        LocalVariable r = rLvc.localVariable();
        VariableData vd0 = VariableDataImpl.of(rLvc);
        VariableInfo rVi0 = vd0.variableInfo(r);
        assertEquals("""
                r.§$s←1:rr.§$s,r.§$s⊇0:in.§$s,r.§m≡1:rr.§m,r.§m→0:in.§m\
                """, rVi0.linkedVariables().toString());
        assertFalse(rVi0.isModified());
        VariableInfo rrVi0 = vd0.variableInfo(rr);
        assertTrue(rrVi0.isModified());

        VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().getLast());
        VariableInfo rVi1 = vd1.variableInfo(r);
        assertEquals("""
                r.§$s←1:rr.§$s,r.§$s⊇method.§$s,r.§$s⊇0:in.§$s,r.§m≡1:rr.§m,r.§m→method.§m,r.§m→0:in.§m\
                """, rVi1.linkedVariables().toString());
        assertFalse(rVi1.isModified()); // cannot be modified, because it is newly created
        VariableInfo rrVi1 = vd1.variableInfo(rr);
        assertTrue(rrVi1.isModified());

        // vs the old engine: + method.§$s⊆1:rr*.§$s (the old re-flip on previouslyModified destroyed this
        // same-statement containment; sv keeps it — precision gain) and + method.§$s∩0:in (face variant)
        assertEquals("""
                [0:in.§$s⊆1:rr*.§$s,0:in.§m←1:rr*.§m, 1:rr*.§$s⊇0:in.§$s,1:rr*.§m→0:in.§m] --> \
                method∩0:in.§$s,method.§$s⊆1:rr*.§$s,method.§$s∩0:in,method.§m←1:rr*.§m,method.§m≡0:in.§m\
                """, mlv.toString());
    }


    @Language("java")
    private static final String INPUT4c = """
            package a.b;
            import org.e2immu.annotation.Independent;import org.e2immu.annotation.NotModified;
            import java.util.Set;
            class X {
                interface R<T> { @NotModified T t(); @NotModified R<T> embed(@Independent(hcReturnValue = true) T t); }
                Set<String> method(Set<String> in, R<Set<String>> rr) {
                    R<Set<String>> r = rr.embed(in);
                    return r.t();
                }
            }
            """;

    @DisplayName("values in record, embed in abstract type, now embed() @NotModified")
    @Test
    public void test4c() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT4c);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(runtime, Element::annotations, false);
        shallowAnalyzer.go(List.of(X));

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        TypeInfo R = X.findSubType("R");
        assertEquals("§m - T §t", vfc.compute(R).toString());
        MethodInfo Rt = R.findUniqueMethod("t", 0);
        assertFalse(Rt.isModifying());

        MethodInfo embed = R.findUniqueMethod("embed", 1);
        assertFalse(embed.isModifying());
        assertSame(ValueImpl.IndependentImpl.DEPENDENT, embed.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER,
                ValueImpl.IndependentImpl.DEPENDENT));
        MethodLinkedVariables mlvEmbed = embed.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(embed));

        assertEquals("[-] --> embed.§t←this.§t,embed.§m≡this.§m,embed.§t←0:t", mlvEmbed.toString());

        MethodInfo method = X.findUniqueMethod("method", 2);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
        LocalVariable r = rLvc.localVariable();
        VariableData vd0 = VariableDataImpl.of(rLvc);
        VariableInfo rVi0 = vd0.variableInfo(r);
        assertEquals("""
                r.§$s←1:rr.§$s,r.§$s←0:in,r.§m≡1:rr.§m\
                """, rVi0.linkedVariables().toString());
        assertFalse(rVi0.isModified());

        VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().getLast());
        VariableInfo rVi1 = vd1.variableInfo(r);
        assertEquals("""
                r.§$s←1:rr.§$s,r.§$s←0:in,r.§$s⊇method.§$s,r.§m≡1:rr.§m,r.§m→method.§m\
                """, rVi1.linkedVariables().toString());
        assertFalse(rVi1.isModified());

        assertEquals("""
                [0:in~1:rr.§$s, 1:rr.§$s~0:in] --> method.§$s⊆1:rr.§$s,method.§$s⊆0:in,method.§m←1:rr.§m\
                """, mlv.toString());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.Set;
            import java.util.List;
            class X {
                record R(Set<String> set, List<Integer> list, int i) {}
                static class Builder {
                    Set<String> stringSet;
                    List<Integer> intList;
                    int j;
                    Builder setStringSet(Set<String> set) { stringSet = set; return this; }
                    Builder setIntList(List<Integer>list) { intList = list; return this; }
                    Builder setJ(int k) { j = k; return this; }
                    R build() { return new R(stringSet, intList, j); }
                }
                R method(Set<String> in) {
                    Builder b = new Builder().setJ(3).setIntList(List.of(0, 1)).setStringSet(in);
                    R r = b.build();
                    return r;
                }
            }
            """;

    @DisplayName("simple builder for record")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT5);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        LocalVariableCreation lvc0 = (LocalVariableCreation) method.methodBody().statements().getFirst();
        ApplyGetSetTranslation tm = new ApplyGetSetTranslation(runtime);
        assertEquals("""
                new Builder().j=3,new Builder().intList=List.of(0,1),new Builder().stringSet=in,new Builder()\
                """, lvc0.localVariable().assignmentExpression().translate(tm).toString());
        // switch to more correct method descriptors (openjdk)
        // assertEquals("""
        //         [-] --> method.i←$_ce1,method.list.§$s∋$_ce3,method.list.§$s∋$_ce4,method.set.§m≡0:in*.§m,\
        //         method.set←0:in*\
        //         """, mlv.toString());
        assertEquals("""
                [-] --> method.i←$_ce1,method.set←0:in*,method.list.§$s∋$_ce3,method.list.§$s∋$_ce4,method.set.§m≡0:in*.§m\
                """, mlv.toString());
        // NOTE: in* because of delay, in is linked to field

        TypeInfo R = X.findSubType("R");
        MethodInfo constructorR = R.findConstructor(3);

        MethodLinkedVariables mlvConstructorR = constructorR.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(constructorR));
        assertEquals("""
                [0:set→this*.set,0:set.§m≡this*.set.§m, 1:list→this*.list,1:list.§m≡this*.list.§m, 2:i→this*.i] --> -\
                """, mlvConstructorR.toString());

        LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().get(1);
        LocalVariable r = rLvc.localVariable();
        VariableData vd1 = VariableDataImpl.of(rLvc);
        VariableInfo rVi1 = vd1.variableInfo(r);
        // vs the old engine: all facts retained, plus the finer r.i←$_ce1 and cross-spelling spine/containment
        // extras from the one-slot-one-group merge (gate NOSIBFACE)
        assertEquals("""
                        r.i←$_ce1,r.i←b.j,r.list←b.intList,r.list≻b.intList.§$s,r.set≡b.stringSet,r.set←b.stringSet,r.list.§$s∋$_ce3,r.list.§$s∋$_ce4,r.list.§$s≺b.intList,r.list.§$s≺b,r.list.§$s←b.intList.§$s,r.list.§m≡b.intList.§m,r.set.§m≡b.stringSet.§m,r.set.§m≡0:in.§m,r≈b,r≻b.intList.§$s\
                        """,
                rVi1.linkedVariables().toString());

        // switch to more correct method descriptors (openjdk)
        //assertEquals("""
        //        [-] --> method.i←$_ce1,method.list.§$s∋$_ce3,method.list.§$s∋$_ce4,method.set.§m≡0:in*.§m,method.set←0:in*\
        //        """, mlv.toString());
        assertEquals("""
                [-] --> method.i←$_ce1,method.set←0:in*,method.list.§$s∋$_ce3,method.list.§$s∋$_ce4,method.set.§m≡0:in*.§m\
                """, mlv.toString());
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import java.util.Set;
            import java.util.List;import java.util.function.Function;
            class X {
                record R(Function<String,Integer> function, Object[] variables) {}
                static class Builder {
                    Function<String,Integer> function;
                    Object[] variables;
                    Builder setFunction(Function<String, Integer> f) { function = f; return this; }
                    Builder setVariable(int pos, Object value) { variables[pos]=value; return this; }
                    R build() { return new R(function, variables); }
                }
                Function<String, Integer> method(Set<String> in) {
                    Builder b = new Builder().setFunction(String::length).setVariable(0, "a");
                    R r = b.build();
                    return r.function;
                }
                Function<String, Integer> method2(Set<String> in) {
                    Builder b = new Builder().setFunction(String::length).setVariable(0, "a");
                    R r = b.build();
                    return r.function();
                }
                Object[] method3(String in) {
                    Builder b = new Builder().setFunction(String::length).setVariable(0, in);
                    R r = b.build();
                    return r.variables();
                }
                String method4(String t) {
                    Builder b = new Builder().setFunction(String::length).setVariable(1, t);
                    R r = b.build();
                    return (String)r.variables[1];
                }
                String method5(String t) {
                    R r = new Builder().setFunction(String::length).setVariable(1, t).build();
                    return (String)r.variables[1];
                }
            }
            """;

    @DisplayName("more complex builder for record: indexed objects")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT6);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        TypeInfo R = X.findSubType("R");
        MethodInfo constructorR = R.findConstructor(2);
        MethodLinkedVariables mlvCR = constructorR.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(constructorR));
        assertEquals("[0:function→Λthis*.function, 1:variables→this*.variables,1:variables.§m≡this*.variables.§m] --> -",
                mlvCR.toString());
        assertEquals("this", mlvCR.sortedModifiedString());

        TypeInfo builder = X.findSubType("Builder");

        MethodInfo build = builder.findUniqueMethod("build", 0);
        MethodLinkedVariables mlvBuild = build.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(build));

        assertEquals("[] --> build.function←Λthis.function,build.variables←this.variables,build.variables.§m≡this.variables.§m",
                mlvBuild.toString());

        MethodInfo setVariable = builder.findUniqueMethod("setVariable", 2);
        MethodLinkedVariables mlvSetVariable = setVariable.analysis().getOrCreate(METHOD_LINKS,
                () -> tlc.doMethod(setVariable));
        assertEquals("""
                [-, 1:value∈this.variables*,1:value→this.variables*[0:pos]] --> setVariable←this*,\
                setVariable.variables∋1:value,setVariable.variables←this.variables*,\
                setVariable.variables∋this.variables*[0:pos],setVariable.variables[0:pos]←1:value,\
                setVariable.variables[0:pos]∈this.variables*,setVariable.variables[0:pos]∈setVariable.variables,\
                setVariable.variables[0:pos]←this.variables*[0:pos],setVariable.variables.§m≡this.variables*.§m\
                """, mlvSetVariable.toString());

        Value.FieldValue fv = setVariable.getSetField();
        assertTrue(fv.setter());
        assertEquals(0, fv.parameterIndexOfIndex());
        assertEquals("a.b.X.Builder.variables", fv.field().toString());

        MethodInfo method3 = X.findUniqueMethod("method3", 1);
        MethodLinkedVariables mlvMethod3 = method3.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method3));
        {
            LocalVariableCreation bLvc = (LocalVariableCreation) method3.methodBody().statements().getFirst();
            LocalVariable b = bLvc.localVariable();
            VariableData vd0 = VariableDataImpl.of(bLvc);
            VariableInfo bVi0 = vd0.variableInfo(b);
            // code of ExpressionAnalyzer.methodCallStaticValue
            assertEquals("b.function←Λ$_fi2,b.variables∋0:in,b.variables[0]∈b.variables,b.variables[0]←0:in,b.variables.§m≡b.variables.§m",
                    bVi0.linkedVariables().toString());
        }
        {
            LocalVariableCreation rLvc = (LocalVariableCreation) method3.methodBody().statements().get(1);
            LocalVariable r = rLvc.localVariable();
            VariableData vd1 = VariableDataImpl.of(rLvc);
            VariableInfo rVi1 = vd1.variableInfo(r);
            // code of ExpressionAnalyzer.checkCaseForBuilder
            assertEquals("""
                    r.function←Λ$_fi2,r.function←Λb.function,r.variables~b.variables,r.variables←b.variables,r.variables→b.variables,r.variables∋b.variables[0],r.variables[0]≡b.variables[0],r.variables[0]←b.variables[0],r.variables[0]∈r.variables,r.variables.§m≡b.variables.§m,r.variables[0].§m≡r.variables.§m,r≈b\
                    """, rVi1.linkedVariables().toString());
        }
        assertEquals("[-] --> method3∋0:in*,method3[0]←0:in*", mlvMethod3.toString());

        MethodInfo method2 = X.findUniqueMethod("method2", 1);
        MethodLinkedVariables mlvMethod2 = method2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method2));
        {
            Statement s2 = method2.methodBody().statements().get(2);
            VariableData v2 = VariableDataImpl.of(s2);
            VariableInfo vi2Rv = v2.variableInfo(method2.fullyQualifiedName());
            assertEquals("method2←Λ$_fi8", vi2Rv.linkedVariables().toString());
        }
        // the returned r.function() IS the stored String::length — the old engine lost this (empty summary);
        // recovered by the one-slot-one-group merge (NOSIBFACE)
        assertEquals("[-] --> method2←Λ$_fi8", mlvMethod2.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlvMethod = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        {
            Statement s2 = method.methodBody().statements().get(2);
            VariableData v2 = VariableDataImpl.of(s2);
            VariableInfo vi2Rv = v2.variableInfo(method.fullyQualifiedName());
            assertEquals("method←Λ$_fi15", vi2Rv.linkedVariables().toString());
        }
        // same as method2: the returned r.function IS the stored String::length (NOSIBFACE recovery)
        assertEquals("[-] --> method←Λ$_fi15", mlvMethod.toString());

        MethodInfo method4 = X.findUniqueMethod("method4", 1);
        MethodLinkedVariables mlvMethod4 = method4.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method4));
        {
            Statement s0 = method4.methodBody().statements().getFirst();
            VariableInfo vi0b = VariableDataImpl.of(s0).variableInfo("b");
            assertEquals("b.function←Λ$_fi22,b.variables∋0:t,b.variables[1]∈b.variables,b.variables[1]←0:t,b.variables.§m≡b.variables.§m",
                    vi0b.linkedVariables().toString());
            Statement s1 = method4.methodBody().statements().get(1);
            VariableInfo vi1r = VariableDataImpl.of(s1).variableInfo("r");
            assertEquals("""
                    r.function←Λ$_fi22,r.function←Λb.function,r.variables~b.variables,r.variables←b.variables,r.variables→b.variables,r.variables∋b.variables[1],r.variables[1]≡b.variables[1],r.variables[1]←b.variables[1],r.variables[1]∈r.variables,r.variables.§m≡b.variables.§m,r.variables[1].§m≡r.variables.§m,r≈b\
                    """, vi1r.linkedVariables().toString());
        }
        assertEquals("[-] --> -", mlvMethod4.toString());

        MethodInfo method5 = X.findUniqueMethod("method5", 1);
        MethodLinkedVariables mlvMethod5 = method5.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method5));
        {
            Statement s0 = method5.methodBody().statements().getFirst();
            VariableInfo vi0b = VariableDataImpl.of(s0).variableInfo("r");
            assertEquals("r.function←Λ$_fi29,r.variables∋0:t,r.variables[1]∈r.variables,r.variables[1]←0:t,r.variables[1].§m≡r.variables.§m",
                    vi0b.linkedVariables().toString());
        }
        assertEquals("[-] --> -", mlvMethod5.toString());
    }

    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import org.e2immu.annotation.Independent;
            import org.e2immu.annotation.method.GetSet;
            import java.util.function.Function;
            class X {
                interface R {
                    @GetSet Function<String, Integer> function();
                    @GetSet("variables") Object variable(int i);
                }
                record RI(Function<String,Integer> function, Object[] variables) implements R {
                    public Object variable(int i) { return variables[i]; }
                }
                static class Builder {
                    Function<String,Integer> function;
                    Object[] variables;
                    Builder setFunction(Function<String, Integer> f) { function = f; return this; }
                    Builder setVariable(int pos, Object value) { variables[pos]=value; return this; }
                    R build() { return new RI(function, variables); }
                }
                Function<String, Integer> method(String s) {
                    Builder b = new Builder().setFunction(String::length);
                    b.setVariable(0, s);
                    R r = b.build();
                    return r.function();
                }
                // we see that this is an @Identity method!!
                Object method2(String s) {
                    Builder b = new Builder().setFunction(String::length).setVariable(0, s);
                    R r = b.build();
                    Object o = r.variable(0);
                    return o;
                }
            }
            """;

    @DisplayName("interface in between")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT7);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        TypeInfo R = X.findSubType("RI");
        MethodInfo RConstructor = R.findConstructor(2);
        MethodLinkedVariables mlvRi = RConstructor.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(RConstructor));
        assertEquals("[0:function→Λthis*.function, 1:variables→this*.variables,1:variables.§m≡this*.variables.§m] --> -",
                mlvRi.toString());

        TypeInfo B = X.findSubType("Builder");
        MethodInfo build = B.findUniqueMethod("build", 0);
        MethodLinkedVariables mlvBuild = build.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(build));
        assertEquals("""
                [] --> build.function←Λthis.function,build.variables←this.variables,\
                build.variables.§m≡this.variables.§m\
                """, mlvBuild.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlvMethod = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));
        {
            LocalVariableCreation bLvc = (LocalVariableCreation) method.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(bLvc);
            VariableInfo bVi0 = vd0.variableInfo("b");
            assertEquals("b.function←Λ$_fi2", bVi0.linkedVariables().toString());
        }
        {
            VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().get(1));
            VariableInfo bVi1 = vd1.variableInfo("b");
            assertEquals("b.function←Λ$_fi2,b.variables∋$__sv_variables[0],b.variables[0]∈b.variables,b.variables[0]←0:s",
                    bVi1.linkedVariables().toString());
        }
        {
            LocalVariableCreation rLvc = (LocalVariableCreation) method.methodBody().statements().get(2);
            VariableData vd2 = VariableDataImpl.of(rLvc);
            VariableInfo rVi2 = vd2.variableInfo("r");
            assertEquals("""
                    r.function←Λ$_fi2,r.function←Λb.function,r.function→Λb.function,r.variables∋$__sv_variables[0],r.variables~b.variables,r.variables←b.variables,r.variables∋b.variables[0],r.variables∋0:s,r.variables[0]∈b.variables,r.variables[0]←b.variables[0],r.variables[0]∈r.variables,r.variables[0]←0:s,r.variables.§m≡b.variables.§m,r.variables.§m≡b.variables[0].§m,r.variables.§m≡0:s.§m,r≈b\
                    """, rVi2.linkedVariables().toString());
        }
        // example of the use of VariableTranslationAllowHierarchy
        assertEquals("[-] --> method←Λ$_fi2*", mlvMethod.toString());

        MethodInfo method2 = X.findUniqueMethod("method2", 1);
        MethodInfo variable = R.findUniqueMethod("variable", 1);
        variable.analysis().set(PropertyImpl.INDEPENDENT_METHOD, ValueImpl.IndependentImpl.INDEPENDENT_HC);

        MethodLinkedVariables mlvVariable = variable.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(variable));
        // interpret the @GetSet(string)!!
        assertEquals("[-] --> variable∈this.variables,variable←this.variables[0:i]", mlvVariable.toString());

        MethodLinkedVariables mlvMethod2 = method2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method2));
        {
            VariableData vd0 = VariableDataImpl.of(method2.methodBody().statements().get(1));
            VariableInfo vi0B = vd0.variableInfo("b");
            assertEquals("""
                    b.function→Λr.function,b.variables~r.variables,b.variables→r.variables,b.variables∋0:s,\
                    b.variables[0]∈b.variables,b.variables[0]∈r.variables,b.variables[0]←0:s,b.variables[0]≤r,b≈r,\
                    b.variables.§m≡b.variables.§m,b.variables.§m≡r.variables.§m\
                    """, vi0B.linkedVariables().toString());
        }
        {
            VariableData v1 = VariableDataImpl.of(method2.methodBody().statements().get(1));
            VariableInfo vi2Rv = v1.variableInfo("r");
            assertEquals("""
                    r.function←Λ$_fi8,r.function←Λb.function,r.variables~b.variables,r.variables←b.variables,r.variables→b.variables,r.variables∋b.variables[0],r.variables[0]≡b.variables[0],r.variables[0]←b.variables[0],r.variables[0]∈r.variables,r.variables.§m≡b.variables.§m,r.variables[0].§m≡r.variables.§m,r≈b\
                    """, vi2Rv.linkedVariables().toString());
        }
        {
            Statement s2 = method2.methodBody().statements().get(2);
            VariableData v2 = VariableDataImpl.of(s2);
            VariableInfo vi2Rv = v2.variableInfo("o");
            assertEquals("""
                    o∈?b.variables,o∈b.variables,o∈r.variables,o←r.variables[0],o≤b,o≤r\
                    """, vi2Rv.linkedVariables().toString());
        }
        assertEquals("[-] --> -", mlvMethod2.toString());
    }

    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            class X {
                record R<T>(Set<T> s, List<T> l) {}
                static <T> void method(T t) {
                    Set<T> set = new HashSet<>();
                    List<T> list = new ArrayList<>();
                    R<T> r = new R<>(set, list);
                    Set<T> set2 = r.s;
                    set2.add(t); // assert that set has been modified, but not list
                }
            }
            """;

    // note: modification of of set2/set vs list in TestModificationBasics,5
    @DisplayName("pack and unpack, with local variables")
    @Test
    public void test8() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT8);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        Statement s2 = method.methodBody().statements().get(2);
        VariableData vd2 = VariableDataImpl.of(s2);

        VariableInfo vi2r = vd2.variableInfo("r");
        assertEquals("r.l←list,r.s←set,r.l.§m≡list.§m,r.s.§m≡set.§m", vi2r.linkedVariables().toString());

        VariableInfo vi2Set = vd2.variableInfo("set");
        assertEquals("set.§m≡r.s.§m,set→r.s", vi2Set.linkedVariables().toString());

        VariableInfo vi2List = vd2.variableInfo("list");
        assertEquals("list.§m≡r.l.§m,list→r.l", vi2List.linkedVariables().toString());

        Statement s4 = method.methodBody().statements().get(4);
        VariableData vd4 = VariableDataImpl.of(s4);

        VariableInfo vi4R = vd4.variableInfo("r");
        assertEquals("""
                r.l←list,r.s≻set.§ts,r.s≻set2.§ts,r.s←set,r.s→set2,r.l.§m≡list.§m,r.s.§m≡set.§m,r.s.§m≡set2.§m,\
                r.s.§ts∋0:t,r.s.§ts←set.§ts,r.s.§ts→set2.§ts,r.s.§ts≺set,r.s.§ts≺set2,r≻set.§ts,r≻set2.§ts\
                """, vi4R.linkedVariables().toString()); // r.s.§m≡set2.§m is redundant
        VariableInfo vi4Set = vd4.variableInfo("set");
        // should never link to 'list'!!
        assertEquals("""
                set.§m≡r.s.§m,set.§ts∋0:t,set.§ts→r.s.§ts,set.§ts≺r,set→r.s\
                """, vi4Set.linkedVariables().toString());

        VariableInfo vi4List = vd4.variableInfo("list");
        assertEquals("list.§m≡r.l.§m,list→r.l", vi4List.linkedVariables().toString());

        assertEquals("[-] --> -", mlv.toString());
    }

    @Language("java")
    private static final String INPUT9 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            class X {
                record R<T>(Set<T> s, List<T> l) {}
                static <T> void method(Set<T> set, List<T> list, T t) {
                    R<T> r = new R<>(set, list);
                    Set<T> set2 = r.s;
                    set2.add(t); // assert that set has been modified, but not list
                }
            }
            """;

    @DisplayName("pack and unpack, with parameters")
    @Test
    public void test9() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT9);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodInfo method = X.findUniqueMethod("method", 3);
        MethodLinkedVariables mlv = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        ParameterInfo set = method.parameters().getFirst();
        ParameterInfo list = method.parameters().get(1);
        ParameterInfo t = method.parameters().getLast();

        {
            Statement s0 = method.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);

            VariableInfo vi2r = vd0.variableInfo("r");
            assertEquals("r.l←1:list,r.s←0:set,r.l.§m≡1:list.§m,r.s.§m≡0:set.§m", vi2r.linkedVariables().toString());

            VariableInfo vi2Set = vd0.variableInfo(set);
            assertEquals("0:set→r.s,0:set.§m≡r.s.§m", vi2Set.linkedVariables().toString());
            assertFalse(vi2Set.isModified());

            VariableInfo vi2List = vd0.variableInfo(list);
            assertEquals("1:list→r.l,1:list.§m≡r.l.§m", vi2List.linkedVariables().toString());
            assertFalse(vi2List.isModified());
        }
        {
            Statement s2 = method.methodBody().statements().get(2);
            VariableData vd2 = VariableDataImpl.of(s2);
            assertEquals("""
                    [r, \
                    a.b.X.method(java.util.Set,java.util.List,Object):0:set, \
                    a.b.X.method(java.util.Set,java.util.List,Object):1:list, \
                    set2, \
                    a.b.X.R.s#r, \
                    a.b.X.method(java.util.Set,java.util.List,Object):2:t]\
                    """, vd2.knownVariableNames().toString());
            VariableInfo vi2R = vd2.variableInfo("r");
            assertEquals("""
                    r.l←1:list,r.s←0:set,r.s≻0:set.§ts,r.s≻set2.§ts,r.s→set2,r.l.§m≡1:list.§m,r.s.§m≡0:set.§m,\
                    r.s.§m≡set2.§m,r.s.§ts≺0:set,r.s.§ts∋2:t,r.s.§ts←0:set.§ts,r.s.§ts→set2.§ts,r.s.§ts≺set2,r≻0:set.§ts,\
                    r≻set2.§ts\
                    """, vi2R.linkedVariables().toString());

            VariableInfo vi2Set = vd2.variableInfo(set);
            assertEquals("""
                    0:set→r.s,0:set→set2,0:set.§m≡r.s.§m,0:set.§m≡set2.§m,0:set.§ts∋2:t,0:set.§ts→r.s.§ts,\
                    0:set.§ts→set2.§ts,0:set.§ts≺r\
                    """, vi2Set.linkedVariables().toString());
            // 0:set.§m≡set2.§m is redundant, but because set is a parameter, it is still present
            assertTrue(vi2Set.isModified());

            VariableInfo vi2List = vd2.variableInfo(list);
            assertEquals("1:list→r.l,1:list.§m≡r.l.§m", vi2List.linkedVariables().toString());
            assertFalse(vi2List.isModified());

            VariableInfo vi2Set2 = vd2.variableInfo("set2");
            assertEquals("""
                    set2.§m≡r.s.§m,set2.§ts∋2:t,set2.§ts←r.s.§ts,set2.§ts≺r,set2←r.s\
                    """, vi2Set2.linkedVariables().toString()); // 0:set.§m≡set2.§m is redundant
            assertTrue(vi2Set2.isModified());

            VariableInfo vi2Rs = vd2.variableInfo("a.b.X.R.s#r");
            assertEquals("""
                    r.s←0:set,r.s→set2,r.s.§m≡0:set.§m,r.s.§m≡set2.§m,r.s.§ts∋2:t,r.s.§ts←0:set.§ts,r.s.§ts→set2.§ts\
                    """, vi2Rs.linkedVariables().toString());
            assertTrue(vi2Rs.isModified());

            VariableInfo vi2T = vd2.variableInfo(t);
            assertEquals("2:t∈r.s.§ts,2:t∈0:set.§ts,2:t∈set2.§ts,2:t≤r", vi2T.linkedVariables().toString());
            assertFalse(vi2T.isModified());
        }
        assertEquals("[0:set*.§ts∋2:t, -, 2:t∈0:set*.§ts] --> -", mlv.toString());
    }
}
