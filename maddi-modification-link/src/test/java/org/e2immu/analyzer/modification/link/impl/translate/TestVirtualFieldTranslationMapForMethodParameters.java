package org.e2immu.analyzer.modification.link.impl.translate;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.variable.VirtualFieldTranslationMap;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestVirtualFieldTranslationMapForMethodParameters extends CommonTest {

    @DisplayName("findValue with Collectors.toList")
    @Test
    public void test1() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        VirtualFieldTranslationMapForMethodParameters tm = new VirtualFieldTranslationMapForMethodParameters(vfc, runtime);

        // method is: static <T> Collector<T,?,List<T>> toList()

        // Type java.util.stream.Collector<T,?,java.util.List<T>>
        // Type java.util.stream.Collector<String,?,java.util.List<String>>
        // T=TP#0 in Collectors.toList is what we have
        // result of Generics.translateMap:
        //     T=TP#0 in Collectors.toList -> Type String

        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(List.class);
        TypeInfo collectors = javaInspector.compiledTypesManager().getOrLoad(Collectors.class);
        MethodInfo toList = collectors.findUniqueMethod("toList", 0);
        assertEquals("Type java.util.stream.Collector<T,?,java.util.List<T>>",
                toList.returnType().toString());
        TypeInfo collector = toList.returnType().typeInfo();
        TypeParameter t = toList.typeParameters().getFirst();
        ParameterizedType concrete = runtime.newParameterizedType(collector, List.of(
                runtime.newParameterizedType(t, 0, null),
                runtime.parameterizedTypeWildcard(),
                runtime.newParameterizedType(list, List.of(runtime.stringParameterizedType()))
        ));
        assertEquals("Type java.util.stream.Collector<T,?,java.util.List<String>>", concrete.toString());
        MethodCall mc = runtime.newMethodCallBuilder()
                .setMethodInfo(toList)
                .setConcreteReturnType(concrete)
                .setParameterExpressions(List.of())
                .setObject(runtime.newTypeExpression(collectors.asSimpleParameterizedType(), runtime.diamondNo()))
                .setSource(runtime.noSource())
                .build();
        VirtualFieldTranslationMap vfTm = tm.staticCall(mc);
        assertEquals("T=TP#0 in Collectors.toList [] --> String", vfTm.toString());
    }

    @DisplayName("findValue with Entry.comparingByValue")
    @Test
    public void test2() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        VirtualFieldTranslationMapForMethodParameters tm = new VirtualFieldTranslationMapForMethodParameters(vfc, runtime);

        // method is: static <K,V extends Comparable<? super V>> Comparator<Map.Entry<K,V>> Entry.comparingByValue()

        // Type java.util.Comparator<java.util.Map.Entry<K,V extends Comparable<? super V>>>
        // Type java.util.Comparator<java.util.Map.Entry<CompiledTypesManager.TypeData,Integer>>
        // K=TP#0 in Entry.comparingByValue is what we have
        // result of Generics.translateMap:
        //     T=TP#0 in Comparator -> Type java.util.Map.Entry<CompiledTypesManager.TypeData,Integer>

        TypeInfo map = javaInspector.compiledTypesManager().getOrLoad(Map.class);
        TypeInfo entry = map.findSubType("Entry");
        MethodInfo comparingByValue = entry.findUniqueMethod("comparingByValue", 0);
        assertEquals("Type java.util.Comparator<java.util.Map.Entry<K,V extends Comparable<? super V>>>",
                comparingByValue.returnType().toString());
        TypeInfo comparator = comparingByValue.returnType().typeInfo();
        ParameterizedType concrete = runtime.newParameterizedType(comparator, List.of(
                runtime.newParameterizedType(entry, List.of(runtime.stringParameterizedType(), runtime.integerTypeInfo().asParameterizedType()))
        ));
        assertEquals("Type java.util.Comparator<java.util.Map.Entry<String,Integer>>", concrete.toString());
        MethodCall mc = runtime.newMethodCallBuilder()
                .setMethodInfo(comparingByValue)
                .setConcreteReturnType(concrete)
                .setParameterExpressions(List.of())
                .setObject(runtime.newTypeExpression(entry.asSimpleParameterizedType(), runtime.diamondNo()))
                .setSource(runtime.noSource())
                .build();
        VirtualFieldTranslationMap vfTm = tm.staticCall(mc);
        assertEquals("", vfTm.toString());
    }

}
