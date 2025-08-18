package org.e2immu.analyzer.aapi.parser.archive;

import org.e2immu.analyzer.aapi.parser.CommonTest;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_METHOD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.INDEPENDENT_METHOD;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.IMMUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.INDEPENDENT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TestJavaTime extends CommonTest {

    @Test
    public void testDurationOfMillis() {
        TypeInfo typeInfo = compiledTypesManager().get(Duration.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("ofMillis", 1);
        assertFalse(methodInfo.allowsInterrupts());
        assertFalse(methodInfo.isModifying());
        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(IMMUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
    }

}
