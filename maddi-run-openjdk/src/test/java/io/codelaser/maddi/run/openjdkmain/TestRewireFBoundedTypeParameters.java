package io.codelaser.maddi.run.openjdkmain;

import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rewiring must terminate on <b>mutually F-bounded</b> type parameters.
 * <p>
 * {@code TypeParameterImpl.rewire} enters a parameter into the map of already-rewired parameters
 * <em>before</em> it rewires its bounds, which is exactly what makes a self-referential bound terminate —
 * the bound leads back to an entry that is already there. But {@code ParameterizedTypeImpl.rewire} reached
 * a nested parameter through {@code Element#rewire(infoMap)}, the one-argument form, which starts an
 * <b>empty</b> map. Each nested call therefore threw the memo away.
 * <p>
 * A single self-bound ({@code X extends C<X>}) survives that, because the one entry it needs is made by the
 * call that is already running. Two parameters bounded through each other do not: A's bounds reach B, B
 * starts fresh, B's bounds reach A, A starts fresh, and the stack ends. Found on timefold-solver, whose
 * config and score hierarchies are built from this shape ({@code PhaseConfig<Config_ extends
 * PhaseConfig<Config_>>}).
 * <p>
 * The fixture carries both shapes so the test says which one was broken: {@code Single} is the case that
 * always worked, {@code A}/{@code B} the case that did not. Same hazard the {@code visited} set of
 * {@link TypeParameter#typesReferenced} guards against, on a different traversal.
 */
public class TestRewireFBoundedTypeParameters {

    @TempDir
    Path root;

    private static final String T_FQN = "a.T";

    @Language("java")
    private static final String SOURCE = """
            package a;
            public class T {
                // the shape that always worked: one parameter bounded by a type over itself
                static class Single<X extends Comparable<X>> {
                    X x;
                }
                // and the shape that did not: two parameters, each bounded through the other
                interface Holder<Q> {
                }
                static class Pair<A extends Holder<B>, B extends Holder<A>> {
                    A a;
                    B b;
                }
            }
            """;

    private JavaInspector javaInspector;
    private TypeInfo type;

    @BeforeEach
    public void parse() throws IOException {
        Path mainSrc = Files.createDirectories(root.resolve("main-src/a"));
        Files.writeString(mainSrc.resolve("T.java"), SOURCE);

        var main = new SourceSetImpl.Builder().setName("main")
                .setSourceDirectories(List.of(root.resolve("main-src")))
                .setUri(root.resolve("main-src").toUri()).build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder().addSourceSets(main)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES).build();

        javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(inputConfiguration);
        ParseResult pr = javaInspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        type = pr.findType(T_FQN);
    }

    @DisplayName("rewiring terminates on mutually F-bounded type parameters, and keeps the bounds")
    @Test
    public void testMutuallyFBounded() {
        TypeInfo pair = type.subTypes().stream().filter(t -> "Pair".equals(t.simpleName()))
                .findFirst().orElseThrow();
        // the fixture really is mutually bounded, or the test would pass on the shape that always worked
        assertEquals(2, pair.typeParameters().size());
        assertTrue(pair.typeParameters().getFirst().toStringWithTypeBounds().contains("B"),
                "A is bounded through B: " + pair.typeParameters().getFirst().toStringWithTypeBounds());
        assertTrue(pair.typeParameters().get(1).toStringWithTypeBounds().contains("A"),
                "B is bounded through A: " + pair.typeParameters().get(1).toStringWithTypeBounds());

        InfoMap rewire = javaInspector.runtime().newInfoMap(Set.of(type));
        // used to be a StackOverflowError: every nested rewire began with an empty map of rewired parameters
        assertDoesNotThrow(rewire::rewireAll, "rewiring mutually F-bounded parameters must terminate");

        TypeInfo rewiredPair = assertDoesNotThrow(() -> rewire.typeInfo(pair));
        assertNotSame(pair, rewiredPair, "the rewire produced a fresh object");
        assertEquals(pair.fullyQualifiedName(), rewiredPair.fullyQualifiedName(),
                "BASIC RULE OF REWIRING: same identity, new object");

        // and the bounds survived the traversal rather than being dropped to make it terminate
        assertEquals(2, rewiredPair.typeParameters().size());
        for (int i = 0; i < 2; i++) {
            assertEquals(pair.typeParameters().get(i).toStringWithTypeBounds(),
                    rewiredPair.typeParameters().get(i).toStringWithTypeBounds(),
                    "bound " + i + " must be carried over, not discarded");
        }
    }
}
