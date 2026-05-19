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

    private final Map<String, TypeInfo> singleTypeForFQN = new HashMap<>();
    private final Map<Symbol.MethodSymbol, MethodInfo> methodSymbolMap = new IdentityHashMap<>();
    private final Map<Symbol.VarSymbol, FieldInfo> varSymbolMap = new IdentityHashMap<>();
    private final Map<String, Map<String, TypeParameter>> tmpMethodTypeParameterMap = new HashMap<>();

    // not thread-safe: set for each compilation unit
    private SourceProvider sourceProvider;
    private ElementStack elementStack;

    public enum LoadMode {
        LOAD_MEMBERS, // load everything immediately, and commit
        LAZILY,  // only load what is required, and store in "loaded"
        COMPLETE, // uses to complete a lazily loaded type; check with "loaded"
        COMPLETE_SUB,
    }

    public ClassSymbolScanner(Runtime runtime,
                              InputConfiguration inputConfiguration,
                              SourceSet sourceSetOfCurrentTask,
                              FlagHelper flagHelper,
                              Types types,
                              Elements elements) {
        this.runtime = runtime;
        this.flagHelper = flagHelper;
        this.elements = elements;
        this.types = types;
        this.sourceSetOfCurrentTask = sourceSetOfCurrentTask;
        assert inputConfiguration.sourceSets().contains(sourceSetOfCurrentTask);

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
        predefinedTypes.put("Void", runtime.boxedByteTypeInfo());

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

    TypeInfo type(Symbol.ClassSymbol cs) {
        switch (cs.owner) {
            case Symbol.PackageSymbol _ -> {
                return primaryType(cs);
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

    private TypeInfo primaryType(Symbol.ClassSymbol cs) {
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
            internal = cs.classfile == null;
            URI uri;
            if (internal) {
                uri = URI.create("jrt:/internal/");
            } else {
                uri = cs.classfile.toUri();
            }
            SourceSet sourceSet = ensureSourceSet(cs, uri);
            CompilationUnit cu = runtime.newCompilationUnitBuilder()
                    .setPackageName(packageName)
                    .setSourceSet(sourceSet)
                    .setURI(uri)
                    .build();
            newTypeInfo = runtime.newTypeInfo(cu, simpleName);
        }
        put(newTypeInfo);
        if (!internal) {
            loadType(cs, newTypeInfo, LoadMode.LAZILY);
        }
        return newTypeInfo;
    }

    void loadType(Symbol.ClassSymbol cs, TypeInfo newTypeInfo, LoadMode loadMode) {
        LOGGER.info("Enter loadType: {} {}", newTypeInfo.fullyQualifiedName(), loadMode);
        flagHelper.type(cs, newTypeInfo.builder());
        if (recursionPrevention.add(newTypeInfo)) {
            //The following completely loads 'cs'
            List<? extends Element> members = elements.getAllMembers(cs);

            if (loadMode != LoadMode.COMPLETE) {
                int index = 0;
                newTypeParameterMap();
                for (Symbol.TypeVariableSymbol typeParameter : cs.getTypeParameters()) {
                    TypeParameter newTp = runtime.newTypeParameter(index++, typeParameter.getSimpleName().toString(), newTypeInfo);
                    putInLastTypeParameterMap(newTp);

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
                                    ParameterizedType upper = convert(ct);
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
                    newTypeInfo.builder().addOrSetTypeParameter(newTp);
                }
                popTypeParameterMap();

                ParameterizedType superType = convert(cs.getSuperclass());
                ParameterizedType parentClass = superType == null ? runtime.objectParameterizedType() : superType;
                assert parentClass != null;
                newTypeInfo.builder().setParentClass(parentClass);

                for (Type type : cs.getInterfaces()) {
                    ParameterizedType pt = convert(type);
                    newTypeInfo.builder().addInterfaceImplemented(pt);
                }
            }

            if (loadMode != LoadMode.LAZILY) {
                for (var member : members) {
                    addMemberToType(newTypeInfo, cs, member, loadMode);
                }

                MethodInfo singleAbstractMethod = computeSAM(cs.type);
                newTypeInfo.builder().setSingleAbstractMethod(singleAbstractMethod);
                newTypeInfo.builder().commit(); // everything loaded!
            }
            recursionPrevention.remove(newTypeInfo);
        }
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
                LOGGER.info("?");
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
        if (member instanceof Symbol.MethodSymbol ms && ms.owner == owner) {
            boolean isPublic = (ms.flags() & Flags.PUBLIC) != 0;
            if (isPublic && (loadMode == LoadMode.LOAD_MEMBERS
                             || loadMode == LoadMode.COMPLETE && !methodSymbolMap.containsKey(ms))) {
                addMethodToType(typeInfo, ms, false);
            }
        } else if (member instanceof Symbol.VarSymbol vs && vs.owner == owner) {
            boolean isPublic = (vs.flags() & Flags.PUBLIC) != 0;
            if (isPublic && (loadMode == LoadMode.LOAD_MEMBERS
                             || loadMode == LoadMode.COMPLETE && !varSymbolMap.containsKey(vs))) {
                addFieldToType(typeInfo, vs);
            }
        } else if (member instanceof Symbol.ClassSymbol cs && cs.owner == owner) {
            boolean isPublic = (cs.flags() & Flags.PUBLIC) != 0;
            if (isPublic && (loadMode == LoadMode.LOAD_MEMBERS
                             || loadMode == LoadMode.COMPLETE
                                && typeInfo.findSubType(cs.getSimpleName().toString(), false) == null)) {
                addEnclosedTypeToType(typeInfo, cs, loadMode);
            }
        }
    }

    private void addEnclosedTypeToType(TypeInfo typeInfo, Symbol.ClassSymbol cs, LoadMode loadMode) {
        if (getType(cs.fullname.toString()) != null) return;
        String name = cs.getSimpleName().toString();
        LOGGER.debug("Adding enclosed type {} to {}", name, typeInfo);
        TypeInfo enclosed = runtime.newTypeInfo(typeInfo, name);
        put(enclosed);
        typeInfo.builder().addSubType(enclosed);
        // we must do type parameters, interfaces, parent class etc.
        loadType(cs, enclosed, loadMode == LoadMode.COMPLETE ? LoadMode.COMPLETE_SUB : loadMode);
    }

    private void addFieldToType(TypeInfo typeInfo, Symbol.VarSymbol vs) {
        String name = vs.getSimpleName().toString();
        LOGGER.debug("Adding field {} to {}", name, typeInfo);
        ParameterizedType type = convert(vs.type);
        boolean isStatic = (vs.flags() & Flags.STATIC) != 0;
        FieldInfo fieldInfo = runtime.newFieldInfo(name, isStatic, type, typeInfo);
        typeInfo.builder().addField(fieldInfo);
        flagHelper.field(vs.flags(), fieldInfo.builder());

        put(vs, fieldInfo);
    }

    MethodInfo addMethodToType(TypeInfo typeInfo, Symbol.MethodSymbol ms, boolean synthetic) {
        String name = ms.getSimpleName().toString();
        MethodInfo method;
        if ("<init>".equals(name)) {
            LOGGER.debug("Adding constructor {} to {}", name, typeInfo);
            method = runtime.newConstructor(typeInfo);
            typeInfo.builder().addConstructor(method);
        } else {
            LOGGER.debug("Adding method {} to {}", name, typeInfo);
            MethodInfo.MethodType methodType = flagHelper.methodType(ms.flags(),
                    typeInfo.isInterface() || typeInfo.isAnnotation());
            method = runtime.newMethod(typeInfo, name, methodType);
            typeInfo.builder().addMethod(method);
        }
        int index = 0;
        MethodInfo.Builder builder = method.builder();

        for (Symbol.TypeVariableSymbol typeParameter : ms.getTypeParameters()) {
            TypeParameter newTp = runtime.newTypeParameter(index++, typeParameter.getSimpleName().toString(), method);
            builder.addTypeParameter(newTp);
            putTmpMethodTypeParameter(typeInfo.fullyQualifiedName(), newTp.simpleName(), newTp);
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
        builder.setReturnType(returnType);

        builder.commitParameters();
        // now the fully qualified name has been computed...

        clearTmpMethodTypeParameterMap(typeInfo.fullyQualifiedName());
        put(ms, method);

        return method;
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
                    if (tmpTypeParameter == null) {
                        // method must have been completed, look up!
                        // Note: it could be in a super-type (java.lang.Module -> java.lang.reflect.AnnotatedElement)
                        MethodInfo owner = getMethod(ms);
                        assert owner != null;
                        typeParameter = findTypeParameter(owner, typeParameterName);
                    } else {
                        typeParameter = tmpTypeParameter;
                    }
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
        if ("none".equals(type.toString())) return null; // parent of Object
        throw new UnsupportedOperationException("NYI");
    }

    private @NotNull TypeParameter findTypeParameter(MethodInfo owner, String typeParameterName) {
        TypeParameter inStack = findInTypeParameterStack(typeParameterName);
        if (inStack != null) return inStack;
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
        ParameterizedType pt = convertTreeDontSet(type, detailedSourcesBuilder);
        assert pt != null;
        Source source = sourceProvider.sourceForNode(type);
        detailedSourcesBuilder.put(pt, source);
        // FIXME do we need the following ?
        if (pt.typeInfo() != null && pt.parameters().isEmpty() && pt.arrays() == 0) {
            detailedSourcesBuilder.put(pt.typeInfo(), source);
        }
        return pt;
    }

    private ParameterizedType convertTreeDontSet(Tree type, DetailedSources.Builder detailedSourcesBuilder) {
        if (type == null) return runtime.voidParameterizedType();
        if (type instanceof JCTree.JCPrimitiveTypeTree ptt) {
            TypeKind primitiveTypeKind = ptt.typetag.getPrimitiveTypeKind();
            if (primitiveTypeKind != null) {
                return primitiveType(primitiveTypeKind);
            }
        }
        if (type instanceof JCTree.JCIdent identifier) {
            if (identifier.type instanceof Type.ClassType ct) {
                return classType(ct);
            } else if (identifier.type instanceof Type.TypeVar) {
                String typeParameterName = identifier.getName().toString();
                TypeParameter tp = (TypeParameter) elementStack.find(typeParameterName);
                return runtime.newParameterizedType(tp, 0, null);
            } else {
                throw new UnsupportedOperationException("NYI");
            }
        }
        if (type instanceof JCTree.JCFieldAccess fieldAccess) {
            // enclosing type notation
            String name = fieldAccess.name.toString();
            // recursively descend, but ignore result
            // FIXME   convertTree(fieldAccess.getExpression(), detailedSourcesBuilder);
            ParameterizedType pt = convert(fieldAccess.type);
            if (!"class".equals(name) && pt.typeInfo() != null) {
                String packageName = pt.typeInfo().packageName();
                detailedSourcesBuilder.put(packageName, sourceProvider.sourceForNode(fieldAccess.getExpression()));
            }// else: java.lang.String.class
            return pt;
        }
        if (type instanceof JCTree.JCTypeApply apply) {
            ParameterizedType base = convertTree(apply.getType(), detailedSourcesBuilder);
            List<ParameterizedType> parameters = apply.getTypeArguments().stream()
                    .map(ta -> convertTree(ta, detailedSourcesBuilder)).toList();
            return runtime.newParameterizedType(base.typeInfo(), parameters);
        }
        if (type instanceof JCTree.JCArrayTypeTree att) {
            int n = 1;
            Tree t = att.elemtype;
            while (t instanceof JCTree.JCArrayTypeTree att2) {
                ++n;
                t = att2.elemtype;
            }
            ParameterizedType withoutArray = convertTree(t, detailedSourcesBuilder);
            ParameterizedType withArray = withoutArray.copyWithArrays(withoutArray.arrays() + n);
            detailedSourcesBuilder.putWithArrayToWithoutArray(withArray, withoutArray);
            return withArray;
        }
        if (type instanceof JCTree.JCWildcard) {
            return runtime.parameterizedTypeWildcard();
        }
        throw new UnsupportedOperationException("NYI");
    }

    private ParameterizedType classType(Type.ClassType ct) {
        String fullyQualifiedType = ct.tsym.toString();
        TypeInfo known = getType(fullyQualifiedType);
        TypeInfo typeInfo;
        if (known == null) {
            // on-demand loading; should be replaced by import handling?
            if (ct.tsym instanceof Symbol.ClassSymbol cs) {
                if (cs.owner instanceof Symbol.MethodSymbol) {
                    typeInfo = (TypeInfo) elementStack.find(cs.getSimpleName().toString());
                } else {
                    typeInfo = type(cs);
                }
            } else throw new UnsupportedOperationException("NYI");
        } else {
            typeInfo = known;
        }
        if (ct.getTypeArguments().isEmpty()) {
            return typeInfo.asSimpleParameterizedType();
        }
        List<ParameterizedType> typeParameters = ct.getTypeArguments().stream().map(this::convert).toList();
        return runtime.newParameterizedType(typeInfo, typeParameters);
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

    @Override
    public TypeInfo getType(String fullyQualifiedName) {
        return singleTypeForFQN.get(fullyQualifiedName);
    }

    @Override
    public void put(TypeInfo typeInfo) {
        TypeInfo prev = singleTypeForFQN.put(typeInfo.fullyQualifiedName(), typeInfo);
        assert prev == null : "Duplicating TypeInfo " + typeInfo;
    }

    @Override
    public void put(String anonymousTypeName, TypeInfo typeInfo) {
        TypeInfo prev = singleTypeForFQN.put(anonymousTypeName, typeInfo);
        assert prev == null : "Duplicating TypeInfo " + typeInfo;
    }

    @Override
    public void put(Symbol.MethodSymbol methodSymbol, MethodInfo methodInfo) {
        MethodInfo prev = methodSymbolMap.put(methodSymbol, methodInfo);
        assert prev == null : "Duplicating MethodInfo " + methodInfo;
    }

    Symbol.MethodSymbol theMethod(Symbol.MethodSymbol methodSymbol) {
        if (methodSymbol.baseSymbol() instanceof Symbol.MethodSymbol ms) {
            return ms;
        }
        return methodSymbol;
    }

    @Override
    public MethodInfo getMethod(Symbol.MethodSymbol methodSymbol) {
        if (methodSymbol.baseSymbol() instanceof Symbol.MethodSymbol ms) {
            return methodSymbolMap.get(ms);
        }
        return methodSymbolMap.get(methodSymbol);
    }

    @Override
    public MethodInfo getOrLoadMethod(Symbol.MethodSymbol methodSymbol) {
        Symbol.MethodSymbol theMethod = theMethod(methodSymbol);
        MethodInfo inMap = methodSymbolMap.get(theMethod);
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
        return varSymbolMap.get(varSymbol);
    }

    @Override
    public FieldInfo getOrLoadField(Symbol.VarSymbol vs) {
        return Objects.requireNonNullElseGet(getField(vs), () -> ensureField(vs));
    }

    public Collection<TypeInfo> typesLoaded() {
        return singleTypeForFQN.values();
    }

    public void commitType(TypeInfo typeInfo) {
        if (!typeInfo.hasBeenInspected()) {
            TypeElement typeElement = elements.getTypeElement(typeInfo.fullyQualifiedName());
            Symbol.ClassSymbol cs = (Symbol.ClassSymbol) typeElement;
            loadType(cs, typeInfo, LoadMode.COMPLETE);
            assert typeInfo.hasBeenInspected();
        }
    }

}
