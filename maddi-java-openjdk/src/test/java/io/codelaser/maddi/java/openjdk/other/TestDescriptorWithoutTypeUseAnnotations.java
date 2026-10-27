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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A descriptor is a NAME: it must not vary with the TYPE-USE annotations written on a particular use of a type.
 * <p>
 * Since TYPE-USE annotations are carried on {@code ParameterizedType} (6d189c44e), {@code fullyQualifiedName()} left
 * them out and {@code descriptor()} did not, so {@code MethodInfo.descriptor()} read
 * {@code resolvePoolSize(@…NonNull java.lang.String,…)}. Every lookup of such a method by the signature the rest of
 * the toolchain writes then missed: slowTest 2026-09-25, TestDemoteDeclaredTypesTimefold, three rows its
 * suggestion half offered were "Unknown method" to its apply half.
 */
public class TestDescriptorWithoutTypeUseAnnotations extends CommonTest {

    @Language("java")
    private static final String X = """
            package a.b;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            import java.util.List;
            public class X<T> {
                @Target(ElementType.TYPE_USE) public @interface NonNull {}
                @Target(ElementType.TYPE_USE) public @interface Nullable {}

                public static int poolSize(@NonNull String name, @NonNull String @NonNull... magic) { return 0; }
                public static void classes(List<@Nullable String> list, @NonNull Object context) { }
                private void write(T root, @Nullable StringBuilder xslt) { }
            }
            """;

    @DisplayName("TYPE-USE annotations stay out of parameter and method descriptors")
    @Test
    public void descriptorsCarryNoAnnotations() {
        Map<String, TypeInfo> pr = scan(false, "a.b.X", X);
        TypeInfo x = pr.get("a.b.X");

        MethodInfo poolSize = x.findUniqueMethod("poolSize", 2);
        assertEquals("java.lang.String", poolSize.parameters().getFirst().parameterizedType().descriptor());
        // a method descriptor starts with its source set ("source::a.b.X..."); the parameter list is the point
        assertEquals("a.b.X.poolSize(java.lang.String,java.lang.String[])", belowSourceSet(poolSize));
        assertEquals("a.b.X.classes(java.util.List,java.lang.Object)",
                belowSourceSet(x.findUniqueMethod("classes", 2)));
        assertEquals("a.b.X.write(java.lang.Object,java.lang.StringBuilder)",
                belowSourceSet(x.findUniqueMethod("write", 2)));

        // the annotations themselves are still carried: this is about NAMES, not about dropping them from the model
        assertFalse(poolSize.parameters().getFirst().parameterizedType().annotations().isEmpty(),
                "the model still has the @NonNull on the parameter's type");
    }

    private static String belowSourceSet(MethodInfo methodInfo) {
        String descriptor = methodInfo.descriptor();
        int colons = descriptor.indexOf("::");
        return colons < 0 ? descriptor : descriptor.substring(colons + 2);
    }
}
