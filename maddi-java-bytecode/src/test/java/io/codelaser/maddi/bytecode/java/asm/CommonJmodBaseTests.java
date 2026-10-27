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

package io.codelaser.maddi.bytecode.java.asm;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import io.codelaser.maddi.inspection.api.resource.CompiledTypesManager;
import io.codelaser.maddi.inspection.api.resource.Resources;
import io.codelaser.maddi.inspection.api.resource.SourceFile;
import io.codelaser.maddi.inspection.integration.CompiledTypesManagerImpl;
import io.codelaser.maddi.inspection.integration.ResourcesImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class CommonJmodBaseTests {
    protected static Runtime runtime;
    protected static Resources classPath;
    protected static ByteCodeInspectorImpl byteCodeInspector;
    protected static CompiledTypesManager compiledTypesManager;

    @BeforeAll
    public static void beforeClass() throws IOException, URISyntaxException {
        Resources cp = new ResourcesImpl(Path.of("."));
        classPath = cp;
        SourceSet sourceSet = new SourceSetImpl.Builder().setName("java.base")
                .setUri(URI.create("file:unknown")).setLibrary(true).setExternalLibrary(true).setPartOfJdk(true)
                .setModule(true).build();
        sourceSet.computePriorityDependencies();
        addJavaBase(cp, sourceSet);
        CompiledTypesManagerImpl mgr = new CompiledTypesManagerImpl(sourceSet, classPath);
        compiledTypesManager = mgr;
        runtime = new RuntimeImpl();
        byteCodeInspector = new ByteCodeInspectorImpl(runtime, compiledTypesManager, true,
                false);
        mgr.setByteCodeInspector(byteCodeInspector);
        mgr.addToTrie(cp, true);
        mgr.addPredefinedTypeInfoObjects(runtime.predefinedObjects(), sourceSet);
        mgr.preload("java.lang");
    }

    /**
     * Index {@code java.base}, choosing the source the same way {@code JavaInspectorImpl} does for a
     * {@code jmod:} class-path part: the {@code .jmod} file when the running JDK ships a {@code jmods/}
     * directory, otherwise the runtime image over the {@code jrt} filesystem.
     * <p>
     * Not every JDK ships {@code jmods/} — Eclipse Temurin does not — and hard-coding the {@code .jmod}
     * path made every test in this module fail there with {@code NoSuchFileException}, while the analyzer
     * itself handled that JDK fine. See {@code TestRuntimeImageFallback} for the production-path coverage.
     */
    private static void addJavaBase(Resources cp, SourceSet sourceSet) throws IOException, URISyntaxException {
        Path jmodFile = Path.of(System.getProperty("java.home"), "jmods", "java.base.jmod");
        if (Files.isRegularFile(jmodFile)) {
            URI uri = URI.create("jar:file:" + jmodFile + "!/");
            cp.addJmod(new SourceFile(uri.getRawSchemeSpecificPart(), uri, sourceSet, null));
        } else {
            cp.addModuleFromRuntimeImage(new SourceFile("java.base", URI.create("jrt:/java.base"),
                    sourceSet, null));
        }
    }
}
