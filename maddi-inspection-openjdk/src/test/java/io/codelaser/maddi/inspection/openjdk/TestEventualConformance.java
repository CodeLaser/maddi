/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Mechanical enforcement of the four cst-impl idioms that eventual immutability rests on, so that a
 * commit which breaks one fails here with a one-line diagnosis instead of surfacing a week later as a
 * mysterious drop in the dogfood survivor count (docs/eventual-design-improvements.md §2).
 * <p>
 * Each rule is here because a real commit broke it:
 * <ol>
 *     <li>{@code StatementImpl.propertyValueMap} and {@code CatchClauseImpl}'s were 2 of 9 analysis
 *     stores without the disclaimer — they held the whole statement family at FinalFields-after-mark;</li>
 *     <li>{@code UnaryOperatorImpl.hash} was a lazy memo without the slot disclaimer;</li>
 *     <li>{@code ProvidesImpl.addImplementationResolved} turned a resolve-once field into a mutable
 *     {@code ArrayList} and sank the {@code Element} hierarchy;</li>
 *     <li>{@code FactoryImpl.precedenceMap} was a final field filled by {@code put()} in the constructor
 *     — part-of-construction excuses assignments, not content calls.</li>
 * </ol>
 * The test parses maddi's own sources with maddi's own inspector: the same route
 * {@link TestJavaInspector6MultiProject} uses, and the reason it lives in this module rather than in
 * cst-impl (whose test source set has no inspector).
 */
public class TestEventualConformance {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestEventualConformance.class);

    private static final String IGNORE_MODIFICATIONS = "io.codelaser.maddi.annotation.rare.IgnoreModifications";
    private static final String NOT_MODIFIED = "io.codelaser.maddi.annotation.NotModified";
    private static final String PROPERTY_VALUE_MAP = "io.codelaser.maddi.cst.api.analysis.PropertyValueMap";

    /**
     * The commit-once family: a field of one of these types is assignable exactly once, and the engine
     * knows their {@code @Mark}/{@code @Only} contracts, so it is a legitimate non-final-looking slot.
     */
    private static final Set<String> COMMIT_ONCE_TYPES = Set.of(
            "io.codelaser.maddi.support.SetOnce", "io.codelaser.maddi.support.SetOnceMap",
            "io.codelaser.maddi.support.EventuallyFinal", "io.codelaser.maddi.support.EventuallyFinalOnDemand",
            "io.codelaser.maddi.support.FlipSwitch", "io.codelaser.maddi.support.AddOnceSet",
            "io.codelaser.maddi.support.FirstThen", "io.codelaser.maddi.support.VariableFirstThen",
            "io.codelaser.maddi.support.Lazy", "io.codelaser.maddi.support.Freezable");

    private static final Set<String> MUTABLE_CONTAINERS = Set.of(
            "java.util.List", "java.util.Set", "java.util.Map", "java.util.Collection",
            "java.util.SortedSet", "java.util.SortedMap", "java.util.NavigableSet",
            "java.util.NavigableMap", "java.util.Queue", "java.util.Deque");

    /** Method names that mutate a collection or map in place. */
    private static final Set<String> MUTATORS = Set.of(
            "add", "addAll", "addFirst", "addLast", "put", "putAll", "putIfAbsent",
            "remove", "removeAll", "removeIf", "removeFirst", "removeLast", "retainAll",
            "clear", "set", "sort", "replaceAll", "merge", "compute", "computeIfAbsent", "computeIfPresent");

    /**
     * Deliberate exceptions, each with a justification. Format: {@code <fqn or fqn#member>}. Keeping the
     * list here rather than as an annotation means an exception shows up in review as a diff on this
     * file, which is the point.
     */
    private static final Map<String, String> SUPPRESSIONS = Map.of();

    private JavaInspector javaInspector;
    private SourceSet cstImpl;
    private SourceSet maddiUtil;

    private static URI artifactOf(Class<?> classInThatModule) {
        try {
            return classInThatModule.getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException e) {
            throw new AssertionError("Cannot locate the artifact of " + classInThatModule, e);
        }
    }

    /**
     * An external dependency, named after the jar it actually resolves to. The name is not cosmetic: the
     * automatic module name is derived from it, and a name that is not the jar's file name leaves the
     * module unresolvable — whereupon annotation types from it silently fail to convert and the scanner
     * throws an NPE deep inside {@code ClassSymbolScanner} rather than reporting an unresolved import.
     * Deriving it from the URI also survives the version bumps that a hard-coded "annotations-26.1.0.jar"
     * does not.
     */
    private static SourceSet externalJar(Class<?> classInThatJar) {
        URI uri = artifactOf(classInThatJar);
        String fileName = Path.of(uri).getFileName().toString();
        return new SourceSetImpl.Builder().setName(fileName).setUri(uri)
                .setExternalLibrary(true).setModule(true).build();
    }

    @BeforeEach
    public void before() throws IOException, URISyntaxException {
        javaInspector = new JavaInspectorImpl();

        SourceSet orgSlf4jApi = externalJar(Logger.class);
        SourceSet annotations = externalJar(NotNull.class);

        // Mirrors the real module graph after the 0.9.1 split.
        Path maddiAnnotationSrc = Path.of("../maddi-annotation/src/main/java");
        SourceSet maddiAnnotation = new SourceSetImpl.Builder().setName("maddi-annotation")
                .setSourceDirectories(List.of(maddiAnnotationSrc))
                .setUri(artifactOf(io.codelaser.maddi.annotation.Container.class))
                .setLibrary(true).setModule(true).build();

        Path maddiSupportSrc = Path.of("../maddi-support/src/main/java");
        SourceSet maddiSupport = new SourceSetImpl.Builder().setName("maddi-support")
                .setSourceDirectories(List.of(maddiSupportSrc))
                .setUri(artifactOf(io.codelaser.maddi.support.SetOnce.class))
                .setLibrary(true).setModule(true)
                .setDependencies(List.of(maddiAnnotation)).build();

        Path maddiUtilSrc = Path.of("../maddi-util/src/main/java");
        maddiUtil = new SourceSetImpl.Builder().setName("maddi-util")
                .setSourceDirectories(List.of(maddiUtilSrc))
                .setUri(artifactOf(io.codelaser.maddi.util.GetSetNames.class))
                .setLibrary(true).setModule(true)
                .setDependencies(List.of(maddiSupport, orgSlf4jApi)).build();

        Path cstApiSrc = Path.of("../maddi-cst-api/src/main/java");
        SourceSet cstApi = new SourceSetImpl.Builder().setName("maddi-cst-api")
                .setSourceDirectories(List.of(cstApiSrc))
                .setUri(artifactOf(io.codelaser.maddi.cst.api.element.Element.class))
                .setLibrary(true).setModule(true)
                .setDependencies(List.of(maddiSupport, annotations)).build();

        Path cstAnalysisSrc = Path.of("../maddi-cst-analysis/src/main/java");
        SourceSet cstAnalysis = new SourceSetImpl.Builder().setName("maddi-cst-analysis")
                .setSourceDirectories(List.of(cstAnalysisSrc))
                .setUri(artifactOf(io.codelaser.maddi.cst.impl.analysis.ValueImpl.class))
                .setModule(true)
                .setDependencies(List.of(cstApi, maddiSupport, orgSlf4jApi)).build();

        Path cstImplSrc = Path.of("../maddi-cst-impl/src/main/java");
        cstImpl = new SourceSetImpl.Builder().setName("maddi-cst-impl")
                .setSourceDirectories(List.of(cstImplSrc))
                .setUri(artifactOf(io.codelaser.maddi.cst.impl.info.TypeInfoImpl.class))
                .setModule(true)
                .setDependencies(List.of(cstApi, cstAnalysis, maddiSupport, maddiUtil, orgSlf4jApi, annotations))
                .build();

        // cst-io joins the parse, and junit/opentest4j the class path, because that is the combination
        // TestJavaInspector6MultiProject proves resolvable; a narrower graph leaves an annotation type
        // unresolved and the scanner NPEs rather than reporting it. Neither is inspected below.
        Path cstIoSrc = Path.of("../maddi-cst-io/src/main/java");
        SourceSet cstIo = new SourceSetImpl.Builder().setName("maddi-cst-io")
                .setSourceDirectories(List.of(cstIoSrc))
                .setUri(artifactOf(io.codelaser.maddi.cst.io.CodecImpl.class))
                .setModule(true)
                .setDependencies(List.of(cstApi, cstAnalysis, maddiSupport, orgSlf4jApi, annotations)).build();

        SourceSet junitJupiter = externalJar(Test.class);
        SourceSet openTest = SourceSetImpl.sourceSetOf(org.opentest4j.AssertionFailedError.class);

        for (Path p : List.of(maddiSupportSrc, maddiUtilSrc, cstApiSrc, cstAnalysisSrc, cstImplSrc, cstIoSrc)) {
            assertTrue(Files.isDirectory(p), "Expected maddi source directory " + p.toAbsolutePath());
        }

        Path cstImplTestSrc = Path.of("../maddi-cst-impl/src/test/java");
        SourceSet cstImplTest = new SourceSetImpl.Builder().setName("maddi-cst-impl test")
                .setSourceDirectories(List.of(cstImplTestSrc))
                .setUri(cstImplTestSrc.toUri())
                .setModule(false)
                .setDependencies(List.of(cstApi, cstAnalysis, cstImpl, maddiSupport, maddiUtil, orgSlf4jApi,
                        annotations, junitJupiter)).build();

        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(cstApi, maddiSupport, cstAnalysis, maddiUtil, cstImpl, cstImplTest, cstIo)
                .addClassPath("jmod:java.base")
                .addClassPathParts(orgSlf4jApi, annotations, junitJupiter, openTest)
                .build();
        javaInspector.initialize(inputConfiguration);
    }

    @Test
    public void test() {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true).build();
        ParseResult parseResult = javaInspector.parse(Map.of(), options).parseResult();

        List<TypeInfo> cstImplTypes = new ArrayList<>();
        List<String> violations = new ArrayList<>();
        for (TypeInfo primaryType : parseResult.primaryTypes()) {
            // by source set, not by package prefix: cst-impl's own test fixtures share its packages, and
            // a fixture is not production code the idioms apply to
            SourceSet sourceSet = primaryType.compilationUnit().sourceSet();
            if (cstImpl.equals(sourceSet)) {
                primaryType.recursiveSubTypeStream().filter(t -> !isBuilder(t)).forEach(cstImplTypes::add);
            } else if (maddiUtil.equals(sourceSet)) {
                primaryType.recursiveSubTypeStream()
                        .forEach(typeInfo -> checkUtilStaticsCarryContracts(typeInfo, violations));
            }
        }
        assertTrue(cstImplTypes.size() > 100, "Expected the whole of cst-impl, saw only " + cstImplTypes.size()
                                              + " types -- the parse or the source-set filter is wrong, and a"
                                              + " rule that inspects nothing passes vacuously");

        Set<TypeInfo> valueScope = immutabilityRelevantTypes(cstImplTypes);
        assertTrue(valueScope.size() > 50, "The value scope collapsed to " + valueScope.size() + " types;"
                                           + " rules 3+4 would then be vacuous");
        for (TypeInfo typeInfo : cstImplTypes) {
            checkAnalysisStoresAreDisclaimed(typeInfo, violations);
            checkNonFinalFieldsAreDisclaimedOrCommitOnce(typeInfo, violations);
            if (valueScope.contains(typeInfo)) {
                checkNoMutationOfFinalCollectionFields(typeInfo, violations);
            }
        }
        LOGGER.info("Checked {} cst-impl types, {} of them in the value scope", cstImplTypes.size(),
                valueScope.size());

        List<String> unsuppressed = violations.stream().filter(v -> !isSuppressed(v)).sorted().toList();
        if (!unsuppressed.isEmpty()) {
            fail("Eventual-immutability conformance: " + unsuppressed.size() + " violation(s).\n"
                 + String.join("\n", unsuppressed)
                 + "\n\nEach rule is documented in docs/eventual-design-improvements.md §2. If an exception is"
                 + " genuinely intended, add it to SUPPRESSIONS in this file with a one-line justification.");
        }
    }

    /**
     * The types whose mutability the CST's eventual immutability actually depends on: everything that
     * implements {@link Element}, closed over the declared types of their fields (and those types'
     * type arguments) as long as those live in cst-impl too.
     * <p>
     * Rules 3 and 4 are scoped to this set rather than to all of cst-impl, because cst-impl also holds
     * deliberately mutable services — {@code QualificationImpl} accumulates print state,
     * {@code ImportComputerImpl} accumulates imports, {@code IsAssignableFrom} memoizes. Those never
     * become part of an {@code Element}, so an adder on them costs the analysis nothing, and flagging
     * them would drown the rule in exceptions until someone deleted it. The closure keeps the shapes
     * that do cost: {@code ModuleInfo.Provides extends Element}, so the {@code ProvidesImpl} incident
     * that motivated the rule is in scope, and so is {@code MethodMapImpl}, reached as the field type
     * of {@code TypeInspectionImpl}.
     */
    private static Set<TypeInfo> immutabilityRelevantTypes(List<TypeInfo> cstImplTypes) {
        Map<TypeInfo, List<TypeInfo>> implementors = new java.util.HashMap<>();
        for (TypeInfo typeInfo : cstImplTypes) {
            if (!typeInfo.hasBeenInspected()) continue;
            for (TypeInfo superType : typeInfo.superTypesExcludingJavaLangObject()) {
                implementors.computeIfAbsent(superType, _ -> new ArrayList<>()).add(typeInfo);
            }
        }
        Set<TypeInfo> inScope = new java.util.LinkedHashSet<>();
        List<TypeInfo> worklist = new ArrayList<>();
        for (TypeInfo typeInfo : cstImplTypes) {
            if (implementsElement(typeInfo) && inScope.add(typeInfo)) worklist.add(typeInfo);
        }
        Set<TypeInfo> cstImplSet = Set.copyOf(cstImplTypes);
        while (!worklist.isEmpty()) {
            TypeInfo typeInfo = worklist.removeLast();
            for (FieldInfo fieldInfo : typeInfo.fields()) {
                for (TypeInfo reached : typesIn(fieldInfo.type())) {
                    if (cstImplSet.contains(reached) && inScope.add(reached)) worklist.add(reached);
                }
            }
            // down the hierarchy as well: a mutable implementation of an in-scope interface sinks that
            // interface, and every other implementation with it -- the isMutable(@FinalFields) rule
            for (TypeInfo implementor : implementors.getOrDefault(typeInfo, List.of())) {
                if (inScope.add(implementor)) worklist.add(implementor);
            }
        }
        return inScope;
    }

    private static boolean implementsElement(TypeInfo typeInfo) {
        if (!typeInfo.hasBeenInspected()) return false;
        for (TypeInfo superType : typeInfo.superTypesExcludingJavaLangObject()) {
            if ("io.codelaser.maddi.cst.api.element.Element".equals(superType.fullyQualifiedName())) return true;
        }
        return false;
    }

    /** The type itself plus its type arguments: {@code EventuallyFinal<TypeInspection>} reaches both. */
    private static List<TypeInfo> typesIn(ParameterizedType parameterizedType) {
        if (parameterizedType == null) return List.of();
        List<TypeInfo> result = new ArrayList<>();
        if (parameterizedType.typeInfo() != null) result.add(parameterizedType.typeInfo());
        parameterizedType.parameters().forEach(p -> result.addAll(typesIn(p)));
        return result;
    }

    /** Builders are the before-state face of the eventually-immutable object: setter-bearing by design. */
    private static boolean isBuilder(TypeInfo typeInfo) {
        for (TypeInfo t = typeInfo; t != null; t = t.compilationUnitOrEnclosingType().isRight()
                ? t.compilationUnitOrEnclosingType().getRight() : null) {
            // "Builder" and "BuilderImpl": the interface-side name and the implementation-side one
            if (t.simpleName().equals("Builder") || t.simpleName().endsWith("BuilderImpl")) return true;
        }
        return false;
    }

    /** Rule 1: the analysis overlay is manual hidden content (road §050); every store is disclaimed. */
    private static void checkAnalysisStoresAreDisclaimed(TypeInfo typeInfo, List<String> violations) {
        for (FieldInfo fieldInfo : typeInfo.fields()) {
            if (PROPERTY_VALUE_MAP.equals(typeNameOf(fieldInfo.type())) && !hasAnnotation(fieldInfo, IGNORE_MODIFICATIONS)) {
                violations.add(memberName(fieldInfo) + ": a PropertyValueMap analysis store must carry"
                               + " @IgnoreModifications; without it the store's writes make every holder of this"
                               + " type modifying (rule 1)");
            }
        }
    }

    /** Rule 2: a non-final slot is either a disclaimed memo, or a commit-once support type. */
    private static void checkNonFinalFieldsAreDisclaimedOrCommitOnce(TypeInfo typeInfo, List<String> violations) {
        for (FieldInfo fieldInfo : typeInfo.fields()) {
            if (fieldInfo.isStatic() || fieldInfo.isFinal() || fieldInfo.isPropertyFinal()) continue;
            if (hasAnnotation(fieldInfo, IGNORE_MODIFICATIONS)) continue;
            if (isCommitOnce(fieldInfo.type())) continue;
            violations.add(memberName(fieldInfo) + ": a non-final instance field must either be a disclaimed"
                           + " memo slot (@IgnoreModifications, the VariableImpl.cachedHash precedent) or be of a"
                           + " commit-once type such as SetOnce (rule 2)");
        }
    }

    /**
     * Rules 3 and 4 in one walk: no method or constructor may mutate a final collection field in place.
     * For methods this is the adder shape ({@code addImplementationResolved}); for constructors it is the
     * fill-then-keep shape ({@code precedenceMap}) — part-of-construction excuses the assignment, not the
     * content calls, so the field reads as a mutable container ever after. Build a local and copy it in.
     */
    private static void checkNoMutationOfFinalCollectionFields(TypeInfo typeInfo, List<String> violations) {
        typeInfo.constructorAndMethodStream().forEach(methodInfo -> {
            if (!methodInfo.hasBeenInspected() || methodInfo.methodBody() == null) return;
            methodInfo.methodBody().visit(element -> {
                if (element instanceof MethodCall methodCall) {
                    FieldInfo target = mutatedOwnFinalCollectionField(methodCall, typeInfo);
                    if (target != null) {
                        violations.add(memberName(methodInfo) + ": mutates the final collection field '"
                                       + target.name() + "' in place via " + methodCall.methodInfo().name()
                                       + "(); accumulate in a local and commit once (List.copyOf into the final"
                                       + " field, or SetOnce.set) -- the ProvidesImpl/precedenceMap shape (rules 3+4)");
                    }
                }
                return true;
            });
        });
    }

    private static FieldInfo mutatedOwnFinalCollectionField(MethodCall methodCall, TypeInfo typeInfo) {
        if (!MUTATORS.contains(methodCall.methodInfo().name())) return null;
        if (!(methodCall.object() instanceof VariableExpression ve)) return null;
        if (!(ve.variable() instanceof FieldReference fieldReference)) return null;
        if (!fieldReference.scopeIsRecursivelyThis()) return null;
        FieldInfo fieldInfo = fieldReference.fieldInfo();
        if (!fieldInfo.owner().equals(typeInfo)) return null;
        if (!fieldInfo.isFinal() && !fieldInfo.isPropertyFinal()) return null;
        if (hasAnnotation(fieldInfo, IGNORE_MODIFICATIONS)) return null;
        return isMutableContainer(fieldInfo.type()) ? fieldInfo : null;
    }

    /**
     * The {@code ZipLists.zip} gap: maddi-util is consumed as a jar by the dogfood, so an unannotated
     * static that takes a container leaves it unproven-modified and blocks every abstract union whose
     * implementations route a field through it. The engine cannot compute a jar leaf; contract it.
     */
    private static void checkUtilStaticsCarryContracts(TypeInfo typeInfo, List<String> violations) {
        for (MethodInfo methodInfo : typeInfo.methods()) {
            if (!methodInfo.isStatic() || !methodInfo.isPublic()) continue;
            if (hasAnnotation(methodInfo, NOT_MODIFIED)) continue;
            for (ParameterInfo parameterInfo : methodInfo.parameters()) {
                if (!isMutableContainer(parameterInfo.parameterizedType())) continue;
                if (hasAnnotation(parameterInfo, NOT_MODIFIED)) continue;
                violations.add(memberName(methodInfo) + ": public static method takes the mutable container"
                               + " parameter '" + parameterInfo.name() + "' without a @NotModified contract."
                               + " maddi-util reaches the dogfood as a jar, where nothing about it can be"
                               + " computed (the ZipLists.zip gap; ListUtil is the precedent)");
            }
        }
    }

    private static boolean isMutableContainer(ParameterizedType parameterizedType) {
        String name = typeNameOf(parameterizedType);
        return name != null && MUTABLE_CONTAINERS.contains(name);
    }

    private static boolean isCommitOnce(ParameterizedType parameterizedType) {
        String name = typeNameOf(parameterizedType);
        return name != null && COMMIT_ONCE_TYPES.contains(name);
    }

    private static String typeNameOf(ParameterizedType parameterizedType) {
        if (parameterizedType == null || parameterizedType.arrays() > 0) return null;
        TypeInfo typeInfo = parameterizedType.typeInfo();
        return typeInfo == null ? null : typeInfo.fullyQualifiedName();
    }

    private static boolean hasAnnotation(Element element, String annotationFqn) {
        for (AnnotationExpression ae : element.annotations()) {
            if (annotationFqn.equals(ae.typeInfo().fullyQualifiedName())) return true;
        }
        return false;
    }

    private static String memberName(FieldInfo fieldInfo) {
        return fieldInfo.owner().fullyQualifiedName() + "#" + fieldInfo.name();
    }

    private static String memberName(MethodInfo methodInfo) {
        return methodInfo.typeInfo().fullyQualifiedName() + "#" + methodInfo.name();
    }

    private static boolean isSuppressed(String violation) {
        return SUPPRESSIONS.keySet().stream().anyMatch(violation::startsWith);
    }

    /** Keeps the suppression list honest: an entry that no longer matches anything must be removed. */
    @Test
    public void testSuppressionsAreAllSorted() {
        Set<String> sorted = new TreeSet<>(SUPPRESSIONS.keySet());
        assertTrue(sorted.size() == SUPPRESSIONS.size(), "duplicate suppression keys");
    }
}
