package io.codelaser.maddi.modification.prepwork.kotlin;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.kotlin.api.KotlinFrontEnd;
import io.codelaser.maddi.kotlin.api.KotlinFrontEnds;
import io.codelaser.maddi.kotlin.api.PlaceholderCensus;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.variable.VariableData;
import io.codelaser.maddi.modification.prepwork.variable.VariableInfo;
import io.codelaser.maddi.modification.prepwork.variable.impl.VariableDataImpl;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Prep on KOTLIN input, from inside prepwork. Every fixture is a pair: a Kotlin class `k.X` and the Java class `j.X`
 that says what kotlinc makes of it. Both go through ONE mixed parse (MixedProjectInspector, the production path:
 one runtime, the JDK from bytecode), prep runs on every primary type, and the variables of a method that exists in
 both must get the same definition, assignments and reads.

 The Java twin is the oracle, so a difference is a finding, not a golden to update: either the lowering is not
 faithful (a front-end issue), or prep treats the two CSTs differently (a prepwork issue). Where a difference is
 filed, the test pins the Kotlin side and names the issue.

 Why not KotlinScan on its own: without the Java front end's CompiledTypesManager, K2 builds library types from its
 own symbols, a model no production run uses (`s.length` does not even resolve there; see
 KotlinTypeMapper.bootstrapString).
 */
public abstract class CommonKotlinTest {

    /* The front end runs flat on the test class path, as in maddi-inspection-kotlin's tests. */
    @BeforeAll
    public static void installFrontEnd() {
        if (!KotlinFrontEnds.isInstalled()) KotlinFrontEnds.install(KotlinFrontEnd.load());
    }

    /* The result of one mixed parse, with prep done on all of it. */
    protected record Parsed(Runtime runtime, TypeInfo kotlinX, TypeInfo javaX, List<TypeInfo> kotlinTypes,
                            List<TypeInfo> javaTypes) {
        MethodInfo kotlin(String name) {
            return method(kotlinX, name);
        }

        MethodInfo java(String name) {
            return method(javaX, name);
        }

        /* The differential assertion: the same summary for the method `name` on both sides. */
        void assertSameAsJava(String name) {
            assertEquals(summary(java(name)), summary(kotlin(name)), "method " + name);
        }
    }

    /*
     Parse `kotlin` (package k) and `java` (package j) together, and run prep on every primary type. Each source
     must declare a class X. Extra Java files (`fileName -> source`, package j) may sit beside them.
     */
    protected static Parsed prep(String kotlin, String java, String... extraJava) {
        return prep(true, kotlin, java, extraJava);
    }

    protected static Parsed prep(boolean runPrep, String kotlin, String java, String... extraJava) {
        try {
            Path root = Files.createTempDirectory("prep-kotlin");
            Path kDir = root.resolve("kotlin");
            Path jDir = root.resolve("java");
            Files.createDirectories(kDir.resolve("k"));
            Files.createDirectories(jDir.resolve("j"));
            Files.writeString(kDir.resolve("k/X.kt"), kotlin);
            Files.writeString(jDir.resolve("j/X.java"), java);
            for (int i = 0; i + 1 < extraJava.length; i += 2) {
                Files.writeString(jDir.resolve("j/" + extraJava[i]), extraJava[i + 1]);
            }
            SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                    .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri()).build();
            SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                    .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri())
                    .setDependencies(List.of(javaSet)).build();
            InputConfiguration config = new InputConfigurationImpl.Builder()
                    .addSourceSets(javaSet).addSourceSets(kotlinSet).addClassPath(kotlinStdlibJar()).build();
            MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(config);

            // a construct the front end could not read is a placeholder, and both sides could then agree on
            // variables neither derived from the code: every fixture must convert completely
            PlaceholderCensus census = PlaceholderCensus.of(parsed.getKotlinTypes());
            assertEquals(0, census.getTotal(), "unread Kotlin: " + census.dumpLines());

            Runtime runtime = parsed.getRuntime();
            if (runPrep) {
                Set<TypeInfo> primaryTypes = Stream.concat(parsed.getKotlinTypes().stream(),
                                parsed.getJavaTypes().stream())
                        .map(TypeInfo::primaryType).collect(Collectors.toUnmodifiableSet());
                new PrepAnalyzer(runtime).doPrimaryTypes(primaryTypes);
            }
            return new Parsed(runtime, find(parsed.getKotlinTypes(), "k.X"), find(parsed.getJavaTypes(), "j.X"),
                    parsed.getKotlinTypes(), parsed.getJavaTypes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /* The front end is flat on this JVM's class path, so the stdlib jar is too; the mixed parse needs it named. */
    private static String kotlinStdlibJar() {
        String cp = System.getProperty("java.class.path", "");
        return Stream.of(cp.split(java.io.File.pathSeparator))
                .filter(p -> Path.of(p).getFileName().toString().matches("kotlin-stdlib-\\d[^-]*\\.jar"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no kotlin-stdlib jar on the test class path: " + cp));
    }

    protected static TypeInfo find(List<TypeInfo> types, String fqn) {
        return types.stream().filter(t -> fqn.equals(t.fullyQualifiedName())).findFirst()
                .orElseThrow(() -> new AssertionError("no type " + fqn + " in " + types));
    }

    /*
     The locals and parameters a method's body knows at its end, one line each: simple name, definition and
     assignments, reads. Fields and `this` are left out: Kotlin reaches a property through its accessor, Java
     through the field; the dedicated tests look at that.
     */
    protected static String summary(MethodInfo methodInfo) {
        return summary(VariableDataImpl.of(methodInfo));
    }

    protected static String summary(VariableData vd) {
        return vd.variableInfoStream()
                .filter(vi -> !(vi.variable() instanceof This) && !(vi.variable() instanceof FieldReference))
                .sorted(Comparator.comparing(vi -> vi.variable().simpleName()))
                .map(CommonKotlinTest::line)
                .collect(Collectors.joining("\n"));
    }

    protected static String line(VariableInfo vi) {
        return vi.variable().simpleName() + ": " + vi.assignments() + " | R " + vi.reads();
    }

    protected static MethodInfo method(TypeInfo typeInfo, String name) {
        return typeInfo.methodStream().filter(m -> name.equals(m.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no method " + name + " in " + typeInfo));
    }
}
