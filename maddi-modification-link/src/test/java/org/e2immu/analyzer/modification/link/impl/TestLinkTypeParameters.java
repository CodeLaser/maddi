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
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.*;

public class TestLinkTypeParameters extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import org.e2immu.support.SetOnce;
            public class X {
                static class M { int i; int getI() { return i; } void setI(int i) { this.i = i; } }
                static class N { int i; int getI() { return i; } void setI(int i) { this.i = i; } }
            
                record Pair<F, G>(F f, G g) {
                }
            
                record R<F, G>(Pair<F, G> pair) {
                    public R {
                        assert pair != null;
                    }
                }
            
                record R1<F, G>(SetOnce<Pair<F, G>> setOncePair) {
                }
            
                static <X, Y> Pair<X, Y> create0(X x, Y y) {
                    return new Pair<>(x, y);
                }
            
                static <X> Pair<X, M> create1(X x, M m) {
                    return new Pair<>(x, m);
                }
            
                static <X> Pair<X, M> create2(X x, M m) {
                    //noinspection ALL
                    Pair<X, M> p = new Pair<>(x, m);
                    return p;
                }
            
                static Pair<N, M> create3(N n, M m) {
                    //noinspection ALL
                    Pair<N, M> p = new Pair<>(n, m);
                    return p;
                }
            
                static Pair<Integer, M> create4(Integer i, M m) {
                    //noinspection ALL
                    Pair<Integer, M> p = new Pair<>(i, m);
                    return p;
                }
            }
            """;

    @DisplayName("constructing pairs")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        prepAnalyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        MethodInfo create0 = X.findUniqueMethod("create0", 2);
        assertEquals("[-, -] --> create0.f←0:x,create0.g←1:y", lvs(create0));

        MethodInfo create1 = X.findUniqueMethod("create1", 2);
        assertEquals("[-, -] --> create1.f←0:x,create1.g←1:m", lvs(create1));

        MethodInfo create2 = X.findUniqueMethod("create2", 2);
        assertEquals("[-, -] --> create2.f←0:x,create2.g←1:m", lvs(create2));

        MethodInfo create3 = X.findUniqueMethod("create3", 2);
        assertEquals("[-, -] --> create3.f←0:n,create3.g←1:m", lvs(create3));

        MethodInfo create4 = X.findUniqueMethod("create4", 2);
        assertEquals("[-, -] --> create4.f←0:i,create4.g←1:m", lvs(create4));
    }

    private static String lvs(MethodInfo methodInfo) {
        return methodInfo.analysis().getOrDefault(METHOD_LINKS, MethodLinkedVariablesImpl.EMPTY).toString();
    }

    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            import org.e2immu.support.SetOnce;
            public class X {
            
                record Pair<F, G>(F f, G g) {
                }
            
                record R<F, G>(Pair<F, G> pair) {
                    public R {
                        assert pair != null;
                    }
                }
            
                record R1<F, G>(SetOnce<Pair<F, G>> setOncePair) {
                }
            
                static <X, Y> boolean bothNotNull1(Pair<X, Y> pair) {
                    X x = pair.f;
                    Y y = pair.g();
                    return x != null && y != null;
                }
            
                static <X, Y> boolean bothNotNull2(R<X, Y> r) {
                    X x = r.pair.f;
                    Y y = r.pair().g();
                    return x != null && y != null;
                }
            
                static <X, Y> boolean bothNotNull3(R<X, Y> r) {
                    X x = r.pair.f();
                    Y y = r.pair().g;
                    return x != null && y != null;
                }
            
                static <X, Y> Pair<X, Y> copy(Pair<X, Y> pair) {
                    return new Pair<>(pair.f, pair.g);
                }
            
                static <Y> Pair<?, Y> copy2(Pair<?, Y> pair) {
                    return new Pair<>(pair.f, pair.g);
                }
            
                static Pair<?, ?> copy3(Pair<?, ?> pair) {
                    return new Pair<>(pair.f, pair.g);
                }
            
                static Pair copy4(Pair pair) {
                    return new Pair(pair.f, pair.g);
                }
            
                static <X, Y> Pair<Y, X> reverse(Pair<X, Y> pair) {
                    return new Pair<>(pair.g, pair.f);
                }
            
                static <X, Y> Pair<Y, X> reverse2(Pair<X, Y> pair) {
                    return new Pair<>(pair.g(), pair.f());
                }
            
                static <X, Y> Pair<Y, X> reverse3(R<X, Y> r) {
                    return new Pair<>(r.pair.g, r.pair.f);
                }
            
                static <X, Y> R<Y, X> reverse4(R<X, Y> r) {
                    return new R<>(new Pair<>(r.pair.g, r.pair.f));
                }
            
                static <X, Y> R<Y, X> reverse5(R<X, Y> r) {
                    return new R<>(new Pair<>(r.pair().g(), r.pair().f()));
                }
            
                static <X, Y> R<Y, X> reverse6(R<X, Y> r) {
                    Pair<Y, X> yxPair = new Pair<>(r.pair.g, r.pair.f);
                    return new R<>(yxPair);
                }
            
                static <X, Y> R<Y, X> reverse7(R<X, Y> r) {
                    return new R(new Pair(r.pair.g, r.pair.f));
                }
            
                static <X, Y> R<Y, X> reverse8(X x, Y y) {
                    return new R<>(new Pair<>(y, x));
                }
            
                static <X, Y> R<Y, X> reverse9(R<X, Y> r1, R<X, Y> r2) {
                    return new R<>(new Pair<>(r2.pair.g, r1.pair.f));
                }
            
                static <X, Y> R<Y, X> reverse10(R<X, Y> r1, R<X, Y> r2) {
                    return new R<>(new Pair<>(r2.pair().g(), r1.pair().f()));
                }
            }
            """;

    @DisplayName("reverse, and pack in R")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        prepAnalyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        MethodInfo bnn1 = X.findUniqueMethod("bothNotNull1", 1);
        {
            Statement s0 = bnn1.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo vi0X = vd0.variableInfo("x");
            assertEquals("x←0:pair.f", vi0X.linkedVariables().toString());
        }
        {
            Statement s1 = bnn1.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo vi1Y = vd1.variableInfo("y");
            assertEquals("y←0:pair.g", vi1Y.linkedVariables().toString());
        }

        MethodInfo copy = X.findUniqueMethod("copy", 1);
        assertEquals("[-] --> copy.f←0:pair.f,copy.g←0:pair.g", lvs(copy));

        MethodInfo copy2 = X.findUniqueMethod("copy2", 1);
        assertEquals("[-] --> copy2.f←0:pair.f,copy2.g←0:pair.g", lvs(copy2));

        MethodInfo copy3 = X.findUniqueMethod("copy3", 1);
        assertEquals("[-] --> copy3.f←0:pair.f,copy3.g←0:pair.g", lvs(copy3));

        MethodInfo copy4 = X.findUniqueMethod("copy4", 1);
        assertEquals("[-] --> copy4.f←0:pair.f,copy4.g←0:pair.g", lvs(copy4));

        MethodInfo reverse = X.findUniqueMethod("reverse", 1);
        assertEquals("[-] --> reverse.f←0:pair.g,reverse.g←0:pair.f", lvs(reverse));

        MethodInfo reverse2 = X.findUniqueMethod("reverse2", 1);
        assertEquals("[-] --> reverse2.f←0:pair.g,reverse2.g←0:pair.f", lvs(reverse2));

        MethodInfo reverse3 = X.findUniqueMethod("reverse3", 1);
        assertEquals("[-] --> reverse3.f←0:r.pair.g,reverse3.g←0:r.pair.f", lvs(reverse3));
        {
            Statement s0 = reverse3.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);

            // r.pair.f
            VariableInfo vi0RPairF = vd0.variableInfo(
                    "a.b.X.Pair.f#a.b.X.R.pair#a.b.X.reverse3(a.b.X.R<X,Y>):0:r");
            assertEquals("0:r.pair.f→reverse3.g", vi0RPairF.linkedVariables().toString());

            // r.pair.g
            VariableInfo vi0RPairG = vd0.variableInfo(
                    "a.b.X.Pair.g#a.b.X.R.pair#a.b.X.reverse3(a.b.X.R<X,Y>):0:r");
            assertEquals("0:r.pair.g→reverse3.f", vi0RPairG.linkedVariables().toString());

            // r.pair
            VariableInfo vi0Rpair = vd0.variableInfo("a.b.X.R.pair#a.b.X.reverse3(a.b.X.R<X,Y>):0:r");
            assertEquals("0:r.pair.f→reverse3.g,0:r.pair.g→reverse3.f", vi0Rpair.linkedVariables().toString());

            // r
            VariableInfo vi0R = vd0.variableInfo(reverse3.parameters().getFirst());
            assertEquals("0:r.pair.f→reverse3.g,0:r.pair.g→reverse3.f", vi0R.linkedVariables().toString());

            // return variable
            VariableInfo vi0Rv = vd0.variableInfo(reverse3.fullyQualifiedName());
            assertEquals("reverse3←$_v,reverse3.f←0:r.pair.g,reverse3.g←0:r.pair.f",
                    vi0Rv.linkedVariables().toString());

        }

        MethodInfo reverse4 = X.findUniqueMethod("reverse4", 1);
        assertEquals("""
                [-] --> reverse4.pair.f←0:r.pair.g,reverse4.pair.f≺reverse4.pair,\
                reverse4.pair.g←0:r.pair.f,reverse4.pair.g≺reverse4.pair\
                """, lvs(reverse4));
        //"0,1,2M-4-0,1,*M:f, 0,1,2M-4-0,1,*M:g, 0,1,2M-2-0,1,*M|0-*:pair, 0M,1M,2-2-2M,2M,2:r",
        //   assertEquals("2M-4-*M:f, 2M-4-*M:g, 2M-2-*M|0-*:pair", lvs(reverse4, 0));

        MethodInfo reverse5 = X.findUniqueMethod("reverse5", 1);
        assertEquals("""
                [-] --> reverse5.pair.f←0:r.pair.g,reverse5.pair.f≺reverse5.pair,\
                reverse5.pair.g←0:r.pair.f,reverse5.pair.g≺reverse5.pair\
                """, lvs(reverse5));
        //"0,1,2M-4-0,1,*M:f, 0,1,2M-4-0,1,*M:g, 0,1,2M-2-0,1,*M|0-*:pair, 0M,1M,2-2-2M,2M,2:r"
        //    assertEquals("2M-4-*M:f, 2M-4-*M:g, 2M-2-*M|0-*:pair", lvs(reverse5, 0));

        MethodInfo reverse6 = X.findUniqueMethod("reverse6", 1);
        assertEquals("""
                [-] --> reverse6.pair.f←0:r.pair.g,reverse6.pair.f≺reverse6.pair,\
                reverse6.pair.g←0:r.pair.f,reverse6.pair.g≺reverse6.pair\
                """, lvs(reverse6));
        //       "1,2M-4-*,*M:f, 0,2M-4-*,*M:g, 2M-4-*M:pair, 2M-4-*M:r", lvs(reverse6));
        //    assertEquals("2M-4-*M:f, 2M-4-*M:g, 2M-2-*M|0-*:pair", lvs(reverse6, 0));

        MethodInfo reverse7 = X.findUniqueMethod("reverse7", 1);
        assertEquals("""
                [-] --> reverse7.pair.f←0:r.pair.g,reverse7.pair.f≺reverse7.pair,\
                reverse7.pair.g←0:r.pair.f,reverse7.pair.g≺reverse7.pair\
                """, lvs(reverse7));
        //"0,1,2M-4-0,1,*M:f, 0,1,2M-4-0,1,*M:g, 0,1,2M-2-0,1,*M|0-*:pair, 0M,1M,2-2-2M,2M,2:r"
        //    assertEquals("2M-4-*M:f, 2M-4-*M:g, 2M-2-*M|0-*:pair", lvs(reverse7, 0));

        MethodInfo reverse8 = X.findUniqueMethod("reverse8", 2);
        assertEquals("""
                [-, -] --> reverse8.pair.f←1:y,reverse8.pair.f≺reverse8.pair,\
                reverse8.pair.g←0:x,reverse8.pair.g≺reverse8.pair\
                """, lvs(reverse8));
        //        "0,1,2M-4-0,1,*M:x, 0,1,2M-4-0,1,*M:y", );
        //    assertEquals("", lvs(reverse8, 0));

        MethodInfo reverse9 = X.findUniqueMethod("reverse9", 2);
        assertEquals("""
                [-, -] --> reverse9.pair.f←1:r2.pair.g,reverse9.pair.f≺reverse9.pair,\
                reverse9.pair.g←0:r1.pair.f,reverse9.pair.g≺reverse9.pair\
                """, lvs(reverse9));
        //        "0,1,2M-4-0,1,*M:f, 0,1,2M-4-0,1,*M:g, 0,1,2M-4-0,1,*M:pair, 0,1,2M-4-0,1,*M:pair, 0M,1M,2-4-2M,2M,2:r1, 0M,1M,2-4-2M,2M,2:r2",
        //    assertEquals("2M-4-*M:f, 2M-2-*M|0-*:pair", lvs(reverse9, 0));
        //    assertEquals("2M-4-*M:g, 2M-2-*M|0-*:pair", lvs(reverse9, 1));

        MethodInfo reverse10 = X.findUniqueMethod("reverse10", 2);
        assertEquals("""
                [-, -] --> reverse10.pair.f←1:r2.pair.g,reverse10.pair.f≺reverse10.pair,\
                reverse10.pair.g←0:r1.pair.f,reverse10.pair.g≺reverse10.pair\
                """, lvs(reverse10));
        //        "0,1,2M-4-0,1,*M:f, 0,1,2M-4-0,1,*M:g, 0,1,2M-4-0,1,*M:pair, 0,1,2M-4-0,1,*M:pair, 0M,1M,2-4-2M,2M,2:r1, 0M,1M,2-4-2M,2M,2:r2",
        //   assertEquals("2M-4-*M:f, 2M-2-*M|0-*:pair", lvs(reverse10, 0));
    }
}
