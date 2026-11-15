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

import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.NotModified;

/**
 * The annotated API for {@code kotlinx.serialization.json}. A {@code Json} instance is its configuration and
 * serializers module, both fixed at construction; its descriptor-schema cache is an idempotent memo, invisible from
 * outside. {@code Json.Default} is the default instance, and every call through it (the common
 * {@code Json.encodeToString(x)}) resolves on the {@code Default} object, whose class file declares none of the
 * methods: hence the type-level contract on both, which reaches the inherited members too (see maddi#216 for why a
 * member contract on {@code Json} alone does not).
 */
public class KotlinxSerializationJson {
    public static final String PACKAGE_NAME = "kotlinx.serialization.json";

    @ImmutableContainer
    class Json$ {
        @NotModified
        <T> String encodeToString(@NotModified kotlinx.serialization.SerializationStrategy<? super T> serializer,
                                  @NotModified T value) {
            return null;
        }

        @NotModified
        <T> T decodeFromString(@NotModified kotlinx.serialization.DeserializationStrategy<? extends T> deserializer,
                               @NotModified String string) {
            return null;
        }

        @NotModified
        <T> kotlinx.serialization.json.JsonElement encodeToJsonElement(
                @NotModified kotlinx.serialization.SerializationStrategy<? super T> serializer, @NotModified T value) {
            return null;
        }

        @NotModified
        <T> T decodeFromJsonElement(@NotModified kotlinx.serialization.DeserializationStrategy<? extends T> deserializer,
                                    @NotModified kotlinx.serialization.json.JsonElement element) {
            return null;
        }

        @NotModified
        kotlinx.serialization.json.JsonElement parseToJsonElement(@NotModified String string) { return null; }

        @ImmutableContainer
        class Default {
        }
    }
}
