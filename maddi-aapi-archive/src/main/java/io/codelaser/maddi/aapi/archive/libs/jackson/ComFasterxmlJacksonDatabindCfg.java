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

import java.text.DateFormat;
import java.util.Collection;
import java.util.TimeZone;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.EnumNamingStrategy;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.CacheProvider;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.cfg.DatatypeFeature;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.cfg.MutableCoercionConfig;
import com.fasterxml.jackson.databind.cfg.MutableConfigOverride;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.introspect.AccessorNamingStrategy;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.codelaser.maddi.annotation.NotModified;

/**
 * Jackson 2's mapper builder (jackson-databind 2.20.2); the reading is in ComFasterxmlJacksonDatabind.
 */
public class ComFasterxmlJacksonDatabindCfg {
    public static final String PACKAGE_NAME = "com.fasterxml.jackson.databind.cfg";

    //public abstract class com.fasterxml.jackson.databind.cfg.MapperBuilder<M extends com.fasterxml.jackson.databind.ObjectMapper, B extends com.fasterxml.jackson.databind.cfg.MapperBuilder<M, B>>
    // configuration arguments are read or stored, never changed
    class MapperBuilder$<M extends ObjectMapper, B extends MapperBuilder<M, B>> {
        @NotModified boolean isEnabled(@NotModified MapperFeature p0) { return false; }
        @NotModified boolean isEnabled(@NotModified DeserializationFeature p0) { return false; }
        @NotModified boolean isEnabled(@NotModified SerializationFeature p0) { return false; }
        @NotModified boolean isEnabled(JsonParser.Feature p0) { return false; }
        @NotModified boolean isEnabled(JsonGenerator.Feature p0) { return false; }
        B enable(@NotModified MapperFeature... p0) { return null; }
        B disable(@NotModified MapperFeature... p0) { return null; }
        B configure(@NotModified MapperFeature p0, boolean p1) { return null; }
        B enable(@NotModified SerializationFeature... p0) { return null; }
        B disable(@NotModified SerializationFeature... p0) { return null; }
        B configure(@NotModified SerializationFeature p0, boolean p1) { return null; }
        B enable(@NotModified DeserializationFeature... p0) { return null; }
        B disable(@NotModified DeserializationFeature... p0) { return null; }
        B configure(@NotModified DeserializationFeature p0, boolean p1) { return null; }
        B enable(@NotModified DatatypeFeature... p0) { return null; }
        B disable(@NotModified DatatypeFeature... p0) { return null; }
        B configure(@NotModified DatatypeFeature p0, boolean p1) { return null; }
        B enable(@NotModified StreamReadFeature... p0) { return null; }
        B disable(@NotModified StreamReadFeature... p0) { return null; }
        B configure(@NotModified StreamReadFeature p0, boolean p1) { return null; }
        B enable(@NotModified StreamWriteFeature... p0) { return null; }
        B disable(@NotModified StreamWriteFeature... p0) { return null; }
        B configure(@NotModified StreamWriteFeature p0, boolean p1) { return null; }
        B withConfigOverride(Class<?> p0, @NotModified Consumer<MutableConfigOverride> p1) { return null; }
        B withCoercionConfig(@NotModified LogicalType p0, @NotModified Consumer<MutableCoercionConfig> p1) { return null; }
        B withCoercionConfig(Class<?> p0, @NotModified Consumer<MutableCoercionConfig> p1) { return null; }
        B withCoercionConfigDefaults(@NotModified Consumer<MutableCoercionConfig> p0) { return null; }
        B addModule(@NotModified Module p0) { return null; }
        B addModules(@NotModified Module... p0) { return null; }
        B annotationIntrospector(@NotModified AnnotationIntrospector p0) { return null; }
        B defaultAttributes(@NotModified ContextAttributes p0) { return null; }
        B typeFactory(@NotModified TypeFactory p0) { return null; }
        B subtypeResolver(@NotModified SubtypeResolver p0) { return null; }
        B visibility(@NotModified VisibilityChecker<?> p0) { return null; }
        B visibility(@NotModified PropertyAccessor p0, @NotModified JsonAutoDetect.Visibility p1) { return null; }
        B handlerInstantiator(@NotModified HandlerInstantiator p0) { return null; }
        B propertyNamingStrategy(@NotModified PropertyNamingStrategy p0) { return null; }
        B enumNamingStrategy(@NotModified EnumNamingStrategy p0) { return null; }
        B accessorNaming(@NotModified AccessorNamingStrategy.Provider p0) { return null; }
        B serializerFactory(@NotModified SerializerFactory p0) { return null; }
        B filterProvider(@NotModified FilterProvider p0) { return null; }
        B defaultPrettyPrinter(@NotModified PrettyPrinter p0) { return null; }
        B injectableValues(@NotModified InjectableValues p0) { return null; }
        B nodeFactory(@NotModified JsonNodeFactory p0) { return null; }
        B constructorDetector(@NotModified ConstructorDetector p0) { return null; }
        B cacheProvider(@NotModified CacheProvider p0) { return null; }
        B addHandler(@NotModified DeserializationProblemHandler p0) { return null; }
        B defaultSetterInfo(@NotModified JsonSetter.Value p0) { return null; }
        B defaultDateFormat(@NotModified DateFormat p0) { return null; }
        B defaultTimeZone(@NotModified TimeZone p0) { return null; }
        B defaultBase64Variant(@NotModified Base64Variant p0) { return null; }
        B serializationInclusion(@NotModified JsonInclude.Include p0) { return null; }
        B defaultPropertyInclusion(@NotModified JsonInclude.Value p0) { return null; }
        B registerSubtypes(@NotModified Class<?>... p0) { return null; }
        B registerSubtypes(@NotModified NamedType... p0) { return null; }
        B registerSubtypes(@NotModified Collection<Class<?>> p0) { return null; }
        B polymorphicTypeValidator(@NotModified PolymorphicTypeValidator p0) { return null; }
        B activateDefaultTyping(@NotModified PolymorphicTypeValidator p0) { return null; }
        B activateDefaultTyping(@NotModified PolymorphicTypeValidator p0, @NotModified ObjectMapper.DefaultTyping p1) { return null; }
        B activateDefaultTyping(@NotModified PolymorphicTypeValidator p0, @NotModified ObjectMapper.DefaultTyping p1, @NotModified JsonTypeInfo.As p2) { return null; }
        B activateDefaultTypingAsProperty(@NotModified PolymorphicTypeValidator p0, @NotModified ObjectMapper.DefaultTyping p1, String p2) { return null; }
        B setDefaultTyping(@NotModified TypeResolverBuilder<?> p0) { return null; }
    }

}
