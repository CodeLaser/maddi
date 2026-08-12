/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 */
package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * ⛔ <b>A class path belongs to a source set, so an on-demand compiled-type load must run on the source set that
 * ASKED</b> — not on whichever source set the inspector happens to have a javac task for.
 * <p>
 * The lazy loader behind {@code CompiledTypesManager.getOrLoad(fqn, sourceSetOfRequest)} used to ignore its second
 * argument and always resolve through {@code lastScanUnits}, the set scanned LAST. Every configuration with more
 * than one source set therefore answered by scan order: a jar on set A's class path but not on set B's became
 * unresolvable for A the moment B was scanned after it. jfocus hit exactly this — a class-path preload of
 * {@code io.codelaser.jfocus.transform.support} followed by the scan of a corpus source set without that jar, and
 * the analyzed-package decode died on "Cannot find …Loop.LoopData".
 * <p>
 * Two source sets, each carrying a different real jar, declared in this order — which is the scan order, since
 * {@code computeScanOrder} keeps declaration order between sets that do not depend on each other:
 * <ol>
 *     <li>{@code with-slf4j} — java.base + the slf4j-api jar. NOT the last scanned: this is the regression.</li>
 *     <li>{@code with-opentest4j} — java.base + the opentest4j jar.</li>
 * </ol>
 * ⛔ <b>Each set must carry a jar of its own, not merely lack the other's.</b> {@code setCompileClassPath} returns
 * early on an empty list, so a set with no jar dependency never gets an explicit {@code CLASS_PATH} and javac falls
 * back to the JVM's own — under Gradle that is the whole test runtime, and every negative assertion here would
 * pass vacuously. A JDK module does not work either: {@code ClassSymbolScanner.moduleOnClassPath} reads a map built
 * from the WHOLE input configuration, so a module configured anywhere is visible to every set.
 */
public class TestLazyLoaderSourceSet {

    // in the slf4j jar, which only with-slf4j carries
    private static final String SLF4J_TYPE = "org.slf4j.LoggerFactory";
    private static final String OTHER_SLF4J_TYPE = "org.slf4j.MDC";
    private static final String THIRD_SLF4J_TYPE = "org.slf4j.Marker";
    // in the opentest4j jar, which only with-opentest4j carries
    private static final String OPENTEST4J_TYPE = "org.opentest4j.AssertionFailedError";

    private record Fixture(JavaInspector javaInspector, SourceSet withSlf4j, SourceSet withOpentest4j) {
    }

    private static Fixture parse() throws IOException {
        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet slf4jJar = SourceSetImpl.sourceSetOf(org.slf4j.Logger.class, javaBase);
        SourceSet opentest4jJar = SourceSetImpl.sourceSetOf(org.opentest4j.AssertionFailedError.class, javaBase);
        SourceSet withSlf4j = new SourceSetImpl.Builder()
                .setName("with-slf4j").setUri(URI.create("file:/"))
                .setDependencies(List.of(javaBase, slf4jJar))
                .build();
        SourceSet withOpentest4j = new SourceSetImpl.Builder()
                .setName("with-opentest4j").setUri(URI.create("file:/"))
                .setDependencies(List.of(javaBase, opentest4jJar))
                .build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addClassPathParts(javaBase, slf4jJar, opentest4jJar)
                .addSourceSets(withSlf4j, withOpentest4j)
                .build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration);
        // scans BOTH source sets (the warm-up unit goes into each), so each gets a loader spec of its own, and
        // with-opentest4j -- declared last -- is what lastScanUnits points at afterwards
        javaInspector.onlyPreload();
        return new Fixture(javaInspector, withSlf4j, withOpentest4j);
    }

    /**
     * The regression: with-slf4j carries the slf4j jar and must resolve a type from it through its OWN task, even
     * though with-opentest4j was scanned after it. Before the fix this returned null.
     */
    @Test
    public void requestingSetsOwnClassPathIsUsed() throws IOException {
        Fixture f = parse();
        TypeInfo slf4j = f.javaInspector.compiledTypesManager().getOrLoad(SLF4J_TYPE, f.withSlf4j);
        assertNotNull(slf4j, SLF4J_TYPE + " is on with-slf4j's class path and must resolve for it, whatever was"
                             + " scanned last");
    }

    /**
     * The negative direction, which is what makes the assertion above mean anything: with-opentest4j does not carry
     * the slf4j jar, so a type from it is a miss — and it stays a miss even though with-slf4j's task, which could
     * resolve it, is still alive. The fall-back only runs through the LAST scan's task, and that is this set's own.
     */
    @Test
    public void aTypeOutsideTheRequestingSetsClassPathIsAMiss() throws IOException {
        Fixture f = parse();
        TypeInfo slf4j = f.javaInspector.compiledTypesManager().getOrLoad(OTHER_SLF4J_TYPE, f.withOpentest4j);
        assertNull(slf4j, OTHER_SLF4J_TYPE + " is not on with-opentest4j's class path");
    }

    /** The control, in the other direction: the last-scanned set resolves its own jar too. */
    @Test
    public void theLastScannedSetKeepsItsOwnClassPath() throws IOException {
        Fixture f = parse();
        assertNotNull(f.javaInspector.compiledTypesManager().getOrLoad(OPENTEST4J_TYPE, f.withOpentest4j));
    }

    /** java.base is on both class paths: neither set may lose it. */
    @Test
    public void aSharedDependencyResolvesForEitherSet() throws IOException {
        Fixture f = parse();
        assertNotNull(f.javaInspector.compiledTypesManager().getOrLoad("java.util.List", f.withSlf4j));
        assertNotNull(f.javaInspector.compiledTypesManager().getOrLoad("java.util.Map", f.withOpentest4j));
    }

    /**
     * A nested type is loaded as part of its enclosing type, so it must follow the requesting set's class path too
     * — the shape that broke in jfocus, where the missing type was {@code Loop.LoopData} rather than {@code Loop}.
     */
    @Test
    public void nestedTypesFollowTheRequestingSetToo() throws IOException {
        Fixture f = parse();
        TypeInfo nested = f.javaInspector.compiledTypesManager()
                .getOrLoad("org.slf4j.spi.LoggingEventBuilder", f.withSlf4j);
        assertNotNull(nested, "a with-slf4j-only type in a sub-package must resolve for with-slf4j");
        TypeInfo enclosed = f.javaInspector.compiledTypesManager()
                .getOrLoad("org.slf4j.event.Level", f.withSlf4j);
        assertNotNull(enclosed);
    }

    /**
     * Strict mode is the follow-up audit's switch: it drops the fall-back to the last scan's task, so a request
     * carrying a source set that cannot see the type is a miss even when some other set could have answered it.
     * Off by default, because ~8 production call sites pass {@code mainSources()} rather than the set that is
     * really asking, and would lose types they resolve today.
     */
    @Test
    public void strictModeDropsTheFallBack() throws IOException {
        Fixture f = parse();
        JavaInspectorImpl impl = (JavaInspectorImpl) f.javaInspector;

        // with-slf4j is NOT the last-scanned set, so without strict mode a type only IT can see is still found
        // through its own task; and a type only the LAST set can see is found through the fall-back.
        assertNotNull(impl.compiledTypesManager().getOrLoad(SLF4J_TYPE, f.withSlf4j));
        assertNotNull(impl.compiledTypesManager().getOrLoad(OPENTEST4J_TYPE, f.withSlf4j),
                "the fall-back to the last scan's task is what keeps this change additive");

        impl.setStrictSourceSetLoading(true);
        assertNull(impl.compiledTypesManager().getOrLoad("org.opentest4j.MultipleFailuresError", f.withSlf4j),
                "strict mode: with-slf4j's own class path is the only answer");
        assertNotNull(impl.compiledTypesManager().getOrLoad(THIRD_SLF4J_TYPE, f.withSlf4j),
                "strict mode must not cost a set the types it can see itself");
    }
}
