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

package io.codelaser.maddi.aapi.parser.archive;

import io.codelaser.maddi.aapi.parser.CommonTest;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.stream.Collectors;

import static io.codelaser.maddi.cst.impl.analysis.PropertyImpl.CONTAINER_TYPE;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.junit.jupiter.api.Assertions.*;

public class TestJavaSecurity extends CommonTest {

    @Test
    public void testSecureRandomNextBytes() {
        TypeInfo typeInfo = compiledTypesManager().typeIfLoaded(SecureRandom.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("nextBytes", 1);
        assertFalse(methodInfo.allowsInterrupts());
        assertTrue(methodInfo.isModifying());
        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertFalse(p0.isUnmodified());
    }

    @Test
    public void testMessageDigestUpdate() {
        TypeInfo typeInfo = compiledTypesManager().typeIfLoaded(MessageDigest.class);
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
        TypeInfo typeInfo = compiledTypesManager().typeIfLoaded(MessageDigest.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("digest", 3);
        assertFalse(methodInfo.allowsInterrupts());
        assertTrue(methodInfo.isModifying());
        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertFalse(p0.isModified());
    }

    @Test
    public void testMessageDigestGetDigestLength() {
        TypeInfo typeInfo = compiledTypesManager().typeIfLoaded(MessageDigest.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("getDigestLength", 0);
        assertFalse(methodInfo.allowsInterrupts());
        assertFalse(methodInfo.isModifying());
    }

    // Principal is a read-only identity interface: getName()/implies(Subject) never modify a
    // parameter, so it is a @Container.
    @Test
    public void testPrincipalContainer() {
        TypeInfo typeInfo = compiledTypesManager().typeIfLoaded(Principal.class);
        assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
    }
}
