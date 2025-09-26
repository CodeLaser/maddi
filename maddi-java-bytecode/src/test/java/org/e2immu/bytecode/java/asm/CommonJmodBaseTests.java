package org.e2immu.bytecode.java.asm;

import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.Resources;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.language.inspection.resource.CompiledTypesManagerImpl;
import org.e2immu.language.inspection.resource.ResourcesImpl;
import org.e2immu.language.inspection.resource.TypeMapImpl;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
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
        URL url = new URL("jar:file:" + System.getProperty("java.home") + "/jmods/java.base.jmod!/");
        SourceFile sourceFile = new SourceFile(url.getPath(), url.toURI(), null, null);
        cp.addJmod(sourceFile);
        CompiledTypesManagerImpl mgr = new CompiledTypesManagerImpl(classPath, new TypeMapImpl());
        compiledTypesManager = mgr;
        runtime = new RuntimeImpl();
        byteCodeInspector = new ByteCodeInspectorImpl(runtime, compiledTypesManager, true,
                false);
        mgr.setByteCodeInspector(byteCodeInspector);
    }

}
