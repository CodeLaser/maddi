package io.codelaser.maddi.run.kotlinmain.kotlinc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import io.codelaser.maddi.run.config.compile.CompileInvocation;

/**
 * A {@code kotlinc} and a {@code javac} invocation that write into ONE output directory, as one invocation.
 *
 * <p>⛔ THE OUTPUT DIRECTORY IS THE SOURCE SET'S IDENTITY, and a mixed Maven module gives two compilers the same
 * one: kotlin-maven-plugin and maven-compiler-plugin both write {@code target/classes}, over the same source
 * directory (javalin keeps its {@code .kt} and {@code .java} files side by side in {@code src/main/java}). Left
 * as two invocations, the second's source directories contained the first's, so the containment rule absorbed
 * the Kotlin compile into a LIBRARY over {@code target/classes} -- the module's own bytecode on its own class
 * path -- and the survivor was named {@code javalin/main2}. The test pair was worse: kotlinc compiled
 * {@code src/test/kotlin} and {@code src/test/java}, javac only the latter, so neither contained the other and
 * both survived as two source sets with one URI.
 *
 * <p>One module, one source set, both languages: which is what {@code MixedProjectInspector} expects of a mixed
 * module (mixed-language-integration.md §2). The language of a file is its extension, not its invocation.
 *
 * <p>javac supplies the Java options (release, language level, preview, exports, warning flags, encoding), kotlinc
 * the Kotlin ones (module name, friend paths, language and API version, JVM target, JDK home); class paths and
 * source directories are the union, kotlinc's first.
 */
record SharedDestination(Kotlinc kotlinc, CompileInvocation javac) implements CompileInvocation {

    /**
     * Pairs each javac invocation with the kotlinc invocation of the same destination, at the position of the
     * earlier of the two (log order is dependency order). Anything unpaired passes through unchanged.
     */
    static List<CompileInvocation> merge(List<CompileInvocation> all) {
        List<CompileInvocation> invocations = foldKotlinc(all);
        Map<String, Kotlinc> kotlincByDestination = new LinkedHashMap<>();
        Map<String, CompileInvocation> javacByDestination = new LinkedHashMap<>();
        for (CompileInvocation inv : invocations) {
            if (inv instanceof Kotlinc k) kotlincByDestination.putIfAbsent(k.destination(), k);
            else javacByDestination.putIfAbsent(inv.destination(), inv);
        }
        List<CompileInvocation> result = new ArrayList<>(invocations.size());
        Set<String> emitted = new HashSet<>();
        for (CompileInvocation inv : invocations) {
            String destination = inv.destination();
            Kotlinc k = kotlincByDestination.get(destination);
            CompileInvocation j = javacByDestination.get(destination);
            if (k == null || j == null) {
                result.add(inv);
            } else if (emitted.add(destination)) {
                result.add(new SharedDestination(k, j));
            } else if (inv != k && inv != j) {
                result.add(inv); // a third compile into the same directory is not ours to fold
            }
        }
        return result;
    }

    @Override
    public String destination() {
        return kotlinc.destination();
    }

    @Override
    public List<String> classpath() {
        return union(kotlinc.classpath(), javac.classpath());
    }

    @Override
    public List<String> modulePath() {
        return javac.modulePath();
    }

    @Override
    public List<String> sourcePath() {
        return union(kotlinc.sourcePath(), javac.sourcePath());
    }

    @Override
    public List<String> sourceFiles() {
        return union(kotlinc.sourceFiles(), javac.sourceFiles());
    }

    @Override
    public String encoding() {
        return javac.encoding() != null ? javac.encoding() : kotlinc.encoding();
    }

    @Override
    public int release() {
        return javac.release();
    }

    @Override
    public int sourceRelease() {
        return javac.sourceRelease();
    }

    @Override
    public int effectiveRelease() {
        return javac.effectiveRelease();
    }

    @Override
    public int languageLevel() {
        return javac.languageLevel();
    }

    @Override
    public boolean enablePreview() {
        return javac.enablePreview();
    }

    @Override
    public String kotlinLanguageVersion() {
        return kotlinc.kotlinLanguageVersion();
    }

    @Override
    public String kotlinApiVersion() {
        return kotlinc.kotlinApiVersion();
    }

    @Override
    public List<String> kotlinPluginOptions() {
        return kotlinc.kotlinPluginOptions();
    }

    @Override
    public int jvmTarget() {
        return kotlinc.jvmTarget();
    }

    @Override
    public String jdkHome() {
        return kotlinc.jdkHome();
    }

    @Override
    public List<String> addModules() {
        return javac.addModules();
    }

    @Override
    public List<String> addExports() {
        return javac.addExports();
    }

    @Override
    public List<String> warningFlags() {
        return javac.warningFlags();
    }

    @Override
    public String moduleName() {
        return kotlinc.moduleName();
    }

    /**
     * Several kotlinc invocations of one destination, as one, at the first one's position. kotlin-maven-plugin's
     * {@code kapt} execution logs a block of its own into {@code target/classes} before the {@code compile} one,
     * both listing {@code src/main/kotlin} (quarkus's panache-kotlin runtimes): unfolded, the same {@code .kt}
     * sources sat in two source sets, {@code main} and {@code main2}. Source directories and class paths are the
     * union, so the kapt-generated directories are kept.
     */
    static List<CompileInvocation> foldKotlinc(List<CompileInvocation> invocations) {
        Map<String, Kotlinc> folded = new LinkedHashMap<>();
        for (CompileInvocation inv : invocations) {
            if (inv instanceof Kotlinc k && k.destination() != null) folded.merge(k.destination(), k, SharedDestination::fold);
        }
        List<CompileInvocation> result = new ArrayList<>(invocations.size());
        Set<String> emitted = new HashSet<>();
        for (CompileInvocation inv : invocations) {
            if (inv instanceof Kotlinc k && k.destination() != null) {
                if (emitted.add(k.destination())) result.add(folded.get(k.destination()));
            } else {
                result.add(inv);
            }
        }
        return result;
    }

    private static Kotlinc fold(Kotlinc a, Kotlinc b) {
        return new Kotlinc(a.jvmTarget() != 0 ? a.jvmTarget() : b.jvmTarget(),
                a.apiVersion() != null ? a.apiVersion() : b.apiVersion(),
                a.languageVersion() != null ? a.languageVersion() : b.languageVersion(), a.destination(),
                a.moduleName() != null ? a.moduleName() : b.moduleName(),
                a.jdkHome() != null ? a.jdkHome() : b.jdkHome(), union(a.classpath(), b.classpath()),
                union(a.sourcePath(), b.sourcePath()), union(a.sourceFiles(), b.sourceFiles()),
                union(a.friendPaths(), b.friendPaths()), a.encoding() != null ? a.encoding() : b.encoding(),
                union(a.kotlinPluginOptions(), b.kotlinPluginOptions()));
    }

    @Override
    public List<String> friendPaths() {
        return kotlinc.friendPaths();
    }

    private static List<String> union(List<String> first, List<String> second) {
        return Stream.concat(first == null ? Stream.empty() : first.stream(),
                        second == null ? Stream.empty() : second.stream())
                .filter(Objects::nonNull).collect(LinkedHashSet<String>::new, LinkedHashSet::add, LinkedHashSet::addAll)
                .stream().toList();
    }
}
