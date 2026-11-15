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
 * The annotated API for {@code kotlinx.coroutines.reactive}: awaiting or collecting a Reactive Streams
 * {@code Publisher} subscribes to it, which the {@code Publisher} contract allows any number of times; the publisher
 * object is not changed (as for {@code Flow} in {@link KotlinxCoroutinesFlow}).
 */
public class KotlinxCoroutinesReactive {
    public static final String PACKAGE_NAME = "kotlinx.coroutines.reactive";

    class AwaitKt$ {
        static <T> Object awaitFirst(@NotModified org.reactivestreams.Publisher<T> publisher,
                                     kotlin.coroutines.Continuation<? super T> continuation) {
            return null;
        }

        static <T> Object awaitFirstOrNull(@NotModified org.reactivestreams.Publisher<T> publisher,
                                           kotlin.coroutines.Continuation<? super T> continuation) {
            return null;
        }

        static <T> Object awaitLast(@NotModified org.reactivestreams.Publisher<T> publisher,
                                    kotlin.coroutines.Continuation<? super T> continuation) {
            return null;
        }

        static <T> Object awaitSingle(@NotModified org.reactivestreams.Publisher<T> publisher,
                                      kotlin.coroutines.Continuation<? super T> continuation) {
            return null;
        }
    }

    class ChannelKt$ {
        static <T> Object collect(@NotModified org.reactivestreams.Publisher<T> publisher,
                                  kotlin.jvm.functions.Function1<? super T, kotlin.Unit> action,
                                  kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
            return null;
        }
    }
}
