package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.TypeExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.parser.*;

import java.util.*;
import java.util.stream.Collectors;

/*
Code that goes through the type hierarchy to find the candidate methods or constructors
for method/constructor resolution. Further resolution will only restrict based on parameter types.
 */
public class ListMethodAndConstructorCandidates {
    private final Runtime runtime;
    private final StaticImportMap staticImportMap;

    public enum ScopeNature {
        ABSENT, STATIC, INSTANCE,
    }

    public ListMethodAndConstructorCandidates(Runtime runtime, StaticImportMap staticImportMap) {
        this.staticImportMap = staticImportMap;
        this.runtime = runtime;
    }

    public static final int IGNORE_PARAMETER_NUMBERS = -1;

    public Map<MethodTypeParameterMap, Integer> resolveConstructor(ParameterizedType formalType,
                                                                   ParameterizedType concreteType,
                                                                   int parametersPresented,
                                                                   Map<NamedType, ParameterizedType> typeMap) {
        List<TypeInfo> types = extractTypeInfo(concreteType != null ? concreteType : formalType, typeMap);
        // there's only one situation where we can have multiple types; that's multiple type bounds; only the first one can be a class
        TypeInfo typeInfo = types.getFirst();

        return typeInfo.constructors().stream()
                .filter(methodInspection -> parametersPresented == IGNORE_PARAMETER_NUMBERS ||
                                            compatibleNumberOfParameters(methodInspection, parametersPresented))
                .map(mi -> new MethodTypeParameterMapImpl(mi, typeMap))
                .collect(Collectors.toMap(mt -> mt, _ -> 1));
    }

    public void recursivelyResolveOverloadedMethods(ParameterizedType typeOfObject,
                                                    String methodName,
                                                    int parametersPresented,
                                                    boolean decrementWhenNotStatic,
                                                    Map<NamedType, ParameterizedType> typeMap,
                                                    Map<MethodTypeParameterMap, Integer> result,
                                                    ScopeNature scopeNature) {
        recursivelyResolveOverloadedMethods(typeOfObject, methodName, parametersPresented, decrementWhenNotStatic,
                typeMap, result, new HashSet<>(), new HashSet<>(), false, scopeNature, 0);
    }

    private void recursivelyResolveOverloadedMethods(ParameterizedType typeOfObject,
                                                     String methodName,
                                                     int parametersPresented,
                                                     boolean decrementWhenNotStatic,
                                                     Map<NamedType, ParameterizedType> typeMap,
                                                     Map<MethodTypeParameterMap, Integer> result,
                                                     Set<TypeInfo> visited,
                                                     Set<TypeInfo> visitedStatic,
                                                     boolean staticOnly,
                                                     ScopeNature scopeNature,
                                                     int distance) {
        List<TypeInfo> multipleTypeInfoObjects = extractTypeInfo(typeOfObject, typeMap);
        // more than one: only in the rare situation of multiple type bounds
        for (TypeInfo typeInfo : multipleTypeInfoObjects) {
            Set<TypeInfo> types = staticOnly ? visitedStatic : visited;
            if (!types.contains(typeInfo)) {
                if (!staticOnly) visited.add(typeInfo);
                visitedStatic.add(typeInfo);
                resolveOverloadedMethodsSingleType(typeInfo, staticOnly, scopeNature, methodName, parametersPresented,
                        decrementWhenNotStatic, typeMap, result, visited, visitedStatic, distance + 2);
            }
        }
        // it is possible that we find the method in one of the statically imported types... with * import
        // if the method is static, we must be talking about the same type (See Import_10).
        for (TypeInfo typeInfo : staticImportMap.staticAsterisk()) {
            if (!visited.contains(typeInfo) && !visitedStatic.contains(typeInfo)
                && (scopeNature != ScopeNature.STATIC || typeInfo == typeOfObject.bestTypeInfo())) {
                visitedStatic.add(typeInfo);
                resolveOverloadedMethodsSingleType(typeInfo, true, scopeNature, methodName,
                        parametersPresented, decrementWhenNotStatic, typeMap, result, visited, visitedStatic,
                        distance + 1);
            }
        }
        // or import by name
        TypeInfo byName = staticImportMap.getStaticMemberToTypeInfo(methodName);
        if (byName != null && !visited.contains(byName) && !visitedStatic.contains(byName)) {
            // see TestMethodCall10,1
            if (scopeNature != ScopeNature.INSTANCE || multipleTypeInfoObjects.contains(byName)) {
                visitedStatic.add(byName);
                resolveOverloadedMethodsSingleType(byName, true, scopeNature, methodName,
                        parametersPresented, decrementWhenNotStatic, typeMap, result, visited, visitedStatic, distance);
            }
        }
    }

    private void resolveOverloadedMethodsSingleType(TypeInfo typeInfo,
                                                    boolean staticOnly,
                                                    ScopeNature scopeNature,
                                                    String methodName,
                                                    int parametersPresented,
                                                    boolean decrementWhenNotStatic,
                                                    Map<NamedType, ParameterizedType> typeMap,
                                                    Map<MethodTypeParameterMap, Integer> result,
                                                    Set<TypeInfo> visited,
                                                    Set<TypeInfo> visitedStatic,
                                                    int distance) {
        typeInfo.methodStream()
                .filter(m -> m.name().equals(methodName))
                .filter(m -> !staticOnly || m.isStatic())
                .filter(m -> parametersPresented == IGNORE_PARAMETER_NUMBERS ||
                             compatibleNumberOfParameters(m, parametersPresented +
                                                             (!m.isStatic() && decrementWhenNotStatic ? -1 : 0)))
                .forEach(m -> {
                    MethodTypeParameterMap mt = new MethodTypeParameterMapImpl(m, typeMap);
                    int score = distance + (m.isStatic() && scopeNature == ScopeNature.INSTANCE ? 100 : 0);
                    result.merge(mt, score, Integer::min);
                });


        ParameterizedType parentClass = typeInfo.parentClass();
        boolean isJLO = typeInfo.isJavaLangObject();
        assert isJLO || parentClass != null :
                "Parent class of " + typeInfo.fullyQualifiedName() + " is null";
        if (!isJLO) {
            recursivelyResolveOverloadedMethods(parentClass, methodName, parametersPresented, decrementWhenNotStatic,
                    joinMaps(typeMap, parentClass), result, visited, visitedStatic, staticOnly, scopeNature,
                    distance + 1);
        }
        for (ParameterizedType interfaceImplemented : typeInfo.interfacesImplemented()) {
            recursivelyResolveOverloadedMethods(interfaceImplemented, methodName, parametersPresented,
                    decrementWhenNotStatic, joinMaps(typeMap, interfaceImplemented), result, visited, visitedStatic,
                    staticOnly, scopeNature, distance + 2);
        }
        // See UtilityClass_2 for an example where we should go to the static methods of the enclosing type
        if (typeInfo.compilationUnitOrEnclosingType().isRight()) {
            // if I'm in a static subtype, I can only access the static methods of the enclosing type
            boolean onlyStatic = staticOnly || typeInfo.isStatic();
            if (onlyStatic && scopeNature != ScopeNature.INSTANCE ||
                !onlyStatic && scopeNature != ScopeNature.STATIC) {
                ParameterizedType enclosingType = typeInfo.compilationUnitOrEnclosingType().getRight().asParameterizedType();
                recursivelyResolveOverloadedMethods(enclosingType, methodName, parametersPresented, decrementWhenNotStatic,
                        joinMaps(typeMap, enclosingType), result, visited, visitedStatic,
                        onlyStatic, scopeNature, distance + 1);
            }
        }
    }


    private Map<NamedType, ParameterizedType> joinMaps(Map<NamedType, ParameterizedType> previous,
                                                       ParameterizedType target) {
        HashMap<NamedType, ParameterizedType> res = new HashMap<>(previous);
        Map<NamedType, ParameterizedType> initialTypeParameterMap = target.initialTypeParameterMap();
        if (initialTypeParameterMap != null) res.putAll(initialTypeParameterMap);
        return res;
    }

    private boolean compatibleNumberOfParameters(MethodInfo m, int parametersPresented) {
        int declared = m.parameters().size();
        if (declared == 0) return parametersPresented == 0;
        boolean lastIsVarArgs = m.parameters().get(declared - 1).isVarArgs();
        if (lastIsVarArgs) return parametersPresented >= declared - 1;
        return parametersPresented == declared;
    }


    private List<TypeInfo> extractTypeInfo(ParameterizedType typeOfObject, Map<NamedType, ParameterizedType> typeMap) {
        TypeInfo typeInfo;
        if (typeOfObject.typeInfo() == null) {
            if (typeOfObject.typeParameter() == null) {
                return List.of(runtime.objectTypeInfo());
            }
            ParameterizedType pt = typeMap.get(typeOfObject.typeParameter());
            if (pt == null) {
                // rather than give an exception here, we replace t by the type that it extends, so that we can find those methods
                // in the case that there is no explicit extension/super, we replace it by the implicit Object
                List<ParameterizedType> typeBounds = typeOfObject.typeParameter().typeBounds();
                if (!typeBounds.isEmpty()) {
                    return typeBounds.stream().flatMap(bound -> extractTypeInfo(bound, typeMap).stream()).collect(Collectors.toList());
                } else {
                    typeInfo = runtime.objectTypeInfo();
                }
            } else {
                typeInfo = pt.typeInfo();
            }
        } else if (typeOfObject.typeInfo().isPrimitiveExcludingVoid() && typeOfObject.arrays() > 0) {
            typeInfo = runtime.objectTypeInfo();
        } else {
            typeInfo = typeOfObject.typeInfo();
        }
        assert typeInfo != null;
        return List.of(typeInfo);
    }


    public record Scope(Expression expression,
                        ParameterizedType type,
                        ScopeNature nature,
                        TypeParameterMap typeParameterMap) {

        public boolean objectIsImplicit() {
            assert expression == null || nature != ScopeNature.ABSENT;
            return expression == null;
        }

        public Expression ensureExplicit(Runtime runtime,
                                         HierarchyHelper hierarchyHelper,
                                         MethodInfo methodInfo,
                                         boolean scopeIsThis,
                                         Context context,
                                         TypeInfo enclosingType,
                                         Source unparsedScopeSource) {
            /*
             in 3 situations, we compute (or potentially correct) the scope.
             In the case of a static method, we always replace by the class containing the method.
             In the case of this: we use the type of the current class in case of extension, but not in case of sub-typing,
             because we have to be able to indicate that we're reading the correct "this" in the VariableAccess report.
             In case of parent-child, we activate the "super" boolean.
             See e.g. Lambda_15.
             IMPROVE! https://github.com/e2immu/e2immu/issues/60
             */
            if (objectIsImplicit() || methodInfo.isStatic() || scopeIsThis) {
                TypeInfo exact = methodInfo.typeInfo();
                if (methodInfo.isStatic()) {
                    // every TypeExpression should have its TypeInfo in the detailed sources, if required.
                    DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
                    if (detailedSourcesBuilder != null) {
                        if (unparsedScopeSource != null) {
                            detailedSourcesBuilder.put(exact, unparsedScopeSource);
                        }
                        if (expression != null && expression.source().detailedSources() != null) {
                            detailedSourcesBuilder.addAll(expression.source().detailedSources());
                        }
                    }
                    ParameterizedType pt = expression == null ? exact.asParameterizedType()
                            : expression.parameterizedType();
                    return runtime.newTypeExpressionBuilder()
                            .setParameterizedType(pt)
                            .setDiamond(runtime.diamondNo())
                            .setSource(detailedSourcesBuilder == null || unparsedScopeSource == null
                                    ? unparsedScopeSource
                                    : unparsedScopeSource.withDetailedSources(detailedSourcesBuilder.build()))
                            .build();
                }
                TypeInfo typeInfo;
                boolean writeSuper;
                TypeInfo explicitlyWriteType;
                if (enclosingType == exact || exact.isJavaLangObject()) {
                    //? See TestTypeParameter,5, "super"      || hierarchyHelper.recursivelyImplements(enclosingType, exact.fullyQualifiedName()) != null) {
                    typeInfo = enclosingType; // the same type
                    writeSuper = false;
                    explicitlyWriteType = null;
                } else if (hierarchyHelper.parentalHierarchyContains(enclosingType, exact)) {
                    typeInfo = enclosingType;
                    writeSuper = true;
                    explicitlyWriteType = null;
                } else {
                    // relationship must be inner class of ...
                    typeInfo = exact;
                    writeSuper = false;
                    explicitlyWriteType = exact;
                }
                Variable thisVariable = runtime.newThis(typeInfo.asParameterizedType(),
                        explicitlyWriteType, writeSuper);
                return runtime.newVariableExpressionBuilder().setVariable(thisVariable)
                        .setSource(unparsedScopeSource).build();
            }
            return expression;
        }
    }

    public Scope computeScope(ParseHelper parseHelper,
                              Context context,
                              String index,
                              Object unparsedScope,
                              TypeParameterMap extra) {
        /*
        erasure on failure: in the case of constructor calls with unknown type parameters, we want to have
        the opportunity to re-try.
         */
        ForwardType forward = new ForwardTypeImpl(null, false, true, extra);
        Expression scope = unparsedScope == null ? null
                : parseHelper.parseExpression(context, index, forward, unparsedScope);
        // depending on the object, we'll need to find the method somewhere
        ParameterizedType scopeType;
        ScopeNature scopeNature;

        if (scope == null) {
            scopeType = runtime.newParameterizedType(context.enclosingType(), 0);
            scopeNature = ScopeNature.ABSENT; // could be static, could be object instance
        } else {
            scopeType = scope.parameterizedType();
            scopeNature = scope instanceof TypeExpression ? ScopeNature.STATIC : ScopeNature.INSTANCE;
        }
        Map<NamedType, ParameterizedType> scopeTypeMap = scopeType.initialTypeParameterMap();
        return new Scope(scope, scopeType, scopeNature, new TypeParameterMap(scopeTypeMap));
    }

}

