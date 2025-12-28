package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.VirtualFieldTranslationMap;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TestVirtualFieldTranslationMap extends CommonTest {

    @DisplayName("translate type parameter")
    @Test
    public void test1() {
        TypeInfo iterable = javaInspector.compiledTypesManager().getOrLoad(Iterable.class);
        TypeParameter iterableTp = iterable.typeParameters().getFirst();

        TypeInfo arrayList = javaInspector.compiledTypesManager().getOrLoad(ArrayList.class);
        TypeParameter arrayListTp = arrayList.typeParameters().getFirst();

        VirtualFieldTranslationMap ftm = new VirtualFieldTranslationMapImpl(null, runtime);
        ftm.put(arrayListTp, iterableTp.asParameterizedType());

        ReturnVariable rv = new ReturnVariableImpl(arrayList.findUniqueMethod("get", 1));
        assertEquals("Type param E", rv.parameterizedType().toString());

        VariableExpression veRv = runtime.newVariableExpression(rv);
        VariableExpression tVeRv = (VariableExpression) veRv.translate(ftm);
        assertEquals("return get", tVeRv.toString());
        assertEquals("Type param T", tVeRv.parameterizedType().toString());
    }

    @DisplayName("translate array VF")
    @Test
    public void test2() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        TypeInfo optional = javaInspector.compiledTypesManager().getOrLoad(Optional.class);
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);
        ParameterizedType tpArray = optional.asParameterizedType().parameters().getFirst().copyWithArrays(1);
        ParameterizedType listTpArray = runtime.newParameterizedType(list, List.of(tpArray));
        assertEquals("java.util.List<T[]>", listTpArray.descriptor());
        VirtualFieldComputer.VfTm vfTmListTpArray = vfc.compute(listTpArray, true);
        VirtualFields vfListTpArray = vfTmListTpArray.virtualFields();
        assertEquals("§m - T[][] §tss", vfListTpArray.toString());

        TypeParameter list0 = list.typeParameters().getFirst();
        {
            // replace type parameter by other type parameter
            VirtualFieldTranslationMap ftm = new VirtualFieldTranslationMapImpl(vfc, runtime);
            ftm.put(optional.typeParameters().getFirst(), list0.asParameterizedType());

            FieldReference fr = runtime.newFieldReference(vfListTpArray.hiddenContent());
            Variable tFr = ftm.translateVariableRecursively(fr);
            assertEquals("§ess", tFr.simpleName());
            assertEquals("Type param E[][]", tFr.parameterizedType().toString());
            assertEquals("E=TP#0 in List []", tFr.parameterizedType().typeParameter().toStringWithTypeBounds());
        }
        {
            // inject array VF into array VF
            VirtualFieldTranslationMap ftm = new VirtualFieldTranslationMapImpl(vfc, runtime);
            ftm.put(optional.typeParameters().getFirst(), list0.asParameterizedType().copyWithArrays(1));

            FieldReference fr = runtime.newFieldReference(vfListTpArray.hiddenContent());
            Variable tFr = ftm.translateVariableRecursively(fr);
            assertEquals("§esss", tFr.simpleName());
            assertEquals("Type param E[][][]", tFr.parameterizedType().toString());
            assertEquals("E=TP#0 in List []", tFr.parameterizedType().typeParameter().toStringWithTypeBounds());
        }
        {
            // inject ordinary type into array VF
            VirtualFieldTranslationMap ftm = new VirtualFieldTranslationMapImpl(vfc, runtime);
            ftm.put(optional.typeParameters().getFirst(), runtime.stringParameterizedType());

            FieldReference fr = runtime.newFieldReference(vfListTpArray.hiddenContent());
            Variable tFr = ftm.translateVariableRecursively(fr);
            assertEquals("§$ss", tFr.simpleName());
            assertEquals("Type String[][]", tFr.parameterizedType().toString());
        }
        {
            // inject container VF into array VF
            TypeInfo map = javaInspector.compiledTypesManager().getOrLoad(Map.class);
            ParameterizedType mapTE = runtime.newParameterizedType(map, List.of(
                    optional.asParameterizedType().parameters().getFirst(),
                    list.asParameterizedType().parameters().getFirst()
            ));
            assertEquals("java.util.Map<T,E>", mapTE.descriptor());
            VirtualFieldComputer.VfTm vfTmMapTE = vfc.compute(mapTE, true);
            VirtualFields vfMapTE = vfTmMapTE.virtualFields();
            assertEquals("§m - TE[] §tes", vfMapTE.toString());

            VirtualFieldTranslationMap ftm = new VirtualFieldTranslationMapImpl(vfc, runtime);
            ftm.put(optional.typeParameters().getFirst(), vfMapTE.hiddenContent().type());

            FieldReference fr = runtime.newFieldReference(vfListTpArray.hiddenContent());
            Variable tFr = ftm.translateVariableRecursively(fr);
            assertEquals("§tesss", tFr.simpleName());
            assertEquals("Type java.util.Map.TE[][][]", tFr.parameterizedType().toString());
            assertSame(VirtualFieldComputer.VIRTUAL_FIELD, tFr.parameterizedType().typeInfo().typeNature());
        }
    }

    @DisplayName("translate container VF")
    @Test
    public void test3() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);

        TypeInfo map = javaInspector.compiledTypesManager().getOrLoad(Map.class);
        TypeInfo optional = javaInspector.compiledTypesManager().getOrLoad(Optional.class);
        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);
        ParameterizedType mapTE = runtime.newParameterizedType(map, List.of(
                optional.asParameterizedType().parameters().getFirst(),
                list.asParameterizedType().parameters().getFirst()
        ));
        assertEquals("java.util.Map<T,E>", mapTE.descriptor());
        VirtualFieldComputer.VfTm vfTmMapTE = vfc.compute(mapTE, true);
        VirtualFields vfMapTE = vfTmMapTE.virtualFields();
        assertEquals("§m - TE[] §tes", vfMapTE.toString());

        TypeParameter map0 = map.typeParameters().getFirst();
        {
            // replace type parameter
            VirtualFieldTranslationMap ftm = new VirtualFieldTranslationMapImpl(vfc, runtime);
            ftm.put(optional.typeParameters().getFirst(), map0.asParameterizedType());

            FieldReference fr = runtime.newFieldReference(vfMapTE.hiddenContent());
            Variable tFr = ftm.translateVariableRecursively(fr);
            assertEquals("§kes", tFr.simpleName());
            assertEquals("Type java.util.Map.KE[]", tFr.parameterizedType().toString());
        }
        {
            // inject array VF
            VirtualFieldTranslationMap ftm = new VirtualFieldTranslationMapImpl(vfc, runtime);
            ftm.put(optional.typeParameters().getFirst(), map0.asParameterizedType());
            TypeParameter map1 = map.typeParameters().getLast();
            ftm.put(list.typeParameters().getFirst(), map1.asParameterizedType().copyWithArrays(1));

            FieldReference fr = runtime.newFieldReference(vfMapTE.hiddenContent());
            Variable tFr = ftm.translateVariableRecursively(fr);
            assertEquals("§kvss", tFr.simpleName());
            assertEquals("Type java.util.Map.KVS[]", tFr.parameterizedType().toString());
        }
        {
            // inject ordinary type as the second parameter
            VirtualFieldTranslationMap ftm = new VirtualFieldTranslationMapImpl(vfc, runtime);
            ftm.put(list.typeParameters().getFirst(), runtime.stringParameterizedType());

            FieldReference fr = runtime.newFieldReference(vfMapTE.hiddenContent());
            Variable tFr = ftm.translateVariableRecursively(fr);
            assertEquals("§t$s", tFr.simpleName());
            assertEquals("Type java.util.Map.T$[]", tFr.parameterizedType().toString());
        }
        {
            // inject ordinary type array String[] as the second parameter
            VirtualFieldTranslationMap ftm = new VirtualFieldTranslationMapImpl(vfc, runtime);
            ftm.put(list.typeParameters().getFirst(), runtime.stringParameterizedType().copyWithArrays(1));

            FieldReference fr = runtime.newFieldReference(vfMapTE.hiddenContent());
            Variable tFr = ftm.translateVariableRecursively(fr);
            assertEquals("§t$ss", tFr.simpleName());
            assertEquals("Type java.util.Map.T$S[]", tFr.parameterizedType().toString());
        }
        {
            TypeParameter X = runtime.newTypeParameter(0, "X", runtime.objectTypeInfo());
            X.builder().commit();
            TypeParameter Y = runtime.newTypeParameter(1, "Y", runtime.objectTypeInfo());
            Y.builder().commit();
            // inject other vf as the second parameter
            ParameterizedType mapXY = runtime.newParameterizedType(map, List.of(X.asParameterizedType(),
                    Y.asParameterizedType()));

            assertEquals("java.util.Map<X,Y>", mapXY.descriptor());
            VirtualFieldComputer.VfTm vfTmMapXY = vfc.compute(mapXY, true);
            VirtualFields vfMapXY = vfTmMapXY.virtualFields();
            assertEquals("§m - XY[] §xys", vfMapXY.toString());

            VirtualFieldTranslationMap ftm = new VirtualFieldTranslationMapImpl(vfc, runtime);
            ftm.put(list.typeParameters().getFirst(), vfMapXY.hiddenContent().type());

            FieldReference fr = runtime.newFieldReference(vfMapTE.hiddenContent());
            Variable tFr = ftm.translateVariableRecursively(fr);
            assertEquals("§txyss", tFr.simpleName());
            assertEquals("Type java.util.Map.TXYS[]", tFr.parameterizedType().toString());
        }
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.Set;
            import java.util.HashSet;
            import java.util.List;
            public class C<X> {
                interface I { }
                class II implements I { }
                Set<II> set = new HashSet<>();
                void add(II ii) {
                    set.add(ii);
                }
                void add2(II ii) {
                    add(ii);
                }
                void method(List<II> list) {
                   list.forEach(j -> add(j));
                }
            }
            """;

    @DisplayName("concrete set: no virtual fields, but actual ones")
    @Test
    public void test4() {
        TypeInfo C = javaInspector.parse(INPUT4);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);

        TypeInfo set = javaInspector.compiledTypesManager().getOrLoad(Set.class);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        VirtualFields vfSet = vfc.compute(set);
        assertEquals("§m - E[] §es", vfSet.toString());

        TypeInfo II = C.findSubType("II");
        assertEquals("/ - /", vfc.compute(C).toString());
        ParameterizedType setII = runtime.newParameterizedType(set, List.of(II.asParameterizedType()));
        VirtualFieldComputer.VfTm vfTm = vfc.compute(setII, true);
        assertEquals("§m - /", vfTm.virtualFields().toString());

        FieldReference fr = runtime.newFieldReference(vfSet.hiddenContent());
        Variable translated = vfTm.formalToConcrete().translateVariableRecursively(fr);
        assertEquals("this.§$s", translated.toString());
    }
}
