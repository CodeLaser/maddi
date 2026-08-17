package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code reloadSources} answers one question — which types' source files changed since we parsed them? — by
 * comparing fingerprints. It parses nothing and invalidates nothing; the caller turns its answer into an
 * {@code Invalidated} (see {@code RunRewireTests}). Mirrors the in-house {@code TestInvalidate.testReload}.
 */
public class TestReloadSources {

    private static final String ISOURCE_FQN = "a.b.ISource";
    private static final String SOURCE_FQN = "a.b.Source";

    @Language("java")
    private static final String ISOURCE = """
            package a.b;
            public interface ISource { String name(); }
            """;

    // same type, reformatted: a different fingerprint, and nothing else
    @Language("java")
    private static final String ISOURCE_CHANGED = """
            package a.b;
            public interface ISource {
                String name();
            }
            """;

    @Language("java")
    private static final String SOURCE = """
            package a.b;
            public record Source(String name) implements ISource {
            }
            """;

    @Language("java")
    private static final String EXTRA = """
            package a.b;
            public class Extra { }
            """;

    private JavaInspector javaInspector;
    private SourceSet sourceSet;

    @BeforeEach
    public void before() throws IOException {
        javaInspector = new JavaInspectorImpl(true, false);
        sourceSet = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "1").setUri(URI.create("file:/")).build();
        javaInspector.initialize(inputConfiguration());
    }

    private InputConfiguration inputConfiguration() {
        return new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
    }

    /** reloadSources keys its in-memory sources the way the in-house inspector does: "test-protocol:a.b.X". */
    private Map<String, String> byUriString(Map<String, String> sourcesByFqn) {
        return sourcesByFqn.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                e -> JavaInspectorImpl.TEST_PROTOCOL_PREFIX + e.getKey(), Map.Entry::getValue));
    }

    private ParseResult parse(Map<String, String> sourcesByFqn) {
        return javaInspector.parseMultiSourceSet(Map.of(sourceSet, sourcesByFqn),
                JavaInspectorImpl.DETAILED_SOURCES).parseResult();
    }

    private static final Map<String, String> INITIAL = Map.of(ISOURCE_FQN, ISOURCE, SOURCE_FQN, SOURCE);

    @DisplayName("nothing touched: no type reported as changed")
    @Test
    public void testUnchanged() throws IOException {
        assertEquals(2, parse(INITIAL).primaryTypes().size());
        JavaInspector.ReloadResult rr = javaInspector.reloadSources(inputConfiguration(), byUriString(INITIAL));
        assertEquals(0, rr.problems().size(), rr.problems().toString());
        assertTrue(rr.sourceHasChanged().isEmpty(), "expected nothing changed, got " + rr.sourceHasChanged());
    }

    @DisplayName("one file reformatted: exactly its type is reported")
    @Test
    public void testOneChanged() throws IOException {
        ParseResult pr = parse(INITIAL);
        TypeInfo iSource = pr.findType(ISOURCE_FQN);
        assertNotNull(iSource);

        Map<String, String> changedSources = Map.of(ISOURCE_FQN, ISOURCE_CHANGED, SOURCE_FQN, SOURCE);
        JavaInspector.ReloadResult rr = javaInspector.reloadSources(inputConfiguration(),
                byUriString(changedSources));
        assertEquals(0, rr.problems().size(), rr.problems().toString());
        assertEquals(1, rr.sourceHasChanged().size(), "expected only ISource, got " + rr.sourceHasChanged());
        // the very object the caller will mark INVALID
        assertSame(iSource, rr.sourceHasChanged().iterator().next());
    }

    @DisplayName("a new file is registered, not reported as changed")
    @Test
    public void testNewFile() throws IOException {
        parse(INITIAL);
        assertEquals(2, javaInspector.sourceFiles().size());

        Map<String, String> withExtra = new HashMap<>(INITIAL);
        withExtra.put("a.b.Extra", EXTRA);
        JavaInspector.ReloadResult rr = javaInspector.reloadSources(inputConfiguration(), byUriString(withExtra));
        // new files carry no types yet: nothing to invalidate, so nothing to report
        assertTrue(rr.sourceHasChanged().isEmpty(), "a new file has no types to report: " + rr.sourceHasChanged());
        assertEquals(3, javaInspector.sourceFiles().size(), "the new file must be registered for the next parse");
    }

    @DisplayName("a removed file drops out of sourceFiles")
    @Test
    public void testRemovedFile() throws IOException {
        parse(INITIAL);
        assertEquals(2, javaInspector.sourceFiles().size());

        JavaInspector.ReloadResult rr = javaInspector.reloadSources(inputConfiguration(),
                byUriString(Map.of(SOURCE_FQN, SOURCE)));
        assertTrue(rr.sourceHasChanged().isEmpty(), rr.sourceHasChanged().toString());
        assertEquals(1, javaInspector.sourceFiles().size(), "the removed file must be gone");
        assertEquals("a/b/Source.java", javaInspector.sourceFiles().iterator().next().path());
    }

    @DisplayName("reloadSources without fingerprints is refused rather than silently wrong")
    @Test
    public void testRequiresFingerPrints() throws IOException {
        JavaInspector noFingerPrints = new JavaInspectorImpl(); // computeFingerPrints = false
        noFingerPrints.initialize(inputConfiguration());
        assertThrows(UnsupportedOperationException.class,
                () -> noFingerPrints.reloadSources(inputConfiguration(), byUriString(INITIAL)));
    }
}
