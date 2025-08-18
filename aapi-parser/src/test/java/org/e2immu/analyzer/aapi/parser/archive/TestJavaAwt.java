package org.e2immu.analyzer.aapi.parser.archive;

import org.e2immu.analyzer.aapi.parser.CommonTest;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.COMMUTABLE_METHODS;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaAwt extends CommonTest {

    @Test
    public void testContainerAdd() {
        TypeInfo typeInfo = compiledTypesManager().getOrLoad(Container.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("add", 1);
        assertTrue(methodInfo.isModifying());
        testCommutable(methodInfo);
    }

    @Test
    public void testContainerAddWithConstraints() {
        TypeInfo typeInfo = compiledTypesManager().getOrLoad(Container.class);
        MethodInfo methodInfo = typeInfo.methods().stream()
                                        .filter(m -> m.simpleName().equals("add")
                                                             && m.parameters().size() == 2
                                                             && m.parameters().get(1)
                                                                        .parameterizedType()
                                                                        .equals(runtime().newParameterizedType(runtime().objectTypeInfo(), 0)))
                                        .findFirst().orElseThrow();
        assertTrue(methodInfo.isModifying());
        testCommutable(methodInfo);
    }

    @Test
    public void testContainerSetLayout() {
        TypeInfo typeInfo = compiledTypesManager().getOrLoad(Container.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("setLayout", 1);
        assertTrue(methodInfo.isModifying());
        testCommutable(methodInfo);
    }

    @Test
    public void testComponentAddMouseListener() {
        TypeInfo typeInfo = compiledTypesManager().getOrLoad(Component.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("addMouseListener", 1);
        assertTrue(methodInfo.isModifying());
        testCommutable(methodInfo);
    }

    private void testCommutable(MethodInfo methodInfo) {
        Value.CommutableData cd = methodInfo.analysis().getOrNull(COMMUTABLE_METHODS, ValueImpl.CommutableData.class);
        assertTrue(cd.isBlankMultiParSeq());
    }
}
