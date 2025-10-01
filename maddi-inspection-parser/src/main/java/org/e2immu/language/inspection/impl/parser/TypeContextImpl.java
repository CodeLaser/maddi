package org.e2immu.language.inspection.impl.parser;

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.ImportStatement;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.parser.StaticImportMap;
import org.e2immu.language.inspection.api.parser.TypeContext;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class TypeContextImpl implements TypeContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(TypeContextImpl.class);

    private record StubTypeMap(Map<String, TypeInfo> map) {
    }

    private record Data(Runtime runtime,
                        CompiledTypesManager compiledTypesManager,
                        StaticImportMap staticImportMap,
                        CompilationUnit compilationUnit,
                        StubTypeMap stubTypeMap) {

        public boolean allowCreationOfStubTypes() {
            return stubTypeMap != null;
        }

        Data withCompilationUnit(CompilationUnit cu) {
            return new Data(runtime, compiledTypesManager, new StaticImportMapImpl(), cu, stubTypeMap);
        }
    }

    private final TypeContextImpl parentContext;
    private final Data data;

    private record NamedTypePriority(NamedType namedType, int priority) {
    }

    private final Map<String, NamedTypePriority> map = new HashMap<>();

    /*
    the packageInfo should already contain all the types of the current package
     */
    public TypeContextImpl(Runtime runtime, CompiledTypesManager compiledTypesManager, boolean allowCreationOfStubTypes) {
        this(null, new Data(runtime, compiledTypesManager, null,
                null, allowCreationOfStubTypes ? new StubTypeMap(new HashMap<>()) : null));
    }

    private TypeContextImpl(TypeContextImpl parentContext, Data data) {
        this.parentContext = parentContext;
        this.data = data;
    }

    @Override
    public boolean addToStaticImportMap(CompilationUnit currentCompilationUnit, ImportStatement importStatement) {
        assert importStatement.isStatic();

        String fqnWithAsterisk = importStatement.importString();
        boolean isAsterisk = fqnWithAsterisk.endsWith(".*");
        String fqn = isAsterisk ? fqnWithAsterisk.substring(0, fqnWithAsterisk.length() - 2) : fqnWithAsterisk;

        if (isAsterisk) {
            TypeInfo typeInfo = loadTypeDoNotImport(fqn);
            LOGGER.debug("Add import static wildcard {}", typeInfo);
            addImportStaticWildcard(typeInfo);
            typeInfo.subTypes().forEach(st -> addImportStatic(typeInfo, st.simpleName()));
            return traverseInterfaceHierarchy(currentCompilationUnit, typeInfo, new HashSet<>());
        }

        int dot = fqn.lastIndexOf('.');
        String typeOrSubTypeName = fqn.substring(0, dot);
        String member = fqn.substring(dot + 1);
        TypeInfo typeInfo = loadTypeDoNotImport(typeOrSubTypeName);
        LOGGER.debug("Add import static, type {}, member {}", typeInfo, member);
        addImportStatic(typeInfo, member);
        return true;
    }

    private boolean traverseInterfaceHierarchy(CompilationUnit compilationUnit,
                                               TypeInfo typeInfo,
                                               Set<TypeInfo> visited) {
        // note: we must ignore 'self-references', they are obviously not resolved yet
        if (visited.add(typeInfo) && !compilationUnit.equals(typeInfo.primaryType().compilationUnit())) {
            if (typeInfo.hierarchyNotYetDone()) {
               /*
                this is the situation where we must delay: we need to know the interfaces of this type, but it is
                possible that because of parsing order, they have not been parsed yet.
                */
                return false;
            }
            // see Import4, TestImport4 and variants
            for (ParameterizedType interfaceType : typeInfo.interfacesImplemented()) {
                interfaceType.typeInfo().subTypes()
                        .forEach(st -> addImportStatic(interfaceType.typeInfo(), st.simpleName()));
                if (!traverseInterfaceHierarchy(compilationUnit, interfaceType.typeInfo(), visited)) {
                    // recursion
                    return false;
                }
            }
        }
        return true;
    }

    /*
    this is a rather slow method, but the result will be "cached" in the variable context, see ParseExpression
     */
    @Override
    public Variable findStaticFieldImport(String name) {
        if (data.staticImportMap != null) {
            for (TypeInfo typeInfo : data.staticImportMap.getStaticMemberToTypeInfo(name)) {
                FieldInfo fieldInfo = typeInfo.getFieldByName(name, false);
                if (fieldInfo != null) {
                    return data.runtime.newFieldReference(fieldInfo);
                }
            }
            for (TypeInfo ti : data.staticImportMap.staticAsterisk()) {
                FieldInfo fieldInfo = ti.getFieldByName(name, false);
                if (fieldInfo != null) {
                    return data.runtime.newFieldReference(fieldInfo);
                }
            }
        }
        return null;
    }

    @Override
    public void addNonStaticImportToContext(ImportStatement importStatement) {
        assert !importStatement.isStatic();
        String fqnWithAsterisk = importStatement.importString();
        boolean isAsterisk = fqnWithAsterisk.endsWith(".*");
        if (isAsterisk) {
            String fullyQualified = fqnWithAsterisk.substring(0, fqnWithAsterisk.length() - 2);

            LOGGER.debug("Need to parse package {}", fullyQualified);
            String packageName = data.compilationUnit.packageName();
            if (!fullyQualified.equals(packageName)) { // would be our own package; they are already there
                TypeInfo typeInfo = data.compiledTypesManager.getOrLoad(fullyQualified, sourceSet());
                if (typeInfo != null) {
                    // we must add all the subtypes
                    for (TypeInfo sub : typeInfo.subTypes()) {
                        addToContext(sub, IMPORT_ASTERISK_SUBTYPE_PRIORITY);
                    }
                } else {
                    List<TypeInfo> typesInPackage = data.compiledTypesManager
                            .primaryTypesInPackageEnsureLoaded(fullyQualified, sourceSet());
                    for (TypeInfo ti : typesInPackage) {
                        assert ti.fullyQualifiedName().equals(fullyQualified + "." + ti.simpleName());
                        addToContext(ti, IMPORT_ASTERISK_PACKAGE_PRIORITY);
                    }
                }
            }
        } else {
            TypeInfo typeInfo = data.compiledTypesManager.getOrLoad(importStatement.importString(), sourceSet());
            if (typeInfo != null) {
                addToContext(typeInfo, IMPORT_PRIORITY);
            } else {
                LOGGER.error("Cannot handle import {} at line {} in {}", importStatement.importString(),
                        importStatement.source().beginLine(), data.compilationUnit);
            }
        }
    }

    private TypeInfo loadTypeDoNotImport(String fqn) {
        TypeInfo typeInfo = data.compiledTypesManager.getOrLoad(fqn, sourceSet());
        if (typeInfo == null) {
            LOGGER.error("ERROR: Cannot find type '{}'", fqn);
            throw new UnsupportedOperationException(fqn);
        }
        return typeInfo;
    }

    @Override
    public TypeContext newCompilationUnit(CompilationUnit compilationUnit) {
        return new TypeContextImpl(null, data.withCompilationUnit(compilationUnit));
    }

    @Override
    public StaticImportMap importMap() {
        return data.staticImportMap;
    }

    @Override
    public CompilationUnit compilationUnit() {
        return data.compilationUnit;
    }

    private SourceSet sourceSet() {
        return data.compilationUnit.sourceSet();
    }

    /**
     * Look up a type by FQN. Ensure that the type has been inspected.
     *
     * @param fullyQualifiedName the fully qualified name, such as java.lang.String
     * @return the type
     */
    private TypeInfo getFullyQualified(String fullyQualifiedName) {
        return data.compiledTypesManager.getOrLoad(fullyQualifiedName, sourceSet());
    }

    /*
     we have no idea whether the name is fully qualified, partially qualified... we can try
     the import statements, but they can contain *'s or be incomplete.
     */
    private TypeInfo getOrCreateStubType(String name) {
        TypeInfo inMap = data.stubTypeMap.map.get(name);
        if (inMap != null) return inMap;
        int lastDot = name.lastIndexOf('.');
        String simpleName = lastDot < 0 ? name : name.substring(lastDot + 1);
        String candidatePackageName = tryToDeducePackageName(name);
        CompilationUnit compilationUnitStub = data.runtime.newCompilationUnitStub(candidatePackageName);
        TypeInfo typeInfo = data.runtime.newTypeInfo(compilationUnitStub, simpleName);
        typeInfo.builder().setTypeNature(data.runtime.typeNatureStub());
        data.stubTypeMap.map.put(name, typeInfo);
        return typeInfo;
    }

    private String tryToDeducePackageName(String name) {
        return data.compilationUnit.importStatements().stream().filter(is ->
                        !is.isStatic() && is.importString().endsWith(name))
                .map(is -> {
                    int dot = is.importString().lastIndexOf('.');
                    return dot < 0 ? "" : is.importString().substring(0, dot);
                })
                .findFirst().orElseGet(() -> {
                    int dot = name.lastIndexOf('.');
                    return dot < 0 ? "" : name.substring(0, dot);
                });
    }


    // pretty similar to get(), but we keep track of the qualification
    @Override
    public List<? extends NamedType> getWithQualification(String name, boolean complain) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            NamedType simple = getSimpleName(name);
            if (simple != null) {
                return List.of(simple);
            }
        }
        // name can be fully qualified, or semi qualified; but the package can be empty, too.
        // try fully qualified first
        NamedType fullyQualified = getFullyQualified(name);
        if (fullyQualified instanceof TypeInfo typeInfo) {
            return typeInfo.enclosingTypeStream().toList().reversed();
        }

        if (dot >= 0) {
            // it must be semi qualified now... go recursive;
            String prefix = name.substring(0, dot);
            List<? extends NamedType> prefixTypes = getWithQualification(prefix, complain);
            if (prefixTypes != null) {
                String tail = name.substring(dot + 1);
                TypeInfo tailType = subTypeOfRelated((TypeInfo) prefixTypes.getLast(), tail);
                if (tailType != null) {
                    return Stream.concat(prefixTypes.stream(), Stream.of(tailType)).toList();
                }
            }
        }

        // have been pre-loaded, so we can use "get" and continue with null if it wasn't the one we're looking for
        NamedType javaLang = data.compiledTypesManager.get("java.lang." + name,
                data.compiledTypesManager.javaBase());
        if (javaLang != null) return List.of(javaLang);
        if (data.allowCreationOfStubTypes()) {
            return List.of(getOrCreateStubType(name));
        }
        if (complain) {
            throw new UnsupportedOperationException("Cannot find type " + name);
        }
        return null;
    }

    private TypeInfo subTypeOfRelated(TypeInfo typeInfo, String name) {
        TypeInfo sub = typeInfo.findSubType(name, false);
        if (sub != null) return sub;
        if (typeInfo.parentClass() != null && !typeInfo.parentClass().typeInfo().isJavaLangObject()) {
            TypeInfo subParent = subTypeOfRelated(typeInfo.parentClass().typeInfo(), name);
            if (subParent != null) return subParent;
        }
        for (ParameterizedType interfaceImplemented : typeInfo.interfacesImplemented()) {
            TypeInfo subImplemented = subTypeOfRelated(interfaceImplemented.typeInfo(), name);
            if (subImplemented != null) {
                return subImplemented;
            }
        }
        return null;
    }

    private NamedType getSimpleName(String name) {
        NamedTypePriority namedTypePriority = map.get(name);
        if (namedTypePriority != null) {
            return namedTypePriority.namedType;
        }

        // Same package, and * imports (in that order!)
        if (parentContext != null) {
            NamedType fromParent = parentContext.getSimpleName(name);
            if (fromParent != null) {
                return fromParent;
            }
        }

        /*
        On-demand: subtype from import static statement (see e.g. Import_2)
        This is done on-demand to fight cyclic dependencies if we do eager inspection.
         */
        for (TypeInfo parent : data.staticImportMap.getStaticMemberToTypeInfo(name)) {
            TypeInfo subType = subTypeOfRelated(parent, name);
            if (subType != null) {
                addToContext(subType, STATIC_IMPORT_PRIORITY);
                return subType;
            }
            // we should definitely not return "parent"
        }
        return null;
    }


    @Override
    public void addToContext(@NotNull NamedType namedType, int priority) {
        String simpleName = namedType.simpleName();
        NamedTypePriority ntp = map.get(simpleName);
        if (ntp == null || ntp.priority < priority) {
            map.put(simpleName, new NamedTypePriority(namedType, priority));
        }
        if (namedType instanceof TypeInfo ti && ti.compilationUnitOrEnclosingType().isRight()) {
            // ensure that enclosing types are present, but with lower priority
            addToContext(ti.compilationUnitOrEnclosingType().getRight(), IMPORT_ENCLOSING_PRIORITY);
        }
    }

    @Override
    public TypeContext newTypeContext() {
        return new TypeContextImpl(this, data);
    }

    public void addImportStaticWildcard(TypeInfo typeInfo) {
        data.staticImportMap.addStaticAsterisk(typeInfo);
    }

    public void addImportStatic(TypeInfo typeInfo, String member) {
        data.staticImportMap.addStaticMemberToTypeInfo(member, typeInfo);
    }

    @Override
    public TypeContext newAnonymousClassBody(TypeInfo baseType) {
        TypeContext tc = new TypeContextImpl(this, data);
        tc.addSubTypesOfHierarchyReturnAllDefined(baseType, SUBTYPE_HIERARCHY_ANONYMOUS);
        return tc;
    }

    @Override
    public List<TypeInfo> typesInSamePackage(String packageName, SourceSet sourceSetOfRequest) {
        return data.compiledTypesManager.primaryTypesInPackageEnsureLoaded(packageName, sourceSetOfRequest);
    }


    @Override
    public boolean addSubTypesOfHierarchyReturnAllDefined(TypeInfo typeInfo, int priority) {
        Set<TypeInfo> superTypes = new HashSet<>();
        boolean allDefined = recursivelyComputeSuperTypesExcludingJLO(typeInfo, superTypes);
        Stream.concat(Stream.of(typeInfo), superTypes.stream())
                .forEach(superType -> superType.subTypes()
                        // not checking accessibility here
                        .forEach(ti -> addToContext(ti, priority)));
        return allDefined;
    }

    private boolean recursivelyComputeSuperTypesExcludingJLO(TypeInfo type, Set<TypeInfo> superTypes) {
        ParameterizedType parentPt = type.parentClass();
        if (type.hierarchyNotYetDone()) {
            return false;
        }
        if (type.isJavaLangObject()) return true;
        assert parentPt != null;
        TypeInfo parent = parentPt.typeInfo();
        boolean allDefined = true;
        if (!parent.isJavaLangObject() && superTypes.add(parent)) {
            allDefined = recursivelyComputeSuperTypesExcludingJLO(parent, superTypes);
        }
        for (ParameterizedType interfaceImplemented : type.interfacesImplemented()) {
            TypeInfo i = interfaceImplemented.typeInfo();
            if (superTypes.add(i)) {
                allDefined &= recursivelyComputeSuperTypesExcludingJLO(i, superTypes);
            }
        }
        return allDefined;
    }
}
