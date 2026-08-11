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

package org.e2immu.language.java.openjdk.other;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <b>{@code Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory f)} — the JSpecify array
 * form, where the type-use annotation sits BETWEEN the element type and the brackets (JLS 9.7.4).</b>
 *
 * <h2>⚠ WHAT THIS CLASS ESTABLISHES IS A NEGATIVE, AND THAT IS THE POINT</h2>
 * On timefold-solver, {@code ai.timefold.solver.core.api.score.stream.ConstraintProvider} commits FROM
 * SOURCE as {@code defineConstraints()} — <b>with no parameters at all</b>, while the file declares one.
 * Measured 2026-08-11 on {@code core/main} <i>alone</i>: zero parse errors, the source set's own
 * {@code target/classes} redirected away, and the parameter is still gone. Downstream it costs <b>100
 * dropped compilation units</b>: every unit writing a {@code ConstraintProvider} lambda reaches the type
 * through a class-file load of the real one-parameter method, which cannot be added to a committed type.
 * <p>
 * Four shapes were tried here and <b>none of them reproduces it</b>: the annotated array in a source-local
 * annotation, timefold's declaration verbatim, the annotation arriving from a JAR instead of a source file,
 * and a lambda implementing the interface in the same source set. So the trigger is something else in that
 * source set, and these tests stand as the shapes not to try again. The next step is a bisection over
 * {@code core/main}'s compilation units; see {@code HANDOFF-SLOWTEST-FAILURES.md} §E.
 */
public class TestAnnotatedArrayReturnType extends CommonTest {

    @Language("java")
    private static final String ANNOTATION = """
            package a.b;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            @Target({ElementType.TYPE_USE})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface NonNull {}
            """;

    @Language("java")
    private static final String PROVIDER = """
            package a.b;
            public interface Provider {
                String @NonNull [] define(@NonNull Factory factory);
                String plain(Factory factory);
                String[] alsoPlain(Factory factory);
            }
            """;

    @Language("java")
    private static final String FACTORY = """
            package a.b;
            public interface Factory {
                String name();
            }
            """;

    @DisplayName("an annotation between the element type and the brackets does not eat the parameter list")
    @Test
    public void annotatedArrayReturnTypeKeepsItsParameters() {
        Map<String, TypeInfo> types = scan(false, "a.b.NonNull", ANNOTATION, "a.b.Factory", FACTORY,
                "a.b.Provider", PROVIDER);
        TypeInfo provider = types.get("a.b.Provider");

        MethodInfo plain = provider.findUniqueMethod("plain", 1);
        assertEquals(1, plain.parameters().size());
        MethodInfo alsoPlain = provider.findUniqueMethod("alsoPlain", 1);
        assertEquals(1, alsoPlain.parameters().size());

        MethodInfo define = provider.methods().stream().filter(m -> "define".equals(m.name())).findFirst()
                .orElseThrow();
        assertEquals(1, define.parameters().size(), "THE POINT: the parameter is still there -- "
                                                    + define.descriptor());
        assertEquals("a.b.Factory", define.parameters().getFirst().parameterizedType().fullyQualifiedName());
        assertEquals(1, define.returnType().arrays(), define.returnType().toString());
    }

    @Language("java")
    private static final String JSPECIFY = """
            package org.jspecify.annotations;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            @Target({ElementType.TYPE_USE})
            @Retention(RetentionPolicy.CLASS)
            public @interface NonNull {}
            """;

    @Language("java")
    private static final String TF_CONSTRAINT = """
            package ai.timefold.solver.core.api.score.stream;
            public interface Constraint {
            }
            """;

    @Language("java")
    private static final String TF_FACTORY = """
            package ai.timefold.solver.core.api.score.stream;
            public interface ConstraintFactory {
                Object forEach(Class<?> sourceClass);
            }
            """;

    /** The declaration as timefold writes it, verbatim, imports and javadoc included. */
    @Language("java")
    private static final String TF_PROVIDER = """
            package ai.timefold.solver.core.api.score.stream;

            import org.jspecify.annotations.NonNull;

            /**
             * Used by Constraint Streams' score calculation.
             */
            public interface ConstraintProvider {

                /**
                 * This method is called once to create the constraints.
                 * To create a {@link Constraint}, start with {@link ConstraintFactory#forEach(Class)}.
                 *
                 * @return an array of all {@link Constraint constraints} that could apply.
                 */
                Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory);

            }
            """;

    /** As above, but the type-use annotation arrives from a JAR rather than from a source file beside it. */
    @Language("java")
    private static final String PROVIDER_JAR_ANNOTATION = """
            package a.b;
            import org.jetbrains.annotations.NotNull;
            public interface JarAnnotated {
                String @NotNull [] define(@NotNull Factory factory);
            }
            """;

    @DisplayName("a type-use annotation loaded from bytecode does not eat the parameter list either")
    @Test
    public void annotationFromAJar() {
        Map<String, TypeInfo> types = scan(false, "a.b.Factory", FACTORY,
                "a.b.JarAnnotated", PROVIDER_JAR_ANNOTATION);
        MethodInfo define = types.get("a.b.JarAnnotated").methods().stream()
                .filter(m -> "define".equals(m.name())).findFirst().orElseThrow();
        assertEquals(1, define.parameters().size(), define.descriptor());
    }

    /** A lambda implementing it, in the same source set: this is how every dropped unit reached the type. */
    @Language("java")
    private static final String TF_USER = """
            package ai.timefold.solver.core.api.score.stream;
            public class User {
                public ConstraintProvider provider() {
                    return constraintFactory -> new Constraint[0];
                }
            }
            """;

    @DisplayName("timefold's ConstraintProvider, verbatim: one method, one parameter")
    @Test
    public void theCorpusDeclaration() {
        Map<String, TypeInfo> types = scan(false,
                "org.jspecify.annotations.NonNull", JSPECIFY,
                "ai.timefold.solver.core.api.score.stream.Constraint", TF_CONSTRAINT,
                "ai.timefold.solver.core.api.score.stream.ConstraintFactory", TF_FACTORY,
                "ai.timefold.solver.core.api.score.stream.User", TF_USER,
                "ai.timefold.solver.core.api.score.stream.ConstraintProvider", TF_PROVIDER);
        TypeInfo provider = types.get("ai.timefold.solver.core.api.score.stream.ConstraintProvider");
        MethodInfo define = provider.methods().stream().filter(m -> "defineConstraints".equals(m.name()))
                .findFirst().orElseThrow();
        assertEquals(1, define.parameters().size(), define.descriptor());
    }
}
