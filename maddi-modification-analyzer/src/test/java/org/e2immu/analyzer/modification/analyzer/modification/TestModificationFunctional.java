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
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
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

}