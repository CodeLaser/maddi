package org.e2immu.analyzer.modification.common.util;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypePrinter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.cst.print.formatter2.Formatter2Impl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Lifts one <b>type</b> out of its project into a small standalone source tree that compiles against the JDK
 * alone: the type itself with its members' bodies verbatim, and everything it references reduced to stubs.
 *
 * <h2>What it emits, and why it is a project rather than one file</h2>
 * {@link IsolateMethod} has a single compilation unit to work with, so every stub has to be nested inside the
 * frame — either at its top level, where its simple name is in scope, or in a chain of namespace stubs, where
 * only its fully-qualified name is. Which of the two is right is dictated by how the verbatim text happens to
 * spell the type, one declaration can only satisfy one of them, and getting it wrong drops the whole unit.
 * That single constraint is the source of most of {@code IsolateMethod}'s defects.
 * <p>
 * A class isolate does not have to live with it. It emits <b>one compilation unit per type, in the package the
 * original came from</b>, so {@code p.q.Value} and a bare {@code Value} resolve exactly the way they do in real
 * Java — through the package and an import. There are no namespace stubs, no simple-name arbitration and no
 * placement evidence to gather, and two types with the same simple name in different packages are no longer a
 * conflict at all. This matters more here than it would there: a class isolate reconstructs far more signatures
 * than a method isolate, so it would have hit those cases far more often.
 *
 * <h2>The JDK-only rule</h2>
 * Unchanged from {@code IsolateMethod}, and it is the point of the exercise: a type that ships with the JDK
 * (resolved from a {@code jmod}) is kept as itself and imported; anything that would need a jar on the class
 * path is stubbed. The result compiles and parses with no external dependency.
 *
 * <h2>Bodies</h2>
 * The isolated type's own methods keep their bodies, pasted verbatim from the original source — the same
 * marker-and-substitute trick {@code IsolateMethod} uses, one marker per kept method. Stubs get bodies that
 * merely satisfy the compiler.
 *
 * <h2>Verifying a change here</h2>
 * Two gates, and they answer different questions. A re-parse with the default {@code ParseOptions} is <b>not</b> a
 * compile: {@code failFast} is false there, so javac's errors are logged and the unit is kept. Use
 * {@code setFailFast(true)}, as {@code TestIsolateClass4Compiles} does — that is the only reason its first three
 * defects could survive a green test suite for as long as {@code IsolateClass} has existed.
 * <p>
 * And a driver is not the corpus. Every fix behind {@code TestIsolateClass4Compiles} was written against a
 * reduction that reproduced the failure and then went green; one of them took the hundred-class corpus from 97
 * trees compiling to <b>9</b>, with the whole unit suite still passing. Run
 * {@code TestIsolateClosedCoreClasses} for anything that changes what a stub declares.
 * {@code docs/isolate-class.md} §6 has the twelve causes and §7 the three that are left.
 * <p>
 * One gap is left deliberately: the <b>isolated type itself</b> is always emitted as a {@code class}, so isolating
 * an {@code enum} loses its nature and its constants. The stubs reproduce every nature; the isolated type does not.
 */
public class IsolateClass {
    private static final Logger LOGGER = LoggerFactory.getLogger(IsolateClass.class);

    /** the placeholder body handed to each kept method, replaced by its verbatim source when printing */
    public static final String MEMBER_MARKER_PREFIX = "_member_to_be_replaced_";

    private final JavaInspector javaInspector;
    private final Runtime runtime;

    public IsolateClass(JavaInspector javaInspector) {
        this.javaInspector = javaInspector;
        this.runtime = javaInspector.runtime();
    }

    /**
     * @param isolated     the compilation unit of the isolated type itself
     * @param stubs        one compilation unit per stubbed dependency, in the package its original came from
     * @param markers      marker method -> the original method whose source replaces it
     * @param toImport     stub and JDK types the isolated unit may import
     * @param toQualify    types that lost a simple-name clash and must be printed fully qualified
     * @param staticImports fully-qualified {@code owner.member} names the isolated unit must import statically
     */
    public record Result(CompilationUnit isolated,
                         List<CompilationUnit> stubs,
                         Map<MethodInfo, MethodInfo> markers,
                         Set<TypeInfo> toImport,
                         Set<TypeInfo> toQualify,
                         Set<String> staticImports) {

        /** every compilation unit of the emitted project, the isolated type first */
        public List<CompilationUnit> all() {
            List<CompilationUnit> result = new ArrayList<>();
            result.add(isolated);
            result.addAll(stubs);
            return result;
        }
    }

    /**
     * Placement for a project: a member type nests inside its enclosing type's stub, exactly as in the original;
     * a primary type gets its own compilation unit in its own package. Both are what the original did, so every
     * reference in the verbatim text — simple, {@code Outer.Inner} or fully qualified — resolves the same way it
     * did there.
     */
    private class ClassStubs extends IsolationCore {
        private final TypeInfo isolatedStub;
        // one compilation unit per primary stub type, in declaration order. Not one per package: a stub is
        // public (it is referenced across package boundaries), and a public type has to live in a file named
        // after it
        final Map<TypeInfo, CompilationUnit> compilationUnitPerStub = new LinkedHashMap<>();

        ClassStubs(TypeInfo originalType, TypeInfo isolatedStub) {
            super(IsolateClass.this.javaInspector, originalType);
            this.isolatedStub = isolatedStub;
        }

        // originals the verbatim text names by their simple name; that spelling is fixed, so on a simple-name
        // clash between two stubs (one.Entry vs two.Entry) this is the one that must own the import
        final Set<TypeInfo> namedSimplyInSource = new HashSet<>();

        @Override
        boolean stubsCrossPackageBoundaries() {
            return true;
        }

        // 'owner.member' pairs the verbatim text names WITHOUT a scope, and that some other type declares: they
        // need an 'import static'. The import computer has no notion of static imports (its own source says
        // "IMPROVE static fields and methods"), so they are emitted alongside its output.
        final Set<String> staticImports = new java.util.TreeSet<>();

        @Override
        boolean recordStaticImport(TypeInfo owner, String memberName) {
            if (isJdkType(owner) && "java.lang".equals(owner.packageName())) return false;
            // An INHERITED static member needs no import: 'RSCServiceV1Stub extends Stub' calls 'getFactory(...)'
            // unqualified because inheritance already puts it in scope. Importing it is not merely redundant, it
            // does not compile -- a single-static-import must name an ACCESSIBLE member, and these are typically
            // 'protected static', which does not cross the package boundary the stub now sits behind:
            // "cannot find symbol: static getFactory". The largest single cause of the 34 class isolates that did
            // not compile, and the only one that was a wrong decision rather than a missing one.
            // Returning false sends the caller down its ordinary path, which puts the member on the stub of the
            // type that declares it -- where the isolated type inherits it from, exactly as in the original.
            if (isolatedTypeInherits(owner)) return false;
            staticImports.add(owner.fullyQualifiedName() + "." + memberName);
            return true;
        }

        @Override
        void recordPlacementEvidence(TypeInfo typeInfo, DetailedSources ds) {
            if (ds != null && ds.detail(typeInfo.packageName()) == null) namedSimplyInSource.add(typeInfo);
        }

        @Override
        TypeInfo selfType() {
            return isolatedStub;
        }

        // the isolated type's own members, which are printed verbatim and so need no stub; identity, because two
        // overloads of one name are different members and only the exact one is already declared
        final Set<MethodInfo> keptVerbatim =
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

        @Override
        boolean alreadyDeclaredWithoutStub(TypeInfo owner, MethodInfo methodInfo) {
            return owner == isolatedStub && keptVerbatim.contains(methodInfo);
        }

        @Override
        TypeInfo originalTypeStub() {
            // the isolated type keeps its own name, so a self-reference written 'C.DAYS' resolves to it directly;
            // IsolateMethod needs a separate stub for this only because it renames the frame to 'C_method'
            return isolatedStub;
        }

        @Override
        TypeInfo placeStub(TypeInfo typeInfo, DetailedSources ds) {
            var enclosing = typeInfo.compilationUnitOrEnclosingType();
            if (enclosing.isRight()) {
                // a member type: nest it in its enclosing type's stub, as in the original. 'Outer.Inner' and a
                // simply-named 'Inner' (via an import) then both resolve, which in one flat unit they cannot
                TypeInfo enclosingStub = enclosing.getRight() == originalType
                        ? isolatedStub : ensureType(enclosing.getRight(), ds);
                return runtime.newTypeInfo(enclosingStub, typeInfo.simpleName());
            }
            String packageName = typeInfo.packageName();
            CompilationUnit cu = runtime.newCompilationUnitBuilder()
                    .setPackageName(packageName)
                    .setSourceSet(typeInfo.compilationUnit().sourceSet())
                    .setURI(typeInfo.compilationUnit().uri())
                    .build();
            TypeInfo stub = runtime.newTypeInfo(cu, typeInfo.simpleName());
            compilationUnitPerStub.put(stub, cu);
            return stub;
        }
    }

    /**
     * @param typeInfo the type to isolate; its methods and constructors keep their bodies
     */
    public Result isolate(TypeInfo typeInfo) {
        CompilationUnit isolatedCu = runtime.newCompilationUnitBuilder()
                .setPackageName(typeInfo.packageName())
                .setSourceSet(typeInfo.compilationUnit().sourceSet())
                .setURI(typeInfo.compilationUnit().uri())
                .build();
        TypeInfo isolated = runtime.newTypeInfo(isolatedCu, typeInfo.simpleName());
        isolated.builder().setSource(runtime.noSource())
                .setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType())
                .addTypeModifier(runtime.typeModifierPublic())
                .computeAccess();

        ClassStubs data = new ClassStubs(typeInfo, isolated);
        Map<MethodInfo, MethodInfo> markers = new LinkedHashMap<>();

        // the isolated type keeps its place in the hierarchy: an 'implements Outer.Sink<Entry>' in the verbatim
        // text has to still be there, or the @Override on its implementation does not resolve
        isolated.builder().setParentClass(data.reproducedParentClass(typeInfo));
        for (ParameterizedType itf : typeInfo.interfacesImplemented()) {
            isolated.builder().addInterfaceImplemented(data.ensureTypes(itf,
                    IsolationCore.detailedSources(typeInfo.source())));
        }

        // the isolated type's own fields keep their declared types (stubbed), but not their initializers: an
        // initializer can reach arbitrarily far into the project, and nothing in the isolate needs it to run.
        // The exception is a CONSTANT VARIABLE (JLS 4.12.4): 'static final int FIRST = 1' is the type's own switch
        // 'case FIRST:' label, and without the value -- and the 'final' that goes with it -- javac says "constant
        // expression required". A literal reaches nowhere, so keeping it costs nothing. 5 class isolates, 45 of
        // their errors. 'final' and the initializer are kept or dropped TOGETHER: a final field with no
        // initializer is "variable X not initialized in the default constructor", which is why the initializer
        // cannot simply be dropped from a field that keeps its modifiers
        Set<FieldInfo> ownFields = new HashSet<>();
        for (FieldInfo fieldInfo : typeInfo.fields()) {
            ParameterizedType newType = data.ensureTypes(fieldInfo.type(),
                    IsolationCore.detailedSources(fieldInfo.source()));
            FieldInfo newField = runtime.newFieldInfo(fieldInfo.name(), fieldInfo.isStatic(), newType, isolated);
            boolean constantVariable = fieldInfo.isStatic() && fieldInfo.isFinal()
                                       && fieldInfo.initializer() != null && fieldInfo.initializer().isConstant();
            newField.builder()
                    .setInitializer(constantVariable ? fieldInfo.initializer() : runtime.newEmptyExpression())
                    .setAccess(fieldInfo.access())
                    .setSource(runtime.noSource());
            if (fieldInfo.isStatic()) newField.builder().addFieldModifier(runtime.fieldModifierStatic());
            if (constantVariable) newField.builder().addFieldModifier(runtime.fieldModifierFinal());
            newField.builder().commit();
            isolated.builder().addField(newField);
            ownFields.add(newField);
            // seed the map: a body that reads 'this.value' would otherwise have ensureField declare it a second
            // time, package-private and without the modifiers we just reproduced
            data.fieldMap.put(fieldInfo, newField);
        }

        // one marker method per kept member, standing in for its verbatim source; visitMethod is what pulls
        // everything that member references into the stub graph.
        // The whole list is registered BEFORE the walk: a body reaching a member declared further down would
        // otherwise be stubbed, and the stub and the verbatim declaration would collide.
        List<MethodInfo> kept = allMembers(typeInfo);
        data.keptVerbatim.addAll(kept);
        int ordinal = 0;
        for (MethodInfo methodInfo : kept) {
            data.visitMethod(methodInfo);
            // a kept method that overrides something needs that something to exist: an '@Override' in the
            // verbatim text does not resolve against an empty interface stub, and the unit is dropped. This is
            // the class-isolate counterpart of IsolateMethod's createOverrideSupertype -- but here the supertype
            // is real and stubbed, so the declaration only has to be added to it
            for (MethodInfo overridden : methodInfo.overrides()) {
                TypeInfo declaring = overridden.typeInfo();
                if (declaring == typeInfo) continue;
                TypeInfo declaringStub = data.ensureType(declaring, null);
                if (declaringStub != null && declaringStub != declaring) {
                    data.ensureMethodInfo(declaringStub, overridden);
                }
            }
            MethodInfo marker = runtime.newMethod(isolated, MEMBER_MARKER_PREFIX + ordinal++,
                    runtime.methodTypeMethod());
            marker.builder().setSource(runtime.noSource())
                    .setMethodBody(runtime.emptyBlock())
                    .setReturnType(runtime.voidParameterizedType())
                    .setAccess(runtime.accessPackage())
                    .computeAccess().commit();
            isolated.builder().addMethod(marker);
            markers.put(marker, methodInfo);
        }
        data.addDummyInterfaceMethods();
        data.addDefaultConstructorsWhereExtended();

        data.fieldMap.values().stream().sorted(Comparator.comparing(FieldInfo::name))
                // exclude exactly the ones added above, NOT everything the isolated type owns: a field inherited
                // from a stubbed supertype and read unqualified ('_service') is placed here by ensureField, and
                // filtering on the owner threw it away -- four Axis2 isolates died on "Type _service not found"
                .filter(field -> !ownFields.contains(field))
                .forEach(field -> field.owner().builder().addField(field));

        // deepest first, so a nested stub is committed and attached before its (still open) enclosing stub
        List<TypeInfo> allStubs = new ArrayList<>(data.typeMap.values());
        allStubs.stream()
                .sorted(Comparator.comparingInt(IsolationCore::enclosingDepth).reversed()
                        .thenComparing(TypeInfo::simpleName))
                .forEach(stub -> {
                    stub.builder().commit();
                    var enclosing = stub.compilationUnitOrEnclosingType();
                    if (enclosing.isRight()) enclosing.getRight().builder().addSubType(stub);
                });
        isolated.builder().commit();
        isolatedCu.setTypes(List.of(isolated));

        List<CompilationUnit> stubUnits = new ArrayList<>();
        data.compilationUnitPerStub.forEach((stub, cu) -> {
            cu.setTypes(List.of(stub));
            stubUnits.add(cu);
        });
        LOGGER.info("Isolated {} into {} stub compilation unit(s)", typeInfo, stubUnits.size());

        Set<TypeInfo> importable = arbitrateJdkImports(data);
        Set<TypeInfo> notImportable = new HashSet<>(data.jdkTypesToImport);
        notImportable.removeAll(importable);
        // The pasted bodies are raw text, so the import computer cannot see that they name 'Helper' or 'Value'.
        // Every stub the isolate reaches has to be offered to it explicitly, or the isolated unit compiles
        // against types it never imports. IsolateMethod never needed this: its stubs are nested in the frame.
        Map<Boolean, Set<TypeInfo>> imports = arbitrateImports(data);
        return new Result(isolatedCu, stubUnits, markers,
                imports.get(Boolean.TRUE), imports.get(Boolean.FALSE), Set.copyOf(data.staticImports));
    }

    /**
     * Which types the isolated unit may import, and which have to be printed fully qualified.
     * <p>
     * Candidates are the stubs (one per emitted compilation unit) and the JDK types the isolate keeps. They are
     * arbitrated <b>together</b>, because a stub and a JDK type collide just as readily as two stubs:
     * closed-core's own {@code com.example.core.general.util.ArrayList} against {@code java.util.ArrayList} is
     * the case that dropped four class isolates. Only one type can hold a simple name, the verbatim text cannot
     * be respelled, so the one it names simply keeps the import and the rest are vetoed into their qualified
     * form — the same rule as {@code IsolateMethod.arbitrateJdkImports}, over one merged candidate set.
     *
     * @return TRUE -> may be imported, FALSE -> must be printed fully qualified
     */
    private static Map<Boolean, Set<TypeInfo>> arbitrateImports(ClassStubs data) {
        Set<String> singleImportedByOriginal = data.originalType.compilationUnit() == null ? Set.of()
                : data.originalType.compilationUnit().importStatements().stream()
                .filter(is -> !is.isStar())
                .map(is -> is.importString())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        // candidate -> was it named by its simple name in the verbatim text?
        Map<TypeInfo, Boolean> candidates = new LinkedHashMap<>();
        data.typeMap.forEach((original, stub) -> {
            // NOT just the primary stubs: a member type is named by its simple name in the verbatim text just as
            // readily ('new RowFilter<...>()' for DataUtil.RowFilter), and needs its own
            // 'import a.b.Outer.Inner'. Restricting the candidates to primary types left every such reference
            // unresolved -- all five remaining causes of the second class-isolate corpus run were this one bug.
            candidates.put(stub, data.namedSimplyInSource.contains(original));
        });
        data.jdkTypesToImport.forEach(t -> candidates.put(t, data.jdkNamedSimplyInSource.contains(t)));

        Map<String, List<TypeInfo>> bySimpleName = new LinkedHashMap<>();
        candidates.keySet().forEach(t ->
                bySimpleName.computeIfAbsent(t.simpleName(), _ -> new ArrayList<>()).add(t));

        Set<TypeInfo> importable = new HashSet<>();
        Set<TypeInfo> qualify = new HashSet<>();
        for (Map.Entry<String, List<TypeInfo>> entry : bySimpleName.entrySet()) {
            List<TypeInfo> claimants = entry.getValue();
            if (claimants.size() == 1) {
                importable.add(claimants.getFirst());
                continue;
            }
            // sorted first, so the outcome does not depend on the iteration order of a hash set
            List<TypeInfo> sorted = claimants.stream()
                    .sorted(Comparator.comparing(TypeInfo::fullyQualifiedName)).toList();
            // The original file compiled, so what IT single-imported is what that simple name meant there. This
            // decides the case where several claimants are all named simply -- 'Assert.assertEquals(...)' with
            // both org.junit.Assert and org.assertj.core.api.Assert stubbed, where falling back to alphabetical
            // order picked assertj's Assert, which has no assertEquals.
            TypeInfo winner = sorted.stream().filter(t -> singleImportedByOriginal.contains(t.fullyQualifiedName()))
                    .findFirst()
                    .orElseGet(() -> sorted.stream().filter(t -> Boolean.TRUE.equals(candidates.get(t)))
                            .findFirst().orElse(sorted.getFirst()));
            LOGGER.info("Simple name '{}' claimed by {}; importing {}, qualifying the rest",
                    entry.getKey(), claimants.size(), winner.fullyQualifiedName());
            for (TypeInfo t : sorted) (t == winner ? importable : qualify).add(t);
        }
        return Map.of(Boolean.TRUE, importable, Boolean.FALSE, qualify);
    }

    /**
     * Insert the static imports after the package declaration. Textual, because the import computer produces the
     * import block and has no concept of a static import; inserting them here keeps that one gap contained.
     */
    private static String withStaticImports(String printed, Set<String> staticImports) {
        if (staticImports.isEmpty()) return printed;
        int newline = printed.indexOf('\n');
        if (newline < 0 || !printed.startsWith("package ")) return printed;
        StringBuilder sb = new StringBuilder(printed.substring(0, newline + 1));
        staticImports.stream().sorted().forEach(si -> sb.append("import static ").append(si).append(";\n"));
        return sb.append(printed, newline + 1, printed.length()).toString();
    }

    /** constructors first, then methods, in declaration order */
    private static List<MethodInfo> allMembers(TypeInfo typeInfo) {
        List<MethodInfo> result = new ArrayList<>(typeInfo.constructors());
        result.addAll(typeInfo.methods());
        return result.stream().filter(m -> !m.isSynthetic()).toList();
    }

    /**
     * As {@code IsolateMethod.arbitrateJdkImports}: two JDK types can share a simple name, only one can hold it,
     * and the verbatim text decides which. Unlike there, this is per compilation unit rather than for the whole
     * isolate — but the emitted stubs are one per package, so the clash can only bite inside the isolated type
     * itself, where the pasted bodies are.
     */
    private static Set<TypeInfo> arbitrateJdkImports(IsolationCore data) {
        Map<String, List<TypeInfo>> bySimpleName = new HashMap<>();
        for (TypeInfo t : data.jdkTypesToImport) {
            bySimpleName.computeIfAbsent(t.simpleName(), _ -> new ArrayList<>()).add(t);
        }
        Set<TypeInfo> importable = new HashSet<>();
        for (Map.Entry<String, List<TypeInfo>> entry : bySimpleName.entrySet()) {
            List<TypeInfo> candidates = entry.getValue();
            if (candidates.size() == 1) {
                importable.add(candidates.getFirst());
                continue;
            }
            List<TypeInfo> sorted = candidates.stream()
                    .sorted(Comparator.comparing(TypeInfo::fullyQualifiedName)).toList();
            TypeInfo winner = sorted.stream().filter(data.jdkNamedSimplyInSource::contains)
                    .findFirst().orElse(sorted.getFirst());
            importable.add(winner);
            LOGGER.info("Simple name '{}' claimed by {} JDK types; importing {}, qualifying the rest",
                    entry.getKey(), candidates.size(), winner);
        }
        return importable;
    }

    /**
     * The emitted project, keyed by the path each source belongs at ({@code p/q/Value.java}), ready to be written
     * under a source root.
     *
     * @param memberSources the verbatim source of each kept member, by original method; a member with no entry
     *                      keeps an empty body
     */
    public Map<String, String> print(Result result, Map<MethodInfo, String> memberSources) {
        Map<String, String> sources = new LinkedHashMap<>();
        for (CompilationUnit cu : result.all()) {
            TypeInfo primary = cu.types().getFirst();
            String path = (cu.packageName().isEmpty() ? "" : cu.packageName().replace('.', '/') + "/")
                          + primary.simpleName() + ".java";
            sources.put(path, printUnit(cu, result, memberSources));
        }
        return sources;
    }

    /**
     * Single-type imports only, never {@code p.q.*}.
     * <p>
     * An on-demand import creates simple-name clashes that nothing can then arbitrate. closed-core declares its
     * own {@code com.example.core.general.util.ArrayList}; collapse four types of that package and four of
     * {@code java.util}, and the isolated unit carries {@code import java.util.*} beside
     * {@code import com.example.core.general.util.*} — every bare {@code ArrayList} in the verbatim body is
     * then ambiguous, and the unit is dropped. Vetoing one of the two does not help, because the collapse offers
     * the name implicitly whether or not that type is in the import list. With single-type imports the winner of
     * {@link #arbitrateImports} is the only claimant, which is exactly what the original file expressed.
     */
    private static final int NEVER_COLLAPSE_TO_STAR = Integer.MAX_VALUE;

    private String printUnit(CompilationUnit cu, Result result, Map<MethodInfo, String> memberSources) {
        ImportComputer importComputer =
                javaInspector.importComputer(NEVER_COLLAPSE_TO_STAR, javaInspector.mainSources());
        Set<String> declaredSimpleNames = new HashSet<>();
        cu.types().forEach(t -> t.recursiveSubTypeStream().forEach(st -> declaredSimpleNames.add(st.simpleName())));
        // A referenced type whose simple name is DECLARED in this unit by something else must print qualified:
        // log4j's 'AbstractOutputStreamAppender.Builder extends AbstractAppender.Builder' emitted
        // 'extends Builder<B>', which resolves to the nested Builder itself -- a class extending itself. Leaving
        // such a type out of the import list is not enough, since it then prints as the bare shadowed name; the
        // import computer has to be told to qualify it.
        Stream.concat(result.toImport.stream(), result.toQualify.stream())
                .filter(t -> declaredSimpleNames.contains(t.simpleName()))
                .filter(t -> t.primaryType().compilationUnit() != cu)
                .forEach(importComputer::doNotImport);
        // A nested type of ANOTHER unit in the same package: the computer skips same-package types unless the
        // caller asks, and its simple name is not in scope merely by sharing a package. This holds for stub
        // units too, which otherwise add nothing explicitly -- restricting the request to the isolated unit
        // cost 20 of the 94 clean trees.
        result.toImport.stream()
                .filter(t -> !declaredSimpleNames.contains(t.simpleName()))
                .filter(t -> t.primaryType().compilationUnit() != cu)
                .filter(t -> !t.isPrimaryType() && cu.packageName().equals(t.packageName()))
                .forEach(importComputer::add);
        if (cu == result.isolated) {
            // only the isolated unit holds verbatim text, which the import computer cannot read; a stub unit's
            // references are real CST, so it works those out for itself and an explicit list would only add
            // unused imports
            result.toImport.stream()
                    .filter(t -> !declaredSimpleNames.contains(t.simpleName()))
                    // a stub declared in this very unit needs no import; a member type of another unit does,
                    // even when its package matches, because the simple name does not reach across types
                    .filter(t -> t.primaryType().compilationUnit() != cu)
                    .forEach(importComputer::add);
        }
        result.toQualify.forEach(importComputer::doNotImport);
        if (cu != result.isolated) {
            // a stub unit has no verbatim text in it, so the ordinary printer is enough
            return javaInspector.print2(cu, runtime.qualificationSimpleNames(), importComputer);
        }
        // the isolated unit: every marker method is replaced by the verbatim source of the member it stands for
        TypePrinter.MethodPrinterFactory methodPrinterFactory = (owner, mi, formatter2) -> {
            MethodInfo original = result.markers.get(mi);
            if (original != null) {
                String source = memberSources.get(original);
                return _ -> runtime.newOutputBuilder().add(runtime.newText(source == null ? "" : source));
            }
            return runtime.newMethodPrinter(owner, mi, formatter2);
        };
        OutputBuilder ob = runtime.newCompilationUnitPrinter(cu, true)
                .print(importComputer, runtime.qualificationSimpleNames(),
                        runtime::newTypePrinter, methodPrinterFactory,
                        runtime::newFieldPrinter, runtime::newTypePrinter);
        String printed = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build()).write(ob);
        return withStaticImports(printed, result.staticImports);
    }
}
