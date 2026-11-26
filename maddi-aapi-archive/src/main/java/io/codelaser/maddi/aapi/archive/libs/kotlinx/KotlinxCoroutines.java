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

    /* census 2026-11-16 (maddi-mod#15): the state queries. A Job's cancel/complete do change it, and keep their defaults */
    interface Job$ {
        @NotModified
        boolean isActive();

        /* maddi#216 census: the deprecated 'job + job' returns the other job; a CoroutineContext's plus is a query */
        @NotModified
        kotlinx.coroutines.Job plus(kotlinx.coroutines.Job other);
    }

    interface Deferred$<T> {
        @NotModified
        T getCompleted();
    }

    class JobKt__JobKt$ {
        static kotlinx.coroutines.Job getJob(@NotModified kotlin.coroutines.CoroutineContext context) {
            return null;
        }

        /* throws when the context's job is cancelled; reads it, changes nothing */
        static void ensureActive(@NotModified kotlin.coroutines.CoroutineContext context) {
        }
    }

    class CoroutineDispatcher$ {
        /* maddi#216 census: the CoroutineContext queries it overrides, which its supertype states @NotModified; without a
           hint here they took the default, @Modified */
        @NotModified
        <E extends kotlin.coroutines.CoroutineContext.Element> E get(kotlin.coroutines.CoroutineContext.Key<E> key) {
            return null;
        }

        @NotModified
        kotlin.coroutines.CoroutineContext minusKey(kotlin.coroutines.CoroutineContext.Key<?> key) {
            return null;
        }

        /* deprecated: 'dispatcher + dispatcher' returns the other one */
        @NotModified
        kotlinx.coroutines.CoroutineDispatcher plus(kotlinx.coroutines.CoroutineDispatcher other) {
            return null;
        }

        /* a view sharing this dispatcher's threads, with its own limit: this one is not changed */
        @NotModified
        kotlinx.coroutines.CoroutineDispatcher limitedParallelism(int parallelism, String name) {
            return null;
        }
    }

    class MainCoroutineDispatcher$ {
        @NotModified
        kotlinx.coroutines.MainCoroutineDispatcher getImmediate() {
            return null;
        }
    }
}
