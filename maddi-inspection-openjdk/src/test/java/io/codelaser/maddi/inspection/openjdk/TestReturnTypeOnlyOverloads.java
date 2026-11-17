/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.codelaser.maddi.inspection.openjdk;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static java.lang.constant.ConstantDescs.CD_Integer;
import static java.lang.constant.ConstantDescs.CD_String;
import static java.lang.constant.ConstantDescs.CD_int;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;

/**
 * A class file may declare two methods that differ by return type only, neither a bridge: a Java compiler never
 * writes that, Kotlin does (kotlinx-datetime's compat {@code LocalDate} has {@code kotlinx.datetime.Month getMonth()}
 * and {@code java.time.Month getMonth()}). javac's {@code Elements.getAllMembers} filtered BOTH out, so the loaded
 * type had no {@code getMonth} at all, and Exposed's {@code value.month.number} was a placeholder (CodeLaser/maddi#220).
 */
public class TestReturnTypeOnlyOverloads {

    @Test
    public void bothMethodsOfAReturnTypePairAreLoaded(@TempDir Path tmp) throws IOException {
        Path jar = tmp.resolve("lib-d.jar");
        int publicFinal = ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL;
        byte[] bytes = ClassFile.of().build(ClassDesc.of("lib.D"), cb -> cb
                .withFlags(AccessFlag.PUBLIC, AccessFlag.SUPER)
                .withMethodBody("getA", MethodTypeDesc.of(CD_String), publicFinal, c -> c.aconst_null().areturn())
                .withMethodBody("getA", MethodTypeDesc.of(CD_Integer), publicFinal, c -> c.aconst_null().areturn())
                .withMethodBody("plain", MethodTypeDesc.of(CD_int), publicFinal, c -> c.iconst_0().ireturn()));
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry("lib/D.class"));
            out.write(bytes);
            out.closeEntry();
        }

        JavaInspector javaInspector = new JavaInspectorImpl();
        SourceSet lib = new SourceSetImpl.Builder().setName("lib-d.jar").setUri(jar.toUri())
                .setExternalLibrary(true).build();
        SourceSet sourceSet = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "1").setUri(URI.create("file:/"))
                .setDependencies(List.of(SourceSetImpl.javaBase(), lib)).build();
        javaInspector.initialize(new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet).addClassPathParts(lib)
                .addClassPath("jmod:java.base")
                .build());
        javaInspector.parseMultiSourceSet(Map.of(sourceSet, Map.of("a.X", "package a; public class X { lib.D d; }")),
                new JavaInspector.ParseOptions.Builder().setFailFast(true).build());

        TypeInfo d = javaInspector.compiledTypesManager().typeWithMembers("lib.D", sourceSet);
        assertNotNull(d);
        assertEquals(List.of("getA:Integer", "getA:String", "plain:int"), d.methods().stream()
                .map(m -> m.name() + ":" + m.returnType().typeInfo().simpleName()).sorted().toList());
    }
}
