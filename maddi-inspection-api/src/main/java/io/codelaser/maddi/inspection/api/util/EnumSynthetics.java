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

package io.codelaser.maddi.inspection.api.util;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;

/**
 * The compiler-generated members of an enum ({@code name()}, static {@code values()}, static
 * {@code valueOf(String)}) as signature-only synthetic methods. The entry constants (each a
 * {@code public static final <Enum>} field) are created by the caller. Shared by the Java parsers and the
 * Kotlin front-end; the analogue of {@link RecordSynthetics}.
 */
public class EnumSynthetics {
    private final Runtime runtime;
    private final TypeInfo typeInfo;
    private final TypeInfo.Builder builder;

    public EnumSynthetics(Runtime runtime, TypeInfo typeInfo, TypeInfo.Builder builder) {
        this.runtime = runtime;
        this.typeInfo = typeInfo;
        this.builder = builder;
    }

    public void create() {
        // name() returns String
        MethodInfo name = runtime.newMethod(typeInfo, "name", runtime.methodTypeMethod());
        name.builder()
                .setSource(runtime.noSource())
                .setSynthetic(true)
                .setAccess(runtime.accessPublic())
                .addMethodModifier(runtime.methodModifierPublic())
                .setReturnType(runtime.stringParameterizedType())
                .setMethodBody(runtime.emptyBlock())
                .setMissingData(runtime.methodMissingMethodBody())
                .commitParameters()
                .commit();
        builder.addMethod(name);

        // values() returns E[]
        MethodInfo values = runtime.newMethod(typeInfo, "values", runtime.methodTypeStaticMethod());
        values.builder()
                .setSource(runtime.noSource())
                .setSynthetic(true)
                .setAccess(runtime.accessPublic())
                .addMethodModifier(runtime.methodModifierPublic())
                .addMethodModifier(runtime.methodModifierStatic())
                .setReturnType(runtime.newParameterizedType(typeInfo, 1))
                .setMethodBody(runtime.emptyBlock())
                .setMissingData(runtime.methodMissingMethodBody())
                .commitParameters()
                .commit();
        builder.addMethod(values);

        // valueOf(String) returns E
        MethodInfo valueOf = runtime.newMethod(typeInfo, "valueOf", runtime.methodTypeStaticMethod());
        valueOf.builder()
                .setSource(runtime.noSource())
                .setSynthetic(true)
                .setAccess(runtime.accessPublic())
                .addMethodModifier(runtime.methodModifierStatic())
                .addMethodModifier(runtime.methodModifierPublic())
                .setReturnType(runtime.newParameterizedType(typeInfo, 0))
                .setMethodBody(runtime.emptyBlock())
                .setMissingData(runtime.methodMissingMethodBody())
                .addParameter("name", runtime.stringParameterizedType());
        valueOf.builder()
                .commitParameters()
                .commit();
        builder.addMethod(valueOf);
    }
}
