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

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import io.codelaser.maddi.inspection.resource.InfoByFqn;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link InfoByFqn#allTypes}: every registered copy of a name, one per source set, without stubs. A library a
 * project carries in two versions has two, and analysis results must reach both (CodeLaser/maddi-mod#37).
 */
public class TestInfoByFqnAllTypes {

    @Test
    public void twoVersionsOfOneLibraryType() {
        Runtime runtime = new RuntimeImpl();
        SourceSet v1 = sourceSet("lib-1.0.jar");
        SourceSet v2 = sourceSet("lib-2.0.jar");
        InfoByFqn registry = new InfoByFqn();

        TypeInfo first = type(runtime, v1);
        registry.put(first.fullyQualifiedName(), first, v1);
        assertEquals(List.of(first), registry.allTypes("com.example.lib.Thing"));

        TypeInfo second = type(runtime, v2);
        registry.put(second.fullyQualifiedName(), second, v2);
        assertEquals(List.of(first, second), registry.allTypes("com.example.lib.Thing"));

        assertEquals(List.of(), registry.allTypes("com.example.lib.Other"));
    }

    // what timefold-solver does: its test modules load Mockito 5.21.0 and 5.23.0, each while another source set is
    // scanned, so the second class file OVERWRITES the first in the registry. Resolution answers the newer copy, as
    // before; the displaced one stays listed, since the code parsed earlier still calls it
    @Test
    public void aCopyOverwrittenFromAnotherJarStaysListed() {
        Runtime runtime = new RuntimeImpl();
        SourceSet v1 = library("mockito-core-5.21.0.jar");
        SourceSet v2 = library("mockito-core-5.23.0.jar");
        InfoByFqn registry = new InfoByFqn();

        TypeInfo first = type(runtime, v1);
        registry.put(first.fullyQualifiedName(), first, sourceSet("core/test-classes"));
        TypeInfo second = type(runtime, v2);
        registry.put(second.fullyQualifiedName(), second, sourceSet("spring-boot/test-classes"));

        assertEquals(second, registry.getType("com.example.lib.Thing", sourceSet("core/test-classes")));
        assertEquals(List.of(second, first), registry.allTypes("com.example.lib.Thing"));

        // the first jar re-read: one copy per jar, not two
        TypeInfo firstAgain = type(runtime, v1);
        registry.put(firstAgain.fullyQualifiedName(), firstAgain, sourceSet("other/test-classes"));
        assertEquals(List.of(firstAgain, second), registry.allTypes("com.example.lib.Thing"));
    }

    @Test
    public void aStubIsNoCopy() {
        Runtime runtime = new RuntimeImpl();
        InfoByFqn registry = new InfoByFqn();
        TypeInfo stub = runtime.newTypeInfo(runtime.newCompilationUnitStub("com.example.lib"), "Missing");
        registry.put(stub.fullyQualifiedName(), stub, sourceSet("main"));
        assertEquals(List.of(), registry.allTypes("com.example.lib.Missing"));
    }

    private static SourceSet sourceSet(String name) {
        return new SourceSetImpl.Builder().setName(name).setUri(URI.create("file:/" + name)).build();
    }

    private static SourceSet library(String jar) {
        return new SourceSetImpl.Builder().setName(jar).setUri(URI.create("file:/" + jar)).setLibrary(true)
                .setExternalLibrary(true).build();
    }

    private static TypeInfo type(Runtime runtime, SourceSet sourceSet) {
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("com.example.lib")
                .setSourceSet(sourceSet).setURI(URI.create("file:/" + sourceSet.name() + "/Thing.class")).build();
        return runtime.newTypeInfo(cu, "Thing");
    }
}
