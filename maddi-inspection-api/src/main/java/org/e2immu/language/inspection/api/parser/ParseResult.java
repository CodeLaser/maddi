package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/*
result of successful parsing.

computed from a Summary object

 */
public interface ParseResult {

    ModuleInfo moduleInfo(SourceSet sourceSet);

    Map<SourceSet, ModuleInfo> sourceSetToModuleInfoMap();

    Map<String, SourceSet> sourceSetsByName();

    default MethodInfo findMethod(String methodFqn) {
        return findMethod(methodFqn, true);
    }

    MethodInfo findMethod(String methodFqn, boolean complain);

    List<FieldInfo> findMostLikelyField(String name);

    List<String> findMostLikelyPackage(String name);

    List<TypeInfo> findMostLikelyType(String name);

    // primarily intended for testing; use 'typeByFullyQualifiedName' in production
    TypeInfo findType(String typeFqn);

    Set<TypeInfo> primaryTypes();

    TypeInfo firstType();

    List<MethodInfo> findMostLikelyMethod(String name);

    Map<String, Set<TypeInfo>> primaryTypesPerPackage();

    int size();

    List<TypeInfo> typeByFullyQualifiedName(String fqn);

    Set<TypeInfo> primaryTypesOfPackage(String packageName);

    /**
     * Given type A, return all B which implement or extend A, either directly (children) or recursively (descendants).
     * Only available (precomputed) for source types.
     *
     * @param typeInfo the starting type (A).
     * @param recurse  If false, only direct children are returned. if true, all descendants are returned recursively.
     * @return all descendants, never null.
     */
    Set<TypeInfo> descendants(TypeInfo typeInfo, boolean recurse);
}
