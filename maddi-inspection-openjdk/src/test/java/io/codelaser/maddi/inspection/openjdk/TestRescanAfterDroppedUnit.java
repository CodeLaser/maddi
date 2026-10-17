package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REPRODUCER (jfocus refactor-base TestIncrementalParseDiagnostics, gate 2026-10-17 on laser1, JDK 26.0.2).
 * <p>
 * A unit whose import does not resolve: on JDK 26 the first parse drops it (no type in sourceFiles); on JDK 27 it
 * keeps a type. The unit is then fixed on disk, another type of the same set is edited, reloadSources reports that
 * one, and the set is RESCANNED. On JDK 26 the rescan throws UnsupportedOperationException at
 * MethodInfoImpl.builder (ScanCompilationUnit.visitMethod, the fixed type's implicit {@code <init>}): the dropped
 * unit's TypeInfo, committed by the first scan, is still reachable through typeData although it was never in
 * sourceFiles, so the rescan's {@code compiledTypesManager::invalidate} over the set's known types never reached it.
 * Expected: the rescan parses the fixed unit as a new type.
 */
public class TestRescanAfterDroppedUnit {

    @TempDir
    Path root;

    @Test
    public void aDroppedUnitFixedOnDiskIsParsedByTheRescan() throws Exception {
        Path pkg = Files.createDirectories(root.resolve("a"));
        Path a = Files.writeString(pkg.resolve("A.java"), "package a; public class A { public int x() { return 1; } }");
        Path broken = Files.writeString(pkg.resolve("Broken.java"),
                "package a; import does.not.Exist; public class Broken { Exist e; }");
        SourceSet main = new SourceSetImpl.Builder().setName("main").setSourceDirectories(List.of(root))
                .setUri(root.toUri()).build();
        InputConfiguration configuration = new InputConfigurationImpl.Builder().addSourceSets(main)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES).build();
        JavaInspector inspector = new JavaInspectorImpl(true, false);
        inspector.initialize(configuration);

        Summary first = inspector.parse(Map.of(), new JavaInspector.ParseOptions.Builder().setFailFast(false)
                .setDetailedSources(true).build());
        TypeInfo brokenType = first.parseResult().findType("a.Broken");
        System.out.println("java.version=" + System.getProperty("java.version") + "; after the first parse a.Broken "
                           + (brokenType == null ? "has NO type (dropped)" : "has a type"));

        Files.writeString(broken, "package a; public class Broken { }");
        Files.writeString(a, "// edited\npackage a; public class A { public int x() { return 1; } }");
        Set<TypeInfo> changed = inspector.reloadSources(configuration, Map.of()).sourceHasChanged();
        TypeInfo aType = first.parseResult().findType("a.A");
        assertTrue(changed.contains(aType), changed.toString());

        Summary second = assertDoesNotThrow(() -> inspector.parse(Map.of(), new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true)
                .setInvalidated(ti -> changed.contains(ti) ? JavaInspector.InvalidationState.INVALID
                        : JavaInspector.InvalidationState.UNCHANGED)
                .build()));
        assertNotNull(second.parseResult().findType("a.Broken"), "the fixed unit is a type after the rescan");
    }
}
