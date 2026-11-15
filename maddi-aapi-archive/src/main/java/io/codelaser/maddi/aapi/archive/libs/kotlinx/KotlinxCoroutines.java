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
package io.codelaser.maddi.aapi.archive.libs.kotlinx;

import io.codelaser.maddi.annotation.NotModified;

/**
 * The annotated API for {@code kotlinx.coroutines}: the builders that only READ the context they are given
 * ({@code withContext(ctx)}, {@code CoroutineScope(ctx)}). The scope a {@code launch}/{@code async} starts a child in
 * does gain a child, and a {@code Job}'s {@code cancel} changes it: those keep their defaults.
 */
public class KotlinxCoroutines {
    public static final String PACKAGE_NAME = "kotlinx.coroutines";

    class BuildersKt__Builders_commonKt$ {
        static <T> Object withContext(@NotModified kotlin.coroutines.CoroutineContext context,
                                      kotlin.jvm.functions.Function2<? super kotlinx.coroutines.CoroutineScope, ? super kotlin.coroutines.Continuation<? super T>, ?> block,
                                      kotlin.coroutines.Continuation<? super T> continuation) {
            return null;
        }
    }

    class CoroutineScopeKt$ {
        static kotlinx.coroutines.CoroutineScope CoroutineScope(@NotModified kotlin.coroutines.CoroutineContext context) {
            return null;
        }
    }
}
