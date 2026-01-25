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
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

public class TestModificationFunctional extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Set;
            import java.util.function.Function;
            class X {
                int j;
                int go(String in) {
                    return run(in, this::parse);
                }
                int run(String s, Function<String, Integer> function) {
                    System.out.println("Applying function on "+s);
                    return function.apply(s);
                }
                int parse(String t) {
                    j = Integer.parseInt(t);
                    return j;
                }
            }
            """;

    @DisplayName("MR as direct parameter")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo parse = X.findUniqueMethod("parse", 1);
        assertTrue(parse.isModifying());
        MethodLinkedVariables mlvParse = parse.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> parse←this*.j", mlvParse.toString());

        MethodInfo run = X.findUniqueMethod("run", 2);
        assertTrue(run.isNonModifying());
        MethodLinkedVariables mlvRun = run.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-, 1:function*↗$_afi2] --> run←$_afi2,run↖Λ1:function*", mlvRun.toString());

        MethodInfo go = X.findUniqueMethod("go", 1);
        assertTrue(go.isModifying());
        MethodLinkedVariables mlvGo = go.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> go←this*.j", mlvGo.toString());
        assertEquals("""
                $_fi3, System.out, a.b.X.go(String):0:in, \
                a.b.X.run(String,java.util.function.Function<String,Integer>):0:s, \
                a.b.X.run(String,java.util.function.Function<String,Integer>):1:function, \
                this\
                """, mlvGo.sortedModifiedString());
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            import java.util.function.Function;
            class X {
                record R(Function<String, Integer> function) {}
                int j;
            
                int go(String in) {
                    R nr = new R(this::parse);
                    return run(in, nr);
                }
                int run(String s, R r) {
                    System.out.println("Applying function on "+s);
                    return r.function.apply(s);
                }
                int parse(String t) {
                    j = Integer.parseInt(t);
                    return j;
                }
            }
            """;

    @DisplayName("MR encapsulated in record")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo parse = X.findUniqueMethod("parse", 1);
        MethodLinkedVariables mlvParse = parse.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> parse←this*.j", mlvParse.toString());
        assertEquals("this", mlvParse.sortedModifiedString());
        assertTrue(parse.isModifying());

        MethodInfo run = X.findUniqueMethod("run", 2);
        MethodLinkedVariables mlvRun = run.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-, 1:r.function*↗$_afi2] --> run←$_afi2,run↖Λ1:r.function*", mlvRun.toString());
        ParameterInfo runS = run.parameters().get(0);
        ParameterInfo runR = run.parameters().get(1);

        Statement s1 = run.methodBody().lastStatement();
        VariableData vd1 = VariableDataImpl.of(s1);
        VariableInfo vi1R = vd1.variableInfo(runR);
        assertEquals("1:r.function↗$_afi2,1:r.function↗run", vi1R.linkedVariables().toString());

        assertTrue(runS.isUnmodified());
        assertTrue(runR.isModified());
        assertTrue(run.isNonModifying());

        // now test the propagation
        MethodInfo go = X.findUniqueMethod("go", 1);
        VariableData vd0 = VariableDataImpl.of(go.methodBody().statements().getFirst());
        VariableInfo nr0 = vd0.variableInfo("nr");
        assertEquals("nr.function←Λ$_fi4", nr0.linkedVariables().toString());

        assertTrue(go.isModifying());
        ParameterInfo goIn = go.parameters().getFirst();
        assertTrue(goIn.isUnmodified());

        MethodLinkedVariables mlvGo = go.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> go←this*.j", mlvGo.toString());
    }

    @Language("java")
    private static final String INPUT2b = """
            package a.b;
            import java.util.Set;
            import java.util.function.Function;
            class X {
                record R(Function<String, Integer> function) {}
                record S(R r) {}
                int j;
            
                int go(String in) {
                    R r = new R(this::parse);
                    S s = new S(r);
                    return run(in, s);
                }
                int run(String string, S s) {
                    System.out.println("Applying function on "+string);
                    return s.r().function().apply(string);
                }
                int parse(String t) {
                    j = Integer.parseInt(t);
                    return j;
                }
            }
            """;

    @DisplayName("MR encapsulated in 2 records")
    @Test
    public void test2b() {
        TypeInfo X = javaInspector.parse(INPUT2b);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo parse = X.findUniqueMethod("parse", 1);
        assertFalse(parse.isNonModifying());

        MethodInfo run = X.findUniqueMethod("run", 2);
        ParameterInfo runS = run.parameters().get(1);
        {
            Statement s1 = run.methodBody().lastStatement();
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo vi1S = vd1.variableInfo(runS);
            assertEquals("1:s.r.function↗$_afi2,1:s.r.function↗run", vi1S.linkedVariables().toString());
        }
        assertTrue(runS.isModified());
        assertEquals("""
                [-, 1:s.r.function*↗$_afi2] --> run←$_afi2,run↖Λ1:s.r.function*\
                """, run.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());
        assertTrue(run.isNonModifying());
        MethodInfo go = X.findUniqueMethod("go", 1);
        {
            Statement s0 = go.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0R = vd0.variableInfo("r");
            assertEquals("r.function←Λ$_fi4", vi0R.linkedVariables().toString());
        }
        {
            Statement s1 = go.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);

            VariableInfo vi1R = vd1.variableInfo("r");
            assertEquals("r.function→Λs.r.function,r.function←Λ$_fi4,r→s.r",
                    vi1R.linkedVariables().toString());
            VariableInfo vi1S = vd1.variableInfo("s");
            assertEquals("s.r.function←Λr.function,s.r.function←Λ$_fi4,s.r.function≺s.r,s.r←r",
                    vi1S.linkedVariables().toString());
        }
        assertTrue(go.isModifying());
        ParameterInfo goIn = go.parameters().getFirst();
        assertTrue(goIn.isUnmodified());

        MethodLinkedVariables mlvGo = go.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> go←this*.j", mlvGo.toString());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import org.e2immu.annotation.method.GetSet;
            import java.util.Set;
            import java.util.function.Function;
            class X {
                interface R {
                    @GetSet Function<String, Integer> function();
                }
                record RImpl(Function<String, Integer> function) implements R {}
            
                interface S {
                    @GetSet R r();
                }
                record SImpl(R r) implements S {}
            
                int j;
            
                int go(String in) {
                    R r = new RImpl(this::parse);
                    S s = new SImpl(r);
                    return run(in, s);
                }
                int run(String string, S s) {
                    System.out.println("Applying function on "+string);
                    return s.r().function().apply(string);
                }
                int parse(String t) {
                    j = Integer.parseInt(t);
                    return j;
                }
            }
            """;

    @DisplayName("MR encapsulated by interfaces")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        TypeInfo R = X.findSubType("R");
        FieldInfo functionInR = R.getFieldByName("function", true);
        assertTrue(functionInR.isSynthetic());

        TypeInfo SImpl = X.findSubType("SImpl");
        TypeInfo S = X.findSubType("S");
        assertTrue(SImpl.interfacesImplemented().stream().map(ParameterizedType::typeInfo).anyMatch(ti -> ti == S));
        MethodInfo rInSImpl = SImpl.findUniqueMethod("r", 0);
        MethodInfo rInS = S.findUniqueMethod("r", 0);
        assertTrue(rInSImpl.overrides().contains(rInS));

        MethodInfo parse = X.findUniqueMethod("parse", 1);
        assertTrue(parse.isModifying());

        MethodInfo go = X.findUniqueMethod("go", 1);
        {
            Statement s0 = go.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0R = vd0.variableInfo("r");
            assertEquals("r.function←Λ$_fi4", vi0R.linkedVariables().toString());
        }
        {
            Statement s1 = go.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);

            VariableInfo vi1R = vd1.variableInfo("r");
            assertEquals("r.function→Λs.r.function,r.function←Λ$_fi4,r→Λs.r",
                    vi1R.linkedVariables().toString());
            VariableInfo vi1S = vd1.variableInfo("s");
            assertEquals("""
                    s.r.function←Λr.function,s.r.function←Λ$_fi4,s.r.function≺Λs.r,s.r←Λr\
                    """, vi1S.linkedVariables().toString());
        }

        MethodInfo run = X.findUniqueMethod("run", 2);
        ParameterInfo runS = run.parameters().get(1);
        {
            Statement s1 = run.methodBody().lastStatement();
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo vi1S = vd1.variableInfo(runS);
            assertEquals("1:s.r.function↗$_afi2,1:s.r.function↗run", vi1S.linkedVariables().toString());
        }
        assertTrue(runS.isModified());
        assertTrue(run.isNonModifying());
        assertTrue(go.isModifying());
        ParameterInfo goIn = go.parameters().getFirst();
        assertTrue(goIn.isUnmodified());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.Set;
            import java.util.function.Function;
            class X {
                int j;
                int go(String in) {
                    return indirection(in, this::parse);
                }
                int indirection(String s, Function<String, Integer> function) {
                    Function<String, Integer> f = function;
                    return run(s, f);
                }
                int run(String s, Function<String, Integer> function) {
                    System.out.println("Applying function on "+s);
                    return function.apply(s);
                }
                int parse(String t) {
                    j = Integer.parseInt(t);
                    return j;
                }
            }
            """;

    @DisplayName("Method indirection")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo parse = X.findUniqueMethod("parse", 1);
        assertTrue(parse.isModifying());
        MethodLinkedVariables mlvParse = parse.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> parse←this*.j", mlvParse.toString());

        MethodInfo run = X.findUniqueMethod("run", 2);
        assertTrue(run.isNonModifying());
        MethodLinkedVariables mlvRun = run.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-, 1:function*↗$_afi2] --> run←$_afi2,run↖Λ1:function*", mlvRun.toString());

        MethodInfo indirection = X.findUniqueMethod("indirection", 2);
        assertTrue(run.isNonModifying());
        MethodLinkedVariables mlvIndirection = indirection.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-, -] --> indirection←$_afi2", mlvIndirection.toString());
        assertEquals("""
                System.out, a.b.X.indirection(String,java.util.function.Function<String,Integer>):0:s, \
                a.b.X.indirection(String,java.util.function.Function<String,Integer>):1:function, \
                a.b.X.run(String,java.util.function.Function<String,Integer>):0:s, \
                a.b.X.run(String,java.util.function.Function<String,Integer>):1:function\
                """, mlvIndirection.sortedModifiedString());

        MethodInfo go = X.findUniqueMethod("go", 1);
        assertTrue(go.isModifying());
        MethodLinkedVariables mlvGo = go.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> go←this*.j", mlvGo.toString());
        assertEquals("""
                $_fi4, System.out, a.b.X.go(String):0:in, \
                a.b.X.indirection(String,java.util.function.Function<String,Integer>):0:s, \
                a.b.X.indirection(String,java.util.function.Function<String,Integer>):1:function, \
                a.b.X.run(String,java.util.function.Function<String,Integer>):0:s, \
                a.b.X.run(String,java.util.function.Function<String,Integer>):1:function, this\
                """, mlvGo.sortedModifiedString());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            
            import org.e2immu.annotation.Independent;
            import org.e2immu.annotation.Modified;
            import org.e2immu.annotation.NotModified;
            import org.e2immu.annotation.method.GetSet;
            import java.util.HashSet;import java.util.Set;
            
            public class X {
                @FunctionalInterface
                public interface ThrowingFunction {
                    TryData apply(@Independent(hc = true) @Modified TryData o) throws Throwable;
                }
                public interface TryData {
                    @GetSet
                    @NotModified
                    ThrowingFunction throwingFunction();
            
                    void print();
                }
                public TryData run(TryData td) {
                    try {
                        return td.throwingFunction().apply(td);
                    } catch (Throwable e) {
                        throw new UnsupportedOperationException();
                    }
                }
                public static class TryDataImpl implements TryData {
                    private final ThrowingFunction throwingFunction;
                    public TryDataImpl(ThrowingFunction throwingFunction) {
                        this.throwingFunction = throwingFunction;
                    }
                    @Override
                    public ThrowingFunction throwingFunction() {
                        return throwingFunction;
                    }
                    @Override
                    public void print() { }
                    public static class Builder {
                        private ThrowingFunction bodyThrowingFunction;
            
                        public Builder body(ThrowingFunction throwingFunction) {
                            this.bodyThrowingFunction = throwingFunction;
                            return this;
                        }
                        public TryDataImpl build() {
                            return new TryDataImpl(bodyThrowingFunction);
                        }
                    }
                }
                private Set<Integer> someSet = new HashSet<>();
                public void method() {
                    TryData td = new TryDataImpl.Builder().body(this::methodBody).build();
                    run(td);
                }
                private TryData methodBody(TryData tryData) {
                    this.someSet.add(5);
                    return tryData;
                }
            }
            """;

    /*
    tiny changes:
    - make run instance instead of static?
    - make TryData not a functional interface
     */
    @DisplayName("ThrowingFunction: propagation part 1")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo method = X.findUniqueMethod("method", 0);
        MethodInfo methodBody = X.findUniqueMethod("methodBody", 1);
        assertTrue(methodBody.isModifying());
        MethodLinkedVariablesImpl mlvMethodBody = methodBody.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> methodBody←0:tryData", mlvMethodBody.toString());
        assertEquals("this, this.someSet", mlvMethodBody.sortedModifiedString());
        assertTrue(methodBody.isIdentity());

        TypeInfo throwingFunction = X.findSubType("ThrowingFunction");
        assertTrue(throwingFunction.isFunctionalInterface());

        TypeInfo tryData = X.findSubType("TryData");
        assertFalse(tryData.isFunctionalInterface());
        TypeInfo tryDataImpl = X.findSubType("TryDataImpl");
        TypeInfo builder = tryDataImpl.findSubType("Builder");
        MethodInfo build = builder.findUniqueMethod("build", 0);
        assertEquals("""
                [] --> build.throwingFunction←Λthis.bodyThrowingFunction\
                """, build.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());

        MethodInfo builderBody = builder.findUniqueMethod("body", 1);
        assertEquals("""
                [0:throwingFunction→Λthis*.bodyThrowingFunction] --> \
                body.bodyThrowingFunction←Λ0:throwingFunction,body.bodyThrowingFunction←Λthis*.bodyThrowingFunction,body←this*\
                """, builderBody.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viTd = vd0.variableInfo("td");
        // important intermediary step: we know that the lambda is present as a field of 'td'
        assertEquals("td.throwingFunction←Λ$_fi5", viTd.linkedVariables().toString());

        MethodInfo run = X.findUniqueMethod("run", 1);
        // second step: we must acknowledge that a functional interface is being called by "run"
        assertEquals("""
                [0:td.throwingFunction*↗$_afi2] --> run←$_afi2,run↖Λ0:td.throwingFunction*\
                """, run.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());

        // this is the ultimate goal: we know that run(td) calls the apply function, so we know that methodBody
        // is called, so its modification is propagated.
        assertTrue(method.isModifying());

        FieldInfo someSet = X.getFieldByName("someSet", true);
        assertTrue(someSet.isModified());
    }

    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            
            import org.e2immu.annotation.Independent;
            import org.e2immu.annotation.Modified;
            import org.e2immu.annotation.NotModified;
            import org.e2immu.annotation.method.GetSet;
            
            import java.util.HashSet;
            import java.util.Set;
            
            public class X {
                @FunctionalInterface
                public interface ThrowingFunction {
                    TryData apply(@Independent(hc = true) @Modified TryData o) throws Throwable;
                }
                public interface TryData {
                    @GetSet
                    @NotModified
                    ThrowingFunction throwingFunction();
                    @GetSet("variables")
                    @NotModified
                    Object get(int i);
                }
                public static TryData run(TryData td) {
                    try {
                        return td.throwingFunction().apply(td);
                    } catch (Throwable e) {
                        throw new UnsupportedOperationException();
                    }
                }
                public static class TryDataImpl implements TryData {
                    private final ThrowingFunction throwingFunction;
                    private final Object[] variables;
            
                    public TryDataImpl(ThrowingFunction throwingFunction, Object[] variables) {
                        this.throwingFunction = throwingFunction;
                        this.variables = variables;
                    }
                    @Override
                    public ThrowingFunction throwingFunction() {
                        return throwingFunction;
                    }
                    @Override
                    public Object get(int i) {
                        return variables[i];
                    }

                    public static class Builder {
                        private ThrowingFunction bodyThrowingFunction;
                        private final Object[] variables = new Object[10];
            
                        public Builder body(ThrowingFunction throwingFunction) {
                            this.bodyThrowingFunction = throwingFunction;
                            return this;
                        }
                        public Builder set(int pos, Object value) {
                            variables[pos] = value;
                            return this;
                        }
                        public TryDataImpl build() {
                            return new TryDataImpl(bodyThrowingFunction, variables);
                        }
                    }
                }
                private Set<Integer> someSet = new HashSet<>();
                public void method() {
                    TryData td = new TryDataImpl.Builder().set(0, someSet).body(this::methodBody).build();
                    run(td);
                }
                @SuppressWarnings("unchecked")
                private TryData methodBody(TryData tryData) {
                    Set<Integer> set = (Set<Integer>)tryData.get(0);
                    set.add(5);
                    return tryData;
                }
            }
            """;

    @DisplayName("ThrowingFunction: propagation part 2")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo method = X.findUniqueMethod("method", 0);
        MethodInfo methodBody = X.findUniqueMethod("methodBody", 1);
        assertFalse(methodBody.isModifying());
        ParameterInfo methodBody0 = methodBody.parameters().getFirst();
        assertTrue(methodBody0.isModified());
        // FIXME downcast parameter should be present as explanation for modification of methodBody:0:tryData
        //assertEquals("", methodBody0.analysis().getOrNull(PropertyImpl.DOWNCAST_PARAMETER,
        //        ValueImpl.VariableToTypeInfoSetImpl.class).toString());
        MethodLinkedVariablesImpl mlvMethodBody = methodBody.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
       /* assertEquals("""
                [0:tryData.variables*[0]*.§$s∋$_ce3,0:tryData.variables*[0]*.§$s≤0:tryData.variables*,\
                0:tryData.variables*[0]*.§m≤0:tryData.variables*,0:tryData.variables*[0]*∈0:tryData.variables*] \
                --> \
                methodBody.variables[0].§$s←0:tryData.variables*[0]*.§$s,\
                methodBody.variables[0].§$s∋$_ce3,\
                methodBody.variables[0].§$s≤methodBody.variables,\
                methodBody.variables[0].§$s≤0:tryData.variables*,\
                methodBody.variables[0].§m≡0:tryData.variables*[0]*.§m,\
                methodBody.variables[0].§m≤methodBody.variables,\
                methodBody.variables[0].§m≤0:tryData.variables*,\
                methodBody.variables[0]←0:tryData.variables*[0]*,\
                methodBody.variables[0]∈methodBody.variables,\
                methodBody.variables[0]∈0:tryData.variables*,\
                methodBody.variables←0:tryData.variables*,\
                methodBody.variables∋0:tryData.variables*[0]*,\
                methodBody.variables≥0:tryData.variables*[0]*.§$s,\
                methodBody.variables≥0:tryData.variables*[0]*.§m,\
                methodBody←0:tryData*\
                """, mlvMethodBody.toString());*/
        assertEquals("a.b.X.methodBody(a.b.X.TryData):0:tryData, tryData.variables, tryData.variables[0]",
                mlvMethodBody.sortedModifiedString());
        assertTrue(methodBody.isIdentity());

        TypeInfo throwingFunction = X.findSubType("ThrowingFunction");
        assertTrue(throwingFunction.isFunctionalInterface());

        TypeInfo tryData = X.findSubType("TryData");
        assertFalse(tryData.isFunctionalInterface());

        TypeInfo tryDataImpl = X.findSubType("TryDataImpl");
        TypeInfo builder = tryDataImpl.findSubType("Builder");
        MethodInfo build = builder.findUniqueMethod("build", 0);
        assertEquals("""
                [] --> build.throwingFunction←Λthis.bodyThrowingFunction,\
                build.variables.§m≡this.variables.§m,build.variables←this.variables\
                """, build.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viTd = vd0.variableInfo("td");
        // important intermediary step: we know that the lambda is present as a field of 'td'
        // moreover, modifications to td.variables[0] will propagate into this.someSet
        assertEquals("""
                td.throwingFunction←Λ$_fi9,\
                td.variables[0]←this.someSet,\
                td.variables[0].§m≡this.someSet.§m,\
                td.variables[0]∈td.variables,\
                td.variables∋this.someSet\
                """, viTd.linkedVariables().toString());

        MethodInfo run = X.findUniqueMethod("run", 1);
        // second step: we must acknowledge that a functional interface is being called by "run"
        assertEquals("""
                [0:td.throwingFunction*↗$_afi4] --> run←$_afi4,run↖Λ0:td.throwingFunction*\
                """, run.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());

        // this is the ultimate goal: we know that run(td) calls the apply function, so we know that methodBody
        // is called, so its modification is propagated.
        assertTrue(method.isModifying());

        FieldInfo someSet = X.getFieldByName("someSet", true);
        assertTrue(someSet.isModified());
    }


    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            
            import org.e2immu.annotation.Independent;
            import org.e2immu.annotation.Modified;
            import org.e2immu.annotation.NotModified;
            import org.e2immu.annotation.method.GetSet;
            
            import java.util.HashSet;
            import java.util.Set;
            
            public class X {
                @FunctionalInterface
                public interface ThrowingFunction {
                    void apply(@Independent(hc = true) @Modified TryData o) throws Throwable;
                }
                public interface TryData {
                    @GetSet
                    @NotModified
                    ThrowingFunction throwingFunction();
                    @GetSet("variables")
                    @NotModified
                    Object get(int i);
                }
                public static void run(TryData td) {
                    try {
                        td.throwingFunction().apply(td);
                    } catch (Throwable e) {
                        throw new UnsupportedOperationException();
                    }
                }
                public static class TryDataImpl implements TryData {
                    private final ThrowingFunction throwingFunction;
                    private final Object[] variables;
            
                    public TryDataImpl(ThrowingFunction throwingFunction, Object[] variables) {
                        this.throwingFunction = throwingFunction;
                        this.variables = variables;
                    }
                    @Override
                    public ThrowingFunction throwingFunction() {
                        return throwingFunction;
                    }
                    @Override
                    public Object get(int i) {
                        return variables[i];
                    }

                    public static class Builder {
                        private ThrowingFunction bodyThrowingFunction;
                        private final Object[] variables = new Object[10];
            
                        public Builder body(ThrowingFunction throwingFunction) {
                            this.bodyThrowingFunction = throwingFunction;
                            return this;
                        }
                        public Builder set(int pos, Object value) {
                            variables[pos] = value;
                            return this;
                        }
                        public TryDataImpl build() {
                            return new TryDataImpl(bodyThrowingFunction, variables);
                        }
                    }
                }
                private Set<Integer> someSet = new HashSet<>();
                public void method() {
                    TryData td = new TryDataImpl.Builder().set(0, someSet).body(this::methodBody).build();
                    run(td);
                }
                @SuppressWarnings("unchecked")
                private void methodBody(TryData tryData) {
                    Set<Integer> set = (Set<Integer>)tryData.get(0);
                    set.add(5);
                }
            }
            """;

    @DisplayName("ThrowingFunction: propagation part 3, not returning TryData")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo method = X.findUniqueMethod("method", 0);
        MethodInfo methodBody = X.findUniqueMethod("methodBody", 1);
        assertFalse(methodBody.isModifying());
        ParameterInfo methodBody0 = methodBody.parameters().getFirst();
        assertTrue(methodBody0.isModified());

        MethodLinkedVariablesImpl mlvMethodBody = methodBody.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("a.b.X.methodBody(a.b.X.TryData):0:tryData, tryData.variables, tryData.variables[0]",
                mlvMethodBody.sortedModifiedString());

        TypeInfo tryDataImpl = X.findSubType("TryDataImpl");
        TypeInfo builder = tryDataImpl.findSubType("Builder");
        MethodInfo build = builder.findUniqueMethod("build", 0);
        assertEquals("""
                [] --> build.throwingFunction←Λthis.bodyThrowingFunction,\
                build.variables.§m≡this.variables.§m,build.variables←this.variables\
                """, build.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viTd = vd0.variableInfo("td");
        // important intermediary step: we know that the lambda is present as a field of 'td'
        // moreover, modifications to td.variables[0] will propagate into this.someSet
        assertEquals("""
                td.throwingFunction←Λ$_fi9,\
                td.variables[0]←this.someSet,\
                td.variables[0].§m≡this.someSet.§m,\
                td.variables[0]∈td.variables,\
                td.variables∋this.someSet\
                """, viTd.linkedVariables().toString());

        MethodInfo run = X.findUniqueMethod("run", 1);
        ParameterInfo run0 = run.parameters().getFirst();
        VariableData vdRun000 = VariableDataImpl.of(run.methodBody().statements().getFirst().block().statements().getFirst());
        VariableInfo viRun000Td = vdRun000.variableInfo(run0);
        assertEquals("0:td.throwingFunction↗$_afi4", viRun000Td.linkedVariables().toString());

        // second step: we must acknowledge that a functional interface is being called by "run"
        // contrary to test6, there is no return value (run is a void method)
        assertEquals("[0:td.throwingFunction*↗$_afi4] --> -",
                run.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());

        // this is the ultimate goal: we know that run(td) calls the apply function, so we know that methodBody
        // is called, so its modification is propagated.
        assertTrue(method.isModifying());

        FieldInfo someSet = X.getFieldByName("someSet", true);
        assertTrue(someSet.isModified());
    }


    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            
            import org.e2immu.annotation.Independent;
            import org.e2immu.annotation.Modified;
            import org.e2immu.annotation.NotModified;
            import org.e2immu.annotation.method.GetSet;
            
            import java.util.HashSet;
            import java.util.Set;
            
            public class X {
                @FunctionalInterface
                public interface ThrowingFunction {
                    void apply(@Independent(hc = true) @Modified TryData o) throws Throwable;
                }
                public interface TryData {
                    @GetSet
                    @NotModified
                    ThrowingFunction throwingFunction();
                    @GetSet("variables")
                    @NotModified
                    Object get(int i);
                }
                public static void run(String msg, TryData td) {
                    try {
                        System.out.println(msg);
                        td.throwingFunction().apply(td);
                    } catch (Throwable e) {
                        throw new UnsupportedOperationException();
                    }
                }
                public static class TryDataImpl implements TryData {
                    private final ThrowingFunction throwingFunction;
                    private final Object[] variables;
            
                    public TryDataImpl(ThrowingFunction throwingFunction, Object[] variables) {
                        this.throwingFunction = throwingFunction;
                        this.variables = variables;
                    }
                    @Override
                    public ThrowingFunction throwingFunction() {
                        return throwingFunction;
                    }
                    @Override
                    public Object get(int i) {
                        return variables[i];
                    }

                    public static class Builder {
                        private ThrowingFunction bodyThrowingFunction;
                        private final Object[] variables = new Object[10];
            
                        public Builder body(ThrowingFunction throwingFunction) {
                            this.bodyThrowingFunction = throwingFunction;
                            return this;
                        }
                        public Builder set(int pos, Object value) {
                            variables[pos] = value;
                            return this;
                        }
                        public TryDataImpl build() {
                            return new TryDataImpl(bodyThrowingFunction, variables);
                        }
                    }
                }
                private Set<Integer> someSet = new HashSet<>();
                public void method() {
                    TryData td = new TryDataImpl.Builder().set(0, someSet).body(this::methodBody).build();
                    run("go!", td);
                }
                @SuppressWarnings("unchecked")
                private void methodBody(TryData tryData) {
                    Set<Integer> set = (Set<Integer>)tryData.get(0);
                    set.add(5);
                }
            }
            """;

    @DisplayName("ThrowingFunction: propagation part 4, different parameter")
    @Test
    public void test8() {
        TypeInfo X = javaInspector.parse(INPUT8);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo method = X.findUniqueMethod("method", 0);
        MethodInfo methodBody = X.findUniqueMethod("methodBody", 1);
        assertFalse(methodBody.isModifying());
        ParameterInfo methodBody0 = methodBody.parameters().getFirst();
        assertTrue(methodBody0.isModified());

        MethodLinkedVariablesImpl mlvMethodBody = methodBody.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("a.b.X.methodBody(a.b.X.TryData):0:tryData, tryData.variables, tryData.variables[0]",
                mlvMethodBody.sortedModifiedString());

        TypeInfo tryDataImpl = X.findSubType("TryDataImpl");
        TypeInfo builder = tryDataImpl.findSubType("Builder");
        MethodInfo build = builder.findUniqueMethod("build", 0);
        assertEquals("""
                [] --> build.throwingFunction←Λthis.bodyThrowingFunction,\
                build.variables.§m≡this.variables.§m,\
                build.variables←this.variables\
                """, build.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo viTd = vd0.variableInfo("td");
        // important intermediary step: we know that the lambda is present as a field of 'td'
        // moreover, modifications to td.variables[0] will propagate into this.someSet
        assertEquals("""
                td.throwingFunction←Λ$_fi9,\
                td.variables[0]←this.someSet,\
                td.variables[0].§m≡this.someSet.§m,\
                td.variables[0]∈td.variables,\
                td.variables∋this.someSet\
                """, viTd.linkedVariables().toString());

        MethodInfo run = X.findUniqueMethod("run", 2);

        // second step: we must acknowledge that a functional interface is being called by "run"
        // contrary to test6, there is no return value (run is a void method)
        assertEquals("[-, 1:td.throwingFunction*↗$_afi4] --> -",
                run.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());

        // this is the ultimate goal: we know that run(td) calls the apply function, so we know that methodBody
        // is called, so its modification is propagated.
        assertTrue(method.isModifying());

        FieldInfo someSet = X.getFieldByName("someSet", true);
        assertTrue(someSet.isModified());
    }
}