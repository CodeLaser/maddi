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
 * outside. {@code Json.Default} is the default instance; its class file declares none of the methods, so a Java
 * caller's {@code Json.Default.encodeToString(..)} binds to {@code Json}'s and reads the contracts below.
 * <p>
 * ⚠ A KOTLIN caller does not, yet: the K2 front end builds {@code Json.Default} from its member scope, inherited
 * functions included, and binds {@code Json.encodeToString(x)} to a copy owned by {@code Default} that neither these
 * member contracts nor {@code Default}'s type-level one reach (the census of 2026-11-15 still reads it as modifying;
 * CodeLaser/maddi#218).
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
