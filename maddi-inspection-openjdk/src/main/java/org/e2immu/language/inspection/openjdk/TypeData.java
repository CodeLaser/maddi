package org.e2immu.language.inspection.openjdk;

import com.sun.tools.javac.code.Symbol;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class TypeData {
    private final Map<String, SourceSet> sourceSetMap = new HashMap<>();
    private final Map<String, TypeInfo> singleTypeForFQN = new HashMap<>();
    private final Map<Symbol.MethodSymbol, MethodInfo> methodSymbolMap = new IdentityHashMap<>();
    private final Map<Symbol.VarSymbol, FieldInfo> varSymbolMap = new IdentityHashMap<>();

    private final Map<String, Map<String, TypeParameter>> tmpMethodTypeParameterMap = new HashMap<>();

    public void clearTmpMethodTypeParameterMap(String typeFqn) {
        tmpMethodTypeParameterMap.remove(typeFqn);
    }

    public void putTmpMethodTypeParameter(String typeFqn, String methodTpName, TypeParameter methodTp) {
        tmpMethodTypeParameterMap.computeIfAbsent(typeFqn, _ -> new HashMap<>()).put(methodTpName, methodTp);
    }

    public TypeParameter getTmpMethodTypeParameter(String typeFqn, String methodTpName) {
        Map<String, TypeParameter> typeParameterMap = tmpMethodTypeParameterMap.get(typeFqn);
        return typeParameterMap == null ? null : typeParameterMap.get(methodTpName);
    }

    public SourceSet getSourceSet(String name) {
        return sourceSetMap.get(name);
    }

    public void put(SourceSet sourceSet) {
        SourceSet prev = sourceSetMap.put(sourceSet.name(), sourceSet);
        assert prev == null : "Duplicating SourceSet " + sourceSet;
    }

    public TypeInfo getType(String fullyQualifiedName) {
        return singleTypeForFQN.get(fullyQualifiedName);
    }

    public void put(TypeInfo typeInfo) {
        TypeInfo prev = singleTypeForFQN.put(typeInfo.fullyQualifiedName(), typeInfo);
        assert prev == null : "Duplicating TypeInfo " + typeInfo;
    }

    public void put(String anonymousTypeName, TypeInfo typeInfo) {
        TypeInfo prev = singleTypeForFQN.put(anonymousTypeName, typeInfo);
        assert prev == null : "Duplicating TypeInfo " + typeInfo;
    }

    public void put(Symbol.MethodSymbol methodSymbol, MethodInfo methodInfo) {
        MethodInfo prev = methodSymbolMap.put(methodSymbol, methodInfo);
        assert prev == null : "Duplicating MethodInfo " + methodInfo;
    }

    public MethodInfo getMethod(Symbol.MethodSymbol methodSymbol) {
        return methodSymbolMap.get(methodSymbol);
    }

    public void put(Symbol.VarSymbol varSymbol, FieldInfo fieldInfo) {
        FieldInfo prev = varSymbolMap.put(varSymbol, fieldInfo);
        assert prev == null : "Duplicating FieldInfo " + fieldInfo;
    }

    public FieldInfo getField(Symbol.VarSymbol varSymbol) {
        return varSymbolMap.get(varSymbol);
    }

}
