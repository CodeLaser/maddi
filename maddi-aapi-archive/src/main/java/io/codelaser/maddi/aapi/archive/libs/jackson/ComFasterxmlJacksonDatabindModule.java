/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
