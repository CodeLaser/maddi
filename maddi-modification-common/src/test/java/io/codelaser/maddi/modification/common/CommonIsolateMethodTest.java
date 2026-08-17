package org.e2immu.analyzer.modification.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.modification.common.util.IsolateMethod;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static org.e2immu.analyzer.modification.common.CommonTest.javaInspectorFactory;

public abstract class CommonIsolateMethodTest {
    protected JavaInspector javaInspector;
    protected IsolateMethod isolateMethod;

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger(IsolateMethod.class)).setLevel(Level.DEBUG);
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        javaInspector = javaInspectorFactory().withSources(SourceSetImpl.testProtocolSourceSet());
        isolateMethod = new IsolateMethod(javaInspector, "");
    }

    protected String isolate(TypeInfo X, String methodName, int params, String methodString) {
        IsolateMethod.Result r = isolateMethod.isolate(X.findUniqueMethod(methodName, params));
        return isolateMethod.print(r, methodString);
    }

    // parse() returns the compilation unit's types in resolution order; when the primary type references a
    // sibling's member it can be returned after that sibling, so we look the wanted type up by its FQN.
    protected TypeInfo parse(String fqn, String source) {
        return javaInspector.parse(Map.of(fqn, source), new JavaInspector.ParseOptions.Builder()
                        .setDetailedSources(true)
                        .setFailFast(true)
                        .setIgnoreModule(true)
                        .build())
                .parseResult().findType(fqn);
    }

}
