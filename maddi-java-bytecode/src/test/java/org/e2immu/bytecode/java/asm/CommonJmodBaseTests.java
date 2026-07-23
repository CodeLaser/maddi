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

package org.e2immu.bytecode.java.asm;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.Resources;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.language.inspection.integration.CompiledTypesManagerImpl;
import org.e2immu.language.inspection.integration.ResourcesImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
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
