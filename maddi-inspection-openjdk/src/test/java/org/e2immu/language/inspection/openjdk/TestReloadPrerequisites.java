package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The three prerequisites for reloading + rewiring on the openjdk inspector: the {@code sourceFiles} map that
 * {@code reloadSources} diffs against, and the {@code CompiledTypesManager}'s {@code invalidate} / {@code
 * setRewiredType}, which the invalidated-aware parse will drive. Fingerprints are covered by {@link TestFingerPrint}.
 */
public class TestReloadPrerequisites {

    private JavaInspector javaInspector;
    private SourceSet sourceSet;

    @Language("java")
    private static final String X = """
            package a.b;
            public class X {
                public static class Inner { }
                public int method() { return 1; }
            }
            """;

    @Language("java")
    private static final String Y = """
            package a.b;
            public class Y {
                public X x() { return null; }
            }
            """;

    @BeforeEach
    public void before() throws IOException {
        javaInspector = new JavaInspectorImpl(true, false);
        sourceSet = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "1").setUri(URI.create("file:/")).build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
        javaInspector.initialize(inputConfiguration);
    }

    private ParseResult parse() {
        return javaInspector.parseMultiSourceSet(Map.of(sourceSet, Map.of("a.b.X", X, "a.b.Y", Y)),
                JavaInspectorImpl.DETAILED_SOURCES).parseResult();
    }

    @DisplayName("sourceFiles: one entry per source file, carrying its types and its fingerprint")
    @Test
    public void testSourceFilesPopulated() {
        ParseResult pr = parse();
        assertEquals(2, pr.primaryTypes().size());

        java.util.Set<SourceFile> files = javaInspector.sourceFiles();
        assertEquals(2, files.size(), "expected one SourceFile per compilation unit, have: " + files);
        for (SourceFile sf : files) {
            assertSame(sourceSet, sf.sourceSet());
            assertNotNull(sf.fingerPrint(), "the SourceFile must carry the fingerprint reloadSources compares");
            assertFalse(sf.fingerPrint().isNoFingerPrint());
            assertFalse(sf.path().startsWith("/"), "SourceFile asserts a .java path is not absolute: " + sf.path());
        }
        assertEquals(List.of("a/b/X.java", "a/b/Y.java"),
                files.stream().map(SourceFile::path).sorted().toList());
    }

    @DisplayName("invalidate: the primary type and its subtypes disappear from the registry")
    @Test
    public void testInvalidate() {
        ParseResult pr = parse();
        TypeInfo x = pr.findType("a.b.X");
        assertNotNull(x);
        CompiledTypesManager ctm = javaInspector.compiledTypesManager();
        assertSame(x, ctm.get("a.b.X", sourceSet));
        assertNotNull(ctm.get("a.b.X.Inner", sourceSet), "the subtype should be registered before invalidation");

        ctm.invalidate(x);

        assertNull(ctm.get("a.b.X", sourceSet), "the invalidated type must be gone");
        assertNull(ctm.get("a.b.X.Inner", sourceSet), "its subtypes must go with it");
        // a type of another compilation unit is untouched
        assertSame(pr.findType("a.b.Y"), ctm.get("a.b.Y", sourceSet));
    }

    @DisplayName("setRewiredType: the registry points at the new objects, same FQNs")
    @Test
    public void testSetRewiredType() {
        ParseResult pr = parse();
        TypeInfo x = pr.findType("a.b.X");
        TypeInfo innerBefore = ctmGet("a.b.X.Inner");
        assertNotNull(innerBefore);
        CompiledTypesManager ctm = javaInspector.compiledTypesManager();

        // exactly as the parse does it: newInfoMap over the types to rewire, rewireAll, then register every type the
        // map built -- setRewiredType registers the one type it is given, and the map is what knows the full list
        InfoMap infoMap = javaInspector.runtime().newInfoMap(java.util.Set.of(x));
        java.util.Set<TypeInfo> rewiredSet = infoMap.rewireAll();
        assertEquals(1, rewiredSet.size(), "rewireAll returns the primary types");
        TypeInfo rewired = rewiredSet.iterator().next();
        assertNotSame(x, rewired);
        assertEquals(x.fullyQualifiedName(), rewired.fullyQualifiedName());
        assertTrue(infoMap.rewiredTypes().size() > rewiredSet.size(),
                "rewiredTypes() reports more than the primary types: " + infoMap.rewiredTypes());

        infoMap.rewiredTypes().forEach(ctm::setRewiredType);

        assertSame(rewired, ctm.get("a.b.X", sourceSet));
        assertNotSame(x, ctm.get("a.b.X", sourceSet));
        TypeInfo innerAfter = ctmGet("a.b.X.Inner");
        assertNotSame(innerBefore, innerAfter, "the subtype is rewired too, and re-registered");
        assertSame(rewired, innerAfter.compilationUnitOrEnclosingType().getRight());
    }

    private TypeInfo ctmGet(String fqn) {
        return javaInspector.compiledTypesManager().get(fqn, sourceSet);
    }
}
