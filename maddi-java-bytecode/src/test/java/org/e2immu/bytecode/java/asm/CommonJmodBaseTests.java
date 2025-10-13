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
import org.e2immu.language.inspection.resource.CompiledTypesManagerImpl;
import org.e2immu.language.inspection.resource.ResourcesImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public abstract class CommonJmodBaseTests {
    protected static Runtime runtime;
    protected static Resources classPath;
    protected static ByteCodeInspectorImpl byteCodeInspector;
    protected static CompiledTypesManager compiledTypesManager;

    @BeforeAll
    public static void beforeClass() throws IOException, URISyntaxException {
        Resources cp = new ResourcesImpl(Path.of("."));
        classPath = cp;
        URI uri = URI.create("jar:file:" + System.getProperty("java.home") + "/jmods/java.base.jmod!/");
        SourceSet sourceSet = new SourceSetImpl("java.base", List.of(), URI.create("file:unknown"), StandardCharsets.UTF_8,
                false, true, true, true, false, Set.of(), Set.of());
        sourceSet.computePriorityDependencies();
        SourceFile sourceFile = new SourceFile(uri.getRawSchemeSpecificPart(), uri, sourceSet, null);
        cp.addJmod(sourceFile);
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

}
