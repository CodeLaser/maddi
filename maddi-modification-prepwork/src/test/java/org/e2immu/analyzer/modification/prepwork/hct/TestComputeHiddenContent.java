package org.e2immu.analyzer.modification.prepwork.hct;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.hcs.ComputeHCS;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.analysis.PropertyProviderImpl;
import org.e2immu.language.cst.io.CodecImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.DoubleConsumer;

import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;
import static org.junit.jupiter.api.Assertions.*;

public class TestComputeHiddenContent extends CommonTest {

    @Test
    public void test() {
        ComputeHiddenContent chc = new ComputeHiddenContent(javaInspector.runtime());

        TypeInfo list = javaInspector.compiledTypesManager().get(List.class);
        HiddenContentTypes hctList = chc.compute(list);
        assertEquals("0=E", hctList.detailedSortedTypes());

        Codec codec = new CodecImpl(javaInspector.runtime(), PropertyProviderImpl::get, null,
                null); // we don't have to decode
        Codec.Context context = new CodecImpl.ContextImpl();
        Codec.EncodedValue ev = hctList.encode(codec, context);
        assertEquals("{\"0\":\"PE:0\",\"E\":true,\"M\":1}", ev.toString());

        TypeInfo mapEntry = javaInspector.compiledTypesManager().getOrLoad(Map.Entry.class);
        HiddenContentTypes hctMapEntry = chc.compute(mapEntry);
        assertEquals("0=K, 1=V", hctMapEntry.detailedSortedTypes());

        Codec.EncodedValue ev2 = hctMapEntry.encode(codec, context);
        assertEquals("{\"0\":\"PK:0\",\"1\":\"PV:1\",\"E\":true,\"M\":2}", ev2.toString());
    }

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
                    Integer i;
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
                record R(int k, R r) {
                }
                record RT<T>(T t, RT<T> r) {
                }
            }
            """;

    @DisplayName("basics")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        ComputeHiddenContent chc = new ComputeHiddenContent(javaInspector.runtime());

        TypeInfo integer = runtime.integerTypeInfo();
        assertFalse(integer.isExtensible());
        HiddenContentTypes hctInteger = chc.compute(integer);
        assertEquals("", hctInteger.detailedSortedTypes());
        integer.analysis().set(HIDDEN_CONTENT_TYPES, hctInteger);

        TypeInfo I = X.findSubType("I");
        HiddenContentTypes hctI = chc.compute(I);
        assertEquals("0=T", hctI.detailedSortedTypes());
        I.analysis().set(HIDDEN_CONTENT_TYPES, hctI);

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

        // if a type has a self-reference, and hidden content, the type itself is part of the hidden content!

        // self-reference, but no hidden content
        TypeInfo R = X.findSubType("R");
        HiddenContentTypes hctR = chc.compute(R);
        assertEquals("", hctR.detailedSortedTypes());

        // self-reference and hidden content
        TypeInfo RT = X.findSubType("RT");
        HiddenContentTypes hctRT = chc.compute(RT);
        assertEquals("0=T, 1=RT", hctRT.detailedSortedTypes());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.Spliterator;
            import java.util.function.DoubleConsumer;
            class StreamSpliterators {
                abstract static class SliceSpliterator<T, T_SPLITR extends Spliterator<T>> {
                    T t;
                    T_SPLITR tSplitr;
                    static final class OfDouble extends OfPrimitive<Double, Spliterator.OfDouble, DoubleConsumer>
                                    implements Spliterator.OfDouble {
            
                    }
                    abstract static class OfPrimitive<
                            T,
                            T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>,
                            T_CONS>
                        extends SliceSpliterator<T, T_SPLITR>
                        implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
                        T t;
                        T_SPLITR tSplitr;
                        T_CONS tCons;
                    }
                }
            }
            """;

    @DisplayName("spliterator, from source")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        TypeInfo SliceSpliterator = X.findSubType("SliceSpliterator");
        test(SliceSpliterator);
    }

    @DisplayName("spliterator, from bytecode")
    @Test
    public void test2b() {
        TypeInfo StreamSpliterators = javaInspector.compiledTypesManager()
                .getOrLoad("java.util.stream.StreamSpliterators", javaInspector.javaBase());
        TypeInfo SliceSpliterator = StreamSpliterators.findSubType("SliceSpliterator");
        test(SliceSpliterator);
    }

    private void test(TypeInfo SliceSpliterator) {
        ComputeHiddenContent chc = new ComputeHiddenContent(javaInspector.runtime());

        {
            TypeInfo doubleTi = runtime.doubleParameterizedType().toBoxed(javaInspector.runtime());
            assertEquals("java.lang.Double", doubleTi.fullyQualifiedName());
            assertFalse(doubleTi.isExtensible());
            HiddenContentTypes hctDouble = chc.compute(doubleTi);
            assertEquals("", hctDouble.detailedSortedTypes());
            doubleTi.analysis().set(HIDDEN_CONTENT_TYPES, hctDouble);
        }
        {
            TypeInfo doubleConsumer = javaInspector.compiledTypesManager().get(DoubleConsumer.class);
            assertTrue(doubleConsumer.isExtensible());
            HiddenContentTypes hctDoubleConsumer = chc.compute(doubleConsumer);
            assertEquals("", hctDoubleConsumer.detailedSortedTypes());
            doubleConsumer.analysis().set(HIDDEN_CONTENT_TYPES, hctDoubleConsumer);
        }

        TypeInfo spliterator = javaInspector.compiledTypesManager().get(Spliterator.class);
        HiddenContentTypes hctSpliterator = chc.compute(spliterator);
        assertEquals("0=T", hctSpliterator.detailedSortedTypes());
        assertEquals("T=0", hctSpliterator.detailedSortedTypeToIndex());
        spliterator.analysis().set(HIDDEN_CONTENT_TYPES, hctSpliterator);

        TypeInfo spliteratorOfPrimitive = spliterator.findSubType("OfPrimitive");
        HiddenContentTypes hctSpliteratorOfPrimitive = chc.compute(spliteratorOfPrimitive);
        assertEquals("0=T, 1=T_CONS, 2=T_SPLITR", hctSpliteratorOfPrimitive.detailedSortedTypes());
        assertEquals("T=0, T_CONS=1, T_SPLITR=2", hctSpliteratorOfPrimitive.detailedSortedTypeToIndex());
        spliteratorOfPrimitive.analysis().set(HIDDEN_CONTENT_TYPES, hctSpliteratorOfPrimitive);

        TypeInfo spliteratorOfDouble = spliterator.findSubType("OfDouble");
        HiddenContentTypes hctSpliteratorOfDouble = chc.compute(spliteratorOfDouble);
        // because of the recursive relationship in the type parameters, OfDouble refers to itself
        assertEquals("0=DoubleConsumer, 1=OfDouble", hctSpliteratorOfDouble.detailedSortedTypes());
        assertEquals("DoubleConsumer=0, OfDouble=1, T_CONS=0, T_SPLITR=1", hctSpliteratorOfDouble.detailedSortedTypeToIndex());
        spliteratorOfDouble.analysis().set(HIDDEN_CONTENT_TYPES, hctSpliteratorOfDouble);

        HiddenContentTypes hctSliceSpliterator = chc.compute(SliceSpliterator);
        assertEquals("0=T, 1=T_SPLITR", hctSliceSpliterator.detailedSortedTypes());
        assertEquals("T=0, T_SPLITR=1", hctSliceSpliterator.detailedSortedTypeToIndex());
        SliceSpliterator.analysis().set(HIDDEN_CONTENT_TYPES, hctSliceSpliterator);

        TypeInfo SliceSpliteratorOfPrimitive = SliceSpliterator.findSubType("OfPrimitive");
        HiddenContentTypes hctSliceSpliteratorOfPrimitive = chc.compute(SliceSpliteratorOfPrimitive);
        assertEquals("0=T, 1=T_SPLITR, 2=T_CONS", hctSliceSpliteratorOfPrimitive.detailedSortedTypes());
        assertEquals("T=0, T_CONS=2, T_SPLITR=1",
                hctSliceSpliteratorOfPrimitive.detailedSortedTypeToIndex());
        SliceSpliteratorOfPrimitive.analysis().set(HIDDEN_CONTENT_TYPES, hctSliceSpliteratorOfPrimitive);

        assertEquals(3, SliceSpliteratorOfPrimitive.typeParameters().size());
        ParameterizedType pt = SliceSpliteratorOfPrimitive.asParameterizedType();
        assertEquals(3, pt.parameters().size());

        TypeInfo SliceSpliteratorOfDouble = SliceSpliterator.findSubType("OfDouble");
        HiddenContentTypes hctSliceSpliteratorOfDouble = chc.compute(SliceSpliteratorOfDouble);
        assertEquals("0=DoubleConsumer, 1=OfDouble", hctSliceSpliteratorOfDouble.detailedSortedTypes());
        assertEquals("DoubleConsumer=0, OfDouble=1, T_CONS=0, T_SPLITR=1", hctSliceSpliteratorOfDouble.detailedSortedTypeToIndex());
        SliceSpliteratorOfDouble.analysis().set(HIDDEN_CONTENT_TYPES, hctSliceSpliteratorOfDouble);
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.Collection;
            class X {
                static <T> void add2(Collection<T> ts, T t1, T t2) {
                    ts.add(t1);
                    ts.add(t2);
                }
            }
            """;

    @DisplayName("hct of static method with type parameters")
    @Test
    public void test3() {
        ComputeHiddenContent chc = new ComputeHiddenContent(javaInspector.runtime());
        TypeInfo X = javaInspector.parse(INPUT3);
        HiddenContentTypes hct = chc.compute(X);
        assertEquals("", hct.detailedSortedTypes());
        MethodInfo add2 = X.findUniqueMethod("add2", 3);
        HiddenContentTypes hctMethod = chc.compute(hct, add2);
        assertEquals(" - 0=T, 1=Collection", hctMethod.detailedSortedTypes());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.Collection;
            class X {
                static <T> void add2(Collection<T> ts, T[] tArray, int[] intArray) {
                    for (int i=0; i<intArray.length; i++) {
                        ts.add(tArray[intArray[i]]);
                    }
                }
            }
            """;

    @DisplayName("hct of static method with type parameters and arrays")
    @Test
    public void test4() {
        ComputeHiddenContent chc = new ComputeHiddenContent(javaInspector.runtime());
        TypeInfo X = javaInspector.parse(INPUT3);
        HiddenContentTypes hct = chc.compute(X);
        assertEquals("", hct.detailedSortedTypes());
        MethodInfo add2 = X.findUniqueMethod("add2", 3);
        HiddenContentTypes hctMethod = chc.compute(hct, add2);
        assertEquals(" - 0=T, 1=Collection", hctMethod.detailedSortedTypes());
    }
}