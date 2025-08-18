package org.e2immu.analyzer.aapi.parser.archive;

import org.e2immu.analyzer.aapi.parser.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestJavaSecurity extends CommonTest {

    @Test
    public void testSecureRandomNextBytes() {
        TypeInfo typeInfo = compiledTypesManager().get(SecureRandom.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("nextBytes", 1);
        assertFalse(methodInfo.allowsInterrupts());
        assertTrue(methodInfo.isModifying());
        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertFalse(p0.isUnmodified());
    }

    @Test
    public void testMessageDigestUpdate() {
        TypeInfo typeInfo = compiledTypesManager().get(MessageDigest.class);
        assertEquals("""
                java.security.MessageDigest.update(byte), java.security.MessageDigest.update(byte[]), \
                java.security.MessageDigest.update(byte[],int,int), java.security.MessageDigest.update(java.nio.ByteBuffer)\
                """, typeInfo.methods().stream().filter(m -> "update".equals(m.name()))
                .map(Info::fullyQualifiedName)
                .sorted()
                .collect(Collectors.joining(", ")));
        MethodInfo methodInfo = typeInfo.findUniqueMethod("update", 3);
        assertFalse(methodInfo.allowsInterrupts());
        assertTrue(methodInfo.isModifying());
        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertFalse(p0.isModified());
    }

    @Test
    public void testMessageDigestDigest() {
        TypeInfo typeInfo = compiledTypesManager().get(MessageDigest.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("digest", 3);
        assertFalse(methodInfo.allowsInterrupts());
        assertTrue(methodInfo.isModifying());
        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertFalse(p0.isModified());
    }

    @Test
    public void testMessageDigestGetDigestLength() {
        TypeInfo typeInfo = compiledTypesManager().get(MessageDigest.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("getDigestLength", 0);
        assertFalse(methodInfo.allowsInterrupts());
        assertFalse(methodInfo.isModifying());
    }
}
