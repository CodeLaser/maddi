package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.resource.InfoByFqn;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The safety contract of dropping the javac AST (DESIGN-drop-javac-ast.md): once the lazy loader is
 * disabled, a {@code getOrLoad} miss must be <b>surfaced</b> — counted and logged, or thrown under
 * strict mode — never silently returned as {@code null}, which would corrupt the analysis with missing
 * types. While the loader is live, a genuine miss (type not on the classpath) stays benign.
 */
public class TestDropAstFailLoud {

    private static CompiledTypesManagerImpl newCtm(java.util.function.Function<String, TypeInfo> loader) {
        SourceSet javaBase = new SourceSetImpl.Builder().setName("java.base").setUri(URI.create("file:/")).build();
        CompiledTypesManagerImpl ctm = new CompiledTypesManagerImpl(javaBase, new InfoByFqn());
        ctm.setLazyLoader(loader);
        return ctm;
    }

    @Test
    public void missIsBenignWhileLoaderLive() {
        AtomicInteger calls = new AtomicInteger();
        CompiledTypesManagerImpl ctm = newCtm(fqn -> { calls.incrementAndGet(); return null; });

        assertNull(ctm.getOrLoad("a.b.Absent", null)); // not on the classpath: normal, benign
        assertEquals(1, calls.get());
        assertEquals(0, ctm.distinctMissesAfterDrop());
        assertEquals(0, ctm.totalMissesAfterDrop());
    }

    @Test
    public void missIsSurfacedAndDedupedAfterDrop() {
        CompiledTypesManagerImpl ctm = newCtm(fqn -> null); // loader can no longer serve anything
        ctm.setLazyLoaderDisabled(true);

        assertNull(ctm.getOrLoad("a.b.Gone", null));
        assertNull(ctm.getOrLoad("a.b.Gone", null));   // repeat: counts, but stays one distinct FQN
        assertNull(ctm.getOrLoad("a.b.Other", null));

        assertEquals(2, ctm.distinctMissesAfterDrop());
        assertEquals(3, ctm.totalMissesAfterDrop());
        assertTrue(ctm.sampleMissesAfterDrop(10).contains("a.b.Gone"));
    }

    @Test
    public void strictModeThrowsOnMissAfterDrop() {
        CompiledTypesManagerImpl ctm = newCtm(fqn -> null);
        ctm.setStrictOnDisabledMiss(true);
        ctm.setLazyLoaderDisabled(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ctm.getOrLoad("a.b.Gone", null));
        assertTrue(ex.getMessage().contains("a.b.Gone"));
    }

    @Test
    public void reEnablingClearsSurfacing() {
        CompiledTypesManagerImpl ctm = newCtm(fqn -> null);
        ctm.setLazyLoaderDisabled(true);
        assertNull(ctm.getOrLoad("a.b.Gone", null));
        assertEquals(1, ctm.distinctMissesAfterDrop());

        ctm.setLazyLoaderDisabled(false); // a fresh scan revived the task
        assertNull(ctm.getOrLoad("a.b.StillAbsent", null)); // benign again, not surfaced
        assertEquals(1, ctm.distinctMissesAfterDrop()); // unchanged
    }
}
