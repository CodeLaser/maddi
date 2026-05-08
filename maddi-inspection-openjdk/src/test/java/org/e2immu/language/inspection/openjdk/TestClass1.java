package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestClass1 {
    @Test
    public void test() throws Exception {
        Runtime runtime = new RuntimeImpl();
        SingleDirExplorer sde = new SingleDirExplorer(runtime);
        String sourceDir = "../maddi-inspection-openjdk-example/src/main/java";
        SourceSet sourceSet = new SourceSetImpl(
                "source", List.of(Path.of(sourceDir)),
                URI.create("file:" + Path.of(sourceDir).toAbsolutePath()),
                StandardCharsets.UTF_8, false, false, false,
                false, false, Set.of(), Set.of());
        List<TypeInfo> types = sde.go(sourceSet, "../maddi-inspection-openjdk-example/libs");
        assertEquals(1, types.size());
        TypeInfo class1 = types.getFirst();
        assertEquals("org.e2immu.example.Class1", class1.fullyQualifiedName());

        MethodInfo constructor = class1.findConstructor(0);
        assertEquals("org.e2immu.example.Class1.<init>()", constructor.fullyQualifiedName());
        assertTrue(constructor.isSynthetic());
        assertTrue(constructor.methodModifiers().contains(runtime.methodModifierPublic()));

        MethodInfo method = class1.findUniqueMethod("method", 0);
        assertEquals("source::org.e2immu.example.Class1.method()", method.descriptor());
        assertFalse(method.isSynthetic());
        assertTrue(method.methodModifiers().contains(runtime.methodModifierPrivate()));
        assertEquals(runtime.intParameterizedType(), method.returnType());

        MethodInfo voidMethod = class1.findUniqueMethod("voidMethod", 0);
        assertEquals("source::org.e2immu.example.Class1.voidMethod()", voidMethod.descriptor());
        assertFalse(voidMethod.isSynthetic());
        assertTrue(voidMethod.methodModifiers().contains(runtime.methodModifierProtected()));
        assertEquals(runtime.voidParameterizedType(), voidMethod.returnType());
    }
}
