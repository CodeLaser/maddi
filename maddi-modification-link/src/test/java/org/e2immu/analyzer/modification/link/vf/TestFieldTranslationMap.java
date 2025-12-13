package org.e2immu.analyzer.modification.link.vf;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFieldTranslationMap extends CommonTest {

    @Test
    public void test1() {
        TypeInfo iterable = javaInspector.compiledTypesManager().getOrLoad(Iterable.class);
        TypeParameter iterableTp = iterable.typeParameters().getFirst();

        TypeInfo arrayList = javaInspector.compiledTypesManager().getOrLoad(ArrayList.class);
        TypeParameter arrayListTp = arrayList.typeParameters().getFirst();

        VirtualFieldTranslationMap ftm = new VirtualFieldTranslationMap(runtime);

        ftm.put(arrayListTp, iterableTp, 0);

        ReturnVariable rv = new ReturnVariableImpl(arrayList.findUniqueMethod("get", 1));
        assertEquals("Type param E", rv.parameterizedType().toString());

        VariableExpression veRv = runtime.newVariableExpression(rv);
        VariableExpression tVeRv = (VariableExpression) veRv.translate(ftm);
        assertEquals("return get", tVeRv.toString());
        assertEquals("Type param T", tVeRv.parameterizedType().toString());
    }

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
        assertEquals("$m - T[][] tss", vfListTpArray.toString());

        {
            VirtualFieldTranslationMap ftm = new VirtualFieldTranslationMap(runtime);
            // T in optional -> E in List
            ftm.put(optional.typeParameters().getFirst(), list.typeParameters().getFirst(), 0);

            FieldReference fr = runtime.newFieldReference(vfListTpArray.hiddenContent());
            Variable tFr = ftm.translateVariableRecursively(fr);
            assertEquals("ess", tFr.simpleName());
            assertEquals("Type param E[][]", tFr.parameterizedType().toString());
            assertEquals("E=TP#0 in List []", tFr.parameterizedType().typeParameter().toStringWithTypeBounds());
        }
        {
            VirtualFieldTranslationMap ftm = new VirtualFieldTranslationMap(runtime);
            // T in optional -> E in List with one extra array
            ftm.put(optional.typeParameters().getFirst(), list.typeParameters().getFirst(), 1);

            FieldReference fr = runtime.newFieldReference(vfListTpArray.hiddenContent());
            Variable tFr = ftm.translateVariableRecursively(fr);
            assertEquals("esss", tFr.simpleName());
            assertEquals("Type param E[][][]", tFr.parameterizedType().toString());
            assertEquals("E=TP#0 in List []", tFr.parameterizedType().typeParameter().toStringWithTypeBounds());
        }
    }

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
        assertEquals("$m - TE[] tes", vfMapTE.toString());

        {
            VirtualFieldTranslationMap ftm = new VirtualFieldTranslationMap(runtime);
            ftm.put(optional.typeParameters().getFirst(), map.typeParameters().getFirst(), 0);

            FieldReference fr = runtime.newFieldReference(vfMapTE.hiddenContent());
            Variable tFr = ftm.translateVariableRecursively(fr);
            assertEquals("kes", tFr.simpleName());
            assertEquals("Type java.util.Map.KE[]", tFr.parameterizedType().toString());
        }
        {
            VirtualFieldTranslationMap ftm = new VirtualFieldTranslationMap(runtime);
            ftm.put(optional.typeParameters().getFirst(), map.typeParameters().getFirst(), 0);
            ftm.put(list.typeParameters().getFirst(), map.typeParameters().getLast(), 1);

            FieldReference fr = runtime.newFieldReference(vfMapTE.hiddenContent());
            Variable tFr = ftm.translateVariableRecursively(fr);
            assertEquals("kvss", tFr.simpleName());
            assertEquals("Type java.util.Map.KVS[]", tFr.parameterizedType().toString());
        }
    }
}
