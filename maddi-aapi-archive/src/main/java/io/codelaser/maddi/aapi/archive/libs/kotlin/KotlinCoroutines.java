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
package io.codelaser.maddi.aapi.archive.libs.kotlin;

import io.codelaser.maddi.annotation.NotModified;

/**
 * The annotated API for {@code kotlin.coroutines}. A {@code CoroutineContext} is a persistent, indexed set of
 * elements: {@code plus} and {@code minusKey} return a new context, {@code get} and {@code fold} only read. Its
 * ELEMENTS can be mutable (a {@code Job} is one), so the type carries no immutability claim; its methods are
 * queries. Uncontracted, every {@code withContext(ctx + x)} read the context, and with it the coroutine's scope, as
 * modified (census of 2026-11-15: detekt, exposed, coil, retrofit).
 * <p>
 * Imports nothing from {@code kotlin.*}: the targets are named by {@link #PACKAGE_NAME} and class name, so the unit
 * survives on a hints factory without kotlin-stdlib (see {@code TestParseAnalyzeWrite}).
 */
public class KotlinCoroutines {
    public static final String PACKAGE_NAME = "kotlin.coroutines";

    interface CoroutineContext$ {
        @NotModified
        <E extends kotlin.coroutines.CoroutineContext.Element> E get(
                @NotModified kotlin.coroutines.CoroutineContext.Key<E> key);

        @NotModified
        kotlin.coroutines.CoroutineContext plus(@NotModified kotlin.coroutines.CoroutineContext context);

        @NotModified
        kotlin.coroutines.CoroutineContext minusKey(@NotModified kotlin.coroutines.CoroutineContext.Key<?> key);

        /* hands each element to the operation; whether that modifies it is the operation's contract */
        @NotModified
        <R> R fold(R initial, kotlin.jvm.functions.Function2<? super R, ? super kotlin.coroutines.CoroutineContext.Element,
                ? extends R> operation);
    }

    /* public val context: CoroutineContext -- a continuation's context is fixed when it is created */
    interface Continuation$<T> {
        @NotModified
        kotlin.coroutines.CoroutineContext getContext();
    }
}
