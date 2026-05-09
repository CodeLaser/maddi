package org.e2immu.language.inspection.openjdk;

import com.sun.tools.javac.code.Symbol;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class TypeData {
    private final Map<String, SourceSet> sourceSetMap = new HashMap<>();
    private final Map<String, TypeInfo> singleTypeForFQN = new HashMap<>();
    private final Map<Symbol.MethodSymbol, MethodInfo> methodSymbolMap = new IdentityHashMap<>();

    public SourceSet getSourceSet(String name) {
        return sourceSetMap.get(name);
    }

    public void put(SourceSet sourceSet) {
        sourceSetMap.put(sourceSet.name(), sourceSet);
    }

    public TypeInfo getType(String fullyQualifiedName) {
        return singleTypeForFQN.get(fullyQualifiedName);
    }

    public void put(TypeInfo typeInfo) {
        singleTypeForFQN.put(typeInfo.fullyQualifiedName(), typeInfo);
    }

    public void put(Symbol.MethodSymbol methodSymbol, MethodInfo methodInfo) {
        methodSymbolMap.put(methodSymbol, methodInfo);
    }

    public MethodInfo getMethod(Symbol.MethodSymbol methodSymbol) {
        return methodSymbolMap.get(methodSymbol);
    }
}
