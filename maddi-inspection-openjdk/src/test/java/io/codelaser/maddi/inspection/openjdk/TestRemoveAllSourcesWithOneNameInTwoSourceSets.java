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

package io.codelaser.maddi.inspection.openjdk;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import io.codelaser.maddi.inspection.resource.InfoByFqn;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;

/**
 * {@link InfoByFqn#removeAllSources()} forgets every source type, also a name that two source sets both define.
 *
 * <h2>Where it was found</h2>
 * refactor.codelaser.io, dropping the project hibernate-orm--main (2026-11-13):
 * <pre>
 * WARN ProjectServiceImpl - Releasing the old model's inspector of hibernate-orm--main failed
 * java.lang.UnsupportedOperationException
 *     at java.util.ImmutableCollections$AbstractImmutableCollection.removeIf
 *     at io.codelaser.maddi.inspection.resource.InfoByFqn.lambda$removeAllSources$0(InfoByFqn.java:83)
 *     at io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl.invalidateAllSources(JavaInspectorImpl.java:221)
 * </pre>
 * A name defined in several source sets is registered with a list built by {@code List.of} or
 * {@code Stream.toList()}, and those lists cannot be changed: {@code removeIf} on them throws. hibernate-orm has 60
 * source sets and many class names defined in more than one. The exception stops
 * {@code JavaInspectorImpl.invalidateAllSources()} at its first line, so the javac file managers and the shared jar
 * archives it should close stay open.
 */
public class TestRemoveAllSourcesWithOneNameInTwoSourceSets {

    @DisplayName("a name defined in two source sets is forgotten; a library type with another name stays")
    @Test
    public void bothSourceCopiesGoTheLibraryTypeStays() {
        Runtime runtime = new RuntimeImpl();
        SourceSet main = sourceSet("core/main");
        SourceSet test = sourceSet("core/test");
        SourceSet jar = new SourceSetImpl.Builder().setName("lib-1.0.jar").setUri(URI.create("file:/lib-1.0.jar"))
                .setLibrary(true).setExternalLibrary(true).build();
        InfoByFqn registry = new InfoByFqn();

        TypeInfo inMain = type(runtime, main, "a", "Helper");
        registry.put(inMain.fullyQualifiedName(), inMain, main);
        TypeInfo inTest = type(runtime, test, "a", "Helper");
        registry.put(inTest.fullyQualifiedName(), inTest, test);
        TypeInfo library = type(runtime, jar, "lib", "Thing");
        registry.put(library.fullyQualifiedName(), library, main);
        assertEquals(List.of(inMain, inTest), registry.allTypes("a.Helper"),
                "set-up: both source sets define a.Helper, so the registry holds two copies of that name");

        assertDoesNotThrow(registry::removeAllSources);

        assertEquals(List.of(), registry.allTypes("a.Helper"),
                "a.Helper is defined only in the source sets core/main and core/test; after removeAllSources the"
                + " registry must hold no copy of it");
        assertEquals(List.of(library), registry.allTypes("lib.Thing"),
                "lib.Thing comes from lib-1.0.jar, which is not a source set; removeAllSources must keep it");
    }

    private static SourceSet sourceSet(String name) {
        return new SourceSetImpl.Builder().setName(name).setUri(URI.create("file:/" + name)).build();
    }

    private static TypeInfo type(Runtime runtime, SourceSet sourceSet, String packageName, String simpleName) {
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName(packageName).setSourceSet(sourceSet)
                .setURI(URI.create("file:/" + sourceSet.name() + "/" + simpleName + ".java")).build();
        return runtime.newTypeInfo(cu, simpleName);
    }
}
