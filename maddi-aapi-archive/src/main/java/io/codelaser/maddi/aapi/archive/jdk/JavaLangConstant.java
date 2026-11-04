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

package io.codelaser.maddi.aapi.archive.jdk;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.NotNull;

import java.lang.constant.ConstantDesc;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import io.codelaser.maddi.annotation.Nullable;

public class JavaLangConstant {
    public static final String PACKAGE_NAME = "java.lang.constant";

    @ImmutableContainer
    @Independent
    interface ConstantDesc$ {
        @NotNull
        Object resolveConstantDesc(MethodHandles.Lookup lookup);
    }

    //public interface Constable
    //@Container[M]
    @ImmutableContainer
    @Independent
    class Constable$ {
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        @NotNull
        Optional<? extends ConstantDesc> describeConstable() { return null; }
    }

    //public java.lang.constant.ClassDesc (shadow created for its nullability, generated)
    class ClassDesc$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.lang.constant.ClassDesc componentType() { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified boolean isArray() { return false; }
        @NotModified boolean isClassOrInterface() { return false; }
        @NotModified boolean isPrimitive() { return false; }
    }
}
