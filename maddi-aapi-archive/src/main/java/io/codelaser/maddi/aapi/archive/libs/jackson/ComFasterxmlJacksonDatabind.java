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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.EnumNamingStrategy;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.CacheProvider;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.cfg.DatatypeFeature;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MutableCoercionConfig;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.introspect.AccessorNamingStrategy;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.codelaser.maddi.annotation.NotModified;

/**
 * Jackson 2 (jackson-databind 2.20.2), from the library-call census (TestLibraryCallCensus, 2026-10-13).
 * <p>
 * The mapper: reading, writing and converting are {@code @NotModified} methods. An {@code ObjectMapper} is configured
 * once and then shared; what a read or a write changes inside it is caching, bookkeeping the analysis does not count
 * elsewhere either. Without it, {@code mapper.readTree(in)} modified a {@code static final ObjectMapper} and through
 * it whatever held it. Configuring ({@code configure}, {@code enable}, {@code registerModule}, {@code set*}) keeps its
 * default, it modifies the mapper. Its arguments, features, modules, (de)serializers, strategies, the value written,
 * are read or stored and never changed: {@code @NotModified}. Streams, readers, writers, parsers and generators keep
 * their defaults: a read or a write consumes or fills them; and so does the target of {@code updateValue} and
 * {@code readerForUpdating}, and a format visitor, which is called back.
 * <p>
 * The builder (cfg) and SimpleModule (module) follow the same rule. Jackson 3 ({@code tools.jackson}) is not covered:
 * 7 calls on the census corpora. Generated from the jars with javap; names types in full and imports none.
 */
public class ComFasterxmlJacksonDatabind {
    public static final String PACKAGE_NAME = "com.fasterxml.jackson.databind";

    //public class com.fasterxml.jackson.databind.ObjectMapper extends com.fasterxml.jackson.core.ObjectCodec implements com.fasterxml.jackson.core.Versioned,java.io.Serializable
    // reading, writing and converting leave the mapper as it is (its configuration; caches are bookkeeping); configuration arguments are read or stored, never changed
    class ObjectMapper$ {
        @NotModified ObjectMapper copy() { return null; }
        @NotModified ObjectMapper copyWith(@NotModified JsonFactory p0) { return null; }
        @NotModified Version version() { return null; }
        ObjectMapper registerModule(@NotModified Module p0) { return null; }
        ObjectMapper registerModules(@NotModified Module... p0) { return null; }
        @NotModified Set<Object> getRegisteredModuleIds() { return null; }
        @NotModified JsonGenerator createGenerator(OutputStream p0) { return null; }
        @NotModified JsonGenerator createGenerator(OutputStream p0, @NotModified JsonEncoding p1) { return null; }
        @NotModified JsonGenerator createGenerator(Writer p0) { return null; }
        @NotModified JsonGenerator createGenerator(File p0, @NotModified JsonEncoding p1) { return null; }
        @NotModified JsonGenerator createGenerator(DataOutput p0) { return null; }
        @NotModified JsonParser createParser(File p0) { return null; }
        @NotModified JsonParser createParser(URL p0) { return null; }
        @NotModified JsonParser createParser(InputStream p0) { return null; }
        @NotModified JsonParser createParser(Reader p0) { return null; }
        @NotModified JsonParser createParser(@NotModified byte[] p0) { return null; }
        @NotModified JsonParser createParser(@NotModified byte[] p0, int p1, int p2) { return null; }
        @NotModified JsonParser createParser(String p0) { return null; }
        @NotModified JsonParser createParser(@NotModified char[] p0) { return null; }
        @NotModified JsonParser createParser(@NotModified char[] p0, int p1, int p2) { return null; }
        @NotModified JsonParser createParser(DataInput p0) { return null; }
        @NotModified JsonParser createNonBlockingByteArrayParser() { return null; }
        @NotModified SerializationConfig getSerializationConfig() { return null; }
        @NotModified DeserializationConfig getDeserializationConfig() { return null; }
        @NotModified DeserializationContext getDeserializationContext() { return null; }
        ObjectMapper setSerializerFactory(@NotModified SerializerFactory p0) { return null; }
        @NotModified SerializerFactory getSerializerFactory() { return null; }
        ObjectMapper setSerializerProvider(@NotModified DefaultSerializerProvider p0) { return null; }
        @NotModified SerializerProvider getSerializerProvider() { return null; }
        @NotModified SerializerProvider getSerializerProviderInstance() { return null; }
        ObjectMapper setMixIns(@NotModified Map<Class<?>, Class<?>> p0) { return null; }
        ObjectMapper setMixInResolver(@NotModified ClassIntrospector.MixInResolver p0) { return null; }
        void setMixInAnnotations(@NotModified Map<Class<?>, Class<?>> p0) { }
        @NotModified VisibilityChecker<?> getVisibilityChecker() { return null; }
        ObjectMapper setVisibility(@NotModified VisibilityChecker<?> p0) { return null; }
        ObjectMapper setVisibility(@NotModified PropertyAccessor p0, @NotModified JsonAutoDetect.Visibility p1) { return null; }
        @NotModified SubtypeResolver getSubtypeResolver() { return null; }
        ObjectMapper setSubtypeResolver(@NotModified SubtypeResolver p0) { return null; }
        ObjectMapper setAnnotationIntrospector(@NotModified AnnotationIntrospector p0) { return null; }
        ObjectMapper setAnnotationIntrospectors(@NotModified AnnotationIntrospector p0, @NotModified AnnotationIntrospector p1) { return null; }
        ObjectMapper setPropertyNamingStrategy(@NotModified PropertyNamingStrategy p0) { return null; }
        @NotModified PropertyNamingStrategy getPropertyNamingStrategy() { return null; }
        ObjectMapper setEnumNamingStrategy(@NotModified EnumNamingStrategy p0) { return null; }
        @NotModified EnumNamingStrategy getEnumNamingStrategy() { return null; }
        ObjectMapper setAccessorNaming(@NotModified AccessorNamingStrategy.Provider p0) { return null; }
        ObjectMapper setDefaultPrettyPrinter(@NotModified PrettyPrinter p0) { return null; }
        void setVisibilityChecker(@NotModified VisibilityChecker<?> p0) { }
        ObjectMapper setPolymorphicTypeValidator(@NotModified PolymorphicTypeValidator p0) { return null; }
        @NotModified PolymorphicTypeValidator getPolymorphicTypeValidator() { return null; }
        ObjectMapper setSerializationInclusion(@NotModified JsonInclude.Include p0) { return null; }
        ObjectMapper setPropertyInclusion(@NotModified JsonInclude.Value p0) { return null; }
        ObjectMapper setDefaultPropertyInclusion(@NotModified JsonInclude.Value p0) { return null; }
        ObjectMapper setDefaultPropertyInclusion(@NotModified JsonInclude.Include p0) { return null; }
        ObjectMapper setDefaultSetterInfo(@NotModified JsonSetter.Value p0) { return null; }
        ObjectMapper setDefaultVisibility(@NotModified JsonAutoDetect.Value p0) { return null; }
        void registerSubtypes(@NotModified Class<?>... p0) { }
        void registerSubtypes(@NotModified NamedType... p0) { }
        void registerSubtypes(@NotModified Collection<Class<?>> p0) { }
        ObjectMapper activateDefaultTyping(@NotModified PolymorphicTypeValidator p0) { return null; }
        ObjectMapper activateDefaultTyping(@NotModified PolymorphicTypeValidator p0, @NotModified ObjectMapper.DefaultTyping p1) { return null; }
        ObjectMapper activateDefaultTyping(@NotModified PolymorphicTypeValidator p0, @NotModified ObjectMapper.DefaultTyping p1, @NotModified JsonTypeInfo.As p2) { return null; }
        ObjectMapper activateDefaultTypingAsProperty(@NotModified PolymorphicTypeValidator p0, @NotModified ObjectMapper.DefaultTyping p1, String p2) { return null; }
        ObjectMapper setDefaultTyping(@NotModified TypeResolverBuilder<?> p0) { return null; }
        ObjectMapper enableDefaultTyping(@NotModified ObjectMapper.DefaultTyping p0) { return null; }
        ObjectMapper enableDefaultTyping(@NotModified ObjectMapper.DefaultTyping p0, @NotModified JsonTypeInfo.As p1) { return null; }
        ObjectMapper enableDefaultTypingAsProperty(@NotModified ObjectMapper.DefaultTyping p0, String p1) { return null; }
        MutableCoercionConfig coercionConfigFor(@NotModified LogicalType p0) { return null; }
        @NotModified TypeFactory getTypeFactory() { return null; }
        ObjectMapper setTypeFactory(@NotModified TypeFactory p0) { return null; }
        @NotModified JavaType constructType(Type p0) { return null; }
        @NotModified JavaType constructType(@NotModified TypeReference<?> p0) { return null; }
        @NotModified JsonNodeFactory getNodeFactory() { return null; }
        ObjectMapper setNodeFactory(@NotModified JsonNodeFactory p0) { return null; }
        ObjectMapper setConstructorDetector(@NotModified ConstructorDetector p0) { return null; }
        ObjectMapper setCacheProvider(@NotModified CacheProvider p0) { return null; }
        ObjectMapper addHandler(@NotModified DeserializationProblemHandler p0) { return null; }
        ObjectMapper setConfig(@NotModified DeserializationConfig p0) { return null; }
        void setFilters(@NotModified FilterProvider p0) { }
        ObjectMapper setFilterProvider(@NotModified FilterProvider p0) { return null; }
        ObjectMapper setBase64Variant(@NotModified Base64Variant p0) { return null; }
        ObjectMapper setConfig(@NotModified SerializationConfig p0) { return null; }
        @NotModified JsonFactory tokenStreamFactory() { return null; }
        @NotModified JsonFactory getFactory() { return null; }
        ObjectMapper setDateFormat(@NotModified DateFormat p0) { return null; }
        @NotModified DateFormat getDateFormat() { return null; }
        Object setHandlerInstantiator(@NotModified HandlerInstantiator p0) { return null; }
        ObjectMapper setInjectableValues(@NotModified InjectableValues p0) { return null; }
        @NotModified InjectableValues getInjectableValues() { return null; }
        ObjectMapper setTimeZone(@NotModified TimeZone p0) { return null; }
        ObjectMapper setDefaultAttributes(@NotModified ContextAttributes p0) { return null; }
        @NotModified boolean isEnabled(@NotModified MapperFeature p0) { return false; }
        ObjectMapper configure(@NotModified MapperFeature p0, boolean p1) { return null; }
        ObjectMapper enable(@NotModified MapperFeature... p0) { return null; }
        ObjectMapper disable(@NotModified MapperFeature... p0) { return null; }
        @NotModified boolean isEnabled(@NotModified SerializationFeature p0) { return false; }
        ObjectMapper configure(@NotModified SerializationFeature p0, boolean p1) { return null; }
        ObjectMapper enable(@NotModified SerializationFeature p0) { return null; }
        ObjectMapper enable(@NotModified SerializationFeature p0, @NotModified SerializationFeature... p1) { return null; }
        ObjectMapper disable(@NotModified SerializationFeature p0) { return null; }
        ObjectMapper disable(@NotModified SerializationFeature p0, @NotModified SerializationFeature... p1) { return null; }
        @NotModified boolean isEnabled(@NotModified DeserializationFeature p0) { return false; }
        ObjectMapper configure(@NotModified DeserializationFeature p0, boolean p1) { return null; }
        ObjectMapper enable(@NotModified DeserializationFeature p0) { return null; }
        ObjectMapper enable(@NotModified DeserializationFeature p0, @NotModified DeserializationFeature... p1) { return null; }
        ObjectMapper disable(@NotModified DeserializationFeature p0) { return null; }
        ObjectMapper disable(@NotModified DeserializationFeature p0, @NotModified DeserializationFeature... p1) { return null; }
        ObjectMapper configure(@NotModified DatatypeFeature p0, boolean p1) { return null; }
        @NotModified boolean isEnabled(JsonParser.Feature p0) { return false; }
        @NotModified boolean isEnabled(JsonGenerator.Feature p0) { return false; }
        @NotModified boolean isEnabled(@NotModified JsonFactory.Feature p0) { return false; }
        @NotModified boolean isEnabled(@NotModified StreamReadFeature p0) { return false; }
        @NotModified boolean isEnabled(@NotModified StreamWriteFeature p0) { return false; }
        @NotModified <T> T readValue(JsonParser p0, Class<T> p1) { return null; }
        @NotModified <T> T readValue(JsonParser p0, @NotModified TypeReference<T> p1) { return null; }
        @NotModified <T> T readValue(JsonParser p0, @NotModified ResolvedType p1) { return null; }
        @NotModified <T> T readValue(JsonParser p0, @NotModified JavaType p1) { return null; }
        @NotModified <T extends TreeNode> T readTree(JsonParser p0) { return null; }
        @NotModified <T> MappingIterator<T> readValues(JsonParser p0, @NotModified ResolvedType p1) { return null; }
        @NotModified <T> MappingIterator<T> readValues(JsonParser p0, @NotModified JavaType p1) { return null; }
        @NotModified <T> MappingIterator<T> readValues(JsonParser p0, Class<T> p1) { return null; }
        @NotModified <T> MappingIterator<T> readValues(JsonParser p0, @NotModified TypeReference<T> p1) { return null; }
        @NotModified JsonNode readTree(InputStream p0) { return null; }
        @NotModified JsonNode readTree(Reader p0) { return null; }
        @NotModified JsonNode readTree(String p0) { return null; }
        @NotModified JsonNode readTree(@NotModified byte[] p0) { return null; }
        @NotModified JsonNode readTree(@NotModified byte[] p0, int p1, int p2) { return null; }
        @NotModified JsonNode readTree(File p0) { return null; }
        @NotModified JsonNode readTree(URL p0) { return null; }
        @NotModified void writeValue(JsonGenerator p0, @NotModified Object p1) { }
        @NotModified void writeTree(JsonGenerator p0, @NotModified TreeNode p1) { }
        @NotModified void writeTree(JsonGenerator p0, @NotModified JsonNode p1) { }
        @NotModified ObjectNode createObjectNode() { return null; }
        @NotModified ArrayNode createArrayNode() { return null; }
        @NotModified JsonNode missingNode() { return null; }
        @NotModified JsonNode nullNode() { return null; }
        @NotModified JsonParser treeAsTokens(@NotModified TreeNode p0) { return null; }
        @NotModified <T> T treeToValue(@NotModified TreeNode p0, Class<T> p1) { return null; }
        @NotModified <T> T treeToValue(@NotModified TreeNode p0, @NotModified JavaType p1) { return null; }
        @NotModified <T> T treeToValue(@NotModified TreeNode p0, @NotModified TypeReference<T> p1) { return null; }
        @NotModified <T extends JsonNode> T valueToTree(@NotModified Object p0) { return null; }
        @NotModified boolean canSerialize(Class<?> p0) { return false; }
        @NotModified boolean canSerialize(Class<?> p0, @NotModified AtomicReference<Throwable> p1) { return false; }
        @NotModified boolean canDeserialize(@NotModified JavaType p0) { return false; }
        @NotModified boolean canDeserialize(@NotModified JavaType p0, @NotModified AtomicReference<Throwable> p1) { return false; }
        @NotModified <T> T readValue(File p0, Class<T> p1) { return null; }
        @NotModified <T> T readValue(File p0, @NotModified TypeReference<T> p1) { return null; }
        @NotModified <T> T readValue(File p0, @NotModified JavaType p1) { return null; }
        @NotModified <T> T readValue(URL p0, Class<T> p1) { return null; }
        @NotModified <T> T readValue(URL p0, @NotModified TypeReference<T> p1) { return null; }
        @NotModified <T> T readValue(URL p0, @NotModified JavaType p1) { return null; }
        @NotModified <T> T readValue(String p0, Class<T> p1) { return null; }
        @NotModified <T> T readValue(String p0, @NotModified TypeReference<T> p1) { return null; }
        @NotModified <T> T readValue(String p0, @NotModified JavaType p1) { return null; }
        @NotModified <T> T readValue(Reader p0, Class<T> p1) { return null; }
        @NotModified <T> T readValue(Reader p0, @NotModified TypeReference<T> p1) { return null; }
        @NotModified <T> T readValue(Reader p0, @NotModified JavaType p1) { return null; }
        @NotModified <T> T readValue(InputStream p0, Class<T> p1) { return null; }
        @NotModified <T> T readValue(InputStream p0, @NotModified TypeReference<T> p1) { return null; }
        @NotModified <T> T readValue(InputStream p0, @NotModified JavaType p1) { return null; }
        @NotModified <T> T readValue(@NotModified byte[] p0, Class<T> p1) { return null; }
        @NotModified <T> T readValue(@NotModified byte[] p0, int p1, int p2, Class<T> p3) { return null; }
        @NotModified <T> T readValue(@NotModified byte[] p0, @NotModified TypeReference<T> p1) { return null; }
        @NotModified <T> T readValue(@NotModified byte[] p0, int p1, int p2, @NotModified TypeReference<T> p3) { return null; }
        @NotModified <T> T readValue(@NotModified byte[] p0, @NotModified JavaType p1) { return null; }
        @NotModified <T> T readValue(@NotModified byte[] p0, int p1, int p2, @NotModified JavaType p3) { return null; }
        @NotModified <T> T readValue(DataInput p0, Class<T> p1) { return null; }
        @NotModified <T> T readValue(DataInput p0, @NotModified JavaType p1) { return null; }
        @NotModified void writeValue(File p0, @NotModified Object p1) { }
        @NotModified void writeValue(OutputStream p0, @NotModified Object p1) { }
        @NotModified void writeValue(DataOutput p0, @NotModified Object p1) { }
        @NotModified void writeValue(Writer p0, @NotModified Object p1) { }
        @NotModified String writeValueAsString(@NotModified Object p0) { return null; }
        @NotModified byte[] writeValueAsBytes(@NotModified Object p0) { return null; }
        @NotModified ObjectWriter writer() { return null; }
        @NotModified ObjectWriter writer(@NotModified SerializationFeature p0) { return null; }
        @NotModified ObjectWriter writer(@NotModified SerializationFeature p0, @NotModified SerializationFeature... p1) { return null; }
        @NotModified ObjectWriter writer(@NotModified DateFormat p0) { return null; }
        @NotModified ObjectWriter writerWithView(Class<?> p0) { return null; }
        @NotModified ObjectWriter writerFor(Class<?> p0) { return null; }
        @NotModified ObjectWriter writerFor(@NotModified TypeReference<?> p0) { return null; }
        @NotModified ObjectWriter writerFor(@NotModified JavaType p0) { return null; }
        @NotModified ObjectWriter writer(@NotModified PrettyPrinter p0) { return null; }
        @NotModified ObjectWriter writerWithDefaultPrettyPrinter() { return null; }
        @NotModified ObjectWriter writer(@NotModified FilterProvider p0) { return null; }
        @NotModified ObjectWriter writer(@NotModified FormatSchema p0) { return null; }
        @NotModified ObjectWriter writer(@NotModified Base64Variant p0) { return null; }
        @NotModified ObjectWriter writer(@NotModified CharacterEscapes p0) { return null; }
        @NotModified ObjectWriter writer(@NotModified ContextAttributes p0) { return null; }
        @NotModified ObjectWriter writerWithType(Class<?> p0) { return null; }
        @NotModified ObjectWriter writerWithType(@NotModified TypeReference<?> p0) { return null; }
        @NotModified ObjectWriter writerWithType(@NotModified JavaType p0) { return null; }
        @NotModified ObjectReader reader() { return null; }
        @NotModified ObjectReader reader(@NotModified DeserializationFeature p0) { return null; }
        @NotModified ObjectReader reader(@NotModified DeserializationFeature p0, @NotModified DeserializationFeature... p1) { return null; }
        @NotModified ObjectReader readerForUpdating(Object p0) { return null; }
        @NotModified ObjectReader readerFor(@NotModified JavaType p0) { return null; }
        @NotModified ObjectReader readerFor(Class<?> p0) { return null; }
        @NotModified ObjectReader readerFor(@NotModified TypeReference<?> p0) { return null; }
        @NotModified ObjectReader readerForArrayOf(Class<?> p0) { return null; }
        @NotModified ObjectReader readerForListOf(Class<?> p0) { return null; }
        @NotModified ObjectReader readerForMapOf(Class<?> p0) { return null; }
        @NotModified ObjectReader reader(@NotModified JsonNodeFactory p0) { return null; }
        @NotModified ObjectReader reader(@NotModified FormatSchema p0) { return null; }
        @NotModified ObjectReader reader(@NotModified InjectableValues p0) { return null; }
        @NotModified ObjectReader readerWithView(Class<?> p0) { return null; }
        @NotModified ObjectReader reader(@NotModified Base64Variant p0) { return null; }
        @NotModified ObjectReader reader(@NotModified ContextAttributes p0) { return null; }
        @NotModified ObjectReader reader(@NotModified JavaType p0) { return null; }
        @NotModified ObjectReader reader(Class<?> p0) { return null; }
        @NotModified ObjectReader reader(@NotModified TypeReference<?> p0) { return null; }
        @NotModified <T> T convertValue(@NotModified Object p0, Class<T> p1) { return null; }
        @NotModified <T> T convertValue(@NotModified Object p0, @NotModified TypeReference<T> p1) { return null; }
        @NotModified <T> T convertValue(@NotModified Object p0, @NotModified JavaType p1) { return null; }
        @NotModified <T> T updateValue(T p0, Object p1) { return null; }
        @NotModified JsonSchema generateJsonSchema(Class<?> p0) { return null; }
        @NotModified void acceptJsonFormatVisitor(Class<?> p0, JsonFormatVisitorWrapper p1) { }
        @NotModified void acceptJsonFormatVisitor(@NotModified JavaType p0, JsonFormatVisitorWrapper p1) { }
    }

}
