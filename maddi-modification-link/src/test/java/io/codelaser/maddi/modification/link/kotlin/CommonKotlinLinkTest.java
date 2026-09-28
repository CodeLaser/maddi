package io.codelaser.maddi.modification.link.kotlin;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.kotlin.api.KotlinFrontEnd;
import io.codelaser.maddi.kotlin.api.KotlinFrontEnds;
import io.codelaser.maddi.kotlin.api.PlaceholderCensus;
import io.codelaser.maddi.modification.link.LinkComputer;
import io.codelaser.maddi.modification.link.impl.LinkComputerImpl;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.io.LoadAnalysisResults;
import io.codelaser.maddi.modification.prepwork.variable.MethodLinkedVariables;
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
 Linking KOTLIN input, from inside the link module. Every fixture is a pair: a Kotlin class `k.X` and the Java class
 `j.X` that says what kotlinc makes of it. Both go through ONE mixed parse (MixedProjectInspector, the production
 path), the annotated JDK and library results are loaded as in the Java tests' CommonTest, prep runs on every primary
 type, and LinkComputerImpl computes each method's MethodLinkedVariables.

 A method's links print without fully qualified names (parameters by index, `this`, virtual fields named after type
 parameters), so the two sides compare as strings. The Java twin is the oracle: a difference is a finding. Where a
 difference is filed, the test pins both sides and names the issue, so it fails the day the issue is fixed.
 */
public abstract class CommonKotlinLinkTest {

    /* The front end runs flat on the test class path, as in maddi-inspection-kotlin's tests. */
    @BeforeAll
    public static void installFrontEnd() {
        if (!KotlinFrontEnds.isInstalled()) KotlinFrontEnds.install(KotlinFrontEnd.load());
    }

    protected record Parsed(Runtime runtime, TypeInfo kotlinX, TypeInfo javaX, List<TypeInfo> kotlinTypes,
                            LinkComputer linkComputer) {
        MethodInfo kotlin(String name) {
            return method(kotlinX, name);
        }

        MethodInfo java(String name) {
            return method(javaX, name);
        }

        String kotlinLinks(String name) {
            return links(kotlin(name));
        }

        String javaLinks(String name) {
            return links(java(name));
        }

        String links(MethodInfo methodInfo) {
            MethodLinkedVariables mlv = linkComputer.doMethod(methodInfo);
            return mlv.toString();
        }

        /*
         The differential assertion: the same method links on both sides. Compared in a normal form (the links of
         each parameter, and of the return value, sorted): a constructor or field write prints the same links in
         a different order on the two sides, which is insertion order, not meaning.
         */
        void assertSameAsJava(String name) {
            assertEquals(normalize(javaLinks(name)), normalize(kotlinLinks(name)), "method " + name);
        }
    }

    /*
     Parse `kotlin` (package k) and `java` (package j) together, load the annotated APIs, and run prep on every
     primary type. Each source must declare a class X.
     */
    protected static Parsed link(String kotlin, String java) {
        try {
            Path root = Files.createTempDirectory("link-kotlin");
            Path kDir = root.resolve("kotlin");
            Path jDir = root.resolve("java");
            Files.createDirectories(kDir.resolve("k"));
            Files.createDirectories(jDir.resolve("j"));
            Files.writeString(kDir.resolve("k/X.kt"), kotlin);
            Files.writeString(jDir.resolve("j/X.java"), java);
            // the stdlib is a dependency of BOTH source sets: javac's class path is built from a Java source set's
            // dependencies, so without it every kotlin.* type is a memberless stub on the Java side
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
            // links neither derived from the code: every fixture must convert completely
            PlaceholderCensus census = PlaceholderCensus.of(parsed.getKotlinTypes());
            assertEquals(0, census.getTotal(), "unread Kotlin: " + census.dumpLines());

            Runtime runtime = parsed.getRuntime();
            new LoadAnalysisResults(runtime, kotlinSet).go(LoadAnalysisResults.ANALYZED_RESULTS);
            Set<TypeInfo> primaryTypes = Stream.concat(parsed.getKotlinTypes().stream(),
                            parsed.getJavaTypes().stream())
                    .map(TypeInfo::primaryType).collect(Collectors.toUnmodifiableSet());
            new PrepAnalyzer(runtime).doPrimaryTypes(primaryTypes);
            LinkComputer linkComputer = new LinkComputerImpl(parsed.getJavaInspector(), LinkComputer.Options.TEST);
            return new Parsed(runtime, find(parsed.getKotlinTypes(), "k.X"), find(parsed.getJavaTypes(), "j.X"),
                    parsed.getKotlinTypes(), linkComputer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /*
     The front end is flat on this JVM's class path, so the stdlib jar is too. It must be a class-path PART and a
     dependency, for the Java side as well: a plain class-path string reaches K2 only, and javac then stubs every
     kotlin.* type it meets (memberless), which silently drops a Java file that calls one. The source set is named
     after the jar FILE: that is how ClassSymbolScanner finds a class-path entry's source set.
     */
    private static SourceSet kotlinStdlib() {
        String cp = System.getProperty("java.class.path", "");
        Path jar = Stream.of(cp.split(java.io.File.pathSeparator)).map(Path::of)
                .filter(p -> p.getFileName().toString().matches("kotlin-stdlib-\\d[^-]*\\.jar"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no kotlin-stdlib jar on the test class path: " + cp));
        return new SourceSetImpl.Builder().setName(jar.getFileName().toString())
                .setExternalLibrary(true).setLibrary(true).setUri(jar.toUri()).build();
    }

    /* "[a,b, c] --> d,e": sort the comma-separated links inside each parameter and inside the return value */
    static String normalize(String mlv) {
        int arrow = mlv.indexOf(" --> ");
        String params = mlv.substring(1, arrow - 1);
        String ret = mlv.substring(arrow + 5);
        String sortedParams = params.isEmpty() ? "" : Stream.of(params.split(", "))
                .map(CommonKotlinLinkTest::sortLinks).collect(Collectors.joining(", "));
        return "[" + sortedParams + "] --> " + sortLinks(ret);
    }

    private static String sortLinks(String links) {
        return Stream.of(links.split(",")).sorted().collect(Collectors.joining(","));
    }

    protected static TypeInfo find(List<TypeInfo> types, String fqn) {
        return types.stream().flatMap(TypeInfo::recursiveSubTypeStream)
                .filter(t -> fqn.equals(t.fullyQualifiedName())).findFirst()
                .orElseThrow(() -> new AssertionError("no type " + fqn + " in " + types));
    }

    protected static MethodInfo method(TypeInfo typeInfo, String name) {
        return typeInfo.constructorAndMethodStream().filter(m -> name.equals(m.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no method " + name + " in " + typeInfo));
    }
}
