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

package io.codelaser.maddi.aapi.archive.libs.jackson;

import io.codelaser.maddi.annotation.NotModified;

/**
 * Jackson 2's SimpleModule (jackson-databind 2.20.2); the reading is in ComFasterxmlJacksonDatabind.
 */
public class ComFasterxmlJacksonDatabindModule {
    public static final String PACKAGE_NAME = "com.fasterxml.jackson.databind.module";

    //public class com.fasterxml.jackson.databind.module.SimpleModule extends com.fasterxml.jackson.databind.Module implements java.io.Serializable
    // the (de)serializers and other handlers are stored, never changed
    class SimpleModule$ {
        @NotModified java.lang.Object getTypeId() { return null; }
        void setSerializers(@NotModified com.fasterxml.jackson.databind.module.SimpleSerializers p0) { }
        void setDeserializers(@NotModified com.fasterxml.jackson.databind.module.SimpleDeserializers p0) { }
        void setKeySerializers(@NotModified com.fasterxml.jackson.databind.module.SimpleSerializers p0) { }
        void setKeyDeserializers(@NotModified com.fasterxml.jackson.databind.module.SimpleKeyDeserializers p0) { }
        void setAbstractTypes(@NotModified com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver p0) { }
        void setValueInstantiators(@NotModified com.fasterxml.jackson.databind.module.SimpleValueInstantiators p0) { }
        com.fasterxml.jackson.databind.module.SimpleModule setDeserializerModifier(@NotModified com.fasterxml.jackson.databind.deser.BeanDeserializerModifier p0) { return null; }
        com.fasterxml.jackson.databind.module.SimpleModule setSerializerModifier(@NotModified com.fasterxml.jackson.databind.ser.BeanSerializerModifier p0) { return null; }
        com.fasterxml.jackson.databind.module.SimpleModule addSerializer(@NotModified com.fasterxml.jackson.databind.JsonSerializer<?> p0) { return null; }
        <T> com.fasterxml.jackson.databind.module.SimpleModule addSerializer(java.lang.Class<? extends T> p0, @NotModified com.fasterxml.jackson.databind.JsonSerializer<T> p1) { return null; }
        <T> com.fasterxml.jackson.databind.module.SimpleModule addKeySerializer(java.lang.Class<? extends T> p0, @NotModified com.fasterxml.jackson.databind.JsonSerializer<T> p1) { return null; }
        <T> com.fasterxml.jackson.databind.module.SimpleModule addDeserializer(java.lang.Class<T> p0, @NotModified com.fasterxml.jackson.databind.JsonDeserializer<? extends T> p1) { return null; }
        com.fasterxml.jackson.databind.module.SimpleModule addKeyDeserializer(java.lang.Class<?> p0, @NotModified com.fasterxml.jackson.databind.KeyDeserializer p1) { return null; }
        com.fasterxml.jackson.databind.module.SimpleModule registerSubtypes(@NotModified java.lang.Class<?>... p0) { return null; }
        com.fasterxml.jackson.databind.module.SimpleModule registerSubtypes(@NotModified com.fasterxml.jackson.databind.jsontype.NamedType... p0) { return null; }
        com.fasterxml.jackson.databind.module.SimpleModule registerSubtypes(@NotModified java.util.Collection<java.lang.Class<?>> p0) { return null; }
        com.fasterxml.jackson.databind.module.SimpleModule addValueInstantiator(java.lang.Class<?> p0, @NotModified com.fasterxml.jackson.databind.deser.ValueInstantiator p1) { return null; }
        @NotModified java.lang.String getModuleName() { return null; }
        void setupModule(@NotModified com.fasterxml.jackson.databind.Module.SetupContext p0) { }
    }

}
