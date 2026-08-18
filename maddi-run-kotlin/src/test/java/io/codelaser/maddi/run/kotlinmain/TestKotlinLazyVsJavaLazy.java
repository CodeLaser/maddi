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
 * <b>The equivalence holds, and over a mutable payload it is exact.</b> {@code a.KotlinMutable} and
 * {@code b.JavaHolder} are the same shape over two unrelated {@code Lazy} types — one from the Kotlin
 * standard library, one compiled from source here — and both come out {@code @FinalFields} unconditionally
 * and {@code @Immutable(hc=true)(after="slot")}. Not "both immutable in some sense": the same verdict, at the
 * same precision, on the same lattice. That is the cross-language claim §12.6 rests on.
 * <p>
 * Over an <em>immutable</em> payload the Kotlin side goes one better, and the reason is worth reading twice.
 * {@code a.KotlinExplicit} and {@code a.KotlinDelegated} hold a {@code Lazy<String>} and are plain
 * {@code IMMUTABLE} — substituting {@code T=String} into an {@code @ImmutableContainer(hc=true)} leaves no
 * hidden content, so nothing mutable remains to reach. {@code a.KotlinMutable} is the control that shows this
 * is the substitution talking and not a blanket verdict: identical code over {@code Lazy<StringBuilder>}
 * drops straight back to {@code FINAL_FIELDS}. The Java side stays at {@code FINAL_FIELDS} even for
 * {@code Lazy<String>}, because {@code b.Lazy}'s own unconditional verdict is {@code MUTABLE} and its
 * immutability is carried in the eventual property instead. <b>That gap is the contract, not the language</b>
 * — {@code b.Lazy} is contracted eventually immutable and {@code kotlin.Lazy} unconditionally so.
 * <p>
 * What the Java side still knows and the Kotlin side does not is <em>when</em>. {@code b.JavaHolder.get()}
 * earns {@code @Mark("slot")} from {@code b.Lazy.get()}'s own {@code @Mark("t")}; {@code a.KotlinMutable.get()}
 * earns nothing, because reading {@code val value} is a FIELD read and a mark is set by a METHOD. That is
 * exactly why the annotated API contracts the unconditional form — see the long note in
 * {@code maddi-aapi-archive/.../libs/kotlin/Kotlin.java}, which records what that choice gives up.
 * <p>
 * <h2>kotlin.Lazy is contracted now, and the fix was in the registry</h2>
 * The {@code kotlin.Lazy} row reads {@code IMMUTABLE_HC} because
 * {@code maddi-aapi-archive/.../libs/kotlin/Kotlin.java} says so ({@code c39524c32}). It did not, for a while:
 * the hint was parsed and then dropped — "Skipping analysis hints for kotlin.Lazy: type not on the classpath"
 * — while {@code java.util.List} resolved from the same source set. The type was in the CST, reachable by
 * navigating to it (as the test body still does), and not findable BY NAME, which is the one thing
 * annotated-API loading needs.
 * <p>
 * The cause was that the Kotlin front end registered a library type it minted in the shared {@code InfoByFqn}
 * only, never in the shared {@code CompiledTypesManager} — and {@code Runtime.getFullyQualified}, which is how
 * {@code LoadAnalysisResults} resolves an entry, reads the latter. {@code KotlinTypeMapper.registerLibraryType}
 * now writes both, as the Java front end has always done at commit time. Note the last line of the
 * {@code kotlin.Lazy} block: {@code ANNOTATIONS SEEN: []}. A contract arrives as analysis properties, not as
 * annotations on the {@code TypeInfo} — the row above it is where the contract shows up.
 * <p>
 * The delegated form agrees with the explicit one, and that is also new. {@code val expensive: String by lazy
 * { … }} used to produce no backing field and an empty accessor body, so the analyzer saw a class whose only
 * field was an {@code int}. It concluded plain {@code @Immutable} — the right answer for the wrong reason, and
 * one that would have stayed right-looking here. Since {@code d980df509} a delegated property is modelled as
 * the JVM has it, a private final {@code <name>$delegate} field of the delegate's type, and
 * {@code a.KotlinDelegated} now tracks {@code a.KotlinExplicit} exactly, field name apart.
 * <p>
 * The fixture compiles {@code Lazy} from source rather than taking it off the {@code maddi-support} jar,
 * because the annotated-API archive carries no entry for {@code Lazy} — the jar route would make it an unknown
 * instead of a computed verdict. It is the shipped shape, kept in step with it by hand: {@code supplier} is
 * dropped at the transition, as Kotlin's is.
 */
public class TestKotlinLazyVsJavaLazy {

    /** See the class javadoc for what each line means, and which of them are findings. */
    private static final String EXPECTED = """
            a.KotlinDelegated  type=IMMUTABLE  eventual=<not eventual>
                field n : Type int
                field expensive$delegate : Type kotlin.Lazy<String>
                method getExpensive/0  independent=INDEPENDENT_HC  eventual=<not eventual>
                ANNOTATIONS SEEN: []
            a.KotlinExplicit  type=IMMUTABLE  eventual=<not eventual>
                field n : Type int
                field slot : Type kotlin.Lazy<String>
                method get/0  independent=INDEPENDENT_HC  eventual=<not eventual>
                ANNOTATIONS SEEN: []
            a.KotlinMutable  type=FINAL_FIELDS  eventual=@Immutable(hc=true)(after="slot")
                field n : Type int
                field slot : Type kotlin.Lazy<StringBuilder>
                method get/0  independent=INDEPENDENT_HC  eventual=<not eventual>
                ANNOTATIONS SEEN: []
            b.JavaHolder  type=FINAL_FIELDS  eventual=@Immutable(hc=true)(after="slot")
                field n : Type int
                field slot : Type b.Lazy<String>
                method get/0  independent=INDEPENDENT  eventual=@Mark("slot")
                ANNOTATIONS SEEN: []
            kotlin.Lazy  type=IMMUTABLE_HC  eventual=<not eventual>
                field value : Type param T
                method isInitialized/0  independent=INDEPENDENT  eventual=<not eventual>
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

            // 3. THE CONTROL: the same shape over a MUTABLE payload. The annotated API contracts
            // kotlin.Lazy as @ImmutableContainer(hc=true) -- immutable MODULO hidden content -- and 1 and 2
            // land on plain IMMUTABLE only because String is deeply immutable, so the substitution leaves no
            // hidden content. If the contract were a blanket "immutable" this class would say IMMUTABLE too.
            class KotlinMutable(private val n: Int) {
                private val slot: Lazy<StringBuilder> = lazy { StringBuilder("v" + n) }
                fun get(): StringBuilder = slot.value
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
     * It is the shipped shape: {@code supplier} is dropped at the transition, as Kotlin's is, but each
     * volatile field is read once into a local and the value is published before the supplier is cleared --
     * which the book's listing, read literally, does not do.
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
                @Final(after = "t")
                private volatile Supplier<T> supplier;

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
                    T value = t;
                    if (value != null) return value;
                    Supplier<T> localSupplier = supplier;
                    if (localSupplier == null) return t;
                    t = Objects.requireNonNull(localSupplier.get());
                    supplier = null;
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

        // Named after the JAR FILE, not something friendlier: ClassSymbolScanner.ensureSourceSet attributes a
        // loaded class file by that name, and Runtime.getFullyQualified -- which is how LoadAnalysisResults
        // resolves an annotated-API entry -- needs the type attributed to a source set reachable from the one it
        // is asked about. Named "kotlin-stdlib-bin", every hint for kotlin.Lazy was parsed and then dropped with
        // "type not on the classpath".
        SourceSet stdlib = new SourceSetImpl.Builder()
                .setName(Path.of(kotlinStdlibJar()).getFileName().toString())
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

        // The whole archive, not just its JDK part. Without the JDK, Supplier and Objects are unknowns and the
        // Java side cannot be concluded either, which would make the comparison vacuously equal; and libs/kotlin
        // is what contracts kotlin.Lazy, whose standing is the difference this test measures.
        new LoadAnalysisResults(runtime, kotlinSet).go(LoadAnalysisResults.ANALYZED_RESULTS);

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
                + describe(analyzed.type("a.KotlinMutable"))
                + describe(javaHolder)
                + describe(kotlinLazy)
                + describe(javaLazy);

        assertEquals(EXPECTED, actual);
    }
}
