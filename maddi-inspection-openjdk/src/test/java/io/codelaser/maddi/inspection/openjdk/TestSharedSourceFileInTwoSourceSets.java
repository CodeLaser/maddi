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
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ONE SOURCE FILE COMPILED INTO TWO SOURCE SETS THAT DO NOT DEPEND ON EACH OTHER. Each source set has its own copy of
 * every type in the file, and javac compiles each copy against itself only. A use of a type of the file must
 * therefore resolve to the copy of the SAME source set.
 *
 * <h2>Where it was found</h2>
 * hibernate-orm d6c1a87f14 compiles {@code shared/classification/src/main/java} into two Gradle projects:
 * <pre>
 * local-build-plugins/build.gradle:                 sourceSets.main.java.srcDir("../shared/classification/src/main/java")
 * tooling/hibernate-dialect-provider/...gradle:     main.java.srcDir rootProject.file("shared/classification/src/main/java")
 * </pre>
 * Its inputConfiguration.json has two source sets, {@code local-build-plugins/main} and
 * {@code tooling/hibernate-dialect-provider/main}, each with that directory among its sourceDirectories, and neither
 * lists the other as a dependency. The file is {@code ClassificationMetadataResolver.java}: a method uses a private
 * nested class that is declared FURTHER DOWN the same file.
 *
 * <h2>What maddi does</h2>
 * The first source set's copy is right. In the second source set's copy, {@code new MissingMetadataException( uri )}
 * resolves to the FIRST source set's {@code MissingMetadataException}. Apparently the second scan meets the use
 * before it has registered its own copy of the nested class, asks the registry ({@code InfoByFqn.getType}) for the
 * name, and gets the only copy registered so far -- which belongs to a source set the second one does not depend on.
 * ({@code getType} returns a single registered type without looking at the source set; with several, an unreachable
 * one still wins when it is the only candidate: {@code computeDistance} gives it {@code Integer.MAX_VALUE}, and the
 * choice sorts rather than excludes.)
 * <p>
 * The control below declares the nested class BEFORE its use, and then both copies are right.
 *
 * <h2>What it costs downstream</h2>
 * The CodeLaser dead-code analysis, on hibernate-orm, found nothing creating the second copy of
 * {@code MissingMetadataException} and reported its constructor and its field {@code uri} dead, while the method
 * creating it -- correctly -- stayed alive; the removal then refused the whole run.
 */
public class TestSharedSourceFileInTwoSourceSets {

    private static final String FQN = "shared.ClassificationMetadataResolver";
    private static final String FIRST = TEST_PROTOCOL;          // stands for local-build-plugins/main
    private static final String SECOND = TEST_PROTOCOL + "B";   // stands for tooling/hibernate-dialect-provider/main

    /** ClassificationMetadataResolver.java as hibernate-orm writes it, reduced: the nested class comes LAST. */
    @Language("java")
    private static final String NESTED_CLASS_LAST = """
            package shared;

            import java.io.IOException;
            import java.net.HttpURLConnection;
            import java.net.URI;

            public final class ClassificationMetadataResolver {

                private static void checkStatus(HttpURLConnection connection, URI uri) throws IOException {
                    if ( connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND ) {
                        throw new MissingMetadataException( uri );
                    }
                }

                private static final class MissingMetadataException extends IOException {
                    private final URI uri;

                    private MissingMetadataException(URI uri) {
                        super( "HTTP 404 resolving " + uri );
                        this.uri = uri;
                    }
                }
            }
            """;

    /** The same file with the nested class declared BEFORE the method that uses it. */
    @Language("java")
    private static final String NESTED_CLASS_FIRST = """
            package shared;

            import java.io.IOException;
            import java.net.HttpURLConnection;
            import java.net.URI;

            public final class ClassificationMetadataResolver {

                private static final class MissingMetadataException extends IOException {
                    private final URI uri;

                    private MissingMetadataException(URI uri) {
                        super( "HTTP 404 resolving " + uri );
                        this.uri = uri;
                    }
                }

                private static void checkStatus(HttpURLConnection connection, URI uri) throws IOException {
                    if ( connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND ) {
                        throw new MissingMetadataException( uri );
                    }
                }
            }
            """;

    @DisplayName("a type used before its declaration resolves to the copy of the source set that uses it")
    @Test
    public void typeUsedBeforeItsDeclaration() throws Exception {
        assertEachCopyCreatesItsOwn(parse(NESTED_CLASS_LAST));
    }

    @DisplayName("control: a type declared before its use resolves to the copy of the source set that uses it")
    @Test
    public void typeDeclaredBeforeItsUse() throws Exception {
        assertEachCopyCreatesItsOwn(parse(NESTED_CLASS_FIRST));
    }

    /**
     * For each copy of the file's class: the source sets of the MissingMetadataException that its checkStatus
     * creates. Each copy must create its own.
     */
    private static void assertEachCopyCreatesItsOwn(Summary summary) {
        assertFalse(summary.haveErrors(), "the parse succeeds: " + summary.parseExceptions());
        List<TypeInfo> copies = summary.parseResult().primaryTypes().stream()
                .filter(t -> FQN.equals(t.fullyQualifiedName()))
                .toList();
        assertEquals(List.of(FIRST, SECOND), copies.stream().map(t -> sourceSet(t)).sorted().toList(),
                "one copy of " + FQN + " per source set");
        for (TypeInfo copy : copies) {
            MethodInfo checkStatus = copy.methods().stream().filter(m -> m.name().equals("checkStatus")).findFirst()
                    .orElseThrow();
            List<String> created = new ArrayList<>();
            checkStatus.methodBody().visit(e -> {
                if (e instanceof ConstructorCall cc && cc.constructor() != null) {
                    created.add(sourceSet(cc.constructor().typeInfo()));
                }
                return true;
            });
            assertEquals(List.of(sourceSet(copy)), created,
                    "the copy of checkStatus in source set '" + sourceSet(copy) + "' does `new"
                    + " MissingMetadataException( uri )`; the list is the source set of the MissingMetadataException"
                    + " that call resolved to, and it must be the same source set");
            assertTrue(copy.subTypes().stream().allMatch(sub -> sourceSet(sub).equals(sourceSet(copy))),
                    "the nested class of each copy belongs to that copy's source set");
        }
    }

    private static String sourceSet(TypeInfo type) {
        SourceSet sourceSet = type.primaryType().compilationUnit().sourceSet();
        return sourceSet == null ? "(no source set)" : sourceSet.name();
    }

    /** Two source sets holding the same file, each depending on java.base only: not on each other. */
    private static Summary parse(String file) throws Exception {
        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet first = new SourceSetImpl.Builder().setName(FIRST)
                .setUri(URI.create("file:/local-build-plugins/"))
                .setDependencies(List.of(javaBase)).build();
        SourceSet second = new SourceSetImpl.Builder().setName(SECOND)
                .setUri(URI.create("file:/tooling/hibernate-dialect-provider/"))
                .setDependencies(List.of(javaBase)).build();
        InputConfiguration ic = new InputConfigurationImpl.Builder()
                .addClassPathParts(javaBase)
                .addSourceSets(first, second)
                .build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(ic);
        Map<SourceSet, Map<String, String>> sources = new LinkedHashMap<>();
        sources.put(first, Map.of(FQN, file));
        sources.put(second, Map.of(FQN, file));
        return javaInspector.parseMultiSourceSet(sources, JavaInspectorImpl.DETAILED_SOURCES);
    }
}
