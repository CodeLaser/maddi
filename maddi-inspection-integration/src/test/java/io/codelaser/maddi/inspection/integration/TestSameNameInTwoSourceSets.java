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

package io.codelaser.maddi.inspection.integration;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import io.codelaser.maddi.inspection.api.resource.SourceFile;

/**
 * A fully qualified name known twice: nacos's {@code com.google.protobuf.Any} is protobuf's compiled class in the jar,
 * and a source copy in the istio module, which vendors protobuf's generated Any.java. Each source set must get its
 * own: a lookup from the module that has the jar on its classpath returns the jar's, a lookup from istio returns
 * istio's, whatever the order they were added in, and after either is rewired. mapSingleTypeForFQN, the lookup's fast
 * path, used to hold whichever copy was added (or rewired) last, and answered with it for every requester.
 */
public class TestSameNameInTwoSourceSets {

    private static final String FQN = "com.google.protobuf.Any";

    private final Runtime runtime = new RuntimeImpl();
    private final SourceSet jar = new SourceSetImpl.Builder().setName("protobuf.jar").setExternalLibrary(true)
            .setUri(URI.create("file:/protobuf.jar")).build();
    private final SourceSet api = new SourceSetImpl.Builder().setName("api/main")
            .setUri(URI.create("file:/api/src/main/java")).setDependencies(List.of(jar)).build();
    private final SourceSet istio = new SourceSetImpl.Builder().setName("istio/main")
            .setUri(URI.create("file:/istio/src/main/java")).build();

    {
        // as JavaInspectorImpl does once the source sets are known: the ranking bestCandidate uses
        for (SourceSet sourceSet : List.of(jar, api, istio)) sourceSet.computePriorityDependencies();
    }

    private TypeInfo any(SourceSet sourceSet) {
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("com.google.protobuf")
                .setSourceSet(sourceSet).build();
        TypeInfo typeInfo = runtime.newTypeInfo(cu, "Any");
        // inspected, as a loaded class file is: the registry would otherwise go and load the jar's from bytecode
        typeInfo.builder().setTypeNature(runtime.typeNatureClass()).setAccess(runtime.accessPublic())
                .setParentClass(runtime.objectParameterizedType()).commit();
        return typeInfo;
    }

    private static SourceFile file(SourceSet sourceSet, String suffix) {
        String path = "com/google/protobuf/Any" + suffix;
        return new SourceFile(path, URI.create(sourceSet.uri() + "/" + path), sourceSet, null);
    }

    private void check(CompiledTypesManagerImpl ctm, TypeInfo jarAny, TypeInfo istioAny) {
        assertSame(jarAny, ctm.type(FQN, api));
        assertSame(istioAny, ctm.type(FQN, istio));
        assertSame(jarAny, ctm.typeIfLoaded(FQN, api));
        assertSame(istioAny, ctm.typeIfLoaded(FQN, istio));
    }

    @DisplayName("each source set finds its own type of a name known twice, in either add order")
    @Test
    public void addOrder() {
        for (boolean jarFirst : new boolean[]{true, false}) {
            CompiledTypesManagerImpl ctm = new CompiledTypesManagerImpl(null, null);
            TypeInfo jarAny = any(jar);
            TypeInfo istioAny = any(istio);
            if (jarFirst) {
                ctm.addTypeInfo(file(jar, ".class"), jarAny);
                ctm.addTypeInfo(file(istio, ".java"), istioAny);
            } else {
                ctm.addTypeInfo(file(istio, ".java"), istioAny);
                ctm.addTypeInfo(file(jar, ".class"), jarAny);
            }
            check(ctm, jarAny, istioAny);
        }
    }

    @DisplayName("rewiring one source set's type re-points that source set only")
    @Test
    public void rewire() {
        CompiledTypesManagerImpl ctm = new CompiledTypesManagerImpl(null, null);
        TypeInfo jarAny = any(jar);
        TypeInfo istioAny = any(istio);
        ctm.addTypeInfo(file(jar, ".class"), jarAny);
        ctm.addTypeInfo(file(istio, ".java"), istioAny);

        // dedup's two-step load: the rewired istio copy, then the original istio copy again
        TypeInfo istioRewired = any(istio);
        ctm.setRewiredType(istioRewired);
        check(ctm, jarAny, istioRewired);
        ctm.setRewiredType(istioAny);
        check(ctm, jarAny, istioAny);

        TypeInfo jarRewired = any(jar);
        ctm.setRewiredType(jarRewired);
        check(ctm, jarRewired, istioAny);
    }

    @DisplayName("a name known once keeps its fast path, and a rewire re-points it")
    @Test
    public void single() {
        CompiledTypesManagerImpl ctm = new CompiledTypesManagerImpl(null, null);
        TypeInfo istioAny = any(istio);
        ctm.addTypeInfo(file(istio, ".java"), istioAny);
        assertSame(istioAny, ctm.type(FQN, istio));
        assertSame(istioAny, ctm.type(FQN, api)); // the only one there is
        TypeInfo rewired = any(istio);
        ctm.setRewiredType(rewired);
        assertSame(rewired, ctm.type(FQN, istio));
        assertSame(rewired, ctm.type(FQN, api));
    }
}
