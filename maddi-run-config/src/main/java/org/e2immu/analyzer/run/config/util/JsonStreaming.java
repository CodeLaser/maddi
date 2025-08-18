package org.e2immu.analyzer.run.config.util;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.e2immu.analyzer.aapi.parser.AnnotatedAPIConfiguration;
import org.e2immu.analyzer.aapi.parser.AnnotatedAPIConfigurationImpl;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.runtime.LanguageConfiguration;
import org.e2immu.language.cst.impl.runtime.LanguageConfigurationImpl;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.api.resource.MD5FingerPrint;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JsonStreaming {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonStreaming.class);

    public static SimpleModule configModule() {
        SimpleModule module = new SimpleModule("e2immuConfig", Version.unknownVersion());

        SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();
        resolver.addMapping(SourceSet.class, SourceSetImpl.class);
        resolver.addMapping(InputConfiguration.class, InputConfigurationImpl.class);
        resolver.addMapping(AnnotatedAPIConfiguration.class, AnnotatedAPIConfigurationImpl.class);
        resolver.addMapping(LanguageConfiguration.class, LanguageConfigurationImpl.class);

        module.setAbstractTypes(resolver);

        // only because we want to get the order straight: a correct linearization of the dependencies between the
        // source sets
        module.addSerializer(new InputConfigurationSerializer(InputConfigurationImpl.class));
        module.addSerializer(new SourceSetSerializer(SourceSetImpl.class));
        module.addDeserializer(SourceSetImpl.class, new SourceSetDeserializer(SourceSetImpl.class));
        //FIXME at the moment, AAPIConfig does not have a @JsonProperty in Configuration, so it is skipped
        return module;
    }

    public static ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(configModule());
        return mapper;
    }

    private static boolean getBoolean(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value != null && value.asBoolean();
    }

    private static String getString(JsonNode node, String key, String defaultValue) {
        JsonNode value = node.get(key);
        return value == null ? defaultValue : value.asText();
    }

    static class SourceSetDeserializer extends StdDeserializer<SourceSetImpl> {

        public SourceSetDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public SourceSetImpl deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {
            JsonNode node = jp.getCodec().readTree(jp);
            String name = node.get("name").asText();
            List<Path> sourceDirectories = new ArrayList<>();
            JsonNode sourceDirs = node.get("sourceDirectories");
            if (sourceDirs != null) {
                for (JsonNode sourceDir : sourceDirs) {
                    sourceDirectories.add(Path.of(sourceDir.asText()));
                }
            }
            String uriString = node.get("uri").asText("");
            URI uri = uriString.isBlank() ? null : URI.create(uriString);
            String sourceEncodingString = getString(node, "sourceEncoding", StandardCharsets.UTF_8.toString());
            Charset sourceEncoding = sourceEncodingString.isBlank() ? StandardCharsets.UTF_8 :
                    Charset.forName(sourceEncodingString);
            boolean test = getBoolean(node, "test");
            boolean library = getBoolean(node, "library");
            boolean externalLibrary = getBoolean(node, "externalLibrary");
            boolean partOfJdk = getBoolean(node, "partOfJdk");
            boolean runtimeOnly = getBoolean(node, "runtimeOnly");
            Set<String> restrictToPackages = new HashSet<>();
            JsonNode restrictToPackagesNode = node.get("restrictToPackages");
            if (restrictToPackagesNode != null) {
                for (JsonNode jsonNode : restrictToPackagesNode) {
                    restrictToPackages.add(jsonNode.asText());
                }
            }
            Set<SourceSet> dependencies = new HashSet<>();
            JsonNode dependenciesNode = node.get("dependencies");
            if (dependenciesNode != null) {
                for (JsonNode subNode : dependenciesNode) {
                    String key = subNode.asText();
                    SourceSet dependency = (SourceSet) ctxt.getAttribute(key);
                    if (dependency != null) {
                        dependencies.add(dependency);
                    } else {
                        LOGGER.warn("dependency named '{}' unknown", key);
                    }
                }
            }
            SourceSetImpl ssi = new SourceSetImpl(name, sourceDirectories, uri, sourceEncoding, test, library, externalLibrary,
                    partOfJdk, runtimeOnly, Set.copyOf(restrictToPackages), Set.copyOf(dependencies));
            String fingerPrintToString = getString(node, "fingerPrint", "");
            if (!fingerPrintToString.isBlank()) {
                ssi.setFingerPrint(MD5FingerPrint.from(fingerPrintToString));
            }
            String analysisFingerPrintToString = getString(node, "analysisFingerPrint", "");
            if (!analysisFingerPrintToString.isBlank()) {
                ssi.setAnalysisFingerPrint(MD5FingerPrint.from(analysisFingerPrintToString));
            }
            ctxt.setAttribute(name, ssi);

            return ssi;
        }
    }

    static class SourceSetSerializer extends StdSerializer<SourceSetImpl> {

        public SourceSetSerializer(Class<SourceSetImpl> t) {
            super(t);
        }

        @Override
        public void serialize(SourceSetImpl value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            if (value.sourceEncoding() != null) {
                gen.writeStringField("sourceEncoding", value.sourceEncoding().name());
            }
            gen.writeStringField("name", value.name());
            if (value.sourceDirectories() != null && !value.sourceDirectories().isEmpty()) {
                gen.writeArrayFieldStart("sourceDirectories");
                for (Path dir : value.sourceDirectories()) gen.writeString(dir.toString());
                gen.writeEndArray();
            }
            gen.writeStringField("uri", value.uri().toString());
            if (value.test()) gen.writeBooleanField("test", value.test());
            if (value.library()) gen.writeBooleanField("library", value.library());
            if (value.externalLibrary()) gen.writeBooleanField("externalLibrary", value.externalLibrary());
            if (value.partOfJdk()) gen.writeBooleanField("partOfJdk", value.partOfJdk());
            if (value.runtimeOnly()) gen.writeBooleanField("runtimeOnly", value.runtimeOnly());
            if (value.restrictToPackages() != null) {
                gen.writeArrayFieldStart("restrictToPackages");
                for (String pkg : value.restrictToPackages()) gen.writeString(pkg);
                gen.writeEndArray();
            }
            if (value.dependencies() != null && !value.dependencies().isEmpty()) {
                gen.writeArrayFieldStart("dependencies");
                for (SourceSet d : value.dependencies()) gen.writeString(d.name());
                gen.writeEndArray();
            }
            if (value.fingerPrintOrNull() != null && !value.fingerPrintOrNull().isNoFingerPrint()) {
                gen.writeStringField("fingerPrint", value.fingerPrintOrNull().toString());
            }
            if (value.analysisFingerPrintOrNull() != null && !value.analysisFingerPrintOrNull().isNoFingerPrint()) {
                gen.writeStringField("analysisFingerPrint", value.analysisFingerPrintOrNull().toString());
            }
            gen.writeEndObject();
        }
    }

    static class InputConfigurationSerializer extends StdSerializer<InputConfigurationImpl> {
        public InputConfigurationSerializer(Class<InputConfigurationImpl> t) {
            super(t);
        }

        @Override
        public void serialize(InputConfigurationImpl value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("workingDirectory", value.workingDirectory() == null ? null :
                    value.workingDirectory().toString());
            gen.writeArrayFieldStart("classPathParts");
            for (SourceSet cpp : value.classPathParts()) {
                gen.writeObject(cpp);
            }
            gen.writeEndArray();
            gen.writeArrayFieldStart("sourceSets");
            for (SourceSet cpp : value.sourceSets()) {
                gen.writeObject(cpp);
            }
            gen.writeEndArray();
            gen.writeStringField("alternativeJREDirectory", value.alternativeJREDirectory() == null ? null
                    : value.alternativeJREDirectory().toString());
            gen.writeEndObject();
        }
    }
}
