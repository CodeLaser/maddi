package io.codelaser.maddi.modification.common;

import io.codelaser.maddi.modification.common.util.IsolateMethod;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Disabled
public class TestIsolateMethodCodec extends CommonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestIsolateMethodCodec.class);

    private JavaInspector javaInspector;
    private ParseResult parseResult;

    @BeforeEach
    public void beforeEach() throws IOException {
        javaInspector = javaInspectorFactory().withSources(SourceSetImpl.testProtocolSourceSet());
    }

    @Test
    public void test() throws IOException {

        TypeInfo codecImpl = parseResult.findType("io.codelaser.maddi.cst.io.ExpressionCodec");
        IsolateMethod isolateMethod = new IsolateMethod(javaInspector, "codec");

        MethodInfo encodeExpression = codecImpl.findUniqueMethod("encodeExpression", 1);
        go(isolateMethod, encodeExpression);
        MethodInfo constructor = codecImpl.findConstructor(3);
        go(isolateMethod, constructor);
    }

    private void go(IsolateMethod isolateMethod, MethodInfo methodInfo) {
        IsolateMethod.Result r = isolateMethod.isolate(methodInfo);
        TypeInfo isolatedEncoded = r.typeInfo();
        String printed = javaInspector.print2(isolatedEncoded.compilationUnit(),
                javaInspector.runtime().qualificationSimpleNames(), javaInspector.importComputer(4, javaInspector.mainSources()));
        LOGGER.info("Frame:\n{}", printed);
        try {
            Files.writeString(Path.of("src/test/java/" + isolatedEncoded.simpleName() + ".java"), printed);
        } catch (IOException ioe) {
            throw new UnsupportedOperationException(ioe);
        }
    }
}
