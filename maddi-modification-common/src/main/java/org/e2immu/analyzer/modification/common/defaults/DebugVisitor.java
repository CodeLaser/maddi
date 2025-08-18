package org.e2immu.analyzer.modification.common.defaults;

import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.util.internal.graph.G;

import java.util.List;
import java.util.Map;

public interface DebugVisitor {

    default void allTypes(List<TypeInfo> allTypes) {
    }

    default void dataMapAfterFieldMethodAnalyzer(Map<Element, ShallowAnalyzer.InfoData> dataMap) {
    }

    default void dataMapAfterTypeAnalyzer(Map<Element, ShallowAnalyzer.InfoData> dataMap) {
    }

    default void inputTypes(List<TypeInfo> types) {
    }

    default void sortedLinearized(List<TypeInfo> sorted) {
    }

    default void typeGraph(G<TypeInfo> typeGraph) {
    }
}
