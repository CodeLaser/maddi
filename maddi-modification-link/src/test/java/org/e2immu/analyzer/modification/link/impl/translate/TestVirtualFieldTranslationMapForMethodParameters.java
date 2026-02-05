package org.e2immu.analyzer.modification.link.impl.translate;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestVirtualFieldTranslationMapForMethodParameters extends CommonTest {

    @DisplayName("findValue with Entry.comparingByValue")
    @Test
    public void test1() {
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        VirtualFieldTranslationMapForMethodParameters tm = new VirtualFieldTranslationMapForMethodParameters(vfc, runtime);

        // method is: static <K,V extends Comparable<? super V>> Comparator<Map.Entry<K,V>> Entry.comparingByValue()

        // Type java.util.Comparator<java.util.Map.Entry<K,V extends Comparable<? super V>>>
        // Type java.util.Comparator<java.util.Map.Entry<CompiledTypesManager.TypeData,Integer>>
        // K=TP#0 in Entry.comparingByValue is what we have
        // result of Generics.translateMap:
        //     T=TP#0 in Comparator -> Type java.util.Map.Entry<CompiledTypesManager.TypeData,Integer>
    }

}
