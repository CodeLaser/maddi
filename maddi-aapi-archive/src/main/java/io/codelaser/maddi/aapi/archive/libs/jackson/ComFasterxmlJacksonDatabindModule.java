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

import java.util.Collection;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleKeyDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.module.SimpleValueInstantiators;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import io.codelaser.maddi.annotation.NotModified;

/**
 * Jackson 2's SimpleModule (jackson-databind 2.20.2); the reading is in ComFasterxmlJacksonDatabind.
 */
public class ComFasterxmlJacksonDatabindModule {
    public static final String PACKAGE_NAME = "com.fasterxml.jackson.databind.module";

    //public class com.fasterxml.jackson.databind.module.SimpleModule extends com.fasterxml.jackson.databind.Module implements java.io.Serializable
    // the (de)serializers and other handlers are stored, never changed
    class SimpleModule$ {
        @NotModified Object getTypeId() { return null; }
        void setSerializers(@NotModified SimpleSerializers p0) { }
        void setDeserializers(@NotModified SimpleDeserializers p0) { }
        void setKeySerializers(@NotModified SimpleSerializers p0) { }
        void setKeyDeserializers(@NotModified SimpleKeyDeserializers p0) { }
        void setAbstractTypes(@NotModified SimpleAbstractTypeResolver p0) { }
        void setValueInstantiators(@NotModified SimpleValueInstantiators p0) { }
        SimpleModule setDeserializerModifier(@NotModified BeanDeserializerModifier p0) { return null; }
        SimpleModule setSerializerModifier(@NotModified BeanSerializerModifier p0) { return null; }
        SimpleModule addSerializer(@NotModified JsonSerializer<?> p0) { return null; }
        <T> SimpleModule addSerializer(Class<? extends T> p0, @NotModified JsonSerializer<T> p1) { return null; }
        <T> SimpleModule addKeySerializer(Class<? extends T> p0, @NotModified JsonSerializer<T> p1) { return null; }
        <T> SimpleModule addDeserializer(Class<T> p0, @NotModified JsonDeserializer<? extends T> p1) { return null; }
        SimpleModule addKeyDeserializer(Class<?> p0, @NotModified KeyDeserializer p1) { return null; }
        SimpleModule registerSubtypes(@NotModified Class<?>... p0) { return null; }
        SimpleModule registerSubtypes(@NotModified NamedType... p0) { return null; }
        SimpleModule registerSubtypes(@NotModified Collection<Class<?>> p0) { return null; }
        SimpleModule addValueInstantiator(Class<?> p0, @NotModified ValueInstantiator p1) { return null; }
        @NotModified String getModuleName() { return null; }
        void setupModule(@NotModified Module.SetupContext p0) { }
    }

}
