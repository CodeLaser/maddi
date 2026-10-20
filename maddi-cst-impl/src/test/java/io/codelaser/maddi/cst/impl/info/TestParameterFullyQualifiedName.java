package io.codelaser.maddi.cst.impl.info;

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A parameter's fully qualified name holds its method's full signature. The prepwork analysis keys every statement's
 * variable data by it (VariableDataImpl.vicByFqn), so a name built anew on every call was stored once per statement:
 * N parameters times N statements copies of a name that grows with N. On a closed-core corpus with constructors of 150
 * parameters a 24 GB heap ran out while parsing. The name is built once the method is committed, and shared.
 */
public class TestParameterFullyQualifiedName {

    @DisplayName("a parameter of a committed method returns one shared name, not a new copy per call")
    @Test
    public void theNameOfAParameterIsShared() {
        Runtime runtime = new RuntimeImpl();
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("com.foo").build();
        TypeInfo ti = runtime.newTypeInfo(cu, "Wide");
        MethodInfo m = runtime.newConstructor(ti);
        for (int i = 0; i < 150; i++) {
            m.builder().addParameter("p" + i, runtime.stringParameterizedType()).builder().commit();
        }
        m.builder().commitParameters().commit();
        ParameterInfo p = m.parameters().get(149);
        String first = p.fullyQualifiedName();
        assertEquals(m.fullyQualifiedName() + ":149:p149", first,
                "the name of parameter 149 (actual) against the method's name, index and name (expected)");
        assertSame(first, p.fullyQualifiedName(),
                "the name returned by the second call (actual) against the very string the first call returned (expected)");
    }

    @DisplayName("a name asked for while the method is still being built is not kept")
    @Test
    public void aNameAskedForBeforeTheMethodIsCommittedIsNotKept() {
        Runtime runtime = new RuntimeImpl();
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("com.foo").build();
        TypeInfo ti = runtime.newTypeInfo(cu, "Growing");
        MethodInfo m = runtime.newMethod(ti, "run", runtime.methodTypeMethod());
        ParameterInfo first = m.builder().addParameter("a", runtime.stringParameterizedType());
        first.builder().commit();
        String early = first.fullyQualifiedName();
        m.builder().addParameter("b", runtime.intParameterizedType()).builder().commit();
        m.builder().commitParameters().commit();
        assertEquals(m.fullyQualifiedName() + ":0:a", first.fullyQualifiedName(),
                "the name after the method is committed (actual) against one that holds both parameters (expected); "
                + "asked for while building it was '" + early + "'");
    }
}
