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
 * Jackson 2's mapper builder (jackson-databind 2.20.2); the reading is in ComFasterxmlJacksonDatabind.
 */
public class ComFasterxmlJacksonDatabindCfg {
    public static final String PACKAGE_NAME = "com.fasterxml.jackson.databind.cfg";

    //public abstract class com.fasterxml.jackson.databind.cfg.MapperBuilder<M extends com.fasterxml.jackson.databind.ObjectMapper, B extends com.fasterxml.jackson.databind.cfg.MapperBuilder<M, B>>
    // configuration arguments are read or stored, never changed
    class MapperBuilder$<M extends com.fasterxml.jackson.databind.ObjectMapper, B extends com.fasterxml.jackson.databind.cfg.MapperBuilder<M, B>> {
        @NotModified boolean isEnabled(@NotModified com.fasterxml.jackson.databind.MapperFeature p0) { return false; }
        @NotModified boolean isEnabled(@NotModified com.fasterxml.jackson.databind.DeserializationFeature p0) { return false; }
        @NotModified boolean isEnabled(@NotModified com.fasterxml.jackson.databind.SerializationFeature p0) { return false; }
        @NotModified boolean isEnabled(com.fasterxml.jackson.core.JsonParser.Feature p0) { return false; }
        @NotModified boolean isEnabled(com.fasterxml.jackson.core.JsonGenerator.Feature p0) { return false; }
        B enable(@NotModified com.fasterxml.jackson.databind.MapperFeature... p0) { return null; }
        B disable(@NotModified com.fasterxml.jackson.databind.MapperFeature... p0) { return null; }
        B configure(@NotModified com.fasterxml.jackson.databind.MapperFeature p0, boolean p1) { return null; }
        B enable(@NotModified com.fasterxml.jackson.databind.SerializationFeature... p0) { return null; }
        B disable(@NotModified com.fasterxml.jackson.databind.SerializationFeature... p0) { return null; }
        B configure(@NotModified com.fasterxml.jackson.databind.SerializationFeature p0, boolean p1) { return null; }
        B enable(@NotModified com.fasterxml.jackson.databind.DeserializationFeature... p0) { return null; }
        B disable(@NotModified com.fasterxml.jackson.databind.DeserializationFeature... p0) { return null; }
        B configure(@NotModified com.fasterxml.jackson.databind.DeserializationFeature p0, boolean p1) { return null; }
        B enable(@NotModified com.fasterxml.jackson.databind.cfg.DatatypeFeature... p0) { return null; }
        B disable(@NotModified com.fasterxml.jackson.databind.cfg.DatatypeFeature... p0) { return null; }
        B configure(@NotModified com.fasterxml.jackson.databind.cfg.DatatypeFeature p0, boolean p1) { return null; }
        B enable(@NotModified com.fasterxml.jackson.core.StreamReadFeature... p0) { return null; }
        B disable(@NotModified com.fasterxml.jackson.core.StreamReadFeature... p0) { return null; }
        B configure(@NotModified com.fasterxml.jackson.core.StreamReadFeature p0, boolean p1) { return null; }
        B enable(@NotModified com.fasterxml.jackson.core.StreamWriteFeature... p0) { return null; }
        B disable(@NotModified com.fasterxml.jackson.core.StreamWriteFeature... p0) { return null; }
        B configure(@NotModified com.fasterxml.jackson.core.StreamWriteFeature p0, boolean p1) { return null; }
        B withConfigOverride(java.lang.Class<?> p0, @NotModified java.util.function.Consumer<com.fasterxml.jackson.databind.cfg.MutableConfigOverride> p1) { return null; }
        B withCoercionConfig(@NotModified com.fasterxml.jackson.databind.type.LogicalType p0, @NotModified java.util.function.Consumer<com.fasterxml.jackson.databind.cfg.MutableCoercionConfig> p1) { return null; }
        B withCoercionConfig(java.lang.Class<?> p0, @NotModified java.util.function.Consumer<com.fasterxml.jackson.databind.cfg.MutableCoercionConfig> p1) { return null; }
        B withCoercionConfigDefaults(@NotModified java.util.function.Consumer<com.fasterxml.jackson.databind.cfg.MutableCoercionConfig> p0) { return null; }
        B addModule(@NotModified com.fasterxml.jackson.databind.Module p0) { return null; }
        B addModules(@NotModified com.fasterxml.jackson.databind.Module... p0) { return null; }
        B annotationIntrospector(@NotModified com.fasterxml.jackson.databind.AnnotationIntrospector p0) { return null; }
        B defaultAttributes(@NotModified com.fasterxml.jackson.databind.cfg.ContextAttributes p0) { return null; }
        B typeFactory(@NotModified com.fasterxml.jackson.databind.type.TypeFactory p0) { return null; }
        B subtypeResolver(@NotModified com.fasterxml.jackson.databind.jsontype.SubtypeResolver p0) { return null; }
        B visibility(@NotModified com.fasterxml.jackson.databind.introspect.VisibilityChecker<?> p0) { return null; }
        B visibility(@NotModified com.fasterxml.jackson.annotation.PropertyAccessor p0, @NotModified com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility p1) { return null; }
        B handlerInstantiator(@NotModified com.fasterxml.jackson.databind.cfg.HandlerInstantiator p0) { return null; }
        B propertyNamingStrategy(@NotModified com.fasterxml.jackson.databind.PropertyNamingStrategy p0) { return null; }
        B enumNamingStrategy(@NotModified com.fasterxml.jackson.databind.EnumNamingStrategy p0) { return null; }
        B accessorNaming(@NotModified com.fasterxml.jackson.databind.introspect.AccessorNamingStrategy.Provider p0) { return null; }
        B serializerFactory(@NotModified com.fasterxml.jackson.databind.ser.SerializerFactory p0) { return null; }
        B filterProvider(@NotModified com.fasterxml.jackson.databind.ser.FilterProvider p0) { return null; }
        B defaultPrettyPrinter(@NotModified com.fasterxml.jackson.core.PrettyPrinter p0) { return null; }
        B injectableValues(@NotModified com.fasterxml.jackson.databind.InjectableValues p0) { return null; }
        B nodeFactory(@NotModified com.fasterxml.jackson.databind.node.JsonNodeFactory p0) { return null; }
        B constructorDetector(@NotModified com.fasterxml.jackson.databind.cfg.ConstructorDetector p0) { return null; }
        B cacheProvider(@NotModified com.fasterxml.jackson.databind.cfg.CacheProvider p0) { return null; }
        B addHandler(@NotModified com.fasterxml.jackson.databind.deser.DeserializationProblemHandler p0) { return null; }
        B defaultSetterInfo(@NotModified com.fasterxml.jackson.annotation.JsonSetter.Value p0) { return null; }
        B defaultDateFormat(@NotModified java.text.DateFormat p0) { return null; }
        B defaultTimeZone(@NotModified java.util.TimeZone p0) { return null; }
        B defaultBase64Variant(@NotModified com.fasterxml.jackson.core.Base64Variant p0) { return null; }
        B serializationInclusion(@NotModified com.fasterxml.jackson.annotation.JsonInclude.Include p0) { return null; }
        B defaultPropertyInclusion(@NotModified com.fasterxml.jackson.annotation.JsonInclude.Value p0) { return null; }
        B registerSubtypes(@NotModified java.lang.Class<?>... p0) { return null; }
        B registerSubtypes(@NotModified com.fasterxml.jackson.databind.jsontype.NamedType... p0) { return null; }
        B registerSubtypes(@NotModified java.util.Collection<java.lang.Class<?>> p0) { return null; }
        B polymorphicTypeValidator(@NotModified com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator p0) { return null; }
        B activateDefaultTyping(@NotModified com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator p0) { return null; }
        B activateDefaultTyping(@NotModified com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator p0, @NotModified com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping p1) { return null; }
        B activateDefaultTyping(@NotModified com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator p0, @NotModified com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping p1, @NotModified com.fasterxml.jackson.annotation.JsonTypeInfo.As p2) { return null; }
        B activateDefaultTypingAsProperty(@NotModified com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator p0, @NotModified com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping p1, java.lang.String p2) { return null; }
        B setDefaultTyping(@NotModified com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder<?> p0) { return null; }
    }

}
