package org.e2immu.analyzer.modification.prepwork.hcs;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.hct.ComputeHiddenContent;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.analyzer.modification.prepwork.variable.Indices;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.inspection.api.parser.GenericsHelper;
import org.e2immu.language.inspection.impl.parser.GenericsHelperImpl;
import org.e2immu.util.internal.util.MapUtil;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.*;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.NO_VALUE;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestComputeHCS extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                interface I<T> {
                    String m(T t);
                }
                static class C1<S> implements I<S> {
                    S s;
                    public String m(S s) {
                        return s.toString();
                    }
                }
                static class CI implements I<Integer> {
                    public String m(Integer i) {
                        return (i+1) + "";
                    }
                }
                static class CO {
                    Object o;
                    String s;
                    void set(Object o) {
                        this.o = o;
                        this.s = o.toString();
                    }
                    Object getO() {
                        return o;
                    }
                }
            }
            """;

    @DisplayName("basics, extension of TestComputeHiddenContent.basics")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        ComputeHiddenContent chc = new ComputeHiddenContent(javaInspector.runtime());
        ComputeHCS computeHCS = new ComputeHCS(runtime);

        TypeInfo integer = runtime.integerTypeInfo();
        assertFalse(integer.isExtensible());
        HiddenContentTypes hctInteger = chc.compute(integer);
        assertEquals("", hctInteger.detailedSortedTypes());
        integer.analysis().set(HIDDEN_CONTENT_TYPES, hctInteger);

        TypeInfo I = X.findSubType("I");
        HiddenContentTypes hctI = chc.compute(I);
        assertEquals("0=T", hctI.detailedSortedTypes());
        I.analysis().set(HIDDEN_CONTENT_TYPES, hctI);
        MethodInfo Im = I.findUniqueMethod("m", 1);
        HiddenContentTypes hctIM = chc.compute(hctI, Im);
        assertEquals("I:T - m:", hctIM.toString());
        Im.analysis().set(HIDDEN_CONTENT_TYPES, hctIM);

        HiddenContentSelector hcsIm = computeHCS.doHiddenContentSelector(Im);
        assertEquals("X", hcsIm.detailed());
        HiddenContentSelector hcsImM = Im.parameters().get(0).analysis().getOrDefault(HCS_PARAMETER, NONE);
        assertEquals("0=*", hcsImM.detailed());

        TypeInfo C1 = X.findSubType("C1");
        HiddenContentTypes hctC1 = chc.compute(C1);
        assertEquals("0=S", hctC1.detailedSortedTypes());
        assertEquals("S=0", hctC1.detailedSortedTypeToIndex());
        C1.analysis().set(HIDDEN_CONTENT_TYPES, hctC1);

        TypeInfo CI = X.findSubType("CI");
        HiddenContentTypes hctCI = chc.compute(CI);
        assertEquals("", hctCI.detailedSortedTypes());
        assertEquals("", hctCI.detailedSortedTypeToIndex());
        CI.analysis().set(HIDDEN_CONTENT_TYPES, hctCI);

        TypeInfo string = runtime.stringTypeInfo();
        assertFalse(string.isExtensible());
        HiddenContentTypes hctString = chc.compute(string);
        assertEquals("", hctString.detailedSortedTypes());
        string.analysis().set(HIDDEN_CONTENT_TYPES, hctString);

        TypeInfo object = runtime.objectTypeInfo();
        assertTrue(object.isExtensible());
        HiddenContentTypes hctObject = chc.compute(object);
        assertEquals("", hctObject.detailedSortedTypes());
        object.analysis().set(HIDDEN_CONTENT_TYPES, hctObject);

        TypeInfo comparable = javaInspector.compiledTypesManager().get(Comparable.class);
        assertTrue(object.isExtensible());
        HiddenContentTypes hctComparable = chc.compute(comparable);
        assertEquals("0=T", hctComparable.detailedSortedTypes());
        comparable.analysis().set(HIDDEN_CONTENT_TYPES, hctComparable);

        TypeInfo CO = X.findSubType("CO");
        HiddenContentTypes hctCO = chc.compute(CO);
        assertEquals("0=Object", hctCO.detailedSortedTypes());
        assertEquals("Object=0", hctCO.detailedSortedTypeToIndex());
        CO.analysis().set(HIDDEN_CONTENT_TYPES, hctCO);

        MethodInfo COGetO = CO.findUniqueMethod("getO", 0);
        HiddenContentTypes hctCoGetO = chc.compute(hctCO, COGetO);
        COGetO.analysis().set(HIDDEN_CONTENT_TYPES, hctCoGetO);

        HiddenContentSelector hcsCOGet = computeHCS.doHiddenContentSelector(COGetO);
        assertEquals("0=*", hcsCOGet.detailed());
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class X {
                interface C {
                    void setO(Object o);
                    Object getO();
                }
                static class CO implements C {
                    Object o;
                    String s;
                    void setO(Object o) {
                        this.o = o;
                        this.s = o.toString();
                    }
                    Object getO() {
                        return o;
                    }
                }
            }
            """;

    @DisplayName("interface and implementation")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        ComputeHiddenContent chc = new ComputeHiddenContent(javaInspector.runtime());
        ComputeHCS computeHCS = new ComputeHCS(runtime);

        TypeInfo string = runtime.stringTypeInfo();
        assertFalse(string.isExtensible());
        HiddenContentTypes hctString = chc.compute(string);
        assertEquals("", hctString.detailedSortedTypes());
        string.analysis().set(HIDDEN_CONTENT_TYPES, hctString);

        /*
        in the interface C, setO and getO do not have the @GetSet annotation.
        We don't know whether to take Object as a HCT or not.
        Currently, we don't.
         */
        TypeInfo C = X.findSubType("C");
        HiddenContentTypes hctC = chc.compute(C);
        assertEquals("", hctC.detailedSortedTypes());
        C.analysis().set(HIDDEN_CONTENT_TYPES, hctC);

        MethodInfo CgetO = C.findUniqueMethod("getO", 0);
        HiddenContentTypes hctCgetO = chc.compute(hctC, CgetO);
        assertEquals("C: - getO:Object", hctCgetO.toString());
        CgetO.analysis().set(HIDDEN_CONTENT_TYPES, hctCgetO);

        MethodInfo CsetO = C.findUniqueMethod("setO", 1);
        HiddenContentTypes hctCsetO = chc.compute(hctC, CsetO);
        assertEquals("C: - setO:Object", hctCsetO.toString());
        CsetO.analysis().set(HIDDEN_CONTENT_TYPES, hctCgetO);

        HiddenContentSelector hcsCgetO = computeHCS.doHiddenContentSelector(CgetO);
        assertEquals("0=*", hcsCgetO.detailed());
        HiddenContentSelector hcsCSetO0 = CsetO.parameters().get(0).analysis().getOrDefault(HCS_PARAMETER, NONE);
        assertEquals("X", hcsCSetO0.detailed());

        TypeInfo CO = X.findSubType("CO");
        HiddenContentTypes hctCO = chc.compute(CO);
        assertEquals("0=Object", hctCO.detailedSortedTypes());
        CO.analysis().set(HIDDEN_CONTENT_TYPES, hctCO);

        MethodInfo COgetO = CO.findUniqueMethod("getO", 0);
        HiddenContentTypes hctCOgetO = chc.compute(hctCO, COgetO);
        assertEquals("CO:Object - getO:", hctCOgetO.toString());
        COgetO.analysis().set(HIDDEN_CONTENT_TYPES, hctCOgetO);

        MethodInfo COsetO = CO.findUniqueMethod("setO", 1);
        HiddenContentTypes hctCOsetO = chc.compute(hctCO, COsetO);
        assertEquals("CO:Object - setO:", hctCOsetO.toString());
        COsetO.analysis().set(HIDDEN_CONTENT_TYPES, hctCOgetO);

        // We go via the override...
        HiddenContentSelector hcsCOgetO = computeHCS.doHiddenContentSelector(COgetO);
        assertEquals("0=*", hcsCOgetO.detailed());
        HiddenContentSelector hcsCOSetO0 = COsetO.parameters().get(0).analysis().getOrDefault(HCS_PARAMETER, NONE);
        assertEquals("X", hcsCOSetO0.detailed());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import org.e2immu.annotation.method.GetSet;
            class X {
                interface C {
                    @GetSet void setO(Object o);
                    @GetSet Object getO();
                }
                static class CO implements C {
                    Object o;
                    String s;
                    void setO(Object o) {
                        this.o = o;
                        this.s = o.toString();
                    }
                    Object getO() {
                        return o;
                    }
                }
            }
            """;

    @DisplayName("interface and implementation, with @GetSet")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.initialize(javaInspector.compiledTypesManager().typesLoaded(true));
        prepAnalyzer.doPrimaryType(X);

        TypeInfo string = runtime.stringTypeInfo();
        assertFalse(string.isExtensible());
        HiddenContentTypes hctString = string.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("", hctString.detailedSortedTypes());

        /*
        in the interface C, setO and getO do not have the @GetSet annotation.
        We don't know whether to take Object as a HCT or not.
        Currently, we don't.
         */
        TypeInfo C = X.findSubType("C");
        assertTrue(C.isInterface());
        assertEquals(1, C.fields().size());
        FieldInfo synthetic = C.fields().get(0);
        assertEquals("o", synthetic.name());
        assertEquals("Type Object", synthetic.type().toString());
        assertTrue(synthetic.isSynthetic());
        assertTrue(C.isExtensible());

        MethodInfo CsetO = C.findUniqueMethod("setO", 1);
        assertSame(synthetic, CsetO.getSetField().field());
        MethodInfo CgetO = C.findUniqueMethod("getO", 0);
        assertSame(synthetic, CgetO.getSetField().field());

        HiddenContentTypes hctC = C.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=Object", hctC.detailedSortedTypes());

        HiddenContentTypes hctCgetO = CgetO.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("C:Object - getO:", hctCgetO.toString());

        HiddenContentTypes hctCsetO = CsetO.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("C:Object - setO:", hctCsetO.toString());

        HiddenContentSelector hcsCgetO = CgetO.analysis().getOrDefault(HCS_METHOD, NONE);
        assertEquals("0=*", hcsCgetO.detailed());
        HiddenContentSelector hcsCSetO0 = CsetO.parameters().get(0).analysis().getOrDefault(HCS_PARAMETER, NONE);
        assertEquals("0=*", hcsCSetO0.detailed());

        TypeInfo CO = X.findSubType("CO");
        HiddenContentTypes hctCO = CO.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=Object", hctCO.detailedSortedTypes());

        MethodInfo COgetO = CO.findUniqueMethod("getO", 0);
        HiddenContentTypes hctCOgetO = COgetO.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("CO:Object - getO:", hctCOgetO.toString());

        MethodInfo COsetO = CO.findUniqueMethod("setO", 1);
        HiddenContentTypes hctCOsetO = COsetO.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("CO:Object - setO:", hctCOsetO.toString());

        HiddenContentSelector hcsCOgetO = COgetO.analysis().getOrDefault(HCS_METHOD, NONE);
        assertEquals("0=*", hcsCOgetO.detailed());
        HiddenContentSelector hcsCOSetO0 = COsetO.parameters().get(0).analysis().getOrDefault(HCS_PARAMETER, NONE);
        assertEquals("0=*", hcsCOSetO0.detailed());
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.List;
            class X {
                List<Object> objects;
                Comparable<X> comparable; // a different type
                Object get(int i) {
                    return objects.get(i);
                }
                List<Object> objects() {
                    return objects;
                }
            }
            """;

    @DisplayName("hcs of list types")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.initialize(javaInspector.compiledTypesManager().typesLoaded(true));
        prepAnalyzer.doPrimaryType(X);

        assertEquals("0=List, 1=Object, 2=Comparable, 3=X",
                X.analysis().getOrNull(HIDDEN_CONTENT_TYPES, HiddenContentTypes.class).detailedSortedTypes());

        // 0=* means: all of type 0
        MethodInfo objects = X.findUniqueMethod("objects", 0);
        HiddenContentTypes hctObjects = objects.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=List, 1=Object, 2=Comparable, 3=X - ", hctObjects.detailedSortedTypes());
        HiddenContentSelector hcsObjects = objects.analysis().getOrDefault(HCS_METHOD, NONE);
        // 0=* means: return an object of type List (0), access is direct '*'
        // 1=0 means: objects of type Object (1) can be found at index '0'
        assertEquals("0=*,1=0", hcsObjects.detailed());

        MethodInfo get = X.findUniqueMethod("get", 1);
        HiddenContentTypes hctGet = get.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=List, 1=Object, 2=Comparable, 3=X - ", hctGet.detailedSortedTypes());
        HiddenContentSelector hcsGet = get.analysis().getOrDefault(HCS_METHOD, NONE);
        // 1=* means: return an object of type Object (1), direct access
        assertEquals("1=*", hcsGet.detailed());
    }


    @Language("java")
    private static final String INPUT4b = """
            package a.b;
            import java.util.List;
            class X<T> {
                List<T> objects;
                Comparable<T> comparable; // a different type
                T get(int i) {
                    return objects.get(i);
                }
                List<T> objects() {
                    return objects;
                }
            }
            """;

    @DisplayName("hcs of list types, type parameter")
    @Test
    public void test4b() {
        TypeInfo X = javaInspector.parse(INPUT4b);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.initialize(javaInspector.compiledTypesManager().typesLoaded(true));
        prepAnalyzer.doPrimaryType(X);

        assertEquals("0=T, 1=List, 2=Comparable",
                X.analysis().getOrNull(HIDDEN_CONTENT_TYPES, HiddenContentTypes.class).detailedSortedTypes());

        MethodInfo objects = X.findUniqueMethod("objects", 0);
        HiddenContentTypes hctObjects = objects.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=T, 1=List, 2=Comparable - ", hctObjects.detailedSortedTypes());
        HiddenContentSelector hcsObjects = objects.analysis().getOrDefault(HCS_METHOD, NONE);
        // 1=* objects of type List, direct access; 0=0: access to T via index 0 in List
        assertEquals("0=0,1=*", hcsObjects.detailed());

        // the outcome "0=*" means: a single Object
        MethodInfo get = X.findUniqueMethod("get", 1);
        HiddenContentTypes hctGet = get.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=T, 1=List, 2=Comparable - ", hctGet.detailedSortedTypes());
        HiddenContentSelector hcsGet = get.analysis().getOrDefault(HCS_METHOD, NONE);
        assertEquals("0=*", hcsGet.detailed());
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            class X {
                Object[] objects;
                Comparable<X> comparable; // a different type
                Object get(int i) {
                    return objects[i];
                }
                Object[] objects() {
                    return objects;
                }
            }
            """;

    @DisplayName("hcs of array types")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.initialize(javaInspector.compiledTypesManager().typesLoaded(true));
        prepAnalyzer.doPrimaryType(X);

        assertEquals("0=Object, 1=Comparable, 2=X",
                X.analysis().getOrNull(HIDDEN_CONTENT_TYPES, HiddenContentTypes.class).detailedSortedTypes());

        MethodInfo objects = X.findUniqueMethod("objects", 0);
        HiddenContentTypes hctObjects = objects.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=Object, 1=Comparable, 2=X - ", hctObjects.detailedSortedTypes());
        HiddenContentSelector hcsObjects = objects.analysis().getOrDefault(HCS_METHOD, NONE);
        // 0=0 means: objects of type "Object" can be found at index 0
        assertEquals("0=0", hcsObjects.detailed());

        // the outcome "0=*" means: objects of type Object, direct access
        MethodInfo get = X.findUniqueMethod("get", 1);
        HiddenContentTypes hctGet = get.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=Object, 1=Comparable, 2=X - ", hctGet.detailedSortedTypes());
        HiddenContentSelector hcsGet = get.analysis().getOrDefault(HCS_METHOD, NONE);
        assertEquals("0=*", hcsGet.detailed());
    }

    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.HashSet;
            import java.util.Set;
            import java.util.List;
            class X {
                record R<T>(Set<Integer> set, int i, List<String> list, T t, List<T> ts) {}
                R<Integer> r;
                X(Set<Integer> set) {
                    r = new R<>(set, 0, List.of(), 3, List.of(4));
                }
            }
            """;

    @DisplayName("type parameters and other HC")
    @Test
    public void test6() {
        GenericsHelper genericsHelper = new GenericsHelperImpl(runtime);

        TypeInfo X = javaInspector.parse(INPUT6);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.initialize(javaInspector.compiledTypesManager().typesLoaded(true));
        prepAnalyzer.doPrimaryType(X);
        TypeInfo R = X.findSubType("R");
        HiddenContentTypes hctR = R.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=T, 1=Set, 2=List", hctR.detailedSortedTypes());

        MethodInfo constructorR = R.findConstructor(5);
        HiddenContentTypes hctRConstructor = constructorR.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);

        ParameterInfo Rc0 = constructorR.parameters().get(0);
        HiddenContentSelector hcsRc0 = Rc0.analysis().getOrDefault(HCS_PARAMETER, NONE);
        assertEquals("1=*", hcsRc0.detailed());

        ParameterizedType setInteger = Rc0.parameterizedType();
        Map<Indices, IndicesAndType> translate = hcsRc0.translateHcs(runtime, genericsHelper, setInteger, setInteger,
                false);
        assertEquals("*=IndicesAndType[indices=*, type=Type java.util.Set<Integer>]", MapUtil.nice(translate));

        ParameterInfo Rc1 = constructorR.parameters().get(1);
        HiddenContentSelector hcsRc1 = Rc1.analysis().getOrDefault(HCS_PARAMETER, NONE);
        assertEquals("X", hcsRc1.detailed());
        ParameterInfo Rc2 = constructorR.parameters().get(2);
        HiddenContentSelector hcsRc2 = Rc2.analysis().getOrDefault(HCS_PARAMETER, NONE);
        assertEquals("2=*", hcsRc2.detailed());

        ParameterInfo Rc3 = constructorR.parameters().get(3);
        HiddenContentSelector hcsRc3 = Rc3.analysis().getOrDefault(HCS_PARAMETER, NONE);
        assertEquals("0=*", hcsRc3.detailed());
        ParameterInfo Rc4 = constructorR.parameters().get(4);
        HiddenContentSelector hcsRc4 = Rc4.analysis().getOrDefault(HCS_PARAMETER, NONE);
        assertEquals("0=0,2=*", hcsRc4.detailed());

        ParameterizedType formalR = R.asParameterizedType();
        assertEquals("Type a.b.X.R<T>", formalR.toString());
        HiddenContentSelector hcsFormal = HiddenContentSelector.selectAll(hctR, formalR);
        assertEquals("0=0,1=1,2=2", hcsFormal.detailed());

        HiddenContentTypes hctX = X.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=R", hctX.detailedSortedTypes());
        ParameterizedType formalX = X.asParameterizedType();
        HiddenContentSelector hcsXFormal = HiddenContentSelector.selectAll(hctX, formalX);
        assertEquals("0=0", hcsXFormal.detailed());

        HiddenContentSelector hcsFormalViaConstructor = HiddenContentSelector.selectAll(hctRConstructor, formalR);
        assertEquals("0=0,1=1,2=2", hcsFormalViaConstructor.detailed());

        Map<Indices, IndicesAndType> t = hcsFormalViaConstructor.translateHcs(runtime, genericsHelper, formalR,
                formalR, false);
        assertEquals("""
                0=IndicesAndType[indices=0, type=Type param T], \
                1=IndicesAndType[indices=1, type=Type java.util.Set<E>], \
                2=IndicesAndType[indices=2, type=Type java.util.List<E>]\
                """, MapUtil.nice(t));
    }


    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import java.util.Iterator;
            import java.util.function.Function;
            class X {
                record LL<T>(T head, LL<T> tail) {
                    LL<T> prepend(T t) {
                        return new LL<>(t, this);
                    }
                }
                static <T> void add(LL<T> list, T one) {
                    LL<T> longer = list.prepend(one);
                    assert longer.head != null;
                }
            }
            """;

    @DisplayName("object recursion")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.initialize(javaInspector.compiledTypesManager().typesLoaded(true));
        prepAnalyzer.doPrimaryType(X);
        TypeInfo LL = X.findSubType("LL");
        HiddenContentTypes hctLL = LL.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=T, 1=LL", hctLL.detailedSortedTypes());

        MethodInfo LLC = LL.findConstructor(2);
        ParameterizedType LLpt = LL.asParameterizedType();
        HiddenContentTypes hctLLC = LLC.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=T, 1=LL - 2=Object", hctLLC.detailedSortedTypes());

        HiddenContentSelector hcsFormalViaConstructor = HiddenContentSelector.selectAll(hctLLC, LLpt);
        assertEquals("0=0,1=*", hcsFormalViaConstructor.detailed());

        This thisVar = runtime.newThis(LLpt);
        assertSame(LLpt, thisVar.parameterizedType());
        assertEquals("Type a.b.X.LL<T>", LLpt.toString());
        Map<Indices, ParameterizedType> map = hcsFormalViaConstructor.extract(runtime, thisVar.parameterizedType());
        assertEquals("*=Type a.b.X.LL<T>, 0=Type param T", MapUtil.nice(map));
        GenericsHelper genericsHelper = new GenericsHelperImpl(runtime);

        Map<Indices, IndicesAndType> translate = hcsFormalViaConstructor.translateHcs(runtime, genericsHelper, LLpt,
                LLpt, false);
        assertEquals("""
                *=IndicesAndType[indices=*, type=Type a.b.X.LL<T>], \
                0=IndicesAndType[indices=0, type=Type param T]\
                """, MapUtil.nice(translate));
    }


    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            import java.util.Iterator;
            import java.util.function.Function;
            class X {
                interface L<T> {
                    T head();
                    L<T> tail();
                    L<T> prepend(T t);
                }
                record LL<T>(T head, L<T> tail) implements L<T> {
                    @Override
                    L<T> prepend(T t) {
                        return new LL<>(t, this);
                    }
                }
                static <T> void add(L<T> list, T one) {
                    L<T> longer = list.prepend(one);
                    assert longer.head() != null;
                }
            }
            """;

    @DisplayName("object recursion, interface in between")
    @Test
    public void test8() {
        TypeInfo X = javaInspector.parse(INPUT8);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.initialize(javaInspector.compiledTypesManager().typesLoaded(true));
        prepAnalyzer.doPrimaryType(X);

        TypeInfo L = X.findSubType("L");
        HiddenContentTypes hctL = L.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=T", hctL.detailedSortedTypes());

        TypeInfo LL = X.findSubType("LL");
        HiddenContentTypes hctLL = LL.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=T, 1=L", hctLL.detailedSortedTypes());

        MethodInfo LLC = LL.findConstructor(2);
        ParameterizedType LLpt = LL.asParameterizedType();
        HiddenContentTypes hctLLC = LLC.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=T, 1=L - 2=Object", hctLLC.detailedSortedTypes());

        HiddenContentSelector hcsFormalViaConstructor = HiddenContentSelector.selectAll(hctLLC, LLpt);
        assertEquals("0=0,1=*", hcsFormalViaConstructor.detailed());

        ParameterizedType Lpt = L.asParameterizedType();
        assertEquals("Type a.b.X.L<T>", Lpt.toString());
        assertEquals("Type a.b.X.LL<T>", LLpt.toString());

        Map<Indices, ParameterizedType> mapLL = hcsFormalViaConstructor.extract(runtime, LLpt);
        assertEquals("*=Type a.b.X.LL<T>, 0=Type param T", MapUtil.nice(mapLL));

        Map<Indices, ParameterizedType> mapL = hcsFormalViaConstructor.extract(runtime, Lpt);
        assertEquals("*=Type a.b.X.L<T>, 0=Type param T", MapUtil.nice(mapL));

        GenericsHelper genericsHelper = new GenericsHelperImpl(runtime);
        assertTrue(Lpt.isAssignableFrom(runtime, LLpt));
        Map<Indices, IndicesAndType> translate = hcsFormalViaConstructor.translateHcs(runtime, genericsHelper, Lpt,
                LLpt, false);
        assertEquals("""
                *=IndicesAndType[indices=*, type=Type a.b.X.LL<T>], \
                0=IndicesAndType[indices=0, type=Type param T]\
                """, MapUtil.nice(translate));
    }


    @Language("java")
    private static final String INPUT9 = """
            package a.b;
            import java.util.Iterator;
            import java.util.function.Function;
            class X {
                interface L {
                    Object head();
                    L tail();
                    L prepend(Object t);
                }
                record LL(Object head, L tail) implements L {
                    @Override
                    L prepend(Object t) {
                        return new LL(t, this);
                    }
                }
                static void add(L list, Object one) {
                    L longer = list.prepend(one);
                    assert longer.head() != null;
                }
            }
            """;

    /*
    we're not treating "Object" on equal footing to "T"
    essentially because ???
    there could be multiple fields with type Object? but the same holds for T
    why not??
     */
    @DisplayName("object recursion, interface in between, Object instead of type parameter")
    @Test
    public void test9() {
        TypeInfo X = javaInspector.parse(INPUT9);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        prepAnalyzer.initialize(javaInspector.compiledTypesManager().typesLoaded(true));
        prepAnalyzer.doPrimaryType(X);

        TypeInfo L = X.findSubType("L");
        HiddenContentTypes hctL = L.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("", hctL.detailedSortedTypes()); // this is how the shallow analyzer works

        TypeInfo LL = X.findSubType("LL");
        HiddenContentTypes hctLL = LL.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=Object, 1=L", hctLL.detailedSortedTypes());

        MethodInfo LLC = LL.findConstructor(2);
        ParameterizedType LLpt = LL.asParameterizedType();
        HiddenContentTypes hctLLC = LLC.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        assertEquals("0=Object, 1=L - ", hctLLC.detailedSortedTypes());

        HiddenContentSelector hcsFormalViaConstructor = HiddenContentSelector.selectAll(hctLLC, LLpt);
        assertEquals("0=0,1=*", hcsFormalViaConstructor.detailed());

        ParameterizedType Lpt = L.asParameterizedType();
        assertEquals("Type a.b.X.L", Lpt.toString());
        assertEquals("Type a.b.X.LL", LLpt.toString());

        Map<Indices, ParameterizedType> mapLL = hcsFormalViaConstructor.extract(runtime, LLpt);
        assertEquals("*=Type a.b.X.LL, 0=Type Object", MapUtil.nice(mapLL));

        Map<Indices, ParameterizedType> mapL = hcsFormalViaConstructor.extract(runtime, Lpt);
        assertEquals("*=Type a.b.X.L, 0=Type Object", MapUtil.nice(mapL));

        GenericsHelper genericsHelper = new GenericsHelperImpl(runtime);
        assertTrue(Lpt.isAssignableFrom(runtime, LLpt));
        Map<Indices, IndicesAndType> translate = hcsFormalViaConstructor.translateHcs(runtime, genericsHelper, Lpt,
                LLpt, false);
        assertEquals("*=IndicesAndType[indices=*, type=Type a.b.X.LL], 0=IndicesAndType[indices=0, type=Type Object]", MapUtil.nice(translate));
    }



    @Language("java")
    private static final String INPUT10 = """
            package a.b;
            import java.util.Collection;
            class X {
                static <T> void add(Collection<T> ts, T[] tArray, int[] intArray) {
                    for (int i=0; i<intArray.length; i++) {
                        ts.add(tArray[intArray[i]]);
                    }
                }
            }
            """;

    @DisplayName("hcs of static method with type parameters and arrays")
    @Test
    public void test10() {
        ComputeHiddenContent chc = new ComputeHiddenContent(javaInspector.runtime());
        TypeInfo X = javaInspector.parse(INPUT10);
        HiddenContentTypes hct = chc.compute(X);
        assertEquals("", hct.detailedSortedTypes());
        MethodInfo add = X.findUniqueMethod("add", 3);
        HiddenContentTypes hctMethod = chc.compute(hct, add);
        assertEquals(" - 0=T, 1=Collection", hctMethod.detailedSortedTypes());

        ParameterInfo ts = add.parameters().get(0);
        assertEquals("0=0,1=*", HiddenContentSelector.selectAll(hctMethod, ts.parameterizedType()).detailed());

        ParameterInfo tArray = add.parameters().get(1);
        assertEquals("0=0",  HiddenContentSelector.selectAll(hctMethod, tArray.parameterizedType()).detailed());

        ParameterInfo intArray = add.parameters().get(2);
        assertEquals("X",  HiddenContentSelector.selectAll(hctMethod, intArray.parameterizedType()).detailed());
    }
}
