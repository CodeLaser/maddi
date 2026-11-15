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
 * The annotated API for {@code kotlinx.serialization}. A format encodes a value by walking it with a serializer and
 * decodes into a fresh one: neither the format, the serializer nor the value encoded is modified (a serializer is a
 * stateless description of a type). Uncontracted, each {@code json.encodeToString(x)} read {@code x} as modified.
 * <p>
 * The format interfaces carry the type-level contract that {@code Json} (in {@link KotlinxSerializationJson}) needs
 * from its supertypes: every kotlinx format (Json, Cbor, ProtoBuf, Properties) is its configuration and serializers
 * module, fixed when it is built.
 */
public class KotlinxSerialization {
    public static final String PACKAGE_NAME = "kotlinx.serialization";

    @ImmutableContainer
    interface SerialFormat$ {
    }

    @ImmutableContainer
    interface StringFormat$ {
        @NotModified
        <T> String encodeToString(@NotModified kotlinx.serialization.SerializationStrategy<? super T> serializer,
                                  @NotModified T value);

        @NotModified
        <T> T decodeFromString(@NotModified kotlinx.serialization.DeserializationStrategy<? extends T> deserializer,
                               @NotModified String string);
    }

    @ImmutableContainer
    interface BinaryFormat$ {
        @NotModified
        <T> byte[] encodeToByteArray(@NotModified kotlinx.serialization.SerializationStrategy<? super T> serializer,
                                     @NotModified T value);

        @NotModified
        <T> T decodeFromByteArray(@NotModified kotlinx.serialization.DeserializationStrategy<? extends T> deserializer,
                                  @NotModified byte[] bytes);
    }

    // the `serializer(module, type)` lookup: reads the module's registry
    class SerializersKt__SerializersJvmKt$ {
        @NotModified
        static kotlinx.serialization.KSerializer<Object> serializer(
                @NotModified kotlinx.serialization.modules.SerializersModule module,
                @NotModified java.lang.reflect.Type type) {
            return null;
        }
    }
}
