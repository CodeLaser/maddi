package org.e2immu.language.java.openjdk;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.Wildcard;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.api.resource.ParameterNameIndex;
import org.e2immu.language.inspection.api.util.CreateSyntheticFieldsForGetSet;
import org.e2immu.language.inspection.resource.InfoByFqn;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
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
    // loadType is reachable more than once per type (LAZILY then LOAD_MEMBERS); annotations are appended, so
    // guard the type-level add to run exactly once
    private final Set<TypeInfo> typeAnnotationsLoaded = Collections.newSetFromMap(new IdentityHashMap<>());
    // interfaces are likewise appended (addInterfaceImplemented), so a second pass would duplicate them: guard too
    private final Set<TypeInfo> typeInterfacesLoaded = Collections.newSetFromMap(new IdentityHashMap<>());
    // the type-setup block (access, type parameters, parent class, interfaces) must run exactly once per type,
    // and BEFORE the type is committed. Normally a LAZILY load does it and a later COMPLETE relies on that; but a
    // type reached only via bytecode (e.g. a transitively-referenced JDK internal) can arrive at COMPLETE without a
    // prior LAZILY, leaving parentClass/access null -> commit fails. The guard lets COMPLETE run the setup when it
    // was skipped, while never running it twice (which would re-create/commit type parameters and duplicate the
    // appended interfaces/annotations). It lives on the shared InfoByFqn -- NOT here -- because TypeInfo instances
    // outlive any single scanner: a fresh scanner (next parse's on-demand load) must see a prior scanner's setup as
    // already done. See InfoByFqn.markClassScannerSetupDone.
    private final Map<String, TypeInfo> predefinedTypes = new HashMap<>();
    private final Deque<Map<String, TypeParameter>> typeParameterStack = new ArrayDeque<>();
    private final Map<String, SourceSet> sourceSetMap;
    private final Map<String, SourceSet> sourceSetDirPrefixes;
    private final ComputeMethodOverrides computeMethodOverrides;
    private final MaddiDiagnosticCollector diagnosticCollector;
    // when non-null, supplies faithful formal parameter names for class-file methods (javac only gives arg0,...)
    private final ParameterNameIndex parameterNameIndex;
    // "we're working with JDK internals": when true, jdk.internal.* types are loaded from bytecode like any other
    // type (nature/parent/access set) instead of being left as bare stubs. Needed to analyze code that uses JDK
    // internals (e.g. the java.net.http sources, for hint deduction).
    private final boolean jdkInternals;

    private final InfoByFqn infoByFqn; // Javac qualified name -> typeInfo, methodInfo, fieldInfo
    private final Map<Symbol.MethodSymbol, MethodInfo> methodSymbolMap = new IdentityHashMap<>();
    private final Map<Symbol.VarSymbol, FieldInfo> varSymbolMap = new IdentityHashMap<>();
    private final Map<String, Map<String, TypeParameter>> tmpMethodTypeParameterMap = new HashMap<>();
    private final CreateSyntheticFieldsForGetSet createSyntheticFieldsForGetSet;

    // not thread-safe: set for each compilation unit
    private SourceProvider sourceProvider;
    private ElementStack elementStack;
    private IdentityHashMap<Symbol.ClassSymbol, Boolean> topLevelClassSymbolsOfSources;
    // source-file URI -> its CompilationUnit, built up front by ScanCompilationUnits before any body is scanned;
    // lets a source type referenced before its own scan load onto its real (source-bearing) CU (see
    // lazilyLoadPrimaryTypeFromClassFile) instead of a source-less twin
    private final Map<URI, CompilationUnit> sourceCompilationUnits = new HashMap<>();

    public void registerSourceCompilationUnit(CompilationUnit compilationUnit) {
        if (compilationUnit != null && compilationUnit.uri() != null) {
            sourceCompilationUnits.put(compilationUnit.uri(), compilationUnit);
        }
    }

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
                              MaddiDiagnosticCollector maddiDiagnosticCollector,
                              @Nullable ParameterNameIndex parameterNameIndex,
                              boolean jdkInternals) {
        this.runtime = runtime;
        this.flagHelper = flagHelper;
        this.elements = elements;
        this.types = types;
        this.infoByFqn = Objects.requireNonNull(infoByFqn);
        this.sourceSetOfCurrentTask = sourceSetOfCurrentTask;
        assert inputConfiguration.sourceSets().contains(sourceSetOfCurrentTask);
        this.diagnosticCollector = maddiDiagnosticCollector;
        this.parameterNameIndex = parameterNameIndex;
        this.jdkInternals = jdkInternals;
        this.computeMethodOverrides = new ComputeMethodOverrides(types, elements);
        this.createSyntheticFieldsForGetSet = new CreateSyntheticFieldsForGetSet(runtime);

        runtime.predefinedObjects().forEach(pd -> predefinedTypes.put(pd.simpleName(), pd));

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

            case null -> throw new UnsupportedOperationException("Null owner for type " + cs.fullname);
            default -> {
                if (cs.owner.kind == Kinds.Kind.NIL) {
                    throw new UnresolvedSymbolException("Type " + cs.fullname + " not found");
                }
                throw new UnsupportedOperationException();
            }
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
            URI uri;
            CompilationUnit cu;
            CompilationUnit registeredSourceCu = cs.sourcefile == null ? null
                    : sourceCompilationUnits.get(cs.sourcefile.toUri());
            CompilationUnit currentCu = sourceProvider == null ? null : sourceProvider.currentCompilationUnit();
            if (registeredSourceCu != null) {
                // a source type referenced before (or while) its own source is scanned: reuse the source
                // CompilationUnit built up front for that file (it carries the file's source), so this lazy load
                // does not mint a source-less twin CompilationUnit that the source scan would later reuse
                cu = registeredSourceCu;
                internal = false;
            } else if (currentCu != null && cs.sourcefile != null && cs.sourcefile.toUri().equals(currentCu.uri())) {
                // a forward reference (an 'extends'/'implements' naming a type declared later in the same source file
                // being scanned): reuse that file's CompilationUnit, so all its top-level types share one instance
                // rather than this load minting a second, equal-but-not-identical one
                cu = currentCu;
                internal = false;
            } else if (cs.classfile == null) {
                LOGGER.warn("Creating stub type for {}", cs);
                cu = runtime.newCompilationUnitStub(packageName);
                internal = false;
            } else {
                // jdk.internal.* is normally left as a stub (not loaded); with the JDK-internals flag we load it
                // like any other class-file type, so its nature/parent/access are set and referencing types commit.
                internal = !jdkInternals && cs.packge().toString().startsWith("jdk.internal.");
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
        LOGGER.debug("Enter loadType: {} {}", newTypeInfo, loadMode);
        TypeInfo.Builder builder = newTypeInfo.builder();

        if (recursionPrevention.add(newTypeInfo)) {
            //The following completely loads 'cs', so leave it here even though it can move nearer to its usage
            List<? extends Element> members = elements.getAllMembers(cs);
            flagHelper.type(cs, builder);
            if (infoByFqn.markClassScannerSetupDone(newTypeInfo)) {
                // ensure that the enclosing types have at least been lazily loaded; so that we can compute access
                // as soon as possible
                if (newTypeInfo.compilationUnitOrEnclosingType().isRight()) {
                    if (cs.owner instanceof Symbol.ClassSymbol enclosing) {
                        loadType(enclosing, newTypeInfo.compilationUnitOrEnclosingType().getRight(), LoadMode.LAZILY);
                    }
                }
                builder.computeAccess();
                if (typeAnnotationsLoaded.add(newTypeInfo)) {
                    builder.addAnnotations(loadAnnotations(cs));
                }

                int index = 0;
                newTypeParameterMap();
                List<TypeParameter> created = new ArrayList<>();
                for (Symbol.TypeVariableSymbol typeParameter : cs.getTypeParameters()) {
                    TypeParameter newTp = runtime.newTypeParameter(index++, typeParameter.getSimpleName().toString(), newTypeInfo);
                    putInLastTypeParameterMap(newTp);
                    created.add(newTp);
                    builder.addOrSetTypeParameter(newTp);
                }
                int i = 0;
                for (Symbol.TypeVariableSymbol typeParameter : cs.getTypeParameters()) {
                    TypeParameter newTp = created.get(i++);
                    addTypeBoundsAndCommit(cs, newTypeInfo, typeParameter, newTp);
                }
                popTypeParameterMap();

                ParameterizedType superType = convert(cs.getSuperclass());
                if (!newTypeInfo.isJavaLangObject()) {
                    ParameterizedType parentClass = superType == null ? runtime.objectParameterizedType() : superType;
                    assert parentClass != null;
                    builder.setParentClass(parentClass);
                }
                // Do not load the interfaces of a *source* type here: when such a type is referenced before its own
                // source is scanned, the class scanner reaches it (via lazilyLoadTypeFromClassFile), and then
                // ScanCompilationUnit also adds the interfaces from source -- since addInterfaceImplemented appends,
                // the result is a duplicated interface list. A source type's hierarchy is owned by ScanCompilationUnit
                // (annotations already have this guard: loadAnnotations() returns empty for source symbols).
                // The typeInterfacesLoaded set additionally guards the LAZILY-then-LOAD_MEMBERS re-entry for genuine
                // class-file types (a single TypeInfo whose interface block runs twice in one scan).
                // STOPGAP: the proper fix is a linear inspection-state on the builder (DEFINED_BY_CLASS_SCANNER vs
                // DEFINED_IN_SOURCE), which would make the double-load impossible (and assertable) rather than guarded.
                if (!isSourceSymbol(cs) && typeInterfacesLoaded.add(newTypeInfo)) {
                    for (Type type : cs.getInterfaces()) {
                        ParameterizedType pt = convert(type);
                        builder.addInterfaceImplemented(pt);
                    }
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
                                newTp.typeInfo().typeParameters().stream().map(NamedType::asParameterizedType).toList()));
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
        newTp.builder().addAnnotations(loadAnnotations(typeParameter)).setTypeBounds(List.copyOf(bounds)).commit();
    }

    // looks up faithful parameter names for the method being built, keyed on its (erased) signature; null when
    // there is no index, no parameters, or no entry — callers then keep javac's synthetic arg0, arg1, ... names
    private List<String> lookupParameterNames(TypeInfo typeInfo, String name, List<ParameterizedType> paramTypes) {
        if (parameterNameIndex == null || paramTypes.isEmpty()) return null;
        try {
            List<String> erasedParamFqns = paramTypes.stream()
                    .map(pt -> pt.erasedForFQN().fullyQualifiedName()).collect(Collectors.toList());
            return parameterNameIndex.parameterNames(
                    ParameterNameIndex.key(typeInfo.fullyQualifiedName(), name, erasedParamFqns));
        } catch (RuntimeException re) {
            LOGGER.warn("Cannot look up parameter names for {}.{}: {}", typeInfo, name, re.toString());
            return null;
        }
    }

    // ---- declaration annotations, read from a class file via the symbol's annotation mirrors ----
    // (only RUNTIME/CLASS-retained annotations are present in bytecode; SOURCE-retained ones are not). Everything
    // is defensive: a value we cannot represent is skipped, and a failure never aborts the type's loading.

    // true when 'symbol' belongs to a type we are currently parsing from source: those receive their
    // annotations from ScanCompilationUnit, so we must not also load them here from the (compiled) class file
    private boolean isSourceSymbol(Symbol symbol) {
        if (topLevelClassSymbolsOfSources == null) return false;
        Symbol.ClassSymbol enclosing = symbol.enclClass();
        return enclosing != null && topLevelClassSymbolsOfSources.containsKey(primary(enclosing));
    }

    private List<AnnotationExpression> loadAnnotations(Symbol symbol) {
        if (isSourceSymbol(symbol)) return List.of();
        List<AnnotationExpression> result = new ArrayList<>();
        try {
            for (var mirror : symbol.getAnnotationMirrors()) {
                if (mirror instanceof Attribute.Compound compound) {
                    try {
                        result.add(annotationExpression(compound));
                    } catch (RuntimeException re) {
                        LOGGER.warn("Skipping annotation {} on {}: {}", compound.type, symbol, re.toString());
                    }
                }
            }
        } catch (RuntimeException re) {
            LOGGER.warn("Cannot read annotations on {}: {}", symbol, re.toString());
        }
        return result;
    }

    private AnnotationExpression annotationExpression(Attribute.Compound compound) {
        ParameterizedType type = convert(compound.type);
        List<AnnotationExpression.KV> kvs = new ArrayList<>();
        for (var pair : compound.values) {
            Expression value = annotationValue(pair.snd);
            if (value != null) {
                kvs.add(runtime.newAnnotationExpressionKeyValuePair(pair.fst.getSimpleName().toString(), value));
            }
        }
        return runtime.newAnnotationExpressionBuilder()
                .setTypeInfo(type.typeInfo())
                .setKeyValuesPairs(kvs)
                .build();
    }

    // returns null when the value cannot be represented; the key/value pair is then dropped
    private Expression annotationValue(Attribute attribute) {
        try {
            return switch (attribute) {
                case Attribute.Constant c -> annotationConstant(c);
                case Attribute.Enum en -> runtime.newVariableExpressionBuilder()
                        .setSource(runtime.noSource())
                        .setVariable(runtime.newFieldReference(getOrLoadField(en.value)))
                        .build();
                // a class literal 'X.class'; newClassExpressionBuilder wraps X as the Class<X> overall type
                case Attribute.Class cl -> runtime.newClassExpressionBuilder(convert(cl.classType)).build();
                case Attribute.Array arr -> {
                    List<Expression> values = new ArrayList<>();
                    for (Attribute a : arr.values) {
                        Expression e = annotationValue(a);
                        if (e != null) values.add(e);
                    }
                    ParameterizedType commonType = arr.type instanceof Type.ArrayType at
                            ? convert(at.elemtype) : runtime.objectParameterizedType();
                    yield runtime.newArrayInitializerBuilder().setExpressions(values).setCommonType(commonType).build();
                }
                case Attribute.Compound nested -> annotationExpression(nested);
                default -> null;
            };
        } catch (RuntimeException re) {
            return null;
        }
    }

    private Expression annotationConstant(Attribute.Constant c) {
        Object v = c.value;
        return switch (c.type.getTag()) {
            case BOOLEAN -> runtime.newBoolean(((Integer) v) != 0);
            case CHAR -> runtime.newChar((char) (int) (Integer) v);
            case BYTE -> runtime.newByte((byte) (int) (Integer) v);
            case SHORT -> runtime.newShort((short) (int) (Integer) v);
            case INT -> runtime.newInt((Integer) v);
            case LONG -> runtime.newLong((Long) v);
            case FLOAT -> runtime.newFloat((Float) v);
            case DOUBLE -> runtime.newDouble((Double) v);
            // TypeTag.CLASS on a *constant* is javac's encoding of a String value (java.lang.String); a real
            // class literal 'X.class' arrives as Attribute.Class and is handled in annotationValue()
            case CLASS -> runtime.newStringConstant((String) v);
            default -> null;
        };
    }

    private static final Pattern JAR_FILE = Pattern.compile("(jar:file:.+)/([^/!]+\\.jar)!/.*");

    // javac's authoritative source-vs-binary provenance of a type. NOTE: cs.sourcefile is NOT reliable here, as
    // javac also populates it from the SourceFile debug attribute of a .class file (a synthetic, name-only
    // SourceFileObject). cs.classfile is a real .class for a binary type, a .java (or null) for a source type of
    // the current task.
    static boolean fromClassFile(Symbol.ClassSymbol cs) {
        return cs.classfile != null && cs.classfile.getKind() == JavaFileObject.Kind.CLASS;
    }

    private SourceSet ensureSourceSet(Symbol.ClassSymbol cs, URI uri) {
        if (!fromClassFile(cs)) {
            return sourceSetOfCurrentTask;
        }
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
        if ("file".equals(uri.getScheme())) {
            SourceSet dir = sourceSetDirPrefixes.entrySet().stream()
                    .filter(e -> uri.getPath().startsWith(e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(null);
            if (dir != null) return dir;
        }
        Symbol.ModuleSymbol module = findModule(cs);
        if (module != null && !module.isUnnamed()) {
            SourceSet known = getSourceSet(module.name.toString());
            if (known != null) {
                return known;
            }
            LOGGER.warn("Unknown module {}, add to classpath?", module);
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
            // the check for JLO is actually only for the clone() method.
            // constructors are loaded even when private: a type's constructors are part of its shape and can be
            // referenced by analyzed-package files (e.g. the private no-arg constructor of a static-utility class
            // such as java.lang.Math); skipping them left typeInfo.constructors() empty and broke analysis decode.
            boolean load = (ms.flags() & Flags.PRIVATE) == 0 || ms.isConstructor();
            if (load
                && (alwaysLoad || loadMode == LoadMode.COMPLETE && !methodSymbolMap.containsKey(ms))
                && (loadMode == LoadMode.LOAD_MEMBERS || !methodSymbolMap.containsKey(ms))) {
                MethodInfo methodInfo = addMethodToType(typeInfo, ms, false);
                if (!methodInfo.hasBeenInspected()) methodInfo.builder().computeAccess().commit();
            }

        } else if (member instanceof Symbol.VarSymbol vs && vs.owner == owner) {
            // load fields even when private: like constructors (see above), a type's fields are part of its shape
            // and are referenced in analyzed-package files (@GetSet targets, linked variables). Skipping private
            // ones left typeInfo.fields() short of what the source-side encoder saw (a nested ...Impl.Builder loaded
            // with 0 fields). Still exclude compiler-synthetic fields (this$0, $VALUES, switch-maps): the encoder
            // analysed source, which has none. (Field references decode by NAME now, so the exact field-set size no
            // longer shifts resolution -- see CodecImpl.decodeFieldInfo. JDK ct.sym types carry no private members
            // at all, so their private fields remain unrecoverable -- same caveat as private constructors.)
            boolean isPrivate = (vs.flags() & Flags.PRIVATE) != 0;
            boolean synthetic = (vs.flags() & Flags.SYNTHETIC) != 0;
            boolean load = !isPrivate || !synthetic;
            if (load && (alwaysLoad || loadMode == LoadMode.COMPLETE && !varSymbolMap.containsKey(vs))
                && (loadMode == LoadMode.LOAD_MEMBERS || !varSymbolMap.containsKey(vs))) {
                FieldInfo fieldInfo = addFieldToType(typeInfo, vs);
                if (!fieldInfo.hasBeenInspected()) {
                    fieldInfo.builder().computeAccess().commit();
                    assert fieldInfo.access() != null;
                }
            }
        } else if (member instanceof Symbol.ClassSymbol cs && cs.owner == owner) {
            boolean isNotPrivate = (cs.flags() & Flags.PRIVATE) == 0;
            if (isNotPrivate) {
                TypeInfo enclosed = addEnclosedTypeToType(typeInfo, cs, loadMode);
                if (!enclosed.hasBeenInspected()) {
                    enclosed.builder().computeAccess().commit();
                }
            }
        }
    }

    private TypeInfo addEnclosedTypeToType(TypeInfo typeInfo, Symbol.ClassSymbol cs, LoadMode loadMode) {
        TypeInfo inMap = getType(cs.fullname.toString());
        TypeInfo enclosed;
        if (inMap != null) {
            if (inMap.hasBeenInspected() || loadMode == LoadMode.LAZILY) {
                return inMap;
            }
            enclosed = inMap;
        } else {
            String name = cs.getSimpleName().toString();
            LOGGER.debug("Adding enclosed type {} to {}", name, typeInfo);
            enclosed = runtime.newTypeInfo(typeInfo, name);
            put(enclosed);
            typeInfo.builder().addSubType(enclosed);
        }
        // we must do type parameters, interfaces, parent class etc.
        loadType(cs, enclosed, loadMode == LoadMode.COMPLETE ? LoadMode.COMPLETE_SUB : loadMode);
        return enclosed;
    }

    private FieldInfo addFieldToType(TypeInfo typeInfo, Symbol.VarSymbol vs) {
        FieldInfo inMap = varSymbolMap.get(vs);
        if (inMap != null) return inMap;

        String name = vs.getSimpleName().toString();
        LOGGER.debug("Adding field {} to {}", name, typeInfo);
        ParameterizedType type = convert(vs.type);
        boolean isStatic = (vs.flags() & Flags.STATIC) != 0;
        FieldInfo fieldInfo = runtime.newFieldInfo(name, isStatic, type, typeInfo);
        // we might overwrite them, or not...
        fieldInfo.builder().setInitializer(runtime.newEmptyExpression()).setAccess(runtime.accessPublic());
        typeInfo.builder().addField(fieldInfo);
        flagHelper.field(vs.flags(), fieldInfo.builder());
        fieldInfo.builder().addAnnotations(loadAnnotations(vs));

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
        MethodInfo inMap = methodSymbolMap.get(ms);
        if (inMap != null) return inMap;
        String name = ms.getSimpleName().toString();
        //  assert (ms.flags() & Flags.BRIDGE) == 0 : "Do not want any bridge method " + ms + " in " + typeInfo;
        MethodInfo method;
        boolean isConstructor;
        if ("<init>".equals(name)) {
            LOGGER.debug("Adding constructor {} to {}", name, typeInfo);
            MethodInfo.MethodType methodType = flagHelper.constructorType(ms.flags());
            method = runtime.newConstructor(typeInfo, methodType);
            typeInfo.builder().addConstructor(method);
            isConstructor = true;
        } else {
            LOGGER.debug("Adding method {} to {}", name, typeInfo);
            MethodInfo.MethodType methodType = flagHelper.methodType(ms.flags(),
                    typeInfo.isInterface() || typeInfo.isAnnotation());
            method = runtime.newMethod(typeInfo, name, methodType);
            typeInfo.builder().addMethod(method);
            isConstructor = false;
        }
        // add this early enough to avoid recursion/infinite loop problems with self-referencing type parameters
        put(ms, method);

        int index = 0;
        MethodInfo.Builder builder = method.builder();

        // When this method is declared in source being compiled in the CURRENT task, its declaration will be
        // visited later (ScanCompilationUnit.visitMethod), which sets the parameter sources. We must therefore NOT
        // commit the parameters here, otherwise those sources can never be set (a method reference may cause the
        // method to be created from its symbol before its declaration is reached; see TestParameterInfoSource).
        // Both conditions are needed: (1) !fromClassFile excludes binary types such as java.sql.Connection (whose
        // module may nonetheless fall back to the current source set), which have no declaration and must be
        // committed now, else the method stays incomplete ('?.?'); (2) the source-set check excludes non-class-file
        // types belonging to another source set (e.g. AAPI-loaded), which this task will never visit. Synthetic
        // methods (e.g. enum values()/valueOf()) have no declaration and must be committed now too.
        boolean declaredInCurrentTaskSource = ms.owner instanceof Symbol.ClassSymbol ownerClassSymbol
                && !fromClassFile(ownerClassSymbol)
                && typeInfo.compilationUnit() != null
                && sourceSetOfCurrentTask.equals(typeInfo.compilationUnit().sourceSet());
        boolean deferParameterCommit = !synthetic && declaredInCurrentTaskSource;

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
            // FIXME when source type, do not commit yet, we must set detailed sources
        }

        flagHelper.method(ms.flags(), builder);
        builder.addAnnotations(loadAnnotations(ms));
        if (synthetic) {
            builder.setSynthetic(true);
        }
        // exception types
        ms.getThrownTypes().forEach(t -> builder.addExceptionType(convert(t)));

        if (ms.params != null) {
            // resolve all parameter types up front, so the index can be keyed on the full (erased) signature
            List<ParameterizedType> paramTypes = ms.params.stream().map(p -> convert(p.type))
                    .collect(Collectors.toList());
            List<String> realNames = lookupParameterNames(typeInfo, name, paramTypes);
            // VARARGS is a method-level flag (ACC_VARARGS); it applies to the method's last parameter, which is
            // not itself marked on the parameter symbol when loaded from a class file
            boolean methodIsVarargs = (ms.flags() & Flags.VARARGS) != 0;
            int lastParamIndex = ms.params.size() - 1;
            int pIndex = 0;
            for (Symbol.VarSymbol parameter : ms.params) {
                ParameterizedType pt = paramTypes.get(pIndex);
                String paramName = realNames != null && pIndex < realNames.size()
                        ? realNames.get(pIndex) : parameter.getSimpleName().toString();
                ParameterInfo parameterInfo = builder.addParameter(paramName, pt);
                long flags = parameter.flags();
                if (methodIsVarargs && pIndex == lastParamIndex) parameterInfo.builder().setVarArgs(true);
                if ((flags & Flags.FINAL) != 0) parameterInfo.builder().setIsFinal(true);
                parameterInfo.builder().addAnnotations(loadAnnotations(parameter));
                if (!deferParameterCommit) parameterInfo.builder().commit();
                pIndex++;
            }
        }
        ParameterizedType returnType = isConstructor ? runtime.parameterizedTypeReturnTypeOfConstructor()
                : convert(ms.getReturnType());
        List<MethodInfo> overrides = computeMethodOverrides
                .findOverriddenMethods(ms)
                .stream().map(this::getOrLoadMethod)
                .toList();
        builder.setReturnType(returnType)
                .setSource(runtime.noSource())
                .setMethodBody(runtime.emptyBlock())
                .addOverrides(overrides);
        if (!deferParameterCommit) builder.commitParameters();
        builder.computeAccess();
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
            fieldInfo.builder().addAnnotations(loadAnnotations(vs));
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
        return convert(type, null);
    }

    private ParameterizedType convert(Type type, Set<Type> visited) {
        if (type instanceof Type.JCPrimitiveType primitiveType) {
            return primitiveType(primitiveType.getKind());
        }
        if (type instanceof Type.IntersectionClassType ict) {
            if (ict.supertype_field != null) {
                return convert(ict.supertype_field, visited);
            }
            throw new UnsupportedOperationException("NYI");
        }
        if (type instanceof Type.ClassType ct) {
            return classType(ct, visited);
        }
        if (type instanceof Type.JCVoidType) {
            return runtime.voidParameterizedType();
        }
        if (type instanceof Type.ArrayType at) {
            ParameterizedType base = convert(at.elemtype, visited);
            assert base != null;
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
            ParameterizedType base = convert(wildcardType.type, visited);
            assert base != null;
            // preserve the bound's array dimension and type arguments: e.g. '? extends byte[]' must stay byte[]
            // (an array), not collapse to the primitive byte (which is an illegal type argument), and
            // '? extends List<String>' must keep its String argument.
            if (base.isTypeParameter()) {
                return runtime.newParameterizedType(base.typeParameter(), base.arrays(), wildCard);
            }
            return runtime.newParameterizedType(base.typeInfo(), base.arrays(), wildCard, base.parameters());
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
                    Set<Type> visitedNotNull = visited == null ? new HashSet<>() : visited;
                    if (visitedNotNull.add(typeVar)) {
                        ParameterizedType upperPt = convert(typeVar.getUpperBound(), visitedNotNull);
                        assert upperPt != null;
                        TypeInfo upper = upperPt.typeInfo();
                        // preserve the bound's array dimension and type arguments: e.g. the captured 'CAP extends
                        // byte[]' from byte[].getClass() must stay byte[] (an array), not collapse to the primitive
                        // byte (an illegal type argument)
                        return runtime.newParameterizedType(upper, upperPt.arrays(), runtime.wildcardExtends(),
                                upperPt.parameters());
                    }
                    return runtime.parameterizedTypeWildcard();
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
        if (type instanceof JCTree.JCTypeApply) {
            // attach the type-argument commas to this parameterized type's own source, so nested generics
            // (each a distinct parameterized type) keep their own lists
            List<Source> commas = sourceProvider.typeArgumentCommas(source);
            if (commas != null) {
                DetailedSources.Builder b = runtime.newDetailedSourcesBuilder();
                b.putList(DetailedSources.TYPE_ARGUMENT_COMMAS, commas);
                source = source.withDetailedSources(b.build()); // FIXME check merge?
            }
        }
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
                ParameterizedType pt = classType(ct, null);
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
        if (type instanceof JCTree.JCWildcard wc) {
            if (wc.type.isUnbound()) {
                return runtime.parameterizedTypeWildcard();
            }
            boolean isExtends = wc.type.isExtendsBound();
            Wildcard wildCard = isExtends ? runtime.wildcardExtends() : runtime.wildcardSuper();
            ParameterizedType base = convertTree(wc.getBound(), dsb);
            if (base.isTypeParameter()) {
                return runtime.newParameterizedType(base.typeParameter(), 0, wildCard);
            }
            return runtime.newParameterizedType(base.typeInfo(), 0, wildCard, List.of());
        }
        if (type instanceof JCTree.JCAnnotatedType at) {
            // TODO there is no room for this in maddi's model
            return convertTreeDontSet(at.underlyingType, dsb, source);
        }
        if (type instanceof JCTree.JCTypeIntersection intersection) {
            List<ParameterizedType> bounds = intersection.getBounds().stream()
                    .map(e -> convertTree(e, dsb)).toList();
            return runtime.newIntersectionType(null, bounds);
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

    private ParameterizedType classType(Type.ClassType ct, Set<Type> visited) {
        if (ct.tsym instanceof Symbol.ClassSymbol cs
            && cs.owner instanceof Symbol.MethodSymbol
            && cs.getSimpleName().isEmpty()
            && getType(cs.toString()) == null) {
            // an anonymous class ('new Base(){...}') can surface in a type-argument position through
            // inference (e.g. Optional.map(...) whose inferred R is the anonymous type) *before* its body
            // is scanned and registered in typeData. In that forward-reference case it has no simple name
            // to look up on the element stack and is never generic, so represent it by the type it extends,
            // or, failing that, the interface it implements. Once registered, classTypeInfo resolves it
            // normally via the 'known' path, so only intercept while it is still unknown.
            return anonymousClassType(cs, visited);
        }
        TypeInfo typeInfo = classTypeInfo(ct);
        if (ct.getTypeArguments().isEmpty()) {
            return typeInfo.asSimpleParameterizedType();
        }
        List<ParameterizedType> typeParameters = ct.getTypeArguments().stream()
                .map(ta -> convert(ta, visited))
                .toList();
        return runtime.newParameterizedType(typeInfo, typeParameters);
    }

    private ParameterizedType anonymousClassType(Symbol.ClassSymbol cs, Set<Type> visited) {
        Type superclass = cs.getSuperclass();
        boolean superIsObject = superclass == null || superclass.tsym == null
                                || "java.lang.Object".contentEquals(superclass.tsym.getQualifiedName());
        if (superIsObject) {
            for (Type itf : cs.getInterfaces()) {
                return convert(itf, visited);
            }
            return runtime.objectParameterizedType();
        }
        return convert(superclass, visited);
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
        if (methodSymbol.owner instanceof Symbol.ClassSymbol cs) {
            TypeInfo typeInfo = convert(cs.type).typeInfo();
            if (methodSymbol.isConstructor()) {
                return typeInfo.constructors().stream().filter(c -> sameTypes(c.parameters(), methodSymbol.params))
                        .findFirst().orElse(null);
            }
            String methodName = methodSymbol.name.toString();
            int numParams = methodSymbol.params.size();
            if (typeInfo.hasMethodMap()) {
                return typeInfo.findUniqueMethod(methodName, numParams, () ->
                        methodSymbol.params.stream().map(vs -> convert(types.erasure(vs.type)).fullyQualifiedName())
                                .collect(Collectors.joining(","))
                );
            }
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
        return infoByFqn.typesLoadedForThisSourceSet();
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
