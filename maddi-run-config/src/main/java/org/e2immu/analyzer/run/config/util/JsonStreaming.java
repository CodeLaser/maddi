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
import org.e2immu.analyzer.aapi.parser.AnalysisHintsConfiguration;
import org.e2immu.analyzer.aapi.parser.AnalysisHintsConfigurationImpl;
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
        resolver.addMapping(AnalysisHintsConfiguration.class, AnalysisHintsConfigurationImpl.class);
        resolver.addMapping(LanguageConfiguration.class, LanguageConfigurationImpl.class);

        module.setAbstractTypes(resolver);

        // only because we want to get the order straight: a correct linearization of the dependencies between the
        // source sets
        module.addSerializer(new InputConfigurationSerializer(InputConfigurationImpl.class));
        module.addSerializer(new SourceSetSerializer(SourceSetImpl.class));
        module.addSerializer(new AnalysisHintsConfigurationSerializer(AnalysisHintsConfigurationImpl.class));
        module.addDeserializer(SourceSetImpl.class, new SourceSetDeserializer(SourceSetImpl.class));
        module.addDeserializer(AnalysisHintsConfigurationImpl.class,
                new AnalysisHintsConfigurationDeserializer(AnalysisHintsConfigurationImpl.class));
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
            boolean module = partOfJdk || getBoolean(node, "module");
            Set<String> restrictToPackages = new HashSet<>();
            JsonNode restrictToPackagesNode = node.get("restrictToPackages");
            if (restrictToPackagesNode != null) {
                for (JsonNode jsonNode : restrictToPackagesNode) {
                    restrictToPackages.add(jsonNode.asText());
                }
            }
            List<SourceSet> dependencies = new ArrayList<>();
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
            SourceSet ssi = new SourceSetImpl.Builder().setName(name)
                    .setSourceDirectories(sourceDirectories)
                    .setUri(uri)
                    .setSourceEncoding(sourceEncoding)
                    .setTest(test).setLibrary(library).setExternalLibrary(externalLibrary)
                    .setPartOfJdk(partOfJdk).setModule(module)
                    .setRestrictToPackages(Set.copyOf(restrictToPackages))
                    .setDependencies(List.copyOf(dependencies))
                    .build();
            String fingerPrintToString = getString(node, "fingerPrint", "");
            if (!fingerPrintToString.isBlank()) {
                ssi.setFingerPrint(MD5FingerPrint.from(fingerPrintToString));
            }
            String analysisFingerPrintToString = getString(node, "analysisFingerPrint", "");
            if (!analysisFingerPrintToString.isBlank()) {
                ssi.setAnalysisFingerPrint(MD5FingerPrint.from(analysisFingerPrintToString));
            }
            ctxt.setAttribute(name, ssi);

            return (SourceSetImpl) ssi;
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
            // a JPMS module: its dependencies go on javac's module path rather than the classpath. Parts of
            // the JDK are always modules and re-derive it from partOfJdk, so only write it when it adds
            // something -- but write it we must, or a modular project cannot survive this round trip.
            if (value.isModule() && !value.partOfJdk()) gen.writeBooleanField("module", value.isModule());
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

    // AnalysisHintsConfigurationImpl is jackson-free (like the other config impls); serialize/deserialize its
    // flat fields here through its builder.
    static class AnalysisHintsConfigurationSerializer extends StdSerializer<AnalysisHintsConfigurationImpl> {
        public AnalysisHintsConfigurationSerializer(Class<AnalysisHintsConfigurationImpl> t) {
            super(t);
        }

        @Override
        public void serialize(AnalysisHintsConfigurationImpl value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeArrayFieldStart("preloadAnalysisResultsDirs");
            for (String d : value.preloadAnalysisResultsDirs()) gen.writeString(d);
            gen.writeEndArray();
            if (value.analysisResultsTargetDir() != null) {
                gen.writeStringField("analysisResultsTargetDir", value.analysisResultsTargetDir());
            }
            gen.writeArrayFieldStart("hintsPackages");
            for (String p : value.hintsPackages()) gen.writeString(p);
            gen.writeEndArray();
            if (value.updatedHintsDir() != null) {
                gen.writeStringField("updatedHintsDir", value.updatedHintsDir());
            }
            if (value.updatedHintsPackage() != null) {
                gen.writeStringField("updatedHintsPackage", value.updatedHintsPackage());
            }
            gen.writeEndObject();
        }
    }

    static class AnalysisHintsConfigurationDeserializer extends StdDeserializer<AnalysisHintsConfigurationImpl> {
        public AnalysisHintsConfigurationDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public AnalysisHintsConfigurationImpl deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);
            AnalysisHintsConfigurationImpl.Builder builder = new AnalysisHintsConfigurationImpl.Builder();
            JsonNode dirs = node.get("preloadAnalysisResultsDirs");
            if (dirs != null) for (JsonNode n : dirs) builder.addPreloadAnalysisResultsDirs(n.asText());
            JsonNode packages = node.get("hintsPackages");
            if (packages != null) for (JsonNode n : packages) builder.addHintsPackages(n.asText());
            if (node.hasNonNull("analysisResultsTargetDir")) {
                builder.setAnalysisResultsTargetDir(node.get("analysisResultsTargetDir").asText());
            }
            if (node.hasNonNull("updatedHintsDir")) {
                builder.setUpdatedHintsDir(node.get("updatedHintsDir").asText());
            }
            if (node.hasNonNull("updatedHintsPackage")) {
                builder.setUpdatedHintsPackage(node.get("updatedHintsPackage").asText());
            }
            return (AnalysisHintsConfigurationImpl) builder.build();
        }
    }
}
