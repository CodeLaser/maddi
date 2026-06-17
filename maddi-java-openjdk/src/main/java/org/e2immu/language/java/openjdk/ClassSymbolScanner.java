package org.e2immu.language.java.openjdk;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.Wildcard;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.api.util.CreateSyntheticFieldsForGetSet;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ClassSymbolScanner implements ConvertType, TypeData {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassSymbolScanner.class);
    private final Runtime runtime;
    private final FlagHelper flagHelper;
    private final Elements elements;
    private final Types types;
    private final SourceSet sourceSetOfCurrentTask;
    private final Set<TypeInfo> recursionPrevention = new HashSet<>();
    private final Map<String, TypeInfo> predefinedTypes = new HashMap<>();
    private final Deque<Map<String, TypeParameter>> typeParameterStack = new ArrayDeque<>();
    private final Map<String, SourceSet> sourceSetMap;
    private final Map<String, SourceSet> sourceSetDirPrefixes;
    private final ComputeMethodOverrides computeMethodOverrides;
    private final MaddiDiagnosticCollector diagnosticCollector;

    private final InfoByFqn infoByFqn; // Javac qualified name -> typeInfo, methodInfo, fieldInfo
    private final Map<Symbol.MethodSymbol, MethodInfo> methodSymbolMap = new IdentityHashMap<>();
    private final Map<Symbol.VarSymbol, FieldInfo> varSymbolMap = new IdentityHashMap<>();
    private final Map<String, Map<String, TypeParameter>> tmpMethodTypeParameterMap = new HashMap<>();
    private final CreateSyntheticFieldsForGetSet createSyntheticFieldsForGetSet;

    // not thread-safe: set for each compilation unit
    private SourceProvider sourceProvider;
    private ElementStack elementStack;
    private IdentityHashMap<Symbol.ClassSymbol, Boolean> topLevelClassSymbolsOfSources;

    public enum LoadMode {
        LOAD_MEMBERS, // load everything immediately, and commit
        LAZILY,  // only load what is required, and store in "loaded"
        COMPLETE, // uses to complete a lazily loaded type; check with "loaded"
        COMPLETE_SUB,
    }

    public ClassSymbolScanner(Runtime runtime,
                              InputConfiguration inputConfiguration,
                              InfoByFqn infoByFqn,
                              SourceSet sourceSetOfCurrentTask,
                              FlagHelper flagHelper,
                              Types types,
                              Elements elements,
                              MaddiDiagnosticCollector maddiDiagnosticCollector) {
        this.runtime = runtime;
        this.flagHelper = flagHelper;
        this.elements = elements;
        this.types = types;
        this.infoByFqn = Objects.requireNonNull(infoByFqn);
        this.sourceSetOfCurrentTask = sourceSetOfCurrentTask;
        assert inputConfiguration.sourceSets().contains(sourceSetOfCurrentTask);
        this.diagnosticCollector = maddiDiagnosticCollector;
        this.computeMethodOverrides = new ComputeMethodOverrides(types, elements);
        this.createSyntheticFieldsForGetSet = new CreateSyntheticFieldsForGetSet(runtime);

        predefinedTypes.put("String", runtime.stringTypeInfo());
        predefinedTypes.put("Object", runtime.objectTypeInfo());

        predefinedTypes.put("Integer", runtime.integerTypeInfo());
        predefinedTypes.put("Boolean", runtime.boxedBooleanTypeInfo());
        predefinedTypes.put("Long", runtime.boxedLongTypeInfo());
        predefinedTypes.put("Character", runtime.characterTypeInfo());
        predefinedTypes.put("Double", runtime.boxedDoubleTypeInfo());
        predefinedTypes.put("Float", runtime.boxedFloatTypeInfo());
        predefinedTypes.put("Short", runtime.boxedShortTypeInfo());
        predefinedTypes.put("Byte", runtime.boxedByteTypeInfo());
        predefinedTypes.put("Void", runtime.boxedVoidTypeInfo());

        predefinedTypes.put("Class", runtime.classTypeInfo());

        Map<String, SourceSet> map = new HashMap<>();
        Map<String, SourceSet> prefixes = new HashMap<>();
        for (SourceSet sourceSet : inputConfiguration.sourceSets()) {
            map.put(sourceSet.name(), sourceSet);
        }
        for (SourceSet cpp : inputConfiguration.classPathParts()) {
            SourceSet toAdd;
            if (cpp.name().startsWith("jar-on-classpath:")) {
                int colon = cpp.name().indexOf(':');
                String jarName = cpp.name().substring(colon + 1);
                toAdd = new SourceSetImpl.Builder(cpp).setName(jarName).build();
            } else {
                toAdd = cpp;
                if (cpp.externalLibrary() && "file".equals(cpp.uri().getScheme())) {
                    Path path = Path.of(cpp.uri());
                    if (Files.isDirectory(path)) {
                        try {
                            prefixes.put(path.toRealPath().toAbsolutePath().toString(), cpp);
                        } catch (IOException ioe) {
                            throw new UnsupportedOperationException("Cannot discover real path of " + path);
                        }
                    }
                }
            }
            map.put(toAdd.name(), toAdd);
        }
        sourceSetMap = Map.copyOf(map);
        sourceSetDirPrefixes = Map.copyOf(prefixes);
    }

    TypeInfo lazilyLoadTypeFromClassFile(Symbol.ClassSymbol cs) {
        switch (cs.owner) {
            case Symbol.PackageSymbol _ -> {
                return lazilyLoadPrimaryTypeFromClassFile(cs);
            }
            case Symbol.ClassSymbol enclosedSymbol -> {
                TypeInfo owner = convert(enclosedSymbol.type).typeInfo();
                String simpleName = cs.getSimpleName().toString();
                TypeInfo inMap = owner.findSubType(simpleName, false);
                if (inMap == null) {
                    TypeInfo enclosed = runtime.newTypeInfo(owner, simpleName);
                    // note: first put the type in typeData, only then load it... self-references are common!
                    put(enclosed);
                    loadType(cs, enclosed, LoadMode.LAZILY);
                    owner.builder().addSubType(enclosed);
                    return enclosed;
                }
                return inMap;
            }
            case Symbol.MethodSymbol _ -> throw new UnsupportedOperationException("Should have been picked up earlier");
            case null, default -> throw new UnsupportedOperationException();
        }
    }

    TypeInfo lazilyLoadPrimaryTypeFromClassFile(Symbol.ClassSymbol cs) {
        String simpleName = cs.name.toString();
        assert cs.owner instanceof Symbol.PackageSymbol;
        String packageName = cs.owner.toString();
        TypeInfo newTypeInfo;
        boolean internal;
        TypeInfo predefinedType;
        if ("java.lang".equals(packageName) && (predefinedType = predefinedTypes.get(simpleName)) != null) {
            newTypeInfo = predefinedType;
            internal = false;
        } else {
            try {
                cs.complete();
            } catch (Symbol.CompletionFailure completionFailure) {
                diagnosticCollector.reportMissingClassFile(completionFailure);
            }
            internal = cs.packge().toString().startsWith("jdk.internal.");
            URI uri;
            CompilationUnit cu;
            if (cs.classfile == null) {
                cu = runtime.newCompilationUnitStub(cs.packge().toString());
            } else {
                if (internal) {
                    uri = URI.create("jrt:/internal/");
                } else {
                    uri = cs.classfile.toUri();
                }
                SourceSet sourceSet = ensureSourceSet(cs, uri);
                cu = runtime.newCompilationUnitBuilder()
                        .setPackageName(packageName)
                        .setSourceSet(sourceSet)
                        .setURI(uri)
                        .build();
            }
            newTypeInfo = runtime.newTypeInfo(cu, simpleName);
        }
        put(newTypeInfo);
        if (!internal) {
            loadType(cs, newTypeInfo, LoadMode.LAZILY);
        }
        return newTypeInfo;
    }

    void loadType(Symbol.ClassSymbol cs, TypeInfo newTypeInfo, LoadMode loadMode) {
        if (cs == null) return;
        if (newTypeInfo.hasBeenInspected()) return;
        LOGGER.debug("Enter loadType: {} {}", newTypeInfo.fullyQualifiedName(), loadMode);
        TypeInfo.Builder builder = newTypeInfo.builder();

        if (recursionPrevention.add(newTypeInfo)) {
            //The following completely loads 'cs', so leave it here even though it can move nearer to its usage
            List<? extends Element> members = elements.getAllMembers(cs);
            flagHelper.type(cs, builder);
            if (loadMode != LoadMode.COMPLETE) {
                // ensure that the enclosing types have at least been lazily loaded; so that we can compute access
                // as soon as possible
                if (newTypeInfo.compilationUnitOrEnclosingType().isRight()) {
                    if (cs.owner instanceof Symbol.ClassSymbol enclosing) {
                        loadType(enclosing, newTypeInfo.compilationUnitOrEnclosingType().getRight(), LoadMode.LAZILY);
                    }
                }
                builder.computeAccess();

                int index = 0;
                newTypeParameterMap();
                List<TypeParameter> created = new ArrayList<>();
                for (Symbol.TypeVariableSymbol typeParameter : cs.getTypeParameters()) {
                    TypeParameter newTp = runtime.newTypeParameter(index++, typeParameter.getSimpleName().toString(), newTypeInfo);
                    putInLastTypeParameterMap(newTp);
                    created.add(newTp);
                }
                int i = 0;
                for (Symbol.TypeVariableSymbol typeParameter : cs.getTypeParameters()) {
                    TypeParameter newTp = created.get(i++);
                    addTypeBoundsAndCommit(cs, newTypeInfo, typeParameter, newTp);
                    builder.addOrSetTypeParameter(newTp);
                }
                popTypeParameterMap();

                ParameterizedType superType = convert(cs.getSuperclass());
                if (!newTypeInfo.isJavaLangObject()) {
                    ParameterizedType parentClass = superType == null ? runtime.objectParameterizedType() : superType;
                    assert parentClass != null;
                    builder.setParentClass(parentClass);
                }
                for (Type type : cs.getInterfaces()) {
                    ParameterizedType pt = convert(type);
                    builder.addInterfaceImplemented(pt);
                }
            }

            if (loadMode != LoadMode.LAZILY) {
                for (var member : members) {
                    addMemberToType(newTypeInfo, cs, member, loadMode);
                }

                MethodInfo singleAbstractMethod = computeSAM(cs.type);
                builder.setSingleAbstractMethod(singleAbstractMethod);

                if (newTypeInfo.typeNature().isInterface() || newTypeInfo.typeNature().isClass()
                                                              && builder.isAbstract()) {
                    createSyntheticFieldsForGetSet.createSyntheticFields(newTypeInfo);
                }
                builder.commit();
            }
            recursionPrevention.remove(newTypeInfo);
        }
    }

    private void addTypeBoundsAndCommit(Symbol.ClassSymbol cs,
                                        TypeInfo newTypeInfo,
                                        Symbol.TypeVariableSymbol typeParameter,
                                        TypeParameter newTp) {
        List<ParameterizedType> bounds = new ArrayList<>();
        if (typeParameter.type instanceof Type.TypeVar tv) {
            Type lowerBound = tv.getLowerBound();
            if (lowerBound.getKind() != TypeKind.NULL) {
                throw new UnsupportedOperationException();
            } else {
                Type upperBound = tv.getUpperBound();
                if (upperBound.getKind() != TypeKind.NULL) {
                    if (upperBound.tsym == cs) {
                        // self reference, as in java.lang.Enum<E extends Enum<E>>
                        bounds.add(runtime.newParameterizedType(newTypeInfo,
                                List.of(runtime.newParameterizedType(newTp, 0, null))));
                    } else if (upperBound instanceof Type.ClassType ct) {
                        if (ct instanceof Type.IntersectionClassType ict) {
                            for (Type type : ict.getExplicitComponents()) {
                                ParameterizedType pt = convert(type);
                                if (!pt.isJavaLangObject()) {
                                    bounds.add(pt.withWildcard(runtime.wildcardExtends()));
                                }
                            }
                        } else {
                            ParameterizedType upper = convert(ct);
                            if (!upper.isJavaLangObject()) {
                                bounds.add(upper.withWildcard(runtime.wildcardExtends()));
                            }
                        }
                    } else if (upperBound instanceof Type.TypeVar tv2) {
                        ParameterizedType upper = convert(tv2);
                        if (!upper.isJavaLangObject()) {
                            bounds.add(upper.withWildcard(runtime.wildcardExtends()));
                        }
                    } else {
                        throw new UnsupportedOperationException();
                    }
                }
            }
        }
        newTp.builder().setTypeBounds(List.copyOf(bounds)).commit();
    }

    private static final Pattern JAR_FILE = Pattern.compile("(jar:file:.+)/([^/!]+\\.jar)!/.*");

    private SourceSet ensureSourceSet(Symbol.ClassSymbol cs, URI uri) {
        Matcher m = JAR_FILE.matcher(uri.toString());
        if (m.matches()) {
            String jarName = m.group(2);
            SourceSet known = getSourceSet(jarName);
            if (known == null) {
                throw new UnsupportedOperationException(
                        "Cannot find class path source set interpreted as jar file: " + jarName);
            }
            return known;
        }
        Symbol.ModuleSymbol module = findModule(cs);
        if (module != null) {
            if (module.isUnnamed()) {
                LOGGER.debug("?");
            } else {
                SourceSet known = getSourceSet(module.name.toString());
                if (known == null) {
                    // FIXME when the source is a module... currently not implemented
                    return sourceSetOfCurrentTask;
                }
                return known;
            }
        }
        if ("file".equals(uri.getScheme())) {
            SourceSet dir = sourceSetDirPrefixes.entrySet().stream()
                    .filter(e -> uri.getPath().startsWith(e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(null);
            if (dir != null) return dir;
        }
        return sourceSetOfCurrentTask;
    }

    Symbol.ModuleSymbol findModule(Symbol.ClassSymbol cs) {
        Symbol.ClassSymbol primary = cs;
        while (primary.owner instanceof Symbol.ClassSymbol encl) {
            primary = encl;
        }
        if (primary.owner instanceof Symbol.PackageSymbol ps) {
            return ps.modle;
        }
        return null;
    }

    private void addMemberToType(TypeInfo typeInfo, Symbol.ClassSymbol owner, Element member, LoadMode loadMode) {
        boolean alwaysLoad = loadMode == LoadMode.LOAD_MEMBERS || loadMode == LoadMode.COMPLETE_SUB;
        if (member instanceof Symbol.MethodSymbol ms && ms.owner == owner) {
            // the check for JLO is actually only for the clone() method
            boolean isNotPrivate = (ms.flags() & Flags.PRIVATE) == 0;
            if (isNotPrivate
                && (alwaysLoad || loadMode == LoadMode.COMPLETE && !methodSymbolMap.containsKey(ms))
                && (loadMode == LoadMode.LOAD_MEMBERS || !methodSymbolMap.containsKey(ms))) {
                MethodInfo methodInfo = addMethodToType(typeInfo, ms, false);
                if (!methodInfo.hasBeenInspected()) methodInfo.builder().computeAccess().commit();
            }

        } else if (member instanceof Symbol.VarSymbol vs && vs.owner == owner) {
            boolean isNotPrivate = (vs.flags() & Flags.PRIVATE) == 0;
            if (isNotPrivate && (alwaysLoad || loadMode == LoadMode.COMPLETE && !varSymbolMap.containsKey(vs))
                && (loadMode == LoadMode.LOAD_MEMBERS || !varSymbolMap.containsKey(vs))) {
                FieldInfo fieldInfo = addFieldToType(typeInfo, vs);
                if (!fieldInfo.hasBeenInspected()) {
                    fieldInfo.builder().computeAccess().commit();
                    assert fieldInfo.access() != null;
                }
            }
        } else if (member instanceof Symbol.ClassSymbol cs && cs.owner == owner) {
            boolean isNotPrivate = (cs.flags() & Flags.PRIVATE) == 0;
            if (isNotPrivate && (alwaysLoad || loadMode == LoadMode.COMPLETE
                                               && typeInfo.findSubType(cs.getSimpleName().toString(), false) == null)
                && (loadMode == LoadMode.LOAD_MEMBERS
                    || typeInfo.findSubType(cs.getSimpleName().toString(), false) == null)) {
                TypeInfo enclosed = addEnclosedTypeToType(typeInfo, cs, loadMode);
                if (!enclosed.hasBeenInspected()) {
                    if (enclosed.packageName().startsWith("com.google.cloud")) {
                        LOGGER.info("Committing {}", enclosed);
                    }
                    enclosed.builder().computeAccess().commit();
                }
            }
        }
    }

    private TypeInfo addEnclosedTypeToType(TypeInfo typeInfo, Symbol.ClassSymbol cs, LoadMode loadMode) {
        TypeInfo inMap = getType(cs.fullname.toString());
        if (inMap != null) return inMap;
        String name = cs.getSimpleName().toString();
        LOGGER.debug("Adding enclosed type {} to {}", name, typeInfo);
        TypeInfo enclosed = runtime.newTypeInfo(typeInfo, name);
        put(enclosed);
        typeInfo.builder().addSubType(enclosed);
        // we must do type parameters, interfaces, parent class etc.
        loadType(cs, enclosed, loadMode == LoadMode.COMPLETE ? LoadMode.COMPLETE_SUB : loadMode);
        return enclosed;
    }

    private FieldInfo addFieldToType(TypeInfo typeInfo, Symbol.VarSymbol vs) {
        String name = vs.getSimpleName().toString();
        LOGGER.debug("Adding field {} to {}", name, typeInfo);
        ParameterizedType type = convert(vs.type);
        boolean isStatic = (vs.flags() & Flags.STATIC) != 0;
        FieldInfo fieldInfo = runtime.newFieldInfo(name, isStatic, type, typeInfo);
        // we might overwrite them, or not...
        fieldInfo.builder().setInitializer(runtime.newEmptyExpression()).setAccess(runtime.accessPublic());
        typeInfo.builder().addField(fieldInfo);
        flagHelper.field(vs.flags(), fieldInfo.builder());

        put(vs, fieldInfo);
        return fieldInfo;
    }

    MethodInfo addMethodToType(TypeInfo typeInfo, Symbol.MethodSymbol ms, boolean synthetic) {
        if (typeInfo.hasBeenInspected()) {
            LOGGER.error("Type {} has been committed, yet we're trying to add a method to its builder",
                    typeInfo.descriptor());
            LOGGER.error("Method to add: {}", ms.toString());
            LOGGER.error("Current methods + constructors:\n{}",
                    typeInfo.constructorAndMethodStream()
                            .map(Info::descriptor)
                            .collect(Collectors.joining("\n")));
            throw new UnsupportedOperationException();
        }
        String name = ms.getSimpleName().toString();
        //  assert (ms.flags() & Flags.BRIDGE) == 0 : "Do not want any bridge method " + ms + " in " + typeInfo;
        MethodInfo method;
        if ("<init>".equals(name)) {
            LOGGER.debug("Adding constructor {} to {}", name, typeInfo);
            MethodInfo.MethodType methodType = flagHelper.constructorType(ms.flags());
            method = runtime.newConstructor(typeInfo, methodType);
            typeInfo.builder().addConstructor(method);
        } else {
            LOGGER.debug("Adding method {} to {}", name, typeInfo);
            MethodInfo.MethodType methodType = flagHelper.methodType(ms.flags(),
                    typeInfo.isInterface() || typeInfo.isAnnotation());
            method = runtime.newMethod(typeInfo, name, methodType);
            typeInfo.builder().addMethod(method);
        }
        // add this early enough to avoid recursion/infinite loop problems with self-referencing type parameters
        put(ms, method);

        int index = 0;
        MethodInfo.Builder builder = method.builder();

        List<TypeParameter> newTypeParameters = new ArrayList<>();
        for (Symbol.TypeVariableSymbol typeParameter : ms.getTypeParameters()) {
            TypeParameter newTp = runtime.newTypeParameter(index++, typeParameter.getSimpleName().toString(), method);
            builder.addTypeParameter(newTp);
            putTmpMethodTypeParameter(typeInfo.fullyQualifiedName(), newTp.simpleName(), newTp);
            newTypeParameters.add(newTp);
        }
        int i = 0;
        for (Symbol.TypeVariableSymbol typeParameter : ms.getTypeParameters()) {
            TypeParameter newTp = newTypeParameters.get(i++);
            addTypeBoundsAndCommit(null, null, typeParameter, newTp);
        }

        flagHelper.method(ms.flags(), builder);
        if (synthetic) {
            builder.setSynthetic(true);
        }
        if (ms.params != null) {
            for (Symbol.VarSymbol parameter : ms.params) {
                ParameterizedType pt = convert(parameter.type);
                ParameterInfo parameterInfo = builder.addParameter(parameter.getSimpleName().toString(), pt);
                long flags = parameter.flags();
                if ((flags & Flags.VARARGS) != 0) parameterInfo.builder().setVarArgs(true);
                if ((flags & Flags.FINAL) != 0) parameterInfo.builder().setIsFinal(true);
                parameterInfo.builder().commit();
            }
        }
        ParameterizedType returnType = convert(ms.getReturnType());
        List<MethodInfo> overrides = computeMethodOverrides
                .findOverriddenMethods(ms)
                .stream().map(this::getOrLoadMethod)
                .toList();
        builder.setReturnType(returnType)
                .setMethodBody(runtime.emptyBlock())
                .addOverrides(overrides)
                .commitParameters()
                .computeAccess();
        // now the fully qualified name has been computed...

        clearTmpMethodTypeParameterMap(typeInfo.fullyQualifiedName());


        return method;
    }

    @Override
    public void setTopLevelClassSymbolsOfSources(IdentityHashMap<Symbol.ClassSymbol, Boolean> topLevelClassSymbolsOfSources) {
        this.topLevelClassSymbolsOfSources = topLevelClassSymbolsOfSources;
        this.infoByFqn.startOfNewSourceSet();
    }

    @Override
    public void startCompilationUnit(SourceProvider sourceProvider, ElementStack elementStack) {
        this.elementStack = elementStack;
        this.sourceProvider = sourceProvider;
    }

    // general method, returns null when 'cs' is not a functional type
    @Override
    public @Nullable MethodInfo computeSAM(Type type) {
        ConvertType.SAMDescriptor sd = findInstantiatedSAM(type);
        return sd == null ? null : sd.methodInfo();
    }

    @Override
    public FieldInfo ensureField(Symbol.VarSymbol vs) {
        if (vs.owner instanceof Symbol.ClassSymbol cs) {
            TypeInfo owner = convert(cs.type).typeInfo();
            String name = vs.getSimpleName().toString();
            boolean isStatic = vs.isStatic();
            ParameterizedType type = convert(vs.type);
            FieldInfo fieldInfo = runtime.newFieldInfo(name, isStatic, type, owner);
            // we might overwrite them, or not...
            fieldInfo.builder().setInitializer(runtime.newEmptyExpression()).setAccess(runtime.accessPublic());
            owner.builder().addField(fieldInfo);
            put(vs, fieldInfo);
            return fieldInfo;
        } else throw new UnsupportedOperationException();
    }

    @Override
    public MethodInfo ensureMethod(Symbol.MethodSymbol methodSymbol, boolean synthetic) {
        if (methodSymbol.owner instanceof Symbol.ClassSymbol cs) {
            TypeInfo owner = convert(cs.type).typeInfo();
            return addMethodToType(owner, methodSymbol, synthetic);
        } else throw new UnsupportedOperationException();
    }

    @Override
    public ConvertType.SAMDescriptor findInstantiatedSAM(Type functionalType) {
        if (!functionalType.tsym.isInterface()) return null;
        if (!types.isFunctionalInterface(functionalType.tsym)) return null;

        // Symbol — for name, flags, position, enclosing interface
        Symbol.MethodSymbol samSymbol = (Symbol.MethodSymbol) types.findDescriptorSymbol(functionalType.tsym);
        MethodInfo methodInfo = getOrLoadMethod(samSymbol);

        // Type — with type variables substituted for concrete args
        // e.g. Comparator<String> -> (String, String) -> int
        Type descriptorType = types.findDescriptorType(functionalType);
        Type.MethodType methodType = switch (descriptorType) {
            case Type.MethodType mt -> mt;
            case Type.ForAll forAll -> (Type.MethodType) forAll.qtype;
            default ->
                    throw new UnsupportedOperationException("unexpected descriptor type: " + descriptorType.getClass());
        };

        return new ConvertType.SAMDescriptor(methodInfo, samSymbol, methodType);
    }

    @Override
    public ParameterizedType convert(Type type) {
        if (type instanceof Type.JCPrimitiveType primitiveType) {
            return primitiveType(primitiveType.getKind());
        }
        if (type instanceof Type.IntersectionClassType ict) {
            if (ict.supertype_field != null) {
                return convert(ict.supertype_field);
            }
            throw new UnsupportedOperationException("NYI");
        }
        if (type instanceof Type.ClassType ct) {
            return classType(ct);
        }
        if (type instanceof Type.JCVoidType) {
            return runtime.voidParameterizedType();
        }
        if (type instanceof Type.ArrayType at) {
            ParameterizedType base = convert(at.elemtype);
            return base.copyWithArrays(base.arrays() + 1);
        }
        if (type instanceof Type.CapturedType ct && ct.wildcard.isUnbound()) {
            return runtime.parameterizedTypeWildcard();
        }
        if (type instanceof Type.WildcardType wildcardType) {
            if (wildcardType.isUnbound()) {
                return runtime.parameterizedTypeWildcard();
            }
            boolean isExtends = wildcardType.isExtendsBound();
            Wildcard wildCard = isExtends ? runtime.wildcardExtends() : runtime.wildcardSuper();
            ParameterizedType base = convert(wildcardType.type);
            if (base.isTypeParameter()) {
                return runtime.newParameterizedType(base.typeParameter(), 0, wildCard);
            }
            return runtime.newParameterizedType(base.typeInfo(), 0, wildCard, List.of());
        }
        if (type instanceof Type.TypeVar typeVar) {
            String typeParameterName = typeVar.tsym.toString();
            TypeParameter typeParameter;
            if (typeVar.tsym.owner instanceof Symbol.MethodSymbol ms) {
                if (ms.owner instanceof Symbol.ClassSymbol cs) {
                    String typeFqn = cs.fullname.toString();
                    TypeParameter tmpTypeParameter = getTmpMethodTypeParameter(typeFqn, typeParameterName);
                    // method must have been completed, look up!
                    // Note: it could be in a super-type (java.lang.Module -> java.lang.reflect.AnnotatedElement)
                    typeParameter = Objects.requireNonNullElseGet(tmpTypeParameter,
                            () -> findTypeParameter(ms, typeParameterName));
                } else throw new UnsupportedOperationException();
            } else if (typeVar.isCaptured()) {
                if (typeVar.getUpperBound() != null) {
                    TypeInfo upper = convert(typeVar.getUpperBound()).typeInfo();
                    return runtime.newParameterizedType(upper, 0, runtime.wildcardExtends(), List.of());
                } else {
                    throw new UnsupportedOperationException();
                }
            } else {
                String fullyQualifiedName = typeVar.tsym.owner.toString();
                TypeInfo owner = getType(fullyQualifiedName);
                assert owner != null;
                typeParameter = findTypeParameter(owner, typeParameterName);
            }
            return runtime.newParameterizedType(typeParameter, 0, null);
        }
        if ("none".equals(type.toString()) || type instanceof Type.PackageType) return null; // parent of Object
        throw new UnsupportedOperationException("NYI");
    }

    private @NotNull TypeParameter findTypeParameter(Symbol.MethodSymbol methodSymbol, String typeParameterName) {
        TypeParameter inStack = findInTypeParameterStack(typeParameterName);
        if (inStack != null) return inStack;
        MethodInfo owner = getOrLoadMethod(methodSymbol);
        assert owner != null;

        TypeParameter typeParameter = owner.typeParameters().stream()
                .filter(tp -> tp.simpleName().equals(typeParameterName))
                .findFirst().orElse(null);
        if (typeParameter != null) return typeParameter;
        return findTypeParameter(owner.typeInfo(), typeParameterName);
    }

    private @NotNull TypeParameter findTypeParameter(TypeInfo owner, String typeParameterName) {
        TypeParameter inStack = findInTypeParameterStack(typeParameterName);
        if (inStack != null) return inStack;
        TypeParameter typeParameter = owner.typeParameters().stream()
                .filter(tp -> tp.simpleName().equals(typeParameterName))
                .findFirst().orElse(null);
        if (typeParameter != null) return typeParameter;
        if (owner.compilationUnitOrEnclosingType().isRight()) {
            return findTypeParameter(owner.compilationUnitOrEnclosingType().getRight(), typeParameterName);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public ParameterizedType convertTree(Tree type, DetailedSources.Builder detailedSourcesBuilder) {
        if (type == null) {
            return runtime.voidParameterizedType();
        }
        Source source = sourceProvider.sourceForNode(type);
        ParameterizedType pt = convertTreeDontSet(type, detailedSourcesBuilder, source);
        assert pt != null;
        detailedSourcesBuilder.put(pt, source);
        return pt;
    }

    private ParameterizedType convertTreeDontSet(Tree type, DetailedSources.Builder dsb, Source source) {
        if (type instanceof JCTree.JCPrimitiveTypeTree ptt) {
            TypeKind primitiveTypeKind = ptt.typetag.getPrimitiveTypeKind();
            if (primitiveTypeKind != null) {
                ParameterizedType primitive = primitiveType(primitiveTypeKind);
                dsb.put(primitive.typeInfo(), source);
                return primitive;
            }
            throw new UnsupportedOperationException("Unknown primitive type kind " + ptt.typetag);
        }
        if (type instanceof JCTree.JCIdent identifier) {
            if (identifier.type instanceof Type.PackageType) return null;
            if (identifier.type instanceof Type.ClassType ct) {
                ParameterizedType pt = classType(ct);
                assert pt.typeInfo() != null;
                dsb.put(pt.typeInfo(), source);
                return pt;
            }
            if (identifier.type instanceof Type.TypeVar tv) {
                if (tv.isCaptured()) {
                    ParameterizedType pt = convert(tv.getUpperBound());
                    return pt.withWildcard(runtime.wildcardExtends());
                }
                String typeParameterName = identifier.getName().toString();
                TypeParameter tp = (TypeParameter) elementStack.find(typeParameterName);
                dsb.put(tp, source);
                return runtime.newParameterizedType(tp, 0, null);
            }
            throw new UnsupportedOperationException("Unknown identifier type " + identifier.type);
        }
        if (type instanceof JCTree.JCFieldAccess fieldAccess) {
            if (fieldAccess.type instanceof Type.PackageType) return null;
            // enclosing type notation
            ParameterizedType pt = convert(fieldAccess.type);
            assert pt != null;
            TypeInfo ti = pt.typeInfo();
            assert ti != null;
            dsb.put(ti, source);
            iterateUpToPackageLevel(dsb, fieldAccess, ti);
            return pt;
        }
        if (type instanceof JCTree.JCTypeApply apply) {
            ParameterizedType base = convertTree(apply.getType(), dsb);
            List<ParameterizedType> parameters = apply.getTypeArguments().stream()
                    .map(ta -> convertTree(ta, dsb)).toList();
            return runtime.newParameterizedType(base.typeInfo(), parameters);
        }
        if (type instanceof JCTree.JCArrayTypeTree att) {
            int n = 1;
            Tree t = att.elemtype;
            while (t instanceof JCTree.JCArrayTypeTree att2) {
                ++n;
                t = att2.elemtype;
            }
            ParameterizedType withoutArray = convertTree(t, dsb);
            ParameterizedType withArray = withoutArray.copyWithArrays(withoutArray.arrays() + n);
            dsb.putWithArrayToWithoutArray(withArray, withoutArray);
            return withArray;
        }
        if (type instanceof JCTree.JCWildcard) {
            return runtime.parameterizedTypeWildcard();
        }
        if (type instanceof JCTree.JCAnnotatedType at) {
            // TODO there is no room for this in maddi's model
            return convertTreeDontSet(at.underlyingType, dsb, source);
        }
        throw new UnsupportedOperationException("NYI");
    }

    /*
    important: we backtrack over 'ti' rather than computing the actual type, because it may be different!
    see TestFullyQualified,4, where X.II.J is not the default way of writing, X.I.J is.
     */
    private void iterateUpToPackageLevel(DetailedSources.Builder dsb, JCTree.JCFieldAccess fieldAccess, TypeInfo ti) {
        JCTree.JCExpression expression = fieldAccess.getExpression();
        while (true) {
            Source expressionSource = sourceProvider.sourceForNode(expression);
            if (expression.type instanceof Type.PackageType) {
                String packageName = ti.packageName();
                dsb.put(packageName, expressionSource);
                break;
            }
            if (expression instanceof JCTree.JCIdent) {
                if (ti.compilationUnitOrEnclosingType().isRight()) {
                    ti = ti.compilationUnitOrEnclosingType().getRight();
                    dsb.put(ti, expressionSource);
                }// else: .class
                break;
            }
            if (expression instanceof JCTree.JCFieldAccess fa) {
                if (ti.compilationUnitOrEnclosingType().isRight()) {
                    ti = ti.compilationUnitOrEnclosingType().getRight();
                    dsb.put(ti, expressionSource);
                    expression = fa.getExpression();
                } else {
                    break;// .class
                }
            } else {
                break;
            }
        }
    }

    private ParameterizedType classType(Type.ClassType ct) {
        TypeInfo typeInfo = classTypeInfo(ct);
        if (ct.getTypeArguments().isEmpty()) {
            return typeInfo.asSimpleParameterizedType();
        }
        List<ParameterizedType> typeParameters = ct.getTypeArguments().stream().map(this::convert).toList();
        return runtime.newParameterizedType(typeInfo, typeParameters);
    }

    private TypeInfo classTypeInfo(Type.ClassType ct) {
        String fullyQualifiedType = ct.tsym.toString();
        TypeInfo known = getType(fullyQualifiedType);
        Symbol.ClassSymbol cs = (Symbol.ClassSymbol) ct.tsym;
        TypeInfo typeInfo;
        Symbol.ClassSymbol topCs;
        if (known == null) {
            // on-demand loading; should be replaced by import handling?
            if (cs.owner instanceof Symbol.MethodSymbol) {
                typeInfo = (TypeInfo) elementStack.find(cs.getSimpleName().toString());
            } else {
                typeInfo = lazilyLoadTypeFromClassFile(cs);
            }
        } else {
            CompilationUnit knownCu = known.compilationUnit();
            if (topLevelClassSymbolsOfSources != null
                && topLevelClassSymbolsOfSources.containsKey(topCs = primary(cs))) {
                // ct/cs is one of the source files that we are parsing at the moment
                if (cs.sourcefile != null
                    && !knownCu.uri().equals(topCs.sourcefile.toUri())) {
                    // so if the source file does not agree with known, we must load the class file
                    typeInfo = lazilyLoadTypeFromClassFile(cs);
                } else {
                    typeInfo = known;
                }
            } else if (knownCu.sourceSet() != null // badly loaded type
                       && !knownCu.sourceSet().partOfJdk()
                       && knownCu.sourceSet().externalLibrary()
                       && (topCs = primary(cs)).classfile != null
                       && !knownCu.uri().equals(topCs.classfile.toUri())) {
                // we'll be overwriting the existing type in InfoByFqn
                // this will only happen when the 'known' is known from the compilation of a previous source set
                // in the current source set, the javac analysis will have selected the first.
                typeInfo = lazilyLoadTypeFromClassFile(cs);
            } else {
                typeInfo = known;
            }
        }
        return typeInfo;
    }

    private static Symbol.ClassSymbol primary(Symbol.ClassSymbol csIn) {
        Symbol.ClassSymbol cs = csIn;
        while (cs.owner instanceof Symbol.ClassSymbol owner) {
            cs = owner;
        }
        return cs;
    }

    private ParameterizedType primitiveType(TypeKind primitiveTypeKind) {
        return switch (primitiveTypeKind) {
            case VOID -> runtime.voidParameterizedType();
            case BYTE -> runtime.byteParameterizedType();
            case INT -> runtime.intParameterizedType();
            case DOUBLE -> runtime.doubleParameterizedType();
            case LONG -> runtime.longParameterizedType();
            case FLOAT -> runtime.floatParameterizedType();
            case SHORT -> runtime.shortParameterizedType();
            case BOOLEAN -> runtime.booleanParameterizedType();
            case CHAR -> runtime.charParameterizedType();
            default -> throw new UnsupportedOperationException();
        };
    }


    private void newTypeParameterMap() {
        typeParameterStack.addLast(new HashMap<>());
    }

    private void popTypeParameterMap() {
        typeParameterStack.removeLast();
    }

    private void putInLastTypeParameterMap(TypeParameter typeParameter) {
        typeParameterStack.getLast().put(typeParameter.simpleName(), typeParameter);
    }

    private TypeParameter findInTypeParameterStack(String name) {
        if (!typeParameterStack.isEmpty()) {
            return typeParameterStack.getLast().get(name);
        }
        return null;
    }

    // type data

    private void clearTmpMethodTypeParameterMap(String typeFqn) {
        tmpMethodTypeParameterMap.remove(typeFqn);
    }

    private void putTmpMethodTypeParameter(String typeFqn, String methodTpName, TypeParameter methodTp) {
        tmpMethodTypeParameterMap.computeIfAbsent(typeFqn, _ -> new HashMap<>()).put(methodTpName, methodTp);
    }

    private TypeParameter getTmpMethodTypeParameter(String typeFqn, String methodTpName) {
        Map<String, TypeParameter> typeParameterMap = tmpMethodTypeParameterMap.get(typeFqn);
        return typeParameterMap == null ? null : typeParameterMap.get(methodTpName);
    }

    private SourceSet getSourceSet(String name) {
        return sourceSetMap.get(name);
    }

    record TypeDistance(TypeInfo typeInfo, int distance) implements Comparable<TypeDistance> {
        @Override
        public int compareTo(@NotNull ClassSymbolScanner.TypeDistance o) {
            int c = Integer.compare(distance, o.distance);
            if (c != 0) return c;
            return typeInfo.compilationUnit().sourceSet().name()
                    .compareTo(o.typeInfo.compilationUnit().sourceSet().name());
        }
    }

    @Override
    public TypeInfo getType(String fullyQualifiedName) {
        return infoByFqn.getType(fullyQualifiedName, sourceSetOfCurrentTask);
    }

    @Override
    public void put(TypeInfo typeInfo) {
        String fqn = typeInfo.fullyQualifiedName();
        infoByFqn.put(fqn, typeInfo, sourceSetOfCurrentTask);
    }

    @Override
    public void put(String anonymousTypeName, TypeInfo typeInfo) {
        infoByFqn.put(anonymousTypeName, typeInfo, sourceSetOfCurrentTask);
    }

    @Override
    public void put(Symbol.MethodSymbol methodSymbol, MethodInfo methodInfo) {
        MethodInfo prev = methodSymbolMap.put(methodSymbol, methodInfo);
        assert prev == null : "Duplicating MethodInfo " + methodInfo;
    }

    Symbol.MethodSymbol theMethod(Symbol.MethodSymbol methodSymbol) {
        boolean isDefault = (methodSymbol.flags() & Flags.DEFAULT) != 0;
        Symbol.MethodSymbol result;
        if (!isDefault && methodSymbol.baseSymbol() instanceof Symbol.MethodSymbol baseSymbol) {
            result = baseSymbol;
        } else {
            result = methodSymbol;
        }
        assert (result.flags() & Flags.BRIDGE) == 0;
        return result;
    }

    @Override
    public MethodInfo getMethod(Symbol.MethodSymbol methodSymbol) {
        MethodInfo fromSymbol;
        if (methodSymbol.baseSymbol() instanceof Symbol.MethodSymbol ms) {
            fromSymbol = methodSymbolMap.get(ms);
        } else {
            fromSymbol = methodSymbolMap.get(methodSymbol);
        }
        if (fromSymbol != null) return fromSymbol;

        // now check methods from previous source sets
        // FIXME this is slow, we may want to speed this up? e.g. hashmap name+size
        if (methodSymbol.owner instanceof Symbol.ClassSymbol cs) {
            TypeInfo typeInfo = convert(cs.type).typeInfo();
            if (methodSymbol.isConstructor()) {
                return typeInfo.constructors().stream().filter(c -> sameTypes(c.parameters(), methodSymbol.params))
                        .findFirst().orElse(null);
            }
            String methodName = methodSymbol.name.toString();
            return typeInfo.methodStream()
                    .filter(mi -> mi.name().equals(methodName) && sameTypes(mi.parameters(), methodSymbol.params))
                    .findFirst().orElse(null);
        }
        throw new UnsupportedOperationException("?");
    }

    private boolean sameTypes(List<ParameterInfo> parameters, List<Symbol.VarSymbol> params) {
        if (parameters.size() != params.size()) return false;
        int i = 0;
        for (ParameterInfo pi : parameters) {
            String erased = pi.parameterizedType().erasedForFQN().fullyQualifiedName();
            Symbol.VarSymbol vs = params.get(i++);
            String erased2 = convert(types.erasure(vs.type)).fullyQualifiedName();
            if (!erased.equals(erased2)) return false;
        }
        return true;
    }

    @Override
    public MethodInfo getOrLoadMethod(Symbol.MethodSymbol methodSymbol) {
        Symbol.MethodSymbol theMethod = theMethod(methodSymbol);
        MethodInfo inMap = getMethod(theMethod);
        if (inMap != null) return inMap;
        return ensureMethod(theMethod, false);
    }

    @Override
    public void put(Symbol.VarSymbol varSymbol, FieldInfo fieldInfo) {
        FieldInfo prev = varSymbolMap.put(varSymbol, fieldInfo);
        assert prev == null : "Duplicating FieldInfo " + fieldInfo;
    }

    @Override
    public FieldInfo getField(Symbol.VarSymbol varSymbol) {
        FieldInfo fromSymbol = varSymbolMap.get(varSymbol);
        if (fromSymbol != null) return fromSymbol;

        if (varSymbol.owner instanceof Symbol.ClassSymbol cs) {
            TypeInfo typeInfo = convert(cs.type).typeInfo();
            String fieldName = varSymbol.name.toString();
            return typeInfo.getFieldByName(fieldName, false);
        }

        return null;
    }

    @Override
    public FieldInfo getOrLoadField(Symbol.VarSymbol vs) {
        return Objects.requireNonNullElseGet(getField(vs), () -> ensureField(vs));
    }

    public Collection<TypeInfo> typesLoaded() {
        return infoByFqn.typesLoaded();
    }

    public void commitType(TypeInfo typeInfo) {
        if (!typeInfo.hasBeenInspected()) {
            try {
                TypeElement typeElement = elements.getTypeElement(typeInfo.fullyQualifiedName());
                Symbol.ClassSymbol cs = (Symbol.ClassSymbol) typeElement;
                loadType(cs, typeInfo, LoadMode.COMPLETE);
            } catch (RuntimeException | AssertionError | StackOverflowError re) {
                LOGGER.error("Caught exception committing type {}", typeInfo);
                throw re;
            }
        }
    }
}
