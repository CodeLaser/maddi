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

import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;

/**
 * The annotated API for {@code kotlinx.coroutines.flow}: the terminal operators. Collecting a flow runs its producer
 * against a collector; the {@code Flow} object itself is a description and is not changed by it (a cold flow is
 * re-run by the next collection, a {@code SharedFlow} only gains a subscriber for the duration). What a terminal
 * operator really modifies is a destination it is given ({@code toList(dest)}), which stays {@code @Modified}.
 * <p>
 * The {@code Continuation} parameter is the suspend machinery and the lambdas are behaviour, not data: they keep their
 * defaults. Not here: {@code FlowCollector.emit}, which does hand the value on to the collector.
 */
public class KotlinxCoroutinesFlow {
    public static final String PACKAGE_NAME = "kotlinx.coroutines.flow";

    interface Flow$<T> {
        @NotModified
        Object collect(kotlinx.coroutines.flow.FlowCollector<? super T> collector,
                       kotlin.coroutines.Continuation<? super kotlin.Unit> continuation);
    }

    class FlowKt__CollectionKt$ {
        static <T> Object toList(@NotModified kotlinx.coroutines.flow.Flow<? extends T> flow,
                                 @Modified java.util.List<T> destination,
                                 kotlin.coroutines.Continuation<? super java.util.List<? extends T>> continuation) {
            return null;
        }

        static <T> Object toSet(@NotModified kotlinx.coroutines.flow.Flow<? extends T> flow,
                                @Modified java.util.Set<T> destination,
                                kotlin.coroutines.Continuation<? super java.util.Set<? extends T>> continuation) {
            return null;
        }
    }

    class FlowKt__ReduceKt$ {
        static <S, T extends S> Object reduce(@NotModified kotlinx.coroutines.flow.Flow<? extends T> flow,
                                              kotlin.jvm.functions.Function3<? super S, ? super T, ? super kotlin.coroutines.Continuation<? super S>, ?> operation,
                                              kotlin.coroutines.Continuation<? super S> continuation) {
            return null;
        }

        static <T> Object single(@NotModified kotlinx.coroutines.flow.Flow<? extends T> flow,
                                 kotlin.coroutines.Continuation<? super T> continuation) {
            return null;
        }

        static <T> Object singleOrNull(@NotModified kotlinx.coroutines.flow.Flow<? extends T> flow,
                                       kotlin.coroutines.Continuation<? super T> continuation) {
            return null;
        }

        static <T> Object first(@NotModified kotlinx.coroutines.flow.Flow<? extends T> flow,
                                kotlin.coroutines.Continuation<? super T> continuation) {
            return null;
        }

        static <T> Object firstOrNull(@NotModified kotlinx.coroutines.flow.Flow<? extends T> flow,
                                      kotlin.coroutines.Continuation<? super T> continuation) {
            return null;
        }

        static <T> Object last(@NotModified kotlinx.coroutines.flow.Flow<? extends T> flow,
                               kotlin.coroutines.Continuation<? super T> continuation) {
            return null;
        }

        static <T> Object lastOrNull(@NotModified kotlinx.coroutines.flow.Flow<? extends T> flow,
                                     kotlin.coroutines.Continuation<? super T> continuation) {
            return null;
        }
    }

    class FlowKt__CollectKt$ {
        static Object collect(@NotModified kotlinx.coroutines.flow.Flow<?> flow,
                              kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
            return null;
        }
    }

    /* census 2026-11-16 (maddi-mod#15): collecting a cold flow to count it, as reduce */
    class FlowKt__CountKt$ {
        static <T> Object count(@NotModified kotlinx.coroutines.flow.Flow<? extends T> flow,
                                kotlin.coroutines.Continuation<? super Integer> continuation) {
            return null;
        }
    }

    /* an operator wraps the flow in a new one: dependent, the upstream unchanged */
    class FlowKt__TransformKt$ {
        static <T> kotlinx.coroutines.flow.Flow<T> filterNotNull(@NotModified kotlinx.coroutines.flow.Flow<? extends T> flow) {
            return null;
        }
    }
}
