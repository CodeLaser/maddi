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
package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An unqualified call to an inherited method, in a type whose superclass is not on the class path, resolves to
 * nothing: an erroneous superclass cuts the chain to {@code Object}, and javac reports "cannot find symbol" even for
 * {@code toString()}. Attribution leaves an error symbol on the identifier, a ClassSymbol named after the method and
 * owned by the calling type. The qualified call site has long thrown an UnresolvedSymbolException for the same
 * miss; the unqualified one threw a plain UnsupportedOperationException, which refuses the WHOLE ParseResult.
 * <p>
 * Measured on guava's main sources parsed without their annotation and failureaccess jars:
 * {@code AbstractFutureState} extends {@code InternalFutureFailureAccess} and calls {@code toString()} unqualified.
 * One unit, one parse refused.
 * <p>
 * ⚠ The production stop policy ({@code -XDshould-stop.ifError=FLOW}) matters here: maddi-java-openjdk's
 * CommonTest leaves such a unit unattributed, so its identifier carries no symbol at all and the error symbol
 * never reaches the scanner. That is why this test lives in maddi-inspection-openjdk.
 * <p>
 * ⚠ javac 26 takes a different path through the same source: the simple name of a failed single-type import
 * resolves to an owner-less error symbol, so the unit is already dropped at its {@code extends} clause, before
 * {@code continueType} sets a parent class. The abandoned type is still committed at the end of the source set,
 * and used to be refused there for its null parent class -- a parse ERROR again. ClassSymbolScanner.loadType now
 * finishes the parent from the symbol. Green on javac 27 alone proves nothing about that path; it was found on
 * laser1 (JDK 26.0.1).
 */
public class TestUnqualifiedCallOnUnresolvedSupertype {

    private JavaInspector javaInspector;
    private SourceSet sourceSet;

    @BeforeEach
    public void before() throws IOException {
        javaInspector = new JavaInspectorImpl();
        SourceSet javaBase = SourceSetImpl.javaBase();
        sourceSet = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "1").setUri(URI.create("file:/"))
                .setDependencies(List.of(javaBase)).build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath("jmod:java.base")
                .build();
        javaInspector.initialize(inputConfiguration);
    }

    @Language("java")
    private static final String HEALTHY = """
            package a;
            public class Healthy {
                int twice(int i) {
                    return 2 * i;
                }
            }
            """;

    // the guava shape: an explicit constructor, the missing superclass imported, an inherited method called bare
    @Language("java")
    private static final String ORPHAN = """
            package a;
            import a.gone.Missing;
            public abstract class Orphan extends Missing {
                Orphan() {}
                String describe() {
                    String s = toString();
                    return s.toLowerCase();
                }
            }
            """;

    // no explicit constructor: the implicit super() in the default one is the first unresolved call
    @Language("java")
    private static final String DEFAULT_CONSTRUCTOR = """
            package a;
            public abstract class Orphan extends a.gone.Missing {
                int h() {
                    return hashCode();
                }
            }
            """;

    @Test
    public void unqualifiedInheritedCallDropsOnlyItsUnit() {
        assertDroppedTolerably(ORPHAN);
    }

    @Test
    public void implicitSuperCallDropsOnlyItsUnit() {
        assertDroppedTolerably(DEFAULT_CONSTRUCTOR);
    }

    private void assertDroppedTolerably(String orphan) {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder().setFailFast(false).build();
        Summary summary = javaInspector.parseMultiSourceSet(Map.of(sourceSet,
                Map.of("a.Healthy", HEALTHY, "a.Orphan", orphan)), options);
        assertTrue(summary.parseExceptions().isEmpty(), summary.parseExceptions().toString());
        assertTrue(summary.types().stream().noneMatch(t -> "a.Orphan".equals(t.fullyQualifiedName())));
        assertTrue(summary.parseWarnings().stream().anyMatch(w -> "compilation unit".equals(w.where())
                                                                  && w.uri().toString().endsWith("a/Orphan.java")),
                summary.parseWarnings().toString());
        assertTrue(summary.types().stream().anyMatch(t -> "a.Healthy".equals(t.fullyQualifiedName())));
    }
}
