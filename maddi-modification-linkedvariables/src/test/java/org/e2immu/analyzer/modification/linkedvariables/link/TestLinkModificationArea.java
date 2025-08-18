package org.e2immu.analyzer.modification.linkedvariables.link;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestLinkModificationArea extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Set;
            class X {
                static class M { int i; int get() { return i; } void set(int i) { this.i = i; }}
                record R(M a) {}
                static M getA(R r) {
                    return r.a;
                }
            }
            """;

    @DisplayName("getter")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        TypeInfo M = X.findSubType("M");
        assertTrue(M.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE).isMutable());
        assertTrue(M.isExtensible());
        TypeInfo R = X.findSubType("R");
        HiddenContentTypes hctR = R.analysis().getOrDefault(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("0=M", hctR.detailedSortedTypes());

        MethodInfo getA = X.findUniqueMethod("getA", 1);
        {
            Statement s0 = getA.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo viRv = vd0.variableInfo(getA.fullyQualifiedName());
            assertEquals("E=r.a", viRv.staticValues().toString());
            assertEquals("-1-:a, *M-2-0M|*-0:r", viRv.linkedVariables().toString());
        }
    }

    @Language("java")
    private static final String INPUT1b = """
            package a.b;
            import java.util.Set;
            class X {
                record M(int i) {}
                record R(M a) {}
                static M getA(R r) {
                    return r.a;
                }
            }
            """;

    @DisplayName("getter, immutable, non-extensible variant")
    @Test
    public void test1b() {
        TypeInfo X = javaInspector.parse(INPUT1b);
        List<Info> analysisOrder = prepWork(X);
        // because we do not have an analyzer yet
        TypeInfo M = X.findSubType("M");
        M.analysis().set(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.IMMUTABLE);
        TypeInfo R = X.findSubType("R");
        R.analysis().set(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.IMMUTABLE);
        analyzer.go(analysisOrder);
        assertFalse(M.isExtensible());
        HiddenContentTypes hctR = R.analysis().getOrDefault(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("", hctR.detailedSortedTypes());

        MethodInfo getA = X.findUniqueMethod("getA", 1);
        {
            Statement s0 = getA.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo viRv = vd0.variableInfo(getA.fullyQualifiedName());
            assertEquals("E=r.a", viRv.staticValues().toString());
            assertEquals("-1-:a", viRv.linkedVariables().toString());
        }
    }


    @Language("java")
    private static final String INPUT1c = """
            package a.b;
            import java.util.Set;
            class X {
                static class M<T> { final T t; M(T t) { this.t = t; }}
                record R<T>(M<T> a) {}
                static <T> M<T> getA(R<T> r) {
                    return r.a;
                }
                static <T> T getT(R<T> r) {
                    return r.a.t;
                }
            }
            """;

    @DisplayName("getter, immutable HC, extensible variant")
    @Test
    public void test1c() {
        TypeInfo X = javaInspector.parse(INPUT1c);
        List<Info> analysisOrder = prepWork(X);
        // because we do not have an analyzer yet
        TypeInfo M = X.findSubType("M");
        M.analysis().set(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.IMMUTABLE_HC);
        TypeInfo R = X.findSubType("R");
        R.analysis().set(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.IMMUTABLE_HC);
        analyzer.go(analysisOrder);
        HiddenContentTypes hctR = R.analysis().getOrDefault(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("0=T, 1=M", hctR.detailedSortedTypes());

        MethodInfo getA = X.findUniqueMethod("getA", 1);
        {
            Statement s0 = getA.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo viRv = vd0.variableInfo(getA.fullyQualifiedName());
            assertEquals("E=r.a", viRv.staticValues().toString());
            assertEquals("-1-:a, *-4-1:r", viRv.linkedVariables().toString());
        }
        MethodInfo getT = X.findUniqueMethod("getT", 1);
        {
            Statement s0 = getT.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo viRv = vd0.variableInfo(getT.fullyQualifiedName());
            assertEquals("E=r.a.t", viRv.staticValues().toString());
            // not recursive! we don't need the transitive completion here
            assertEquals("*-4-0:a, *-4-1:r, -1-:t", viRv.linkedVariables().toString());
        }
    }


    @Language("java")
    private static final String INPUT1d = """
            package a.b;
            import java.util.Set;
            class X {
                static final class M { int i; int get() { return i; } void set(int i) { this.i = i; }}
                record R(M a) {}
                static M getA(R r) {
                    return r.a;
                }
            }
            """;

    @DisplayName("getter, mutable, non-extensible variant")
    @Test
    public void test1d() {
        TypeInfo X = javaInspector.parse(INPUT1d);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        TypeInfo M = X.findSubType("M");
        assertFalse(M.isExtensible());
        TypeInfo R = X.findSubType("R");
        HiddenContentTypes hctR = R.analysis().getOrDefault(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("", hctR.detailedSortedTypes());

        MethodInfo getA = X.findUniqueMethod("getA", 1);
        {
            Statement s0 = getA.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo viRv = vd0.variableInfo(getA.fullyQualifiedName());
            assertEquals("E=r.a", viRv.staticValues().toString());
            assertEquals("-1-:a, -2-|*-0:r", viRv.linkedVariables().toString());
        }
    }

    @Language("java")
    private static final String INPUT1e = """
            package a.b;
            import java.util.Set;
            class X {
                static class M { int i; int get() { return i; } void set(int i) { this.i = i; }}
                record R(M a) {}
                static M getA(R r) {
                    return r.a();
                }
            }
            """;

    @DisplayName("getter, accessor variant")
    @Test
    public void test1e() {
        TypeInfo X = javaInspector.parse(INPUT1e);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        TypeInfo M = X.findSubType("M");
        assertTrue(M.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE).isMutable());
        assertTrue(M.isExtensible());
        TypeInfo R = X.findSubType("R");
        HiddenContentTypes hctR = R.analysis().getOrDefault(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("0=M", hctR.detailedSortedTypes());

        MethodInfo getA = X.findUniqueMethod("getA", 1);
        {
            Statement s0 = getA.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo viRv = vd0.variableInfo(getA.fullyQualifiedName());
            assertEquals("E=r.a", viRv.staticValues().toString());
            assertEquals("-1-:a, *M-2-0M|*-0:r", viRv.linkedVariables().toString());
        }
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Set;
            class X {
                static class M { int i; int get() { return i; } void set(int i) { this.i = i; }}
                record R(M a, M b) {}
                static void modifyA(R r) {
                    M aa = r.a;
                    M bb = r.b;
                    aa.set(3);
                }
            }
            """;

    @DisplayName("modify one component")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        MethodInfo modifyA = X.findUniqueMethod("modifyA", 1);
        ParameterInfo r = modifyA.parameters().get(0);
        {
            Statement s0 = modifyA.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo viA = vd0.variableInfo("aa");
            assertEquals("E=r.a", viA.staticValues().toString());
            assertEquals("-1-:a, *M-2-0M|*-0:r", viA.linkedVariables().toString());

            VariableInfo viR = vd0.variableInfo(r);
            assertEquals("0M-2-*M|0-*:a, 0M-2-*M|0-*:aa", viR.linkedVariables().toString());
        }
        {
            Statement s1 = modifyA.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo viA = vd1.variableInfo("aa");
            assertEquals("E=r.a", viA.staticValues().toString());
            assertEquals("-1-:a, *M-2-0M|*-0:r", viA.linkedVariables().toString());

            VariableInfo viB = vd1.variableInfo("bb");
            assertEquals("E=r.b", viB.staticValues().toString());
            assertEquals("-1-:b, *M-2-0M|*-1:r", viB.linkedVariables().toString());

            VariableInfo viR = vd1.variableInfo(r);
            assertEquals("0M-2-*M|0-*:a, 0M-2-*M|0-*:aa, 0M-2-*M|1-*:b, 0M-2-*M|1-*:bb",
                    viR.linkedVariables().toString());
        }
        {
            Statement s2 = modifyA.methodBody().statements().get(2);
            VariableData vd2 = VariableDataImpl.of(s2);
            VariableInfo viA = vd2.variableInfo("aa");
            assertTrue(viA.isModified());

            VariableInfo viR = vd2.variableInfo(r);
            assertTrue(viR.isModified());

            VariableInfo viB = vd2.variableInfo("bb");
            assertFalse(viB.isModified());
        }
    }


    @Language("java")
    private static final String INPUT2b = """
            package a.b;
            import java.util.Set;
            class X {
                static class M { int i; int get() { return i; } void set(int i) { this.i = i; }}
                record R(M a, M b) {}
                static void modifyA(R r) {
                    M aa = r.a();
                    M bb = r.b();
                    aa.set(3);
                }
            }
            """;

    @DisplayName("modify one component, variant with accessors")
    @Test
    public void test2b() {
        TypeInfo X = javaInspector.parse(INPUT2b);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        MethodInfo modifyA = X.findUniqueMethod("modifyA", 1);
        ParameterInfo r = modifyA.parameters().get(0);
        {
            Statement s0 = modifyA.methodBody().statements().get(0);
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo viA = vd0.variableInfo("aa");
            assertEquals("E=r.a", viA.staticValues().toString());
            assertEquals("-1-:a, *M-2-0M|*-0:r", viA.linkedVariables().toString());

            VariableInfo viR = vd0.variableInfo(r);
            assertEquals("0M-2-*M|0-*:a, 0M-2-*M|0-*:aa", viR.linkedVariables().toString());
        }
        {
            Statement s1 = modifyA.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo viA = vd1.variableInfo("aa");
            assertEquals("E=r.a", viA.staticValues().toString());
            assertEquals("-1-:a, *M-2-0M|*-0:r", viA.linkedVariables().toString());

            VariableInfo viB = vd1.variableInfo("bb");
            assertEquals("E=r.b", viB.staticValues().toString());
            assertEquals("-1-:b, *M-2-0M|*-1:r", viB.linkedVariables().toString());

            VariableInfo viR = vd1.variableInfo(r);
            assertEquals("0M-2-*M|0-*:a, 0M-2-*M|0-*:aa, 0M-2-*M|1-*:b, 0M-2-*M|1-*:bb",
                    viR.linkedVariables().toString());
        }
        {
            Statement s2 = modifyA.methodBody().statements().get(2);
            VariableData vd2 = VariableDataImpl.of(s2);
            VariableInfo viA = vd2.variableInfo("aa");
            assertTrue(viA.isModified());

            VariableInfo viR = vd2.variableInfo(r);
            assertTrue(viR.isModified());

            VariableInfo viB = vd2.variableInfo("bb");
            assertFalse(viB.isModified());
        }
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.Set;
            class X {
                static <T> T nonNull(T t) {
                    if(t == null) throw new NullPointerException();
                    return t;
                }
                static class M { int i; int get() { return i; } void set(int i) { this.i = i; }}
                record R(M a) {}
                static M getADirectly(R r ) {
                    return r.a;
                }
                static M getA(R r) {
                    return nonNull(r.a);
                }
            }
            """;

    @DisplayName("identity function preserves modification area information, static values")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder, 2);
        MethodInfo nonNull = X.findUniqueMethod("nonNull", 1);
        assertTrue(nonNull.isIdentity());

        TypeInfo M = X.findSubType("M");
        assertTrue(M.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE).isMutable());
        assertTrue(M.isExtensible());
        TypeInfo R = X.findSubType("R");
        HiddenContentTypes hctR = R.analysis().getOrDefault(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("0=M", hctR.detailedSortedTypes());

        MethodInfo getADirectly = X.findUniqueMethod("getADirectly", 1);
        {
            Statement s0 = getADirectly.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo viRv = vd0.variableInfo(getADirectly.fullyQualifiedName());
            assertEquals("E=r.a", viRv.staticValues().toString());
            assertEquals("-1-:a, *M-2-0M|*-0:r", viRv.linkedVariables().toString());
        }
        MethodInfo getA = X.findUniqueMethod("getA", 1);
        {
            Statement s0 = getA.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            VariableInfo viRv = vd0.variableInfo(getA.fullyQualifiedName());
            assertEquals("E=r.a", viRv.staticValues().toString());
            assertEquals("-1-:a, *M-2-0M|*-0:r", viRv.linkedVariables().toString());
        }
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            class X {
                static class M { int i; int get() { return i; } void set(int i) { this.i = i; }}
                M[] method(M m1, M m2) {
                    M[] array = new M[2];
                    array[0] = m1;
                    array[1] = m2;
                    m1.set(3);
                    return array;
                }
            }
            """;

    @DisplayName("modification area of arrays")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo method = X.findUniqueMethod("method", 2);
        ParameterInfo m1 = method.parameters().get(0);
        ParameterInfo m2 = method.parameters().get(1);

        {
            VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().get(1));
            VariableInfo vi1Array = vd1.variableInfo("array");
            assertEquals("Type a.b.X.M[] E=new M[2] this[0]=m1", vi1Array.staticValues().toString());
            assertEquals("0M-2-*M|0-*:array[0], 0M-2-*M|0-*:m1", vi1Array.linkedVariables().toString());
            assertTrue(vi1Array.isModified());
        }
        {
            VariableData vd2 = VariableDataImpl.of(method.methodBody().statements().get(2));
            VariableInfo vi2Array = vd2.variableInfo("array");
            assertEquals("Type a.b.X.M[] E=new M[2] this[0]=m1, this[1]=m2", vi2Array.staticValues().toString());
            assertEquals("0M-2-*M|0-*:array[0], 0M-2-*M|1-*:array[1], 0M-2-*M|0-*:m1, 0M-2-*M|1-*:m2",
                    vi2Array.linkedVariables().toString());
            assertTrue(vi2Array.isModified());
            VariableInfo vi2M1 = vd2.variableInfo(m1);
            assertFalse(vi2M1.isModified());
            VariableInfo vi2M2 = vd2.variableInfo(m2);
            assertFalse(vi2M2.isModified());
        }
        {
            VariableData vd3 = VariableDataImpl.of(method.methodBody().statements().get(3));
            VariableInfo vi3Array = vd3.variableInfo("array");
            assertTrue(vi3Array.isModified());
            VariableInfo vi3M1 = vd3.variableInfo(m1);
            assertTrue(vi3M1.isModified());
            VariableInfo vi2M2 = vd3.variableInfo(m2);
            assertFalse(vi2M2.isModified());
        }
    }


    @Language("java")
    private static final String INPUT4b = """
            package a.b;
            class X {
                static class M { int i; int get() { return i; } void set(int i) { this.i = i; }}
                M[] method(M m1, M m2, int i) {
                    M[] array = new M[2];
                    array[i] = m1;
                    array[i+1] = m2;
                    m1.set(3);
                    return array;
                }
            }
            """;

    @DisplayName("modification area of arrays, index variables")
    @Test
    public void test4b() {
        TypeInfo X = javaInspector.parse(INPUT4b);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo method = X.findUniqueMethod("method", 3);
        ParameterInfo m1 = method.parameters().get(0);
        ParameterInfo m2 = method.parameters().get(1);

        {
            VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().get(1));
            VariableInfo vi1Array = vd1.variableInfo("array");
            assertEquals("Type a.b.X.M[] E=new M[2] this[i]=m1", vi1Array.staticValues().toString());
            assertEquals("0M-2-*M|?-*:array[i], 0M-2-*M|?-*:m1", vi1Array.linkedVariables().toString());
            assertTrue(vi1Array.isModified());
        }
        {
            VariableData vd2 = VariableDataImpl.of(method.methodBody().statements().get(2));
            VariableInfo vi2Array = vd2.variableInfo("array");
            assertEquals("Type a.b.X.M[] E=new M[2] this[i+1]=m2, this[i]=m1", vi2Array.staticValues().toString());
            assertEquals("0M-2-*M|?-*:array[i+1], 0M-2-*M|?-*:array[i], 0M-2-*M|?-*:m1, 0M-2-*M|?-*:m2",
                    vi2Array.linkedVariables().toString());
            assertTrue(vi2Array.isModified());
            VariableInfo vi2M1 = vd2.variableInfo(m1);
            assertFalse(vi2M1.isModified());
            VariableInfo vi2M2 = vd2.variableInfo(m2);
            assertFalse(vi2M2.isModified());
        }
        {
            VariableData vd3 = VariableDataImpl.of(method.methodBody().statements().get(3));
            VariableInfo vi3Array = vd3.variableInfo("array");
            assertTrue(vi3Array.isModified());
            VariableInfo vi3M1 = vd3.variableInfo(m1);
            assertTrue(vi3M1.isModified());
            VariableInfo vi2M2 = vd3.variableInfo(m2);
            assertFalse(vi2M2.isModified());
        }
    }

}