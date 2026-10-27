/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.java.openjdk.method;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A METHOD type parameter used to lose its declaration source, and get a different bound, when the compilation
 * unit that CALLS the generic method was scanned before the one that declares it.
 * <p>
 * The companion of {@code TestParameterInfoSource} and {@code TestReturnTypeSource}, for the third thing
 * {@code ScanCompilationUnit}'s already-known-method branch has to back-fill.
 * <p>
 * Two symptoms, and the second is the one that was not merely a refactoring inconvenience:
 * <ol>
 *     <li>{@code tp.source()} was null, so nothing could locate {@code <Score_ ...>} in the text — rename
 *     rewrote every USE of it and left the declaration behind, silently;</li>
 *     <li>the bound was {@code ? extends Score<Score_>} rather than {@code Score<Score_>}, the symbol path's
 *     {@code withWildcard(wildcardExtends())} form. The CST of one source file depended on the order its
 *     compilation units were scanned in.</li>
 * </ol>
 * Fixed by deferring the commit to the declaration, as {@code deferParameterCommit} already did for the
 * parameters one loop below. The two caller-first drivers are now identical to the declaration-first ones,
 * which is the property that matters.
 */
public class TestMethodTypeParameterSource extends CommonTest {

    @Language("java")
    private static final String SCORE = """
            package a;
            interface Score<S extends Score<S>> {
            }
            """;

    @Language("java")
    private static final String INNER = """
            package a;
            class InnerScore<S extends Score<S>> {
            }
            """;

    @Language("java")
    private static final String SCOPE = """
            package a;
            abstract class AbstractStepScope<Solution_> {
                protected Object score;
                @SuppressWarnings("unchecked")
                public <Score_ extends Score<Score_>> InnerScore<Score_> getScore() {
                    return (InnerScore<Score_>) score;
                }
            }
            """;

    /** The caller. Resolving {@code s.getScore()} creates the method from its symbol. */
    @Language("java")
    private static final String USER = """
            package a;
            class User {
                void use(AbstractStepScope<Object> s) {
                    InnerScore<?> x = s.getScore();
                }
            }
            """;

    private TypeInfo scope(boolean callerFirst) {
        Map<String, String> sources = new LinkedHashMap<>();
        if (callerFirst) sources.put("a.User", USER);
        sources.put("a.Score", SCORE);
        sources.put("a.InnerScore", INNER);
        sources.put("a.AbstractStepScope", SCOPE);
        if (!callerFirst) sources.put("a.User", USER);

        return scan(false, sources).primaryTypes().stream()
                .filter(t -> "a.AbstractStepScope".equals(t.fullyQualifiedName()))
                .findFirst().orElseThrow();
    }

    private static TypeParameter methodTypeParameter(TypeInfo scope) {
        MethodInfo getScore = scope.findUniqueMethod("getScore", 0);
        assertEquals(1, getScore.typeParameters().size());
        return getScore.typeParameters().getFirst();
    }

    @DisplayName("declaration scanned first: the type parameter is the one the parser built")
    @Test
    public void testDeclarationFirst() {
        TypeParameter tp = methodTypeParameter(scope(false));
        assertNotNull(tp.source(), "the declaration's own source");
        assertEquals("5-13:5-40", tp.source().compact2());
        assertEquals("[Type a.Score<Score_ extends a.Score<Score_>>]", tp.typeBounds().toString());
    }

    @DisplayName("caller scanned first: the declaration still wins -- same source, same bound")
    @Test
    public void testCallerFirst() {
        TypeParameter tp = methodTypeParameter(scope(true));
        // Identical to testDeclarationFirst, which is the whole point: the CST of a source file may not depend
        // on the order its compilation units were scanned in. Before the fix this returned a null source and
        // the bound `? extends a.Score<Score_>`, the symbol path's wildcard-widened form.
        assertNotNull(tp.source(), "the declaration's own source, whichever order the units were scanned in");
        assertEquals("5-13:5-40", tp.source().compact2());
        assertEquals("[Type a.Score<Score_ extends a.Score<Score_>>]", tp.typeBounds().toString());
    }

    /**
     * The contrast that localises the bug. {@code AbstractStepScope<Solution_>} is a CLASS type parameter, in the
     * same file, reached by the same caller-first scan — and it keeps its source either way, because
     * {@code ScanCompilationUnit} builds fresh type parameters and calls
     * {@code TypeInfo.Builder.addOrSetTypeParameter}, which REPLACES what the symbol scanner left. The method
     * builder offers only {@code addTypeParameter}: no replace, so the symbol-built instance stays.
     * <p>
     * Not a claim that class type parameters are safe in general — on timefold-solver 285 of 3469 of them also
     * come back without a source, by some route this driver does not reach. It is a claim about THIS shape, and
     * it is what shows the method-side loss is the missing replace rather than something about the file.
     */
    @DisplayName("the CLASS type parameter in the same file keeps its source, declaration first")
    @Test
    public void testClassTypeParameterDeclarationFirst() {
        assertClassTypeParameterHasSource(scope(false));
    }

    @DisplayName("the CLASS type parameter in the same file keeps its source, CALLER first -- unlike the method's")
    @Test
    public void testClassTypeParameterCallerFirst() {
        assertClassTypeParameterHasSource(scope(true));
    }

    // one scan per test: the harness registers its types in a shared InfoByFqn, so a second scan in the same
    // instance finds them already there
    private static void assertClassTypeParameterHasSource(TypeInfo scope) {
        assertEquals(1, scope.typeParameters().size());
        TypeParameter solution = scope.typeParameters().getFirst();
        assertNotNull(solution.source());
        assertEquals("2-34:2-42", solution.source().compact2());
    }
}
