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
        @NotModified com.fasterxml.jackson.databind.ObjectMapper copy() { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectMapper copyWith(@NotModified com.fasterxml.jackson.core.JsonFactory p0) { return null; }
        @NotModified com.fasterxml.jackson.core.Version version() { return null; }
        com.fasterxml.jackson.databind.ObjectMapper registerModule(@NotModified com.fasterxml.jackson.databind.Module p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper registerModules(@NotModified com.fasterxml.jackson.databind.Module... p0) { return null; }
        @NotModified java.util.Set<java.lang.Object> getRegisteredModuleIds() { return null; }
        @NotModified com.fasterxml.jackson.core.JsonGenerator createGenerator(java.io.OutputStream p0) { return null; }
        @NotModified com.fasterxml.jackson.core.JsonGenerator createGenerator(java.io.OutputStream p0, @NotModified com.fasterxml.jackson.core.JsonEncoding p1) { return null; }
        @NotModified com.fasterxml.jackson.core.JsonGenerator createGenerator(java.io.Writer p0) { return null; }
        @NotModified com.fasterxml.jackson.core.JsonGenerator createGenerator(java.io.File p0, @NotModified com.fasterxml.jackson.core.JsonEncoding p1) { return null; }
        @NotModified com.fasterxml.jackson.core.JsonGenerator createGenerator(java.io.DataOutput p0) { return null; }
        @NotModified com.fasterxml.jackson.core.JsonParser createParser(java.io.File p0) { return null; }
        @NotModified com.fasterxml.jackson.core.JsonParser createParser(java.net.URL p0) { return null; }
        @NotModified com.fasterxml.jackson.core.JsonParser createParser(java.io.InputStream p0) { return null; }
        @NotModified com.fasterxml.jackson.core.JsonParser createParser(java.io.Reader p0) { return null; }
        @NotModified com.fasterxml.jackson.core.JsonParser createParser(@NotModified byte[] p0) { return null; }
        @NotModified com.fasterxml.jackson.core.JsonParser createParser(@NotModified byte[] p0, int p1, int p2) { return null; }
        @NotModified com.fasterxml.jackson.core.JsonParser createParser(java.lang.String p0) { return null; }
        @NotModified com.fasterxml.jackson.core.JsonParser createParser(@NotModified char[] p0) { return null; }
        @NotModified com.fasterxml.jackson.core.JsonParser createParser(@NotModified char[] p0, int p1, int p2) { return null; }
        @NotModified com.fasterxml.jackson.core.JsonParser createParser(java.io.DataInput p0) { return null; }
        @NotModified com.fasterxml.jackson.core.JsonParser createNonBlockingByteArrayParser() { return null; }
        @NotModified com.fasterxml.jackson.databind.SerializationConfig getSerializationConfig() { return null; }
        @NotModified com.fasterxml.jackson.databind.DeserializationConfig getDeserializationConfig() { return null; }
        @NotModified com.fasterxml.jackson.databind.DeserializationContext getDeserializationContext() { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setSerializerFactory(@NotModified com.fasterxml.jackson.databind.ser.SerializerFactory p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ser.SerializerFactory getSerializerFactory() { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setSerializerProvider(@NotModified com.fasterxml.jackson.databind.ser.DefaultSerializerProvider p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.SerializerProvider getSerializerProvider() { return null; }
        @NotModified com.fasterxml.jackson.databind.SerializerProvider getSerializerProviderInstance() { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setMixIns(@NotModified java.util.Map<java.lang.Class<?>, java.lang.Class<?>> p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setMixInResolver(@NotModified com.fasterxml.jackson.databind.introspect.ClassIntrospector.MixInResolver p0) { return null; }
        void setMixInAnnotations(@NotModified java.util.Map<java.lang.Class<?>, java.lang.Class<?>> p0) { }
        @NotModified com.fasterxml.jackson.databind.introspect.VisibilityChecker<?> getVisibilityChecker() { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setVisibility(@NotModified com.fasterxml.jackson.databind.introspect.VisibilityChecker<?> p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setVisibility(@NotModified com.fasterxml.jackson.annotation.PropertyAccessor p0, @NotModified com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility p1) { return null; }
        @NotModified com.fasterxml.jackson.databind.jsontype.SubtypeResolver getSubtypeResolver() { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setSubtypeResolver(@NotModified com.fasterxml.jackson.databind.jsontype.SubtypeResolver p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setAnnotationIntrospector(@NotModified com.fasterxml.jackson.databind.AnnotationIntrospector p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setAnnotationIntrospectors(@NotModified com.fasterxml.jackson.databind.AnnotationIntrospector p0, @NotModified com.fasterxml.jackson.databind.AnnotationIntrospector p1) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setPropertyNamingStrategy(@NotModified com.fasterxml.jackson.databind.PropertyNamingStrategy p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.PropertyNamingStrategy getPropertyNamingStrategy() { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setEnumNamingStrategy(@NotModified com.fasterxml.jackson.databind.EnumNamingStrategy p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.EnumNamingStrategy getEnumNamingStrategy() { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setAccessorNaming(@NotModified com.fasterxml.jackson.databind.introspect.AccessorNamingStrategy.Provider p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setDefaultPrettyPrinter(@NotModified com.fasterxml.jackson.core.PrettyPrinter p0) { return null; }
        void setVisibilityChecker(@NotModified com.fasterxml.jackson.databind.introspect.VisibilityChecker<?> p0) { }
        com.fasterxml.jackson.databind.ObjectMapper setPolymorphicTypeValidator(@NotModified com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator getPolymorphicTypeValidator() { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setSerializationInclusion(@NotModified com.fasterxml.jackson.annotation.JsonInclude.Include p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setPropertyInclusion(@NotModified com.fasterxml.jackson.annotation.JsonInclude.Value p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setDefaultPropertyInclusion(@NotModified com.fasterxml.jackson.annotation.JsonInclude.Value p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setDefaultPropertyInclusion(@NotModified com.fasterxml.jackson.annotation.JsonInclude.Include p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setDefaultSetterInfo(@NotModified com.fasterxml.jackson.annotation.JsonSetter.Value p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setDefaultVisibility(@NotModified com.fasterxml.jackson.annotation.JsonAutoDetect.Value p0) { return null; }
        void registerSubtypes(@NotModified java.lang.Class<?>... p0) { }
        void registerSubtypes(@NotModified com.fasterxml.jackson.databind.jsontype.NamedType... p0) { }
        void registerSubtypes(@NotModified java.util.Collection<java.lang.Class<?>> p0) { }
        com.fasterxml.jackson.databind.ObjectMapper activateDefaultTyping(@NotModified com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper activateDefaultTyping(@NotModified com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator p0, @NotModified com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping p1) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper activateDefaultTyping(@NotModified com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator p0, @NotModified com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping p1, @NotModified com.fasterxml.jackson.annotation.JsonTypeInfo.As p2) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper activateDefaultTypingAsProperty(@NotModified com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator p0, @NotModified com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping p1, java.lang.String p2) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setDefaultTyping(@NotModified com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder<?> p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper enableDefaultTyping(@NotModified com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper enableDefaultTyping(@NotModified com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping p0, @NotModified com.fasterxml.jackson.annotation.JsonTypeInfo.As p1) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper enableDefaultTypingAsProperty(@NotModified com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping p0, java.lang.String p1) { return null; }
        com.fasterxml.jackson.databind.cfg.MutableCoercionConfig coercionConfigFor(@NotModified com.fasterxml.jackson.databind.type.LogicalType p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.type.TypeFactory getTypeFactory() { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setTypeFactory(@NotModified com.fasterxml.jackson.databind.type.TypeFactory p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.JavaType constructType(java.lang.reflect.Type p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.JavaType constructType(@NotModified com.fasterxml.jackson.core.type.TypeReference<?> p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.node.JsonNodeFactory getNodeFactory() { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setNodeFactory(@NotModified com.fasterxml.jackson.databind.node.JsonNodeFactory p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setConstructorDetector(@NotModified com.fasterxml.jackson.databind.cfg.ConstructorDetector p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setCacheProvider(@NotModified com.fasterxml.jackson.databind.cfg.CacheProvider p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper addHandler(@NotModified com.fasterxml.jackson.databind.deser.DeserializationProblemHandler p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setConfig(@NotModified com.fasterxml.jackson.databind.DeserializationConfig p0) { return null; }
        void setFilters(@NotModified com.fasterxml.jackson.databind.ser.FilterProvider p0) { }
        com.fasterxml.jackson.databind.ObjectMapper setFilterProvider(@NotModified com.fasterxml.jackson.databind.ser.FilterProvider p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setBase64Variant(@NotModified com.fasterxml.jackson.core.Base64Variant p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setConfig(@NotModified com.fasterxml.jackson.databind.SerializationConfig p0) { return null; }
        @NotModified com.fasterxml.jackson.core.JsonFactory tokenStreamFactory() { return null; }
        @NotModified com.fasterxml.jackson.core.JsonFactory getFactory() { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setDateFormat(@NotModified java.text.DateFormat p0) { return null; }
        @NotModified java.text.DateFormat getDateFormat() { return null; }
        java.lang.Object setHandlerInstantiator(@NotModified com.fasterxml.jackson.databind.cfg.HandlerInstantiator p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setInjectableValues(@NotModified com.fasterxml.jackson.databind.InjectableValues p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.InjectableValues getInjectableValues() { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setTimeZone(@NotModified java.util.TimeZone p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper setDefaultAttributes(@NotModified com.fasterxml.jackson.databind.cfg.ContextAttributes p0) { return null; }
        @NotModified boolean isEnabled(@NotModified com.fasterxml.jackson.databind.MapperFeature p0) { return false; }
        com.fasterxml.jackson.databind.ObjectMapper configure(@NotModified com.fasterxml.jackson.databind.MapperFeature p0, boolean p1) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper enable(@NotModified com.fasterxml.jackson.databind.MapperFeature... p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper disable(@NotModified com.fasterxml.jackson.databind.MapperFeature... p0) { return null; }
        @NotModified boolean isEnabled(@NotModified com.fasterxml.jackson.databind.SerializationFeature p0) { return false; }
        com.fasterxml.jackson.databind.ObjectMapper configure(@NotModified com.fasterxml.jackson.databind.SerializationFeature p0, boolean p1) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper enable(@NotModified com.fasterxml.jackson.databind.SerializationFeature p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper enable(@NotModified com.fasterxml.jackson.databind.SerializationFeature p0, @NotModified com.fasterxml.jackson.databind.SerializationFeature... p1) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper disable(@NotModified com.fasterxml.jackson.databind.SerializationFeature p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper disable(@NotModified com.fasterxml.jackson.databind.SerializationFeature p0, @NotModified com.fasterxml.jackson.databind.SerializationFeature... p1) { return null; }
        @NotModified boolean isEnabled(@NotModified com.fasterxml.jackson.databind.DeserializationFeature p0) { return false; }
        com.fasterxml.jackson.databind.ObjectMapper configure(@NotModified com.fasterxml.jackson.databind.DeserializationFeature p0, boolean p1) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper enable(@NotModified com.fasterxml.jackson.databind.DeserializationFeature p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper enable(@NotModified com.fasterxml.jackson.databind.DeserializationFeature p0, @NotModified com.fasterxml.jackson.databind.DeserializationFeature... p1) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper disable(@NotModified com.fasterxml.jackson.databind.DeserializationFeature p0) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper disable(@NotModified com.fasterxml.jackson.databind.DeserializationFeature p0, @NotModified com.fasterxml.jackson.databind.DeserializationFeature... p1) { return null; }
        com.fasterxml.jackson.databind.ObjectMapper configure(@NotModified com.fasterxml.jackson.databind.cfg.DatatypeFeature p0, boolean p1) { return null; }
        @NotModified boolean isEnabled(com.fasterxml.jackson.core.JsonParser.Feature p0) { return false; }
        @NotModified boolean isEnabled(com.fasterxml.jackson.core.JsonGenerator.Feature p0) { return false; }
        @NotModified boolean isEnabled(@NotModified com.fasterxml.jackson.core.JsonFactory.Feature p0) { return false; }
        @NotModified boolean isEnabled(@NotModified com.fasterxml.jackson.core.StreamReadFeature p0) { return false; }
        @NotModified boolean isEnabled(@NotModified com.fasterxml.jackson.core.StreamWriteFeature p0) { return false; }
        @NotModified <T> T readValue(com.fasterxml.jackson.core.JsonParser p0, java.lang.Class<T> p1) { return null; }
        @NotModified <T> T readValue(com.fasterxml.jackson.core.JsonParser p0, @NotModified com.fasterxml.jackson.core.type.TypeReference<T> p1) { return null; }
        @NotModified <T> T readValue(com.fasterxml.jackson.core.JsonParser p0, @NotModified com.fasterxml.jackson.core.type.ResolvedType p1) { return null; }
        @NotModified <T> T readValue(com.fasterxml.jackson.core.JsonParser p0, @NotModified com.fasterxml.jackson.databind.JavaType p1) { return null; }
        @NotModified <T extends com.fasterxml.jackson.core.TreeNode> T readTree(com.fasterxml.jackson.core.JsonParser p0) { return null; }
        @NotModified <T> com.fasterxml.jackson.databind.MappingIterator<T> readValues(com.fasterxml.jackson.core.JsonParser p0, @NotModified com.fasterxml.jackson.core.type.ResolvedType p1) { return null; }
        @NotModified <T> com.fasterxml.jackson.databind.MappingIterator<T> readValues(com.fasterxml.jackson.core.JsonParser p0, @NotModified com.fasterxml.jackson.databind.JavaType p1) { return null; }
        @NotModified <T> com.fasterxml.jackson.databind.MappingIterator<T> readValues(com.fasterxml.jackson.core.JsonParser p0, java.lang.Class<T> p1) { return null; }
        @NotModified <T> com.fasterxml.jackson.databind.MappingIterator<T> readValues(com.fasterxml.jackson.core.JsonParser p0, @NotModified com.fasterxml.jackson.core.type.TypeReference<T> p1) { return null; }
        @NotModified com.fasterxml.jackson.databind.JsonNode readTree(java.io.InputStream p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.JsonNode readTree(java.io.Reader p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.JsonNode readTree(java.lang.String p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.JsonNode readTree(@NotModified byte[] p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.JsonNode readTree(@NotModified byte[] p0, int p1, int p2) { return null; }
        @NotModified com.fasterxml.jackson.databind.JsonNode readTree(java.io.File p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.JsonNode readTree(java.net.URL p0) { return null; }
        @NotModified void writeValue(com.fasterxml.jackson.core.JsonGenerator p0, @NotModified java.lang.Object p1) { }
        @NotModified void writeTree(com.fasterxml.jackson.core.JsonGenerator p0, @NotModified com.fasterxml.jackson.core.TreeNode p1) { }
        @NotModified void writeTree(com.fasterxml.jackson.core.JsonGenerator p0, @NotModified com.fasterxml.jackson.databind.JsonNode p1) { }
        @NotModified com.fasterxml.jackson.databind.node.ObjectNode createObjectNode() { return null; }
        @NotModified com.fasterxml.jackson.databind.node.ArrayNode createArrayNode() { return null; }
        @NotModified com.fasterxml.jackson.databind.JsonNode missingNode() { return null; }
        @NotModified com.fasterxml.jackson.databind.JsonNode nullNode() { return null; }
        @NotModified com.fasterxml.jackson.core.JsonParser treeAsTokens(@NotModified com.fasterxml.jackson.core.TreeNode p0) { return null; }
        @NotModified <T> T treeToValue(@NotModified com.fasterxml.jackson.core.TreeNode p0, java.lang.Class<T> p1) { return null; }
        @NotModified <T> T treeToValue(@NotModified com.fasterxml.jackson.core.TreeNode p0, @NotModified com.fasterxml.jackson.databind.JavaType p1) { return null; }
        @NotModified <T> T treeToValue(@NotModified com.fasterxml.jackson.core.TreeNode p0, @NotModified com.fasterxml.jackson.core.type.TypeReference<T> p1) { return null; }
        @NotModified <T extends com.fasterxml.jackson.databind.JsonNode> T valueToTree(@NotModified java.lang.Object p0) { return null; }
        @NotModified boolean canSerialize(java.lang.Class<?> p0) { return false; }
        @NotModified boolean canSerialize(java.lang.Class<?> p0, @NotModified java.util.concurrent.atomic.AtomicReference<java.lang.Throwable> p1) { return false; }
        @NotModified boolean canDeserialize(@NotModified com.fasterxml.jackson.databind.JavaType p0) { return false; }
        @NotModified boolean canDeserialize(@NotModified com.fasterxml.jackson.databind.JavaType p0, @NotModified java.util.concurrent.atomic.AtomicReference<java.lang.Throwable> p1) { return false; }
        @NotModified <T> T readValue(java.io.File p0, java.lang.Class<T> p1) { return null; }
        @NotModified <T> T readValue(java.io.File p0, @NotModified com.fasterxml.jackson.core.type.TypeReference<T> p1) { return null; }
        @NotModified <T> T readValue(java.io.File p0, @NotModified com.fasterxml.jackson.databind.JavaType p1) { return null; }
        @NotModified <T> T readValue(java.net.URL p0, java.lang.Class<T> p1) { return null; }
        @NotModified <T> T readValue(java.net.URL p0, @NotModified com.fasterxml.jackson.core.type.TypeReference<T> p1) { return null; }
        @NotModified <T> T readValue(java.net.URL p0, @NotModified com.fasterxml.jackson.databind.JavaType p1) { return null; }
        @NotModified <T> T readValue(java.lang.String p0, java.lang.Class<T> p1) { return null; }
        @NotModified <T> T readValue(java.lang.String p0, @NotModified com.fasterxml.jackson.core.type.TypeReference<T> p1) { return null; }
        @NotModified <T> T readValue(java.lang.String p0, @NotModified com.fasterxml.jackson.databind.JavaType p1) { return null; }
        @NotModified <T> T readValue(java.io.Reader p0, java.lang.Class<T> p1) { return null; }
        @NotModified <T> T readValue(java.io.Reader p0, @NotModified com.fasterxml.jackson.core.type.TypeReference<T> p1) { return null; }
        @NotModified <T> T readValue(java.io.Reader p0, @NotModified com.fasterxml.jackson.databind.JavaType p1) { return null; }
        @NotModified <T> T readValue(java.io.InputStream p0, java.lang.Class<T> p1) { return null; }
        @NotModified <T> T readValue(java.io.InputStream p0, @NotModified com.fasterxml.jackson.core.type.TypeReference<T> p1) { return null; }
        @NotModified <T> T readValue(java.io.InputStream p0, @NotModified com.fasterxml.jackson.databind.JavaType p1) { return null; }
        @NotModified <T> T readValue(@NotModified byte[] p0, java.lang.Class<T> p1) { return null; }
        @NotModified <T> T readValue(@NotModified byte[] p0, int p1, int p2, java.lang.Class<T> p3) { return null; }
        @NotModified <T> T readValue(@NotModified byte[] p0, @NotModified com.fasterxml.jackson.core.type.TypeReference<T> p1) { return null; }
        @NotModified <T> T readValue(@NotModified byte[] p0, int p1, int p2, @NotModified com.fasterxml.jackson.core.type.TypeReference<T> p3) { return null; }
        @NotModified <T> T readValue(@NotModified byte[] p0, @NotModified com.fasterxml.jackson.databind.JavaType p1) { return null; }
        @NotModified <T> T readValue(@NotModified byte[] p0, int p1, int p2, @NotModified com.fasterxml.jackson.databind.JavaType p3) { return null; }
        @NotModified <T> T readValue(java.io.DataInput p0, java.lang.Class<T> p1) { return null; }
        @NotModified <T> T readValue(java.io.DataInput p0, @NotModified com.fasterxml.jackson.databind.JavaType p1) { return null; }
        @NotModified void writeValue(java.io.File p0, @NotModified java.lang.Object p1) { }
        @NotModified void writeValue(java.io.OutputStream p0, @NotModified java.lang.Object p1) { }
        @NotModified void writeValue(java.io.DataOutput p0, @NotModified java.lang.Object p1) { }
        @NotModified void writeValue(java.io.Writer p0, @NotModified java.lang.Object p1) { }
        @NotModified java.lang.String writeValueAsString(@NotModified java.lang.Object p0) { return null; }
        @NotModified byte[] writeValueAsBytes(@NotModified java.lang.Object p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writer() { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writer(@NotModified com.fasterxml.jackson.databind.SerializationFeature p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writer(@NotModified com.fasterxml.jackson.databind.SerializationFeature p0, @NotModified com.fasterxml.jackson.databind.SerializationFeature... p1) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writer(@NotModified java.text.DateFormat p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writerWithView(java.lang.Class<?> p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writerFor(java.lang.Class<?> p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writerFor(@NotModified com.fasterxml.jackson.core.type.TypeReference<?> p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writerFor(@NotModified com.fasterxml.jackson.databind.JavaType p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writer(@NotModified com.fasterxml.jackson.core.PrettyPrinter p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writerWithDefaultPrettyPrinter() { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writer(@NotModified com.fasterxml.jackson.databind.ser.FilterProvider p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writer(@NotModified com.fasterxml.jackson.core.FormatSchema p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writer(@NotModified com.fasterxml.jackson.core.Base64Variant p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writer(@NotModified com.fasterxml.jackson.core.io.CharacterEscapes p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writer(@NotModified com.fasterxml.jackson.databind.cfg.ContextAttributes p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writerWithType(java.lang.Class<?> p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writerWithType(@NotModified com.fasterxml.jackson.core.type.TypeReference<?> p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectWriter writerWithType(@NotModified com.fasterxml.jackson.databind.JavaType p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader reader() { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader reader(@NotModified com.fasterxml.jackson.databind.DeserializationFeature p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader reader(@NotModified com.fasterxml.jackson.databind.DeserializationFeature p0, @NotModified com.fasterxml.jackson.databind.DeserializationFeature... p1) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader readerForUpdating(java.lang.Object p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader readerFor(@NotModified com.fasterxml.jackson.databind.JavaType p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader readerFor(java.lang.Class<?> p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader readerFor(@NotModified com.fasterxml.jackson.core.type.TypeReference<?> p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader readerForArrayOf(java.lang.Class<?> p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader readerForListOf(java.lang.Class<?> p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader readerForMapOf(java.lang.Class<?> p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader reader(@NotModified com.fasterxml.jackson.databind.node.JsonNodeFactory p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader reader(@NotModified com.fasterxml.jackson.core.FormatSchema p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader reader(@NotModified com.fasterxml.jackson.databind.InjectableValues p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader readerWithView(java.lang.Class<?> p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader reader(@NotModified com.fasterxml.jackson.core.Base64Variant p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader reader(@NotModified com.fasterxml.jackson.databind.cfg.ContextAttributes p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader reader(@NotModified com.fasterxml.jackson.databind.JavaType p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader reader(java.lang.Class<?> p0) { return null; }
        @NotModified com.fasterxml.jackson.databind.ObjectReader reader(@NotModified com.fasterxml.jackson.core.type.TypeReference<?> p0) { return null; }
        @NotModified <T> T convertValue(@NotModified java.lang.Object p0, java.lang.Class<T> p1) { return null; }
        @NotModified <T> T convertValue(@NotModified java.lang.Object p0, @NotModified com.fasterxml.jackson.core.type.TypeReference<T> p1) { return null; }
        @NotModified <T> T convertValue(@NotModified java.lang.Object p0, @NotModified com.fasterxml.jackson.databind.JavaType p1) { return null; }
        @NotModified <T> T updateValue(T p0, java.lang.Object p1) { return null; }
        @NotModified com.fasterxml.jackson.databind.jsonschema.JsonSchema generateJsonSchema(java.lang.Class<?> p0) { return null; }
        @NotModified void acceptJsonFormatVisitor(java.lang.Class<?> p0, com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper p1) { }
        @NotModified void acceptJsonFormatVisitor(@NotModified com.fasterxml.jackson.databind.JavaType p0, com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper p1) { }
        @NotModified com.fasterxml.jackson.core.TreeNode createArrayNode() { return null; }
        @NotModified com.fasterxml.jackson.core.TreeNode createObjectNode() { return null; }
        @NotModified java.util.Iterator readValues(com.fasterxml.jackson.core.JsonParser p0, @NotModified com.fasterxml.jackson.core.type.ResolvedType p1) { return null; }
        @NotModified java.util.Iterator readValues(com.fasterxml.jackson.core.JsonParser p0, @NotModified com.fasterxml.jackson.core.type.TypeReference p1) { return null; }
        @NotModified java.util.Iterator readValues(com.fasterxml.jackson.core.JsonParser p0, java.lang.Class p1) { return null; }
        @NotModified com.fasterxml.jackson.core.TreeNode nullNode() { return null; }
        @NotModified com.fasterxml.jackson.core.TreeNode missingNode() { return null; }
    }
}
