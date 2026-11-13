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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
 * {@link InfoByFqn#removeAllSources()} with a name registered in more than one source set. Its list could not be
 * changed, and {@code removeIf} threw {@code UnsupportedOperationException}: the refactor server dropping hibernate-orm
 * (60 source sets) could not release the inspector's file managers and jar archives (CodeLaser/maddi#210).
 */
public class TestInfoByFqnRemoveAllSources {

    @DisplayName("a name two source sets define is forgotten; a library type stays")
    @Test
    public void twoSourceSets() {
        Runtime runtime = new RuntimeImpl();
        SourceSet main = sourceSet("core/main");
        SourceSet test = sourceSet("core/test");
        InfoByFqn registry = new InfoByFqn();

        TypeInfo inMain = type(runtime, main, "a", "Helper");
        registry.put(inMain.fullyQualifiedName(), inMain, main);
        TypeInfo inTest = type(runtime, test, "a", "Helper");
        registry.put(inTest.fullyQualifiedName(), inTest, test);
        TypeInfo library = type(runtime, library("lib-1.0.jar"), "lib", "Thing");
        registry.put(library.fullyQualifiedName(), library, main);
        assertEquals(List.of(inMain, inTest), registry.allTypes("a.Helper"));

        registry.removeAllSources();

        assertEquals(List.of(), registry.allTypes("a.Helper"));
        assertNull(registry.getType("a.Helper", main));
        assertEquals(List.of(library), registry.allTypes("lib.Thing"));
    }

    @DisplayName("a name a source set and a library both define: the library copy is left, and still resolves")
    @Test
    public void sourceSetAndLibrary() {
        Runtime runtime = new RuntimeImpl();
        SourceSet main = sourceSet("core/main");
        SourceSet jar = library("helper-1.0.jar");
        InfoByFqn registry = new InfoByFqn();

        TypeInfo inMain = type(runtime, main, "a", "Helper");
        registry.put(inMain.fullyQualifiedName(), inMain, main);
        TypeInfo inJar = type(runtime, jar, "a", "Helper");
        registry.put(inJar.fullyQualifiedName(), inJar, jar);
        assertEquals(List.of(inMain, inJar), registry.allTypes("a.Helper"));

        registry.removeAllSources();

        assertEquals(List.of(inJar), registry.allTypes("a.Helper"));
        // one copy left: a single entry again, as getType requires (a one-element multi entry failed its assertion)
        assertEquals(inJar, registry.getType("a.Helper", main));

        // and the source set can be registered again
        TypeInfo inMainAgain = type(runtime, main, "a", "Helper");
        registry.put(inMainAgain.fullyQualifiedName(), inMainAgain, main);
        assertEquals(inMainAgain, registry.getType("a.Helper", main));
    }

    private static SourceSet sourceSet(String name) {
        return new SourceSetImpl.Builder().setName(name).setUri(URI.create("file:/" + name)).build();
    }

    private static SourceSet library(String jar) {
        return new SourceSetImpl.Builder().setName(jar).setUri(URI.create("file:/" + jar)).setLibrary(true)
                .setExternalLibrary(true).build();
    }

    private static TypeInfo type(Runtime runtime, SourceSet sourceSet, String packageName, String simpleName) {
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName(packageName).setSourceSet(sourceSet)
                .setURI(URI.create("file:/" + sourceSet.name() + "/" + simpleName + ".java")).build();
        return runtime.newTypeInfo(cu, simpleName);
    }
}
