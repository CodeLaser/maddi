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

package org.e2immu.analyzer.modification.analyzer.integration;


import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl.LINKS;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestLinkConstructorInMethodCall extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                interface Exit { }
            
                record ExceptionThrown(Exception exception) implements Exit { }
            
                interface LoopData {
                    LoopData withException(Exception e);
                }
            
                static class LoopDataImpl implements LoopData {
                    private Exit exit;
            
                    LoopDataImpl(Exit exit) {
                        this.exit = exit;
                    }
            
                    @Override
                    public LoopData withException(Exception e) {
                        Exit ee = new ExceptionThrown(e);
                        return new LoopDataImpl(ee);
                    }
                }
            }
            """;

    @DisplayName("construction separate from method call")
    @Test
    public void test1() {
        // see TestAnalysisOrder,2 for a test of the analysis order of this code
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        assertEquals("""
                [a.b.X.<init>(), a.b.X.ExceptionThrown.<init>(Exception), a.b.X.ExceptionThrown.exception(), \
                a.b.X.Exit, a.b.X.LoopData.withException(Exception), a.b.X.ExceptionThrown.exception, a.b.X.LoopData, \
                a.b.X.LoopDataImpl.<init>(a.b.X.Exit), a.b.X.ExceptionThrown, a.b.X.LoopDataImpl.exit, \
                a.b.X.LoopDataImpl.withException(Exception), a.b.X.LoopDataImpl, a.b.X]\
                """, analysisOrder.toString());

        TypeInfo loopDataImpl = X.findSubType("LoopDataImpl");
        // test initial value
        assertSame(DEPENDENT, loopDataImpl.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));

        FieldInfo exit = loopDataImpl.getFieldByName("exit", true);
        assertTrue(exit.isPropertyFinal());

        analyzer.go(analysisOrder, 2);

        testImmutable(X);

        TypeInfo exceptionThrown = X.findSubType("ExceptionThrown");
        {
            MethodInfo exceptionAccessor = exceptionThrown.findUniqueMethod("exception", 0);
            VariableData vd = VariableDataImpl.of(exceptionAccessor.methodBody().statements().getFirst());
            assertEquals("""
                    a.b.X.ExceptionThrown.exception, \
                    a.b.X.ExceptionThrown.exception(), \
                    a.b.X.ExceptionThrown.this\
                    """, vd.knownVariableNamesToString());
            VariableInfo viThis = vd.variableInfo("a.b.X.ExceptionThrown.this");
            assertFalse(viThis.isModified());
            MethodLinkedVariablesImpl mlvExAcc = exceptionAccessor.analysis().getOrNull(METHOD_LINKS,
                    MethodLinkedVariablesImpl.class);
            assertEquals("[] --> exception←this.exception", mlvExAcc.toString());
            FieldInfo exception = exceptionThrown.getFieldByName("exception", true);
            assertTrue(exception.isPropertyFinal());

            assertTrue(exceptionThrown.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE).isMutable());
            assertSame(FINAL_FIELDS, exceptionThrown.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));

            assertTrue(exceptionAccessor.isNonModifying());
            assertTrue(exception.isUnmodified());
        }
        {
            {
                MethodInfo constructor = loopDataImpl.findConstructor(1);
                assertEquals("[0:exit→this*.exit] --> -", constructor.analysis().getOrNull(METHOD_LINKS,
                        MethodLinkedVariablesImpl.class).toString());

                ParameterInfo p0 = constructor.parameters().getFirst();
                assertSame(INDEPENDENT_HC, p0.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));
            }
            MethodInfo withException = loopDataImpl.findUniqueMethod("withException", 1);
            assertTrue(withException.isNonModifying());
            assertSame(INDEPENDENT, withException.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));

            {
                Statement s0 = withException.methodBody().statements().getFirst();
                VariableData vd0 = VariableDataImpl.of(s0);
                VariableInfo vi0Ee = vd0.variableInfo("ee");
                assertEquals("ee.exception←0:e", vi0Ee.linkedVariables().toString());
            }
            {
                Statement s1 = withException.methodBody().lastStatement();
                VariableData vd1 = VariableDataImpl.of(s1);
                VariableInfo vi1Rv = vd1.variableInfo(withException.fullyQualifiedName());
                Links links = vi1Rv.linkedVariables();
                assertEquals("""
                        a.b.X.LoopDataImpl.withException(Exception)
                        a.b.X.Exit.exception#a.b.X.LoopData.exit#a.b.X.LoopDataImpl.withException(Exception)
                        $_v
                        a.b.X.LoopDataImpl.withException(Exception):0:e
                        a.b.X.LoopData.exit#a.b.X.LoopDataImpl.withException(Exception)
                        """, Stream.concat(links.stream().map(Link::from), links.stream().map(Link::to))
                        .distinct()
                        .map(Variable::fullyQualifiedName)
                        .collect(Collectors.joining("\n", "", "\n")));
            }
            assertEquals("""
                    [-] --> withException.exit.exception←0:e,\
                    withException.exit.exception≺withException.exit\
                    """, withException.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class).toString());
        }

        testLoopData(X);
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class X {
                interface Exit { }
            
                record ExceptionThrown(Exception exception) implements Exit { }
            
                interface LoopData {
                    LoopData withException(Exception e);
                }
            
                static class LoopDataImpl implements LoopData {
                    private final Exit exit;
            
                    LoopDataImpl(Exit exit) {
                        this.exit = exit;
                    }
            
                    @Override
                    public LoopData withException(Exception e) {
                        return new LoopDataImpl(new ExceptionThrown(e)); // sole difference is here
                    }
                }
            }
            """;

    @DisplayName("construction as argument to method call")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder, 1);
        testImmutable(X);

        TypeInfo loopDataImpl = X.findSubType("LoopDataImpl");
        MethodInfo ldConstructor = loopDataImpl.findConstructor(1);
        MethodLinkedVariables mlvLdCon = ldConstructor.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[0:exit→this*.exit] --> -", mlvLdCon.toString());

        MethodInfo withException = loopDataImpl.findUniqueMethod("withException", 1);
        assertTrue(withException.isNonModifying());
        {
            Statement s0 = withException.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0Rv = vd0.variableInfo(withException.fullyQualifiedName());
            // formal type of variable:
            assertEquals("Type a.b.X.LoopData", vi0Rv.variable().parameterizedType().toString());
            assertEquals("""
                    withException←Λ$_v,\
                    withException.exit.exception←0:e,\
                    withException.exit.exception≺withException.exit\
                    """, vi0Rv.linkedVariables().toString());
        }
        testLoopData(X);
    }

    private void testImmutable(TypeInfo X) {
        TypeInfo exception = javaInspector.compiledTypesManager().get(Exception.class);
        assertTrue(exception.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE).isMutable());

        TypeInfo exit = X.findSubType("Exit");
        assertSame(IMMUTABLE_HC, exit.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(INDEPENDENT, exit.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
    }

    private void testLoopData(TypeInfo X) {
        TypeInfo loopDataImpl = X.findSubType("LoopDataImpl");
        FieldInfo exit = loopDataImpl.getFieldByName("exit", true);
        assertTrue(exit.isUnmodified());
        assertEquals("this.exit←0:exit", exit.analysis().getOrNull(LINKS, LinksImpl.class).toString());

        MethodInfo ldImplWithException = loopDataImpl.findUniqueMethod("withException", 1);
        assertTrue(ldImplWithException.isNonModifying());
        assertSame(INDEPENDENT, loopDataImpl.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT));
        assertSame(IMMUTABLE_HC, loopDataImpl.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));

        // LoopData's properties are computed from LoopDataImpl
        TypeInfo loopData = X.findSubType("LoopData");
        MethodInfo ldWithException = loopData.findUniqueMethod("withException", 1);
        assertFalse(ldWithException.isModifying());
        assertSame(INDEPENDENT, ldWithException.analysis().getOrDefault(PropertyImpl.INDEPENDENT_METHOD, INDEPENDENT_HC));
        assertSame(IMMUTABLE_HC, loopData.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
    }
}
