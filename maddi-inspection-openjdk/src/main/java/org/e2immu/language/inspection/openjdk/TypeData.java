package org.e2immu.language.inspection.openjdk;

import com.sun.tools.javac.code.Symbol;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TypeData implements CompiledTypesManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TypeData.class);

    private final SourceSet ANY = new SourceSetImpl("any source set will do", List.of(), URI.create("file:/"),
            StandardCharsets.UTF_8, false, false, false, false, false,
            Set.of(), Set.of());

    private final SourceSet javaBase;
    private final Map<String, SourceSet> sourceSetMap = new HashMap<>();
    private final Map<String, TypeInfo> singleTypeForFQN = new HashMap<>();
    private final Map<Symbol.MethodSymbol, MethodInfo> methodSymbolMap = new IdentityHashMap<>();
    private final Map<Symbol.VarSymbol, FieldInfo> varSymbolMap = new IdentityHashMap<>();
    private final Map<String, Map<String, TypeParameter>> tmpMethodTypeParameterMap = new HashMap<>();

    public TypeData(SourceSet javaBase) {
        this.javaBase = javaBase;
    }

    @Override
    public SourceSet javaBase() {
        return javaBase;
    }

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


    @Override
    public TypeData typeDataOrNull(String fqn,
                                   SourceSet sourceSetOfRequest,
                                   SourceSet nearestSourceSet,
                                   boolean complainSingle) {
        throw new UnsupportedOperationException(
                "This is a communications method between ByteCodeInspector and CompiledTypesManager");
    }

    @Override
    public void addTypeInfo(SourceFile sourceFile, TypeInfo typeInfo) {
        put(typeInfo); // TODO source file
    }

    @Override
    public TypeInfo get(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        return getType(fullyQualifiedName); // TODO sourceSet
    }

    @Override
    public TypeInfo getOrLoad(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        throw new UnsupportedOperationException("NYI"); // FIXME load needs access to ClassSymbolScanner
    }
}
