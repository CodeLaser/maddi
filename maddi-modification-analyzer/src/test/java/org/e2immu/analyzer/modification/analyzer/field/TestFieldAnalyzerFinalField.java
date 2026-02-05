package org.e2immu.analyzer.modification.analyzer.field;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestFieldAnalyzerFinalField extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Set;
            class B<C> {
                private final Set<C> set;
                B(Set<C> set) {
                    this.set = set;
                }
                public Set<C> getSet() {
                    return set;
                }
            }
            """;

    @DisplayName("constructor and getter")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        FieldInfo set = B.getFieldByName("set", true);
        LinksImpl links = set.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class);
        assertEquals("this.set←0:set,this.set.§m≡0:set.§m,this.set→getSet", links.toString());
        assertTrue(set.analysis().getOrDefault(PropertyImpl.INDEPENDENT_FIELD, ValueImpl.IndependentImpl.DEPENDENT)
                .isDependent());
        assertTrue(set.isUnmodified());
        assertTrue(set.isFinal());
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            class B<C> {
                private final Set<C> set;
                B(Set<C> set) {
                    this.set = set;
                }
                public Set<C> getSet() {
                    return set;
                }
                public void add(C c) {
                    set.add(c);
                }
            }
            """;

    @DisplayName("constructor, getter, add")
    @Test
    public void test2() {
        TypeInfo B = javaInspector.parse(INPUT2);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        FieldInfo set = B.getFieldByName("set", true);
        Links fieldLinks = set.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class);
        assertEquals("this.set←0:set,this.set.§m≡0:set.§m,this.set→getSet,this.set.§cs∋0:c", fieldLinks.toString());
        assertTrue(set.analysis().getOrDefault(PropertyImpl.INDEPENDENT_FIELD, ValueImpl.IndependentImpl.DEPENDENT)
                .isDependent());
        assertTrue(set.isModified());
        assertTrue(set.isFinal());

        MethodInfo constructor = B.findConstructor(1);
        ParameterInfo pi = constructor.parameters().getFirst();
        assertTrue(pi.isModified()); // via add!
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.HashSet;
            import java.util.Set;
            class B<C> {
                private final Set<C> set;
                B(Set<C> set) {
                    this.set = new HashSet<>(set);
                }
                public Set<C> getSet() {
                    return set;
                }
                public void add(C c) {
                    set.add(c);
                }
            }
            """;

    @DisplayName("constructor copy, getter, add")
    @Test
    public void test3() {
        TypeInfo B = javaInspector.parse(INPUT3);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        FieldInfo set = B.getFieldByName("set", true);
        Links fieldLinks = set.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class);
        assertEquals("this.set.§cs⊆0:set.§cs,this.set→getSet,this.set.§cs∋0:c", fieldLinks.toString());
        assertTrue(set.analysis().getOrDefault(PropertyImpl.INDEPENDENT_FIELD, ValueImpl.IndependentImpl.DEPENDENT)
                .isDependent());
        assertTrue(set.isModified()); // via add
        assertTrue(set.isFinal());

        MethodInfo constructor = B.findConstructor(1);
        ParameterInfo pi = constructor.parameters().getFirst();
        assertTrue(pi.isUnmodified()); // a copy of the parameter is made
        assertTrue(pi.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.DEPENDENT)
                .isIndependentHc());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.HashSet;
            import java.util.Set;
            class B {
                public static class M { int i; public void setI(int i) { this.i = i; } }
            
                private final Set<M> set;
            
                B(Set<M> set) {
                    this.set = new HashSet<>(set);
                }
                public Set<M> getSet() {
                    return set;
                }
                public void add(M m) {
                    set.add(m);
                }
            }
            """;

    @DisplayName("constructor copy, getter, add, modifiable")
    @Test
    public void test4() {
        TypeInfo B = javaInspector.parse(INPUT4);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        TypeInfo M = B.findSubType("M");
        assertTrue(M.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE).isMutable());
        assertTrue(M.analysis().getOrDefault(PropertyImpl.INDEPENDENT_TYPE, ValueImpl.IndependentImpl.DEPENDENT).isIndependent());

        FieldInfo set = B.getFieldByName("set", true);
        Links fieldLinks = set.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class);
        assertEquals("this.set.§$s⊆0:set.§$s,this.set→getSet,this.set.§$s∋0:m", fieldLinks.toString());
        assertTrue(set.analysis().getOrDefault(PropertyImpl.INDEPENDENT_FIELD, ValueImpl.IndependentImpl.DEPENDENT)
                .isDependent());
        assertTrue(set.isModified()); // via add
        assertTrue(set.isFinal());

        MethodInfo constructor = B.findConstructor(1);
        ParameterInfo pi = constructor.parameters().getFirst();
        assertTrue(pi.isUnmodified()); // a copy is made
        assertTrue(pi.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.DEPENDENT)
                .isDependent());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            public class X {
                static class M { int i; M(int i) { this.i = i; } void setI(int i) { this.i = i; } }
                static Go callGo(int i) {
                    M m = new M(i);
                    return new Go(m);
                }
                static class Go {
                    private M m;
                    Go(M m) {
                        this.m = m;
                    }
                    void inc() {
                        this.m.i++;
                    }
                }
                static class Go2 {
                    private M m;
                    Go2(M m) {
                        this.m = m;
                    }
                    int get() {
                        return this.m.i;
                    }
                }
            }
            """;

    @DisplayName("does the modification travel via the field?")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        {
            TypeInfo go = X.findSubType("Go");
            MethodInfo constructor = go.findConstructor(1);
            ParameterInfo p0 = constructor.parameters().getFirst();
            assertFalse(p0.isUnmodified());
        }
        {
            TypeInfo go = X.findSubType("Go2");
            MethodInfo constructor = go.findConstructor(1);
            ParameterInfo p0 = constructor.parameters().getFirst();
            assertTrue(p0.isUnmodified());
        }
    }

}
