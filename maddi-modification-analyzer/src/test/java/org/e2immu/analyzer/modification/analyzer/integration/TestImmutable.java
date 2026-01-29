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
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.junit.jupiter.api.Assertions.*;

public class TestImmutable extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Set;
            class X {
                record R(String s, int i) {}
            
                // hidden content
                record RT<T>(String s, T t) {}
            
                // setter modifies field
                static class M { int i; void setI(int i) { this.i = i; }}
            
                // field is exposed
                static class MF { final M m; MF(M m) { this.m = m; }}
            
                // field is exposed through accessor
                record RM(M m, int i) {}
            }
            """;

    @DisplayName("basics")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(X);
        assertEquals("""
                [a.b.X.<init>(), a.b.X.M.<init>(), a.b.X.M.setI(int), a.b.X.R.<init>(String,int), a.b.X.R.i(), \
                a.b.X.R.s(), a.b.X.RM.i(), a.b.X.RT.<init>(String,T), a.b.X.RT.s(), a.b.X.RT.t(), a.b.X.M.i, a.b.X.R.i, \
                a.b.X.R.s, a.b.X.RT.s, a.b.X.RT.t, a.b.X.M, a.b.X.R, a.b.X.RT, a.b.X.MF.<init>(a.b.X.M), \
                a.b.X.RM.<init>(a.b.X.M,int), a.b.X.RM.m(), a.b.X.MF.m, a.b.X.RM.i, a.b.X.RM.m, a.b.X.MF, a.b.X.RM, a.b.X]\
                """, ao.toString());
        analyzer.go(ao);

        assertTrue(immutable(runtime.stringTypeInfo()).isImmutable());

        TypeInfo R = X.findSubType("R");
        assertTrue(immutable(R).isImmutable());

        TypeInfo RT = X.findSubType("RT");
        assertFalse(immutable(RT).isImmutable());
        assertTrue(immutable(RT).isAtLeastImmutableHC());

        TypeInfo M = X.findSubType("M");
        assertTrue(immutable(M).isMutable());

        TypeInfo MF = X.findSubType("MF");
        assertTrue(immutable(MF).isMutable());

        TypeInfo RM = X.findSubType("RM");
        FieldInfo mField = RM.getFieldByName("m", true);
        assertTrue(mField.analysis().getOrDefault(INDEPENDENT_FIELD, DEPENDENT).isDependent());
        MethodInfo mAccessor = RM.findUniqueMethod("m", 0);
        assertTrue(mAccessor.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT).isDependent());
        assertTrue(independent(RM).isDependent());
        assertTrue(immutable(RM).isMutable());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import org.e2immu.annotation.Modified;
            class X {
                interface I {}
            
                interface J extends Comparable<J> {}
            
                // modifying
                interface K {
                    @Modified
                    void add(String s);
                }
            
                // modifying, because K is, even if we don't have any data for "int get()"
                interface KK extends K {
                    int get();
                }
            
                // we just don't know
                interface N {
                    int get();
                }
            
                // has an implementation
                interface L {
                    int get();
                }
            
                class LI implements L {
                    int get() { return 10; }
                }
            
                //modifying (implicit in abstract void methods)
                interface M extends L {
                    void set(int i);
                }
            
            }
            """;

    @DisplayName("interfaces")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        TypeInfo I = X.findSubType("I");
        assertTrue(immutable(I).isImmutableHC());
        assertTrue(independent(I).isIndependent());

        TypeInfo J = X.findSubType("J");
        assertTrue(immutable(J).isImmutableHC());
        assertTrue(independent(J).isIndependent());

        TypeInfo K = X.findSubType("K");
        MethodInfo add = K.findUniqueMethod("add", 1);
        assertFalse(add.isNonModifying());

        assertTrue(immutable(K).isMutable());
        assertTrue(independent(K).isIndependent());

        TypeInfo KK = X.findSubType("KK");
        assertTrue(immutable(KK).isMutable()); // from extension!

        TypeInfo N = X.findSubType("N");
        assertNull(immutable(N)); // there is no code
        assertTrue(N.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE).isMutable());

        TypeInfo L = X.findSubType("L");
        assertTrue(immutable(L).isImmutableHC());

        TypeInfo M = X.findSubType("M");
        MethodInfo mSet = M.findUniqueMethod("set", 1);
        assertTrue(mSet.isModifying());
        assertNull(immutable(M)); // there is no code, and the modification of 'set' is not recorded
    }

    private static Value.Immutable immutable(TypeInfo r) {
        return r.analysis().getOrNull(IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
    }

    private static Value.Independent independent(TypeInfo r) {
        return r.analysis().getOrNull(INDEPENDENT_TYPE, ValueImpl.IndependentImpl.class);
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.Set;
            class X {
                record Pair<F, G>(F f, G g) {
                }
            
                record R<F, G>(Pair<F, G> pair) {
                    public R {
                        assert pair != null;
                    }
                }
            }
            """;

    @DisplayName("type parameters in records")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        TypeInfo Pair = X.findSubType("Pair");
        Value.Immutable immutablePair = immutable(Pair);
        assertTrue(immutablePair.isImmutableHC(), "Have " + immutablePair);

        TypeInfo R = X.findSubType("R");
        MethodInfo pair = R.findUniqueMethod("pair", 0);
        assertNotNull(pair.getSetField().field());
        Value.Independent pairIndependent = pair.analysis().getOrDefault(PropertyImpl.INDEPENDENT_METHOD,
                DEPENDENT);
        assertTrue(pairIndependent.isIndependentHc());

        Value.Immutable immutableR = immutable(R);
        assertTrue(immutableR.isImmutableHC(), "Have " + immutableR);
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            public class X {
                public interface Exit {
                }
            
                public static class Break implements Exit {
                    private final int level;
            
                    public Break(int level) {
                        this.level = level;
                    }
                }
            
                public static class Continue implements Exit {
                    private final int level;
            
                    public Continue(int level) {
                        this.level = level;
                    }
            
                    public int level() {
                        return level;
                    }
                }
            
                public static class Return implements Exit {
                    private final Object value;
            
                    public Return(Object value) {
                        this.value = value;
                    }
            
                    public Object value() {
                        return value;
                    }
                }
            
                public static class ExceptionThrown implements Exit {
                    private final Exception exception;
            
                    public ExceptionThrown(Exception exception) {
                        this.exception = exception;
                    }
            
                    public Exception exception() {
                        return exception;
                    }
                }
            }
            """;

    @DisplayName("only one is @FinalFields")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        List<Info> ao = prepWork(X);
        assertEquals("""
                [a.b.X.<init>(), a.b.X.Break.<init>(int), a.b.X.Continue.<init>(int), a.b.X.Continue.level(), \
                a.b.X.ExceptionThrown.<init>(Exception), a.b.X.ExceptionThrown.exception(), a.b.X.Exit, \
                a.b.X.Return.<init>(Object), a.b.X.Return.value(), a.b.X.Break.level, a.b.X.Continue.level, \
                a.b.X.ExceptionThrown.exception, a.b.X.Return.value, a.b.X.Break, a.b.X.Continue, a.b.X.ExceptionThrown, \
                a.b.X.Return, a.b.X]\
                """, ao.toString());
        analyzer.go(ao);

        TypeInfo Exit = X.findSubType("Exit");
        Value.Immutable immutableExit = immutable(Exit);
        assertTrue(immutableExit.isImmutableHC(), "Have " + immutableExit);
        Value.Independent independentExit = independent(Exit);
        assertTrue(independentExit.isIndependent());

        TypeInfo Continue = X.findSubType("Continue");
        Value.Independent independentContinue = independent(Continue);
        assertTrue(independentContinue.isIndependent());
        Value.Immutable immutableContinue = immutable(Continue);
        assertTrue(immutableContinue.isImmutableHC(), "Have " + immutableContinue);

        TypeInfo Break = X.findSubType("Continue");
        Value.Immutable immutableBreak = immutable(Break);
        assertTrue(immutableBreak.isImmutableHC(), "Have " + immutableBreak);

        TypeInfo Return = X.findSubType("Return");
        Value.Immutable immutableReturn = immutable(Return);
        assertTrue(immutableReturn.isImmutableHC(), "Have " + immutableReturn);

        TypeInfo exceptionThrown = X.findSubType("ExceptionThrown");
        Value.Immutable immutableEt = immutable(exceptionThrown);
        assertTrue(immutableEt.isFinalFields(), "Have " + immutableEt);
    }

}
