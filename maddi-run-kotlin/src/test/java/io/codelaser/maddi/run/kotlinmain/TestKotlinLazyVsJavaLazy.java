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

package io.codelaser.maddi.run.kotlinmain;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.modification.analyzer.IteratingAnalyzer;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.callgraph.ComputeAnalysisOrder;
import io.codelaser.maddi.modification.prepwork.io.LoadAnalysisResults;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A lazily initialised value, written twice: once in Kotlin and once in Java, through
 * {@code io.codelaser.maddi.support.Lazy}. Does maddi reach the same conclusion about the two?
 * <p>
 * This is <em>The Road to Immutability</em> §12.6 asked across languages. §12.6 keeps its
 * {@code Lazy} section although the analyzer's own code no longer uses the type (see
 * {@code docs/book-vs-support-divergence.md}, findings 2 and 11); the reason to keep it is that
 * {@code Lazy} is the Java half of an idiom Kotlin has in its standard library, and this test is where
 * that claim is checked rather than asserted.
 * <p>
 * <h2>What the run says</h2>
 * <b>The equivalence holds.</b> {@code a.KotlinExplicit} and {@code b.JavaHolder} are both concluded immutable
 * for a private field of a lazily-initialising type, from two unrelated {@code Lazy} types, neither of which is
 * itself immutable. That is the cross-language claim §12.6 rests on, checked rather than asserted.
 * <p>
 * It holds at different <em>precision</em>, and that is the finding. The Java side carries its eventual
 * contract all the way to the holder — {@code b.JavaHolder} is {@code @FinalFields} unconditionally and
 * {@code @Immutable(hc=true)(after="slot")}, with {@code get()} earning {@code @Mark("slot")} from
 * {@code Lazy.get()}'s own {@code @Mark("t")}. The Kotlin side states {@code @Immutable(hc=true)} flatly, with
 * no eventual story at all, because {@code kotlin.Lazy} arrives as unannotated library bytecode: it is
 * {@code @Mutable} with every method {@code DEPENDENT}, and there is no Kotlin annotated API to say otherwise
 * ({@code analyzedPackageFiles/} holds {@code jdk} and {@code libs/…} only). <b>The unconditional verdict is
 * the same; only the Java side knows when it becomes true.</b> Writing an AAPI entry for {@code kotlin.Lazy}
 * is what would close that, and this block is where it would show up.
 * <p>
 * The third shape is the one to worry about. {@code val expensive: String by lazy { … }} — the idiom — is
 * <b>not modelled, and the verdict is wrong rather than absent</b>. The K2 front-end produces no backing field
 * and an empty accessor body for a delegated property, so the analyzer sees a class whose only field is an
 * {@code int} and concludes plain {@code @Immutable}: a type with mutable lazy state reported as deeply
 * immutable, one level ABOVE the honest {@code @Immutable(hc=true)} of the same value written explicitly.
 * Checked against three controls — a plain {@code val}, a custom {@code get()}, and an explicit {@code Lazy}
 * field all produce correct fields and bodies — so it is delegated properties specifically, not a resolution
 * failure.
 * <p>
 * The fixture compiles {@code Lazy} from source rather than taking it off the {@code maddi-support} jar,
 * because the annotated-API archive carries no entry for {@code Lazy} — the jar route would make it an unknown
 * instead of a computed verdict. It is the shipped shape, not the book's: {@code supplier} is {@code final}
 * and is never cleared (finding 2).
 */
public class TestKotlinLazyVsJavaLazy {

    /** See the class javadoc for what each line means, and which of them are findings. */
    private static final String EXPECTED = """
            a.KotlinDelegated  type=IMMUTABLE  eventual=<not eventual>
                field n : Type int
                method getExpensive/0  independent=INDEPENDENT  eventual=<not eventual>
                ANNOTATIONS SEEN: []
            a.KotlinExplicit  type=IMMUTABLE_HC  eventual=<not eventual>
                field n : Type int
                field slot : Type kotlin.Lazy<String>
                method get/0  independent=INDEPENDENT_HC  eventual=<not eventual>
                ANNOTATIONS SEEN: []
            b.JavaHolder  type=FINAL_FIELDS  eventual=@Immutable(hc=true)(after="slot")
                field n : Type int
                field slot : Type b.Lazy<String>
                method get/0  independent=INDEPENDENT  eventual=@Mark("slot")
                ANNOTATIONS SEEN: []
            kotlin.Lazy  type=MUTABLE  eventual=<not eventual>
                field value : Type param T
                method isInitialized/0  independent=DEPENDENT  eventual=<not eventual>
                method equals/1  independent=DEPENDENT  eventual=<not eventual>
                method hashCode/0  independent=DEPENDENT  eventual=<not eventual>
                method toString/0  independent=DEPENDENT  eventual=<not eventual>
                ANNOTATIONS SEEN: []
            b.Lazy  type=MUTABLE  eventual=@Immutable(hc=true)(after="t")
                field supplier : Type java.util.function.Supplier<T>
                field t : Type param T
                method get/0  independent=INDEPENDENT_HC  eventual=@Mark("t")
                method hasBeenEvaluated/0  independent=INDEPENDENT  eventual=@TestMark("t")
                ANNOTATIONS SEEN: [@ImmutableContainer(after="t",hc=true)]
            """;


    private static final String KOTLIN_SRC = """
            package a

            // 1. the idiom
            class KotlinDelegated(private val n: Int) {
                val expensive: String by lazy { "v" + n }
            }

            // 2. the same machinery, no delegate
            class KotlinExplicit(private val n: Int) {
                private val slot: Lazy<String> = lazy { "v" + n }
                fun get(): String = slot.value
            }
            """;

    /**
     * {@code io.codelaser.maddi.support.Lazy} <b>verbatim</b>, minus javadoc — annotations included, because
     * without them this would be a rigged comparison: the eventual annotations are what let a holder of a
     * {@code Lazy} field be more than {@code @FinalFields}, and Kotlin's {@code Lazy} gets its standing from
     * being an interface. Compiled from source rather than taken off the {@code maddi-support} jar because the
     * annotated-API archive carries no entry for {@code Lazy}, so the jar route would make it an unknown
     * instead of a computed verdict.
     * <p>
     * It is the shipped shape, not the book's: {@code supplier} is {@code final} and is never cleared
     * (finding 2).
     */
    private static final String JAVA_LAZY_SRC = """
            package b;

            import io.codelaser.maddi.annotation.Final;
            import io.codelaser.maddi.annotation.ImmutableContainer;
            import io.codelaser.maddi.annotation.Modified;
            import io.codelaser.maddi.annotation.NotModified;
            import io.codelaser.maddi.annotation.NotNull;
            import io.codelaser.maddi.annotation.eventual.Mark;
            import io.codelaser.maddi.annotation.eventual.TestMark;

            import java.util.Objects;
            import java.util.function.Supplier;

            @ImmutableContainer(after = "t", hc = true)
            public class Lazy<T> {
                private final Supplier<T> supplier;

                @Final(after = "t")
                private volatile T t;

                public Lazy(Supplier<T> supplierParam) {
                    if (supplierParam == null) throw new NullPointerException("Null not allowed");
                    this.supplier = supplierParam;
                }

                @NotNull
                @Modified
                @Mark(value = "t")
                public T get() {
                    if (t != null) return t;
                    t = Objects.requireNonNull(supplier.get());
                    return t;
                }

                @NotModified
                @TestMark("t")
                public boolean hasBeenEvaluated() {
                    return t != null;
                }
            }
            """;

    private static final String JAVA_HOLDER_SRC = """
            package b;

            public class JavaHolder {
                private final int n;
                private final Lazy<String> slot;

                public JavaHolder(int n) {
                    this.n = n;
                    this.slot = new Lazy<>(() -> "v" + n);
                }

                public String get() {
                    return slot.get();
                }
            }
            """;

    /** The kotlin-stdlib bytecode, so K2 can resolve {@code lazy} and {@code kotlin.Lazy}. */
    private static String kotlinStdlibJar() {
        return Stream.of(System.getProperty("java.class.path").split(File.pathSeparator))
                .filter(p -> p.matches(".*kotlin-stdlib-[0-9].*\\.jar$"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("kotlin-stdlib jar not on the test classpath"));
    }


    private record Analyzed(Runtime runtime, Set<TypeInfo> primaryTypes) {
        TypeInfo type(String fqn) {
            return primaryTypes.stream().filter(t -> fqn.equals(t.fullyQualifiedName())).findFirst()
                    .orElseThrow(() -> new AssertionError("no type " + fqn + " in "
                            + primaryTypes.stream().map(TypeInfo::fullyQualifiedName).sorted().toList()));
        }
    }

    /**
     * Parse the mixed project, prep it, and run the iterating modification analysis over the result. Mirrors
     * {@link RunMixedPrepAnalyzer#go} — which returns only counts, and this test needs the elements.
     */
    private Analyzed analyze(Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("b"));
        Files.writeString(kDir.resolve("a/Holders.kt"), KOTLIN_SRC);
        Files.writeString(jDir.resolve("b/Lazy.java"), JAVA_LAZY_SRC);
        Files.writeString(jDir.resolve("b/JavaHolder.java"), JAVA_HOLDER_SRC);

        SourceSet stdlib = new SourceSetImpl.Builder().setName("kotlin-stdlib-bin")
                .setSourceDirectories(List.of()).setUri(URI.create("file:" + kotlinStdlibJar()))
                .setLibrary(true).setExternalLibrary(true).build();
        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri())
                .setDependencies(List.of(stdlib)).build();
        // MUST be sourceSetOf, not a hand-rolled Builder. ClassSymbolScanner.ensureSourceSet attributes a
        // loaded 'jar:file:...!/...' class file by looking the JAR'S FILE NAME up among the source-set names,
        // and sourceSetOf is what derives that name (tail(uri)). Naming the set anything friendlier makes every
        // annotation type resolve to "off-classpath", after which the contract silently never arrives.
        SourceSet annotations = SourceSetImpl.sourceSetOf(io.codelaser.maddi.annotation.ImmutableContainer.class);
        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri())
                .setDependencies(List.of(annotations)).build();
        InputConfiguration config = new InputConfigurationImpl.Builder()
                .addClassPathParts(stdlib).addClassPathParts(annotations)
                .addSourceSets(kotlinSet).addSourceSets(javaSet).build();

        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(config);
        Runtime runtime = parsed.getRuntime();
        Set<TypeInfo> primaryTypes = Stream.concat(parsed.getKotlinTypes().stream(),
                        parsed.getJavaTypes().stream())
                .map(TypeInfo::primaryType).collect(Collectors.toUnmodifiableSet());

        // Without the JDK annotated API, Supplier and Objects are unknowns and the Java side cannot be
        // concluded either -- which would make the comparison vacuously equal.
        new LoadAnalysisResults(runtime, kotlinSet).go(List.of(LoadAnalysisResults.ANALYZED_RESULTS_JDK));

        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime,
                new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
        G<Info> callGraph = prepAnalyzer.doPrimaryTypesReturnGraph(primaryTypes);
        List<Info> order = new ComputeAnalysisOrder().go(callGraph);
        IteratingAnalyzer.Configuration configuration = new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setMaxIterations(30).setStopWhenCycleDetectedAndNoImprovements(true).setFaultTolerant(true)
                .build();
        new IteratingAnalyzerImpl(parsed.getJavaInspector(), configuration).analyze(order, callGraph);
        return new Analyzed(runtime, primaryTypes);
    }

    private static String immutable(TypeInfo typeInfo) {
        Value.Immutable v = typeInfo.analysis()
                .getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE);
        if (v.isImmutable()) return "IMMUTABLE";
        if (v.isAtLeastImmutableHC()) return "IMMUTABLE_HC";
        if (v.isFinalFields()) return "FINAL_FIELDS";
        return "MUTABLE";
    }

    private static String independent(MethodInfo methodInfo) {
        Value.Independent v = methodInfo.analysis().getOrDefault(PropertyImpl.INDEPENDENT_METHOD,
                ValueImpl.IndependentImpl.DEPENDENT);
        if (v.isIndependentHc()) return "INDEPENDENT_HC";
        if (v.isDependent()) return "DEPENDENT";
        return "INDEPENDENT";
    }

    /**
     * The {@code after="…"} promise, kept deliberately out of {@link PropertyImpl#IMMUTABLE_TYPE} — that lattice
     * records only what holds unconditionally. Reading just the lattice would report the shipped {@code Lazy} as
     * plain {@code MUTABLE} and hide the entire point of the type.
     */
    private static String eventual(TypeInfo typeInfo) {
        Object v = typeInfo.analysis().getOrDefault(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE,
                ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL);
        String s = String.valueOf(v);
        return s.isEmpty() ? "-" : s;
    }

    private static String describe(TypeInfo typeInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(typeInfo.fullyQualifiedName()).append("  type=").append(immutable(typeInfo))
                .append("  eventual=").append(eventual(typeInfo)).append('\n');
        for (FieldInfo f : typeInfo.fields()) {
            sb.append("    field ").append(f.name()).append(" : ").append(f.type()).append('\n');
        }
        for (MethodInfo m : typeInfo.methods()) {
            Object ev = m.analysis().getOrDefault(PropertyImpl.EVENTUAL_METHOD,
                    ValueImpl.EventualImpl.NOT_EVENTUAL);
            sb.append("    method ").append(m.name()).append('/').append(m.parameters().size())
                    .append("  independent=").append(independent(m))
                    .append("  eventual=").append(ev).append('\n');
        }
        sb.append("    ANNOTATIONS SEEN: ").append(typeInfo.annotations()).append('\n');
        return sb.toString();
    }

    @DisplayName("a lazily initialised value in Kotlin and in Java: what does maddi make of each?")
    @Test
    public void kotlinLazyVersusJavaLazy(@TempDir Path tmp) throws Exception {
        Analyzed analyzed = analyze(tmp);
        TypeInfo kotlinExplicit = analyzed.type("a.KotlinExplicit");
        TypeInfo javaHolder = analyzed.type("b.JavaHolder");
        // Neither Lazy comes back among the primary types, so reach each through the field that uses it.
        TypeInfo kotlinLazy = kotlinExplicit.getFieldByName("slot", true).type().typeInfo();
        TypeInfo javaLazy = javaHolder.getFieldByName("slot", true).type().typeInfo();

        String actual = describe(analyzed.type("a.KotlinDelegated"))
                + describe(kotlinExplicit)
                + describe(javaHolder)
                + describe(kotlinLazy)
                + describe(javaLazy);

        assertEquals(EXPECTED, actual);
    }
}
