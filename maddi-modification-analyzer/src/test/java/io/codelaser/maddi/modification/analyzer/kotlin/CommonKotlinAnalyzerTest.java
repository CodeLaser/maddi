package io.codelaser.maddi.modification.analyzer.kotlin;

import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.kotlin.api.KotlinFrontEnd;
import io.codelaser.maddi.kotlin.api.KotlinFrontEnds;
import io.codelaser.maddi.kotlin.api.PlaceholderCensus;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.callgraph.ComputeAnalysisOrder;
import io.codelaser.maddi.modification.prepwork.io.LoadAnalysisResults;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 The modification analyzer on KOTLIN input, from inside the analyzer module. Every fixture is a pair: Kotlin sources
 in package `k` and the Java that kotlinc makes of them in package `j`, with the same type and member names. Both go
 through ONE mixed parse (MixedProjectInspector, the production path), the annotated JDK and libs/kotlin results are
 loaded, prep runs fault-tolerantly and must isolate nothing, and the iterating analyzer runs to its end, as the
 mixed CLI does.

 The verdicts of a member are printed as one line (see method/field/type below), so the two sides compare as
 strings. The Java twin is the oracle: a difference is a finding. Where it is filed, the test pins both sides and
 names the issue, so it fails the day the issue is fixed.
 */
public abstract class CommonKotlinAnalyzerTest {

    /* The front end runs flat on the test class path, as in maddi-inspection-kotlin's tests. */
    @BeforeAll
    public static void installFrontEnd() {
        if (!KotlinFrontEnds.isInstalled()) KotlinFrontEnds.install(KotlinFrontEnd.load());
    }

    protected record Analyzed(Runtime runtime, List<TypeInfo> kotlinTypes, List<TypeInfo> javaTypes) {
        TypeInfo kotlinType(String simpleName) {
            return find(kotlinTypes, "k." + simpleName);
        }

        TypeInfo javaType(String simpleName) {
            return find(javaTypes, "j." + simpleName);
        }

        /* the verdicts of type `simpleName` and of all its members, as one block per side */
        String kotlin(String simpleName) {
            return verdicts(kotlinType(simpleName));
        }

        String java(String simpleName) {
            return verdicts(javaType(simpleName));
        }

        /* The differential assertion: type `simpleName` has the same verdicts, member by member, on both sides. */
        void assertSameAsJava(String simpleName) {
            assertEquals(java(simpleName), kotlin(simpleName), "type " + simpleName);
        }

        /* the same, for the members named here only (Kotlin synthesizes members Java does not have) */
        void assertSameAsJava(String simpleName, String... members) {
            assertEquals(verdicts(javaType(simpleName), members), verdicts(kotlinType(simpleName), members),
                    "type " + simpleName);
        }

        String kotlin(String simpleName, String... members) {
            return verdicts(kotlinType(simpleName), members);
        }

        String java(String simpleName, String... members) {
            return verdicts(javaType(simpleName), members);
        }
    }

    /*
     Parse `kotlin` (files in package k: name -> source) and `java` (files in package j), load the annotated APIs,
     prep, and analyze.
     */
    protected static Analyzed analyze(List<String> kotlin, List<String> java) {
        try {
            Path root = Files.createTempDirectory("analyzer-kotlin");
            Path kDir = root.resolve("kotlin");
            Path jDir = root.resolve("java");
            Files.createDirectories(kDir.resolve("k"));
            Files.createDirectories(jDir.resolve("j"));
            for (int i = 0; i + 1 < kotlin.size(); i += 2) Files.writeString(kDir.resolve("k/" + kotlin.get(i)), kotlin.get(i + 1));
            for (int i = 0; i + 1 < java.size(); i += 2) Files.writeString(jDir.resolve("j/" + java.get(i)), java.get(i + 1));
            // the stdlib is a class-path part AND a dependency of both source sets: javac's class path is built
            // from a Java source set's dependencies, and without it every kotlin.* type is a memberless stub
            SourceSet stdlib = kotlinStdlib();
            SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                    .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri())
                    .setDependencies(List.of(stdlib)).build();
            SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                    .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri())
                    .setDependencies(List.of(javaSet, stdlib)).build();
            InputConfiguration config = new InputConfigurationImpl.Builder()
                    .addSourceSets(javaSet).addSourceSets(kotlinSet).addClassPathParts(stdlib).build();
            MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(config);

            // a construct the front end could not read is a placeholder, and both sides could then agree on
            // verdicts neither derived from the code: every fixture must convert completely
            PlaceholderCensus census = PlaceholderCensus.of(parsed.getKotlinTypes());
            assertEquals(0, census.getTotal(), "unread Kotlin: " + census.dumpLines());

            Runtime runtime = parsed.getRuntime();
            // without the annotated APIs java.util.List is an unknown, and nothing can be concluded on either side
            new LoadAnalysisResults(runtime, kotlinSet).go(LoadAnalysisResults.ANALYZED_RESULTS);
            Set<TypeInfo> primaryTypes = Stream.concat(parsed.getKotlinTypes().stream(),
                            parsed.getJavaTypes().stream())
                    .map(TypeInfo::primaryType).collect(Collectors.toUnmodifiableSet());
            PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime,
                    new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
            G<Info> callGraph = prepAnalyzer.doPrimaryTypesReturnGraph(primaryTypes);
            assertEquals(List.of(), prepAnalyzer.exceptions().stream().map(String::valueOf).toList(),
                    "prep must isolate nothing");
            List<Info> order = new ComputeAnalysisOrder().go(callGraph);
            new IteratingAnalyzerImpl(parsed.getJavaInspector(), new IteratingAnalyzerImpl.ConfigurationBuilder()
                    .setMaxIterations(30).setStopWhenCycleDetectedAndNoImprovements(true).setFaultTolerant(true)
                    .build()).analyze(order, callGraph);
            return new Analyzed(runtime, parsed.getKotlinTypes(), parsed.getJavaTypes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected static Analyzed analyze(String kotlin, String java) {
        return analyze(List.of("X.kt", kotlin), List.of("X.java", java));
    }

    private static SourceSet kotlinStdlib() {
        String cp = System.getProperty("java.class.path", "");
        Path jar = Stream.of(cp.split(java.io.File.pathSeparator)).map(Path::of)
                .filter(p -> p.getFileName().toString().matches("kotlin-stdlib-\\d[^-]*\\.jar"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no kotlin-stdlib jar on the test class path: " + cp));
        return new SourceSetImpl.Builder().setName(jar.getFileName().toString())
                .setExternalLibrary(true).setLibrary(true).setUri(jar.toUri()).build();
    }

    protected static MethodInfo method(TypeInfo typeInfo, String name) {
        return typeInfo.constructorAndMethodStream().filter(m -> name.equals(m.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no method " + name + " in " + typeInfo));
    }

    protected static TypeInfo find(List<TypeInfo> types, String fqn) {
        return types.stream().flatMap(TypeInfo::recursiveSubTypeStream)
                .filter(t -> fqn.equals(t.fullyQualifiedName())).findFirst()
                .orElseThrow(() -> new AssertionError("no type " + fqn + " in " + types));
    }

    /*
     The type's verdicts, then one line per field, constructor and method, sorted: K2 and javac declare fields in a
     different order (an object's INSTANCE, an enum's constants), which is not a difference in meaning. With `members`,
     only those fields and methods (a constructor is `<init>`): Kotlin synthesizes members Java does not declare.
     */
    protected static String verdicts(TypeInfo typeInfo, String... members) {
        Set<String> only = Set.of(members);
        Stream<String> fields = typeInfo.fields().stream()
                .filter(f -> only.isEmpty() || only.contains(f.name())).map(CommonKotlinAnalyzerTest::field);
        Stream<String> methods = typeInfo.constructorAndMethodStream()
                .filter(m -> only.isEmpty() || only.contains(m.name())).map(CommonKotlinAnalyzerTest::method);
        return type(typeInfo) + Stream.concat(fields, methods).sorted().map(l -> "\n" + l).collect(Collectors.joining());
    }

    protected static String type(TypeInfo t) {
        return "type " + t.simpleName() + ": " + v(t, PropertyImpl.IMMUTABLE_TYPE) + " " + v(t, PropertyImpl.INDEPENDENT_TYPE);
    }

    protected static String field(FieldInfo f) {
        return "field " + f.name() + ": final=" + v(f, PropertyImpl.FINAL_FIELD) + " unmodified="
               + v(f, PropertyImpl.UNMODIFIED_FIELD) + " " + v(f, PropertyImpl.INDEPENDENT_FIELD);
    }

    protected static String method(MethodInfo m) {
        String params = m.parameters().stream().map(CommonKotlinAnalyzerTest::parameter)
                .collect(Collectors.joining(", "));
        return (m.isConstructor() ? "constructor" : "method " + m.name()) + ": nonModifying="
               + v(m, PropertyImpl.NON_MODIFYING_METHOD) + " " + v(m, PropertyImpl.INDEPENDENT_METHOD)
               + (params.isEmpty() ? "" : " | " + params);
    }

    protected static String parameter(ParameterInfo p) {
        return p.index() + ": unmodified=" + v(p, PropertyImpl.UNMODIFIED_PARAMETER) + " "
               + v(p, PropertyImpl.INDEPENDENT_PARAMETER);
    }

    private static String v(Info info, Property property) {
        Value value = info.analysis().getOrNull(property, Value.class);
        return value == null ? "?" : value.toString();
    }
}
